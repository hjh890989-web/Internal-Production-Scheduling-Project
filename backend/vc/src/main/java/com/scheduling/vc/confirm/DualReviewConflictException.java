package com.scheduling.vc.confirm;

import java.util.UUID;

/**
 * Sprint 16 BR-X05 dual-review 위반 — 작성자(createdBy) == 승인자(plannerId).
 *
 * <p>HTTP 409 Conflict 매핑 (VcExceptionHandler). Frontend 가 본 코드로 dual-review Modal 안내.
 */
public class DualReviewConflictException extends RuntimeException {

    private final UUID vcScheduleId;
    private final String createdBy;
    private final String plannerId;

    public DualReviewConflictException(UUID vcScheduleId, String createdBy, String plannerId) {
        super(String.format(
            "BR-X05 dual-review 위반: vc_schedule_id=%s 작성자(%s)와 승인자(%s) 동일 — 다른 ROLE_PLANNER 승인 필수",
            vcScheduleId, createdBy, plannerId));
        this.vcScheduleId = vcScheduleId;
        this.createdBy = createdBy;
        this.plannerId = plannerId;
    }

    public UUID getVcScheduleId() { return vcScheduleId; }
    public String getCreatedBy() { return createdBy; }
    public String getPlannerId() { return plannerId; }
}
