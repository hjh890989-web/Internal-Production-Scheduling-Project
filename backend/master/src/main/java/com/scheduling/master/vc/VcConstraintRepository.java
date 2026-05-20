package com.scheduling.master.vc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * VcConstraint 영속 — TK-04-1-1.
 *
 * <p>{@link #findUnschedulable} = BR-V11 Unschedulable 품번 조회 (부분 인덱스 적중).
 * {@link #findAllByHoseIds} = TK-04-1-2 Matrix 빌드 batch 조회.
 */
public interface VcConstraintRepository extends JpaRepository<VcConstraint, String> {

    /** 다중 hose_id 일괄 조회 — Matrix 빌드 hot path. */
    @Query("SELECT v FROM VcConstraint v WHERE v.hoseId IN :hoseIds")
    List<VcConstraint> findAllByHoseIds(@Param("hoseIds") List<String> hoseIds);

    /** BR-V11 Unschedulable — 모든 7 슬롯 X 인 품번. */
    @Query("""
        SELECT v FROM VcConstraint v
        WHERE v.lpSlotTop = false AND v.lpSlotUpmid = false
          AND v.lpSlotLowmid = false AND v.lpSlotBot = false
          AND v.icSlotTop = false AND v.icSlotMid = false AND v.icSlotBot = false
        """)
    List<VcConstraint> findUnschedulable();
}
