package com.scheduling.vc.routing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MachineDecisionAudit 영속 — TK-05-4-2.
 *
 * <p>K-V06 KPI 집계 + TK-05-4-3 회귀 검증용.
 */
public interface MachineDecisionRepository extends JpaRepository<MachineDecisionAudit, UUID> {

    long countByDecisionType(DecisionType decisionType);

    List<MachineDecisionAudit> findByHoseId(String hoseId);

    Optional<MachineDecisionAudit> findFirstByOrderByDecidedAtAsc();

    Optional<MachineDecisionAudit> findFirstByDecisionTypeOrderByDecidedAtAsc(DecisionType decisionType);
}
