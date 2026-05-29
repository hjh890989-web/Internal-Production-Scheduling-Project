import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import SettingGroupAdminPage from '../SettingGroupAdminPage'

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
   */
  // TODO: Sprint 24 EP-OPS-FEEDBACK ST-FB-2 — AntD Modal Portal jsdom 호환 (Phase 4 carry-over)
  it.skip('수정 Modal 저장 → PUT 호출 후 목록 재조회', async () => {
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

    await waitFor(() => expect(screen.getByTestId('edit-display-name')).toBeInTheDocument())

    const editInput = screen.getByTestId('edit-display-name')
    fireEvent.change(editInput, { target: { value: '그룹 A 수정' } })

    fireEvent.click(screen.getByText('수정', { selector: 'button[type="submit"]' }))

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
   */
  // TODO: Sprint 24 EP-OPS-FEEDBACK ST-FB-2 — AntD Popconfirm Portal jsdom 호환 (Phase 4 carry-over)
  it.skip('비활성화 Popconfirm 확인 → DELETE 호출 + 성공 메시지', async () => {
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

    // 첫 번째 활성 그룹의 비활성화 버튼 클릭
    const deactivateButtons = screen.getAllByText('비활성화')
    fireEvent.click(deactivateButtons[0]!)

    // Popconfirm 확인 버튼 클릭
    await waitFor(() => {
      const popconfirmOk = screen.getAllByText('비활성화').find(
        el => el.closest('button')?.className.includes('ant-btn-dangerous'),
      )
      if (popconfirmOk) fireEvent.click(popconfirmOk)
    })

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/master/setting-groups/1'),
        expect.objectContaining({ method: 'DELETE' }),
      )
    })
  })
})
