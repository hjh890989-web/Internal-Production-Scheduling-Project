package com.scheduling.vc.mes;

import com.scheduling.vc.events.MesDegradedModeChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sprint 17 BR-X06 degraded mode 감지 — TK-DAY-LOCK-3-2.
 *
 * <p>임계: 1 shift = 6 시간 (4 shifts/day). 마지막 MES 수신 후 6h 경과 → degraded.
 * Excel 폴백 (source=EXCEL_FALLBACK) 도 수신으로 인정 — 운영자가 수동 입력 시 degraded 해제.
 *
 * <p>Sprint 17 baseline 은 stateless 조회 — Redis flag persistence 는 Phase 5+ (실 MES 연동 시).
 * 본 서비스는 in-memory 계산만 수행 — 매 status 조회마다 최근 event 시각 vs now 비교.
 *
 * <p>{@code scheduling.mes.enabled=false} (default) 시 항상 NORMAL 반환 — 실 MES 미연동 노이즈 차단.
 */
@Service
@Profile("with-infra")
public class DegradedModeService {

    private static final Logger log = LoggerFactory.getLogger(DegradedModeService.class);

    /** 1 shift = 6 시간 (4 shifts/day, PDD BR-V04 정합). */
    public static final Duration SHIFT_DURATION = Duration.ofHours(6);

    /** 베타 5 머신 — Sprint 14 V039 vc_machine seed 정합. */
    public static final List<String> ACTIVE_MACHINES = List.of("LP-01", "LP-02", "LP-03", "LP-04", "IC-01");

    private final MesShiftPort mesPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final boolean mesEnabled;

    /**
     * Sprint 18 ST-NOTIFY-4 — 머신별 직전 degraded 상태 (in-memory cache).
     * polling 마다 현재 상태와 비교 → 변화 시 {@link MesDegradedModeChangedEvent} publish.
     * 서버 재시작 시 초기 false (NORMAL) — 재시작 직후 첫 polling 에서 진입 이벤트 발행 가능.
     */
    private final Map<String, Boolean> lastDegradedState = new HashMap<>();

    public DegradedModeService(
        MesShiftPort mesPort,
        ApplicationEventPublisher eventPublisher,
        Clock clock,
        @org.springframework.beans.factory.annotation.Value("${scheduling.mes.enabled:false}") boolean mesEnabled
    ) {
        this.mesPort = mesPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.mesEnabled = mesEnabled;
    }

    /** 단일 머신 degraded 여부 — true = degraded (1 shift+ 미수신). */
    public boolean isDegraded(String machineId) {
        if (!mesEnabled) return false;
        Instant now = Instant.now(clock);
        Optional<MesShiftEvent> last = mesPort.lastReceivedShift(machineId);
        if (last.isEmpty()) {
            // 한 번도 수신 안 됨 — 시스템 신규 부팅 직후 노이즈 차단 위해 NORMAL 으로 처리
            return false;
        }
        Duration elapsed = Duration.between(last.get().getReceivedAt(), now);
        return elapsed.compareTo(SHIFT_DURATION) > 0;
    }

    /** 전체 머신 status snapshot — Frontend 배너 + GET /degraded/status 응답 source. */
    public DegradedSnapshot snapshot() {
        Instant now = Instant.now(clock);
        if (!mesEnabled) {
            return new DegradedSnapshot(false, now, "MES disabled (scheduling.mes.enabled=false)",
                List.of());
        }
        List<MachineStatus> statuses = ACTIVE_MACHINES.stream()
            .map(m -> {
                Optional<MesShiftEvent> last = mesPort.lastReceivedShift(m);
                Instant lastAt = last.map(MesShiftEvent::getReceivedAt).orElse(null);
                MesShiftSource source = last.map(MesShiftEvent::getSource).orElse(null);
                boolean degraded = isDegraded(m);
                return new MachineStatus(m, degraded, lastAt, source);
            })
            .toList();
        boolean anyDegraded = statuses.stream().anyMatch(MachineStatus::degraded);
        String summary = anyDegraded
            ? statuses.stream().filter(MachineStatus::degraded)
                .map(MachineStatus::machineId)
                .reduce((a, b) -> a + ", " + b)
                .map(s -> "Degraded: " + s)
                .orElse("Degraded")
            : "MES 정상 수신";
        return new DegradedSnapshot(anyDegraded, now, summary, statuses);
    }

    /**
     * Sprint 18 ST-NOTIFY-4 — 1분 주기 polling + 변화 시 이벤트 발행.
     *
     * <p>{@code scheduling.notification.degraded-poll.interval-ms} (기본 60s) 주기로 머신별
     * 현재 degraded 여부 계산 → 직전 상태와 다르면 {@link MesDegradedModeChangedEvent} publish.
     *
     * <p>mesEnabled=false 시 polling skip (snapshot 가 항상 NORMAL 반환).
     */
    @Scheduled(fixedDelayString = "${scheduling.notification.degraded-poll.interval-ms:60000}")
    public void pollAndPublish() {
        if (!mesEnabled) return;
        Instant now = Instant.now(clock);
        for (String machineId : ACTIVE_MACHINES) {
            boolean current = isDegraded(machineId);
            Boolean prev = lastDegradedState.get(machineId);
            // 신규 머신 (prev null) 은 baseline 으로 set (이벤트 미발행, NORMAL → NORMAL 노이즈 차단)
            if (prev == null) {
                lastDegradedState.put(machineId, current);
                if (current) {
                    log.warn("MES degraded baseline 진입 — machineId={} (서버 재시작 직후 첫 polling)", machineId);
                    eventPublisher.publishEvent(
                        new MesDegradedModeChangedEvent(machineId, false, true, now));
                }
                continue;
            }
            if (prev != current) {
                log.warn("MES degraded 전이 — machineId={} {} → {}", machineId,
                    prev ? "DEGRADED" : "NORMAL", current ? "DEGRADED" : "NORMAL");
                eventPublisher.publishEvent(
                    new MesDegradedModeChangedEvent(machineId, prev, current, now));
                lastDegradedState.put(machineId, current);
            }
        }
    }

    public record MachineStatus(String machineId, boolean degraded, Instant lastReceivedAt, MesShiftSource lastSource) {}

    public record DegradedSnapshot(boolean anyDegraded, Instant checkedAt, String summary, List<MachineStatus> machines) {}
}
