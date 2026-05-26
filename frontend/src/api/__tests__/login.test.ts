import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { login, HttpError } from '../client'

/**
 * Sprint 10 EP-AUTH client.login() 단위 test (TK-AUTH-5-6).
 *
 * <p>POST /api/v1/auth/login fetch mock — 200/401/423 응답 분기.
 */
describe('client.login', () => {
  const fetchSpy = vi.spyOn(globalThis, 'fetch')

  beforeEach(() => {
    fetchSpy.mockReset()
  })

  afterEach(() => {
    fetchSpy.mockReset()
  })

  it('200 정상 응답 — LoginResponse (token + role + expiresAt) 반환', async () => {
    fetchSpy.mockResolvedValueOnce(new Response(
      JSON.stringify({
        token: 'jwt-xyz',
        employeeId: '12345678',
        role: 'PLANNER',
        expiresAt: '2026-05-27T18:00:00Z',
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))

    const result = await login('12345678', '1234')

    expect(result.token).toBe('jwt-xyz')
    expect(result.role).toBe('PLANNER')
    expect(result.employeeId).toBe('12345678')
  })

  it('401 잘못된 PIN — HttpError(401)', async () => {
    fetchSpy.mockResolvedValueOnce(new Response(
      JSON.stringify({ title: '로그인 실패', detail: '사번 또는 PIN 불일치' }),
      { status: 401, headers: { 'Content-Type': 'application/json' } },
    ))

    await expect(login('12345678', '9999')).rejects.toMatchObject({
      name: 'HttpError',
      status: 401,
    })
  })

  it('423 잠금 — HttpError(423) + detail 메시지 포함', async () => {
    fetchSpy.mockResolvedValueOnce(new Response(
      JSON.stringify({ title: '로그인 실패', detail: '계정 잠금 — 5회 실패' }),
      { status: 423, headers: { 'Content-Type': 'application/json' } },
    ))

    try {
      await login('12345678', '1234')
      expect.fail('should throw')
    } catch (e) {
      expect(e).toBeInstanceOf(HttpError)
      expect((e as HttpError).status).toBe(423)
    }
  })
})
