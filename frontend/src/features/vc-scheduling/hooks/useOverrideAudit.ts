import { useMutation } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type {
  OverrideAuditRequest,
  OverrideAuditResponse,
} from '../types/violation'

/**
 * 비적합 슬롯 강제 배치 시 audit 기록 — TK-04-3-2 (REQ-FUNC-CO-010).
 *
 * <p>POST /api/v1/audit/override — 사유 + 시각 + actor 자동 audit.
 * 백엔드는 EP-11 Sprint 2 후속에서 활성. Sprint 2 baseline 은 endpoint stub.
 */
export function useOverrideAudit() {
  return useMutation<OverrideAuditResponse, Error, OverrideAuditRequest>({
    mutationFn: (payload) =>
      apiFetch<OverrideAuditResponse>('/api/v1/audit/override', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
  })
}
