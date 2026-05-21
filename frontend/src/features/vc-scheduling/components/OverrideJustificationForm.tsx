import { Alert, Button, Form, Input, message, Space } from 'antd'
import { useOverrideAudit } from '../hooks/useOverrideAudit'
import type { ViolationInfo } from '../types/violation'

interface Props {
  violation: ViolationInfo
  onSuccess: () => void
  onCancel: () => void
}

interface FormValues {
  justification: string
}

/**
 * 강제 배치 사유 입력 폼 — TK-04-3-2 (REQ-FUNC-CO-010).
 *
 * <p>10자 이상 사유 강제 (단순 "ok" 차단). POST /api/v1/audit/override 호출 후
 * 성공 시 onSuccess (강제 배치 진행).
 */
export function OverrideJustificationForm({ violation, onSuccess, onCancel }: Props) {
  const [form] = Form.useForm<FormValues>()
  const mutation = useOverrideAudit()

  const handleSubmit = async () => {
    const values = await form.validateFields()
    mutation.mutate(
      {
        hoseId: violation.hoseId,
        slotPosition: violation.slot,
        justification: values.justification,
      },
      {
        onSuccess: () => {
          message.success('Override audit 기록 완료 — 강제 배치 진행')
          onSuccess()
        },
        onError: (e) => {
          message.error(`Audit 기록 실패: ${e.message}`)
        },
      },
    )
  }

  return (
    <Form<FormValues> form={form} layout="vertical" data-testid="override-form">
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Alert
          type="warning"
          showIcon
          message="강제 배치는 모든 사용자에게 audit 기록됩니다 (REQ-FUNC-CO-010)"
        />
        <Form.Item
          name="justification"
          label="강제 배치 사유 (10자 이상 필수)"
          rules={[
            { required: true, message: '사유 입력 필수' },
            { min: 10, message: '최소 10자 이상 — 진정성 확보' },
            { max: 500, message: '500자 이하' },
          ]}
        >
          <Input.TextArea
            rows={4}
            placeholder="예: 긴급 납기 대응, 현장 협의 완료 (이수진 반장 승인)"
            aria-label="강제 배치 사유"
          />
        </Form.Item>
        <Space>
          <Button onClick={onCancel}>취소</Button>
          <Button
            type="primary"
            danger
            loading={mutation.isPending}
            onClick={handleSubmit}
          >
            강제 배치 + audit 기록
          </Button>
        </Space>
      </Space>
    </Form>
  )
}
