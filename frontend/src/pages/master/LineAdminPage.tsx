import { useEffect, useState } from 'react'
import {
  Button, Drawer, Form, Input, message, Popconfirm, Select,
  Space, Switch, Table, Tabs, Tag, Transfer, Typography,
} from 'antd'
import type { TransferProps } from 'antd'
import { lineApi, type LineSummary, type LineCreatePayload, type LineType } from '@/api/lineApi'
import { productSpecApi } from '@/api/productSpecApi'
import { HttpError } from '@/api/client'

const { Title, Paragraph } = Typography

interface CreateFormValues {
  lineCode: string
  lineName: string
  lineType: LineType
}

interface EditFormValues {
  lineName: string
}

const LINE_TYPE_OPTIONS: { label: string; value: LineType }[] = [
  { label: 'LP (저압가류)', value: 'LP' },
  { label: 'IC (IC가류)', value: 'IC' },
  { label: 'EX (압출)', value: 'EX' },
]

const LINE_TYPE_COLOR: Record<LineType, string> = {
  LP: 'blue',
  IC: 'green',
  EX: 'orange',
}

/**
 * Sprint 21 ST-CRUD-4 — Line master CRUD + product compatibility mapping.
 * IT_OPS only write. lineCode PK (immutable after create).
 */
export default function LineAdminPage() {
  const [rows, setRows] = useState<LineSummary[]>([])
  const [allHoseIds, setAllHoseIds] = useState<string[]>([])
  const [loading, setLoading] = useState(false)

  // Create modal state
  const [createOpen, setCreateOpen] = useState(false)
  const [createForm] = Form.useForm<CreateFormValues>()

  // Edit drawer state
  const [editTarget, setEditTarget] = useState<LineSummary | null>(null)
  const [editForm] = Form.useForm<EditFormValues>()

  // Products drawer state
  const [productsTarget, setProductsTarget] = useState<LineSummary | null>(null)
  const [selectedHoseIds, setSelectedHoseIds] = useState<string[]>([])
  const [productsSaving, setProductsSaving] = useState(false)

  const reload = async () => {
    setLoading(true)
    try {
      const [lines, specs] = await Promise.all([
        lineApi.list(),
        productSpecApi.list(),
      ])
      setRows(lines)
      setAllHoseIds(specs.map(s => s.hoseId))
    } catch {
      message.error('데이터 로드 실패')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void reload() }, [])

  // Create handlers
  const handleOpenCreate = () => {
    createForm.resetFields()
    setCreateOpen(true)
  }

  const handleCreate = async (v: CreateFormValues) => {
    const payload: LineCreatePayload = {
      lineCode: v.lineCode.trim(),
      lineName: v.lineName.trim(),
      lineType: v.lineType,
    }
    try {
      await lineApi.create(payload)
      message.success(`라인 추가 완료 — ${payload.lineCode}`)
      setCreateOpen(false)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        message.error('IT_OPS 권한 필요')
      } else if (e instanceof HttpError && e.status === 409) {
        message.error(`lineCode 중복 — ${payload.lineCode} 는 이미 등록됨`)
      } else {
        message.error('추가 실패')
      }
    }
  }

  // Edit handlers
  const handleOpenEdit = (row: LineSummary) => {
    setEditTarget(row)
    editForm.setFieldsValue({ lineName: row.lineName })
  }

  const handleEdit = async (v: EditFormValues) => {
    if (!editTarget) return
    try {
      await lineApi.update(editTarget.lineCode, { lineName: v.lineName.trim() })
      message.success(`수정 완료 — ${editTarget.lineCode}`)
      setEditTarget(null)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        message.error('IT_OPS 권한 필요')
      } else {
        message.error('수정 실패')
      }
    }
  }

  // Active toggle
  const handleToggleActive = async (row: LineSummary) => {
    try {
      await lineApi.remove(row.lineCode)
      message.success(`${row.lineCode} — ${row.active ? '비활성' : '활성'} 전환`)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        message.error('IT_OPS 권한 필요')
      } else {
        message.error('상태 변경 실패')
      }
    }
  }

  // Products handlers
  const handleOpenProducts = (row: LineSummary) => {
    setProductsTarget(row)
    setSelectedHoseIds(row.productCompatibility)
  }

  const handleSaveProducts = async () => {
    if (!productsTarget) return
    setProductsSaving(true)
    try {
      await lineApi.updateProducts(productsTarget.lineCode, selectedHoseIds)
      message.success(`호환 매핑 저장 — ${productsTarget.lineCode} (${selectedHoseIds.length}건)`)
      setProductsTarget(null)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        message.error('IT_OPS 권한 필요')
      } else {
        message.error('호환 매핑 저장 실패')
      }
    } finally {
      setProductsSaving(false)
    }
  }

  const transferDataSource: TransferProps['dataSource'] = allHoseIds.map(id => ({
    key: id,
    title: id,
  }))

  const handleTransferChange: TransferProps['onChange'] = (targetKeys) => {
    setSelectedHoseIds(targetKeys as string[])
  }

  const columns = [
    { title: 'Line Code', dataIndex: 'lineCode', key: 'lineCode', width: 120 },
    { title: '라인명', dataIndex: 'lineName', key: 'lineName' },
    {
      title: 'Type',
      dataIndex: 'lineType',
      key: 'lineType',
      width: 100,
      render: (t: LineType) => <Tag color={LINE_TYPE_COLOR[t]}>{t}</Tag>,
    },
    {
      title: '활성',
      dataIndex: 'active',
      key: 'active',
      width: 80,
      render: (active: boolean, row: LineSummary) => (
        <Popconfirm
          title={`${row.lineCode} — ${active ? '비활성' : '활성'} 전환?`}
          okText="확인"
          cancelText="취소"
          onConfirm={() => handleToggleActive(row)}
        >
          <Switch checked={active} size="small" />
        </Popconfirm>
      ),
    },
    {
      title: '호환 품번 수',
      key: 'compatCount',
      width: 110,
      render: (_: unknown, row: LineSummary) => (
        <span>{row.productCompatibility.length}개</span>
      ),
    },
    {
      title: '액션',
      key: 'actions',
      width: 160,
      render: (_: unknown, row: LineSummary) => (
        <Space>
          <Button size="small" onClick={() => handleOpenEdit(row)}>수정</Button>
          <Button size="small" onClick={() => handleOpenProducts(row)}>호환 설정</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Title level={3}>라인 관리 (LINE MASTER)</Title>
      <Paragraph type="secondary">
        저압가류(LP) · IC가류(IC) · 압출(EX) 라인 기본 정보 및 품번 호환 매핑. IT_OPS 만 변경.
      </Paragraph>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={handleOpenCreate}>신규 추가</Button>
        <Button onClick={() => void reload()}>새로고침</Button>
      </Space>
      <Table
        rowKey="lineCode"
        loading={loading}
        dataSource={rows}
        columns={columns}
        pagination={{ pageSize: 20 }}
      />

      {/* Create Modal */}
      <Form.Provider>
        {createOpen && (
          <div
            style={{
              position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)',
              zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
            role="dialog"
            aria-modal="true"
            aria-label="신규 라인 추가"
          >
            <div style={{ background: '#fff', borderRadius: 8, padding: 24, width: 480 }}>
              <Title level={5} style={{ marginTop: 0 }}>신규 라인 추가</Title>
              <Form<CreateFormValues> form={createForm} layout="vertical" onFinish={handleCreate}>
                <Form.Item
                  label="Line Code"
                  name="lineCode"
                  rules={[{ required: true, message: 'lineCode 필수' }, { max: 20 }]}
                >
                  <Input placeholder="예: LP-01" maxLength={20} />
                </Form.Item>
                <Form.Item
                  label="라인명"
                  name="lineName"
                  rules={[{ required: true, message: '라인명 필수' }, { max: 60 }]}
                >
                  <Input placeholder="예: 저압가류기 1호" maxLength={60} />
                </Form.Item>
                <Form.Item
                  label="라인 타입"
                  name="lineType"
                  rules={[{ required: true, message: '타입 선택 필수' }]}
                >
                  <Select options={LINE_TYPE_OPTIONS} placeholder="타입 선택" />
                </Form.Item>
                <Space style={{ justifyContent: 'flex-end', width: '100%' }}>
                  <Button onClick={() => setCreateOpen(false)}>취소</Button>
                  <Button type="primary" htmlType="submit">추가</Button>
                </Space>
              </Form>
            </div>
          </div>
        )}
      </Form.Provider>

      {/* Edit Drawer */}
      <Drawer
        title={editTarget ? `라인 수정 — ${editTarget.lineCode}` : ''}
        open={!!editTarget}
        onClose={() => setEditTarget(null)}
        width={400}
        destroyOnClose
      >
        {editTarget && (
          <Form<EditFormValues> form={editForm} layout="vertical" onFinish={handleEdit}>
            <Form.Item label="Line Code">
              <Input value={editTarget.lineCode} disabled aria-label="lineCode (수정 불가)" />
            </Form.Item>
            <Form.Item label="라인 타입">
              <Input value={editTarget.lineType} disabled />
            </Form.Item>
            <Form.Item
              label="라인명"
              name="lineName"
              rules={[{ required: true, message: '라인명 필수' }, { max: 60 }]}
            >
              <Input maxLength={60} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" block>저장</Button>
            </Form.Item>
          </Form>
        )}
      </Drawer>

      {/* Products Drawer */}
      <Drawer
        title={productsTarget ? `호환 품번 설정 — ${productsTarget.lineCode}` : ''}
        open={!!productsTarget}
        onClose={() => setProductsTarget(null)}
        width={640}
        destroyOnClose
        extra={
          <Button
            type="primary"
            loading={productsSaving}
            onClick={handleSaveProducts}
            data-testid="save-products-btn"
          >
            저장
          </Button>
        }
      >
        {productsTarget && (
          <Tabs
            items={[
              {
                key: 'transfer',
                label: '호환 품번 선택',
                children: (
                  <Transfer
                    dataSource={transferDataSource}
                    titles={['미선택', '선택됨']}
                    targetKeys={selectedHoseIds}
                    onChange={handleTransferChange}
                    render={item => item.title ?? item.key}
                    showSearch
                    filterOption={(input, item) =>
                      (item.title ?? item.key).toLowerCase().includes(input.toLowerCase())
                    }
                    listStyle={{ width: 240, height: 400 }}
                    locale={{ itemUnit: '건', itemsUnit: '건' }}
                  />
                ),
              },
            ]}
          />
        )}
      </Drawer>
    </div>
  )
}
