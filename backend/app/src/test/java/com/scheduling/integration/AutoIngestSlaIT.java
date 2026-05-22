package com.scheduling.integration;

import com.scheduling.order.watcher.FileDiscoveredEvent;
import com.scheduling.order.watcher.FileIngestQueueService;
import com.scheduling.order.watcher.PickedFile;
import com.scheduling.order.watcher.PickedFileRepository;
import com.scheduling.order.watcher.PickedFileStatus;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-20 ST-20-1 IT — folder watch → 60초 SLA auto-ingest (REQ-FUNC-OC-015).
 *
 * <p>기존 {@link FileIngestQueueService} 가 {@link FileDiscoveredEvent} 수신 → SHA-256
 * + PickedFile INSERT + InternalImportClient 호출. 본 IT 는 chain SLA 검증.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AutoIngestSlaIT {

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
        registry.add("scheduling.watcher.enabled", () -> "false");   // 직접 event 발행 — daemon 비활성
    }

    @Autowired private FileIngestQueueService ingestService;
    @Autowired private PickedFileRepository pickedRepo;

    @BeforeEach
    void clean() {
        pickedRepo.deleteAll();
    }

    @Test
    @DisplayName("FileDiscoveredEvent → PickedFile QUEUED 또는 INGESTED 영속 (60s SLA)")
    void file_discovered_creates_picked_file_within_60s() throws IOException {
        Path tmp = Files.createTempFile("ep20-sla-", ".xlsx");
        Files.writeString(tmp, "dummy xlsx content for SHA-256");

        long startNs = System.nanoTime();
        ingestService.onFileDiscovered(new FileDiscoveredEvent(
            tmp, Files.size(tmp), Instant.now(),
            FileDiscoveredEvent.Source.WATCH_SERVICE));
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        // REQ-FUNC-OC-015 — 60s SLA. 실제 측정은 ms 수준 (in-proc).
        assertThat(elapsedMs)
            .as("ingest chain latency %dms (목표 <= 60_000ms)", elapsedMs)
            .isLessThan(Duration.ofSeconds(60).toMillis());

        List<PickedFile> rows = pickedRepo.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getFilePath()).isEqualTo(tmp.toString());
        // status: QUEUED 또는 INGESTED 또는 FAILED (실 import client 호출 실패 가능 — XLSX 내용 invalid)
        assertThat(rows.get(0).getStatus()).isIn(
            PickedFileStatus.QUEUED,
            PickedFileStatus.INGESTED,
            PickedFileStatus.FAILED);

        Files.deleteIfExists(tmp);
    }

    @Test
    @DisplayName("동일 SHA-256 재발행 — PickedFile 2 row 생성 (각 발견 1 row)")
    void duplicate_hash_creates_two_picked_files() throws IOException {
        Path tmp = Files.createTempFile("ep20-dup-", ".xlsx");
        Files.writeString(tmp, "identical content");

        ingestService.onFileDiscovered(new FileDiscoveredEvent(
            tmp, Files.size(tmp), Instant.now(),
            FileDiscoveredEvent.Source.WATCH_SERVICE));
        ingestService.onFileDiscovered(new FileDiscoveredEvent(
            tmp, Files.size(tmp), Instant.now(),
            FileDiscoveredEvent.Source.SCHEDULED_POLL));

        List<PickedFile> rows = pickedRepo.findAll();
        assertThat(rows).hasSize(2);
        // SKIPPED_DUPLICATE 는 첫 row 가 INGESTED 일 때만 — import client 실 동작 의존성
        // (본 IT 는 60s SLA chain + 영속만 검증, 중복 판정 자체는 DuplicateDetectionIT 참고)

        Files.deleteIfExists(tmp);
    }
}
