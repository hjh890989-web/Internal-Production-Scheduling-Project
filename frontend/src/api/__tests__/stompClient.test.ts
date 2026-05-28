import { describe, it, expect } from 'vitest'
import { SchedulingStompClient, TOPIC_EXTRUSION_UPDATES } from '../stompClient'

/**
 * SchedulingStompClient — Sprint 5 EP-17 WebSocket client wrapper.
 *
 * <p>실 STOMP 연결은 E2E (Playwright) 에서 검증. 본 unit 은 wrapper 계약만.
 */
describe('SchedulingStompClient', () => {
  it('TOPIC_EXTRUSION_UPDATES — /topic/extrusion-updates (백엔드 EP-EX14 ExReplanPushListener)', () => {
    expect(TOPIC_EXTRUSION_UPDATES).toBe('/topic/extrusion-updates')
  })

  it('연결 안 된 상태 — subscribe 는 registry 만 저장 (Sprint 19 hotfix, onConnect 시 자동 재구독)', () => {
    const c = new SchedulingStompClient()
    let callbackInvoked = false
    const unsubscribe = c.subscribe('/topic/x', () => {
      callbackInvoked = true
    })
    expect(typeof unsubscribe).toBe('function')
    // 호출되지 않은 콜백 — 실 메시지는 connect 후 STOMP 수신 시점
    expect(callbackInvoked).toBe(false)
    // cleanup
    unsubscribe()
  })

  it('isConnected — 초기 false', () => {
    const c = new SchedulingStompClient()
    expect(c.isConnected()).toBe(false)
  })
})
