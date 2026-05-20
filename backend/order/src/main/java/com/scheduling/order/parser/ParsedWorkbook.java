package com.scheduling.order.parser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 엑셀 파싱 결과 — TK-01-1-1.
 *
 * <p>3종 수주 엑셀(월별 예상·주간·확정·KD) 모두 본 구조로 표현.
 * 후속 {@link com.scheduling.order.parser.classifier 워크북 분류기} (TK-01-1-2)
 * 가 sheet[0].rows[0]·rows[1] 헤더 시그니처를 보고 소스 판별.
 */
public final class ParsedWorkbook {

    private final String filename;
    private final List<ParsedSheet> sheets;

    public ParsedWorkbook(String filename) {
        this.filename = filename;
        this.sheets = new ArrayList<>();
    }

    /** Jackson 역직렬화용 (TK-01-2-3 Redis 캐시 라운드트립). */
    @JsonCreator
    public ParsedWorkbook(
        @JsonProperty("filename") String filename,
        @JsonProperty("sheets") List<ParsedSheet> sheets
    ) {
        this.filename = filename;
        this.sheets = sheets == null ? new ArrayList<>() : new ArrayList<>(sheets);
    }

    public void addSheet(ParsedSheet sheet) {
        sheets.add(sheet);
    }

    @JsonProperty("filename")
    public String filename() {
        return filename;
    }

    @JsonProperty("sheets")
    public List<ParsedSheet> sheets() {
        return Collections.unmodifiableList(sheets);
    }

    public int sheetCount() {
        return sheets.size();
    }

    public ParsedSheet sheet(int index) {
        return sheets.get(index);
    }

    public ParsedSheet sheet(String name) {
        return sheets.stream()
            .filter(s -> s.name().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No sheet: " + name));
    }
}
