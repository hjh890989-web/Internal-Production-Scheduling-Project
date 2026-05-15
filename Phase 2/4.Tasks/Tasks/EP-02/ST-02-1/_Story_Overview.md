# Story Overview — [EP-02] ST-02-1 (품번+납기) 중복 검출

**Sprint**: S1 (수주 통합 기반) | **Epic**: EP-02 중복 감지 (M-02) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.1 EP-02 인용: "ST-02-1 — TK-02-1-1 UNIQUE 제약 + violation 핸들, TK-02-1-2 100사이클 회귀 (중복 0), TK-02-1-3 ORM 레벨 검증"
> SRS REQ-FUNC-OC-005 인용: "시스템은 표준 스키마 매핑 후 `(hose_id, delivery_date)` 복합키로 중복을 감지해야 한다. 100회 회귀 사이클에서 커밋된 마스터에 중복 0건."

본 Story는 ST-01-2의 매핑 결과(OrderDraft)에서 **(품번, 납기) 복합키 중복을 DB 제약 + ORM 검증 + 룰 엔진 3단 방어**로 차단한다. INT-1(김정훈 주임) 사건: *"지난달 KD 발주 변경을 못 봐서 300개 못 맞췄어요"* — 중복 row가 마스터에 들어가면 다운스트림(스케줄링) 전체 신뢰가 무너짐. **Sprint 1 DoD 가시 지표**.

**왜 본 Story가 Sprint 1 핵심인가**:
- **REQ-FUNC-OC-005 100사이클 중복 0** — Sprint 1 DoD 강제 항목
- **데이터 신뢰성의 마지막 방어선** — ST-01-1·2가 통과해도 중복이 들어오면 의미 없음
- **ORM 레벨 검증**으로 DB-only 의존 회피 — 친절한 에러 메시지 + 추적성
- **K-O03 KPI** 매핑 성공률과 함께 중복 0 KPI도 자동 측정

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-02-1-1](TK-02-1-1.md) | PostgreSQL UNIQUE 제약 + ConstraintViolationException 처리 | 0.8 | Backend + DBA | T-U + T-I | ☐ |
| [TK-02-1-2](TK-02-1-2.md) | 100사이클 회귀 (중복 0건) + KPI 메트릭 | 0.8 | QA + Backend | T-I + A | ☐ |
| [TK-02-1-3](TK-02-1-3.md) | ORM 레벨 사전 검증 (DB hit 전 차단) | 0.5 | Backend | T-U + I | ☐ |

> **선행 의존**: ST-01-1·1-2 (OrderDraft 출력), ST-00-1 (PostgreSQL)
> **후행 차단**: ST-02-2 (우선순위 해소 — 본 Story의 중복 감지 결과 사용), ST-03-1 (Diff)
> **병렬 가능**: ST-01-3, ST-03-2

---

## Story 레벨 DoD

- [ ] **DB UNIQUE 제약** `(hose_id, delivery_date, master_version)` 활성 (SRS §6.2.4 정합)
- [ ] **ConstraintViolationException** 한국어 메시지 + 위반 row 정보 반환
- [ ] **100사이클 회귀** 중복 0건 — TC-OC-005
- [ ] **ORM 사전 검증** — DB hit 전 사전 차단 (성능 + UX)
- [ ] **K-O03 부속 KPI** `order.duplicate.detected.total` Prometheus 노출
- [ ] 단위 + 통합 테스트 ≥ 80% 커버리지

---

## References (공통)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.1 EP-02 ST-02-1
- **SAD**: §6.1.2 `(hose_id, delivery_date, master_version)` UNIQUE 인덱스
- **SRS REQ-FUNC**: REQ-FUNC-OC-005
- **SRS REQ-NF**: REQ-NF-REL-002 (ACID), REQ-NF-USA-002 (설명적 피드백)
- **PDD-01**: §4 A3 T3.1 "duplicates by composite key"
- **BR**: BR-X02 (audit 강제)
- **TestPlan**: TC-OC-005 (100사이클 중복 0)
- **연관**: 선행 [ST-01-2](../../EP-01/ST-01-2/_Story_Overview.md), 후속 [ST-02-2](../ST-02-2/_Story_Overview.md)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.1 EP-02 ST-02-1 + SAD §6.1.2 + REQ-FUNC-OC-005 + TC-OC-005 기반 |
