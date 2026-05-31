#!/usr/bin/env bash
# =============================================================================
# blue_green_rollback.sh — 1줄 즉시 롤백 (TK-32-1-3)
# =============================================================================
# 직전 배포 (blue_green_deploy.sh) 직후 문제 발견 시 호출.
# NGINX upstream 만 이전 색상으로 토글 — 컨테이너는 stop 상태에서 재기동.
#
# 사용: bash infrastructure/scripts/blue_green_rollback.sh
#
# DRY_RUN=1 시 cp/reload/stop 건너뜀 (흐름 검증):
#   DRY_RUN=1 bash infrastructure/scripts/blue_green_rollback.sh
# =============================================================================

set -euo pipefail

DRY_RUN="${DRY_RUN:-0}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NGINX_DIR="$INFRA_DIR/nginx"
# Issue#1 fix: PROD mount 경로 — prod-active.conf (switch.sh 동일 패턴)
NGINX_CONF="$NGINX_DIR/prod-active.conf"
COMPOSE_FILE="${COMPOSE_FILE:-$INFRA_DIR/docker-compose.prod.yml}"

cd "$INFRA_DIR"

# DRY_RUN helper
_exec() {
    if [[ "$DRY_RUN" == "1" ]]; then
        echo "[DRY-RUN] would execute: $*"
    else
        "$@"
    fi
}

# 현재·이전 색상 검출
CURRENT=$(grep -oP 'server backend-\K(blue|green)(?=:8080)' "$NGINX_CONF" | head -1)
PREVIOUS=$([[ "$CURRENT" == "blue" ]] && echo "green" || echo "blue")

echo "Rollback: ${CURRENT} → ${PREVIOUS}"
[[ "$DRY_RUN" == "1" ]] && echo "   *** DRY_RUN=1 — 실제 변경 없음 ***"

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
    _exec docker compose -f "$COMPOSE_FILE" start "backend-${PREVIOUS}"
    # healthy 대기 (최대 30s — 이미 부팅된 상태라 짧음)
    DEADLINE=$(($(date +%s) + 30))
    while (( $(date +%s) < DEADLINE )); do
        S=$(docker inspect --format='{{.State.Health.Status}}' "scheduling-backend-${PREVIOUS}" 2>/dev/null || echo "?")
        [[ "$S" == "healthy" ]] && break
        sleep 2
    done
fi

# NGINX upstream 토글 (cp prod-<previous>.conf prod-active.conf — switch.sh 동일 패턴)
# Issue#1 fix: sed → cp 방식으로 전환
_exec cp "$NGINX_DIR/prod-${PREVIOUS}.conf" "$NGINX_CONF"

# NGINX reload
_exec docker compose -f "$COMPOSE_FILE" exec -T nginx nginx -t
_exec docker compose -f "$COMPOSE_FILE" exec -T nginx nginx -s reload

# CURRENT 컨테이너 stop (롤백 완료)
_exec docker compose -f "$COMPOSE_FILE" stop "backend-${CURRENT}" 2>/dev/null || true

echo "Rollback NGINX 전환 완료 — ${PREVIOUS} 활성"

# ---------- readiness smoke test (NFR-REL-001 99.5% 가용성 정합) ----------
echo "readiness smoke test (최대 30s)…"
HEALTH_URL="http://localhost/api/actuator/health"
DEADLINE=$(($(date +%s) + 30))
SMOKE_OK=0
while (( $(date +%s) < DEADLINE )); do
    if [[ "$DRY_RUN" == "1" ]]; then
        echo "[DRY-RUN] would execute: curl -fsS $HEALTH_URL"
        SMOKE_OK=1
        break
    fi
    HTTP_STATUS=$(curl -o /dev/null -w '%{http_code}' -fsS --max-time 5 "$HEALTH_URL" 2>/dev/null || echo "000")
    if [[ "$HTTP_STATUS" == "200" ]]; then
        SMOKE_OK=1
        echo "  ✓ readiness UP (HTTP 200)"
        break
    fi
    echo "  $(date +%H:%M:%S)  HTTP $HTTP_STATUS — 재시도..."
    sleep 3
done

if [[ "$SMOKE_OK" != "1" ]]; then
    echo "  ❌ readiness smoke 실패 (30s 초과) — 수동 확인 필요"
    echo "     docker compose logs backend-${PREVIOUS} --tail 30"
    exit 4
fi

echo "Rollback 완료 — ${PREVIOUS} 활성"
