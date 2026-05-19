# Secrets 관리 정책 (TK-33-1-3)

본 문서는 사내 공정 스케줄링 시스템의 환경 변수·비밀 관리 정책. NFR-SEC-005 (DLP) 정합.

---

## 1. 원칙 (Phase 1.0)

| 원칙 | 강제 방법 |
|---|---|
| `.env.*` (example 제외) **git 추적 금지** | `.gitignore` rule + `git check-ignore` 정기 검증 |
| PROD secret = 사내 vault 주입 (평문 파일 금지) | 운영 절차 (본 가이드 §4) |
| `.env.prod` 파일 = chmod **600** (소유자만 read) | OS file permission |
| 분기별 1회 rotation (DB·Keycloak·Backup·Slack·Sentry) | 본 가이드 §5 + cron job |
| Emergency 비밀번호 = 봉인 봉투 보관 (사용 시 IT lead 결재) | 운영 절차 (idp-failover.md 참조) |
| 사고 시 즉시 rotation + audit 로그 | EP-11 Audit + Slack alert (Sprint 1+) |

---

## 2. 환경별 .env 파일

| 환경 | 파일 | git 추적 | secrets 출처 |
|---|---|---|---|
| Template | `.env.dev.example`·`.env.stg.example`·`.env.prod.example` | ✓ | 자리표시자 (`<vault>`) |
| **DEV** | `.env` 또는 `.env.dev` | ✗ | 개발자 PC 평문 (chmod 600 권장) |
| **STG** | `.env.stg` | ✗ | 사내 vault (개발 vault) |
| **PROD** | `.env.prod` | ✗ | 사내 vault (production vault, IT 부서 관리) |

### .gitignore 룰 (확인됨)
```gitignore
infrastructure/.env
infrastructure/.env.*
!infrastructure/.env.example
!infrastructure/.env.*.example
infrastructure/jenkins/secrets/*
!infrastructure/jenkins/secrets/.gitkeep
infrastructure/nginx/ssl/*.{key,crt,pem}
```

### 검증 명령 (CI/CD pre-commit hook 검토)
```bash
# .env.* (example 제외) 가 추적되지 않는지 확인
git check-ignore infrastructure/.env.prod infrastructure/.env.stg infrastructure/.env
# → 매칭 룰 출력 (= 무시 처리됨)

# negation rule (example) 정상 동작 확인
git check-ignore -v infrastructure/.env.prod.example
# → 출력에 ! rule 매칭 (= 추적됨)
```

---

## 3. Secret 카테고리 + Rotation 주기

### 3.1 PostgreSQL · Redis 비밀번호
- `POSTGRES_PASSWORD`, `REDIS_PASSWORD`
- **분기 1회 rotation** (Q1·Q2·Q3·Q4 — IT 인프라팀 + DevOps)
- 절차: vault 갱신 → `docker compose restart postgres redis backend-*` → 검증

### 3.2 Keycloak admin·DB 비밀번호 + 일반 사용자 PIN (NFR-SEC-007 v1.5)
- `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_DB_PASSWORD` (관리자) — 분기 1회 rotation
- 일반 사용자 PIN — 사용자 자율 변경 권장 (분기 캠페인). Login ID = 사번 8자리.
- emergency 계정 (`99000001`~`99000003`) — 봉인 봉투 (idp-failover.md)
- 절차: Keycloak Admin REST API → vault 갱신 → `docker compose restart keycloak`

#### PIN 정책 운영 (NFR-SEC-007 §pin-policy — 2026-05-19 v1.5)
- **사용자 ID**: 사번 (숫자 8자리) — IT 부서 발급. 이메일 로그인 불허.
- **PIN**: 숫자 4자리 (`^[0-9]{4}$`). 5회 실패 → 10분 자동 잠금.
- **잠금 후 즉시 해제 필요 시**: IT lead 가 Keycloak Admin Console → Users → Unlock.
- **보안 교육 (분기)**: (a) PIN 추측 회피 (1234·0000 등 금지 권장 — 정책 강제 아님),
  (b) 어깨너머 보기 차단 (블록 입력기 사용), (c) PIN 공유 금지 (1인 1계정 audit).

### 3.3 Jenkins admin · Harbor · Sonar token
- `JENKINS_ADMIN_PASSWORD`, `HARBOR_PASSWORD`, `SONARQUBE_TOKEN`
- 분기 1회 + Keycloak federation 활성 후 polldemic
- 절차: Jenkins Manage → Credentials 갱신 + CASC `casc/jenkins.yaml` 변수 재주입

### 3.4 Backup 암호화 키
- `BACKUP_ENCRYPTION_KEY` (AES-256)
- **년 1회 rotation** (백업 호환성 — 이전 키는 archive 1년 보관)
- 절차: TK-33-2-* 백업 스크립트 + vault key versioning

### 3.5 Slack webhook · Sentry DSN
- `SLACK_WEBHOOK_URL`, `SENTRY_DSN`
- rotation 불필요 (vendor 발급, 갱신은 사용자 권한 변경 시)
- 분기 audit (사용 채널·권한 검토)

---

## 4. PROD secret 주입 (Phase 1.0 — 평문 .env.prod)

```bash
# 1. PROD 서버 (사내 single-host)
ssh devops@prod-server

# 2. 사내 vault 에서 .env.prod 생성
sudo mkdir -p /opt/scheduling/infrastructure
sudo cp /etc/scheduling-secrets/.env.prod /opt/scheduling/infrastructure/.env.prod
sudo chmod 600 /opt/scheduling/infrastructure/.env.prod
sudo chown root:docker /opt/scheduling/infrastructure/.env.prod

# 3. compose 부팅
cd /opt/scheduling/infrastructure
docker compose --env-file .env.prod \
    -f docker-compose.yml -f docker-compose.prod.yml \
    up -d
```

**`/etc/scheduling-secrets/` 디렉토리**:
- 사내 vault 또는 secret manager (HashiCorp Vault / 1Password Connect) 에서 fetch
- IT 관리자만 접근 권한
- 30일마다 vault 토큰 rotation (사내 정책 준수)

---

## 5. Vault 통합 (Phase 2+ 검토)

현재 Phase 1.0 = 평문 `.env.prod` + chmod 600. Phase 2+ 후보:

### 옵션 A. HashiCorp Vault
- Vault Agent 가 secret fetch 후 .env.prod 자동 생성
- 토큰 자동 rotation (Vault TTL 24시간)
- 사내 인프라 추가 (Vault cluster)

### 옵션 B. 1Password Connect
- 1Password 사내 라이센스 활용
- Connect API 토큰 + docker-compose env_file 주입
- SaaS 외부 의존 (사내망 정책 검토 필요 — NFR-SEC-001)

### 옵션 C. Doppler
- 환경별 secret CLI 자동 동기
- 가장 간편 — 다만 SaaS 의존

### 옵션 D. Docker Secrets + Swarm
- Docker Swarm 모드 활용
- 사내 단일 호스트라 Swarm overkill — 비추

**Phase 1.0 기본**: 평문 `.env.prod` + chmod 600 + 분기 rotation. Phase 2+ 사용자 5+ 인프라팀 결정.

---

## 6. 사고 대응 (Secret 노출)

### 6.1 GitHub 추적 발견
```bash
# 1. 즉시 git history 에서 제거
git filter-repo --invert-paths --path infrastructure/.env.prod    # ⚠ 모든 clone 재 push 필요
# (또는 git filter-branch — deprecated)

# 2. 노출된 secret 즉시 rotation (KEYCLOAK_ADMIN_PASSWORD 등 모두)

# 3. 사고 로그
docker exec scheduling-postgres psql -U app_user -d scheduling -c "
    INSERT INTO audit.security_incident (occurred_at, severity, summary, response)
    VALUES (NOW(), 'CRITICAL', 'PROD secret leaked to GitHub', 'Immediate rotation + git history rewrite');
"
```

### 6.2 Emergency 계정 사용
- idp-failover.md §Emergency-계정 참조
- 사용 직후 비밀번호 변경 + audit 로그

### 6.3 IT lead 보고
- Slack `#security-incident` (Sprint 1+ EP-44 ST-44-3)
- 4시간 내 보고서 작성 (RTO·RPO 영향)

---

## 7. 검증 체크리스트 (PROD sign-off)

- [ ] 3 `.env.*.example` 모두 git 추적 (`git ls-files infrastructure/.env.*.example | wc -l == 3`)
- [ ] `.env`·`.env.prod` 등 실 파일 모두 .gitignore 처리 (`git check-ignore` 확인)
- [ ] PROD 서버 `/opt/scheduling/infrastructure/.env.prod` = chmod 600
- [ ] 모든 `<vault>` 자리표시자 채움 (절대 평문 commit 금지)
- [ ] 분기별 rotation cron job 등록 (PostgreSQL·Keycloak·Backup)
- [ ] Emergency 계정 비밀번호 봉인 봉투 (IT 금고)
- [ ] 사내 보안팀 sign-off (이 가이드 + .gitignore 룰)

---

## 8. 환경별 .env 파일 분리 패턴 (Compose --env-file)

3 환경 모두 동일한 `docker-compose.yml` (base) + 환경별 override + 환경별 .env:

| 환경 | 명령 |
|---|---|
| DEV | `docker compose --env-file .env up -d` (또는 .env.dev) |
| STG | `docker compose --env-file .env.stg -f docker-compose.yml -f docker-compose.stg.yml up -d` |
| PROD | `docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d` |

→ 단일 git 저장소 + 단일 base compose + 환경별 변수 분리 = GitOps 친화.
