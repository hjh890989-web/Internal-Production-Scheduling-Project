package com.scheduling.order.diff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * OrderChange 영속 — TK-03-1-3.
 *
 * <p>주요 조회:
 * <ul>
 *   <li>{@link #findByTrackingIdOrderByChangedAtAsc} — import 추적 단위 전체 diff</li>
 *   <li>{@link #findByPreviousVersionAndNewVersion} — 버전 간 비교 (시점 복원)</li>
 * </ul>
 */
@Repository
public interface OrderChangeRepository extends JpaRepository<OrderChangeEntity, UUID> {

    List<OrderChangeEntity> findByTrackingIdOrderByChangedAtAsc(UUID trackingId);

    @Query("""
        SELECT c FROM OrderChangeEntity c
        WHERE c.previousVersion = :previousVersion
          AND c.newVersion = :newVersion
        ORDER BY c.changedAt ASC
        """)
    List<OrderChangeEntity> findByPreviousVersionAndNewVersion(
        @Param("previousVersion") int previousVersion,
        @Param("newVersion") int newVersion);
}
