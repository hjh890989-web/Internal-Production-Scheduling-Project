package com.scheduling.vc.capacity_overflow;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.master.api.KdOrderLookup;
import com.scheduling.master.api.KdOrderSummary;
import com.scheduling.master.api.SettingGroupLookup;
import com.scheduling.master.api.HoseSettingGroupSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * BR-V13 capa 부족 시 KD 잔량 보충 — Sprint 7 (REQ-FUNC-VC-023, deferred 활성).
 *
 * <p>알고리즘:
 * <ol>
 *   <li>1차: 동일 hose KD 잔량 — orderDate ASC (오래된 발주 우선)</li>
 *   <li>2차: 동일 셋팅 그룹 hose 들 KD 잔량 (셋팅 교체 없이 보충)</li>
 *   <li>atomic consume(uuid, qty) — remaining_qty 차감 + status auto-update</li>
 * </ol>
 *
 * <p>활성 조건 — 수주통합 + DI-08 KD_ORDER 마스터 입력.
 */
@Service
@Profile("with-infra")
public class KdSupplementService {

    private static final Logger log = LoggerFactory.getLogger(KdSupplementService.class);

    private final KdOrderLookup kdLookup;
    private final SettingGroupLookup settingLookup;

    public KdSupplementService(KdOrderLookup kdLookup, SettingGroupLookup settingLookup) {
        this.kdLookup = kdLookup;
        this.settingLookup = settingLookup;
    }

    public record SupplementResult(
        String hoseId,
        int shortage,           // 부족 qty
        int supplemented,       // 실 보충 qty (≤ shortage)
        List<ConsumedEntry> consumed
    ) {}

    public record ConsumedEntry(
        java.util.UUID kdOrderId,
        String fromHose,        // 동일 hose 면 본인, 아니면 그룹 멤버
        int qty
    ) {}

    /**
     * @param hoseId    부족 hose
     * @param shortage  부족 수량 (Σ Q_required - daily_capa 의 일부)
     * @param actor     audit actor (Planner 또는 system)
     * @return 보충 결과 + consumed 내역
     */
    @Auditable("BR-V13 KD 잔량 보충 (capa 부족)")
    @Transactional
    public SupplementResult supplement(String hoseId, int shortage, String actor) {
        if (shortage <= 0) {
            return new SupplementResult(hoseId, 0, 0, List.of());
        }
        List<ConsumedEntry> consumed = new ArrayList<>();
        int remaining = shortage;

        // 1차 — 동일 hose KD
        remaining = consumeFrom(kdLookup.findOpenByHose(hoseId), hoseId, remaining, consumed, actor);

        // 2차 — 동일 셋팅 그룹 hose 들 KD (셋팅 교체 없이 보충)
        if (remaining > 0) {
            List<String> groupHoseIds = settingLookup.findPrimaryGroup(hoseId)
                .map(g -> settingLookup.findHosesInGroup(g.groupNumber()))
                .orElse(List.of()).stream()
                .map(HoseSettingGroupSummary::hoseId)
                .filter(h -> !h.equals(hoseId))      // 1차에서 이미 처리
                .toList();
            if (!groupHoseIds.isEmpty()) {
                remaining = consumeFrom(
                    kdLookup.findOpenByHoseIn(groupHoseIds),
                    "group", remaining, consumed, actor);
            }
        }

        int supplemented = shortage - remaining;
        log.info("BR-V13 KD 보충 — hose={}, shortage={}, supplemented={}, consumed={} entries",
            hoseId, shortage, supplemented, consumed.size());
        return new SupplementResult(hoseId, shortage, supplemented, consumed);
    }

    private int consumeFrom(List<KdOrderSummary> kds, String sourceLabel,
                             int remaining, List<ConsumedEntry> consumed, String actor) {
        for (KdOrderSummary kd : kds) {
            if (remaining <= 0) break;
            if (!kd.isAvailable()) continue;
            int requested = Math.min(remaining, kd.remainingQty());
            int actual = kdLookup.consume(kd.kdOrderId(), requested, actor);
            if (actual > 0) {
                consumed.add(new ConsumedEntry(kd.kdOrderId(),
                    "group".equals(sourceLabel) ? kd.hoseId() : sourceLabel, actual));
                remaining -= actual;
            }
        }
        return remaining;
    }
}
