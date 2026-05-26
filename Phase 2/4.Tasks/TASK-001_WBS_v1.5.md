# 작업 분할 구조서 (WBS) v1.5 — Sprint 9 마감 + 표준 베타 Sprint 10~19 진입 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.5 | **작성일**: 2026-05-27
**전판**: [v1.4](TASK-001_WBS_v1.4.md) (Sprint 8 V12 풀 스택 + V13 Grafana panel Addendum)
**상태**: Addendum — **(A) Sprint 9 마감** + **(B) 표준 베타 Sprint 10~19 신규 진입**

> v1.4 (Sprint 8 마감, 51 Epic / 291 SP) 의 차순위 §4 에서 식별된 2 항목을 Sprint 9 신규 Epic
> 으로 진입 + 마감, 동시에 **본 PC 알파 단계** (S-D Hybrid Dev Mode) 안정화 마감.
> 추가로 사용자 결정 **"표준 베타로 진입"** (2026-05-27 결정) 반영 — Sprint 10~19 신규 Epic 10개
> 등재 (인증 활성 → RBAC → 마스터 → 3 PDD 완성 → 확정 게이트 → MES 폴백 → 알림 → 베타 운영).
> **본 문서는 v1.4 변경 델타만 정리** — 전체 WBS 콘텐츠 v1.2 유지, 변경 chain v1.4 → v1.5.

---

## 1. v1.4 → v1.5 변경 요지

| 항목 | v1.4 (Sprint 8 마감) | v1.5 (Sprint 9 마감 + 표준 베타 진입) |
|---|---|---|
| Epic 총수 | 51 | **63** (+2 Sprint 9 carry-over 마감 + **+10 표준 베타 Sprint 10~19**) |
| SP 총수 | 291 | **341** (+4 실 Sprint 9 + ~46 계획 Sprint 10~19, AI 가속 2배 압축 후 ~23 실 추정) |
| Sprint | S0~S8 | **S0~S19** (S10~S19 = 표준 베타 진입 신규 plan) |
| 베타 정의 | 미확정 | ✅ **표준 베타** — 3 PDD + 확정 게이트 + 4 페르소나 + 알림 (사용자 2026-05-27 결정) |
| 알파 (S-D 본 PC) | Sprint 8 진입 후 진행 중 | ✅ **Smoke 알파 6/6 페이지 검증 완료** (2026-05-27) → 표준 베타 진입 게이트 통과 |

---

## (A) Sprint 9 마감 — V12 carry-over 2 Epic + 알파 안정화

### 2. 신규 Epic 상세 (Sprint 9)

#### EP-V12-Auto-Expire (BR-V12 PENDING 24h 자동 만료)

**Sprint**: **S9** / **출처**: [WBS v1.4 §4 차순위](TASK-001_WBS_v1.4.md) / **SP 실**: ~1.5 / **선행**: EP-V12-승인 (S8 마감)

| Story | 구현 | Commit |
|---|---|---|
| ST-V12-AE-1 — Repository custom finder | findByStatusAndRequestedAtBefore(Status, Instant) | `b89ba77` |
| ST-V12-AE-2 — Service expirePending() + @Scheduled 03:00 KST | `expirePending()` + `@Scheduled(cron="0 0 3 * * *", zone="Asia/Seoul")` + @Auditable | `b89ba77` |
| ST-V12-AE-3 — IT 3 — 25h 만료 / 12h 보존 / 혼합 selective | CapacityOverflowApprovalIT 확장 (Testcontainers Service 단독 호출 검증) | `b89ba77` |

#### EP-V12-Allocator-Chain (BR-V12 ACCEPTED → vc_schedule INSERT chain 진입점)

**Sprint**: **S9** / **출처**: [WBS v1.4 §4 차순위](TASK-001_WBS_v1.4.md) / **SP 실**: ~1.5 / **선행**: EP-V12-승인 (S8 마감)

| Story | 구현 | Commit |
|---|---|---|
| ST-V12-AC-1 — Domain event record | CapacityOverflowAcceptedEvent (vc.events 패키지, requestId/hoseId/qty/rank/acceptedBy/acceptedAt) | `496e1d0` |
| ST-V12-AC-2 — Service.accept() event publish | ApplicationEventPublisher.publishEvent (AFTER_COMMIT 비동기 listener 진입점) | `496e1d0` |
| ST-V12-AC-3 — AllocatorChainListener stub @ApplicationModuleListener | Modulith 모듈간 직접 호출 회피 (vc → allocator) — 실 Allocator 호출은 베타 운영 후 별 turn | `496e1d0` |
| ST-V12-AC-4 — IT 2 — accept event publish + reject 미발행 | CapacityOverflowApprovalIT 확장 (Awaitility 5s + AcceptedEventCapture) | `496e1d0` |

#### EP-S-D-Hybrid (S-D 본 PC 알파 — Hybrid Dev Mode 안정화)

**Sprint**: **S9** / **출처**: 사용자 요청 "FCB 패턴 가벼운 테스트" / **SP 실**: ~1 / **선행**: 없음

| Story | 구현 | Commit |
|---|---|---|
| ST-SD-1 — SecurityConfig DEV fallback | KEYCLOAK_ISSUER_URI 미설정 시 anonymous + 4 역할 자동 부여 (permitAll) | `e738459` |
| ST-SD-2 — Vite proxy + sockjs polyfill | proxy 127.0.0.1 + window.global=window (main.tsx runtime) + /actuator → /api/actuator base-path 우회 | `020141f`+`8c5266e`+`d53c0cc` |
| ST-SD-3 — Controller Principal null fallback | actorOf() helper 4 method 적용 (HTTP 500 NPE fix) | `31537b7` |
| ST-SD-4 — V035 capacity_overflow_request audit trigger | audit gap closure (BR-X02) + IT 4 cases | `7fa91cc`+`8a08c86` |
| ST-SD-5 — MainLayout FCB 패턴 — Header EVS + Footer Main + horizontal 박스 nav | 로고 swap + Sider 폐지 + Ant Button 박스 메뉴 (다크 그라디언트) | `30d12c1`+`1328fc6` |
| ST-SD-6 — Smoke 알파 6/6 페이지 검증 | Home/OrderImport/VcSimview/CapacityQueue(5단계)/ExMatrix/MasterRestore — 500/콘솔 에러 0건 | (검증 turn, commit 없음) |

### 3. Sprint 9 Task 매트릭스

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-V12-AE-1-1 Repository finder | ST-V12-AE-1 | 0.2 | `b89ba77` |
| TK-V12-AE-2-1 expirePending + @Scheduled | ST-V12-AE-2 | 0.8 | `b89ba77` |
| TK-V12-AE-3-1 IT 3 (25h/12h/selective) | ST-V12-AE-3 | 0.5 | `b89ba77` |
| EP-V12-Auto-Expire 소계 | | **~1.5 SP** | |
| TK-V12-AC-1-1 Event record | ST-V12-AC-1 | 0.2 | `496e1d0` |
| TK-V12-AC-2-1 publishEvent | ST-V12-AC-2 | 0.3 | `496e1d0` |
| TK-V12-AC-3-1 Listener stub | ST-V12-AC-3 | 0.5 | `496e1d0` |
| TK-V12-AC-4-1 IT 2 | ST-V12-AC-4 | 0.5 | `496e1d0` |
| EP-V12-Allocator-Chain 소계 | | **~1.5 SP** | |
| TK-SD-1~6 (Hybrid Dev Mode 8 commits 합계) | ST-SD-1~6 | 1.0 | `e738459`~`1328fc6` |
| EP-S-D-Hybrid 소계 | | **~1 SP** | |
| **Sprint 9 합계** | | **~4 SP** | (계획 ~8 SP — AI 가속 2배 압축) |

---

## (B) 표준 베타 진입 — Sprint 10~19 신규 plan

### 4. 표준 베타 정의 (2026-05-27 사용자 결정)

| 항목 | 범위 |
|---|---|
| **목표 사용자** | 사내 5~10명 (4 페르소나 분리 — PLANNER + STK_USER + IT_OPS + READ_ONLY) |
| **활성 워크플로우** | PDD-01 수주통합 + PDD-02 성형 + PDD-03 압출 (3 PDD 전체) |
| **활성 비즈니스 룰** | BR-V07 당일 락 + BR-X01 확정 게이트 + BR-X02 audit + BR-X04 KST + BR-X05 dual-review + BR-X06 MES 폴백 + BR-X07 D-2 hard + BR-E05 수율 + BR-O02 알림 |
| **인증** | 사번+PIN 로컬 (NFR-SEC-007 v1.5) + Keycloak OIDC 통합 (선택) |
| **알림** | 카카오 webhook 활성 + in-app STOMP push |
| **운영 자동화** | Windows 자동시작 (NSSM) + 사내 LAN bind (VITE_LAN_HOST=1) |
| **기간 / SP** | ~10 Sprint × 평균 5 SP = ~46 SP 계획 (AI 가속 ~23 실 추정) |

### 5. Sprint 10~19 신규 Epic Roadmap

| Sprint | Epic | 핵심 산출물 | SP 계획 | 선행 |
|---|---|---|---|---|
| **S10** | **EP-AUTH** — 사번+PIN 인증 활성 (NFR-SEC-007) | V036 app.user (employee_id 8 + pin_hash + locked_until + failed_attempts) + LoginPage.tsx + DaoAuthenticationProvider + UserDetailsService + 5회/10분 잠금 + SecurityConfig DEV fallback 제거 | 5 | — |
| **S11** | **EP-RBAC** — 4 페르소나 RBAC 강화 | ROLE_STK_USER + ROLE_IT_OPS + ROLE_READ_ONLY 분리 + 페이지별 권한 매트릭스 + @PreAuthorize 전면 적용 + @WithMockUser IT 변환 (anonymous IT 모두 mockUser) | 4 | S10 |
| **S12** | **EP-MASTER-UI** — 마스터 데이터 입력 UI (IT_OPS) | 47 품번 + LP-01~04 + IC + 셋팅그룹 1~8 + 합금형 (composite 1·2·3·6) + 회전수 (주8/야10) + PRODUCT_PRIORITY + KD_ORDER (마스터 페이지 활성 — 현재 disabled 해제) | 5 | S11 |
| **S13** | **EP-OC-FULL** — 수주통합 PDD-01 완성 | Excel 업로드 → SXSSF streaming 파싱 → severity 분류 (BR-O02 ±20% + 납기 + hose ID + new + deleted always-critical) → 수정 요청 워크플로우 + 알림 stub | 5 | S12 |
| **S14** | **EP-VC-FULL** — 성형 시뮬뷰 PDD-02 완성 | AG Grid 1500 row × 30 col 실 데이터 + dnd-kit 드래그 → 백엔드 반영 + STOMP 실시간 broadcast + Sprint 7~9 Capa/KD 통합 (split + queue + supplement) + EP-21 좌/우·호기·앵글상한·규격<7 제약 | 6 | S13 |
| **S15** | **EP-EX-FULL** — 압출 PDD-03 완성 | 4-shift × 75% 매트릭스 (EP-17) + 다중 후보 ranking (EP-18) + BR-E01 압출 deadline + BR-E05 수율 (2531 reference) + Excel 역-export (EP-12) | 5 | S14 |
| **S16** | **EP-CONFIRM** — 확정 게이트 (BR-X01·X05·X07) | D-2 ~ D-1 락 + D-2 hard 제약 (D-2 이후 신규 차단) + dual-review (작성자 ≠ 승인자, ROLE_PLANNER 내) + 확정 후 immutable trigger | 5 | S14, S15 |
| **S17** | **EP-DAY-LOCK** — 당일 락 + MES 폴백 (BR-V07·X06) | D-0 락 (수정 불가 trigger) + MES 미수신 시 Excel degraded mode + 폴백 알림 + 1 shift 미수신 임계 | 4 | S16 |
| **S18** | **EP-NOTIFY** — 알림 (BR-O02) + 카카오 | Critical/Important 분류 (BR-O02) + 카카오 webhook 활성 (현재 stub) + Resilience4j retry/circuitbreaker (이미 application.yml 설정됨) + in-app STOMP push | 4 | S13 (S14~S15 와 병렬 가능) |
| **S19** | **EP-BETA-LAUNCH** — 회귀 + LAN + 운영 매뉴얼 | 전 IT 회귀 (Testcontainers 20+) + VITE_LAN_HOST=1 사내 5명 접속 + Windows 자동시작 (NSSM) + Planner/STK/IT_OPS 매뉴얼 v2 + 베타 시작 공지 | 3 | 전부 |
| **Sprint 10~19 합계** | | | **46 SP 계획** | |

### 6. Sprint 10~19 의존성 DAG

```
S10 (AUTH) ─→ S11 (RBAC) ─→ S12 (MASTER-UI) ─→ S13 (OC-FULL) ─→ S14 (VC-FULL) ─→ S15 (EX-FULL)
                                                          ↓
                                                       S18 (NOTIFY) ─ 병렬 (S14/S15 와 동시 진행 가능)
                                                          
                                                       S16 (CONFIRM) ─→ S17 (DAY-LOCK) ─→ S19 (BETA-LAUNCH)
                                                       (S14, S15 후)
```

**병렬 윈도우 (1인 작업 가속):**
- **S14/S15 진행 중 S18 (NOTIFY) 분리 작업** — frontend/backend 디커플링 (알림 API + 메시지 형식 정의는 데이터 흐름 무관)
- **S12 후반에 S11 잔여 (페이지별 권한) 병렬** — IT_OPS 마스터 페이지에서 ROLE 검증 추가 자연스러움

---

## 7. v1.4 §4 차순위 carry-over → v1.5 갱신

| 항목 | v1.4 (Sprint 8 마감) | v1.5 (Sprint 9 마감 + 표준 베타 진입) |
|---|---|---|
| ~~V12 ACCEPTED → vc_schedule 자동 INSERT chain (Allocator 후속)~~ | Sprint 9+ Medium | ✅ **Sprint 9 마감** (listener stub, 실 Allocator는 베타 운영 후) |
| ~~V12 PENDING 자동 만료 (24h 후 자동 REJECTED)~~ | Sprint 9+ Low | ✅ **Sprint 9 마감** |
| 실 Allocator 호출 (vc_schedule INSERT chain 본격화) | (식별 안 됨) | 🆕 **Phase 5+ (베타 운영 후)** — 베타 트래픽 분석 후 priority/slot 알고리즘 결정 |
| Capa 분기 audit trigger | (식별 안 됨) | ✅ **Sprint 9 마감** (V035 + IT 4 cases) |
| Mobile App (Flutter 압출 패드) | High | High (변동 없음, Phase 5+) |
| ML 추천 (EP-18 ranking 자동화) | Low | Phase 6+ |
| ArchUnit DDD layer 강화 | Medium | Medium (변동 없음) |
| 사내 NAS S3 호환 (Excel attachment) | Phase 5+ | Phase 5+ (변동 없음) |

---

## 8. v1.2 § 추가 영향 정리 (v1.4 → v1.5 확장)

| § | v1.4 → v1.5 변경 |
|---|---|
| §9 Deferred Epic | ~~EP-22·23~~ (S7) + ~~EP-V12-승인·EP-V13-Grafana~~ (S8) + **EP-V12-Auto-Expire·EP-V12-Allocator-Chain·EP-S-D-Hybrid (S9 마감)** + **EP-AUTH·EP-RBAC·EP-MASTER-UI·EP-OC-FULL·EP-VC-FULL·EP-EX-FULL·EP-CONFIRM·EP-DAY-LOCK·EP-NOTIFY·EP-BETA-LAUNCH (S10~S19 신규)** |
| §14 SP 합계 | 291 → **341** (Sprint 9 +4 실 + Sprint 10~19 +46 계획) |
| §16 Phase B 진입 조건 | Sprint 8 충족 + Sprint 9 carry-over 마감 + **Smoke 알파 6/6 페이지 검증 (2026-05-27)** |
| §17 GitHub label | `sprint:S8` 추가 → `sprint:S9` + `sprint:S10`~`sprint:S19` 라벨 신설 권장 (베타 진입 후) |

---

## 9. 관련 자료

- [TASK-001_WBS_v1.4](TASK-001_WBS_v1.4.md) — Sprint 8 V12 풀 스택 + V13 Grafana panel Addendum
- [TASK-001_WBS_v1.2](TASK-001_WBS_v1.2.md) — 전체 WBS (1303 line, v1.5 변경 외 그대로 유효)
- [Sprint 9 commits chain](#) — `b89ba77` (Auto-Expire) + `496e1d0` (Allocator-Chain) + `e738459`~`1328fc6` (S-D Hybrid Dev Mode 8 commits)
- [V035 audit trigger](../../backend/audit/src/main/resources/db/migration/V035__audit_capacity_overflow_request.sql) — Sprint 8 후속 hotfix
- [SRS v1.5 §NFR-SEC-007](../2.SRS/SRS-001_Production_Scheduling_System_v1.5.md) — 사번 8자리 + PIN 4자리 + 5회/10분 잠금 정책 (S10 진입 근거)
- [SRS v1.6 Addendum](../2.SRS/) — REQ-FUNC-VC-022·023 Should → Must 승격 (S7 carry-over 정합)
- [ADR-022](../5.ADR/) — Sprint 7 carry-over 5 architecture decision

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — Epic·Story·Task 3단계 분해 |
| 1.1 | 2026-05-15 | (작성자) | 산술 오류 정정 + EP-34 보강 |
| 1.2 | 2026-05-15 | (작성자) | 결함 10건 해소 (49 Epic / 285 SP) |
| 1.3 | 2026-05-23 | Claude Code | Sprint 7 carry-over EP-22·23 deferred 활성 마감 Addendum |
| 1.4 | 2026-05-23 | Claude Code | Sprint 8 신규 Epic 2 (EP-V12-승인 + EP-V13-Grafana) 마감 Addendum (51 Epic / 291 SP) |
| 1.5 | 2026-05-27 | Claude Code | **Addendum (A) Sprint 9 마감 — V12-Auto-Expire + V12-Allocator-Chain + S-D-Hybrid (알파 안정화 + Smoke 검증 6/6). (B) 표준 베타 진입 — Sprint 10~19 신규 Epic 10개 (AUTH→RBAC→MASTER-UI→OC/VC/EX-FULL→CONFIRM→DAY-LOCK→NOTIFY→BETA-LAUNCH). 63 Epic / 341 SP (계획 46 + AI 가속 ~23 실 추정)** |
