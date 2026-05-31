# USER_MANUAL Screenshot Baseline — Sprint 24 S24-A

본 디렉터리는 `USER_MANUAL_v1.4` (Sprint 25 예정) 의 페르소나별 스크린샷 자산을 보관합니다.

---

## 디렉터리 구조

```
docs/manual/screenshots/
  planner/        # ROLE_PLANNER — 생산계획 담당자 (6 화면)
  stk-user/       # ROLE_STK_USER — 현장 STK (3 화면)
  it-ops/         # ROLE_IT_OPS — IT 운영 (3 화면, sample 가용 부분만)
  read-only/      # ROLE_READ_ONLY — 감사·임원 (2 화면)
```

각 sub-folder 의 `.gitkeep` 으로 빈 디렉터리 보존 (실 PNG 는 spec 실행 후 생성).

---

## 캡처 spec

- 파일 — `frontend/e2e/manual-capture/sample-screenshots.spec.ts`
- 시드 의존 — `V039` PLANNER + `99999-SAMPLE` 호스 (Sprint 0 sample seed)
- 페르소나 — 4 role × 14 화면 (sample 가능 분량)

---

## 실행

```bash
# 1. STG 또는 dev 환경 가동 (frontend + backend with-infra + Keycloak sample realm)
cd frontend
npm run dev   # http://localhost:5173

# 2. Playwright 브라우저 설치 (최초 1회)
npx playwright install chromium

# 3. 스크린샷 캡처 실행 (manual-capture 그룹만)
npx playwright test e2e/manual-capture/ --reporter=line

# 4. STG 대상
PLAYWRIGHT_BASE_URL=http://stg.intranet npx playwright test e2e/manual-capture/
```

캡처 결과는 본 디렉터리 (`../docs/manual/screenshots/<role>/NN-name.png`) 에 `fullPage` 형식으로 저장됩니다.

---

## Carry-over (S24-B / Sprint 25)

- **실 캡처 실행** — Docker stack + sample seed 활성 후 (~10분 소요) 사용자 또는 Sprint 25 단계에서 실행
- **USER_MANUAL_v1.4 발행** — 캡처 PNG 링크 삽입 + revision history 추가
- **IT_OPS 운영 의존 화면** — Grafana 대시보드 / Excel 폴백 / 감사 변조 알림 (sample seed 부재) 은 운영 환경 캡처
- **Visual Regression** — Phase 1.0 후반 검토 (기준 PNG ↔ 신 PNG diff)

---

## 참조

- WBS Sprint 24 — `Phase 2/4.Tasks/WBS-001_v1.5.md` (S24-A ST-FB-1)
- 기존 매뉴얼 — `docs/manual/USER_MANUAL_v1.3.md`
- Playwright 설정 — `frontend/playwright.config.ts`
