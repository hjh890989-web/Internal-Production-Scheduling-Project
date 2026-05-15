# Story Overview — [EP-42] ST-42-1 사내망 전용 + 방화벽 룰

**Sprint**: S0+S5 | **Epic**: EP-42 | **SP**: 2

## Story 목적
> SRS SEC-001: "사내망에서만 접근, 외부 노출 금지"

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-42-1-1](TK-42-1-1.md) | 방화벽 룰셋 (NGINX whitelist) | 0.8 |
| [TK-42-1-2](TK-42-1-2.md) | Egress 필터 (외부 도메인 차단) | 0.7 |
| [TK-42-1-3](TK-42-1-3.md) | 방화벽 감사 (Burp Suite scan) | 0.5 |

## DoD
- [ ] NGINX IP whitelist (사내 CIDR)
- [ ] Egress 필터 (DLP)
- [ ] Burp Suite 외부 노출 zero 검증

## References
- WBS §8.5 EP-42 ST-42-1, SRS SEC-001

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
