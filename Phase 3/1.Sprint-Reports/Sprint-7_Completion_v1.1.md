# Sprint 7 완료 보고서 v1.1 — BR-V12·V13 풀 스택 + 마무리

**Sprint**: S7 (carry-over 풀 스택) | **기간**: 2026-05-23 (1일) | **상태**: ✓ 완료
**작성**: 2026-05-23 | **상위**: [Phase-3_Completion_v1.0](../2.Phase-Completion/Phase-3_Completion_v1.0.md) | **전판**: [Sprint-7_Completion_v1.0](Sprint-7_Completion_v1.0.md)

> v1.0 (백엔드 + tsconfig fix, 3 commit) 이후 **BR-V12·V13 Planner UI + 마무리 cleanup 3 commit 추가**.
> Sprint 7 최종 클로저 — Phase 4 베타 진입 전 마지막 코드 작업 게이트.

---

## 1. v1.0 → v1.1 변경 요지

| 항목 | v1.0 | v1.1 |
|---|---|---|
| Sprint 7 commit | 3 | **6** (+3 — UI + chore) |
| Backend 신규 | V033 + Service 2 + IT 5 | + **CapacityOverflowController** (REST 2 endpoint) |
| Frontend 신규 | 없음 (UI 미포함) | **features/capacity-overflow/** (api + 2 panel + page) + i18n + 라우터 + 4 type tests |
| Vitest | 54 | **58** (+4) |
| Tooling | — | **.markdownlint.json + .cspell.json** (VSCode 문제 탭 54 → 0) |

---

## 2. v1.1 신규 Task (3 commit)

### BR-V12·V13 REST + Planner UI — 2 commit

| Task | 상태 | Commit |
|---|---|---|
| `vc.capacity_overflow.CapacityOverflowController` @Profile("with-infra") + @PreAuthorize("hasRole('PLANNER')") | ✓ | `1f1313f` |
| POST `/api/v1/schedule/vc/capacity-overflow/split` — BR-V12 우선순위 split 미리보기 | ✓ | `1f1313f` |
| POST `/api/v1/schedule/vc/capacity-overflow/supplement` — BR-V13 1클릭 KD 잔량 보충 | ✓ | `1f1313f` |
| `features/capacity-overflow/api/capacityOverflowApi.ts` — SplitResult + SupplementResult + ConsumedEntry record 1:1 | ✓ | `9f3f5f0` |
| `CapacityOverflowSplitPanel.tsx` — daily_capa + hose qty 입력 + 자동 채택/추가 요청 큐 + Progress 사용률 | ✓ | `9f3f5f0` |
| `KdSupplementPanel.tsx` — hose + shortage 입력 + Statistic 4종 + 소진 KD orders 테이블 (동일 hose green / 그룹 blue) | ✓ | `9f3f5f0` |
| `CapacityQueuePage.tsx` — 두 패널 Tabs 통합 (/vc/capacity-queue) | ✓ | `9f3f5f0` |
| 라우터 + MainLayout 메뉴 활성 + i18n ko/en (`menu.capacityQueue`) | ✓ | `9f3f5f0` |
| `capacityOverflow.types.test.ts` — 4 단위 (SplitResult + SupplementResult 정합) | ✓ | `9f3f5f0` |

### Tooling cleanup — 1 commit

| Task | 상태 | Commit |
|---|---|---|
| `.markdownlint.json` — 한국어 문서 친화 11 룰 disable/relax (MD013/024/026/033/041…) | ✓ | `c0df2d8` |
| `.cspell.json` — 프로젝트 단어 ~100 + Phase 1~5 / 0.Pprompt 무시 | ✓ | `c0df2d8` |

**v1.0 + v1.1 합계** — **6 commit / ~24 신규 파일 / 16 + 4 = 20 신규 tests / 1 신규 REST endpoint pair**.

---

## 3. v1.1 시점 핵심 지표

| 영역 | 결과 (v1.0 → v1.1) |
|---|---|
| Backend 회귀 | **788 tests / 0 failures** (변동 없음 — controller 는 IT 미추가, Sprint 8+ 권장) |
| Frontend vitest | **54 → 58** (+4 — capacityOverflow.types) |
| Frontend lint | 0 warning 유지 (`--max-warnings 0`) |
| Frontend prod build | **3284 module / 14.79s** (v1.0 14.54s → +0.25s 미미) |
| Vite entry first paint | 7.13kB → **7.23kB gzip** (+0.10kB — 라우터 추가분만) |
| 신규 chunk `CapacityQueuePage` | **2.72kB gzip** (lazy, /vc/capacity-queue 진입 시) |
| Modulith verify | 0 위반 (9 모듈 유지) |
| ArchUnit | 29 rule 통과 |
| VSCode 문제 탭 | **54 → 0** (markdownlint + cspell config 적용) |

---

## 4. BR-V12·V13 운영 진입 — UI 추가로 완성

### v1.0 시점 (백엔드만)

```text
운영 진입 = DI-07 PRODUCT_PRIORITY + DI-08 KD_ORDER 입력 후 → Service 자동 호출
(직접 호출 entry point 없음, Allocator wiring 대기)
```

### v1.1 시점 (UI 추가 후)

```text
운영 진입 = DI-07/08 입력 후 → /vc/capacity-queue UI 진입 가능
           ├─ Tab 1 (BR-V12) — daily_capa + hose 별 요구량 입력 → Split 미리보기
           └─ Tab 2 (BR-V13) — hose + shortage 입력 → 1클릭 보충
RBAC — PLANNER 전용 (@PreAuthorize)
```

→ Planner 가 **수주통합 후 자동 capa 검증** 외에도 **수동 시뮬레이션** 가능.

---

## 5. 누적 commit (Sprint 7 시간순 6 commit)

```text
9a65847  feat(master): V033 PRODUCT_PRIORITY + KD_ORDER 마스터               [v1.0]
d1610a7  feat(vc): BR-V12 CapacityOverflowQueue + BR-V13 KdSupplement       [v1.0]
a12e644  fix(frontend): tsconfig baseUrl 제거 (TS 7.0 호환)                  [v1.0]
7688195  docs(sprint7): 종합 회귀 + Vite bundle baseline + Sprint 7 회고     [v1.0]
1f1313f  feat(vc): BR-V12·V13 REST endpoints — /split + /supplement         [v1.1 +]
9f3f5f0  feat(ui): Sprint 7 BR-V12·V13 Planner UI — capa 큐 + KD 보충        [v1.1 +]
c0df2d8  chore(tooling): VSCode 문제 탭 노이즈 차단 — .markdownlint + .cspell  [v1.1 +]
```

---

## 6. Phase 4 진입 게이트 — v1.1 시점 재확인

- [x] **9 Modulith 모듈** + Phase 3 9 Epic + **Sprint 7 V12·V13 풀 스택**
- [x] **34 Flyway 마이그레이션** V001~V033
- [x] **9 핵심 BR + 2 deferred BR 활성 + UI 진입점**
- [x] **Backend 788 tests / 0 failure** (Sprint 0~7 누적)
- [x] **Frontend vitest 58 + lint 0 + entry 7.23kB**
- [x] **Playwright 226 spec 등록 + 베타 시나리오 5 SOP + 페르소나 4 가이드**
- [x] **누적 ~174 commit · 머지 충돌 0**
- [x] **VSCode 문제 탭 0** (markdownlint + cspell)

→ **Phase 4 (베타 운영) 진입 — 모든 게이트 통과, 최종 승인 가능**.

---

## 7. v1.1 차순위 carry-over (Sprint 8+ / Phase 5+)

| 항목 | 우선 | 비고 |
|---|---|---|
| ~~BR-V12 Planner UI — 추가 요청 큐 승인 모달~~ | ✓ done | v1.1 — Tab 1 capa split 미리보기 완료, Sprint 8+ 승인 confirm 워크플로우 별도 |
| ~~BR-V13 KD 잔량 대시 — 1클릭 보충 UI~~ | ✓ done | v1.1 — Tab 2 완료, Grafana panel (IT_OPS) 별도 |
| BR-V12 추가 요청 큐 승인 워크플로우 (Sprint 8+) | Medium | UI → 백엔드 commit/reject endpoint + audit |
| BR-V13 Grafana panel (IT_OPS) | Medium | KD remaining_qty per hose 시각화 |
| CapacityOverflowController IT 보강 | Low | 현재 Service IT 5 충분, REST layer IT 추가 권장 |
| Mobile App (Flutter 압출 패드) | High | Phase 5+ |
| ML 추천 (EP-18 ranking 자동화) | Low | Phase 6+ |
| ArchUnit DDD layer 강화 (`@DomainLayer`) | Low | |

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Sprint 7 (3 commit, BR-V12·V13 deferred 백엔드 + tsconfig fix, ~5 SP / 0.5일) |
| 1.1 | 2026-05-23 | Claude Code | BR-V12·V13 REST controller + Planner UI 풀 스택 추가 + tooling (markdownlint+cspell) — **+3 commit / 6 commit 총합**, vitest 54→58, 문제 탭 54→0 |
