package com.scheduling.order.parser;

import java.util.List;

/**
 * 엑셀 파싱 결과의 단일 row — TK-01-1-1.
 *
 * @param index 0-based row index (sheet 내부)
 * @param cells 0-based 셀 값 리스트 — 빈 셀은 빈 문자열, 수식은 원본 보존
 */
public record ParsedRow(int index, List<String> cells) {

    public ParsedRow {
        cells = List.copyOf(cells);
    }

    public String cell(int col) {
        return col < cells.size() ? cells.get(col) : "";
    }

    public int size() {
        return cells.size();
    }

    public boolean isEmpty() {
        return cells.stream().allMatch(String::isEmpty);
    }
}
