import { useEffect } from 'react'
import {
  stompClient,
  TOPIC_MES_DEGRADED_UPDATES,
  TOPIC_NOTIFICATIONS_PREFIX,
} from '@/api/stompClient'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '../notificationStore'

interface ServerNotification {
  notificationId?: string
  severity?: 'CRITICAL' | 'IMPORTANT' | 'STANDARD' | 'INFO'
  hoseId?: string
  deliveryDate?: string
  changeSummary?: string
  changedAt?: string
}

interface MesDegradedPayload {
  machineId: string
  wasDegraded: boolean
  isDegraded: boolean
  changedAt: string
  transition: 'ENTERING' | 'RECOVERED' | 'NONE'
}

/**
 * Sprint 18 EP-NOTIFY ST-NOTIFY-5 — STOMP topic 구독 → notificationStore 누적 (TK-NOTIFY-5-2).
 *
 * <p>로그인 후 한 번 mount — Layout 에서 사용. 토픽:
 * <ul>
 *   <li>{@link TOPIC_NOTIFICATIONS_PREFIX}/{role} — Sprint 1 일반 알림 (Critical Diff 등)</li>
 *   <li>{@link TOPIC_MES_DEGRADED_UPDATES} — Sprint 18 ST-NOTIFY-4 MES degraded 전이</li>
 * </ul>
 *
 * <p>인증 미완료 시 STOMP 연결 skip — 로그인 후 재호출 시 자동 구독.
 */
export function useStompNotificationFeed() {
  const role = useAuthStore((s) => s.user?.role)
  const add = useNotificationStore((s) => s.add)

  useEffect(() => {
    if (!role) return
    let cancelled = false
    let unsubscribers: Array<() => void> = []

    stompClient
      .connect({ debug: false })
      .then(() => {
        if (cancelled) return
        // 1) role 별 일반 알림 토픽
        unsubscribers.push(
          stompClient.subscribe<ServerNotification>(
            `${TOPIC_NOTIFICATIONS_PREFIX}/${role}`,
            (payload) => {
              add({
                title: payload.severity
                  ? `${payload.severity} 알림 — ${payload.hoseId ?? '미분류'}`
                  : '알림',
                body: payload.changeSummary ?? '내용 없음',
                severity: payload.severity ?? 'STANDARD',
                source: 'ORDER_DIFF',
              })
            },
          ),
        )

        // 2) MES degraded 전이 토픽 (Sprint 18 ST-NOTIFY-4)
        unsubscribers.push(
          stompClient.subscribe<MesDegradedPayload>(TOPIC_MES_DEGRADED_UPDATES, (payload) => {
            if (payload.transition === 'NONE') return
            const entering = payload.transition === 'ENTERING'
            add({
              title: entering
                ? `MES degraded 진입 — ${payload.machineId}`
                : `MES 정상 복구 — ${payload.machineId}`,
              body: entering
                ? '1 shift (6h) 미수신 — 직전 계획값 임시 사용. Excel 폴백 입력 권고.'
                : '자동 재조정 진행.',
              severity: 'CRITICAL',
              source: 'MES_DEGRADED',
              link: '/vc/simview',
            })
          }),
        )
      })
      .catch(() => {
        // 미연결 시 silent — apiFetch 가 STOMP 미가용 환경도 처리
      })

    return () => {
      cancelled = true
      unsubscribers.forEach((u) => u())
    }
  }, [role, add])
}
