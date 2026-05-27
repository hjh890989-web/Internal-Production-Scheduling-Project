import { useEffect, useState } from 'react'
import { Alert, Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import { userAdminApi, type UserSummary, type CreateUserRequest } from '@/api/userAdminApi'
import { HttpError } from '@/api/client'
import type { Role } from '@/stores/authStore'

const { Title, Paragraph } = Typography

const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'PLANNER', label: 'PLANNER (생산계획)' },
  { value: 'STK_USER', label: 'STK_USER (현장 STK)' },
  { value: 'IT_OPS', label: 'IT_OPS (IT 운영)' },
  { value: 'READ_ONLY', label: 'READ_ONLY (조회만)' },
]

interface ResetPinModalState {
  open: boolean
  employeeId: string | null
}

/**
 * Sprint 12 EP-MASTER-UI 사용자 관리 (TK-MASTER-2-3, IT_OPS only).
 *
 * <p>list/create/resetPin/unlock/delete 5 action — 모든 변경 audit_log 기록 (BR-X02).
 * RoleGuard 상위 라우터에서 IT_OPS 권한 검증 — 미충족 시 /forbidden.
 */
export default function UserAdminPage() {
  const [users, setUsers] = useState<UserSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [resetPin, setResetPin] = useState<ResetPinModalState>({ open: false, employeeId: null })

  const reload = async () => {
    setLoading(true)
    setError(null)
    try {
      setUsers(await userAdminApi.list())
    } catch (e) {
      setError(e instanceof Error ? e.message : '조회 실패')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void reload() }, [])

  const handleCreate = async (values: CreateUserRequest) => {
    try {
      await userAdminApi.create(values)
      message.success(`사용자 추가 완료 — ${values.employeeId}`)
      setCreateOpen(false)
      await reload()
    } catch (e) {
      if (e instanceof HttpError && e.status === 409) {
        message.error('사번 중복')
      } else {
        message.error('사용자 추가 실패')
      }
    }
  }

  const handleResetPin = async (values: { newPin: string }) => {
    if (!resetPin.employeeId) return
    try {
      await userAdminApi.resetPin(resetPin.employeeId, values.newPin)
      message.success(`PIN reset 완료 — ${resetPin.employeeId} / 새 PIN ${values.newPin}`)
      setResetPin({ open: false, employeeId: null })
      await reload()
    } catch {
      message.error('PIN reset 실패')
    }
  }

  const handleUnlock = async (employeeId: string) => {
    try {
      await userAdminApi.unlock(employeeId)
      message.success(`잠금 해제 완료 — ${employeeId}`)
      await reload()
    } catch {
      message.error('잠금 해제 실패')
    }
  }

  const handleDelete = (employeeId: string) => {
    Modal.confirm({
      title: `사용자 삭제 — ${employeeId}`,
      content: '삭제 후 복구 불가. audit_log 에 기록됩니다.',
      okText: '삭제',
      okType: 'danger',
      cancelText: '취소',
      onOk: async () => {
        try {
          await userAdminApi.delete(employeeId)
          message.success(`삭제 완료 — ${employeeId}`)
          await reload()
        } catch {
          message.error('삭제 실패')
        }
      },
    })
  }

  const columns = [
    { title: '사번', dataIndex: 'employeeId', key: 'employeeId' },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      render: (r: Role) => <Tag color={r === 'IT_OPS' ? 'gold' : r === 'PLANNER' ? 'blue' : 'default'}>{r}</Tag>,
    },
    {
      title: '실패 횟수',
      dataIndex: 'failedAttempts',
      key: 'failedAttempts',
      render: (n: number) => (n > 0 ? <Tag color="orange">{n}</Tag> : <Tag>0</Tag>),
    },
    {
      title: '잠금',
      dataIndex: 'lockedUntil',
      key: 'lockedUntil',
      render: (until: string | null) =>
        until && new Date(until).getTime() > Date.now() ? (
          <Tag color="red">잠김 (~{new Date(until).toLocaleTimeString()})</Tag>
        ) : (
          <Tag color="green">정상</Tag>
        ),
    },
    { title: '생성', dataIndex: 'createdAt', key: 'createdAt',
      render: (d: string) => new Date(d).toLocaleDateString() },
    {
      title: '액션',
      key: 'actions',
      render: (_: unknown, r: UserSummary) => (
        <Space>
          <Button size="small" onClick={() => setResetPin({ open: true, employeeId: r.employeeId })}>
            PIN reset
          </Button>
          <Button size="small" onClick={() => handleUnlock(r.employeeId)}>
            잠금 해제
          </Button>
          <Button size="small" danger onClick={() => handleDelete(r.employeeId)}>
            삭제
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Title level={3}>사용자 관리 (IT_OPS)</Title>
      <Paragraph type="secondary">
        NFR-SEC-007 — 사번 8자리 + PIN 4자리 BCrypt(12) · 5회 실패 → 10분 잠금 · BR-X02 audit 기록
      </Paragraph>

      {error && <Alert type="error" message={error} style={{ marginBottom: 16 }} />}

      <Space style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => setCreateOpen(true)}>신규 사용자 추가</Button>
        <Button onClick={() => void reload()}>새로고침</Button>
      </Space>

      <Table
        rowKey="employeeId"
        loading={loading}
        dataSource={users}
        columns={columns}
        pagination={false}
      />

      {/* 신규 사용자 모달 */}
      <Modal
        title="신규 사용자 추가"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form layout="vertical" onFinish={handleCreate}>
          <Form.Item
            label="사번 (8자리 숫자)"
            name="employeeId"
            rules={[{ required: true, pattern: /^[0-9]{8}$/, message: '사번 8자리 숫자' }]}
          >
            <Input maxLength={8} inputMode="numeric" />
          </Form.Item>
          <Form.Item
            label="초기 PIN (4자리 숫자)"
            name="pin"
            rules={[{ required: true, pattern: /^[0-9]{4}$/, message: 'PIN 4자리 숫자' }]}
          >
            <Input maxLength={4} inputMode="numeric" />
          </Form.Item>
          <Form.Item label="Role" name="role" rules={[{ required: true }]}>
            <Select options={ROLE_OPTIONS} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>추가</Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* PIN reset 모달 */}
      <Modal
        title={`PIN reset — ${resetPin.employeeId}`}
        open={resetPin.open}
        onCancel={() => setResetPin({ open: false, employeeId: null })}
        footer={null}
        destroyOnClose
      >
        <Alert
          type="warning"
          message="새 PIN 은 본 모달에서 1회만 표시됩니다. IT_OPS 가 별도 채널로 사용자에게 전달 후 첫 로그인 시 변경 권고."
          style={{ marginBottom: 16 }}
        />
        <Form layout="vertical" onFinish={handleResetPin}>
          <Form.Item
            label="새 PIN (4자리 숫자)"
            name="newPin"
            rules={[{ required: true, pattern: /^[0-9]{4}$/, message: 'PIN 4자리 숫자' }]}
          >
            <Input maxLength={4} inputMode="numeric" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>Reset</Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
