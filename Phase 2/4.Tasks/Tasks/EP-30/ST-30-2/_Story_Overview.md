# Story Overview — [EP-30] ST-30-2 RBAC + Spring Security 필터

**Sprint**: S1 | **Epic**: EP-30 | **Priority**: Must ⭐⭐ | **SP**: 3 | **PD**: ~2.1 PD

---

## Story 목적

> WBS §10 EP-30 ST-30-2: "TK-30-2-1 Planner·Floor Supervisor·IT Operator·Read-only role, TK-30-2-2 RBAC 매트릭스, TK-30-2-3 403 처리"
> SRS REQ-NF-SEC-003: "RBAC 매트릭스를 모든 API에서 강제"

본 Story는 Spring Security 6.x + JWT (Keycloak) — 모든 controller method `@PreAuthorize` 강제. 403/401 사용자 친화 응답.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 |
|---|---|:--:|:--:|:--:|
| [TK-30-2-1](TK-30-2-1.md) | 4 role 매핑 (PLANNER·STK·IT_OPS·READ_ONLY) | 0.7 | Backend | T-U + T-I |
| [TK-30-2-2](TK-30-2-2.md) | RBAC 매트릭스 + Spring Security Config | 0.8 | Backend | T-U + T-I |
| [TK-30-2-3](TK-30-2-3.md) | 403/401 처리 + ProblemDetail | 0.6 | Backend | T-U + T-I + A |

> **선행**: [ST-30-1](../ST-30-1/_Story_Overview.md)
> **후행**: EP-42 (보안), 모든 controller (PLANNER·STK 분리)

---

## Story 레벨 DoD

- [ ] **4 role 매핑**: PLANNER (생산계획)·STK_USER (현장)·IT_OPS (IT)·READ_ONLY (감사·조회)
- [ ] **Spring Security Config** — `@EnableMethodSecurity` + JWT resource server
- [ ] **모든 controller method @PreAuthorize** 강제 (ArchUnit 규칙)
- [ ] **403/401 ProblemDetail** — 한국어 메시지 + redirect
- [ ] **RBAC 매트릭스 문서화** — 사용자 친화

---

## References

- **WBS**: §10 EP-30 ST-30-2
- **SRS REQ-NF**: SEC-003
- **SAD ADR-012**

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
