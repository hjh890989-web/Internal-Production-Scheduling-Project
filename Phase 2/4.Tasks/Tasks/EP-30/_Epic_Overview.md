# Epic Overview — [EP-30] 인증·인가 (Keycloak — ADR-012) ⭐

**Sprint**: S0~S1 분산 | **Priority**: Must ⭐⭐ (Phase 3 진입 게이트) | **SP**: 8 | **PD**: ~5.6 PD

---

## Epic 목적

> WBS §10 EP-30 인용: "Keycloak 24 컨테이너 + 사내 SSO 페더레이션 + RBAC + Spring Security 필터"
> SAD ADR-012 / SRS REQ-FUNC-CO-001·NFR-SEC-002·003: "사내 SSO 페더레이션 (SAML/OIDC) + Planner·STK·IT·Read-only RBAC"

본 Epic은 **Phase 3 진입 결정적 인프라**. NFR EP-42 (보안)·EP-44 (관측성)·EP-40 (성능)이 모두 본 Epic 선행 의존. Keycloak 24 단일 IdP — 사내 Active Directory·Entra ID 페더레이션 + local fallback.

**Why P1 Critical (Phase 3 진입 게이트)**:
- **NFR EP-42 SEC-002·003** — SSO + RBAC 직접 의존
- **EP-10 거버넌스** — Planner role 검증 의존
- **EP-EX14·EP-15** — WebSocket 인증 토큰 사용

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-30-1](ST-30-1/_Story_Overview.md) | Keycloak 24 컨테이너 + 사내 SSO 페더레이션 | 5 | ~3.5 | T-U + T-I + A | ☐ |
| [ST-30-2](ST-30-2/_Story_Overview.md) | RBAC + Spring Security 필터 | 3 | ~2.1 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **Keycloak 24 LTS 컨테이너** — Docker Compose 환경 통합
- [ ] **사내 SSO 페더레이션** — SAML 또는 OIDC (사내 IdP 정합)
- [ ] **Local fallback** — IdP 장애 시 ID/PW 로그인 가능
- [ ] **RBAC 4 role**: PLANNER·STK_USER·IT_OPS·READ_ONLY
- [ ] **Spring Security 필터** — JWT 검증 + 모든 endpoint @PreAuthorize
- [ ] **NFR-SEC-002·003 정합** — 침투 테스트 PASS
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §10 EP-30
- **SAD ADR-012**: Keycloak 24 채택
- **SRS REQ-FUNC**: REQ-FUNC-CO-001 (인증)
- **SRS REQ-NF**: REQ-NF-SEC-002 (SSO), REQ-NF-SEC-003 (RBAC), REQ-NF-SEC-007 (비밀번호 정책)
- **선행**: EP-00 (Docker Compose 인프라)
- **후행**: EP-10 (확정 게이트 PLANNER role 의존), EP-15·EX14 (WebSocket 인증), **EP-42 (보안 NFR)**, **EP-44 (운영 NFR)**

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §10 EP-30, ADR-012 + REQ-NF-SEC-002·003 |
