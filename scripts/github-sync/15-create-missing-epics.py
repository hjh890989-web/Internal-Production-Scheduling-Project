#!/usr/bin/env python3
"""
누락된 8개 Epic Issue 생성 + GitHub Project 추가.

배경:
    14번 dry-run 에서 Project 미등록 Epic 8건 발견:
    EP-00, EP-01, EP-02, EP-03, EP-04, EP-05, EP-06, EP-99
    Phase 2 자동화(03·04 스크립트) 시 누락. EP-00 (Sprint 0 인프라 기반) 도 누락 →
    Roadmap 에 EP-30+ 만 보임.

기능:
    1. WBS v1.2 에서 각 Epic 의 (Sprint, 제목, SP, 선행, Story 표) 추출
    2. gh issue create — title + body + labels + milestone
    3. gh project item-add — Project 4 에 추가
"""
import argparse
import io
import json
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

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
        Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs' / 'GitHub CLI' / 'gh.exe',
    ]:
        if c.exists():
            return str(c)
    raise FileNotFoundError("gh CLI not found")

_GH = _resolve_gh()
REPO = 'hjh890989-web/Internal-Production-Scheduling-Project'
PROJECT_NUM = 4

# Sprint label → milestone number (직전 fetch 결과)
SPRINT_TO_MILESTONE = {
    'S0': 1,    # Sprint 0 - Foundation
    'S1': 2,    # Sprint 1 - Order Integration
    'S2': 3,    # Sprint 2 - VC Scheduling
    'S3': 4,    # Sprint 3 - EX Scheduling
    'S4': 5,    # Sprint 4 - Governance
    'S5': 6,    # Sprint 5 - UI plus E2E
}

# WBS v1.2 추출 결과 — 누락된 8 Epic 메타
EPICS = [
    {'id': 'EP-00', 'title': '인프라 기반 셋업 (Foundation)',  'sprint': 'S0', 'sp': 8,  'pre': '없음',
     'sad_ref': 'REF-SAD ADR-010·013, §8 배포', 'cross_cutting': True},
    {'id': 'EP-99', 'title': '마스터 데이터 정비 (선행 작업)', 'sprint': 'S0', 'sp': 5,  'pre': '없음',
     'sad_ref': 'REF-PDD §A-01, REF-SAD ADR-016·017', 'cross_cutting': True},
    {'id': 'EP-01', 'title': '엑셀 통합 Parser (M-01)',     'sprint': 'S1', 'sp': 13, 'pre': 'EP-00',
     'sad_ref': 'REF-PDD M-01, REQ-FUNC-OC-001~004'},
    {'id': 'EP-02', 'title': '중복 감지 (M-02)',             'sprint': 'S1', 'sp': 5,  'pre': 'EP-01',
     'sad_ref': 'REF-PDD M-02, REQ-FUNC-OC-005~006'},
    {'id': 'EP-03', 'title': 'Diff·알림 (M-03)',             'sprint': 'S1', 'sp': 8,  'pre': 'EP-02',
     'sad_ref': 'REF-PDD M-03, REQ-FUNC-OC-007~010'},
    {'id': 'EP-04', 'title': '슬롯 O/X 검증 (M-04)',          'sprint': 'S2', 'sp': 8,  'pre': 'EP-01, EP-99',
     'sad_ref': 'REF-PDD M-04, REQ-FUNC-VC-001~004'},
    {'id': 'EP-05', 'title': '회전수 배치 (M-05)',            'sprint': 'S2', 'sp': 13, 'pre': 'EP-04',
     'sad_ref': 'REF-PDD M-05, REQ-FUNC-VC-005~011'},
    {'id': 'EP-06', 'title': '납기 D-2 역산 (M-06)',          'sprint': 'S2', 'sp': 3,  'pre': 'EP-05',
     'sad_ref': 'REF-PDD M-06, REQ-FUNC-VC-008'},
]


def build_body(ep: dict) -> str:
    """Issue body — WBS §X EP-XX 인용 + Story 목록 (TK 파일 디렉토리에서 추출)."""
    project_root = Path(__file__).resolve().parent.parent.parent
    ep_dir = project_root / 'Phase 2' / '4.Tasks' / 'Tasks' / ep['id']

    story_rows = []
    if ep_dir.exists():
        for st_dir in sorted(ep_dir.glob('ST-*')):
            # ST 디렉토리 안의 TK-*.md 개수
            tk_count = len(list(st_dir.glob('TK-*.md')))
            story_rows.append(f"| [{st_dir.name}](Phase%202/4.Tasks/Tasks/{ep['id']}/{st_dir.name}/) | (WBS 참조) | {tk_count} Task |")

    stories_section = ""
    if story_rows:
        stories_section = (
            "\n## Story 목록\n\n"
            "| Story | 제목 | Task |\n"
            "|---|---|:--:|\n"
            + "\n".join(story_rows) + "\n"
        )

    body = f"""# Epic Overview — [{ep['id']}] {ep['title']}

**Sprint**: {ep['sprint']} | **SP**: {ep['sp']} | **선행**: {ep['pre']}
**출처**: {ep['sad_ref']}

---

## Epic 목적

> WBS v1.2 §{ep['id']} 인용. 자세한 내용은 [TASK-001_WBS_v1.2.md](Phase%202/4.Tasks/TASK-001_WBS_v1.2.md) 참조.

본 Epic 산하 Task 파일은 [Phase 2/4.Tasks/Tasks/{ep['id']}/](Phase%202/4.Tasks/Tasks/{ep['id']}/) 에 위치.
{stories_section}
---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §{ep['id']}
- **출처 문서**: {ep['sad_ref']}
- **선행 Epic**: {ep['pre']}

---

> 본 Issue 는 누락된 Epic 일괄 등록 (15-create-missing-epics.py — 2026-05-19).
> 상세 Story·Task 는 디렉토리 참조.
"""
    return body


def create_issue(ep: dict) -> tuple[bool, str]:
    """Issue 생성 + Issue URL 반환."""
    labels = [f'sprint:{ep["sprint"]}', 'type:epic', 'priority:must']
    if ep.get('cross_cutting'):
        labels.append('cross-cutting')

    title = f"[{ep['id']}] {ep['title']}"
    body = build_body(ep)
    milestone = SPRINT_TO_MILESTONE.get(ep['sprint'])

    args = [_GH, 'issue', 'create',
            '--repo', REPO,
            '--title', title,
            '--body', body,
            '--label', ','.join(labels)]
    if milestone:
        args += ['--milestone', f'Sprint {milestone-1} - ' + {
            1: 'Foundation', 2: 'Order Integration', 3: 'VC Scheduling',
            4: 'EX Scheduling', 5: 'Governance', 6: 'UI plus E2E',
        }[milestone]]

    r = subprocess.run(args, capture_output=True, text=True, encoding='utf-8')
    if r.returncode != 0:
        return False, r.stderr.strip()
    # gh issue create는 URL을 stdout으로 출력
    url = (r.stdout or '').strip().split('\n')[-1]
    return True, url


def add_to_project(issue_url: str) -> tuple[bool, str]:
    args = [_GH, 'project', 'item-add', str(PROJECT_NUM),
            '--owner', '@me', '--url', issue_url, '--format', 'json']
    r = subprocess.run(args, capture_output=True, text=True, encoding='utf-8')
    if r.returncode != 0:
        return False, r.stderr.strip()
    try:
        item = json.loads(r.stdout)
        return True, item.get('id', '?')
    except Exception:
        return True, '(parsed fail but exit 0)'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--dry-run', action='store_true')
    args = ap.parse_args()

    print(f"📋 대상 Epic: {len(EPICS)}건\n")
    for ep in EPICS:
        print(f"  [{ep['id']}] {ep['title']:35s} sprint={ep['sprint']} sp={ep['sp']} pre={ep['pre']}")

    if args.dry_run:
        print(f"\n🔁 dry-run — Issue body 샘플 (EP-00):")
        print("─" * 60)
        print(build_body(EPICS[0]))
        return

    print()
    ok = 0
    fail = 0
    for ep in EPICS:
        print(f"\n🚀 {ep['id']} — Issue create…")
        success, info = create_issue(ep)
        if not success:
            print(f"   ❌ create fail: {info[:160]}")
            fail += 1
            continue
        url = info
        print(f"   ✓ created: {url}")

        time.sleep(0.5)   # rate limit 회피
        print(f"   📌 Project 추가…")
        success, info = add_to_project(url)
        if not success:
            print(f"   ⚠️ project add fail: {info[:160]}")
        else:
            print(f"   ✓ project item: {info[:40]}")

        ok += 1
        time.sleep(0.5)

    print(f"\n✅ Issue 생성 + Project 추가: 성공 {ok} / 실패 {fail}")
    if fail:
        sys.exit(2)


if __name__ == '__main__':
    main()
