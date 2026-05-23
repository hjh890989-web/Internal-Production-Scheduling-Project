# 베타 시나리오 6 — Capa 초과 큐 + KD 잔량 보충 (BR-V12 · BR-V13)

**시나리오 ID**: BS-06 | **페르소나**: Planner + IT_OPS | **소요 시간**: ~25분
**Phase**: 4-B (Phase-4_EntryPlan_v1.1 §6 후보) | **빈도**: capa 초과/부족 발생 시 (운영 중 수시)
**상태**: 🔄 후보 (DI-07/08 활성 조건 충족 후 정식 시나리오)

> Sprint 7 carry-over 풀 스택 마감. capa 초과 시 priority rank ASC 분리 (BR-V12) +
> capa 부족 시 KD 잔량 1클릭 보충 (BR-V13). DI-07 PRODUCT_PRIORITY + DI-08 KD_ORDER
> 마스터 입력 후 정식 활성.

---

## 1. 사전 조건

- [x] STG 환경 정상 부팅 + Keycloak SSO 로그인 (role `PLANNER`)
- [x] 시나리오 BS-01 (정상 1주 horizon) 1회 이상 완료
- [ ] **🆕 DI-07 PRODUCT_PRIORITY 마스터 입력** (IT_OPS 협의)
- [ ] **🆕 DI-08 KD_ORDER 마스터 입력** (수주통합 시 자동 또는 수동)
- [x] Grafana 대시 `scheduling-overview` open (audit row 자동 발행 모니터)

### 1.1 DI-07 PRODUCT_PRIORITY 시드 (IT_OPS)

```sql
-- 우선순위 1 = VIP, 99 = fallback (미등록)
INSERT INTO master.product_priority
    (hose_id, priority_rank, rationale, effective_from, updated_by)
VALUES
    ('29673-2R060', 1, 'VIP 고객 X사', CURRENT_DATE, 'it_ops_사번'),
    ('28422-2M800', 2, '긴급 수주',    CURRENT_DATE, 'it_ops_사번'),
    ('28421-2M800', 3, '일반',         CURRENT_DATE, 'it_ops_사번');
```

### 1.2 DI-08 KD_ORDER 시드 (IT_OPS 또는 수주통합 자동)

```sql
-- composite + remaining_qty CHECK ≤ order_qty + 4-status 머신
INSERT INTO master.kd_order
    (kd_order_id, hose_id, order_qty, remaining_qty, order_date, customer_code,
     status, updated_at, updated_by)
VALUES
    (gen_random_uuid(), '29673-2R060', 100, 100, '2026-06-01', 'CUST-X',
     'OPEN', now(), 'system');
```

---

## 2. 단계별 절차

### 2.1 진입 — `/vc/capacity-queue`

```text
1. 좌측 메뉴 [Capa 큐 + KD 보충] 클릭
2. 화면 상단 Title — "Capa 초과 큐 + KD 보충 (BR-V12 · BR-V13)"
3. Tabs — [BR-V12 우선순위 Split] / [BR-V13 KD 잔량 보충]
```

### 2.2 Tab 1 — BR-V12 우선순위 Split 미리보기

```text
1. Daily capacity 입력 (BR-V05 기본 LP 72 + IC 18 = 90)
2. Hose 별 요구량 입력 (기본 3 row 예시):
   - 29673-2R060 : 60  (rank 1)
   - 28422-2M800 : 50  (rank 2)
   - 28421-2M800 : 40  (rank 3 또는 99 fallback)
3. [Split 미리보기] 클릭 → POST /api/v1/schedule/vc/capacity-overflow/split
4. 결과 카드 확인:
   - (a) 자동 채택 — priority rank ASC 정렬 후 daily_capa 까지 채택
   - (b) 추가 요청 큐 — 잔여분 (Planner 승인 대기 — Sprint 8+ confirm 워크플로우 별도)
   - capa 사용률 Progress bar — 100% 도달 시 exception 표시
```

### 2.3 Tab 2 — BR-V13 KD 잔량 보충

```text
1. Hose ID 입력 (예: 29673-2R060)
2. 부족량 입력 (예: 80 회전)
3. [KD 잔량 보충] 클릭 → POST /api/v1/schedule/vc/capacity-overflow/supplement
4. 결과 카드 확인:
   - 요청 shortage / 실 보충 / 부족 잔여 / 소진 KD orders 4 Statistic
   - 소진 KD orders 테이블:
     - "동일 hose" (green tag) — 1차 우선순위
     - "그룹 (XXX)" (blue tag) — 동일 셋팅 그룹 hose 2차 fallback
   - audit 자동 — @Auditable AOP + V025 trigger
```

---

## 3. 기대 결과 + 검증

| 항목 | 기대 | 검증 |
|---|---|---|
| Tab 1 자동 채택 총량 | ≤ daily_capa | UI Progress bar ≤ 100% |
| Tab 1 추가 요청 큐 | priority rank 큰 순 | "추가 요청 큐" 카드 표시 |
| Tab 2 동일 hose 1차 | KD remaining_qty 부터 차감 | UI "동일 hose" green tag |
| Tab 2 그룹 fallback 2차 | 동일 셋팅 그룹 hose 의 KD 사용 | UI "그룹 (...)" blue tag |
| KD status 자동 전이 | OPEN → PARTIAL → FILLED | `SELECT status FROM master.kd_order WHERE kd_order_id=...` |
| audit row 발행 (BR-V13) | 100% (@Auditable) | `SELECT count(*) FROM audit.schedule_audit_log WHERE table_name='kd_order'` |
| KD remaining_qty CHECK | ≤ order_qty 보존 | DB CHECK constraint |
| RBAC | PLANNER 200 / STK_USER 403 / 미인증 401 | CapacityOverflowControllerIT 5 테스트 |

---

## 4. 실패 시 대처

| 증상 | 원인 | 대처 |
|---|---|---|
| Tab 1 빈 응답 (accepted/queue 모두 empty) | required map 0 row 또는 dailyCapa 0 | 입력값 재확인 |
| Tab 2 supplemented = 0 | DI-08 KD_ORDER 미입력 또는 hose 잘못 | IT_OPS 협의 (master.kd_order seed 확인) |
| Tab 1 모든 rank 99 fallback | DI-07 PRODUCT_PRIORITY 효력 미발효 (effective_from 미래) | effective_from = CURRENT_DATE 또는 과거 |
| 403 Forbidden | 비-PLANNER 시도 | Keycloak realm role `PLANNER` 확인 |
| Tab 2 그룹 fallback 미발생 | DS-VC-CONSTRAINT-47 setting_group 미시드 | IT_OPS 협의 (master_seed.sql 재실행) |

---

## 5. KPI 영향

본 시나리오 정식 활성 후 Grafana business-kpi 대시 추가 panel (Sprint 8+ 예정):
- **KD remaining_qty per hose** (IT_OPS Grafana) — 잔량 부족 사전 경고
- **BR-V12 추가 요청 큐 누적** — Planner 승인 워크플로우 진입 빈도
- **BR-V13 그룹 fallback 비율** — 동일 hose 부족 빈도 (마스터 보충 필요 신호)

---

## 6. 관련 자료

- [Sprint-7_Completion_v1.1.md](../../../Phase%203/1.Sprint-Reports/Sprint-7_Completion_v1.1.md) — Sprint 7 carry-over 풀 스택 마감
- [Phase-4_EntryPlan_v1.1.md](../../../Phase%204/Phase-4_EntryPlan_v1.1.md) §6 — BS-06 후보 명세
- [Planner 페르소나 v1.1](../persona/01-planner.md) — `/vc/capacity-queue` 진입점
- Backend — `vc.capacity_overflow.CapacityOverflowController` + `CapacityOverflowQueueService` + `KdSupplementService`
- Frontend — `features/capacity-overflow/` (api + 2 panel) + `pages/CapacityQueuePage.tsx`
- IT — `CapacityOverflowControllerIT` (5 tests / 5 PASSED, RBAC + happy path) + `BrV12V13IT` (5 tests / 5 PASSED, Service chain)

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Sprint 7 carry-over 풀 스택 마감 반영, DI-07/08 활성 조건 명시 + BS-06 후보 |
