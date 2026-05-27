import { useEffect, useState } from 'react'
import { Button, DatePicker, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import dayjs, { Dayjs } from 'dayjs'
import { kdOrderApi, type KdOrderSummary, type KdOrderPayload, type KdStatus } from '@/api/kdOrderApi'

const { Title, Paragraph } = Typography

const STATUS_OPTIONS: { value: KdStatus; label: string; color: string }[] = [
  { value: 'OPEN', label: 'OPEN (미소진)', color: 'blue' },
  { value: 'PARTIAL', label: 'PARTIAL (일부 소진)', color: 'orange' },
  { value: 'FILLED', label: 'FILLED (전량 소진)', color: 'green' },
  { value: 'CANCELLED', label: 'CANCELLED (취소)', color: 'default' },
]

interface FormValues {
  hoseId: string
  orderQty: number
  remainingQty: number
  orderDate: Dayjs
  customerCode?: string
  status: KdStatus
}

/**
 * Sprint 12 EP-MASTER-UI BR-V13 KD_ORDER (TK-MASTER-4-3, IT_OPS write).
 *
 * <p>변경 후 supplement() 가 next call 시 즉시 새 remaining_qty 반영.
 */
export default function KdOrderPage() {
  const [rows, setRows] = useState<KdOrderSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<KdOrderSummary | null>(null)
  const [form] = Form.useForm<FormValues>()

  const reload = async () => {
    setLoading(true)
    try { setRows(await kdOrderApi.list()) }
    finally { setLoading(false) }
  }
  useEffect(() => { void reload() }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ orderDate: dayjs(), status: 'OPEN' })
    setModalOpen(true)
  }
  const openEdit = (row: KdOrderSummary) => {
    setEditing(row)
    form.setFieldsValue({
      hoseId: row.hoseId, orderQty: row.orderQty, remainingQty: row.remainingQty,
      orderDate: dayjs(row.orderDate), customerCode: row.customerCode ?? undefined, status: row.status,
    })
    setModalOpen(true)
  }

  const handleSubmit = async (v: FormValues) => {
    const payload: KdOrderPayload = {
      hoseId: v.hoseId, orderQty: v.orderQty, remainingQty: v.remainingQty,
      orderDate: v.orderDate.format('YYYY-MM-DD'),
      customerCode: v.customerCode, status: v.status,
    }
    try {
      if (editing) {
        await kdOrderApi.update(editing.kdOrderId, payload)
        message.success(`수정 완료 — ${v.hoseId} (remaining ${v.remainingQty})`)
      } else {
        await kdOrderApi.create(payload)
        message.success(`추가 완료 — ${v.hoseId} (qty ${v.orderQty})`)
      }
      setModalOpen(false)
      await reload()
    } catch {
      message.error(editing ? '수정 실패' : '추가 실패')
    }
  }

  const handleDelete = (id: string, hoseId: string) =>
    Modal.confirm({
      title: `KD 발주 삭제 — ${hoseId}`,
      content: '삭제 후 supplement() 가 본 발주 무시 (audit_log 기록).',
      okText: '삭제', okType: 'danger', cancelText: '취소',
      onOk: async () => {
        try { await kdOrderApi.delete(id); message.success('삭제 완료'); await reload() }
        catch { message.error('삭제 실패') }
      },
    })

  const columns = [
    { title: 'Hose ID', dataIndex: 'hoseId', key: 'hoseId' },
    { title: '발주 수량', dataIndex: 'orderQty', key: 'orderQty' },
    {
      title: '잔량', dataIndex: 'remainingQty', key: 'remainingQty',
      render: (n: number) => <Tag color={n === 0 ? 'green' : n < 100 ? 'orange' : 'blue'}>{n}</Tag>,
    },
    { title: '발주일', dataIndex: 'orderDate', key: 'orderDate' },
    { title: '거래처', dataIndex: 'customerCode', key: 'customerCode', render: (v: string | null) => v ?? '—' },
    {
      title: '상태', dataIndex: 'status', key: 'status',
      render: (s: KdStatus) => {
        const opt = STATUS_OPTIONS.find((o) => o.value === s)
        return <Tag color={opt?.color}>{s}</Tag>
      },
    },
    { title: '수정자', dataIndex: 'updatedBy', key: 'updatedBy' },
    {
      title: '액션', key: 'actions',
      render: (_: unknown, r: KdOrderSummary) => (
        <Space>
          <Button size="small" onClick={() => openEdit(r)}>수정</Button>
          <Button size="small" danger onClick={() => handleDelete(r.kdOrderId, r.hoseId)}>삭제</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Title level={3}>KD_ORDER (BR-V13)</Title>
      <Paragraph type="secondary">
        capacity supplement() 가 본 표 remaining_qty 참조. IT_OPS 만 변경.
      </Paragraph>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={openCreate}>신규 추가</Button>
        <Button onClick={() => void reload()}>새로고침</Button>
      </Space>
      <Table rowKey="kdOrderId" loading={loading} dataSource={rows} columns={columns} pagination={{ pageSize: 20 }} />

      <Modal
        title={editing ? `KD 발주 수정 — ${editing.hoseId}` : '신규 KD 발주 추가'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="Hose ID" name="hoseId" rules={[{ required: true }]}>
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item label="발주 수량" name="orderQty" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="잔량 (0 ≤ remaining ≤ orderQty)" name="remainingQty" rules={[{ required: true }]}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="발주일" name="orderDate" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="거래처 코드 (선택)" name="customerCode">
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item label="상태" name="status" rules={[{ required: true }]}>
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>{editing ? '수정' : '추가'}</Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
