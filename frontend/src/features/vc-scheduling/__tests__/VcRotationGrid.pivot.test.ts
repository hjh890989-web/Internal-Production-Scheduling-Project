import { describe, it, expect } from 'vitest'
import type { VcSlotRow } from '../api/vcScheduleApi'

/**
 * VcRotationGrid pivot 로직 — TK-15-1-1 (BR-V04 rotation 1~18).
 *
 * <p>row = (date, machineId, slotPosition), col = r1..r18 = hoseId.
 * 본 테스트는 컴포넌트 외부에서 같은 로직을 재현해 정합 검증.
 */

interface PivotRow {
  date: string
  machineSlot: string
  [key: string]: string | undefined
}

function pivot(rows: VcSlotRow[]): PivotRow[] {
  const map = new Map<string, PivotRow>()
  for (const r of rows) {
    const key = `${r.productionDate}|${r.machineId}|${r.slotPosition}`
    let row = map.get(key)
    if (!row) {
      row = { date: r.productionDate, machineSlot: `${r.machineId}·${r.slotPosition}` }
      map.set(key, row)
    }
    row[`r${r.rotationNo}`] = r.hoseId
  }
  return Array.from(map.values()).sort((a, b) => {
    const d = a.date.localeCompare(b.date)
    return d !== 0 ? d : a.machineSlot.localeCompare(b.machineSlot)
  })
}

const T0 = '2026-06-01'

function row(
  rotation: number,
  hoseId: string,
  machineId = 'LP-01',
  slotPosition = 1,
  productionDate = T0,
): VcSlotRow {
  return {
    vcScheduleId: `uuid-${rotation}`,
    hoseId,
    machineId,
    slotPosition,
    productionDate,
    rotationNo: rotation,
    angleId: 'ANG-A',
    plannedQty: 100,
    status: 'CANDIDATE',
  }
}

describe('VcRotationGrid pivot', () => {
  it('같은 (date, machine, slot) 의 18 rotation → 1 row 18 column', () => {
    const input: VcSlotRow[] = []
    for (let r = 1; r <= 18; r++) input.push(row(r, '29673-2R060'))

    const pivoted = pivot(input)
    expect(pivoted).toHaveLength(1)
    const first = pivoted[0]!
    expect(first.date).toBe(T0)
    expect(first.machineSlot).toBe('LP-01·1')
    for (let r = 1; r <= 18; r++) {
      expect(first[`r${r}`]).toBe('29673-2R060')
    }
  })

  it('서로 다른 (machine, slot) → 2 row', () => {
    const pivoted = pivot([
      row(5, 'A', 'LP-01', 1),
      row(5, 'B', 'LP-02', 1),
    ])
    expect(pivoted).toHaveLength(2)
    expect(pivoted.map((p) => p.machineSlot)).toEqual(['LP-01·1', 'LP-02·1'])
  })

  it('정렬 — date asc + machineSlot asc', () => {
    const pivoted = pivot([
      row(1, 'X', 'LP-02', 1, '2026-06-02'),
      row(1, 'Y', 'LP-01', 1, '2026-06-01'),
      row(1, 'Z', 'LP-01', 2, '2026-06-01'),
    ])
    expect(pivoted.map((p) => `${p.date}|${p.machineSlot}`)).toEqual([
      '2026-06-01|LP-01·1',
      '2026-06-01|LP-01·2',
      '2026-06-02|LP-02·1',
    ])
  })

  it('빈 입력 → 빈 결과', () => {
    expect(pivot([])).toEqual([])
  })
})
