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

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected SettingGroup() {}

    public short getGroupNumber() { return groupNumber; }
    public String getGroupName() { return groupName; }
    public String getDescription() { return description; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
