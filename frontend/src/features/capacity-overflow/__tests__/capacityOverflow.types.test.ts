import { describe, it, expect } from 'vitest'
import type {
  SplitResult,
  SupplementResult,
  ConsumedEntry,
} from '../api/capacityOverflowApi'

/**
 * BR-V12·V13 SplitResult + SupplementResult — 백엔드 record 1:1 정합.
 */
describe('CapacityOverflow types', () => {
  it('SplitResult — 4 field (accepted + queue)', () => {
    const r: SplitResult = {
      accepted: { '29673-2R060': 60, '28422-2M800': 30 },
      requestQueue: { '28421-2M800': 40 },
      totalAccepted: 90,
      totalQueued: 40,
    }
    expect(r.totalAccepted).toBe(90)
    expect(r.totalQueued).toBe(40)
    expect(Object.keys(r.accepted)).toHaveLength(2)
  })

  it('SplitResult — 추가 요청 없는 경우 (capa 내 모두 채택)', () => {
    const r: SplitResult = {
      accepted: { '29673-2R060': 50 },
      requestQueue: {},
      totalAccepted: 50,
      totalQueued: 0,
    }
    expect(Object.keys(r.requestQueue)).toHaveLength(0)
  })

  it('SupplementResult — 동일 hose 1차 + 그룹 2차 모두 사용', () => {
    const consumed: ConsumedEntry[] = [
      { kdOrderId: 'uuid-1', fromHose: '29673-2R060', qty: 30 },
      { kdOrderId: 'uuid-2', fromHose: '29673-2R061', qty: 20 },
    ]
    const r: SupplementResult = {
      hoseId: '29673-2R060',
      shortage: 50,
      supplemented: 50,
      consumed,
    }
    expect(r.supplemented).toBe(r.shortage)
    expect(r.consumed).toHaveLength(2)
  })

  it('SupplementResult — 잔량 부족 (부분 보충)', () => {
    const r: SupplementResult = {
      hoseId: 'X-NOT-IN-KD',
      shortage: 100,
      supplemented: 30,
      consumed: [{ kdOrderId: 'uuid-3', fromHose: 'X-NOT-IN-KD', qty: 30 }],
    }
    expect(r.supplemented).toBeLessThan(r.shortage)
  })
})
