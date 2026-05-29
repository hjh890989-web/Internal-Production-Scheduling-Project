import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import VcMachineAdminPage from '../VcMachineAdminPage'
import { HttpError } from '@/api/client'
import * as vcMachineApiModule from '@/api/vcMachineApi'

// ─── helpers ────────────────────────────────────────────────────────────────

const MACHINES: vcMachineApiModule.VcMachineSummary[] = [
  { machineId: 'LP-01', machineType: 'LP', totalSlots: 8, dayRotations: 8, nightRotations: 10, active: true,  updatedAt: '2026-05-01T00:00:00Z', updatedBy: '00000001' },
  { machineId: 'LP-02', machineType: 'LP', totalSlots: 8, dayRotations: 8, nightRotations: 10, active: true,  updatedAt: '2026-05-01T00:00:00Z', updatedBy: '00000001' },
  { machineId: 'LP-03', machineType: 'LP', totalSlots: 8, dayRotations: 8, nightRotations: 10, active: true,  updatedAt: '2026-05-01T00:00:00Z', updatedBy: '00000001' },
  { machineId: 'LP-04', machineType: 'LP', totalSlots: 8, dayRotations: 8, nightRotations: 10, active: false, updatedAt: '2026-05-01T00:00:00Z', updatedBy: '00000002' },
  { machineId: 'IC-01', machineType: 'IC', totalSlots: 6, dayRotations: 8, nightRotations: 10, active: true,  updatedAt: '2026-05-01T00:00:00Z', updatedBy: '00000001' },
]

function withProviders(node: React.ReactNode) {
  return <ConfigProvider locale={koKR}>{node}</ConfigProvider>
}

// ─── suite ───────────────────────────────────────────────────────────────────

describe('VcMachineAdminPage — Sprint 21 ST-CRUD-1', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  // Case 1: READ — list() 모킹 → Table 5 row 렌더링 검증
  it('list() 모킹 → Table 에 5개 machine row 렌더링', async () => {
    vi.spyOn(vcMachineApiModule.vcMachineApi, 'list').mockResolvedValueOnce(MACHINES)

    render(withProviders(<VcMachineAdminPage />))

    await waitFor(() => {
      expect(screen.getByText('LP-01')).toBeInTheDocument()
      expect(screen.getByText('LP-02')).toBeInTheDocument()
      expect(screen.getByText('LP-03')).toBeInTheDocument()
      expect(screen.getByText('LP-04')).toBeInTheDocument()
      expect(screen.getByText('IC-01')).toBeInTheDocument()
    })
  })

  // Case 2: EDIT 저장 — PUT update() 호출 + reload 검증
  it('수정 Modal 저장 → vcMachineApi.update() 호출 + reload', async () => {
    const listSpy = vi.spyOn(vcMachineApiModule.vcMachineApi, 'list').mockResolvedValue(MACHINES)
    const updateSpy = vi.spyOn(vcMachineApiModule.vcMachineApi, 'update').mockResolvedValueOnce({
      machineId: 'LP-01',
      machineType: 'LP',
      totalSlots: 8,
      dayRotations: 9,
      nightRotations: 10,
      active: true,
      updatedAt: '2026-05-01T00:00:00Z',
      updatedBy: '00000001',
    })

    render(withProviders(<VcMachineAdminPage />))

    // 목록 로드 대기
    await waitFor(() => expect(screen.getByText('LP-01')).toBeInTheDocument())

    // LP-01 행의 수정 버튼 클릭
    const editButtons = screen.getAllByText('수정')
    fireEvent.click(editButtons[0]!)

    // Modal 열림 확인
    await waitFor(() => expect(screen.getByText(/가류기 수정 — LP-01/)).toBeInTheDocument())

    // 저장 버튼 클릭
    fireEvent.click(screen.getByText('저장'))

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith('LP-01', expect.objectContaining({
        dayRotations: 8,
        nightRotations: 10,
      }))
      // reload: list() 는 초기 1회 + reload 1회 = 최소 2회
      expect(listSpy).toHaveBeenCalledTimes(2)
    })
  })

  // Case 3: 비활성 toggle — DELETE 호출 + 안내 메시지 검증
  // TODO: Sprint 24 EP-OPS-FEEDBACK ST-FB-2 — AntD Popconfirm Portal jsdom 호환 (Phase 4 carry-over)
  it.skip('활성 기계 비활성화 Popconfirm 확인 → vcMachineApi.delete() 호출', async () => {
    vi.spyOn(vcMachineApiModule.vcMachineApi, 'list').mockResolvedValue(MACHINES)
    const deleteSpy = vi.spyOn(vcMachineApiModule.vcMachineApi, 'delete').mockResolvedValueOnce(undefined)

    render(withProviders(<VcMachineAdminPage />))

    await waitFor(() => expect(screen.getByText('LP-01')).toBeInTheDocument())

    // LP-01(active=true) 비활성화 버튼 클릭
    const deactivateButtons = screen.getAllByText('비활성화')
    fireEvent.click(deactivateButtons[0]!)

    // Popconfirm OK 클릭
    await waitFor(() => expect(screen.getByText('비활성화')).toBeInTheDocument())
    const popOkButtons = screen.getAllByText('비활성화')
    // Popconfirm 내 확인 버튼 (button role 중 마지막)
    const confirmBtn = popOkButtons[popOkButtons.length - 1]!
    fireEvent.click(confirmBtn)

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith('LP-01')
    })
  })

  // Case 4: 권한 없음 (403) — HttpError 던지면 "IT_OPS 권한 필요" 표시 검증
  it('list() 403 HttpError → "IT_OPS 권한 필요" 메시지 표시', async () => {
    vi.spyOn(vcMachineApiModule.vcMachineApi, 'list').mockRejectedValueOnce(
      new HttpError(403, { detail: 'Forbidden' }),
    )

    render(withProviders(<VcMachineAdminPage />))

    // AntD message.error 는 DOM 에 렌더 — waitFor 로 확인
    await waitFor(() => {
      expect(screen.getByText('IT_OPS 권한 필요')).toBeInTheDocument()
    })
  })
})
