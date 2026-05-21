import { useDroppable } from '@dnd-kit/core'
import { CloseCircleFilled, CheckCircleFilled } from '@ant-design/icons'
import { useDragValidation } from '../hooks/useDragValidation'
import type { SlotPosition } from '../types/slotCompat'

interface Props {
  slotPosition: SlotPosition
  /** 현재 드래그 중인 hose_id (없으면 호버 시각화 비활성). */
  draggedHoseId?: string
  /** 슬롯 내 콘텐츠 (배정된 카드). */
  children?: React.ReactNode
}

/**
 * 드래그 가능 슬롯 — TK-04-3-1 (REQ-FUNC-VC-004).
 *
 * <p>매트릭스 사전 검증:
 * <ul>
 *   <li>호버 + 적합 → 녹색 border + check 아이콘</li>
 *   <li>호버 + 비적합 → 빨간 border + 금지 아이콘 (≤1초 시각화 — NFR-PER-006)</li>
 *   <li>비호버 → 기본 회색 border</li>
 * </ul>
 *
 * <p>실제 drop 차단 + override 모달은 TK-04-3-2.
 */
export function DroppableSlot({ slotPosition, draggedHoseId, children }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id: slotPosition })
  const { check, isLoading } = useDragValidation()

  const eligible = draggedHoseId ? check(draggedHoseId, slotPosition) : true
  const showHover = isOver && !!draggedHoseId

  const style: React.CSSProperties = {
    minHeight: 60,
    padding: 10,
    border: showHover
      ? `2px solid ${eligible ? '#52c41a' : '#ff4d4f'}`
      : '1px dashed #d9d9d9',
    borderRadius: 4,
    background: showHover ? (eligible ? '#f6ffed' : '#fff1f0') : '#fafafa',
    transition: 'border-color 0.1s, background 0.1s',
    position: 'relative',
    opacity: isLoading ? 0.5 : 1,
  }

  return (
    <div
      ref={setNodeRef}
      style={style}
      aria-label={`슬롯 ${slotPosition}${showHover ? (eligible ? ' (적합)' : ' (비적합)') : ''}`}
      data-testid={`droppable-slot-${slotPosition}`}
      data-eligible={eligible}
      data-hover={showHover}
    >
      <div style={{ fontWeight: 600, color: '#595959', marginBottom: 4 }}>
        {slotPosition}
        {showHover && eligible && (
          <CheckCircleFilled
            style={{ color: '#52c41a', marginLeft: 8 }}
            aria-label="적합"
            data-testid={`slot-icon-ok-${slotPosition}`}
          />
        )}
        {showHover && !eligible && (
          <CloseCircleFilled
            style={{ color: '#ff4d4f', marginLeft: 8 }}
            aria-label="비적합 — 드롭 차단"
            data-testid={`slot-icon-blocked-${slotPosition}`}
          />
        )}
      </div>
      {children}
    </div>
  )
}
