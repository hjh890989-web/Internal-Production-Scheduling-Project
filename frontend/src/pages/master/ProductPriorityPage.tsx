import { useEffect, useState } from 'react'
import { Button, DatePicker, Form, Input, InputNumber, Modal, Space, Table, Typography, message } from 'antd'
import dayjs, { Dayjs } from 'dayjs'
import { productPriorityApi, type PrioritySummary, type PriorityPayload } from '@/api/productPriorityApi'
import { HttpError } from '@/api/client'

const { Title, Paragraph } = Typography

interface FormValues {
  hoseId: string
  priorityRank: number
  rationale?: string
  effectiveFrom: Dayjs
  effectiveTo?: Dayjs
}

/**
 * Sprint 12 EP-MASTER-UI BR-V12 PRODUCT_PRIORITY (TK-MASTER-3-3, IT_OPS write).
 *
 * <p>변경 후 capacity-overflow split() 가 next call 시 즉시 새 priority 반영.
 */
export default function ProductPriorityPage() {
  const [rows, setRows] = useState<PrioritySummary[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<PrioritySummary | null>(null)
  const [form] = Form.useForm<FormValues>()

  const reload = async () => {
    setLoading(true)
    try { setRows(await productPriorityApi.list()) }
    finally { setLoading(false) }
  }
  useEffect(() => { void reload() }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldValue('effectiveFrom', dayjs())
    setModalOpen(true)
  }
  const openEdit = (row: PrioritySummary) => {
    setEditing(row)
    form.setFieldsValue({
      hoseId: row.hoseId,
      priorityRank: row.priorityRank,
      rationale: row.rationale ?? undefined,
      effectiveFrom: dayjs(row.effectiveFrom),
      effectiveTo: row.effectiveTo ? dayjs(row.effectiveTo) : undefined,
    })
    setModalOpen(true)
  }

  const handleSubmit = async (v: FormValues) => {
    const payload: PriorityPayload = {
      hoseId: v.hoseId,
      priorityRank: v.priorityRank,
      rationale: v.rationale,
      effectiveFrom: v.effectiveFrom.format('YYYY-MM-DD'),
      effectiveTo: v.effectiveTo?.format('YYYY-MM-DD'),
    }
    try {
      if (editing) {
        await productPriorityApi.update(editing.hoseId, payload)
        message.success(`수정 완료 — ${v.hoseId} (rank ${v.priorityRank})`)
      } else {
        await productPriorityApi.create(payload)
        message.success(`추가 완료 — ${v.hoseId} (rank ${v.priorityRank})`)
      }
      setModalOpen(false)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 409) {
        message.error(`Hose ID 중복 — ${v.hoseId} 는 이미 등록됨. 기존 항목을 수정하세요.`)
      } else if (e instanceof HttpError && e.status === 404) {
        message.error('대상 Hose ID 미존재')
      } else {
        message.error(editing ? '수정 실패' : '추가 실패')
      }
    }
  }

  const handleDelete = (hoseId: string) =>
    Modal.confirm({
      title: `우선순위 삭제 — ${hoseId}`,
      content: '삭제 후 capacity-overflow 가 fallback rank 99 사용 (audit_log 기록).',
      okText: '삭제', okType: 'danger', cancelText: '취소',
      onOk: async () => {
        try { await productPriorityApi.delete(hoseId); message.success('삭제 완료'); await reload() }
        catch { message.error('삭제 실패') }
      },
    })

  const columns = [
    { title: 'Hose ID', dataIndex: 'hoseId', key: 'hoseId' },
    { title: 'Rank', dataIndex: 'priorityRank', key: 'priorityRank', sorter: (a: PrioritySummary, b: PrioritySummary) => a.priorityRank - b.priorityRank },
    { title: '사유', dataIndex: 'rationale', key: 'rationale', ellipsis: true },
    { title: '유효 시작', dataIndex: 'effectiveFrom', key: 'effectiveFrom' },
    { title: '유효 종료', dataIndex: 'effectiveTo', key: 'effectiveTo', render: (v: string | null) => v ?? '—' },
    { title: '수정자', dataIndex: 'updatedBy', key: 'updatedBy' },
    {
      title: '액션', key: 'actions',
      render: (_: unknown, r: PrioritySummary) => (
        <Space>
          <Button size="small" onClick={() => openEdit(r)}>수정</Button>
          <Button size="small" danger onClick={() => handleDelete(r.hoseId)}>삭제</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Title level={3}>PRODUCT_PRIORITY (BR-V12)</Title>
      <Paragraph type="secondary">
        capacity-overflow split() 가 본 표 priority_rank ASC 정렬로 자동 채택. IT_OPS 만 변경.
      </Paragraph>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={openCreate}>신규 추가</Button>
        <Button onClick={() => void reload()}>새로고침</Button>
      </Space>
      <Table rowKey="hoseId" loading={loading} dataSource={rows} columns={columns} pagination={{ pageSize: 20 }} />

      <Modal
        title={editing ? `우선순위 수정 — ${editing.hoseId}` : '신규 우선순위 추가'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Form<FormValues> form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="Hose ID" name="hoseId" rules={[{ required: true }]}>
            <Input disabled={!!editing} maxLength={40} />
          </Form.Item>
          <Form.Item label="Rank (1~99, 낮을수록 우선)" name="priorityRank" rules={[{ required: true }]}>
            <InputNumber min={1} max={99} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="사유 (선택)" name="rationale">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="유효 시작" name="effectiveFrom" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="유효 종료 (선택)" name="effectiveTo">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>{editing ? '수정' : '추가'}</Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
