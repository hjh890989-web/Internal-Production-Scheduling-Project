#!/usr/bin/env python3
"""
RCPM (Resource-Constrained Critical Path Method) 기반 Task별 일자 자동 계산
+ GitHub Project Start/Target 필드 update.

배경:
    기존 09-fill-date-fields.ps1 은 Sprint 단위 동일 일자 부여 → Roadmap에서 모두 겹침.
    초기 11번 (capacity 모드) 은 (Sprint, Owner) PD 누적만 → cross-owner 의존성·자원 한계 무시.
    본 RCPM 모드 = 진짜 DAG (각 Task 파일의 "선행/후행") + 자원 제약 동시 반영.

알고리즘:
    1. Task 파일 (Phase 2/4.Tasks/Tasks/EP-*/ST-*/TK-*.md) 모두 파싱
       — sprint·epic·story·owner·PD + predecessors (선행 Task ID 집합)
    2. Predecessor 필터 (자기 자신/존재하지 않는 ID 제거) → DAG 구축
    3. Topological sort (Kahn) — Sprint 순서 + id tie-break
    4. RCPM forward pass:
         earliest_start = max(
             모든 predecessor.end + 1 영업일,
             owner_busy_until[primary_owner],
             sprint_start
         )
         end = add_business_days(start, ceil(pd) - 1)
         owner_busy_until[primary_owner] = end + 1 영업일
       — multi-owner (`backend+qa`) 는 첫 owner 만 자원 점유 (단순화)
    5. Critical Path = backward pass 후 slack=0 chain → 리포트에 표시
    6. gh CLI 통해 두 필드 쌍 update (Start date/Target date + Start/Target)

사용:
    python scripts/github-sync/11-fill-task-level-dates.py --dry-run    # 계산만, GitHub 미연동
    python scripts/github-sync/11-fill-task-level-dates.py --report scripts/github-sync/task_dates_report.md
    python scripts/github-sync/11-fill-task-level-dates.py              # 실제 update

표준 라이브러리만 사용 (subprocess + json + datetime + pathlib + re + argparse + math).
"""
import argparse
import io
import json
import math
import re
import subprocess
import sys
from collections import defaultdict
from datetime import date, timedelta
from pathlib import Path
from typing import Optional

# Windows cp949 환경 안전
if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', line_buffering=True)


# ---------- Sprint 일정 ----------
# PLAN-001 + WBS v1.2 §12.1 기반. 각 Sprint = 2주 (10 영업일).
SPRINT_RANGES = {
    'S0': (date(2026, 5, 18), date(2026, 5, 29)),   # 10 영업일
    'S1': (date(2026, 6,  1), date(2026, 6, 12)),
    'S2': (date(2026, 6, 15), date(2026, 6, 26)),
    'S3': (date(2026, 6, 29), date(2026, 7, 10)),
    'S4': (date(2026, 7, 13), date(2026, 7, 24)),
    'S5': (date(2026, 7, 27), date(2026, 8,  7)),
}
# Sprint label이 없는 NFR/cross-cutting Epic은 전 기간 분산
FALLBACK_RANGE = (date(2026, 5, 18), date(2026, 8, 7))

# ---------- Task 파일 파싱 패턴 ----------
# ID prefix 형식 (도메인 segment): 알파벳·숫자 혼합 자유. 예:
#   TK-00-1-1, TK-EX13-1-1, TK-VC15-1-2, TK-E2E-1-1   (마지막 segment들은 숫자)
#   EP-07, EP-EX13, EP-E2E
#   ST-07-1, ST-EX13-1, ST-E2E-1
TASK_ID_RE   = re.compile(r'\*\*Task ID\*\*[:：]\s*(TK-[A-Z0-9]+(?:-\d+)+)')
PD_RE        = re.compile(r'\*\*추정\*\*[:：]\s*([0-9.]+)\s*PD', re.IGNORECASE)
TITLE_RE     = re.compile(r'^title:\s*"?\[(TK-[A-Z0-9]+(?:-\d+)+)\]\s*(.*?)"?$', re.MULTILINE)
LABELS_RE    = re.compile(r"^labels:\s*'([^']*)'", re.MULTILINE)
SPRINT_RE    = re.compile(r'sprint:(S\d|Deferred|Cross\-?cutting)')
EPIC_RE      = re.compile(r'epic:(EP-[A-Z0-9]+)')
STORY_RE     = re.compile(r'story:(ST-[A-Z0-9]+-\d+)')
OWNER_RE     = re.compile(r'owner:([a-zA-Z+]+)')

# ---------- 데이터 구조 ----------

TK_ID_INLINE_RE = re.compile(r'TK-[A-Z0-9]+(?:-\d+)+')

class Task:
    __slots__ = ('id', 'title', 'sprint', 'epic', 'story', 'owner', 'pd', 'path',
                 'predecessors', 'start', 'end')

    def __init__(self, path: Path):
        self.path: Path = path
        self.id: Optional[str] = None
        self.title: Optional[str] = None
        self.sprint: Optional[str] = None
        self.epic: Optional[str] = None
        self.story: Optional[str] = None
        self.owner: Optional[str] = None
        self.pd: float = 0.5  # 기본값 (PD 미명시 시)
        self.predecessors: set[str] = set()
        self.start: Optional[date] = None
        self.end: Optional[date] = None

    def parse(self) -> bool:
        """파일에서 모든 메타 추출. 실패 시 False."""
        try:
            content = self.path.read_text(encoding='utf-8')
        except OSError as e:
            print(f"  ⚠️ read fail {self.path.name}: {e}", file=sys.stderr)
            return False

        # Task ID
        m = TASK_ID_RE.search(content) or TITLE_RE.search(content)
        if not m:
            return False
        self.id = m.group(1)
        if hasattr(m, 'lastindex') and m.lastindex and m.lastindex >= 2:
            self.title = (m.group(2) or '').strip().rstrip('"')

        # labels (frontmatter)
        m_labels = LABELS_RE.search(content)
        if m_labels:
            labels = m_labels.group(1)
            m_sprint = SPRINT_RE.search(labels)
            m_epic   = EPIC_RE.search(labels)
            m_story  = STORY_RE.search(labels)
            m_owner  = OWNER_RE.search(labels)
            self.sprint = m_sprint.group(1) if m_sprint else None
            self.epic   = m_epic.group(1)   if m_epic   else None
            self.story  = m_story.group(1)  if m_story  else None
            self.owner  = m_owner.group(1).lower() if m_owner else None

        # PD
        m_pd = PD_RE.search(content)
        if m_pd:
            try:
                self.pd = float(m_pd.group(1))
            except ValueError:
                pass

        # Predecessors — 4가지 표기 모두 처리:
        #   - **연관**: 선행 [TK-X](url), [TK-Y](url), 후행 [TK-Z](url)
        #   - **선행**: [TK-X](url) (코멘트)
        #   -   선행 (모두): [TK-X](url)
        #   - 연관: 선행 ..., 후행 ...
        # 전략: 각 줄에서 '선행' 키워드 발견 → 그 줄(+ 후속 1줄)에서
        #       '후행' 이후를 제외한 부분의 TK-* ID 모두 수집.
        lines = content.splitlines()
        for i, line in enumerate(lines):
            if '선행' not in line:
                continue
            chunk = line
            # 다음 줄이 같은 리스트 항목(들여쓰기 + ID만 있는 경우) 이어붙임
            if i + 1 < len(lines):
                nxt = lines[i + 1]
                if (nxt.strip().startswith(('[TK-', '- [TK-', '* [TK-'))
                        and '선행' not in nxt and '후행' not in nxt):
                    chunk += '\n' + nxt
            # '후행' 이후는 잘라냄 (후행 ID는 reverse edge — 사용 안 함)
            before_hu = chunk.split('후행')[0]
            for tk in TK_ID_INLINE_RE.findall(before_hu):
                if tk != self.id:
                    self.predecessors.add(tk)

        return True


# ---------- 영업일 계산 ----------

def is_business_day(d: date) -> bool:
    return d.weekday() < 5  # Mon=0 ~ Fri=4

def add_business_days(start: date, n: int) -> date:
    """start 부터 n 영업일 더한 일자 (n=0이면 start 자체. 주말이면 다음 월요일)."""
    if n < 0:
        raise ValueError("n must be >= 0")
    d = start
    # 시작이 주말이면 다음 영업일로 이동
    while not is_business_day(d):
        d += timedelta(days=1)
    days_added = 0
    while days_added < n:
        d += timedelta(days=1)
        if is_business_day(d):
            days_added += 1
    return d


# ---------- 메인 알고리즘 ----------

def collect_tasks(root: Path) -> list[Task]:
    """Phase 2/4.Tasks/Tasks/ 아래 모든 TK-*.md 파일 수집."""
    task_files = list(root.glob('Phase 2/4.Tasks/Tasks/EP-*/ST-*/TK-*.md'))
    tasks: list[Task] = []
    skipped = 0
    for f in task_files:
        t = Task(f)
        if t.parse() and t.id:
            tasks.append(t)
        else:
            skipped += 1
    print(f"📂 Task 파일: {len(task_files)} 발견 / {len(tasks)} 파싱 성공 / {skipped} 건너뜀")
    return tasks


SPRINT_ORDER = {f'S{i}': i for i in range(10)}


def _sprint_range(t: Task) -> tuple[date, date]:
    return SPRINT_RANGES.get(t.sprint, FALLBACK_RANGE)


def _primary_owner(t: Task) -> str:
    """multi-owner (`backend+qa`)의 첫 번째만 자원 점유 — 단순화 가정."""
    return (t.owner or 'unassigned').split('+')[0]


def schedule_rcpm(tasks: list[Task]) -> tuple[list[Task], list[Task]]:
    """
    Resource-Constrained Critical Path Method.
    forward pass — 각 Task earliest_start = max(predecessors+1day, owner_busy, sprint_start)
    backward pass — slack=0 chain = Critical Path
    반환: (topo_order, critical_path)
    """
    # 1. predecessor 정리 — 존재하지 않는 ID는 drop
    by_id = {t.id: t for t in tasks if t.id}
    for t in tasks:
        t.predecessors = {p for p in t.predecessors if p in by_id and p != t.id}

    # 2. Kahn 토폴로지 정렬 — 우선순위: Sprint 순 → epic → id
    in_degree = {t.id: len(t.predecessors) for t in tasks if t.id}
    dependents: dict[str, list[str]] = defaultdict(list)
    for t in tasks:
        if not t.id:
            continue
        for p in t.predecessors:
            dependents[p].append(t.id)

    ready = [t for t in tasks if t.id and in_degree[t.id] == 0]
    topo: list[Task] = []

    def _ready_key(t: Task) -> tuple:
        return (SPRINT_ORDER.get(t.sprint or '', 99), t.epic or '', t.id or '')

    while ready:
        ready.sort(key=_ready_key)
        t = ready.pop(0)
        topo.append(t)
        for dep_id in dependents.get(t.id or '', []):
            in_degree[dep_id] -= 1
            if in_degree[dep_id] == 0:
                ready.append(by_id[dep_id])

    if len(topo) < len(by_id):
        leftover = [t for t in tasks if t.id and t not in topo]
        print(f"  ⚠️ DAG cycle 의심 — 토폴로지 미정렬 Task {len(leftover)}건. "
              f"sprint 순으로 강제 추가.", file=sys.stderr)
        leftover.sort(key=_ready_key)
        topo.extend(leftover)

    # 3. Forward pass — earliest start/end
    owner_busy_until: dict[str, date] = {}
    for t in topo:
        sprint_start, sprint_end = _sprint_range(t)

        earliest = sprint_start
        for p_id in t.predecessors:
            p = by_id[p_id]
            if p.end:
                cand = add_business_days(p.end, 1)
                if cand > earliest:
                    earliest = cand

        primary = _primary_owner(t)
        owner_avail = owner_busy_until.get(primary, sprint_start)
        if owner_avail > earliest:
            earliest = owner_avail

        # Sprint 종료 넘어가면 Sprint 끝에 채워넣음 (overflow 표시 목적)
        if earliest > sprint_end:
            earliest = sprint_end

        pd_days = max(1, math.ceil(t.pd))
        t.start = earliest
        t.end = add_business_days(earliest, pd_days - 1)
        if t.end > sprint_end:
            t.end = sprint_end

        owner_busy_until[primary] = add_business_days(t.end, 1)

    # 4. Backward pass — latest start/end → slack 계산 → Critical Path
    # project_finish = 모든 Task end의 최댓값
    project_finish = max((t.end for t in topo if t.end), default=date.today())
    latest_end: dict[str, date] = {t.id: project_finish for t in topo if t.id}

    # 역방향 순회 — 후행자가 모두 결정된 후 자신을 갱신
    for t in reversed(topo):
        deps = dependents.get(t.id or '', [])
        if deps:
            # latest_end = min(후행자의 latest_start - 1 영업일)
            min_succ_start = None
            for dep_id in deps:
                dep_task = by_id[dep_id]
                if dep_task.start is None:
                    continue
                # dep의 latest_start = latest_end[dep] - (pd-1)
                dep_pd_days = max(1, math.ceil(dep_task.pd))
                dep_le = latest_end.get(dep_id, project_finish)
                dep_ls = _subtract_business_days(dep_le, dep_pd_days - 1)
                cand = _subtract_business_days(dep_ls, 1)
                if min_succ_start is None or cand < min_succ_start:
                    min_succ_start = cand
            if min_succ_start is not None and min_succ_start < latest_end[t.id]:
                latest_end[t.id] = min_succ_start

    # slack = latest_end - earliest_end. slack=0 → Critical
    critical: list[Task] = []
    for t in topo:
        if t.id and t.end and latest_end.get(t.id) == t.end:
            critical.append(t)

    return topo, critical


def _subtract_business_days(start: date, n: int) -> date:
    """start 부터 n 영업일 뺀 일자."""
    if n <= 0:
        return start
    d = start
    while not is_business_day(d):
        d -= timedelta(days=1)
    removed = 0
    while removed < n:
        d -= timedelta(days=1)
        if is_business_day(d):
            removed += 1
    return d


# ---------- GitHub Project 연동 ----------

def _resolve_gh() -> str:
    """gh CLI 실행 가능 경로 탐색 (PATH → Windows 표준 설치 위치 fallback)."""
    import os, shutil
    found = shutil.which('gh')
    if found:
        return found
    # Windows 표준 위치
    candidates = [
        Path(os.environ.get('ProgramFiles', '')) / 'GitHub CLI' / 'gh.exe',
        Path(os.environ.get('ProgramFiles(x86)', '')) / 'GitHub CLI' / 'gh.exe',
        Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs' / 'GitHub CLI' / 'gh.exe',
    ]
    for c in candidates:
        if c.exists():
            return str(c)
    raise FileNotFoundError("gh CLI 미발견 — PATH 또는 표준 설치 위치 모두 부재")

_GH_BIN: Optional[str] = None

def gh_run(args: list[str]) -> tuple[int, str]:
    """gh CLI 실행. (exit_code, stdout+stderr 합본) 반환."""
    global _GH_BIN
    try:
        if _GH_BIN is None:
            _GH_BIN = _resolve_gh()
        r = subprocess.run([_GH_BIN] + args, capture_output=True, text=True,
                           encoding='utf-8', errors='replace')
        return r.returncode, (r.stdout or '') + (r.stderr or '')
    except FileNotFoundError as e:
        print(f"❌ {e}", file=sys.stderr)
        sys.exit(1)


def fetch_project_meta(project: int, owner: str) -> tuple[str, dict[str, str]]:
    """프로젝트 ID + 필드 ID 사전 (name → id) 반환."""
    code, out = gh_run(['project', 'view', str(project), '--owner', owner,
                        '--format', 'json'])
    if code != 0:
        print(f"❌ project view 실패:\n{out}", file=sys.stderr)
        sys.exit(1)
    proj = json.loads(out)

    code, out = gh_run(['project', 'field-list', str(project), '--owner', owner,
                        '--format', 'json', '--limit', '50'])
    if code != 0:
        print(f"❌ field-list 실패:\n{out}", file=sys.stderr)
        sys.exit(1)
    fields = json.loads(out)['fields']
    field_ids = {f['name']: f['id'] for f in fields}
    return proj['id'], field_ids


def fetch_project_items(project: int, owner: str) -> list[dict]:
    """프로젝트의 모든 item (Issue 포함) 반환."""
    code, out = gh_run(['project', 'item-list', str(project), '--owner', owner,
                        '--limit', '2000', '--format', 'json'])
    if code != 0:
        print(f"❌ item-list 실패:\n{out}", file=sys.stderr)
        sys.exit(1)
    return json.loads(out).get('items', [])


def map_tasks_to_items(tasks: list[Task], items: list[dict]) -> dict[str, dict]:
    """
    Task ID → Project item 매핑.
    Title이 'TK-NN-M-K' 로 시작하는 Task-named Issue를 최우선.
    Title 매칭 실패 시에만 body 본문 fallback (Epic 본문이 Task 참조하는 경우 등).
    """
    by_id: dict[str, dict] = {}
    tk_pattern  = re.compile(r'\b(TK-[A-Z0-9]+(?:-\d+)+)\b')
    tk_title_re = re.compile(r'\[?(TK-[A-Z0-9]+(?:-\d+)+)\]?')

    # 1단계: title이 'TK-…'로 시작하는 Task Issue 우선
    for item in items:
        if item.get('content', {}).get('type') != 'Issue':
            continue
        title = item.get('content', {}).get('title', '') or ''
        m = tk_title_re.match(title.lstrip())
        if m:
            tk_id = m.group(1)
            # 같은 TK가 title인 Issue가 둘 이상이면 첫 번째 유지
            if tk_id not in by_id:
                by_id[tk_id] = item

    # 2단계: title 매칭 실패한 TK는 body 본문 fallback
    pending = {t.id for t in tasks if t.id and t.id not in by_id}
    if pending:
        for item in items:
            if item.get('content', {}).get('type') != 'Issue':
                continue
            body = item.get('content', {}).get('body', '') or ''
            for tk_id in tk_pattern.findall(body):
                if tk_id in pending and tk_id not in by_id:
                    by_id[tk_id] = item

    mapped = 0
    for t in tasks:
        if t.id and t.id in by_id:
            mapped += 1
    print(f"🔗 Task ↔ Issue 매핑: {mapped} / {len(tasks)} (title 우선, body fallback)")
    return by_id


def update_date_fields(tasks: list[Task], task_to_item: dict[str, dict],
                       project_id: str,
                       field_pairs: list[tuple[str, str, str]]) -> tuple[int, int]:
    """
    각 Task의 일자 필드 update.
    field_pairs: [(label, start_field_id, target_field_id), …]
        — 여러 쌍을 모두 update (예: 'Start/Target' 커스텀 + 'Start date/Target date' 표준).
    (success, failure) 반환.
    """
    ok = 0
    fail = 0
    for i, t in enumerate(tasks, 1):
        if not t.id or t.id not in task_to_item or t.start is None or t.end is None:
            continue
        item_id = task_to_item[t.id]['id']

        all_ok = True
        diag = []
        for label, start_id, target_id in field_pairs:
            c1, o1 = gh_run(['project', 'item-edit',
                             '--id', item_id,
                             '--field-id', start_id,
                             '--date', t.start.isoformat(),
                             '--project-id', project_id])
            c2, o2 = gh_run(['project', 'item-edit',
                             '--id', item_id,
                             '--field-id', target_id,
                             '--date', t.end.isoformat(),
                             '--project-id', project_id])
            if c1 != 0 or c2 != 0:
                all_ok = False
                diag.append(f"{label}: start={c1}/target={c2}  {o1[:80]} {o2[:80]}")

        if all_ok:
            ok += 1
            if ok % 25 == 0:
                print(f"  ... {ok} updated")
        else:
            fail += 1
            if fail <= 3:
                print(f"  ⚠️ {t.id}: " + " | ".join(diag), file=sys.stderr)
    return ok, fail


# ---------- 메인 ----------

def main():
    parser = argparse.ArgumentParser(description='WBS Critical Path 기반 Task별 일자 자동 계산')
    parser.add_argument('--dry-run', action='store_true', help='계산만, GitHub 미연동')
    parser.add_argument('--project', type=int, default=4, help='GitHub Project number (default 4)')
    parser.add_argument('--owner',   default='@me',         help='Project owner (default @me)')
    parser.add_argument('--report',  metavar='FILE',        help='계산 결과 마크다운 리포트 저장')
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parent.parent.parent
    print(f"🏠 프로젝트 루트: {project_root}\n")

    # 1. Task 수집·파싱
    tasks = collect_tasks(project_root)
    if not tasks:
        print("❌ Task 0건", file=sys.stderr)
        sys.exit(1)

    # 2. RCPM 스케줄링
    print("🧠 RCPM 계산 시작 (predecessors + owner 자원 제약)…")
    topo, critical = schedule_rcpm(tasks)
    print(f"   ✓ 토폴로지 정렬: {len(topo)} Task")
    print(f"   ✓ Critical Path: {len(critical)} Task")

    # 3. 요약
    by_sprint: dict[str, int] = defaultdict(int)
    by_owner:  dict[str, int] = defaultdict(int)
    pred_count = 0
    no_pred_count = 0
    for t in tasks:
        by_sprint[t.sprint or '(none)'] += 1
        by_owner[t.owner or '(none)']   += 1
        if t.predecessors:
            pred_count += 1
        else:
            no_pred_count += 1
    print()
    print("📊 Sprint별 Task 수:")
    for s in sorted(by_sprint.keys()):
        print(f"   {s}: {by_sprint[s]}")
    print("📊 Owner별 Task 수:")
    for o in sorted(by_owner.keys(), key=lambda x: -by_owner[x])[:10]:
        print(f"   {o:24s} {by_owner[o]}")
    print(f"📊 의존성: {pred_count} Task 가 선행 보유 / {no_pred_count} 가 root")
    print()

    # 4. 리포트 저장 (선택)
    if args.report:
        write_report(Path(args.report), tasks, critical)
        print(f"📝 리포트: {args.report}")

    # 5. dry-run 종료
    if args.dry_run:
        print("🔁 모드: dry-run — GitHub 미연동")
        # Critical Path 8건 샘플 표시
        print("\n🔥 Critical Path (앞 12건):")
        for t in critical[:12]:
            preds = ', '.join(sorted(t.predecessors)[:3])
            if len(t.predecessors) > 3:
                preds += f' (+{len(t.predecessors)-3})'
            print(f"   {t.id:14s} {t.sprint or '?':4s} {_primary_owner(t):10s} "
                  f"{t.pd:>4.1f}PD  {t.start} → {t.end}   ← {preds or '(root)'}")
        return

    # 6. GitHub 연동
    print("📡 GitHub Project 메타 fetch…")
    project_id, field_ids = fetch_project_meta(args.project, args.owner)
    # Roadmap이 어느 필드 쌍을 쓰는지 사용자 설정 의존 →
    # 두 쌍(커스텀 Start/Target + 표준 Start date/Target date) 모두 존재하면 둘 다 update.
    field_pairs: list[tuple[str, str, str]] = []
    if 'Start date' in field_ids and 'Target date' in field_ids:
        field_pairs.append(('Start date/Target date',
                            field_ids['Start date'], field_ids['Target date']))
    if 'Start' in field_ids and 'Target' in field_ids:
        field_pairs.append(('Start/Target',
                            field_ids['Start'], field_ids['Target']))
    if not field_pairs:
        print(f"❌ 일자 필드 없음. 발견 필드: {list(field_ids.keys())}", file=sys.stderr)
        sys.exit(1)
    print(f"   ✓ update 대상 필드 쌍: {len(field_pairs)}")
    for label, sid, tid in field_pairs:
        print(f"      - {label}: start={sid}, target={tid}")

    print(f"📡 GitHub Project items fetch…")
    items = fetch_project_items(args.project, args.owner)
    print(f"   {len(items)} items")

    print(f"🔗 Task ↔ Issue 매핑…")
    task_to_item = map_tasks_to_items(tasks, items)

    print(f"\n⏳ 일자 필드 update 시작 ({len(tasks)} tasks × {len(field_pairs)} 쌍)…")
    ok, fail = update_date_fields(tasks, task_to_item, project_id, field_pairs)
    print(f"\n✅ 완료: 성공 {ok} / 실패 {fail}")
    if fail:
        sys.exit(2)


def write_report(out_path: Path, tasks: list[Task],
                 critical: Optional[list[Task]] = None) -> None:
    """RCPM 일자 계산 결과를 마크다운 표로 저장."""
    out_path.parent.mkdir(parents=True, exist_ok=True)
    today = date.today().isoformat()
    critical_ids = {t.id for t in (critical or []) if t.id}

    lines: list[str] = [
        f"# RCPM Task별 일자 계산 리포트 ({today})\n\n",
        f"- 총 Task: {len(tasks)}\n",
        f"- Critical Path: {len(critical_ids)} Task (⭐ 표시)\n",
        f"- 스크립트: `scripts/github-sync/11-fill-task-level-dates.py`\n",
        f"- 알고리즘: RCPM (Resource-Constrained CPM)\n",
        f"  - 선행 Task 파일의 `**선행**: [TK-…](url)` 파싱 → DAG 구축\n",
        f"  - Topological sort (Sprint 순서 tie-break)\n",
        f"  - Forward pass: earliest = max(predecessors+1, owner_busy, sprint_start)\n",
        f"  - Backward pass: slack=0 → Critical Path\n",
        f"  - Owner 자원: multi-owner (`backend+qa`) 는 첫 owner만 점유 (단순화)\n\n",
    ]

    if critical:
        lines.append("## 🔥 Critical Path\n\n")
        lines.append("| Task ID | Sprint | Owner | PD | Start | End | 선행 |\n")
        lines.append("|---|---|---|---:|---|---|---|\n")
        for t in critical:
            preds = ', '.join(sorted(t.predecessors)) or '-'
            lines.append(
                f"| **{t.id or '?'}** | {t.sprint or '?'} | {_primary_owner(t)} "
                f"| {t.pd:.1f} | {t.start or '?'} | {t.end or '?'} | {preds} |\n"
            )
        lines.append("\n")

    lines.append("## 전체 Task 일자\n\n")
    lines.append("| Task ID | Sprint | Epic | Story | Owner | PD | Start | End | 선행 |\n")
    lines.append("|---|---|---|---|---|---:|---|---|---|\n")
    for t in sorted(tasks, key=lambda x: (x.start or date(2099, 12, 31), x.id or '')):
        mark = '⭐ ' if t.id in critical_ids else ''
        preds = ', '.join(sorted(t.predecessors)) or '-'
        lines.append(
            f"| {mark}{t.id or '?'} | {t.sprint or '?'} | {t.epic or '?'} | {t.story or '?'} "
            f"| {t.owner or '?'} | {t.pd:.1f} | {t.start or '?'} | {t.end or '?'} | {preds} |\n"
        )
    out_path.write_text(''.join(lines), encoding='utf-8')


if __name__ == '__main__':
    main()
