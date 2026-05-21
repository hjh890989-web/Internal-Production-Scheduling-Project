import { useSlotCompatibilityMatrix } from './useSlotCompatibilityMatrix'
import { isEligible, type SlotPosition } from '../types/slotCompat'
import type { ValidationResult } from '../types/violation'

/**
 * 드래그 중 (hose_id, slot) 적합성 검증 — TK-04-3-1 + TK-04-3-2.
 *
 * <p>매트릭스 미로딩 / 매트릭스 미적재 품번 — Optimistic OK (UX 보호).
 * 실제 저장 차단 + override 모달은 VcGanttBoard 가 {@link #validate} 결과로 처리.
 */
export function useDragValidation() {
  const { data: compat, isLoading, isError } = useSlotCompatibilityMatrix()

  function check(hoseId: string | undefined, slot: SlotPosition): boolean {
    return isEligible(compat, hoseId, slot)
  }

  /**
   * TK-04-3-2 — 위반 시 reason + 대안 슬롯 (NFR-USA-002 "최소 1 대안") 반환.
   *
   * 우선순위:
   *  1. 매트릭스 미로딩 → ok=true (Optimistic, UX)
   *  2. 매트릭스에서 hoseId 존재 + slot=true → ok=true
   *  3. hoseId 매트릭스 미적재 → ok=true (Optimistic)
   *  4. 비적합 → ok=false + reason + alternatives (해당 hoseId 의 모든 true 슬롯)
   */
  function validate(hoseId: string, slot: SlotPosition): ValidationResult {
    if (!compat) {
      return { ok: true } // Optimistic — 미로딩 시 차단 X
    }
    const hoseSlots = compat.byHose[hoseId]
    if (!hoseSlots) {
      return { ok: true } // 매트릭스 미적재 품번 → Optimistic OK
    }
    if (hoseSlots[slot]) {
      return { ok: true }
    }
    const alternatives = (Object.entries(hoseSlots) as Array<[SlotPosition, boolean]>)
      .filter(([, eligible]) => eligible)
      .map(([s]) => s)
    return {
      ok: false,
      reason: `품번 ${hoseId} 는 ${slot} 슬롯에 적합하지 않습니다 (BR-V13)`,
      alternatives,
    }
  }

  function unschedulableHoseIds(): string[] {
    return compat?.unschedulableHoseIds ?? []
  }

  function isHoseUnschedulable(hoseId: string): boolean {
    return unschedulableHoseIds().includes(hoseId)
  }

  return {
    check,
    validate,
    unschedulableHoseIds,
    isHoseUnschedulable,
    isLoading,
    isError,
    matrixVersion: compat?.version,
  }
}
