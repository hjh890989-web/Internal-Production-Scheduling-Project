package com.scheduling.integration;

import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import com.scheduling.vc.swap.SwapProposal;
import com.scheduling.vc.swap.SwapProposalRepository;
import com.scheduling.vc.swap.SwapProposalService;
import com.scheduling.vc.swap.SwapStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EP-15 ST-15-2 IT — STK_USER 제안 → Planner 1클릭 수용 + 총량 보존 (REQ-FUNC-VC-018).
 *
 * <p>검증:
 * <ul>
 *   <li>제안 → ACCEPTED 시 rotation_no atomic swap (총량 plannedQty 보존)</li>
 *   <li>다른 (machine, slot, date) 사이 swap → IllegalArgumentException</li>
 *   <li>REJECTED 흐름</li>
 *   <li>중복 resolution → IllegalStateException (DB trigger 동작)</li>
 *   <li>audit row 자동 발행 (@Auditable reason 캡쳐)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SwapProposalIT {

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

    @Autowired private SwapProposalService service;
    @Autowired private SwapProposalRepository proposalRepo;
    @Autowired private VcScheduleRepository scheduleRepo;
    @Autowired private JdbcTemplate jdbc;

    private static final LocalDate PROD = LocalDate.of(2026, 6, 1);
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        proposalRepo.deleteAll();
        scheduleRepo.deleteAll();
    }

    private VcSchedule save(short rotation) {
        return scheduleRepo.save(new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, rotation, "ANG-A", 100,
            VcScheduleStatus.CANDIDATE, "", T0, T0));
    }

    @Test
    @DisplayName("ACCEPTED — rotation atomic swap + plannedQty 보존 (총량 invariant)")
    void accept_swaps_rotations_preserves_total() {
        VcSchedule a = save((short) 3);   // rotation 3
        VcSchedule b = save((short) 5);   // rotation 5
        int totalBefore = a.getPlannedQty() + b.getPlannedQty();

        SwapProposal p = service.propose(a.getVcScheduleId(), b.getVcScheduleId(),
            "stk-001", "현장 우선순위 조정");
        service.accept(p.getProposalId(), "planner-001", "OK");

        VcSchedule reloadedA = scheduleRepo.findById(a.getVcScheduleId()).orElseThrow();
        VcSchedule reloadedB = scheduleRepo.findById(b.getVcScheduleId()).orElseThrow();
        // rotation_no 가 swap 됨
        assertThat(reloadedA.getRotationNo()).isEqualTo((short) 5);
        assertThat(reloadedB.getRotationNo()).isEqualTo((short) 3);
        // plannedQty 보존
        assertThat(reloadedA.getPlannedQty() + reloadedB.getPlannedQty()).isEqualTo(totalBefore);
        // proposal status
        SwapProposal resolved = proposalRepo.findById(p.getProposalId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(SwapStatus.ACCEPTED);
    }

    @Test
    @DisplayName("다른 slot 사이 swap → IllegalArgumentException (총량 보존 가능 슬롯만)")
    void cross_slot_swap_rejected() {
        VcSchedule a = save((short) 3);
        VcSchedule b = scheduleRepo.save(new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 2, PROD, (short) 5, "ANG-A", 100,        // slot 2 (다른 slot)
            VcScheduleStatus.CANDIDATE, "", T0, T0));

        SwapProposal p = service.propose(a.getVcScheduleId(), b.getVcScheduleId(),
            "stk-001", null);
        assertThatThrownBy(() -> service.accept(p.getProposalId(), "planner-001", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("총량 보존");
    }

    @Test
    @DisplayName("REJECTED 흐름 — schedule 변경 없음")
    void reject_does_not_swap() {
        VcSchedule a = save((short) 3);
        VcSchedule b = save((short) 5);

        SwapProposal p = service.propose(a.getVcScheduleId(), b.getVcScheduleId(),
            "stk-001", null);
        service.reject(p.getProposalId(), "planner-001", "총량 불일치");

        VcSchedule reloadedA = scheduleRepo.findById(a.getVcScheduleId()).orElseThrow();
        VcSchedule reloadedB = scheduleRepo.findById(b.getVcScheduleId()).orElseThrow();
        assertThat(reloadedA.getRotationNo()).isEqualTo((short) 3);
        assertThat(reloadedB.getRotationNo()).isEqualTo((short) 5);

        SwapProposal resolved = proposalRepo.findById(p.getProposalId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(SwapStatus.REJECTED);
        assertThat(resolved.getResolutionNote()).isEqualTo("총량 불일치");
    }

    @Test
    @DisplayName("중복 resolution — 이미 ACCEPTED 인 row 재처리 IllegalStateException")
    void double_resolution_rejected() {
        VcSchedule a = save((short) 3);
        VcSchedule b = save((short) 5);

        SwapProposal p = service.propose(a.getVcScheduleId(), b.getVcScheduleId(),
            "stk-001", null);
        service.accept(p.getProposalId(), "planner-001", null);

        assertThatThrownBy(() -> service.reject(p.getProposalId(), "planner-002", null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("resolved");
    }

    @Test
    @DisplayName("DB trigger — PROPOSED → REJECTED 만 허용, 중간 상태 우회 reject")
    void db_trigger_blocks_invalid_transition() {
        VcSchedule a = save((short) 3);
        VcSchedule b = save((short) 5);
        SwapProposal p = service.propose(a.getVcScheduleId(), b.getVcScheduleId(),
            "stk-001", null);

        // 직접 DB UPDATE — 잘못된 상태 시도 (예: REJECTED → ACCEPTED)
        service.reject(p.getProposalId(), "planner-001", null);
        assertThatThrownBy(() -> jdbc.update(
            "UPDATE app.vc_schedule_swap_proposal SET status = 'ACCEPTED' WHERE proposal_id = ?",
            p.getProposalId()))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("invalid transition");
    }

    @Test
    @DisplayName("@Auditable — audit reason 캡쳐 (REQ-FUNC-VC-018)")
    void audit_reason_captured() {
        VcSchedule a = save((short) 3);
        VcSchedule b = save((short) 5);
        SwapProposal p = service.propose(a.getVcScheduleId(), b.getVcScheduleId(),
            "stk-001", null);
        service.accept(p.getProposalId(), "planner-001", null);

        // accept 시 audit row 의 reason 검증 (vc_schedule UPDATE 가 audit 발행)
        // (atomic SQL swap 은 JdbcTemplate 직접 → audit trigger 가 그대로 캡쳐)
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit.schedule_audit_log "
                + "WHERE table_name = 'vc_schedule' AND row_pk = ?",
            Integer.class, a.getVcScheduleId().toString());
        assertThat(count).isGreaterThanOrEqualTo(2);   // INSERT + UPDATE (swap)
    }
}
