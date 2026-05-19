# PostgreSQL 백업·복원 운영 가이드 (ST-33-2)

본 문서는 사내 공정 스케줄링 시스템의 PostgreSQL 백업·복원 절차.
NFR-REL-005 (RPO ≤ 24h 명목·실측 ≤ 5분) + REQ-NF-REL-005 정합.

---

## 1. 아키텍처 (TK-33-2-1·2)

| 컴포넌트 | 목적 | 위치 |
|---|---|---|
| `pg_basebackup` (TK-33-2-1) | 일 1회 풀백업 | `/backup/base/YYYYMMDD_HHMMSS/` |
| `archive_command` (TK-33-2-2) | WAL continuous archive (5분 단위) | `/backup/wal/` |
| `backup_user` role | REPLICATION 권한 (백업 전용) | PG role |
| 보존 정책 | 풀백업 30일 + WAL 7일 (DEV), PROD 30일/30일 | retention script |

### DEV / PROD 차이

| 항목 | DEV | PROD |
|---|---|---|
| 백업 트리거 | 수동 (`docker compose --profile backup`) | systemd timer 02:00 KST |
| 백업 저장소 | docker volume `pg-backup-data` | 사내 NAS bind mount `/mnt/nas/scheduling-backup` |
| WAL 저장소 | `/backup/wal` (volume) | 사내 NAS WAL 디렉토리 |
| 보존 기간 | 30일 (DEV 검증용) | 30일 + 사내 정책 |
| Slack 알림 | NOOP (`SLACK_WEBHOOK_URL` 빈값) | `#scheduling-backup` 채널 |

---

## 2. 풀백업 실행 (TK-33-2-1)

### DEV — 수동 실행
```bash
cd infrastructure
docker compose --profile backup run --rm pg-backup
```

출력:
```
[YYYY-MM-DD HH:MM:SS KST] Starting pg_basebackup → /backup/base/...
pg_basebackup: base backup completed
[YYYY-MM-DD HH:MM:SS KST] Backup tar.gz: NNNNN bytes
[YYYY-MM-DD HH:MM:SS KST] DONE
```

### PROD — systemd timer (02:00 KST)
```bash
# 설치 (1회)
sudo cp infrastructure/backup/systemd/pg-backup.service /etc/systemd/system/
sudo cp infrastructure/backup/systemd/pg-backup.timer   /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now pg-backup.timer

# 검증
systemctl list-timers pg-backup.timer
journalctl -u pg-backup.service --since "today"
```

### 백업 검증
```bash
docker compose exec postgres ls -la /backup/base/
# 디렉토리별 base.tar.gz + backup_manifest 있어야 함
```

---

## 3. WAL Archive (TK-33-2-2)

### 동작 방식
- `archive_mode = on` + `archive_timeout = 5min`
- 5분마다 WAL switch 강제 → `archive-wal.sh` 호출 → `/backup/wal/` 복사
- WAL upload 실패 시 PostgreSQL 자동 재시도 (WAL 보존)

### 검증
```bash
# 강제 WAL switch
docker compose exec postgres psql -U app_user -d scheduling -c "SELECT pg_switch_wal();"

# /backup/wal 디렉토리에 새 WAL 파일 생성 확인
docker compose exec postgres ls -lt /backup/wal/ | head -5
```

### WAL 누락 모니터링 (TK-44-3-1 후행)
- Prometheus alert: `pg_wal_archive_failed_total` rate > 0
- Grafana 대시보드: System Overview 의 'WAL archive lag' 패널 (Sprint 1+ 추가)

---

## 4. 복원 (PITR — Point-in-Time Recovery)

### 4.1 전체 복원 (백업 시점)
```bash
# 1. PG 정지
docker compose stop postgres

# 2. data 볼륨 비우기
docker volume rm scheduling_postgres-data

# 3. tar.gz 풀기 (호스트 또는 임시 컨테이너)
docker run --rm -v scheduling_postgres-data:/var/lib/postgresql/data \
           -v scheduling_pg-backup-data:/backup \
           postgres:16-alpine \
           tar -xzf /backup/base/20260519_143141/base.tar.gz -C /var/lib/postgresql/data

# 4. PG 시작
docker compose up -d postgres
```

### 4.2 PITR (특정 시점 복원)
```bash
# 1. 위 4.1 단계로 base 복원
# 2. recovery target 설정 (postgresql.conf 또는 환경)
echo "restore_command = 'cp /backup/wal/%f %p'"        >  /var/lib/postgresql/data/postgresql.auto.conf
echo "recovery_target_time = '2026-05-19 14:30:00+09'" >> /var/lib/postgresql/data/postgresql.auto.conf
touch /var/lib/postgresql/data/recovery.signal
# 3. PG 시작 → 자동 WAL replay → target_time 도달 시 promote
docker compose start postgres
```

---

## 5. 분기 복원 드릴 (TK-33-2-3 — 후속 Task)

분기 1회 STG 환경에서 의도적 복원 시연:
1. STG DB 에 dummy 변경 → 백업
2. 새 dummy 데이터 추가
3. PITR로 백업 시점 복원
4. RTO 측정 (목표: < 1시간)
5. Slack `#scheduling-ops` 보고

---

## 6. 검증 체크리스트 (PROD sign-off)

- [ ] systemd timer `OnCalendar=*-*-* 02:00:00 Asia/Seoul` 정상
- [ ] 첫 풀백업 tar.gz ≥ 100MB 확인
- [ ] `archive_mode = on` + `archive_timeout = 5min` (RPO 5분)
- [ ] `backup_user` REPLICATION 권한 + pg_hba.conf 매칭
- [ ] `/backup/base` + `/backup/wal` 사내 NAS bind mount
- [ ] 30일 retention cron 검증 (`find -mtime +30 -exec rm`)
- [ ] Slack alert 발송 (성공·실패 양쪽)
- [ ] PITR 드릴 1회 통과 (분기 1회)

---

## 7. 사고 대응

### 7.1 백업 실패 (3일 이상)
1. Slack 알림 확인 (`#scheduling-backup` 채널 silence 여부)
2. `journalctl -u pg-backup.service --since "3 days ago"` 로그 분석
3. 디스크 공간 + NAS 마운트 점검
4. 수동 백업으로 복구 후 root cause fix

### 7.2 WAL archive 실패
1. PG 로그: `archive command failed with exit code N`
2. `/backup/wal` 디스크 공간 점검 (PG 가 archive 실패 시 WAL 누적)
3. `archive-wal.sh` 권한·경로 확인
4. PG 가 자동 재시도 — 5분 내 정상 복귀 못 하면 IT lead 호출
