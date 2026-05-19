package com.scheduling.order.mapping;

import java.util.List;

/**
 * 단일 표준 필드의 매핑 정의 — TK-01-2-1.
 *
 * @param standardField   표준 필드명 (hose_id·delivery_date·qty·customer)
 * @param aliases         사용자 워크북 컬럼명 후보 (대소문자·공백 무시)
 * @param dateFormatHints delivery_date 만 — 시도할 날짜 포맷 후보
 * @param coerceInteger   qty 정수 강제 ("1,000" → 1000)
 * @param regexStrip      hose_id 정규식 제거 패턴 (예: "[\\s_]+")
 * @param toUpperCase     hose_id 대문자 정규화
 * @param defaultValue    선택 필드 fallback (customer="내수")
 * @param required        필수 여부 — false 이면 매핑 실패해도 OrderDraft 생성
 */
public record FieldMapping(
    String standardField,
    List<String> aliases,
    List<String> dateFormatHints,
    boolean coerceInteger,
    String regexStrip,
    boolean toUpperCase,
    String defaultValue,
    boolean required
) {
    public FieldMapping {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        dateFormatHints = dateFormatHints == null ? List.of() : List.copyOf(dateFormatHints);
    }
}
