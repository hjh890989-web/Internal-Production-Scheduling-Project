# Story Overview — [EP-30] ST-30-1 Keycloak 24 컨테이너 + 사내 SSO 페더레이션

**Sprint**: S0 | **Epic**: EP-30 | **Priority**: Must ⭐⭐ | **SP**: 5 | **PD**: ~3.5 PD

---

## Story 목적

> WBS §10 EP-30 ST-30-1: "TK-30-1-1 Keycloak 컨테이너, TK-30-1-2 SAML/OIDC 페더레이션, TK-30-1-3 local fallback"
> SAD ADR-012 / SRS REQ-NF-SEC-002: "Keycloak 24 LTS — 사내 SSO 페더레이션 + ID/PW fallback"

본 Story는 인증 인프라의 기반. Docker Compose 통합 + 사내 IdP (AD FS·Entra ID·Okta) 페더레이션 + JWT 발급.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 |
|---|---|:--:|:--:|:--:|
| [TK-30-1-1](TK-30-1-1.md) | Keycloak 24 컨테이너 (Docker Compose 통합) | 1.0 | DevOps | T-I |
| [TK-30-1-2](TK-30-1-2.md) | SAML/OIDC 사내 IdP 페더레이션 | 1.5 | DevOps + Backend | T-U + T-I |
| [TK-30-1-3](TK-30-1-3.md) | Local fallback (IdP 장애 시) | 1.0 | DevOps | T-I + A |

> **선행**: [EP-00 ST-00-1](../../EP-00/ST-00-1/_Story_Overview.md) (Docker Compose)
> **후행**: ST-30-2, EP-42 (보안 NFR)

---

## Story 레벨 DoD

- [ ] **Keycloak 24 LTS** 컨테이너 — `docker-compose.yml`에 통합
- [ ] **realm `scheduling-system`** + client `scheduling-app` + 4 role 사전 설정
- [ ] **사내 SSO 페더레이션** — SAML 또는 OIDC (Phase 0 사내 IdP 확정 후)
- [ ] **Local fallback** — IdP 장애 시 manual ID/PW 로그인 (REQ-NF-SEC-002)
- [ ] **JWT issuance** + Spring Security resource server 검증
- [ ] **Health check** `/health/ready` `/health/live`
- [ ] 통합 테스트 ≥ 80%

---

## References

- **WBS**: §10 EP-30 ST-30-1
- **SAD**: ADR-012
- **SRS REQ-NF**: SEC-002

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
