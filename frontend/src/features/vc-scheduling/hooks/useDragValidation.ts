import { useSlotCompatibilityMatrix } from './useSlotCompatibilityMatrix'
import { isEligible, type SlotPosition } from '../types/slotCompat'

/**
 * 드래그 중 (hose_id, slot) 적합성 실시간 검증 — TK-04-3-1.
 *
 * <p>매트릭스 미로딩 / 매트릭스 미적재 품번 — Optimistic OK 반환 (UX 보호).
 * 실제 저장 차단은 TK-04-3-2 (서버 측 가드 + override 사유) 가 담당.
 */
export function useDragValidation() {
  const { data: compat, isLoading, isError } = useSlotCompatibilityMatrix()

  function check(hoseId: string | undefined, slot: SlotPosition): boolean {
    return isEligible(compat, hoseId, slot)
  }

  function unschedulableHoseIds(): string[] {
    return compat?.unschedulableHoseIds ?? []
  }

  function isHoseUnschedulable(hoseId: string): boolean {
    return unschedulableHoseIds().includes(hoseId)
  }

  return {
    check,
    unschedulableHoseIds,
    isHoseUnschedulable,
    isLoading,
    isError,
    matrixVersion: compat?.version,
  }
}
