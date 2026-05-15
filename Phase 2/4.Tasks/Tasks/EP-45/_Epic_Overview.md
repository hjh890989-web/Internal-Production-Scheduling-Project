# Epic Overview — [EP-45] 호환성·확장성 NFR (Compatibility)

**Sprint**: S0+S4+S5 | **Priority**: Must ⭐ | **SP**: 8 | **PD**: ~5.6 PD

## Epic 목적
> SRS §4.2.6 / REQ-NF-COM-001~005: 30 동시 사용자, 5년 데이터 (10M row), 엑셀 충실도, API 전방 호환, 브라우저 호환

## Story 목록

| Story | 제목 | SP | NFR |
|---|---|:--:|:--:|
| [ST-45-1](ST-45-1/_Story_Overview.md) | 30 동시 사용자 SLO | 2 | COM-001 |
| [ST-45-2](ST-45-2/_Story_Overview.md) | 5년 데이터 볼륨 (≤ 10M row) | 2 | COM-002 |
| [ST-45-3](ST-45-3/_Story_Overview.md) | 엑셀 역-Export 포맷 충실도 | — | COM-003 (EP-12 참조) |
| [ST-45-4](ST-45-4/_Story_Overview.md) | API 전방 호환성 | 2 | COM-004 |
| [ST-45-5](ST-45-5/_Story_Overview.md) | 브라우저 호환성 (Chromium 최신 2) | 2 | COM-005 |

## DoD
- [ ] k6 30 user 동시 부하 PASS
- [ ] 5년 시뮬레이션 + 파티션 정책
- [ ] OpenAPI 버저닝 정책
- [ ] Chrome·Edge 최신 2 호환 매트릭스

## References
- WBS §8.5 EP-45, SRS COM-001~005

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
