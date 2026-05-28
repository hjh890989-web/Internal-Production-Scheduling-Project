import { useMemo, useRef } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, GridReadyEvent } from 'ag-grid-community'
import { initAgGrid, agTheme } from '@/grid/agGridSetup'
import type { ExMatrixRow } from '../api/exMatrixApi'

interface Props {
  rows: ExMatrixRow[]
  loading?: boolean
}

/**
 * EX 매트릭스 AG Grid 컴포넌트 — TK-17-1-2 (EP-17 ST-17-1).
 *
 * <p>row = candidate, col = hose / deadline / yield / status. Virtual scrolling +
 * column filter + status statusBar. AG Grid Enterprise 라이센스는 {@code agGridSetup}.
 */
export function ExMatrixGrid({ rows, loading }: Props) {
  initAgGrid()

  const gridRef = useRef<AgGridReact<ExMatrixRow>>(null)

  const columnDefs: ColDef<ExMatrixRow>[] = useMemo(
    () => [
      { field: 'hoseId', headerName: '품번', filter: 'agTextColumnFilter', minWidth: 140 },
      { field: 'vcProductionDate', headerName: '성형 투입일', filter: 'agDateColumnFilter' },
      { field: 'extrusionDeadline', headerName: '압출 deadline (BR-E01)', filter: 'agDateColumnFilter' },
      {
        field: 'vcYield',
        headerName: '회전 yield',
        type: 'numericColumn',
        filter: 'agNumberColumnFilter',
        cellStyle: (p) => {
          if (p.value === 2531) return { color: '#0050b3', fontWeight: 600 } // BR-E05 reference
          return undefined
        },
      },
      {
        field: 'status',
        headerName: '상태',
        filter: 'agTextColumnFilter',
        cellStyle: (p) => {
          switch (p.value) {
            case 'CONFIRMED': return { color: '#389e0d' }
            case 'SCHEDULED': return { color: '#1677ff' }
            case 'FAILED':    return { color: '#cf1322' }
            case 'PENDING':   return { color: '#8c8c8c' }
            default:          return undefined
          }
        },
      },
    ],
    [],
  )

  const defaultColDef: ColDef = useMemo(
    () => ({
      sortable: true,
      resizable: true,
      flex: 1,
      minWidth: 100,
    }),
    [],
  )

  const onGridReady = (event: GridReadyEvent) => {
    event.api.sizeColumnsToFit()
  }

  return (
    <div style={{ height: 600, width: '100%' }}>
      <AgGridReact<ExMatrixRow>
        ref={gridRef}
        theme={agTheme}
        rowData={rows}
        columnDefs={columnDefs}
        defaultColDef={defaultColDef}
        rowSelection="single"
        animateRows
        loading={loading}
        onGridReady={onGridReady}
      />
    </div>
  )
}
