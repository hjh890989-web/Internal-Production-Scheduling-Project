import { describe, expect, it } from 'vitest'
import { renderHook } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useDragValidation } from '../hooks/useDragValidation'
import type { CompatibilityResponse } from '../types/slotCompat'

const compat: CompatibilityResponse = {
  version: 9,
  builtAt: '2026-05-21T00:00:00Z',
  byHose: {
    'A001': {
      LP_TOP: false, LP_UPMID: true, LP_LOWMID: true, LP_BOT: false,
      IC_TOP: true, IC_MID: false, IC_BOT: false,
    },
    'UNSCHED': {
      LP_TOP: false, LP_UPMID: false, LP_LOWMID: false, LP_BOT: false,
      IC_TOP: false, IC_MID: false, IC_BOT: false,
    },
  },
  bySlot: {
    LP_TOP: [], LP_UPMID: ['A001'], LP_LOWMID: ['A001'], LP_BOT: [],
    IC_TOP: ['A001'], IC_MID: [], IC_BOT: [],
  },
  unschedulableHoseIds: ['UNSCHED'],
}

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  qc.setQueryData(['vc', 'compat'], compat)
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>
}

describe('useDragValidation — TK-04-3-2', () => {
  it('validate — 적합 슬롯 ok=true', () => {
    const { result } = renderHook(() => useDragValidation(), { wrapper })
    expect(result.current.validate('A001', 'LP_UPMID').ok).toBe(true)
  })

  it('validate — 비적합 + reason + alternatives 3개', () => {
    const { result } = renderHook(() => useDragValidation(), { wrapper })
    const v = result.current.validate('A001', 'LP_TOP')
    expect(v.ok).toBe(false)
    expect(v.reason).toContain('A001')
    expect(v.reason).toContain('LP_TOP')
    expect(v.reason).toContain('BR-V13')
    expect(v.alternatives).toEqual(['LP_UPMID', 'LP_LOWMID', 'IC_TOP'])
  })

  it('validate — 매트릭스 미적재 품번 → Optimistic ok=true', () => {
    const { result } = renderHook(() => useDragValidation(), { wrapper })
    expect(result.current.validate('UNKNOWN', 'LP_TOP').ok).toBe(true)
  })

  it('isHoseUnschedulable — UNSCHED 식별', () => {
    const { result } = renderHook(() => useDragValidation(), { wrapper })
    expect(result.current.isHoseUnschedulable('UNSCHED')).toBe(true)
    expect(result.current.isHoseUnschedulable('A001')).toBe(false)
  })

  it('matrixVersion — 매트릭스 version 노출', () => {
    const { result } = renderHook(() => useDragValidation(), { wrapper })
    expect(result.current.matrixVersion).toBe(9)
  })
})
