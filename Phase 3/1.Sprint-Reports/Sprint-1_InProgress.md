# Sprint 1 진행 현황 (live)

**Sprint**: S1 | **기간**: 2026-05-20 ~ | **상태**: 🔄 진행 중 (Day 1)
**최종 갱신**: 2026-05-20

본 문서는 Sprint 1 종료 시점 `Sprint-1_Completion_v1.0.md` 로 승급.

---

## 1. Sprint 1 목표 (PLAN-001 D1~D10 발췌)

- **EP-01 엑셀 통합 Parser** (M-01) — 4.2h 수작업 → 30분 자동화 (EXP-1)
- **EP-30-2 RBAC 활성** — Keycloak SSO 완성 + 4 roles 적용
- **EP-02 중복 감지** (ST-02-1 시작)

---

## 2. 진행 매트릭스

### EP-01 엑셀 통합 Parser (M-01)
| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-01-1 (Excel 입력) | TK-01-1-1 POI streaming | ✓ | ca4476f |
| | TK-01-1-2 SourceClassifier 4종 | ✓ | dad27b7 |
| | TK-01-1-3 Multipart 엔드포인트 | ✓ | 6af607b |
| | TK-01-1-4 30 회귀 + ≥99% 정확도 | ✓ partial | DS-ORDER-3X 30/30 = 100% ✓ — 보강 deferred (아래) |
| ST-01-2 (스키마 매핑) | TK-01-2-1·2·3·4 | ⏳ | ST-01-1 종결 후 |
| ST-01-3 (폴더 watcher) | TK-01-3-1·2·3 | ⏳ | Could |

### EP-30 RBAC 활성 (선행: Sprint 0 baseline)
| Story | Task | 상태 |
|---|---|---|
| ST-30-2 (RBAC) | TK-30-2-1·2·3 | ⏳ 진행 예정 |
| ST-30-1 (Keycloak SSO 완성 — Sprint 0 baseline) | TK-30-1-2·3 | ⏳ IdP 사전 통합 대기 |

### EP-02 중복 감지
| Story | 상태 |
|---|---|
| ST-02-1 | ⏳ EP-01 ST-01-1 종결 후 |

---

## 3. 핵심 산출 (실시간)

- **backend/order/parser/** — POI streaming + DTO 3 + SourceClassifier (8 + 13 unit tests PASSED)
- **backend/order/api/** — OrderImportController + 2 DTO (10 controller tests PASSED)
- **backend/order/import_/** — Orchestrator + Tracking + AsyncConfig (5 tracking tests PASSED)
- **backend/order/.../resources/classification/** — header-signatures.yaml 4 SourceType
- **누적 회귀 47 tests PASSED** (KST + ArchUnit + Modulith + Parser + Classifier + Controller + Tracking)

---

## 4. 운영 결정·예외 (Sprint 1 내)

### TK-01-1-4 부분 완료 — 보강 deferred
- ✓ **DS-ORDER-3X 30 워크북** 합성 + 정확도 100% (30/30) — `docs/classification_accuracy_v1.md`
- ⏳ **OrderImportControllerIT @WithMockUser MockMvc** — ST-30-2 (RBAC 활성) 후. 현재는 Controller 단위 테스트(`OrderImportControllerTest` 10 케이스) 로 대체
- ⏳ **ImportEndToEndIT Testcontainers** (PG + Redis 통합) — Sprint 1 후반, Docker 의존성 ↑
- ⏳ **k6 부하 테스트** (`perf/scripts/import_load.js`) — Jenkins CI stage 활성 후 (EP-32 ST-32-1 후행)
- ⏳ **Phase 0 실 데이터 회귀 5~10건** — P1 김정훈 주임 보유 워크북 입수 후 (`docs/classification_accuracy_v1.md` §7)

### 기존 결정
- DECISION-001 NFR-SEC-007 v1.5 (Sprint 0 commit, 적용 — PROD 배포 시 fresh boot)

---

## 5. Sprint 1 종료 시 결산 항목 (sprint review)

- [ ] TK-01-1-4 (회귀 워크북 ≥99% 분류 정확도) 통과
- [ ] EP-30 RBAC 활성 — 4 roles 토큰 발급 + @PreAuthorize 통합
- [ ] 4.2h → 30분 EXP-1 측정 (Sprint Review 데모)
- [ ] dual-review 사인오프 (Sprint 0 6 리포트 + Sprint 1 신규 리포트)
- [ ] Sprint 1 회고 (`Phase 3/1.Sprint-Reports/Sprint-1_Retrospective.md`)

---

## 6. 진행 이력 (체크인 단위)

| 일자 | 항목 | 비고 |
|---|---|---|
| 2026-05-20 | Sprint 1 진입 + EP-01 ST-01-1 TK-01-1-1·2·3 완료 | 회귀 47 tests PASS |
| 2026-05-20 | Phase 3/ 폴더 신설 + Sprint 0 완료 보고 발행 | DECISION-001 동기 |
