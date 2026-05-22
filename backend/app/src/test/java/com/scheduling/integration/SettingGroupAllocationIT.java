package com.scheduling.integration;

import com.scheduling.ex.grouping.SettingGroupAllocator;
import com.scheduling.ex.grouping.ShiftAssignment;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.master.api.HoseSettingGroupSummary;
import com.scheduling.master.api.SettingGroupLookup;
import com.scheduling.master.api.SettingGroupSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-09 ST-09-1 통합 IT — V021 setting_group + product_setting_group seed + Allocator.
 *
 * <p>4주 호라이즌 회귀 — shift 내 셋업 0건 (BR-E06) + 같은 그룹 hose 동시 생산 (BR-E07).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SettingGroupAllocationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
    }

    @Autowired private SettingGroupAllocator allocator;
    @Autowired private SettingGroupLookup lookup;

    // ---------- V021 seed ----------

    @Test
    @DisplayName("V021 setting_group seed — 8 그룹 (G1-소형 ~ G8-우측)")
    void setting_group_seed_8_groups() {
        List<SettingGroupSummary> groups = lookup.findAllGroups();
        assertThat(groups).hasSize(8);
        assertThat(groups).extracting(SettingGroupSummary::groupNumber)
            .containsExactly((short) 1, (short) 2, (short) 3, (short) 4,
                (short) 5, (short) 6, (short) 7, (short) 8);
    }

    @Test
    @DisplayName("V021 product_setting_group seed — 핵심 11 매핑 (28422-08HA0→G6, 28421-2M800→G7+G2 etc.)")
    void product_setting_group_seed() {
        Optional<HoseSettingGroupSummary> primary = lookup.findPrimaryGroup("28422-08HA0");
        assertThat(primary).isPresent();
        assertThat(primary.get().groupNumber()).isEqualTo((short) 6);

        List<HoseSettingGroupSummary> g28421 = lookup.findGroupsForHose("28421-2M800");
        assertThat(g28421).hasSize(2);
        assertThat(g28421.get(0).primaryGroup()).isTrue();
        assertThat(g28421.get(0).groupNumber()).isEqualTo((short) 7);
        assertThat(g28421.get(1).primaryGroup()).isFalse();
        assertThat(g28421.get(1).groupNumber()).isEqualTo((short) 2);

        // 29673-2R060 (BR-E05 reference) → G5 합금형
        Optional<HoseSettingGroupSummary> g29673 = lookup.findPrimaryGroup("29673-2R060");
        assertThat(g29673).isPresent();
        assertThat(g29673.get().groupNumber()).isEqualTo((short) 5);

        // G1 (spec<7 소형) 호환 hose 3건
        List<HoseSettingGroupSummary> g1 = lookup.findHosesInGroup((short) 1);
        assertThat(g1).hasSizeGreaterThanOrEqualTo(3);
    }

    // ---------- SettingGroupAllocator — 4주 회귀 ----------

    private ExScheduleCandidate candidate(String hose, LocalDate deadline) {
        Instant now = Instant.parse("2026-05-22T00:00:00Z");
        return new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), hose, UUID.randomUUID(),
            deadline.plusDays(1), deadline, 100,
            CandidateStatus.PENDING, now, now);
    }

    @Test
    @DisplayName("4주 호라이즌 회귀 — shift 내 셋업 0건 (BR-E06)")
    void four_week_horizon_zero_setup_within_shift() {
        // 4주 × 5 영업일 = 20 영업일 × 4 hose (다양한 그룹) — 80 candidate
        LocalDate base = LocalDate.of(2026, 3, 2);  // 월
        String[] hoses = {"28442-6T010", "29673-2R060", "28421-2M800", "28422-2M800"};
        List<ExScheduleCandidate> candidates = new ArrayList<>();

        for (int week = 0; week < 4; week++) {
            for (int day = 0; day < 5; day++) {
                LocalDate d = base.plusDays(week * 7L + day);
                for (String h : hoses) {
                    candidates.add(candidate(h, d));
                }
            }
        }

        List<ShiftAssignment> assignments = allocator.allocate(candidates);

        // 각 (date, lineCode, shift) 키마다 단일 그룹 — 셋업 0건 (BR-E06)
        Map<String, Set<Short>> shiftGroups = new HashMap<>();
        for (ShiftAssignment a : assignments) {
            String key = a.date() + "/" + a.lineCode() + "/" + a.shiftCode();
            shiftGroups.computeIfAbsent(key, k -> new HashSet<>()).add(a.groupNumber());
        }

        assertThat(shiftGroups.values())
            .as("BR-E06 — 각 shift 슬롯에 단일 그룹 (셋업 0건)")
            .allMatch(groups -> groups.size() == 1);
    }

    @Test
    @DisplayName("같은 그룹 (G5 합금형) hose 동시 생산 — 1 shift 묶음 (BR-E07)")
    void same_group_hoses_co_produced() {
        LocalDate d = LocalDate.of(2026, 3, 5);
        // G5 합금형: 29673-2R060, 29673-2F900
        List<ShiftAssignment> assignments = allocator.allocate(List.of(
            candidate("29673-2R060", d),
            candidate("29673-2F900", d)));

        assertThat(assignments)
            .as("같은 그룹 (G5) → 1 shift 묶음")
            .hasSize(1);
        assertThat(assignments.get(0).candidateIds()).hasSize(2);
        assertThat(assignments.get(0).groupNumber()).isEqualTo((short) 5);
    }
}
