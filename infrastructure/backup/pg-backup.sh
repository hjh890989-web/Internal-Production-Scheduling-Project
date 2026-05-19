#!/usr/bin/env bash
# =============================================================================
# pg-backup.sh — PostgreSQL 풀백업 (TK-33-2-1)
# =============================================================================
# 환경:
#   - 사내망 단일 호스트 (S3 호환 NAS 또는 사내 NFS mount → 로컬 path)
#   - 매일 02:00 KST 자동 실행 (systemd timer 또는 ofelia/cron)
#
# 환경 변수 (compose env_file 또는 systemd EnvironmentFile 로 주입):
#   POSTGRES_HOST                 default: postgres
#   POSTGRES_USER                 default: app_user
#   POSTGRES_DB                   default: scheduling
#   PGPASSWORD                    required (pg_basebackup 인증)
#   BACKUP_DIR                    default: /backup/base  (컨테이너 내 mount path)
#   BACKUP_RETENTION_DAYS         default: 30
#   SLACK_WEBHOOK_URL             optional (빈 값이면 발송 skip)
#
# 종료 코드:
#   0  성공
#   1  pg_basebackup 실패
#   2  validation 실패 (tar size < 1KB)
# =============================================================================
set -euo pipefail

POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_USER="${POSTGRES_USER:-app_user}"
POSTGRES_DB="${POSTGRES_DB:-scheduling}"
BACKUP_DIR="${BACKUP_DIR:-/backup/base}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"

TIMESTAMP=$(TZ=Asia/Seoul date +%Y%m%d_%H%M%S)
TARGET_DIR="${BACKUP_DIR}/${TIMESTAMP}"
mkdir -p "${TARGET_DIR}"

log() {
    echo "[$(TZ=Asia/Seoul date '+%Y-%m-%d %H:%M:%S KST')] $*"
}

slack_notify() {
    local emoji="$1"
    local msg="$2"
    if [[ -n "${SLACK_WEBHOOK_URL}" ]]; then
        curl -sS -X POST -H 'Content-Type: application/json' \
             --data "{\"text\":\"${emoji} PG backup ${TIMESTAMP}: ${msg}\"}" \
             "${SLACK_WEBHOOK_URL}" >/dev/null || log "Slack 발송 실패 (무시)"
    fi
}

# ---------------------------------------------------------------------------
# 1. pg_basebackup (tar.gz + WAL streaming)
# ---------------------------------------------------------------------------
log "Starting pg_basebackup → ${TARGET_DIR}"
if ! pg_basebackup \
        --host="${POSTGRES_HOST}" \
        --username="${POSTGRES_USER}" \
        --pgdata="${TARGET_DIR}" \
        --format=tar \
        --gzip \
        --compress=6 \
        --progress \
        --verbose \
        --checkpoint=fast \
        --wal-method=stream \
        --no-password
then
    log "ERROR: pg_basebackup 실패"
    slack_notify ":x:" "pg_basebackup FAILED"
    exit 1
fi

# ---------------------------------------------------------------------------
# 2. Validation — tar.gz size 검증 (최소 1KB)
# ---------------------------------------------------------------------------
BASE_TAR="${TARGET_DIR}/base.tar.gz"
if [[ ! -f "${BASE_TAR}" ]]; then
    log "ERROR: ${BASE_TAR} 미생성"
    slack_notify ":x:" "base.tar.gz missing"
    exit 2
fi

SIZE_BYTES=$(stat -c '%s' "${BASE_TAR}" 2>/dev/null || stat -f '%z' "${BASE_TAR}")
if (( SIZE_BYTES < 1024 )); then
    log "ERROR: ${BASE_TAR} size ${SIZE_BYTES}B < 1KB (suspicious)"
    slack_notify ":warning:" "base.tar.gz too small (${SIZE_BYTES}B)"
    exit 2
fi
log "Backup tar.gz: ${SIZE_BYTES} bytes"

# ---------------------------------------------------------------------------
# 3. Retention — 30일 초과 디렉토리 삭제
# ---------------------------------------------------------------------------
DELETED=$(find "${BACKUP_DIR}" -maxdepth 1 -mindepth 1 -type d \
              -mtime "+${BACKUP_RETENTION_DAYS}" -print -exec rm -rf {} + | wc -l)
log "Retention: ${DELETED} 디렉토리 삭제 (>${BACKUP_RETENTION_DAYS}d)"

# ---------------------------------------------------------------------------
# 4. Slack 알림
# ---------------------------------------------------------------------------
slack_notify ":white_check_mark:" "complete (size=$((SIZE_BYTES/1024/1024))MB, retention=${BACKUP_RETENTION_DAYS}d)"
log "DONE"
