import { useState } from 'react'
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core'
import { Alert, Card, Col, message, Row, Skeleton, Space, Tag } from 'antd'
import { DraggableHose } from './DraggableHose'
import { DroppableSlot } from './DroppableSlot'
import { ViolationModal } from './ViolationModal'
import { useDragValidation } from '../hooks/useDragValidation'
import { ALL_SLOTS, slotMachineType, type DragData, type SlotPosition } from '../types/slotCompat'
import type { ViolationInfo } from '../types/violation'

interface HoseCandidate {
  hoseId: string
  qty: number
}

interface Props {
  candidates: HoseCandidate[]
  /** 슬롯에 이미 배정된 항목 (Sprint 2 baseline — 빈 Map). */
  initialAssignments?: Partial<Record<SlotPosition, HoseCandidate>>
  /** 적합 슬롯에 drop 시 호출. 비적합 drop 은 자동 차단되어 호출 안 됨. */
  onAssign?: (slot: SlotPosition, hose: HoseCandidate) => void
}

/**
 * 성형 가류 간트 보드 (Sprint 2 baseline 골격) — TK-04-3-1.
 *
 * <p>좌측 후보 패널 + 우측 7 슬롯 (LP 4 + IC 3). 매트릭스 사전 검증으로
 * 비적합 슬롯에 드롭 시 onAssign 미호출. TK-04-3-2 에서 override 모달 추가.
 */
export function VcGanttBoard({ candidates, initialAssignments = {}, onAssign }: Props) {
  const { validate, isHoseUnschedulable, isLoading, isError, matrixVersion } = useDragValidation()
  const [draggedId, setDraggedId] = useState<string | undefined>()
  const [assignments, setAssignments] =
    useState<Partial<Record<SlotPosition, HoseCandidate>>>(initialAssignments)
  const [violation, setViolation] = useState<ViolationInfo | null>(null)

  const handleDragStart = (event: DragStartEvent) => {
    setDraggedId(String(event.active.id))
  }

  const place = (slot: SlotPosition, hose: HoseCandidate) => {
    setAssignments((prev) => ({ ...prev, [slot]: hose }))
    onAssign?.(slot, hose)
  }

  const handleDragEnd = (event: DragEndEvent) => {
    setDraggedId(undefined)
    if (!event.over) return

    const slot = event.over.id as SlotPosition
    const data = event.active.data.current as DragData | undefined
    if (!data) return

    // ≤1초 가드 (REQ-NF-PER-006) — 실측 ms 단위
    const t0 = performance.now()
    const result = validate(data.hoseId, slot)
    const elapsed = performance.now() - t0
    if (elapsed > 1000) {
      // 매트릭스 조회는 인메모리 — 1초 초과는 비정상 (REQ-NF-PER-006).
      // 운영 가시화는 Sprint 3+ Sentry / Prometheus FE metric 으로.
    }

    if (!result.ok) {
      message.error(result.reason ?? '비적합 슬롯')
      setViolation({
        hoseId: data.hoseId,
        qty: data.qty,
        slot,
        reason: result.reason ?? '비적합',
        alternatives: result.alternatives ?? [],
      })
      return
    }
    place(slot, { hoseId: data.hoseId, qty: data.qty })
  }

  const handleOverride = (v: ViolationInfo) => {
    place(v.slot, { hoseId: v.hoseId, qty: v.qty })
    message.warning(`강제 배치 완료 — ${v.hoseId} → ${v.slot} (audit 기록됨)`)
  }

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 4 } }))

  if (isLoading) {
    return <Skeleton active paragraph={{ rows: 5 }} />
  }
  if (isError) {
    return <Alert type="error" message="매트릭스 조회 실패 — 새로고침 후 재시도" showIcon />
  }

  const lpSlots = ALL_SLOTS.filter((s) => slotMachineType(s) === 'LP')
  const icSlots = ALL_SLOTS.filter((s) => slotMachineType(s) === 'IC')

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <Row gutter={16}>
        <Col span={8}>
          <Card title={<Space>후보 품번 <Tag>{candidates.length}건</Tag></Space>} size="small">
            <Space wrap>
              {candidates.map((c) => (
                <DraggableHose
                  key={c.hoseId}
                  hoseId={c.hoseId}
                  qty={c.qty}
                  disabled={isHoseUnschedulable(c.hoseId)}
                />
              ))}
            </Space>
          </Card>
        </Col>
        <Col span={16}>
          <Card
            title={<Space>가류기 슬롯 <Tag>matrix v{matrixVersion ?? '?'}</Tag></Space>}
            size="small"
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <strong style={{ marginBottom: 8, display: 'block' }}>LP (저압) 4 슬롯</strong>
                <Row gutter={8}>
                  {lpSlots.map((s) => (
                    <Col key={s} span={6}>
                      <DroppableSlot slotPosition={s} draggedHoseId={draggedId}>
                        {assignments[s] && (
                          <Tag color="processing">
                            {assignments[s]!.hoseId} · {assignments[s]!.qty}EA
                          </Tag>
                        )}
                      </DroppableSlot>
                    </Col>
                  ))}
                </Row>
              </div>
              <div>
                <strong style={{ marginBottom: 8, display: 'block' }}>IC 3 슬롯</strong>
                <Row gutter={8}>
                  {icSlots.map((s) => (
                    <Col key={s} span={8}>
                      <DroppableSlot slotPosition={s} draggedHoseId={draggedId}>
                        {assignments[s] && (
                          <Tag color="processing">
                            {assignments[s]!.hoseId} · {assignments[s]!.qty}EA
                          </Tag>
                        )}
                      </DroppableSlot>
                    </Col>
                  ))}
                </Row>
              </div>
            </Space>
          </Card>
        </Col>
      </Row>
      <DragOverlay>
        {draggedId ? (
          <DraggableHose
            hoseId={draggedId}
            qty={candidates.find((c) => c.hoseId === draggedId)?.qty ?? 0}
          />
        ) : null}
      </DragOverlay>
      <ViolationModal
        violation={violation}
        onClose={() => setViolation(null)}
        onOverride={handleOverride}
      />
    </DndContext>
  )
}
