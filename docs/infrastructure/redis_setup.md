# Redis 7 운영 가이드

본 문서는 TK-00-1-2 산출물 (`infrastructure/redis/`) 운영 안내.

## 1. 부팅·중지

```powershell
cd infrastructure
docker compose up redis -d
docker compose ps              # (healthy) ≤10초
docker compose logs redis
docker compose down            # 중지 (AOF 보존)
```

## 2. 용도 (SAD §6.2 캐싱 전략)

| 용도 | TTL | 키 패턴 |
|---|---|---|
| Spring Session | 8h | `spring:session:sessions:*` |
| 슬롯 O/X 매트릭스 | 1h | `slot:occupancy:<date>:<line>` |
| `v_product_with_spec` | 1d | `product:spec:<hose_id>` |
| Active candidate 스케줄 | 1w | `schedule:active:<id>` |
| Pub/Sub WebSocket 백업 | — | `master:invalidate`, `vc:changed` |

## 3. 접속 검증

```powershell
docker exec -it scheduling-redis redis-cli -a $env:REDIS_PASSWORD

# 또는 호스트
redis-cli -h localhost -p 6379 -a <password> PING   # → PONG
redis-cli ... SET foo bar
redis-cli ... GET foo                                # → bar
redis-cli ... CONFIG GET maxmemory                   # → 1073741824 (1 GB)
redis-cli ... CONFIG GET notify-keyspace-events      # → KEA
```

## 4. 영속성 (AOF)

- `appendonly yes` + `appendfsync everysec` → **RPO 1초** (NFR-REL-002).
- `redis-data` volume 의 `appendonly.aof` 파일.
- 재기동 시 자동 복원 — `docker compose down` 후 `up` → 데이터 보존.
- AOF 손상: `redis-check-aof --fix appendonly.aof`.

## 5. 메모리·Eviction 모니터링

```powershell
redis-cli ... INFO memory                # used_memory, used_memory_peak
redis-cli ... INFO stats | grep evicted  # evicted_keys (LRU eviction 발생량)
redis-cli ... INFO keyspace              # db0:keys=N,expires=N
```

`maxmemory = 1gb`, `maxmemory-policy = allkeys-lru` → 1 GB 도달 시 가장 오래 안 쓴 키 자동 제거.

부하 테스트 (TK-40-1-3) 후 `maxmemory` 조정 가능.

## 6. Keyspace Notifications (캐시 invalidation)

`notify-keyspace-events KEA` 활성 → 키 만료·변경 이벤트 자동 발행.

Spring Boot 측 구독 예 (Sprint 1+):
```java
@Component
class MasterCacheInvalidationListener {
    @EventListener(RedisKeyExpiredEvent.class)
    void onExpired(RedisKeyExpiredEvent event) { ... }
}
```

또는 PG LISTEN/NOTIFY 와 결합:
- 1차: PG NOTIFY `master_changed` → 단일 BE 인스턴스
- 2차 (백업): Redis PUBLISH `master:invalidate` → 다중 BE 인스턴스 동기

## 7. 백업

```powershell
# AOF 파일 복사 (실시간, RPO 1초)
docker cp scheduling-redis:/data/appendonly.aof ./backup/redis-$(Get-Date -Format yyyyMMdd).aof
# RDB 스냅샷 강제 생성
docker exec scheduling-redis redis-cli -a <pwd> BGSAVE
docker cp scheduling-redis:/data/dump.rdb ./backup/
```

## 8. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| `NOAUTH Authentication required` | `-a <password>` 누락 | `.env` 의 REDIS_PASSWORD 사용 |
| `OOM command not allowed` | `maxmemory` 도달 + write 명령 | `maxmemory-policy` 가 LRU 라 자동 처리되지만 `noeviction` 모드면 발생 |
| 컨테이너 부팅 실패 — password 미설정 | `.env` 누락 | `cp .env.example .env` + REDIS_PASSWORD 채움 |
| AOF rewrite 시 hiccup | `auto-aof-rewrite-min-size 64mb` 도달 | 정상 동작 — fsync 일시 지연 |

## 9. 운영 환경 차이

| 항목 | DEV | STG/PROD |
|---|---|---|
| ports | `127.0.0.1:6379` | `expose: ["6379"]` (BE만 접근) |
| 패스워드 | `.env` | 사내 vault |
| FLUSHDB/FLUSHALL | 활성 | rename-command 로 차단 (`redis.conf` 주석 해제) |
| Sentinel/Cluster | 미사용 (단일 인스턴스) | 미사용 (사내 단일 서버 + AOF 백업 정책) |
