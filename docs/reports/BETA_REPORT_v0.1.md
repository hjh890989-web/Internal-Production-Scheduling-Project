# 베타 운영 보고서 v0.1 (골격) — 1개월 데이터 보강 대기

**작성일**: 2026-06-01
**버전**: 0.1 (골격)
**Sprint**: 24 S24-A ST-FB-4
**상태**: 골격 — 데이터 보강 대기 (Phase 5+ v1.0 정식 발행 예정)

> **제약**: 본 v0.1 은 측정 절차 / Go-No-Go 기준 명세만 담는다. 정식 v1.0 발행은 베타 1개월 운영 데이터 누적 완료 후 (Phase 5+) 가능.
> **관련 문서**: USER_MANUAL_v1.3 §3.6 (MES adapter), PLAN-Standard-Beta v1.1, COST-ZERO_POLICY_v1.0

---

## §1 운영 기간 정보

| 항목 | 값 | 비고 |
|---|---|---|
| Cutover 일자 | 2026-05-26 | Sprint 23 마감 직후 |
| 본 보고서 작성일 | 2026-06-01 | Sprint 24 S24-A 진행 중 |
| 베타 사용자 발급 | TBD | ⏳ S24-A 종료 후 IT_OPS 배포 예정 |
| 베타 운영 시작일 | TBD | ⏳ 사용자 발급 익일 |
| 베타 운영 종료일 (예정) | TBD | ⏳ 시작일 + 30일 |
| 데이터 수집 차수 | 0/4 (주차 단위) | ⏳ 미시작 |

**참여 페르소나 (예정)**: P1 생산계획 2명 · P2 공장장 1명 · P3 현장 작업자 3명 · P4 IT_OPS 2명 (총 8명)

---

## §2 KPI 측정 절차 (Phase 5+ 누적 후 측정)

| # | 지표 | 목표값 | 측정 방법 (PromQL / 절차) | 현재값 |
|---|---|---|---|---|
| 1 | HTTP p95 응답시간 | < 800ms (BR-NFR-PERF-001) | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` | ⏳ TBD |
| 2 | Critical retry 비율 | < 1% / 일 | `sum(rate(retry_attempts_total{outcome="failed"}[1d])) / sum(rate(retry_attempts_total[1d]))` | ⏳ TBD |
| 3 | MES polling 성공률 | ≥ 99% / 일 | `sum(rate(mes_polling_success_total[1d])) / sum(rate(mes_polling_total[1d]))` | ⏳ TBD |
| 4 | MES degraded mode 지속시간 | < 30분 / 일 | `sum_over_time(mes_degraded_mode_active[1d]) * 15s` | ⏳ TBD |
| 5 | Slack delivery FAILED | 0건 / 일 | `sum(increase(slack_delivery_failed_total[1d]))` | ⏳ TBD |
| 6 | Backend IT 회귀 | 0건 / Sprint | `./gradlew verifyAll` 일일 결과 누적 | ⏳ TBD |
| 7 | 사용자 만족도 | ≥ 4.0 / 5.0 | 정성 인터뷰 종료 후 5점 척도 평균 (§3) | ⏳ TBD |

**측정 주기**: 주 1회 (월요일 09:00 KST) IT_OPS 가 Grafana dashboard 캡처 → 본 §2 표 업데이트.

---

## §3 정성 인터뷰 절차 (Phase 5+ 8명 30분)

**대상**: 베타 사용자 8명 (P1~P4 전 페르소나)
**시간**: 1인당 30분 (총 4시간)
**진행**: IT_OPS 1차 수집 → P1 생산계획 검토 → 본 §3 요약 갱신
**시점**: 베타 운영 시작일 + 30일 (= 종료일 D-Day)

**질문 6개**:
1. 일일 스케줄 작성 소요시간이 기존 Excel 대비 얼마나 단축되었는가? (TBD ⏳)
2. BR-X01 확정 게이트의 D-2 hard 제약이 실제 운영에 적합한가? (TBD ⏳)
3. MES degraded mode 진입 시 Excel 폴백 절차가 명확했는가? (BR-X06 / TBD ⏳)
4. 1500 row × 30 col 그리드의 가독성·반응성은 만족스러운가? (TBD ⏳)
5. Slack 알림 빈도·내용이 업무에 도움이 되는가? (TBD ⏳)
6. 종합 만족도 (5점 척도) 및 자유 의견 (TBD ⏳)

---

## §4 Sprint 25 PROD Go/No-Go 기준 (안)

| 항목 | 기준 | 측정 | Go 조건 | 현재 상태 |
|---|---|---|---|---|
| KPI 7 지표 | §2 전 지표 목표값 충족 | 30일 누적 평균 | 7/7 PASS | ⏳ TBD |
| 사용자 만족도 | ≥ 4.0 / 5.0 | §3 인터뷰 평균 | 4.0 이상 | ⏳ TBD |
| IT 회귀 | 0건 | verifyAll 30일 연속 GREEN | 0 회귀 | ⏳ TBD |
| 30명 부하 테스트 | p95 < 800ms · 동시 30 사용자 | k6 시나리오 | PASS | ⏳ TBD |

**판정 회의**: 베타 종료일 + 3 영업일 내 (P1 + P2 + IT_OPS 합동). 1개 항목이라도 No-Go 시 Sprint 25 재계획.

---

## §5 Sprint 24 S24-A 산출물

| # | 산출물 | 상태 | 비고 |
|---|---|---|---|
| 1 | vitest 4 fix | ✅ 완료 | ST-FB-x 회귀 제거 |
| 2 | AntD 3 deprecation 정리 | ✅ 완료 | v5 마이그레이션 잔여 |
| 3 | MES Grafana 패널 | ✅ 완료 | polling success + degraded duration |
| 4 | alert YAML 4건 | ✅ 완료 | MES / Slack / retry / p95 |
| 5 | 스크린샷 baseline 12+ | ✅ 완료 | Playwright visual regression |
| 6 | PLAN-Standard-Beta v1.1 | ✅ 완료 | Sprint 24 분기 갱신 |
| 7 | USER_MANUAL v1.4 | ⏳ 진행 중 | §3.6 MES adapter 추가 |
| 8 | BETA_REPORT v0.1 (본 문서) | ✅ 완료 | 골격 — 본 파일 |

---

## §6 Phase 5+ S24-B carry-over

| Task | 내용 | 비고 |
|---|---|---|
| ST-FB-1 | 실 운영 화면 피드백 수집 | 베타 사용자 발급 후 시작 |
| ST-FB-2 | P1 / P2 페르소나 심층 인터뷰 | §3 절차 적용 |
| ST-FB-3 | KPI threshold 재조정 | §2 30일 데이터 기반 |
| ST-FB-4 | BETA_REPORT v1.0 정식 발행 | 본 v0.1 → v1.0 승격 |

---

## §7 개정 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|---|---|---|---|
| 0.1 | 2026-06-01 | Sprint 24 S24-A ST-FB-4 | 골격 초안 — 운영 기간 / KPI 절차 / 인터뷰 절차 / Go-No-Go 기준 / S24-A 산출물 / S24-B carry-over 명세. 데이터 항목 전수 TBD ⏳. |
| 1.0 (예정) | TBD (Phase 5+) | TBD | 베타 1개월 데이터 누적 후 정식 발행. §2 / §3 / §4 실측치 반영. |
