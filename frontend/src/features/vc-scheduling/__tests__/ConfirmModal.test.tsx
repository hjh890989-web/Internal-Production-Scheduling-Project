import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import { ConfirmModal, type ConfirmTarget } from '../components/ConfirmModal'

const target: ConfirmTarget = {
  vcScheduleId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  hoseId: '29673-2R060',
  machineId: 'LP-01',
  productionDate: '2026-06-01',
  rotationNo: 5,
  slotPosition: 1,
  plannedQty: 100,
}

function withProviders(node: React.ReactNode) {
  return <ConfigProvider locale={koKR}>{node}</ConfigProvider>
}

describe('ConfirmModal — Sprint 16 EP-CONFIRM TK-CONFIRM-5-3 분기 메시지', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockReset()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('target null → 미렌더', () => {
    const { container } = render(
      withProviders(
        <ConfirmModal target={null} open={false} onClose={() => {}} onSuccess={() => {}} />,
      ),
    )
    expect(container.textContent).not.toContain('VC schedule 확정')
  })

  it('Modal 표시 + dual-review 안내 + 대상 정보 노출', () => {
    render(
      withProviders(
        <ConfirmModal target={target} open onClose={() => {}} onSuccess={() => {}} />,
      ),
    )
    expect(screen.getByText(/VC schedule 확정/)).toBeInTheDocument()
    expect(screen.getByText(/dual-review 안내/)).toBeInTheDocument()
    expect(screen.getByText('29673-2R060')).toBeInTheDocument()
    expect(screen.getByText(/LP-01 \/ slot 1/)).toBeInTheDocument()
  })

  it('200 OK → onSuccess 콜백 호출', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          vcScheduleId: target.vcScheduleId,
          status: 'CONFIRMED',
          confirmedBy: '00000002',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    const onSuccess = vi.fn()
    render(
      withProviders(
        <ConfirmModal target={target} open onClose={() => {}} onSuccess={onSuccess} />,
      ),
    )
    fireEvent.click(screen.getByTestId('confirm-ok'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1))
  })

  it('409 BR-X05 → dual-review 안내 메시지 노출', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          detail: 'BR-X05 dual-review 위반: ...',
          brCode: 'BR-X05',
        }),
        { status: 409, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    render(
      withProviders(
        <ConfirmModal target={target} open onClose={() => {}} onSuccess={() => {}} />,
      ),
    )
    fireEvent.click(screen.getByTestId('confirm-ok'))
    await waitFor(() => {
      expect(screen.getByTestId('confirm-error')).toBeInTheDocument()
      expect(screen.getByText(/dual-review — 확정 불가/)).toBeInTheDocument()
      expect(screen.getByText(/본인이 작성한 row 는 확정할 수 없습니다/)).toBeInTheDocument()
    })
  })

  it('423 BR-X07 → D-2 hard 제약 안내', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          detail: 'BR-X07 D-2 hard 제약 위반: production_date=...',
          brCode: 'BR-X07',
        }),
        { status: 423, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    render(
      withProviders(
        <ConfirmModal target={target} open onClose={() => {}} onSuccess={() => {}} />,
      ),
    )
    fireEvent.click(screen.getByTestId('confirm-ok'))
    await waitFor(() => {
      expect(screen.getByTestId('confirm-error')).toBeInTheDocument()
      expect(screen.getAllByText(/BR-X07 D-2 hard 제약/).length).toBeGreaterThan(0)
    })
  })

  it('409 BR-X01 → 이미 CONFIRMED 안내', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          detail: 'BR-X01 confirm 전이는 CANDIDATE 에서만',
          brCode: 'BR-X01',
        }),
        { status: 409, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    render(
      withProviders(
        <ConfirmModal target={target} open onClose={() => {}} onSuccess={() => {}} />,
      ),
    )
    fireEvent.click(screen.getByTestId('confirm-ok'))
    await waitFor(() => {
      expect(screen.getByText(/이미 확정된 row/)).toBeInTheDocument()
    })
  })

  it('403 → 권한 없음 안내', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce(
      new Response(null, { status: 403 }),
    )
    render(
      withProviders(
        <ConfirmModal target={target} open onClose={() => {}} onSuccess={() => {}} />,
      ),
    )
    fireEvent.click(screen.getByTestId('confirm-ok'))
    await waitFor(() => {
      expect(screen.getByText(/권한 없음/)).toBeInTheDocument()
      expect(screen.getByText(/PLANNER 권한이 필요합니다/)).toBeInTheDocument()
    })
  })

  it('취소 버튼 → onClose 호출', () => {
    const onClose = vi.fn()
    render(
      withProviders(
        <ConfirmModal target={target} open onClose={onClose} onSuccess={() => {}} />,
      ),
    )
    fireEvent.click(screen.getByTestId('confirm-cancel'))
    expect(onClose).toHaveBeenCalled()
  })
})
