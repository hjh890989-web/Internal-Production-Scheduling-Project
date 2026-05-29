import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import PinForceChangeModal from '../PinForceChangeModal'

function withProviders(node: React.ReactNode) {
  return <ConfigProvider locale={koKR}>{node}</ConfigProvider>
}

describe('PinForceChangeModal — Sprint 22 ST-SEC-2 강제 변경 (NFR-SEC-007)', () => {
  it('open=true → 노출 + 30일 정책 안내', () => {
    render(withProviders(<PinForceChangeModal open onSubmit={vi.fn()} />))
    expect(screen.getByText('PIN 변경 필요')).toBeInTheDocument()
    expect(screen.getByText(/30일마다 변경/)).toBeInTheDocument()
  })

  it('취소 불가 — 취소 버튼 미노출 + 닫기(X) 없음', () => {
    const { container } = render(withProviders(<PinForceChangeModal open onSubmit={vi.fn()} />))
    expect(screen.queryByRole('button', { name: '취소' })).not.toBeInTheDocument()
    // closable=false → 우상단 close(X) 아이콘 미존재
    expect(container.querySelector('.ant-modal-close')).toBeNull()
  })

  it('변경 성공 — 새 PIN + 확인 일치 시 onSubmit(newPin) 호출', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(withProviders(<PinForceChangeModal open onSubmit={onSubmit} />))

    const pins = document.querySelectorAll<HTMLInputElement>('input[type="password"]')
    fireEvent.change(pins[0]!, { target: { value: '5678' } })
    fireEvent.change(pins[1]!, { target: { value: '5678' } })
    fireEvent.click(screen.getByRole('button', { name: 'PIN 변경' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith('5678'))
  })

  it('확인 불일치 — onSubmit 미호출', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    render(withProviders(<PinForceChangeModal open onSubmit={onSubmit} />))

    const pins = document.querySelectorAll<HTMLInputElement>('input[type="password"]')
    fireEvent.change(pins[0]!, { target: { value: '5678' } })
    fireEvent.change(pins[1]!, { target: { value: '0000' } })
    fireEvent.click(screen.getByRole('button', { name: 'PIN 변경' }))

    await waitFor(() => expect(screen.getByText(/일치하지 않습니다/)).toBeInTheDocument())
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
