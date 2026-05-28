# Sprint 24 진입 계획 — EP-OPS-FEEDBACK (베타 1개월 피드백 반영) v1.0

**작성일**: 2026-05-28 | **버전**: 1.0 | **상태**: Phase 4 다섯 번째 sprint 진입 권고안 (S20~S23 완료 후 + 베타 1개월 데이터)

> **참조**: [PHASE-4_STABILIZATION_v1.0 §3 S24](PHASE-4_STABILIZATION_v1.0.md) + [BETA_RUNBOOK_v1.0](../../docs/cutover/BETA_RUNBOOK_v1.0.md) + 베타 1개월 KPI 측정 결과

---

## 1. 목적

**Sprint 19 EP-BETA-LAUNCH cutover 후 약 4주 운영 데이터 + 사용자 8명 정성 인터뷰 기반 안정화 sprint. Sprint 25 EP-PROD-LAUNCH 진입 직전 UX/운영 미세 조정.**

| 영역 | Sprint 19 baseline | Sprint 24 반영 |
|---|---|---|
| 매뉴얼 v1.x | 텍스트 only | ✅ **스크린샷 추가** (`docs/manual/screenshots/`) 4 role × 5+ 핵심 화면 |
| UX 미세 조정 | 베타 baseline | ✅ **사용자 8명 인터뷰 기반 12+ 미세 fix** (Drawer 위치 / Batch UI / Banner / Modal 등) |
| Grafana alert | Sprint 19 임계 (기본값) | ✅ **1개월 데이터 기반 threshold 재조정** (Critical retry / Kakao 도달률 / HTTP p95) |
| KPI 측정 보고 | 없음 | ✅ **베타 보고서 v1.0** — 1개월 정량 지표 + 정성 인터뷰 + Sprint 25 PROD 진입 권고 |

**Pre-Phase 의존 (Sprint 24 진입 전 필수):**
- 베타 운영 4주 누적 (S20~S23 진행 동안 자연스럽게 충족)
- 사용자 8명 정성 인터뷰 1회 (S23 종료 직후 — 30분 × 8명)
- Grafana 1개월 metric raw 추출

**활성 후 효과:**
- 매뉴얼 신규 사용자 (Sprint 25 30명+ 확장) 자체 학습 가능 → IT_OPS 교육 부담 ↓
- UX 미세 조정 → 사용자 만족도 KPI 4.0/5.0+ 달성 (베타 → PROD 전환 게이트)
- Grafana 정교화 → false alert ↓ + 실 issue 조기 발견율 ↑

---

## 2. Sprint 24 SP·기간

| Story | SP | 추정 PD |
|---|:--:|:--:|
| ST-FB-1 매뉴얼 스크린샷 추가 (4 role × 5+ 핵심 화면) | 1.0 | 0.5 |
| ST-FB-2 UX 미세 조정 (12+ 사용자 요청 반영) | 1.5 | 0.8 |
| ST-FB-3 Grafana alert rule 정교화 (1개월 데이터 기반) | 1.0 | 0.5 |
| ST-FB-4 베타 보고서 v1.0 (KPI + 인터뷰 + PROD 진입 권고) | 0.5 | 0.2 |
| **합계** | **~4 SP** | **~2 PD** |

---

## 3. 의존성 DAG

```
Pre-Phase (베타 4주 데이터 + 인터뷰 8명)
    ↓
ST-FB-1 (스크린샷) ──┐
                    │
ST-FB-2 (UX 조정) ──┤  (각 독립)
                    │
ST-FB-3 (Grafana) ──┤
                    ↓
                ST-FB-4 (베타 보고서)
                    ↓
              Sprint 25 EP-PROD-LAUNCH
```

---

## 4. Story · Task 매트릭스

### ST-FB-1 — 매뉴얼 스크린샷 (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-FB-1-1 | `docs/manual/screenshots/` 폴더 생성 + 4 role 별 폴더 (planner / stk-user / it-ops / read-only) | 0.1 |
| TK-FB-1-2 | PLANNER — 5+ 화면 (로그인 / 수주 import / Diff / VC 시뮬뷰 + batch / ConfirmModal / Drawer) | 0.3 |
| TK-FB-1-3 | STK_USER — 3 화면 (로그인 / 시뮬뷰 read / swap 제안) | 0.2 |
| TK-FB-1-4 | IT_OPS — 5+ 화면 (5 entity Admin / Excel 폴백 / PIN reset / Grafana) | 0.3 |
| TK-FB-1-5 | READ_ONLY — 2 화면 (시뮬뷰 read / 감사 로그) + USER_MANUAL_v1.4 → v1.5 (스크린샷 링크 일괄 추가) | 0.1 |

### ST-FB-2 — UX 미세 조정 (1.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-FB-2-1 | 인터뷰 데이터 분석 + 12+ issue 분류 (UI 위치 / 메시지 / 워크플로우 / 단축키 / 성능) | 0.3 |
| TK-FB-2-2 | 우선순위 P0 issue 4건 fix (예: Drawer 위치 / Batch 전체 선택 UX / DegradedBanner 가시성 / ConfirmModal 분기 메시지) | 0.5 |
| TK-FB-2-3 | P1 issue 4건 fix (Tag 색상 / 폰트 크기 / Loading 인디케이터 / 한국어 명확화) | 0.3 |
| TK-FB-2-4 | P2 issue 4건 fix (단축키 추가 / 다중 탭 정합 / 빈 상태 안내 / 검색 필터) | 0.2 |
| TK-FB-2-5 | vitest 회귀 — 변경 컴포넌트 단위 테스트 신규 4+ cases | 0.2 |

### ST-FB-3 — Grafana alert 정교화 (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-FB-3-1 | 1개월 metric raw 추출 — Prometheus query 시계열 (Critical retry / Kakao 도달 / HTTP p95 / MES degraded) | 0.3 |
| TK-FB-3-2 | threshold 재조정 — 1σ baseline + 2σ alert (예: Kakao 도달률 95% 이하 30분 → Slack alert) | 0.3 |
| TK-FB-3-3 | `infrastructure/observability/grafana/alerts/` Grafana alert rule YAML 추가 (notify-sprint18 dashboard 와 연결) | 0.2 |
| TK-FB-3-4 | IT_OPS 매뉴얼 §3.3 — Grafana alert 대응 절차 추가 (각 alert 별 1차 대응 + 2차 escalation) | 0.2 |

### ST-FB-4 — 베타 보고서 v1.0 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-FB-4-1 | `docs/reports/BETA_REPORT_v1.0.md` — §1 운영 4주 KPI 정량 (HTTP p95 / Critical retry / Kakao 도달 / MES degraded duration) + §2 사용자 인터뷰 8명 정성 요약 + §3 issue 12+ resolution status + §4 Sprint 25 PROD 진입 권고 (Go/No-Go) | 0.4 |
| TK-FB-4-2 | 사내 IT/관리팀 + 베타 사용자 8명 보고서 공유 | 0.1 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ 매뉴얼 v1.5 — 4 role × 5+ 스크린샷 (총 15+ 이미지)
2. ✅ UX P0/P1/P2 12+ issue 모두 resolution status closed
3. ✅ Grafana alert rule 4+ (Critical / Kakao / p95 / MES degraded)
4. ✅ 매뉴얼 §3.3 IT_OPS Grafana alert 대응 절차
5. ✅ 베타 보고서 v1.0 — 사내 IT/관리팀 공유 + 사용자 8명 배포
6. ✅ Sprint 25 PROD 진입 Go/No-Go 권고 결정

**비기능 DoD:**
1. ✅ ArchUnit GREEN
2. ✅ Backend IT 회귀 0
3. ✅ TypeScript compile + vitest 신규 4+ + 기존 GREEN
4. ✅ 사용자 만족도 KPI ≥ 4.0/5.0 (인터뷰 평균)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| 사용자 인터뷰 8명 일정 조정 어려움 (4주차 운영 중) | S24 진입 지연 | 30분 단위 분할 가능 + IT_OPS 가 1차 수집 후 개발 분석 |
| UX P0 issue 4건 fix scope 초과 (사용자 요청 다양) | Sprint 4 SP 초과 | P0 만 sprint 안 + P1/P2 Phase 5+ carry-over |
| Grafana alert rule false positive 다수 (1개월 데이터 부족) | 운영 noise | 2σ 임계 보수적 시작 → Phase 5+ 운영 3개월 후 재조정 |
| 베타 보고서 정량 데이터 부족 (Slack/Kakao 실 발급 시점 차이) | 보고서 신뢰도 ↓ | S20 Slack/Kakao 발급 시점 명시 + 측정 기간 분리 (예: Kakao 2주 측정) |

---

## 7. 작업 순서 추천

**Day 1** — 인터뷰 분석 + 스크린샷 + Grafana:
1. TK-FB-2-1 (인터뷰 분석)
2. TK-FB-1-1~5 (스크린샷 4 role)
3. TK-FB-3-1~2 (Grafana metric 분석 + threshold)

**Day 2** — UX fix + alert rule:
4. TK-FB-2-2~5 (P0/P1/P2 fix)
5. TK-FB-3-3~4 (alert rule YAML + 매뉴얼)

**Day 3** — 베타 보고서:
6. TK-FB-4-1~2 (보고서 + 공유)

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Docs Manual | USER_MANUAL_v1.5.md (스크린샷 링크) + docs/manual/screenshots/ 폴더 |
| Frontend | 12+ component 미세 조정 (vitest 회귀 신규 4+) |
| Infra Grafana | alerts/ 폴더 + 4+ alert rule YAML (Critical / Kakao / p95 / MES degraded) |
| Docs Cutover | docs/manual/v1.5 §3.3 Grafana alert 대응 절차 |
| Docs Reports | docs/reports/BETA_REPORT_v1.0.md (KPI + 인터뷰 + PROD 진입 권고) |

---

## 9. Sprint 24 후 다음 단계

**Sprint 25 (EP-PROD-LAUNCH) 진입 조건:**
- ✅ DoD 10/10 충족
- ✅ 베타 보고서 Go 결정 (No-Go 시 Phase 5 추가 sprint carry-over)
- ⏳ 사내 IT — 30명+ 사용자 사전 안내 + Keycloak 신규 sync 준비

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 다섯 번째 sprint EP-OPS-FEEDBACK 4 Story / 16 Task / ~4 SP 분해. 베타 1개월 KPI + 인터뷰 8명 기반 UX/문서/alert 안정화. DoD 10 + 리스크 4. Pre-Phase 베타 4주 데이터 누적 + 인터뷰. Sprint 25 PROD 진입 직전 마지막 게이트. |
