package com.scheduling.order.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 중복 감지 + 우선순위 해소 통합 서비스 — TK-02-2-1 (ST-02-2 entry point).
 *
 * <p>흐름:
 * <ol>
 *   <li>{@link DuplicateDetectionService#detect} — DuplicateGroup 산출</li>
 *   <li>{@link PrecedenceResolver#resolve} — 그룹별 winner 결정</li>
 *   <li>{@link PrecedenceAuditLogger#log} — BR-X02 audit (SLF4J)</li>
 * </ol>
 *
 * <p>{@code @Profile("with-infra")} — Repository 의존, DB 환경에서만 활성.
 */
@Service
@Profile("with-infra")
public class ResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionService.class);

    private final DuplicateDetectionService detector;
    private final PrecedenceResolver resolver;
    private final PrecedenceAuditLogger auditLogger;

    public ResolutionService(
        DuplicateDetectionService detector,
        PrecedenceResolver resolver,
        PrecedenceAuditLogger auditLogger
    ) {
        this.detector = detector;
        this.resolver = resolver;
        this.auditLogger = auditLogger;
    }

    /**
     * batch 의 중복 그룹을 BR-O01 우선순위로 해소.
     *
     * @return 해소된 Resolution 목록 (중복 없으면 빈 리스트)
     */
    public List<Resolution> resolveAll(List<OrderDraft> batch) {
        List<DuplicateGroup> groups = detector.detect(batch);
        if (groups.isEmpty()) {
            return List.of();
        }
        List<Resolution> resolutions = new ArrayList<>(groups.size());
        for (DuplicateGroup g : groups) {
            Resolution r = resolver.resolve(g);
            auditLogger.log(r);
            resolutions.add(r);
        }
        log.info("Resolved {} duplicate group(s) from batch of {}", resolutions.size(), batch.size());
        return resolutions;
    }
}
