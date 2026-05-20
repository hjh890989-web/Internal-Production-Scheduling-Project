package com.scheduling.order.mapping;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.order.parser.ClassificationResult;
import com.scheduling.order.parser.ExcelParserService;
import com.scheduling.order.parser.ParsedWorkbook;
import com.scheduling.order.parser.SourceClassifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * TC-OC-003 — 자동 매핑 ≥95% 통합 회귀 (TK-01-2-4).
 *
 * <p>DS-ORDER-3X 30 워크북 (TK-01-1-4 산출) 을 통해 전체 pipeline 검증:
 * <ol>
 *   <li>{@link ExcelParserService#parse} — 워크북 → {@link ParsedWorkbook}</li>
 *   <li>{@link SourceClassifierService#classify} — SourceType 분류</li>
 *   <li>{@link SchemaMappingService#map} — 표준 OrderDraft 변환</li>
 * </ol>
 *
 * <p>총 row 수 대비 성공 매핑 row 수 ≥ 95% (목표). 실측은 합성 데이터 100% 예상.
 *
 * <p>실 데이터 회귀 보강 (P1 김정훈 주임 워크북) — Phase 0 측정 후 본 테스트에 추가.
 */
class MappingAccuracyTest {

    private static final String DATASET_ROOT = "workbooks/DS-ORDER-3X";
    private static final double ACCURACY_THRESHOLD = 0.95;

    private ExcelParserService parser;
    private SourceClassifierService classifier;
    private SchemaMappingService mapper;

    @BeforeEach
    void setUp() {
        parser = new ExcelParserService(20, 100, 4096);
        classifier = new SourceClassifierService(
            new ClassPathResource("classification/header-signatures.yaml"));
        classifier.load();

        MappingRuleLoader ruleLoader = new MappingRuleLoader();
        ruleLoader.load();
        SchedulingMetrics metricsMock = mock(SchedulingMetrics.class);
        mapper = new SchemaMappingService(ruleLoader, new FieldNormalizer(), metricsMock);
    }

    @Test
    @DisplayName("TC-OC-003 — DS-ORDER-3X 30 워크북 평균 매핑 성공률 ≥95%")
    void overall_mapping_success_rate_at_least_95() throws Exception {
        List<Path> workbooks = collectAllWorkbooks();
        assertThat(workbooks).as("DS-ORDER-3X 30 워크북").hasSize(30);

        int totalRows = 0;
        int totalSuccess = 0;
        StringBuilder perWorkbookReport = new StringBuilder();

        for (Path path : workbooks) {
            try (InputStream is = Files.newInputStream(path)) {
                ParsedWorkbook wb = parser.parse(path.getFileName().toString(), is, Files.size(path));
                ClassificationResult cls = classifier.classify(wb);
                MappingResult result = mapper.map(wb, cls.sourceType());

                totalRows += result.totalRows();
                totalSuccess += result.successes().size();

                if (result.successRate() < ACCURACY_THRESHOLD) {
                    perWorkbookReport.append(String.format("%n  ⚠️ %s: rate=%.1f%% (%d/%d)",
                        path.getFileName(),
                        result.successRate() * 100,
                        result.successes().size(),
                        result.totalRows()));
                }
            }
        }

        double overallRate = totalRows == 0 ? 0.0 : (double) totalSuccess / totalRows;
        assertThat(overallRate)
            .as("전체 매핑 성공률 %d/%d = %.1f%% (목표 ≥%.0f%%)%s",
                totalSuccess, totalRows, overallRate * 100, ACCURACY_THRESHOLD * 100, perWorkbookReport)
            .isGreaterThanOrEqualTo(ACCURACY_THRESHOLD);
    }

    @Test
    @DisplayName("TC-OC-003 보조 — SourceType 별 성공률 ≥95% 확인")
    void per_source_type_success_rate_at_least_95() throws Exception {
        List<Path> workbooks = collectAllWorkbooks();
        java.util.Map<com.scheduling.order.parser.SourceType, int[]> stats = new java.util.EnumMap<>(com.scheduling.order.parser.SourceType.class);

        for (Path path : workbooks) {
            try (InputStream is = Files.newInputStream(path)) {
                ParsedWorkbook wb = parser.parse(path.getFileName().toString(), is, Files.size(path));
                ClassificationResult cls = classifier.classify(wb);
                MappingResult result = mapper.map(wb, cls.sourceType());
                stats.computeIfAbsent(cls.sourceType(), k -> new int[2]);
                stats.get(cls.sourceType())[0] += result.totalRows();
                stats.get(cls.sourceType())[1] += result.successes().size();
            }
        }

        for (var entry : stats.entrySet()) {
            int total = entry.getValue()[0];
            int success = entry.getValue()[1];
            double rate = total == 0 ? 0.0 : (double) success / total;
            assertThat(rate)
                .as("%s rate=%.1f%% (%d/%d)", entry.getKey(), rate * 100, success, total)
                .isGreaterThanOrEqualTo(ACCURACY_THRESHOLD);
        }
    }

    private List<Path> collectAllWorkbooks() throws Exception {
        URL root = getClass().getClassLoader().getResource(DATASET_ROOT);
        if (root == null) {
            throw new IllegalStateException("DS-ORDER-3X 리소스 미존재");
        }
        Path rootPath = Paths.get(root.toURI());
        List<Path> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(p -> p.toString().endsWith(".xlsx")).sorted().forEach(result::add);
        }
        return result;
    }

}
