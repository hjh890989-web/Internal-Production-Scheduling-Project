package com.scheduling.order.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 엑셀 워크북 내부의 단일 sheet — TK-01-1-1.
 *
 * <p>{@link ParsedWorkbook} 의 자식. {@link ParsedRow} 의 부모.
 */
public final class ParsedSheet {

    private final String name;
    private final List<ParsedRow> rows;

    public ParsedSheet(String name) {
        this.name = name;
        this.rows = new ArrayList<>();
    }

    public void addRow(ParsedRow row) {
        rows.add(row);
    }

    public String name() {
        return name;
    }

    public List<ParsedRow> rows() {
        return Collections.unmodifiableList(rows);
    }

    public int rowCount() {
        return rows.size();
    }

    public ParsedRow row(int index) {
        return rows.get(index);
    }
}
