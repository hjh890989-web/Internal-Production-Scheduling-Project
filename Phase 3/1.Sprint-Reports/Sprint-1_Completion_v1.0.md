# Sprint 1 완료 보고서 (Phase 3 Sprint 1 종료 게이트)

**Sprint**: S1 | **기간**: 2026-05-20 ~ 2026-05-21 (2일 · AI 가속 압축) | **상태**: ✓ 완료
**작성**: 2026-05-21 | **결재**: 작성 — Claude Code, 검토 대기 — STK-01 + STK-08

> Sprint 0 (5일, 인프라/마스터/CI) 종료 직후 진입. AI 가속 vibe coding 으로 인력 가정
> (3 dev × 10 영업일) 대비 ~5배 압축. 본 보고서는 Sprint Review 데모 가능 시점 기준.

---

## 1. Sprint 1 목표 (PLAN-001 + WBS v1.2 §4)

> "EP-01 수주 통합 Parser (M-01) — 4.2h 수작업 → 30분 자동화 (EXP-1).
>  EP-02 중복 감지 (M-02) + EP-03 Diff·알림 (M-03) + EP-30 RBAC (보안 게이트)."

핵심 KPI — REQ-FUNC-OC-001~010 + REQ-FUNC-OC-015 (Could) + REQ-FUNC-OC-005 + REQ-FUNC-OC-007~010.

---

## 2. Task 매트릭스 (29/29 = 100% 완료)

### EP-01 엑셀 통합 Parser (Sprint 1 핵심 — 11/11)

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-01-1 입력·분류 | TK-01-1-1 POI streaming | ✓ | ca4476f |
| | TK-01-1-2 SourceClassifier 4종 | ✓ | dad27b7 |
| | TK-01-1-3 Multipart 엔드포인트 | ✓ | 6af607b |
| | TK-01-1-4 30 회귀 정확도 ≥99% | ✓ | b3d5733 (parser) + 61c0adf (IT) |
| ST-01-2 스키마 매핑 | TK-01-2-1 SchemaMapping 엔진 + 4 YAML 룰셋 | ✓ | cfff116 |
| | TK-01-2-2 매핑 보정 UI (Frontend Must) | ✓ | 2e51185 (BE) + 495dae5 (FE) |
| | TK-01-2-3 라운드트립 Redis 캐시 | ✓ | 299dcc0 |
| | TK-01-2-4 정확도 통합 회귀 ≥95% | ✓ | a516c4b |
| ST-01-3 폴더 watcher (Could) | TK-01-3-1 NIO + 폴링 fallback | ✓ | ba3ce15 |
| | TK-01-3-2 PickedFile audit + 해시 중복 처리 | ✓ | 84cf695 |
| | TK-01-3-3 fs close 안정성 | ✓ | 7eef049 |

### EP-02 중복 감지 (6/6)

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-02-1 | TK-02-1-1 UNIQUE 제약 + Exception | ✓ | aa60d31 |
| | TK-02-1-2 100사이클 IT + KPI 메트릭 | ✓ | c9f38bc + 9306a3a + 63e0991 |
| | TK-02-1-3 ORM 사전 중복 감지 | ✓ | cea09d0 |
| ST-02-2 | TK-02-2-1 우선순위 룰 (BR-O01) | ✓ | 6a19288 |
| | TK-02-2-2 해소 audit + V003 | ✓ | 470aeb7 |
| | TK-02-2-3 PrecedenceResolver 4 케이스 | ✓ | 2c1a524 |

### EP-03 Diff·분류·알림 (9/9)

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-03-1 | TK-03-1-1 row-level Diff 엔진 | ✓ | 97ebb49 |
| | TK-03-1-2 DiffEngine 회귀 100% | ✓ | 2f95e72 |
| | TK-03-1-3 DiffResult 영속 + V004 | ✓ | d31ba1c |
| ST-03-2 | TK-03-2-1 Critical 분류기 | ✓ | bae40c9 |
| | TK-03-2-2 SeverityClassifier 19 회귀 | ✓ | 82e3176 |
| | TK-03-2-3 Severity enum + Config | ✓ | abc1bc3 |
| ST-03-3 | TK-03-3-1 알림 라우팅 + WebSocket + 카톡 stub | ✓ | e87b677 |
| | TK-03-3-2 NOTIFICATION + Ack API + Escalator | ✓ | 90a2cf0 |
| | TK-03-3-3 k6 SLA 부하 시나리오 | ✓ | 20481fa |

### EP-30 RBAC (Sprint 1 backlog — 3/3)

| Story | Task | 상태 | Commit |
|---|---|---|---|
| ST-30-2 RBAC | TK-30-2-1 Keycloak JWT → 4 role | ✓ | 3d78e8b |
| | TK-30-2-2 Spring Security 6 + @PreAuthorize + ArchUnit | ✓ | 79e08ed |
| | TK-30-2-3 401/403 ProblemDetail 한국어 | ✓ | cca025e |

**합계** — Epic 4 / Story 9 / Task 29 (Must 26 + Could 3) — 100% 완료.

---

## 3. 핵심 지표 (KPI 달성)

| 영역 | 지표 | 목표 | 실측 | 상태 |
|---|---|---|---|:--:|
| **분류 정확도** | DS-ORDER-3X 30 워크북 | ≥99% | 100% (30/30) | ✓ |
| **매핑 정확도** | TC-OC-003 | ≥95% | 100% (실측) | ✓ |
| **중복 감지** | 100사이클 × 50쌍 | row=50 + reject≥5000 | row=50, reject=9950 | ✓ |
| | 동시성 10 thread | 1 win + 9 reject | 1 + 9 | ✓ |
| **Diff** | TC-OC-007 | 100% 정확도 | 100% (DiffEngineServiceTest 회귀) | ✓ |
| **Severity 분류** | False Negative | 0 (Critical 누락) | 19 케이스 PASS | ✓ |
| **알림** | Critical 인앱+카톡 routing | 100% 양쪽 | 5/5 row (in_app 3 + kakao 2) | ✓ |
| | DispatchSummary status | SENT/FAILED 정확 | E2E IT PASS | ✓ |
| **RBAC** | @PreAuthorize ArchUnit | 100% endpoint 강제 | 모든 @RestController public method | ✓ |
| | 401/403 ProblemDetail | 한국어 100% | 13 케이스 IT PASS | ✓ |
| **Modulith** | 모듈 경계 위반 | 0 | 0 (8 모듈) | ✓ |
| **테스트 회귀** | 누적 PASS | — | order ~200 + app ~60 + notify 18 + frontend 14 | ✓ |
| **Testcontainers IT** | PG E2E | 종단 검증 | DiffNotify 3 + DuplicateDetection 3 IT | ✓ |
| **Prometheus 메트릭** | order_duplicate / order_commit | 3 카운터 emit | scheduling.events{module,operation} | ✓ |

---

## 4. 발견된 Production Domain Bug 5건 (실 PG IT 로 surfaced)

Sprint 1 의 가장 큰 부가가치 — Testcontainers IT 가 unit test 통과한 잠재 결함을 발굴.

| # | 버그 | 영향 | Fix Commit | 영향 모듈 |
|---|---|---|---|---|
| 1 | `OrderChangeEntity.fieldDiffsJson` — Hibernate VARCHAR ↔ PG JSONB 미스매치 | INSERT 시 PSQLException "expression is of type character varying" | 6138950 — `@JdbcTypeCode(SqlTypes.JSON)` | EP-03 ST-03-1 |
| 2 | `OrderCommitService.save()` — INSERT 가 transaction commit 까지 지연되어 try/catch 우회 | DataIntegrityViolationException 미포착 → DuplicateOrderException 전파 X | c9f38bc — `saveAndFlush()` | EP-02 ST-02-1 |
| 3 | `OrderCommitService.commit()` — 동일 entity id 재사용 시 save() = MERGE (UPDATE) → UNIQUE 제약 우회 | 100 cycle IT 에서 5000 commit 성공 (50 unique 보장 깨짐) | c9f38bc — 항상 fresh `UUID.randomUUID()` | EP-02 ST-02-1 |
| 4 | V006 `file_hash CHAR(64)` ↔ JPA `@Column length=64` (VARCHAR) Schema-validation 실패 | Spring Boot startup 시 SchemaManagementException | 7eef049 — V006 → VARCHAR(64) | EP-01 ST-01-3 |
| 5 | `InternalImportClient` @Service 어노테이션 + "Client" 종료 → ArchUnit NamingConvention 위반 | 빌드 FAILED | 7eef049 — @Service → @Component | EP-01 ST-01-3 |

**의의** — Sprint 0 baseline 의 unit test (Mockito) 만으로는 Hibernate runtime 동작 + PG dialect 차이 + ArchUnit 정적 규칙 미검출. **Testcontainers + ArchUnit 두 게이트가 Sprint 1 의 안전망 핵심**.

---

## 5. 신규 Database 마이그레이션 (V003 ~ V006, 4건)

| Migration | 테이블 | 도입 Task |
|---|---|---|
| V003 | `audit.precedence_resolution` | TK-02-2-2 |
| V004 | `app.order_change` (4 인덱스, severity 부분 인덱스 포함) | TK-03-1-3 |
| V005 | `app.notification` (4 인덱스, SLA hot path 부분 인덱스) | TK-03-3-2 |
| V006 | `app.picked_file` (3 인덱스, 중복 윈도우 부분 인덱스) | TK-01-3-2 |

> Sprint 0 V001 (order) + V002 (UNIQUE) 까지 합해 마이그레이션 V001~V006 총 6건.

---

## 6. 운영 산출 (Sprint 1 신규)

### 6.1 Prometheus 메트릭 — `scheduling.events{module, operation}`
- `order_duplicate.detected_batch` / `vs_master` / `within_batch` (DuplicateDetectionService)
- `order_commit.success` / `unique_violation` / `error` (OrderCommitService — K-O03 핫스팟)

### 6.2 Grafana 패널 (신규 1건)
- `order-duplicate-detection.json` — 7 패널 (commit success/duplicate/error 누적·rate, batch 감지 vs master·within batch)

### 6.3 Alertmanager rule (신규 3건)
- `OrderDuplicateSurge` — detected_batch ≥10/min × 5m (양식 분화 의심, warning)
- `OrderUniqueViolationSurge` — unique_violation ≥5/min × 5m (race 신호, critical)
- `OrderCommitErrorSpike` — error ≥1/min × 5m (UNIQUE 외 DB 오류, critical)
- **promtool check rules SUCCESS** — 기존 3 + 신규 3 = 6 rules

### 6.4 부하 시나리오 (k6)
- `perf/scripts/notification_sla_test.js` — Critical 100건 × VU 10 + threshold (sla_compliance ≥0.99, p95 <60s)
- `perf/scripts/notification_normal_sla_test.js` — Normal 100건 × 5분 SLA
- `perf/README.md` + `fixtures/critical_events.json` — BR-O02 5 시나리오 표본

### 6.5 Frontend (신규 16 파일)
- `frontend/src/features/order-import/` — 5 컴포넌트 (Ant Design 5 모달 + Form.List 별칭 편집) + 3 hooks (TanStack Query) + types + API + page
- i18n ko.json 17 키 신규 + `/orders/import` 라우트 등록
- Vitest 14 PASS + tsc build PASS + ESLint --max-warnings 0 PASS

---

## 7. 주요 commit (Sprint 1 — 32건)

```
036ff19 chore(transcript): 대화기록_2026-05-21 신규 (Sprint 1 100% 완료 기록)
7eef049 feat(watcher): TK-01-3-3 fs close 안정성
84cf695 feat(watcher): TK-01-3-2 PickedFile audit + 중복 해시 처리
ba3ce15 feat(watcher): TK-01-3-1 NIO WatchService + @Scheduled 60s 폴링
495dae5 feat(frontend): TK-01-2-2 매핑 보정 UI
2e51185 feat(order): TK-01-2-2 mapping-result 조회 endpoint
63e0991 chore(observability): TK-02-1-2 Grafana 패널 + Alertmanager rule
9306a3a test(integration): TK-02-1-2 DuplicateDetectionIT 100사이클
c9f38bc feat(order): TK-02-1-2 OrderCommitService 메트릭 + saveAndFlush fix
6138950 test(integration): EP-03 E2E IT (Testcontainers PG) + JSONB fix
61c0adf test(order): TK-01-1-4 IT OrderImportControllerIT 13건
20481fa test(perf): TK-03-3-3 k6 SLA 부하 시나리오
90a2cf0 feat(notify): TK-03-3-2 NOTIFICATION + Ack + Escalator
e87b677 feat(notify): TK-03-3-1 알림 라우팅 + WebSocket + 카톡 stub
82e3176 test(severity): TK-03-2-2 SeverityClassifier 19 회귀
bae40c9 feat(severity): TK-03-2-1 Critical 분류기
abc1bc3 feat(severity): TK-03-2-3 Severity + Config
2f95e72 test(diff): TK-03-1-2 DiffEngineService 회귀 100%
d31ba1c feat(diff): TK-03-1-3 DiffResult 영속 + V004
97ebb49 feat(diff): TK-03-1-1 row-level Diff 엔진
2c1a524 test(order): TK-02-2-3 PrecedenceResolver 4 케이스
470aeb7 feat(order): TK-02-2-2 해소 audit + V003
6a19288 feat(order): TK-02-2-1 우선순위 룰 (BR-O01)
cea09d0 feat(order): TK-02-1-3 ORM 사전 중복 감지
aa60d31 feat(order): TK-02-1-1 ORDER UNIQUE + Exception
79e08ed feat(security): TK-30-2-2 Spring Security 6 + ArchUnit
cca025e feat(security): TK-30-2-3 401/403 ProblemDetail 한국어
3d78e8b feat(security): TK-30-2-1 Keycloak JWT → 4 role
a516c4b test(mapping): TK-01-2-4 매핑 정확도 통합 회귀
299dcc0 feat(retry): TK-01-2-3 라운드트립 Redis 캐시
cfff116 feat(mapping): TK-01-2-1 SchemaMapping 엔진 + YAML 룰셋
b3d5733 test(parser): TK-01-1-4 DS-ORDER-3X 30 회귀
```

---

## 8. 운영 결정·예외 (Sprint 1)

| 항목 | 결정 |
|---|---|
| **TK-33-2-3 STG PITR 드릴** | Sprint 5 carry-over (Sprint 0 기록) — STG 환경 미구축 |
| **TK-30-1-2·3 SAML/OIDC + Local fallback** | 사내 IdP 통합 대기 — Sprint 2+ 진행 |
| **TK-03-3-3 k6 부하 — Jenkins 통합** | 스크립트 작성 완료, Jenkinsfile.perf stage 활성 = EP-32 Sprint 2 후속 |
| **TK-01-2-2 UAT** | P4 페르소나 단독 시연 — STG 가동 후 실시 (Sprint Review 시점) |
| **NFR-SEC-007 v1.5 정책 적용** | PROD 배포 시 Keycloak realm fresh boot — 사용자 명시 결재 필요 |

---

## 9. dual-review 대기 (BR-X05 — Sprint 0 carry-over + Sprint 1 신규)

### Sprint 0 carry-over (6 리포트, 사용자 측 결재 작업)
```
tools/master_validation/reports/
  vc_master_kl_2026-05-19.md
  cross_check_special_rules_2026-05-19.md
  ex_master_b_2026-05-19.md
  spec_distribution_2026-05-19.md
  br_v17_impact_2026-05-19.md
  (TK-99-1-3 회귀 SQL 코드 리뷰 — Sprint 1 후 Flyway 실행)
```

### Sprint 1 신규 산출 (코드 리뷰 1명 — Sprint Review 시점)
- 32 commit code review (작성자: Claude / 승인자: 사용자)
- frontend `MappingReviewModal` UAT — P4 페르소나

---

## 10. Sprint 2 진입 조건

- [x] Sprint 1 Must Task 26/26 + Could Task 3/3 모두 완료
- [x] Modulith verify + ArchUnit (8 모듈) 0 위반 유지
- [x] Testcontainers IT 통과 (DiffNotify 3 + DuplicateDetection 3)
- [x] Prometheus 메트릭 emit + Grafana 패널 + Alertmanager rule 6
- [x] Frontend Vite build + Vitest 14 PASS
- [x] Production bug 5건 fix (commit 인라인)
- [ ] dual-review 사인오프 — Sprint 2 진행 중 병행 가능
- [ ] STG 환경 활성 — Sprint Review 데모 + UAT 시점

→ **Sprint 2 (성형 가류 핵심) 진입 OK** — 별도 plan [Sprint-2_EntryPlan_v1.0.md](Sprint-2_EntryPlan_v1.0.md) 참조.

---

## 11. Sprint Review 데모 가능 항목

1. **수주 import E2E** — Excel 업로드 → 매핑 모달 → 별칭 추가 → 재시도 → 100% 성공
2. **중복 감지 100사이클 IT** — `./gradlew :app:test --tests *DuplicateDetectionIT` → 3 시나리오 PASS
3. **Diff → Notify E2E** — `./gradlew :app:test --tests *DiffNotifyEndToEndIT` → Testcontainers PG + 5 NotificationEntity row 자동 생성 (in_app SENT + kakao FAILED stub)
4. **RBAC + ProblemDetail** — `./gradlew :app:test --tests *OrderImportControllerIT` → 13 케이스 (READ_ONLY → 403 + 인증 미부여 → 401 한국어)
5. **Grafana 패널** — http://127.0.0.1:3000 → "Order — Duplicate Detection (K-O03)" 7 패널
6. **Frontend 매핑 UI** — `cd frontend && npm run dev` → /orders/import → 모달 자동 노출 시연

---

## 12. Sprint 2 우선순위 미리보기

Sprint 2 (2주, ~37 SP) — 성형 핵심:

| Epic | SP | 주제 | Critical Path |
|---|:--:|---|:--:|
| **EP-04** | 8 | 슬롯 O/X 검증 (BR-V13) | ⭐ |
| **EP-05** | 13 | 회전수 + GreedyRotation + LP/IC 라우팅 | ⭐⭐ |
| **EP-06** | 3 | D-2 영업일 역산 (BR-X07) | |
| **EP-21** | 13 | 좌/우·호기·앵글상한·규격<7 (v1.4 신규) | ⭐ |
| **EP-VC15** | 3 | 충돌 리포트 | |
| **EP-VC16** | 2 | On-Demand 검사 | |
| **EP-34 ST-34-1·3** | 8 | Dual-review + KST UI (carry-over) | |

→ 상세 의존성·일정은 [Sprint-2_EntryPlan_v1.0.md](Sprint-2_EntryPlan_v1.0.md) 참조.

---

## 13. 개정 이력

| 버전 | 일자 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-21 | Claude Code (검토 대기) | 초안 — Sprint 1 100% 완료 (Must + Could) 기준 |
