---
name: QA Task (E2E Data Simulator)
about: 단일 작업 단위 — 0.7 PD
title: "[TK-E2E-1-1] 데이터 시뮬레이터 (1주 분량 수주 생성)"
labels: 'sprint:S5, epic:EP-E2E, story:ST-E2E-1, type:test, priority:must, owner:backend+qa'
assignees: ''
---

## :dart: Task Summary
- **Task ID**: TK-E2E-1-1
- **소속**: EP-E2E / ST-E2E-1 / Sprint S5
- **우선순위**: Must / **추정**: 0.7 PD (~5.6h) / **Owner**: Backend + QA
- **작업 요약**: `E2EDataSimulator` — 1주 분량 현실적 수주 생성. 47품번 가중치 + 납기 분포 (월~금 mix) + 다양 quantity (50~500). 베타 환경 시드용. Spring Boot CLI tool 또는 JUnit fixture.

---

## :link: References
- **상위 Story**: [`_Story_Overview.md`](_Story_Overview.md)
- **WBS**: §8 EP-E2E ST-E2E-1 (Task 1)
- **연관**: 선행 [TK-01-1-3](../../EP-01/ST-01-1/TK-01-1-3.md), 후행 [TK-E2E-1-2](TK-E2E-1-2.md)

---

## :hammer_and_wrench: Implementation Plan

```
backend/scheduling-engine/
  src/main/java/com/scheduling/test/simulator/
    E2EDataSimulator.java                        [신규]
    SimulatorConfig.java                         [Bean]
    SalesOrderGenerator.java                     [수주 random gen]
  src/test/resources/fixtures/
    e2e-baseline-1week.csv                       [재현 baseline]
```

### `E2EDataSimulator.java`

```java
@Component @RequiredArgsConstructor @Slf4j
public class E2EDataSimulator {

    private final SalesOrderGenerator orderGen;
    private final OrderRepository orderRepo;

    /**
     * 1주 분량 (월~금) 수주 생성.
     *  - 47품번 가중치 (Phase 1 분석 분포)
     *  - 납기 분포: 월~다음주 금 (10일 범위)
     *  - 수량: 50~500 random
     *  - 총: ≈ 200~300 수주 (일 평균 40~60건)
     */
    public SimulationResult generateWeek(LocalDate startDate, long seed) {
        var random = new Random(seed);
        var orders = new ArrayList<Order>();

        for (LocalDate date = startDate; date.isBefore(startDate.plusDays(7)); date = date.plusDays(1)) {
            int dailyCount = 40 + random.nextInt(20);

            for (int i = 0; i < dailyCount; i++) {
                var order = orderGen.generate(date, random);
                orders.add(order);
            }
        }

        orderRepo.saveAll(orders);

        log.info("E2E 시뮬레이션 1주 데이터: {} 수주 생성", orders.size());
        return new SimulationResult(orders.size(), startDate, seed);
    }
}
```

### `SalesOrderGenerator.java`

```java
@Component
public class SalesOrderGenerator {

    // Phase 1 47품번 가중치 (자주 발주 vs 드문 품번)
    private static final List<HoseWeight> HOSE_WEIGHTS = List.of(
        new HoseWeight("29673-2R060", 15),     // 자주
        new HoseWeight("28912-2U000", 12),
        new HoseWeight("29689-2U000", 10),
        new HoseWeight("A6722030900",  8),
        new HoseWeight("28422-08HA0",  5),
        new HoseWeight("28421-2M800",  3),
        new HoseWeight("28422-2M800",  3),
        // ... 47품번 분포
    );
    private static final int TOTAL_WEIGHT = HOSE_WEIGHTS.stream().mapToInt(HoseWeight::weight).sum();

    public Order generate(LocalDate orderDate, Random random) {
        String hoseId = pickWeightedHose(random);
        int quantity = 50 + random.nextInt(450);     // 50~500
        LocalDate deliveryDate = orderDate.plusDays(3 + random.nextInt(10));     // 3~12일 후

        return Order.builder()
            .id(UUID.randomUUID())
            .hoseId(hoseId)
            .quantity(quantity)
            .orderDate(orderDate)
            .deliveryDate(deliveryDate)
            .source("E2E_SIMULATOR")
            .build();
    }

    private String pickWeightedHose(Random random) {
        int r = random.nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (var hw : HOSE_WEIGHTS) {
            cumulative += hw.weight();
            if (r < cumulative) return hw.hoseId();
        }
        return HOSE_WEIGHTS.get(0).hoseId();     // fallback
    }

    record HoseWeight(String hoseId, int weight) {}
}
```

---

## :test_tube: Acceptance Criteria

**검증**: T-U + T-I

- [ ] **1주 200~300 수주** 생성
- [ ] **47품번 모두 등장** (가중치 분포)
- [ ] **납기 3~12일 후** 분포
- [ ] **수량 50~500** 범위
- [ ] **seed 동일 시 재현** (deterministic — 동일 seed = 동일 결과)
- [ ] **baseline CSV** 저장 — 회귀 시 동일 입력 보장
- [ ] 단위 + 통합 테스트 ≥ 4 케이스

---

## :checkered_flag: Definition of Done

- [ ] 위 측정 기준 통과
- [ ] Simulator + Generator + baseline CSV
- [ ] Testcontainers 통합 테스트
- [ ] 커버리지 ≥ 80%
- [ ] 코드 리뷰 1명 이상 승인

---

## :construction: Dependencies

- **선행**: [TK-01-1-3](../../EP-01/ST-01-1/TK-01-1-3.md), [TK-99-1-3](../../EP-99/ST-99-1/TK-99-1-3.md) (47품번 master)
- **후행**: [TK-E2E-1-2](TK-E2E-1-2.md), [TK-E2E-1-3](TK-E2E-1-3.md)
- **Critical Path**: ⭐⭐

---

## :memo: Implementation Notes

- 가중치는 Phase 1 분석 결과 (현장 실 분포) — 운영팀 검토 후 조정 가능
- baseline CSV 저장으로 회귀 재현성 보장 — random seed 외 백업
- 향후 production replay (실 수주 anonymized) 검토 — Phase 2+
