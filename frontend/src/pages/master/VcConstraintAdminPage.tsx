import { useEffect, useState } from 'react'
import {
  Button,
  Checkbox,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Table,
  Typography,
  message,
} from 'antd'
import {
  vcConstraintApi,
  COMPOSITE_COUNT_OPTIONS,
  type CompositeCount,
  type VcConstraintSummary,
  type VcConstraintPayload,
} from '@/api/vcConstraintApi'
import { HttpError } from '@/api/client'

const { Title, Paragraph } = Typography

const SLOT_KEYS = ['slot1', 'slot2', 'slot3', 'slot4', 'slot5', 'slot6', 'slot7'] as const
type SlotKey = (typeof SLOT_KEYS)[number]

interface FormValues {
  hoseId: string
  compositeCount: CompositeCount
  lpMoldQty: number
  icMoldQty: number
  slot1: boolean
  slot2: boolean
  slot3: boolean
  slot4: boolean
  slot5: boolean
  slot6: boolean
  slot7: boolean
}

const buildPayload = (v: FormValues): VcConstraintPayload => ({
  hoseId: v.hoseId,
  compositeCount: v.compositeCount,
  lpMoldQty: v.lpMoldQty,
  icMoldQty: v.icMoldQty,
  slot1: v.slot1 ?? false,
  slot2: v.slot2 ?? false,
  slot3: v.slot3 ?? false,
  slot4: v.slot4 ?? false,
  slot5: v.slot5 ?? false,
  slot6: v.slot6 ?? false,
  slot7: v.slot7 ?? false,
})

/**
 * Sprint 21 ST-CRUD-3 — VC 제약 마스터 관리 (IT_OPS write).
 *
 * <p>compositeCount IN (1,2,3,6) — BR-V14 합금형 제약.
 * <p>slotEligibility 7개 boolean — 저압가류기 slot 가용성.
 */
export default function VcConstraintAdminPage() {
  const [rows, setRows] = useState<VcConstraintSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editing, setEditing] = useState<VcConstraintSummary | null>(null)
  const [searchText, setSearchText] = useState('')
  const [form] = Form.useForm<FormValues>()

  const reload = async () => {
    setLoading(true)
    try {
      setRows(await vcConstraintApi.list())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  const openEdit = (row: VcConstraintSummary) => {
    setEditing(row)
    form.setFieldsValue({
      hoseId: row.hoseId,
      compositeCount: row.compositeCount,
      lpMoldQty: row.lpMoldQty,
      icMoldQty: row.icMoldQty,
      slot1: row.slot1,
      slot2: row.slot2,
      slot3: row.slot3,
      slot4: row.slot4,
      slot5: row.slot5,
      slot6: row.slot6,
      slot7: row.slot7,
    })
    setDrawerOpen(true)
  }

  const handleSubmit = async (v: FormValues) => {
    const payload = buildPayload(v)
    try {
      if (editing) {
        await vcConstraintApi.update(editing.hoseId, payload)
        message.success(`수정 완료 — ${v.hoseId}`)
      } else {
        await vcConstraintApi.create(payload)
        message.success(`추가 완료 — ${v.hoseId}`)
      }
      setDrawerOpen(false)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        message.error('IT_OPS 권한 필요 — 수정 권한이 없습니다.')
        return
      }
      if (e instanceof HttpError && e.status === 400) {
        const body = e.body as { detail?: string; brCode?: string } | null
        const detail = body?.detail ?? 'BR-V14 위반 — compositeCount 는 1·2·3·6 만 허용됩니다.'
        message.error(detail)
        return
      }
      message.error(editing ? '수정 실패' : '추가 실패')
    }
  }

  const filteredRows = rows.filter((r) =>
    r.hoseId.toLowerCase().includes(searchText.toLowerCase()),
  )

  const slotSummary = (row: VcConstraintSummary) =>
    SLOT_KEYS.filter((k) => row[k])
      .map((k) => k.replace('slot', ''))
      .join(', ') || '없음'

  const columns = [
    {
      title: 'Hose ID',
      dataIndex: 'hoseId',
      key: 'hoseId',
      sorter: (a: VcConstraintSummary, b: VcConstraintSummary) =>
        a.hoseId.localeCompare(b.hoseId),
    },
    {
      title: '합금형 수 (BR-V14)',
      dataIndex: 'compositeCount',
      key: 'compositeCount',
      width: 140,
      filters: COMPOSITE_COUNT_OPTIONS.map((v) => ({ text: String(v), value: v })),
      onFilter: (value: unknown, record: VcConstraintSummary) =>
        record.compositeCount === value,
    },
    { title: 'LP 금형 수량', dataIndex: 'lpMoldQty', key: 'lpMoldQty', width: 110 },
    { title: 'IC 금형 수량', dataIndex: 'icMoldQty', key: 'icMoldQty', width: 110 },
    {
      title: '가용 Slot',
      key: 'slots',
      render: (_: unknown, r: VcConstraintSummary) => slotSummary(r),
    },
    {
      title: '액션',
      key: 'actions',
      width: 90,
      render: (_: unknown, r: VcConstraintSummary) => (
        <Button size="small" onClick={() => openEdit(r)}>
          수정
        </Button>
      ),
    },
  ]

  return (
    <div>
      <Title level={3}>VC 제약 마스터 (BR-V14)</Title>
      <Paragraph type="secondary">
        compositeCount IN (1·2·3·6) 합금형 제약 + 저압가류기 slot 가용성 관리. IT_OPS 전용 수정.
      </Paragraph>

      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="Hose ID 검색"
          allowClear
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: 240 }}
        />
        <Button onClick={() => void reload()}>새로고침</Button>
      </Space>

      <Table
        rowKey="hoseId"
        loading={loading}
        dataSource={filteredRows}
        columns={columns}
        pagination={{ pageSize: 50, showTotal: (total) => `총 ${total}건` }}
        size="small"
      />

      <Drawer
        title={editing ? `VC 제약 수정 — ${editing.hoseId}` : 'VC 제약 추가'}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={480}
        destroyOnClose
        footer={null}
      >
        <Form<FormValues> form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="Hose ID" name="hoseId" rules={[{ required: true }]}>
            <Input disabled={!!editing} maxLength={40} />
          </Form.Item>

          <Form.Item
            label="합금형 수 (compositeCount) — BR-V14"
            name="compositeCount"
            rules={[{ required: true, message: 'compositeCount 를 선택하세요' }]}
          >
            <Select
              options={COMPOSITE_COUNT_OPTIONS.map((v) => ({
                label: String(v),
                value: v,
              }))}
              placeholder="1 / 2 / 3 / 6 중 선택"
            />
          </Form.Item>

          <Form.Item
            label="LP 금형 수량"
            name="lpMoldQty"
            rules={[{ required: true }, { type: 'number', min: 0 }]}
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="IC 금형 수량"
            name="icMoldQty"
            rules={[{ required: true }, { type: 'number', min: 0 }]}
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item label="Slot 가용성 (1~7)">
            <Space wrap>
              {SLOT_KEYS.map((key) => (
                <Form.Item
                  key={key}
                  name={key as SlotKey}
                  valuePropName="checked"
                  noStyle
                >
                  <Checkbox>Slot {key.replace('slot', '')}</Checkbox>
                </Form.Item>
              ))}
            </Space>
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              data-testid="vc-constraint-submit"
            >
              {editing ? '수정' : '추가'}
            </Button>
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  )
}
