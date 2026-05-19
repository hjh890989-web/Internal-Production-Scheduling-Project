# Keycloak 24 LTS 운영 가이드

본 문서는 TK-30-1-1 산출물 (`infrastructure/keycloak/` + compose service `keycloak`·`keycloak-db`) 운영 안내.

## 1. 부팅·중지

```powershell
cd infrastructure
docker compose up -d keycloak-db keycloak       # 두 컨테이너 부팅
make status                                      # healthy 확인
docker compose logs -f keycloak                  # 로그 follow
```

첫 부팅 ~80초 (Keycloak DB 마이그레이션 124 changeset + realm import + JVM).

## 2. Admin Console 접근

| URL | 용도 |
|---|---|
| http://localhost:8180/admin | Keycloak Admin Console (master realm) |
| http://localhost:8180/realms/scheduling-system/account | 사용자 계정 페이지 |
| http://localhost:8180/realms/scheduling-system/protocol/openid-connect/auth | OIDC Auth endpoint |
| http://localhost:8180/realms/scheduling-system/.well-known/openid-configuration | OIDC discovery |

**DEV 자격**:
- 사용자: `admin`
- 패스워드: `.env` 의 `KEYCLOAK_ADMIN_PASSWORD`

## 3. Realm 구성 (`scheduling-system`)

자동 import (`realm-scheduling-system.json` → `--import-realm`).

### 4 Role (CLAUDE.md RBAC)
| Keycloak Role | Spring Authority | 대상 |
|---|---|---|
| `PLANNER` | `ROLE_PLANNER` | 생산계획 — 작성·확정·override (BR-X05 작성자 ≠ 승인자) |
| `STK_USER` | `ROLE_STK_USER` | 현장 STK — 시뮬뷰·제안 |
| `IT_OPS` | `ROLE_IT_OPS` | IT 운영 — 마스터·Actuator·Grafana |
| `READ_ONLY` | `ROLE_READ_ONLY` | 감사·임원 — 조회 |

### Client (`scheduling-app`)
- Protocol: openid-connect
- Confidential client + PKCE S256
- Redirect URIs: `https://localhost/*`, `https://scheduling.internal/*`
- Web Origins: `https://localhost`, `https://scheduling.internal`
- Access token lifespan: 30분
- SSO session idle: 8시간 / max: 10시간

### Password Policy
- 12자 이상
- 숫자 1개+ / 대문자 1개+ / 특수문자 1개+ (NFR-SEC-007)
- Brute-force protection: 5회 실패 시 잠금 (`failureFactor=5`)

## 4. 사용자 추가 (수동, Sprint 1+ Keycloak Admin REST API로 자동화)

Admin Console → `scheduling-system` realm → Users → Add user:
- Username, Email, First/Last name
- 비밀번호 설정 (Temporary OFF — 영구) 또는 Temporary ON (사용자 첫 로그인 시 재설정)
- Role mapping → Realm Roles → PLANNER 등 할당

## 5. OIDC 토큰 발급 검증 (Spring Boot 통합 전 sanity)

```powershell
# Admin token (master realm)
curl -X POST 'http://localhost:8180/realms/master/protocol/openid-connect/token' `
  -d 'grant_type=password' -d 'client_id=admin-cli' `
  -d 'username=admin' -d "password=$env:KEYCLOAK_ADMIN_PASSWORD"

# Application user token (scheduling-system realm, scheduling-app client)
curl -X POST 'http://localhost:8180/realms/scheduling-system/protocol/openid-connect/token' `
  -d 'grant_type=password' -d 'client_id=scheduling-app' `
  -d 'client_secret=dev-client-secret-replace-in-prod' `
  -d 'username=<user>' -d 'password=<pwd>'
```

## 6. /health/ready

```powershell
curl http://localhost:8180/health/ready
# → {"status":"UP","checks":[{"name":"Keycloak database...","status":"UP"}]}
```

Keycloak 24는 health endpoint를 8080 port에서 제공 (25+ 부터는 별도 management port 9000).

## 7. 데이터 영속성 (volume)

- `keycloak-db-data` volume 에 PostgreSQL data 저장.
- `docker compose down` 후 `up` → 사용자·realm·세션 보존.
- `docker compose down -v` → ⚠ realm 손실 (자동 import 다시 실행).

## 8. 환경별 차이

| 항목 | DEV | STG/PROD |
|---|---|---|
| `KC_HOSTNAME` | `localhost` | `scheduling.internal` |
| `KC_HOSTNAME_STRICT` | `false` | `true` |
| `KC_HOSTNAME_STRICT_HTTPS` | `false` | `true` |
| ports | `127.0.0.1:8180→8080` | NGINX `/auth/*` reverse proxy only (host 직접 노출 X) |
| `realm.sslRequired` | `external` (HTTP OK) | `all` (HTTPS 강제) |
| `client.secret` | `dev-client-secret-replace-in-prod` | 사내 vault에서 주입 |
| TLS | DEV 자체 서명 | 사내 CA |
| Brute-force lockout | 5회 | 5회 (동일) |

## 9. NGINX 통합 (Sprint 1+ — 본 Task 외)

향후 `infrastructure/nginx/conf.d/scheduling.conf` 에 `/auth/*` upstream 추가:

```nginx
upstream keycloak {
    server keycloak:8080;
    keepalive 16;
}

location /auth/ {
    proxy_pass http://keycloak;
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_buffer_size 128k;
    proxy_buffers 4 256k;
}
```

이렇게 하면 `https://localhost/auth/admin` 으로 NGINX 통과 접근 (Mixed-content 회피).

## 10. 백업·복원

```powershell
# realm export (정기 백업)
docker exec scheduling-keycloak /opt/keycloak/bin/kc.sh export `
  --dir /tmp/export --realm scheduling-system --users realm_file
docker cp scheduling-keycloak:/tmp/export ./backup/keycloak-$(Get-Date -Format yyyyMMdd)

# DB level 백업 (전체)
docker exec scheduling-keycloak-db pg_dump -U keycloak keycloak | gzip > ./backup/keycloak-db-$(Get-Date -Format yyyyMMdd).sql.gz
```

복원:
```powershell
gunzip -c backup/keycloak-db-YYYYMMDD.sql.gz | docker exec -i scheduling-keycloak-db psql -U keycloak -d keycloak
docker compose restart keycloak
```

## 11. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| 부팅 90초 후 unhealthy | realm import 실패 또는 DB 연결 거부 | `docker logs scheduling-keycloak` → import error 또는 keycloak-db 연결 확인 |
| admin token 401 | `KEYCLOAK_ADMIN_PASSWORD` 변경 후 미반영 | `make clean` (⚠ 데이터 손실) 또는 admin REST API로 패스워드 변경 |
| OIDC redirect_uri 거부 | client 의 redirectUris 미일치 | Admin Console → clients → scheduling-app → Valid redirect URIs 확인 |
| brute-force lockout | 5회 실패 후 60초 대기 | Admin Console → Authentication → Brute Force Detection → Reset user |

## 12. 다음 단계

본 Task 산출물 위에서 Sprint 1+ 진행:
- **TK-30-1-2** SAML/OIDC 사내 IdP 페더레이션 (Active Directory 연동)
- **TK-30-1-3** Local fallback (IdP 장애 시)
- **TK-30-2-1·2** Spring Security 6 Resource Server + RBAC 통합 (`ROLE_PLANNER` 등)
