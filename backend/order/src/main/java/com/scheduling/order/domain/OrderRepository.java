package com.scheduling.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Repository — TK-02-1-1·3.
 *
 * <p>JPA-based persistence. Spring `with-infra` profile 활성 시 사용 가능.
 *
 * <p>주요 메서드:
 * <ul>
 *   <li>{@link #findByHoseIdAndDeliveryDateAndStatus} — 단일 키 조회 (사전 검증)</li>
 *   <li>{@link #findActiveByHoseDeliveryPairs} — batch 조회 (N+1 회피)</li>
 * </ul>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** 단일 키 조회 — ACTIVE 만. */
    Optional<Order> findByHoseIdAndDeliveryDateAndStatus(
        String hoseId, LocalDate deliveryDate, String status);

    /**
     * batch 조회 — (hose_id, delivery_date) 다중 쌍 조회.
     *
     * <p>PostgreSQL row-value IN 호환성 회피를 위해 hose_ids + delivery_dates 별도 인자.
     * caller 는 결과를 {@link OrderKey} 로 매핑.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.status = 'ACTIVE'
          AND o.hoseId IN :hoseIds
          AND o.deliveryDate IN :deliveryDates
        """)
    List<Order> findActiveByHoseDeliveryPairs(
        @Param("hoseIds") List<String> hoseIds,
        @Param("deliveryDates") List<LocalDate> deliveryDates);

    /** 특정 master_version 의 ACTIVE row — TK-03-1-1 Diff 입력. */
    @Query("SELECT o FROM Order o WHERE o.masterVersion = :version AND o.status = 'ACTIVE'")
    List<Order> findByMasterVersion(@Param("version") int version);
}
