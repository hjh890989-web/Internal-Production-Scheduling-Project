import { useState } from 'react'
import { Alert, Form, Input, Modal, Typography } from 'antd'

const { Paragraph } = Typography

interface PinForceChangeModalProps {
  open: boolean
  /** 새 PIN 제출 — 성공 시 resolve, 실패 시 reject(메시지). */
  onSubmit: (newPin: string) => Promise<void>
}

interface FormValues {
  newPin: string
  confirmPin: string
}

/**
 * Sprint 22 ST-SEC-2 — PIN 30일 만료 강제 변경 모달 (NFR-SEC-007).
 *
 * <p>로그인 응답 pinExpired=true 시 노출. 취소 불가 (closable/maskClosable/keyboard 모두 비활성,
 * 취소 버튼 없음) — 새 PIN 변경 완료해야만 진입. 새 PIN 4자리 + 확인 일치 검증.
 */
export default function PinForceChangeModal({ open, onSubmit }: PinForceChangeModalProps) {
  const [form] = Form.useForm<FormValues>()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleOk = async () => {
    setError(null)
    let values: FormValues
    try {
      values = await form.validateFields()
    } catch {
      return // 검증 실패 — 필드 메시지 표시
    }
    setSubmitting(true)
    try {
      await onSubmit(values.newPin)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'PIN 변경 실패 — 다시 시도해 주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open={open}
      title="PIN 변경 필요"
      okText="PIN 변경"
      onOk={handleOk}
      confirmLoading={submitting}
      closable={false}
      maskClosable={false}
      keyboard={false}
      cancelButtonProps={{ style: { display: 'none' } }}
    >
      <Paragraph type="secondary">
        보안 정책(NFR-SEC-007)에 따라 PIN 을 30일마다 변경해야 합니다. 새 PIN 을 설정해 주세요.
      </Paragraph>

      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}

      <Form<FormValues> form={form} layout="vertical" autoComplete="off">
        <Form.Item
          label="새 PIN (4자리 숫자)"
          name="newPin"
          rules={[
            { required: true, message: '새 PIN 입력 필수' },
            { pattern: /^[0-9]{4}$/, message: 'PIN 4자리 숫자' },
          ]}
        >
          <Input.Password size="large" maxLength={4} inputMode="numeric" placeholder="••••" autoFocus />
        </Form.Item>

        <Form.Item
          label="새 PIN 확인"
          name="confirmPin"
          dependencies={['newPin']}
          rules={[
            { required: true, message: '새 PIN 확인 입력 필수' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPin') === value) return Promise.resolve()
                return Promise.reject(new Error('새 PIN 이 일치하지 않습니다.'))
              },
            }),
          ]}
        >
          <Input.Password size="large" maxLength={4} inputMode="numeric" placeholder="••••" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
