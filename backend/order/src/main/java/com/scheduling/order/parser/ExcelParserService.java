package com.scheduling.order.parser;

import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 엑셀 워크북 파서 — TK-01-1-1.
 *
 * <p>SXSSF Streaming Reader 사용 — 10,000 row 워크북도 JVM 힙 ≤256MB 유지.
 * SAD §11 SAD-RSK-005 (POI 메모리) 완화.
 *
 * <p>제약:
 * <ul>
 *   <li>파일 크기 ≤20MB ({@code scheduling.excel.max-file-size-mb} — REQ-FUNC-OC-001)</li>
 *   <li>row cache 100 (메모리 안정)</li>
 *   <li>날짜 셀 → ISO-8601 (KST 변환은 {@code OrderMapper} 단계)</li>
 *   <li>수식 셀 → 원본 보존 (역-Export REQ-FUNC-OC-013 대비)</li>
 * </ul>
 */
@Service
public class ExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(ExcelParserService.class);

    private final long maxFileSizeBytes;
    private final int rowCacheSize;
    private final int bufferSize;

    public ExcelParserService(
        @Value("${scheduling.excel.max-file-size-mb:20}") int maxFileSizeMb,
        @Value("${scheduling.excel.row-cache-size:100}") int rowCacheSize,
        @Value("${scheduling.excel.buffer-size-bytes:4096}") int bufferSize
    ) {
        this.maxFileSizeBytes = (long) maxFileSizeMb * 1024L * 1024L;
        this.rowCacheSize = rowCacheSize;
        this.bufferSize = bufferSize;
    }

    /**
     * 엑셀 워크북 파싱. caller 가 {@code fileSize} 를 multipart 메타로부터 전달해야 함
     * (스트림 길이를 미리 알아야 20MB 가드).
     */
    public ParsedWorkbook parse(String filename, InputStream is, long fileSize) {
        if (fileSize > maxFileSizeBytes) {
            throw new ExcelParseException(
                "FILE_TOO_LARGE",
                "Workbook %s exceeds %d MB limit (%d bytes)".formatted(
                    filename, maxFileSizeBytes / 1024 / 1024, fileSize));
        }

        ParsedWorkbook result = new ParsedWorkbook(filename);
        long started = System.nanoTime();

        try (Workbook workbook = StreamingReader.builder()
                .rowCacheSize(rowCacheSize)
                .bufferSize(bufferSize)
                .open(is)) {

            for (Sheet sheet : workbook) {
                result.addSheet(parseSheet(sheet));
            }

        } catch (ExcelParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelParseException(
                "PARSE_FAILED",
                "Failed to parse %s: %s".formatted(filename, e.getMessage()),
                e);
        }

        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        int totalRows = result.sheets().stream().mapToInt(ParsedSheet::rowCount).sum();
        log.info("Parsed {} ({} sheets, {} rows) in {}ms",
            filename, result.sheetCount(), totalRows, elapsedMs);

        return result;
    }

    private ParsedSheet parseSheet(Sheet sheet) {
        ParsedSheet ps = new ParsedSheet(sheet.getSheetName());
        int rowIdx = 0;

        for (Row row : sheet) {
            int maxCol = row.getLastCellNum();
            if (maxCol < 0) {
                ps.addRow(new ParsedRow(rowIdx++, List.of()));
                continue;
            }
            List<String> cells = new ArrayList<>(maxCol);
            for (int c = 0; c < maxCol; c++) {
                Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cells.add(extractCellValue(cell));
            }
            ps.addRow(new ParsedRow(rowIdx++, cells));
        }

        return ps;
    }

    private String extractCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // ISO-8601 (UTC instant) — KST 변환은 OrderMapper 단계 (BR-X04)
                    Instant instant = cell.getDateCellValue().toInstant();
                    yield DateTimeFormatter.ISO_INSTANT.format(instant);
                }
                double v = cell.getNumericCellValue();
                // 정수면 정수 표기 (2531.0 → "2531")
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    yield String.valueOf((long) v);
                }
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> "=" + cell.getCellFormula();        // 원본 보존
            case BLANK, _NONE -> "";
            case ERROR -> "#ERR";
        };
    }
}
