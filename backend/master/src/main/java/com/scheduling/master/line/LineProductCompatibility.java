package com.scheduling.master.line;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * 라인 ↔ 품번 호환성 — TK-14-1-2 (EP-14 ST-14-1, BR-E08).
 *
 * <p>{@code ford_only=TRUE} → 포드 전용 품번 (신규 라인 시도 차단, zero 오라우팅).
 */
@Entity
@Table(name = "line_product_compatibility", schema = "master")
@IdClass(LineProductCompatibility.PK.class)
public class LineProductCompatibility {

    @Id
    @Column(name = "hose_id", nullable = false, length = 40, updatable = false)
    private String hoseId;

    @Id
    @Column(name = "line_id", nullable = false, length = 10, updatable = false)
    private String lineId;

    @Column(name = "ford_only", nullable = false)
    private boolean fordOnly;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected LineProductCompatibility() {}

    public String getHoseId() { return hoseId; }
    public String getLineId() { return lineId; }
    public boolean isFordOnly() { return fordOnly; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }

    public static class PK implements Serializable {
        private String hoseId;
        private String lineId;

        public PK() {}
        public PK(String hoseId, String lineId) {
            this.hoseId = hoseId; this.lineId = lineId;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(hoseId, pk.hoseId) && Objects.equals(lineId, pk.lineId);
        }
        @Override public int hashCode() { return Objects.hash(hoseId, lineId); }
    }
}
