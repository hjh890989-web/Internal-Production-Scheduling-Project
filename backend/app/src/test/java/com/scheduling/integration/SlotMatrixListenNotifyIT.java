package com.scheduling.integration;

import com.scheduling.master.vc.SlotCompatibilityMatrix;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.master.vc.SlotPosition;
import com.scheduling.master.vc.VcConstraint;
import com.scheduling.master.vc.VcConstraintRepository;
import org.awaitility.Awaitility;
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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-04 ST-04-1 TK-04-1-2 — SlotCompatibilityMatrix 빌드 + LISTEN/NOTIFY 무효화 IT.
 *
 * <p>실 PG 위에서 검증:
 * <ul>
 *   <li>{@link SlotCompatibilityMatrixService#initialBuild} 가 application 시작 후 매트릭스 생성</li>
 *   <li>VcConstraint INSERT → V007 트리거 pg_notify → Listener 가 수신 → matrixService.invalidate() → 새 버전</li>
 *   <li>UPDATE 시에도 동일 흐름 (version 증가)</li>
 *   <li>매트릭스 build 시간 SLA ≤ 1000ms</li>
 * </ul>
 *
 * <p>비동기 검증 — Awaitility 로 새 version 도달 대기 (LISTEN poll 5초 timeout 내).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SlotMatrixListenNotifyIT {

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

    @Autowired private VcConstraintRepository repository;
    @Autowired private SlotCompatibilityMatrixService matrixService;

    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    @BeforeEach
    void cleanState() {
        repository.deleteAll();
        // PG NOTIFY 가 cleanup INSERT/DELETE 로 트리거되어 비동기 rebuild 가 fire — 안정화 위해 잠시 대기
        try { Thread.sleep(200); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private VcConstraint vc(String hoseId, boolean lpTop, boolean lpUpmid, boolean lpLowmid, boolean lpBot,
                            boolean icTop, boolean icMid, boolean icBot) {
        return new VcConstraint(
            hoseId, 45, (short) 1,
            (short) 1, (short) 20,
            lpTop, lpUpmid, lpLowmid, lpBot,
            (short) 1, (short) 20,
            icTop, icMid, icBot,
            T0, "test"
        );
    }

    @Test
    @DisplayName("initialBuild — @PostConstruct 가 빈 DB 로 빌드 (version ≥ 1)")
    void initial_build_runs_on_startup() {
        SlotCompatibilityMatrix m = matrixService.current();
        assertThat(m).isNotNull();
        assertThat(m.version()).isPositive();
    }

    @Test
    @DisplayName("manual rebuild — version monotonic +1")
    void manual_rebuild_increments_version() {
        SlotCompatibilityMatrix before = matrixService.current();
        SlotCompatibilityMatrix after = matrixService.rebuild();

        assertThat(after.version()).isGreaterThan(before.version());
        assertThat(after.builtAt()).isAfterOrEqualTo(before.builtAt());
    }

    @Test
    @DisplayName("LISTEN/NOTIFY — VcConstraint INSERT 후 ≤ 10초 내 matrix 자동 무효화")
    void listen_notify_triggers_invalidate_on_insert() {
        int versionBefore = matrixService.current().version();

        repository.save(vc("LN-INSERT", true, false, false, false, true, true, true));

        // V007 트리거 → pg_notify → Listener thread (5초 poll) → invalidate → rebuild
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                SlotCompatibilityMatrix current = matrixService.current();
                assertThat(current.version()).isGreaterThan(versionBefore);
                assertThat(current.byHose()).containsKey("LN-INSERT");
                assertThat(current.isEligible("LN-INSERT", SlotPosition.LP_TOP)).isTrue();
                assertThat(current.isEligible("LN-INSERT", SlotPosition.LP_UPMID)).isFalse();
            });
    }

    @Test
    @DisplayName("LISTEN/NOTIFY — UPDATE 도 트리거 (slot 변경 시 matrix 자동 갱신)")
    void listen_notify_triggers_invalidate_on_update() {
        repository.save(vc("LN-UPDATE", false, false, false, false, false, false, false));
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> assertThat(matrixService.current().byHose())
                .containsKey("LN-UPDATE"));

        // 초기 — Unschedulable
        assertThat(matrixService.current().unschedulableHoseIds()).contains("LN-UPDATE");
        int versionAfterInsert = matrixService.current().version();

        // 슬롯 추가 — UPDATE → NOTIFY → invalidate
        VcConstraint updated = vc("LN-UPDATE", true, true, false, false, false, false, false);
        repository.save(updated);

        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                SlotCompatibilityMatrix current = matrixService.current();
                assertThat(current.version()).isGreaterThan(versionAfterInsert);
                assertThat(current.isEligible("LN-UPDATE", SlotPosition.LP_TOP)).isTrue();
                assertThat(current.unschedulableHoseIds()).doesNotContain("LN-UPDATE");
            });
    }

    @Test
    @DisplayName("rebuild SLA — 47품번 빌드 ≤ 1000ms (TC-VC-001)")
    void rebuild_sla_within_one_second() {
        // 47품번 시드 (병렬 saveAll)
        java.util.List<VcConstraint> seed = new java.util.ArrayList<>(47);
        for (int i = 0; i < 47; i++) {
            seed.add(vc(String.format("PERF-%03d", i),
                i % 2 == 0, i % 3 == 0, i % 5 == 0, i % 7 == 0,
                i % 2 == 1, i % 3 == 1, i % 5 == 1));
        }
        repository.saveAll(seed);

        long startNanos = System.nanoTime();
        SlotCompatibilityMatrix m = matrixService.rebuild();
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertThat(m.byHose()).hasSizeGreaterThanOrEqualTo(47);
        assertThat(elapsedMs)
            .as("47품번 빌드 SLA ≤ 1000ms (실측 %dms)", elapsedMs)
            .isLessThan(1000L);
    }
}
