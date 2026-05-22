package com.scheduling.integration;

import com.scheduling.audit.snapshot.AuditSnapshotService;
import com.scheduling.vc.confirm.VcScheduleConfirmationService;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-19 ST-19-1 IT — AuditSnapshot 임의 시점 복원 (REQ-FUNC-OC-014).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditSnapshotIT {

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

    @Autowired private AuditSnapshotService snapshotService;
    @Autowired private VcScheduleConfirmationService confirmService;
    @Autowired private VcScheduleRepository scheduleRepo;

    private static final LocalDate PROD = LocalDate.of(2026, 6, 10);
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        scheduleRepo.deleteAll();
    }

    @Test
    @DisplayName("INSERT 직후 snapshot — rowExisted=true + action=INSERT")
    void snapshot_after_insert() {
        VcSchedule s = scheduleRepo.save(new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 5, "ANG-A", 100,
            VcScheduleStatus.CANDIDATE, "", T0, T0));

        AuditSnapshotService.SnapshotResult result = snapshotService.reconstructAt(
            "vc_schedule", s.getVcScheduleId().toString(), Instant.now());

        assertThat(result.rowExisted()).isTrue();
        assertThat(result.lastAction()).isEqualTo("INSERT");
        assertThat(result.jsonPayload()).contains("29673-2R060");
        assertThat(result.jsonPayload()).contains("CANDIDATE");
    }

    @Test
    @DisplayName("UPDATE 후 snapshot — 마지막 UPDATE 의 new_row 반영")
    void snapshot_after_update_returns_latest() {
        VcSchedule s = scheduleRepo.save(new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 5, "ANG-A", 100,
            VcScheduleStatus.CANDIDATE, "", T0, T0));

        // Planner confirm → UPDATE audit row 발행 (status CANDIDATE → CONFIRMED)
        confirmService.confirm(s.getVcScheduleId(), "planner-001");

        AuditSnapshotService.SnapshotResult result = snapshotService.reconstructAt(
            "vc_schedule", s.getVcScheduleId().toString(), Instant.now());

        assertThat(result.rowExisted()).isTrue();
        assertThat(result.lastAction()).isEqualTo("UPDATE");
        assertThat(result.jsonPayload()).contains("CONFIRMED");
    }

    @Test
    @DisplayName("과거 시점 (INSERT 이전) snapshot — rowExisted=false")
    void snapshot_before_insert() {
        VcSchedule s = scheduleRepo.save(new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 5, "ANG-A", 100,
            VcScheduleStatus.CANDIDATE, "", T0, T0));

        // 매우 과거 시점
        AuditSnapshotService.SnapshotResult result = snapshotService.reconstructAt(
            "vc_schedule", s.getVcScheduleId().toString(),
            Instant.parse("2020-01-01T00:00:00Z"));

        assertThat(result.rowExisted()).isFalse();
        assertThat(result.lastAction()).isNull();
    }

    @Test
    @DisplayName("timeline — INSERT + UPDATE 시간순 ASC")
    void timeline_returns_history_asc() {
        VcSchedule s = scheduleRepo.save(new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 5, "ANG-A", 100,
            VcScheduleStatus.CANDIDATE, "", T0, T0));
        confirmService.confirm(s.getVcScheduleId(), "planner-001");

        List<Map<String, Object>> timeline = snapshotService.timeline(
            "vc_schedule", s.getVcScheduleId().toString());

        assertThat(timeline).hasSizeGreaterThanOrEqualTo(2);
        assertThat(timeline.get(0).get("action")).isEqualTo("INSERT");
        assertThat(timeline.get(1).get("action")).isEqualTo("UPDATE");
        // INSERT 의 reason 은 system actor (BR-X02 fallback)
        assertThat(timeline.get(1).get("reason")).asString().contains("BR-X01");
    }
}
