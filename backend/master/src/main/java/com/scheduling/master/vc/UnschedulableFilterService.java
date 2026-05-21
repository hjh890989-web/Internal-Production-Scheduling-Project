package com.scheduling.master.vc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Unschedulable 품번 사전 필터 — TK-04-2-1 (BR-V11 / REQ-FUNC-VC-003).
 *
 * <p>{@link SlotCompatibilityMatrix#unschedulableHoseIds()} (7 슬롯 모두 X) 를 키로
 * 입력 hose_id 리스트를 (a) schedulable 과 (b) unschedulable 로 분리.
 * 후속 EP-05 회전 배치는 schedulable 만 입력 — 비현실 스케줄 생성 차단.
 *
 * <p>입력은 {@code List<String> hoseIds} — vc::api / 외부 모듈은 자체 도메인 엔티티에서
 * hose_id 만 추출하여 호출 (Modulith 경계: master::api 만 사용). 결과 매핑은 caller 책임.
 *
 * <p>{@code @Profile("with-infra")} — {@link SlotCompatibilityMatrixService} (JPA) 의존.
 */
@Service
@Profile("with-infra")
public class UnschedulableFilterService {

    private static final Logger log = LoggerFactory.getLogger(UnschedulableFilterService.class);

    private final SlotCompatibilityMatrixService matrixService;

    public UnschedulableFilterService(SlotCompatibilityMatrixService matrixService) {
        this.matrixService = matrixService;
    }

    /**
     * hose_id 리스트를 schedulable / unschedulable 로 분리.
     *
     * @param hoseIds 입력 (중복 허용 — 결과에 순서·중복 보존)
     * @return {@link FilterResult} — 분리 결과 + matrixVersion (caller 의 audit 정합)
     */
    public FilterResult separate(Collection<String> hoseIds) {
        if (hoseIds == null || hoseIds.isEmpty()) {
            SlotCompatibilityMatrix matrix = matrixService.current();
            return new FilterResult(List.of(), List.of(),
                matrix != null ? matrix.version() : 0);
        }

        SlotCompatibilityMatrix matrix = matrixService.current();
        if (matrix == null) {
            // matrix 미초기화 — 모든 hose_id 를 schedulable 로 분류 (보수적). 운영 환경에서는 @PostConstruct 후 진입.
            log.warn("Matrix 미초기화 — 모든 hose_id schedulable 로 fallback ({} 건)", hoseIds.size());
            return new FilterResult(List.copyOf(hoseIds), List.of(), 0);
        }

        Set<String> unschedSet = matrix.unschedulableHoseIds();
        List<String> schedulable = new ArrayList<>(hoseIds.size());
        List<String> unschedulable = new ArrayList<>();
        for (String hoseId : hoseIds) {
            if (unschedSet.contains(hoseId)) {
                unschedulable.add(hoseId);
            } else {
                schedulable.add(hoseId);
            }
        }

        log.info("Unschedulable filter — input={}, schedulable={}, unschedulable={}, matrixVersion={}",
            hoseIds.size(), schedulable.size(), unschedulable.size(), matrix.version());
        return new FilterResult(List.copyOf(schedulable), List.copyOf(unschedulable), matrix.version());
    }
}
