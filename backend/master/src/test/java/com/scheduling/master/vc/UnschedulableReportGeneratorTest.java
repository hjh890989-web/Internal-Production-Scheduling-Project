package com.scheduling.master.vc;

import com.scheduling.common.metrics.SchedulingMetrics;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UnschedulableReportGeneratorTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-05-21T05:30:45Z"), ZoneId.of("Asia/Seoul"));

    private SchedulingMetrics metrics;
    private UnschedulableReportGenerator generator;

    @BeforeEach
    void setUp() {
        metrics = mock(SchedulingMetrics.class);
        generator = new UnschedulableReportGenerator(metrics, CLOCK);
    }

    @Test
    @DisplayName("3 rows — Excel 6 컬럼 (품번·납기·수량·거래처·사유·권장조치) 한국어 헤더 + 데이터")
    void generates_excel_with_korean_headers_and_data(@TempDir Path tmp) throws Exception {
        List<UnschedulableReportRow> rows = List.of(
            new UnschedulableReportRow("7X375-H0020", LocalDate.of(2026, 6, 1), 30, "현대모비스"),
            new UnschedulableReportRow("28415-08400", LocalDate.of(2026, 6, 2), 50, "기아"),
            new UnschedulableReportRow("37863-8EXJ0", LocalDate.of(2026, 6, 3), 20, "내수")
        );

        Path out = generator.generateExcel(rows, tmp);

        assertThat(out).isNotNull().exists();
        assertThat(out.getFileName().toString()).startsWith("unschedulable_").endsWith(".xlsx");
        assertThat(Files.size(out)).isPositive();

        try (InputStream is = Files.newInputStream(out); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet("Unschedulable");
            assertThat(sheet).isNotNull();

            // 헤더 — 한국어 6 컬럼
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("품번");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("납기일");
            assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("수량");
            assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("거래처");
            assertThat(sheet.getRow(0).getCell(4).getStringCellValue()).isEqualTo("사유");
            assertThat(sheet.getRow(0).getCell(5).getStringCellValue()).isEqualTo("권장 조치");

            // 데이터 row 1
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("7X375-H0020");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("2026-06-01");
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(30.0);
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("현대모비스");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).contains("BR-V11");
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).contains("ASM-10");

            // 3 데이터 row
            assertThat(sheet.getLastRowNum()).isEqualTo(3);   // header + 3
        }

        verify(metrics).increment("vc_unschedulable", "report_generated");
    }

    @Test
    @DisplayName("빈 rows → null 반환 + 파일 미생성 + metric 미발행")
    void empty_rows_returns_null(@TempDir Path tmp) throws Exception {
        Path out = generator.generateExcel(List.of(), tmp);

        assertThat(out).isNull();
        try (var stream = Files.list(tmp)) {
            assertThat(stream).isEmpty();
        }
    }

    @Test
    @DisplayName("null rows → null 반환 (defensive)")
    void null_rows_returns_null(@TempDir Path tmp) throws Exception {
        assertThat(generator.generateExcel(null, tmp)).isNull();
    }

    @Test
    @DisplayName("nullable 필드 (deliveryDate/customer null) — 안전 처리")
    void nullable_fields_handled(@TempDir Path tmp) throws Exception {
        List<UnschedulableReportRow> rows = List.of(
            new UnschedulableReportRow("X-NULL", null, 10, null)
        );

        Path out = generator.generateExcel(rows, tmp);

        assertThat(out).exists();
        try (InputStream is = Files.newInputStream(out); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet("Unschedulable");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEmpty();
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEmpty();
        }
    }

    @Test
    @DisplayName("파일명 — unschedulable_yyyyMMdd_HHmmss.xlsx (KST 포맷)")
    void filename_kst_format(@TempDir Path tmp) throws Exception {
        Path out = generator.generateExcel(
            List.of(new UnschedulableReportRow("A", LocalDate.of(2026, 6, 1), 1, "x")), tmp);

        // 2026-05-21T05:30:45Z → KST 2026-05-21 14:30:45
        assertThat(out.getFileName().toString()).isEqualTo("unschedulable_20260521_143045.xlsx");
    }
}
