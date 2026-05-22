/**
 * EX 매트릭스 + replan cascade TypeScript types — Sprint 5 EP-17·EX14 통합.
 *
 * <p>백엔드 Java record 와 1:1 동기화:
 * <ul>
 *   <li>{@code com.scheduling.ex.events.ExReplanCompletedEvent}</li>
 *   <li>{@code com.scheduling.ex.schedule.ExScheduleCandidate} (status)</li>
 * </ul>
 *
 * <p>type drift 방지 — 백엔드 record 수정 시 본 파일 동시 갱신.
 */

/** 백엔드 com.scheduling.ex.schedule.CandidateStatus. */
export type ExCandidateStatus =
  | 'PENDING'
  | 'READY'
  | 'SCHEDULED'
  | 'CONFIRMED'
  | 'FAILED'

/** EP-EX14 ExReplanCompletedEvent — STOMP /topic/extrusion-updates payload. */
export interface ExReplanCompletedPayload {
  vcScheduleId: string
  completedAt: string // ISO 8601 UTC (BR-X04 KST 변환은 UI 책임)
  triggeredCount: number
  candidateIds: string[]
}

/** EP-17 매트릭스 row — hose_id × 일자 × shift × line. */
export interface ExMatrixCell {
  hoseId: string
  date: string // YYYY-MM-DD
  shiftCode: string
  lineId: string
  yield: number
  status: ExCandidateStatus
}
