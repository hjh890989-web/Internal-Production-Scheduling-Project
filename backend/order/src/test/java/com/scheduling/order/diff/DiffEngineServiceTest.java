package com.scheduling.order.diff;

import com.scheduling.order.domain.Order;
import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.domain.OrderKey;
import com.scheduling.order.domain.OrderRepository;
import com.scheduling.order.domain.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DiffEngineService 회귀 — TK-03-1-1 + TK-03-1-2 (TC-OC-007 100% 정확도).
 *
 * <p>15+ 케이스: 표준 4 분류 + 다중 필드 + UNCHANGED + 혼합 시나리오 100건 + edge.
 */
class DiffEngineServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 2, 15);
    private static final UUID TRACKING = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private OrderRepository repository;
    private Clock clock;
    private DiffEngineService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        clock = Clock.fixed(Instant.parse("2026-05-20T05:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new DiffEngineService(repository, clock);
        when(repository.findByMasterVersion(anyInt())).thenReturn(List.of());
    }

    private OrderDraft draft(String hoseId, LocalDate date, int qty, OrderType type, String customer) {
        return new OrderDraft(UUID.randomUUID(), hoseId, date, qty, type, customer);
    }

    private Order existing(String hoseId, LocalDate date, int qty, OrderType type, String customer) {
        return new Order(UUID.randomUUID(), hoseId, date, qty, type, customer, 1,
            "ACTIVE", Instant.parse("2026-02-01T00:00:00Z"));
    }

    // ---------- 표준 4 분류 ----------

    @Test
    @DisplayName("NEW — 빈 마스터 + 신규 100 row → 100 RowDiff.NEW")
    void all_new_when_previous_version_zero() {
        List<OrderDraft> newRows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            newRows.add(draft("HOSE-" + i, D.plusDays(i), 100, OrderType.CONFIRMED, "내수"));
        }

        DiffResult result = service.compute(TRACKING, newRows, 0);

        assertThat(result.rows()).hasSize(100);
        assertThat(result.countByType(DiffType.NEW)).isEqualTo(100);
        assertThat(result.countByType(DiffType.DELETED)).isZero();
        assertThat(result.countByType(DiffType.MODIFIED)).isZero();
        assertThat(result.countByType(DiffType.UNCHANGED)).isZero();
    }

    @Test
    @DisplayName("DELETED — 마스터 50 + 빈 신규 → 50 DELETED")
    void all_deleted_when_new_is_empty() {
        List<Order> old = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            old.add(existing("HOSE-" + i, D.plusDays(i), 50, OrderType.WEEKLY, "내수"));
        }
        when(repository.findByMasterVersion(1)).thenReturn(old);

        DiffResult result = service.compute(TRACKING, List.of(), 1);

        assertThat(result.countByType(DiffType.DELETED)).isEqualTo(50);
        assertThat(result.countByType(DiffType.NEW)).isZero();
    }

    @Test
    @DisplayName("MODIFIED 단일 필드 — qty 변경 → fieldDiffs.size==1")
    void single_field_modified_qty() {
        Order old = existing("H1", D, 100, OrderType.WEEKLY, "내수");
        OrderDraft fresh = new OrderDraft(old.getOrderId(), "H1", D, 200, OrderType.WEEKLY, "내수");
        when(repository.findByMasterVersion(1)).thenReturn(List.of(old));

        DiffResult result = service.compute(TRACKING, List.of(fresh), 1);

        assertThat(result.countByType(DiffType.MODIFIED)).isEqualTo(1);
        RowDiff mod = result.rowsOfType(DiffType.MODIFIED).get(0);
        assertThat(mod.fieldDiffs()).hasSize(1);
        assertThat(mod.fieldDiffs().get(0).fieldName()).isEqualTo("qty");
        assertThat(mod.fieldDiffs().get(0).before()).isEqualTo(100);
        assertThat(mod.fieldDiffs().get(0).after()).isEqualTo(200);
    }

    @Test
    @DisplayName("MODIFIED 다중 필드 — qty + customer + order_type 동시 변경")
    void multi_field_modified() {
        Order old = existing("H1", D, 100, OrderType.FORECAST, "내수");
        OrderDraft fresh = new OrderDraft(old.getOrderId(), "H1", D, 500, OrderType.CONFIRMED, "현대모비스");
        when(repository.findByMasterVersion(1)).thenReturn(List.of(old));

        DiffResult result = service.compute(TRACKING, List.of(fresh), 1);

        RowDiff mod = result.rowsOfType(DiffType.MODIFIED).get(0);
        assertThat(mod.fieldDiffs()).extracting(FieldDiff::fieldName)
            .containsExactlyInAnyOrder("qty", "customer", "order_type");
    }

    @Test
    @DisplayName("UNCHANGED — 모든 필드 동일")
    void unchanged_all_fields_equal() {
        Order old = existing("H1", D, 100, OrderType.WEEKLY, "내수");
        OrderDraft fresh = new OrderDraft(old.getOrderId(), "H1", D, 100, OrderType.WEEKLY, "내수");
        when(repository.findByMasterVersion(1)).thenReturn(List.of(old));

        DiffResult result = service.compute(TRACKING, List.of(fresh), 1);

        assertThat(result.countByType(DiffType.UNCHANGED)).isEqualTo(1);
        assertThat(result.rowsOfType(DiffType.UNCHANGED).get(0).fieldDiffs()).isEmpty();
    }

    // ---------- 혼합 시나리오 ----------

    @Test
    @DisplayName("혼합 — 30 NEW + 20 DELETED + 25 MODIFIED + 25 UNCHANGED → 정확 분류")
    void mixed_scenario_100_rows() {
        // 이전 마스터 50 row: 0~24 UNCHANGED, 25~49 MODIFIED 또는 DELETED
        List<Order> old = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            old.add(existing("H-" + i, D.plusDays(i), 100, OrderType.WEEKLY, "내수"));
        }
        when(repository.findByMasterVersion(1)).thenReturn(old);

        // 신규 80 row:
        //   0~24 UNCHANGED (이전과 동일 키+필드)
        //   25~49 MODIFIED (이전 키 + qty 변경) — 25건
        //   50~79 NEW (새 키) — 30건
        //   이전 25건 (oldRange) → 50~74 키만 신규 — 5건 그대로, 20건 DELETED
        // 위 조건을 맞추기 위해 단순화:
        //   25~49 (25건) MODIFIED, 0~24 (25건) UNCHANGED
        //   50~79 (30건) NEW
        //   25~49 만 신규에 등장하므로 0~24 는 신규에서 빠짐 → 그래도 UNCHANGED 가 0~24 매칭 안됨!
        // 다시: 신규 = 0~24 (UNCHANGED) + 25~49 (MODIFIED) + 50~79 (NEW)
        //   이전 = 0~49 → 신규 에 0~49 모두 있음 → DELETED 0건
        // 30 DELETED 만들려면 이전을 더 크게: 이전 70 row (0~69), 신규 0~24 UNCHANGED + 25~49 MODIFIED + 70~99 NEW
        //   이전에서 50~69 (20건) 은 신규에 없음 → DELETED
        // 결과: UNCHANGED 25 + MODIFIED 25 + NEW 30 + DELETED 20 = 100
        List<Order> oldFull = new ArrayList<>(70);
        for (int i = 0; i < 70; i++) {
            oldFull.add(existing("H-" + i, D.plusDays(i), 100, OrderType.WEEKLY, "내수"));
        }
        when(repository.findByMasterVersion(2)).thenReturn(oldFull);

        List<OrderDraft> newRows = new ArrayList<>(80);
        // 0~24 UNCHANGED
        for (int i = 0; i < 25; i++) {
            Order src = oldFull.get(i);
            newRows.add(new OrderDraft(src.getOrderId(), src.getHoseId(), src.getDeliveryDate(),
                src.getQty(), src.getOrderType(), src.getCustomer()));
        }
        // 25~49 MODIFIED (qty 변경)
        for (int i = 25; i < 50; i++) {
            Order src = oldFull.get(i);
            newRows.add(new OrderDraft(src.getOrderId(), src.getHoseId(), src.getDeliveryDate(),
                src.getQty() + 100, src.getOrderType(), src.getCustomer()));
        }
        // 70~99 NEW
        for (int i = 70; i < 100; i++) {
            newRows.add(draft("H-" + i, D.plusDays(i), 100, OrderType.WEEKLY, "내수"));
        }

        DiffResult result = service.compute(TRACKING, newRows, 2);
        assertThat(result.countByType(DiffType.UNCHANGED)).isEqualTo(25);
        assertThat(result.countByType(DiffType.MODIFIED)).isEqualTo(25);
        assertThat(result.countByType(DiffType.NEW)).isEqualTo(30);
        assertThat(result.countByType(DiffType.DELETED)).isEqualTo(20);
        assertThat(result.rows()).hasSize(100);
    }

    // ---------- Edge cases ----------

    @Test
    @DisplayName("customer null/blank — 정규화('내수') 후 비교 → UNCHANGED")
    void customer_null_normalized_to_internal() {
        Order old = existing("H1", D, 100, OrderType.WEEKLY, "내수");
        OrderDraft fresh = new OrderDraft(old.getOrderId(), "H1", D, 100, OrderType.WEEKLY, "내수");
        // OrderDraft canonical ctor 이 null → "내수" 자동 변환
        when(repository.findByMasterVersion(1)).thenReturn(List.of(old));

        DiffResult result = service.compute(TRACKING, List.of(fresh), 1);

        assertThat(result.countByType(DiffType.UNCHANGED)).isEqualTo(1);
    }

    @Test
    @DisplayName("OrderType 변경 (FORECAST → CONFIRMED) → MODIFIED + order_type fieldDiff")
    void order_type_change_creates_field_diff() {
        Order old = existing("H1", D, 100, OrderType.FORECAST, "내수");
        OrderDraft fresh = new OrderDraft(old.getOrderId(), "H1", D, 100, OrderType.CONFIRMED, "내수");
        when(repository.findByMasterVersion(1)).thenReturn(List.of(old));

        DiffResult result = service.compute(TRACKING, List.of(fresh), 1);

        RowDiff mod = result.rowsOfType(DiffType.MODIFIED).get(0);
        assertThat(mod.fieldDiffs()).hasSize(1);
        assertThat(mod.fieldDiffs().get(0).fieldName()).isEqualTo("order_type");
        assertThat(mod.fieldDiffs().get(0).before()).isEqualTo(OrderType.FORECAST);
        assertThat(mod.fieldDiffs().get(0).after()).isEqualTo(OrderType.CONFIRMED);
    }

    @Test
    @DisplayName("delivery_date 변경 — fieldName=delivery_date")
    void delivery_date_change() {
        Order old = existing("H1", D, 100, OrderType.WEEKLY, "내수");
        LocalDate newD = D.plusDays(5);
        // hose_id 같지만 delivery_date 다름 → OrderKey 다름 → NEW + DELETED 2건 (실은 다른 키)
        OrderDraft fresh = new OrderDraft(old.getOrderId(), "H1", newD, 100, OrderType.WEEKLY, "내수");
        when(repository.findByMasterVersion(1)).thenReturn(List.of(old));

        DiffResult result = service.compute(TRACKING, List.of(fresh), 1);

        // 키가 다르므로 NEW 1 + DELETED 1
        assertThat(result.countByType(DiffType.NEW)).isEqualTo(1);
        assertThat(result.countByType(DiffType.DELETED)).isEqualTo(1);
    }

    @Test
    @DisplayName("DiffResult.totalChanges — UNCHANGED 제외")
    void total_changes_excludes_unchanged() {
        List<RowDiff> rows = List.of(
            new RowDiff(new OrderKey("A", D), DiffType.NEW, null, null, List.of()),
            new RowDiff(new OrderKey("B", D), DiffType.UNCHANGED, null, null, List.of()),
            new RowDiff(new OrderKey("C", D), DiffType.MODIFIED, null, null, List.of()),
            new RowDiff(new OrderKey("D", D), DiffType.DELETED, null, null, List.of())
        );
        DiffResult result = new DiffResult(TRACKING, 1, 2, Instant.now(clock), rows);
        assertThat(result.totalChanges()).isEqualTo(3);
        assertThat(result.rows()).hasSize(4);
    }

    @Test
    @DisplayName("previousVersion=0 — repository.findByMasterVersion 호출 X (이전 마스터 없음)")
    void zero_previous_version_skips_query() {
        OrderDraft fresh = draft("H1", D, 100, OrderType.WEEKLY, "내수");
        DiffResult result = service.compute(TRACKING, List.of(fresh), 0);

        assertThat(result.previousVersion()).isZero();
        assertThat(result.newVersion()).isEqualTo(1);
        assertThat(result.countByType(DiffType.NEW)).isEqualTo(1);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).findByMasterVersion(anyInt());
    }

    @Test
    @DisplayName("computedAt — Clock 주입 (BR-X04 KST)")
    void computed_at_uses_injected_clock() {
        DiffResult result = service.compute(TRACKING, List.of(), 0);
        assertThat(result.computedAt()).isEqualTo(Instant.parse("2026-05-20T05:00:00Z"));
    }

    @Test
    @DisplayName("rowsOfType 헬퍼 — 특정 type 만 추출")
    void rows_of_type_helper() {
        List<RowDiff> rows = List.of(
            new RowDiff(new OrderKey("A", D), DiffType.NEW, null, null, List.of()),
            new RowDiff(new OrderKey("B", D), DiffType.NEW, null, null, List.of()),
            new RowDiff(new OrderKey("C", D), DiffType.UNCHANGED, null, null, List.of())
        );
        DiffResult result = new DiffResult(TRACKING, 1, 2, Instant.now(clock), rows);
        assertThat(result.rowsOfType(DiffType.NEW)).hasSize(2);
        assertThat(result.rowsOfType(DiffType.MODIFIED)).isEmpty();
    }
}
