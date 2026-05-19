#!/usr/bin/env python3
"""
Task 완료 표시 — TK-ID 받아서 해당 GitHub Issue close + Project Status → Done.

배경:
    Trunk-based commit 후 GitHub Project Status가 자동 Done으로 안 됨.
    표준 흐름 = Issue close → Project workflow가 Status: Done으로 자동 변경.
    본 스크립트는 그 흐름을 한 줄로 묶음.

사용:
    python scripts/github-sync/13-mark-task-done.py TK-00-2-1
    python scripts/github-sync/13-mark-task-done.py TK-00-2-1 TK-00-2-2 TK-00-2-3
    python scripts/github-sync/13-mark-task-done.py TK-00-2-1 --commit 7694e12
    python scripts/github-sync/13-mark-task-done.py TK-00-2-1 --dry-run
    python scripts/github-sync/13-mark-task-done.py TK-00-2-1 --no-auto-commit  # commit 자동탐지 끔

흐름:
    1. Title이 정확히 '[TK-…]'로 시작하는 OPEN Issue 검색
    2. --commit 미지정 시 git log --grep으로 첫 commit hash 자동 탐지
    3. gh issue close --reason completed --comment '<TK> 완료 — commit <hash>'
    4. (fallback) Project Status 필드를 'Done'으로 강제 update — workflow 없을 때 대비
"""
import argparse
import io
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Optional

if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', line_buffering=True)

DEFAULT_REPO = 'hjh890989-web/Internal-Production-Scheduling-Project'
DEFAULT_PROJECT = 4
TK_ID_RE = re.compile(r'^TK-[A-Z0-9]+(?:-\d+)+$')


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


def find_issue_number(repo: str, tk_id: str) -> Optional[int]:
    """Title이 '[TK-…]'로 시작하는 OPEN Issue 번호 반환."""
    code, out = gh_run(['issue', 'list', '--repo', repo,
                        '--search', f'"[{tk_id}]" in:title',
                        '--state', 'open', '--json', 'number,title',
                        '--limit', '20'])
    if code != 0:
        print(f"  ⚠️ gh issue list fail: {out[:200]}", file=sys.stderr)
        return None
    issues = json.loads(out)
    # 정확 매치: title이 '[TK-XX]' 로 시작
    for it in issues:
        title = it.get('title') or ''
        if title.startswith(f'[{tk_id}]'):
            return it['number']
    return None


def find_commit_hash(tk_id: str) -> Optional[str]:
    """
    git log로 TK-ID '구현 완료' commit hash 탐지.
    우선 subject 끝의 '(TK-XX)' 패턴 매칭 → 본 commit이 해당 Task 완료물.
    body에 단순 언급된 commit (예: '다음은 TK-XX 작업')은 제외.
    """
    # `(TK-XX)` 패턴이 commit subject(첫 줄)에 있는 commit
    r = subprocess.run(['git', 'log', f'--grep=({tk_id})', '-F',
                        '-n', '1', '--format=%H'],
                       capture_output=True, text=True, encoding='utf-8')
    if r.returncode != 0:
        return None
    h = (r.stdout or '').strip()
    return h[:7] if h else None


def fetch_project_status_meta(project: int) -> tuple[str, str, str]:
    """Project ID + Status field ID + 'Done' option ID 반환."""
    q = '''
    {
      viewer {
        projectV2(number: %d) {
          id
          fields(first: 50) {
            nodes {
              ... on ProjectV2SingleSelectField {
                id name options { id name }
              }
            }
          }
        }
      }
    }
    ''' % project
    data = gh_graphql(q)
    proj = data['data']['viewer']['projectV2']
    for f in proj['fields']['nodes']:
        if f.get('name') == 'Status':
            for o in f['options']:
                if o['name'] == 'Done':
                    return proj['id'], f['id'], o['id']
    raise RuntimeError("'Status'/'Done' field option not found")


def find_project_item_id(project: int, issue_number: int) -> Optional[str]:
    """Issue 번호 → Project item ID. 360 item paginate."""
    q = '''
    query($cursor: String) {
      viewer {
        projectV2(number: %d) {
          items(first: 100, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
              id
              content { ... on Issue { number } }
            }
          }
        }
      }
    }
    ''' % project
    cursor = None
    for _ in range(20):
        data = gh_graphql(q, cursor=cursor) if cursor else gh_graphql(q)
        page = data['data']['viewer']['projectV2']['items']
        for it in page['nodes']:
            co = it.get('content') or {}
            if co.get('number') == issue_number:
                return it['id']
        if not page['pageInfo']['hasNextPage']:
            break
        cursor = page['pageInfo']['endCursor']
    return None


def set_status_done(project_id: str, item_id: str, field_id: str, option_id: str) -> bool:
    """Project Status 필드를 'Done'으로 강제 update (workflow fallback)."""
    q = '''
    mutation($pid: ID!, $iid: ID!, $fid: ID!, $oid: String!) {
      updateProjectV2ItemFieldValue(input: {
        projectId: $pid, itemId: $iid, fieldId: $fid,
        value: { singleSelectOptionId: $oid }
      }) { projectV2Item { id } }
    }
    '''
    # ※ -F (type-infer) 가 순수 숫자 option_id를 integer로 변환 → String! 오류.
    #   12번 스크립트와 같은 이유로 -f literal string 강제.
    code, out = gh_run(['api', 'graphql',
                        '-f', f'pid={project_id}',
                        '-f', f'iid={item_id}',
                        '-f', f'fid={field_id}',
                        '-f', f'oid={option_id}',
                        '-f', f'query={q}'])
    if code != 0:
        print(f"  ⚠️ Status update fail: {out[:200]}", file=sys.stderr)
        return False
    return True


def close_issue(repo: str, number: int, comment: str) -> bool:
    code, out = gh_run(['issue', 'close', str(number), '--repo', repo,
                        '--reason', 'completed', '--comment', comment])
    if code != 0:
        print(f"  ⚠️ Issue close fail: {out[:200]}", file=sys.stderr)
        return False
    return True


def process_task(tk_id: str, repo: str, project: int,
                 commit_hash: Optional[str], auto_detect_commit: bool,
                 dry_run: bool, status_meta: tuple[str, str, str]) -> bool:
    print(f"\n🎯 {tk_id}")

    # 1. Issue 번호 찾기
    issue_no = find_issue_number(repo, tk_id)
    if not issue_no:
        print(f"   ❌ Open issue not found (already closed or missing)")
        return False
    print(f"   ✓ Issue #{issue_no}")

    # 2. commit hash 자동 탐지 (--commit 미지정 + --no-auto-commit 미지정 시)
    if commit_hash is None and auto_detect_commit:
        commit_hash = find_commit_hash(tk_id)
        if commit_hash:
            print(f"   ✓ commit auto-detected: {commit_hash}")
        else:
            print(f"   ⚠️ commit hash 자동탐지 실패 (계속 진행)")

    # 3. close comment 구성
    comment = f"{tk_id} 완료"
    if commit_hash:
        comment += f" — commit {commit_hash}"

    if dry_run:
        print(f"   🔁 DRY-RUN: gh issue close {issue_no} --reason completed --comment '{comment}'")
        return True

    # 4. Issue close
    if not close_issue(repo, issue_no, comment):
        return False
    print(f"   ✓ Issue closed")

    # 5. Project Status → Done (fallback, workflow도 동일 효과)
    project_id, field_id, done_option_id = status_meta
    item_id = find_project_item_id(project, issue_no)
    if not item_id:
        print(f"   ⚠️ Project item not found (Project workflow가 처리할 것)")
        return True
    if set_status_done(project_id, item_id, field_id, done_option_id):
        print(f"   ✓ Project Status → Done")
    return True


def main():
    ap = argparse.ArgumentParser(description='Task 완료 처리: Issue close + Project Status → Done')
    ap.add_argument('tk_ids', nargs='+', help='Task ID (예: TK-00-2-1)')
    ap.add_argument('--repo', default=DEFAULT_REPO)
    ap.add_argument('--project', type=int, default=DEFAULT_PROJECT)
    ap.add_argument('--commit', help='고정 commit hash (지정 시 모든 TK에 동일 적용)')
    ap.add_argument('--no-auto-commit', action='store_true',
                    help='commit hash 자동탐지 비활성 (close comment에 commit 제외)')
    ap.add_argument('--dry-run', action='store_true')
    args = ap.parse_args()

    # TK-ID 검증
    invalid = [t for t in args.tk_ids if not TK_ID_RE.match(t)]
    if invalid:
        print(f"❌ 잘못된 TK-ID 형식: {invalid}", file=sys.stderr)
        sys.exit(1)

    print(f"📡 repo  = {args.repo}")
    print(f"📡 project = #{args.project}")
    print(f"📋 대상 Task: {len(args.tk_ids)}건")

    # Project Status meta (1회 fetch)
    try:
        status_meta = fetch_project_status_meta(args.project)
        print(f"   ✓ Status field 'Done' option ready")
    except Exception as e:
        print(f"   ⚠️ Status meta fetch 실패: {e}", file=sys.stderr)
        status_meta = ('', '', '')

    ok = 0
    fail = 0
    auto_detect = not args.no_auto_commit
    for tk_id in args.tk_ids:
        if process_task(tk_id, args.repo, args.project, args.commit,
                        auto_detect, args.dry_run, status_meta):
            ok += 1
        else:
            fail += 1

    print(f"\n✅ 결과: 성공 {ok} / 실패 {fail}")
    if fail:
        sys.exit(2)


if __name__ == '__main__':
    main()
