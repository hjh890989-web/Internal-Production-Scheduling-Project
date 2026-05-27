import { useEffect, useState } from 'react'
import { Badge, Button, Drawer, Empty, List, Space, Tag, Tooltip, Typography } from 'antd'
import { BellOutlined, CheckOutlined, DeleteOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useNotificationStore, type NotificationSeverity } from '../notificationStore'

const { Text } = Typography

const SEVERITY_COLOR: Record<NotificationSeverity, string> = {
  CRITICAL: 'red',
  IMPORTANT: 'orange',
  STANDARD: 'blue',
  INFO: 'default',
}

/**
 * Sprint 18 EP-NOTIFY ST-NOTIFY-5 — 우상단 종 아이콘 + Badge + Drawer (TK-NOTIFY-5-1).
 *
 * <p>4 role 공통 — Layout 우측 상단 nav 영역에 배치. Badge 는 미읽음 수, 클릭 시 Drawer 슬라이드 in.
 * Drawer 안 리스트는 severity 색상 Tag + 시각 + 본문 + (있으면) deep-link 버튼.
 */
export function NotificationDrawer() {
  const [open, setOpen] = useState(false)
  const notifications = useNotificationStore((s) => s.notifications)
  const unread = useNotificationStore((s) => s.notifications.filter((n) => !n.read).length)
  const markRead = useNotificationStore((s) => s.markRead)
  const markAllRead = useNotificationStore((s) => s.markAllRead)
  const clear = useNotificationStore((s) => s.clear)
  const navigate = useNavigate()

  // Drawer 가 열릴 때 표시된 항목들을 자동 markRead 하지는 않음 (사용자가 명시적 클릭 시 read 처리)
  // 단, 닫을 때 전체 읽음 처리는 별도 버튼으로 제공
  useEffect(() => {
    // no-op — 사용자 explicit action 만 read 처리
  }, [open])

  return (
    <>
      <Tooltip title="알림 (Sprint 18 EP-NOTIFY)">
        <Badge count={unread} size="small" overflowCount={99}>
          <Button
            type="text"
            icon={<BellOutlined style={{ fontSize: 18, color: '#fff' }} />}
            onClick={() => setOpen(true)}
            data-testid="notification-bell"
          />
        </Badge>
      </Tooltip>
      <Drawer
        title={
          <Space>
            <BellOutlined />
            <span>알림 ({notifications.length}건 · 미읽음 {unread})</span>
          </Space>
        }
        open={open}
        onClose={() => setOpen(false)}
        width={400}
        extra={
          <Space>
            <Button
              size="small"
              icon={<CheckOutlined />}
              disabled={unread === 0}
              onClick={markAllRead}
              data-testid="notification-mark-all-read"
            >
              모두 읽음
            </Button>
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              disabled={notifications.length === 0}
              onClick={clear}
              data-testid="notification-clear"
            >
              비우기
            </Button>
          </Space>
        }
      >
        {notifications.length === 0 ? (
          <Empty description="알림 없음" />
        ) : (
          <List
            size="small"
            dataSource={notifications}
            renderItem={(n) => (
              <List.Item
                style={{
                  backgroundColor: n.read ? undefined : '#fff7e6',
                  cursor: n.link ? 'pointer' : 'default',
                }}
                onClick={() => {
                  markRead(n.id)
                  if (n.link) {
                    navigate(n.link)
                    setOpen(false)
                  }
                }}
                data-testid={`notification-item-${n.id}`}
              >
                <Space direction="vertical" size={2} style={{ width: '100%' }}>
                  <Space>
                    <Tag color={SEVERITY_COLOR[n.severity]}>{n.severity}</Tag>
                    <Text strong>{n.title}</Text>
                  </Space>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {n.body}
                  </Text>
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    {n.receivedAt.substring(0, 19).replace('T', ' ')}
                    {n.source && ` · ${n.source}`}
                  </Text>
                </Space>
              </List.Item>
            )}
          />
        )}
      </Drawer>
    </>
  )
}
