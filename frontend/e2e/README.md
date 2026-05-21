# Playwright E2E 시나리오 — TK-04-3-3 (EP-04 ST-04-3)

`frontend/e2e/` 의 Playwright 스펙은 **TC-VC-004** + **REQ-FUNC-CO-010** + **NS-S03** 검증 목적.
Sprint 2 baseline 단계 — 코드 형태로 보관, 실제 실행은 STG 환경 가동 + 브라우저 설치 후.

---

## 디렉터리

```
e2e/
  vc-scheduling/
    slot-drag-guard.spec.ts          # TC-VC-004 100건 드래그 + ≤1초 차단
    override-justification.spec.ts   # REQ-FUNC-CO-010 사유 ≥10자 강제
    p4-persona-uat.spec.ts           # P4 단독 사용 + 한국어 UI 시연
  fixtures/
    drag-scenarios-100.json          # DS-VC-CONSTRAINT-47 100 시나리오 (backend 데이터셋과 동기)
```

---

## 사전 준비

1. **Frontend dev server 가동** — `cd frontend && npm run dev` (http://localhost:5173)
2. **Backend (with-infra) 가동** — Testcontainers 또는 STG 환경
3. **Playwright 브라우저 설치** — `npx playwright install chromium edge`
4. **VcGanttBoard 라우트 활성** — `/orders/vc-board` (Sprint 2+ 라우터 추가)
5. **자동 로그인 setup** (선택) — STG Keycloak SSO 시 `playwright.auth.json` 사전 생성

---

## 실행

```bash
# 로컬 (dev server 가동 후)
cd frontend
npx playwright test

# STG
PLAYWRIGHT_BASE_URL=http://stg.intranet npx playwright test

# 특정 spec
npx playwright test slot-drag-guard.spec.ts

# Edge 만
npx playwright test --project=edge
```

---

## Acceptance Criteria 매트릭스

| spec | TC | 검증 항목 |
|---|---|---|
| `slot-drag-guard.spec.ts` | TC-VC-004 | 100건 회귀 (적합 50 / 비적합 50), 차단 ≤1초 |
| `override-justification.spec.ts` | REQ-FUNC-CO-010 | 사유 ≥10자 강제, audit 호출 + 토스트 |
| `p4-persona-uat.spec.ts` | NS-S03 | 5건 단독 배치, 한국어 UI 100% |

---

## Jenkins CI 통합 (예정 — EP-32 Sprint 2 후속)

```groovy
stage('E2E — VC drag guard') {
    steps {
        sh '''
          cd frontend
          npx playwright install --with-deps chromium
          npx playwright test --reporter=junit,html
        '''
        archiveArtifacts artifacts: 'frontend/playwright-report/**'
        junit 'frontend/test-results/junit.xml'
    }
    post {
        failure {
            slackSend(channel: '#ops-scheduling', color: 'danger',
                message: "VC drag guard E2E FAILED — REQ-FUNC-VC-004 위반")
        }
    }
}
```

---

## Sprint 2 baseline 한계

- Playwright 브라우저 미설치 (140MB) — STG 환경 활성 후 별 PR
- VcGanttBoard `/orders/vc-board` 라우트 미연결 — 후속 Frontend Task (HomePage 또는 navigation menu)
- Override audit endpoint backend stub — EP-11 Sprint 2 활성 후 실 동작
- Visual regression (Playwright VR) — Phase 1.0 후반 도입

---

## 참조

- TC-VC-004 — Phase 2/5.TestPlan/TEST-001
- REQ-FUNC-VC-004 — Phase 2/2.SRS/SRS-001 §4.3
- REQ-FUNC-CO-010 — Phase 2/2.SRS/SRS-001 §4.6
- NS-S03 (P4 페르소나) — Phase 1/1.JTBD/personas.md
