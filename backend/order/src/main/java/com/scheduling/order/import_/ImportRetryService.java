package com.scheduling.order.import_;

import com.scheduling.order.mapping.MappingResult;
import com.scheduling.order.mapping.SchemaMappingService;
import com.scheduling.order.parser.ClassificationResult;
import com.scheduling.order.parser.ParsedWorkbook;
import com.scheduling.order.parser.SourceClassifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 라운드트립 재매핑 서비스 — TK-01-2-3 (REQ-FUNC-OC-004 핵심).
 *
 * <p>흐름:
 * <ol>
 *   <li>사용자가 룰 보정 후 {@code POST /api/v1/orders/import/{trackingId}/retry} 호출</li>
 *   <li>{@link ImportTrackingService#loadParsedWorkbooks} 로 캐시된 ParsedWorkbook 로드 (재업로드 X)</li>
 *   <li>{@link SourceClassifierService} 분류 재실행 (룰 reload 영향 가능)</li>
 *   <li>{@link SchemaMappingService} 매핑 재실행 (새 룰셋 적용)</li>
 *   <li>{@link ImportTrackingService#cacheMappingResult} 로 결과 캐시</li>
 *   <li>TTL 24h 만료 시 NoSuchElementException → 사용자 안내 (재업로드 필요)</li>
 * </ol>
 */
@Service
public class ImportRetryService {

    private static final Logger log = LoggerFactory.getLogger(ImportRetryService.class);

    private final ImportTrackingService tracking;
    private final SourceClassifierService classifier;
    private final SchemaMappingService mapper;

    public ImportRetryService(
        ImportTrackingService tracking,
        SourceClassifierService classifier,
        SchemaMappingService mapper
    ) {
        this.tracking = tracking;
        this.classifier = classifier;
        this.mapper = mapper;
    }

    @Async(AsyncConfig.EXCEL_IMPORT_EXECUTOR)
    public void retryAsync(UUID trackingId) {
        log.info("Retry started for trackingId={}", trackingId);
        tracking.update(trackingId, ImportStatus.PARSING, null);

        try {
            List<ParsedWorkbook> workbooks = tracking.loadParsedWorkbooks(trackingId);

            int totalSuccess = 0;
            int totalFailed = 0;
            for (ParsedWorkbook wb : workbooks) {
                ClassificationResult cls = classifier.classify(wb);
                MappingResult result = mapper.map(wb, cls.sourceType());
                tracking.cacheMappingResult(trackingId, result);
                totalSuccess += result.successes().size();
                totalFailed += result.failures().size();
            }

            ImportStatus next = totalFailed == 0 ? ImportStatus.MAPPED : ImportStatus.PARSED;
            tracking.update(trackingId, next, null);
            log.info("Retry done for {}: success={} failed={}", trackingId, totalSuccess, totalFailed);

        } catch (NoSuchElementException e) {
            log.warn("Retry cache miss for {} — TTL 24h 만료", trackingId);
            tracking.update(trackingId, ImportStatus.FAILED,
                "캐시 만료 (24h) — 원본 파일을 다시 업로드해 주세요.");
        } catch (Exception e) {
            log.error("Retry failed for {}", trackingId, e);
            tracking.update(trackingId, ImportStatus.FAILED, e.getMessage());
        }
    }
}
