# Sprint 5 진입 계획 (UI 통합 + AG Grid + 시뮬뷰)

**Sprint**: S5 | **목표 기간**: 2026-05-23 ~ (2주, AI 가속 시 ~1~2일) | **상태**: 🔄 진입 게이트
**작성**: 2026-05-22 | **상위 참조**: [Sprint-4_Completion_v1.0.md](Sprint-4_Completion_v1.0.md) §10·11, [WBS v1.2 §7](../../Phase%202/4.Tasks/TASK-001_WBS_v1.2.md)

> Sprint 4 (거버넌스·당일 락·라우팅·Export·cascade 7 Epic 16 commit) 종료 직후 진입.
> **Frontend 본격 진입** — React 18 + Vite + TypeScript + AG Grid Enterprise + TanStack Query.
> Sprint 5 = **UI 통합 + 시뮬뷰 + 매트릭스 뷰 + 알림 채널**.

---

## 1. Sprint 5 목표 (PDD-MASTER v1.7 + SRS v1.5 § Frontend·UX)

- **EP-15 성형 현장 시뮬뷰** (S-03) — AG Grid 1500 row × 30 col + 회전 격자 + 충돌 시각화.
- **EP-16 카톡 백업 채널** (S-04) — Kakao SDK + REQ-NF-OPS p99 < 5s + audit 통합.
- **EP-17 일자×shift×라인 매트릭스 뷰** (S-05) — 압출 패드 SockJS subscribe + EP-EX14 chain UI.
- **EP-18 다중 후보 ranking** (C-01) — 충돌 시 ≥ 3 distinct alternative 점수 정렬.
- **EP-19 임의 시점 마스터 복원 UI** (C-02) — audit log → master snapshot UI.
- **EP-20 영업 폴더 watch 자동 송신** (C-03) — folder watcher → upload 자동화.
- **NFR — UI 성능** — AG Grid p95 < 500ms (1500 row 렌더링) + Vite bundle ≤ 200kB gzip.

---

## 2. Sprint 5 Epic·SP 매트릭스

| Epic | 제목 | SP | 의존 (선행) | 핵심 산출 |
|---|---|:--:|---|---|
| **EP-15** ⭐ | 성형 현장 시뮬뷰 | 5 | EP-05 ✓ EP-10 ✓ | AG Grid 1500 row + 회전 격자 컴포넌트 + 시뮬레이션 모드 |
| **EP-16** | 카톡 백업 채널 | 3 | EP-EX14 ✓ (PUSH) | Kakao SDK 클라이언트 + audit + retry 큐 |
| **EP-17** ⭐ | 일자×shift×라인 매트릭스 뷰 | 5 | EP-EX13 ✓ EP-EX14 ✓ | 압출 패드 SockJS + 매트릭스 컴포넌트 + p95 ≤ 2초 |
| **EP-18** | 다중 후보 ranking | 3 | EP-EX12 ✓ | ranking 알고리즘 + UI 정렬 |
| **EP-19** | 임의 시점 마스터 복원 UI | 3 | EP-11 ✓ (audit) | audit 시점 복원 + diff 뷰 |
| **EP-20** | 영업 폴더 watch 자동 송신 | 2 | EP-01 ✓ | FolderWatcher 확장 + REQ-FUNC-OC-014 |

**합계**: **~21 SP** (Sprint 5 capacity 50 SP velocity 기준 · ~42% 활용 — UI 첫 진입 보수적).
EP-15 → EP-17 가 critical path (10 SP — 시뮬뷰 + 매트릭스 뷰가 Frontend 양대 축).

---

## 3. 의존성 그래프

```
Sprint 4 (거버넌스 종단)
       │
       ├──► EP-15 (성형 시뮬뷰) ⭐
       │      │
       │      └──► EP-18 (다중 후보 ranking) — 충돌 시 ranking UI
       │
       ├──► EP-17 (매트릭스 뷰) ⭐
       │      │
       │      └──► EP-16 (카톡 백업) — 알림 채널 통합
       │
       ├──► EP-19 (마스터 복원 UI) — audit 활용
       │
       └──► EP-20 (폴더 watch) — Sprint 0 ImportController 확장
```

Critical Path: **EP-15 → EP-17** (~10 SP, ~7 PD).

---

## 4. 권장 진행 순서 (AI 가속 vibe coding)

| 단계 | Epic·Story | 비고 |
|---|---|---|
| **Phase A** (Day 1) | Frontend 환경 셋업 — Vite + React 18 + TypeScript + AG Grid Enterprise + TanStack Query + Zustand + dayjs | npm 의존성 + tsconfig + Tailwind + ESLint |
| **Phase A** (Day 1) | EP-15 ST-15-1 (AG Grid + 회전 격자) + ST-15-2 (시뮬레이션 모드) | 1500 row × 30 col 가상 스크롤 |
| **Phase B** (Day 1~2) | EP-17 ST-17-1 (매트릭스 뷰) + SockJS subscribe + EX-14 chain | 일자×shift×라인 |
| **Phase C** (Day 2) | EP-18 (ranking) + EP-19 (복원 UI) | EP-15/17 컴포넌트 재사용 |
| **Phase D** (Day 2) | EP-16 (카톡) + EP-20 (폴더 watch 확장) | 백엔드 통합 |
| **Phase E** (Day 2~3) | Sprint 5 회고 + Sprint 6 plan | E2E + 부하 + 베타 운영 진입 |

**병렬 옵션** (의존성 그래프 기반):
- **A. EP-15 + EP-17 병렬** — 시뮬뷰 vs 매트릭스 뷰 (다른 도메인 컴포넌트, 첫 진입 권장)
- **B. EP-16 + EP-20 병렬** — 카톡 백업 vs 폴더 watch (백엔드만, Frontend 분리)
- **C. EP-18 + EP-19 병렬** — ranking vs 복원 (UI 보조 기능)

---

## 5. 신규 인프라 (Frontend 본격 진입)

### `frontend/` (Sprint 5 신규 생성)

```
frontend/
  package.json          React 18 + Vite + TypeScript 5 + AG Grid Enterprise + TanStack Query + Zustand
  tsconfig.json         strict + ESM
  vite.config.ts        proxy /api → backend:8080, /ws → backend:8080
  tailwind.config.js
  src/
    main.tsx
    App.tsx
    api/                Axios + TanStack Query (REST endpoints — EP-12 export, EP-10 confirm)
    grid/               AGGridProvider + 회전 격자 column def
    pages/
      VcSimulationPage      (EP-15 시뮬뷰)
      ExMatrixPage          (EP-17 매트릭스 뷰)
      OverrideModalPage     (EP-13 BR-V07 override UI)
      MasterRestorePage     (EP-19 복원 UI)
      RankingPage           (EP-18 ranking UI)
    websocket/          SockJS + STOMP — /ws/notifications subscribe
    stores/             Zustand — confirm/override state
    types/              TypeScript types (record 동기화 from backend Java records)
```

### `backend/notify/` 확장 — Kakao + 카톡 큐

```
com.scheduling.notify/
  kakao/           KakaoClient (이미 stub 존재) — REQ-FUNC-CO-008 도달 추적 강화
  channels/        + KakaoChannel + 큐 retry (EP-16)
```

### `backend/order/` 확장 — 폴더 watch 자동 송신

```
com.scheduling.order/
  watcher/         FolderWatcherService (이미 Sprint 0 stub) — REQ-FUNC-OC-014 자동 upload
```

---

## 6. Sprint 5 DoD (진입 게이트 충족 → 종료 게이트 목표)

| 영역 | 지표 | 목표 |
|---|---|---|
| **EP-15 AG Grid 렌더링** | 1500 row × 30 col p95 | ≤ 500ms |
| **EP-15 시뮬뷰** | confirm/override UI 통합 | 100% (EP-10·13 백엔드 chain) |
| **EP-16 카톡 도달** | p99 | ≤ 5초 (REQ-NF-OPS-001) |
| **EP-16 carry retry** | 실패 시 ≤ 3 회 재시도 + audit | 100% |
| **EP-17 매트릭스 뷰** | SockJS subscribe + chain | EP-EX14 PUSH 즉시 반영 |
| **EP-17 p95** | 매트릭스 갱신 | ≤ 2초 |
| **EP-18 ranking** | ≥ 3 distinct + 점수 정렬 | 100% (EP-EX12 활용) |
| **EP-19 복원 UI** | audit timestamp → snapshot | 100% (EP-11 활용) |
| **EP-20 폴더 watch** | 신규 파일 자동 upload | REQ-FUNC-OC-014 100% |
| **Vite bundle** | gzip 크기 | ≤ 200kB |
| **TypeScript strict** | 0 error | 100% |
| **회귀** | E2E (Playwright) + 백엔드 회귀 유지 | 100% |

---

## 7. Phase 2 React 진입 — Frontend Stack 확정

| 영역 | 라이브러리 | 버전 | 이유 |
|---|---|---|---|
| Framework | React | 18.3.x | Concurrent + Suspense |
| Build | Vite | 5.4.x | ESM + HMR (Webpack 대비 10배 빠름) |
| Language | TypeScript | 5.5.x | strict mode |
| Grid | AG Grid Enterprise | 32.x | 1500 row 가상 스크롤 + 라이센스 (사내) |
| State (server) | TanStack Query | 5.x | 캐시 + invalidation |
| State (client) | Zustand | 4.x | 경량 (Redux 대비) |
| Style | TailwindCSS | 3.x | utility-first |
| Date | dayjs | 1.11.x | Asia/Seoul (BR-X04) |
| WebSocket | sockjs-client + @stomp/stompjs | 1.6.x + 7.x | STOMP over WebSocket |
| Test (unit) | Vitest | 2.x | Vite 통합 |
| Test (E2E) | Playwright | 1.45.x | Chromium + Firefox + WebKit |

---

## 8. 진입 게이트 체크리스트 (Sprint 4 완료 → Sprint 5 진입)

- [x] **Sprint 4 7 Epic 100% 완료** (EP-10·11·12·13·14·EX13·EX14) — Sprint-4_Completion §10
- [x] **거버넌스 4-layer 통과** (DB trigger + AOP + RBAC + immutability)
- [x] **REST API 안정** (Sprint 4 신규 — confirm + override + export + EX matrix)
- [x] **WebSocket STOMP 인프라** (/ws/notifications + /topic/extrusion-updates)
- [x] **Modulith verify 0 위반** + ArchUnit 29 rule 통과
- [x] **AI harness 안정** (Sprint 0~4 누적 95 commit · 머지 충돌 0)

→ **Sprint 5 진입 승인 가능**. Phase A (Frontend 셋업) 즉시 시작 가능.

---

## 9. 잠재 리스크 + 완화 전략

| 리스크 | 영향 | 완화 |
|---|---|---|
| AG Grid Enterprise 라이센스 | 빌드 차단 | 라이센스 키 환경 변수 주입, dev mode 워터마크 허용 |
| 1500 row × 30 col 렌더 p95 | UX 저하 | 가상 스크롤 + cellRenderer memoization + columnsToFit lazy |
| SockJS reconnect 처리 | 알림 누락 | exponential backoff + last-event-id 기반 catchup |
| TypeScript ↔ Java record 동기화 | 타입 drift | OpenAPI spec 자동 생성 (Springdoc → openapi-typescript) |
| Vite proxy 인증 (Keycloak) | CORS / SAML | dev 환경 mock JWT, 실 환경 Keycloak SPA flow |
| 첫 Frontend 진입 학습 곡선 | velocity 감소 | 보수적 SP 산정 (50 capacity 중 21 SP 활용) |

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-22 | Claude Code | 초안 — Sprint 5 진입 계획 (EP-15·16·17·18·19·20 = ~21 SP, critical path 10 SP, Frontend React/Vite 본격 진입) |
