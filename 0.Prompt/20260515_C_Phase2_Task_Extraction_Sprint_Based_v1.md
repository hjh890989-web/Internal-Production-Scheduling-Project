# Prompt: Phase 2 Task 상세 추출 (Sprint별 분해 v1)

> 본 프로젝트(사내 공정 스케줄링 시스템)의 Phase 2/4.Tasks 단계 — WBS v1.2의 ~290개 Task를 Sprint별로 GitHub Issue 형식으로 상세 추출하기 위한 프롬프트.
>
> **사용 기준**:
> - WBS v1.2 (`Phase 2/4.Tasks/TASK-001_WBS_v1.2.md`) 5개 사용자 기준 모두 충족된 상태
> - 분해 방식 = **Sprint별 (1번)** 채택 — 한 Sprint당 1 마크다운 파일
> - 단일 Claude 세션 환경 (멀티 에이전트 절차 미사용)
> - 사용자 정책: 영문 파일명 통일, ISO/IEC/IEEE 표준 준수, 상세 revision history 관리

---

## Role
당신은 **시니어 Software Engineer**입니다. 백엔드(Spring Boot Modulith)·프론트엔드(React+TS+Vite)·QA(JUnit·Playwright·k6)·DBA(PostgreSQL+Flyway)·DevOps(Docker Compose·Jenkins) 모두를 이해하는 풀스택 시각으로 작업하되, **각 Task의 owner role은 Task 본문 `assignees`/`labels:owner:` 필드를 참조하여 명확히 분리**합니다.

본 프로젝트 컨텍스트:
- **시스템**: 사내 공정 스케줄링 시스템 (Internal Production Scheduling System)
- **3 프로세스**: 수주 통합(PDD-01) / 성형 가류 스케줄링(PDD-02) / 압출 스케줄링(PDD-03)
- **4 페르소나**: P1 김정훈 주임 (Key Person) / P2 이수진 반장 / P3 박도영 반장 / P4 최민혁 대리
- **표준**: ISO/IEC/IEEE 12207 (Process) + 29148 (Requirements) + 42010 (Architecture) + BPMN 2.0
- **북극성 지표**: NS-01 P1·P4 만족도 ≥4/5

---

## Target Task

1. **전체 WBS 확인**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` 와 그 안의 §5(Epic·Story·Task 분해), §11(추적성 매트릭스), §12(Critical Path + 병렬 실행), §13(Risk 매핑), §14(SP 합계), §19(GitHub Import 가이드) 일독.

2. **분해 단위**: 한 번에 **1 Sprint 분량(~30~50 Task)을 1 마크다운 파일로 일괄 작성**.
   - 파일 경로: `Phase 2/4.Tasks/Sprints/<SprintID>.md`
   - 파일 단위: `Phase0.md` (= EP-99 마스터 정비 + EP-00 인프라 기반), `S1.md`·`S2.md`·`S3.md`·`S4.md`·`S5.md`, `Deferred.md` (EP-22·EP-23), `Cross-cutting.md` (EP-30~34 + EP-40~47 NFR Epic 횡단 분산분 일부) — **총 ~8 파일**.
   - **참고**: NFR Epic(EP-40~47)은 횡단 분산되므로 일부 Story는 해당 Sprint 파일에, 일부는 `Cross-cutting.md`에 등재 — Story별 1차 소속 Sprint 결정 후 작성.

3. **작성 순서 권장**: Phase 0 → S1 → S2 → S3 → S4 → S5 → Deferred → Cross-cutting.
   - **각 Sprint 작성 전 사용자 확정 받기** — 점진 검토·반복 가능하도록.

4. **이미 작성된 Sprint 파일이 있으면 건드리지 않음** (단일 세션이지만 점진 추출 패턴 유지).

---

## Instruction

### A. 파일 구조 (한 Sprint = 한 파일)

각 Sprint 파일은 다음 5 부분으로 구성:

```markdown
# Sprint <ID> Tasks — <Sprint Goal>
문서 ID: TASKS-<SprintID>
원천 WBS: Phase 2/4.Tasks/TASK-001_WBS_v1.2.md §<해당 섹션>
작성일: <YYYY-MM-DD>
개정: 1.0

## Sprint 메타
- 기간: 2주
- Sprint Goal (Demo Statement): <WBS §10에서 인용>
- SP 합계: <WBS §14.1 인용>
- Velocity 가정: <시나리오 C 권장: 55 SP/Sprint>
- DoD (Sprint 종료 시): <WBS §15.3에서 인용>
- 포함 Epic: EP-NN, EP-NN, ...

## Stories (GitHub Issue 단위)
### [EP-NN] ST-NN-M <Story 제목>
<아래 GitHub Issue Template 본문 그대로>

### [EP-NN] ST-NN-M <Story 제목>
...

## Sprint 종료 체크리스트
- [ ] 모든 Story DoD 충족
- [ ] Sprint Goal Demo 시연
- [ ] WBS §15.3 추가 종료 조건 충족
- [ ] 다음 Sprint DoR 준비 완료
```

### B. GitHub Issue Template (Strict Format — 각 Story 1개)

각 Story를 아래 템플릿에 따라 작성. **모든 필드 필수 작성**. 정보 부재 시 `<TBD: 사용자 확인 필요>` 표기.

```markdown
---
name: <Feature | NFR | Risk-Mitigation | Could | Infra | Cross-Cutting> Task
about: SRS/PDD/SAD 기반 구체적 개발 태스크 명세
title: "[EP-NN] ST-NN-M {Story 제목}"
labels: 'sprint:S<N>, epic:EP-NN, type:{feature|nfr|risk-mitigation|infra|could|cross-cutting}, priority:{must|should|could|deferred}, process:{order|vc|ex|cross-cutting}, owner:{backend|frontend|qa|dba|devops}'
assignees: ''
---

## :dart: Summary
- **기능명**: [EP-NN-ST-NN-M] {Story 제목}
- **소속 Epic**: EP-NN {Epic 제목}
- **Sprint**: S<N> ({Sprint Goal 요약})
- **우선순위**: Must | Should | Could | Deferred (Phase B 후 활성)
- **추정**: <SP> SP (~<PD> PD, owner: {백엔드|프론트|QA|DBA|DevOps} 기준)
- **목적**: <SRS REQ-FUNC 본문 또는 BR 요약 1~2문장>

## :link: References (Spec & Context)
> :bulb: **AI Agent & Dev Note**: 작업 시작 전 아래 문서를 반드시 먼저 Read/Evaluate 할 것.

- **SRS REQ-FUNC**: `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.4.md` §<섹션> **REQ-FUNC-<ID>** ("...AC 원문 1줄 인용...")
- **SRS NFR** (해당 시): **REQ-NF-<ID>** ("...명세 1줄 인용...")
- **PDD BR**: `Phase 2/1.PDD/<X>.process_*_Opus.md` §9 **BR-<ID>** ("...룰 1줄 인용...")
- **PDD Activity·Task**: §8 A<N>.<N> · T<N>.<N>
- **SAD ADR**: `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` §10 **ADR-<NNN>** ("...의사결정 요약...")
- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §<해당 EP 섹션> **ST-NN-M**
- **SRS-RSK 완화** (해당 시): SRS §1.7 **SRS-RSK-<NNN>** ("...리스크 진술 요약...")
- **ERD/스키마** (해당 시): SRS §6.2 또는 SAD §6.1 (`master.VC_CONSTRAINT` 등 테이블명 명시)
- **BPMN 다이어그램** (해당 시): PDD §7 (예: PDD-02 §7.2 To-Be `T5` Service Task)
- **마스터 데이터** (해당 시): `Phase 1/2.Raw Materials/...` 엑셀 컬럼 (예: K열 좌측셋팅, L열 우측셋팅, B열 규격)

## :white_check_mark: Task Breakdown (실행 계획)
> WBS의 Story별 "핵심 Task" 컬럼을 체크리스트로 풀어쓴 것. 각 Task는 1~3 PD, 8h 룰 준수.

- [ ] **TK-NN-M-1** {Task 1 제목 — 구체 동사형} (~<X> PD)
- [ ] **TK-NN-M-2** {Task 2 제목} (~<X> PD)
- [ ] **TK-NN-M-3** {Task 3 제목} (~<X> PD)
- [ ] **TK-NN-M-T** **테스트**: 단위 테스트 ≥80% 커버리지 + 회귀 시나리오 추가
- [ ] **TK-NN-M-D** **문서**: API spec(OpenAPI) 또는 운영 노트 갱신

## :test_tube: Acceptance Criteria

**1차 검증 방법** (ISO/IEC/IEEE 29148:2018 Annex C — SRS §4.1.6 카탈로그):
> **I** Inspection · **A** Analysis · **D** Demonstration · **T-U** Unit Test · **T-I** Integration Test · **T-L** Load Test · **T-S** Soak Test · **T-UAT** UAT

- **1차**: T-U | T-I | ...
- **2차** (선택): I | A | ...

### SRS AC 원문 인용
> {SRS REQ-FUNC-<ID>의 AC 컬럼 본문 — "100건 회귀 배치에서 슬롯 O/X 위반 0건" 등 측정 가능한 임계치 포함}

### Scenario (가능 시 GWT 변환, 측정 가능한 임계치 필수)

**Scenario 1: 정상 케이스**
- **Given**: {전제 조건 — 입력 데이터·시스템 상태}
- **When**: {수행 동작 — API 호출 / UI 액션 / 이벤트 발생}
- **Then**: {기대 결과 — HTTP 상태 코드 / 로그 / DB 상태 / 측정값 임계치}

**Scenario 2: 예외 / 경계 케이스**
- **Given**:
- **When**:
- **Then**:

**Scenario 3 (선택): override / 사용자 강제 케이스**
- **Given**: 사용자가 사유 입력 + Planner role
- **When**: ...
- **Then**: audit 사유 기록 + ...

## :gear: Technical & Non-Functional Constraints
> 본 Task에 적용되는 NFR — 해당 항목만 명시 (불필요 카테고리 제거).

- **성능** (NFR-PER-<ID>): p95 ≤<X>초 / 처리량 <N> TPS / 메모리 ≤<X> MB
- **신뢰성** (NFR-REL-<ID>): ACID / 가용성 ≥99.5% / RPO·RTO 임계치
- **보안** (NFR-SEC-<ID>): RBAC role · TLS · Audit / 민감 데이터 마스킹
- **사용성** (NFR-USA-<ID>): 한국어 UI / 1초 피드백 / 해상도 지원
- **운영·관측** (NFR-OPS-<ID>): 구조화 로그 / KPI 대시보드 / Slack 알림
- **호환성** (NFR-COM-<ID>): 30 동시 사용자 / 5년 데이터 / 브라우저 최신 2개
- **비용** (NFR-COS-<ID>): 잉여 서버 / OSS 우선 / ≤0.5 FTE
- **표준**: ISO/IEC/IEEE 12207·29148·42010 / BPMN 2.0 준수

## :checkered_flag: Definition of Done (DoD)
> 공통 DoD (WBS §15.2) + Sprint별 추가 (WBS §15.3) + 본 Task 특화.

### 공통
- [ ] 위 모든 Scenario AC 100% 통과
- [ ] 단위 테스트 ≥80% 라인 커버리지 (변경 라인 기준)
- [ ] 회귀 테스트 통과 (CI green, 이전 Sprint 산출물 포함)
- [ ] 코드 리뷰 1명 이상 승인 + ArchUnit 모듈 경계 위반 0건
- [ ] SonarQube quality gate 통과
- [ ] Audit·BR-X 룰 위반 0건 (해당 Task)
- [ ] API spec(OpenAPI) 갱신 (해당 시)
- [ ] 한국어 UI 텍스트 검수 (해당 시)
- [ ] 검증 방법 카탈로그 회귀 결과 보고 (1차·2차)
- [ ] Sprint Review 데모 시연 PASS

### Task 특화 (해당 시)
- [ ] {예: 마스터 K/L열 데이터 정합성 사전 검증}
- [ ] {예: 일중 앵글 교체 0건 회귀 (1주 호라이즌)}
- [ ] {예: `29673-2R060` 주간 전반 = 2,531개 BR-E05 검증}

## :construction: Dependencies & Blockers
- **선행 (Depends on)** (WBS §12 DAG 인용):
  - TK-NN-M-Z (같은 Epic 내 선행)
  - EP-NN (다른 Epic 선행)
- **후행 (Blocks)**:
  - EP-NN (본 Task가 막고 있는 후속 작업)
- **Critical Path 여부**: ✅ Critical Path 속함 / ⚪ Float <X> PD 여유 (WBS §12.1)
- **Phase B 종속** (해당 시): 수주통합 작업 완료 후 활성 (Deferred 표기)

## :shield: Risk Mitigation
- **완화하는 SRS-RSK** (해당 시): SRS §1.7 **SRS-RSK-<NNN>** "{리스크 진술 요약}" (SRS-RSK ↔ Task 매핑은 WBS §13.1)
- **잔여 위험·가정** (해당 시): {예: 마스터 K/L열 미정합 시 본 Task 차단 — Phase 0 EP-99에서 사전 점검}

## :memo: Implementation Notes (선택)
- **기술 힌트**: SAD ADR-<NNN> 패턴 적용 / 기존 코드 베이스 참고 위치
- **참고 표준**: ISO 12207 §<섹션> / BPMN 2.0 §<섹션>
- **유사 패턴**: 다른 EP·ST에서 동일 패턴 사용 (예: "EP-04와 유사한 RuleEngine 검증 구조 차용")
```

### C. 작성 시 엄격 준수 사항

1. **임의 내용 금지** (SRS·PDD·SAD·WBS 원천 인용만): SRS 외 수치·가정 추가 시 `<WBS §2.4 인용>` 또는 `<TBD: 사용자 확인>` 표기 — TASK-002 검토 보고서 REV-D-004 룰 준수.

2. **AC 텍스트 원문 인용**: SRS REQ-FUNC의 AC 컬럼을 1줄로 정확히 인용 (요약 금지). 측정 임계치(예: "p95 ≤2초", "100건 회귀 0건") 보존.

3. **검증 방법 카탈로그 명시**: 모든 Story에 1차 검증 방법(T-U/T-I/T-L/T-S/T-UAT/I/A/D) 1개 이상 명시 — SRS §4.1.6.

4. **Owner role 정밀화**: WBS §2.4 인력 가정(시나리오 C: 백엔드 2 + 프론트 1 + QA 0.5)과 SAD §5 기술 스택을 결합하여 가장 자연스러운 owner 1명 추정. 결정 어려운 경우 `<TBD: 인력 배정 회의>` 표기.

5. **Sprint 단위 self-contained**: 한 Sprint 파일만 보면 그 Sprint의 모든 Story·DoD·Demo Statement·Sprint 종료 체크리스트 확인 가능해야 함. cross-cutting Epic Story는 본 Sprint에 활용되는 부분만 발췌.

6. **링크 정확성**: 모든 References는 현재 영문 파일명 + 정확한 § 섹션 명시 — 2026-05-15 영문 파일명 통일 commit 이후 기준.

7. **Deferred 항목**: BR-V12·V13 / REQ-FUNC-VC-022·023 / EP-22·EP-23 관련 Task는 `priority:deferred` 라벨 + Summary에 "Phase B 수주통합 후 활성" 명시.

8. **revision history**: 각 Sprint 파일에 §개정 이력 섹션 — 첫 작성 시 1.0 entry.

### D. 결과물 검증 게이트 (각 Sprint 파일 작성 후 자가 확인)

- [ ] 본 Sprint의 모든 WBS Story가 빠짐없이 추출됨 (grep `ST-NN-` 으로 카운트 확인)
- [ ] 모든 Story가 GitHub Issue Template 8 섹션(`:dart:`·`:link:`·`:white_check_mark:`·`:test_tube:`·`:gear:`·`:checkered_flag:`·`:construction:`·`:shield:`) 완비
- [ ] 모든 AC가 SRS 원문 인용 + 측정 임계치 포함
- [ ] 모든 Story에 검증 방법(I/A/D/T-*) 명시
- [ ] 모든 Story가 owner role 명시 (또는 `<TBD>`)
- [ ] 의존성·Blocks가 WBS §12 DAG와 일치
- [ ] Critical Path 속한 Story는 별표(`⭐` 또는 명시)로 표시
- [ ] Sprint 파일 끝에 종료 체크리스트 + 다음 Sprint DoR 준비 사항

### E. 사용 예시 (Sprint 0 시작 명령)

> 사용자 → Claude:
> ```
> Phase 0 (S0) Sprint 파일 작성해 줘.
> 위 프롬프트 (20260515_C_Phase2_Task_Extraction_Sprint_Based_v1.md) 따라.
> ```
>
> Claude는:
> 1. WBS §5.1 EP-99(마스터 정비) + EP-00(인프라 기반) Stories 추출
> 2. EP-30·31·32·33의 S0 분산분 결합
> 3. `Phase 2/4.Tasks/Sprints/Phase0.md` 작성
> 4. 자가 검증 게이트 통과
> 5. 사용자에게 검토 요청

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | **초안 발행** — 이전 프로젝트 GitHub Issue Template 골격을 본 프로젝트(공정 스케줄링)에 맞춰 5가지 조정: (1) 경로 매핑 (`Phase 2/4.Tasks/...`), (2) 멀티 에이전트 절차 제거 (단일 Claude 세션), (3) Sprint 단위 추출 변경 (5~10 Task → 30~50 Task/파일), (4) 도메인 특화 7개 항목 추가 (BR·SRS-RSK·검증 방법·PDD Activity·Sprint/Epic 메타·Deferred·Owner role), (5) References 매핑 갱신 (SRS·SAD·PDD-04·WBS·BPMN·ERD). TASK-002 검토 보고서 REV-D-001~010 준수 — 임의 내용 금지, AC 원문 인용, 검증 카탈로그 적용 |

## 참조

| 분류 | 문서 |
|------|------|
| 원천 WBS | `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` |
| WBS 검토 | `Phase 2/4.Tasks/TASK-002_WBS_Review_Report_v1.0.md` |
| 원천 SRS | `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.4.md` |
| 원천 PDD | `Phase 2/1.PDD/4.PDD_master_integrated_Opus_final.md` v1.6 |
| 원천 SAD | `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` v1.1 |
| 이전 프롬프트 (참고) | `0.Prompt/20260514_C_Phase2_Design_Documents_PDD_SRS_SAD.md` |
| 표준 | ISO/IEC/IEEE 29148:2018 §6.4 + 12207 §6.4 + 42010 + IEEE 1028 |
| 방법론 | PMBOK 7th Edition (WBS) + Scrum Guide 2020 (Issue·Sprint·Increment) + INVEST |
