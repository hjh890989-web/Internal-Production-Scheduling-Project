import { Client, IFrame, type IMessage } from '@stomp/stompjs'
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
 */

const SOCKJS_URL = '/ws/notifications'

export type StompCallback<T> = (payload: T, frame: IMessage) => void

export interface StompConnectionOptions {
  /** 재연결 시도 간격 (ms). 기본 5000. */
  reconnectDelayMs?: number
  /** debug 로그 (개발 환경) */
  debug?: boolean
}

export class SchedulingStompClient {
  private client: Client | null = null
  private readonly subscriptions = new Map<string, () => void>()

  connect(options: StompConnectionOptions = {}): Promise<void> {
    if (this.client?.connected) return Promise.resolve()
    const token = useAuthStore.getState().token
    return new Promise((resolve, reject) => {
      const c = new Client({
        webSocketFactory: () => new SockJS(SOCKJS_URL),
        connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
        reconnectDelay: options.reconnectDelayMs ?? 5000,
        debug: options.debug ? (msg) => console.debug('[STOMP]', msg) : () => {},
        onConnect: () => resolve(),
        onStompError: (frame: IFrame) =>
          reject(new Error(`STOMP error: ${frame.headers.message ?? 'unknown'}`)),
      })
      c.activate()
      this.client = c
    })
  }

  /**
   * topic 구독. 콜백은 JSON.parse 된 payload + 원본 frame 수신.
   * 반환된 unsubscribe 함수를 useEffect cleanup 에서 호출.
   */
  subscribe<T = unknown>(topic: string, callback: StompCallback<T>): () => void {
    if (!this.client?.connected) {
      throw new Error('STOMP not connected — call connect() first')
    }
    const sub = this.client.subscribe(topic, (frame) => {
      const body = JSON.parse(frame.body) as T
      callback(body, frame)
    })
    const unsubscribe = () => {
      sub.unsubscribe()
      this.subscriptions.delete(topic)
    }
    this.subscriptions.set(topic, unsubscribe)
    return unsubscribe
  }

  disconnect(): void {
    this.subscriptions.forEach((u) => u())
    this.subscriptions.clear()
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
