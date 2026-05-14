# 작업 기록 — 2026-05-14 (C)
## Phase 2 설계 산출물 작성 — PDD · SRS · SAD

> 본 파일은 이전 세션(`20260427_B`·`20260428_B`)에 이어진 작업 로그.
> 형식: 결정·산출물·다음 단계 중심 요약. 상세 코드/명령은 산출 파일 자체 참조.

---

## 1. 세션 진입 상태

| 항목 | 상태 |
|------|------|
| Phase 1 (분석) | ✅ 완료 — 문제정의서 마스터 v2.0, 페르소나 12명, JTBD 결과, KPI·CSF, 통합 PRD |
| Phase 2 (설계) | ⬜ 미진입 — 직전 작업: `12.problem_statement_master.md` 통합 완료 |
| 신규 자료 | `Phase 1/2.Raw Materials/`에 **PDD 작성 규칙 PDF 2종** 추가 (BPMN 2.0 · IEEE 12207:2008) — 다음 단계 신호 |

세션 목적: Phase 2 진입 — PDD·SRS·SAD 작성 및 산출물 간 정합성 확보.

---

## 2. 산출물

### 2.1 Phase 2/1.PDD/ — 프로세스 정의서

| 파일 | 내용 | 최종 |
|------|------|:----:|
| `0.PDD_template.md` | IEEE 12207 + BPMN 2.0 동시 준수 템플릿 (12 sections) | — |
| `1.process_order_consolidation_Opus.md` | PDD-01 수주 정보 통합 (GAP=4 최우선) | v1.0 |
| `2.process_vulcanization_scheduling_Opus.md` | PDD-02 성형 스케줄링 (회전수 기반, 슬롯 O/X, 앵글 최소화) | v1.0 |
| `3.process_extrusion_scheduling_Opus.md` | PDD-03 압출 스케줄링 (D-1 역산, 셋팅 그룹핑, 신규 우선) | v1.0 |
| **`4.PDD_master_integrated_Opus.md`** | **3 PDD 통합 + PRD 통합본 (정본)** | **v1.3** |
| `5.PDD_master_Gemini.md` | 비교/참조용 — 순수 PRD (Gemini 작성) | v0.1 |
| `README.md` | 폴더 인덱스 + 사용 가이드 | — |

#### 4번 마스터 통합본 진화 흐름

| 버전 | 추가 | 결과 |
|-----|------|------|
| v1.0 | 3 PDD 통합 + 데이터 사전·횡단 룰·통합 KPI/Risk/Traceability | 단일 정본 |
| v1.1 | PRD 요소 6 섹션 (North Star, User Stories G-W-T, MoSCoW, NFR, Differential Value, Rollout/EXP, Proof) | PDD+PRD 통합 |
| v1.2 | Gemini 강점 흡수 — §4.5 ERD, UI 임계치 (드래그 차단 ≤1초·PUSH ≤2초), Slack Alert, §22 PRD-Cheatsheet 1페이지 | ERD 신설 |
| **v1.3** | §14 Assumptions(10개)·§15 ADR(7건 ADR-001~007)·§17.5 의존성 DAG + Sprint 1~5 배분 | **6/6 PRD 품질 기준 PASS** |

### 2.2 Phase 2/2.SRS/ — 소프트웨어 요구사항 명세 (ISO/IEC/IEEE 29148:2018)

| 파일 | 언어 | 버전 | 줄수 |
|------|:----:|:----:|:---:|
| `SRS-001_Production_Scheduling_System.md` | 영문 | v1.1 | 1,193 |
| `SRS-001_Production_Scheduling_System_v1.2.md` | 영문 | v1.2 | 1,877 |
| **`SRS-001_공정스케줄링시스템_v1.3.md`** | **한글** | **v1.3 (+p1·p2)** | **1,877** |

#### SRS 진화 흐름

| 버전 | 변경 | 결과 |
|-----|------|------|
| v1.0 (in-place) | REF-01 v1.3 기반 초안 — 68 REQ-FUNC + 54 REQ-NF + 추적 매트릭스 + 시퀀스 7개 | 9/9 매핑 미달 |
| v1.1 (in-place) | §1.7 Risks(SRS-RSK-001~014)·§4.1.6 Verification Catalog·§5.1·5.2 커버리지·AOS/Validator 정의 | **9/9 PRD→SRS 매핑 PASS** |
| v1.2 (신규 파일) | §3.5 UseCase + §6.2.0 ERD + §6.2.13 Class + §6.4 Component 다이어그램, KPI 13→19, US-06 명시적 이연 | **8/8 DoD PASS**, 다이어그램 7→11 |
| **v1.3 (신규 파일)** | **한글 전면 번역** — 동일 구조 유지 (1,877줄) | 한·영 동등 |
| v1.3p1 (in-place) | Mermaid 11.14 호환 패치 — `PK_FK`→`PK`, `{}`→`()`, subgraph 따옴표 | 다이어그램 정상 |
| v1.3p2 (in-place) | 잔여 패치 — 세미콜론→`+`, UI participant 명시 | 모든 다이어그램 렌더 |

### 2.3 Phase 2/3.SAD/ — 소프트웨어 아키텍처 (ISO/IEC/IEEE 42010 · arc42)

| 파일 | 버전 | 줄수 |
|------|:----:|:---:|
| **`SAD-001_공정스케줄링시스템_v1.0.md`** | **Draft v1.0** | **996** |

#### SAD 핵심 결정 — 기술 스택

| 계층 | 채택 | ADR |
|-----|------|:---:|
| 백엔드 | Java 21 LTS + Spring Boot 3.3 + Spring Modulith | ADR-008 |
| 프론트엔드 | React 18 + TS 5 + Vite + Ant Design 5 + TanStack Query + Zustand | ADR-009 |
| 데이터 | PostgreSQL 16 + Flyway + Redis 7 | ADR-010 |
| 이벤트 | Spring ApplicationEvent + PG LISTEN/NOTIFY + Redis Pub/Sub (Kafka 미채택) | ADR-011 |
| 인증 | Keycloak 24 (SAML/OIDC) | ADR-012 |
| 컨테이너 | Docker + Docker Compose v2 (Kubernetes 미채택) | ADR-013 |
| 관측성 | Prometheus + Loki + Grafana 10 + Slack 알림 | ADR-014 |
| CI/CD | Jenkins LTS + Harbor + SonarQube + Trivy | ADR-015 |
| API 게이트웨이 | NGINX 1.25 (TLS 1.3) | — |
| Excel 파싱 | Apache POI 5 (streaming) | — |
| OS·인프라 | Ubuntu 22.04 LTS / 8 vCPU·32GB·500GB SSD (단일 서버) | — |

C4 다이어그램 4종 (Context · Container · Component · Deployment), SAD-RSK-001~008 신규 리스크, 추적성 매트릭스(SAD → SRS REQ), NFR 실현 매핑 23행 포함.

---

## 3. 주요 의사결정

### 3.1 워크플로우 규칙 (메모리에 영구 저장)

| 결정 | 요지 | 메모리 |
|------|------|------|
| **갱신은 새 파일로** | 문서 v1.0→v1.1로 갱신 시 in-place 수정 금지, 새 파일 생성. 단순 오탈자·문법 버그는 in-place 허용 | `memory/feedback_versioning.md` |
| **영림원 ERP는 통합 범위 외** | 사내 영림원 ERP 운영 중이나 본 시스템과 연결 안 함. SRS·SAD의 EXT-SYS 목록에 추가하지 말 것 | `memory/project_scope_erp.md` |

### 3.2 Phase 구분 명확화

| Phase | 산출물 | 상태 |
|------|------|:----:|
| Phase 1 (분석) | 문제정의·페르소나·JTBD·PRD | ✅ |
| Phase 2 (설계) | 1.PDD · 2.SRS · 3.SAD · 4.Tasks · 5.TestPlan | 1·2·3 ✅ / 4·5 ⬜ |
| Phase 3 (개발·도입) | Stage 0 (As-Is 측정) · 1.0 (파일럿 8주) · 1.1 (확장 8주) · 1.2 (안정화 4주) | ⬜ |
| 운영 | 0.5 FTE 평상 운영 + KPI 모니터링 | ⬜ |

> ⚠️ "Phase 0/1.0/1.1/1.2"는 Phase 3 내부의 **Stage**임. 프로젝트 Phase 1·2·3과 명명 혼동 주의.

### 3.3 PRD 품질 6/6 PASS 기준 적용 (v1.3)

| # | 기준 | 결과 |
|---|------|:----:|
| 1 | 목표·지표 (북극성·보조 KPI 수치화) | ✅ |
| 2 | 스토리·AC (G-W-T + SLO + 실패 케이스 ≥2) | ✅ |
| 3 | 기능 요구 (MoSCoW·근거·의존성·1 스프린트 구현성) | ✅ |
| 4 | 비기능 (성능·가용성·보안·비용 + 모니터링) | ✅ |
| 5 | 리스크·가정·ADR | ✅ |
| 6 | 범위 (In/Out 명확) | ✅ |

### 3.4 SRS 8/8 DoD 기준 적용 (v1.2)

| # | 기준 | 결과 |
|---|------|:----:|
| 1 | PRD Story·AC → REQ-FUNC | ✅ |
| 2 | KPI·성능 → REQ-NF | ✅ (19 KPI) |
| 3 | API 목록 | ✅ (30 엔드포인트) |
| 4 | 엔터티·스키마 | ✅ (13 엔터티 + ERD) |
| 5 | 추적성 매트릭스 | ✅ (3 매트릭스) |
| 6 | UseCase·ERD·Class·Component 다이어그램 | ✅ (4종) |
| 7 | 시퀀스 다이어그램 3~5개 | ✅ (7개) |
| 8 | ISO 29148 구조 | ✅ |

### 3.5 기술 스택 비교 — 현행 유지 결정

이전 프로젝트 스택(Next.js + Vercel + Supabase + Gemini)과 비교:
- **사내망 제약(CON-01)**으로 Vercel·Supabase·Gemini 외부 API 사용 불가
- **사내 Java/Spring 표준**과 **LTS 8년·엔터프라이즈 패턴**을 우선
- 차용 가능 요소: shadcn/ui(Ant Design 대체)·Prisma 스타일 마이그레이션·Docker Compose 단일 호스트 — 향후 검토 가능
- **결정: 현 SAD v1.0(Java + Spring + React + PostgreSQL + Docker Compose) 유지**

---

## 4. 추적성 — Phase 2 산출물 간 정합

```
REF-01 (PRD Master v1.3)
  ├─ §16 User Stories (US-01~07)        ──→ SRS §4.1 Source 컬럼
  ├─ §17 MoSCoW (M·S·C·W)               ──→ SRS §4.1 Priority 컬럼
  ├─ §18 NFR (PER·REL·SEC·USA·OPS·COM·COS·KPI) ──→ SRS §4.2 60개 NFR
  ├─ §15 ADR-001~007                    ──→ SRS §1.5 CON-01~10 (제약)
  ├─ §14 Assumptions (10개)              ──→ SRS §1.6 ASM-01~10
  ├─ §12 Risks (R-X/O/V/E 14개)         ──→ SRS §1.7 SRS-RSK-001~014
  └─ §4.5 ERD                           ──→ SRS §6.2.0 (Mermaid)
                                              │
                          SRS (122 REQ) ─────┘
                                              │
                          SAD §10 (ADR-008~015) — 기술 스택
                                              │
                          SAD §12 추적성 매트릭스 (SAD 영역 → SRS REQ)
```

---

## 5. 문서 메타데이터

| 폴더 | 파일 수 | 상태 |
|------|:------:|:----:|
| `Phase 2/1.PDD/` | 7 | ✅ |
| `Phase 2/2.SRS/` | 3 | ✅ |
| `Phase 2/3.SAD/` | 1 | ✅ |
| `Phase 2/4.Tasks/` | 0 | ⬜ |
| `Phase 2/5.TestPlan/` | 0 | ⬜ |

총 11개 신규/갱신 산출 파일.

---

## 6. Mermaid 호환성 이슈 정리 (참고)

본 세션에서 발견한 Mermaid 11.14 strict 파싱 이슈:

| 패턴 | 회피 방법 |
|------|---------|
| `PK_FK` 등 복합 ER 어노테이션 | 단일 `PK` 또는 `FK`만 |
| 시퀀스 메시지 내 `{...}` | `(...)` 로 치환 |
| 시퀀스 메시지 내 `;` | `+` 또는 `,` 로 치환 |
| 미선언 participant 묵시적 생성 | 상단에 명시적 `participant` 선언 |
| `subgraph X [title with em-dash —]` | `subgraph X ["title"]` (따옴표 래핑) |
| 노드 라벨에 `(...)` | `node["label with (...)"]` (따옴표 래핑) |

---

## 7. 다음 단계

### Phase 2 잔여
| 작업 | 위치 | 예상 |
|------|------|:----:|
| Sprint 백로그 | `Phase 2/4.Tasks/` | 3~5일 |
| Test Plan (TC-NNN 68건) | `Phase 2/5.TestPlan/` | 3~5일 (QA 병행) |

### Phase 3 진입 (Phase 2 완료 후)
| Stage | 기간 | 게이트 조건 |
|-------|:--:|----------|
| Stage 0 As-Is 측정 | 2주 | KPI 18종 베이스라인 확보 |
| Stage 1.0 파일럿 | 8주 | NS-01 ≥4/5, NS-S01 ≤1h, NS-S02 = 0, Must 12 동작 |
| Stage 1.1 확장 | 8주 | 단계별 NS-01 유지 |
| Stage 1.2 안정화 | 4주 | P1 부재 시뮬레이션 통과 |

---

## 8. 참조

| 분류 | 위치 |
|------|------|
| 이전 세션 1 | `0.Prompt/20260427_B_Selecting Internal Service Architecture.md` |
| 이전 세션 2 | `0.Prompt/20260428_B_Consolidating Problem Statement Documentation.md` |
| 본 세션 정본 | `Phase 2/1.PDD/4.PDD_master_integrated_Opus.md` v1.3 |
| 본 세션 SRS | `Phase 2/2.SRS/SRS-001_공정스케줄링시스템_v1.3.md` |
| 본 세션 SAD | `Phase 2/3.SAD/SAD-001_공정스케줄링시스템_v1.0.md` |
| 표준 1 | ISO/IEC/IEEE 12207:2008 (`Phase 1/2.Raw Materials/PDD 작성 규칙_*`) |
| 표준 2 | OMG BPMN 2.0 (`Phase 1/2.Raw Materials/PDD 작성 규칙_*`) |
| 표준 3 | ISO/IEC/IEEE 29148:2018 |
| 표준 4 | ISO/IEC/IEEE 42010:2011 · arc42 v8 · C4 Model |
| 메모리 | `~/.claude/projects/.../memory/{MEMORY,feedback_versioning,project_scope_erp}.md` |
