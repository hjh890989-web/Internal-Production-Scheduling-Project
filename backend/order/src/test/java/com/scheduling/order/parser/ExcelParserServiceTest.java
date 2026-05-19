package com.scheduling.order.parser;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExcelParserService 회귀 테스트 — TK-01-1-1 §AC.
 *
 * <p>8 케이스 (Story DoD):
 *   1. 정상 — 100row × 20col
 *   2. 대용량 — 1k row (10k 는 부하 테스트에서, 단위 테스트는 1k 로 빠르게)
 *   3. 초과 — FILE_TOO_LARGE
 *   4. 병합 셀
 *   5. 다중 시트
 *   6. 수식 셀
 *   7. 날짜 셀
 *   8. 손상된 .xlsx
 */
class ExcelParserServiceTest {

    private final ExcelParserService parser = new ExcelParserService(20, 100, 4096);

    @Test
    @DisplayName("정상 — 100row × 20col 워크북")
    void normal_100x20() throws IOException {
        byte[] xlsx = buildWorkbook(wb -> {
            Sheet sheet = wb.createSheet("Orders");
            for (int r = 0; r < 100; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < 20; c++) {
                    row.createCell(c).setCellValue("R" + r + "C" + c);
                }
            }
        });

        ParsedWorkbook result = parser.parse("orders.xlsx", new ByteArrayInputStream(xlsx), xlsx.length);

        assertThat(result.filename()).isEqualTo("orders.xlsx");
        assertThat(result.sheetCount()).isEqualTo(1);
        assertThat(result.sheet(0).name()).isEqualTo("Orders");
        assertThat(result.sheet(0).rowCount()).isEqualTo(100);
        assertThat(result.sheet(0).row(0).cell(0)).isEqualTo("R0C0");
        assertThat(result.sheet(0).row(99).cell(19)).isEqualTo("R99C19");
    }

    @Test
    @DisplayName("대용량 — 1000 row 워크북 (10k 는 부하 테스트 TC-PER-001 에서)")
    void large_1000_rows() throws IOException {
        byte[] xlsx = buildWorkbook(wb -> {
            Sheet sheet = wb.createSheet("Big");
            for (int r = 0; r < 1000; r++) {
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue("hose-" + r);
                row.createCell(1).setCellValue(r);
            }
        });

        long t0 = System.nanoTime();
        ParsedWorkbook result = parser.parse("big.xlsx", new ByteArrayInputStream(xlsx), xlsx.length);
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;

        assertThat(result.sheet(0).rowCount()).isEqualTo(1000);
        // 1k row 단위 테스트 SLA — 충분히 빠름
        assertThat(elapsedMs).isLessThan(5_000);
    }

    @Test
    @DisplayName("초과 — 20MB 한도 위반 시 FILE_TOO_LARGE")
    void file_too_large() {
        byte[] dummy = new byte[100];   // 실제 내용 불필요 — size 만 보고 거부
        long fakeSize = 21L * 1024L * 1024L;     // 21MB

        assertThatThrownBy(() ->
            parser.parse("oversize.xlsx", new ByteArrayInputStream(dummy), fakeSize)
        )
        .isInstanceOf(ExcelParseException.class)
        .satisfies(e -> {
            ExcelParseException pe = (ExcelParseException) e;
            assertThat(pe.getCode()).isEqualTo("FILE_TOO_LARGE");
            assertThat(pe.getMessage()).contains("oversize.xlsx", "20 MB");
        });
    }

    @Test
    @DisplayName("병합 셀 — 첫 셀에 값, 나머지 빈 문자열")
    void merged_cells() throws IOException {
        byte[] xlsx = buildWorkbook(wb -> {
            Sheet sheet = wb.createSheet("Merged");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("수주정보");
            // A1:E1 병합
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        });

        ParsedWorkbook result = parser.parse("merged.xlsx", new ByteArrayInputStream(xlsx), xlsx.length);

        ParsedRow row = result.sheet(0).row(0);
        assertThat(row.cell(0)).isEqualTo("수주정보");
        // 병합된 나머지 셀은 빈 문자열 (POI streaming 기본 동작)
        assertThat(row.cell(1)).isEmpty();
        assertThat(row.cell(2)).isEmpty();
    }

    @Test
    @DisplayName("다중 시트 — 3개 시트 모두 파싱")
    void multiple_sheets() throws IOException {
        byte[] xlsx = buildWorkbook(wb -> {
            for (int s = 1; s <= 3; s++) {
                Sheet sheet = wb.createSheet("Sheet" + s);
                Row row = sheet.createRow(0);
                row.createCell(0).setCellValue("data-" + s);
            }
        });

        ParsedWorkbook result = parser.parse("multi.xlsx", new ByteArrayInputStream(xlsx), xlsx.length);

        assertThat(result.sheetCount()).isEqualTo(3);
        assertThat(result.sheet("Sheet1").row(0).cell(0)).isEqualTo("data-1");
        assertThat(result.sheet("Sheet3").row(0).cell(0)).isEqualTo("data-3");
    }

    @Test
    @DisplayName("수식 셀 — =SUM(A1:A2) 원본 보존")
    void formula_cell_preserved() throws IOException {
        byte[] xlsx = buildWorkbook(wb -> {
            Sheet sheet = wb.createSheet("Formula");
            sheet.createRow(0).createCell(0).setCellValue(10);
            sheet.createRow(1).createCell(0).setCellValue(20);
            sheet.createRow(2).createCell(0).setCellFormula("SUM(A1:A2)");
        });

        ParsedWorkbook result = parser.parse("formula.xlsx", new ByteArrayInputStream(xlsx), xlsx.length);

        // 수식 보존 — '=SUM(A1:A2)' 또는 계산 결과 어느 쪽이든 비공백
        String cell = result.sheet(0).row(2).cell(0);
        assertThat(cell).isNotEmpty();
        // streaming reader 는 cached value 가 있으면 cached, 아니면 formula text 반환
        assertThat(cell).satisfiesAnyOf(
            s -> assertThat(s).startsWith("="),
            s -> assertThat(s).isEqualTo("30")
        );
    }

    @Test
    @DisplayName("날짜 셀 — ISO-8601 변환")
    void date_cell_iso8601() throws IOException {
        byte[] xlsx = buildWorkbook(wb -> {
            Sheet sheet = wb.createSheet("Dates");
            Calendar cal = Calendar.getInstance();
            cal.set(2026, Calendar.MAY, 19, 14, 30, 0);
            cal.set(Calendar.MILLISECOND, 0);

            org.apache.poi.ss.usermodel.CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

            org.apache.poi.ss.usermodel.Cell cell = sheet.createRow(0).createCell(0);
            cell.setCellValue(cal.getTime());
            cell.setCellStyle(dateStyle);
        });

        ParsedWorkbook result = parser.parse("dates.xlsx", new ByteArrayInputStream(xlsx), xlsx.length);

        String dateStr = result.sheet(0).row(0).cell(0);
        // ISO-8601 (UTC instant): "2026-05-19T05:30:00Z" 같은 형식 (KST 14:30 = UTC 05:30)
        assertThat(dateStr).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    }

    @Test
    @DisplayName("손상된 .xlsx — PARSE_FAILED 예외")
    void corrupted_file(@TempDir Path tmp) {
        byte[] garbage = "Not a valid xlsx file".getBytes();

        assertThatThrownBy(() ->
            parser.parse("corrupted.xlsx", new ByteArrayInputStream(garbage), garbage.length)
        )
        .isInstanceOf(ExcelParseException.class)
        .satisfies(e -> {
            ExcelParseException pe = (ExcelParseException) e;
            assertThat(pe.getCode()).isEqualTo("PARSE_FAILED");
            assertThat(pe.getMessage()).contains("corrupted.xlsx");
        });
    }

    // ---------- helpers ----------

    private byte[] buildWorkbook(WorkbookBuilder builder) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            builder.build(wb);
            wb.write(out);
            return out.toByteArray();
        }
    }

    @FunctionalInterface
    private interface WorkbookBuilder {
        void build(Workbook wb) throws IOException;
    }
}
