#!/usr/bin/env python3
"""
Epic 의 Start date / Target date 를 children Task date 의 min/max 로 자동 채움.

배경:
    11-fill-task-level-dates.py 는 Task 만 일자 갱신 → Epic 은 비어 있어서
    Roadmap 에서 '+' 표시 (date 없음). Epic 은 abstraction layer 라 자체 PD/일자
    없음. children Task 의 일자 범위가 자연스러운 Epic 일정.

알고리즘:
    1. Project 의 모든 Item + 4 date 필드 (Start date / Target date / Start / Target) 가져옴
    2. TK-{EP_ID}-{ST}-{TK} 패턴에서 EP_ID 추출 → epic 별 children date 집계
    3. epic_min_start = min(children Start date)
       epic_max_end   = max(children Target date)
    4. 각 Epic Item (title 시작 '[EP-{EP_ID}]') 에 4 필드 update
       — gh -f literal string (12·13번 -F type-infer 버그 회피와 동일)
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
from collections import defaultdict
from datetime import date
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
        Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs' / 'GitHub CLI' / 'gh.exe',
    ]:
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
        raise RuntimeError(f"GraphQL fail: {out}")
    return json.loads(out)


def fetch_project_meta(project_number: int) -> tuple[str, dict[str, str]]:
    """Project ID + {field_name: field_id} (date 필드만 — Start date·Target date·Start·Target)."""
    q = '''
    {
      viewer {
        projectV2(number: %d) {
          id
          fields(first: 50) {
            nodes { ... on ProjectV2Field { id name dataType } }
          }
        }
      }
    }
    ''' % project_number
    data = gh_graphql(q)
    project = data['data']['viewer']['projectV2']
    field_ids = {}
    for f in project['fields']['nodes']:
        if f.get('dataType') == 'DATE':
            field_ids[f['name']] = f['id']
    return project['id'], field_ids


def fetch_all_items(project_number: int) -> list[dict]:
    """모든 Project item + title + 4 date 필드."""
    q = '''
    query($cursor: String) {
      viewer {
        projectV2(number: %d) {
          items(first: 100, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
              id
              content { ... on Issue { number title } }
              fieldValues(first: 40) {
                nodes {
                  __typename
                  ... on ProjectV2ItemFieldDateValue {
                    date
                    field { ... on ProjectV2Field { name } }
                  }
                }
              }
            }
          }
        }
      }
    }
    ''' % project_number
    items = []
    cursor = None
    for _ in range(20):
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


def set_date_field(project_id: str, item_id: str, field_id: str, iso_date: str) -> tuple[int, str]:
    q = '''
    mutation($pid: ID!, $iid: ID!, $fid: ID!, $d: Date!) {
      updateProjectV2ItemFieldValue(input: {
        projectId: $pid, itemId: $iid, fieldId: $fid,
        value: { date: $d }
      }) { projectV2Item { id } }
    }
    '''
    return gh_run(['api', 'graphql',
                   '-f', f'pid={project_id}',
                   '-f', f'iid={item_id}',
                   '-f', f'fid={field_id}',
                   '-f', f'd={iso_date}',
                   '-f', f'query={q}'])


# Patterns
TK_TITLE_RE = re.compile(r'^\s*\[?(TK-([A-Z0-9]+)(?:-\d+)+)\]?')
EP_TITLE_RE = re.compile(r'^\s*\[?(EP-([A-Z0-9]+))\]?')


def main():
    ap = argparse.ArgumentParser(
        description='Epic 의 Start date / Target date 를 children Task min/max 로 자동 채움'
    )
    ap.add_argument('--project', type=int, default=4)
    ap.add_argument('--dry-run', action='store_true')
    args = ap.parse_args()

    print("📡 Project 메타 fetch…")
    project_id, field_ids = fetch_project_meta(args.project)
    print(f"   project_id = {project_id}")
    print(f"   date 필드: {list(field_ids.keys())}")

    needed = ['Start date', 'Target date', 'Start', 'Target']
    missing = [f for f in needed if f not in field_ids]
    if missing:
        print(f"⚠️  누락 필드: {missing} (있는 것만 update)")

    print(f"📡 Items fetch…")
    items = fetch_all_items(args.project)
    print(f"   {len(items)} items")

    # 1. EP_ID → children (start, end) 집계
    epic_children: dict[str, list[tuple[str, str]]] = defaultdict(list)
    tk_count = 0
    for it in items:
        co = it.get('content') or {}
        title = (co.get('title') or '').lstrip()
        m = TK_TITLE_RE.match(title)
        if not m:
            continue
        tk_count += 1
        ep_id = m.group(2)  # 예: '00', 'EX13', 'E2E'
        # Start date · Target date 추출
        start, end = None, None
        for fv in (it.get('fieldValues') or {}).get('nodes', []):
            if fv.get('__typename') == 'ProjectV2ItemFieldDateValue':
                fn = (fv.get('field') or {}).get('name', '')
                if fn == 'Start date':
                    start = fv.get('date')
                elif fn == 'Target date':
                    end = fv.get('date')
        if start and end:
            epic_children[ep_id].append((start, end))

    print(f"\n📋 Task Items 파싱: {tk_count}")
    print(f"📋 Epic 후보: {len(epic_children)} (TK 가 매핑된 EP_ID 기준)")

    # 2. Epic Item 찾기 + min/max 계산
    epic_items: dict[str, dict] = {}
    for it in items:
        co = it.get('content') or {}
        title = (co.get('title') or '').lstrip()
        m = EP_TITLE_RE.match(title)
        if not m:
            continue
        ep_id = m.group(2)
        epic_items[ep_id] = {
            'item_id': it['id'],
            'number': co.get('number'),
            'title': co.get('title'),
        }

    print(f"📋 Project 의 Epic Items: {len(epic_items)}")

    # 3. update plan
    plan: list[tuple[str, str, str, str, dict]] = []
    skipped_no_children = []
    skipped_no_epic_item = []

    for ep_id, dates in epic_children.items():
        if ep_id not in epic_items:
            skipped_no_epic_item.append(ep_id)
            continue
        ep_min = min(d[0] for d in dates)
        ep_max = max(d[1] for d in dates)
        ei = epic_items[ep_id]
        plan.append((ep_id, ep_min, ep_max, ei['item_id'], ei))

    # children 없는 Epic (혹시 있나)
    for ep_id, ei in epic_items.items():
        if ep_id not in epic_children:
            skipped_no_children.append((ep_id, ei['number'], ei['title']))

    plan.sort(key=lambda p: p[0])

    print(f"\n📅 계산 결과 ({len(plan)} Epic):")
    for ep_id, ep_min, ep_max, _, ei in plan:
        print(f"   EP-{ep_id:8s}  #{ei['number']:3d}  {ep_min} → {ep_max}  "
              f"({(date.fromisoformat(ep_max) - date.fromisoformat(ep_min)).days + 1}일)  "
              f"({(ei.get('title') or '')[:40]})")

    if skipped_no_children:
        print(f"\n⚠️  children 없는 Epic ({len(skipped_no_children)}): " +
              ", ".join(f"EP-{e[0]}" for e in skipped_no_children))
    if skipped_no_epic_item:
        print(f"⚠️  Project 미등록 Epic ({len(skipped_no_epic_item)}): " +
              ", ".join(f"EP-{e}" for e in skipped_no_epic_item))

    if args.dry_run:
        print("\n🔁 dry-run — 실제 update 없음")
        return

    # 4. 실제 update — 4 필드 모두 (Start date / Target date / Start / Target)
    update_pairs = []
    for fname in needed:
        if fname in field_ids:
            update_pairs.append((fname, field_ids[fname]))

    print(f"\n⏳ update {len(plan)} Epic × {len(update_pairs)} 필드…")
    ok = 0
    fail = 0
    for ep_id, ep_min, ep_max, item_id, ei in plan:
        all_ok = True
        for fname, fid in update_pairs:
            iso_date = ep_min if fname in ('Start date', 'Start') else ep_max
            code, out = set_date_field(project_id, item_id, fid, iso_date)
            if code != 0:
                all_ok = False
                if fail < 3:
                    print(f"  ⚠️ EP-{ep_id} {fname}: {out[:120]}", file=sys.stderr)
            time.sleep(0.15)   # secondary rate limit 회피
        if all_ok:
            ok += 1
        else:
            fail += 1

    print(f"\n✅ 완료: 성공 {ok} / 실패 {fail}")
    if fail:
        sys.exit(2)


if __name__ == '__main__':
    main()
