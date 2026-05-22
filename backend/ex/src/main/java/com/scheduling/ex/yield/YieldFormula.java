package com.scheduling.ex.yield;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 압출 yield 수식 — TK-08-2-1 (EP-08 ST-08-2, BR-E05).
 *
 * <p><b>공식</b>: {@code yield = round(speed_m_per_min × effective_min × 1000 / length_mm)}
 *
 * <p>SRS BR-E05 spec doc 은 "floor" 표기하나 reference 값 (2531) 이 round-half-up 결과
 * 와 일치 — spec 의도가 round-half-up (HALF_UP) 임을 채택. 실무 yield 산출도 보통
 * 반올림 사용 (산업적 관행).
 *
 * <p><b>BR-E05 reference case</b>: {@code 29673-2R060} 주간전반 = 2,531
 * <ul>
 *   <li>speed = 14.06 m/min, length = 1000 mm, effective_min = 180</li>
 *   <li>round(14.06 × 180 × 1000 / 1000) = round(2530.8) = 2531</li>
 * </ul>
 *
 * <p><b>단위 가드</b> (BR-E05):
 * <ul>
 *   <li>speed ≤ 0 → IllegalArgumentException</li>
 *   <li>speed > 200 m/min → UnitMismatchException (mm/min 입력 의심)</li>
 *   <li>length ≤ 0 → IllegalArgumentException</li>
 *   <li>length > 100,000 mm → UnitMismatchException (μm 입력 의심)</li>
 *   <li>effectiveMin ≤ 0 → IllegalArgumentException</li>
 * </ul>
 */
@Component
public class YieldFormula {

    /** speed 상한 (m/min) — 초과 시 단위 오류 의심. */
    public static final BigDecimal MAX_REASONABLE_SPEED = BigDecimal.valueOf(200);
    /** length 상한 (mm) — 초과 시 단위 오류 의심. */
    public static final int MAX_REASONABLE_LENGTH = 100_000;

    /**
     * BR-E05 yield 계산.
     *
     * @param speedMPerMin  압출 속도 m/min
     * @param effectiveMin  shift effective_min (nominal × efficiency)
     * @param lengthMm      단위 길이 mm
     * @return floor(speed × min × 1000 / length)
     */
    public long compute(BigDecimal speedMPerMin, int effectiveMin, int lengthMm) {
        validate(speedMPerMin, effectiveMin, lengthMm);

        // yield = speed × min × 1000 / length, round-half-up (BR-E05 reference 2531 정합)
        BigDecimal numerator = speedMPerMin
            .multiply(BigDecimal.valueOf(effectiveMin))
            .multiply(BigDecimal.valueOf(1000));
        BigDecimal raw = numerator.divide(BigDecimal.valueOf(lengthMm), 10, RoundingMode.HALF_UP);
        return raw.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private void validate(BigDecimal speed, int effectiveMin, int lengthMm) {
        if (speed == null || speed.signum() <= 0) {
            throw new IllegalArgumentException("speed > 0 필수: " + speed);
        }
        if (speed.compareTo(MAX_REASONABLE_SPEED) > 0) {
            throw new UnitMismatchException(
                "speed %s m/min — 비현실적 (단위 오류 의심, mm/min 입력?). 한도 ≤ %s"
                    .formatted(speed, MAX_REASONABLE_SPEED));
        }
        if (effectiveMin <= 0) {
            throw new IllegalArgumentException("effectiveMin > 0 필수: " + effectiveMin);
        }
        if (lengthMm <= 0) {
            throw new IllegalArgumentException("lengthMm > 0 필수: " + lengthMm);
        }
        if (lengthMm > MAX_REASONABLE_LENGTH) {
            throw new UnitMismatchException(
                "length %d mm — 비현실적 (단위 오류 의심, μm 입력?). 한도 ≤ %d"
                    .formatted(lengthMm, MAX_REASONABLE_LENGTH));
        }
    }
}
