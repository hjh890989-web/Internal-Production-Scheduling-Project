package com.scheduling.order.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * 우선순위 해소 audit 로거 — TK-02-2-2 (BR-X02 강제).
 *
 * <p>본 Sprint 1 baseline 은 SLF4J 로그 (Loki 90일 보존) — Sprint 1+ EP-11 audit 모듈 활성 후
 * {@code audit.precedence_resolution} 테이블 INSERT 로 영속화.
 *
 * <p>로그 구조:
 * <pre>
 *   AUDIT precedence_resolution key={hose, date} decision=NEW_WINS winner=CONFIRMED losers=[FORECAST, KD] existing=null at={KST instant}
 * </pre>
 */
@Component
public class PrecedenceAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(PrecedenceAuditLogger.class);

    private final Clock clock;

    public PrecedenceAuditLogger(Clock clock) {
        this.clock = clock;
    }

    public void log(Resolution resolution) {
        if (resolution == null) return;
        Instant at = Instant.now(clock);
        log.info("AUDIT precedence_resolution key={} decision={} winner={} losers={} existing={} at={}",
            resolution.key(),
            resolution.decision(),
            resolution.winner().orderType(),
            loserTypes(resolution),
            resolution.hasExisting() ? resolution.existingMaster().getOrderType() : null,
            at);
    }

    private String loserTypes(Resolution resolution) {
        if (resolution.losers().isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < resolution.losers().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(resolution.losers().get(i).orderType());
        }
        return sb.append("]").toString();
    }
}
