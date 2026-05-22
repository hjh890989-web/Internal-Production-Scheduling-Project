import { apiFetch } from '@/api/client'

/**
 * Swap proposal REST DTO — 백엔드 SwapProposalController.ProposalResponse 1:1.
 */
export interface SwapProposalDto {
  proposalId: string
  sourceRowId: string
  targetRowId: string
  proposedBy: string
  proposedAt: string
  status: 'PROPOSED' | 'ACCEPTED' | 'REJECTED'
  resolvedBy: string | null
  resolvedAt: string | null
}

export interface ProposePayload {
  sourceRowId: string
  targetRowId: string
  reason?: string
}

export async function proposeSwap(payload: ProposePayload): Promise<SwapProposalDto> {
  return apiFetch<SwapProposalDto>('/api/v1/schedule/vc/proposals', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function acceptProposal(id: string, note?: string): Promise<SwapProposalDto> {
  return apiFetch<SwapProposalDto>(`/api/v1/schedule/vc/proposals/${id}/accept`, {
    method: 'POST',
    body: JSON.stringify({ note: note ?? null }),
  })
}

export async function rejectProposal(id: string, note?: string): Promise<SwapProposalDto> {
  return apiFetch<SwapProposalDto>(`/api/v1/schedule/vc/proposals/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify({ note: note ?? null }),
  })
}

export async function listProposals(
  status?: 'PROPOSED' | 'ACCEPTED' | 'REJECTED',
): Promise<SwapProposalDto[]> {
  const qs = status ? `?status=${status}` : ''
  return apiFetch<SwapProposalDto[]>(`/api/v1/schedule/vc/proposals${qs}`)
}
