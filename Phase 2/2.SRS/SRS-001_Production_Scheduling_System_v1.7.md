# SRS v1.7 — Cost-Zero 정책 반영 (Sprint 20 ST-COST-2 Addendum)

**문서 ID**: SRS-001 | **개정**: 1.7 | **작성일**: 2026-05-29
**전판**: [SRS-001 v1.6](SRS-001_Production_Scheduling_System_v1.6.md) (Addendum — REQ-FUNC-VC-022·023 Should → Must 승격)
**상태**: Addendum — Sprint 20 Cost-Zero 정책 통합 (Phase 4 첫 sprint)

> v1.6 (REQ-FUNC-VC-022·023 Must 승격) 위 **Cost-Zero 정책 통합**. 사용자 10명 이내 사내 운영 전제로 REQ-NF-USA-003 (Kakao 알림) 및 NS-04 KPI (Kakao 도달률) 를 **Phase 5+ carry-over** 로 deferred. 전체 SRS 콘텐츠는 v1.5 본문 유지.

---

## 1. v1.6 → v1.7 변경 요지

| 항목 | v1.6 (2026-05-23) | v1.7 (2026-05-29) |
|---|---|---|
| REQ-FUNC 총수 | 75 | 75 (변동 없음) |
| **REQ-NF-USA-003 우선순위 (Kakao)** | Should — Sprint 18 stub + Sprint 20 활성 | **Phase 5+ deferred** ⏸ (사용자 10명 이내 / Slack 모바일 push 로 충분) |
| **NS-04 KPI Kakao 도달률 95%** | Phase 4 측정 활성 | **Phase 5+ deferred** ⏸ — 대체 KPI: **Slack delivery_attempt FAILED 비율 < 5%** (신규) |
| **외부 의존 비용 정책** | (명시 없음) | **Cost-Zero 정책 신규** (§4) — 사용자 10명 이내 운영 전제, 연 운영 비용 0원 |
| 구현 상태 | (변경 없음) | KakaoTalkClient + kakao_delivery_log 코드 stub 유지 (부활 가능 상태) |

---

## 2. Deferred 요구사항 상세

### REQ-NF-USA-003 — Kakao 비즈메시지 알림 (v1.6 Should → v1.7 **Phase 5+ deferred**)

| 필드 | v1.7 |
|---|---|
| 우선순위 | **Phase 5+ deferred** (사용자 10명 이내 사내 운영 / Slack 모바일 push 충분 / 월 ~1만원 비용 회피) |
| 활성 조건 (재판단 시점) | (1) 사용자 30명+ 확장 시 OR (2) Slack delivery_attempt FAILED 비율 ≥ 5% OR (3) PLANNER 1차 응답 시간 > 5분 |
| 코드 상태 | `KakaoTalkClient` (notify 모듈) + `kakao_delivery_log` 테이블 (Flyway V0xx) 유지 — 부활 시 즉시 활성 가능 |
| 부활 비용 | ~0.5 PD — `KAKAO_ENABLED=true` + `KAKAO_WEBHOOK_URL` + `KAKAO_BOT_TOKEN` NSSM env 추가만 |
| 폐기 시점 | Phase 4 Sprint 24 (EP-OPS-FEEDBACK) 1개월 베타 인터뷰 결과 + PROD 30명+ 확장 시점에 재판단 |

### NS-04 KPI Kakao 도달률 95% (v1.6 Phase 4 활성 → v1.7 **Phase 5+ deferred**)

| 필드 | v1.7 |
|---|---|
| 측정 시점 | Phase 5+ — Kakao 부활 후 |
| 대체 KPI (Phase 4) | **Slack delivery_attempt FAILED 비율 < 5%** (신규) — Grafana resilience4j_circuitbreaker_state{name=slack} 기반 |
| 폐기 조건 | Phase 4 Sprint 24 인터뷰 결과 — Slack 단독 운영 충분 시 NS-04 영구 폐기 |

---

## 3. 영향 받지 않은 요구사항 (v1.6 유지)

| 항목 | v1.6 = v1.7 |
|---|---|
| REQ-FUNC-VC-022·023 Must 승격 | ✅ 유지 (v1.6 변경 사항) |
| NFR-SEC-007 사번 8자리 + PIN 4자리 + 5회/10분 잠금 | ✅ v1.5 유지 |
| 모든 BR-* (BR-X01·X02·X05·X06·X07·V07·V12·V13 등) | ✅ v1.5 유지 |
| Modulith 7 도메인 모듈 (PDD-01·02·03 기반) | ✅ v1.5 유지 |
| 1500 row × 30 col p95 < 800ms (REQ-NF-PER-001) | ✅ v1.5 유지 |

---

## 4. Cost-Zero 정책 (신규)

**판단 기준**: 사용자 10명 이내 사내 한정 운영 / 외부 노출 없음 / 베타 ~ 1년 PROD 운영.

| 항목 | 유료 시 | 무료 대안 (Sprint 20 적용) | 부활 비용 (1년 뒤) |
|---|---|---|---|
| AG Grid Enterprise | $999~$1,599/dev/년 | **Community (MIT)** — Sprint 20 ST-COST-1 | Single Dev License $999/년 또는 TanStack Table 자체 구현 ~3 PD |
| Kakao 알림톡 | 월 ~1만원 / 연 12만원 | **Phase 5+ deferred** — Sprint 20 ST-COST-2 | ~0.5 PD (config flag + biz token + NSSM env) |
| Slack Pro | $7.25/user/월 / 연 ~$870 @ 10명 | **Free plan** — 사용자 10명 / 통합 1개 충분 | 사용자 10+ 명 / 통합 11+ 개 시 $7.25/user/월 |
| Sentry SaaS | Free tier ~ Team $26/월 | **미도입** — Loki + Promtail label 기반 검색만 | Free tier 또는 self-hosted Sentry (운영 부담) |
| Docker Desktop | $5/user/월 @ 250+ 회사 | **Docker Engine + WSL2** — 조건부 | (조건부 — 회사 직원 수에 따라) |
| **연간 총 비용** | **~$2,469 + 12만원** | **$0 / 0원** | 부분 부활 시 ~$1,000~$1,500/년 가능 |

자세한 정책: [docs/cost-policy/COST-ZERO_POLICY_v1.0.md](../../docs/cost-policy/COST-ZERO_POLICY_v1.0.md)

---

## 5. 추적성 갱신 (Trace Matrix)

| REQ ID | v1.6 상태 | v1.7 상태 | Sprint 매핑 |
|---|---|---|---|
| REQ-NF-USA-003 Kakao 알림 | Should — Sprint 18 stub | **Phase 5+ deferred** | Sprint 18 stub 코드 유지 / Sprint 20 ST-COST-2 deferred 공식화 / Phase 5+ 재판단 |
| NS-04 KPI Kakao 도달률 | Phase 4 측정 | **Phase 5+ deferred** | Sprint 20 ST-COST-2 / 대체 KPI Slack delivery FAILED 비율 신규 |
| (신규) Cost-Zero 정책 | (없음) | **Sprint 20 적용** | Sprint 20 ST-COST-1·2 (AG Grid + Sentry + Kakao + Slack + Docker) |

---

## 6. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.6 | 2026-05-23 | Claude Code | REQ-FUNC-VC-022·023 Should → Must 승격 (Sprint 7 carry-over 마감 Addendum) |
| **1.7** | **2026-05-29** | **Claude Code** | **Cost-Zero 정책 통합 — 사용자 10명 이내 사내 운영 전제. (1) REQ-NF-USA-003 Kakao 알림 Phase 5+ deferred (Should 폐기 아님). (2) NS-04 KPI Kakao 도달률 Phase 5+ deferred + 대체 KPI Slack delivery FAILED 비율 < 5% 신규. (3) Cost-Zero 정책 신규 (§4) — 5 유료 항목 → 무료 대안 + 1년 뒤 부활 비용 표. KakaoTalkClient 코드 stub 유지 (부활 가능 상태). Sprint 20 ST-COST-2 산출물.** |
