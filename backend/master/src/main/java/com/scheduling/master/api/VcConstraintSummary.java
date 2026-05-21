package com.scheduling.master.api;

/**
 * VcConstraint 요약 DTO — TK-05-2-1 (cross-module).
 *
 * <p>{@code com.scheduling.master.vc.VcConstraint} 의 immutable 사본. vc 모듈은
 * {@link VcConstraintLookup} facade 로 본 record 만 받음 (Modulith 경계).
 *
 * @param hoseId             품번
 * @param compositeCount     합금형 1·2·3·6 (BR-V14)
 * @param lpMoldsPerAngle    LP 앵글당 금형수 (null 가능)
 * @param lpAngleQty         LP 앵글 보유수 (null 가능)
 * @param icMoldsPerAngle    IC 앵글당 금형수 (null 가능)
 * @param icAngleQty         IC 앵글 보유수 (null 가능)
 */
public record VcConstraintSummary(
    String hoseId,
    short compositeCount,
    Short lpMoldsPerAngle,
    Short lpAngleQty,
    Short icMoldsPerAngle,
    Short icAngleQty
) {
    /** BR-V03: yield per rotation = composite × lp_molds_per_angle. */
    public int lpYieldPerRotation() {
        return lpMoldsPerAngle == null ? 0 : compositeCount * lpMoldsPerAngle;
    }

    /** BR-V03: yield per rotation = composite × ic_molds_per_angle. */
    public int icYieldPerRotation() {
        return icMoldsPerAngle == null ? 0 : compositeCount * icMoldsPerAngle;
    }
}
