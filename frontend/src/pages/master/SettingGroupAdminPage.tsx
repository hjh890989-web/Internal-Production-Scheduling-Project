import { useEffect, useState } from 'react'
import {
  Button, Form, Input, InputNumber, Modal, Popconfirm,
  Space, Table, Tag, Typography, message,
} from 'antd'
import { settingGroupApi, type SettingGroupSummary } from '@/api/settingGroupApi'
import { HttpError } from '@/api/client'

const { Title, Paragraph } = Typography

interface CreateFormValues {
  groupId: number
  displayName: string
}

interface EditFormValues {
  displayName: string
}

/**
 * Sprint 21 ST-CRUD-2 — 설정 그룹 관리 (IT_OPS write).
 *
 * groupId 범위 1~8 강제 (BR-V12·V13).
 * active toggle → DELETE endpoint (soft-delete).
 */
export default function SettingGroupAdminPage() {
  const [rows, setRows] = useState<SettingGroupSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<SettingGroupSummary | null>(null)
  const [createError, setCreateError] = useState<string | null>(null)
  const [createForm] = Form.useForm<CreateFormValues>()
  const [editForm] = Form.useForm<EditFormValues>()

  const reload = async () => {
    setLoading(true)
    try { setRows(await settingGroupApi.list()) }
    finally { setLoading(false) }
  }
  useEffect(() => { void reload() }, [])

  const openCreate = () => {
    setCreateError(null)
    createForm.resetFields()
    setCreateOpen(true)
  }

  const openEdit = (row: SettingGroupSummary) => {
    setEditTarget(row)
    editForm.setFieldsValue({ displayName: row.displayName })
  }

  const handleCreate = async (v: CreateFormValues) => {
    setCreateError(null)
    try {
      await settingGroupApi.create({ groupId: v.groupId, displayName: v.displayName })
      message.success(`설정 그룹 ${v.groupId} 추가 완료`)
      setCreateOpen(false)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 400) {
        const body = e.body as { detail?: string } | null
        setCreateError(body?.detail ?? 'BR-V12·V13 위반 — groupId 는 1~8 범위만 허용됩니다.')
      } else if (e instanceof HttpError && e.status === 403) {
        setCreateError('IT_OPS 권한 필요')
      } else {
        setCreateError('추가 실패')
      }
    }
  }

  const handleEdit = async (v: EditFormValues) => {
    if (!editTarget) return
    try {
      await settingGroupApi.update(editTarget.groupId, { displayName: v.displayName })
      message.success(`그룹 ${editTarget.groupId} 수정 완료`)
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

  const handleToggleActive = async (row: SettingGroupSummary) => {
    try {
      await settingGroupApi.toggleActive(row.groupId)
      message.success(
        row.active
          ? `그룹 ${row.groupId} 비활성화 완료`
          : `그룹 ${row.groupId} 활성화 완료`,
      )
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        message.error('IT_OPS 권한 필요')
      } else {
        message.error('상태 변경 실패')
      }
    }
  }

  const columns = [
    {
      title: 'Group ID',
      dataIndex: 'groupId',
      key: 'groupId',
      sorter: (a: SettingGroupSummary, b: SettingGroupSummary) => a.groupId - b.groupId,
    },
    { title: '표시명', dataIndex: 'displayName', key: 'displayName' },
    {
      title: '상태',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) =>
        active ? <Tag color="green">활성</Tag> : <Tag color="default">비활성</Tag>,
    },
    {
      title: '액션',
      key: 'actions',
      render: (_: unknown, row: SettingGroupSummary) => (
        <Space>
          <Button size="small" onClick={() => openEdit(row)}>수정</Button>
          <Popconfirm
            title={row.active ? `그룹 ${row.groupId} 비활성화` : `그룹 ${row.groupId} 활성화`}
            description={
              row.active
                ? '비활성화하면 스케줄링에서 제외됩니다. 계속하시겠습니까?'
                : '활성화하면 스케줄링에 다시 포함됩니다. 계속하시겠습니까?'
            }
            okText={row.active ? '비활성화' : '활성화'}
            okType={row.active ? 'danger' : 'primary'}
            cancelText="취소"
            onConfirm={() => handleToggleActive(row)}
          >
            <Button size="small" danger={row.active}>
              {row.active ? '비활성화' : '활성화'}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Title level={3}>설정 그룹 관리 (BR-V12·V13)</Title>
      <Paragraph type="secondary">
        groupId 1~8 범위만 허용. active toggle 은 soft-delete (DELETE endpoint). IT_OPS 만 변경 가능.
      </Paragraph>
      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={openCreate}>신규 추가</Button>
        <Button onClick={() => void reload()}>새로고침</Button>
      </Space>
      <Table
        rowKey="groupId"
        loading={loading}
        dataSource={rows}
        columns={columns}
        pagination={false}
      />

      {/* Create Modal */}
      <Modal
        title="설정 그룹 신규 추가"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        footer={null}
        destroyOnHidden
      >
        {createError && (
          <div
            data-testid="create-error"
            className="mb-3 rounded border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700"
          >
            {createError}
          </div>
        )}
        <Form<CreateFormValues> form={createForm} layout="vertical" onFinish={handleCreate}>
          <Form.Item
            label="Group ID (1~8, BR-V12·V13)"
            name="groupId"
            rules={[
              { required: true, message: 'Group ID 를 입력하세요.' },
              { type: 'number', min: 1, max: 8, message: 'groupId 는 1~8 범위만 허용됩니다.' },
            ]}
          >
            <InputNumber min={1} max={8} style={{ width: '100%' }} data-testid="input-group-id" />
          </Form.Item>
          <Form.Item
            label="표시명"
            name="displayName"
            rules={[{ required: true, message: '표시명을 입력하세요.' }]}
          >
            <Input maxLength={100} data-testid="input-display-name" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>추가</Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Modal */}
      <Modal
        title={editTarget ? `설정 그룹 수정 — ${editTarget.groupId}` : ''}
        open={!!editTarget}
        onCancel={() => setEditTarget(null)}
        footer={null}
        destroyOnHidden
      >
        <Form<EditFormValues> form={editForm} layout="vertical" onFinish={handleEdit}>
          <Form.Item
            label="표시명"
            name="displayName"
            rules={[{ required: true, message: '표시명을 입력하세요.' }]}
          >
            <Input maxLength={100} data-testid="edit-display-name" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>수정</Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
