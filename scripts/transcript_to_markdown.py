#!/usr/bin/env python3
"""
Claude Code transcript JSONL → 일자별 마크다운 누적 변환기.

증분 모드 (기본): 0.Pprompt/ 안의 다른 날짜 파일들의 마지막 timestamp 이후 메시지만
                  오늘 파일로 빌드. 어제까지의 기록은 보존.
전체 모드 (--full): JSONL 전체를 오늘 파일로 빌드.

표준 라이브러리만 사용. UTF-8 입출력. KST 기준 날짜.

사용:
    python scripts/transcript_to_markdown.py
    python scripts/transcript_to_markdown.py --full
    python scripts/transcript_to_markdown.py --include-thinking
    python scripts/transcript_to_markdown.py --tool-result-limit 3000
    python scripts/transcript_to_markdown.py --tool-result-limit 0   # 무제한
"""

import argparse
import io
import json
import os
import re
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Iterator, Optional, Tuple

# Windows cp949 환경에서도 UTF-8 출력 보장
if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', line_buffering=True)

KST = timezone(timedelta(hours=9))

# 본문 안에 invisible HTML comment로 ISO timestamp를 임베드 → 증분 cutoff 검출용
TS_MARKER_RE = re.compile(r'<!-- ts: (\S+) -->')

# 일자별 파일 패턴
DATE_FILE_RE = re.compile(r'^대화기록_(\d{4}-\d{2}-\d{2})(?:_\d+)?\.md$')


# ---------- 경로 유틸 ----------

def sanitize_path(p: Path) -> str:
    """절대 경로를 영문/숫자/하이픈만 남기고 그 외는 '-'로 치환."""
    s = str(p.resolve())
    return re.sub(r'[^A-Za-z0-9-]', '-', s)


def find_jsonl(project_root: Path) -> Path:
    """~/.claude/projects/{sanitized-cwd}/ 안 가장 최근 mtime의 *.jsonl 파일 반환."""
    home = Path.home()
    sanitized = sanitize_path(project_root)
    target_dir = home / '.claude' / 'projects' / sanitized
    if not target_dir.exists():
        raise FileNotFoundError(
            f"Claude 프로젝트 디렉토리 없음:\n  {target_dir}\n"
            f"  (sanitized cwd: {sanitized})"
        )
    candidates = list(target_dir.glob('*.jsonl'))
    if not candidates:
        raise FileNotFoundError(f"JSONL 파일 없음: {target_dir}")
    return max(candidates, key=lambda f: f.stat().st_mtime)


# ---------- timestamp 파싱 ----------

def parse_timestamp(ts: Optional[str]) -> Optional[datetime]:
    """ISO8601 timestamp → datetime (timezone-aware) 변환. 실패 시 None."""
    if not ts or not isinstance(ts, str):
        return None
    try:
        # 'Z' suffix → +00:00 치환
        if ts.endswith('Z'):
            ts = ts[:-1] + '+00:00'
        return datetime.fromisoformat(ts)
    except (ValueError, TypeError):
        return None


def format_timestamp_kst(ts: Optional[str]) -> str:
    """ISO8601 → KST 'YYYY-MM-DD HH:MM:SS'."""
    dt = parse_timestamp(ts)
    if dt is None:
        return ts or '(시각 미상)'
    return dt.astimezone(KST).strftime('%Y-%m-%d %H:%M:%S')


# ---------- 증분 모드 cutoff ----------

def find_cutoff(prompt_dir: Path, today_filename: str) -> Optional[str]:
    """
    0.Pprompt/ 안의 오늘이 아닌 대화기록 파일들에서 마지막 ISO timestamp 추출.
    가장 최근(date) 파일의 가장 마지막 임베드 timestamp 반환.
    파일들에 timestamp marker가 없으면 None.
    """
    if not prompt_dir.exists():
        return None
    files = sorted(
        [
            f for f in prompt_dir.glob('대화기록_*.md')
            if f.name != today_filename and DATE_FILE_RE.match(f.name)
        ],
        key=lambda f: f.name,
    )
    if not files:
        return None
    # 가장 최근 파일부터 역순으로 timestamp 검색 (빈 파일 대비)
    for f in reversed(files):
        try:
            text = f.read_text(encoding='utf-8')
        except OSError:
            continue
        matches = TS_MARKER_RE.findall(text)
        if matches:
            return matches[-1]
    return None


# ---------- JSONL 파싱 ----------

def iter_jsonl(path: Path) -> Iterator[dict]:
    """JSONL 라인별 dict yield (파싱 실패 라인은 stderr 보고 후 skip)."""
    with open(path, 'r', encoding='utf-8') as f:
        for lineno, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            try:
                yield json.loads(line)
            except json.JSONDecodeError as e:
                print(f"  ⚠️ Line {lineno} JSON 파싱 실패 — 건너뜀 ({e})", file=sys.stderr)


# ---------- 콘텐츠 렌더링 ----------

def truncate(text: str, limit: int) -> str:
    """limit자 초과 시 절단 + '... (생략됨, 총 NNN자)' 표기. limit=0이면 무제한."""
    if limit <= 0 or len(text) <= limit:
        return text
    return text[:limit] + f"\n\n... (생략됨, 총 {len(text):,}자)"


def render_user_block(content, tool_result_limit: int) -> Tuple[str, int, int]:
    """
    user message의 content 블록을 마크다운 본문으로 변환.
    Returns: (rendered_text, user_msg_count_increment, tool_result_count)
    """
    text_parts = []
    is_tool_result_only = True
    tool_result_count = 0

    if isinstance(content, str):
        # 단순 문자열 user message
        text_parts.append(content.rstrip() + "\n")
        is_tool_result_only = False
    elif isinstance(content, list):
        for block in content:
            if not isinstance(block, dict):
                continue
            btype = block.get('type')
            if btype == 'text':
                text_parts.append(block.get('text', '').rstrip() + "\n")
                is_tool_result_only = False
            elif btype == 'tool_result':
                tool_result_count += 1
                tool_use_id = block.get('tool_use_id', '')
                short_id = tool_use_id[-8:] if len(tool_use_id) > 8 else tool_use_id
                raw_content = block.get('content', '')
                # content가 list (block list)일 수도, string일 수도
                if isinstance(raw_content, list):
                    result_text = '\n'.join(
                        b.get('text', '') if isinstance(b, dict) else str(b)
                        for b in raw_content
                    )
                else:
                    result_text = str(raw_content)
                truncated = truncate(result_text, tool_result_limit)
                is_error = block.get('is_error', False)
                icon = '⚠️' if is_error else '📤'
                summary_label = '도구 결과 (오류)' if is_error else '도구 결과'
                text_parts.append(
                    f"\n<details><summary>{icon} {summary_label} <code>{short_id}</code></summary>\n\n"
                    f"```\n{truncated}\n```\n\n"
                    f"</details>\n"
                )

    user_inc = 0 if is_tool_result_only else 1
    return ''.join(text_parts), user_inc, tool_result_count


def render_assistant_block(content, include_thinking: bool, tool_result_limit: int) -> Tuple[str, int]:
    """
    assistant message의 content 블록을 마크다운 본문으로 변환.
    Returns: (rendered_text, tool_use_count)
    """
    text_parts = []
    tool_use_count = 0

    if isinstance(content, str):
        text_parts.append(content.rstrip() + "\n")
    elif isinstance(content, list):
        for block in content:
            if not isinstance(block, dict):
                continue
            btype = block.get('type')
            if btype == 'text':
                text_parts.append(block.get('text', '').rstrip() + "\n\n")
            elif btype == 'thinking':
                if include_thinking:
                    thinking_text = block.get('thinking', '')
                    truncated = truncate(thinking_text, tool_result_limit)
                    text_parts.append(
                        f"\n<details><summary>💭 사고 과정</summary>\n\n"
                        f"```\n{truncated}\n```\n\n"
                        f"</details>\n\n"
                    )
            elif btype == 'tool_use':
                tool_use_count += 1
                tool_name = block.get('name', '?')
                tool_id = block.get('id', '')
                short_id = tool_id[-8:] if len(tool_id) > 8 else tool_id
                tool_input = block.get('input', {})
                try:
                    input_repr = json.dumps(tool_input, ensure_ascii=False, indent=2)
                except (TypeError, ValueError):
                    input_repr = repr(tool_input)
                truncated = truncate(input_repr, tool_result_limit)
                text_parts.append(
                    f"\n<details><summary>🛠️ <b>{tool_name}</b> <code>{short_id}</code></summary>\n\n"
                    f"```json\n{truncated}\n```\n\n"
                    f"</details>\n\n"
                )

    return ''.join(text_parts), tool_use_count


# ---------- 메인 변환 ----------

def convert(
    jsonl_path: Path,
    out_path: Path,
    project_name: str,
    cutoff: Optional[str],
    include_thinking: bool,
    tool_result_limit: int,
) -> dict:
    """JSONL → 마크다운 파일 작성. 변환 통계 반환."""
    cutoff_dt = parse_timestamp(cutoff) if cutoff else None

    out_lines = [f"# {project_name} 프로젝트 — 대화 기록\n\n"]
    skipped = 0
    user_count = 0
    assistant_count = 0
    tool_count = 0  # tool_use + tool_result 합산
    other_count = 0

    for entry in iter_jsonl(jsonl_path):
        ts = entry.get('timestamp', '')
        ts_dt = parse_timestamp(ts)

        # cutoff 이전 (또는 같음) 제외
        if cutoff_dt and ts_dt and ts_dt <= cutoff_dt:
            skipped += 1
            continue

        etype = entry.get('type', '')
        ts_marker = f"<!-- ts: {ts} -->" if ts else ""
        ts_display = format_timestamp_kst(ts)

        if etype == 'user':
            content = entry.get('message', {}).get('content')
            body, user_inc, tool_res_cnt = render_user_block(content, tool_result_limit)
            user_count += user_inc
            tool_count += tool_res_cnt
            heading = f"### 👤 사용자 — {ts_display}\n{ts_marker}\n\n" if user_inc \
                else f"### 📥 도구 결과 — {ts_display}\n{ts_marker}\n\n"
            out_lines.append(heading + body + "\n---\n\n")

        elif etype == 'assistant':
            content = entry.get('message', {}).get('content', [])
            body, tool_use_cnt = render_assistant_block(content, include_thinking, tool_result_limit)
            assistant_count += 1
            tool_count += tool_use_cnt
            heading = f"### 🤖 어시스턴트 — {ts_display}\n{ts_marker}\n\n"
            out_lines.append(heading + body + "---\n\n")

        elif etype in ('summary', 'system'):
            other_count += 1
            summary_text = entry.get('summary') or entry.get('content') or ''
            if not isinstance(summary_text, str):
                summary_text = json.dumps(summary_text, ensure_ascii=False)[:300]
            out_lines.append(
                f"<details><summary>⚙️ {etype} — {ts_display}</summary>\n{ts_marker}\n\n"
                f"```\n{summary_text}\n```\n\n"
                f"</details>\n\n"
            )

        # 기타 type은 silent skip

    out_path.write_text(''.join(out_lines), encoding='utf-8')

    return {
        'skipped': skipped,
        'user': user_count,
        'assistant': assistant_count,
        'tool': tool_count,
        'other': other_count,
        'size': out_path.stat().st_size,
    }


# ---------- 보조 파일 정리 (_N suffix) ----------

def cleanup_suffix_files(prompt_dir: Path, today_filename: str) -> int:
    """
    오늘 파일의 _N suffix 보조 파일 (예: 대화기록_2026-05-15_1.md) 삭제.
    Returns: 정리한 파일 개수.
    """
    if not prompt_dir.exists():
        return 0
    base = today_filename[:-3]  # .md 제거
    pattern = re.compile(rf'^{re.escape(base)}_\d+\.md$')
    cleaned = 0
    for f in prompt_dir.iterdir():
        if f.is_file() and pattern.match(f.name):
            print(f"  🧹 보조 파일 정리: {f.name}")
            try:
                f.unlink()
                cleaned += 1
            except OSError as e:
                print(f"     ⚠️ 삭제 실패: {e}", file=sys.stderr)
    return cleaned


# ---------- 엔트리 포인트 ----------

def main():
    parser = argparse.ArgumentParser(
        description='Claude Code transcript JSONL → 일자별 마크다운 누적 변환기',
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        '--full',
        action='store_true',
        help='증분 모드 무시, JSONL 전체를 오늘 파일로 빌드',
    )
    parser.add_argument(
        '--include-thinking',
        action='store_true',
        help="assistant 'thinking' 블록도 포함 (기본 제외)",
    )
    parser.add_argument(
        '--tool-result-limit',
        type=int,
        default=1500,
        metavar='N',
        help='도구 결과 N자 절단 (기본 1500, 0=무제한)',
    )
    args = parser.parse_args()

    # 프로젝트 루트 = 본 스크립트의 부모의 부모 (scripts/ → root)
    project_root = Path(__file__).resolve().parent.parent
    project_name = project_root.name

    # 출력 폴더
    prompt_dir = project_root / '0.Pprompt'
    prompt_dir.mkdir(parents=True, exist_ok=True)

    # 오늘 파일명 (KST)
    today = datetime.now(KST).strftime('%Y-%m-%d')
    today_filename = f'대화기록_{today}.md'
    out_path = prompt_dir / today_filename

    # JSONL 자동 탐색
    try:
        jsonl_path = find_jsonl(project_root)
    except FileNotFoundError as e:
        print(f"❌ {e}", file=sys.stderr)
        sys.exit(1)

    print(f"📂 JSONL 입력: {jsonl_path}")
    print(f"   (크기: {jsonl_path.stat().st_size:,} bytes, mtime: "
          f"{datetime.fromtimestamp(jsonl_path.stat().st_mtime, KST).strftime('%Y-%m-%d %H:%M:%S')} KST)")
    print(f"📝 마크다운 출력: {out_path}")

    # 보조 _N suffix 파일 정리
    cleaned = cleanup_suffix_files(prompt_dir, today_filename)
    if cleaned:
        print(f"   {cleaned}개 보조 파일 정리됨")

    # cutoff 결정
    if args.full:
        cutoff = None
        print("🔁 모드: 전체 변환 (--full)")
    else:
        cutoff = find_cutoff(prompt_dir, today_filename)
        if cutoff:
            print(f"🔁 모드: 증분 (cutoff = {cutoff})")
        else:
            print("🔁 모드: 증분 (이전 일자 파일 없음 — 첫 빌드 = 전체 변환)")

    # 변환 실행
    print()
    print("⏳ 변환 중...")
    stats = convert(
        jsonl_path=jsonl_path,
        out_path=out_path,
        project_name=project_name,
        cutoff=cutoff,
        include_thinking=args.include_thinking,
        tool_result_limit=args.tool_result_limit,
    )

    # 결과 보고
    print()
    print(f"✅ 완료")
    print(f"   출력 파일: {out_path}")
    print(f"   크기: {stats['size']:,} bytes")
    print(f"   사용자 메시지: {stats['user']:,}건")
    print(f"   어시스턴트 응답: {stats['assistant']:,}건")
    print(f"   도구 호출/결과: {stats['tool']:,}건")
    if stats['other']:
        print(f"   기타(summary/system): {stats['other']:,}건")
    if cutoff:
        print(f"   cutoff 이전 건너뜀: {stats['skipped']:,}건")


if __name__ == '__main__':
    main()
