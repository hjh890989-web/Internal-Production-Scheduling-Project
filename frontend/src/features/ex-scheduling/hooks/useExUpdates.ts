import { useEffect, useState } from 'react'
import { stompClient, TOPIC_EXTRUSION_UPDATES } from '@/api/stompClient'
import type { ExReplanCompletedPayload } from '../types/exMatrix'

/**
 * STOMP /topic/extrusion-updates 구독 — Sprint 5 EP-17 + EP-EX14 chain.
 *
 * <p>마운트 시 STOMP 연결 + 구독, unmount 시 unsubscribe (연결 유지 — 싱글톤).
 * 백엔드 cascade (vc.changed → replan → STOMP) 가 발생할 때마다 lastUpdate 갱신.
 *
 * <p>BR-X03 — 수동 호출 0건, 자동 PUSH.
 */
export function useExUpdates(): {
  connected: boolean
  lastUpdate: ExReplanCompletedPayload | null
  history: ExReplanCompletedPayload[]
} {
  const [connected, setConnected] = useState(false)
  const [lastUpdate, setLastUpdate] = useState<ExReplanCompletedPayload | null>(null)
  const [history, setHistory] = useState<ExReplanCompletedPayload[]>([])

  useEffect(() => {
    let unsubscribe: (() => void) | undefined
    let cancelled = false

    void stompClient
      .connect({ reconnectDelayMs: 5000 })
      .then(() => {
        if (cancelled) return
        setConnected(true)
        unsubscribe = stompClient.subscribe<ExReplanCompletedPayload>(
          TOPIC_EXTRUSION_UPDATES,
          (payload) => {
            setLastUpdate(payload)
            setHistory((prev) => [payload, ...prev].slice(0, 100))
          },
        )
      })
      .catch((err) => {
        console.error('[useExUpdates] STOMP connect failed', err)
        setConnected(false)
      })

    return () => {
      cancelled = true
      if (unsubscribe) unsubscribe()
    }
  }, [])

  return { connected, lastUpdate, history }
}
