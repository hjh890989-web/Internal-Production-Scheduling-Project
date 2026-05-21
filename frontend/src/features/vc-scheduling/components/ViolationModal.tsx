import { useState } from 'react'
import { Alert, Button, Divider, Empty, Modal, Space, Tag, Typography } from 'antd'
import { WarningOutlined } from '@ant-design/icons'
import { OverrideJustificationForm } from './OverrideJustificationForm'
import { slotDisplayName, type ViolationInfo } from '../types/violation'

const { Title } = Typography

interface Props {
  violation: ViolationInfo | null
  onClose: () => void
  /** override audit 성공 시 — caller 가 강제 배치 수행. */
  onOverride: (violation: ViolationInfo) => void
}

/**
 * 슬롯 적합성 위반 모달 — TK-04-3-2 (NFR-USA-002 사유 + 대안 ≥1).
 *
 * <p>3 영역:
 * <ol>
 *   <li>사유 — Alert error</li>
 *   <li>대안 슬롯 — Tag 그리드 (없으면 Empty BR-V11)</li>
 *   <li>Override 버튼 — 클릭 시 OverrideJustificationForm 표시</li>
 * </ol>
 */
export function ViolationModal({ violation, onClose, onOverride }: Props) {
  const [showOverride, setShowOverride] = useState(false)

  if (!violation) return null

  const handleClose = () => {
    setShowOverride(false)
    onClose()
  }

  return (
    <Modal
      title={
        <Space>
          <WarningOutlined style={{ color: '#ff4d4f' }} />
          <span>슬롯 적합성 위반</span>
        </Space>
      }
      open={!!violation}
      onCancel={handleClose}
      width={640}
      destroyOnClose
      footer={
        showOverride
          ? null
          : [
              <Button key="cancel" onClick={handleClose} data-testid="violation-cancel">
                취소
              </Button>,
              <Button
                key="override"
                type="primary"
                danger
                onClick={() => setShowOverride(true)}
                data-testid="violation-override-trigger"
              >
                강제 배치 (사유 입력)
              </Button>,
            ]
      }
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Alert
          type="error"
          showIcon
          message={violation.reason}
          data-testid="violation-reason"
        />
        <div>
          <Title level={5} style={{ marginBottom: 8 }}>
            대안 슬롯 ({violation.alternatives.length}개)
          </Title>
          {violation.alternatives.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="대안 없음 — BR-V11 Unschedulable 품번 (외주·재고 권고)"
            />
          ) : (
            <Space wrap data-testid="violation-alternatives">
              {violation.alternatives.map((s) => (
                <Tag key={s} color="green">
                  {slotDisplayName(s)}
                </Tag>
              ))}
            </Space>
          )}
        </div>
        {showOverride && (
          <>
            <Divider />
            <OverrideJustificationForm
              violation={violation}
              onSuccess={() => {
                onOverride(violation)
                handleClose()
              }}
              onCancel={() => setShowOverride(false)}
            />
          </>
        )}
      </Space>
    </Modal>
  )
}
