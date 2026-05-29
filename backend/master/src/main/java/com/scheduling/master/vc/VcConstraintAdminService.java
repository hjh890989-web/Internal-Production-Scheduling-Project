package com.scheduling.master.vc;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Sprint 21 ST-CRUD-3 — VcConstraint (합금형 슬롯 적합성) 관리 Service (IT_OPS 전용).
 *
 * <p>BR-V14 — {@code composite_count} ∈ {1, 2, 3, 6}. 4·5·7 등 위반 시 {@link IllegalArgumentException}.
 * <p>{@code @Auditable} — BR-X02 모든 mutation audit_log 강제 기록.
 *
 * @see VcConstraint
 * @see BR-V14
 * @see BR-X02
 */
@Service
public class VcConstraintAdminService {

    private static final Logger log = LoggerFactory.getLogger(VcConstraintAdminService.class);

    private final VcConstraintRepository repository;
    private final Clock clock;

    public VcConstraintAdminService(VcConstraintRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** 전체 조회 — PLANNER, STK_USER, IT_OPS, READ_ONLY. */
    public List<VcConstraint> list() {
        return repository.findAll();
    }

    /**
     * 신규 생성 (IT_OPS) — hoseId 중복 시 {@link IllegalStateException}.
     *
     * @see BR-V14
     * @see BR-X02
     */
    @Auditable("ST-CRUD-3 VcConstraint 신규 생성 (IT_OPS)")
    @Transactional
    public VcConstraint create(VcConstraintPayload payload, String actor) {
        validateCompositeCount(payload.compositeCount());
        if (repository.existsById(payload.hoseId())) {
            throw new IllegalStateException("hose_id 중복: " + payload.hoseId());
        }
        VcConstraint entity = buildEntity(payload, actor);
        log.info("VcConstraintAdmin create — hose={} composite={}", payload.hoseId(), payload.compositeCount());
        return repository.save(entity);
    }

    /**
     * 수정 (IT_OPS) — hoseId 미존재 시 {@link EntityNotFoundException}.
     *
     * @see BR-V14
     * @see BR-X02
     */
    @Auditable("ST-CRUD-3 VcConstraint 수정 (IT_OPS)")
    @Transactional
    public VcConstraint update(String hoseId, VcConstraintPayload payload, String actor) {
        validateCompositeCount(payload.compositeCount());
        if (!repository.existsById(hoseId)) {
            throw new EntityNotFoundException("hose_id 미존재: " + hoseId);
        }
        VcConstraint entity = buildEntityWithId(hoseId, payload, actor);
        log.info("VcConstraintAdmin update — hose={} composite={}", hoseId, payload.compositeCount());
        return repository.save(entity);
    }

    /**
     * BR-V14 — composite_count 는 합금형 1·2·3·6 만 허용.
     * 4·5·7 등 입력 시 400 ProblemDetail 로 변환 (Controller 에서 catch).
     */
    private static void validateCompositeCount(short compositeCount) {
        if (compositeCount != 1 && compositeCount != 2 && compositeCount != 3 && compositeCount != 6) {
            throw new IllegalArgumentException(
                "BR-V14 합금형 1·2·3·6 만 허용: " + compositeCount);
        }
    }

    private VcConstraint buildEntity(VcConstraintPayload p, String actor) {
        return buildEntityWithId(p.hoseId(), p, actor);
    }

    private VcConstraint buildEntityWithId(String hoseId, VcConstraintPayload p, String actor) {
        return new VcConstraint(
            hoseId,
            p.lpMoldQty(),
            p.compositeCount(),
            null, null,
            p.slot1(), p.slot2(), p.slot3(), p.slot4(),
            null, null,
            p.slot5(), p.slot6(), p.slot7(),
            clock.instant(),
            actor == null ? "anonymousUser" : actor
        );
    }

    /**
     * Payload record — Controller 와 IT 공유 (내부 패키지 가시성).
     *
     * <p>slotEligibility: slot1~4 = LP(top·upmid·lowmid·bot), slot5~7 = IC(top·mid·bot) — BR-V14.
     */
    public record VcConstraintPayload(
        String hoseId,
        short compositeCount,
        int lpMoldQty,
        int icMoldQty,
        boolean slot1,
        boolean slot2,
        boolean slot3,
        boolean slot4,
        boolean slot5,
        boolean slot6,
        boolean slot7
    ) {}
}
