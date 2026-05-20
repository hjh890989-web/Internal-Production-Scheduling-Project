// TK-01-2-2 — Frontend 매핑 보정 UI TypeScript 타입.
// 백엔드 com.scheduling.order.mapping.* + com.scheduling.order.parser.* + .api.ImportStatusResponse 와 정합.

export type SourceType =
  | 'MONTHLY_FORECAST'
  | 'WEEKLY_PLAN'
  | 'CONFIRMED_ORDER'
  | 'KD_ORDER'
  | 'UNRECOGNIZED'

export type ImportStatus =
  | 'QUEUED'
  | 'PARSING'
  | 'PARSED'
  | 'MAPPING'
  | 'MAPPED'
  | 'REVIEW_REQUIRED'
  | 'FAILED'

export type FailureField =
  | 'hose_id'
  | 'delivery_date'
  | 'qty'
  | 'customer'
  | 'HEADER'

/** com.scheduling.order.mapping.MappingFailure (record) 정합. */
export interface MappingFailure {
  sheetName: string
  rowIndex: number
  failedField: FailureField
  reason: string
  originalCells: string[]
}

/** com.scheduling.order.domain.OrderDraft (record) 정합. */
export interface OrderDraft {
  orderId: string
  hoseId: string
  deliveryDate: string // ISO-8601 yyyy-MM-dd
  qty: number
  orderType: 'FORECAST' | 'WEEKLY' | 'KD' | 'CONFIRMED'
  customer: string
}

/** com.scheduling.order.mapping.MappingResult — `/api/v1/orders/import/{id}/mapping-result`. */
export interface MappingResult {
  successes: OrderDraft[]
  failures: MappingFailure[]
  sourceType: SourceType
}

/** com.scheduling.order.api.ImportStatusResponse. */
export interface ImportStatusResponse {
  trackingId: string
  status: ImportStatus
  startedAt: string
  updatedAt: string
  filenames: string[]
  classifications: Record<string, string> // "filename" → "SOURCE_TYPE:0.95"
  error: string | null
}

/** com.scheduling.order.mapping.FieldMapping (record) 정합. */
export interface FieldMapping {
  standardField: 'hose_id' | 'delivery_date' | 'qty' | 'customer'
  aliases: string[]
  dateFormatHints: string[]
  coerceInteger: boolean
  regexStrip: string | null
  toUpperCase: boolean
  defaultValue: string | null
  required: boolean
}

/** com.scheduling.order.mapping.MappingRule (record) 정합 — `/api/v1/master/mapping-rule/{sourceType}`. */
export interface MappingRule {
  sourceType: SourceType
  defaultOrderType: 'FORECAST' | 'WEEKLY' | 'KD' | 'CONFIRMED'
  fields: Record<string, FieldMapping>
  headerRowCandidates: number[]
  defaultCustomer: string
}

/** REQ-FUNC-OC-004 — 매핑 실패율 ≥1% 시 보정 모달 노출. */
export function requiresReviewModal(result: MappingResult): boolean {
  const total = result.successes.length + result.failures.length
  if (total === 0) return false
  return result.failures.length / total >= 0.01
}

export function successRate(result: MappingResult): number {
  const total = result.successes.length + result.failures.length
  return total === 0 ? 0 : result.successes.length / total
}
