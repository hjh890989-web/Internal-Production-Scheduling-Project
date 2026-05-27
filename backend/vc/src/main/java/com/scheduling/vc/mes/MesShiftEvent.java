package com.scheduling.vc.mes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Sprint 17 BR-X06 MES shift event entity — TK-DAY-LOCK-3-3.
 *
 * <p>app.mes_shift_event (V044) — MES 자동 수신 + Excel 폴백 SSoT.
 * UNIQUE (machine_id, shift_date, shift_no) — shift 1회만 적재 (ON CONFLICT UPDATE 경로).
 */
@Entity
@Table(name = "mes_shift_event", schema = "app")
public class MesShiftEvent {

    @Id
    @Column(name = "shift_event_id", nullable = false, updatable = false)
    private UUID shiftEventId;

    @Column(name = "machine_id", nullable = false, length = 10)
    private String machineId;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "shift_no", nullable = false)
    private short shiftNo;

    @Column(name = "planned_qty", nullable = false)
    private int plannedQty;

    @Column(name = "actual_qty")
    private Integer actualQty;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private MesShiftSource source;

    @Column(name = "reported_by", length = 40)
    private String reportedBy;

    @Column(name = "note", length = 500)
    private String note;

    protected MesShiftEvent() {}

    public MesShiftEvent(UUID shiftEventId, String machineId, LocalDate shiftDate, short shiftNo,
                         int plannedQty, Integer actualQty, Instant receivedAt,
                         MesShiftSource source, String reportedBy, String note) {
        if (machineId == null || machineId.isBlank()) {
            throw new IllegalArgumentException("machineId 필수");
        }
        if (shiftNo < 1 || shiftNo > 4) {
            throw new IllegalArgumentException("shiftNo 1..4: " + shiftNo);
        }
        if (source == null) {
            throw new IllegalArgumentException("source 필수 (MES/EXCEL_FALLBACK)");
        }
        if (source == MesShiftSource.EXCEL_FALLBACK
            && (reportedBy == null || reportedBy.isBlank())) {
            throw new IllegalArgumentException("EXCEL_FALLBACK 시 reportedBy 필수 (BR-X02)");
        }
        this.shiftEventId = shiftEventId;
        this.machineId = machineId;
        this.shiftDate = shiftDate;
        this.shiftNo = shiftNo;
        this.plannedQty = plannedQty;
        this.actualQty = actualQty;
        this.receivedAt = receivedAt;
        this.source = source;
        this.reportedBy = reportedBy;
        this.note = note;
    }

    public UUID getShiftEventId() { return shiftEventId; }
    public String getMachineId() { return machineId; }
    public LocalDate getShiftDate() { return shiftDate; }
    public short getShiftNo() { return shiftNo; }
    public int getPlannedQty() { return plannedQty; }
    public Integer getActualQty() { return actualQty; }
    public Instant getReceivedAt() { return receivedAt; }
    public MesShiftSource getSource() { return source; }
    public String getReportedBy() { return reportedBy; }
    public String getNote() { return note; }

    public void updateActual(Integer actualQty, Instant receivedAt, MesShiftSource source, String reportedBy) {
        this.actualQty = actualQty;
        this.receivedAt = receivedAt;
        this.source = source;
        this.reportedBy = reportedBy;
    }
}
