package com.scheduling.vc.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * VcSchedule 영속 — TK-05-1-1.
 *
 * <p>{@link #findByDateRange} = CapacityLedger 빌드 hot path.
 */
public interface VcScheduleRepository extends JpaRepository<VcSchedule, UUID> {

    @Query("""
        SELECT s FROM VcSchedule s
        WHERE s.productionDate >= :fromDate
          AND s.productionDate <= :toDate
        """)
    List<VcSchedule> findByDateRange(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);
}
