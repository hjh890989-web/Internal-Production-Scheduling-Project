package com.scheduling.ex.ranking;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 다중 후보 ranking REST — TK-18-1-1 (EP-18, REQ-FUNC-XT-001).
 *
 * <p>{@code GET /api/v1/schedule/ex/candidates/ranking?from=&to=&limit=3}
 * → ≥ 3 distinct ranked candidates (totalScore desc).
 */
@RestController
@RequestMapping("/api/v1/schedule/ex/candidates")
@Profile("with-infra")
public class CandidateRankingController {

    private final CandidateRankingService service;

    public CandidateRankingController(CandidateRankingService service) {
        this.service = service;
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public List<CandidateRankingService.RankedCandidate> ranking(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return service.rank(from, to).stream().limit(Math.max(3, limit)).toList();
    }
}
