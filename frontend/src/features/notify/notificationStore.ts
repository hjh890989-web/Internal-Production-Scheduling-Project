import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/**
 * Sprint 18 EP-NOTIFY ST-NOTIFY-5 — in-app 알림 누적 store.
 *
 * <p>STOMP `/topic/notifications/{role}` + `/topic/mes-degraded-updates` 구독 시 append.
 * 최근 50건 cap (메모리 leak 방지). localStorage 영속 — 새로고침 후 미읽음 유지.
 */
export type NotificationSeverity = 'CRITICAL' | 'IMPORTANT' | 'STANDARD' | 'INFO'

export interface InAppNotification {
  id: string             // client-side UUID (server 미발급 시 crypto.randomUUID)
  title: string
  body: string
  severity: NotificationSeverity
  receivedAt: string     // ISO Instant
  read: boolean
  /** 클릭 시 deep-link path (예: /vc/simview). */
  link?: string
  /** 알림 소스 식별 — UI 분류 용. */
  source?: 'ORDER_DIFF' | 'VC_SCHEDULE' | 'MES_DEGRADED' | 'EX_REPLAN' | 'OTHER'
}

const MAX_NOTIFICATIONS = 50

interface NotificationState {
  notifications: InAppNotification[]
  add: (n: Omit<InAppNotification, 'id' | 'receivedAt' | 'read'> & { id?: string }) => void
  markRead: (id: string) => void
  markAllRead: () => void
  clear: () => void
  unreadCount: () => number
}

export const useNotificationStore = create<NotificationState>()(
  persist(
    (set, get) => ({
      notifications: [],
      add: (n) => {
        const id = n.id ?? (typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : `n-${Date.now()}-${Math.random().toString(36).slice(2)}`)
        const entry: InAppNotification = {
          id,
          title: n.title,
          body: n.body,
          severity: n.severity,
          receivedAt: new Date().toISOString(),
          read: false,
          link: n.link,
          source: n.source,
        }
        set((s) => ({
          notifications: [entry, ...s.notifications].slice(0, MAX_NOTIFICATIONS),
        }))
      },
      markRead: (id) =>
        set((s) => ({
          notifications: s.notifications.map((x) => (x.id === id ? { ...x, read: true } : x)),
        })),
      markAllRead: () =>
        set((s) => ({ notifications: s.notifications.map((x) => ({ ...x, read: true })) })),
      clear: () => set({ notifications: [] }),
      unreadCount: () => get().notifications.filter((n) => !n.read).length,
    }),
    {
      name: 'scheduling-notifications',
      // 1주일 후 자동 만료 (timestamp 기반 — 단순 cap 만 우선, TTL 은 Sprint 19+ 검토)
    },
  ),
)
