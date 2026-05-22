import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  acceptProposal,
  listProposals,
  proposeSwap,
  rejectProposal,
  type ProposePayload,
  type SwapProposalDto,
} from '../api/swapApi'

const KEY = ['swap-proposals']

/**
 * Swap proposal CRUD hook — TK-15-2-1·3 (EP-15 ST-15-2).
 *
 * <p>PROPOSED 상태 목록 + propose/accept/reject mutation 통합. 1클릭 수용은
 * UI 에서 onAccept(id) 하나로 호출 — 본 hook 이 invalidate 후 자동 재조회.
 */
export function useSwapProposals(status: SwapProposalDto['status'] = 'PROPOSED') {
  const qc = useQueryClient()
  const listQuery = useQuery<SwapProposalDto[]>({
    queryKey: [...KEY, status],
    queryFn: () => listProposals(status),
    staleTime: 10_000,
  })

  const propose = useMutation({
    mutationFn: (payload: ProposePayload) => proposeSwap(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const accept = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => acceptProposal(id, note),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  const reject = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => rejectProposal(id, note),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })

  return { listQuery, propose, accept, reject }
}
