# Sprint 24 진입 계획 — EP-OPS-FEEDBACK (베타 1개월 피드백 반영) v1.1

**작성일**: 2026-06-01 | **버전**: 1.1 | **상태**: Phase 4 다섯 번째 sprint **조기 진입 모드 (S24-A / S24-B 분할)**

> **참조**: [PHASE-4_STABILIZATION_v1.0 §3 S24](PHASE-4_STABILIZATION_v1.0.md) + [BETA_RUNBOOK_v1.0](../../docs/cutover/BETA_RUNBOOK_v1.0.md) + [PLAN-SPRINT-24 v1.0](PLAN-SPRINT-24_EP-OPS-FEEDBACK_v1.0.md)

---

## 0. v1.0 → v1.1 변경 요약

1. **Pre-Phase 14% 충족 → 정식 진입 불가, 분할.** 베타 사용자 발급 0/8, 운영 누적 0/4주, 인터뷰 0/8명 — 사전 게이트 미충족. 정식 v1.0 4 SP 일괄 진입 보류, **early-entry 가능 범위만 S24-A 로 분리**, 잔여는 **S24-B carry-over** (Phase 5+ 베타 4주 운영 후 진입).
2. **S24-A 조기 (~1.5 SP, 0.7 PD)** — ST-FB-1 sample 캡처 12~13장 + ST-FB-2 P0 4건 (vitest 4 fix + AntD 3 deprecation + Drawer/selectAll UX + DegradedBanner 시각) + ST-FB-3 골격 (alert YAML 4 + MES Grafana 패널 + Micrometer Counter/Timer 노출).
3. **S24-B carry-over (~2.5 SP, Phase 5+ 베타 4주 후)** — ST-FB-1 실 운영 화면 (Grafana 실 데이터 / Excel 폴백 실 사례 / 감사 로그 실 row) + ST-FB-2 P1/P2 (AG Grid 재도입 / Kakao 부활 / 인터뷰 12+ fix) + ST-FB-3 threshold 재조정 (1개월 metric raw 기반) + ST-FB-4 베타 보고서 v1.0 (KPI + 인터뷰 8명 + Go/No-Go).
4. **BETA_REPORT 골격** — S24-A 산출물로 **v0.1 (골격)** 발행 (목차 + KPI 항목 + 측정 plan), **v1.0** 은 S24-B 산출물 명시.
5. **DoD 갱신 (10 → 7)** — S24-A 한정 — 정량 KPI / Go-No-Go 결정 등 4주 운영 의존 항목은 S24-B 로 이동.

---

## 1. 목적 (early-entry 명시)

**v1.0 정식 진입 조건 (베타 운영 4주 + 인터뷰 8명) 미충족 → S24-A 조기 진입으로 sample/골격/P0 차원 작업만 수행.** 베타 사용자 발급 + 4주 운영 + 인터뷰 8명 완료 시점에 S24-B 정식 진입.

| 영역 | v1.0 baseline | **S24-A 조기 (지금)** | **S24-B carry-over (Phase 5+)** |
|---|---|---|---|
| 매뉴얼 스크린샷 | 텍스트 only | **sample 캡처 12~13장** (개발 환경) | **실 운영 화면 교체** (Grafana real / Excel 폴백 real / 감사 row real) |
| UX fix | baseline | **P0 4건** (vitest 4 / AntD 3 deprecation / Drawer+selectAll / DegradedBanner) | **P1/P2** (AG Grid 재도입 / Kakao 부활 / 인터뷰 12+ 기반) |
| Grafana alert | 기본값 | **골격** (alert YAML 4 + MES 패널 + Counter/Timer 노출) | **threshold 재조정** (1σ baseline / 2σ alert, 1개월 raw 기반) |
| 베타 보고서 | 없음 | **v0.1 골격** (목차 + KPI 항목 + 측정 plan) | **v1.0 정식** (KPI 정량 + 인터뷰 정성 + Go/No-Go) |

**S24-A 진입 효과:** 베타 4주 누적 동안 sample/골격 선행 → S24-B 진입 시 정식 작업이 교체/조정 만으로 단축. early UX P0 fix 는 베타 발급 전 안정화로 흡수.

---

## 2. Sprint 24-A SP·기간 (1.5 SP / 0.7 PD)

| Story | S24-A SP | S24-B 잔여 | PD |
|---|:--:|:--:|:--:|
| ST-FB-1 매뉴얼 sample 스크린샷 12~13장 (개발 환경) | 0.4 | 0.6 (실 운영 교체) | 0.2 |
| ST-FB-2 UX P0 4건 fix (vitest/AntD/Drawer/Banner) | 0.5 | 1.0 (P1/P2) | 0.25 |
| ST-FB-3 Grafana alert 골격 (YAML 4 + 패널 + 메트릭) | 0.6 | 0.4 (threshold 재조정) | 0.25 |
| ST-FB-4 베타 보고서 v0.1 골격 | — | 0.5 (v1.0 정식) | — |
| **S24-A 합계** | **~1.5 SP** | **~2.5 SP (S24-B)** | **~0.7 PD** |

---

## 3. 의존성 DAG (S24-A 5 task 독립 병렬)

```
S24-A (지금, Pre-Phase 미충족 — sample/골격만)
  ├─ ST-FB-1 sample 캡처 (TK-1-1~5)        ─┐
  ├─ ST-FB-2 P0 fix (TK-2-2 vitest)          ├─ 독립 병렬 가능
  ├─ ST-FB-2 P0 fix (TK-2-3 AntD deprec)     │  (5 task DAG 교차 없음)
  ├─ ST-FB-2 P0 fix (TK-2-4 Drawer+selAll)   │
  ├─ ST-FB-3 alert YAML + 패널 + 메트릭      ┘
                  ↓
            BETA_REPORT v0.1 골격 발행
                  ↓
   ━━━━━━━ 베타 사용자 8명 발급 + 4주 운영 + 인터뷰 8명 ━━━━━━━
                  ↓
S24-B (Phase 5+, 정식 진입)
  ├─ ST-FB-1 실 운영 화면 교체 (Grafana real / Excel 폴백 / 감사 row)
  ├─ ST-FB-2 P1/P2 (AG Grid / Kakao / 인터뷰 12+)
  ├─ ST-FB-3 threshold 재조정 (1개월 metric raw)
  └─ ST-FB-4 베타 보고서 v1.0 (KPI + 인터뷰 + Go/No-Go)
                  ↓
            Sprint 25 EP-PROD-LAUNCH
```

---

## 4. Story · Task 매트릭스 (TK 별 S24-A / S24-B 라벨)

### ST-FB-1 — 매뉴얼 스크린샷

| Task | 내용 | 라벨 | SP |
|---|---|:--:|:--:|
| TK-FB-1-1 | `docs/manual/screenshots/` 폴더 + 4 role 별 폴더 생성 | **S24-A** | 0.05 |
| TK-FB-1-2 | PLANNER sample 5장 (로그인/수주 import/Diff/시뮬뷰+batch/ConfirmModal) | **S24-A** | 0.15 |
| TK-FB-1-3 | STK_USER sample 3장 (로그인/시뮬뷰 read/swap 제안) | **S24-A** | 0.08 |
| TK-FB-1-4 | IT_OPS sample 3장 (5 entity Admin / Excel 폴백 stub / PIN reset) | **S24-A** | 0.08 |
| TK-FB-1-5 | READ_ONLY sample 2장 (시뮬뷰 read / 감사 stub) + USER_MANUAL v1.4 스크린샷 링크 삽입 | **S24-A** | 0.04 |
| TK-FB-1-6 | **실 운영 교체** — Grafana 실 데이터 / Excel 폴백 실 사례 / 감사 로그 실 row + USER_MANUAL v1.5 발행 | **S24-B** | 0.6 |

### ST-FB-2 — UX 미세 조정

| Task | 내용 | 라벨 | SP |
|---|---|:--:|:--:|
| TK-FB-2-1 | 인터뷰 데이터 분석 + 12+ issue 분류 | **S24-B** | 0.3 |
| TK-FB-2-2 | **P0 vitest 4 fix** (회귀 실패 4건 해소) | **S24-A** | 0.15 |
| TK-FB-2-3 | **P0 AntD 3 deprecation** (Modal/Drawer/Table API 갱신) | **S24-A** | 0.15 |
| TK-FB-2-4 | **P0 Drawer 위치 + Batch selectAll UX** | **S24-A** | 0.1 |
| TK-FB-2-5 | **P0 DegradedBanner 시각 강화** (대비/위치/아이콘) | **S24-A** | 0.1 |
| TK-FB-2-6 | P1 — AG Grid 재도입 + Kakao 부활 검토 | **S24-B** | 0.5 |
| TK-FB-2-7 | P1/P2 인터뷰 기반 12+ fix (Tag/폰트/Loading/단축키/탭 정합/빈 상태/필터) | **S24-B** | 0.5 |

### ST-FB-3 — Grafana alert 정교화

| Task | 내용 | 라벨 | SP |
|---|---|:--:|:--:|
| TK-FB-3-1 | **alert YAML 4건 골격** (Critical retry / Kakao 도달 / HTTP p95 / MES degraded) `infrastructure/observability/grafana/alerts/baseline-alerts.yml` | **S24-A** | 0.2 |
| TK-FB-3-2 | **MES Grafana 패널 추가** `mes-sprint23.json` (polling success / degraded duration / latency p95) | **S24-A** | 0.2 |
| TK-FB-3-3 | **Micrometer Counter/Timer 노출** (mes.poll.success / mes.degraded.duration / mes.fetch.latency) | **S24-A** | 0.15 |
| TK-FB-3-4 | helper script — alert rule 검증 (`scripts/validate-alerts.sh`) | **S24-A** | 0.05 |
| TK-FB-3-5 | **threshold 재조정** (1σ baseline / 2σ alert, 1개월 metric raw 기반) | **S24-B** | 0.3 |
| TK-FB-3-6 | IT_OPS 매뉴얼 §3.3 alert 대응 절차 (1차/2차 escalation) | **S24-B** | 0.1 |

### ST-FB-4 — 베타 보고서

| Task | 내용 | 라벨 | SP |
|---|---|:--:|:--:|
| TK-FB-4-0 | **BETA_REPORT v0.1 골격** (목차 + KPI 항목 + 측정 plan + S24-A 산출물 인덱스) | **S24-A** | 0.0 (deliverable 포함) |
| TK-FB-4-1 | **v1.0 정식** — §1 운영 4주 KPI + §2 인터뷰 8명 정성 + §3 12+ issue resolution + §4 PROD Go/No-Go | **S24-B** | 0.4 |
| TK-FB-4-2 | 사내 IT/관리팀 + 베타 사용자 8명 공유 | **S24-B** | 0.1 |

---

## 5. Definition of Done (S24-A 한정 7건)

1. sample 스크린샷 12~13장 (4 role) — `docs/manual/screenshots/` 누적
2. USER_MANUAL v1.4 스크린샷 링크 일괄 삽입 (실 교체는 S24-B v1.5)
3. P0 UX 4건 fix — vitest 4 GREEN + AntD 3 deprecation 해소 + Drawer/selectAll UX + DegradedBanner 시각
4. Grafana alert YAML 4건 골격 (baseline-alerts.yml)
5. MES Grafana 패널 (mes-sprint23.json) + Micrometer Counter/Timer 노출
6. BETA_REPORT v0.1 골격 발행 (목차 + KPI 항목 + 측정 plan)
7. ArchUnit GREEN + Backend IT 회귀 0 + frontend vitest GREEN

---

## 6. 리스크 + 회피 (early-entry 신규)

| 리스크 | 영향 | 회피 |
|---|---|---|
| **sample 캡처 ↔ 실 운영 화면 차이** — 개발 환경 screenshot 이 베타 UI 와 미세 다름 | 사용자 혼동 | S24-B 에서 실 운영 화면으로 교체 (TK-FB-1-6) + USER_MANUAL v1.4 → v1.5 전환 명시 |
| **baseline alert false positive** — 운영 데이터 0개 상태에서 임계 설정 | 운영 noise | YAML 골격만 발행 + threshold 는 S24-B 에서 1개월 metric raw 기반 재조정 (TK-FB-3-5) |
| **AntD deprecation 회귀** — Modal/Drawer/Table API 변경이 다른 컴포넌트 영향 | UI breakage | vitest 회귀 4+ 케이스 신규 + 변경 컴포넌트 단위 격리 |
| **BETA_REPORT v0.1 ↔ v1.0 일관성** — 골격 → 정식 변환 시 KPI 항목 누락 | 보고서 신뢰도 ↓ | v0.1 골격에 측정 plan 명시 + S24-B 진입 시 v0.1 인덱스 기반 KPI 수집 |
| **P0 fix scope 초과** — Drawer/selectAll/Banner 가 cascade 여러 컴포넌트 | S24-A SP 초과 | P0 4건 한정 + 그 외 P1/P2 carry-over (TK-FB-2-6/7) |

---

## 7. 작업 순서 추천 (1 Day S24-A 일괄)

**Day 1 (~0.7 PD, 5~6시간):**
1. (오전) TK-FB-3-1~4 — alert YAML 4 + MES 패널 + Counter/Timer + validator (병렬 가능, 백엔드)
2. (오전) TK-FB-2-2 vitest 4 fix (프론트엔드 병렬)
3. (오후) TK-FB-2-3 AntD deprecation 3건 + TK-FB-2-4 Drawer/selectAll + TK-FB-2-5 DegradedBanner 시각
4. (오후) TK-FB-1-1~5 sample 캡처 12~13장 (개발 환경)
5. (마무리) BETA_REPORT v0.1 골격 작성 + commit/push (gitflow-commit)

---

## 8. 산출물 (Deliverables)

| 분류 | S24-A 파일 |
|---|---|
| Plan | `Phase 2/4.Tasks/PLAN-SPRINT-24_EP-OPS-FEEDBACK_v1.1.md` |
| Reports | `docs/reports/BETA_REPORT_v0.1.md` (골격) |
| Docs Manual | `docs/manual/USER_MANUAL_v1.4.md` (스크린샷 링크 삽입) + `docs/manual/screenshots/{planner,stk-user,it-ops,read-only}/` 12~13 파일 |
| Infra Grafana | `infrastructure/observability/grafana/dashboards/mes-sprint23.json` + `infrastructure/observability/grafana/alerts/baseline-alerts.yml` |
| Infra Helper | `scripts/validate-alerts.sh` |
| Backend Metrics | `vc` 모듈 — Micrometer Counter/Timer 노출 (mes.poll.success / mes.degraded.duration / mes.fetch.latency) |
| Frontend | 4 component fix — vitest 4 GREEN + AntD 3 deprecation 해소 + Drawer/selectAll UX + DegradedBanner |

---

## 9. Sprint 24-B 진입 조건 (다음 단계)

**S24-B 정식 진입 게이트:**
- ✅ 베타 사용자 8명 Keycloak 발급 + IDP sync 완료
- ✅ 4주 운영 누적 (Grafana metric raw 추출 가능)
- ✅ 인터뷰 8명 완료 (30분 × 8명, IT_OPS 1차 수집)
- ⏳ S24-A 산출물 7건 DoD 충족 + commit/push merge

**S24-B 진입 시:**
- ST-FB-1 실 운영 화면 교체 → USER_MANUAL v1.5
- ST-FB-2 P1/P2 (AG Grid 재도입 / Kakao 부활 / 인터뷰 12+ fix)
- ST-FB-3 threshold 재조정 (1σ/2σ baseline) + IT_OPS §3.3 escalation
- ST-FB-4 BETA_REPORT v1.0 정식 (KPI + 인터뷰 + Go/No-Go) → Sprint 25 EP-PROD-LAUNCH

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 다섯 번째 sprint EP-OPS-FEEDBACK 4 Story / 16 Task / ~4 SP 분해. 베타 1개월 KPI + 인터뷰 8명 기반 UX/문서/alert 안정화. DoD 10 + 리스크 4. Pre-Phase 베타 4주 데이터 누적 + 인터뷰. Sprint 25 PROD 진입 직전 마지막 게이트. |
| **1.1** | **2026-06-01** | **Claude Code** | **조기 진입 모드 — S24-A / S24-B 분할.** Pre-Phase 14% 충족 (베타 발급 0/8, 운영 0/4주, 인터뷰 0/8명) → 정식 4 SP 일괄 진입 불가. **S24-A ~1.5 SP (0.7 PD)** — sample 캡처 12~13 + P0 4건 (vitest/AntD/Drawer/Banner) + alert 골격 (YAML 4 + MES 패널 + Counter/Timer) + BETA_REPORT v0.1 골격. **S24-B ~2.5 SP carry-over** (Phase 5+ 베타 4주 후) — 실 운영 화면 교체 + P1/P2 (AG Grid/Kakao/인터뷰 12+) + threshold 재조정 + BETA_REPORT v1.0. DoD 10 → 7 (S24-A 한정). 리스크 4 → 5 (early-entry 신규: sample↔실 화면 차이, baseline false positive 등). 작업 순서 3 Day → 1 Day S24-A 일괄. 산출물 — PLAN v1.1 + BETA_REPORT v0.1 + USER_MANUAL v1.4 (스크린샷 링크) + mes-sprint23.json + baseline-alerts.yml + validate-alerts.sh + 12~13 screenshots. |
