import { describe, it, expect } from 'vitest'
import type {
  CapacityOverflowRequest,
  ConsumedEntry,
  EnqueueResponse,
  SplitResult,
  SupplementResult,
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

  it('Sprint 8 — CapacityOverflowRequest PENDING 상태', () => {
    const r: CapacityOverflowRequest = {
      requestId: 'uuid-pending-1',
      hoseId: '29673-2R060',
      requestedQty: 40,
      priorityRank: 1,
      requestedAt: '2026-06-01T09:00:00Z',
      requestedBy: 'planner-001',
      status: 'PENDING',
      decidedAt: null,
      decidedBy: null,
      decisionReason: null,
    }
    expect(r.status).toBe('PENDING')
    expect(r.decidedAt).toBeNull()
  })

  it('Sprint 8 — CapacityOverflowRequest REJECTED + reason', () => {
    const r: CapacityOverflowRequest = {
      requestId: 'uuid-rejected-1',
      hoseId: 'X-LOW-PRIORITY',
      requestedQty: 50,
      priorityRank: 99,
      requestedAt: '2026-06-01T09:00:00Z',
      requestedBy: 'planner-001',
      status: 'REJECTED',
      decidedAt: '2026-06-01T10:00:00Z',
      decidedBy: 'planner-002',
      decisionReason: 'capa 불충분',
    }
    expect(r.status).toBe('REJECTED')
    expect(r.decisionReason).toBe('capa 불충분')
  })

  it('Sprint 8 — EnqueueResponse 빈 list 가능', () => {
    const empty: EnqueueResponse = { requestIds: [] }
    const filled: EnqueueResponse = { requestIds: ['uuid-1', 'uuid-2'] }
    expect(empty.requestIds).toHaveLength(0)
    expect(filled.requestIds).toHaveLength(2)
  })
})
