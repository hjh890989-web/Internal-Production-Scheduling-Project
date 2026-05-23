package com.scheduling.vc.swap;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * STK_USER swap 제안 + Planner 1클릭 수용 — TK-15-2-2 (EP-15 ST-15-2, REQ-FUNC-VC-018).
 *
 * <p>총량 보존 invariant — accept 시 두 row 의 rotation_no 만 atomic swap.
 * plannedQty / hoseId 는 그대로 → 같은 (machine, slot, date) 안 yield 합 변경 0.
 *
 * <p>BR-V07 일중 락 정합 — 두 row 가 같은 (machine, slot, date) 안이고 같은 angle 이면
 * trigger pass. 다른 angle 시 trigger reject (사용자가 override 명시 필요 — 별도 흐름).
 */
@Service
@Profile("with-infra")
public class SwapProposalService {

    private static final Logger log = LoggerFactory.getLogger(SwapProposalService.class);

    private final SwapProposalRepository proposalRepo;
    private final VcScheduleRepository scheduleRepo;
    private final Clock clock;

    public SwapProposalService(SwapProposalRepository proposalRepo,
                               VcScheduleRepository scheduleRepo,
                               Clock clock) {
        this.proposalRepo = proposalRepo;
        this.scheduleRepo = scheduleRepo;
        this.clock = clock;
    }

    @Auditable("STK_USER swap 제안 (REQ-FUNC-VC-018)")
    @Transactional
    public SwapProposal propose(UUID sourceRowId, UUID targetRowId,
                                String stkUserId, String reason) {
        scheduleRepo.findById(sourceRowId)
            .orElseThrow(() -> new IllegalArgumentException("source row 미존재: " + sourceRowId));
        scheduleRepo.findById(targetRowId)
            .orElseThrow(() -> new IllegalArgumentException("target row 미존재: " + targetRowId));

        SwapProposal p = new SwapProposal(
            UUID.randomUUID(), sourceRowId, targetRowId, stkUserId,
            Instant.now(clock), reason);
        proposalRepo.save(p);
        log.info("Swap 제안 등록 — proposalId={}, by={}", p.getProposalId(), stkUserId);
        return p;
    }

    @Auditable("Planner 1클릭 수용 — atomic swap 총량 보존 (REQ-FUNC-VC-018)")
    @Transactional
    public SwapProposal accept(UUID proposalId, String plannerId, String note) {
        SwapProposal p = proposalRepo.findById(proposalId)
            .orElseThrow(() -> new IllegalArgumentException("proposal 미존재: " + proposalId));

        VcSchedule source = scheduleRepo.findById(p.getSourceRowId()).orElseThrow();
        VcSchedule target = scheduleRepo.findById(p.getTargetRowId()).orElseThrow();

        // 총량 보존 사전 검증 — 같은 (machine, slot, date) 안인지 확인
        boolean sameSlot = source.getMachineId().equals(target.getMachineId())
            && source.getSlotPosition() == target.getSlotPosition()
            && source.getProductionDate().equals(target.getProductionDate());
        if (!sameSlot) {
            throw new IllegalArgumentException(
                "swap 은 같은 (machine, slot, date) 안에서만 — 총량 보존 invariant");
        }

        // rotation_no atomic swap — yield (plannedQty) 보존
        Instant now = Instant.now(clock);
        applyRotationSwap(source, target, now);

        p.accept(plannerId, now, note);
        proposalRepo.save(p);
        log.info("Swap 수용 — proposalId={}, planner={}, swap rotation {}↔{}",
            proposalId, plannerId, source.getRotationNo(), target.getRotationNo());
        return p;
    }

    @Auditable("Planner swap 거절 (REQ-FUNC-VC-018)")
    @Transactional
    public SwapProposal reject(UUID proposalId, String plannerId, String reason) {
        SwapProposal p = proposalRepo.findById(proposalId)
            .orElseThrow(() -> new IllegalArgumentException("proposal 미존재: " + proposalId));
        p.reject(plannerId, Instant.now(clock), reason);
        proposalRepo.save(p);
        log.info("Swap 거절 — proposalId={}, planner={}", proposalId, plannerId);
        return p;
    }

    /**
     * Atomic rotation_no swap — V028 DEFERRABLE UNIQUE + SET CONSTRAINTS DEFERRED 활용
     * ({@link SwapHelper#swapRotation}).
     */
    private void applyRotationSwap(VcSchedule a, VcSchedule b, Instant now) {
        SwapHelper.swapRotation(a, b, now);
    }
}
