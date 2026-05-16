# TASK-003 — Phase 3 Gantt Chart (v1.0)

> WBS v1.2 §12 의존성 매트릭스 + §12.1 Critical Path + §12.2 병렬 실행 그룹 기반 시각화.
> 작성일: 2026-05-16 / 기준 시나리오: **C (3 dev + 0.5 QA, 11주)** / 시작: 2026-05-18 (월)
>
> 본 차트는 **병렬·독립 진행 가능 영역** 을 한눈에 보이게 설계. Mermaid Gantt + 매트릭스 표 + Critical Path 강조 3 view 제공.

---

## 1. 최상위 Gantt — Sprint 0 ~ 5 + 횡단 + NFR

```mermaid
gantt
    title Phase 3 Phase 1.0 — Sprint 0 ~ 5 (시나리오 C 기준 11주)
    dateFormat YYYY-MM-DD
    axisFormat %m/%d
    excludes weekends

    section ★ Critical Path
    EP-00 인프라+EP-99 마스터        :crit, cp0, 2026-05-18, 5d
    EP-01 수주 Parser                :crit, cp1, after cp0, 4d
    EP-02 중복 감지                  :crit, cp2, after cp1, 3d
    EP-03 Diff·알림                  :crit, cp3, after cp2, 3d
    EP-04 슬롯 O/X                   :crit, cp4, after cp3, 3d
    EP-05 회전 배치                  :crit, cp5, after cp4, 4d
    EP-21 v1.4 좌·우·호기·앵글       :crit, cp21, after cp5, 3d
    EP-07 D-1 역산                   :crit, cp7, after cp21, 3d
    EP-08 압출 수식                  :crit, cp8, after cp7, 4d
    EP-09 셋팅 그룹                  :crit, cp9, after cp8, 3d
    EP-12-INFRA cross-master VIEW    :crit, cp12i, after cp9, 2d
    EP-EX13 cascade                  :crit, cpx13, after cp12i, 2d
    EP-EX14 WebSocket PUSH           :crit, cpx14, after cpx13, 2d
    EP-10 확정 게이트                :crit, cp10, after cpx14, 4d
    EP-11 Audit                      :crit, cp11, after cp10, 3d
    EP-13 v1.4 당일 락               :crit, cp13, after cp11, 3d
    EP-15 시뮬뷰                     :crit, cp15, after cp13, 5d
    EP-E2E E2E 베타                  :crit, cpe2e, after cp15, 5d

    section S0 병렬 (~5/22)
    EP-30 Keycloak 컨테이너          :s0a, 2026-05-18, 5d
    EP-32 Jenkins+Harbor             :s0b, 2026-05-18, 5d
    EP-31 Actuator+Prometheus skel.  :s0c, 2026-05-18, 5d
    EP-33 Docker Compose STG         :s0d, 2026-05-18, 5d
    EP-34 KST+dual-review skeleton   :s0e, 2026-05-18, 5d
    EP-46 인벤토리·SBOM 베이스라인   :s0f, 2026-05-18, 5d

    section S1 병렬 (~6/05)
    EP-42 RBAC 보강 (Keycloak 후속)   :s1a, after cp0, 6d
    EP-44 관측성 분산                :s1b, after s0c, 4d

    section S2 병렬 (~6/19)
    EP-06 D-2 역산 (slack 3PD)        :s2a, after cp5, 5d
    EP-VC15 충돌 ≥3 대안              :s2b, after cp21, 3d
    EP-VC16 On-Demand 검사            :s2c, after s2b, 3d
    EP-40 PER 측정 분산               :s2d, after cp4, 8d
    EP-44 OPS 보강                    :s2e, after s2d, 5d

    section S3 병렬 (~7/03)
    EP-EX11 압출 게이트               :s3a, after cp8, 3d
    EP-EX12 압출 대안                 :s3b, after s3a, 3d
    EP-43 사용성 분산                 :s3c, after cp7, 6d

    section S4 병렬 (~7/17)
    EP-12 역-Export (slack 20+PD)     :s4a, after cp3, 5d
    EP-14 신규 라인 우선 (slack 8PD)  :s4b, after cp9, 4d
    EP-41 신뢰성 NFR (MES 폴백)       :s4c, after cp11, 4d
    EP-42 보안 RBAC 마무리            :s4d, after cp10, 3d
    EP-44 운영 마무리                 :s4e, after cp11, 3d

    section S5 병렬 (~7/31)
    EP-16 카톡 백업                   :s5a, after cp3, 3d
    EP-17 매트릭스 뷰                 :s5b, after s4a, 4d
    EP-18 다중 후보 ranking           :s5c, after cp9, 4d
    EP-19 마스터 복원 UI              :s5d, after cp11, 3d
    EP-20 영업 자동 송신              :s5e, after cp3, 3d
    EP-40 부하 테스트                 :s5f, after cp15, 5d
    EP-43 UX 마무리                   :s5g, after cp15, 4d
    EP-45 호환·확장 NFR               :s5h, after cp15, 4d
    EP-46 비용 NFR                    :s5i, after cp15, 3d
    EP-47 사업 KPI 통합 대시보드      :s5j, after cp15, 4d

    section Deferred (Phase B+)
    EP-22 우선순위 큐                 :milestone, def22, 2026-08-03, 0d
    EP-23 KD 보충                     :milestone, def23, 2026-08-03, 0d
```

---

## 2. Sprint 마일스톤 + 게이트

```mermaid
gantt
    title Sprint 마일스톤 + DoD 게이트
    dateFormat YYYY-MM-DD
    axisFormat %m/%d
    excludes weekends

    section Sprint 경계
    S0 1주 Foundation        :s0blk, 2026-05-18, 5d
    S0 DoD 게이트            :milestone, s0gate, 2026-05-22, 0d
    S1 2주 수주 통합         :s1blk, after s0gate, 10d
    S1 DoD 게이트            :milestone, s1gate, 2026-06-05, 0d
    S2 2주 성형 핵심         :s2blk, after s1gate, 10d
    S2 DoD 게이트            :milestone, s2gate, 2026-06-19, 0d
    S3 2주 압출 핵심         :s3blk, after s2gate, 10d
    S3 DoD 게이트            :milestone, s3gate, 2026-07-03, 0d
    S4 2주 거버넌스          :s4blk, after s3gate, 10d
    S4 DoD 게이트            :milestone, s4gate, 2026-07-17, 0d
    S5 2주 UI·E2E·NFR        :s5blk, after s4gate, 10d
    Phase 1.0 베타 GO        :milestone, beta, 2026-07-31, 0d
```

---

## 3. Critical Path 단일 흐름 (직선)

> §12.1 정의 — 어떤 Epic 이라도 지연 시 전체 일정 지연. 총 **120 SP · 84 PD**.

```mermaid
flowchart LR
    CP0["S0<br>EP-00 + EP-99<br>13 SP"]:::cp
    CP1["S1<br>EP-01→02→03<br>26 SP"]:::cp
    CP2["S2<br>EP-04→05→21<br>26 SP"]:::cp
    CP3["S3<br>EP-07→08→09→12I<br>21 SP"]:::cp
    CP4["S3~S4<br>EP-EX13→EX14<br>6 SP"]:::cp
    CP5["S4<br>EP-10→11→13<br>18 SP"]:::cp
    CP6["S5<br>EP-15→E2E<br>10 SP"]:::cp
    GO["✅ Phase 1.0 베타 GO<br>2026-07-31"]:::go

    CP0 --> CP1 --> CP2 --> CP3 --> CP4 --> CP5 --> CP6 --> GO

    classDef cp fill:#fecaca,stroke:#b91c1c,stroke-width:2px,color:#000
    classDef go fill:#bbf7d0,stroke:#166534,stroke-width:3px,color:#000
```

---

## 4. Sprint 별 병렬 실행 매트릭스

> §12.2 정의 — 같은 Sprint 내 동시 실행 가능 Epic. 시나리오 C (3 dev + 0.5 QA) 기준 배치.

### Sprint 0 (1주, 5 영업일) — 2026-05-18 ~ 22

| Dev / 시간 | 일1 (월) | 일2 (화) | 일3 (수) | 일4 (목) | 일5 (금) |
|:---:|:---:|:---:|:---:|:---:|:---:|
| **Dev1** | EP-00 Spring Modulith 7 모듈 셋업 → → → → DoD |
| **Dev2** | EP-99 마스터 검증 + EP-34 KST baseline |
| **Dev3** | EP-30 Keycloak 컨테이너 + EP-32 Jenkins+Harbor+SonarQube+Trivy |
| **QA 0.5** | EP-31 Actuator skel + EP-33 Docker Compose STG + EP-46 SBOM 베이스라인 |

**S0 DoD 게이트** — TK-00-1-1 baseline (`app·audit·master` schema + 3 role) · TK-00-2-3 ArchUnit 7 모듈 검증 · Keycloak 컨테이너 healthy · Jenkins 빌드 1회 성공.

### Sprint 1 (2주, 10 영업일) — 2026-05-25 ~ 06-05

| 그룹 | Epic | 인력 | 의존 |
|---|---|:--:|---|
| **A (Critical)** | EP-01 → EP-02 → EP-03 (직렬 26 SP) | Dev1+Dev2 | S0 완료 |
| **B (병렬)** | EP-42 RBAC 보강 + EP-44 관측성 분산 | Dev3 | EP-30 완료 |
| **C (병렬)** | EP-46 비용 NFR 보강 (S0 잔여) | QA 0.5 | — |

### Sprint 2 (2주, 10 영업일) — 2026-06-08 ~ 19 ⭐ **부하 큼**

| 그룹 | Epic | 인력 | 의존 |
|---|---|:--:|---|
| **A (Critical)** | EP-04 → EP-05 → EP-21 (직렬 26 SP, v1.4 신규 ⭐) | Dev1+Dev2 | EP-01·EP-99 |
| **B (병렬)** | EP-06 D-2 역산 (slack 3PD) | Dev3 | EP-05 |
| **C (병렬)** | EP-VC15 충돌 대안 + EP-VC16 On-Demand 검사 | Dev3 | EP-21 |
| **D (병렬)** | EP-40 PER 측정 (생성·압출 SLO) + EP-44 OPS 보강 | QA 0.5 | EP-31 |

### Sprint 3 (2주, 10 영업일) — 2026-06-22 ~ 07-03

| 그룹 | Epic | 인력 | 의존 |
|---|---|:--:|---|
| **A (Critical)** | EP-07 → EP-08 → EP-09 → EP-12-INFRA (직렬 21 SP) | Dev1+Dev2 | EP-06 |
| **B (Critical)** | EP-EX13 cascade + EP-EX14 WebSocket PUSH (6 SP) | Dev2 (말미) | EP-09 |
| **C (병렬)** | EP-EX11 압출 게이트 + EP-EX12 압출 대안 | Dev3 | EP-08 |
| **D (병렬)** | EP-43 사용성 NFR 분산 | QA 0.5 | — |

### Sprint 4 (2주, 10 영업일) — 2026-07-06 ~ 17

| 그룹 | Epic | 인력 | 의존 |
|---|---|:--:|---|
| **A (Critical)** | EP-10 → EP-11 → EP-13 (직렬 18 SP, v1.4 당일 락 ⭐) | Dev1+Dev2 | EP-EX14·EP-30 |
| **B (병렬)** | EP-12 역-Export (slack 20+PD) + EP-14 신규 라인 우선 (slack 8PD) | Dev3 | EP-01·EP-09 |
| **C (병렬)** | EP-41 신뢰성 (MES 폴백) + EP-42 보안 마무리 + EP-44 운영 마무리 | QA 0.5 | — |

### Sprint 5 (2주, 10 영업일) — 2026-07-20 ~ 31

| 그룹 | Epic | 인력 | 의존 |
|---|---|:--:|---|
| **A (Critical)** | EP-15 시뮬뷰 + EP-E2E 베타 (10 SP) | Dev1 | EP-13 |
| **B (병렬, Should)** | EP-16 카톡 백업 + EP-17 매트릭스 뷰 | Dev2 | EP-03·EP-12 |
| **C (병렬, Could)** | EP-18 다중 후보 + EP-19 마스터 복원 + EP-20 영업 자동 송신 | Dev3 | EP-09·EP-11·EP-01 |
| **D (NFR 마무리)** | EP-40 부하 + EP-43 UX + EP-45 호환·확장 + EP-46 비용 + EP-47 KPI 대시보드 | QA + 전원 | — |

**Phase 1.0 베타 GO** — 2026-07-31 (금)

---

## 5. 횡단 Epic 분포 (Sprint 0~5)

> EP-30·31·32·33·34 는 Sprint 0 에 골격 + 전 Sprint 분산 검증·보강.

```mermaid
gantt
    title 횡단 Epic 분포 (전 Sprint 분산)
    dateFormat YYYY-MM-DD
    axisFormat %m/%d
    excludes weekends

    section EP-30 인증
    Keycloak 컨테이너+SAML/OIDC      :active, ep30a, 2026-05-18, 5d
    RBAC 4 role 매핑                  :ep30b, after ep30a, 6d
    PROD 통합 검증                    :ep30c, 2026-07-06, 10d

    section EP-31 관측성
    Actuator+Prometheus skel.         :ep31a, 2026-05-18, 5d
    Loki+Grafana+Slack alert          :ep31b, 2026-06-08, 10d
    KPI 17개 대시보드                 :ep31c, 2026-07-20, 10d

    section EP-32 CI/CD
    Jenkins+Harbor+SonarQube+Trivy    :ep32a, 2026-05-18, 5d
    PR 게이트 (ADR-015)               :ep32b, after ep32a, 5d
    Nightly 회귀                      :ep32c, 2026-07-06, 20d

    section EP-33 배포
    Docker Compose STG                :ep33a, 2026-05-18, 5d
    Blue/Green NGINX upstream         :ep33b, 2026-06-22, 10d
    pg_basebackup+WAL+PITR 드릴       :ep33c, 2026-07-20, 10d

    section EP-34 횡단 CO
    KST 통일 (Spring+DB+UI)            :ep34a, 2026-05-18, 5d
    BR-X05 dual-review (EP-10 결합)   :ep34b, 2026-07-06, 5d
    BR-X06 MES 폴백 (EP-41 결합)      :ep34c, 2026-07-13, 5d
```

---

## 6. NFR Epic 분포 (EP-40~47)

> 158 파일 8 Epic 60 NFR — Sprint 0 베이스라인 + 분산 측정.

```mermaid
gantt
    title NFR Epic 분포 (EP-40~47)
    dateFormat YYYY-MM-DD
    axisFormat %m/%d
    excludes weekends

    section EP-40 PER
    PER 측정 인프라 (k6+Gatling)      :ep40a, 2026-06-08, 10d
    부하 시나리오 5종                 :ep40b, 2026-07-20, 10d

    section EP-41 REL
    인프라 (HA·재시작)                :ep41a, 2026-05-18, 5d
    MES 폴백 (BR-X06)                 :ep41b, 2026-07-06, 10d
    DR 드릴                           :ep41c, 2026-07-20, 10d

    section EP-42 SEC
    인프라 (TLS·시크릿)               :ep42a, 2026-05-18, 5d
    RBAC 보강 (S1+S4)                 :ep42b, 2026-05-25, 25d
    침투 테스트                       :ep42c, 2026-07-20, 10d

    section EP-43 USA
    UX 가이드라인                     :ep43a, 2026-06-22, 10d
    접근성 검증                       :ep43b, 2026-07-20, 10d

    section EP-44 OPS
    Actuator+Loki 베이스라인          :ep44a, 2026-05-18, 5d
    KPI 17개 측정 분산                :ep44b, 2026-06-08, 30d
    운영 매뉴얼                       :ep44c, 2026-07-06, 10d

    section EP-45 COM
    API 버저닝+호환 매트릭스          :ep45, 2026-07-20, 10d

    section EP-46 COS
    인벤토리·SBOM·라이선스            :ep46, 2026-05-18, 5d

    section EP-47 KPI
    측정 인프라 (베이스라인)          :ep47a, 2026-05-18, 5d
    19 KPI 통합 대시보드              :ep47b, 2026-07-20, 10d
```

---

## 7. 병렬·독립 진행 가능 영역 요약 (한눈에)

### 7.1 시작 즉시 가능 (S0 D1, 의존 0)
| Epic | 비고 |
|---|---|
| EP-00 | 인프라 (모든 후속 선행) |
| EP-99 | 마스터 정비 (EP-04 선행) |
| EP-30 | Keycloak 컨테이너 |
| EP-31 | Prometheus skeleton |
| EP-32 | Jenkins+Harbor 컨테이너 |
| EP-33 | Docker Compose STG |
| EP-34 | KST baseline (Spring+DB+UI 3 layer) |
| EP-46 | 인벤토리·SBOM (라이선스 baseline) |

### 7.2 Critical Path 외 Slack 보유 Epic

| Epic | Slack | 시작 가능 시점 |
|---|---|---|
| EP-06 D-2 역산 | 3 PD | EP-05 종료 후 (~S2 후반) |
| EP-12 역-Export | **20+ PD** | EP-01 종료 후 (S1 말~S4 어디든) |
| EP-14 신규 라인 우선 | 8 PD | EP-09 종료 후 (S3 말~S4 어디든) |
| EP-16·17·18·19·20 | 5~10 PD | S5 내 자유 |
| NFR EP-40~47 | 분산 | 선행 충족 후 어디든 |
| 횡단 EP-30~34 | 분산 | Sprint 0~5 자유 |

### 7.3 의존 없는 병렬 쌍 (같은 Dev 가 swap 가능)

| 쌍 | Sprint | 조건 |
|---|:--:|---|
| EP-12 ↔ EP-14 | S4 | 둘 다 Critical Path 외, 의존 다름 |
| EP-16 ↔ EP-17 ↔ EP-18 ↔ EP-19 ↔ EP-20 | S5 | Should/Could 등급 — 5개 모두 독립 |
| EP-VC15 ↔ EP-VC16 | S2 | EP-21 후 직렬 (15→16) |
| EP-EX11 ↔ EP-EX12 | S3 | EP-08 후 직렬 (11→12) |
| EP-41 ↔ EP-42 ↔ EP-44 | S4 | NFR 분산 — 의존 다름 |

---

## 8. 위험 신호 (지연 시 cascade)

| Epic | 지연 시 영향 | 임계 |
|---|---|:--:|
| EP-00 | S0~S5 전체 차단 | ⛔⛔⛔ |
| EP-01 | S1~S5 전체 차단 | ⛔⛔⛔ |
| EP-05 | S2·S3·S4·S5 차단 | ⛔⛔ |
| EP-09 | S4·S5 차단 | ⛔⛔ |
| EP-EX13·EX14 | S4 EP-10 지연 | ⛔ |
| EP-10 | S4·S5 차단 | ⛔⛔ |
| EP-15 | Phase 1.0 베타 GO 지연 | ⛔⛔⛔ |

**위험 완화** — Critical Path Epic 은 Dev1+Dev2 (시니어) 페어로 배치. 슬랙 보유 Epic 만 Dev3 (주니어·신규) 단독 배치 권고.

---

## 9. 시나리오 비교 (WBS §12.2 v1.2)

| 시나리오 | 인력 | Capacity / Sprint | 총 SP | Sprint 수 | 총 기간 | 평가 |
|:--:|:--:|:--:|:--:|:--:|:--:|---|
| A | 2 dev | 35 SP | 261 SP | 7.5 | **15주** | ⚠️ Phase 1.0 10주 목표 초과 |
| B | 3 dev | 50 SP | 261 SP | 5.3 | **11주** | ✓ 거의 목표 |
| **C (권장)** | **3 dev + 0.5 QA** | **55 SP** | **261 SP** | **4.8** | **10주** | ✓ 목표 충족 (본 차트 기준) |
| D | 4 dev + 1 QA | 70 SP | 261 SP | 3.8 | **8주** | 여유 |

> 본 차트는 시나리오 C 기준 + S0 (1주) 포함 = **총 11주**. WBS §12.2 의 "10주" 는 S1~S5 만 (4.8 × 2주 ≈ 10주) — S0 포함 시 11주.

---

## 10. 사용 가이드

- **PM·공장장** — §1 최상위 Gantt + §3 Critical Path 만 확인 (5분)
- **개발 리드** — §4 Sprint 별 병렬 매트릭스 + §7 병렬 가능 영역 + §8 위험 신호 (15분)
- **QA·운영** — §5 횡단 + §6 NFR 분포 (10분)
- **신규 합류자** — §2 마일스톤 + §3 Critical Path + 본인 담당 Sprint §4 (20분)

---

## 11. 참조 + 갱신 정책

- 기준 WBS — [TASK-001_WBS_v1.2.md](TASK-001_WBS_v1.2.md) §12·12.1·12.2
- 시작 일자 변경 — 본 파일을 in-place 수정 금지, `TASK-002_Gantt_Chart_v1.1.md` 신규 발행
- 인력 시나리오 변경 (A·B·D) — 동일 (신규 파일)
- v1.4 신규 Epic (EP-21·EP-13) ⭐ 강조 유지

---

## 12. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-16 | (작성자) | 초안 — WBS v1.2 §12 의존성 매트릭스 + §12.1 Critical Path + §12.2 병렬 그룹 시각화. Mermaid Gantt 6 + 매트릭스 표 4 + Critical Path flowchart 1. 시나리오 C 기준 (3 dev + 0.5 QA, 11주). 시작 2026-05-18 → 2026-07-31 베타 GO. |
