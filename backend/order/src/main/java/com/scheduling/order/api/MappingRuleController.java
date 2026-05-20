package com.scheduling.order.api;

import com.scheduling.order.mapping.MappingRule;
import com.scheduling.order.mapping.MappingRuleService;
import com.scheduling.order.parser.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * 매핑 룰 운영 endpoint — TK-01-2-3.
 *
 * <p>{@code GET /api/v1/master/mapping-rule/{sourceType}} — 현재 룰 조회 (PLANNER·IT_OPS)
 * <p>{@code PUT /api/v1/master/mapping-rule/{sourceType}} — 룰 변경 (PLANNER·IT_OPS)
 *
 * <p>@PreAuthorize 는 Sprint 1 ST-30-2 RBAC 활성 후 추가. 현재 Sprint 1 baseline 은
 * 인증된 사용자 전체 허용 (SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/master/mapping-rule")
public class MappingRuleController {

    private static final Logger log = LoggerFactory.getLogger(MappingRuleController.class);

    private final MappingRuleService ruleService;

    public MappingRuleController(MappingRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/{sourceType}")
    @PreAuthorize("hasAnyRole('PLANNER', 'IT_OPS', 'READ_ONLY')")
    public MappingRule get(@PathVariable SourceType sourceType) {
        return ruleService.getRule(sourceType);
    }

    @PutMapping("/{sourceType}")
    @PreAuthorize("hasAnyRole('PLANNER', 'IT_OPS')")
    public MappingRule update(
        @PathVariable SourceType sourceType,
        @RequestBody MappingRule updated
    ) {
        // Sprint 1 baseline — actor 는 인증 컨텍스트 미연결 시 "anonymous".
        // ST-30-2 활성 후 @AuthenticationPrincipal Jwt jwt → jwt.getSubject() 추출.
        String actor = "anonymous";
        log.info("Rule update request: sourceType={}", sourceType);
        return ruleService.updateRule(sourceType, updated, actor);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException e) {
        return e.getMessage();
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NoSuchElementException e) {
        return e.getMessage();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missingRule(IllegalStateException e) {
        // MappingRuleLoader.loadRuleFor throws IllegalStateException — 존재 안 함
        return e.getMessage();
    }
}
