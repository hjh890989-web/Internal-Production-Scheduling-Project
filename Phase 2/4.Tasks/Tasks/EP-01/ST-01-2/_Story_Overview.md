# Story Overview — [EP-01] ST-01-2 스키마 매핑 + 사용자 보정

**Sprint**: S1 (수주 통합 기반) | **Epic**: EP-01 엑셀 통합 Parser (M-01) | **Priority**: Must
**SP 합계**: 5 | **PD 추정**: ~3.5 PD (5 SP × 0.7 PD)

---

## Story 목적

> WBS §5.1 EP-01 인용: "ST-01-2 — 자동 매핑 엔진(≥95% 성공), 매핑 보정 UI (Ant Design Form), 라운드트립 세션 보존, 통합 테스트"
> SRS REQ-FUNC-OC-003 인용: "시스템은 파싱된 row를 구성 가능한 매핑 룰을 통해 표준 수주 스키마(`order_id`, `hose_id`, `delivery_date`, `qty`, `order_type`, `customer`)로 변환해야 한다. 자동 매핑 성공률 ≥95% on regression set."
> SRS REQ-FUNC-OC-004 인용: "매핑 실패율이 1% 이상이면 시스템은 매핑 룰 보정 후 파싱 데이터를 잃지 않고 재시도할 수 있는 검토 모달을 제시해야 한다."

본 Story는 **ST-01-1의 출력(ParsedWorkbook + SourceType)을 표준 Order 스키마로 변환**하는 매핑 계층을 구축한다. 영업·관리 부서의 워크북마다 컬럼 명칭·순서가 미세하게 다른 현실(SRS-RSK-007)을 **외부 YAML 룰셋 + 사용자 보정 UI**의 결합으로 흡수한다.

**Why 본 Story가 Sprint 1 핵심인가**:
- **REQ-FUNC-OC-003 자동 매핑 ≥95%** — Sprint 1 DoD 의 가시 지표
- **REQ-FUNC-OC-004 보정 워크플로우** — 자동 매핑 실패 시 사용자가 룰을 즉시 수정하고 재시도. **파싱 데이터를 잃지 않는 라운드트립**(Redis 세션)이 핵심
- **NFR-USA-002 설명적 피드백** — 매핑 실패 row마다 "어떤 컬럼이 어떤 필드에 매핑 못 됐는지" 명시
- **K-O03 (REQ-NF-KPI-014) 자동 매핑 성공률 ≥95% 정식 KPI** — 본 Story가 직접 충족
- 후속 ST-02-1(중복 감지)과 ST-03-1(Diff)는 본 Story의 표준 스키마 출력에 의존

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-01-2-1](TK-01-2-1.md) | 자동 매핑 엔진 (SourceType별 YAML 룰셋, ≥95% 성공) | 1.5 | Backend | T-U + A + I | ☐ |
| [TK-01-2-2](TK-01-2-2.md) | 매핑 보정 UI (Ant Design Form + 실패 row 인라인 편집) | 1.0 | Frontend | T-UAT + T-I | ☐ |
| [TK-01-2-3](TK-01-2-3.md) | 라운드트립 세션 보존 (Redis 캐시 + 재매핑 endpoint) | 0.5 | Backend | T-I + T-L | ☐ |
| [TK-01-2-4](TK-01-2-4.md) | 매핑 정확도 통합 테스트 + REQ-FUNC-OC-003·004 회귀 | 0.5 | Backend + QA | T-I + A | ☐ |

> **선행 의존**: ST-01-1 (ParsedWorkbook + SourceType 입력)
> **후행 차단**: ST-02-1 (중복 감지 — 표준 Order schema 필요), ST-03-1 (Diff — 동일)
> **병렬 가능**: ST-01-3 (폴더 watcher — 본 Story와 독립)

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] **자동 매핑 성공률 ≥ 95%** on 30 회귀 워크북 (DS-ORDER-3X) — REQ-FUNC-OC-003 / TC-OC-003
- [ ] **매핑 실패 1% 이상 시 검토 모달** 자동 노출 + 사용자 룰 수정 + 재시도 시 **파싱 데이터 100% 보존** — REQ-FUNC-OC-004 / TC-OC-004
- [ ] **표준 Order 스키마 6 필드** (order_id, hose_id, delivery_date, qty, order_type, customer) 완전 매핑
- [ ] **K-O03 KPI** (REQ-NF-KPI-014) Grafana 패널에 자동 매핑 성공률 추세 노출
- [ ] 단위 테스트 ≥ 80% 커버리지 (변경 라인)
- [ ] 통합 테스트 — 4종 SourceType × 7~8 워크북 × 매핑 실행 매트릭스
- [ ] Sprint Review 데모: P1 페르소나 시각 — "30개 워크북 중 1개에서 신규 컬럼 발견 → 보정 모달 → 룰 추가 → 재시도 PASS"

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.1 EP-01 ST-01-2
- **SAD**:
  - `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` §4 컴포넌트 — `ParserMod`·`RuleMod` 협업
  - §5.1 Backend (Spring Modulith — order 모듈 내부 매핑 서비스)
  - §5.2 Frontend (Ant Design 5 Form, TanStack Query)
  - §10 **ADR-008** (Java/Spring) · **ADR-009** (React/TS/Ant Design)
- **SRS REQ-FUNC**:
  - **REQ-FUNC-OC-003** (자동 매핑 ≥95%)
  - **REQ-FUNC-OC-004** (보정 워크플로우 — 세션 보존)
- **SRS REQ-NF**:
  - **REQ-NF-USA-002** (설명적 피드백 — 위반 사유 + 대안)
  - **REQ-NF-USA-003** (한국어 UI — 보정 모달 한국어 100%)
  - **REQ-NF-KPI-014** (K-O03 자동 매핑 성공률 ≥95%)
- **PDD-01**: `Phase 2/1.PDD/1.process_order_consolidation_Opus.md` §4 Activity A2 "Map to Master Schema" (T2.1·T2.2·T2.3)
- **TestPlan**: `Phase 2/5.TestPlan/TEST-001_Test_Plan_v1.0.md`
  - **TC-OC-003** (자동 매핑 ≥95%)
  - **TC-OC-004** (매핑 보정 라운드트립 세션 보존)
- **SRS-RSK**:
  - **SRS-RSK-007** "3종 엑셀 포맷 분화" — 본 Story가 YAML 룰셋 외부화로 직접 완화
  - **SRS-RSK-008** "신규 컬럼 매핑 실패 누적" — 본 Story의 5% 임계 자동 에스컬레이션
- **연관 Story**:
  - 선행: [ST-01-1](../ST-01-1/_Story_Overview.md)
  - 후속: ST-02-1 (중복 감지), ST-03-1 (Diff)

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | EP-01 ST-01-2 초안 (ST-01-1 패턴 이어받음) |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.1 EP-01 ST-01-2 + SAD §5.1·§5.2 ADR-008·009 + REQ-FUNC-OC-003·004 + REQ-NF-KPI-014 + TC-OC-003·004 기반 |
