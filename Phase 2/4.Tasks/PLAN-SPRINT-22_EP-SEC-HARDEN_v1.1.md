# Sprint 22 진입 계획 — EP-SEC-HARDEN (Security 강화) v1.1

**작성일**: 2026-05-29 | **버전**: 1.1 | **상태**: Phase 4 세 번째 sprint 진입 권고안 (S21 완료 직후 — 실 코드베이스 대조 갱신)

> **참조**: [v1.0 (2026-05-28 초안)](PLAN-SPRINT-22_EP-SEC-HARDEN_v1.0.md) + [PHASE-4_STABILIZATION_v1.1 §3 S22](PHASE-4_STABILIZATION_v1.1.md) + [SRS NFR-SEC-007 (v1.5 사번8+PIN4)](../2.SRS/SRS-001_Production_Scheduling_System_v1.5.md) + SRS NFR-SEC-004 (audit 3년 불변)

---

## 0. v1.0 → v1.1 변경 요약 (실 코드베이스 검증 반영)

v1.0 은 S20/S21 진입 **전** 작성되어 일부 기술 가정이 stale. 본 v1.1 은 2026-05-29 실 코드/DB 대조 후 정정:

| 항목 | v1.0 가정 | v1.1 정정 (검증) |
|---|---|---|
| **마이그레이션 번호** | V047 (PIN), V048 (partition) | ❌ V047 은 이미 `V047__audit_vc_machine.sql` 사용 중, 전역 최신 **V051**. → PIN = **V052** (app), partition 유지 = **V053** (audit) |
| **ST-SEC-1 대상** | "DaoAuthenticationProvider bean + ProviderManager **제거**" | ⚠️ `AuthenticationManager` bean 은 `AuthController` 가 주입받아 사용 — **제거 불가**. 실제 부채는 [SecurityConfig.java:150-156](../../backend/app/src/main/java/com/scheduling/SecurityConfig.java#L150-L156) 의 `new DaoAuthenticationProvider()` + setter (6.1+ deprecated) → **생성자 주입 현대화** |
| **ST-SEC-3 audit partition** | "월별 partition 자동 생성 (m05 → m06)" 신규 | ✅ [V030__partition_audit_log_monthly.sql](../../backend/audit/src/main/resources/db/migration/V030__partition_audit_log_monthly.sql) 로 **이미 36개 partition (2026m01~2028m12) + DEFAULT + REVOKE + 불변 트리거 전부 구현됨**. → 신규 생성이 아니라 **rolling-window 유지 (2029+ 사전 생성) + 검증 IT** 로 재정의 (scope ↓) |
| **클래스 경로** | `SecurityConfig.java`, `AppUserDetailsService` (경로 미상) | ✅ `com.scheduling.SecurityConfig` (top-level), `com.scheduling.security.auth.{AppUser, AppUserDetailsService, AuthController, AppUserRepository, LoginAttemptService}` |
| **DEV fallback** | (미언급) | ⚠️ SecurityConfig 3-branch (issuer / devFallback / strict). `APP_AUTH_DEV_FALLBACK=false` 시 strict — 베타/PROD 전제. ST-SEC-1 회귀는 3 branch 모두 검증 |

**SP·구조 영향**: 합계 **~4 SP 유지** (ST-SEC-3 scope ↓ 만큼 ST-SEC-1 현대화 정밀도 ↑ 로 상쇄). Task 14 → 13 (ST-SEC-3 3 → 2).

---

## 1. 목적

**Phase 4 보안 부채 일괄 해소 — Sprint 10 EP-AUTH 의 베타 baseline 위에 운영 단계 정책 강화.**

| 정책 | 베타 baseline | Sprint 22 강화 |
|---|---|---|
| Spring Security 인증 | `new DaoAuthenticationProvider()` + setter (6.1+ deprecated) | ✅ 생성자 주입 (`new DaoAuthenticationProvider(uds)`) — deprecation 0 |
| PIN 변경 강제 | 첫 로그인 권고 (수동) | ✅ **30일 경과 시 강제 변경 화면 redirect** (NFR-SEC-007 보완) |
| audit_log 보존 | V030 월별 partition (2028m12 까지 고정) + DEFAULT fallback | ✅ **rolling-window 유지 scheduler** (2029+ 사전 생성, DEFAULT 누적 방지) |
| PIN 재설정 흐름 | IT_OPS 직접 hash 갱신 | ✅ **사용자 첫 로그인 시 강제 변경 화면** (IT_OPS reset 후) |

**Pre-Phase 의존:**
- S20 (Slack/Cost-Zero) + S21 (CRUD UI — ✅ 2026-05-29 마감, DoD 12/12) 완료
- 베타 운영 ~3주 데이터 누적 (PIN 만료 사용자 시뮬 가능 — 본 PC 는 last_pin_change_at 직접 set)

**활성 후 효과:**
- Spring Security 6.1+ 호환 (deprecation 경고 0 — ArchUnit 검증)
- 30일 경과 PIN 사용자 강제 변경 → NFR-SEC-007 100% 준수
- audit_log 2029+ partition 자동 유지 → 3년 보존 NFR-SEC-004 영속 + DEFAULT partition 비대화 방지
- IT_OPS reset → 사용자 첫 로그인 PIN 변경 → 보안 사고 시 신속 대응

---

## 2. Sprint 22 SP·기간

| Story | SP | 추정 PD |
|---|:--:|:--:|
| ST-SEC-1 DaoAuthenticationProvider 생성자 주입 현대화 + 3-branch 회귀 | 1.5 | 0.7 |
| ST-SEC-2 PIN 강제 변경 30일 정책 (V052 last_pin_change_at + pinExpired redirect) | 1.0 | 0.5 |
| ST-SEC-3 audit_log rolling-window 유지 scheduler (V053 + 기존 V030 검증) | 1.0 | 0.5 |
| ST-SEC-4 UserAdminPage PIN reset + 사용자 첫 로그인 강제 변경 | 0.5 | 0.3 |
| **합계** | **~4 SP** | **~2 PD** |

---

## 3. 의존성 DAG

```
ST-SEC-1 (DaoAuth 생성자 주입) ──┐
                                │
ST-SEC-2 (PIN 30일 만료) ───────┤  (각 Story 독립 — ST-SEC-1/2/3 병렬)
                                ↓
ST-SEC-4 (Reset 흐름) ───────────┘  (ST-SEC-2 의 last_pin_change_at 의존)
                                ↓
ST-SEC-3 (audit rolling-window) ─→ Sprint 22 회귀 (verifyAll)
```

**병렬 윈도우:** ST-SEC-1/2/3 독립 (Day 1 동시). ST-SEC-4 는 ST-SEC-2 의 V052 + pinExpired 흐름 의존 → Day 2.

---

## 4. Story · Task 매트릭스

### ST-SEC-1 — DaoAuthenticationProvider 생성자 주입 현대화 (1.5 SP)

> **부채 위치**: [SecurityConfig.java:150-156](../../backend/app/src/main/java/com/scheduling/SecurityConfig.java#L150-L156) `authenticationManager` bean.
> 현재 `new DaoAuthenticationProvider(); provider.setUserDetailsService(uds); provider.setPasswordEncoder(pe);` — 6.1+ 에서 no-arg 생성자 + `setUserDetailsService` deprecated.
> **AuthenticationManager bean 은 유지** (AuthController ST-AUTH-4 가 주입받아 `authenticate()` 호출).

| Task | 내용 | SP |
|---|---|:--:|
| TK-SEC-1-1 | SecurityConfig `authenticationManager` bean — `new DaoAuthenticationProvider(userDetailsService)` 생성자 주입 + `setPasswordEncoder()` 유지 (6.1+ 비-deprecated). `ProviderManager` 래핑 유지 | 0.5 |
| TK-SEC-1-2 | AppUserDetailsService 호환 검증 — `loadUserByUsername(employeeId)` 그대로 + 미존재 시 `UsernameNotFoundException` | 0.2 |
| TK-SEC-1-3 | 회귀 IT — SecurityConfig 3-branch (issuer-uri / devFallback=true / strict=false) 별 AuthLogin + RBAC 정합. 기존 StrictAuthMode/AuthLogin/UserAdmin IT GREEN | 0.5 |
| TK-SEC-1-4 | ArchUnit — deprecated API 참조 검증: `DaoAuthenticationProvider` no-arg 생성자 + `setUserDetailsService` 0건 (또는 deprecation 경고 0 컴파일 옵션) | 0.3 |

### ST-SEC-2 — PIN 강제 변경 30일 (1.0 SP)

> user_account (app 스키마, [V036__create_user_account.sql](../../backend/app/src/main/resources/db/migration/V036__create_user_account.sql)) 컬럼: employee_id, pin_hash, role, failed_attempts, locked_until, created_at, updated_at — **last_pin_change_at 없음 (신규)**.

| Task | 내용 | SP |
|---|---|:--:|
| TK-SEC-2-1 | **V052**__alter_user_account_pin_change.sql (app) — `last_pin_change_at TIMESTAMPTZ NOT NULL DEFAULT now()` + 기존 row 는 created_at 으로 backfill | 0.2 |
| TK-SEC-2-2 | AppUser — `lastPinChangeAt` 필드 + `recordPinChange(Clock)` (PIN 변경 시 갱신, BR-X04 KST `Clock` 주입) | 0.1 |
| TK-SEC-2-3 | AuthController.login — JWT 발급 응답 body 에 `pinExpired: boolean` 포함 (`now - last_pin_change_at > 30d`, `Clock` 기반). LoginResponse record 확장 | 0.3 |
| TK-SEC-2-4 | Frontend LoginPage — `pinExpired=true` 시 PinForceChangeModal 자동 노출 (취소 불가, 변경 성공 후에만 정상 진입) | 0.3 |
| TK-SEC-2-5 | IT — 31일 경과 사용자 → pinExpired=true / 정상 사용자 → false (`Clock` fixed 주입) | 0.1 |

### ST-SEC-3 — audit_log rolling-window 유지 scheduler (1.0 SP)

> **기존 자산 (재사용)**: [V030__partition_audit_log_monthly.sql](../../backend/audit/src/main/resources/db/migration/V030__partition_audit_log_monthly.sql) — 36 partition (2026m01~2028m12) + DEFAULT + `idx_audit_log_*` + V026 불변 트리거(`fn_block_mutation`/`fn_block_truncate`) + REVOKE UPDATE/DELETE/TRUNCATE + `audit_reader` GRANT 전부 구현 완료. **신규 생성 불필요.**
> **신규 부채**: 2028m12 이후 partition 미생성 → 2029-01 부터 DEFAULT partition 으로 fallback (비대화 + 인덱스 효율 저하). rolling-window 유지 필요.

| Task | 내용 | SP |
|---|---|:--:|
| TK-SEC-3-1 | **V053**__audit_partition_maintenance_fn.sql (audit) — `audit.ensure_month_partition(target DATE)` PL/pgSQL: 해당 월 partition 미존재 시 생성 + REVOKE 동봉 (V030 패턴 재사용, idempotent `IF NOT EXISTS`) | 0.5 |
| TK-SEC-3-2 | Backend `PartitionMaintenanceScheduler` — `@Scheduled(cron="0 0 3 25 * *", zone="Asia/Seoul")` 매월 25일 03시 → 향후 12개월 partition `ensure_month_partition` 호출 (2029+ 사전 확보). audit 모듈 `@NamedInterface` 경계 준수 | 0.3 |
| TK-SEC-3-3 | IT — (1) `ensure_month_partition('2029-01-01')` → partition 자동 생성 + REVOKE 정합 + DEFAULT 로 안 가는지, (2) 기존 2026m05 row 보존, (3) 불변 트리거 신규 partition 상속 검증 | 0.2 |

### ST-SEC-4 — PIN reset 흐름 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-SEC-4-1 | UserAdminPage — "PIN 재설정" 버튼 (IT_OPS only, 사번 선택 → 임시 PIN 4자리 자동 생성 + Modal 1회 표시) + 백엔드 `last_pin_change_at = now() - 31d` 강제 set (다음 로그인 시 pinExpired trigger) + @Auditable BR-X02 | 0.3 |
| TK-SEC-4-2 | IT — IT_OPS reset → 사용자 임시 PIN 로그인 → pinExpired=true → 강제 변경 후 정상 진입 | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ SecurityConfig `authenticationManager` — DaoAuthenticationProvider 생성자 주입 (no-arg + setUserDetailsService deprecation 0)
2. ✅ 30일 경과 사용자 로그인 → pinExpired=true → 강제 변경 화면 (취소 불가)
3. ✅ `ensure_month_partition` + 매월 25일 cron → 2029+ partition rolling-window 자동 유지
4. ✅ IT_OPS PIN 재설정 → 임시 PIN 1회 표시 + 사용자 첫 로그인 강제 변경

**비기능 DoD:**
1. ✅ ArchUnit GREEN (deprecated DaoAuth API 참조 0 + audit 모듈 경계)
2. ✅ Backend `verifyAll` GREEN (신규 PinForceChange/AuditPartitionMaintenance IT + 회귀 전체)
3. ✅ TypeScript compile + vitest 신규 3+ cases (PinForceChangeModal) + 기존 GREEN
4. ✅ NFR-SEC-007 100% 준수 (사번 8 + PIN 4 + 5회/10분 잠금 + 30일 강제 변경) · NFR-SEC-004 (audit 불변 + 3년 보존 영속)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| Spring Security 6.1+ 생성자 주입 회귀 (3-branch 중 1개 fail) | 인증 전반 영향 | 사전 회귀 IT 3-branch 별 실행 + 한 commit 안 backout 가능. devFallback=true(본 PC) / false(strict) 양쪽 검증 |
| AuthenticationManager bean 잘못 제거 (v1.0 오해 답습) | AuthController 주입 실패 → 로그인 전면 불가 | **bean 유지** 명시 (§4 ST-SEC-1 주석). 내부 Provider 생성 방식만 변경 |
| 30일 만료 사용자 폭증 (베타 8명 동시 만료) | 운영 지연 | IT_OPS reset 배치 가능 (다중 사번) + 첫 로그인 강제 변경은 ~10초 |
| audit partition 유지 cron 실패 (2029 도래 전 DB 권한 부족) | DEFAULT partition 비대화 (즉시 장애 아님 — fallback 동작) | DEFAULT partition 안전망으로 데이터 손실 0 + cron 실패 시 Slack alert (S20 정합) + idempotent 재시도 |
| 임시 PIN UserAdminPage 화면 노출 | Modal 캡쳐 위험 | 30초 후 자동 close + clipboard 복사 1회 + 화면 재표시 불가 |
| V052/V053 번호 충돌 (병렬 S23 가 동일 번호 점유) | Flyway 적용 실패 | S22 진입 시 전역 최신 재확인 (현재 V051) + S23 와 번호 사전 분배 |

---

## 7. 작업 순서 추천

**Day 1** — Spring Security + partition 유지 (병렬):
1. TK-SEC-1-1~4 (DaoAuth 생성자 주입 + 3-branch 회귀 + ArchUnit)
2. TK-SEC-3-1~3 (V053 ensure_month_partition + scheduler + IT)

**Day 2** — PIN 정책:
3. TK-SEC-2-1~5 (V052 + AppUser.recordPinChange + JWT pinExpired + LoginPage Modal)
4. TK-SEC-4-1~2 (UserAdminPage reset → 강제 변경 흐름)

**Day 3** — DoD + 검증:
5. **본 PC 시각 검증** — DB 직접 `last_pin_change_at = now() - interval '31 day'` set → 해당 사번 로그인 → PinForceChangeModal 노출 → 변경 후 정상 진입 (변경 seed 는 검증 후 원복)
6. `./gradlew verifyAll` + frontend tsc/vitest 전체 GREEN → commit/push

**총 ~2 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Migration | **V052** (app, last_pin_change_at) + **V053** (audit, ensure_month_partition fn) |
| Backend Config/Service | SecurityConfig (DaoAuth 생성자 주입), AppUser (recordPinChange), AuthController (pinExpired), PartitionMaintenanceScheduler |
| Backend IT | PinForceChangeIT + AuditPartitionMaintenanceIT + SecurityConfig 3-branch 회귀 |
| Frontend | LoginPage (pinExpired Modal trigger) + PinForceChangeModal (신규) + UserAdminPage (PIN reset 버튼) |
| Frontend Test | vitest 신규 3+ cases (PinForceChangeModal — 노출/취소불가/변경성공) |
| Docs | USER_MANUAL_v1.3 §0.2 보안 갱신 + §3.1 IT_OPS PIN reset 절차 (S20 v1.1 webhook §6 합본 검토) |
| WBS | TASK-001_WBS Addendum (S22) |

---

## 9. Sprint 22 후 다음 단계

**Sprint 23 (EP-MES-ADAPTER-1) 진입 조건:**
- ✅ DoD 8/8 충족
- ✅ 본 PC PIN 만료 강제 변경 시각 검증
- ⏳ 실 MES 시스템 협의 진행 (벤더 API spec 확보) — S22 와 병렬 가능 (PHASE-4 §4 S22↔S23 독립)

**Sprint 23 첫 작업** — PLAN-SPRINT-23 v1.x 갱신 (기존 v1.0 위 실 코드 대조 — MesShiftPort/DegradedModeService 현황 반영).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 세 번째 sprint EP-SEC-HARDEN 4 Story / 14 Task / ~4 SP 분해. DaoAuth deprecation + PIN 30일 만료 + audit partition + reset 흐름. DoD 8 + 리스크 4 + 3-Day. Sprint 10 EP-AUTH baseline 위 운영 단계 강화. |
| **1.1** | **2026-05-29** | **Claude Code** | **S21 마감 직후 실 코드/DB 대조 갱신 — v1.0 stale 가정 4건 정정: (1) 마이그레이션 번호 V047/V048 → V052(app)/V053(audit) (V047 이미 사용·전역 최신 V051). (2) ST-SEC-1 "AuthenticationManager 제거" → AuthController 의존이라 제거 불가, 실 부채는 SecurityConfig:150-156 DaoAuthenticationProvider no-arg 생성자+setter → 생성자 주입 현대화로 재정의. (3) ST-SEC-3 audit partition 신규 생성 → V030 으로 36개(2028m12까지)+DEFAULT+REVOKE+불변트리거 이미 구현 확인 → rolling-window 유지(2029+) scheduler 로 scope 재정의 (Task 3→2). (4) 클래스 경로·SecurityConfig 3-branch(issuer/devFallback/strict)·Clock 주입(BR-X04) 명시. SP ~4 유지, Task 14→13. 리스크 +2 (bean 오제거 답습 / 번호 충돌).** |
