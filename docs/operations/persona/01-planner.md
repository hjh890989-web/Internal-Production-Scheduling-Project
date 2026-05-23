# Planner — 생산 계획 담당자 가이드

**Role**: PLANNER (Keycloak) | **권한 등급**: P1 (최고)
**책임**: 일정 작성·확정·override + 충돌 처리 + 베타 5 시나리오 운영

> Planner 는 시스템의 **핵심 운영자**. 모든 confirm + override + cascade chain 의
> 출발점. RBAC `ROLE_PLANNER` 단독 권한: confirm + accept/reject + override.

---

## 1. 접근 가능 화면

| 경로 | 화면 | 용도 |
|---|---|---|
| `/home` | Home | 진입점 + KPI 요약 |
| `/orders/import` | 수주 통합 | xlsx Excel import + 매핑 검토 |
| `/vc/simview` | 성형 시뮬뷰 | 회전 격자 (BR-V04 1~18) + SwapProposalPanel (수용) |
| `/extrusion-matrix` | 압출 매트릭스 | Tabs 매트릭스/Ranking + STOMP cascade + Excel 다운로드 |
| `/vc/capacity-queue` | **Capa 큐 + KD 보충** (Sprint 7 v1.1) | BR-V12 split 미리보기 + BR-V13 1클릭 보충 (DI-07/08 입력 후) |
| `/audit/restore` | 마스터 복원 (read-only) | audit timeline + 시점 snapshot (복원 자체는 IT_OPS 협의) |

---

## 2. 일일 운영 루틴 (Beta 1주 horizon)

### 2.1 월요일 09:00 — 1주 스케줄 작성

1. [수주 통합] xlsx Excel import → 매핑 검토 → 확정
2. [성형 시뮬뷰] 회전 격자 자동 생성 확인 (CANDIDATE)
3. 회전 격자 검토 — 모든 row 의 angle (BR-V07 일중 락) + 18 회전 분포 확인
4. 전체 row 선택 → [확정] (CANDIDATE → CONFIRMED, BR-X01)
5. [압출 매트릭스] EX cascade 자동 발생 확인 (STOMP badge connected)
6. Excel 다운로드 (BR-E09 시트명) — 현장 작업자 전달

### 2.2 매일 — 변경 대응

- 영업 변경 통보 → 해당 row override
- override 사유 입력 (BR-V07) → cascade chain 자동
- STK_USER swap 제안 도착 시 → SwapProposalPanel 1클릭 수용 (총량 보존)

### 2.3 매주 금요일 — KPI 점검

- Grafana business-kpi 대시 확인
- NS-S04 도달률 / NS-S09 신규 라인 / K-V04 위반 (목표 0) 등
- 임계값 미달 시 IT_OPS + 영업 통보

---

## 3. 핵심 액션 매트릭스

| 액션 | 화면 | API | 검증 |
|---|---|---|---|
| **Confirm** | /vc/simview | POST /schedule/vc/{id}/confirm | V022 trigger + audit auto |
| **Confirm Batch** | /vc/simview | POST /schedule/vc/confirm-batch | N row CANDIDATE → CONFIRMED |
| **Swap 수용** | /vc/simview SwapProposalPanel | POST /schedule/vc/proposals/{id}/accept | atomic rotation swap |
| **Override** | /vc/simview Override modal | POST /schedule/vc/{id}/override | reason + by 강제 + V027 |
| **EX Confirm** | /extrusion-matrix | POST /schedule/ex/{id}/confirm | V023 SCHEDULED → CONFIRMED |
| **Excel 다운로드** | /extrusion-matrix | GET /export/extrusion-matrix | BR-E09 시트명 |
| **Capa 큐 split (BR-V12)** | /vc/capacity-queue Tab1 | POST /schedule/vc/capacity-overflow/split | priority rank ASC + 추가 요청 큐 분리 |
| **KD 잔량 보충 (BR-V13)** | /vc/capacity-queue Tab2 | POST /schedule/vc/capacity-overflow/supplement | 동일 hose 1차 + 그룹 2차 + audit @Auditable |

---

## 4. FAQ

### Q1. CANDIDATE row 를 직접 DB 에서 CONFIRMED 로 바꿀 수 있나요?
**A**. 불가능. V022 DB trigger 가 application bypass 차단. 반드시 UI Confirm.

### Q2. BR-V07 일중 락 override 는 몇 번까지 가능한가요?
**A**. 제한 없음. 단 K-V04 KPI 가 일별 0 목표 — 5회/일 초과 시 Slack alert.

### Q3. Excel 다운로드 시트명 정규식이 안 맞으면?
**A**. EP-12 회귀 실패. IT_OPS 에 통보 (BR-E09 정규식 `\d+월\d+일\(압출\)`).

### Q4. swap 수용 후 총량이 달라졌어요?
**A**. 시스템 버그 — atomic SQL 충돌 가능성. IT_OPS 통보. SwapHelper SET CONSTRAINTS DEFERRED invariant 회귀.

### Q5. `/vc/capacity-queue` 진입 시 빈 결과만 나옵니다.
**A**. **활성 조건 미충족**. DI-07 PRODUCT_PRIORITY + DI-08 KD_ORDER 마스터 입력 후 정상 동작. IT_OPS 협의 후 시드 입력. ([BS-06](../beta-scenarios/06-capacity-overflow-kd-supplement.md) 참조)

### Q6. capa 초과 시 split 미리보기 결과 (Tab1) 의 "추가 요청 큐" 는 자동 채택되나요?
**A**. **아니오**. v1.1 시점 미리보기만 (Sprint 7 carry-over). Planner 가 별도 결정. 1클릭 승인 워크플로우는 Sprint 8+ 예정.

---

## 5. 비상 연락

| 상황 | 연락처 |
|---|---|
| 시스템 장애 | Slack `#scheduling-ops` |
| 마스터 복원 필요 | Slack `#scheduling-ops` + IT_OPS DM |
| 영업 변경 통보 | 영업 1팀 카카오 |

---

## 6. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Planner 페르소나 가이드 |
| 1.1 | 2026-05-23 | Claude Code | Sprint 7 carry-over 마감 반영 — `/vc/capacity-queue` 추가, 액션 매트릭스 V12·V13 2건, FAQ Q5·Q6 추가 |
