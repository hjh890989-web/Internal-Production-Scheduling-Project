package com.scheduling.master.priority;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Sprint 12 EP-MASTER-UI BR-V12 PRODUCT_PRIORITY 관리 Service (TK-MASTER-3-2, IT_OPS 전용).
 *
 * <p>{@code @Auditable} BR-X02 — 모든 mutation audit_log 기록 (actor=IT_OPS 사번 + reason).
 * 변경 후 CapacityOverflowQueueService 가 next call 시 즉시 새 priority 반영 (BR-V12).
 */
@Service
public class ProductPriorityAdminService {

    private static final Logger log = LoggerFactory.getLogger(ProductPriorityAdminService.class);

    private final ProductPriorityRepository repository;
    private final Clock clock;

    public ProductPriorityAdminService(ProductPriorityRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<ProductPriority> list() {
        return repository.findAll();
    }

    public List<ProductPriority> listEffective() {
        return repository.findEffectiveAt(LocalDate.now(clock));
    }

    @Auditable("EP-MASTER-UI PRODUCT_PRIORITY 추가 (IT_OPS)")
    @Transactional
    public ProductPriority create(String hoseId, short priorityRank, String rationale,
                                   LocalDate effectiveFrom, LocalDate effectiveTo, String actor) {
        if (repository.existsById(hoseId)) {
            throw new EntityExistsException("hose_id 중복: " + hoseId);
        }
        ProductPriority p = new ProductPriority(hoseId, priorityRank, rationale,
            effectiveFrom, effectiveTo, clock.instant(), actor);
        log.info("EP-MASTER-UI priority create — hose={} rank={}", hoseId, priorityRank);
        return repository.save(p);
    }

    @Auditable("EP-MASTER-UI PRODUCT_PRIORITY 수정 (IT_OPS)")
    @Transactional
    public ProductPriority update(String hoseId, short priorityRank, String rationale,
                                   LocalDate effectiveFrom, LocalDate effectiveTo, String actor) {
        if (!repository.existsById(hoseId)) {
            throw new EntityNotFoundException("hose_id 미존재: " + hoseId);
        }
        // update 는 새 row 로 대체 — protected 생성자 회피
        ProductPriority p = new ProductPriority(hoseId, priorityRank, rationale,
            effectiveFrom, effectiveTo, clock.instant(), actor);
        log.info("EP-MASTER-UI priority update — hose={} rank={}", hoseId, priorityRank);
        return repository.save(p);
    }

    @Auditable("EP-MASTER-UI PRODUCT_PRIORITY 삭제 (IT_OPS)")
    @Transactional
    public void delete(String hoseId) {
        if (!repository.existsById(hoseId)) {
            throw new EntityNotFoundException("hose_id 미존재: " + hoseId);
        }
        repository.deleteById(hoseId);
        log.info("EP-MASTER-UI priority delete — hose={}", hoseId);
    }
}
