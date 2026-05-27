package com.scheduling.vc.validate;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * On-Demand 전체 스케줄 검증 REST — TK-VC16-1-1 (REQ-FUNC-VC-016).
 *
 * <p>{@code POST /api/v1/schedule/validate-all?from=YYYY-MM-DD&to=YYYY-MM-DD} —
 * 인증된 모든 role 호출. 결과: 위반 목록 + 카테고리별 summary + executedAt.
 *
 * <p>p95 ≤ 3초 목표 (TK-VC16-1-3 측정).
 */
@RestController
@RequestMapping("/api/v1/schedule")
@Profile("with-infra")
public class ScheduleValidateController {

    private final ScheduleValidatorService validator;

    public ScheduleValidateController(ScheduleValidatorService validator) {
        this.validator = validator;
    }

    @PostMapping("/validate-all")
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<ValidationResult> validateAll(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(validator.validateRange(from, to));
    }
}
