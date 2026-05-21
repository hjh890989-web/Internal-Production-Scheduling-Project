import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import { ViolationModal } from '../components/ViolationModal'
import type { ViolationInfo } from '../types/violation'

function withProviders(node: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return (
    <ConfigProvider locale={koKR}>
      <QueryClientProvider client={qc}>{node}</QueryClientProvider>
    </ConfigProvider>
  )
}

const violation: ViolationInfo = {
  hoseId: 'A001',
  qty: 100,
  slot: 'LP_TOP',
  reason: '품번 A001 는 LP_TOP 슬롯에 적합하지 않습니다 (BR-V13)',
  alternatives: ['LP_UPMID', 'LP_LOWMID', 'IC_TOP'],
}

describe('ViolationModal — TK-04-3-2', () => {
  it('violation null → 미렌더', () => {
    const { container } = render(
      withProviders(
        <ViolationModal violation={null} onClose={() => {}} onOverride={() => {}} />,
      ),
    )
    expect(container.textContent).not.toContain('슬롯 적합성 위반')
  })

  it('사유 + 대안 슬롯 한국어 디스플레이 + override 버튼 노출', () => {
    render(
      withProviders(
        <ViolationModal violation={violation} onClose={() => {}} onOverride={() => {}} />,
      ),
    )
    expect(screen.getByText(/슬롯 적합성 위반/)).toBeInTheDocument()
    expect(screen.getByText(violation.reason)).toBeInTheDocument()
    // 대안 한국어 라벨
    expect(screen.getByText('LP 중상')).toBeInTheDocument()
    expect(screen.getByText('LP 중하')).toBeInTheDocument()
    expect(screen.getByText('IC 상단')).toBeInTheDocument()
    expect(screen.getByTestId('violation-override-trigger')).toBeInTheDocument()
  })

  it('대안 0건 — BR-V11 Unschedulable 안내', () => {
    const noAlts: ViolationInfo = { ...violation, alternatives: [] }
    render(
      withProviders(<ViolationModal violation={noAlts} onClose={() => {}} onOverride={() => {}} />),
    )
    expect(screen.getByText(/BR-V11 Unschedulable/)).toBeInTheDocument()
  })

  it('취소 버튼 → onClose 호출', () => {
    const onClose = vi.fn()
    render(
      withProviders(
        <ViolationModal violation={violation} onClose={onClose} onOverride={() => {}} />,
      ),
    )
    fireEvent.click(screen.getByTestId('violation-cancel'))
    expect(onClose).toHaveBeenCalled()
  })

  it('override 트리거 → OverrideJustificationForm 노출 (사유 입력 textarea)', () => {
    render(
      withProviders(
        <ViolationModal violation={violation} onClose={() => {}} onOverride={() => {}} />,
      ),
    )
    fireEvent.click(screen.getByTestId('violation-override-trigger'))
    expect(screen.getByTestId('override-form')).toBeInTheDocument()
    expect(screen.getByLabelText('강제 배치 사유')).toBeInTheDocument()
  })
})
