package com.scheduling.vc.internal;

import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import com.scheduling.vc.events.VcConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * VC 확정 이벤트 발행 — TK-07-1-1 (EP-05 + EP-07 연결점).
 *
 * <p>VcSchedule status 가 CONFIRMED 로 전환되는 시점에 호출. 호라이즌 [from, to] 의
 * CONFIRMED row 들을 묶어 {@link VcConfirmedEvent} 발행.
 *
 * <p>{@link org.springframework.context.event.EventListener} (압출 모듈) 가 구독.
 * Spring Modulith {@code @ApplicationModuleListener} = TransactionalEventListener(AFTER_COMMIT)
 * + Async — 본 트랜잭션 commit 후 처리, 압출 실패해도 VC commit 유지.
 *
 * <p>{@code @Profile("with-infra")} — VcScheduleRepository (JPA) 의존.
 */
@Component
@Profile("with-infra")
public class VcConfirmedPublisher {

    private static final Logger log = LoggerFactory.getLogger(VcConfirmedPublisher.class);

    private final ApplicationEventPublisher publisher;
    private final VcScheduleRepository scheduleRepo;
    private final Clock clock;

    public VcConfirmedPublisher(
        ApplicationEventPublisher publisher,
        VcScheduleRepository scheduleRepo,
        Clock clock
    ) {
        this.publisher = publisher;
        this.scheduleRepo = scheduleRepo;
        this.clock = clock;
    }

    /**
     * 호라이즌 [from, to] 의 CONFIRMED row 들을 단일 batch event 발행.
     *
     * @param scheduleId 배치 식별자 (운영 batch 추적용 — 호출자가 발급)
     * @param from       horizon 시작 (포함)
     * @param to         horizon 끝 (포함)
     * @return 발행된 row 수
     */
    @Transactional(readOnly = true)
    public int publishConfirmedRange(UUID scheduleId, LocalDate from, LocalDate to) {
        List<VcSchedule> confirmedRows = scheduleRepo.findByDateRange(from, to).stream()
            .filter(s -> s.getStatus() == VcScheduleStatus.CONFIRMED)
            .toList();

        if (confirmedRows.isEmpty()) {
            log.info("VcConfirmedPublisher [{} ~ {}] — CONFIRMED row 0건, 이벤트 미발행", from, to);
            return 0;
        }

        List<VcConfirmedEvent.VcConfirmedRow> rows = confirmedRows.stream()
            .map(s -> new VcConfirmedEvent.VcConfirmedRow(
                s.getVcScheduleId(), s.getHoseId(), s.getProductionDate(),
                s.getMachineId(), s.getRotationNo(), s.getSlotPosition(),
                s.getPlannedQty()))
            .toList();

        VcConfirmedEvent event = new VcConfirmedEvent(scheduleId, Instant.now(clock), rows);
        publisher.publishEvent(event);
        log.info("VcConfirmedEvent published — scheduleId={}, rows={}", scheduleId, rows.size());
        return rows.size();
    }
}
