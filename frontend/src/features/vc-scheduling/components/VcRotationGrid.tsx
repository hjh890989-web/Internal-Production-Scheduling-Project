import { useMemo, useRef } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, GridReadyEvent, ValueGetterParams } from 'ag-grid-enterprise'
import 'ag-grid-enterprise/styles/ag-grid.css'
import 'ag-grid-enterprise/styles/ag-theme-quartz.css'
import { initAgGridEnterprise } from '@/grid/agGridSetup'
import type { VcSlotRow } from '../api/vcScheduleApi'

interface Props {
  rows: VcSlotRow[]
  loading?: boolean
}

interface PivotRow {
  date: string
  machineSlot: string // 'LP-01·1' 형태 (machineId·slotPosition)
  /** rotation 1~18 → hose_id 또는 빈 셀 */
  [key: `r${number}`]: string | undefined
}

/**
 * VC 회전 격자 AG Grid — TK-15-1-1 (EP-15 ST-15-1, BR-V04 1~18 회전).
 *
 * <p>row = (date, machineSlot), col = r1...r18, cell = hoseId.
 * BR-V07 일중 락 시각화 — 같은 row 의 모든 cell 이 같은 angle 인 것을 한눈에 확인.
 */
export function VcRotationGrid({ rows, loading }: Props) {
  initAgGridEnterprise()

  const gridRef = useRef<AgGridReact<PivotRow>>(null)

  const pivoted = useMemo<PivotRow[]>(() => {
    const map = new Map<string, PivotRow>()
    for (const r of rows) {
      const key = `${r.productionDate}|${r.machineId}|${r.slotPosition}`
      let row = map.get(key)
      if (!row) {
        row = {
          date: r.productionDate,
          machineSlot: `${r.machineId}·${r.slotPosition}`,
        }
        map.set(key, row)
      }
      row[`r${r.rotationNo}` as `r${number}`] = r.hoseId
    }
    return Array.from(map.values()).sort((a, b) => {
      const d = a.date.localeCompare(b.date)
      return d !== 0 ? d : a.machineSlot.localeCompare(b.machineSlot)
    })
  }, [rows])

  const columnDefs: ColDef<PivotRow>[] = useMemo(() => {
    const cols: ColDef<PivotRow>[] = [
      { field: 'date', headerName: '일자', pinned: 'left', minWidth: 110, filter: 'agDateColumnFilter' },
      { field: 'machineSlot', headerName: '머신·슬롯', pinned: 'left', minWidth: 110 },
    ]
    for (let r = 1; r <= 18; r++) {
      cols.push({
        headerName: r <= 8 ? `D${r}` : `N${r - 8}`,   // BR-V04 주간 D1-8 + 야간 N1-10
        colId: `r${r}`,
        valueGetter: (p: ValueGetterParams<PivotRow>) =>
          p.data?.[`r${r}` as `r${number}`] ?? '',
        minWidth: 80,
        cellStyle: { textAlign: 'center', fontSize: 11 },
      })
    }
    return cols
  }, [])

  const defaultColDef: ColDef = useMemo(
    () => ({
      sortable: true,
      resizable: true,
    }),
    [],
  )

  const onGridReady = (event: GridReadyEvent) => {
    event.api.sizeColumnsToFit()
  }

  return (
    <div className="ag-theme-quartz" style={{ height: 600, width: '100%' }}>
      <AgGridReact<PivotRow>
        ref={gridRef}
        rowData={pivoted}
        columnDefs={columnDefs}
        defaultColDef={defaultColDef}
        rowSelection="single"
        animateRows
        loading={loading}
        statusBar={{
          statusPanels: [
            { statusPanel: 'agTotalAndFilteredRowCountComponent', align: 'left' },
            { statusPanel: 'agAggregationComponent', align: 'right' },
          ],
        }}
        onGridReady={onGridReady}
      />
    </div>
  )
}
