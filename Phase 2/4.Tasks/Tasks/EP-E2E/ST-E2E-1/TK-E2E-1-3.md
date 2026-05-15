---
name: QA Task (D-Day Compliance Verification)
about: 단일 작업 단위 — 0.7 PD
title: "[TK-E2E-1-3] 모든 납기 D-Day 충족 검증"
labels: 'sprint:S5, epic:EP-E2E, story:ST-E2E-1, type:test, priority:must, owner:qa'
assignees: ''
---

## :dart: Task Summary
- **Task ID**: TK-E2E-1-3
- **소속**: EP-E2E / ST-E2E-1 / Sprint S5
- **우선순위**: Must / **추정**: 0.7 PD (~5.6h) / **Owner**: QA
- **작업 요약**: TC-E2E-1 — 시뮬레이션 결과 모든 수주 D-Day 충족 검증. 수주 납기 vs schedule production_date (성형) vs ex_production_date (압출). 5% 미달 시 Phase 2 fail. NS-S05~07 KPI 종합 측정.

---

## :link: References
- **상위 Story**: [`_Story_Overview.md`](_Story_Overview.md)
- **WBS**: §8 EP-E2E ST-E2E-1 (Task 3)
- **EXP**: EXP-5
- **연관**: 선행 [TK-E2E-1-2](TK-E2E-1-2.md)

---

## :hammer_and_wrench: Implementation Plan

```
backend/scheduling-engine/
  src/test/java/com/scheduling/test/e2e/
    DDayComplianceVerifier.java
    E2EKpiReport.java                            [결과 종합]
```

### `DDayComplianceVerifier.java`

```java
@Component @RequiredArgsConstructor
public class DDayComplianceVerifier {

    private final OrderRepository orderRepo;
    private final VcScheduleRepository vcRepo;
    private final ExScheduleCandidateRepository exRepo;
    private final WorkingCalendarService calendar;

    public ComplianceResult verifyAllOrders(LocalDate weekStart) {
        var orders = orderRepo.findByOrderDateBetween(weekStart, weekStart.plusDays(7));

        var perOrder = new ArrayList<OrderCompliance>();
        for (var order : orders) {
            var vcSchedule = vcRepo.findEarliestByHoseIdAndStatus(
                order.getHoseId(), VcScheduleStatus.CONFIRMED);
            var exCandidate = exRepo.findEarliestByHoseId(order.getHoseId());

            boolean vcMet = vcSchedule != null
                && !vcSchedule.getProductionDate().isAfter(
                    calendar.subtractWorkingDays(order.getDeliveryDate(), 2));
            boolean exMet = exCandidate != null
                && !exCandidate.getExtrusionDeadline().isAfter(
                    calendar.subtractWorkingDays(vcSchedule.getProductionDate(), 1));
            boolean dDay = vcMet && exMet;

            perOrder.add(new OrderCompliance(order.getId(), order.getHoseId(),
                order.getDeliveryDate(), vcMet, exMet, dDay));
        }

        long total = perOrder.size();
        long compliantCount = perOrder.stream().filter(OrderCompliance::dDay).count();
        double rate = (double) compliantCount / total;

        return new ComplianceResult(total, compliantCount, rate, perOrder);
    }
}

public record OrderCompliance(
    UUID orderId, String hoseId, LocalDate deliveryDate,
    boolean vcMet, boolean exMet, boolean dDay
) {}

public record ComplianceResult(
    long totalOrders, long compliantOrders, double complianceRate,
    List<OrderCompliance> details
) {}
```

### `E2EDDayIT.java`

```java
@SpringBootTest @Testcontainers
class E2EDDayIT {

    @Autowired E2EDataSimulator simulator;
    @Autowired DDayComplianceVerifier verifier;
    // ... + 모든 cascade dependencies

    @Test
    void all_orders_dday_compliance_above_95_percent() {
        // 1주 cascade 실행 (TK-E2E-1-2 패턴)
        simulator.generateWeek(LocalDate.of(2026, 3, 2), 12345L);
        runFullCascade();

        // D-Day 검증
        var result = verifier.verifyAllOrders(LocalDate.of(2026, 3, 2));

        System.out.printf("D-Day compliance: %d/%d = %.1f%%%n",
            result.compliantOrders(), result.totalOrders(), result.complianceRate() * 100);

        // 5% 미달 허용 (현실적 — 일부 충돌 케이스)
        assertThat(result.complianceRate())
            .as("Phase 2 DoD: D-Day 충족 ≥ 95%")
            .isGreaterThanOrEqualTo(0.95);

        // 미달 케이스 분석
        var nonCompliant = result.details().stream()
            .filter(c -> !c.dDay()).toList();
        for (var nc : nonCompliant) {
            System.out.println("D-Day 미달: " + nc);
        }
    }

    @Test
    void ns_s05_through_s07_kpis_pass() {
        simulator.generateWeek(LocalDate.of(2026, 3, 2), 12345L);
        runFullCascade();

        // NS-S05: 납기 D-Day 준수율
        // NS-S06: D-2 준수율 (성형)
        // NS-S07: D-1 준수율 (압출)
        assertThat(kpiService.computeNS_S05()).isGreaterThanOrEqualTo(0.95);
        assertThat(kpiService.computeNS_S06()).isGreaterThanOrEqualTo(0.98);
        assertThat(kpiService.computeNS_S07()).isGreaterThanOrEqualTo(0.98);
    }
}
```

---

## :test_tube: Acceptance Criteria

**검증**: T-E2E + A

- [ ] **D-Day 준수율 ≥ 95%** (TC-E2E-1)
- [ ] **NS-S05 ≥ 95%** (납기 D-Day)
- [ ] **NS-S06 ≥ 98%** (D-2 성형)
- [ ] **NS-S07 ≥ 98%** (D-1 압출)
- [ ] **미달 케이스 보고** — 운영팀 검토 입력
- [ ] CI Jenkins stage 통합 — Phase 2 DoD 최종 게이트
- [ ] 통합 + E2E 테스트 ≥ 2 케이스

---

## :checkered_flag: Definition of Done

- [ ] 위 측정 기준 통과
- [ ] Verifier + IT + KPI report
- [ ] CI 통합 + Phase 2 종합 게이트
- [ ] **Sprint Review 데모**: D-Day compliance dashboard 시연
- [ ] 코드 리뷰 1명 이상 승인

---

## :construction: Dependencies

- **선행**: [TK-E2E-1-1](TK-E2E-1-1.md), [TK-E2E-1-2](TK-E2E-1-2.md), 모든 Sprint 1~5
- **후행**: ST-E2E-2 (베타 시작 전 검증)
- **Critical Path**: ⭐⭐⭐ (Phase 2 → Phase 3 진입 게이트)

---

## :memo: Implementation Notes

- 95% 임계값 — 5% 미달은 충돌 conflict (외주·납기 협상 등) 정상 — 운영 가능
- 100% 시 의심 (현실적 부담 없음 = 시뮬레이션 단순) — 운영 인터뷰로 검증
