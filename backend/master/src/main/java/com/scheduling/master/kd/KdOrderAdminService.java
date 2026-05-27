package com.scheduling.master.kd;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Sprint 12 EP-MASTER-UI BR-V13 KD_ORDER 관리 Service (TK-MASTER-4-2, IT_OPS 전용).
 *
 * <p>{@code @Auditable} BR-X02 — 모든 mutation audit_log 기록. 변경 후 KdSupplementService
 * 가 next call 시 즉시 새 remaining_qty 반영 (BR-V13).
 */
@Service
public class KdOrderAdminService {

    private static final Logger log = LoggerFactory.getLogger(KdOrderAdminService.class);

    private final KdOrderRepository repository;
    private final Clock clock;

    public KdOrderAdminService(KdOrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<KdOrder> list() {
        return repository.findAll();
    }

    @Auditable("EP-MASTER-UI KD_ORDER 추가 (IT_OPS)")
    @Transactional
    public KdOrder create(String hoseId, int orderQty, int remainingQty, LocalDate orderDate,
                          String customerCode, KdOrder.Status status, String actor) {
        UUID id = UUID.randomUUID();
        KdOrder k = new KdOrder(id, hoseId, orderQty, remainingQty, orderDate,
            customerCode, status, clock.instant(), actor);
        log.info("EP-MASTER-UI kd create — id={} hose={} qty={}", id, hoseId, orderQty);
        return repository.save(k);
    }

    @Auditable("EP-MASTER-UI KD_ORDER 수정 (IT_OPS)")
    @Transactional
    public KdOrder update(UUID id, String hoseId, int orderQty, int remainingQty,
                          LocalDate orderDate, String customerCode, KdOrder.Status status,
                          String actor) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("kd_order_id 미존재: " + id);
        }
        KdOrder k = new KdOrder(id, hoseId, orderQty, remainingQty, orderDate,
            customerCode, status, clock.instant(), actor);
        log.info("EP-MASTER-UI kd update — id={} remaining={}", id, remainingQty);
        return repository.save(k);
    }

    @Auditable("EP-MASTER-UI KD_ORDER 삭제 (IT_OPS)")
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("kd_order_id 미존재: " + id);
        }
        repository.deleteById(id);
        log.info("EP-MASTER-UI kd delete — id={}", id);
    }
}
