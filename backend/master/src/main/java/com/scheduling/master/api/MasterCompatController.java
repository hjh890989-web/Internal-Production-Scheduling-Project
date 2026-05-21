package com.scheduling.master.api;

import com.scheduling.master.vc.SlotCompatibilityMatrix;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.master.vc.SlotPosition;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 슬롯 적합성 매트릭스 REST endpoint — TK-04-1-3 (SAD §6.1).
 *
 * <p>{@code GET /api/v1/master/compat} — 전체 매트릭스 + ETag {@code "v{version}"} 캐싱.
 * If-None-Match 일치 시 304 Not Modified. Cache-Control max-age 300s.
 *
 * <p>{@code GET /api/v1/master/compat/{hoseId}/{slotPosition}} — 단일 (품번, 슬롯) 조회.
 * 잘못된 slotPosition → 400 한국어 사유.
 *
 * <p>RBAC — 인증된 모든 role 허용 (READ_ONLY 포함). Frontend dnd-kit 가 사전 검증용으로 소비.
 *
 * <p>{@code @Profile("with-infra")} — SlotCompatibilityMatrixService (JPA 의존) 가 with-infra 만.
 */
@RestController
@RequestMapping("/api/v1/master/compat")
@Profile("with-infra")
public class MasterCompatController {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final SlotCompatibilityMatrixService matrixService;

    public MasterCompatController(SlotCompatibilityMatrixService matrixService) {
        this.matrixService = matrixService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompatibilityResponse> get(
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        SlotCompatibilityMatrix matrix = matrixService.current();
        String etag = "\"v" + matrix.version() + "\"";

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        return ResponseEntity.ok()
            .eTag(etag)
            .cacheControl(CacheControl.maxAge(CACHE_TTL).mustRevalidate())
            .body(CompatibilityResponse.from(matrix));
    }

    @GetMapping("/{hoseId}/{slotPosition}")
    @PreAuthorize("isAuthenticated()")
    public CompatibilityResponse.PointCheck check(
        @PathVariable String hoseId,
        @PathVariable String slotPosition
    ) {
        SlotPosition slot = parseSlot(slotPosition);
        SlotCompatibilityMatrix matrix = matrixService.current();
        return new CompatibilityResponse.PointCheck(
            hoseId, slot.name(), matrix.isEligible(hoseId, slot)
        );
    }

    private SlotPosition parseSlot(String raw) {
        try {
            return SlotPosition.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidSlotPositionException(raw);
        }
    }

    @ExceptionHandler(InvalidSlotPositionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badSlot(InvalidSlotPositionException e) {
        return e.getMessage();
    }

    /** 잘못된 slotPosition path 변수 — 400 변환용. */
    static class InvalidSlotPositionException extends RuntimeException {
        InvalidSlotPositionException(String raw) {
            super("잘못된 슬롯 위치: '" + raw + "' — 허용 값: LP_TOP, LP_UPMID, LP_LOWMID, LP_BOT, IC_TOP, IC_MID, IC_BOT");
        }
    }
}
