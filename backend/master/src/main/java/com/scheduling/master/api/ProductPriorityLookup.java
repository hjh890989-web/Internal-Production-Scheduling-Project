package com.scheduling.master.api;

import java.time.LocalDate;
import java.util.List;

/**
 * BR-V12 PRODUCT_PRIORITY facade — Sprint 7 (REQ-FUNC-VC-022).
 *
 * <p>vc 모듈 {@code CapacityOverflowQueueService} 가 본 인터페이스로 priority rank ASC
 * 조회.
 */
public interface ProductPriorityLookup {

    /** 본 일자 유효한 priority — rank ASC. */
    List<ProductPrioritySummary> findEffectiveAt(LocalDate at);
}
