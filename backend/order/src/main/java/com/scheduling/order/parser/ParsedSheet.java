package com.scheduling.order.parser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    /** Jackson 역직렬화용 (TK-01-2-3 Redis 캐시 라운드트립). */
    @JsonCreator
    public ParsedSheet(
        @JsonProperty("name") String name,
        @JsonProperty("rows") List<ParsedRow> rows
    ) {
        this.name = name;
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
    }

    public void addRow(ParsedRow row) {
        rows.add(row);
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("rows")
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
