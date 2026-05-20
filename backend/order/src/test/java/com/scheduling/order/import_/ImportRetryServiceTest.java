package com.scheduling.order.import_;

import com.scheduling.order.mapping.MappingFailure;
import com.scheduling.order.mapping.MappingResult;
import com.scheduling.order.mapping.SchemaMappingService;
import com.scheduling.order.parser.ClassificationResult;
import com.scheduling.order.parser.ParsedWorkbook;
import com.scheduling.order.parser.SourceClassifierService;
import com.scheduling.order.parser.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ImportRetryService 회귀 — TK-01-2-3 (REQ-FUNC-OC-004 라운드트립 핵심).
 */
class ImportRetryServiceTest {

    private ImportTrackingService tracking;
    private SourceClassifierService classifier;
    private SchemaMappingService mapper;
    private ImportRetryService service;

    @BeforeEach
    void setUp() {
        tracking = mock(ImportTrackingService.class);
        classifier = mock(SourceClassifierService.class);
        mapper = mock(SchemaMappingService.class);
        service = new ImportRetryService(tracking, classifier, mapper);
    }

    @Test
    @DisplayName("정상 retry — 캐시 hit → 분류 + 매핑 + MAPPED 전이")
    void retry_normal_cache_hit() {
        UUID id = UUID.randomUUID();
        ParsedWorkbook wb = new ParsedWorkbook("a.xlsx", List.of());
        when(tracking.loadParsedWorkbooks(id)).thenReturn(List.of(wb));
        when(classifier.classify(wb)).thenReturn(
            new ClassificationResult(SourceType.MONTHLY_FORECAST, 0.95, Map.of()));
        when(mapper.map(wb, SourceType.MONTHLY_FORECAST))
            .thenReturn(new MappingResult(List.of(), List.of(), SourceType.MONTHLY_FORECAST));

        service.retryAsync(id);

        verify(tracking).update(eq(id), eq(ImportStatus.PARSING), eq(null));
        verify(tracking).cacheMappingResult(eq(id), any(MappingResult.class));
        verify(tracking).update(eq(id), eq(ImportStatus.MAPPED), eq(null));
    }

    @Test
    @DisplayName("실패 row 있는 retry — PARSED 상태 유지 (보정 필요)")
    void retry_with_failures_stays_parsed() {
        UUID id = UUID.randomUUID();
        ParsedWorkbook wb = new ParsedWorkbook("a.xlsx", List.of());
        when(tracking.loadParsedWorkbooks(id)).thenReturn(List.of(wb));
        when(classifier.classify(wb)).thenReturn(
            new ClassificationResult(SourceType.MONTHLY_FORECAST, 0.95, Map.of()));
        MappingResult withFailure = new MappingResult(
            List.of(),
            List.of(new MappingFailure("Sheet1", 0, List.of(), "hose_id", "값 없음")),
            SourceType.MONTHLY_FORECAST);
        when(mapper.map(wb, SourceType.MONTHLY_FORECAST)).thenReturn(withFailure);

        service.retryAsync(id);

        verify(tracking).update(eq(id), eq(ImportStatus.PARSED), eq(null));
        verify(tracking, never()).update(eq(id), eq(ImportStatus.MAPPED), any());
    }

    @Test
    @DisplayName("TTL 만료 — 캐시 miss → FAILED + 안내 메시지")
    void retry_cache_miss_marks_failed() {
        UUID id = UUID.randomUUID();
        when(tracking.loadParsedWorkbooks(id))
            .thenThrow(new NoSuchElementException("ParsedWorkbook 캐시 만료: " + id));

        service.retryAsync(id);

        verify(tracking).update(eq(id), eq(ImportStatus.FAILED),
            eq("캐시 만료 (24h) — 원본 파일을 다시 업로드해 주세요."));
        verify(classifier, never()).classify(any());
        verify(mapper, never()).map(any(), any());
    }

    @Test
    @DisplayName("매핑 중 예외 — FAILED + 에러 메시지")
    void retry_mapping_throws_marks_failed() {
        UUID id = UUID.randomUUID();
        ParsedWorkbook wb = new ParsedWorkbook("a.xlsx", List.of());
        when(tracking.loadParsedWorkbooks(id)).thenReturn(List.of(wb));
        when(classifier.classify(wb)).thenThrow(new RuntimeException("classifier 오류"));

        service.retryAsync(id);

        verify(tracking).update(eq(id), eq(ImportStatus.FAILED), eq("classifier 오류"));
    }

    @Test
    @DisplayName("다중 워크북 — 모두 매핑 시도")
    void retry_multiple_workbooks() {
        UUID id = UUID.randomUUID();
        ParsedWorkbook wb1 = new ParsedWorkbook("a.xlsx", List.of());
        ParsedWorkbook wb2 = new ParsedWorkbook("b.xlsx", List.of());
        when(tracking.loadParsedWorkbooks(id)).thenReturn(List.of(wb1, wb2));
        when(classifier.classify(any())).thenReturn(
            new ClassificationResult(SourceType.MONTHLY_FORECAST, 0.9, Map.of()));
        when(mapper.map(any(), any())).thenReturn(
            new MappingResult(List.of(), List.of(), SourceType.MONTHLY_FORECAST));

        service.retryAsync(id);

        verify(classifier, times(2)).classify(any());
        verify(mapper, times(2)).map(any(), any());
        verify(tracking, times(2)).cacheMappingResult(eq(id), any());
    }
}
