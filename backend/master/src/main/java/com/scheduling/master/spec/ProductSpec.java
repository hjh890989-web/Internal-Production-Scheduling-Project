package com.scheduling.master.spec;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

/**
 * 품번 + 압출 규격 cross-master VIEW 매핑 — TK-21-5-1 (ADR-017).
 *
 * <p>{@code master.v_product_with_spec} VIEW — VC_CONSTRAINT + EX_CONSTRAINT JOIN.
 * Read-only ({@link Immutable}). 어느 underlying 테이블 변경되어도 캐시 invalidate
 * 필요 ({@link Synchronize}).
 *
 * <p>spec NULL = EX_CONSTRAINT 미등록 → is_spec_lt7=false (BR-V17 미적용).
 */
@Entity
@Immutable
@Subselect("SELECT * FROM master.v_product_with_spec")
@Synchronize({"master.vc_constraint", "master.ex_constraint"})
public class ProductSpec {

    @Id
    @Column(name = "hose_id", nullable = false, length = 40)
    private String hoseId;

    @Column(name = "spec")
    private Integer spec;

    @Column(name = "composite_count")
    private Short compositeCount;

    @Column(name = "lp_left_setting", length = 1)
    private String lpLeftSetting;

    @Column(name = "lp_right_setting", length = 1)
    private String lpRightSetting;

    @Column(name = "angle_count", nullable = false)
    private int angleCount;

    @Column(name = "is_spec_lt7", nullable = false)
    private boolean isSpecLt7;

    protected ProductSpec() {}

    public String getHoseId() { return hoseId; }
    public Integer getSpec() { return spec; }
    public Short getCompositeCount() { return compositeCount; }
    public String getLpLeftSetting() { return lpLeftSetting; }
    public String getLpRightSetting() { return lpRightSetting; }
    public int getAngleCount() { return angleCount; }
    public boolean isSpecLt7() { return isSpecLt7; }
}
