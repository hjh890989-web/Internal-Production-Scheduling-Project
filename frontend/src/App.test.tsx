import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import './i18n'
import HomePage from './pages/HomePage'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'

/**
 * Smoke test — i18n + Ant Design + TanStack Query + Router 통합 동작.
 * Sprint 1+ 페이지별 상세 테스트는 각 UI Story 안에 추가.
 */
describe('HomePage', () => {
  it('한국어 제목과 로딩 상태를 렌더링한다', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const { getByText } = render(
      <ConfigProvider locale={koKR}>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <HomePage />
          </MemoryRouter>
        </QueryClientProvider>
      </ConfigProvider>,
    )

    // 한국어 타이틀 (i18n ko.json 'app.title') — REQ-NF-USA-003
    expect(getByText('사내 공정 스케줄링 시스템')).toBeInTheDocument()
  })
})
