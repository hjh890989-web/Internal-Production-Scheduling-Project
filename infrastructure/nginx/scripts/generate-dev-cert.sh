#!/usr/bin/env bash
# =============================================================================
# generate-dev-cert.sh — DEV 환경 전용 자체 서명 TLS 인증서 생성
# =============================================================================
# 산출물:
#   infrastructure/nginx/ssl/dev-self-signed.crt  (1년 유효)
#   infrastructure/nginx/ssl/dev-self-signed.key  (0600)
#
# 운영 (STG/PROD): 사내 CA 발급 인증서로 교체.
#   교체 절차는 docs/infrastructure/nginx_setup.md 참조.
#
# DEV 브라우저 경고: "고급 → 안전하지 않은 사이트로 이동" — DEV 환경 한정 예외.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SSL_DIR="$SCRIPT_DIR/../ssl"

mkdir -p "$SSL_DIR"

openssl req -x509 -nodes -days 365 -newkey rsa:4096 \
    -keyout "$SSL_DIR/dev-self-signed.key" \
    -out    "$SSL_DIR/dev-self-signed.crt" \
    -subj   "/C=KR/ST=Local/L=Dev/O=Internal-Production-Scheduling/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,DNS:scheduling.local,IP:127.0.0.1"

chmod 600 "$SSL_DIR/dev-self-signed.key"
chmod 644 "$SSL_DIR/dev-self-signed.crt"

echo ""
echo "✅ DEV 인증서 생성 완료 (1년 유효)"
echo "   key: $SSL_DIR/dev-self-signed.key (0600)"
echo "   crt: $SSL_DIR/dev-self-signed.crt"
echo ""
echo "⚠️  STG/PROD 는 사내 CA 발급 인증서로 교체할 것."
echo "    절차: docs/infrastructure/nginx_setup.md §인증서-교체"
