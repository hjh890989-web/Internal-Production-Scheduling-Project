package com.scheduling.audit.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Sprint 22 ST-SEC-3 TK-SEC-3-2 — audit_log 월별 partition rolling-window 유지.
 *
 * <p>V030 이 2026m01~2028m12 (36개) partition 을 사전 생성했으나, 2029-01 부터는
 * DEFAULT partition fallback. 본 스케줄러가 매월 향후 12개월 partition 을 미리 확보하여
 * DEFAULT 비대화 방지 + 인덱스 효율 유지 (NFR-SEC-004 3년 보존 영속).
 *
 * <p>{@code audit.ensure_month_partition(DATE)} (V053) 는 idempotent — 이미 존재하는 월은 skip.
 *
 * <p>{@code @Profile("with-infra")} — DEV 컨텍스트 미활성 (DataSource 부재 시 schedule fail 방지).
 * {@link Clock} 주입 — BR-X04 KST 정합 (cron zone Asia/Seoul).
 */
@Component
@Profile("with-infra")
public class PartitionMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(PartitionMaintenanceScheduler.class);

    /** 현재 월 포함 향후 N개월 partition 사전 확보. */
    private static final int LOOKAHEAD_MONTHS = 12;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public PartitionMaintenanceScheduler(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * 매월 25일 03시 (KST) — 다음 달 진입 전 향후 12개월 partition 확보.
     *
     * @return 생성/존재 결과 요약 (테스트·로그용)
     */
    @Scheduled(cron = "0 0 3 25 * ?", zone = "Asia/Seoul")
    public String ensureUpcomingPartitions() {
        LocalDate base = LocalDate.now(clock).withDayOfMonth(1);
        int created = 0;
        for (int i = 0; i <= LOOKAHEAD_MONTHS; i++) {
            LocalDate target = base.plusMonths(i);
            String result = jdbc.queryForObject(
                "SELECT audit.ensure_month_partition(?)", String.class, Date.valueOf(target));
            if (result != null && result.startsWith("CREATED")) {
                created++;
                log.info("audit partition rolling-window — {}", result);
            }
        }
        String summary = String.format(
            "audit partition maintenance 완료 — base=%s, lookahead=%d개월, created=%d",
            base, LOOKAHEAD_MONTHS, created);
        log.info(summary);
        return summary;
    }
}
