#!/bin/sh
# =============================================================================
# archive-wal.sh — PostgreSQL WAL archive_command (TK-33-2-2)
# =============================================================================
# postgresql.conf 의 archive_command 가 호출:
#   archive_command = '/usr/local/bin/archive-wal.sh %p %f'
#
# 인자:
#   $1 = %p  WAL 파일의 절대 경로 (e.g., pg_wal/000000010000000000000003)
#   $2 = %f  WAL 파일명만 (e.g., 000000010000000000000003)
#
# 종료 코드:
#   0  성공  — PostgreSQL 이 다음 WAL 로 진행
#   비0 실패 — PostgreSQL 이 archive_command 재시도 (WAL 보존)
#
# 환경:
#   ARCHIVE_DEST   default: /backup/wal/
#   (PROD 에서는 NAS bind 마운트 또는 rclone wrapper 로 확장)
# =============================================================================

set -e

WAL_PATH="$1"
WAL_NAME="$2"
ARCHIVE_DEST="${ARCHIVE_DEST:-/backup/wal}"

# 디렉토리 보장
mkdir -p "${ARCHIVE_DEST}"

DEST_FILE="${ARCHIVE_DEST}/${WAL_NAME}"

# 중복 방지 — 이미 archive 된 WAL 은 fail (PostgreSQL 가 skip 시그널로 해석 가능)
if [ -f "${DEST_FILE}" ]; then
    echo "archive-wal: ${WAL_NAME} already archived — skip" >&2
    exit 1
fi

# atomic copy (.tmp → rename)
cp "${WAL_PATH}" "${DEST_FILE}.tmp"
mv "${DEST_FILE}.tmp" "${DEST_FILE}"

# (옵션) PROD: NAS 또는 S3-compat 업로드 — 본 line uncomment 후 활성
# rclone copyto "${DEST_FILE}" "nas:scheduling-backup/wal/${WAL_NAME}" --quiet || exit 1

exit 0
