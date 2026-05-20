import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import koKR from 'antd/locale/ko_KR'
import { FailureSummaryPanel } from '../components/FailureSummaryPanel'
import type { MappingFailure } from '../types/mapping'

function withProviders(node: React.ReactNode) {
  return <ConfigProvider locale={koKR}>{node}</ConfigProvider>
}

describe('FailureSummaryPanel — TK-01-2-2', () => {
  it('빈 failures — Empty 표시', () => {
    render(withProviders(<FailureSummaryPanel failures={[]} />))
    expect(screen.getByText(/실패 없음/)).toBeInTheDocument()
  })

  it('failedField 별 그룹 + 한국어 라벨', () => {
    const failures: MappingFailure[] = [
      {
        sheetName: 'Sheet1',
        rowIndex: 5,
        failedField: 'hose_id',
        reason: '품번 정규식 불일치',
        originalCells: ['xxx', '100'],
      },
      {
        sheetName: 'Sheet1',
        rowIndex: 7,
        failedField: 'delivery_date',
        reason: '날짜 포맷 미식별',
        originalCells: ['2026/02/xx'],
      },
      {
        sheetName: 'Sheet1',
        rowIndex: 8,
        failedField: 'qty',
        reason: '음수 불가',
        originalCells: ['-100'],
      },
    ]
    render(withProviders(<FailureSummaryPanel failures={failures} />))
    expect(screen.getByText('품번')).toBeInTheDocument()
    expect(screen.getByText('납기일')).toBeInTheDocument()
    expect(screen.getByText('수량')).toBeInTheDocument()
  })

  it('각 그룹 — 건수 Tag 표시', () => {
    const failures: MappingFailure[] = [
      { sheetName: 'S', rowIndex: 1, failedField: 'qty', reason: 'r1', originalCells: [] },
      { sheetName: 'S', rowIndex: 2, failedField: 'qty', reason: 'r2', originalCells: [] },
      { sheetName: 'S', rowIndex: 3, failedField: 'qty', reason: 'r3', originalCells: [] },
    ]
    render(withProviders(<FailureSummaryPanel failures={failures} />))
    expect(screen.getByText('3건')).toBeInTheDocument()
  })
})
