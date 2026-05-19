#!/usr/bin/env bash
# =============================================================================
# blue_green_rollback.sh — 1줄 즉시 롤백 (TK-32-1-3)
# =============================================================================
# 직전 배포 (blue_green_deploy.sh) 직후 문제 발견 시 호출.
# NGINX upstream 만 이전 색상으로 토글 — 컨테이너는 stop 상태에서 재기동.
#
# 사용: bash infrastructure/scripts/blue_green_rollback.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NGINX_CONF="$INFRA_DIR/nginx/conf.d/scheduling.conf"
COMPOSE_FILE="${COMPOSE_FILE:-$INFRA_DIR/docker-compose.yml}"

cd "$INFRA_DIR"

# 현재·이전 색상 검출
CURRENT=$(grep -oP 'server backend-\K(blue|green)(?=:8080)' "$NGINX_CONF" | head -1)
PREVIOUS=$([[ "$CURRENT" == "blue" ]] && echo "green" || echo "blue")

echo "↩️  Rollback: ${CURRENT} → ${PREVIOUS}"

# 이전 색상 컨테이너 상태 확인
PREV_STATUS=$(docker inspect --format='{{.State.Status}}' "scheduling-backend-${PREVIOUS}" 2>/dev/null || echo "missing")
if [[ "$PREV_STATUS" == "missing" ]]; then
    echo "  ❌ ${PREVIOUS} 컨테이너 없음 — 자동 롤백 불가"
    echo "     docker compose up -d backend-${PREVIOUS} 후 재시도"
    exit 1
fi

# 이전 컨테이너 재기동 (stopped 상태면)
if [[ "$PREV_STATUS" != "running" ]]; then
    echo "  ⚙  ${PREVIOUS} 재기동 ($PREV_STATUS → running)…"
    docker compose -f "$COMPOSE_FILE" start "backend-${PREVIOUS}"
    # healthy 대기 (최대 30s — 이미 부팅된 상태라 짧음)
    DEADLINE=$(($(date +%s) + 30))
    while (( $(date +%s) < DEADLINE )); do
        S=$(docker inspect --format='{{.State.Health.Status}}' "scheduling-backend-${PREVIOUS}" 2>/dev/null || echo "?")
        [[ "$S" == "healthy" ]] && break
        sleep 2
    done
fi

# NGINX upstream 토글 (deploy 가 만든 .bak 활용 — 동일 sed 패턴)
sed -i "s|server backend-${CURRENT}:8080|server backend-${PREVIOUS}:8080|g" "$NGINX_CONF"

# NGINX reload
docker compose -f "$COMPOSE_FILE" exec -T nginx nginx -t
docker compose -f "$COMPOSE_FILE" exec -T nginx nginx -s reload

# CURRENT 컨테이너 stop (롤백 완료)
docker compose -f "$COMPOSE_FILE" stop "backend-${CURRENT}" 2>/dev/null || true

echo "✅ Rollback 완료 — ${PREVIOUS} 활성"
