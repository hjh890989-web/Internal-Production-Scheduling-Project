import { apiFetch } from '@/api/client'
import type { ExCandidateStatus } from '../types/exMatrix'

/**
 * EX 매트릭스 row — 백엔드 ExMatrixQueryController.MatrixRow record 1:1.
 */
export interface ExMatrixRow {
  exCandidateId: string
  hoseId: string
  vcProductionDate: string // YYYY-MM-DD
  extrusionDeadline: string
  vcYield: number
  status: ExCandidateStatus
}

/** EP-17 매트릭스 fetch — TK-17-1-2. */
export async function fetchExMatrix(from: string, to: string): Promise<ExMatrixRow[]> {
  return apiFetch<ExMatrixRow[]>(
    `/api/v1/schedule/ex/matrix?from=${from}&to=${to}`,
  )
}

/** EP-12 Excel matrix XLSX 다운로드 (Blob — UI 다운로드 트리거용). */
export async function downloadExMatrixXlsx(from: string, to: string): Promise<Blob> {
  const res = await fetch(`/api/v1/export/extrusion-matrix?from=${from}&to=${to}`)
  if (!res.ok) throw new Error(`Export failed: HTTP ${res.status}`)
  return res.blob()
}
