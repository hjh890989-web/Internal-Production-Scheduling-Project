package com.scheduling.vc.mes;

import com.scheduling.master.api.VcMachineQuery;
import com.scheduling.master.api.VcMachineSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Sprint 23 ST-MES-2 — MES 자동 polling scheduler (EP-MES-ADAPTER-1, BR-X06).
 *
 * <p>{@code scheduling.mes.adapter=http} 일 때만 활성 (default jpa 모드는 polling 불필요).
 * 60초 주기로 활성 vc_machine × 당일(KST) 4 shift 를 {@link MesShiftClient#fetchShift} 로 조회,
 * 수신분만 {@link MesShiftPort#reportProduction} 으로 영속 (degraded mode 가 미수신 임계 감지).
 *
 * <p>fetch 실패(circuit OPEN·timeout) 는 {@link HttpMesShiftClient} 가 {@code Optional.empty()}
 * 로 흡수 — 본 서비스는 다음 cycle 재시도 (LOG WARN). {@link Clock} 주입 (BR-X04 KST).
 */
@Service
@Profile("with-infra")
@ConditionalOnProperty(name = "scheduling.mes.adapter", havingValue = "http")
public class MesPollingService {

    private static final Logger log = LoggerFactory.getLogger(MesPollingService.class);

    /** BR-X06 — 4 shifts/day (1=주간전반 2=주간후반 3=야간전반 4=야간후반). */
    private static final short[] SHIFT_NOS = {1, 2, 3, 4};

    private static final String REPORTED_BY = "system:mes-polling";

    private final MesShiftClient client;
    private final MesShiftPort port;
    private final VcMachineQuery machineQuery;
    private final Clock clock;

    public MesPollingService(MesShiftClient client, MesShiftPort port,
                             VcMachineQuery machineQuery, Clock clock) {
        this.client = client;
        this.port = port;
        this.machineQuery = machineQuery;
        this.clock = clock;
    }

    @Scheduled(
        fixedDelayString = "${scheduling.mes.poll-interval-ms:60000}",
        initialDelayString = "${scheduling.mes.poll-initial-delay-ms:60000}")
    public void poll() {
        pollOnce();
    }

    /**
     * 1 polling cycle — 활성 머신 × 당일 4 shift fetch → 수신분 영속.
     *
     * @return 영속(INSERT/UPDATE)된 shift event 수
     */
    public int pollOnce() {
        LocalDate today = LocalDate.now(clock);
        List<VcMachineSummary> machines = machineQuery.findAllActive();
        int persisted = 0;
        for (VcMachineSummary m : machines) {
            for (short shiftNo : SHIFT_NOS) {
                try {
                    Optional<MesShiftResponse> received = client.fetchShift(m.machineId(), today, shiftNo);
                    if (received.isPresent()) {
                        MesShiftResponse d = received.get();
                        port.reportProduction(d.machineId(), d.shiftDate(), d.shiftNo(),
                            d.plannedQty(), d.actualQty(), MesShiftSource.MES, REPORTED_BY, null);
                        persisted++;
                    }
                } catch (Exception e) {
                    log.warn("MES polling 실패 — machine={} shift={} cause={} (다음 cycle 재시도)",
                        m.machineId(), shiftNo, e.getClass().getSimpleName());
                }
            }
        }
        if (persisted > 0) {
            log.info("MES polling cycle 완료 — machines={} persisted={}", machines.size(), persisted);
        }
        return persisted;
    }
}
