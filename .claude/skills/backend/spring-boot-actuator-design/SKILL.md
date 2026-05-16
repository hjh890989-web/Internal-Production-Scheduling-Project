---
name: spring-boot-actuator-design
description: Spring Boot Actuator + Micrometer + Prometheus 노출 설계 (이 프로젝트 17 KPI skeleton 포함).
---

# Spring Boot Actuator Design

본 프로젝트 (Spring Boot 3.3 + Prometheus + Grafana) 의 Actuator 메트릭 노출 표준.

## When to use
- 새 Service / Repository 추가 시 메트릭 노출
- 신규 KPI 추가 시 Micrometer 계측
- Prometheus scrape target 변경

## Endpoint 노출 (application.yml)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus, modulith
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
    prometheus:
      access: read-only
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      # SLA bucket — Phase 2 PER-005 (UI 1초) · PER-007 (on-demand 3초) 기준
      sla:
        http.server.requests: 100ms, 500ms, 1s, 3s
  prometheus:
    metrics:
      export:
        enabled: true
```

## Custom Metric 패턴

### Counter — BR 룰 적용 카운트
```java
@Component
@RequiredArgsConstructor
public class ScheduleMetrics {

    private final MeterRegistry registry;

    public void brEnforced(BrCode code) {
        registry.counter("br.enforced", "code", code.name()).increment();
    }
}
```

### Timer — 스케줄 생성 p95
```java
public Schedule generate(Period p) {
    return Timer.builder("schedule.generate")
        .tag("horizon", String.valueOf(p.days()))
        .register(registry)
        .record(() -> doGenerate(p));
}
```

### Gauge — 활성 사용자
```java
@PostConstruct
void initGauges() {
    Gauge.builder("scheduling.users.active", sessions, Set::size)
        .description("Currently logged-in users")
        .register(registry);
}
```

## 본 프로젝트 17 KPI Skeleton

임계값은 **Phase 2 EP-40 PER-001~008 + EP-31 + EP-47** 에 매핑. 본 표는 metric 이름 표준 + Phase 2 임계 참조 (임의 임계 정의 금지 — 측정 SLO 는 PER 문서 단일 소스).

| KPI | Metric name | Type | 임계 (Phase 2 참조) |
|---|---|---|---|
| 1. 수주 Import (10K row) | `order.import` | Timer | PER-001 — p95 ≤ 60s |
| 2. 성형 후보 생성 | `schedule.generate.vc` | Timer | PER-002 — p95 ≤ 5min |
| 3. 압출 후보 생성 | `schedule.generate.ex` | Timer | PER-003 — p95 ≤ 2min |
| 4. WebSocket PUSH | `websocket.push` | Timer | PER-004 — p95 ≤ 2s (confirm → UI) |
| 5. UI 인지 RT | `ui.interaction` (RUM) | Timer | PER-005·006·008 — EP-40 ST-40-4 |
| 6. on-demand 검사 | `constraint.check.full` | Timer | PER-007 — p95 ≤ 3s |
| 7. BR-X02 audit 누락 | `audit.missing` | Counter | = 0 (BR 강제) |
| 8. BR-X07 D-2 차단 | `br.enforced{code=X07}` | Counter | 정보용 |
| 9. Cache hit rate (L1) | `cache.gets{cache=*,result=hit}` | Counter | EP-31 ST-31-1 기준 |
| 10. Cache hit rate (L2) | `redis.gets{result=hit}` | Counter | EP-31 ST-31-1 기준 |
| 11. N+1 query count | `hibernate.query.execution.count` | Counter | EP-40 회귀 기준 |
| 12. MES 호출 실패 | `mes.call.failures` | Counter | BR-X06 폴백 트리거 |
| 13. MES 폴백 발동 | `mes.fallback.triggered` | Counter | 정보용 (1 shift 미수신 시) |
| 14. WebSocket 활성 세션 | `websocket.sessions.active` | Gauge | — |
| 15. HTTP 5xx | `http.server.requests{status=5xx}` | Counter | EP-41 신뢰성 NFR |
| 16. DB connection pool | `hikaricp.connections.active` | Gauge | EP-41 |
| 17. JVM heap 사용률 | `jvm.memory.used` | Gauge | EP-41 |

**중요** — 본 표는 **이름 표준** 만 정의. 실제 SLO 임계는 **Phase 2 EP-40 ST-40-1~5 + REQ-NF-PER-001~008** 단일 소스.

## Health Indicator (커스텀)

```java
@Component
public class MesHealthIndicator implements HealthIndicator {
    private final MesClient mes;

    @Override
    public Health health() {
        return mes.ping()
            ? Health.up().withDetail("mode", "online").build()
            : Health.down().withDetail("mode", "fallback (BR-X06)").build();
    }
}
```

`/actuator/health` 응답 — `mes` 가 down 이어도 fallback 가능 (BR-X06).

## Prometheus scrape

```yaml
# prometheus.yml
scrape_configs:
  - job_name: scheduling
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['scheduling-stg:8080', 'scheduling-prod:8080']
    scrape_interval: 15s
```

## Security
- `/actuator/prometheus` 는 사내 IP 허용 list 한정 (Spring Security)
- `/actuator/health` 는 공개 (Kubernetes/Docker probe)
- 그 외 endpoint 는 `ROLE_OPS` 만 접근

## Anti-patterns
- 모든 endpoint `*` 노출 (보안 위협)
- `/actuator/env` · `/actuator/heapdump` 노출 (시크릿 누출)
- Counter 를 Gauge 로 (재시작 시 0 으로 리셋되어 cumulative 손실)
- Tag 카디널리티 폭증 (`userId` · `requestId` 를 tag 로 — 메트릭 폭증)

## 참고
- Phase 2 EP-31 (관측성) — `Phase 2/4.Tasks/Tasks/EP-31/`
- Phase 2 EP-47 (KPI 인프라) — `Phase 2/4.Tasks/Tasks/EP-47/`
