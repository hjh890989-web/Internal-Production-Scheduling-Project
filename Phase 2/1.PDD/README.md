# Phase 2 / 1. PDD — Process Definition Documents

본 폴더는 공정 스케줄링 시스템 **설계 단계(Phase 2)**의 첫 번째 산출물로, **3대 핵심 프로세스**에 대한 **PDD(Process Definition Document)** 모음이다.
각 PDD는 **ISO/IEC/IEEE 12207:2008** Purpose–Outcomes–Activities 골격과 **OMG BPMN 2.0 Descriptive Conformance** 표기를 동시 준수하며, 향후 SRS·구현·테스트의 단일 근거 문서로 사용된다.

> **프로젝트 Phase 구분**
> - **Phase 1 (분석)** — 문제정의·페르소나·JTBD 등 (✅ 완료, `Phase 1/3.Analysis/` 12개 문서)
> - **Phase 2 (설계)** — **PDD → SRS → 상세 Task** (← 현재 단계, 본 폴더부터 시작)
> - **Phase 3 (개발)** — 프로토타이핑 → 구현 → 안정화

---

## 문서 구성

| # | 파일 | 프로세스 | 상태 | 우선순위 근거 |
|---|------|---------|:---:|--------------|
| 0 | [0.PDD_template.md](0.PDD_template.md) | (재사용 템플릿) | — | — |
| 1 | [1.process_order_consolidation.md](1.process_order_consolidation.md) | 수주 정보 통합 | Draft v1.0 | GAP=4, DOS=4.0 (1순위) |
| 2 | [2.process_vulcanization_scheduling.md](2.process_vulcanization_scheduling.md) | 성형 공정 스케줄링 | Draft v1.0 | GAP=4, DOS=4.0 (성형 제약 검증) |
| 3 | [3.process_extrusion_scheduling.md](3.process_extrusion_scheduling.md) | 압출 공정 스케줄링 | Draft v1.0 | GAP=4, DOS=4.0 (성형-압출 연동) |
| **4** | **[4.PDD_master_integrated_Opus_final.md](4.PDD_master_integrated_Opus_final.md)** | **3개 PDD 통합본 + PRD + ERD + ADR (Master)** | **Draft v1.5** | **SRS·구현·테스트 단일 기준 문서** (PDD + PRD + ERD + ADR + Cheatsheet) |
| 5 | [5.PDD_master_Gemini.md](5.PDD_master_Gemini.md) | 순수 PRD (Gemini 작성) | Draft v0.1 | 비교/참조용 — 핵심 강점은 4번에 흡수 |

---

## 작성 규칙 (요약)

### IEEE 12207에서 가져온 3-요소
각 PDD는 다음 3개 핵심 절을 **반드시** 포함한다.

1. **Purpose** — "The purpose of … is to …" 1~3문장
2. **Outcomes** — a)~g) 검증 가능한 결과 목록
3. **Activities and Tasks** — Activity(동사형 그룹) → Task("shall / should …" 단위 행위) 2단 계층

### BPMN 2.0에서 가져온 표기 규칙
- **Descriptive Sub-Class**만 사용 (Task / Sub-Process / Start·End Event / 기본 Gateway / Sequence Flow / Message Flow / Data Object)
- **Pool**은 조직 경계: 사내 vs 외부(고객사·영업·관리부서)
- **Lane**은 역할/시스템 단위
- **Message Flow**(점선)는 Pool 간에만, **Sequence Flow**(실선)는 Pool 내부
- Mermaid `flowchart`로 1차 임베드 → 향후 `.bpmn` 정식 파일 별첨

### 트레이서빌리티
모든 Outcome은 §10.3 Traceability Matrix를 통해 다음과 연결:
- **위(Why)**: 마스터 문제정의서, JTBD 결과, GAP 분석
- **아래(What/How)**: 향후 SRS 항목 ID (`SRS-FR-XX-NNN`)

---

## 12207 매핑

본 프로젝트의 3개 프로세스는 모두 **§6.4 Technical Processes** 계열로 매핑되며, 세부적으로는 다음 표준 프로세스의 사내 특화 인스턴스이다.

| PDD | 12207 매핑 | 특성 |
|-----|-----------|------|
| PDD-01 수주 통합 | §6.4.1 Stakeholder Requirements Definition + §6.4.9 Operation | 외부 요구사항 수집 + 운영성 |
| PDD-02 성형 스케줄링 | §6.4.9 Operation | 핵심 운영 프로세스 |
| PDD-03 압출 스케줄링 | §6.4.9 Operation | PDD-02 연동 운영 프로세스 |

---

## 문서 사용 가이드

- **통합본(`4.PDD_master_integrated.md`)** = 정본(Master). SRS·테스트 진입 시 1차 기준 문서.
- **개별 PDD(`1.~3.`)** = 상세 부록. 각 프로세스의 9개 Activity / 27개 Task / 11개 BR / BPMN 원본을 확인할 때 참조.

## 다음 단계

1. **통합본 리뷰** — 횡단 룰(BR-X01~07)·데이터 사전·End-to-End BPMN의 적합성 검토
2. **정식 `.bpmn` 파일** — Camunda Modeler로 작성, `/diagrams/PDD-NN.bpmn` 별첨
3. **Phase 2/2.SRS 작성 진입** — 본 통합본의 Activities/Tasks·BR-X를 기능요구(FR)·비기능요구(NFR)로 분해
4. **Phase 2/3.Tasks 도출** — SRS 기반 개발 백로그 작성 후 Phase 3 진입

---

## 참조 표준 / 상위 문서

| 분류 | 문서 |
|------|------|
| 표준 | ISO/IEC/IEEE 12207:2008 (Software life cycle processes) |
| 표준 | OMG BPMN 2.0 (formal-13-12-09) |
| 상위 문제정의 | `Phase 1/3.Analysis/12.problem_statement_master.md` |
| JTBD 결과 | `Phase 1/3.Analysis/10.jtbd_interview_results.md` |
| Pain/Goal GAP | `Phase 1/3.Analysis/7.persona_pain_goal_analysis.md` |
| 페르소나 | `Phase 1/3.Analysis/6.persona_spectrum.md` |
| KPI | `Phase 1/3.Analysis/3.kpi_definition.md` |
| CSF | `Phase 1/3.Analysis/2.critical_success_factors.md` |
