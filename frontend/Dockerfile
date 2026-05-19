# =============================================================================
# Frontend Dockerfile — multi-stage (Node builder → NGINX serve)
# =============================================================================
# TK-00-3-1 — npm run build 산출물(dist/)을 NGINX 정적 파일로 서빙.
# 본 이미지는 단독으로도 동작 가능 (NGINX 내장). 통합 환경에서는
# infrastructure/docker-compose.yml 의 `frontend-static` 볼륨에 dist 만 복사
# (TK-00-1-4 통합 시 docker-compose override).
# =============================================================================

# ---------- Builder ----------
FROM node:20-alpine AS builder
WORKDIR /workspace
COPY package.json package-lock.json* ./
RUN npm ci || npm install --no-audit --no-fund
COPY . .
RUN npm run build

# ---------- Runtime (NGINX alpine — 정적 파일 serve) ----------
FROM nginx:1.25-alpine
COPY --from=builder /workspace/dist /usr/share/nginx/html
# Healthcheck 는 infrastructure/nginx 서비스가 담당 (TK-00-1-3).
