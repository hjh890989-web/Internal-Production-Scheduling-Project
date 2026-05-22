package com.scheduling.master.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * 47품번 ↔ 셋팅 그룹 M:N 매핑 — TK-09-1-1 (EP-09 ST-09-1).
 *
 * <p>{@code master.product_setting_group}. {@code primary_group=TRUE} = 우선 추천 그룹.
 */
@Entity
@Table(name = "product_setting_group", schema = "master")
@IdClass(ProductSettingGroup.PK.class)
public class ProductSettingGroup {

    @Id
    @Column(name = "hose_id", nullable = false, length = 40, updatable = false)
    private String hoseId;

    @Id
    @Column(name = "group_number", nullable = false, updatable = false)
    private short groupNumber;

    @Column(name = "primary_group", nullable = false)
    private boolean primaryGroup;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected ProductSettingGroup() {}

    public String getHoseId() { return hoseId; }
    public short getGroupNumber() { return groupNumber; }
    public boolean isPrimaryGroup() { return primaryGroup; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }

    public static class PK implements Serializable {
        private String hoseId;
        private short groupNumber;

        public PK() {}
        public PK(String hoseId, short groupNumber) {
            this.hoseId = hoseId; this.groupNumber = groupNumber;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return groupNumber == pk.groupNumber && Objects.equals(hoseId, pk.hoseId);
        }
        @Override public int hashCode() { return Objects.hash(hoseId, groupNumber); }
    }
}
