# Story Overview — [EP-42] ST-42-6 TLS 1.2+ + 비밀번호 정책

**Sprint**: S0 | **Epic**: EP-42 | **SP**: 2

## Story 목적
> SRS SEC-007: "TLS 1.2+, 비밀번호 ≥ 12자 3종 클래스, 5회 실패 잠금"

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-42-6-1](TK-42-6-1.md) | NGINX TLS 1.3 | 0.6 |
| [TK-42-6-2](TK-42-6-2.md) | HSTS | 0.3 |
| [TK-42-6-3](TK-42-6-3.md) | 비밀번호 정책 (12자/3종/5회 잠금) | 0.5 |

## DoD
- [ ] TLS 1.3 + 1.2 fallback (legacy 호환)
- [ ] HSTS max-age=31536000
- [ ] Keycloak 비밀번호 정책 적용
- [ ] TLS 스캐너 (testssl.sh) PASS

## References
- WBS §8.5 EP-42 ST-42-6, SRS SEC-007

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
