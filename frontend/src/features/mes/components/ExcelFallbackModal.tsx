import { useState } from 'react'
import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
} from 'antd'
import dayjs from 'dayjs'
import { HttpError } from '@/api/client'
import { postShiftFallback } from '../api/mesApi'

interface Props {
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

const MACHINES = ['LP-01', 'LP-02', 'LP-03', 'LP-04', 'IC-01']

interface FormValues {
  machineId: string
  shiftDate: dayjs.Dayjs
  shiftNo: number
  plannedQty: number
  actualQty?: number
  note?: string
}

/**
 * Sprint 17 EP-DAY-LOCK BR-X06 — Excel 폴백 수동 입력 Modal (TK-DAY-LOCK-4-2).
 *
 * <p>PLANNER/IT_OPS 가 MES 미수신 shift 의 실적을 수동 입력. 입력 성공 시 backend 가
 * mes_shift_event INSERT (source=EXCEL_FALLBACK, reported_by=Principal) → DegradedModeService
 * 가 다음 polling 에서 degraded 해제 감지.
 */
export function ExcelFallbackModal({ open, onClose, onSuccess }: Props) {
  const [form] = Form.useForm<FormValues>()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async () => {
    setError(null)
    let values: FormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    setLoading(true)
    try {
      await postShiftFallback({
        machineId: values.machineId,
        shiftDate: values.shiftDate.format('YYYY-MM-DD'),
        shiftNo: values.shiftNo,
        plannedQty: values.plannedQty,
        actualQty: values.actualQty ?? null,
        note: values.note ?? null,
      })
      onSuccess()
      handleClose()
    } catch (e) {
      if (e instanceof HttpError) {
        const body = (e.body ?? {}) as { detail?: string }
        setError(`HTTP ${e.status}: ${body.detail ?? e.message}`)
      } else {
        setError(e instanceof Error ? e.message : String(e))
      }
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    setError(null)
    form.resetFields()
    onClose()
  }

  return (
    <Modal
      title="MES Excel 폴백 입력 (BR-X06)"
      open={open}
      onCancel={handleClose}
      width={520}
      destroyOnClose
      footer={[
        <Button key="cancel" onClick={handleClose} disabled={loading} data-testid="fallback-cancel">
          취소
        </Button>,
        <Button
          key="ok"
          type="primary"
          loading={loading}
          onClick={handleSubmit}
          data-testid="fallback-submit"
        >
          입력
        </Button>,
      ]}
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="MES 미수신 shift 실적 수동 입력"
          description="MES 자동 수신이 1 shift 이상 지연 시 PLANNER/IT_OPS 가 수동 입력 (REQ-FUNC-CO-004)."
        />
        <Form form={form} layout="vertical" initialValues={{ shiftDate: dayjs(), shiftNo: 1, plannedQty: 0 }}>
          <Form.Item
            name="machineId"
            label="가류기"
            rules={[{ required: true, message: '가류기 선택 필수' }]}
          >
            <Select options={MACHINES.map((m) => ({ value: m, label: m }))} />
          </Form.Item>
          <Form.Item
            name="shiftDate"
            label="Shift 일자"
            rules={[{ required: true, message: 'shift_date 필수' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="shiftNo"
            label="Shift 번호 (1=주간 전반, 2=주간 후반, 3=야간 전반, 4=야간 후반)"
            rules={[{ required: true, message: 'shift_no 1~4' }]}
          >
            <Select
              options={[
                { value: 1, label: '1 — 주간 전반' },
                { value: 2, label: '2 — 주간 후반' },
                { value: 3, label: '3 — 야간 전반' },
                { value: 4, label: '4 — 야간 후반' },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="plannedQty"
            label="계획 수량 (planned_qty)"
            rules={[{ required: true, message: '계획 수량 필수' }]}
          >
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="actualQty" label="실 수량 (actual_qty, 선택)">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="note" label="비고 (note)">
            <Input.TextArea rows={2} maxLength={500} />
          </Form.Item>
        </Form>
        {error && <Alert type="error" showIcon message="입력 실패" description={error} data-testid="fallback-error" />}
      </Space>
    </Modal>
  )
}
