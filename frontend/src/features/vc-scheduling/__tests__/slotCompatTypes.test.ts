import { describe, expect, it } from 'vitest'
import {
  ALL_SLOTS,
  isEligible,
  slotMachineType,
  type CompatibilityResponse,
} from '../types/slotCompat'

describe('slotCompat types — TK-04-3-1', () => {
  it('ALL_SLOTS — LP 4 + IC 3 = 7', () => {
    expect(ALL_SLOTS).toHaveLength(7)
    expect(ALL_SLOTS).toContain('LP_TOP')
    expect(ALL_SLOTS).toContain('IC_BOT')
  })

  it('slotMachineType — LP_*/IC_* 접두 매핑', () => {
    expect(slotMachineType('LP_TOP')).toBe('LP')
    expect(slotMachineType('LP_BOT')).toBe('LP')
    expect(slotMachineType('IC_TOP')).toBe('IC')
    expect(slotMachineType('IC_BOT')).toBe('IC')
  })

  describe('isEligible', () => {
    const compat: CompatibilityResponse = {
      version: 1,
      builtAt: '2026-05-21T00:00:00Z',
      byHose: {
        'A001': { LP_TOP: false, LP_UPMID: true, LP_LOWMID: true, LP_BOT: false, IC_TOP: true, IC_MID: false, IC_BOT: false },
      },
      bySlot: {
        LP_TOP: [], LP_UPMID: ['A001'], LP_LOWMID: ['A001'], LP_BOT: [],
        IC_TOP: ['A001'], IC_MID: [], IC_BOT: [],
      },
      unschedulableHoseIds: [],
    }

    it('적합 슬롯 → true', () => {
      expect(isEligible(compat, 'A001', 'LP_UPMID')).toBe(true)
      expect(isEligible(compat, 'A001', 'IC_TOP')).toBe(true)
    })

    it('비적합 슬롯 → false', () => {
      expect(isEligible(compat, 'A001', 'LP_TOP')).toBe(false)
      expect(isEligible(compat, 'A001', 'IC_BOT')).toBe(false)
    })

    it('매트릭스 미적재 품번 → Optimistic OK (true)', () => {
      expect(isEligible(compat, 'UNKNOWN', 'LP_TOP')).toBe(true)
    })

    it('매트릭스 미로딩 (undefined) → Optimistic OK', () => {
      expect(isEligible(undefined, 'A001', 'LP_TOP')).toBe(true)
    })

    it('hoseId 미정 → Optimistic OK', () => {
      expect(isEligible(compat, undefined, 'LP_TOP')).toBe(true)
    })
  })
})
