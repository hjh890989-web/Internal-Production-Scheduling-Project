---
name: jpa-query-optimization
description: Spring Data JPA + QueryDSL 쿼리 최적화 (N+1·projection·index·EXPLAIN — 이 프로젝트 1500 row × 30 col p95 < 800ms).
---

# JPA Query Optimization

본 프로젝트 (Spring Data JPA + QueryDSL + PostgreSQL 16) 의 쿼리 최적화 표준. 성능 SLO 는 **Phase 2 EP-40 REQ-NF-PER-001~008** 단일 소스 (조회·생성·on-demand 검사·UI RT 분리).

## When to use
- 새 조회 API 추가 시
- p95 회귀 (Actuator `schedule.generate` 등)
- N+1 의심
- 인덱스 누락 의심

## N+1 진단

### Hibernate Statistics
```yaml
spring:
  jpa:
    properties:
      hibernate.generate_statistics: true
      hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS: 100
```

### Actuator
- `/actuator/metrics/hibernate.query.execution.count` — 요청 당 쿼리 수
- 임계 — 1 요청 당 50 쿼리 초과 시 alert

### 패턴별 해결

#### `@EntityGraph` — 단순 join fetch
```java
public interface ScheduleRepository extends JpaRepository<ScheduleEntry, UUID> {

    @EntityGraph(attributePaths = {"product", "machine"})
    List<ScheduleEntry> findByTargetDate(LocalDate date);
}
```

#### QueryDSL Projection — 필요 컬럼만
```java
public List<ScheduleRow> findScheduleRows(LocalDate from, LocalDate to) {
    return query.select(Projections.constructor(ScheduleRow.class,
            schedule.id,
            schedule.targetDate,
            schedule.machineCode,
            schedule.productCode,
            schedule.rotations,
            schedule.yieldUnit,
            product.name))
        .from(schedule)
        .leftJoin(product).on(schedule.productCode.eq(product.code))
        .where(schedule.targetDate.between(from, to))
        .orderBy(schedule.targetDate.asc(), schedule.machineCode.asc())
        .fetch();
}
```

#### `@BatchSize` — 컬렉션 fetch
```java
@OneToMany(mappedBy = "schedule")
@BatchSize(size = 100)
private List<ScheduleRotation> rotations;
```

## Index 설계

### 표준 인덱스 (Phase 2 EP-08)
```sql
-- 일자 범위 조회 (가장 빈번)
CREATE INDEX idx_schedule_target_date ON scheduling.schedule_entry(target_date);

-- 기계별 일자 조회
CREATE INDEX idx_schedule_machine_date ON scheduling.schedule_entry(machine_code, target_date);

-- 상태별 필터
CREATE INDEX idx_schedule_status ON scheduling.schedule_entry(status) WHERE status IN ('DRAFT', 'CONFIRMED');

-- audit join (cross-module 가 아닌 같은 schema 내)
CREATE INDEX idx_audit_entity_id ON audit.audit_log(entity_type, entity_id);
```

### Index hint (PostgreSQL — 없음, 통계 의존)
```sql
ANALYZE scheduling.schedule_entry;
SET enable_seqscan = off;  -- 진단 시만, PROD 금지
```

## EXPLAIN 분석

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
SELECT s.id, s.target_date, s.machine_code, p.name
FROM scheduling.schedule_entry s
LEFT JOIN scheduling.product p ON s.product_code = p.code
WHERE s.target_date BETWEEN '2026-05-16' AND '2026-05-22';
```

체크 항목:
- `Seq Scan` 대신 `Index Scan` 인가?
- `Rows Removed by Filter` 가 작은가?
- `Buffers: shared hit / read` — disk read 최소화
- `Planning Time` vs `Execution Time` — planner 캐시 활용

## 페이지네이션

### Offset 페이징 (작은 데이터)
```java
PageRequest.of(0, 100, Sort.by("targetDate").ascending());
```

### Keyset 페이징 (1500 row 이상)
```java
public List<ScheduleRow> next(LocalDate lastDate, UUID lastId, int size) {
    return query.selectFrom(schedule)
        .where(schedule.targetDate.gt(lastDate)
            .or(schedule.targetDate.eq(lastDate).and(schedule.id.gt(lastId))))
        .orderBy(schedule.targetDate.asc(), schedule.id.asc())
        .limit(size)
        .fetch();
}
```

## 캐싱 (Caffeine + Redis 2-tier)

### Read-heavy entity
```java
@Cacheable(cacheNames = "products", key = "#code")
public Optional<Product> byCode(String code) {
    return repo.findByCode(code);
}
```

### Cache invalidation — Event 기반
```java
@ApplicationModuleListener
void on(ProductUpdatedEvent e) {
    cacheManager.getCache("products").evict(e.code());
}
```

## Bulk 쓰기

### Batch insert (POI 파싱 후)
```yaml
spring:
  jpa:
    properties:
      hibernate.jdbc.batch_size: 100
      hibernate.order_inserts: true
      hibernate.order_updates: true
```

```java
@Transactional
public void importBulk(List<ScheduleEntry> entries) {
    int batchSize = 100;
    for (int i = 0; i < entries.size(); i++) {
        em.persist(entries.get(i));
        if (i % batchSize == 0) {
            em.flush();
            em.clear();
        }
    }
}
```

## Anti-patterns
- `findAll()` + Java stream 필터 (DB 에서 필터)
- `@OneToMany` fetch=EAGER (N+1 보장 + 메모리)
- Entity 를 DTO 로 직접 노출 (lazy proxy serialization 실패)
- Open Session In View (`spring.jpa.open-in-view=true`) — Sprint 0 부터 `false`
- `@Transactional` 메서드 같은 클래스 내 호출 (proxy 우회)
- 페이지네이션 없이 전체 조회

## 표준 application.yml

```yaml
spring:
  jpa:
    open-in-view: false
    properties:
      hibernate.format_sql: false
      hibernate.use_sql_comments: false
      hibernate.jdbc.batch_size: 100
      hibernate.order_inserts: true
      hibernate.jdbc.time_zone: Asia/Seoul   # BR-X04
      hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS: 100
  datasource:
    hikari:
      maximum-pool-size: 20
      connection-timeout: 3000
      leak-detection-threshold: 5000
```

## 참고
- Phase 2 EP-08 (스케줄 조회 + 인덱스)
- Phase 2 EP-40 (성능 NFR) — `Phase 2/4.Tasks/Tasks/EP-40/`
