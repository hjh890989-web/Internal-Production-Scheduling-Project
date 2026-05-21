package com.scheduling.master.vc;

import com.scheduling.master.api.HoseRuleLookup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * VC_HOSE_RULE 마스터 REST — TK-21-2-1 (IT_OPS RBAC).
 *
 * <p>{@code GET /api/v1/master/vc-hose-rule[?hoseId=]} — 인증된 모든 role.
 * <p>{@code POST / DELETE} — ROLE_IT_OPS.
 *
 * <p>변경 후 LISTEN/NOTIFY 트리거가 자동 발행 → Listener 가 캐시 invalidate.
 * 보조로 직접 {@link HoseRuleLookup#invalidate} 호출 (multi-instance fallback).
 */
@RestController
@RequestMapping("/api/v1/master/vc-hose-rule")
@Profile("with-infra")
public class HoseRuleController {

    private final VcHoseRuleRepository repository;
    private final HoseRuleLookup lookup;
    private final Clock clock;

    public HoseRuleController(VcHoseRuleRepository repository, HoseRuleLookup lookup, Clock clock) {
        this.repository = repository;
        this.lookup = lookup;
        this.clock = clock;
    }

    public record RulePayload(
        @NotBlank @Size(max = 40) String hoseId,
        @Size(max = 10) String machinePin,
        @NotNull Integer maxConcurrentSlots,
        @Size(max = 5) String sideLock,
        Boolean lpOnly,
        String notes
    ) {}

    public record RuleResponse(
        String hoseId, String machinePin, int maxConcurrentSlots,
        String sideLock, boolean lpOnly, String notes, String updatedBy
    ) {
        static RuleResponse from(VcHoseRule r) {
            return new RuleResponse(r.getHoseId(), r.getMachinePin(), r.getMaxConcurrentSlots(),
                r.getSideLock(), r.isLpOnly(), r.getNotes(), r.getUpdatedBy());
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<RuleResponse> list() {
        return repository.findAll().stream().map(RuleResponse::from).toList();
    }

    @GetMapping("/{hoseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RuleResponse> get(@PathVariable String hoseId) {
        return repository.findById(hoseId).map(RuleResponse::from)
            .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<RuleResponse> save(@Valid @RequestBody RulePayload payload,
                                              Principal principal) {
        VcHoseRule saved = repository.save(new VcHoseRule(
            payload.hoseId(), payload.machinePin(), payload.maxConcurrentSlots(),
            payload.sideLock(), Boolean.TRUE.equals(payload.lpOnly()),
            payload.notes(), Instant.now(clock), principal.getName()));
        lookup.invalidate(payload.hoseId());
        return ResponseEntity.status(HttpStatus.CREATED).body(RuleResponse.from(saved));
    }

    @DeleteMapping("/{hoseId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<Void> remove(@PathVariable String hoseId) {
        if (!repository.existsById(hoseId)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(hoseId);
        lookup.invalidate(hoseId);
        return ResponseEntity.noContent().build();
    }
}
