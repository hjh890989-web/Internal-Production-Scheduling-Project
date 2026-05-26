package com.scheduling.master.kd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KdOrderRepository extends JpaRepository<KdOrder, UUID> {

    /** 동일 hose 의 OPEN/PARTIAL — orderDate ASC (오래된 발주 우선 소진). */
    @Query("""
        SELECT k FROM KdOrder k
        WHERE k.hoseId = :hoseId AND k.status IN
            (com.scheduling.master.kd.KdOrder.Status.OPEN,
             com.scheduling.master.kd.KdOrder.Status.PARTIAL)
        ORDER BY k.orderDate ASC
        """)
    List<KdOrder> findOpenByHose(@Param("hoseId") String hoseId);

    /** 동일 셋팅 그룹 hose 의 OPEN/PARTIAL (BR-V13 2차 우선순위). */
    @Query("""
        SELECT k FROM KdOrder k
        WHERE k.hoseId IN :hoseIds AND k.status IN
            (com.scheduling.master.kd.KdOrder.Status.OPEN,
             com.scheduling.master.kd.KdOrder.Status.PARTIAL)
        ORDER BY k.orderDate ASC
        """)
    List<KdOrder> findOpenByHoseIn(@Param("hoseIds") List<String> hoseIds);

    /** Sprint 8 EP-V13-Grafana — hose 별 remaining_qty 합계 (OPEN+PARTIAL). IT_OPS metric source. */
    @Query("""
        SELECT k.hoseId AS hoseId, SUM(k.remainingQty) AS totalRemaining
        FROM KdOrder k
        WHERE k.status IN
            (com.scheduling.master.kd.KdOrder.Status.OPEN,
             com.scheduling.master.kd.KdOrder.Status.PARTIAL)
        GROUP BY k.hoseId
        """)
    List<HoseRemainingProjection> findRemainingSumByHose();

    /** Projection for hose-level remaining qty aggregation. */
    interface HoseRemainingProjection {
        String getHoseId();
        Long getTotalRemaining();
    }
}
