package com.scheduling.vc.rule;

import com.scheduling.master.api.ProductSpecLookup;
import com.scheduling.master.api.ProductSpecSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 규격<7 가류기당 동시 점유 앵글 ≤ 4 — TK-21-5-3 (BR-V17, REQ-FUNC-VC-027).
 *
 * <p>cross-master rule — ProductSpec VIEW 의 {@code is_spec_lt7=true} 품번만 활성.
 * 같은 (machine, date) 의 spec<7 hose들의 angle_count 누적 ≤ 4 검증.
 *
 * <p>{@code @Profile("with-infra")} — ProductSpecLookup (master::api) 의존.
 */
@Component
@Profile("with-infra")
public class SpecLt7CapRule {

    /** BR-V17 — 가류기당 spec<7 동시 점유 앵글 상한. */
    public static final int SPEC_LT7_MAX_ANGLES = 4;

    private final ProductSpecLookup lookup;

    public SpecLt7CapRule(ProductSpecLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * 본 hose 가 spec<7 인지 + angle_count.
     */
    public Optional<ProductSpecSummary> productSpec(String hoseId) {
        return lookup.findById(hoseId);
    }

    /**
     * spec<7 누적 검증.
     *
     * @param hoseId        품번
     * @param currentAngles 같은 (machine, date) 의 이미 점유된 spec<7 angle 누계
     * @return true = 허용, false = 본 hose 가 spec<7 이고 (currentAngles + angle_count) > 4 → fail
     */
    public boolean fitsAngleCap(String hoseId, int currentAngles) {
        Optional<ProductSpecSummary> specOpt = lookup.findById(hoseId);
        if (specOpt.isEmpty() || !specOpt.get().isSpecLt7()) {
            return true;     // spec NULL or ≥7 — 룰 미적용
        }
        int candAngles = specOpt.get().angleCount();
        return (currentAngles + candAngles) <= SPEC_LT7_MAX_ANGLES;
    }

    /**
     * Allocator helper — (machine, date) 별 spec<7 angle 누적 카운트 ledger 인터페이스.
     */
    public static class AngleLedger {
        private final Map<String, Integer> angles = new HashMap<>();

        public int count(String machineId, LocalDate date) {
            return angles.getOrDefault(key(machineId, date), 0);
        }

        public void add(String machineId, LocalDate date, int angles) {
            this.angles.merge(key(machineId, date), angles, Integer::sum);
        }

        private static String key(String machineId, LocalDate date) {
            return machineId + "/" + date;
        }
    }
}
