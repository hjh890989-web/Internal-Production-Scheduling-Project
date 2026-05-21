package com.scheduling.vc.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 라우팅 결정 audit 영속 — TK-05-4-2 (BR-X02).
 *
 * <p>GreedyRotationAllocator 가 각 슬롯 배치마다 호출. Repository 부재 시 (DEV context)
 * 로그만 출력 (ObjectProvider fallback).
 */
@Component
public class RoutingAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(RoutingAuditLogger.class);
    private static final String DEFAULT_ACTOR = "system:allocator";

    private final ObjectProvider<MachineDecisionRepository> repositoryProvider;
    private final Clock clock;

    public RoutingAuditLogger(
        ObjectProvider<MachineDecisionRepository> repositoryProvider,
        Clock clock
    ) {
        this.repositoryProvider = repositoryProvider;
        this.clock = clock;
    }

    public void logDecision(
        String hoseId,
        LocalDate productionDate,
        String machineId,
        String machineType,
        DecisionType decisionType,
        String policyId,
        String reason
    ) {
        MachineDecisionAudit row = new MachineDecisionAudit(
            UUID.randomUUID(),
            Instant.now(clock),
            hoseId,
            productionDate,
            machineId,
            machineType,
            decisionType,
            policyId,
            reason,
            DEFAULT_ACTOR
        );
        MachineDecisionRepository repo = repositoryProvider.getIfAvailable();
        if (repo == null) {
            log.warn("[Routing-FALLBACK] Repository 부재 — audit row 미영속: hose={} machine={} decision={} policy={}",
                hoseId, machineId, decisionType, policyId);
            return;
        }
        repo.save(row);
        log.debug("Routing decision audited: hose={} {} → {} ({}, policy={})",
            hoseId, decisionType, machineId, machineType, policyId);
    }
}
