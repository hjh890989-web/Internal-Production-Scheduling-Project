#!/usr/bin/env python3
"""
GitHub 라벨(`priority:must|should|could`) → Project v2 Priority single-select 필드(P0/P1/P2) 일괄 매핑.

배경:
    Project의 Priority 필드(옵션 P0/P1/P2)가 비어 있어 Roadmap group-by Priority 가 동작 안 함.
    라벨에는 priority:must(=must-have, P0) / should(P1) / could(P2) 가 이미 붙어 있으니 매핑만 하면 됨.

사용:
    python scripts/github-sync/12-fill-priority-field.py --dry-run
    python scripts/github-sync/12-fill-priority-field.py
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
from typing import Optional

if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', line_buffering=True)

LABEL_TO_PRIORITY = {
    'priority:must':   'P0',
    'priority:should': 'P1',
    'priority:could':  'P2',
}


def _resolve_gh() -> str:
    found = shutil.which('gh')
    if found:
        return found
    candidates = [
        Path(os.environ.get('ProgramFiles', '')) / 'GitHub CLI' / 'gh.exe',
        Path(os.environ.get('ProgramFiles(x86)', '')) / 'GitHub CLI' / 'gh.exe',
        Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs' / 'GitHub CLI' / 'gh.exe',
    ]
    for c in candidates:
        if c.exists():
            return str(c)
    raise FileNotFoundError("gh CLI not found")

_GH = _resolve_gh()


def gh_run(args: list[str]) -> tuple[int, str]:
    r = subprocess.run([_GH] + args, capture_output=True, text=True,
                       encoding='utf-8', errors='replace')
    return r.returncode, (r.stdout or '') + (r.stderr or '')


def gh_graphql(query: str, **vars) -> dict:
    args = ['api', 'graphql', '-f', f'query={query}']
    for k, v in vars.items():
        args += ['-f', f'{k}={v}']
    code, out = gh_run(args)
    if code != 0:
        print(f"❌ GraphQL fail: {out}", file=sys.stderr)
        sys.exit(1)
    return json.loads(out)


def fetch_priority_field(project_number: int) -> tuple[str, str, dict[str, str]]:
    """Project ID + Priority field ID + {옵션명: 옵션ID} 반환."""
    q = '''
    {
      viewer {
        projectV2(number: %d) {
          id
          fields(first: 50) {
            nodes {
              ... on ProjectV2SingleSelectField {
                id
                name
                options { id name }
              }
            }
          }
        }
      }
    }
    ''' % project_number
    data = gh_graphql(q)
    project = data['data']['viewer']['projectV2']
    project_id = project['id']
    for f in project['fields']['nodes']:
        if f.get('name') == 'Priority':
            options = {o['name']: o['id'] for o in f['options']}
            return project_id, f['id'], options
    print("❌ Priority field 없음", file=sys.stderr)
    sys.exit(1)


def fetch_all_items(project_number: int) -> list[dict]:
    """전체 Project item + 라벨 + 현재 Priority 값."""
    items = []
    cursor = None
    q = '''
    query($cursor: String) {
      viewer {
        projectV2(number: %d) {
          items(first: 100, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
              id
              content { ... on Issue { number title labels(first: 30) { nodes { name } } } }
              fieldValues(first: 40) {
                nodes {
                  __typename
                  ... on ProjectV2ItemFieldSingleSelectValue {
                    name
                    field { ... on ProjectV2SingleSelectField { name } }
                  }
                }
              }
            }
          }
        }
      }
    }
    ''' % project_number

    while True:
        if cursor:
            data = gh_graphql(q, cursor=cursor)
        else:
            data = gh_graphql(q)
        page = data['data']['viewer']['projectV2']['items']
        items.extend(page['nodes'])
        if not page['pageInfo']['hasNextPage']:
            break
        cursor = page['pageInfo']['endCursor']
    return items


def set_priority(project_id: str, item_id: str, field_id: str, option_id: str) -> tuple[int, str]:
    q = '''
    mutation($pid: ID!, $iid: ID!, $fid: ID!, $oid: String!) {
      updateProjectV2ItemFieldValue(input: {
        projectId: $pid, itemId: $iid, fieldId: $fid,
        value: { singleSelectOptionId: $oid }
      }) {
        projectV2Item { id }
      }
    }
    '''
    # ※ gh api: '-F' 는 type-infer (순수 숫자는 integer로 변환)
    #           → GraphQL String! 타입 오류 발생 (예: P0 option_id='79628723').
    #   '-f' 사용해 모든 값을 literal string으로 강제 전달.
    args = ['api', 'graphql',
            '-f', f'pid={project_id}',
            '-f', f'iid={item_id}',
            '-f', f'fid={field_id}',
            '-f', f'oid={option_id}',
            '-f', f'query={q}']
    return gh_run(args)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--dry-run', action='store_true')
    ap.add_argument('--project', type=int, default=4)
    ap.add_argument('--overwrite', action='store_true',
                    help='이미 Priority 값이 있어도 라벨 기준으로 덮어쓰기')
    args = ap.parse_args()

    print("📡 Project 메타 fetch…")
    project_id, field_id, options = fetch_priority_field(args.project)
    print(f"   project_id  = {project_id}")
    print(f"   field_id    = {field_id}")
    print(f"   options     = {options}")

    # 라벨→옵션ID 매핑 사전 점검
    target_options = {}
    for label, p_name in LABEL_TO_PRIORITY.items():
        if p_name not in options:
            print(f"❌ Priority 옵션 '{p_name}' 없음 (필요 라벨: {label})", file=sys.stderr)
            sys.exit(1)
        target_options[label] = options[p_name]

    print("📡 Items fetch…")
    items = fetch_all_items(args.project)
    print(f"   {len(items)} items")

    TK_RE = re.compile(r'^\s*\[?(TK-[A-Z0-9]+(?:-\d+)+)\]?')

    plan: list[tuple[str, str, str, str, str]] = []  # (item_id, tk_id, label, priority_name, option_id)
    skipped_no_label = 0
    skipped_already_set = 0

    for it in items:
        co = it.get('content') or {}
        title = co.get('title') or ''
        if not TK_RE.match(title.lstrip()):
            continue
        tk_id = TK_RE.match(title.lstrip()).group(1)
        labels = [l['name'] for l in ((co.get('labels') or {}).get('nodes') or [])]
        pr_labels = [l for l in labels if l in LABEL_TO_PRIORITY]
        if not pr_labels:
            skipped_no_label += 1
            continue
        # 다중 priority 라벨이면 가장 높은 우선순위 (must > should > could)
        priority = (
            'priority:must'   if 'priority:must'   in pr_labels else
            'priority:should' if 'priority:should' in pr_labels else
            'priority:could'
        )

        # 이미 값 있는지 확인
        current = None
        for fv in (it.get('fieldValues') or {}).get('nodes', []):
            if fv.get('__typename') == 'ProjectV2ItemFieldSingleSelectValue':
                fname = (fv.get('field') or {}).get('name', '')
                if fname == 'Priority':
                    current = fv.get('name')
        if current and not args.overwrite:
            skipped_already_set += 1
            continue

        plan.append((it['id'], tk_id, priority, LABEL_TO_PRIORITY[priority], target_options[priority]))

    print(f"\n📋 대상 Task: {len(plan)}건  (라벨 없음 {skipped_no_label} / 이미 설정 {skipped_already_set})")

    # 분포
    from collections import Counter
    dist = Counter(p[3] for p in plan)
    for k in ('P0', 'P1', 'P2'):
        print(f"   {k}: {dist.get(k, 0)}")

    if args.dry_run:
        print("\n🔁 dry-run — 미실행. 샘플 5건:")
        for p in plan[:5]:
            print(f"   {p[1]:14s} {p[2]:18s} → {p[3]}")
        return

    print("\n⏳ Priority 필드 update (간격 200ms, secondary rate limit 회피)…")
    ok = 0
    fail = 0
    for i, (item_id, tk_id, label, p_name, oid) in enumerate(plan, 1):
        # 최대 3회 retry — secondary rate limit 응답에 backoff
        attempts = 0
        while True:
            attempts += 1
            code, out = set_priority(project_id, item_id, field_id, oid)
            if code == 0:
                break
            if attempts >= 3:
                break
            # backoff: 2s, 4s
            wait = 2 ** attempts
            print(f"  ⏳ {tk_id} retry {attempts} after {wait}s (msg: {out[:80].strip()})",
                  file=sys.stderr)
            time.sleep(wait)

        if code == 0:
            ok += 1
            if ok % 50 == 0:
                print(f"  ... {ok} updated")
        else:
            fail += 1
            if fail <= 5:
                print(f"  ⚠️ {tk_id}: {out[:200]}", file=sys.stderr)

        # 기본 인터벌 (secondary rate limit ≈ POST 80/분 → 200ms 안전)
        time.sleep(0.2)

    print(f"\n✅ 완료: 성공 {ok} / 실패 {fail}")
    if fail:
        sys.exit(2)


if __name__ == '__main__':
    main()
