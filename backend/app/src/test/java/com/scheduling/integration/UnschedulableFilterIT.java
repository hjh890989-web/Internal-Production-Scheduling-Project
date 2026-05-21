package com.scheduling.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.vc.FilterResult;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.master.vc.UnschedulableFilterService;
import com.scheduling.master.vc.UnschedulableReportGenerator;
import com.scheduling.master.vc.UnschedulableReportRow;
import com.scheduling.master.vc.VcConstraint;
import com.scheduling.master.vc.VcConstraintRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-04 ST-04-2 TK-04-2-3 — UnschedulableFilter + ReportGenerator 통합 회귀.
 *
 * <p>실 PG (Testcontainers) + DS-VC-CONSTRAINT-47 정답 대조:
 * <ul>
 *   <li>{@code unschedulable_expected.json} (REF-09 zero-slot 3 품번) 정확 식별 — TC-VC-003</li>
 *   <li>46품번 입력 → schedulable + unschedulable 합계 = 46</li>
 *   <li>Excel 파일 row 수 = unschedulable 개수 (POI 재읽기 검증)</li>
 *   <li>빈 unschedulable → 파일 미생성</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class UnschedulableFilterIT {

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

    @Autowired private UnschedulableFilterService filterService;
    @Autowired private UnschedulableReportGenerator reportGenerator;
    @Autowired private VcConstraintRepository repository;
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private ObjectMapper objectMapper;

    private List<String> allHoseIds;
    private List<String> expectedUnschedulable;

    @BeforeEach
    void rebuildMatrix() throws Exception {
        // @Sql 적재 → matrix 강제 재빌드 (LISTEN/NOTIFY 비동기 race 회피)
        matrixService.invalidate();
        allHoseIds = repository.findAll().stream().map(VcConstraint::getHoseId).toList();
        expectedUnschedulable = loadJson("unschedulable_expected.json", new TypeReference<>() {});
    }

    private <T> T loadJson(String filename, TypeReference<T> typeRef) throws Exception {
        ClassPathResource resource = new ClassPathResource(
            "datasets/DS-VC-CONSTRAINT-47/" + filename);
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, typeRef);
        }
    }

    // ---------- TC-VC-003 ----------

    @Test
    @DisplayName("REF-09 zero-slot 품번 100% 식별 — DS-VC-CONSTRAINT-47 정답 대조")
    void identifies_ref09_zero_slot_products() {
        FilterResult result = filterService.separate(allHoseIds);

        assertThat(result.unschedulable())
            .as("REF-09 unschedulable_expected.json 와 일치")
            .containsExactlyInAnyOrderElementsOf(expectedUnschedulable);
        assertThat(result.unschedulable()).contains("7X375-H0020", "28415-08400");
    }

    @Test
    @DisplayName("46품번 분리 합계 정합 — schedulable + unschedulable = 입력")
    void filter_sum_matches_input() {
        FilterResult result = filterService.separate(allHoseIds);

        assertThat(result.schedulable().size() + result.unschedulable().size())
            .isEqualTo(allHoseIds.size());
        assertThat(result.matrixVersion()).isPositive();
    }

    // ---------- Excel report ----------

    @Test
    @DisplayName("Excel row 수 = unschedulable 개수 (POI 재읽기 검증)")
    void excel_row_count_matches_unschedulable(@TempDir Path tmp) throws Exception {
        FilterResult result = filterService.separate(allHoseIds);
        List<UnschedulableReportRow> rows = result.unschedulable().stream()
            .map(hoseId -> new UnschedulableReportRow(
                hoseId, LocalDate.of(2026, 6, 1), 30, "현대모비스"))
            .toList();

        Path excel = reportGenerator.generateExcel(rows, tmp);

        assertThat(excel).isNotNull().exists();
        try (InputStream is = Files.newInputStream(excel);
             Workbook wb = new XSSFWorkbook(is)) {
            int dataRowCount = wb.getSheetAt(0).getLastRowNum();  // header 제외, 0-based 마지막 row index
            assertThat(dataRowCount).isEqualTo(result.unschedulableCount());
            assertThat(wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue())
                .isIn(result.unschedulable().toArray());
        }
    }

    @Test
    @DisplayName("빈 unschedulable → Excel 파일 미생성")
    void empty_unschedulable_skips_file(@TempDir Path tmp) throws Exception {
        Path excel = reportGenerator.generateExcel(List.of(), tmp);

        assertThat(excel).isNull();
        try (Stream<Path> stream = Files.list(tmp)) {
            assertThat(stream).isEmpty();
        }
    }

    @Test
    @DisplayName("모두 schedulable 인 hose_id 만 입력 → unschedulable 0 + Excel 미생성")
    void only_schedulable_inputs() {
        List<String> onlySchedulable = allHoseIds.stream()
            .filter(h -> !expectedUnschedulable.contains(h))
            .toList();

        FilterResult result = filterService.separate(onlySchedulable);

        assertThat(result.unschedulable()).isEmpty();
        assertThat(result.hasUnschedulable()).isFalse();
        assertThat(result.schedulable()).hasSize(onlySchedulable.size());
    }
}
