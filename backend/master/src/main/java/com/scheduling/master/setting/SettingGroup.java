package com.scheduling.master.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 압출 셋팅 그룹 1~8 마스터 — TK-09-1-1 (EP-09 ST-09-1, BR-E06·E07).
 *
 * <p>{@code master.setting_group}. shift 내 단일 그룹 강제 (BR-E06) + 같은 그룹 품번
 * 동시 생산 가능 (BR-E07).
 *
 * <p>Sprint 21 ST-CRUD-2 — {@code active} 컬럼 추가 (V034), {@code groupName} 수정 지원.
 *
 * @see BR-V12
 * @see BR-V13
 */
@Entity
@Table(name = "setting_group", schema = "master")
public class SettingGroup {

    @Id
    @Column(name = "group_number", nullable = false, updatable = false)
    private short groupNumber;

    @Column(name = "group_name", nullable = false, length = 40)
    private String groupName;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected SettingGroup() {}

    /** Sprint 21 ST-CRUD-2 — POST 신규 생성용 full constructor. groupNumber 범위 1~8 은 호출 측에서 강제. */
    public SettingGroup(short groupNumber, String groupName, String description,
                        boolean active, String updatedBy) {
        this.groupNumber = groupNumber;
        this.groupName   = groupName;
        this.description = description;
        this.active      = active;
        this.updatedBy   = updatedBy;
    }

    public short getGroupNumber() { return groupNumber; }
    public String getGroupName() { return groupName; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }

    /** Sprint 21 ST-CRUD-2 — PUT 수정용 mutators. */
    public void updateDisplayName(String groupName, String updatedBy) {
        this.groupName   = groupName;
        this.updatedBy   = updatedBy;
    }

    public void deactivate(String updatedBy) {
        this.active    = false;
        this.updatedBy = updatedBy;
    }
}
