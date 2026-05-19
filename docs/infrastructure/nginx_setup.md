# NGINX 1.25 운영 가이드

본 문서는 TK-00-1-3 산출물 (`infrastructure/nginx/`) 운영 안내.

## 1. DEV 인증서 생성 (최초 1회)

```bash
# Git Bash / WSL
bash infrastructure/nginx/scripts/generate-dev-cert.sh
```

산출물:
- `infrastructure/nginx/ssl/dev-self-signed.crt` (1년 유효)
- `infrastructure/nginx/ssl/dev-self-signed.key` (0600)

⚠ 인증서 파일은 `.gitignore` 처리됨. 각 dev 환경마다 생성.

## 2. 부팅·중지

```powershell
cd infrastructure
docker compose up nginx -d
docker compose ps              # (healthy) ≤5초
docker compose down
```

> ⚠ Backend 미부팅 상태로 NGINX 단독 시작 가능 (`depends_on` 주석 처리됨).
> Backend 까지 통합은 TK-00-1-4 (Sprint 0) 에서 활성.

## 3. 검증

```powershell
# Healthcheck (평문)
curl http://localhost/health                # → OK

# HTTPS Healthcheck (자체 서명 — -k 필수)
curl -k https://localhost/health            # → HTTP/2 200 + OK

# 보안 헤더 (HSTS·X-Frame·XSS)
curl -k -I https://localhost/

# HTTP → HTTPS 리다이렉트
curl -I http://localhost/api/test           # → 301

# gzip 활성
curl -H "Accept-Encoding: gzip" -kI https://localhost/  # → Content-Encoding: gzip
```

## 4. WebSocket Upgrade 검증

```powershell
# wscat 설치: npm install -g wscat
wscat -c wss://localhost/ws/test -n         # → 101 Switching Protocols
```

Backend 미부팅 시 wscat 은 connect 후 BE upstream 응답 못 받아 502/끊김. 정상 동작은 TK-00-1-4 에서.

## 5. TLS 스캐너 (NFR-SEC-006 — TLS 1.2+ 만)

```powershell
# nmap 사용 (1.0/1.1 비활성 확인)
nmap --script ssl-enum-ciphers -p 443 localhost

# 또는 testssl.sh (Linux/WSL)
testssl.sh https://localhost
```

## 6. 인증서 교체 (STG/PROD)

DEV 자체 서명 → 사내 CA 발급 인증서로 교체:

1. 사내 CA 에 CSR 제출:
   ```bash
   openssl req -new -newkey rsa:4096 -nodes \
       -keyout scheduling.key -out scheduling.csr \
       -subj "/C=KR/ST=Gyeonggi/L=Hwaseong/O=Internal/CN=scheduling.internal.local"
   ```
2. 사내 CA 발급 받은 `scheduling.crt` 와 `scheduling.key` 를 `infrastructure/nginx/ssl/` 에 배치
3. `infrastructure/nginx/conf.d/scheduling.conf` 의 `ssl_certificate` 경로 수정
4. `docker compose restart nginx`
5. 만료 30일 전 Slack 자동 알림 (EP-44 ST-44-3)

## 7. 운영 환경 차이

| 항목 | DEV | STG/PROD |
|---|---|---|
| ports | `127.0.0.1:80, 127.0.0.1:443` | `0.0.0.0:80, 0.0.0.0:443` + 사내 IP whitelist |
| 인증서 | 자체 서명 (1년) | 사내 CA (1년, 갱신 알림) |
| `client_max_body_size` | 20MB (Excel 업로드) | 동일 |
| `add_header HSTS` | 1년 | 1년 + `preload` (사내 HSTS preload 시) |

## 8. 트러블슈팅

| 증상 | 원인 | 대처 |
|---|---|---|
| 부팅 실패 — `cannot load certificate` | `ssl/*.crt` 누락 | `generate-dev-cert.sh` 재실행 |
| 502 Bad Gateway | backend upstream 미부팅 | `docker compose up backend -d` (TK-00-1-4 이후) |
| WebSocket 1분 후 끊김 | `proxy_read_timeout 60s` 기본값 | `scheduling.conf` 의 `proxy_read_timeout 3600s` 확인 |
| 브라우저 "안전하지 않은 사이트" 경고 | DEV 자체 서명 | 정상 — "고급 → 안전하지 않은 사이트로 이동" |
| SPA 새로고침 404 | `try_files` 누락 | `scheduling.conf` 의 `try_files $uri $uri/ /index.html` 확인 |

## 9. 보안 체크리스트

- [ ] TLS 1.2/1.3 만 활성 (1.0/1.1 차단) — `ssl_protocols`
- [ ] HSTS 1년 — `Strict-Transport-Security` header
- [ ] Clickjacking 차단 — `X-Frame-Options: DENY`
- [ ] MIME sniffing 차단 — `X-Content-Type-Options: nosniff`
- [ ] DEV 포트 `127.0.0.1` 바인딩 (사내망 외 차단)
- [ ] CVE 추적 — `nginx:1.25-alpine` 정기 (월간) 이미지 업데이트
