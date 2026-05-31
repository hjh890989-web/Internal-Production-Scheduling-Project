import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import SettingGroupAdminPage from '../SettingGroupAdminPage'
import { openPopconfirmAndConfirm } from '@/test-utils/antdHelpers'

const mockRows = [
  { groupId: 1, displayName: '그룹 A', active: true },
  { groupId: 2, displayName: '그룹 B', active: true },
  { groupId: 3, displayName: '그룹 C', active: false },
  { groupId: 4, displayName: '그룹 D', active: true },
  { groupId: 5, displayName: '그룹 E', active: true },
  { groupId: 6, displayName: '그룹 F', active: false },
  { groupId: 7, displayName: '그룹 G', active: true },
  { groupId: 8, displayName: '그룹 H', active: true },
]

function withProviders(node: React.ReactNode) {
  return <ConfigProvider locale={koKR}>{node}</ConfigProvider>
}

describe('SettingGroupAdminPage — Sprint 21 ST-CRUD-2', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockReset()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  /**
   * Case 1: READ — list() → Table 8 row 렌더링
   */
  it('list() 호출 → Table 에 8 row 표시', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(mockRows), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    render(withProviders(<SettingGroupAdminPage />))

    await waitFor(() => {
      expect(screen.getByText('그룹 A')).toBeInTheDocument()
      expect(screen.getByText('그룹 H')).toBeInTheDocument()
    })

    // 8 row — Group ID 1~8 모두 렌더
    for (let i = 1; i <= 8; i++) {
      expect(screen.getByText(String(i))).toBeInTheDocument()
    }
  })

  /**
   * Case 2: EDIT 저장 — PUT + reload
   * Sprint 24 ST-FB-2: within(document.body) 로 Modal portal scope 해결
   */
  it('수정 Modal 저장 → PUT 호출 후 목록 재조회', async () => {
    // 초기 list
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce(
        new Response(JSON.stringify(mockRows), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      // PUT 응답
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ groupId: 1, displayName: '그룹 A 수정', active: true }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )
      // reload list
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify([
            ...mockRows.slice(1),
            { groupId: 1, displayName: '그룹 A 수정', active: true },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )

    render(withProviders(<SettingGroupAdminPage />))

    await waitFor(() => expect(screen.getByText('그룹 A')).toBeInTheDocument())

    // 첫 번째 수정 버튼 클릭
    const editButtons = screen.getAllByText('수정')
    fireEvent.click(editButtons[0]!)

    // Modal은 document.body portal에 렌더 — within(document.body)로 scope
    await waitFor(() =>
      expect(within(document.body).getByTestId('edit-display-name')).toBeInTheDocument(),
    )

    const editInput = within(document.body).getByTestId('edit-display-name')
    fireEvent.change(editInput, { target: { value: '그룹 A 수정' } })

    // submit 버튼 — document.body 내 type="submit" button
    const submitBtn = within(document.body).getAllByRole('button', { name: '수정' }).find(
      (btn) => btn.getAttribute('type') === 'submit',
    )!
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/master/setting-groups/1'),
        expect.objectContaining({ method: 'PUT' }),
      )
    })

    // reload 후 수정된 displayName 확인
    await waitFor(() => expect(screen.getByText('그룹 A 수정')).toBeInTheDocument())
  })

  /**
   * Case 3: 비활성 toggle — DELETE + 안내
   * Sprint 24 ST-FB-2: openPopconfirmAndConfirm helper 로 portal scope 해결
   */
  it('비활성화 Popconfirm 확인 → DELETE 호출 + 성공 메시지', async () => {
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce(
        new Response(JSON.stringify(mockRows), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      // DELETE 응답 (204)
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      // reload list
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify(
            mockRows.map(r => (r.groupId === 1 ? { ...r, active: false } : r)),
          ),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )

    render(withProviders(<SettingGroupAdminPage />))

    await waitFor(() => expect(screen.getByText('그룹 A')).toBeInTheDocument())

    // openPopconfirmAndConfirm: 트리거("비활성화") 클릭 → popup 내 OK("비활성화") 클릭
    await openPopconfirmAndConfirm('비활성화', '비활성화')

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/master/setting-groups/1'),
        expect.objectContaining({ method: 'DELETE' }),
      )
    })
  })
})
