package com.scheduling.vc.draft;

import com.scheduling.audit.aop.Auditable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Sprint 26 S26-A ST-ORDER-1 — OrderCommittedEvent → VC 드래프트 배치 진입점.
 *
 * <p>Phase 4 carry-over Medium: Order 확정 자동 INSERT chain 활성 baseline.
 *
 * <p>현재 구현은 minimal placeholder — audit_log INSERT (actor=system, reason=auto-chain)
 * + log.info 만 처리. GreedyRotationAllocator wire-up 은 베타 운영 후 실 요구 식별 시점
 * 별도 turn 진행 (과설계 회피 — AllocatorChainListener 동일 패턴).
 *
 * <p>config flag {@code scheduling.order.auto-draft.enabled} (default false) 로 기능 ON/OFF.
 * OrderCommittedListener 에서 flag 를 조회하여 조건부 호출.
 *
 * <h3>향후 알고리즘 wire-up 계획</h3>
 * <ol>
 *   <li>AllocationContext 빌드 — OrderInput 목록 + CapacityLedger (현재 CANDIDATE/CONFIRMED 슬롯)</li>
 *   <li>GreedyRotationAllocator.allocate(ctx) 호출 → AllocationResult</li>
 *   <li>VcScheduleRepository.saveAll(result.schedules())</li>
 *   <li>VcChangedEvent 발행 → ex partial replan cascade (BR-X03)</li>
 *   <li>notify 모듈 → Planner 자동 배치 결과 웹소켓 push</li>
 * </ol>
 *
 * @see BR-X02 (audit 강제)
 * @see BR-X04 (KST Clock 주입)
 */
@Service
@Profile("with-infra")
public class VcScheduleService {

    private static final Logger log = LoggerFactory.getLogger(VcScheduleService.class);

    /**
     * scheduling.order.auto-draft.enabled (default false).
     * application.yml 에서 설정 — Agent 4 ORDER-3 범위 (본 Agent 수정 금지).
     */
    @Value("${scheduling.order.auto-draft.enabled:false}")
    private boolean autoDraftEnabled;

    /**
     * 수주 확정 이벤트 수신 후 VC 드래프트 배치 진입.
     *
     * <p>BR-X02 — @Auditable 로 audit.reason 세션 변수 주입 → DB trigger 캡쳐.
     * actor 는 AuditableAspect 가 SecurityContext 에서 추출 (비동기 listener 이므로 "system" fallback).
     *
     * @param trackingId 수주 import 추적 ID (OrderCommittedEvent.trackingId)
     * @param actor      처리 주체 (자동 chain: "system")
     * @param reason     처리 사유 (BR-X02 audit)
     */
    @Auditable("VC 드래프트 배치 진입 — auto-chain-from-order-commit (BR-X02)")
    @Transactional
    public void draftBatch(UUID trackingId, String actor, String reason) {
        log.info("ST-ORDER-1 draftBatch — trackingId={} actor={} reason='{}' autoDraftEnabled={}",
                trackingId, actor, reason, autoDraftEnabled);

        // TODO Sprint 26+ 알고리즘 wire-up:
        //   1. OrderInput 목록 조회 (order::api NamedInterface 통해 trackingId 기반)
        //   2. CapacityLedger 빌드 (현재 CANDIDATE + CONFIRMED 슬롯 로드)
        //   3. AllocationContext 빌드 (workingDays 1주 horizon, requestedBy=actor)
        //   4. GreedyRotationAllocator.allocate(ctx) → AllocationResult
        //   5. VcScheduleRepository.saveAll(result.schedules())
        //   6. 결과 요약 VcChangedEvent 발행
        //   현재: baseline log + audit trail (과설계 회피 — AllocatorChainListener 동일 패턴)
    }

    /** config flag 조회 — OrderCommittedListener 조건부 호출 판단용. */
    public boolean isAutoDraftEnabled() {
        return autoDraftEnabled;
    }
}
