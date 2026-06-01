# PHASE-5_FOUNDATION_v1.0

**문서 ID**: PHASE-5_FOUNDATION_v1.0
**작성일**: 2026-06-01
**작성자**: Claude (AI Harness) + 사용자 공동
**상태**: Draft (Phase 5+ 진입 게이트 미통과 — 베타 1개월 KPI Go 후 활성)
**패턴**: Sprint 24/25 PLAN v1.1 분할 패턴 (early-entry + carry-over) 확장 적용

---

## §0 Phase 4 → Phase 5+ 전환 요약

- Phase 4 진행률 **87% (26/30 SP)** → Phase 5+ 진입 prep 단계
- Phase 4 carry-over **6 항목** → Phase 5+ Sprint 26 ~ 진입 흡수 (Sprint 20 Slack 1 + S24-B 1군 + S25-B 4 = 6)
- 본 문서는 **Phase 5+ Foundation (Sprint 26 ~ 28, ~3주)** Master Plan
- Sprint 29 이후 (Phase 5+ Operation, Sprint 29~31) 는 **조건부** — 베타 데이터 + 사내 IT 의존
- Sprint 24/25 v1.1 패턴 확장 — A 트랙 (data-free 즉시) / B 트랙 (data-dependent carry-over) 분리

---

## §1 Phase 5+ 목적

1. **Phase 4 carry-over 6 흡수** — Sprint 20 ST-EXT-1 Slack 실 webhook + S24-B 4 + S25-B 4
2. **실 vendor 통합** — MES DTO 교체 + Order 자동 INSERT chain + Kafka/MQ/file adapter
3. **사내 30+ 사용자 확장 후 운영 안정화** — 성능 튜닝 (인덱스/쿼리/HikariCP/JVM) + Sentry/AG Grid 재판단
4. **베타 1개월 KPI Go 결정 후 본격 운영** — Sprint 25 S25-B → Sprint 26 Day 1 연결

---

## §2 Phase 5+ Sprint 구조 (Sprint 26 ~ 31)

| Sprint | Epic | SP | PD | 조건 |
|---|---|---|---|---|
| 26 | EP-VENDOR-INTEGRATION | ~4 | 2 | S25-B Keycloak 실 활성 후 진입 |
| 27 | EP-MQ-ADAPTER | ~4 | 2 | Sprint 23 HTTP baseline 위 확장 |
| 28 | EP-OBS-ENHANCE | ~3 | 1.5 | 베타 1개월 KPI 측정 후 결정 |
| 29 | EP-SCALE-TUNING | ~4 | 2 | 30명+ 확장 시점 (조건부) |
| 30 | EP-ML-PRIORITY | ~5+ | 3 | 데이터 6개월 누적 후 (조건부) |
| 31 | EP-MULTI-TENANT | ~5+ | 3 | 사내 multi-plant 결정 시 (조건부) |

**Sprint 26~28** = Foundation 확정 (~11 SP / 5.5 PD / ~3주)
**Sprint 29~31** = Operation 조건부 (~14+ SP / 8 PD / ~6주)
**총 SP 추정** = 25+ SP / ~9주 (조건부 포함)

---

## §3 Sprint 별 Epic 상세

### Sprint 26 — EP-VENDOR-INTEGRATION (~4 SP / 2 PD)
- MES vendor 실 DTO 교체 (Sprint 23 baseline 위)
- Order 자동 INSERT chain 활성 (수주 → 가류 스케줄 자동 전파)
- Pre-Phase: 사내 IT 협의 (vendor spec 확정 + Keycloak realm 활성)
- carry-over 흡수: Sprint 20 ST-EXT-1 Slack 실 webhook (사내 IT 협의 통합)

### Sprint 27 — EP-MQ-ADAPTER (~4 SP / 2 PD)
- Kafka or MQ or file adapter 추가 (3 옵션 中 사내 결정)
- Sprint 23 HTTP polling baseline 위에 다중 transport 추가
- DegradedModeService 확장 (transport-aware 폴백)

### Sprint 28 — EP-OBS-ENHANCE (~3 SP / 1.5 PD)
- Sentry 도입 검토 — Loki 검색 부담 측정 후 결정 (Cost-Zero 정책 재판단)
- AG Grid Enterprise 재도입 검토 — 사용자 호소 측정 후 결정
- Micrometer Counter/Timer 추가 (Sprint 24 carry-over)
- carry-over 흡수: S24-B 4 (스크린샷 실 운영 / UX P1·P2 / Grafana threshold / BETA_REPORT v1.0)

### Sprint 29 — EP-SCALE-TUNING (~4 SP / 2 PD, 조건부)
- 30명+ 확장 후 인덱스 / QueryDSL projection / HikariCP pool / JVM 튜닝
- Sprint 25 S25-B 실 부하 결과 기반
- k6 부하 재측정 (1500 row × 30명 동시)

### Sprint 30 — EP-ML-PRIORITY (~5+ SP / 3 PD, 조건부)
- PRODUCT_PRIORITY ML/AI baseline (데이터 6개월 누적 후)
- 우선순위 추천 모델 prototype

### Sprint 31 — EP-MULTI-TENANT (~5+ SP / 3 PD, 조건부)
- Multi-tenant 확장 (사내 multi-plant 결정 시)
- schema-per-tenant vs row-level RLS 결정

---

## §4 carry-over 매핑 (총 6 항목)

| # | Source | Target Sprint | 내용 |
|---|---|---|---|
| 1 | Sprint 20 ST-EXT-1 | Sprint 26 ST-VENDOR-0 | Slack 실 webhook (사내 IT 협의 통합) |
| 2 | S24-B #1 | Sprint 28 EP-OBS-ENHANCE | 스크린샷 실 운영 (베타 데이터) |
| 3 | S24-B #2 | Sprint 28 EP-OBS-ENHANCE | UX P1/P2 인터뷰 결과 반영 |
| 4 | S24-B #3 | Sprint 28 EP-OBS-ENHANCE | Grafana threshold 실측 기반 조정 |
| 5 | S24-B #4 / S25-B | Sprint 28 EP-OBS-ENHANCE | BETA_REPORT v1.0 발행 |
| 6 | S25-B Keycloak | Sprint 26 / 29 | 실 활성 + 실 부하 측정 + 튜닝 |
| **7** | **BR-X07 날짜 회귀 IT 18건** | **Sprint 27 첫 작업 또는 환경 hotfix** | **2026-06-01 발견 — V039 sample seed production_date 가 today (=오늘) hardcode → BR-X07 D-2 hard trigger 거부. 영향 IT 4 클래스 (AuditTriggerIT 5 + IntraDayLockIT 5 + SwapProposalIT 6 + VcScheduleQueryControllerIT 2). 해결 — sample seed production_date 를 (a) LocalDate.now().plusDays(7) 또는 (b) hardcode future (2026-12-31) 로 일괄 갱신. Sprint 26 S26-A commit 13e957e~40dd80f 변경과 무관 (회귀 0건 확정)** |

---

## §5 사용자 결정 게이트 5항

- **G1. Phase 5+ 진입 시점** — 옵션 A (베타 1개월 Go 후 직렬 안전) vs 옵션 B (베타 진행 중 data-free 병렬)
- **G2. Sprint 26 첫 작업** — vendor MES DTO vs Order chain vs Cost-Zero 부활
- **G3. Cost-Zero 정책 유지 vs 부활** — Kakao / AG Grid / Sentry (베타 1개월 측정 결과 기반)
- **G4. 30명+ 확장 시점** — 베타 8명 → 정식 migration 타이밍
- **G5. Phase 5+ 기간** — 6개월 fixed vs sprint-by-sprint 진화

---

## §6 DoD (Phase 5+ 종료)

- Phase 4 carry-over 0건 (6/6 흡수 완료)
- 30+ 사용자 PROD 안정 운영
- MES vendor 실 통합 + Order chain 활성
- KPI 7 지표 모두 PROD 목표 충족
- BETA_REPORT v1.0 발행 + Phase 5+ closing report 발행

---

## §7 리스크 5

1. **사내 IT 협의 지연** (Slack / Keycloak / MES vendor) → Sprint 26 ~ 진입 지연
2. **베타 1개월 KPI No-Go** → Phase 5+ 진입 보류 + Sprint 24·25 S24-B/S25-B carry-over 만 진행
3. **Cost-Zero 정책 부활 시 예산 협의** (Sentry / AG Grid / Kakao) — 사용자 결재 필요
4. **30명+ 확장 후 성능 SLA 위반** (인덱스/쿼리 튜닝 부족) → Sprint 29 우선순위 상향
5. **ML/AI / Multi-tenant 우선순위 결정 어려움** (사용자 결정 의존) → Sprint 30·31 조건부 유지

---

## §8 산출물

- **PHASE-5_FOUNDATION_v1.0** (본 문서)
- **Sprint 26 ~ 28 PLAN 6** (Sprint 26·27·28 + 조건부 Sprint 29·30·31)
- **WBS v1.21 Addendum** (Phase 4 마감 + Phase 5+ 진입)
- **BETA_REPORT v1.0** (Phase 5+ Sprint 26 진입 게이트)

---

## §9 작업 순서 (Phase 5+ 진입 Day 1)

1. 사용자 결정 5 게이트 확인 (사내 IT 협의 일정 / 베타 진입 시점 / 첫 sprint 결정)
2. Sprint 26 EP-VENDOR-INTEGRATION 진입 OR S24-B/S25-B carry-over 부분 진행 (G1 결정 의존)
3. BETA_REPORT v1.0 발행 후 Sprint 27·28 sequential 진입
4. Sprint 29 이후 조건부 — 베타 데이터 + 사내 IT 협의 결과 기반 재판단

---

## §10 개정 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|---|---|---|---|
| v1.0 | 2026-06-01 | Claude + 사용자 | 최초 작성. Sprint 24/25 PLAN v1.1 분할 패턴 확장 적용. Sprint 26~28 Foundation + Sprint 29~31 조건부 Operation. carry-over 6 매핑. 사용자 결정 게이트 5항. |

---

**END OF PHASE-5_FOUNDATION_v1.0**
