import { describe, it, expect } from 'vitest'
import type { SnapshotResult, TimelineEntry } from '../api/auditSnapshotApi'

/**
 * EP-19 SnapshotResult + TimelineEntry — 백엔드 record 1:1 정합.
 */
describe('AuditSnapshot types', () => {
  it('SnapshotResult — 7 field (rowExisted true 경로)', () => {
    const r: SnapshotResult = {
      tableName: 'vc_schedule',
      rowPk: 'uuid-1',
      atTimestamp: '2026-05-22T10:00:00Z',
      capturedAt: '2026-05-22T09:30:00Z',
      lastAction: 'UPDATE',
      rowExisted: true,
      jsonPayload: '{"hose_id":"29673-2R060"}',
    }
    expect(r.rowExisted).toBe(true)
    expect(r.lastAction).toBe('UPDATE')
  })

  it('SnapshotResult — rowExisted false (DELETE 이후)', () => {
    const r: SnapshotResult = {
      tableName: 'vc_schedule',
      rowPk: 'uuid-2',
      atTimestamp: '2026-05-22T10:00:00Z',
      capturedAt: '2026-05-22T08:00:00Z',
      lastAction: 'DELETE',
      rowExisted: false,
      jsonPayload: null,
    }
    expect(r.jsonPayload).toBeNull()
  })

  it('TimelineEntry — 5 field (INSERT actor system fallback)', () => {
    const e: TimelineEntry = {
      audit_id: 1,
      action: 'INSERT',
      actor: 'system',
      reason: null,
      occurred_at: '2026-05-22T09:00:00Z',
    }
    expect(e.action).toBe('INSERT')
  })
})
