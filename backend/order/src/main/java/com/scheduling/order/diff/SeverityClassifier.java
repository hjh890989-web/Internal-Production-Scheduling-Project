package com.scheduling.order.diff;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RowDiff → Severity 분류기 — TK-03-2-1 (BR-O02 / REQ-FUNC-OC-008).
 *
 * <p>분류 규칙 (conservative — False Negative 0 정신):
 * <ol>
 *   <li>{@link DiffType#UNCHANGED} → NORMAL</li>
 *   <li>{@link DiffType#NEW} → 설정 시 CRITICAL (기본 true)</li>
 *   <li>{@link DiffType#DELETED} → 설정 시 CRITICAL (기본 true)</li>
 *   <li>{@link DiffType#MODIFIED} — 필드별 평가:
 *     <ul>
 *       <li>{@code delivery_date} 변경 → CRITICAL</li>
 *       <li>{@code hose_id} 변경 → CRITICAL</li>
 *       <li>{@code qty} 변경 ≥ ±{threshold}% → CRITICAL</li>
 *       <li>{@code qty} 0 → N 변경 → CRITICAL (특수)</li>
 *       <li>{@code qty} non-numeric → CRITICAL (conservative)</li>
 *       <li>customer·order_type 변경만 → NORMAL</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>임계치 외부화 — {@link SeverityConfig}. 운영 중 application.yml 조정 가능.
 */
@Component
@EnableConfigurationProperties(SeverityConfig.class)
public class SeverityClassifier {

    private static final String FIELD_DELIVERY_DATE = "delivery_date";
    private static final String FIELD_HOSE_ID = "hose_id";
    private static final String FIELD_QTY = "qty";

    private final SeverityConfig config;

    public SeverityClassifier(SeverityConfig config) {
        this.config = config;
    }

    public Severity classify(RowDiff diff) {
        if (diff == null || diff.type() == DiffType.UNCHANGED) {
            return Severity.NORMAL;
        }
        if (diff.type() == DiffType.NEW) {
            return config.isNewAlwaysCritical() ? Severity.CRITICAL : Severity.NORMAL;
        }
        if (diff.type() == DiffType.DELETED) {
            return config.isDeletedAlwaysCritical() ? Severity.CRITICAL : Severity.NORMAL;
        }

        // MODIFIED — 필드별 평가
        for (FieldDiff fd : diff.fieldDiffs()) {
            if (FIELD_DELIVERY_DATE.equals(fd.fieldName()) && config.isDeliveryDateAlwaysCritical()) {
                return Severity.CRITICAL;
            }
            if (FIELD_HOSE_ID.equals(fd.fieldName()) && config.isHoseIdAlwaysCritical()) {
                return Severity.CRITICAL;
            }
            if (FIELD_QTY.equals(fd.fieldName()) && qtyChangeOverThreshold(fd)) {
                return Severity.CRITICAL;
            }
        }
        return Severity.NORMAL;
    }

    /**
     * qty 변경이 임계 비율 이상인지.
     * <ul>
     *   <li>non-numeric → true (conservative)</li>
     *   <li>before=0, after≠0 → true (0 → N 변경)</li>
     *   <li>before=0, after=0 → false (변경 없음 동일)</li>
     *   <li>그 외 → abs(after-before)/abs(before) ≥ threshold</li>
     * </ul>
     */
    private boolean qtyChangeOverThreshold(FieldDiff fd) {
        Number before = asNumber(fd.before());
        Number after = asNumber(fd.after());
        if (before == null || after == null) {
            return true;     // conservative — non-numeric 은 항상 Critical
        }
        double oldVal = before.doubleValue();
        double newVal = after.doubleValue();
        if (oldVal == 0d) {
            return newVal != 0d;
        }
        double pct = Math.abs(newVal - oldVal) / Math.abs(oldVal);
        return pct >= config.getQtyChangeThresholdPct();
    }

    private Number asNumber(Object value) {
        if (value instanceof Number n) return n;
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim().replace(",", ""));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
