package com.scheduling.order.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-OC-002 — 30 회귀 워크북 분류 정확도 ≥99% (TK-01-1-4).
 *
 * <p>입력 데이터셋 (DS-ORDER-3X):
 * <pre>
 *   src/test/resources/workbooks/DS-ORDER-3X/
 *     monthly/   7 .xlsx — 모두 MONTHLY_FORECAST 로 분류되어야 함
 *     weekly/    7 .xlsx — 모두 WEEKLY_PLAN
 *     confirmed/ 8 .xlsx — 모두 CONFIRMED_ORDER
 *     kd/        8 .xlsx — 모두 KD_ORDER
 *   합계 30 workbook
 * </pre>
 *
 * <p>스크립트로 합성: {@code scripts/generate_test_workbooks.py} (seed=42 재현 가능).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassificationAccuracyTest {

    private static final String DATASET_ROOT = "workbooks/DS-ORDER-3X";
    private static final double ACCURACY_THRESHOLD = 0.99;

    private final ExcelParserService parser = new ExcelParserService(20, 100, 4096);
    private final SourceClassifierService classifier = new SourceClassifierService(
        new ClassPathResource("classification/header-signatures.yaml")
    );

    {
        classifier.load();
    }

    @Test
    @DisplayName("TC-OC-002 — 30 회귀 워크북 분류 정확도 ≥99% (29/30 이상)")
    void accuracy_at_least_99_percent() throws Exception {
        List<TestCase> cases = collectAllWorkbooks();
        assertThat(cases).as("DS-ORDER-3X 30 워크북").hasSize(30);

        int correct = 0;
        StringBuilder errors = new StringBuilder();

        for (TestCase tc : cases) {
            ClassificationResult result;
            try (InputStream is = Files.newInputStream(tc.path)) {
                ParsedWorkbook wb = parser.parse(
                    tc.path.getFileName().toString(),
                    is,
                    Files.size(tc.path)
                );
                result = classifier.classify(wb);
            }
            if (result.sourceType() == tc.expected) {
                correct++;
            } else {
                errors.append(String.format("%n  - %s: expected=%s, got=%s (conf=%.2f)",
                    tc.path.getFileName(), tc.expected, result.sourceType(), result.confidence()));
            }
        }

        double accuracy = (double) correct / cases.size();
        assertThat(accuracy)
            .as("분류 정확도 %d/%d = %.1f%%%s", correct, cases.size(), accuracy * 100, errors)
            .isGreaterThanOrEqualTo(ACCURACY_THRESHOLD);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("workbookArguments")
    @DisplayName("개별 워크북 회귀 (실패 시 개별 진단 가능)")
    void individual_workbook_classifies_correctly(String filename, SourceType expected) throws Exception {
        Path path = findByFilename(filename);
        try (InputStream is = Files.newInputStream(path)) {
            ParsedWorkbook wb = parser.parse(filename, is, Files.size(path));
            ClassificationResult result = classifier.classify(wb);
            assertThat(result.sourceType())
                .as("%s (confidence=%.2f, allScores=%s)",
                    filename, result.confidence(), result.allScores())
                .isEqualTo(expected);
        }
    }

    Stream<Arguments> workbookArguments() throws Exception {
        return collectAllWorkbooks().stream()
            .map(tc -> Arguments.of(tc.path.getFileName().toString(), tc.expected));
    }

    private List<TestCase> collectAllWorkbooks() throws Exception {
        URL root = getClass().getClassLoader().getResource(DATASET_ROOT);
        if (root == null) {
            throw new IllegalStateException("DS-ORDER-3X 리소스 미존재: " + DATASET_ROOT);
        }
        Path rootPath = Paths.get(root.toURI());

        List<TestCase> cases = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(p -> p.toString().endsWith(".xlsx"))
                .sorted()
                .forEach(p -> {
                    String subdir = p.getParent().getFileName().toString().toLowerCase(Locale.ROOT);
                    cases.add(new TestCase(p, mapDir(subdir)));
                });
        }
        return cases;
    }

    private Path findByFilename(String filename) throws Exception {
        URL root = getClass().getClassLoader().getResource(DATASET_ROOT);
        if (root == null) throw new IllegalStateException("missing dataset");
        Path rootPath = Paths.get(root.toURI());
        try (Stream<Path> walk = Files.walk(rootPath)) {
            return walk.filter(p -> p.getFileName().toString().equals(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("not found: " + filename));
        }
    }

    private SourceType mapDir(String subdir) {
        return switch (subdir) {
            case "monthly"   -> SourceType.MONTHLY_FORECAST;
            case "weekly"    -> SourceType.WEEKLY_PLAN;
            case "confirmed" -> SourceType.CONFIRMED_ORDER;
            case "kd"        -> SourceType.KD_ORDER;
            default -> throw new IllegalArgumentException("unknown subdir: " + subdir);
        };
    }

    private record TestCase(Path path, SourceType expected) {}
}
