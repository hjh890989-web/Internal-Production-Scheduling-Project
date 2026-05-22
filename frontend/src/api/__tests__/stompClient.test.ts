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

  it('연결 안 된 상태 — subscribe → Error', () => {
    const c = new SchedulingStompClient()
    expect(() => c.subscribe('/topic/x', () => {})).toThrow(/not connected/)
  })

  it('isConnected — 초기 false', () => {
    const c = new SchedulingStompClient()
    expect(c.isConnected()).toBe(false)
  })
})
