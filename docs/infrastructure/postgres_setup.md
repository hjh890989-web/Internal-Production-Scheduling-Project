# PostgreSQL 16 운영 가이드

본 문서는 TK-00-1-1 산출물 (`infrastructure/postgres/`) 운영 안내.

## 1. 부팅·중지

```powershell
# DEV — infrastructure/.env 작성 후
cd infrastructure
docker compose up postgres -d
docker compose ps              # (healthy) 확인 ≤30초
docker compose logs postgres   # 로그 (KST timezone)
docker compose down            # 중지 (volume 보존)
```

## 2. 스키마·권한 구조 (SAD §6.1.1)

| 스키마 | 용도 | app_user | auditor | master_admin |
|---|---|---|---|---|
| `app` | 운영 데이터 (PRODUCT·ORDER·VC_SCHEDULE) | ALL | — | — |
| `audit` | 감사 로그 (≥3년) | **INSERT only** | SELECT | — |
| `master` | 마스터 데이터 (제약·우선순위·KD) | **SELECT only** | — | ALL |

## 3. 접속 검증

```powershell
# 컨테이너 내부 (DEV)
docker exec -it scheduling-postgres psql -U app_user -d scheduling

# 호스트에서 (psql 설치 시)
psql -h localhost -p 5432 -U app_user -d scheduling

# 스키마 확인 — 3개 (app·audit·master) 보여야 함
\dn

# timezone 확인 (Asia/Seoul, BR-X04)
SELECT current_setting('timezone');
```

## 4. 데이터 영속성 (volume)

- 데이터는 Docker volume `postgres-data` 에 저장 (호스트: `/var/lib/docker/volumes/`).
- `docker compose down` 후 `up` → 데이터 보존됨.
- 완전 초기화: `docker compose down -v` (⚠ volume까지 삭제 — 데이터 손실).

## 5. 백업·복원 (NFR-REL-005, RPO ≤24h, RTO ≤4h)

본 Task는 컨테이너만 구성. 풀 백업 자동화는 TK-33-2-1 (Sprint 0 EP-33).

수동 백업:
```powershell
docker exec scheduling-postgres pg_basebackup -U app_user -D /tmp/backup -Ft -z -P
docker cp scheduling-postgres:/tmp/backup ./backup-$(Get-Date -Format yyyyMMdd)
```

복원:
```powershell
docker compose down -v
# volume 삭제 후 backup 디렉토리를 postgres-data volume 으로 복사 → up
```

## 6. WAL 아카이브 (RPO 단축)

`postgresql.conf` 의 `archive_command = 'cp %p /backup/wal/%f'` 가 활성.
WAL 파일이 호스트 `/backup/wal/` 디렉토리에 쌓이도록 volume 마운트는 STG/PROD 별도 docker-compose.{staging,prod}.yml 에서 추가.

DEV 는 비활성 (volume 미마운트 → archive_command 실패 무시).

## 7. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| 컨테이너 부팅 실패 — `POSTGRES_PASSWORD: variable is not set` | `.env` 없음/POSTGRES_PASSWORD 누락 | `cp .env.example .env` + 패스워드 채움 |
| healthcheck unhealthy | 30초 내 부팅 안 됨 (대용량 데이터 복원) | `start_period: 30s` → 120s 늘림 (docker-compose.yml 임시) |
| psql 접속 거부 | role/password 불일치 | `docker compose down -v` → up (init script 재실행) |

## 8. 운영 환경 차이

| 항목 | DEV | STG/PROD |
|---|---|---|
| ports | `127.0.0.1:5432` | `expose: ["5432"]` (NGINX·BE 만 접근) |
| 인증서·암호 | `.env` | 사내 vault (HashiCorp Vault 또는 사내 secret store) |
| WAL 아카이브 | 비활성 | `/backup/wal/` NAS 마운트 |
| 백업 자동화 | 수동 | systemd timer (TK-33-2-1) |
