package com.scheduling.order.api;

import com.scheduling.order.import_.ImportOrchestratorService;
import com.scheduling.order.import_.ImportStatus;
import com.scheduling.order.import_.ImportTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderImportController 회귀 테스트 — TK-01-1-3.
 *
 * <p>Controller 단위만 검증 — @Async 동작은 통합 테스트 (Sprint 1+) 에서.
 */
class OrderImportControllerTest {

    private ImportOrchestratorService orchestrator;
    private com.scheduling.order.import_.ImportRetryService retryService;
    private ImportTrackingService tracking;
    private Clock clock;
    private OrderImportController controller;

    @BeforeEach
    void setUp() {
        orchestrator = mock(ImportOrchestratorService.class);
        retryService = mock(com.scheduling.order.import_.ImportRetryService.class);
        tracking = mock(ImportTrackingService.class);
        clock = Clock.fixed(Instant.parse("2026-05-19T05:00:00Z"), ZoneId.of("Asia/Seoul"));
        controller = new OrderImportController(orchestrator, retryService, tracking, clock);
        doNothing().when(orchestrator).processAsync(any(UUID.class), anyList());
    }

    private MockMultipartFile xlsx(String name, long size) {
        byte[] content = size > 0 ? new byte[(int) Math.min(size, Integer.MAX_VALUE)] : new byte[0];
        return new MockMultipartFile("files", name, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }

    @Test
    @DisplayName("정상 — 3 파일 multipart → 202 + trackingId 반환")
    void normal_3_files() {
        List<MockMultipartFile> files = List.of(
            xlsx("monthly.xlsx", 1024),
            xlsx("weekly.xlsx", 1024),
            xlsx("kd.xlsx", 1024)
        );

        ImportResponse response = controller.importWorkbooks(List.copyOf(files));

        assertThat(response.trackingId()).isNotNull();
        assertThat(response.status()).isEqualTo(ImportStatus.QUEUED);
        assertThat(response.statusUrl()).startsWith("/api/v1/orders/import/");
        assertThat(response.filenames()).containsExactly("monthly.xlsx", "weekly.xlsx", "kd.xlsx");
        assertThat(response.acceptedAt()).isEqualTo(Instant.parse("2026-05-19T05:00:00Z"));

        verify(tracking).markStarted(eq(response.trackingId()), eq(response.filenames()));
        verify(orchestrator).processAsync(eq(response.trackingId()), anyList());
    }

    @Test
    @DisplayName("단일 파일 — 정상 처리")
    void single_file() {
        ImportResponse response = controller.importWorkbooks(List.of(xlsx("single.xlsx", 1024)));
        assertThat(response.filenames()).hasSize(1);
    }

    @Test
    @DisplayName("4 파일 — HTTP 400 (max 3)")
    void four_files_rejected() {
        List<MockMultipartFile> files = List.of(
            xlsx("a.xlsx", 100), xlsx("b.xlsx", 100), xlsx("c.xlsx", 100), xlsx("d.xlsx", 100));

        assertThatThrownBy(() -> controller.importWorkbooks(List.copyOf(files)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Maximum 3");
    }

    @Test
    @DisplayName("빈 리스트 — HTTP 400")
    void empty_list_rejected() {
        assertThatThrownBy(() -> controller.importWorkbooks(List.of()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("At least 1");
    }

    @Test
    @DisplayName("빈 파일 — HTTP 400")
    void empty_file_rejected() {
        assertThatThrownBy(() -> controller.importWorkbooks(List.of(xlsx("empty.xlsx", 0))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Empty");
    }

    @Test
    @DisplayName("확장자 .csv — HTTP 400")
    void wrong_extension_rejected() {
        MockMultipartFile csv = new MockMultipartFile("files", "data.csv", "text/csv", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> controller.importWorkbooks(List.of(csv)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Only .xlsx");
    }

    @Test
    @DisplayName("20MB 초과 — HTTP 413")
    void oversized_rejected() {
        MockMultipartFile big = new MockMultipartFile(
            "files", "big.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1, 2}
        ) {
            @Override
            public long getSize() {
                return 21L * 1024L * 1024L;     // 21MB
            }
        };

        assertThatThrownBy(() -> controller.importWorkbooks(List.of(big)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("20MB");
    }

    @Test
    @DisplayName("status 조회 — Redis 에서 ImportStatusResponse 반환")
    void status_query() {
        UUID id = UUID.randomUUID();
        ImportStatusResponse expected = new ImportStatusResponse(
            id, ImportStatus.PARSED,
            Instant.parse("2026-05-19T04:50:00Z"),
            Instant.parse("2026-05-19T04:55:00Z"),
            List.of("file1.xlsx"),
            java.util.Map.of("file1.xlsx", "MONTHLY_FORECAST:0.95"),
            null
        );
        when(tracking.get(id)).thenReturn(expected);

        ImportStatusResponse actual = controller.status(id);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("status 조회 — 존재 안 함 → HTTP 404")
    void status_not_found() {
        UUID id = UUID.randomUUID();
        when(tracking.get(id)).thenThrow(new NoSuchElementException("Tracking ID not found"));

        // ExceptionHandler 가 NoSuchElementException → 404 변환 — 단위 테스트는 직접 throw 확인
        assertThatThrownBy(() -> controller.status(id))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("검증 실패 시 orchestrator 호출 안 됨")
    void validation_failure_short_circuits() {
        try {
            controller.importWorkbooks(List.of());
        } catch (ResponseStatusException e) {
            // expected
        }
        verify(orchestrator, never()).processAsync(any(), anyList());
        verify(tracking, never()).markStarted(any(), any());
    }

    @Test
    @DisplayName("TK-01-2-3 retry — 캐시 hit → 202 + retryAsync 호출")
    void retry_cache_hit_triggers_async() {
        UUID id = UUID.randomUUID();
        when(tracking.loadParsedWorkbooks(id))
            .thenReturn(java.util.List.of(new com.scheduling.order.parser.ParsedWorkbook("a.xlsx", java.util.List.of())));

        RetryResponse response = controller.retry(id);

        assertThat(response.trackingId()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(ImportStatus.QUEUED);
        assertThat(response.statusUrl()).isEqualTo("/api/v1/orders/import/" + id);
        verify(retryService).retryAsync(id);
    }

    @Test
    @DisplayName("TK-01-2-3 retry — 캐시 만료 → HTTP 410 Gone")
    void retry_cache_miss_returns_410() {
        UUID id = UUID.randomUUID();
        when(tracking.loadParsedWorkbooks(id))
            .thenThrow(new NoSuchElementException("ParsedWorkbook 캐시 만료: " + id));

        assertThatThrownBy(() -> controller.retry(id))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> {
                ResponseStatusException rse = (ResponseStatusException) e;
                assertThat(rse.getStatusCode().value()).isEqualTo(410);
                assertThat(rse.getReason()).contains("캐시 만료");
            });
        verify(retryService, never()).retryAsync(any());
    }
}
