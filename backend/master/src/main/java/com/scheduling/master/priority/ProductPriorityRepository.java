package com.scheduling.master.priority;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductPriorityRepository extends JpaRepository<ProductPriority, String> {

    /** 본 일자 유효한 priority — rank ASC. */
    @Query("""
        SELECT p FROM ProductPriority p
        WHERE p.effectiveFrom <= :at
          AND (p.effectiveTo IS NULL OR p.effectiveTo >= :at)
        ORDER BY p.priorityRank ASC
        """)
    List<ProductPriority> findEffectiveAt(@Param("at") LocalDate at);
}
