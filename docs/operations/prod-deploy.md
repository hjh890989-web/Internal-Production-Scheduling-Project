# PROD 배포 가이드 — Blue/Green 무중단 (TK-33-1-2)

본 문서는 `docker-compose.prod.yml` + `blue-green-switch.sh` 운영 절차.

PROD 환경: 사내 단일 호스트 (또는 PROD 서버 2대 + 사내 LB) + Harbor 사내 image registry.

---

## 1. 사전 준비 (Phase 0 IT부서 협의)

| 항목 | 요구사항 |
|---|---|
| Harbor 사내 image registry | `harbor.internal/scheduling/` (admin 계정) |
| `harbor.internal` DNS 등록 | 사내 DNS — 사내 서버에서 접근 가능 |
| 사내 CA TLS 인증서 | `infrastructure/nginx/ssl/scheduling.{crt,key}` |
| PROD 사내 IP whitelist | NGINX `allow/deny` 룰 (필요 시) |
| 사내 vault (Vault·1Password 등) | PostgreSQL·Redis·Keycloak 비밀번호 |

---

## 2. PROD `.env.prod` 작성 (DEV `.env` 와 별도)

```bash
# infrastructure/.env.prod (사내 vault 에서 주입)
POSTGRES_DB=scheduling
POSTGRES_USER=app_user
POSTGRES_PASSWORD=<vault>

REDIS_PASSWORD=<vault>

HARBOR_REGISTRY=harbor.internal/scheduling
TAG_BLUE=v1.0.0           # 초기 PROD release
TAG_GREEN=v1.0.0          # 첫 부팅 시 동일 (다음 배포부터 toggle)

SPRING_PROFILES_ACTIVE=prod,with-infra
JAVA_OPTS=-Xms1g -Xmx4g

KEYCLOAK_HOST=scheduling.internal
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=<vault>
KEYCLOAK_DB_PASSWORD=<vault>
```

`.env.prod` 는 `.gitignore` (이미 룰: `infrastructure/.env.*`).

---

## 3. 초기 PROD 부팅 (Day 1)

```bash
cd infrastructure
# 1. 인증서 배치 (사내 CA 발급 — DEV self-signed 와 별도)
cp /vault/secrets/scheduling.crt nginx/ssl/
cp /vault/secrets/scheduling.key nginx/ssl/
chmod 600 nginx/ssl/scheduling.key

# 2. Harbor 로그인 (사내 admin 계정)
docker login harbor.internal

# 3. 초기 active = blue
cp nginx/prod-blue.conf nginx/prod-active.conf

# 4. Compose 부팅 (DEV compose + PROD override)
docker compose --env-file .env.prod \
    -f docker-compose.yml -f docker-compose.prod.yml \
    up -d postgres redis frontend keycloak-db keycloak backend-blue nginx

# 5. 상태 확인
docker compose ps
curl -k https://scheduling.internal/health   # → OK
```

`backend-green` 은 초기 비활성 — 첫 무중단 배포 시점에 활성.

---

## 4. 무중단 배포 흐름

```
[현재] backend-blue v1.0.0 active
       backend-green stopped

[Step 1] Jenkins build → image push: harbor.internal/scheduling/backend:v1.1.0

[Step 2] 운영자: bash blue-green-switch.sh green v1.1.0
  ↓
  ① backend-green (v1.1.0) 시작
  ② healthy 대기 (120s, JVM + Spring + Flyway 마이그레이션 포함)
  ③ NGINX upstream 토글 (cp prod-green.conf prod-active.conf + nginx -s reload)
     → 즉시 모든 신규 요청 backend-green 으로
  ④ 30초 대기 (backend-blue 처리 중인 요청 완료)
  ⑤ backend-blue stop

[현재] backend-green v1.1.0 active
       backend-blue stopped

[다음 배포] bash blue-green-switch.sh blue v1.2.0
```

**핵심**: 사용자 요청은 NGINX reload 시점에 잠시 (1초 미만) 큐 대기 후 새 backend 로 라우팅. **5xx 응답 0건**.

---

## 5. Rollback (1줄 명령)

배포 직후 문제 발견 → 이전 색깔 복원:

```bash
# 직전 backend-blue 가 active 였다면, blue-green-switch 직후 rollback:
bash infrastructure/scripts/blue-green-switch.sh blue $(grep TAG_BLUE .env.prod | cut -d= -f2)
```

backend-blue 컨테이너는 stop 상태이지만 이미지·볼륨 보존 → 빠른 재기동 (~30s).

---

## 6. DB Migration 호환성 (중요)

Blue/Green 사이 **DB schema 호환성 필수**:
- ⚠ **Breaking change 금지**: column 삭제, 타입 변경, NOT NULL 추가 (값 없는 row 존재 시)
- ✓ **Expand-Contract 패턴**:
  1. **Expand** (v1.1): 새 column 추가 (NULL 허용) — v1.0 backend 무관
  2. **Migration**: 점진 데이터 채우기
  3. **Contract** (v1.2 또는 추후): 기존 column drop — v1.1 backend 만 동작
- Flyway migration script 는 양방향 (V001+, U001-) 함께 작성

---

## 7. 검증 시나리오

### 케이스 1 — 정상 배포
1. `blue-green-switch.sh green v1.1.0`
2. NGINX reload 시점 ~1초 — 신규 요청은 green
3. `/actuator/health/readiness` 200 OK 확인
4. k6 부하 모니터: 5xx 0건

### 케이스 2 — healthy 실패 (배포 자동 중단)
1. backend-green 시작 → 120s 동안 healthy 안 됨 (예: DB 연결 실패)
2. script 가 exit 2 + logs 출력
3. NGINX 는 여전히 blue → 사용자 영향 0

### 케이스 3 — 배포 후 회귀 발견 → rollback
1. v1.1.0 배포 직후 critical bug 발견 (예: Excel parsing 오류)
2. `blue-green-switch.sh blue v1.0.0` (이전 tag)
3. 30s 후 v1.0.0 복원

---

## 8. 운영 환경 차이

| 항목 | DEV | PROD |
|---|---|---|
| backend | 단일 컨테이너 (build local) | blue + green (Harbor pull) |
| NGINX ports | `127.0.0.1:80, 443` | `0.0.0.0:80, 443` + 사내 firewall |
| TLS 인증서 | DEV 자체 서명 | 사내 CA |
| `.env` | DEV 평문 | `.env.prod` 사내 vault |
| 백업 | 수동 | `pg_basebackup` 자동 (TK-41-4-1 cron + TK-33-2-1 systemd timer) |

---

## 9. PROD sign-off 체크리스트 (Phase 1.0 출시 전)

- [ ] Harbor 사내 image registry 셋업 + admin 계정
- [ ] 사내 CA TLS 인증서 발급 + 만료 알림 (Slack — EP-44 ST-44-3)
- [ ] `.env.prod` 사내 vault 에서 주입
- [ ] 초기 backend-blue v1.0.0 배포 + health check 통과
- [ ] 두 번째 배포로 blue/green toggle 시연 (STG 환경)
- [ ] Rollback 절차 IT 부서 시연
- [ ] DB migration 호환성 룰 (expand-contract) 문서화
- [ ] 운영자 매뉴얼 (장애 대응 + 비상 연락) IT 부서 sign-off
