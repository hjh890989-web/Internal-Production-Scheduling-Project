package com.scheduling.master.vc;

import com.scheduling.common.metrics.SchedulingMetrics;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Unschedulable 품번 Excel 리포트 생성기 — TK-04-2-2 (ASM-10 외주·재고 대응).
 *
 * <p>POI XSSF 6 컬럼 — 품번 · 납기일 · 수량 · 거래처 · 사유 · 권장 조치.
 * 헤더 한국어 + 데이터 한국어 (REQ-NF-USA-003).
 *
 * <p>출력 파일명: {@code unschedulable_{yyyyMMdd_HHmmss}.xlsx} (KST — BR-X04).
 * 24h 후 {@link UnschedulableReportCleanupJob} 가 자동 정리.
 *
 * <p>{@code rows} 가 빈 리스트면 파일 생성 안 함 — caller 가 {@code FilterResult.hasUnschedulable()}
 * 로 사전 차단 권장. metrics.increment("vc_unschedulable", "report_generated") emit.
 */
@Component
public class UnschedulableReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(UnschedulableReportGenerator.class);
    private static final String SHEET_NAME = "Unschedulable";
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter
        .ofPattern("yyyyMMdd_HHmmss")
        .withZone(ZoneId.of("Asia/Seoul"));

    private static final String[] HEADERS = {
        "품번", "납기일", "수량", "거래처", "사유", "권장 조치"
    };

    private final SchedulingMetrics metrics;
    private final Clock clock;

    public UnschedulableReportGenerator(SchedulingMetrics metrics, Clock clock) {
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * @return 생성된 Excel 파일 경로, 또는 rows 비어있으면 {@code null}
     */
    public Path generateExcel(List<UnschedulableReportRow> rows, Path outDir) throws IOException {
        if (rows == null || rows.isEmpty()) {
            log.debug("Unschedulable rows empty — Excel 미생성");
            return null;
        }
        Files.createDirectories(outDir);

        Instant now = Instant.now(clock);
        Path out = outDir.resolve("unschedulable_" + TS_FMT.format(now) + ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(SHEET_NAME);

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (UnschedulableReportRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.hoseId());
                row.createCell(1).setCellValue(r.deliveryDate() != null ? r.deliveryDate().toString() : "");
                row.createCell(2).setCellValue(r.qty());
                row.createCell(3).setCellValue(r.customer() != null ? r.customer() : "");
                row.createCell(4).setCellValue("모든 슬롯 X (BR-V11)");
                row.createCell(5).setCellValue("외주 의뢰 또는 재고 대응 (ASM-10)");
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream os = Files.newOutputStream(out)) {
                wb.write(os);
            }
        }

        log.info("Unschedulable Excel 생성: {} ({} rows, {} bytes)",
            out, rows.size(), Files.size(out));
        if (metrics != null) {
            metrics.increment("vc_unschedulable", "report_generated");
        }
        return out;
    }
}
