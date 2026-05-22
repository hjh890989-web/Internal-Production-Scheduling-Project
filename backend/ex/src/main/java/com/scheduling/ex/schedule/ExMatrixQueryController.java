package com.scheduling.ex.schedule;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * EX 매트릭스 JSON 조회 — TK-17-1-2 (EP-17 ST-17-1).
 *
 * <p>{@code GET /api/v1/schedule/ex/matrix?from=YYYY-MM-DD&to=YYYY-MM-DD}
 *
 * <p>Frontend AG Grid 행 = candidate, col = hose/date/yield/status. STOMP cascade 갱신은
 * EP-EX14 chain 으로 별도 ({@code /topic/extrusion-updates}). 본 controller 는 초기 fetch.
 */
@RestController
@RequestMapping("/api/v1/schedule/ex")
@Profile("with-infra")
public class ExMatrixQueryController {

    private final ExScheduleCandidateRepository repository;

    public ExMatrixQueryController(ExScheduleCandidateRepository repository) {
        this.repository = repository;
    }

    /** AG Grid row payload — Java record → JSON automatic. */
    public record MatrixRow(
        UUID exCandidateId,
        String hoseId,
        LocalDate vcProductionDate,
        LocalDate extrusionDeadline,
        int vcYield,
        String status
    ) {
        static MatrixRow from(ExScheduleCandidate c) {
            return new MatrixRow(
                c.getExCandidateId(), c.getHoseId(),
                c.getVcProductionDate(), c.getExtrusionDeadline(),
                c.getVcYield(), c.getStatus().name());
        }
    }

    @GetMapping("/matrix")
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public List<MatrixRow> findMatrix(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return repository.findAll().stream()
            .filter(c -> !c.getExtrusionDeadline().isBefore(from)
                      && !c.getExtrusionDeadline().isAfter(to))
            .map(MatrixRow::from)
            .toList();
    }
}
