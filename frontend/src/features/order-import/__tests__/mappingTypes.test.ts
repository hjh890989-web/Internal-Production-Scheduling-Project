import { describe, expect, it } from 'vitest'
import {
  requiresReviewModal,
  successRate,
  type MappingResult,
} from '../types/mapping'

const EMPTY: MappingResult = { successes: [], failures: [], sourceType: 'UNRECOGNIZED' }

function result(s: number, f: number): MappingResult {
  return {
    successes: Array.from({ length: s }, (_, i) => ({
      orderId: `id-${i}`,
      hoseId: 'X',
      deliveryDate: '2026-02-15',
      qty: 1,
      orderType: 'WEEKLY',
      customer: '내수',
    })),
    failures: Array.from({ length: f }, (_, i) => ({
      sheetName: 'S',
      rowIndex: i,
      failedField: 'qty',
      reason: 'r',
      originalCells: [],
    })),
    sourceType: 'WEEKLY_PLAN',
  }
}

describe('MappingResult helpers (REQ-FUNC-OC-004)', () => {
  it('빈 결과 — successRate 0, modal 불필요', () => {
    expect(successRate(EMPTY)).toBe(0)
    expect(requiresReviewModal(EMPTY)).toBe(false)
  })

  it('100% 성공 — modal 불필요', () => {
    const r = result(100, 0)
    expect(successRate(r)).toBe(1)
    expect(requiresReviewModal(r)).toBe(false)
  })

  it('99% 성공 (1% 실패) — modal 필요 (임계 동등)', () => {
    const r = result(99, 1)
    expect(requiresReviewModal(r)).toBe(true)
  })

  it('99.5% 성공 (0.5% 실패) — modal 불필요', () => {
    const r = result(199, 1)
    expect(requiresReviewModal(r)).toBe(false)
  })

  it('50% 성공 — 심각한 실패율', () => {
    const r = result(50, 50)
    expect(successRate(r)).toBeCloseTo(0.5)
    expect(requiresReviewModal(r)).toBe(true)
  })
})
