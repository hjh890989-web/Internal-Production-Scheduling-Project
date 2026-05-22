package com.scheduling.master.spec;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.master.api.ProductSpecLookup;
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
import java.util.List;

/**
 * Cross-master LISTEN/NOTIFY — TK-21-5-2 (ADR-017).
 *
 * <p>VC_CONSTRAINT 와 EX_CONSTRAINT 두 채널 모두 구독 — VIEW {@code v_product_with_spec}
 * 가 양쪽 underlying 테이블 결합이므로 어느 쪽 변경이든 캐시 invalidate 필요.
 *
 * <p>구독 채널:
 * <ul>
 *   <li>{@code vc_constraint_changed} — V007 트리거 (TK-04-1-2)</li>
 *   <li>{@code ex_constraint_changed} — V016 트리거 (TK-21-5-1)</li>
 * </ul>
 */
@Component
@Profile("with-infra")
public class ProductSpecChangeListener {

    private static final Logger log = LoggerFactory.getLogger(ProductSpecChangeListener.class);
    private static final List<String> CHANNELS = List.of(
        "vc_constraint_changed", "ex_constraint_changed");
    private static final int POLL_TIMEOUT_MS = 5_000;
    private static final long RECONNECT_DELAY_MS = 30_000L;

    private final DataSource dataSource;
    private final ProductSpecLookup lookup;
    private final SchedulingMetrics metrics;

    private Thread listenerThread;
    private volatile boolean running = false;

    public ProductSpecChangeListener(
        DataSource dataSource,
        ProductSpecLookup lookup,
        SchedulingMetrics metrics
    ) {
        this.dataSource = dataSource;
        this.lookup = lookup;
        this.metrics = metrics;
    }

    @PostConstruct
    public void start() {
        running = true;
        listenerThread = new Thread(this::listenLoop, "product-spec-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        log.info("ProductSpecChangeListener started (channels={})", CHANNELS);
    }

    void listenLoop() {
        while (running) {
            try (Connection conn = dataSource.getConnection()) {
                PGConnection pgConn = conn.unwrap(PGConnection.class);
                try (Statement st = conn.createStatement()) {
                    for (String ch : CHANNELS) {
                        st.execute("LISTEN " + ch);
                    }
                }
                log.info("LISTEN {} subscribed", CHANNELS);

                while (running) {
                    PGNotification[] notifications = pgConn.getNotifications(POLL_TIMEOUT_MS);
                    if (!running) break;
                    if (notifications != null && notifications.length > 0) {
                        for (PGNotification n : notifications) {
                            String hoseId = n.getParameter();
                            log.info("product spec change (channel={}, hoseId={}) — invalidate",
                                n.getName(), hoseId);
                            try {
                                if (hoseId == null || hoseId.isBlank()) {
                                    lookup.invalidateAll();
                                } else {
                                    lookup.invalidate(hoseId);
                                }
                                if (metrics != null) metrics.increment("product_spec", "listen_notify");
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
                if (metrics != null) metrics.increment("product_spec", "listen_error");
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /** 1h 주기 fallback — LISTEN 누락 안전 가드. */
    @Scheduled(fixedDelay = 60L * 60L * 1000L, initialDelay = 60L * 60L * 1000L)
    public void fallbackInvalidate() {
        log.debug("ProductSpec fallback invalidateAll (1h safety)");
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
        log.info("ProductSpecChangeListener stopped");
    }
}
