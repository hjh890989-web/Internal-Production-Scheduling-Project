package com.scheduling.ex.grouping;

import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.master.api.HoseSettingGroupSummary;
import com.scheduling.master.api.SettingGroupLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 압출 셋팅 그룹 배정 — TK-09-1-2 (EP-09 ST-09-1, BR-E06·E07).
 *
 * <p><b>알고리즘</b>:
 * <ol>
 *   <li>ExScheduleCandidate 리스트 입력 (PENDING/READY status)</li>
 *   <li>hose 별 primary 그룹 조회 (SettingGroupLookup)</li>
 *   <li>(date, lineCode) 별 그룹화 → 같은 그룹끼리 shift 배정</li>
 *   <li>shift 내 단일 그룹 강제 — 다른 그룹은 다음 shift</li>
 * </ol>
 *
 * <p>BR-E06: shift 내 셋업 변경 금지 — 본 allocator 가 그룹별로 shift 슬롯 분리.
 * BR-E07: 같은 그룹 hose 들은 동시 생산 가능 — shift 내 grouping.
 */
@Component
@Profile("with-infra")
public class SettingGroupAllocator {

    private static final Logger log = LoggerFactory.getLogger(SettingGroupAllocator.class);

    /** 4-shift 순서 — sort_order 기준 (DAY_EARLY → DAY_LATE → NIGHT_EARLY → NIGHT_LATE). */
    private static final List<String> SHIFT_ORDER = List.of(
        "DAY_EARLY", "DAY_LATE", "NIGHT_EARLY", "NIGHT_LATE");

    private final SettingGroupLookup lookup;

    public SettingGroupAllocator(SettingGroupLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * 후보 → shift 배정. 같은 (date, lineCode, group) 묶음을 단일 shift 에 할당.
     *
     * @return ShiftAssignment 리스트 (BR-E06 셋업 0건 보장)
     */
    public List<ShiftAssignment> allocate(List<ExScheduleCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        // 1. hose 별 primary group 조회
        Map<String, Short> hoseToGroup = new HashMap<>();
        for (ExScheduleCandidate c : candidates) {
            if (hoseToGroup.containsKey(c.getHoseId())) continue;
            Optional<HoseSettingGroupSummary> primary = lookup.findPrimaryGroup(c.getHoseId());
            if (primary.isPresent()) {
                hoseToGroup.put(c.getHoseId(), primary.get().groupNumber());
            } else {
                // 미매핑 hose — 그룹 0 (fallback) 으로 분리
                hoseToGroup.put(c.getHoseId(), (short) 0);
                log.warn("hose {} setting_group 미매핑 — fallback group 0", c.getHoseId());
            }
        }

        // 2. (date, lineCode, group) 별 groupingBy
        // lineCode 는 ex_candidate 자체에 없음 — Sprint 3 단순화: vc 머신 ID 를 lineCode 로 간주
        Map<GroupKey, List<UUID>> grouped = new LinkedHashMap<>();
        for (ExScheduleCandidate c : candidates) {
            short g = hoseToGroup.get(c.getHoseId());
            // line 식별 — Sprint 3 단순화: extrusionDeadline 일자 기준 (실제는 ex_constraint.line_code)
            String line = "L1";  // TODO: Sprint 4 ex_candidate 에 line_code 컬럼 추가
            GroupKey key = new GroupKey(c.getExtrusionDeadline(), line, g);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(c.getExCandidateId());
        }

        // 3. (date, lineCode) 별 그룹 → shift 순환 배정
        Map<DateLineKey, Integer> shiftIndex = new HashMap<>();
        List<ShiftAssignment> out = new ArrayList<>();
        // grouped 를 (date, line) 별로 재그룹화
        Map<DateLineKey, List<Map.Entry<GroupKey, List<UUID>>>> byDateLine = grouped.entrySet().stream()
            .collect(Collectors.groupingBy(
                e -> new DateLineKey(e.getKey().date(), e.getKey().line()),
                Collectors.toList()));

        for (Map.Entry<DateLineKey, List<Map.Entry<GroupKey, List<UUID>>>> entry : byDateLine.entrySet()) {
            DateLineKey dl = entry.getKey();
            List<Map.Entry<GroupKey, List<UUID>>> groupsForDateLine = entry.getValue();

            for (Map.Entry<GroupKey, List<UUID>> g : groupsForDateLine) {
                int idx = shiftIndex.getOrDefault(dl, 0);
                if (idx >= SHIFT_ORDER.size()) {
                    log.warn("shift 부족 — date={}, line={}, group={} 배정 실패 (4 shift 초과)",
                        dl.date(), dl.line(), g.getKey().group());
                    continue;
                }
                String shiftCode = SHIFT_ORDER.get(idx);
                out.add(new ShiftAssignment(
                    dl.date(), dl.line(), shiftCode, g.getKey().group(), g.getValue()));
                shiftIndex.put(dl, idx + 1);
            }
        }

        log.info("SettingGroupAllocator — candidates={} → assignments={} (groups={})",
            candidates.size(), out.size(), grouped.size());
        return out;
    }

    private record GroupKey(LocalDate date, String line, short group) {}
    private record DateLineKey(LocalDate date, String line) {}
}
