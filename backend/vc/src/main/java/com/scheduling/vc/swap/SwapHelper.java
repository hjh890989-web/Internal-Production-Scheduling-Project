package com.scheduling.vc.swap;

import com.scheduling.vc.domain.VcSchedule;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * VcSchedule rotation_no atomic swap — TK-15-2-2 (EP-15 ST-15-2).
 *
 * <p>JPA UPDATE 2회 분리 시 {@code UNIQUE(machine, slot, date, rotation_no)} 충돌 발생.
 * 단일 SQL {@code CASE WHEN} statement 로 동시에 두 row 갱신 (atomic, UNIQUE 일시 통과).
 */
@Component
@Profile("with-infra")
public class SwapHelper {

    private static final JdbcTemplate[] HOLDER = new JdbcTemplate[1];

    public SwapHelper(JdbcTemplate jdbc) {
        HOLDER[0] = jdbc;
    }

    /**
     * 두 VcSchedule row 의 rotation_no atomic swap. updated_at 갱신.
     *
     * <p>{@link SwapProposalService} 에서 정적 호출 — Spring DI 후 JdbcTemplate 캐시.
     */
    public static void swapRotation(VcSchedule a, VcSchedule b, Instant now) {
        JdbcTemplate jdbc = HOLDER[0];
        if (jdbc == null) {
            throw new IllegalStateException("SwapHelper 미초기화 (Spring DI 필요)");
        }
        UUID aId = a.getVcScheduleId();
        UUID bId = b.getVcScheduleId();
        short rotA = a.getRotationNo();
        short rotB = b.getRotationNo();

        // V028 — UNIQUE 제약을 DEFERRED 로 잠깐 전환 (COMMIT 시점 enforce)
        jdbc.execute("SET CONSTRAINTS app.uq_vc_schedule_slot_rotation DEFERRED");

        int updated = jdbc.update(
            "UPDATE app.vc_schedule "
                + "SET rotation_no = CASE vc_schedule_id "
                + "        WHEN ? THEN ? "
                + "        WHEN ? THEN ? "
                + "    END, "
                + "    updated_at = ? "
                + "WHERE vc_schedule_id IN (?, ?)",
            aId, (int) rotB, bId, (int) rotA,
            java.sql.Timestamp.from(now),
            aId, bId);

        if (updated != 2) {
            throw new IllegalStateException(
                "atomic swap 행 수 불일치 (expected 2, actual " + updated + ")");
        }
    }
}
