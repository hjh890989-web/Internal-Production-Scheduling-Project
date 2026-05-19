package com.scheduling.order.mapping;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * 매핑 필드 값 정규화 — TK-01-2-1.
 *
 * <p>책임:
 * <ul>
 *   <li>날짜 다중 포맷 시도 ({@code yyyy-MM-dd} / {@code yyyy/MM/dd} / {@code yyyyMMdd} 등)</li>
 *   <li>숫자 정수 변환 (콤마·공백·과학표기 처리)</li>
 *   <li>품번 정규화 (대문자·공백 제거)</li>
 *   <li>canonical token (헤더 매칭용 — 대소문자·공백·언더스코어 무시)</li>
 * </ul>
 *
 * <p>{@link SchemaMappingService} 의 협력자 (stateless).
 */
@Component
public class FieldNormalizer {

    /** 헤더 row 의 셀 → 매칭용 canonical 토큰 (대소문자·공백·언더스코어 무시). */
    public String canonical(String raw) {
        if (raw == null) return "";
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_]+", "");
    }

    /**
     * 날짜 다중 포맷 시도. 모두 실패 시 {@link MappingException}.
     * KST 기준 LocalDate (시간 정보 무시 — BR-X04 timezone 처리).
     */
    public LocalDate parseDate(String raw, List<String> formats, boolean required) {
        if (raw == null || raw.isBlank()) {
            if (required) {
                throw new MappingException("delivery_date", "값 없음");
            }
            return null;
        }
        String trimmed = raw.trim();
        // ISO datetime 일 경우 (TK-01-1-1 ExcelParserService 가 ISO-8601 형식으로 출력)
        if (trimmed.contains("T")) {
            try {
                return LocalDate.parse(trimmed.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
                // fall through to custom formats
            }
        }
        for (String fmt : formats) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new MappingException("delivery_date",
            "포맷 인식 실패: '%s' (시도 포맷: %s)".formatted(trimmed, formats));
    }

    /**
     * 정수 변환. 콤마·공백 제거 후 try.
     * 음수·0·소수 (정수 아닌 값) 는 거부.
     */
    public int parseInt(String raw, boolean required) {
        if (raw == null || raw.isBlank()) {
            if (required) {
                throw new MappingException("qty", "값 없음");
            }
            return 0;
        }
        String cleaned = raw.trim().replaceAll("[,\\s]", "");
        try {
            // 과학 표기 (1.0e3) 또는 소수 (1000.0) 형태도 정수면 수용
            double d = Double.parseDouble(cleaned);
            if (d < 1) {
                throw new MappingException("qty", "양수 정수 필수 (값=%s)".formatted(cleaned));
            }
            if (d != Math.floor(d)) {
                throw new MappingException("qty", "정수만 허용 (값=%s)".formatted(cleaned));
            }
            return (int) d;
        } catch (NumberFormatException e) {
            throw new MappingException("qty", "숫자 변환 실패: '%s'".formatted(raw));
        }
    }

    /**
     * 품번 정규화 — 정규식 strip + 대문자 + 정규식 검증.
     * SRS §6.2.4 ORDER.hose_id 정규식: {@code ^[A-Z0-9 -]+$} (대문자·숫자·공백·하이픈).
     */
    public String normalizeHoseId(String raw, String regexStrip, boolean toUpper) {
        if (raw == null || raw.isBlank()) {
            throw new MappingException("hose_id", "값 없음");
        }
        String result = raw.trim();
        if (regexStrip != null && !regexStrip.isBlank()) {
            result = result.replaceAll(regexStrip, "");
        }
        if (toUpper) {
            result = result.toUpperCase(Locale.ROOT);
        }
        if (!result.matches("^[A-Z0-9 -]+$")) {
            throw new MappingException("hose_id",
                "정규식 위반: '%s' (기대: ^[A-Z0-9 -]+$)".formatted(result));
        }
        return result;
    }
}
