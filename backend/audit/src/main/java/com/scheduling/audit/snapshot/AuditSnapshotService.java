package com.scheduling.audit.snapshot;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 임의 시점 마스터 row 재구성 — TK-19-1-1 (EP-19, REQ-FUNC-OC-014).
 *
 * <p>audit.schedule_audit_log JSONB old_row/new_row 시간순 재생 → 특정 timestamp 시점의
 * row 상태 복원. NFR-SEC-004 immutable audit 활용 — 실제 복원은 별도 confirm 흐름 (위험 방지).
 *
 * <p>알고리즘:
 * <ol>
 *   <li>{@code occurred_at <= at} AND {@code table_name = ? AND row_pk = ?} 조회</li>
 *   <li>가장 최신 row 의 new_row (INSERT/UPDATE) 또는 null (DELETE)</li>
 * </ol>
 *
 * <p>p95 ≤ 5초 목표 (NFR-PER-006) — audit_log 인덱스 {@code (table_name, row_pk)} 활용.
 */
@Service
@Profile("with-infra")
public class AuditSnapshotService {

    private final JdbcTemplate jdbc;

    public AuditSnapshotService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record SnapshotResult(
        String tableName,
        String rowPk,
        Instant atTimestamp,
        Instant capturedAt,
        String lastAction,
        boolean rowExisted,
        String jsonPayload
    ) {}

    /**
     * 특정 시점 row 상태 조회.
     *
     * @param tableName 'vc_schedule' / 'ex_schedule_candidate' / 'order'
     * @param rowPk     row UUID 문자열
     * @param at        시점 (이 시간 이전 가장 최근 mutation 반영)
     * @return rowExisted=false 면 해당 시점에 row 미존재 (DELETE 이후 또는 INSERT 이전)
     */
    public SnapshotResult reconstructAt(String tableName, String rowPk, Instant at) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT action, new_row::text AS new_row_txt, old_row::text AS old_row_txt, "
                + "       occurred_at "
                + "FROM audit.schedule_audit_log "
                + "WHERE table_name = ? AND row_pk = ? AND occurred_at <= ? "
                + "ORDER BY occurred_at DESC LIMIT 1",
            tableName, rowPk, java.sql.Timestamp.from(at));

        if (rows.isEmpty()) {
            return new SnapshotResult(tableName, rowPk, at, null, null, false, null);
        }
        Map<String, Object> last = rows.get(0);
        String action = (String) last.get("action");
        Instant capturedAt = ((java.sql.Timestamp) last.get("occurred_at")).toInstant();

        boolean existed = !"DELETE".equals(action);
        String payload = existed
            ? (String) last.get("new_row_txt")
            : (String) last.get("old_row_txt");

        return new SnapshotResult(tableName, rowPk, at, capturedAt, action, existed, payload);
    }

    /**
     * 전체 timeline 조회 — UI slider 용. ASC 정렬.
     */
    public List<Map<String, Object>> timeline(String tableName, String rowPk) {
        return jdbc.queryForList(
            "SELECT audit_id, action, actor, reason, occurred_at "
                + "FROM audit.schedule_audit_log "
                + "WHERE table_name = ? AND row_pk = ? "
                + "ORDER BY occurred_at ASC",
            tableName, rowPk);
    }
}
