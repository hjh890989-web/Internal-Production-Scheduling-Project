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
 * Sprint 17 BR-X06 MesShiftPort JPA 구현체 (Adapter 패턴 — write side, default adapter).
 *
 * <p>활성 조건: {@code spring.profiles.active=with-infra} — JPA/DB Bean 필요.
 *
 * <p>UNIQUE (machine_id, shift_date, shift_no) 충돌 시 UPDATE — MES 자동 수신 후 Excel 폴백
 * 으로 override 가능 (source = EXCEL_FALLBACK 우선). DB UNIQUE constraint 가 결정성 보장.
 *
 * <p>Sprint 26 S26-A: Adapter 패턴 Javadoc 보강. MesShiftPort 인터페이스는 write/persist Port
 * 로 read/fetch Port (MesShiftClient) 와 분리되어 adapter 모드(jpa/http) 와 무관하게 항상 활성.
 * HttpMesShiftClient 는 별도 read Port — {@code @ConditionalOnProperty(adapter=http)} 자체 조건.
 *
 * @see MesShiftPort
 * @see MesShiftClient
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
