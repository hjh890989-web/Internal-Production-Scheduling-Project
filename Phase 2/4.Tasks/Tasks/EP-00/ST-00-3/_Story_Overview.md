# Story Overview — [EP-00] ST-00-3 React + Vite 프론트엔드 골격

**Sprint**: S0 (Phase 0 사전 준비) | **Epic**: EP-00 인프라 기반 셋업 (Foundation) | **Priority**: Must
**SP 합계**: 2 | **PD 추정**: ~1.4 PD (2 SP × 0.7 PD)

---

## Story 목적

> SAD §5.2 인용: "**TypeScript 5.4 + React 18.3 + Vite 5.x + Ant Design 5.x** — 한국 시장 압도적 점유, 풍부한 생태계, Ant Design 호환, 한국어 i18n 내장"

본 Story는 **Frontend SPA(Single Page Application)의 골격**을 셋업한다. SAD §5.2 + ADR-009에서 정의한 프론트엔드 스택을 Vite 5 프로젝트로 부트스트랩하고, **3가지 핵심 인프라**를 셋업:
1. **빌드 시스템**: Vite 5 + TypeScript 5.4 + React 18.3 (TK-00-3-1)
2. **UI 컴포넌트 + i18n**: Ant Design 5 한국어 로케일 + i18next 한국어 단일 (TK-00-3-2)
3. **라우팅 + 상태관리**: React Router 6 + Zustand 4 + TanStack Query 5 (TK-00-3-3)

본 Story는 **빌드 산출물(`dist/`)을 NGINX 정적 파일 볼륨에 마운트**할 수 있는 상태까지 — Sprint 1+ UI Epic에서 페이지·컴포넌트를 구체화.

**Why 본 Story가 Phase 0 핵심 작업인가**:
- 모든 UI Epic (EP-07 시뮬뷰·EP-15 매트릭스·EP-17 카톡 백업 + Sprint 2~5 모든 UI Story) 본 골격 위에 구현
- ST-00-1 NGINX 컨테이너의 `frontend-static` 볼륨 placeholder 교체 → Sprint 0 DoD 항목 1 (5 컨테이너 부팅) 완결 기여
- ADR-009 의사결정 검증 — Vite + Ant Design 5 + 한국어 i18n 조합이 실제로 빌드되고 동작하는지 사전 확인
- REQ-NF-USA-003 (한국어 UI 100% 커버리지) 기반 — 본 Story가 i18n 셋업으로 모든 UI Story의 한국어 강제

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-00-3-1](TK-00-3-1.md) | Vite 5 + TypeScript 5.4 + React 18.3 프로젝트 + Dockerfile | 0.5 | 프론트엔드 | T-U + I | ☐ |
| [TK-00-3-2](TK-00-3-2.md) | Ant Design 5 + i18next 한국어 로케일 + theme provider | 0.5 | 프론트엔드 | T-U + UAT | ☐ |
| [TK-00-3-3](TK-00-3-3.md) | React Router 6 + Zustand 4 + TanStack Query 5 골격 | 0.4 | 프론트엔드 | T-U | ☐ |

> **선행 의존**: ST-00-1 (NGINX 정적 파일 서빙 — 빌드 산출물 마운트 대상)
> **후행 차단**: Sprint 2~5 모든 UI Story (EP-04 ST-04-3 dnd-kit, EP-07·EP-15·EP-17·EP-18·EP-19·EP-20 등)

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] `npm run build` 한 번에 성공 (≤30초, Vite 빌드 속도)
- [ ] 빌드 산출물(`frontend/dist/`)이 ST-00-1 NGINX `frontend-static` 볼륨에 마운트됨
- [ ] `docker compose up`으로 NGINX → frontend SPA 진입 → **한국어 hello world 페이지 200 OK**
- [ ] React Router 라우팅 동작 (`/`·`/about` 등 기본 라우트)
- [ ] Zustand 상태 변경 + TanStack Query mock fetch 시나리오 통합 테스트 PASS
- [ ] Ant Design 컴포넌트(`Button`·`Form`·`DatePicker`) 1개 이상 사용 + 한국어 로케일 적용 확인
- [ ] **i18next 한국어 번역 100% 커버리지** — 모든 UI 텍스트가 `t('key')` 또는 정적 한국어 문자열 (영어 leak 0건)
- [ ] **REQ-NF-USA-003** 정합 — 한국어 UI 100% 커버리지 가이드 (Sprint 1+ UI Story 적용 표준)
- [ ] ESLint + Prettier + TypeScript strict 모드 모두 PASS
- [ ] Vitest 표본 단위 테스트 1개 이상 PASS
- [ ] Sprint Review 데모: `docker compose up` → NGINX → 한국어 SPA 진입 + Ant Design Button 클릭 → Zustand state 변경 시연

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-00 ST-00-3
- **SAD ADR**:
  - **ADR-009** ("**(a) React 18 + TS + Vite + Ant Design 5** — 한국 점유율 1위, Ant Design 한국어 i18n 내장, 풍부한 간트·드래그앤드롭 생태계, 채용 용이")
- **SAD §5.2 프론트엔드 스택** (완전 명세):
  - 언어: TypeScript 5.4
  - 프레임워크: React 18.3
  - 빌드: Vite 5.x
  - UI: Ant Design 5.x
  - 서버 상태: TanStack Query 5.x (캐싱·재시도·낙관적 업데이트)
  - 클라이언트 상태: Zustand 4.x (Redux 대비 boilerplate ↓)
  - 라우팅: React Router 6.x
  - 간트/스케줄 뷰: Frappe Gantt + 커스텀 React 래퍼 (S5에서 사용)
  - 매트릭스: AG Grid Community (S5에서 사용)
  - 드래그앤드롭: dnd-kit 6.x (REQ-FUNC-VC-004, S2에서 사용)
  - WebSocket: @stomp/stompjs + sockjs-client (S4에서 사용)
  - 차트: Recharts 2.x (Phase 2+)
  - 테스트: Vitest + React Testing Library + Playwright
  - i18n: i18next 23.x (한국어 단일이나 향후 확장 대비)
- **SRS REQ-NF**:
  - **REQ-NF-USA-003** ("모든 사용자 가시 텍스트는 한국어") → i18next 셋업이 본 Story 책임
  - **REQ-NF-USA-004** ("플래너 UI ≥1280×800, 현장 패드 ≥1024×768 가로") → 본 Story에서 viewport meta 설정
  - **REQ-NF-PER-005** ("UI 페이지 응답 p95 ≤1초") → Vite code splitting + lazy load 기본 적용
  - **REQ-NF-COM-005** ("최신 2개 메이저 Chromium 기반 브라우저") → `browserslist` 설정
- **연관 Story**:
  - 선행: [ST-00-1 TK-00-1-3](../ST-00-1/TK-00-1-3.md) (NGINX — SPA 정적 파일 서빙 + SPA fallback `/index.html`)
  - 후행: Sprint 2~5 모든 UI Story — 본 Story의 패턴 위에 구체 페이지·컴포넌트 구현

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | 초안 작성. 성격: 프론트엔드 골격(Vite + React + TS) — ST-99-1/99-2(데이터)·ST-00-1(인프라)·ST-00-2(백엔드)와 다른 5번째 도메인 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.2 EP-00 ST-00-3 + SAD ADR-009·§5.2 기반. Task 기반 분해 v1 다섯 번째 적용 (프론트엔드 — 5번째 도메인 패턴 강건성 검증 완결) |
