package com.scheduling.ex.export;

import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 압출 일별 매트릭스 XLSX export — TK-12-2-1 (EP-12 ST-12-2, REQ-FUNC-EX-018).
 *
 * <p>BR-E09 — 시트명 정규식 {@code \d+월\d+일(압출)} 일치 (예: {@code "5월25일(압출)"}).
 *
 * <p>매트릭스 row = hose_id, col = 일자, value = 누적 yield.
 */
@Component
@Profile("with-infra")
public class ExtrusionMatrixExporter {

    /** BR-E09 압출 시트명 규약. */
    public static final Pattern SHEET_NAME_REGEX = Pattern.compile("^\\d+월\\d+일\\(압출\\)$");

    private final ExScheduleCandidateRepository repository;

    public ExtrusionMatrixExporter(ExScheduleCandidateRepository repository) {
        this.repository = repository;
    }

    /**
     * @param from 시작일 (포함)
     * @param to   종료일 (포함)
     * @return 일별 시트 워크북 byte
     */
    public byte[] exportMatrix(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("from/to 필수 + from ≤ to: " + from + " → " + to);
        }
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            for (long i = 0; i < days; i++) {
                LocalDate d = from.plusDays(i);
                writeDailySheet(wb, d);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("EX_MATRIX.xlsx 생성 실패", e);
        }
    }

    private void writeDailySheet(Workbook wb, LocalDate date) {
        String sheetName = formatSheetName(date);
        var sheet = wb.createSheet(sheetName);
        CellStyle headerStyle = headerStyle(wb);

        List<ExScheduleCandidate> rows = repository.findByHoseIdAndExtrusionDeadlineBetween(
            "", date, date);
        // 단일 날짜 fetch — repository 메서드 인자 hose_id 빈값은 매칭 불가, 모든 후보를 한 번에 가져온 후 필터링
        rows = repository.findAll().stream()
            .filter(c -> c.getExtrusionDeadline().equals(date))
            .toList();

        Row header = sheet.createRow(0);
        String[] headers = {"hose_id", "vc_production_date", "yield", "status"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        Map<String, Integer> aggregated = rows.stream()
            .collect(Collectors.groupingBy(
                ExScheduleCandidate::getHoseId,
                TreeMap::new,
                Collectors.summingInt(ExScheduleCandidate::getVcYield)));

        int r = 1;
        for (Map.Entry<String, Integer> e : aggregated.entrySet()) {
            // 최초 발견 row 의 status 만 표시 (단순화)
            ExScheduleCandidate first = rows.stream()
                .filter(c -> c.getHoseId().equals(e.getKey()))
                .findFirst().orElse(null);
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(first == null ? "" : first.getVcProductionDate().toString());
            row.createCell(2).setCellValue(e.getValue());
            row.createCell(3).setCellValue(first == null ? "" : first.getStatus().name());
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    /** BR-E09: {@code M월d일(압출)} — leading zero 없음. */
    public static String formatSheetName(LocalDate date) {
        return date.getMonthValue() + "월" + date.getDayOfMonth() + "일(압출)";
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
