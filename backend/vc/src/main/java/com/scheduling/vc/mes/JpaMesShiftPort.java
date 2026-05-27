package com.scheduling.vc.mes;

import com.scheduling.audit.aop.Auditable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 17 BR-X06 MesShiftPort JPA stub impl — TK-DAY-LOCK-3-1.
 *
 * <p>UNIQUE (machine, shift_date, shift_no) 충돌 시 update — MES 자동 수신 후 Excel 폴백
 * 으로 override 가능 (source = EXCEL_FALLBACK 우선). DB UNIQUE constraint 가 결정성 보장.
 */
@Component
@Profile("with-infra")
public class JpaMesShiftPort implements MesShiftPort {

    private static final Logger log = LoggerFactory.getLogger(JpaMesShiftPort.class);

    private final MesShiftEventRepository repository;
    private final Clock clock;

    public JpaMesShiftPort(MesShiftEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Auditable("BR-X06 MES shift event 보고 (자동 또는 Excel 폴백)")
    @Transactional
    public MesShiftEvent reportProduction(String machineId, LocalDate shiftDate, short shiftNo,
                                           int plannedQty, Integer actualQty,
                                           MesShiftSource source, String reportedBy, String note) {
        Instant now = Instant.now(clock);
        Optional<MesShiftEvent> existing = repository
            .findByMachineIdAndShiftDateAndShiftNo(machineId, shiftDate, shiftNo);
        if (existing.isPresent()) {
            MesShiftEvent e = existing.get();
            e.updateActual(actualQty, now, source, reportedBy);
            log.info("MES shift event updated — machine={} date={} shift={} source={} actual={}",
                machineId, shiftDate, shiftNo, source, actualQty);
            return repository.save(e);
        }
        MesShiftEvent created = new MesShiftEvent(
            UUID.randomUUID(), machineId, shiftDate, shiftNo,
            plannedQty, actualQty, now, source, reportedBy, note);
        log.info("MES shift event created — machine={} date={} shift={} source={} actual={}",
            machineId, shiftDate, shiftNo, source, actualQty);
        return repository.save(created);
    }

    @Override
    public Optional<MesShiftEvent> lastReceivedShift(String machineId) {
        return repository.findTopByMachineIdOrderByReceivedAtDesc(machineId);
    }
}
