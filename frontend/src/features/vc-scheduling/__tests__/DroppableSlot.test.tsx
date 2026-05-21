import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import { DndContext } from '@dnd-kit/core'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DroppableSlot } from '../components/DroppableSlot'
import type { CompatibilityResponse, SlotPosition } from '../types/slotCompat'

const compat: CompatibilityResponse = {
  version: 1,
  builtAt: '2026-05-21T00:00:00Z',
  byHose: {
    'A001': {
      LP_TOP: false, LP_UPMID: true, LP_LOWMID: true, LP_BOT: false,
      IC_TOP: true, IC_MID: false, IC_BOT: false,
    },
  },
  bySlot: {
    LP_TOP: [], LP_UPMID: ['A001'], LP_LOWMID: ['A001'], LP_BOT: [],
    IC_TOP: ['A001'], IC_MID: [], IC_BOT: [],
  },
  unschedulableHoseIds: [],
}

function withProviders(node: React.ReactNode, primeCache = true) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  if (primeCache) qc.setQueryData(['vc', 'compat'], compat)
  return (
    <ConfigProvider locale={koKR}>
      <QueryClientProvider client={qc}>
        <DndContext>{node}</DndContext>
      </QueryClientProvider>
    </ConfigProvider>
  )
}

describe('DroppableSlot — TK-04-3-1', () => {
  it('비드래그 상태 — 기본 dashed border 렌더', () => {
    const { getByTestId } = render(
      withProviders(<DroppableSlot slotPosition="LP_UPMID" />),
    )
    const node = getByTestId('droppable-slot-LP_UPMID')
    expect(node.getAttribute('data-hover')).toBe('false')
    expect(node.textContent).toContain('LP_UPMID')
  })

  it('drag aware data-eligible — 적합 슬롯 true', () => {
    const { getByTestId } = render(
      withProviders(<DroppableSlot slotPosition="LP_UPMID" draggedHoseId="A001" />),
    )
    expect(getByTestId('droppable-slot-LP_UPMID').getAttribute('data-eligible')).toBe('true')
  })

  it('drag aware data-eligible — 비적합 슬롯 false', () => {
    const { getByTestId } = render(
      withProviders(<DroppableSlot slotPosition="LP_TOP" draggedHoseId="A001" />),
    )
    expect(getByTestId('droppable-slot-LP_TOP').getAttribute('data-eligible')).toBe('false')
  })

  it('각 슬롯 위치 — 7 슬롯 모두 렌더', () => {
    const slots: SlotPosition[] = ['LP_TOP', 'LP_UPMID', 'LP_LOWMID', 'LP_BOT', 'IC_TOP', 'IC_MID', 'IC_BOT']
    slots.forEach((s) => {
      const { getByTestId } = render(withProviders(<DroppableSlot slotPosition={s} />))
      expect(getByTestId(`droppable-slot-${s}`)).toBeInTheDocument()
    })
  })

  it('매트릭스 미캐시 — Optimistic OK (data-eligible true)', () => {
    const { getByTestId } = render(
      withProviders(<DroppableSlot slotPosition="LP_TOP" draggedHoseId="A001" />, false),
    )
    // compat 미적재 → isEligible Optimistic true
    expect(getByTestId('droppable-slot-LP_TOP').getAttribute('data-eligible')).toBe('true')
  })

  it('children 콘텐츠 렌더 (배정된 카드)', () => {
    const { getByText } = render(
      withProviders(
        <DroppableSlot slotPosition="LP_UPMID">
          <span>29673-2F900 · 100EA</span>
        </DroppableSlot>,
      ),
    )
    expect(getByText('29673-2F900 · 100EA')).toBeInTheDocument()
  })
})
