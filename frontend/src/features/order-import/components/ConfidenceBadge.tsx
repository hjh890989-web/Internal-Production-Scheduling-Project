import { Tag } from 'antd'
import type { SourceType } from '../types/mapping'

interface Props {
  sourceType?: SourceType
  confidence: number // 0.0 ~ 1.0
}

const SOURCE_LABEL: Record<SourceType, string> = {
  MONTHLY_FORECAST: '월별 예상',
  WEEKLY_PLAN: '주간 계획',
  CONFIRMED_ORDER: '확정 발주',
  KD_ORDER: 'KD 발주',
  UNRECOGNIZED: '미식별',
}

/**
 * 분류 + 신뢰도 시각화 — TK-01-2-2.
 *
 * - confidence ≥ 0.7 → green
 * - 0.5 ≤ confidence < 0.7 → orange
 * - confidence < 0.5 → red
 */
export function ConfidenceBadge({ sourceType, confidence }: Props) {
  const label = sourceType ? SOURCE_LABEL[sourceType] : '미분류'
  const color = confidence >= 0.7 ? 'green' : confidence >= 0.5 ? 'orange' : 'red'
  return (
    <Tag color={color}>
      {label} · {(confidence * 100).toFixed(0)}%
    </Tag>
  )
}
