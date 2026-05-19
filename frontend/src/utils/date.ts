// =============================================================================
// date.ts — KST 시간 통일 (BR-X04) — TK-34-3-1
// =============================================================================
// 모든 UI 시간 표시는 Asia/Seoul 기준. 한국어 locale (ko).
// 백엔드 timestamp 는 RFC3339 (yyyy-MM-dd'T'HH:mm:ssXXX) — Spring jackson.time-zone.
// =============================================================================

import dayjs, { type Dayjs } from 'dayjs';
import 'dayjs/locale/ko';
import duration from 'dayjs/plugin/duration';
import isoWeek from 'dayjs/plugin/isoWeek';
import relativeTime from 'dayjs/plugin/relativeTime';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(isoWeek);
dayjs.extend(duration);
dayjs.extend(relativeTime);

dayjs.tz.setDefault('Asia/Seoul');
dayjs.locale('ko');

export const KST = 'Asia/Seoul' as const;

/**
 * 백엔드 timestamp(또는 Date) → KST 표시 문자열.
 * 기본 포맷: '2026-05-19 15:42:33'
 */
export function formatKst(d: string | number | Date | Dayjs, fmt = 'YYYY-MM-DD HH:mm:ss'): string {
    return dayjs(d).tz(KST).format(fmt);
}

/**
 * 영업일 기준 일자 (시간 제외) — D-Day · D-1 · D-2 계산용.
 */
export function toBusinessDate(d: string | number | Date | Dayjs): Dayjs {
    return dayjs(d).tz(KST).startOf('day');
}

/**
 * 현재 KST 시각 — Clock 주입 패턴의 frontend 대응체.
 * 테스트에서 mock 가능 (vi.setSystemTime).
 */
export function nowKst(): Dayjs {
    return dayjs().tz(KST);
}

/**
 * 한국 표준시 = UTC+9 고정 (DST 없음 — 1988 이후 미적용). 검증용 헬퍼.
 */
export function isKstFixedOffset(d: string | number | Date | Dayjs = nowKst()): boolean {
    return dayjs(d).tz(KST).utcOffset() === 540;     // 9*60 = 540분
}
