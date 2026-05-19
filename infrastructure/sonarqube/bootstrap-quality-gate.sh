#!/usr/bin/env bash
# =============================================================================
# bootstrap-quality-gate.sh — TK-32-2-2 SonarQube Quality Gate 초기 설정
# =============================================================================
# 첫 부팅 후 1회 실행 — admin token 으로 Quality Gate 'Scheduling' 생성 + 기본 지정.
#
# 사용:
#   1. SonarQube 부팅 + admin 비밀번호 변경 (UI 또는 API)
#   2. SonarQube UI → My Account → Security → Generate Token
#   3. SONAR_TOKEN 환경 변수 설정 후 본 스크립트 실행:
#      export SONAR_URL=http://localhost:9001
#      export SONAR_TOKEN=<admin token>
#      ./bootstrap-quality-gate.sh
#
# 게이트 정의 (SAD §5.8):
#   - 신규 라인 커버리지 ≥ 80% (REQ DoD 표준)
#   - 신규 중복 ≤ 3%
#   - 신규 Critical 위반 = 0
#   - 신규 보안 핫스팟 review = 100%
# =============================================================================

set -euo pipefail

SONAR_URL="${SONAR_URL:-http://localhost:9001}"
SONAR_TOKEN="${SONAR_TOKEN:?SONAR_TOKEN 환경 변수 필수 (admin 발급 토큰)}"
GATE_NAME="${GATE_NAME:-Scheduling}"

curl_auth() {
    curl -sS -u "${SONAR_TOKEN}:" "$@"
}

log() { echo "[$(date '+%H:%M:%S')] $*"; }

# ---------------------------------------------------------------------------
# 1. 기존 게이트 확인 (idempotent)
# ---------------------------------------------------------------------------
EXISTING=$(curl_auth "${SONAR_URL}/api/qualitygates/list" | \
           grep -o "\"name\":\"${GATE_NAME}\"" || true)

if [[ -n "${EXISTING}" ]]; then
    log "Quality Gate '${GATE_NAME}' 이미 존재 — 건너뜀 (조건만 추가/확인)"
else
    log "Quality Gate '${GATE_NAME}' 생성"
    curl_auth -X POST "${SONAR_URL}/api/qualitygates/create" \
        --data-urlencode "name=${GATE_NAME}" >/dev/null
fi

# ---------------------------------------------------------------------------
# 2. 조건 추가 (중복 추가는 SonarQube 가 409 반환 — set -e 회피 위해 || true)
# ---------------------------------------------------------------------------
add_condition() {
    local metric="$1"
    local op="$2"
    local error="$3"
    log "  + 조건: ${metric} ${op} ${error}"
    curl_auth -X POST "${SONAR_URL}/api/qualitygates/create_condition" \
        --data-urlencode "gateName=${GATE_NAME}" \
        --data-urlencode "metric=${metric}" \
        --data-urlencode "op=${op}" \
        --data-urlencode "error=${error}" >/dev/null 2>&1 || \
        log "    (이미 존재 또는 충돌 — 무시)"
}

add_condition "new_coverage"                     "LT" "80"
add_condition "new_duplicated_lines_density"     "GT" "3"
add_condition "new_violations"                   "GT" "0"
add_condition "new_security_hotspots_reviewed"   "LT" "100"
add_condition "new_blocker_violations"           "GT" "0"

# ---------------------------------------------------------------------------
# 3. 기본 게이트로 설정
# ---------------------------------------------------------------------------
log "Quality Gate '${GATE_NAME}' 을 default 로 설정"
curl_auth -X POST "${SONAR_URL}/api/qualitygates/set_as_default" \
    --data-urlencode "name=${GATE_NAME}" >/dev/null

log "DONE — Quality Gate '${GATE_NAME}' 활성"
echo ""
echo "다음 단계:"
echo "  1. Jenkins → Manage Credentials → 'sonarqube-token' 등록"
echo "  2. CASC jenkins.yaml 의 sonarGlobalConfiguration 활성"
echo "  3. backend/frontend 첫 분석 — './gradlew sonar' / 'sonar-scanner'"
