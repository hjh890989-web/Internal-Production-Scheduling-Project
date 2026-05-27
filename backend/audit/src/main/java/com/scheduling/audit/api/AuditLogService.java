package com.scheduling.audit.api;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Sprint 13 hotfix — Direct audit_log INSERT API (BR-X02 mutation-less endpoint 정합).
 *
 * <p>일반 패턴: {@link com.scheduling.audit.aop.Auditable} AOP 가 set_config 주입 → 도메인
 * 테이블의 audit trigger 가 INSERT/UPDATE/DELETE 시점에 audit_log row 자동 생성.
 *
 * <p>본 service 의 용도: **mutation 없이 의사결정 자체를 BR-X02 audit** 해야 하는 endpoint —
 * 예: PLANNER 가 수주 import 를 commit (실 Order INSERT 는 Sprint 14 listener 가 별도 처리,
 * 본 commit 자체는 event 발행만 → trigger 미발화). 이 경우 명시적 audit row INSERT 필요.
 *
 * <p>{@code @Profile("with-infra")} — JdbcTemplate 활성 환경에서만 bean 생성. Test 는 Testcontainers.
 *
 * @see com.scheduling.audit.aop.Auditable
 */
@Service
@Profile("with-infra")
public class AuditLogService {

    private final JdbcTemplate jdbc;

    public AuditLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** audit_log CHECK constraint 정합 action — INSERT/UPDATE/DELETE 만 허용. 의사결정은 reason 에 명시. */
    public enum Action {
        INSERT, UPDATE, DELETE
    }

    /**
     * audit_log 에 직접 row INSERT — domain mutation 없는 의사결정 endpoint 용.
     *
     * <p>action 은 CHECK constraint 정합 위해 enum 강제. 의사결정 종류 (commit/reject 등) 는
     * reason 에 명시 — 예: {@code record("order_change", trackingId, UPDATE, "00000001",
     * "EP-OC-FULL 확정 (BR-X05): 정기 발주")}.
     *
     * @param tableName 의사결정 대상 table 명 (예: "order_change")
     * @param rowPk     대상 row PK (예: trackingId UUID string)
     * @param action    INSERT / UPDATE / DELETE (audit_log CHECK 정합)
     * @param actor     수행 사용자 사번 (BR-X05 dual-review 작성자)
     * @param reason    의사결정 종류 + 사유 (BR-X02 필수)
     */
    public void record(String tableName, String rowPk, Action action, String actor, String reason) {
        jdbc.update(
            "INSERT INTO audit.schedule_audit_log (table_name, row_pk, action, actor, reason) "
                + "VALUES (?, ?, ?, ?, ?)",
            tableName, rowPk, action.name(), actor, reason);
    }
}
