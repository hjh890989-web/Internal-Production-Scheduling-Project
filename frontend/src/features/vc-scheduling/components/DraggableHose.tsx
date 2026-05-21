import { useDraggable } from '@dnd-kit/core'
import { CSS } from '@dnd-kit/utilities'
import { Tag, Tooltip } from 'antd'
import type { DragData } from '../types/slotCompat'

interface Props {
  hoseId: string
  qty: number
  /** BR-V11 — 드래그 자체를 차단할지 여부 (Unschedulable 품번). */
  disabled?: boolean
}

/**
 * 드래그 가능 품번 카드 — TK-04-3-1.
 *
 * <p>{@code data} payload 에 hose_id + qty 포함 → DroppableSlot 이 useDragValidation 으로 검증.
 * disabled=true (Unschedulable) → cursor not-allowed + 회색 처리.
 */
export function DraggableHose({ hoseId, qty, disabled = false }: Props) {
  const data: DragData = { hoseId, qty }
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: hoseId,
    data,
    disabled,
  })

  const style: React.CSSProperties = {
    transform: CSS.Translate.toString(transform),
    cursor: disabled ? 'not-allowed' : isDragging ? 'grabbing' : 'grab',
    opacity: disabled ? 0.5 : 1,
    padding: '6px 10px',
    border: '1px solid #d9d9d9',
    borderRadius: 4,
    background: disabled ? '#f5f5f5' : '#fff',
    userSelect: 'none',
    display: 'inline-flex',
    gap: 8,
    alignItems: 'center',
  }

  const card = (
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      aria-disabled={disabled}
      aria-label={`품번 ${hoseId} · 수량 ${qty}`}
      data-testid={`draggable-hose-${hoseId}`}
    >
      <strong>{hoseId}</strong>
      <Tag color={disabled ? 'default' : 'blue'}>{qty}EA</Tag>
      {disabled && <Tag color="red">스케줄 불가</Tag>}
    </div>
  )

  return disabled ? (
    <Tooltip title="BR-V11 — 모든 슬롯에서 적합성 X (외주·재고 대응 권고)">{card}</Tooltip>
  ) : (
    card
  )
}
