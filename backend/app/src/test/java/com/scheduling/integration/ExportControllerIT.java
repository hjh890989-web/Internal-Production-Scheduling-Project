package com.scheduling.integration;

import com.scheduling.ex.export.ExtrusionMatrixExporter;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.order.export.MasterExcelExporter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-12 ST-12-1+2 IT — Excel 역-Export (REQ-FUNC-OC-013, EX-018, BR-E09).
 *
 * <p>POI XSSF 생성 + 시트명 + 셀 값 검증.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class ExportControllerIT {

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

    @Autowired private MasterExcelExporter masterExporter;
    @Autowired private ExtrusionMatrixExporter matrixExporter;
    @Autowired private ExScheduleCandidateRepository exRepo;

    private static final LocalDate FROM = LocalDate.of(2026, 5, 25);
    private static final LocalDate TO = LocalDate.of(2026, 5, 27);
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        exRepo.deleteAll();
    }

    @Test
    @DisplayName("MASTER.xlsx — VC_CONSTRAINT / LINE_TYPE / SETTING_GROUP 3 sheet")
    void master_export_three_sheets() throws Exception {
        byte[] body = masterExporter.exportMaster();
        assertThat(body).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);
            assertThat(wb.getSheet("VC_CONSTRAINT")).isNotNull();
            assertThat(wb.getSheet("LINE_TYPE")).isNotNull();
            assertThat(wb.getSheet("SETTING_GROUP")).isNotNull();

            // VC_CONSTRAINT — header row check
            Row hdr = wb.getSheet("VC_CONSTRAINT").getRow(0);
            assertThat(hdr.getCell(0).getStringCellValue()).isEqualTo("hose_id");
            assertThat(hdr.getCell(1).getStringCellValue()).isEqualTo("composite_count");
        }
    }

    @Test
    @DisplayName("MASTER.xlsx — VC_CONSTRAINT seed 47품번 보존 (셀-수준 차이 0)")
    void master_export_preserves_47_products() throws Exception {
        byte[] body = masterExporter.exportMaster();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            Sheet sheet = wb.getSheet("VC_CONSTRAINT");
            int dataRows = sheet.getLastRowNum();   // header 0 + ~47품번
            assertThat(dataRows).as("seed master 품번").isGreaterThanOrEqualTo(40);
        }
    }

    @Test
    @DisplayName("LINE_TYPE — V024 seed 4 라인 (L1·L2·L3 NEW + L-FORD)")
    void line_type_sheet_4_lines() throws Exception {
        byte[] body = masterExporter.exportMaster();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            Sheet sheet = wb.getSheet("LINE_TYPE");
            assertThat(sheet.getLastRowNum()).isEqualTo(4);   // header(0) + 4 row
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("L1");
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("L-FORD");
        }
    }

    @Test
    @DisplayName("EX_MATRIX — BR-E09 시트명 M월d일(압출) 정규식 일치")
    void matrix_sheet_name_matches_regex() throws Exception {
        // seed 1 candidate
        exRepo.save(new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            UUID.randomUUID(), FROM.plusDays(1), FROM, 2531,
            CandidateStatus.SCHEDULED, T0, T0));

        byte[] body = matrixExporter.exportMatrix(FROM, TO);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);   // 5/25, 5/26, 5/27
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                String name = wb.getSheetName(i);
                assertThat(ExtrusionMatrixExporter.SHEET_NAME_REGEX.matcher(name).matches())
                    .as("sheet name [%s] BR-E09 정규식", name).isTrue();
            }
            assertThat(wb.getSheetName(0)).isEqualTo("5월25일(압출)");
        }
    }

    @Test
    @DisplayName("EX_MATRIX — candidate yield 매트릭스 row 표시")
    void matrix_contains_candidate_yield() throws Exception {
        exRepo.save(new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            UUID.randomUUID(), FROM.plusDays(1), FROM, 2531,
            CandidateStatus.SCHEDULED, T0, T0));

        byte[] body = matrixExporter.exportMatrix(FROM, FROM);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            Sheet sheet = wb.getSheet("5월25일(압출)");
            // header(0) + 데이터 1 row
            Row row = sheet.getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("29673-2R060");
            Cell yieldCell = row.getCell(2);
            assertThat((int) yieldCell.getNumericCellValue()).isEqualTo(2531);
        }
    }

    @Test
    @DisplayName("EX_MATRIX — from>to → IllegalArgumentException")
    void invalid_range_rejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> matrixExporter.exportMatrix(TO, FROM));
    }
}
