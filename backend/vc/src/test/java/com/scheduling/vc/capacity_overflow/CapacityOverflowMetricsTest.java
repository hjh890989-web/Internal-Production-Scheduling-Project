package com.scheduling.vc.capacity_overflow;

import com.scheduling.master.api.KdOrderLookup;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sprint 8 EP-V13-Grafana — CapacityOverflowMetrics 단위 검증.
 */
class CapacityOverflowMetricsTest {

    private KdOrderLookup kdLookup;
    private CapacityOverflowRequestRepository requestRepo;
    private MeterRegistry registry;
    private CapacityOverflowMetrics metrics;

    @BeforeEach
    void setUp() {
        kdLookup = mock(KdOrderLookup.class);
        requestRepo = mock(CapacityOverflowRequestRepository.class);
        registry = new SimpleMeterRegistry();
        metrics = new CapacityOverflowMetrics(kdLookup, requestRepo, registry);
    }

    @Test
    @DisplayName("refresh — hose 별 KD remaining_qty gauge 등록 (Tag = hose)")
    void refresh_registers_kd_remaining_per_hose() {
        when(kdLookup.remainingByHose()).thenReturn(Map.of(
            "29673-2R060", 70L,
            "28422-2M800", 30L
        ));
        when(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        metrics.refresh();

        Gauge g1 = registry.find("scheduling.v13.kd.remaining.qty").tag("hose", "29673-2R060").gauge();
        Gauge g2 = registry.find("scheduling.v13.kd.remaining.qty").tag("hose", "28422-2M800").gauge();
        assertThat(g1).isNotNull();
        assertThat(g1.value()).isEqualTo(70.0);
        assertThat(g2).isNotNull();
        assertThat(g2.value()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("refresh — V12 PENDING/ACCEPTED/REJECTED 3 status gauge 모두 등록")
    void refresh_registers_v12_pending_status_counts() {
        when(kdLookup.remainingByHose()).thenReturn(Map.of());

        CapacityOverflowRequest pending = new CapacityOverflowRequest(
            UUID.randomUUID(), "X", 10, (short) 1,
            java.time.Instant.parse("2026-06-01T00:00:00Z"), "actor");
        when(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.PENDING)).thenReturn(List.of(pending, pending));
        when(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.ACCEPTED)).thenReturn(List.of(pending));
        when(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.REJECTED)).thenReturn(List.of());

        metrics.refresh();

        assertThat(registry.find("scheduling.v12.pending.request.count")
            .tag("status", "PENDING").gauge().value()).isEqualTo(2.0);
        assertThat(registry.find("scheduling.v12.pending.request.count")
            .tag("status", "ACCEPTED").gauge().value()).isEqualTo(1.0);
        assertThat(registry.find("scheduling.v12.pending.request.count")
            .tag("status", "REJECTED").gauge().value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("refresh — 빈 결과 (마스터 미입력 시점) 도 에러 없이 처리")
    void refresh_handles_empty_state() {
        when(kdLookup.remainingByHose()).thenReturn(Map.of());
        when(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        metrics.refresh();

        // KD gauge 없음 — hose tag 가 등록 안 됨
        assertThat(registry.find("scheduling.v13.kd.remaining.qty").gauges()).isEmpty();
        // V12 status gauge 0 으로 등록
        assertThat(registry.find("scheduling.v12.pending.request.count")
            .tag("status", "PENDING").gauge().value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("refresh — KdLookup 예외 발생 시 metric 갱신 보류 + log warn (다음 cycle 재시도)")
    void refresh_swallows_lookup_exception() {
        when(kdLookup.remainingByHose()).thenThrow(new RuntimeException("transient DB"));
        when(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        // 예외 외부 propagate 안 됨 (Scheduled job 안정성)
        metrics.refresh();
        // gauge 없음 (전 refresh 실패 시 직전 값 유지하거나 미등록)
        assertThat(registry.find("scheduling.v13.kd.remaining.qty").gauges()).isEmpty();
    }
}
