#!/usr/bin/env python3
"""
Roadmap 종합 점검 — 개발 순서 정합성 + Sprint 분류 + DAG 무결성 + PD capacity.

배경:
    Roadmap이 "사용자가 진실의 원천으로 보는 화면". 단일 결함 (Sprint 라벨 불일치,
    cycle, 누락된 선행) 1건 만으로도 작업 흐름이 어그러짐. 본 스크립트는 점검·진단만,
    수정은 별도 (결함 발견 시 우선순위 정렬 후 사용자 결정).

검증 항목:
    [A] WBS Epic sprint 분류 vs Task 파일 sprint 라벨 일치
    [B] Task 파일의 선행 cross-reference 유효성 (참조된 TK-XX 실재 여부)
    [C] DAG cycle 검출 (Kahn 후 잔여 노드 + DFS)
    [D] PLAN-001 Sprint 0 Critical Path 의 RCPM 결과 정합성
    [E] Sprint 별 PD 총합 vs 가용 capacity (Sprint 0: 3 dev × 10일 = 30 PD)
    [F] GitHub Project Item sprint single-select 필드 vs Task 라벨
    [G] WBS 정의 Epic·Story 가 Task 파일에 누락 없이 존재
"""
import io
import json
import os
import re
import shutil
import subprocess
import sys
from collections import defaultdict
from pathlib import Path
from typing import Optional

if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', line_buffering=True)


def _resolve_gh() -> str:
    found = shutil.which('gh')
    if found:
        return found
    for c in [
        Path(os.environ.get('ProgramFiles', '')) / 'GitHub CLI' / 'gh.exe',
        Path(os.environ.get('ProgramFiles(x86)', '')) / 'GitHub CLI' / 'gh.exe',
    ]:
        if c.exists():
            return str(c)
    raise FileNotFoundError("gh CLI not found")

_GH = _resolve_gh()
ROOT = Path(__file__).resolve().parent.parent.parent
WBS_FILE = ROOT / 'Phase 2' / '4.Tasks' / 'TASK-001_WBS_v1.2.md'
TASKS_DIR = ROOT / 'Phase 2' / '4.Tasks' / 'Tasks'

# ---------- 패턴 ----------
TK_ID_RE  = re.compile(r'TK-[A-Z0-9]+(?:-\d+)+')
SPRINT_RE = re.compile(r"sprint:(S\d|Deferred|Cross\-?cutting)")
EPIC_RE   = re.compile(r"epic:(EP-[A-Z0-9]+)")
OWNER_RE  = re.compile(r"owner:([a-zA-Z+]+)")
PD_RE     = re.compile(r'\*\*추정\*\*[:：]\s*([0-9.]+)\s*PD')
TITLE_TK  = re.compile(r'^title:\s*"?\[(TK-[A-Z0-9]+(?:-\d+)+)\]', re.MULTILINE)
TASK_ID_TK = re.compile(r'\*\*Task ID\*\*[:：]\s*(TK-[A-Z0-9]+(?:-\d+)+)')

# WBS Epic 추출: ### EP-XX 제목 + Sprint
WBS_EPIC_RE = re.compile(r'^###\s+(EP-[A-Z0-9]+)\s+(.+?)$', re.MULTILINE)
# WBS Sprint 라인 전체 (\*\*Sprint\*\*:.*) 에서 모든 S\d 추출 — cross-cutting 표기 처리
# 예) "S0~S1 분산", "S0(기반) + S2·S3·S4(WebSocket)·S5", "S2~S5"
WBS_SPRINT_LINE_RE = re.compile(r'\*\*Sprint\*\*:\s*([^/\n]+)')
WBS_SX_RE = re.compile(r'\bS(\d)\b')


def parse_task_files() -> dict[str, dict]:
    """모든 TK 파일에서 메타 + predecessors 추출."""
    out: dict[str, dict] = {}
    for f in TASKS_DIR.glob('EP-*/ST-*/TK-*.md'):
        try:
            c = f.read_text(encoding='utf-8')
        except Exception:
            continue
        m = TASK_ID_TK.search(c) or TITLE_TK.search(c)
        if not m:
            continue
        tk_id = m.group(1)

        sprint = epic = owner = None
        labels_m = re.search(r"^labels:\s*'([^']*)'", c, re.MULTILINE)
        if labels_m:
            labels = labels_m.group(1)
            sp = SPRINT_RE.search(labels)
            ep = EPIC_RE.search(labels)
            ow = OWNER_RE.search(labels)
            if sp: sprint = sp.group(1)
            if ep: epic = ep.group(1)
            if ow: owner = ow.group(1).lower()

        pd = 0.5
        pdm = PD_RE.search(c)
        if pdm:
            try: pd = float(pdm.group(1))
            except: pass

        # predecessors (선행)
        preds = set()
        for line in c.splitlines():
            if '선행' not in line: continue
            chunk = line.split('후행')[0]
            for x in TK_ID_RE.findall(chunk):
                if x != tk_id:
                    preds.add(x)

        out[tk_id] = {
            'file': f.relative_to(ROOT),
            'sprint': sprint, 'epic': epic, 'owner': owner, 'pd': pd,
            'preds': preds,
        }
    return out


def parse_wbs_epic_sprint() -> dict[str, set[str]]:
    """WBS v1.2 에서 Epic → Sprint 집합 매핑 (cross-cutting 다중 Sprint 표기 지원).
    예) "S0~S5 분산" → {S0,S1,S2,S3,S4,S5}
        "S0(기반) + S2·S3" → {S0,S2,S3}
        "S0" → {S0}
    """
    content = WBS_FILE.read_text(encoding='utf-8')
    out: dict[str, set[str]] = {}
    lines = content.splitlines()
    current_epic = None
    for line in lines:
        m = WBS_EPIC_RE.match(line)
        if m:
            current_epic = m.group(1)
            continue
        if current_epic:
            lm = WBS_SPRINT_LINE_RE.search(line)
            if lm:
                sprint_text = lm.group(1)
                nums = WBS_SX_RE.findall(sprint_text)
                if nums:
                    # '~' 또는 '분산' 같은 범위 표기면 시작·끝 사이 모두 포함
                    if '~' in sprint_text and len(nums) >= 2:
                        start, end = int(nums[0]), int(nums[-1])
                        out[current_epic] = {f'S{i}' for i in range(start, end+1)}
                    else:
                        out[current_epic] = {f'S{n}' for n in nums}
                    current_epic = None
    return out


def check_dag_cycle(tasks: dict[str, dict]) -> tuple[list[list[str]], set[str]]:
    """Kahn 토폴로지 → 잔여 노드 + DFS 로 정확한 cycle 추출."""
    in_deg = {tk: len(meta['preds']) for tk, meta in tasks.items()}
    dependents = defaultdict(list)
    for tk, meta in tasks.items():
        for p in meta['preds']:
            if p in tasks:  # 존재하는 ID만
                dependents[p].append(tk)

    # in_degree 재계산 (존재하는 preds만)
    in_deg = {tk: sum(1 for p in meta['preds'] if p in tasks) for tk, meta in tasks.items()}

    ready = [tk for tk, d in in_deg.items() if d == 0]
    sorted_ = []
    while ready:
        t = ready.pop()
        sorted_.append(t)
        for d in dependents[t]:
            in_deg[d] -= 1
            if in_deg[d] == 0:
                ready.append(d)

    cycle_nodes = set(tasks.keys()) - set(sorted_)

    # DFS 로 실제 cycle path 찾기
    WHITE, GRAY, BLACK = 0, 1, 2
    color = {t: WHITE for t in tasks}
    parent = {t: None for t in tasks}
    cycles = []

    def dfs(start):
        stack = [(start, iter([p for p in tasks[start]['preds'] if p in tasks]))]
        color[start] = GRAY
        while stack:
            node, it = stack[-1]
            try:
                nxt = next(it)
                if color[nxt] == GRAY:
                    # cycle
                    cyc = [nxt]
                    cur = node
                    while cur != nxt and cur is not None:
                        cyc.append(cur); cur = parent[cur]
                    cyc.append(nxt)
                    cycles.append(list(reversed(cyc)))
                elif color[nxt] == WHITE:
                    color[nxt] = GRAY
                    parent[nxt] = node
                    stack.append((nxt, iter([p for p in tasks[nxt]['preds'] if p in tasks])))
            except StopIteration:
                color[node] = BLACK
                stack.pop()

    for t in tasks:
        if color[t] == WHITE:
            dfs(t)

    return cycles, cycle_nodes


def main():
    print("=" * 70)
    print("Roadmap 종합 점검 (audit)")
    print("=" * 70)

    print("\n[step 1] Task 파일 파싱…")
    tasks = parse_task_files()
    print(f"   ✓ {len(tasks)} Task 파일")

    print("\n[step 2] WBS Epic Sprint 추출…")
    wbs_epic_sprint = parse_wbs_epic_sprint()
    print(f"   ✓ {len(wbs_epic_sprint)} Epic 매핑")
    for k, v in sorted(wbs_epic_sprint.items()):
        print(f"     {k}: {v}")

    print("\n" + "=" * 70)
    print("[A] Task 파일 sprint 라벨 vs WBS Epic Sprint 일치")
    print("=" * 70)
    mismatch = []
    for tk, meta in tasks.items():
        ep = meta['epic']
        if not ep or ep not in wbs_epic_sprint:
            continue
        wbs_sprints = wbs_epic_sprint[ep]    # set
        tk_sp = meta['sprint']
        if tk_sp and tk_sp not in wbs_sprints:
            mismatch.append((tk, ep, sorted(wbs_sprints), tk_sp))
    if mismatch:
        print(f"❌ 불일치 {len(mismatch)}건:")
        for tk, ep, wbs_sp, tk_sp in mismatch[:30]:
            print(f"   {tk:14s}  epic={ep}  WBS={wbs_sp}  Task={tk_sp}")
        if len(mismatch) > 30:
            print(f"   ... +{len(mismatch)-30}건")
    else:
        print("✅ 모든 Task 의 sprint 라벨이 Epic 의 WBS Sprint 와 일치")
    print(f"   (단, 같은 Epic 안에 Story 별로 Sprint 다른 케이스는 정상 — ST-30-1 S0 + ST-30-2 S1)")

    print("\n" + "=" * 70)
    print("[B] 선행 cross-reference 유효성")
    print("=" * 70)
    dangling = []
    for tk, meta in tasks.items():
        for p in meta['preds']:
            if p not in tasks:
                dangling.append((tk, p))
    if dangling:
        print(f"❌ 존재하지 않는 선행 참조 {len(dangling)}건:")
        for tk, p in dangling[:20]:
            print(f"   {tk}  →  {p} (없음)")
        if len(dangling) > 20:
            print(f"   ... +{len(dangling)-20}건")
    else:
        print("✅ 모든 선행 참조가 실재 Task")

    print("\n" + "=" * 70)
    print("[C] DAG cycle 검출")
    print("=" * 70)
    cycles, cycle_nodes = check_dag_cycle(tasks)
    if cycle_nodes:
        print(f"❌ Cycle 영향 Task {len(cycle_nodes)}건:")
        for tk in sorted(cycle_nodes):
            print(f"   {tk:14s}  preds: {sorted(tasks[tk]['preds'] & set(tasks.keys()))}")
        if cycles:
            print(f"\n   DFS 검출 cycle path:")
            for c in cycles[:5]:
                print(f"     {' → '.join(c)}")
    else:
        print("✅ DAG cycle 없음")

    print("\n" + "=" * 70)
    print("[D] PLAN-001 Sprint 0 Critical Path 정합성")
    print("=" * 70)
    plan001_cp = ['TK-00-1-4', 'TK-30-1-1', 'TK-30-1-2', 'TK-32-1-2', 'TK-33-1-2']
    print("PLAN-001 §Sprint 0 Critical Path: " + " → ".join(plan001_cp))
    for i, tk in enumerate(plan001_cp):
        if tk not in tasks:
            print(f"   ❌ {tk}: Task 파일 없음")
            continue
        meta = tasks[tk]
        # 선행 체인 확인
        if i > 0:
            prev = plan001_cp[i-1]
            has_prev = prev in meta['preds']
            mark = "✓" if has_prev else "?"
            print(f"   {mark} {tk}  (sprint={meta['sprint']}, owner={meta['owner']}, preds includes {prev}: {has_prev})")
        else:
            print(f"   ✓ {tk}  (sprint={meta['sprint']}, owner={meta['owner']}) — root")

    print("\n" + "=" * 70)
    print("[E] Sprint 별 PD 총합 vs Capacity (Sprint 0: 3 dev × 10일 = 30 PD)")
    print("=" * 70)
    by_sprint_pd = defaultdict(float)
    by_sprint_count = defaultdict(int)
    by_sprint_owner = defaultdict(lambda: defaultdict(float))
    for tk, meta in tasks.items():
        sp = meta['sprint'] or '(none)'
        by_sprint_pd[sp] += meta['pd']
        by_sprint_count[sp] += 1
        owner = (meta['owner'] or '?').split('+')[0]
        by_sprint_owner[sp][owner] += meta['pd']

    cap_per_sprint = 30  # 3 dev × 10일 가정
    for sp in sorted(by_sprint_pd.keys()):
        total_pd = by_sprint_pd[sp]
        n = by_sprint_count[sp]
        utilization = total_pd / cap_per_sprint * 100 if sp.startswith('S') else 0
        mark = "✅" if utilization <= 100 else "❌" if utilization > 120 else "⚠️"
        print(f"   {mark} {sp}: {n} Task, {total_pd:.1f} PD (capacity 30, util {utilization:.0f}%)")
        if total_pd > cap_per_sprint:
            owners_sorted = sorted(by_sprint_owner[sp].items(), key=lambda x: -x[1])
            print(f"        Owner 부담: " + ", ".join(f"{o}: {p:.1f}" for o, p in owners_sorted[:5]))

    print("\n" + "=" * 70)
    print("[F] GitHub Project Item sprint 필드 점검 (sampling)")
    print("=" * 70)
    print("   (생략 — 320 Item 검증은 별도 GraphQL 호출 필요. 11번 스크립트가 라벨 기반으로 일자 계산 → 라벨 기반 검증으로 갈음.)")

    print("\n" + "=" * 70)
    print("[G] WBS 정의 Epic 가 Task 파일에 누락 없이 존재")
    print("=" * 70)
    wbs_epics = set(wbs_epic_sprint.keys())
    task_epics = set()
    for ep_dir in TASKS_DIR.glob('EP-*'):
        if ep_dir.is_dir():
            task_epics.add(ep_dir.name)
    missing_in_task = wbs_epics - task_epics
    extra_in_task = task_epics - wbs_epics
    if missing_in_task:
        print(f"❌ WBS 에 있지만 Task 폴더 없음: {sorted(missing_in_task)}")
    if extra_in_task:
        print(f"⚠️  Task 폴더는 있지만 WBS 미정의: {sorted(extra_in_task)} (잘못된 폴더? 또는 WBS 누락?)")
    if not missing_in_task and not extra_in_task:
        print(f"✅ Epic 폴더 완전 일치 ({len(wbs_epics)}개)")

    print("\n" + "=" * 70)
    print("📋 요약")
    print("=" * 70)
    print(f"  Task 파일: {len(tasks)}개")
    print(f"  WBS Epic 정의: {len(wbs_epic_sprint)}개")
    print(f"  Task Epic 폴더: {len(task_epics)}개")
    print(f"  Sprint 라벨 불일치: {len(mismatch)}건  {'❌' if mismatch else '✅'}")
    print(f"  Dangling 선행 참조: {len(dangling)}건  {'❌' if dangling else '✅'}")
    print(f"  DAG cycle 영향 Task: {len(cycle_nodes)}건  {'❌' if cycle_nodes else '✅'}")
    print(f"  Critical Path Task 모두 존재: {sum(1 for t in plan001_cp if t in tasks)}/{len(plan001_cp)}")


if __name__ == '__main__':
    main()
