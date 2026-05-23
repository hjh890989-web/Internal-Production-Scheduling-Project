# ADR-021 — Sprint 6 결정사항 종합 (v1.0)

**Status**: Accepted | **Date**: 2026-05-23 | **Authors**: Claude Code
**Sprint**: S6 (Phase 3 마지막) | **상위 ADR**: ADR-008~020 (Phase 2 SAD-001)

> Sprint 6 (E2E + NFR EP-40~47 + 인프라) 도중 결정된 **5개 architecture decision** 영구
> 기록. ADR-008~020 (Phase 2 SAD) 와 ADR-016 (당일 락 4-layer) · ADR-017 (cross-VIEW)
> 위에서 운영 NFR 본격 활성 시 결정한 사항. **in-place 금지 원칙** 으로 SAD-001 본문 수정
> 대신 별도 파일.

---

## ADR-021-A — Resilience4j 도입 + 적용 범위 (EP-41)

### Context
Sprint 5 EP-16 KakaoDeliveryService 가 inline 3회 retry 만 구현. Kakao Workplace Bot
실 운영 시 외부 의존 — circuit breaker + exponential backoff 필요. NFR-OPS-001 (도달률
≥ 95%) 충족 위해 표준 안정성 라이브러리 도입 필요.

### Decision
- **라이브러리**: `io.github.resilience4j:resilience4j-spring-boot3:2.2.0`
- **적용 모듈**: `notify/KakaoTalkClient.send` (Sprint 6 첫 적용)
- **annotation**: `@CircuitBreaker(name="kakaotalk", fallbackMethod="fallbackSend")` + `@Retry(name="kakaotalk")`
- **config 위치**: `application.yml` (root) — 모든 profile 공유
  ```yaml
  resilience4j:
    retry.instances.kakaotalk:
      max-attempts: 3, wait-duration: 1s, exponential-backoff-multiplier: 2
    circuitbreaker.instances.kakaotalk:
      sliding-window-size: 10, minimum-number-of-calls: 5
      failure-rate-threshold: 50%, wait-duration-in-open-state: 30s
      automatic-transition-from-open-to-half-open-enabled: true
  ```
- **fallback signature**: `boolean fallbackSend(Notification n, Throwable t)` — R4j 규약
- **확장 우선순위**: Sprint 7+ — InternalImportClient + Excel parser HTTP 호출

### Consequences
- (+) 외부 의존 장애 격리 — Kakao 측 다운 30초 대기 후 자동 복구
- (+) Prometheus metric 자동 expose — `resilience4j_retry_calls_total` / `resilience4j_circuitbreaker_state` → Grafana scheduling-overview 대시 통합
- (+) AOP 기반 — service code 비침습 (annotation 만 추가)
- (-) AOP 추가 시 부팅 cost ↑ — Spring AOP starter 추가 (notify build.gradle)
- (-) test fixture — `KakaoDeliveryIT` 가 CB OPEN 시나리오 회귀 어려움 (Phase 7+ 도입)

### Alternatives 검토
- Spring Retry — annotation 호환 OK 이지만 CircuitBreaker 미내장
- Spring Cloud CircuitBreaker — Spring Cloud 의존 (스택 무거움)
- 자체 inline retry (Sprint 5 baseline) — NFR-OPS-001 충족 어려움

---

## ADR-021-B — audit.schedule_audit_log 월별 RANGE 파티셔닝 (V030)

### Context
NFR-SEC-004 — audit ≥ 3년 보존. 단일 테이블 구조 (Sprint 4 V025) 로는 운영 1년 후
~10M row 도달 시 query 성능 저하 + REINDEX cost. PostgreSQL RANGE partitioning 으로
월별 child 분리 + index 효율.

### Decision
- **전략**: `PARTITION BY RANGE (occurred_at)` — 월별 child
- **범위**: 2026-01 ~ 2028-12 = 36 child + DEFAULT partition (fallback)
- **PK 변경**: `(audit_id)` → `(audit_id, occurred_at)` 복합 (PostgreSQL 요건 — partition key 포함)
- **트리거 재구성**: V025/V026 trigger 임시 disable → 테이블 재생성 → 재활성
- **인덱스 자동 상속**: `(table_name, row_pk)` + `(occurred_at)` + `(actor)`
- **DEFAULT partition**: 미래/과거 fallback (2025 이전 · 2029 이후)
- **Flyway baseline**: V030 부팅 시 1회 실행 — 기존 데이터 백업 → 복원

### Consequences
- (+) query 성능 — partition pruning 으로 월별 검색 시 단일 child 만 scan
- (+) 3년 후 detach + archive 손쉬움 — `ALTER TABLE DETACH PARTITION`
- (+) REINDEX 비용 분리 — child 별 maintenance
- (-) PK 복합 — JPA `@IdClass` 또는 native query 필요 (현재 service 는 INSERT only)
- (-) V030 무거운 마이그레이션 — 운영 시 점검 시간 필요 (10~30분)
- (-) 정기 partition 추가 작업 (Phase 6+ 자동화 필요 — pg_partman 검토)

### Alternatives 검토
- HASH partitioning — table_name 분산 OK 이지만 시간 기반 archive 불가
- 단일 테이블 + 인덱스 — 3년 후 ~20M row 성능 미달 (NFR-OBS-002 위반)
- Citus / TimescaleDB — 사내 서버 single node + 라이센스 비용

---

## ADR-021-C — spring-modulith-events-jpa schema 위치 (V031)

### Context
EP-41 ST-41-2 spring-modulith-events-jpa 활성 — `@ApplicationModuleListener`
publication 영속 → 재시작 복구 (BR-X03 cascade chain 신뢰성). `event_publication`
테이블이 필요한데 SpringBoot `ddl-auto=validate` + 자동 schema 생성 안 함.

### Decision
- **방법**: Flyway 마이그레이션 V031 로 직접 생성 (Modulith schema-initialization 의존 X)
- **schema 위치**: `public.event_publication` (NOT `app.event_publication`)
  - Hibernate `default_schema` 미설정 시 search_path 첫 schema (public) 검색
  - 다른 도메인 entity 는 `@Table(schema="app")` 명시
- **Flyway schemas 확장**: `app,master,audit` → `app,master,audit,public,business_kpi`
- **추가 보조**: `application.yml` 에 `spring.modulith.events.jdbc.schema-initialization.enabled=true`

### Consequences
- (+) 재시작 시 미완료 publication 자동 재처리 — BR-X03 chain 신뢰성
- (+) Flyway 단일 source of truth — JPA validate 통과
- (+) public schema 분리 — 도메인 schema (app) 와 분리, framework-level 명확
- (-) 도메인 + framework schema 혼재 — Spring Boot 관습이지만 일관성 ↓
- (-) Sprint 0 기존 baseline (autoconfigure exclude) 와 충돌 — `JpaEventPublicationAutoConfiguration` 도 exclude 리스트에 추가

### Alternatives 검토
- `app.event_publication` — Hibernate `default_schema=app` 설정 필요, 다른 entity 의 `@Table(schema=...)` 와 호환성 검증 부담
- spring-modulith-events-jdbc (JDBC native) — JPA 활용도 ↓
- modulith schema init 단독 — Hibernate validate 보다 늦게 실행 → 부팅 실패

---

## ADR-021-D — Vite bundle 7-chunk 세분화 정책 (EP-46)

### Context
Sprint 5 baseline 의 ant-design 단일 청크가 1.2MB (gzip 383kB). Sprint 5 DoD `Vite entry
bundle ≤ 200kB gzip` 미달. AG Grid Enterprise 추가 후 ant-design + AG Grid + react-vendor
+ tanstack + i18n + dnd-kit + dayjs + stomp 가 entry 에 묶이면 첫 진입 시간 NFR-PER-005
초과.

### Decision
- **`vite.config.ts` `rollupOptions.output.manualChunks` 7 chunk 정책**:
  - `react-vendor` — react + react-dom + react-router-dom
  - `antd-core` — antd 단일 (1.2MB lazy chunk — 페이지 진입 시만 fetch)
  - `antd-icons` — @ant-design/icons (4kB)
  - `tanstack` — @tanstack/react-query + devtools
  - `i18n` — i18next + react-i18next
  - `dayjs` — 단일
  - `dnd-kit` — @dnd-kit/core + sortable + utilities
  - `stomp` — @stomp/stompjs + sockjs-client
  - **AG Grid** — 자동 분리 (agGridSetup.ts import 시점 lazy, 653kB)
- **`chunkSizeWarningLimit`**: 500 → 700kB (AG Grid Enterprise 2.4MB 정상)
- **Entry first paint**: index + react-vendor + i18n ≈ ~50kB gzip (DoD 200kB 큰 폭 통과)

### Consequences
- (+) DoD 통과 — Sprint 5 NFR ≤ 200kB → 실측 ~50kB
- (+) 페이지 진입 시 lazy load — 첫 진입 빠름, 페이지 이동 시 chunk 캐시 적중
- (+) HTTP/2 multiplex — 다중 chunk 병렬 로드 OK
- (-) ant-core 1.2MB 첫 페이지 진입 시 fetch — 사용자 첫 클릭 약간 지연 (Phase 4 베타 측정 후 조정)
- (-) chunk 수 ↑ → Vite build 시간 약간 증가 (3138 → 3273 module)

### Alternatives 검토
- ant-design 트리쉐이킹 (babel-plugin-import) — Ant Design 5+ ESM 트리쉐이킹 미보장
- Server-side rendering (Next.js) — 인프라 변경 큼 (Phase 7+ 검토)
- Code splitting 페이지별 lazy (이미 적용 — React.lazy + Suspense)
- 단일 청크 유지 + chunkSizeWarningLimit 1500 — DoD 미충족

---

## ADR-021-E — Redis Pub/Sub STOMP fan-out 진입점 (Sprint 6 인프라)

### Context
Sprint 4 EP-EX14 WebSocket STOMP — Spring Boot `enableSimpleBroker` (in-memory). 단일
인스턴스 한정. 다중 인스턴스 (Phase 5+ scale-out) 시 instance A 의 publish 가 instance B
의 subscriber 에 전달 안 됨. NFR-PER (확장성) 요구사항.

### Decision
- **Sprint 6 baseline**: `RedisStompFanoutConfig` (notify 모듈) — toggle 가능 config 만 등록
- **활성 flag**: `scheduling.notify.redis-fanout.enabled=false` (기본) — 단일 인스턴스
- **본격 fan-out 구현**: Sprint 7+ — Redis Pub/Sub listener + ChannelTopic 등록
- **대안 검토 (Phase 5+)**: 본격 STOMP relay (RabbitMQ / ActiveMQ) 도입 시 본 stub 폐기

### Consequences
- (+) 인터페이스 + 진입점 미리 확보 — Phase 5+ scale-out 시 코드 변경 최소
- (+) 단일 instance 운영 시 영향 0 (enabled=false 기본)
- (-) Sprint 6 단계에서 본격 활성 안 함 — Phase 5+ 까지 dead code
- (-) Spring lifecycle 충돌 우려 — Sprint 6 도중 `afterPropertiesSet()` 수동 호출 제거 (Spring auto)

### Alternatives 검토
- STOMP broker relay (RabbitMQ) — Phase 5+ 본격 도입 시 표준 패턴, 현재는 단일 inst 충분
- 단일 instance 운영 영구 유지 — 사내 ~10명 사용자 가정 하 충분, but 확장성 0
- Hazelcast / Apache Ignite — 도입 비용 큼

---

## 6. 종합 영향 매트릭스

| ADR | 영향 영역 | NFR | Sprint 검증 |
|---|---|---|---|
| 021-A R4j | notify 모듈 (Kakao + Phase 7+ Import) | NFR-OPS-001 도달률 ≥ 95% | EP-41 IT 통과 |
| 021-B audit partition | audit 운영 효율 + 3년 보존 | NFR-SEC-004 | V030 + AuditTriggerIT 5 통과 |
| 021-C event_publication | BR-X03 cascade 신뢰성 | NFR-PER (재시작 복구) | V031 + 부팅 검증 |
| 021-D Vite chunk | Frontend entry first paint | NFR-PER-005 ≤ 200kB | 실측 ~50kB |
| 021-E Redis fanout | scale-out 진입점 | NFR-PER (확장성) | Sprint 7+ 본격 |

---

## 7. SAD-001 (Phase 2) 와의 관계

- SAD-001 ADR-008~017 는 Phase 2 설계 시점 결정 (Java 21 + Spring Boot 3 + Modulith + PG + Keycloak + Docker + Prometheus + 4-layer 일중 락 + cross-VIEW).
- **ADR-021 은 Phase 3 Sprint 6 운영 NFR 본격 활성 시 결정** — SAD-001 변경 없이 보강. PDD-MASTER v1.7 의 운영·확장 정책 추가.
- 향후 SAD v2.0 발행 시 본 ADR 흡수 검토 (Phase 5 PROD cutover 후).

---

## 8. 결재

| 필드 | 값 |
|---|---|
| Document ID | ADR-021 |
| 개정 | 1.0 |
| 상태 | Accepted (Sprint 6 commit ffd6c75 + cf27721 + af16221 + d677ef8 채택) |
| 소유자 | 시니어 아키텍트 / STK-08 (IT 운영자) |
| 검토 | STK-01·STK-08 검토 대기 |
| 상위 ADR | ADR-016 (4-layer) + ADR-017 (cross-VIEW) — Phase 2 SAD-001 |

---

## 9. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Sprint 6 5 결정사항 (Resilience4j + partition + event_publication + Vite chunk + Redis fanout) 영구 기록 |
