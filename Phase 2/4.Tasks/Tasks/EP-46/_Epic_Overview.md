# Epic Overview — [EP-46] 비용 NFR (Cost)

**Sprint**: S0+S5 | **Priority**: Should | **SP**: 4 | **PD**: ~2.8 PD

## Epic 목적
> SRS §4.2.7 / REQ-NF-COS-001~003: 잉여 서버 우선, OSS 우선·SBOM, 평상 운영 ≤ 0.5 FTE

## Story 목록

| Story | 제목 | SP | NFR |
|---|---|:--:|:--:|
| [ST-46-1](ST-46-1/_Story_Overview.md) | 잉여 서버 인벤토리 확인 | 1 | COS-001 |
| [ST-46-2](ST-46-2/_Story_Overview.md) | OSS 우선 + SBOM | 2 | COS-002 |
| [ST-46-3](ST-46-3/_Story_Overview.md) | 평상 운영 ≤ 0.5 FTE | 1 | COS-003 |

## DoD
- [ ] 잉여 서버 사양 검증 (≥ 8 vCPU·32GB·500GB SSD)
- [ ] Syft SBOM 자동 생성 + 라이선스 검토
- [ ] 운영 런북·배포·백업·복원 자동화

## References
- WBS §8.5 EP-46, SRS COS-001~003

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
