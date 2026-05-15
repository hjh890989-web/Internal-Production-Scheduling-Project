# Story Overview — [EP-21] ST-21-2 VC_HOSE_RULE 마스터 + 호기·앵글 상한

**Sprint**: S2 | **Epic**: EP-21 v1.4 신규 Must | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.2 EP-21 ST-21-2: "TK-21-2-1 `master.VC_HOSE_RULE` DDL (machine_pin·max_concurrent_slots·side_lock·lp_only), TK-21-2-2 마스터 → 테이블 마이그레이션 스크립트, TK-21-2-3 LISTEN/NOTIFY 캐시 무효화"
> SRS REQ-FUNC-VC-024 (BR-V14, 28422-08HA0 예시): "특정 품번은 지정된 호기에만 배치 가능 (`machine_pin`) + 동시 다중 슬롯 차단 (`max_concurrent_slots`) + 좌/우 락 (`side_lock`) + LP 전용 (`lp_only`) 규칙을 마스터 테이블로 표현."

본 Story는 ST-21-1의 K/L 컬럼과 별도로 **품번 단위 추가 룰**을 마스터화. K/L은 슬롯 셋팅 호환성, VC_HOSE_RULE은 품번 자체의 운영 정책(고정 호기·동시생산 상한 등). PostgreSQL LISTEN/NOTIFY로 캐시 동기 무효화 — IT_OPS가 마스터 갱신 시 즉시 RuleEngine 반영.

**Why Must**:
- 현장 28422-08HA0 운영 룰 (LP-01만, 동시 1슬롯) 코드화 — R-V08 (단일 호기 고장 시 백업) 대응 토대
- ST-21-3 (호기 핀 강제) 선행
- BR-V14 hard 제약

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-21-2-1](TK-21-2-1.md) | `master.VC_HOSE_RULE` DDL + Entity | 0.7 | Backend | T-U + I | ☐ |
| [TK-21-2-2](TK-21-2-2.md) | 마스터 → 테이블 마이그레이션 스크립트 | 0.6 | Backend | T-U + I | ☐ |
| [TK-21-2-3](TK-21-2-3.md) | LISTEN/NOTIFY 캐시 무효화 통합 | 0.8 | Backend | T-U + T-I | ☐ |

> **선행**: [EP-99 ST-99-1](../../EP-99/ST-99-1/_Story_Overview.md)
> **후행**: ST-21-3, ST-21-4

---

## Story 레벨 DoD

- [ ] `master.vc_hose_rule` 테이블 + 4 컬럼 (machine_pin·max_concurrent_slots·side_lock·lp_only)
- [ ] 47품번 운영 룰 마이그레이션 적용 (현재는 28422-08HA0·28422-2M800·28421-2M800 등 핵심)
- [ ] LISTEN/NOTIFY 트리거 + Caffeine 캐시 무효화 → ≤500ms 반영
- [ ] HoseRuleController IT_OPS POST/PUT/DELETE
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-21 ST-21-2
- **PDD-02 v1.1**: BR-V14
- **SRS REQ-FUNC**: REQ-FUNC-VC-024
- **SAD**: ADR-005 (LISTEN/NOTIFY 캐시 무효화 패턴)
- **선행**: EP-99 ST-99-1
- **후행**: ST-21-3, ST-21-4

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-21 ST-21-2 |
