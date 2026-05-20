# Sprint 2 진입 계획 (성형 가류 핵심)

**Sprint**: S2 | **목표 기간**: 2026-05-21 ~ (2주, AI 가속 시 ~3일) | **상태**: 🔄 진입 게이트
**작성**: 2026-05-21 | **상위 참조**: [Sprint-1_Completion_v1.0.md](Sprint-1_Completion_v1.0.md) §10·12, [WBS v1.2 §5](../../Phase%202/4.Tasks/TASK-001_WBS_v1.2.md)

> Sprint 1 (수주 통합 + 중복 감지 + Diff/알림 + RBAC) 종료 직후 진입.
> Sprint 2 = **성형 (VC) 가류 스케줄링 핵심** — BR-V07~V17 + LP/IC 가류기 + 회전수 + 슬롯 O/X.

---

## 1. Sprint 2 목표 (PDD-MASTER v1.7 + SRS v1.5 § VC)

- **성형 슬롯 O/X 검증** (M-04) — 47품번 × 5 가류기(LP 4 + IC 1) × 셋팅그룹 1~8 가능성 결정.
- **회전수 배치** (M-05) — 주간 8 + 야간 10 = 18 회전 × LP 4 = 72 슬롯/일 + IC GreedyRotation.
- **D-2 영업일 역산** (M-06) — BR-X07 hard 제약 (D-2 이후 신규 수주 거부).
- **VC v1.4 신규 제약** — BR-V14·V15·V16·V17 (좌/우 호기·앵글 상한·규격<7 분기, 합금형 composite 1·2·3·6).
- **충돌 리포트 + On-Demand 검사** — REQ-FUNC-VC-015·016.
- **EP-34 carry-over** — Dual-review (BR-X05) + KST UI (BR-X04 — UI 컴포넌트 단위 검증).

---

## 2. Sprint 2 Epic·SP 매트릭스

| Epic | 제목 | SP | 의존 (선행) | 핵심 산출 |
|---|---|:--:|---|---|
| **EP-04** ⭐ | 성형 슬롯 O/X 결정 | 8 | EP-01·EP-99 | `SlotAvailabilityService` + 47품번 × 5 가류기 매트릭스 + BR-V13 KD 보충 룰 |
| **EP-05** ⭐⭐ | 회전수 배치 (GreedyRotation) | 13 | EP-04 | `RotationAllocator` + LP/IC 라우팅 + 1주 horizon scheduler |
| **EP-06** | D-2 영업일 역산 | 3 | EP-05 | `BusinessDayCalculator` + BR-X07 hard guard |
| **EP-21** ⭐ | 좌/우·호기·앵글상한·규격<7 (v1.4) | 13 | EP-04·EP-99 | `VcConstraintEngine` (BR-V14·V15·V16·V17) + ADR-017 LISTEN/NOTIFY 룰 캐시 |
| **EP-VC15** | 충돌 리포트 (≥3 distinct) | 3 | EP-21 | `ConflictReportService` + p95 ≤3s |
| **EP-VC16** | On-Demand 검사 endpoint | 2 (S2~S3) | EP-05 | `POST /api/v1/vc/check-on-demand` |
| **EP-34 ST-34-1** | Dual-review (BR-X05) | 4 | EP-99 산출 | `MasterChangeReviewService` + master_change_request 테이블 |
| **EP-34 ST-34-3** | KST UI 통합 (BR-X04 FE) | 4 | EP-00 React | UI 시각 표시 KST 강제 + dayjs Asia/Seoul plugin 통합 검증 |

**합계**: **~50 SP** (Sprint 2 capacity 30 PD = 50 SP velocity 기준). EP-04 + EP-05 가 critical path (21 SP).

---

## 3. 의존성 그래프

```
Sprint 1 (수주 통합)
       │
       └──► EP-04 (슬롯 O/X) ⭐
              │
              ├──► EP-05 (회전수 배치) ⭐⭐
              │      │
              │      ├──► EP-06 (D-2 역산)
              │      └──► EP-VC16 (On-Demand)
              │
              └──► EP-21 (v1.4 제약)
                     │
                     └──► EP-VC15 (충돌 리포트)

EP-99 (마스터) ─────► EP-34 ST-34-1 (Dual-review)
EP-00 (React) ─────► EP-34 ST-34-3 (KST UI)
```

Critical Path: **EP-04 → EP-05 → EP-VC15** (~24 SP, ~15 PD).

---

## 4. 권장 진행 순서 (AI 가속 vibe coding)

| 단계 | Epic·Story | 비고 |
|---|---|---|
| **Phase A** (Day 1) | EP-04 ST-04-1 (슬롯 SO/SX 매트릭스) | 47품번 마스터 + 5 가류기 + 셋팅그룹 1~8 → JPA 모델 |
| | EP-04 ST-04-2 (BR-V13 KD 보충 룰) | KD 발주 보충 — Confirmed Slot O 가능 시 자동 활용 |
| | EP-04 ST-04-3 (회귀 100% — Testcontainers PG) | Slot O/X 매트릭스 검증 |
| **Phase B** (Day 1~2) | EP-05 ST-05-1 (LP 회전수 18) | 주간 8 + 야간 10 — 시간 슬롯 |
| | EP-05 ST-05-2 (IC GreedyRotation) | LP 부하 ≥75% 시 IC 보충 |
| | EP-05 ST-05-3 (라우팅 + 1주 horizon) | 1500 row × 30 col target |
| | EP-05 ST-05-4 (NFR-PER-001 부하 검증) | p95 <800ms |
| **Phase C** (Day 2) | EP-21 ST-21-1~5 (v1.4 제약 5건) | BR-V14·V15·V16·V17 + ADR-017 LISTEN/NOTIFY |
| **Phase D** (Day 2~3) | EP-06 (D-2 역산) + EP-VC15 (충돌 리포트) + EP-VC16 (On-Demand) | |
| **Phase E** (Day 3) | EP-34 ST-34-1 (Dual-review) + ST-34-3 (KST UI) | carry-over |
| **Phase F** (Day 3 종료) | Sprint 2 회고 + Sprint 3 진입 plan | |

---

## 5. 신규 데이터베이스 마이그레이션 (예상 V007~V012)

| Migration | 테이블 | Epic·Task |
|---|---|---|
| V007 | `app.vc_slot_availability` (47품번 × 5 가류기 SO/SX) | EP-04 ST-04-1 |
| V008 | `app.vc_rotation_schedule` (회전수 18 × 7일) | EP-05 ST-05-1 |
| V009 | `app.vc_assignment` (수주 → 가류기 슬롯 매핑) | EP-05 ST-05-3 |
| V010 | `master.master_change_request` (BR-X05 dual-review) | EP-34 ST-34-1 |
| V011 | `app.vc_constraint_violation` (충돌 리포트 영속) | EP-VC15 |
| V012 | `master.vc_constraint_v14_v17` (v1.4 제약 룰 캐시) | EP-21 |

> 모든 마이그레이션은 `backend/order` 또는 신규 `backend/vc/src/main/resources/db/migration/` 에 배치
> (Flyway 단일 classpath — 모듈 boundary 와 무관).

---

## 6. 신규 모듈 활성 (`com.scheduling.vc`)

`backend/vc/` 모듈은 Sprint 0/1 에서 placeholder 만 존재 (`OrderChangedListener`).
Sprint 2 에서 본격 구현:

```
backend/vc/src/main/java/com/scheduling/vc/
  domain/        — SlotAvailability, RotationSlot, VcAssignment
  service/       — SlotAvailabilityService, RotationAllocator, RoutingService
  rule/          — VcConstraintEngine (BR-V14·V15·V16·V17)
  api/           — VcController + DTOs
  events/        — VcAssignmentCompletedEvent (NamedInterface "events")
```

**Modulith 의존성** — `vc::events` 는 이미 notify·order 가 구독 가능.

---

## 7. Sprint 1 carry-over (Sprint 2 진행 중 병행)

| 항목 | 상태 | 책임 |
|---|---|---|
| Sprint 1 dual-review 32 commit code review | 대기 | 사용자 |
| Master report 6건 dual-review 사인오프 (Sprint 0+1 carry-over) | 대기 | P1 + STK-08 |
| TK-01-2-2 UAT (P4 페르소나 매핑 UI) | 대기 | STG 가동 시 |
| TK-03-3-3 k6 부하 — Jenkins CI 통합 | deferred | EP-32 Sprint 2 후속 |
| Keycloak realm v1.5 정책 — PROD fresh boot | deferred | PROD 배포 시 |

---

## 8. 운영 리스크 + 완화

| 리스크 | 영향 | 완화 |
|---|---|---|
| **EP-21 v1.4 신규 제약** 마스터 일관성 | BR-V14·V15·V16·V17 위반 시 잘못된 슬롯 배정 | EP-99 마스터 검증 회귀 SQL 재실행 (Sprint 0 산출) |
| **EP-05 1500 row × 30 col 부하** | NFR-PER-001 800ms 미충족 | EP-04 종료 직후 QueryDSL projection + index hint (jpa-query-optimization skill 활용) |
| **Modulith 모듈 경계** | vc 모듈이 order 도메인 직접 import 시 위반 | order::api 만 사용 (NamedInterface), ArchUnit 게이트 유지 |
| **Testcontainers PG startup 시간** | 매 IT 30s+ | shared container (Singleton container 패턴) 검토 |
| **STG 환경 미가동** | UAT + 데모 차단 | Sprint 2 중반까지 EP-33 ST-33-1 활성 또는 docker-compose.stg.yml local boot |

---

## 9. Sprint 2 진입 게이트 (Sprint 1 완료 기준 충족)

- [x] Sprint 1 = 29/29 Task 100% 완료
- [x] Modulith verify + ArchUnit 8 모듈 0 위반
- [x] Testcontainers IT 통과 (6 시나리오)
- [x] Production bug 5건 fix
- [x] Prometheus + Grafana + Alertmanager 운영 게이트
- [ ] dual-review carry-over — Sprint 2 진행 중 병행

→ **Sprint 2 진입 OK**.

---

## 10. Sprint 2 종료 조건 (Definition of Done)

- [ ] EP-04·05·06·21·VC15·VC16 — Story 모두 PASS (Task 단위 회귀)
- [ ] BR-V07·V12·V13·V14·V15·V16·V17 — 모든 제약 ArchUnit + Testcontainers IT 검증
- [ ] LP/IC 라우팅 1500 row × 30 col 부하 — p95 <800ms (k6 또는 단위 부하 테스트)
- [ ] Sprint Review 데모 — 47품번 × 7일 horizon 자동 배정 시연 (1 시나리오)
- [ ] Modulith 모듈 경계 유지 (8 모듈 + vc 본격 활성)
- [ ] V007~V012 마이그레이션 6건 Flyway 적용
- [ ] dual-review carry-over 청산

---

## 11. 다음 권장 Task (Sprint 2 진입 직후)

**Phase A — EP-04 ST-04-1 첫 Task** (TK-04-1-1 정도). 또는 사용자 결정에 따라:
- A. **EP-04 ST-04-1 진입 (TK-04-1-1)** — 슬롯 O/X 매트릭스 JPA 모델 (Recommended)
- B. **EP-34 ST-34-1 진입** — Dual-review carry-over 먼저 정리
- C. **Sprint 1 carry-over 청산 우선** — STG 환경 활성 + UAT

---

## 12. 개정 이력

| 버전 | 일자 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-21 | Claude Code (검토 대기) | 초안 — Sprint 1 100% 완료 직후 Sprint 2 진입 게이트 |
