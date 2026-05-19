# 신규 개발자 — DEV 환경 셋업 (15분)

본 문서는 Sprint 0 ST-00-1 산출물 (`infrastructure/`) 의 신규 개발자 onboarding 가이드.

## 1. 사전 요구사항

| 항목 | 요구사항 | 검증 |
|---|---|---|
| OS | Windows 10/11 + WSL 2, macOS 12+, Ubuntu 22+ | — |
| Docker Desktop | ≥4.20 (Compose v2 내장) | `docker --version`, `docker compose version` |
| RAM | ≥8 GB (컨테이너 합 ~3 GB) | — |
| SSD | ≥20 GB 여유 공간 | — |
| 권한 | Docker 그룹 또는 관리자 | `docker ps` 동작 |
| Git Bash 또는 WSL (Windows) | Makefile + bash 스크립트 실행용 | `bash --version` |

## 2. 저장소 클론

```bash
git clone https://github.com/hjh890989-web/Internal-Production-Scheduling-Project.git
cd "Internal-Production-Scheduling-Project/infrastructure"
```

## 3. 환경 변수 설정

```bash
cp .env.example .env
# .env 파일 열어 POSTGRES_PASSWORD·REDIS_PASSWORD 12자+ 3종 (NFR-SEC-007) 설정
# 예시:
#   POSTGRES_PASSWORD=Dev_Pg_2026_Local
#   REDIS_PASSWORD=Dev_Redis_2026_Local
```

## 4. DEV TLS 인증서 생성 (최초 1회)

```bash
make cert
# → infrastructure/nginx/ssl/dev-self-signed.crt (1년 유효)
```

## 5. 5개 컨테이너 부팅

```bash
make up
# = docker compose --env-file .env up -d --build
```

처음 빌드 시 ~10분 (backend Spring Boot jar 빌드 + frontend npm install). 다음부터는 캐시 활용 ~30초.

## 6. 상태 확인

```bash
make status
```

예상 출력:
```
=== Container Status ===
NAME                    IMAGE                STATUS
scheduling-postgres     postgres:16-alpine   Up (healthy)
scheduling-redis        redis:7-alpine       Up (healthy)
scheduling-backend      scheduling-backend   Up (healthy)
scheduling-frontend     scheduling-frontend  Up (healthy)
scheduling-nginx        nginx:1.25-alpine    Up (healthy)

=== Healthcheck Detail ===
scheduling-postgres: healthy
scheduling-redis: healthy
scheduling-backend: healthy
scheduling-frontend: healthy
scheduling-nginx: healthy
```

5개 모두 `healthy` 표시 → 성공.

## 7. 통합 검증 (자동)

```bash
make validate
# = bash tools/infra_validation/validate_compose.sh
```

검증 항목:
- 5개 healthcheck 모두 통과 (최대 180초 대기 — backend JVM 60s 포함)
- `https://localhost/health` 200 OK
- `psql SELECT 1` 정상
- `redis-cli PING` → PONG
- `https://localhost/actuator/health` 통과 (backend)

## 8. 사용 시나리오

| URL | 용도 | DEV 인증서 경고 |
|---|---|---|
| https://localhost/ | Frontend SPA | ⚠ 브라우저 경고 (DEV 자체 서명) → "고급 → 안전하지 않은 사이트로 이동" |
| https://localhost/api/* | Backend REST | 동일 |
| https://localhost/actuator/health | Backend Actuator | 동일 |
| wss://localhost/ws/ | WebSocket | 동일 |

## 9. 종료

```bash
make down          # 종료 (volume 보존, PG·Redis 데이터 유지)
make clean         # 완전 초기화 (⚠ volume 삭제, 데이터 손실)
make reset         # clean + cert + up (전체 리셋)
```

## 10. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| `error parsing reference` (build 실패) | Dockerfile context 경로 오류 | `infrastructure/` 안에서 명령 실행 확인 |
| backend `unhealthy` 60초 후 fail | JVM heap 부족 또는 application.yml 오류 | `docker compose logs backend` 확인, `JAVA_OPTS=-Xms256m -Xmx1g` 축소 |
| frontend build OOM | npm install 메모리 부족 | Dockerfile 의 `--max-old-space-size=2048` 추가 |
| `port 443 already in use` | 다른 NGINX/Apache 실행 중 | 기존 서비스 중지 또는 docker-compose.yml ports 변경 |
| WSL2 + 느린 build | 호스트 디스크 사용 (volume mount) | WSL 안에서 git clone (WSL 파일시스템) |

## 11. 다음 단계

본 환경 동작 확인 후 Sprint 1 진입:
1. `EP-30` Keycloak 인증 컨테이너 추가 (compose.staging.yml 또는 override)
2. `EP-01` 수주 통합 — backend 실제 코드 (Flyway V001~V010 마이그레이션 → PG 스키마 활성)
3. `EP-31` 관측성 (Prometheus + Loki + Grafana 컨테이너 추가)

## 부록: 파일 구조

```
infrastructure/
├── docker-compose.yml       # 5 service 통합
├── .env                     # 패스워드 등 (gitignore)
├── .env.example             # 템플릿
├── Makefile                 # up/down/status/clean wrapper
├── postgres/
│   ├── postgresql.conf
│   └── init/
│       ├── 01_create_schemas.sql
│       └── 02_grant_permissions.sql
├── redis/
│   └── redis.conf
└── nginx/
    ├── nginx.conf
    ├── conf.d/scheduling.conf
    ├── scripts/generate-dev-cert.sh
    └── ssl/                 # 인증서 (gitignore)

tools/infra_validation/
└── validate_compose.sh      # CI/CD 자동 검증 스크립트
```
