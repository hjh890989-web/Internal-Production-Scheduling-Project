import { Client, IFrame, type IMessage, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/stores/authStore'

/**
 * STOMP over SockJS — Sprint 5 EP-17 매트릭스 뷰 / EP-EX14 cascade PUSH 구독.
 *
 * 백엔드 엔드포인트:
 *   - {@code /ws/notifications} (SockJS — WebSocketStompConfig)
 *   - {@code /topic/extrusion-updates} (EP-EX14 ExReplanCompletedEvent payload)
 *   - {@code /topic/notifications/{role}} (Sprint 1 일반 알림)
 *
 * Bearer JWT 는 CONNECT 헤더로 전송. 미인증 시 mock 모드 (브로커 미연결).
 *
 * <p>Sprint 19 베타 시뮬 hotfix — backend 재기동 시 STOMP disconnect → 자동 reconnect
 * 시 기존 subscription 잃음 문제 해결. {@link subscriptionRegistry} 가 (topic, callback)
 * 보존 → onConnect 마다 {@link resubscribeAll} 호출로 자동 재구독.
 */

const SOCKJS_URL = '/ws/notifications'

export type StompCallback<T> = (payload: T, frame: IMessage) => void

export interface StompConnectionOptions {
  /** 재연결 시도 간격 (ms). 기본 5000. */
  reconnectDelayMs?: number
  /** debug 로그 (개발 환경) */
  debug?: boolean
}

interface SubscriptionEntry {
  callback: StompCallback<unknown>
  /** 현재 STOMP subscription handle — reconnect 마다 갱신. */
  handle: StompSubscription | null
}

export class SchedulingStompClient {
  private client: Client | null = null
  /**
   * Sprint 19 hotfix — topic → callback registry. reconnect 시 onConnect 가 호출돼
   * registry 순회하며 client.subscribe 다시 호출 → 자동 재구독 보장.
   */
  private readonly subscriptionRegistry = new Map<string, SubscriptionEntry>()

  connect(options: StompConnectionOptions = {}): Promise<void> {
    if (this.client?.connected) return Promise.resolve()
    const token = useAuthStore.getState().token
    return new Promise((resolve, reject) => {
      let firstResolveDone = false
      const c = new Client({
        webSocketFactory: () => new SockJS(SOCKJS_URL),
        connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
        reconnectDelay: options.reconnectDelayMs ?? 5000,
        debug: options.debug ? (msg) => console.debug('[STOMP]', msg) : () => {},
        onConnect: () => {
          // 매 connect 마다 호출 (첫 연결 + reconnect 모두) — registry 일괄 재구독
          this.resubscribeAll()
          if (!firstResolveDone) {
            firstResolveDone = true
            resolve()
          }
        },
        onStompError: (frame: IFrame) => {
          if (!firstResolveDone) {
            firstResolveDone = true
            reject(new Error(`STOMP error: ${frame.headers.message ?? 'unknown'}`))
          }
        },
      })
      c.activate()
      this.client = c
    })
  }

  /**
   * topic 구독. 콜백은 JSON.parse 된 payload + 원본 frame 수신.
   * 반환된 unsubscribe 함수를 useEffect cleanup 에서 호출.
   *
   * <p>connection 미수립 상태에서 호출 시 registry 만 두고 다음 onConnect 시 자동 subscribe.
   * reconnect 후에도 동일 registry 가 재구독 트리거.
   */
  subscribe<T = unknown>(topic: string, callback: StompCallback<T>): () => void {
    const entry: SubscriptionEntry = {
      callback: callback as StompCallback<unknown>,
      handle: null,
    }
    this.subscriptionRegistry.set(topic, entry)

    // 이미 연결돼 있으면 즉시 subscribe
    if (this.client?.connected) {
      entry.handle = this.doSubscribe(topic, entry)
    }
    // 미연결 상태면 onConnect 의 resubscribeAll() 이 처리

    return () => {
      entry.handle?.unsubscribe()
      this.subscriptionRegistry.delete(topic)
    }
  }

  /**
   * Sprint 19 hotfix — onConnect 마다 호출돼 registry 일괄 재구독.
   * 첫 연결 + 모든 reconnect 모두에서 트리거.
   */
  private resubscribeAll(): void {
    if (!this.client?.connected) return
    for (const [topic, entry] of this.subscriptionRegistry.entries()) {
      // 이전 handle 이 stale 일 수 있음 — 새로 subscribe
      entry.handle = this.doSubscribe(topic, entry)
    }
  }

  private doSubscribe(topic: string, entry: SubscriptionEntry): StompSubscription {
    return this.client!.subscribe(topic, (frame) => {
      const body = JSON.parse(frame.body) as unknown
      entry.callback(body, frame)
    })
  }

  disconnect(): void {
    this.subscriptionRegistry.forEach((e) => e.handle?.unsubscribe())
    this.subscriptionRegistry.clear()
    this.client?.deactivate().catch(() => {})
    this.client = null
  }

  isConnected(): boolean {
    return this.client?.connected ?? false
  }
}

/** 싱글톤 — 페이지 간 연결 재사용. */
export const stompClient = new SchedulingStompClient()

/** EP-EX14 — 압출 매트릭스 PUSH 토픽. */
export const TOPIC_EXTRUSION_UPDATES = '/topic/extrusion-updates'

/** Sprint 14 EP-VC-FULL ST-VC-4 — VC schedule 변경/확정 PUSH 토픽. */
export const TOPIC_VC_SCHEDULE_UPDATES = '/topic/vc-schedule-updates'

/** Sprint 18 EP-NOTIFY ST-NOTIFY-4 — MES degraded 전이 PUSH 토픽 (DegradedBanner 즉시 갱신). */
export const TOPIC_MES_DEGRADED_UPDATES = '/topic/mes-degraded-updates'

/** Sprint 18 EP-NOTIFY ST-NOTIFY-5 — role 별 in-app 알림 토픽 prefix. */
export const TOPIC_NOTIFICATIONS_PREFIX = '/topic/notifications'
