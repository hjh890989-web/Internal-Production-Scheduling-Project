package com.scheduling.master.vc;

import com.scheduling.common.metrics.SchedulingMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PG LISTEN/NOTIFY 구독 → SlotCompatibilityMatrix 무효화 — TK-04-1-2 (ADR-017).
 *
 * <p>V007 트리거 {@code trg_vc_constraint_notify} 가 channel {@code vc_constraint_changed} 로
 * pg_notify 발행. 본 listener 는 dedicated daemon thread 에서 5초 blocking poll —
 * 알림 수신 시 즉시 {@link SlotCompatibilityMatrixService#invalidate()} 호출.
 *
 * <p>장애 격리:
 * <ul>
 *   <li>connection 실패 → 30s 후 재연결</li>
 *   <li>1h 주기 {@code @Scheduled} fallback rebuild — LISTEN 누락·재시작 안전 가드</li>
 *   <li>@PreDestroy 시 thread 정지 + connection 정리</li>
 * </ul>
 *
 * <p>{@code @Profile("with-infra")} — DataSource (PG) 의존.
 */
@Component
@Profile("with-infra")
public class VcConstraintChangeListener {

    private static final Logger log = LoggerFactory.getLogger(VcConstraintChangeListener.class);
    private static final String CHANNEL = "vc_constraint_changed";
    private static final int POLL_TIMEOUT_MS = 5_000;
    private static final long RECONNECT_DELAY_MS = 30_000L;

    private final DataSource dataSource;
    private final SlotCompatibilityMatrixService matrixService;
    private final SchedulingMetrics metrics;

    private Thread listenerThread;
    private volatile boolean running = false;

    public VcConstraintChangeListener(
        DataSource dataSource,
        SlotCompatibilityMatrixService matrixService,
        SchedulingMetrics metrics
    ) {
        this.dataSource = dataSource;
        this.matrixService = matrixService;
        this.metrics = metrics;
    }

    @PostConstruct
    public void start() {
        running = true;
        listenerThread = new Thread(this::listenLoop, "vc-constraint-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        log.info("VcConstraintChangeListener started (channel={})", CHANNEL);
    }

    void listenLoop() {
        while (running) {
            try (Connection conn = dataSource.getConnection()) {
                PGConnection pgConn = conn.unwrap(PGConnection.class);
                try (Statement st = conn.createStatement()) {
                    st.execute("LISTEN " + CHANNEL);
                }
                log.info("LISTEN {} subscribed", CHANNEL);

                while (running) {
                    PGNotification[] notifications = pgConn.getNotifications(POLL_TIMEOUT_MS);
                    if (!running) break;
                    if (notifications != null && notifications.length > 0) {
                        for (PGNotification n : notifications) {
                            log.info("vc_constraint changed (hoseId={}) — invalidate matrix",
                                n.getParameter());
                        }
                        try {
                            matrixService.invalidate();
                            if (metrics != null) metrics.increment("vc_matrix", "listen_notify");
                        } catch (RuntimeException e) {
                            log.error("Matrix invalidate failed: {}", e.getMessage(), e);
                        }
                    }
                }
            } catch (SQLException e) {
                if (!running) break;
                log.error("LISTEN connection failed — reconnect in {}ms: {}",
                    RECONNECT_DELAY_MS, e.getMessage());
                if (metrics != null) metrics.increment("vc_matrix", "listen_error");
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /** 1h 주기 fallback rebuild — LISTEN 누락·PG 재시작 안전 가드. */
    @Scheduled(fixedDelay = 60L * 60L * 1000L, initialDelay = 60L * 60L * 1000L)
    public void fallbackRebuild() {
        log.debug("VC matrix fallback rebuild (1h safety)");
        try {
            matrixService.invalidate();
        } catch (RuntimeException e) {
            log.warn("Fallback rebuild failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        log.info("VcConstraintChangeListener stopped");
    }
}
