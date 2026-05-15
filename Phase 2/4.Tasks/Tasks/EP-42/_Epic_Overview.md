# Epic Overview — [EP-42] 보안 NFR (Security)

**Sprint**: S0+S1+S4+S5 | **Priority**: Must ⭐ | **SP 합계**: 13 | **PD 추정**: ~9.1 PD

## Epic 목적

> SRS §4.2.3 / REQ-NF-SEC-001~007: 사내망·SSO·RBAC·audit 불변성·DLP·TLS·비밀번호.

6 Story로 분해 — 보안 전 영역.

## Story 목록

| Story | 제목 | SP | NFR |
|---|---|:--:|:--:|
| [ST-42-1](ST-42-1/_Story_Overview.md) | 사내망 전용 + 방화벽 룰 | 2 | SEC-001 |
| [ST-42-2](ST-42-2/_Story_Overview.md) | SSO(SAML/OIDC) + 폴백 | 3 | SEC-002 |
| [ST-42-3](ST-42-3/_Story_Overview.md) | RBAC 전 API 강제 | 3 | SEC-003 |
| [ST-42-4](ST-42-4/_Story_Overview.md) | Audit 3년 보존·불변성 | 2 | SEC-004 |
| [ST-42-5](ST-42-5/_Story_Overview.md) | DLP·egress 필터 | 1 | SEC-005·006 |
| [ST-42-6](ST-42-6/_Story_Overview.md) | TLS 1.2+ + 비밀번호 정책 | 2 | SEC-007 |

## DoD
- [ ] 방화벽 룰셋 + egress 필터 + DLP
- [ ] Keycloak SAML/OIDC 페더레이션 + ID/PW 폴백
- [ ] 침투 테스트 (Burp Suite·OWASP ZAP)
- [ ] REVOKE UPDATE/DELETE on audit.*
- [ ] TLS 1.3 + HSTS + 비밀번호 12자/3종/5회

## References
- WBS §8.5 EP-42, SRS REQ-NF-SEC-001~007, ADR-012
- 선행: EP-30, EP-32

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
