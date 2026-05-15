# Epic Overview — [EP-14] 신규 라인 우선 라우팅 (S-02)

**Sprint**: S4 | **Priority**: Must ⭐ | **SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Epic 목적

> WBS §7 EP-14 인용: "ST-14-1 신규 우선 → 포드 폴백 라우팅. TK-14-1-1 라우팅 정책 (신규 90%↑), TK-14-1-2 포드 전용 품번 차단, TK-14-1-3 라인 capa accounting"
> SRS REQ-FUNC-EX-008·009 / BR-E08: "압출 신규 라인 사용률 90% 이상. 포드 라인은 신규 capa 부족 시에만 fallback. 포드 전용 품번 (호환성 제약)은 신규 라인 시도 0건."

본 Epic은 압출 라우팅의 **2-tier 정책** 정형화. 신규 라인 (EX-A·B·C)은 빠르고 효율 높음 → 우선 사용. 포드 라인 (EX-FORD)은 노후 라인이지만 일부 품번 호환성 제약으로 유지. 정상 케이스 90%+ 신규 라우팅, 포드는 capa 부족 시에만 fallback. 포드 전용 품번은 신규 시도 자체 차단 (불량 risk).

**Why Sprint 4 핵심**:
- **BR-E08** — 라인 가동 정책 정형화
- **NS-S09** 신규 라인 사용률 KPI 90%+
- **S-02 Sprint Goal** — 라우팅 정확성

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-14-1](ST-14-1/_Story_Overview.md) | 신규 우선 → 포드 폴백 라우팅 | 3 | ~2.1 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **`LineRoutingPolicy`** — 신규 우선 + 포드 fallback
- [ ] **`master.line_type`** — 신규/포드 분류 + 호환 품번
- [ ] **포드 전용 품번** RuleEngine 차단 (오라우팅 0건)
- [ ] **라인 capa accounting** — shift 단위 누계
- [ ] **회귀 1주 호라이즌**: 신규 라인 사용률 ≥ 90%
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-14
- **PDD-03**: S-02 라우팅
- **SRS REQ-FUNC**: REQ-FUNC-EX-008·009
- **BR**: BR-E08
- **SRS REQ-NF-KPI**: NS-S09 (신규 라인 사용률)
- **TestPlan**: TC-EX-008·009
- **선행**: EP-09 (셋팅 그룹핑)
- **후행**: 없음 (Sprint 4 마무리)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
