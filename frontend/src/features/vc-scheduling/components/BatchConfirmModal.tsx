import { useMemo, useState } from 'react'
import { Alert, Button, Descriptions, Modal, Space, Tag, Typography } from 'antd'
import { CheckCircleOutlined } from '@ant-design/icons'
import { HttpError } from '@/api/client'
import { confirmVcScheduleBatch } from '../api/vcScheduleApi'
import type { VcSlotRow } from '../api/vcScheduleApi'

const { Text } = Typography

interface Props {
  /** 선택된 row (이미 자기 작성 제외 필터링 완료된 상태로 전달). */
  selected: VcSlotRow[]
  /** 자기 작성으로 자동 제외된 row 수 (사용자 안내). */
  excludedSelfAuthoredCount: number
  open: boolean
  onClose: () => void
  onSuccess: (confirmedCount: number) => void
}

interface BranchedMessage {
  level: 'error' | 'warning'
  title: string
  description: string
  brCode?: string
}

/**
 * Sprint 17 hotfix — Batch 확정 Modal (BR-X01·X05 정합).
 *
 * <p>{@link confirmVcScheduleBatch} POST /confirm-batch — backend autocommit
 * (자기 작성 row 포함 시 전체 409 reject). Frontend 가 미리 자기 작성 제외하면 깨끗한 200.
 *
 * <p>응답 분기:
 * <ul>
 *   <li>200 → onSuccess(count)</li>
 *   <li>409 BR-X05 → "선택한 row 중 본인 작성 포함" 안내 (선택 갱신 권고)</li>
 *   <li>423 BR-X07 → D-2 hard (드물게 — 보통 INSERT 차단)</li>
 *   <li>403 / 400 / 기타 → 일반 안내</li>
 * </ul>
 */
export function BatchConfirmModal({
  selected,
  excludedSelfAuthoredCount,
  open,
  onClose,
  onSuccess,
}: Props) {
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<BranchedMessage | null>(null)

  const summary = useMemo(() => {
    const byMachine = new Map<string, number>()
    const byHose = new Map<string, number>()
    let totalQty = 0
    for (const r of selected) {
      byMachine.set(r.machineId, (byMachine.get(r.machineId) ?? 0) + 1)
      byHose.set(r.hoseId, (byHose.get(r.hoseId) ?? 0) + 1)
      totalQty += r.plannedQty
    }
    return { byMachine, byHose, totalQty }
  }, [selected])

  const branchFromError = (e: unknown): BranchedMessage => {
    if (e instanceof HttpError) {
      const body = (e.body ?? {}) as { brCode?: string; detail?: string }
      const detail = body.detail ?? e.message
      if (e.status === 409 && body.brCode === 'BR-X05') {
        return {
          level: 'error',
          title: 'BR-X05 dual-review — 일괄 확정 거부',
          description:
            '선택한 row 중 본인이 작성한 row 가 포함되어 전체 거부되었습니다. 본인 작성 row 는 자동 제외되어 있으나, 페이지 갱신 후 재시도 권고.',
          brCode: 'BR-X05',
        }
      }
      if (e.status === 423 && body.brCode === 'BR-V07') {
        return {
          level: 'error',
          title: 'BR-V07 D-0 (당일) 락',
          description:
            '선택한 row 중 오늘 (D-0) 의 row 가 포함되어 일괄 확정 거부되었습니다. 일중 앵글 교체 시 OverrideJustificationForm 으로 사유 입력 후 단건 처리 권고.',
          brCode: 'BR-V07',
        }
      }
      if (e.status === 423) {
        return { level: 'error', title: 'BR-X07 D-2 hard 제약', description: detail, brCode: body.brCode ?? 'BR-X07' }
      }
      if (e.status === 409) {
        return { level: 'warning', title: '확정 상태 충돌', description: detail, brCode: body.brCode }
      }
      if (e.status === 403) {
        return { level: 'error', title: '권한 없음', description: 'PLANNER 권한이 필요합니다.' }
      }
      if (e.status === 400) {
        return { level: 'error', title: '입력 검증 실패', description: detail }
      }
      return { level: 'error', title: `HTTP ${e.status}`, description: detail }
    }
    return {
      level: 'error',
      title: '확정 실패',
      description: e instanceof Error ? e.message : String(e),
    }
  }

  const handleConfirm = async () => {
    if (selected.length === 0) return
    setLoading(true)
    setMessage(null)
    try {
      const batchId = crypto.randomUUID()
      const res = await confirmVcScheduleBatch(
        selected.map((r) => r.vcScheduleId),
        batchId,
      )
      onSuccess(res.confirmedCount)
      handleClose()
    } catch (e) {
      setMessage(branchFromError(e))
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    setMessage(null)
    onClose()
  }

  return (
    <Modal
      title={
        <Space>
          <CheckCircleOutlined style={{ color: '#52c41a' }} />
          <span>VC schedule 일괄 확정 (BR-X01·X05)</span>
        </Space>
      }
      open={open}
      onCancel={handleClose}
      width={620}
      destroyOnClose
      footer={[
        <Button
          key="cancel"
          onClick={handleClose}
          disabled={loading}
          data-testid="batch-confirm-cancel"
        >
          취소
        </Button>,
        <Button
          key="ok"
          type="primary"
          loading={loading}
          disabled={selected.length === 0}
          onClick={handleConfirm}
          data-testid="batch-confirm-ok"
        >
          {selected.length} 건 확정
        </Button>,
      ]}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {excludedSelfAuthoredCount > 0 && (
          <Alert
            type="warning"
            showIcon
            message="BR-X05 dual-review — 본인 작성 row 자동 제외"
            description={`선택 항목 중 본인(작성자) row ${excludedSelfAuthoredCount} 건은 자동 제외되어 확정 대상에서 빠집니다. 다른 PLANNER 가 별도 확정해야 합니다.`}
            data-testid="batch-confirm-excluded"
          />
        )}
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="확정 대상">
            <Text strong>{selected.length}</Text> 건 (본인 작성 {excludedSelfAuthoredCount} 건 제외 후)
          </Descriptions.Item>
          <Descriptions.Item label="총 계획 수량">
            <Text strong>{summary.totalQty.toLocaleString()}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="가류기별">
            <Space wrap>
              {[...summary.byMachine.entries()].map(([m, c]) => (
                <Tag key={m} color="blue">
                  {m} · {c}건
                </Tag>
              ))}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="Hose ID 별">
            <Space wrap>
              {[...summary.byHose.entries()].map(([h, c]) => (
                <Tag key={h}>
                  {h} · {c}건
                </Tag>
              ))}
            </Space>
          </Descriptions.Item>
        </Descriptions>
        {message && (
          <Alert
            type={message.level}
            showIcon
            message={message.title}
            description={
              <Space direction="vertical" size={2}>
                <span>{message.description}</span>
                {message.brCode && <Text type="secondary">코드: {message.brCode}</Text>}
              </Space>
            }
            data-testid="batch-confirm-error"
          />
        )}
      </Space>
    </Modal>
  )
}
