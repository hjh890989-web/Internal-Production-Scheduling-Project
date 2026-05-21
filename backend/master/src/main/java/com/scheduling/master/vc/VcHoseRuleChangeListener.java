package com.scheduling.master.vc;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.master.api.HoseRuleLookup;
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
 * PG LISTEN/NOTIFY 구독 → HoseRule 캐시 무효화 — TK-21-2-3.
 *
 * <p>V014 트리거 {@code trg_vc_hose_rule_notify} 가 channel {@code vc_hose_rule_changed}
 * 로 pg_notify 발행. 본 listener 는 dedicated daemon thread 에서 5초 blocking poll —
 * 알림 수신 시 즉시 {@link HoseRuleLookup#invalidate(String)} 호출 (selective).
 *
 * <p>{@link com.scheduling.master.vc.VcConstraintChangeListener} 패턴 재사용 —
 * 동일 connection·재연결·1h fallback 정책.
 *
 * <p>{@code @Profile("with-infra")} — DataSource (PG) 의존.
 */
@Component
@Profile("with-infra")
public class VcHoseRuleChangeListener {

    private static final Logger log = LoggerFactory.getLogger(VcHoseRuleChangeListener.class);
    private static final String CHANNEL = "vc_hose_rule_changed";
    private static final int POLL_TIMEOUT_MS = 5_000;
    private static final long RECONNECT_DELAY_MS = 30_000L;

    private final DataSource dataSource;
    private final HoseRuleLookup lookup;
    private final SchedulingMetrics metrics;

    private Thread listenerThread;
    private volatile boolean running = false;

    public VcHoseRuleChangeListener(
        DataSource dataSource,
        HoseRuleLookup lookup,
        SchedulingMetrics metrics
    ) {
        this.dataSource = dataSource;
        this.lookup = lookup;
        this.metrics = metrics;
    }

    @PostConstruct
    public void start() {
        running = true;
        listenerThread = new Thread(this::listenLoop, "vc-hose-rule-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        log.info("VcHoseRuleChangeListener started (channel={})", CHANNEL);
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
                            String hoseId = n.getParameter();
                            log.info("vc_hose_rule changed (hoseId={}) — invalidate cache", hoseId);
                            try {
                                if (hoseId == null || hoseId.isBlank()) {
                                    lookup.invalidateAll();
                                } else {
                                    lookup.invalidate(hoseId);
                                }
                                if (metrics != null) metrics.increment("vc_hose_rule", "listen_notify");
                            } catch (RuntimeException e) {
                                log.error("Cache invalidate failed: {}", e.getMessage(), e);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                if (!running) break;
                log.error("LISTEN connection failed — reconnect in {}ms: {}",
                    RECONNECT_DELAY_MS, e.getMessage());
                if (metrics != null) metrics.increment("vc_hose_rule", "listen_error");
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /** 1h 주기 fallback — LISTEN 누락·PG 재시작 안전 가드. */
    @Scheduled(fixedDelay = 60L * 60L * 1000L, initialDelay = 60L * 60L * 1000L)
    public void fallbackInvalidate() {
        log.debug("VC hose rule fallback invalidateAll (1h safety)");
        try {
            lookup.invalidateAll();
        } catch (RuntimeException e) {
            log.warn("Fallback invalidate failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        log.info("VcHoseRuleChangeListener stopped");
    }
}
