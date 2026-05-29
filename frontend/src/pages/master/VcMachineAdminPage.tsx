import { useEffect, useState } from 'react'
import {
  Button,
  Form,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { vcMachineApi, type VcMachineSummary, type VcMachineUpdatePayload } from '@/api/vcMachineApi'
import { HttpError } from '@/api/client'

const { Title, Paragraph, Text } = Typography

interface EditFormValues {
  dayRotations: number
  nightRotations: number
  active: boolean
}

/**
 * Sprint 21 ST-CRUD-1 VcMachine 관리 Page (IT_OPS write — BR-X02 audit).
 *
 * <p>LP-01~04 / IC-01 5대 목록 표시. active toggle + dayRotations / nightRotations 수정.
 * machineType · totalSlots(LP=8 고정) 은 비활성화 표시.
 */
export default function VcMachineAdminPage() {
  const [rows, setRows] = useState<VcMachineSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<VcMachineSummary | null>(null)
  const [form] = Form.useForm<EditFormValues>()

  const reload = async () => {
    setLoading(true)
    try {
      setRows(await vcMachineApi.list())
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        void message.error('IT_OPS 권한 필요')
      } else {
        void message.error('목록 조회 실패')
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void reload() }, [])

  const openEdit = (row: VcMachineSummary) => {
    setEditing(row)
    form.setFieldsValue({
      dayRotations: row.dayRotations,
      nightRotations: row.nightRotations,
      active: row.active,
    })
    setModalOpen(true)
  }

  const handleSubmit = async (v: EditFormValues) => {
    if (!editing) return
    const payload: VcMachineUpdatePayload = {
      totalSlots: editing.totalSlots,
      dayRotations: v.dayRotations,
      nightRotations: v.nightRotations,
      active: v.active,
    }
    try {
      await vcMachineApi.update(editing.machineId, payload)
      void message.success(`수정 완료 — ${editing.machineId}`)
      setModalOpen(false)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        void message.error('IT_OPS 권한 필요')
      } else if (e instanceof HttpError && e.status === 404) {
        void message.error('대상 Machine 미존재')
      } else {
        void message.error('수정 실패')
      }
    }
  }

  const handleToggleActive = async (row: VcMachineSummary) => {
    try {
      if (row.active) {
        // active → inactive: soft-delete via DELETE
        await vcMachineApi.delete(row.machineId)
        void message.success(`${row.machineId} 비활성화 완료`)
      } else {
        // inactive → active: PUT with active=true (restore)
        await vcMachineApi.update(row.machineId, {
          totalSlots: row.totalSlots,
          dayRotations: row.dayRotations,
          nightRotations: row.nightRotations,
          active: true,
        })
        void message.success(`${row.machineId} 활성화 완료`)
      }
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        void message.error('IT_OPS 권한 필요')
      } else {
        void message.error('상태 변경 실패')
      }
    }
  }

  const columns = [
    {
      title: 'Machine ID',
      dataIndex: 'machineId',
      key: 'machineId',
      render: (v: string) => <Text strong>{v}</Text>,
    },
    {
      title: '유형',
      dataIndex: 'machineType',
      key: 'machineType',
      render: (v: string) => <Tag color={v === 'LP' ? 'blue' : 'purple'}>{v}</Tag>,
    },
    { title: '슬롯 수', dataIndex: 'totalSlots', key: 'totalSlots' },
    { title: '주간 회전', dataIndex: 'dayRotations', key: 'dayRotations' },
    { title: '야간 회전', dataIndex: 'nightRotations', key: 'nightRotations' },
    {
      title: '활성',
      dataIndex: 'active',
      key: 'active',
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? '활성' : '비활성'}</Tag>,
    },
    { title: '수정자', dataIndex: 'updatedBy', key: 'updatedBy' },
    {
      title: '액션',
      key: 'actions',
      render: (_: unknown, r: VcMachineSummary) => (
        <Space>
          <Button size="small" onClick={() => openEdit(r)}>수정</Button>
          <Popconfirm
            title={r.active ? `${r.machineId} 비활성화` : `${r.machineId} 활성화`}
            description={
              r.active
                ? '해당 기계를 비활성화합니다. 스케줄 생성 시 제외됩니다.'
                : '해당 기계를 다시 활성화합니다.'
            }
            okText={r.active ? '비활성화' : '활성화'}
            cancelText="취소"
            okButtonProps={{ danger: r.active }}
            onConfirm={() => handleToggleActive(r)}
          >
            <Button size="small" danger={r.active}>
              {r.active ? '비활성화' : '활성화'}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Title level={3}>VC 가류기 관리</Title>
      <Paragraph type="secondary">
        LP-01~LP-04 (저압가류기) · IC-01 (IC가류기) 5대 관리. IT_OPS 만 수정 가능 (BR-X02 audit).
      </Paragraph>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => void reload()}>새로고침</Button>
      </Space>
      <Table
        rowKey="machineId"
        loading={loading}
        dataSource={rows}
        columns={columns}
        pagination={false}
      />

      <Modal
        title={editing ? `가류기 수정 — ${editing.machineId}` : '가류기 수정'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        {editing && (
          <Paragraph type="secondary" style={{ marginBottom: 16 }}>
            <Text strong>유형:</Text> {editing.machineType}&nbsp;&nbsp;
            <Text strong>슬롯:</Text> {editing.totalSlots}
            {editing.machineType === 'LP' && ' (LP 고정값 — 수정 불가)'}
          </Paragraph>
        )}
        <Form<EditFormValues> form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            label="주간 회전 수 (1~24)"
            name="dayRotations"
            rules={[{ required: true }]}
          >
            <InputNumber min={1} max={24} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            label="야간 회전 수 (1~24)"
            name="nightRotations"
            rules={[{ required: true }]}
          >
            <InputNumber min={1} max={24} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="활성 여부" name="active" valuePropName="checked">
            <Switch checkedChildren="활성" unCheckedChildren="비활성" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>저장</Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
