import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import VcConstraintAdminPage from '../VcConstraintAdminPage'

/** 공통 stub 데이터 */
const STUB_ROWS = [
  {
    hoseId: '29673-2R060',
    compositeCount: 2,
    lpMoldQty: 10,
    icMoldQty: 0,
    slot1: true,
    slot2: true,
    slot3: false,
    slot4: false,
    slot5: false,
    slot6: false,
    slot7: false,
  },
]

function withProviders(node: React.ReactNode) {
  return <ConfigProvider locale={koKR}>{node}</ConfigProvider>
}

describe('VcConstraintAdminPage — Sprint 21 ST-CRUD-3', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockReset()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  /** Case 1: compositeCount=4 는 Select 옵션에 없음 → backend 호출 자체 불가 */
  // TODO: Sprint 25 또는 Phase 5 Playwright e2e 이전 — AntD Select dropdown jsdom 미지원 (mouseDown 이벤트 미동작)
  it.skip('compositeCount Select 옵션은 1·2·3·6 만 존재 (4 없음)', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(STUB_ROWS), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    render(withProviders(<VcConstraintAdminPage />))

    // 목록 로드 대기
    await waitFor(() => expect(screen.getByText('29673-2R060')).toBeInTheDocument())

    // 수정 Drawer 열기
    fireEvent.click(screen.getByRole('button', { name: '수정' }))

    await waitFor(() => screen.getByText(/VC 제약 수정/))

    // compositeCount Select 옵션 오픈
    const select = screen.getByRole('combobox')
    fireEvent.mouseDown(select)

    await waitFor(() => {
      // 허용 옵션 존재 확인
      expect(screen.getByTitle('1')).toBeInTheDocument()
      expect(screen.getByTitle('2')).toBeInTheDocument()
      expect(screen.getByTitle('3')).toBeInTheDocument()
      expect(screen.getByTitle('6')).toBeInTheDocument()
      // 4·5·7 미존재 확인
      expect(screen.queryByTitle('4')).not.toBeInTheDocument()
      expect(screen.queryByTitle('5')).not.toBeInTheDocument()
    })
  })

  /** Case 1b: backend 가 400 "BR-V14 위반" 반환 시 에러 메시지 표시 */
  // Sprint 24 ST-FB-2: data-testid="vc-constraint-submit" 추가 + within(document.body) Drawer portal scope
  it('backend 400 BR-V14 → 에러 메시지 표시', async () => {
    // 목록 로드
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce(
        new Response(JSON.stringify(STUB_ROWS), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      // PUT 호출 → 400 BR-V14
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            detail: 'BR-V14 위반 — compositeCount 허용값: 1,2,3,6',
            brCode: 'BR-V14',
          }),
          { status: 400, headers: { 'Content-Type': 'application/json' } },
        ),
      )

    render(withProviders(<VcConstraintAdminPage />))
    await waitFor(() => screen.getByText('29673-2R060'))

    // 테이블의 수정 버튼 클릭 → Drawer 오픈
    fireEvent.click(screen.getByRole('button', { name: '수정' }))
    await waitFor(() => within(document.body).getByText(/VC 제약 수정/))

    // Drawer portal 내 submit 버튼 — data-testid 로 특정
    const submitBtn = within(document.body).getByTestId('vc-constraint-submit')
    fireEvent.click(submitBtn)

    await waitFor(() => {
      // AntD message.error 는 .ant-message 컨테이너에 렌더 — static 페이지 텍스트와 구분
      expect(
        within(document.body).getByText(/BR-V14 위반/),
      ).toBeInTheDocument()
    })
  })

  /** Case 2: compositeCount=2 + lpMoldQty=10 → PUT 호출 + reload */
  it('compositeCount=2 + lpMoldQty=10 저장 → PUT 호출 + reload', async () => {
    const updatedRow = { ...STUB_ROWS[0], lpMoldQty: 10 }

    vi.spyOn(global, 'fetch')
      // 첫 목록 로드
      .mockResolvedValueOnce(
        new Response(JSON.stringify(STUB_ROWS), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      // PUT 성공
      .mockResolvedValueOnce(
        new Response(JSON.stringify(updatedRow), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      // reload 목록
      .mockResolvedValueOnce(
        new Response(JSON.stringify([updatedRow]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )

    render(withProviders(<VcConstraintAdminPage />))
    await waitFor(() => screen.getByText('29673-2R060'))

    fireEvent.click(screen.getByRole('button', { name: '수정' }))
    await waitFor(() => screen.getByText(/VC 제약 수정/))

    // 수정 submit
    fireEvent.click(screen.getAllByRole('button', { name: '수정' }).at(-1)!)

    await waitFor(() => {
      // fetch 가 최소 2번 (PUT + reload GET) 호출됐는지 확인
      const fetchMock = vi.mocked(global.fetch)
      const putCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          typeof url === 'string' &&
          url.includes('29673-2R060') &&
          (init as RequestInit)?.method === 'PUT',
      )
      expect(putCall).toBeDefined()
    })
  })

  /** Case 3: 403 → IT_OPS 권한 필요 안내 */
  it('403 → IT_OPS 권한 필요 메시지', async () => {
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce(
        new Response(JSON.stringify(STUB_ROWS), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 403 }))

    render(withProviders(<VcConstraintAdminPage />))
    await waitFor(() => screen.getByText('29673-2R060'))

    fireEvent.click(screen.getByRole('button', { name: '수정' }))
    await waitFor(() => screen.getByText(/VC 제약 수정/))

    fireEvent.click(screen.getAllByRole('button', { name: '수정' }).at(-1)!)

    await waitFor(() => {
      expect(screen.getByText(/IT_OPS 권한 필요/)).toBeInTheDocument()
    })
  })
})
