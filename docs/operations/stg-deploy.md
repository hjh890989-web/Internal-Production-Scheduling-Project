# STG 환경 배포·운영 가이드 (TK-33-1-1)

본 문서는 STG (Staging) 환경 운영 절차. PROD 배포 전 베타 사용자 검증 + 통합 테스트.

PROD 환경 — [prod-deploy.md](prod-deploy.md) 참조.
CI/CD 자동 흐름 — [deployment.md](../infrastructure/deployment.md) 참조.

---

## 1. STG 환경 특성

| 항목 | DEV | **STG** | PROD |
|---|---|---|---|
| backend 컨테이너 | 단일 (build local) | **단일 (Harbor pull)** | blue + green |
| DB | scheduling | **scheduling_stg** | scheduling |
| 도메인 | localhost | **stg.scheduling.internal** | scheduling.internal |
| 사용자 | 개발자 | **베타 사용자 (~5명) + QA** | 전사 (~30명) |
| 데이터 | 빈 DB | **Flyway profile=stg seed** | 운영 데이터 |
| Spring Profile | dev | **stg,with-infra** | prod,with-infra |
| 배포 트리거 | 수동 | Jenkins develop 브랜치 push | Jenkins main 브랜치 push |
| Blue-Green | — | — | ✓ |

---

## 2. 사전 준비

### 2.1 Harbor 사내 image registry
PROD 와 동일. backend image 가 push 되어 있어야 STG pull 가능.

### 2.2 사내 DNS 등록
`stg.scheduling.internal` → STG 서버 IP. 사내 IT부서에 요청.

### 2.3 사내 CA TLS 인증서
PROD 와 별도 STG 인증서 (또는 wildcard `*.scheduling.internal` 공용).
`infrastructure/nginx/ssl/scheduling.crt` + `scheduling.key`.

### 2.4 `.env.stg` 작성

```bash
cp infrastructure/.env.stg.example infrastructure/.env.stg
# .env.stg 의 <vault> 자리에 사내 vault 값 채움
```

`.env.stg` 는 `.gitignore` (rule: `infrastructure/.env.*`).

---

## 3. 초기 STG 부팅 (Day 1)

```bash
cd infrastructure

# 1. 인증서 배치
cp /vault/secrets/stg-scheduling.crt nginx/ssl/scheduling.crt
cp /vault/secrets/stg-scheduling.key nginx/ssl/scheduling.key
chmod 600 nginx/ssl/scheduling.key

# 2. Harbor 로그인
docker login harbor.internal

# 3. STG compose 부팅 (DEV base + STG override)
docker compose --env-file .env.stg \
    -f docker-compose.yml -f docker-compose.stg.yml \
    up -d postgres redis frontend keycloak-db keycloak backend nginx

# 4. 상태 확인
docker compose ps
curl -k https://stg.scheduling.internal/health   # → OK (STG)
```

---

## 4. Flyway STG profile — test data seed

`backend` 의 `application-stg.yml` (Sprint 1+ 작성) 에서:

```yaml
spring:
  flyway:
    enabled: true
    schemas: app,master,audit
    locations: classpath:db/migration,classpath:db/migration/stg
    # → db/migration (V001~V0NN production schemas)
    # → db/migration/stg (V900_STG_*.sql — STG 전용 seed)
```

STG 전용 seed 예 (Sprint 1+ TK-01-1-* 작업 시 추가):
- `V900_STG_47_products.sql` — 47 품번 마스터 데이터
- `V901_STG_sample_orders.sql` — 표본 수주 30건
- `V902_STG_beta_users.sql` — 베타 사용자 4명 (Keycloak emergency 계정 + 평소 계정)

---

## 5. Jenkins develop 브랜치 자동 배포 (Sprint 1+ 활용)

Jenkinsfile.backend 의 deploy stage 분기 (현재 main 만 — develop 추가):

```groovy
stage('Deploy STG') {
    when { branch 'develop' }
    steps {
        sshagent(['stg-deploy-key']) {
            sh """
                ssh deploy@stg-server '
                    cd /opt/scheduling &&
                    git pull origin develop &&
                    TAG=${IMAGE_TAG##*:} \
                    docker compose --env-file .env.stg \
                        -f docker-compose.yml -f docker-compose.stg.yml \
                        up -d backend
                '
            """
        }
    }
}
```

STG 는 Blue-Green 불필요 — backend 단일 컨테이너 재기동 (~30~60s 다운). 베타 사용자에게는 사전 공지.

---

## 6. 베타 사용자 안내

베타 사용자에게 알려야 할 것:

| 항목 | 안내 |
|---|---|
| URL | https://stg.scheduling.internal |
| TLS 경고 | 사내 CA — 본인 PC 에 신뢰 인증서 설치 후 정상 |
| 데이터 | STG 환경 — 실제 운영 데이터 아님. 자유롭게 시도 |
| 장애 보고 | Slack #scheduling-stg-beta 또는 IT 헬프데스크 |
| 운영 시간 | 평일 09:00~18:00 (외 시간 SLA 없음) |
| 배포 빈도 | develop 브랜치 push 시 자동 — 30~60초 다운 |

---

## 7. STG 검증 시나리오 (PROD 배포 전)

1. **Smoke test**: `curl -k https://stg.scheduling.internal/api/health` → 200
2. **Login flow**: Keycloak `/admin` → admin 로그인 → realm 'scheduling-system' 확인
3. **Backend API**: `/api/actuator/health` → `{"status":"UP"}` + DB·Redis 모두 UP
4. **DB seed**: `psql -h stg-server -U app_user -d scheduling_stg -tAc "SELECT COUNT(*) FROM master.product"` → 47
5. **베타 사용자 1주 운영**: NS-01 사전 설문 baseline (Sprint 5 EP-44 ST-44-7)

---

## 8. STG → PROD 승격

STG 에서 1주+ 안정 운영 후 PROD 배포:

1. Git develop → main merge (사용자 dual-review — BR-X05)
2. Jenkins main 브랜치 자동 trigger → `blue_green_deploy.sh` (TK-32-1-3)
3. PROD 환경 무중단 배포 (≤30초)

---

## 9. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| backend 부팅 실패 — Harbor pull 거부 | docker login 미실행 또는 credential 만료 | `docker login harbor.internal` 재실행 |
| backend 부팅 실패 — Flyway migration error | scheduling_stg DB 와 backend 코드 schema 불일치 | `migration repair` 또는 DB clean → re-init |
| stg.scheduling.internal 응답 없음 | 사내 DNS 미등록 또는 firewall | 사내 IT 부서에 확인 |
| TLS 인증서 만료 | 1년 만료 도래 | EP-44 ST-44-3 Slack 알림 후 사내 CA 재발급 |
| Keycloak realm 'scheduling-system' 미인식 | KEYCLOAK_HOST=stg.scheduling.internal 만 변경, realm 자동 import 안 됨 | keycloak-db volume rm 후 재부팅 |

---

## 10. STG 환경 종료 / 일시 중지

```bash
# 일시 중지 (volume 보존)
docker compose --env-file .env.stg \
    -f docker-compose.yml -f docker-compose.stg.yml \
    down

# 완전 초기화 (⚠ STG DB 데이터 손실)
docker compose --env-file .env.stg \
    -f docker-compose.yml -f docker-compose.stg.yml \
    down -v
```
