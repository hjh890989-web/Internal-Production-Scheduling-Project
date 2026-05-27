import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { orderDiffApi } from '../orderDiffApi'
import { HttpError } from '../client'

/**
 * Sprint 13 EP-OC-FULL ST-OC-4 — orderDiffApi 단위 test.
 *
 * <p>get/commit/reject fetch mock — 200/403/404 분기.
 */
describe('orderDiffApi', () => {
  const fetchSpy = vi.spyOn(globalThis, 'fetch')

  beforeEach(() => { fetchSpy.mockReset() })
  afterEach(() => { fetchSpy.mockReset() })

  it('get — 200 + DiffSummaryResponse (severity count)', async () => {
    fetchSpy.mockResolvedValueOnce(new Response(
      JSON.stringify({
        trackingId: 'abc', totalRows: 3, criticalCount: 1, importantCount: 1,
        standardCount: 1, unclassifiedCount: 0, rows: [],
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))

    const result = await orderDiffApi.get('abc')

    expect(result.totalRows).toBe(3)
    expect(result.criticalCount).toBe(1)
  })

  it('commit — 200 + CommitResponse (decidedBy 사번)', async () => {
    fetchSpy.mockResolvedValueOnce(new Response(
      JSON.stringify({
        trackingId: 'abc', decidedBy: '00000001',
        decidedAt: '2026-05-27T10:00:00Z', affectedRows: 3, reason: 'OK',
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))

    const result = await orderDiffApi.commit('abc', 'OK')

    expect(result.decidedBy).toBe('00000001')
    expect(result.affectedRows).toBe(3)
  })

  it('commit — 403 STK_USER → HttpError(403)', async () => {
    fetchSpy.mockResolvedValueOnce(new Response(
      JSON.stringify({ title: 'Access Denied', status: 403 }),
      { status: 403, headers: { 'Content-Type': 'application/json' } },
    ))

    await expect(orderDiffApi.commit('abc', 'try')).rejects.toMatchObject({
      name: 'HttpError', status: 403,
    })
  })

  it('reject — 404 미존재 → HttpError(404)', async () => {
    fetchSpy.mockResolvedValueOnce(new Response(
      JSON.stringify({ title: '수주 import 확정 오류', detail: 'trackingId 미존재' }),
      { status: 404, headers: { 'Content-Type': 'application/json' } },
    ))

    try {
      await orderDiffApi.reject('xyz', '잘못된 데이터')
      expect.fail('should throw')
    } catch (e) {
      expect(e).toBeInstanceOf(HttpError)
      expect((e as HttpError).status).toBe(404)
    }
  })
})
