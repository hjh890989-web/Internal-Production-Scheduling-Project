package com.scheduling.master.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 영업일 캘린더 휴일 — TK-06-1-1 (EP-06 ST-06-1).
 *
 * <p>{@code master.holiday} 테이블. PK = holiday_date (자연키, 단일 일자 = 단일 row).
 * BR-X02 mutation 은 IT_OPS 만 — HolidayController + REST audit 로 추적.
 */
@Entity
@Table(name = "holiday", schema = "master")
public class Holiday {

    @Id
    @Column(name = "holiday_date", nullable = false)
    @NotNull
    private LocalDate holidayDate;

    @Column(name = "holiday_name", nullable = false, length = 100)
    @NotBlank
    @Size(max = 100)
    private String holidayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false, length = 20)
    @NotNull
    private HolidayType holidayType;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 40)
    private String createdBy;

    protected Holiday() {}

    public Holiday(LocalDate holidayDate, String holidayName, HolidayType holidayType,
                   String description, String createdBy) {
        this.holidayDate = Objects.requireNonNull(holidayDate, "holidayDate");
        this.holidayName = Objects.requireNonNull(holidayName, "holidayName");
        this.holidayType = Objects.requireNonNull(holidayType, "holidayType");
        this.description = description;
        this.createdBy = Objects.requireNonNullElse(createdBy, "system");
    }

    public LocalDate getHolidayDate() { return holidayDate; }
    public String getHolidayName() { return holidayName; }
    public HolidayType getHolidayType() { return holidayType; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
}
