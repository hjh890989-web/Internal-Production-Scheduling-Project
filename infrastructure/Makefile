# =============================================================================
# infrastructure/Makefile — 자주 쓰는 docker compose 명령 wrapper
# =============================================================================
# 사용 (Git Bash / WSL / make 설치된 PowerShell):
#   cd infrastructure
#   make cert        # DEV TLS 인증서 생성 (최초 1회)
#   make up          # 5개 컨테이너 백그라운드 부팅
#   make status      # 5개 healthcheck 한눈에
#   make logs        # 전체 로그 follow
#   make down        # 종료 (volume 보존)
#   make clean       # 완전 초기화 (volume 삭제 — ⚠ 데이터 손실)
#   make reset       # clean + cert + up (전체 리셋)
# =============================================================================

.PHONY: up down logs ps restart clean status reset cert validate

# 환경변수 .env 자동 로드
up:
	docker compose --env-file .env up -d --build

down:
	docker compose down

logs:
	docker compose logs -f --tail=100

ps:
	docker compose ps

restart:
	docker compose restart

clean:
	docker compose down -v

status:
	@echo "=== Container Status ==="
	@docker compose ps
	@echo ""
	@echo "=== Healthcheck Detail ==="
	@for c in scheduling-postgres scheduling-redis scheduling-backend scheduling-frontend scheduling-nginx; do \
		echo "$$c: $$(docker inspect --format='{{.State.Health.Status}}' $$c 2>/dev/null || echo 'not running')"; \
	done

reset: clean cert up

cert:
	bash nginx/scripts/generate-dev-cert.sh

validate:
	bash ../tools/infra_validation/validate_compose.sh
