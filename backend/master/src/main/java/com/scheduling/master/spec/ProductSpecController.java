package com.scheduling.master.spec;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sprint 12 EP-MASTER-UI ST-MASTER-5 — 47 품번 spec read endpoint (TK-MASTER-5-1).
 *
 * <p>RBAC: 4 role 모두 read 허용. PLANNER/STK_USER 가 스케줄 작성 시 품번 spec 조회.
 * CRUD 는 Sprint 13 EP-OC-FULL 부속 (underlying VC_CONSTRAINT/EX_CONSTRAINT 변경).
 */
@RestController
@RequestMapping("/api/v1/master/product-spec")
public class ProductSpecController {

    private final ProductSpecRepository repository;

    public ProductSpecController(ProductSpecRepository repository) {
        this.repository = repository;
    }

    public record SpecSummary(
        String hoseId, Integer spec, Short compositeCount,
        String lpLeftSetting, String lpRightSetting,
        int angleCount, boolean isSpecLt7
    ) {
        public static SpecSummary from(ProductSpec p) {
            return new SpecSummary(p.getHoseId(), p.getSpec(), p.getCompositeCount(),
                p.getLpLeftSetting(), p.getLpRightSetting(), p.getAngleCount(), p.isSpecLt7());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<List<SpecSummary>> list() {
        return ResponseEntity.ok(repository.findAll().stream().map(SpecSummary::from).toList());
    }

    @GetMapping("/{hoseId}")
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<SpecSummary> get(@PathVariable String hoseId) {
        return repository.findById(hoseId)
            .map(SpecSummary::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
