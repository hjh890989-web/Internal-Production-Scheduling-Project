# Story Overview — [EP-99] ST-99-2 압출 마스터 B열(규격) 정합성 검증 + 47품번 규격 분포 분석

**Sprint**: S0 (Phase 0 사전 준비) | **Epic**: EP-99 마스터 데이터 정비 (선행 작업) | **Priority**: Must
**SP 합계**: 2 | **PD 추정**: ~1.4 PD (2 SP × 0.7 PD)

---

## Story 목적

> WBS §5.1 EP-99 인용: "Sprint 1 진입 전 인프라·마스터·CI/CD 셋업. 별도 Sprint로 분리하여 개발 흐름 차단 방지."

본 Story는 **v1.4 신규 BR-V17 + PDD-03 BR-E12 cross-process 룰이 강제하는 압출 마스터 데이터의 무결성을 Phase 1.0 개발 진입 전에 사전 검증**한다. 구체적으로:

1. 압출 마스터(`압출공정_제약조건.xlsx`) **B열(`규격`)** 47품번 모두 정수형 숫자로 정합한지 검증 (NULL·문자열·소수 비표준 값 등 탐지)
2. **규격<7 품번 식별·리스트 출력** — BR-V17 직접 영향 범위 (가류기당 동시 앵글 ≤4 제약 적용 대상). 사전 데이터 확인 결과 **약 7품번 (15.2%)** 예상 (규격=5)
3. **BR-V17 영향 품번 사전 점검**: 영향 7품번이 (a) PDD-02 회전 배치 시 자주 등장하는지, (b) `28422-2M800`·`28421-2M800` 등 다른 특수 제약 품번과 겹치는지, (c) 호싱불량률 통계와 연관 있는지 데이터 분석

**Why 본 Story가 Phase 0 필수 작업인가**:
- 본 Story가 통과하지 못하면 EP-21 ST-21-5 (S2 — 규격<7 가류기당 앵글 ≤4 cross-master 강제) 및 EP-12-INFRA (S3 — `v_product_with_spec` VIEW + Caffeine 캐시) 가 **잘못된 데이터로 빌드**됨 (WBS §12 의존성 DAG)
- SRS-RSK-001 (마스터 데이터 부정확) + SAD-RSK-010 (마스터 K/L·B열 무결성 누락) 직접 완화 (WBS §13.1)
- 압출 마스터는 PDD-03 자체와 PDD-02 BR-V17 **두 곳에서 cross-reference**되므로 정합성 영향 범위가 가장 큼

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-99-2-1](TK-99-2-1.md) | `압출공정_제약조건.xlsx` B열(규격) 정수형 무결성 검증 | 0.5 | QA + DBA | I + T-U | ☐ |
| [TK-99-2-2](TK-99-2-2.md) | 규격<7 품번 식별·리스트 출력 + 규격 분포 통계 | 0.5 | QA | A + T-U | ☐ |
| [TK-99-2-3](TK-99-2-3.md) | BR-V17 영향 품번 사전 점검 (수주 빈도·특수제약 중첩·호싱불량 연관) | 0.4 | QA + 백엔드 | A + I | ☐ |

> **선행 의존**: 없음 — ST-99-1과 병렬 진행 가능 (성형 마스터 vs 압출 마스터, 서로 독립)
> **후행 차단**:
> - EP-04 (S2 슬롯 O/X), EP-21 ST-21-5 (S2 — 규격<7 가류기당 ≤4), EP-12-INFRA (S3 — `v_product_with_spec` VIEW)
> - ST-99-1 통과 후 EP-99 전체 종료 (Sprint 0 DoD 항목)

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] **압출 마스터 B열 47품번(또는 실제 row 수) 모두 정수형** (NULL·문자열·기타 0건) — TK-99-2-1
- [ ] **규격<7 품번 리스트 확정 + 통계 리포트 산출**:
  - 예상: 약 7품번 (규격=5)
  - 분포 확인: 5 / 7 / 9 / 11 / 13.5 / 18 등 — TK-99-2-2
- [ ] **BR-V17 영향 7품번 운영 점검 완료**:
  - 수주 빈도 (월별 평균)
  - `28422-08HA0`·`28422-2M800`·`28421-2M800` (BR-V14·V15·V16 대상)과 중첩 여부
  - 호싱불량 통계와 연관성 — TK-99-2-3
- [ ] **dual-review 사인오프** (P1 김정훈 주임 + STK-08 IT lead) — BR-X05
- [ ] **As-Is 베이스라인 일자 기록** (Phase 0 시점 정합 상태 스냅샷, ST-99-1과 동일 형식)
- [ ] Sprint Review 데모 시연 — 압출 마스터 B열 100% 정합 + 규격<7 영향 리스트 + 운영 점검 결과

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.1 EP-99 ST-99-2
- **SRS REQ-FUNC**: `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.4.md` §4.1.2
  - REQ-FUNC-VC-027 ("압출 제약 마스터 `EX_CONSTRAINT.spec`(B열, `규격`) 값이 `< 7`인 hose_id에 대해 1대 가류기당 동시 앵글 점유 ≤ 4")
- **PDD BR**:
  - `Phase 2/1.PDD/2.process_vulcanization_scheduling_Opus.md` §9 **BR-V17** (규격<7 가류기당 앵글 ≤4)
  - `Phase 2/1.PDD/3.process_extrusion_scheduling_Opus.md` §9 **BR-E12** (cross-process 명시 — "압출 제약 마스터의 `규격`(B열)은 본 프로세스(`DI-02`) 외에 PDD-02 BR-V17 판정에도 cross-reference됨")
- **SAD ADR / VIEW**: `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` §10 **ADR-017** (성형↔압출 cross-master VIEW + Caffeine)
  - `CREATE VIEW master.v_product_with_spec AS SELECT p.*, ec.spec FROM master.PRODUCT p JOIN master.EX_CONSTRAINT ec USING (hose_id);`
- **SRS-RSK**: SRS §1.7.1 **SRS-RSK-001** "마스터 데이터 부정확 → 비현실적 스케줄"
- **SAD-RSK**: SAD §11 **SAD-RSK-010** "마스터 엑셀 K/L열·압출 B열 무결성 누락 시 신규 제약(BR-V14~V17) 무력화"
- **마스터 데이터 원본**: `Phase 1/2.Raw Materials/Extrusion/압출공정_제약조건.xlsx`
  - 46~47품번 × 16열 — A=HOSE·**B=규격**·C=내경·D=두께·E=압출셋팅·F=압출속도(m/m)·G=헤드/핀·H=합금형·I=재단길이(mm)·J=압출라인1·K=압출라인2
  - 헤더는 row 1 (성형 마스터 row 2와 다름 — Task 명세에서 주의)
- **연관 Story (병렬)**: [ST-99-1](../ST-99-1/_Story_Overview.md) (성형 마스터 K/L열) — 동일 패턴, 독립 진행 가능

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | ST-99-1 패턴 재사용으로 초안 작성 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.1 EP-99 ST-99-2 기반. ST-99-1 패턴 재사용 (성격: 마스터 검증 + 데이터 분석). 압출 마스터 B열 분포 사전 조사 결과 반영 (규격<7 = 7품번 15.2%) |
