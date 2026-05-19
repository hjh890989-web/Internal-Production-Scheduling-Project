#!/usr/bin/env bash
# =============================================================================
# blue_green_deploy.sh — Jenkins 자동 Blue-Green 배포 (TK-32-1-3)
# =============================================================================
# 직전 PROD blue-green-switch.sh (운영자 수동) 와 분담:
#   - blue-green-switch.sh : 운영자 명시 인자 (color + tag)
#   - blue_green_deploy.sh : Jenkins 자동 trigger + 활성 색상 동적 검출 + smoke test
#
# 사용:
#   bash infrastructure/scripts/blue_green_deploy.sh <image_tag>
#
# 예:
#   bash infrastructure/scripts/blue_green_deploy.sh harbor.internal/scheduling/backend:42-abc1234
#
# 흐름 (≤30초 토글 SLO — NFR-REL-001 99.5% 가용성):
#   1. NGINX scheduling.conf 의 ACTIVE_COLOR_MARKER 영역에서 현재 색상 검출
#   2. NEXT 색상 컨테이너에 새 image 배포 (--no-deps --build)
#   3. healthcheck 통과 대기 (최대 60s)
#   4. NEXT 컨테이너에서 smoke test (/actuator/health/readiness)
#   5. sed 로 NGINX conf 활성 색상 line 교체
#   6. nginx -t (구문 검증) → nginx -s reload (graceful, 신규 worker)
#   7. CURRENT 컨테이너 drain (10s 대기 후 stop)
#
# 실패 시 자동 롤백: NEXT 컨테이너 stop + 이전 색상 유지.
# =============================================================================

set -euo pipefail

IMAGE_TAG="${1:?image tag 필수 — 예: harbor.internal/scheduling/backend:42-abc1234}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NGINX_CONF="$INFRA_DIR/nginx/conf.d/scheduling.conf"
COMPOSE_FILE="${COMPOSE_FILE:-$INFRA_DIR/docker-compose.yml}"

cd "$INFRA_DIR"

# ---------- 1. 현재 활성 색상 검출 ----------
# scheduling.conf 의 'upstream backend { server backend-<color>:8080; }' 라인에서 추출.
# blue/green 둘 다 fallback 대비 — 미발견 시 blue 기본.
CURRENT_COLOR=$(grep -oP 'server backend-\K(blue|green)(?=:8080)' "$NGINX_CONF" 2>/dev/null | head -1 || echo "blue")
NEXT_COLOR=$([[ "$CURRENT_COLOR" == "blue" ]] && echo "green" || echo "blue")

echo "🔄 배포 시작: ${CURRENT_COLOR} → ${NEXT_COLOR}"
echo "   image:    $IMAGE_TAG"
echo "   compose:  $COMPOSE_FILE"
echo "─────────────────────────────────────────"

# ---------- 2. NEXT 색상 컨테이너 새 image 배포 ----------
echo "[1/6] ${NEXT_COLOR} 컨테이너 배포…"
export BACKEND_IMAGE="$IMAGE_TAG"
docker compose -f "$COMPOSE_FILE" up -d --no-deps "backend-${NEXT_COLOR}"

# ---------- 3. healthy 대기 (최대 60초) ----------
echo "[2/6] ${NEXT_COLOR} healthy 대기 (최대 60s)…"
DEADLINE=$(($(date +%s) + 60))
STATUS="starting"
while (( $(date +%s) < DEADLINE )); do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' "scheduling-backend-${NEXT_COLOR}" 2>/dev/null || echo "missing")
    if [[ "$STATUS" == "healthy" ]]; then
        echo "  ✓ ${NEXT_COLOR} healthy"
        break
    fi
    sleep 3
done

if [[ "$STATUS" != "healthy" ]]; then
    echo "  ❌ ${NEXT_COLOR} healthcheck 실패 (${STATUS}) — 자동 롤백"
    docker compose -f "$COMPOSE_FILE" logs "backend-${NEXT_COLOR}" --tail 30
    docker compose -f "$COMPOSE_FILE" stop "backend-${NEXT_COLOR}"
    exit 2
fi

# ---------- 4. NEXT smoke test ----------
echo "[3/6] ${NEXT_COLOR} smoke test (/actuator/health/readiness)…"
if ! docker exec "scheduling-backend-${NEXT_COLOR}" \
        wget --no-verbose --tries=1 --spider \
        "http://localhost:8080/actuator/health/readiness" 2>/dev/null; then
    echo "  ❌ smoke test 실패 — 자동 롤백"
    docker compose -f "$COMPOSE_FILE" stop "backend-${NEXT_COLOR}"
    exit 3
fi
echo "  ✓ smoke test PASS"

# ---------- 5. NGINX upstream 토글 (sed in-place) ----------
echo "[4/6] NGINX upstream ${CURRENT_COLOR} → ${NEXT_COLOR} 토글…"
# scheduling.conf 의 'server backend-blue:8080' 또는 'server backend-green:8080' 라인 교체.
# 백업 .bak 생성 (Rollback 자동화에 활용).
sed -i.bak "s|server backend-${CURRENT_COLOR}:8080|server backend-${NEXT_COLOR}:8080|g" "$NGINX_CONF"

# ---------- 6. NGINX 무중단 reload ----------
echo "[5/6] NGINX 구문 검증 + graceful reload…"
docker compose -f "$COMPOSE_FILE" exec -T nginx nginx -t
docker compose -f "$COMPOSE_FILE" exec -T nginx nginx -s reload
echo "  ✓ NGINX reload 완료 — 신규 요청 ${NEXT_COLOR} 로 라우팅"

# ---------- 7. CURRENT 컨테이너 graceful drain ----------
echo "[6/6] ${CURRENT_COLOR} drain (10s 대기 후 stop)…"
sleep 10
docker compose -f "$COMPOSE_FILE" stop "backend-${CURRENT_COLOR}" 2>/dev/null || true

echo ""
echo "🎉 배포 완료: ${IMAGE_TAG} → ${NEXT_COLOR} 활성"
echo "   롤백: bash infrastructure/scripts/blue_green_rollback.sh"
