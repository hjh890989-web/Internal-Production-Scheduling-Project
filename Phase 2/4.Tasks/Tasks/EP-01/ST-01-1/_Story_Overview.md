# Story Overview — [EP-01] ST-01-1 엑셀 워크북 입력·검증 (3종 포맷)

**Sprint**: S1 (수주 통합 기반) | **Epic**: EP-01 엑셀 통합 Parser (M-01) | **Priority**: Must
**SP 합계**: 5 | **PD 추정**: ~3.5 PD (5 SP × 0.7 PD)

---

## Story 목적

> WBS §5.1 EP-01 인용: "M-01 엑셀 통합 Parser — Sprint 1 핵심, EXP-1 (4.2h → 30분) 검증 대상"
> SRS REQ-FUNC-OC-001 인용: "시스템은 웹 UI를 통해 최대 3종의 엑셀 워크북(`.xlsx`, ≤ 20 MB each) 동시 업로드를 수용해야 한다"

본 Story는 **3종 수주 엑셀(월별 예상·KD 발주·주간 발주)을 안전하게 수신·파싱하고 출처를 자동 분류하는 입력 계층**을 구축한다. M-01 전체 워크플로우(Import → 매핑 → 중복 감지 → Diff → 알림 → Commit)의 첫 관문이며, **사용자(P1·P4)의 가장 큰 Pain인 "4.2h 수작업 통합"의 시간 단축이 이 Story에서 시작**된다.

**Why 본 Story가 Sprint 1 핵심인가**:
- **EXP-1 검증의 직접 입력 단계** — Phase 0에서 측정한 4.2h 베이스라인의 80%(~3.4h)가 본 Story에서 자동화됨
- **후속 Story 차단**: ST-01-2(매핑) 이전 단계로, parse 결과의 `Map<String, Object>` 또는 row DTO를 ST-01-2에 공급
- **SAD ADR-008** (Java 21 + Spring Boot) + ADR-010 (PostgreSQL) 통합 첫 적용
- **NFR-PER-001** (10k row import p95 ≤ 60초) 충족 가능 여부의 1차 시험대
- **REQ-FUNC-OC-002**의 핵심 — 워크북 헤더 시그니처만으로 4종 소스(월별 예상·주간·확정·KD) 자동 분류 ≥99% 정확도

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-01-1-1](TK-01-1-1.md) | Apache POI XSSF 통합 + 스트리밍 reader 설정 (10k row 메모리 한도) | 1.0 | Backend | T-U + T-I + I | ☐ |
| [TK-01-1-2](TK-01-1-2.md) | 워크북 헤더 자동 분류기 (월별 예상·주간·확정·KD 4종) | 1.0 | Backend | T-U + A | ☐ |
| [TK-01-1-3](TK-01-1-3.md) | Multipart 업로드 엔드포인트 + 추적 ID 발급 (응답 ≤2초) | 0.5 | Backend | T-I + T-L | ☐ |
| [TK-01-1-4](TK-01-1-4.md) | 30개 회귀 워크북 단위 테스트 + 분류 정확도 ≥99% 검증 | 1.0 | Backend + QA | T-U + A | ☐ |

> **선행 의존**: ST-00-1 (Docker Compose — DEV 환경), ST-00-2 (Spring Boot 골격 — 모듈러 모놀리식)
> **후행 차단**: ST-01-2 (스키마 매핑은 본 Story의 parse 결과 필요), ST-01-3 (폴더 watcher는 본 Story의 import 호출)
> **병렬 가능**: ST-02-1 (중복 감지 — 본 Story와 독립)

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] **`POST /api/v1/orders/import` (multipart, 3 파일) 한 번에 추적 ID 반환** — REQ-FUNC-OC-001 (TC-OC-001)
- [ ] **30개 회귀 워크북에서 소스 자동 분류 정확도 ≥99%** — REQ-FUNC-OC-002 (TC-OC-002)
- [ ] **10,000 row 워크북 import p95 ≤ 60초** (Apache POI streaming) — REQ-NF-PER-001 (TC-PER-001)
- [ ] **20 MB 초과 워크북 거부** — HTTP 413 + 사유 메시지
- [ ] **파싱 실패 시 row-level 에러 리포트** (어떤 시트·어떤 row·어떤 컬럼)
- [ ] 단위 테스트 ≥80% 커버리지 (변경 라인)
- [ ] 통합 테스트 — Testcontainers + 실제 .xlsx 워크북 (DS-ORDER-3X 합성 30건)
- [ ] Sprint Review 데모: P1 페르소나 시각 — "월요일 09시, 3종 엑셀 업로드 → 추적 ID 반환 → 분류 결과 표시"

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.1 EP-01 ST-01-1
- **SAD**:
  - `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` §5.1 **백엔드 스택** (Java 21 + Spring Boot 3.3, Apache POI XSSF 5.x)
  - §5.6 **컨테이너·배포** (메모리 한도 8 GB · JVM Xmx 2 GB)
  - §9.1 **성능 실현** ("Apache POI streaming reader + batch insert + 인덱스 지연 활성")
  - §10 **ADR-008** (Java 21 LTS 채택) · **ADR-010** (PostgreSQL 16)
- **SRS REQ-FUNC**:
  - **REQ-FUNC-OC-001** ("3종 엑셀 ≤20MB 동시 업로드, 2초 이내 tracking ID")
  - **REQ-FUNC-OC-002** ("헤더 시그니처로 4종 분류 ≥99%")
- **SRS REQ-NF**:
  - **REQ-NF-PER-001** ("10,000 row 워크북 import p95 ≤60초")
  - **REQ-NF-PER-005** ("UI 페이지 응답 p95 ≤1초" — 업로드 후 즉시 응답)
  - **REQ-NF-SEC-005** ("민감 데이터 사내 한정" — 파일은 사내 볼륨 저장)
- **PDD-01**: `Phase 2/1.PDD/1.process_order_consolidation_Opus.md` §4 Activities A1·A2
- **TestPlan**: `Phase 2/5.TestPlan/TEST-001_Test_Plan_v1.0.md`
  - **TC-OC-001** (3종 워크북 2초 응답)
  - **TC-OC-002** (30개 회귀 ≥99% 분류)
  - **TC-PER-001** (10k row p95 ≤60초)
- **데이터셋**: `TEST-001 §7.1` DS-ORDER-3X (30 워크북), DS-LOAD-10K (부하 테스트)
- **SAD-RSK**:
  - SAD §11 **SAD-RSK-005** "Apache POI 메모리 사용 (대용량 엑셀)" — streaming reader 필수, 20MB 한도
- **연관 Story**:
  - 선행: [ST-00-1](../../EP-00/ST-00-1/_Story_Overview.md) (Docker Compose), [ST-00-2](../../EP-00/ST-00-2/_Story_Overview.md) (Spring Boot)
  - 후속: ST-01-2 (스키마 매핑 — Sprint 1), ST-01-3 (폴더 watcher — Could)

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | EP-01 ST-01-1 초안 작성 (Task 기반 분해 v1 패턴 재사용) |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.1 EP-01 ST-01-1 + SAD ADR-008·010·§5.1·§9.1 + SRS REQ-FUNC-OC-001·002 + TC-OC-001·002 + TC-PER-001 기반 |
