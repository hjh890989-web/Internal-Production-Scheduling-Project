# Story Overview — [EP-21] ST-21-5 규격<7 가류기당 앵글 ≤4 (cross-master)

**Sprint**: S2 | **Epic**: EP-21 v1.4 신규 Must | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §5.2 EP-21 ST-21-5: "TK-21-5-1 `v_product_with_spec` VIEW 생성, TK-21-5-2 Caffeine 캐시 + EX_CONSTRAINT LISTEN/NOTIFY 무효화, TK-21-5-3 RuleEngine `spec_lt7_cap` 함수, TK-21-5-4 회귀 (규격<7 품번 가류기당 ≤4 위반 0건)"
> SRS REQ-FUNC-VC-027 (BR-V17, ADR-017): "압출공정_제약조건.xlsx B열 규격값 < 7인 품번은 가류기 1대당 동시 점유 앵글 ≤ 4. cross-master (VC + EX) VIEW로 데이터 결합."

본 Story는 **cross-master 제약** — 압출 마스터(B열 규격)와 성형 마스터를 PostgreSQL VIEW로 결합해 RuleEngine이 단일 인터페이스로 조회. ADR-017의 VIEW + 캐시 패턴 적용. 두 마스터 중 어느 쪽이 변경되어도 캐시 무효화.

**Why Must**:
- 규격<7 품번은 가류기 1대 4앵글 초과 시 불량 발생 (현장 누적 경험)
- ADR-017 cross-master 패턴의 첫 적용 — 향후 다른 cross-master 제약 (BR-V19 등)에 재사용 토대
- BR-V17 hard 제약

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-21-5-1](TK-21-5-1.md) | `v_product_with_spec` VIEW 생성 | 0.5 | Backend | T-U + I | ☐ |
| [TK-21-5-2](TK-21-5-2.md) | Caffeine 캐시 + EX_CONSTRAINT LISTEN/NOTIFY 무효화 | 0.6 | Backend | T-U + T-I | ☐ |
| [TK-21-5-3](TK-21-5-3.md) | RuleEngine `spec_lt7_cap` 함수 | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-21-5-4](TK-21-5-4.md) | 회귀 (규격<7 품번 가류기당 ≤4 위반 0건) | 0.5 | QA + Backend | T-I + A | ☐ |

> **선행**: [ST-99-2](../../EP-99/ST-99-2/_Story_Overview.md) (B열 정합성), [ST-21-2](../ST-21-2/_Story_Overview.md) (LISTEN/NOTIFY 패턴)
> **후행**: 없음 (Sprint 2 마감)

---

## Story 레벨 DoD

- [ ] `master.v_product_with_spec` VIEW — VC + EX 마스터 JOIN
- [ ] `ProductSpecCache` Caffeine 1h expireAfterWrite + LISTEN/NOTIFY 무효화
- [ ] `SpecLt7CapRule` — 규격<7 품번 → 가류기당 점유 앵글 ≤ 4
- [ ] 회귀 100건 위반 0건 (TC-VC-027)
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-21 ST-21-5
- **PDD-02 v1.1**: BR-V17
- **SRS REQ-FUNC**: REQ-FUNC-VC-027
- **SAD ADR-017**: cross-master VIEW + 캐시 패턴
- **TestPlan**: TC-VC-027
- **REF-Master**: `Phase 1/2.Raw Materials/Extrusion/압출공정_제약조건.xlsx` (B열 규격)
- **선행**: ST-99-2, ST-21-2

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-21 ST-21-5 |
