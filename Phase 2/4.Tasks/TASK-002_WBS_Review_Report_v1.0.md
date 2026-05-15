# WBS 검토 보고서 (Work Breakdown Structure Review Report)
문서 ID: TASK-002
개정: 1.0
작성일: 2026-05-15
표준 참조: ISO/IEC/IEEE 29148:2018 §6.4 (Requirements verification) + IEEE 1028 (Software Reviews) + PMBOK 7th §5.3 (Scope Validation)

검토 대상: [TASK-001 WBS v1.1](TASK-001_WBS_v1.1.md)
검토 사유: 사용자 정의 5개 기준 부합성 검증 — Phase 3(구현) 진입 전 게이트 검토
검토 결과: **5개 기준 모두 부분 충족** — 보강 필수 (→ WBS v1.2)

---

## 1. 검토 목적 (Review Purpose)

본 보고서는 [TASK-001 WBS v1.1](TASK-001_WBS_v1.1.md)이 다음 5개 사용자 정의 기준에 부합하는지 IEEE 1028 Inspection 방식으로 검증한다. 모든 결함(Defect)은 식별자(REV-D-NNN), 심각도, 위치, 권장 조치, 영향받는 v1.2 섹션을 포함한다.

## 2. 검토 기준 (Review Criteria)

| ID | 사용자 정의 기준 | 검증 방법 |
|:--:|---|---|
| C1 | SRS 계획 전체를 커버할 것 | SRS v1.4의 75 REQ-FUNC + 60 REQ-NF + 14 SRS-RSK ↔ WBS Story/Task 양방향 매핑 검사 |
| C2 | SRS를 넘어서는 임의 내용이 없을 것 | WBS 본문의 모든 가정·수치·방법론을 PDD/SRS/SAD 원천에 역추적 |
| C3 | SRS에서 추구하는 서술 방침을 따를 것 | SRS의 "shall" 형식·AC 명시·검증 방법 카탈로그 패턴 일치성 검사 |
| C4 | GitHub 프로젝트로 관리될 수 있는 포맷을 따를 것 | GitHub Issues/Projects 직접 Import 가능성 + 라벨·milestone·assignee 매핑 |
| C5 | 순차·병렬 계획 수립의 근거자료로 충분할 것 | Critical Path 명시·병렬 실행 기회·Resource Leveling·Float/Slack 표기 |

## 3. 검토 방법 (Methodology)

| 단계 | 도구 | 산출 |
|---|---|---|
| 정량 검사 | grep 카운트 (REQ-FUNC, REQ-NF, SRS-RSK) | 매핑 갭 수치화 |
| 정성 검사 | WBS 본문 § 단위 정독 + SRS § 4.1·4.2 대조 | "포함" 표기·임의 가정 식별 |
| 포맷 검사 | GitHub Issues 직접 Import 시뮬레이션 | 변환 차단 요소 식별 |
| 계획 가능성 | DAG·Critical Path·Resource 분석 | 누락 차원 식별 |

## 4. 검토 결과 요약 (Findings Summary)

| 기준 | 충족 | 결함 ID | 심각도 |
|:--:|:--:|---|:--:|
| C1 | 🟡 부분 | REV-D-001·002·003 | **High** |
| C2 | 🟡 부분 | REV-D-004 | Medium |
| C3 | 🟡 부분 | REV-D-005·006 | Medium |
| C4 | 🔴 부분 | REV-D-007 | **High** (실질적으로 직접 Import 불가) |
| C5 | 🟡 부분 | REV-D-008·009·010 | Medium |
| **전체** | **5/5 부분 충족** | **결함 10건** | **High 3 / Medium 7** |

> **종합 판정**: WBS v1.1은 골격은 견고하나, **NFR·SRS-RSK 분해 부재 + AC 텍스트 미포함 + GitHub Import 불가 + 병렬 계획 근거 부족**으로 Phase 3(실제 구현) 진입 게이트 기준 미달. **v1.2 보강 필수**.

---

## 5. 상세 결함 (Detailed Defects)

### REV-D-001 — NFR 60건 중 ~54건 분해 누락 [C1, High]

| 항목 | 내용 |
|---|---|
| 위치 | [WBS v1.1 §5 전체](TASK-001_WBS_v1.1.md), 특히 §11 추적성 매트릭스 |
| 발견 | grep 결과: WBS의 REQ-NF 참조 **6건만 산발 등장** (PER-004·PER-006·OPS-001~007 일괄·REL-005·SEC-004·USA-003). SRS v1.4 §4.2의 60 NFR 중 PER 8·REL 6·SEC 7·USA 5·OPS 7·COM 5·COS 3·KPI 19개 **대부분 명시적 Task 없음** |
| SRS 출처 | [SRS v1.4 §4.2.1~4.2.8 라인 731~828](../2.SRS/SRS-001_Production_Scheduling_System_v1.4.md) |
| 영향 | 비기능 요건(성능·보안·사용성·관측성 등) 검증·구현이 Sprint Plan에서 누락 → Phase 1.0 출시 시 NFR-PER-001 (1만 row 60초)·NFR-REL-001 (99.5% 가용성) 등 측정 가능한 게이트 부재 |
| 권장 조치 (v1.2) | NFR 분해 Epic **EP-40~47** 신규 (PER·REL·SEC·USA·OPS·COM·COS·KPI 각 1 Epic). 각 NFR은 1 Story 이상으로 분해 |
| 검증 | v1.2에서 grep `REQ-NF-` 결과 **≥60건** 매핑 |

### REV-D-002 — SRS-RSK 14건 완전 누락 [C1, High]

| 항목 | 내용 |
|---|---|
| 위치 | [WBS v1.1 §13 리스크 ↔ Task 매핑](TASK-001_WBS_v1.1.md#13-리스크--task-매핑-mitigation) |
| 발견 | WBS §13 매핑은 PRD R-X01~R-V09·SAD-RSK-009~012만 다룸. SRS v1.4 §1.7의 SRS-RSK-001~014 **전부 미언급** |
| SRS 출처 | [SRS v1.4 §1.7 라인 173~221](../2.SRS/SRS-001_Production_Scheduling_System_v1.4.md) |
| 영향 | SRS는 14 리스크 모두에 명시적 완화 REQ를 연결 (`§1.7.5 리스크 → 완화 커버리지 — 잔류 0`). WBS는 이 추적성을 끊음 → Sprint Review에서 리스크 검증 누락 가능 |
| 권장 조치 (v1.2) | §13에 SRS-RSK-001~014 행 추가, 각 리스크의 완화 REQ → WBS Task 매핑 명시 |
| 검증 | v1.2에서 grep `SRS-RSK-0` 결과 14건 이상 |

### REV-D-003 — REQ-FUNC 6건 "포함:" 표기로 명시 Story 부재 [C1, Medium]

| 항목 | 내용 |
|---|---|
| 위치 | [WBS v1.1 §11.2~11.4](TASK-001_WBS_v1.1.md#11-추적성-매트릭스-req--epicstory) |
| 발견 | 다음 6 REQ가 명시적 Story 없이 "(포함: ...)" 표기:<br>- REQ-FUNC-VC-015 (충돌 리포트 ≥3 대안)<br>- REQ-FUNC-VC-016 (on-demand 전체 검사 ≤3초 p95)<br>- REQ-FUNC-EX-010 (Q_ext 필요 수량)<br>- REQ-FUNC-EX-011 (검증 게이트)<br>- REQ-FUNC-EX-012 (충돌 대안)<br>- REQ-FUNC-EX-013 (`vc.changed` 자동 트리거)<br>- REQ-FUNC-EX-014 (WebSocket PUSH ≤2초)<br>- REQ-FUNC-CO-009 (한국어 UI) |
| 영향 | Owner·DoD·SP·테스트 케이스가 명시되지 않아 Sprint 운영 시 누락 위험 |
| 권장 조치 (v1.2) | 각 REQ를 명시적 Story로 승격, 또는 기존 Story에 명확히 흡수 표기 (예: "ST-04-1 충돌 리포트 알고리즘에 REQ-FUNC-VC-015 AC 포함") |
| 검증 | v1.2 §11에서 "(포함:" 표기 0건 |

### REV-D-004 — WBS 고유 가정 4건 (Velocity·인력·SP 척도·방법론) [C2, Medium]

| 항목 | 내용 |
|---|---|
| 위치 | [WBS v1.1 §1·§2.4](TASK-001_WBS_v1.1.md) (line 5, 57, 137, 779~781) |
| 발견 | 다음 4가지가 SRS·PDD·SAD 원천에 없음:<br>(a) `Velocity 35 SP/Sprint × 5 Sprint = 175 SP` — 산정 가정<br>(b) `2-person team × 10 days = 20 person-days/Sprint` — 인력 가정<br>(c) Fibonacci SP 척도 (1·2·3·5·8·13) — 추정 방법론<br>(d) 표준 참조 `PMBOK 7th + Scrum Guide 2020 + INVEST` — SRS는 ISO 12207·29148만 인용 |
| 영향 | 사용자 기준 "SRS를 넘어서는 임의 내용이 없을 것"에 형식적 위배. 특히 (b) 인력 가정은 실제 사내 자원과 다를 시 SP 합계·Sprint 분배 의미 상실 |
| 권장 조치 (v1.2) | (a)(b) [SRS NFR-COS-003 (≤0.5 FTE 평상 운영) + PDD-04 §17.5.4 (각 Must 4 PD 추정)](../2.SRS/SRS-001_Production_Scheduling_System_v1.4.md) 인용으로 정당화 + 사용자 검토 게이트 명시. (c)(d) WBS 방법론 부록으로 분리하여 "SRS 외 보조 도구" 명시 |
| 검증 | v1.2 §2.4에 정당화 출처 + 사용자 검토 필요 명시 |

### REV-D-005 — AC 텍스트 본문 미포함 (REQ ID만 참조) [C3, Medium]

| 항목 | 내용 |
|---|---|
| 위치 | WBS v1.1 §5 전체 (Story 표) |
| 발견 | 각 Story는 "AC 출처" 열에 `REQ-FUNC-OC-001·002` 같은 ID만 표기. SRS의 실제 AC 텍스트("자동 매핑 ≥95%", "1만 row 60초 p95" 등) 미포함 |
| SRS 출처 | SRS v1.4 §4.1·4.2 각 REQ 행의 "AC" 컬럼 (인수 기준 텍스트) |
| 영향 | WBS만으로는 Task 시작이 self-contained하지 않음 — 개발자가 매번 SRS를 별도 조회해야 함. Sprint Review 데모 시 AC 검증 누락 가능 |
| 권장 조치 (v1.2) | 각 Story 표에 **"AC 핵심 텍스트"** 열 추가. SRS 원문 AC를 1~2줄 발췌 |
| 검증 | v1.2 §5 각 Story가 측정 가능한 AC 텍스트 1줄 이상 포함 |

### REV-D-006 — SRS 검증 방법 카탈로그(I·A·D·T-U·T-I·T-L·T-S·T-UAT) 미반영 [C3, Medium]

| 항목 | 내용 |
|---|---|
| 위치 | WBS v1.1 §5 Story 표 |
| 발견 | SRS §4.1.6은 ISO/IEC/IEEE 29148:2018 Annex C에 따른 검증 방법 8종(Inspection / Analysis / Demonstration / Unit·Integration·Load·Soak·UAT) 카탈로그를 제공. WBS는 Task에 검증 방법을 매핑 안 함 |
| SRS 출처 | [SRS v1.4 §4.1.6 라인 641~728](../2.SRS/SRS-001_Production_Scheduling_System_v1.4.md) |
| 영향 | DoD에 "단위 테스트 ≥80%"만 있고, Story별로 어떤 검증 카테고리가 요구되는지 불명확 |
| 권장 조치 (v1.2) | §11 추적성 매트릭스에 "검증 방법" 열 추가, SRS §4.1.6 카탈로그 인용 |
| 검증 | v1.2 §11 각 REQ 행에 1차/2차 검증 방법 명시 |

### REV-D-007 — GitHub Projects 직접 Import 불가 (라벨·milestone·frontmatter·assignee·Issue 본문 부재) [C4, High]

| 항목 | 내용 |
|---|---|
| 위치 | WBS v1.1 전체 (포맷 차원) |
| 발견 | GitHub Projects는 (1) Issues 단위 (2) milestone (3) labels (4) assignee (5) custom fields (6) blocks 관계로 구성. WBS는:<br>- ✅ 고유 ID (EP/ST/TK) 있음 — Issue 제목 변환 가능<br>- ❌ YAML frontmatter 없음<br>- ❌ 라벨 매핑표 없음 (Sprint·Epic·type·priority)<br>- ❌ Milestone 정의 없음 (Sprint S0~S5·Deferred 7개 후보)<br>- ❌ Assignee 필드 없음<br>- ❌ 각 Task가 Issue로 변환되려면 Description·AC·DoD 본문 필요 — 현재 표 셀 한 줄만 있음<br>- ❌ "blocks/blocked by" 링크 형식 없음 ("선행: EP-XX" 텍스트만)<br>- ❌ gh CLI 또는 자동화 스크립트 부재 — 190 Task 수동 입력 불가 |
| 영향 | **현재 문서로는 GitHub Projects에 자동 Import 불가**. 별도 변환 스크립트 또는 수동 입력 필요 |
| 권장 조치 (v1.2) | §19 신규 — GitHub Import 가이드:<br>(a) 라벨 정의 표 (Sprint·Epic·Type·Priority)<br>(b) Milestone 정의 표 (S0·S1·S2·S3·S4·S5·Deferred·Phase 0)<br>(c) Issue Title 명명 규칙 (`[EP-04] ST-04-1 슬롯 적합성 매트릭스 빌드`)<br>(d) gh CLI 또는 Python 스크립트 예시 (CSV 또는 직접 API 호출)<br>(e) 의존성 표기 변환 (`선행: EP-XX` → `Closes #N` 또는 issue link) |
| 검증 | v1.2 §19에서 모든 5요소 명시, 변환 스크립트 의사코드 1개 이상 |

### REV-D-008 — Critical Path 명시 부재 [C5, Medium]

| 항목 | 내용 |
|---|---|
| 위치 | WBS v1.1 §12 의존성 매트릭스 |
| 발견 | DAG 다이어그램은 있으나 가장 긴 경로(Critical Path) + 총 SP/PD 명시 없음 |
| 영향 | Sprint 일정 단축 시 어디를 우선해야 할지 PM이 판단 어려움 |
| 권장 조치 (v1.2) | §12에 Critical Path 명시:<br>예: `S0(EP-00·99) → S1(EP-01→02→03) → S2(EP-04→05→21) → S3(EP-07→08→09→12I) → S4(EP-10→11→13) → S5(EP-15·E2E)` + 총 PD 계산 |
| 검증 | v1.2 §12에 Critical Path 표 + 임계 경로 시각화 (Mermaid Gantt 또는 강조 표기) |

### REV-D-009 — 병렬 실행 기회 명시 부재 [C5, Medium]

| 항목 | 내용 |
|---|---|
| 위치 | WBS v1.1 §12 |
| 발견 | "어떤 Epic·Story가 같은 시점에 병렬 진행 가능한지" 명시적 표 없음. 의존성 DAG로 추론만 가능 |
| 영향 | 다인력 배치 시 PM이 별도 분석해야 함. 자원 leveling 어려움 |
| 권장 조치 (v1.2) | §12에 "동시 실행 가능 그룹" 표:<br>예: `S1: [EP-01 ↔ EP-30(Keycloak)] [EP-02 ↔ EP-32(CI/CD 보강)]` |
| 검증 | v1.2 §12에 병렬 실행 표 |

### REV-D-010 — Float/Slack·Resource Leveling 부재 [C5, Medium]

| 항목 | 내용 |
|---|---|
| 위치 | WBS v1.1 §14 |
| 발견 | S2 합계 37 SP > Velocity 35 SP — 과부하지만 해결 가이드 부재.<br>인력 가정 2-person만 있고 3-person·4-person 시나리오 부재.<br>각 Epic의 Float(여유 시간) 표기 없음 |
| 영향 | 일정 위험 조기 식별 어려움. Sprint 시작 후 결함 발견 시 대응 지연 |
| 권장 조치 (v1.2) | §14에 시나리오 분석 추가:<br>(a) 인력 시나리오 2/3/4-person별 총 기간<br>(b) S2 과부하 해결 옵션 3가지 (인력 보강·일부 S3 이월·범위 축소)<br>(c) 각 Epic Float 일수 (Critical Path 외 Epic은 여유 명시) |
| 검증 | v1.2 §14에 시나리오 표 3종 |

---

## 6. v1.2 보강 계획 (Action Plan)

| Step | 작업 | 영향 섹션 | 결함 해소 |
|:--:|---|---|---|
| 1 | NFR 분해 Epic 신규 (EP-40~47) | §5 신규 8 Epic | REV-D-001 |
| 2 | SRS-RSK 14건 완화 Task 매핑 | §13 보강 | REV-D-002 |
| 3 | "포함" Story 6건 명시화 | §5 명시적 Story 신규 | REV-D-003 |
| 4 | 가정 정당화 (NFR-COS-003 인용 + 사용자 검토 게이트) | §2.4 | REV-D-004 |
| 5 | AC 텍스트 본문 인용 열 추가 | §5 표 열 추가 (전체 Story) | REV-D-005 |
| 6 | 검증 방법 (I·A·D·T-U/T-I/T-L/T-S/T-UAT) 매핑 | §11 표 열 추가 | REV-D-006 |
| 7 | GitHub Import 가이드 §19 신규 | §19 신규 | REV-D-007 |
| 8 | Critical Path + 병렬 실행 + Float/Slack 표 | §12·§14 보강 | REV-D-008·009·010 |
| 9 | §14 추정 요약 재계산 (NFR Epic 추가로 SP 증가 반영) | §14 | (산술 정합성) |
| 10 | §17 Revision history v1.2 entry | §17 | (메타) |

**예상 v1.2 분량**: v1.1의 779줄 → v1.2 ~1,100~1,200줄 (NFR Epic 8개·SRS-RSK 매핑·AC 텍스트 열·GitHub 가이드 §19 신규)

## 7. 검증 게이트 (Acceptance Criteria for v1.2)

다음 모든 조건 충족 시 v1.2 승인:

- [ ] grep `REQ-NF-` 결과 ≥60건 명시 매핑 (REV-D-001)
- [ ] grep `SRS-RSK-` 결과 ≥14건 매핑 (REV-D-002)
- [ ] §11 "포함:" 표기 0건 (REV-D-003)
- [ ] §2.4 인력·Velocity 가정에 SRS NFR-COS-003·PDD-04 §17.5.4 인용 (REV-D-004)
- [ ] 모든 Story에 AC 핵심 텍스트 1줄 이상 (REV-D-005)
- [ ] §11 각 REQ 행에 검증 방법 카탈로그(I·A·D·T-*) 명시 (REV-D-006)
- [ ] §19 GitHub Import 가이드 5요소(라벨·milestone·title 규칙·스크립트·의존성 변환) 모두 명시 (REV-D-007)
- [ ] §12에 Critical Path 명시 + 병렬 실행 표 + Float 표기 (REV-D-008·009·010)
- [ ] §14 추정 합계 본문 카운트와 일치 (산술 정합성)
- [ ] §17 Revision history v1.2 entry (변경 사유·영향 범위 명시)

## 8. 리스크 (Review-level Risk)

| ID | 리스크 | 완화 |
|:--:|---|---|
| REV-RSK-001 | v1.2 분량 증가로 가독성·유지보수 부담 | NFR Epic은 §5에서 압축 표 형식, GitHub 가이드는 §19 부록으로 분리 |
| REV-RSK-002 | AC 텍스트 인용으로 SRS와 중복 — SRS 개정 시 동기화 부담 | SRS REQ ID 링크 형식 유지, 핵심 1줄만 발췌 |
| REV-RSK-003 | NFR 60건 분해로 SP 합계 급증 (현 185 → 추정 230~250) | Velocity 가정 재검토 필요, Phase 1.0 범위 조정 또는 NFR Epic 일부 Phase 2 이연 검토 |

## 9. 개정 이력 (Revision History)

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | **초안 발행** — TASK-001 WBS v1.1 5개 기준 부합성 IEEE 1028 Inspection 결과. 결함 10건(High 3 + Medium 7) 식별, v1.2 보강 계획 + 검증 게이트 + REV-RSK 3건 정의 |

## 10. 참조 (References)

| 분류 | 문서 |
|------|------|
| 검토 대상 | [TASK-001 WBS v1.1](TASK-001_WBS_v1.1.md) |
| 원천 SRS | [SRS v1.4](../2.SRS/SRS-001_Production_Scheduling_System_v1.4.md) (75 REQ-FUNC + 60 REQ-NF + 14 SRS-RSK) |
| 원천 PDD | [PDD-04 v1.6](../1.PDD/4.PDD_master_integrated_Opus_final.md) |
| 원천 SAD | [SAD v1.1](../3.SAD/SAD-001_Production_Scheduling_System_v1.0.md) (in-place v1.1) |
| 표준 | ISO/IEC/IEEE 29148:2018 §6.4 Requirements verification |
| 표준 | IEEE 1028-2008 Software Reviews and Audits |
| 방법론 | PMBOK 7th Edition §5.3 Scope Validation |
