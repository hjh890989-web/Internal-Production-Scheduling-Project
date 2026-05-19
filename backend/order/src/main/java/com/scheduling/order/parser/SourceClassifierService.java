package com.scheduling.order.parser;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 워크북 헤더 시그니처 기반 SourceType 분류기 — TK-01-1-2.
 *
 * <p>알고리즘 (결정적·deterministic):
 * <ol>
 *   <li>workbook 의 sheetName + 처음 5 row 셀 + 파일명 → token set</li>
 *   <li>각 SourceType 의 {@link HeaderSignature} 에 대해 score 계산</li>
 *   <li>excluded 매칭 1건이라도 → score 0</li>
 *   <li>requiredAny 매칭 비율 × weight → score (max 1.0)</li>
 *   <li>최고 score ≥ 0.5 → 해당 type / 미만 → UNRECOGNIZED</li>
 * </ol>
 *
 * <p>룰셋은 {@code classpath:classification/header-signatures.yaml} 외부 파일 —
 * 영업·관리 부서 양식 변경 시 코드 수정 없이 룰만 갱신 (SRS-RSK-007).
 *
 * @see SourceType
 * @see ClassificationResult
 */
@Service
public class SourceClassifierService {

    private static final Logger log = LoggerFactory.getLogger(SourceClassifierService.class);
    private static final double CONFIDENCE_THRESHOLD = 0.5;
    private static final int HEADER_ROW_LIMIT = 5;

    private final Resource signatureFile;
    private Map<SourceType, HeaderSignature> signatures = new EnumMap<>(SourceType.class);

    public SourceClassifierService(
        @Value("classpath:classification/header-signatures.yaml") Resource signatureFile
    ) {
        this.signatureFile = signatureFile;
    }

    @PostConstruct
    public void load() {
        try (InputStream is = signatureFile.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> parsed = yaml.load(is);
            this.signatures = parseSignatures(parsed);
            log.info("Loaded {} classification signatures", signatures.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load classification rules from " + signatureFile, e);
        }
    }

    /** 분류 룰셋 reload — 운영자가 YAML 변경 후 호출 (REST endpoint 노출 향후). */
    public void reload() {
        load();
    }

    public ClassificationResult classify(ParsedWorkbook workbook) {
        Set<String> tokens = extractHeaderTokens(workbook);

        Map<SourceType, Double> scores = new EnumMap<>(SourceType.class);
        for (Map.Entry<SourceType, HeaderSignature> entry : signatures.entrySet()) {
            scores.put(entry.getKey(), computeScore(tokens, entry.getValue()));
        }

        SourceType best = SourceType.UNRECOGNIZED;
        double bestScore = 0.0;
        for (Map.Entry<SourceType, Double> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                best = entry.getKey();
            }
        }
        if (bestScore < CONFIDENCE_THRESHOLD) {
            best = SourceType.UNRECOGNIZED;
        }

        return new ClassificationResult(best, bestScore, scores);
    }

    private Set<String> extractHeaderTokens(ParsedWorkbook workbook) {
        Set<String> tokens = new HashSet<>();
        tokens.add(workbook.filename());

        for (ParsedSheet sheet : workbook.sheets()) {
            tokens.add(sheet.name());
            int limit = Math.min(HEADER_ROW_LIMIT, sheet.rowCount());
            for (int r = 0; r < limit; r++) {
                for (String cell : sheet.row(r).cells()) {
                    if (cell != null && !cell.isBlank()) {
                        tokens.add(cell);
                    }
                }
            }
        }
        return tokens;
    }

    private double computeScore(Set<String> tokens, HeaderSignature sig) {
        for (String ex : sig.excluded()) {
            if (anyContains(tokens, ex)) {
                return 0.0;
            }
        }
        if (sig.requiredAny().isEmpty()) {
            return 0.0;
        }
        long matches = sig.requiredAny().stream()
            .filter(kw -> anyContains(tokens, kw))
            .count();
        if (matches == 0) {
            return 0.0;
        }
        // requiredAny 는 OR — 1 매치도 valid. base 0.4 + 매치당 0.3, weight 가중치 곱.
        // 1 매치 × w=1.0 → 0.7 / 1 매치 × w=1.5 (KD) → 1.05→cap 1.0
        // 2 매치 × w=1.0 → 1.0 (cap) / 2 매치 × w=1.5 → 1.5→cap 1.0
        double raw = (0.4 + 0.3 * matches) * sig.weight();
        return Math.min(1.0, raw);
    }

    private boolean anyContains(Set<String> tokens, String keyword) {
        String kw = keyword.toUpperCase(Locale.ROOT);
        for (String token : tokens) {
            if (token.toUpperCase(Locale.ROOT).contains(kw)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<SourceType, HeaderSignature> parseSignatures(Map<String, Object> root) {
        Map<SourceType, HeaderSignature> result = new EnumMap<>(SourceType.class);
        Object sigsObj = root.get("signatures");
        if (!(sigsObj instanceof Map<?, ?> sigsMap)) {
            throw new IllegalStateException("'signatures' key missing in YAML");
        }
        for (Map.Entry<?, ?> entry : sigsMap.entrySet()) {
            String typeName = String.valueOf(entry.getKey());
            SourceType type;
            try {
                type = SourceType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown SourceType in YAML: {} — skipping", typeName);
                continue;
            }
            Map<String, Object> sigData = (Map<String, Object>) entry.getValue();
            List<String> required = toStringList(sigData.get("requiredAny"));
            List<String> excluded = toStringList(sigData.get("excluded"));
            double weight = sigData.get("weight") instanceof Number n ? n.doubleValue() : 1.0;
            result.put(type, new HeaderSignature(required, excluded, weight));
        }
        return result;
    }

    private List<String> toStringList(Object obj) {
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        return List.of();
    }

    Map<SourceType, HeaderSignature> signatures() {
        return signatures;
    }
}
