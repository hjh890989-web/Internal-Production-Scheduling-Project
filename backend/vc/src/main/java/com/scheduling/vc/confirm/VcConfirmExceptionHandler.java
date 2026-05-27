package com.scheduling.vc.confirm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sprint 16 EP-CONFIRM 예외 → HTTP 매핑 — TK-CONFIRM-5-2.
 *
 * <p>RFC 7807 ProblemDetail 응답:
 * <ul>
 *   <li>{@link DualReviewConflictException} → 409 Conflict (BR-X05)</li>
 *   <li>{@link D2HardConstraintException} → 423 Locked (BR-X07)</li>
 *   <li>{@link IllegalStateException} → 409 Conflict (BR-X01 already-confirmed 등)</li>
 *   <li>{@link IllegalArgumentException} → 400 Bad Request (validation)</li>
 * </ul>
 *
 * <p>Frontend ConfirmModal 이 status code 로 분기 — 친화 메시지 노출.
 */
@RestControllerAdvice(basePackages = "com.scheduling.vc.confirm")
public class VcConfirmExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(VcConfirmExceptionHandler.class);

    @ExceptionHandler(DualReviewConflictException.class)
    public ResponseEntity<ProblemDetail> handleDualReview(DualReviewConflictException e) {
        log.warn("BR-X05 dual-review reject — id={} createdBy={} planner={}",
            e.getVcScheduleId(), e.getCreatedBy(), e.getPlannerId());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        pd.setTitle("BR-X05 dual-review");
        pd.setProperty("brCode", "BR-X05");
        pd.setProperty("vcScheduleId", e.getVcScheduleId().toString());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(D2HardConstraintException.class)
    public ResponseEntity<ProblemDetail> handleD2Hard(D2HardConstraintException e) {
        log.warn("BR-X07 D-2 hard reject — production_date={} today={} gap={}",
            e.getProductionDate(), e.getToday(), e.getGapDays());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED, e.getMessage());
        pd.setTitle("BR-X07 D-2 hard");
        pd.setProperty("brCode", "BR-X07");
        pd.setProperty("productionDate", e.getProductionDate().toString());
        pd.setProperty("gapDays", e.getGapDays());
        return ResponseEntity.status(HttpStatus.LOCKED).body(pd);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException e) {
        log.warn("VC confirm state reject — {}", e.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        pd.setTitle("VC confirm state");
        pd.setProperty("brCode", "BR-X01");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArg(IllegalArgumentException e) {
        log.warn("VC confirm validation reject — {}", e.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("VC confirm validation");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }
}
