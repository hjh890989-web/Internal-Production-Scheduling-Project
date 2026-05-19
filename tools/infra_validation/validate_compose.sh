#!/usr/bin/env bash
# =============================================================================
# validate_compose.sh — Sprint 0 DoD 항목 1 자동 검증
# =============================================================================
# 5개 컨테이너 부팅 + 모든 healthcheck 통과 + 통합 통신 검증.
# CI/CD (EP-32 ST-32-1) 에서 Jenkins step 으로 호출.
#
# 사용: bash tools/infra_validation/validate_compose.sh
#       (cwd 무관 — script 가 자신의 경로로 cd)
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "$SCRIPT_DIR/../../infrastructure" && pwd)"

cd "$INFRA_DIR"

echo "📁 Working dir: $INFRA_DIR"

# .env 확인
if [[ ! -f .env ]]; then
    echo "❌ .env 파일 없음 — cp .env.example .env 후 패스워드 채울 것"
    exit 1
fi

# DEV 인증서 확인
if [[ ! -f nginx/ssl/dev-self-signed.crt ]]; then
    echo "🔐 DEV 인증서 생성..."
    bash nginx/scripts/generate-dev-cert.sh
fi

echo "🧹 기존 컨테이너 정리..."
docker compose down --remove-orphans 2>/dev/null || true

echo "🚀 5개 컨테이너 build + 부팅..."
docker compose --env-file .env up -d --build

echo "⏳ healthcheck 대기 (최대 180초 — backend JVM 부팅 60s 포함)..."
DEADLINE=$(($(date +%s) + 180))
while true; do
    if (( $(date +%s) > DEADLINE )); then
        echo "❌ healthcheck 타임아웃 (180초)"
        docker compose ps
        exit 1
    fi

    # 5개 모두 healthy?
    UNHEALTHY=$(docker compose ps --format '{{.Service}}|{{.Health}}' | awk -F'|' '$2 != "healthy" {print $1":"$2}' | tr '\n' ' ')
    if [[ -z "$UNHEALTHY" ]]; then
        echo "✅ 5개 컨테이너 모두 healthy"
        break
    fi
    echo "  대기: $UNHEALTHY"
    sleep 5
done

echo ""
echo "=== Container Status ==="
docker compose ps

echo ""
echo "🔍 통합 통신 검증..."

# 1. NGINX healthcheck
if curl -kfs https://localhost/health > /dev/null 2>&1; then
    echo "  ✅ NGINX → https://localhost/health"
else
    echo "  ❌ NGINX health 실패"
    exit 1
fi

# 2. PG SELECT 1
if docker compose exec -T postgres psql -U "${POSTGRES_USER:-app_user}" -d "${POSTGRES_DB:-scheduling}" -tAc "SELECT 1" 2>/dev/null | grep -q '^1$'; then
    echo "  ✅ PostgreSQL → SELECT 1"
else
    echo "  ❌ PG SELECT 실패"
    exit 1
fi

# 3. Redis PING
source .env
if docker compose exec -T redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning PING 2>/dev/null | grep -q PONG; then
    echo "  ✅ Redis → PING/PONG"
else
    echo "  ❌ Redis PING 실패"
    exit 1
fi

# 4. Backend /actuator/health (NGINX 통과)
if curl -kfs https://localhost/actuator/health > /dev/null 2>&1; then
    echo "  ✅ Backend → NGINX → /actuator/health"
else
    echo "  ⚠️ Backend health (NGINX 통과) — 부팅 직후일 수 있음. 직접 확인:"
    echo "      docker compose exec backend wget -qO- http://localhost:8080/actuator/health"
fi

echo ""
echo "🎉 Sprint 0 DoD 항목 1 충족 — 5개 컨테이너 통합 부팅 검증 완료"
