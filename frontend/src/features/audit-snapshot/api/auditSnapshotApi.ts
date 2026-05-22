import { apiFetch } from '@/api/client'

/**
 * 백엔드 AuditSnapshotService.SnapshotResult record 1:1.
 */
export interface SnapshotResult {
  tableName: string
  rowPk: string
  atTimestamp: string
  capturedAt: string | null
  lastAction: 'INSERT' | 'UPDATE' | 'DELETE' | null
  rowExisted: boolean
  jsonPayload: string | null
}

export interface TimelineEntry {
  audit_id: number
  action: 'INSERT' | 'UPDATE' | 'DELETE'
  actor: string
  reason: string | null
  occurred_at: string
}

export async function fetchSnapshot(
  table: string,
  rowPk: string,
  at: string,
): Promise<SnapshotResult> {
  return apiFetch<SnapshotResult>(
    `/api/v1/audit/snapshot?table=${encodeURIComponent(table)}&rowPk=${encodeURIComponent(rowPk)}&at=${encodeURIComponent(at)}`,
  )
}

export async function fetchTimeline(table: string, rowPk: string): Promise<TimelineEntry[]> {
  return apiFetch<TimelineEntry[]>(
    `/api/v1/audit/timeline?table=${encodeURIComponent(table)}&rowPk=${encodeURIComponent(rowPk)}`,
  )
}
