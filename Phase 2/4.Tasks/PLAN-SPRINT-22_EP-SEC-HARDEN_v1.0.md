# Sprint 22 진입 계획 — EP-SEC-HARDEN (Security 강화) v1.0

**작성일**: 2026-05-28 | **버전**: 1.0 | **상태**: Phase 4 세 번째 sprint 진입 권고안 (S20 + S21 완료 후)

> **참조**: [PHASE-4_STABILIZATION_v1.0 §3 S22](PHASE-4_STABILIZATION_v1.0.md) + [SRS NFR-SEC-007](../../Phase%202/2.SRS/SRS-001_Production_Scheduling_System_v1.5.md) + [SRS NFR-SEC-004](../../Phase%202/2.SRS/SRS-001_Production_Scheduling_System_v1.5.md)

---

## 1. 목적

**Phase 4 보안 부채 일괄 해소 — Sprint 10 EP-AUTH 의 베타 baseline 위에 운영 단계 정책 강화.**

| 정책 | 베타 baseline | Sprint 22 강화 |
|---|---|---|
| Spring Security 인증 | `DaoAuthenticationProvider` (deprecated in 6.1+) | ✅ `AuthenticationManager` builder pattern (HttpSecurity 안 직접 set) |
| PIN 변경 강제 | 첫 로그인 권고 (수동) | ✅ **30일 경과 시 강제 변경 화면 redirect** (NFR-SEC-007 보완) |
| audit_log 보존 | 단일 테이블 (3년 보존 정책만) | ✅ **월별 partition 자동 생성** (`schedule_audit_log_y2026m05 → m06 → ...`) |
| PIN 재설정 흐름 | IT_OPS 직접 hash 갱신 | ✅ **사용자 첫 로그인 시 강제 변경 화면** (IT_OPS reset 후) |

**Pre-Phase 의존:**
- S20 (Slack/Kakao) + S21 (CRUD UI) 완료
- 베타 운영 ~3주 데이터 누적 (PIN 만료 사용자 시뮬 가능)

**활성 후 효과:**
- Spring Security 6.1+ 호환 (deprecation 경고 0)
- 30일 경과 PIN 사용자 강제 변경 → NFR-SEC-007 100% 준수
- audit_log partition 자동 → 3년 보존 NFR-SEC-004 + 쿼리 성능 (월별 인덱스)
- IT_OPS reset → 사용자 첫 로그인 PIN 변경 → 보안 사고 시 신속 대응

---

## 2. Sprint 22 SP·기간

| Story | SP | 추정 PD |
|---|:--:|:--:|
| ST-SEC-1 DaoAuth deprecation 제거 + AuthenticationManager builder | 1.5 | 0.7 |
| ST-SEC-2 PIN 강제 변경 30일 정책 (last_pin_change_at + redirect) | 1.0 | 0.5 |
| ST-SEC-3 audit_log 월별 partition 자동 생성 | 1.0 | 0.5 |
| ST-SEC-4 UserAdminPage PIN reset + 사용자 첫 로그인 강제 변경 | 0.5 | 0.3 |
| **합계** | **~4 SP** | **~2 PD** |

---

## 3. 의존성 DAG

```
ST-SEC-1 (AuthenticationManager) ──┐
                                   │
ST-SEC-2 (PIN 30일 만료) ──────────┤  (각 Story 독립)
                                   ↓
ST-SEC-4 (Reset 흐름) ──────────────┘
                                   ↓
ST-SEC-3 (audit partition) ────────→ Sprint 22 회귀
```

**병렬 윈도우:** ST-SEC-1/2/3 독립.

---

## 4. Story · Task 매트릭스

### ST-SEC-1 — Spring Security 6.1+ AuthenticationManager (1.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-SEC-1-1 | SecurityConfig.java — DaoAuthenticationProvider bean + ProviderManager 제거. `HttpSecurity.authenticationManager(AuthenticationManager)` builder 패턴 (UserDetailsService + PasswordEncoder 직접 주입) | 0.6 |
| TK-SEC-1-2 | AppUserDetailsService 호환 검증 — `loadUserByUsername(employeeId)` 그대로 + 미존재 시 UsernameNotFoundException | 0.2 |
| TK-SEC-1-3 | 회귀 IT — StrictAuthModeIT + AuthLoginIT + UserAdminIT 모두 GREEN 재확인 | 0.4 |
| TK-SEC-1-4 | ArchUnit — `DaoAuthenticationProvider` 참조 0건 (deprecated 제거 검증) | 0.3 |

### ST-SEC-2 — PIN 강제 변경 30일 (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-SEC-2-1 | V047__alter_user_account_pin_change.sql — `last_pin_change_at TIMESTAMPTZ NOT NULL DEFAULT now()` + index | 0.2 |
| TK-SEC-2-2 | AppUser.recordPinChange(now) — PIN 변경 시 last_pin_change_at 갱신 | 0.1 |
| TK-SEC-2-3 | AuthController.login — JWT 발급 시 응답 body 에 `pinExpired: boolean` 포함 (now - last_pin_change_at > 30d) | 0.3 |
| TK-SEC-2-4 | Frontend LoginPage — pinExpired=true 시 자동 PinForceChangeModal 노출 (취소 불가, 변경 후 정상 진입) | 0.3 |
| TK-SEC-2-5 | IT — 31일 경과 사용자 로그인 → pinExpired=true / 정상 사용자 → false | 0.1 |

### ST-SEC-3 — audit_log 월별 partition (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-SEC-3-1 | V048__audit_log_partition_function.sql — `audit.create_next_month_partition()` PL/pgSQL (다음 달 partition 자동 생성 + REVOKE UPDATE/DELETE 동봉) | 0.5 |
| TK-SEC-3-2 | Backend `@Scheduled(cron = "0 0 1 25 * ?")` — 매월 25일 03시 다음 달 partition 사전 생성 (Asia/Seoul) | 0.3 |
| TK-SEC-3-3 | IT — 함수 호출 시 audit.schedule_audit_log_y2026m06 자동 생성 + REVOKE 정합 + 기존 m05 row 보존 | 0.2 |

### ST-SEC-4 — PIN reset 흐름 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-SEC-4-1 | UserAdminPage — "PIN 재설정" 버튼 (IT_OPS만, 사번 선택 → 임시 PIN 4자리 자동 생성 + Modal 안 1회 표시) + 백엔드 `last_pin_change_at = now() - 31d` 강제 set (다음 로그인 시 강제 변경 trigger) | 0.3 |
| TK-SEC-4-2 | IT — IT_OPS reset 후 사용자 임시 PIN 로그인 → pinExpired=true → 강제 변경 후 정상 | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ SecurityConfig — DaoAuthenticationProvider 0 참조 (AuthenticationManager builder)
2. ✅ 30일 경과 사용자 로그인 → pinExpired=true → 강제 변경 화면 (취소 불가)
3. ✅ audit_log 다음 달 partition 자동 생성 (cron 매월 25일 03시 KST)
4. ✅ IT_OPS PIN 재설정 → 임시 PIN 1회 표시 + 사용자 첫 로그인 강제 변경

**비기능 DoD:**
1. ✅ ArchUnit GREEN (DaoAuthenticationProvider 참조 0)
2. ✅ Backend IT 신규 5+ + 회귀 0
3. ✅ TypeScript compile + vitest 신규 3+ cases (PinForceChangeModal)
4. ✅ NFR-SEC-007 100% 준수 (사번 8 + PIN 4 + 5회/10분 잠금 + 30일 강제 변경)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| Spring Security 6.1+ builder 패턴 회귀 (StrictAuthModeIT fail) | 인증 전반 영향 | 사전 회귀 IT 실행 + 한 commit 안에 backout 가능 |
| 30일 만료 사용자 폭증 (베타 8명 동시 만료) | 운영 지연 | IT_OPS reset 배치 가능 (다중 사번 한 번에) |
| audit_log partition 생성 cron 실패 (DB 권한 부족) | 다음 달 audit insert fail | 재시도 Resilience4j + Slack alert (Sprint 18 정합) |
| 임시 PIN UserAdminPage 화면 표시 중 보안 노출 | Modal 캡쳐 위험 | 30초 후 자동 close + clipboard 복사 1회만 가능 |

---

## 7. 작업 순서 추천

**Day 1** — Spring Security + partition:
1. TK-SEC-1-1~4 (AuthenticationManager builder)
2. TK-SEC-3-1~3 (partition 자동 생성)

**Day 2** — PIN 정책:
3. TK-SEC-2-1~5 (V047 + JWT pinExpired + LoginPage)
4. TK-SEC-4-1~2 (UserAdminPage reset)

**Day 3** — DoD:
5. **본 PC 시각 검증** — 30일 만료 사용자 로그인 시뮬 (DB 직접 last_pin_change_at 31일 전 set) → 강제 변경 화면 노출

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Migration | V047 (last_pin_change_at) + V048 (audit partition function) |
| Backend Config | SecurityConfig (AuthenticationManager builder), PartitionMaintenanceScheduler |
| Backend IT | PinForceChangeIT + AuditPartitionIT + Sprint 10/11 회귀 |
| Frontend | LoginPage (pinExpired Modal trigger) + PinForceChangeModal + UserAdminPage (PIN reset 버튼) |
| Docs | USER_MANUAL_v1.3 §0.2 보안 갱신 + §3.1 IT_OPS PIN reset 절차 |

---

## 9. Sprint 22 후 다음 단계

**Sprint 23 (EP-MES-ADAPTER-1) 진입 조건:**
- ✅ DoD 8/8 충족
- ⏳ 실 MES 시스템 협의 진행 (벤더 API spec 확보)

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 세 번째 sprint EP-SEC-HARDEN 4 Story / 14 Task / ~4 SP 분해. DaoAuth deprecation + PIN 30일 만료 + audit partition + reset 흐름. DoD 8 + 리스크 4 + 3-Day. Sprint 10 EP-AUTH baseline 위 운영 단계 강화. |
