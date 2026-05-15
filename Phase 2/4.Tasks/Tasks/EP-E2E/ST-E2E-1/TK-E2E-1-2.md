---
name: QA Task (Cascade Scenario)
about: 단일 작업 단위 — 0.7 PD
title: "[TK-E2E-1-2] 수주 → 성형 → 압출 cascade 시나리오"
labels: 'sprint:S5, epic:EP-E2E, story:ST-E2E-1, type:test, priority:must, owner:qa'
assignees: ''
---

## :dart: Task Summary
- **Task ID**: TK-E2E-1-2
- **소속**: EP-E2E / ST-E2E-1 / Sprint S5
- **우선순위**: Must / **추정**: 0.7 PD (~5.6h) / **Owner**: QA
- **작업 요약**: E2E cascade — Order → VC Allocator → confirm → EX cascade (vc.confirmed → vc.changed 처리) → 시뮬뷰·매트릭스 확인. Playwright + JUnit chain. 1주 분량 수주 시뮬레이션 후 모든 단계 PASS.

---

## :link: References
- **상위 Story**: [`_Story_Overview.md`](_Story_Overview.md)
- **WBS**: §8 EP-E2E ST-E2E-1 (Task 2)
- **연관**: 선행 [TK-E2E-1-1](TK-E2E-1-1.md), 후행 [TK-E2E-1-3](TK-E2E-1-3.md)

---

## :hammer_and_wrench: Implementation Plan

```
backend/scheduling-engine/
  src/test/java/com/scheduling/test/e2e/
    E2ECascadeIT.java                            [Full chain]
frontend/tests/e2e/
  e2e-full-cascade.spec.ts                       [Playwright UI chain]
```

### `E2ECascadeIT.java`

```java
@SpringBootTest @Testcontainers
class E2ECascadeIT {

    @Autowired E2EDataSimulator simulator;
    @Autowired MasterImportService importer;
    @Autowired GreedyRotationAllocator vcAllocator;
    @Autowired VcScheduleConfirmService vcConfirm;
    @Autowired SettingGroupAllocator exAllocator;
    @Autowired VcScheduleRepository vcRepo;
    @Autowired ExScheduleCandidateRepository exRepo;

    @Test
    void full_cascade_one_week_passes_all_phases() {
        // Phase 1: 데이터 시뮬레이션
        var sim = simulator.generateWeek(LocalDate.of(2026, 3, 2), 12345L);
        assertThat(sim.orderCount()).isBetween(200, 300);

        // Phase 2: VC Allocator → Candidate 생성
        var ctx = AllocationContext.fromOrdersInRange(LocalDate.of(2026, 3, 2), 7);
        var vcResult = vcAllocator.allocate(ctx);
        assertThat(vcResult.schedule()).isNotEmpty();
        assertThat(vcResult.conflicts())
            .as("Cascade 1주 — 충돌 ≤ 5%")
            .hasSizeLessThan((int) (vcResult.schedule().size() * 0.05));

        // Phase 3: Confirm (Planner role 시뮬)
        var firstScheduleId = vcResult.schedule().get(0).scheduleId();
        withPlannerRole("planner1", () -> vcConfirm.confirm(firstScheduleId));

        // Phase 4: EX cascade — vc.confirmed event 도달 후 대기
        awaitListenerCompletion(5_000);

        var exCandidates = exRepo.findByScheduleId(firstScheduleId);
        assertThat(exCandidates).as("vc.confirmed → ex 자동 생성").isNotEmpty();

        // Phase 5: EX Allocator
        var exCtx = ExAllocationContext.fromCandidates(exCandidates);
        var exResult = exAllocator.allocate(exCtx);
        assertThat(exResult.schedule()).isNotEmpty();

        // Phase 6: 모든 BR 회귀 PASS
        verifyAllBRsPassed(vcResult, exResult);
    }

    private void verifyAllBRsPassed(AllocationResult vc, ExAllocationResult ex) {
        // BR-V07 일중 락 — 모든 (machine, slot, date) 슬롯 단일 hose
        var slotKeys = vc.schedule().stream()
            .collect(Collectors.groupingBy(
                r -> Tuple.of(r.machineId(), r.slotNumber(), r.productionDate()),
                Collectors.mapping(r -> r.hoseId(), Collectors.toSet())));
        assertThat(slotKeys.values()).allMatch(s -> s.size() == 1);

        // BR-V13 slot O/X — 부적합 슬롯 0건 (RuleEngine 통과)
        // BR-X07 D-2 — 모든 deadline ≥ production_date
        // ... 추가 BR 검증
    }
}
```

### `e2e-full-cascade.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test('1주 cascade — UI 양 끝 확인', async ({ page, request }) => {
  // 1) Trigger 시뮬레이션
  await request.post('/api/v1/test/simulate-week', {
    data: { startDate: '2026-03-02', seed: 12345 },
    headers: { Authorization: `Bearer ${process.env.ADMIN_TOKEN}` },
  });

  // 2) VC 시뮬뷰 진입 → 매트릭스 row 등장
  await page.goto('/simview');
  await expect(page.locator('.ag-row').first()).toBeVisible({ timeout: 15_000 });

  // 3) 후보 비교 페이지 → 1 후보 확정
  await page.goto('/candidate-comparison');
  await page.getByText('이 후보로 확정').first().click();

  // 4) 압출 매트릭스 → 자동 cascade 후 row 등장
  await page.goto('/extrusion-matrix');
  await expect(page.locator('.ag-row').first()).toBeVisible({ timeout: 30_000 });

  // 5) Excel 다운로드 — 파일 정상 생성
  const downloadPromise = page.waitForEvent('download');
  await page.getByText('엑셀 다운로드').click();
  const download = await downloadPromise;
  expect(await download.path()).toBeTruthy();
});
```

---

## :test_tube: Acceptance Criteria

**검증**: T-E2E

- [ ] **Phase 1~6 cascade** — 모두 PASS (E2ECascadeIT)
- [ ] **충돌 ≤ 5%** (1주 분량)
- [ ] **vc.confirmed → ex 자동 cascade** 검증
- [ ] **모든 BR PASS** (V07·V13·X07·E01·E05·E06 etc.)
- [ ] **UI cascade**: 시뮬뷰 → 확정 → 매트릭스 → Excel 다운로드 (Playwright)
- [ ] CI Jenkins stage 통합 — Sprint 5 DoD 핵심
- [ ] 통합 + E2E 테스트 ≥ 3 케이스

---

## :checkered_flag: Definition of Done

- [ ] 위 측정 기준 통과
- [ ] E2ECascadeIT + Playwright spec
- [ ] CI 통합 + Sprint 5 DoD 회귀 게이트
- [ ] **Sprint Review 데모**: 1주 cascade 라이브 (15분 데모)
- [ ] 코드 리뷰 1명 이상 승인

---

## :construction: Dependencies

- **선행**: [TK-E2E-1-1](TK-E2E-1-1.md), 모든 Sprint 1~5 핵심 task
- **후행**: [TK-E2E-1-3](TK-E2E-1-3.md)
- **Critical Path**: ⭐⭐⭐ (Phase 2 전체 검증)

---

## :memo: Implementation Notes

- E2E test 환경 — Testcontainers (PostgreSQL + Redis + Keycloak) 합산 1~2분 booting
- Playwright multi-context — Planner·STK 동시 로그인
- CI nightly 실행 (full E2E 비용) + PR 시점은 단축 버전 (smoke)
