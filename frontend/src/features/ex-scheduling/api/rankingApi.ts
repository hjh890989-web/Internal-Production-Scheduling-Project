import { apiFetch } from '@/api/client'

/**
 * 백엔드 CandidateRankingService.RankedCandidate record 1:1.
 */
export interface RankedCandidate {
  exCandidateId: string
  hoseId: string
  extrusionDeadline: string
  vcYield: number
  slackDaysScore: number
  balanceScore: number
  settingScore: number
  totalScore: number
}

export async function fetchRanking(from: string, to: string, limit = 10): Promise<RankedCandidate[]> {
  return apiFetch<RankedCandidate[]>(
    `/api/v1/schedule/ex/candidates/ranking?from=${from}&to=${to}&limit=${limit}`,
  )
}
