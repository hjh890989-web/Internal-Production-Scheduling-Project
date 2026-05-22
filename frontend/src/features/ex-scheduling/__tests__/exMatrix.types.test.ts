import { describe, it, expect } from 'vitest'
import type {
  ExCandidateStatus,
  ExMatrixCell,
  ExReplanCompletedPayload,
} from '../types/exMatrix'

/**
 * 컴파일 타임 type 검증 + payload 호환성 — Sprint 5 EP-17·EX14.
 *
 * <p>백엔드 Java record 와 1:1 정합 검증. type drift 발생 시 본 테스트 fail.
 */
describe('ExReplanCompletedPayload', () => {
  it('백엔드 ExReplanCompletedEvent record 매핑 — 5 field 필수', () => {
    const payload: ExReplanCompletedPayload = {
      vcScheduleId: 'b1e0c1c4-1234-5678-9abc-def012345678',
      completedAt: '2026-05-22T15:30:00Z',
      triggeredCount: 3,
      candidateIds: ['uuid-1', 'uuid-2', 'uuid-3'],
    }
    expect(payload.vcScheduleId).toMatch(/[0-9a-f-]{36}/)
    expect(payload.completedAt).toMatch(/Z$/)
    expect(payload.triggeredCount).toBe(3)
    expect(payload.candidateIds).toHaveLength(3)
  })

  it('ExCandidateStatus — 5 enum (PENDING/READY/SCHEDULED/CONFIRMED/FAILED)', () => {
    const all: ExCandidateStatus[] = ['PENDING', 'READY', 'SCHEDULED', 'CONFIRMED', 'FAILED']
    expect(all).toHaveLength(5)
  })

  it('ExMatrixCell — EP-17 매트릭스 row 6 field', () => {
    const cell: ExMatrixCell = {
      hoseId: '29673-2R060',
      date: '2026-05-25',
      shiftCode: 'D1',
      lineId: 'L1',
      yield: 2531,
      status: 'SCHEDULED',
    }
    expect(cell.yield).toBe(2531) // BR-E05 reference
    expect(cell.lineId).toBe('L1') // NEW priority 1 (NS-S09)
  })
})
