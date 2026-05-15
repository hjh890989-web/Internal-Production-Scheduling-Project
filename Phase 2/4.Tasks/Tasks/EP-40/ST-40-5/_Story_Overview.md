# Story Overview — [EP-40] ST-40-5 전체 스케줄 on-demand 검사 ≤ 3초

**Sprint**: S2~S3 | **Epic**: EP-40 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

## Story 목적
> SRS REQ-NF-PER-007: "On-demand 제약 검사 p95 ≤ 3초"

EP-VC16 (on-demand 검사) 의 부하 회귀.

## 포함 Task 목록

| Task ID | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-40-5-1](TK-40-5-1.md) | 부하 시나리오 작성 (1주 호라이즌) | 0.7 | QA |
| [TK-40-5-2](TK-40-5-2.md) | 1주 호라이즌 p95 ≤ 3s 회귀 | 0.7 | QA + DevOps |

> 선행: [EP-VC16](../../EP-VC16/)

## DoD
- [ ] k6 시나리오 — 1주 호라이즌 (1,500 row)
- [ ] p95 ≤ 3,000ms (REQ-NF-PER-007)
- [ ] CI nightly 통합

## References
- WBS §8.5 EP-40 ST-40-5, SRS PER-007

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
