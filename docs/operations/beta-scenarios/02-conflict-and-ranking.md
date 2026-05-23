# 베타 시나리오 2 — 충돌 + 다중 후보 ranking

**시나리오 ID**: BS-02 | **페르소나**: Planner + STK_USER | **소요 시간**: ~20분
**관련 BR**: BR-E04·E10 (검증 게이트) + REQ-FUNC-XT-001 (≥3 distinct)

> 마스터 충돌 입력 → ExtrusionValidationGate 가 fail 판정 → ExConflictCategorizer 가
> 6 카테고리 분류 → ExAlternativeGenerator 가 ≥3 distinct alternative 제시 → Planner 가
> Ranking 점수 (slack/balance/setting) 기반 1클릭 선택.

---

## 1. 사전 조건

- [x] 시나리오 1 (정상 1주 horizon) 완료
- [x] EX schedule_candidate ~6,300 row 존재
- [x] master.shift + master.ex_constraint 정상 시드

---

## 2. 충돌 의도적 발생 (테스트용)

### 2.1 ex_constraint 임시 수정 — IT_OPS 권한

```sql
-- shift D1 의 effective_min 을 일부러 낮춰 capacity 부족 유발
UPDATE master.shift SET nominal_min = 60, efficiency = 0.5
WHERE shift_code = 'D1';
-- effective_min = 60 × 0.5 = 30분 (정상 180 → 30, 1500 row 처리 불가)
```

### 2.2 ExtrusionValidationGate 자동 fail

- 백엔드 자동 — VC 확정 cascade 후 gate.validateBatch 가 SHIFT_CAPACITY_EXCEEDED 분류
- `app.ex_schedule_candidate.status = 'FAILED'` 영속

---

## 3. UI 충돌 확인 + Ranking 활용

```
1. /extrusion-matrix 진입 → 매트릭스 grid
2. status 컬럼 FAILED row (빨간색) 표시 확인 (~ 일부 row)
3. Tabs [다중 후보 ranking] 클릭
4. GET /api/v1/schedule/ex/candidates/ranking?from=&to=
   → ≥ 3 distinct ranked candidate 표시
5. 컬럼:
   - 순위 1 = gold, 2-3 = blue
   - 품번 (29673-2R060 yield=2531 ⭐ 강조)
   - deadline + yield
   - 기한 여유 score (slack) — Progress bar
   - 라인 균형 score (balance)
   - 셋팅 단일 score (setting)
   - total score = 0.4·slack + 0.3·balance + 0.3·setting
6. Planner 가 1순위 (총량 최고) 행 클릭 → 상세 모달 (Sprint 7+)
```

---

## 4. STK_USER swap 제안 (Sprint 5 EP-15 ST-15-2)

```
1. STK_USER 로그인 (별도 세션 권장 — 동시 검증)
2. /vc/simview 진입
3. 회전 격자에서 rotation A 우클릭 → "다른 회전으로 변경 제안"
4. target rotation 선택 (같은 machine·slot·date 안)
5. 사유 입력 (예: "전기 사용량 평준화")
6. POST /api/v1/schedule/vc/proposals → status=PROPOSED
7. Planner UI 의 SwapProposalPanel 에 즉시 표시 (1초 이내)
```

---

## 5. Planner 1클릭 수용 (총량 보존 invariant)

```
1. Planner 로그인 → /vc/simview → SwapProposalPanel
2. PROPOSED row [수용] 버튼 클릭
3. POST /api/v1/schedule/vc/proposals/{id}/accept
   - SwapHelper.swapRotation (SET CONSTRAINTS DEFERRED + atomic CASE WHEN)
   - 두 row 의 rotation_no 만 교체, plannedQty 보존 (총량 invariant)
4. 성공 메시지 — "swap atomic 완료 + audit 자동 발행"
5. 회전 격자 즉시 갱신
```

---

## 6. 기대 결과 + 검증

| 항목 | 기대 | 검증 |
|---|---|---|
| FAILED candidate 감지 | < 5초 | ExGateResult.passed=false |
| Ranking ≥ 3 distinct | 100% | REQ-FUNC-XT-001 |
| Ranking 점수 합산 | total = 0.4s+0.3b+0.3s | 단위 테스트 검증 |
| Swap 총량 보존 | plannedQty 합 변경 0 | DB invariant |
| Swap audit row | 100% | `audit.schedule_audit_log` reason="REQ-FUNC-VC-018" |
| UI 갱신 (Planner) | < 1초 | useSwapProposals invalidateQueries |

---

## 7. 시나리오 종료 — 원상복귀 (SQL)

```sql
-- 충돌 의도 입력 원복
UPDATE master.shift SET nominal_min = 240, efficiency = 0.75
WHERE shift_code = 'D1';
-- effective_min = 240 × 0.75 = 180분 (정상)
```

---

## 8. KPI 영향

- **K-V01** 슬롯 점유율 — swap 후 동일 (총량 보존)
- **K-E02** 압출 셋업 시간 — Ranking 선택 시 setting score 높은 후보 → 단축

---

## 9. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — 충돌 + ranking SOP |
