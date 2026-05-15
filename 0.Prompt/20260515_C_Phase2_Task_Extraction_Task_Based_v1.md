# Prompt: Phase 2 Task 상세 추출 — Task별 1파일 (v1)

> 본 프로젝트(사내 공정 스케줄링 시스템) Phase 2/4.Tasks 단계 — WBS v1.2의 ~290개 Task를 **Task 단위 1마크다운 파일**로 추출하기 위한 프롬프트.
>
> **사용 기준**:
> - WBS v1.2 (`Phase 2/4.Tasks/TASK-001_WBS_v1.2.md`) 5개 사용자 기준 모두 충족된 상태
> - 분해 방식 = **Task별 1파일 (B)** 채택 — 1 Task = 1 마크다운 파일, GitHub Issue 1:1 매핑
> - 단일 Claude 세션 환경 (멀티 에이전트 절차 미사용)
> - 사용자 정책: 영문 파일명 통일, ISO/IEC/IEEE 표준, 상세 revision history
> - **학습 친화 (바이브 코딩)**: AI 에이전트에 1 Task씩 던지면 한 단위 완결 — 컨텍스트 좁고 결과 추적 명확
>
> **비교 보존 (선택 안 됨)**: `0.Prompt/20260515_C_Phase2_Task_Extraction_Sprint_Based_v1.md` (Sprint 단위 분해 옵션 — 1번 안)

---

## Role

당신은 **시니어 Software Engineer**입니다. 백엔드(Spring Boot Modulith)·프론트엔드(React + TypeScript + Vite + Ant Design)·QA(JUnit·Testcontainers·Playwright·k6)·DBA(PostgreSQL + Flyway)·DevOps(Docker Compose + Jenkins + Prometheus) 모두를 이해하는 풀스택 시각으로 작업하되, **각 Task의 owner role은 해당 Task 파일의 `labels:owner:` 필드를 참조하여 명확히 분리**합니다.

본 프로젝트 컨텍스트:
- **시스템**: 사내 공정 스케줄링 시스템 (Internal Production Scheduling System)
- **3 프로세스**: 수주 통합(PDD-01) / 성형 가류 스케줄링(PDD-02) / 압출 스케줄링(PDD-03)
- **4 페르소나**: P1 김정훈 주임 (Key Person) / P2 이수진 반장 / P3 박도영 반장 / P4 최민혁 대리
- **표준**: ISO/IEC/IEEE 12207 (Process) + 29148 (Requirements) + 42010 (Architecture) + BPMN 2.0 + IEEE 1028 (Reviews)
- **북극성 지표**: NS-01 P1·P4 만족도 ≥4/5

---

## Target Task

1. **전체 WBS 확인**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5(Epic·Story·Task 분해), §11(추적성 매트릭스), §12(Critical Path + 병렬 실행), §13(SRS-RSK 매핑), §14(SP 합계), §19(GitHub Import 가이드) 일독.

2. **분해 단위**: **1 Task = 1 마크다운 파일**.
   - 폴더 구조: `Phase 2/4.Tasks/Tasks/<EP-NN>/<ST-NN-M>/<TK-NN-M-K>.md`
   - 예: `Phase 2/4.Tasks/Tasks/EP-04/ST-04-1/TK-04-1-1.md`
   - 한 Epic 폴더 → 그 안에 Story 폴더 → 그 안에 Task 파일들
   - 총 약 ~290 파일 (Epic ~46개, Story ~104개)

3. **추출 단위 (한 번에 작업할 양)**: **1 Story의 모든 Task를 일괄 작성** (평균 3~7 파일).
   - 사용자가 "ST-04-1 Task들 작성해 줘" 또는 "EP-04 ST-04-1" 같은 형식으로 명시
   - Claude는 해당 Story 안의 모든 TK-XX-Y-Z를 한 번에 추출
   - **Story 단위 일관성** 자연스럽게 확보 (같은 Story 내 Task끼리 의존성·맥락 공유)

4. **작성 순서 권장**: WBS Sprint 흐름 따름 — Phase 0 EP-99 → EP-00 → EP-01 → EP-02 → ... → EP-47 → EP-22·23 (Deferred).
   - **각 Story 작성 전 사용자 확정 받기** — 점진 검토·반복 가능.

5. **이미 작성된 Task 파일이 있으면 건드리지 않음** (점진 추출 패턴).

---

## Instruction

### A. 폴더 자동 생성

각 Story의 첫 Task 작성 시 폴더 구조 자동 생성:
```
Phase 2/4.Tasks/Tasks/
├── EP-NN/                       (Epic 폴더 — 첫 Story 작성 시 생성)
│   ├── ST-NN-M/                 (Story 폴더 — 첫 Task 작성 시 생성)
│   │   ├── TK-NN-M-1.md         (Task 파일)
│   │   ├── TK-NN-M-2.md
│   │   ├── TK-NN-M-3.md
│   │   ├── ...
│   │   └── _Story_Overview.md   (선택 — Story 메타·진행 상황 요약)
│   └── ST-NN-M+1/
└── EP-NN+1/
```

**`_Story_Overview.md`** (선택 — 권장): 각 Story 폴더 안에 1개. Story 전체 메타 + 포함 Task 목록 + Story 레벨 DoD + 진행 추적. AI 에이전트가 Story 단위로 컨텍스트 조회 시 진입점.

### B. Task 명세 템플릿 (Strict Format — 각 Task 1파일)

각 Task 파일은 아래 9개 섹션을 **모두 포함**. 정보 부재 시 `<TBD: 사용자 확인 필요>` 표기.

```markdown
---
name: <Feature | NFR | Risk-Mitigation | Could | Infra | Cross-Cutting> Task
about: 단일 작업 단위 — 1~3 PD 구현 명세
title: "[TK-NN-M-K] {Task 제목}"
labels: 'sprint:S<N>, epic:EP-NN, story:ST-NN-M, type:{feature|nfr|risk-mitigation|infra|could|cross-cutting}, priority:{must|should|could|deferred}, process:{order|vc|ex|cross-cutting}, owner:{backend|frontend|qa|dba|devops}'
assignees: ''
---

## :dart: Task Summary
- **Task ID**: TK-NN-M-K
- **소속**: EP-NN {Epic 제목} / ST-NN-M {Story 제목} / Sprint S<N>
- **우선순위**: Must | Should | Could | Deferred (Phase B 후 활성)
- **추정**: <X> PD (~<Y> hours, 8h 룰 준수)
- **Owner role**: 백엔드 | 프론트 | QA | DBA | DevOps (또는 `<TBD: 인력 배정 회의>`)
- **작업 요약**: <한 문장 — 무엇을 만드는가>

## :link: References
> :bulb: **AI Agent & Dev Note**: 작업 시작 전 아래 문서를 반드시 먼저 Read/Evaluate 할 것.

- **상위 Story**: `Phase 2/4.Tasks/Tasks/EP-NN/ST-NN-M/_Story_Overview.md` (작성된 경우)
- **WBS Story 정의**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §<해당 EP 섹션> **ST-NN-M**
- **SRS REQ-FUNC**: `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.4.md` §<섹션> **REQ-FUNC-<ID>** ("...AC 원문 1줄 인용...")
- **SRS NFR** (해당 시): **REQ-NF-<ID>** ("...명세 1줄...")
- **PDD BR**: `Phase 2/1.PDD/<X>.process_*_Opus.md` §9 **BR-<ID>** ("...룰 1줄 인용...")
- **PDD Activity·Task** (해당 시): §8 A<N>.<N> · T<N>.<N> (BPMN 매핑)
- **SAD ADR**: `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` §10 **ADR-<NNN>** ("...의사결정 요약...")
- **SRS-RSK 완화** (해당 시): SRS §1.7 **SRS-RSK-<NNN>** ("...리스크 요약...")
- **ERD/스키마** (해당 시): SRS §6.2 또는 SAD §6.1 (테이블·컬럼명 명시)
- **BPMN 다이어그램** (해당 시): PDD §7 (예: PDD-02 §7.2 To-Be `T5` Service Task)
- **마스터 데이터** (해당 시): `Phase 1/2.Raw Materials/...` 엑셀 (K열/L열/B열 등 컬럼 명시)
- **연관 Task**: 같은 Story 내 다른 TK (선행: TK-NN-M-Z, 후행: TK-NN-M-Z+1) — Dependencies 섹션 참조

## :hammer_and_wrench: Implementation Plan
> **Task의 본질** — AI 에이전트가 이 섹션만 읽으면 코딩 시작 가능한 수준의 구체성.

### 변경 대상 (Files / Modules to Touch)
구체적 파일 경로 또는 패턴:
- `backend/src/main/java/<package>/<Class>.java` (신규/수정)
- `backend/src/test/java/<package>/<TestClass>.java` (신규)
- `backend/db/migration/V<NNN>__<name>.sql` (신규 — Flyway)
- `frontend/src/<feature>/<Component>.tsx` (신규/수정)
- `frontend/src/<feature>/__tests__/<Component>.test.tsx` (신규)
- `infrastructure/docker-compose.yml` (수정 — 해당 시)
- `.github/workflows/<name>.yml` (수정 — 해당 시)
- 등

### 핵심 로직 / 알고리즘
> 1~3 문단으로 구현해야 하는 핵심 동작을 자연어로 기술.
>
> 예시 (TK-04-1-2 매트릭스 빌드 서비스):
> "`VC_CONSTRAINT` Repository에서 모든 row를 조회한 뒤, (hose_id, machine_type, slot_position) 키로 O/X 적합성 매트릭스(`Map<TripleKey, Boolean>`)를 빌드한다. Caffeine 캐시(`@Cacheable("compat-matrix")`, TTL 1시간)에 저장. PG `LISTEN/NOTIFY`로 `master.vc_constraint` 변경 이벤트 수신 시 즉시 무효화. p95 ≤1초 보장 위해 batch fetch (single SELECT) 사용."

### 예시 코드/스키마 (선택, 권장)
```java
// 예시: VC_CONSTRAINT 엔티티
@Entity
@Table(name = "vc_constraint", schema = "master")
public class VcConstraint {
    @Id
    @Column(name = "hose_id")
    private String hoseId;

    @Column(name = "lp_slot_top")
    private boolean lpSlotTop;
    // ... G~J 컬럼 + K(lp_left_setting) + L(lp_right_setting) + M~O
}
```

```sql
-- 예시: Flyway 마이그레이션
ALTER TABLE master.vc_constraint
    ADD COLUMN lp_left_setting CHAR(1) NOT NULL DEFAULT 'o'
        CHECK (lp_left_setting IN ('o','x'));
```

## :test_tube: Acceptance Criteria (Task 레벨)

**1차 검증 방법** (ISO/IEC/IEEE 29148:2018 Annex C — SRS §4.1.6 카탈로그):
> **I** Inspection · **A** Analysis · **D** Demonstration · **T-U** Unit Test · **T-I** Integration · **T-L** Load · **T-S** Soak · **T-UAT** UAT

- **1차**: T-U | T-I | ...
- **2차** (선택): I | A | ...

### 측정 가능 기준 (체크리스트)
- [ ] {기준 1: SRS AC 원문 또는 측정 임계치 — 예: "마스터 변경 시 매트릭스 ≤1초 재구축"}
- [ ] {기준 2: 예: "100건 회귀 슬롯 O/X 위반 0건"}
- [ ] {기준 3: 예: "Caffeine 캐시 hit ratio ≥90% (정상 운영)"}

### 회귀 시나리오 (T-U/T-I 대상)
- **입력**: {테스트 데이터 — 예: 47품번 마스터 + 정상/이상 케이스}
- **수행**: {API 호출 / 메서드 실행 / 이벤트 발생}
- **기대 결과**: {DB 상태 / 응답값 / 로그 / 측정값}

## :checkered_flag: Definition of Done
- [ ] 위 모든 측정 기준 통과
- [ ] 단위 테스트 ≥80% 라인 커버리지 (변경 라인 기준)
- [ ] 통합 테스트 통과 (Testcontainers PG·Redis 자동 기동)
- [ ] 코드 리뷰 1명 이상 승인
- [ ] ArchUnit 모듈 경계 위반 0건 (해당 시)
- [ ] SonarQube quality gate 통과
- [ ] Audit·BR-X 룰 위반 0건 (해당 Task가 DB 쓰기 시)
- [ ] API spec(OpenAPI) 갱신 (해당 시)
- [ ] 한국어 UI 텍스트 검수 (프론트 Task)
- [ ] 1차/2차 검증 방법 회귀 결과 보고
- [ ] {Task 특화 추가 DoD 1~2개}

## :construction: Dependencies
- **선행 (Depends on)**:
  - **같은 Story 내**: TK-NN-M-Z (Story 내 직선 흐름)
  - **다른 Story·Epic**: ST-XX-Y / EP-XX (WBS §12 DAG 인용)
- **후행 (Blocks)**:
  - TK-NN-M-Z+1 / ST-XX-Y / EP-XX
- **Critical Path 여부**: ⭐ Critical Path 속함 (WBS §12.1) / ⚪ Float <X> PD 여유 / — (해당 없음)
- **Phase B 종속** (해당 시): 수주통합 작업 완료 후 활성 (`priority:deferred` 라벨)

## :shield: Risk Mitigation
- **완화 SRS-RSK** (해당 시): SRS §1.7.<N> **SRS-RSK-<NNN>** "{리스크 진술 요약}" (WBS §13.1 매핑)
- **잔여 위험·전제** (해당 시): {예: "마스터 K/L열 무결성 가정 — Phase 0 EP-99에서 사전 검증 필수"}

## :memo: Implementation Notes (선택)
- **기술 힌트**: {예: "Spring Cache Abstraction 사용, 직접 Caffeine API 호출 회피"}
- **유사 패턴**: {예: "TK-12I-1-1 cross-master VIEW와 동일한 LISTEN/NOTIFY 패턴 적용"}
- **표준 참조**: {예: ISO/IEC/IEEE 12207 §6.4.9 Operation / BPMN 2.0 §10.3 Task}
- **참고 외부 자료** (선택): Spring Docs URL, GitHub 예시 Repo 등
```

### C. `_Story_Overview.md` 템플릿 (선택, 권장)

각 Story 폴더에 1개. AI 에이전트가 Story 진입 시 첫 조회 대상.

```markdown
# Story Overview — [EP-NN] ST-NN-M {Story 제목}

**Sprint**: S<N> | **Epic**: EP-NN {Epic 제목} | **Priority**: Must|Should|Could
**SP 합계**: <X> | **PD 추정**: <Y>

## Story 목적 (WBS §<섹션> 인용)
> {WBS의 Story 제목 + 한 문단 설명}

## 포함 Task 목록
| Task ID | 제목 | PD | Owner | 상태 |
|---|---|:--:|:--:|:--:|
| [TK-NN-M-1](TK-NN-M-1.md) | {제목} | 1 | 백엔드 | ☐ |
| [TK-NN-M-2](TK-NN-M-2.md) | {제목} | 2 | 백엔드 | ☐ |
| [TK-NN-M-3](TK-NN-M-3.md) | {제목} | 1 | QA | ☐ |
| ... | | | | |

## Story 레벨 DoD (모든 Task 완료 후)
- [ ] 모든 Task DoD 통과
- [ ] Story AC 100% 통과 (WBS AC 출처 인용)
- [ ] 회귀 시나리오 통과
- [ ] Sprint Review 데모 시연 PASS

## References
- WBS: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §<섹션>
- SRS: `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.4.md` §<섹션>

## 진행 이력
| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-MM-DD | TK-NN-M-1 | ☐ → ☑ | PR #N 머지 |
```

### D. 엄격 준수 사항

1. **임의 내용 금지**: SRS·PDD·SAD·WBS·마스터 데이터 원천 인용만. 가정 추가 시 `<TBD: 사용자 확인>` 또는 출처 명시 — TASK-002 검토 보고서 REV-D-004 룰.

2. **AC 측정 임계치 필수**: 모든 측정 기준은 정량적 임계치 포함 (예: "p95 ≤1초", "회귀 100건 위반 0건"). 모호한 표현("적절히", "충분히") 금지.

3. **검증 방법 카탈로그 1차/2차 명시**: SRS §4.1.6 8종(I·A·D·T-U·T-I·T-L·T-S·T-UAT)에서 선택.

4. **Owner role 정밀화**: WBS §2.4 인력 가정(시나리오 C: 백엔드 2 + 프론트 1 + QA 0.5)과 SAD §5 기술 스택 결합. 결정 불가 시 `<TBD: 인력 배정 회의>` 표기.

5. **Implementation Plan 구체성**: AI 에이전트가 본 섹션만 읽고 코딩 시작 가능한 수준. 변경 대상 파일/모듈 경로 + 핵심 로직 자연어 + 예시 코드/스키마 (해당 시).

6. **Self-contained**: 1 Task 파일만 보면 그 Task 완결 작업 가능. 다른 파일은 References에서 명시 (Read 우선순위 분명).

7. **링크 정확성**: 모든 References는 영문 파일명 + 정확한 § 섹션. 2026-05-15 영문화 commit 이후 기준.

8. **Deferred 명시**: BR-V12·V13 / REQ-FUNC-VC-022·023 / EP-22·23 관련 Task는 `priority:deferred` 라벨 + Summary에 "Phase B 수주통합 후 활성" 명시.

9. **Story 단위 일관성**: 같은 Story 내 Task끼리는 Implementation 패턴 공유 가능 (예: 동일 Entity·Service 변경). `_Story_Overview.md`에 공통 context 명시 권장.

### E. 결과물 검증 게이트 (각 Story 작성 후 자가 확인)

- [ ] 본 Story의 모든 WBS Task가 빠짐없이 파일로 생성됨 (WBS의 "핵심 Task" 컬럼 TK 개수와 일치)
- [ ] 모든 파일이 9개 섹션(`:dart:`·`:link:`·`:hammer_and_wrench:`·`:test_tube:`·`:checkered_flag:`·`:construction:`·`:shield:`·`:memo:` + frontmatter) 완비
- [ ] 모든 AC가 측정 임계치 포함 (정량 검증 가능)
- [ ] 모든 Task에 검증 방법(I/A/D/T-*) 1차/2차 명시
- [ ] 모든 Task가 owner role 명시 (또는 `<TBD>`)
- [ ] 의존성·Blocks가 WBS §12 DAG 일치
- [ ] Critical Path Task는 ⭐ 표시
- [ ] `_Story_Overview.md` 생성 (또는 명시적으로 생략)
- [ ] 폴더 구조 정확: `EP-NN/ST-NN-M/TK-NN-M-K.md`
- [ ] 모든 References 링크가 현재 영문 파일명 + 정확한 § 섹션

### F. 사용 예시

**예시 1: Story 단위 추출**
> 사용자 → Claude:
> ```
> ST-04-1 슬롯 적합성 매트릭스 빌드의 Task들 작성해 줘.
> 위 프롬프트 (20260515_C_Phase2_Task_Extraction_Task_Based_v1.md) 따라.
> ```
>
> Claude는:
> 1. WBS §5 EP-04 ST-04-1 확인 → 4개 Task 식별 (TK-04-1-1~4)
> 2. 폴더 `Phase 2/4.Tasks/Tasks/EP-04/ST-04-1/` 생성
> 3. 5개 파일 작성:
>    - `_Story_Overview.md` (Story 메타)
>    - `TK-04-1-1.md` (VC_CONSTRAINT 엔티티 + Repository)
>    - `TK-04-1-2.md` (매트릭스 빌드 서비스)
>    - `TK-04-1-3.md` (`/api/v1/master/compat` 엔드포인트)
>    - `TK-04-1-4.md` (회귀 100건 검증)
> 4. 자가 검증 게이트 통과
> 5. 사용자에게 검토 요청

**예시 2: 단일 Task 우선 작성**
> 사용자 → Claude:
> ```
> TK-04-1-1만 먼저 작성해 줘. 학습 단위로 시작하려고.
> ```
>
> Claude는 폴더 생성 후 TK-04-1-1.md만 작성.

**예시 3: Epic 단위 (확장)**
> 사용자 → Claude:
> ```
> EP-04 전체 Task 작성해 줘.
> ```
>
> Claude는 EP-04 안의 모든 Story(예: ST-04-1·ST-04-2·ST-04-3) 순회하며 폴더·Task 파일 일괄 작성. Story 단위 자가 검증 + 사용자 검토 요청.

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | **초안 발행** — 사용자 선택 (B 옵션, Task별 1파일). Sprint 기반 v1(`20260515_C_Phase2_Task_Extraction_Sprint_Based_v1.md`)을 베이스로 다음 조정: (1) 파일 단위 Sprint → **Task**, (2) 폴더 구조 신설 `Tasks/<EP-NN>/<ST-NN-M>/<TK-NN-M-K>.md`, (3) `_Story_Overview.md` 선택적 보조 파일, (4) Task 명세 9개 섹션 정의 (Task Summary·References·Implementation Plan·AC·DoD·Dependencies·Risk·Notes + frontmatter), (5) Implementation Plan 섹션 강화 (변경 파일/모듈·핵심 로직·예시 코드·스키마), (6) 추출 단위 Sprint → Story (한 Story의 모든 Task 일괄), (7) 자가 검증 게이트 폴더 구조 검사 추가. **학습 친화 (바이브 코딩)** — 1 Task = 1 단위, AI 에이전트 컨텍스트 최소화 |

## 참조

| 분류 | 문서 |
|------|------|
| 원천 WBS | `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` |
| WBS 검토 | `Phase 2/4.Tasks/TASK-002_WBS_Review_Report_v1.0.md` |
| 원천 SRS | `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.4.md` |
| 원천 PDD | `Phase 2/1.PDD/4.PDD_master_integrated_Opus_final.md` v1.6 |
| 원천 SAD | `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` v1.1 |
| 비교 옵션 (Sprint 기반) | `0.Prompt/20260515_C_Phase2_Task_Extraction_Sprint_Based_v1.md` |
| 이전 프로젝트 (참고) | `0.Prompt/20260514_C_Phase2_Design_Documents_PDD_SRS_SAD.md` |
| 표준 | ISO/IEC/IEEE 29148:2018 §6.4 + 12207 §6.4 + 42010 + IEEE 1028 |
| 방법론 | PMBOK 7th Edition (WBS) + Scrum Guide 2020 + INVEST + Vibe Coding (단일 Task 컨텍스트) |
