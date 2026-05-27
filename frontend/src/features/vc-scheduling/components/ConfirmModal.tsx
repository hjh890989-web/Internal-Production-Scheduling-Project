import { useState } from 'react'
import { Alert, Button, Descriptions, Modal, Space, Tag, Typography } from 'antd'
import { CheckCircleOutlined } from '@ant-design/icons'
import { HttpError } from '@/api/client'
import { confirmVcSchedule } from '../api/vcScheduleApi'

const { Text } = Typography

export interface ConfirmTarget {
  vcScheduleId: string
  hoseId: string
  machineId: string
  productionDate: string
  rotationNo: number
  slotPosition: number
  plannedQty: number
}

interface Props {
  target: ConfirmTarget | null
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

interface BranchedMessage {
  level: 'error' | 'warning'
  title: string
  description: string
  brCode?: string
}

/**
 * Sprint 16 EP-CONFIRM — Planner 확정 Modal (TK-CONFIRM-5-1·2).
 *
 * <p>응답 분기:
 * <ul>
 *   <li>200 OK → onSuccess 콜백 (CONFIRMED 전이)</li>
 *   <li>409 brCode=BR-X05 → dual-review 안내 ("본인이 작성한 row 는 확정 불가")</li>
 *   <li>409 brCode=BR-X01 → 이미 CONFIRMED row 안내</li>
 *   <li>423 brCode=BR-X07 → D-2 hard 제약 안내</li>
 *   <li>400 validation → 입력 검증 실패 안내</li>
 *   <li>그 외 → 일반 오류 안내</li>
 * </ul>
 */
export function ConfirmModal({ target, open, onClose, onSuccess }: Props) {
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<BranchedMessage | null>(null)

  const branchFromError = (e: unknown): BranchedMessage => {
    if (e instanceof HttpError) {
      const body = (e.body ?? {}) as { brCode?: string; detail?: string; title?: string }
      const detail = body.detail ?? e.message
      if (e.status === 409 && body.brCode === 'BR-X05') {
        return {
          level: 'error',
          title: 'BR-X05 dual-review — 확정 불가',
          description: '본인이 작성한 row 는 확정할 수 없습니다. 다른 PLANNER 에게 승인을 요청하세요.',
          brCode: body.brCode,
        }
      }
      if (e.status === 409 && body.brCode === 'BR-X01') {
        return {
          level: 'warning',
          title: 'BR-X01 — 이미 확정된 row',
          description: detail,
          brCode: body.brCode,
        }
      }
      if (e.status === 423) {
        return {
          level: 'error',
          title: 'BR-X07 D-2 hard 제약',
          description: detail,
          brCode: body.brCode ?? 'BR-X07',
        }
      }
      if (e.status === 400) {
        return { level: 'error', title: '입력 검증 실패', description: detail }
      }
      if (e.status === 403) {
        return {
          level: 'error',
          title: '권한 없음',
          description: 'PLANNER 권한이 필요합니다.',
        }
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
    if (!target) return
    setLoading(true)
    setMessage(null)
    try {
      await confirmVcSchedule(target.vcScheduleId)
      onSuccess()
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

  if (!target) return null

  return (
    <Modal
      title={
        <Space>
          <CheckCircleOutlined style={{ color: '#52c41a' }} />
          <span>VC schedule 확정 (BR-X01·X05)</span>
        </Space>
      }
      open={open}
      onCancel={handleClose}
      width={560}
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={handleClose} disabled={loading} data-testid="confirm-cancel">
          취소
        </Button>,
        <Button
          key="ok"
          type="primary"
          loading={loading}
          onClick={handleConfirm}
          data-testid="confirm-ok"
        >
          확정
        </Button>,
      ]}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Alert
          type="info"
          showIcon
          message="dual-review 안내 (BR-X05)"
          description="본인이 작성한 row 는 확정 불가 — 다른 PLANNER 가 승인해야 합니다."
        />
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="Hose ID">
            <Tag color="blue">{target.hoseId}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="가류기 / 슬롯">
            {target.machineId} / slot {target.slotPosition}
          </Descriptions.Item>
          <Descriptions.Item label="생산일 / 회전">
            {target.productionDate} / rot {target.rotationNo}
          </Descriptions.Item>
          <Descriptions.Item label="계획 수량">
            <Text strong>{target.plannedQty}</Text>
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
            data-testid="confirm-error"
          />
        )}
      </Space>
    </Modal>
  )
}
