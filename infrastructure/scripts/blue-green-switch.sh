#!/usr/bin/env bash
# =============================================================================
# blue-green-switch.sh — PROD Blue/Green 무중단 배포 스크립트
# =============================================================================
# 사용:
#   bash infrastructure/scripts/blue-green-switch.sh <blue|green> [<tag>]
#
# 예:
#   bash infrastructure/scripts/blue-green-switch.sh green 25-abc1234
#       → backend-green 컨테이너 새 image (tag=25-abc1234) 로 시작
#       → healthy 대기
#       → NGINX upstream backend-green 전환
#       → backend-blue stop
# =============================================================================

set -euo pipefail

TARGET_COLOR="${1:-}"
NEW_TAG="${2:-latest}"

if [[ ! "$TARGET_COLOR" =~ ^(blue|green)$ ]]; then
    echo "Usage: $0 <blue|green> [<tag>]"
    echo "  예: $0 green 25-abc1234"
    exit 1
fi

PREVIOUS_COLOR=$([ "$TARGET_COLOR" = "blue" ] && echo "green" || echo "blue")
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NGINX_DIR="$INFRA_DIR/nginx"

cd "$INFRA_DIR"

echo "🚀 Blue-Green Switch: $PREVIOUS_COLOR → $TARGET_COLOR (tag=$NEW_TAG)"
echo "─────────────────────────────────────────────────────────────"

# 1) 새 색깔 컨테이너 시작 (TAG_BLUE 또는 TAG_GREEN 환경 변수로 image tag 주입)
export "TAG_${TARGET_COLOR^^}=$NEW_TAG"
echo "[1/4] backend-$TARGET_COLOR 시작 (image tag=$NEW_TAG)..."
docker compose --env-file .env.prod \
    -f docker-compose.yml -f docker-compose.prod.yml \
    up -d "backend-$TARGET_COLOR"

# 2) Health check 대기 (최대 120s — JVM 부팅 60s + 여유)
echo "[2/4] healthy 대기 (최대 120s)..."
DEADLINE=$(($(date +%s) + 120))
while (( $(date +%s) < DEADLINE )); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' "scheduling-backend-$TARGET_COLOR" 2>/dev/null || echo "missing")
    if [[ "$STATUS" == "healthy" ]]; then
        echo "  ✓ backend-$TARGET_COLOR healthy"
        break
    fi
    echo "  $(date +%H:%M:%S)  $STATUS"
    sleep 5
done

if [[ "$STATUS" != "healthy" ]]; then
    echo "  ❌ backend-$TARGET_COLOR healthcheck 타임아웃 — 배포 중단"
    docker compose logs "backend-$TARGET_COLOR" --tail 30
    exit 2
fi

# 3) NGINX upstream switch (prod-active.conf 갱신 + reload)
echo "[3/4] NGINX upstream 전환..."
cp "$NGINX_DIR/prod-$TARGET_COLOR.conf" "$NGINX_DIR/prod-active.conf"
docker compose exec -T nginx nginx -t          # 구문 사전 검증
docker compose exec -T nginx nginx -s reload
echo "  ✓ NGINX → backend-$TARGET_COLOR"

# 4) 이전 색깔 graceful stop (30초 진행 중 요청 마무리 대기)
echo "[4/4] backend-$PREVIOUS_COLOR graceful stop (30s 대기)..."
sleep 30
docker compose stop "backend-$PREVIOUS_COLOR" 2>/dev/null || true
echo "  ✓ backend-$PREVIOUS_COLOR stopped"

echo ""
echo "🎉 Switch 완료: $PREVIOUS_COLOR → $TARGET_COLOR"
echo "    Rollback: bash infrastructure/scripts/blue-green-switch.sh $PREVIOUS_COLOR"
