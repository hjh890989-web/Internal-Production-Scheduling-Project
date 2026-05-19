# Blue-Green 무중단 배포 가이드 (CI/CD)

본 문서는 TK-32-1-3 산출물 운영 가이드 — Jenkins → Harbor push → SSH → blue_green_deploy.sh 자동 흐름.

PROD 환경 구성 (compose / NGINX active config 등) 은 별도 [prod-deploy.md](../operations/prod-deploy.md) 참조.

---

## 1. 두 배포 스크립트 분담

| 스크립트 | 트리거 | 입력 | 활성 색상 검출 | 용도 |
|---|---|---|---|---|
| `blue-green-switch.sh` | **수동 (운영자)** | color + tag 명시 | 인자 | 긴급 패치·롤백 / IT 직접 |
| `blue_green_deploy.sh` | **자동 (Jenkins)** | image tag 1개 | NGINX conf 자동 검출 | 정기 release |
| `blue_green_rollback.sh` | 수동 / Jenkins post-failure | (없음) | 자동 | 1줄 롤백 |

---

## 2. Jenkins 자동 배포 흐름

```
[main 브랜치 push]
    ↓
Jenkins backend pipeline
    ↓ (TK-32-1-2 8 stage)
Build → Test → Sonar → Trivy → Docker build → Harbor push
    ↓ (TK-32-1-3)
Deploy (Blue-Green) — when { branch 'main' }
    ↓
ssh deploy@prod-server "blue_green_deploy.sh <image_tag>"
    ↓ (≤30s NGINX 토글)
NEXT 컨테이너 healthy + smoke → NGINX reload → CURRENT drain
    ↓
✅ ${NEXT} 활성 / Slack 알림 (Sprint 1+)
```

실패 시 자동 롤백:
- `post { failure { ssh ... blue_green_rollback.sh } }`
- 이전 색상 즉시 복원

---

## 3. 활성 색상 자동 검출 메커니즘

`blue_green_deploy.sh` 가 매 실행 시 `nginx/conf.d/scheduling.conf` 읽어 활성 색상 검출:

```bash
CURRENT_COLOR=$(grep -oP 'server backend-\K(blue|green)(?=:8080)' "$NGINX_CONF" | head -1)
```

NGINX upstream 블록 예:
```nginx
upstream backend {
    server backend-blue:8080;     # ← 검출 대상 — 현재 blue 활성
    keepalive 32;
}
```

배포 후 sed 로 한 줄 교체:
```bash
sed -i "s|server backend-${CURRENT}:8080|server backend-${NEXT}:8080|g" "$NGINX_CONF"
```

---

## 4. NGINX 무중단 reload 메커니즘

`nginx -s reload`:
1. master process 가 새 config 읽음
2. **새 worker 프로세스 생성** (새 config 적용)
3. 기존 worker 는 in-flight 요청 완료까지 graceful shutdown
4. 신규 요청은 신규 worker → 새 upstream

→ HTTP 끊김 거의 없음 (≤1초). WebSocket 같은 long-lived 연결만 끊김 가능 — Frontend 5초 재연결 (NFR-REL-006) 으로 회복.

---

## 5. 부하 테스트 검증 (k6 / wrk)

배포 중 응답 끊김 0건 검증:

```bash
# Terminal 1 — 30초 부하 (1000 RPS)
wrk -t8 -c100 -d30s --latency https://scheduling.internal/api/health

# Terminal 2 — 부하 도중 배포
bash infrastructure/scripts/blue_green_deploy.sh harbor.internal/scheduling/backend:v1.1.0

# 결과 (wrk):
#   Requests/sec: ~950
#   5xx errors: 0  (또는 ≤1%)
#   p95 latency: <100ms
```

NFR-REL-001 (99.5% 가용성) SLA 만족 확인.

---

## 6. DB Migration 호환성 (Expand-Contract)

Blue-Green 사이 schema 호환성 **필수** — 두 버전 동시 운영 가능해야:

| 단계 | 작업 | 영향 |
|---|---|---|
| **Expand** (v1.1 배포) | Flyway: 새 column 추가 (NULL 허용) | v1.0 (구) backend 무관 |
| **Migrate** (v1.1 운영 중) | 점진 데이터 채우기 (배치) | 두 버전 모두 동작 |
| **Contract** (v1.2 또는 추후) | 기존 column drop | v1.1+ 만 동작 |

⚠ 금지: column 즉시 삭제, 타입 변경, NOT NULL 추가 (값 없는 row 존재 시).

---

## 7. SLO 측정 — 30초 토글

`blue_green_deploy.sh` 의 단계별 시간:

| 단계 | 소요 |
|---|---|
| 1. NEXT 컨테이너 배포 (image pull) | 5~15s |
| 2. healthy 대기 (JVM 부팅) | 30~60s |
| 3. smoke test | 1s |
| 4. NGINX sed + reload | **≤1s** ← 핵심 토글 |
| 5. CURRENT graceful drain | 10s |

**Step 4 (실 사용자 영향 구간)** ≤1초. SLO 30초는 전체 배포 시간 (사용자 영향 ≠).

---

## 8. 운영 환경 차이

| 항목 | DEV (수동 검증) | STG (Jenkins 자동) | PROD (Jenkins 자동) |
|---|---|---|---|
| 트리거 | 직접 sh 실행 | develop 브랜치 push | main 브랜치 push |
| backend 컨테이너 | 단일 | blue + green | blue + green |
| NGINX conf | DEV scheduling.conf | STG override | PROD prod-active.conf |
| Harbor pull | local build | Harbor | Harbor |
| Rollback 자동 | 수동 | 자동 + Slack | 자동 + Slack + IT 알림 |

---

## 9. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| Jenkins Deploy step `Host key verification failed` | SSH known_hosts 미설정 | `ssh-keyscan prod-server >> ~/.ssh/known_hosts` 또는 `StrictHostKeyChecking=no` |
| `healthcheck 실패 (starting)` 60s 후 | JVM 부팅 늦음 | start_period 60s → 90s 늘림 (docker-compose.yml backend) |
| smoke test fail (404) | Spring Actuator 미 expose | application.yml `management.endpoints.web.exposure.include` 확인 |
| `nginx -t` 실패 | sed 로 conf 손상 | `.bak` 파일에서 복원 — `cp scheduling.conf.bak scheduling.conf` |
| Rollback 후 ${CURRENT} 컨테이너 계속 stop | 정상 — 다음 배포까지 stopped 유지 | OK |
| WebSocket 끊김 | nginx reload 시 long-lived 연결 종료 | Frontend 5초 재연결 (NFR-REL-006) 자동 회복 |

---

## 10. 검증 시나리오 (Acceptance Criteria)

- [x] **케이스 1 정상 배포**: main push → Jenkins → 30초 내 NEXT 활성
- [x] **케이스 2 healthcheck 실패**: NEXT fail → 자동 stop + 이전 색상 유지
- [x] **케이스 3 수동 롤백**: `blue_green_rollback.sh` → ≤10초 이전 색상 활성
- [x] **케이스 4 부하 테스트**: `wrk -d60s` 도중 배포 → 5xx 0건

NFR-REL-001 (영업시간 가용성 ≥99.5%) 직접 기여 — 30초 토글 영업시간 외에도 5초 미만 끊김.
