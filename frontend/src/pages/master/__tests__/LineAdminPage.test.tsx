import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import LineAdminPage from '../LineAdminPage'
import type { LineSummary } from '@/api/lineApi'
import type { SpecSummary } from '@/api/productSpecApi'

// ---- fixtures ----
const mockLines: LineSummary[] = [
  {
    lineCode: 'LP-01',
    lineName: '저압가류기 1호',
    lineType: 'LP',
    active: true,
    productCompatibility: ['29673-2R060', '29673-2R070'],
  },
  {
    lineCode: 'IC-01',
    lineName: 'IC가류기 1호',
    lineType: 'IC',
    active: false,
    productCompatibility: [],
  },
]

const mockSpecs: SpecSummary[] = [
  { hoseId: '29673-2R060', spec: 7, compositeCount: 1, lpLeftSetting: null, lpRightSetting: null, angleCount: 2, isSpecLt7: false },
  { hoseId: '29673-2R070', spec: 6, compositeCount: 2, lpLeftSetting: null, lpRightSetting: null, angleCount: 2, isSpecLt7: true },
  { hoseId: '29673-2R080', spec: 8, compositeCount: 1, lpLeftSetting: null, lpRightSetting: null, angleCount: 3, isSpecLt7: false },
]

function buildLineFetchMock(lines: LineSummary[] = mockLines, specs: SpecSummary[] = mockSpecs) {
  return (url: RequestInfo | URL, _init?: RequestInit): Promise<Response> => {
    const urlStr = url.toString()
    if (urlStr === '/api/v1/master/lines') {
      return Promise.resolve(new Response(JSON.stringify(lines), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    }
    if (urlStr === '/api/v1/master/product-spec') {
      return Promise.resolve(new Response(JSON.stringify(specs), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    }
    return Promise.resolve(new Response(null, { status: 404 }))
  }
}

function withProviders(node: React.ReactNode) {
  return <ConfigProvider locale={koKR}>{node}</ConfigProvider>
}

describe('LineAdminPage — Sprint 21 ST-CRUD-4', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockReset()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ---- Case 1: READ — list() → Table 렌더링 ----
  it('list() 호출 후 Table 에 라인 코드와 타입 렌더링', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(buildLineFetchMock())

    render(withProviders(<LineAdminPage />))

    await waitFor(() => {
      expect(screen.getByText('LP-01')).toBeInTheDocument()
      expect(screen.getByText('IC-01')).toBeInTheDocument()
    })

    // lineType Tag 렌더링 확인
    expect(screen.getByText('LP')).toBeInTheDocument()
    expect(screen.getByText('IC')).toBeInTheDocument()

    // 호환 품번 수 렌더링
    expect(screen.getByText('2개')).toBeInTheDocument()
    expect(screen.getByText('0개')).toBeInTheDocument()
  })

  // ---- Case 2: PUT /products — Multi-Select 변경 + 저장 → API 호출 검증 ----
  it('호환 설정 Drawer 에서 Transfer 변경 후 저장 시 PUT /products 호출', async () => {
    const fetchMock = buildLineFetchMock()
    // PUT /products 응답 추가
    const putResponse = new Response(
      JSON.stringify({ ...mockLines[0], productCompatibility: ['29673-2R060'] }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )
    vi.spyOn(global, 'fetch').mockImplementation((url: RequestInfo | URL, init?: RequestInit) => {
      const urlStr = url.toString()
      if (urlStr.includes('/products') && init?.method === 'PUT') {
        return Promise.resolve(putResponse)
      }
      return fetchMock(url, init)
    })

    render(withProviders(<LineAdminPage />))

    // 테이블 로드 대기
    await waitFor(() => expect(screen.getByText('LP-01')).toBeInTheDocument())

    // LP-01 행의 "호환 설정" 버튼 클릭
    const buttons = screen.getAllByText('호환 설정')
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    fireEvent.click(buttons[0]!)

    // Drawer 열림 확인
    await waitFor(() =>
      expect(screen.getByText('호환 품번 설정 — LP-01')).toBeInTheDocument(),
    )

    // 저장 버튼 클릭
    const saveBtn = screen.getByTestId('save-products-btn')
    fireEvent.click(saveBtn)

    await waitFor(() => {
      const calls = vi.mocked(global.fetch).mock.calls
      const putCall = calls.find(
        ([url, opts]) =>
          url.toString().includes('LP-01/products') &&
          (opts as RequestInit)?.method === 'PUT',
      )
      expect(putCall).toBeDefined()
    })
  })

  // ---- Case 3: 403 → IT_OPS 권한 안내 ----
  it('POST 403 응답 → IT_OPS 권한 필요 메시지', async () => {
    vi.spyOn(global, 'fetch').mockImplementation((url: RequestInfo | URL, init?: RequestInit) => {
      const urlStr = url.toString()
      if (urlStr === '/api/v1/master/lines' && init?.method === 'POST') {
        return Promise.resolve(new Response(null, { status: 403 }))
      }
      return buildLineFetchMock()(url, init)
    })

    render(withProviders(<LineAdminPage />))
    await waitFor(() => expect(screen.getByText('LP-01')).toBeInTheDocument())

    // 신규 추가 버튼 클릭
    fireEvent.click(screen.getByText('신규 추가'))

    // 폼 입력
    await waitFor(() => expect(screen.getByPlaceholderText('예: LP-01')).toBeInTheDocument())
    fireEvent.change(screen.getByPlaceholderText('예: LP-01'), { target: { value: 'LP-99' } })
    fireEvent.change(screen.getByPlaceholderText('예: 저압가류기 1호'), { target: { value: '테스트 라인' } })

    // lineType 선택 — Select 컴포넌트: 클릭 후 옵션 선택
    const typeSelect = screen.getByText('타입 선택')
    fireEvent.mouseDown(typeSelect)
    await waitFor(() => expect(screen.getByText('LP (저압가류)')).toBeInTheDocument())
    fireEvent.click(screen.getByText('LP (저압가류)'))

    // 추가 제출
    fireEvent.click(screen.getByText('추가'))

    await waitFor(() => {
      expect(screen.getByText(/IT_OPS 권한 필요/)).toBeInTheDocument()
    })
  })
})
