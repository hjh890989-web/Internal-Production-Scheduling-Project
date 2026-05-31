# Sprint 26 진입 계획 — EP-VENDOR-INTEGRATION (Phase 5+ Foundation 첫 sprint) v1.0

**작성일**: 2026-06-01 | **버전**: 1.0 | **상태**: Phase 5+ Foundation 첫 sprint **조기 진입 모드 (S26-A / S26-B 분할)**

> **참조**: [PHASE-5_FOUNDATION_v1.0 §3 Sprint 26](PHASE-5_FOUNDATION_v1.0.md) + [PLAN-SPRINT-24 v1.1](PLAN-SPRINT-24_EP-OPS-FEEDBACK_v1.1.md) + [PLAN-SPRINT-25 v1.1](PLAN-SPRINT-25_EP-PROD-LAUNCH_v1.1.md) 분할 패턴 + [PLAN-SPRINT-23 v1.1 (EP-MES-ADAPTER-1)](PLAN-SPRINT-23_EP-MES-ADAPTER-1_v1.1.md)

---

## 0. v1.0 변경 요약 (Phase 5+ Foundation §3 Sprint 26 정합)

1. **Sprint 26 EP-VENDOR-INTEGRATION (~3.8 SP / 1.8 PD / 1 Day)** — Phase 5+ Foundation 첫 sprint. Phase 4 carry-over `Order 자동 INSERT chain` (Medium Phase 5+) 해소 + MES vendor DTO 골격 (mock contract 유지, Adapter 패턴 정합) + USER_MANUAL v1.6 갱신.
2. **S26-A 조기 진입 (data-free, 즉시 가능, ~3.8 SP / 1.8 PD / 1 Day)** — 사내 IT vendor spec 수신과 무관한 코드/문서 차원 작업 일괄. Sprint 24/25 PLAN v1.1 분할 패턴 (`fafa2db`) 재사용.
3. **S26-B carry-over (~1.0 SP, Phase 5+, 사내 IT vendor spec 수신 후)** — MES vendor DTO mapper 실 spec 적용 + JpaMesShiftPort ↔ HttpMesShiftClient 실 endpoint 회귀. mock contract DTO → 실 vendor DTO 전환 cascade.
4. **DoD (S26-A 한정 6건)** — OrderCommittedListener TODO 0건 (자동 chain 활성) / 자동 draft IT 2~3 case GREEN / config flag `scheduling.order.auto-draft.enabled` = false (default, 점진 활성) / USER_MANUAL v1.6 발행 / backend verifyAll BUILD SUCCESSFUL / Adapter 패턴 Hexagonal 정합 (`MesShiftPort` + 2 구현체).
5. **리스크 5건** — (1) 자동 chain 활성 후 priority/slot 알고리즘 호출 부하 증가, (2) BR-X02 audit `actor=system` 분리 정책, (3) `VcScheduleService.draftBatch` 호출 시점 (event publish 직후 vs scheduled), (4) mock contract DTO 유지 시 실 vendor spec 수신 후 mapper cascade 분량 증가, (5) config flag default false 시 사용자 활성 시점 결정 (Gate B 정합).

---

## 1. 목적 (early-entry 분할 명시)

**Phase 5+ Foundation §3 Sprint 26 정식 진입 조건 (사내 IT vendor spec 수신 + Keycloak realm 활성) 미충족이지만, code/contract/문서 차원 data-free 작업은 즉시 가능 → S26-A 조기 진입으로 Adapter 패턴 정합 + Order 자동 chain 활성 + USER_MANUAL v1.6 일괄 수행.** 사내 IT vendor spec 수신 + Phase 5+ 본격 진입 시점에 S26-B 정식 진입.

| 영역 | v1.0 baseline | **S26-A 조기 (지금)** | **S26-B carry-over (Phase 5+)** |
|---|---|---|---|
| MES vendor DTO | mock contract (Sprint 23) | **MesShiftPort 인터페이스 + 2 구현체 (HTTP/JPA) + config flag** | **실 vendor spec mapper 적용 + 실 endpoint 회귀** |
| Order 자동 chain | OrderCommittedListener TODO | **TODO 해소 + draftBatch 호출 + audit actor=system + IT 2~3 case** | — (S26-A 에서 완료) |
| config flag | 없음 | **`scheduling.order.auto-draft.enabled` = false (default)** | PROD 점진 활성 (사용자 결정) |
| USER_MANUAL | v1.5 | **v1.6 §1.2 자동 chain + §3.6 vendor placeholder + §7 이력** | v1.7 (vendor spec 수신 후) |

**S26-A 진입 효과:** Phase 4 carry-over (Order 자동 INSERT chain Medium) 즉시 해소 + Adapter 패턴 Hexagonal 정합 강화 → S26-B 진입 시 mock contract DTO → 실 vendor DTO 전환만으로 단축. PLAN-SPRINT-24/25 v1.1 분할 패턴 재사용으로 진입 일관성 확보.

---

## 2. Sprint 26-A SP·기간 (3.8 SP / 1.8 PD / 1 Day)

| Story | SP | PD | data-free |
|---|:--:|:--:|:--:|
| ST-VENDOR-1 MES vendor DTO 골격 (Adapter 패턴) | 1.0 | 0.5 | ✅ |
| ST-ORDER-1 Order 자동 INSERT chain 활성 | 1.5 | 0.7 | ✅ |
| ST-ORDER-2 자동 draft IT 신규 | 0.5 | 0.3 | ✅ |
| ST-ORDER-3 config flag scheduling.order.auto-draft.enabled | 0.3 | 0.1 | ✅ |
| ST-DOC-1 USER_MANUAL v1.6 §1.2 + §3.6 갱신 | 0.5 | 0.2 | ✅ |
| **S26-A 합계** | **3.8 SP** | **1.8 PD** | |
| ST-VENDOR-1-B vendor DTO mapper 실 spec 적용 (carry-over) | 1.0 | 0.5 | **Phase 5+ (S26-B)** |

---

## 3. 의존성 DAG

```
S26-A (지금, data-free)
  ├─ ST-VENDOR-1 MesShiftPort + 2 구현체 + config flag       (독립 병렬, 1 agent)
  │           (TK-VENDOR-1-1 ~ 1-4)
  │
  ├─ ST-ORDER-1 OrderCommittedListener TODO 해소 + draftBatch (1 agent, 병렬 가능)
  │           (TK-ORDER-1-1 ~ 1-4)
  │              ↓
  │           ST-ORDER-2 자동 draft IT (Testcontainers 2~3 case)
  │           (TK-ORDER-2-1 ~ 2-2)
  │              ↓
  │           ST-ORDER-3 config flag scheduling.order.auto-draft.enabled
  │           (TK-ORDER-3-1 ~ 3-2)
  │
  └─ ST-DOC-1 USER_MANUAL v1.6 §1.2 + §3.6 + §7              (마지막 1 agent)
              (TK-DOC-1-1 ~ 1-3)
                  ↓
            S26-A 산출물 commit/push (gitflow-commit)
                  ↓
 ━━━ 사내 IT vendor spec 수신 + Phase 5+ 본격 진입 ━━━
                  ↓
S26-B (Phase 5+)
  └─ ST-VENDOR-1-B mock contract DTO → 실 vendor DTO mapper 적용
              + JpaMesShiftPort ↔ HttpMesShiftClient 실 endpoint 회귀
                  ↓
            Sprint 27 EP-MQ-ADAPTER 진입 (Kafka/MQ/file adapter 확장, ~4 SP)
```

---

## 4. Story · Task 매트릭스 (각 Story 별 Task 2~4)

### ST-VENDOR-1 (1.0 SP) — MES vendor DTO 골격 (Adapter 패턴)

- **TK-VENDOR-1-1** `MesShiftPort` 인터페이스 + `MesShiftDto` record 분리 + Adapter 패턴 정합 (Hexagonal 강화) — Sprint 23 `MesShiftClient.fetchShift(machineId, shiftDate, shiftNo)` 시그니처 유지
- **TK-VENDOR-1-2** `HttpMesShiftClient` + `JpaMesShiftPort` 분리 + bean 선택 (config flag `scheduling.mes.adapter=jpa|http`, default `jpa`)
- **TK-VENDOR-1-3** mock contract DTO 명세 명시 (vendor spec 수신 전 placeholder 명확화 — `// @placeholder vendor spec 수신 후 mapper 갱신 (S26-B)` 주석)
- **TK-VENDOR-1-4** unit IT 회귀 (Sprint 23 WireMock IT 4 시나리오 GREEN 유지)

### ST-ORDER-1 (1.5 SP) — Order 자동 INSERT chain 활성

- **TK-ORDER-1-1** `OrderCommittedListener` TODO 해소 — `VcScheduleService.draftBatch(trackingId)` 호출
- **TK-ORDER-1-2** `VcScheduleService.draftBatch` 메서드 신규 또는 기존 활성 (priority/slot 알고리즘 호출)
- **TK-ORDER-1-3** BR-X02 audit 정합 (자동 INSERT 도 `actor=system` + `reason=auto-chain`)
- **TK-ORDER-1-4** `OrderChangedListener` TODO 해소 명시 (Sprint 2 TK-13-? carry-over 표기)

### ST-ORDER-2 (0.5 SP) — 자동 draft IT 신규

- **TK-ORDER-2-1** `OrderCommittedEvent` → `VcScheduleService` 자동 INSERT IT (Testcontainers, 2~3 case)
- **TK-ORDER-2-2** `audit_log` row 영속 검증 (`actor=system`, `br_id=BR-X02`)

### ST-ORDER-3 (0.3 SP) — config flag

- **TK-ORDER-3-1** `application.yml` `scheduling.order.auto-draft.enabled` (default `false`, PROD 점진 활성)
- **TK-ORDER-3-2** USER_MANUAL §1.2 config 안내 (TK-DOC-1-1 과 정합)

### ST-DOC-1 (0.5 SP) — USER_MANUAL v1.6

- **TK-DOC-1-1** USER_MANUAL v1.6 §1.2 PLANNER 자동 chain 흐름 + config flag 안내
- **TK-DOC-1-2** §3.6 vendor mapper placeholder + 실 spec 수신 후 v1.7 갱신 명시 (S26-B 트리거 명확화)
- **TK-DOC-1-3** §7 개정 이력 v1.6 entry

---

## 5. DoD (S26-A 한정 6건)

1. `OrderCommittedListener` TODO 0건 (자동 chain 활성)
2. 자동 draft IT 2~3 case GREEN (Testcontainers, BR-X02 audit row 검증 포함)
3. config flag `scheduling.order.auto-draft.enabled` = `false` (default, 점진 활성)
4. USER_MANUAL v1.6 발행 (§1.2 + §3.6 + §7 이력)
5. `./gradlew verifyAll` BUILD SUCCESSFUL (회귀 0건)
6. Adapter 패턴 Hexagonal 정합 — `MesShiftPort` 인터페이스 + 2 구현체 (`HttpMesShiftClient`, `JpaMesShiftPort`) + config flag `scheduling.mes.adapter`

---

## 6. 리스크 5건

1. **자동 chain 활성 후 priority/slot 알고리즘 호출 부하 증가** — 현재 PLANNER manual 호출만, 자동 호출 시 부하 증가 가능 → config flag default false 로 점진 활성, k6 측정 (Sprint 25 S25-B carry-over) 후 활성 시점 결정.
2. **BR-X02 audit 자동 INSERT `actor=system` 분리 정책** — PLANNER 가 명시 안 함, audit query 시 PLANNER vs system 구분 필요 → `actor=system` + `reason=auto-chain` 명시, audit 조회 UI 필터 옵션 (Phase 5+ 추가).
3. **`VcScheduleService.draftBatch` 호출 시점** — `OrderCommittedEvent` publish 직후 vs scheduled (`@Scheduled`) → 동기 호출 (publish 직후) 채택, 부하 분산 필요 시 Phase 5+ async 전환.
4. **mock contract DTO 유지 시 실 vendor spec 수신 후 mapper 갱신 cascade** — S26-B carry-over 분량 증가 가능 → 명세 placeholder 주석 명시 + S26-B Story 분리 (1.0 SP 추가).
5. **config flag default false 시 사용자 활성 시점 결정** — Gate B (Phase 5+ vendor spec 수신) 정합, 사용자가 활성 결정 → USER_MANUAL §1.2 활성 절차 명시 + Phase 5+ Foundation §3 Sprint 26 Gate B 회신 트리거 표기.

---

## 7. 작업 순서 (1 Day)

- **오전 (4h)**: ST-VENDOR-1 (1 agent) + ST-ORDER-1 (1 agent) 병렬 진행
- **오후 (3h)**: ST-ORDER-2 + ST-ORDER-3 (순차) + ST-DOC-1 (마지막 1 agent)
- **마감 (1h)**: `./gradlew verifyAll` + `tsc` (frontend lint) + commit 분리 (`feat(vc):` + `feat(order):` + `feat(common):` + `docs(manual):`)

---

## 8. 산출물

- **PLAN v1.0** (본 문서)
- `backend/vc/.../mes/MesShiftPort.java` (신규 인터페이스)
- `backend/vc/.../mes/HttpMesShiftClient.java` (기존, 리팩터)
- `backend/vc/.../mes/JpaMesShiftPort.java` (신규 또는 기존 리팩터)
- `backend/vc/.../OrderCommittedListener.java` (TODO 해소 + `draftBatch` 호출)
- `backend/vc/.../VcScheduleService.java` (`draftBatch(trackingId)` 메서드)
- `backend/app/src/test/.../OrderAutoDraftIT.java` (신규, Testcontainers 2~3 case)
- `backend/app/src/main/resources/application.yml` (`scheduling.order.auto-draft.enabled` + `scheduling.mes.adapter`)
- `docs/manual/USER_MANUAL_v1.6.md` (v1.5 → v1.6)

---

## 9. Sprint 26 후 다음 단계

- **Sprint 27 EP-MQ-ADAPTER** 진입 (Kafka/MQ/file adapter 확장, ~4 SP) — Adapter 패턴 정합 후 외부 시스템 확장
- **S26-B vendor DTO mapper 실 spec 적용** (사내 IT vendor spec 수신 후, ~1.0 SP) — mock contract → 실 vendor DTO 전환

---

## 10. 개정 이력

| 버전 | 일자 | 작성자 | 주요 변경 |
|---|---|---|---|
| **v1.0** | 2026-06-01 | Claude (Opus 4.7) | Sprint 26 EP-VENDOR-INTEGRATION Phase 5+ Foundation 첫 sprint PLAN 신규 작성 — S26-A 조기 진입 (~3.8 SP / 1.8 PD / 1 Day, data-free) + S26-B carry-over (~1.0 SP, Phase 5+ 사내 IT vendor spec 수신 후) 분할. Sprint 24/25 PLAN v1.1 분할 패턴 (`fafa2db`) 재사용. 5 Story × 14 Task DAG + DoD 6건 + 리스크 5건. |
