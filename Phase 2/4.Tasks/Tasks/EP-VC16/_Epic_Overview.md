# Epic Overview — [EP-VC16] On-Demand 전체 스케줄 검사

**Sprint**: S2~S3 (성형 핵심 ~ 압출 핵심) | **Priority**: Must ⭐ | **SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Epic 목적

> WBS §5.2 EP-VC16 인용: "On-demand 전체 스케줄 제약 검사를 3초 이내 모든 위반과 함께 반환해야 한다."
> SRS REQ-FUNC-VC-016: "사용자가 임의 시점에 현재 확정된 스케줄 전체의 제약 위반을 점검할 수 있는 API. p95 ≤ 3초."

본 Epic은 EP-VC15가 *생성 시점* 충돌이라면, 본 Epic은 *기 확정 스케줄의 후행 점검*. 마스터 변경·운영 이벤트 발생 후 기존 스케줄의 신규 위반 발견. UI "검사 실행" 버튼 데이터 소스.

**Why Must**:
- 마스터 변경 (휴일 추가·VC_HOSE_RULE 갱신 등) 시 기존 스케줄 retroactive 위반 발견 핵심
- ST-VC15와 페어 — 한쪽은 생성, 다른 쪽은 후행 검증
- v1.2 명시화(REV-D-003)

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-VC16-1](ST-VC16-1/_Story_Overview.md) | 전체 스케줄 제약 검사 API ≤ 3초 p95 | 2 | ~1.4 | T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] `/api/v1/schedule/validate-all` 엔드포인트 — 1주 호라이즌 전체 스케줄 점검
- [ ] **모든 Rule 평가** — EP-04·05·06·21 룰 통합 (RulePipeline 재사용)
- [ ] **p95 ≤ 3초** (1주 호라이즌 부하)
- [ ] 결과: `{ violations: [...], summary: {...}, executedAt: ... }`
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-VC16
- **SRS REQ-FUNC**: REQ-FUNC-VC-016
- **TestPlan**: TC-VC-016
- **선행**: EP-05 (스케줄 확정), EP-21 (RulePipeline)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-VC16 + REQ-FUNC-VC-016 |
