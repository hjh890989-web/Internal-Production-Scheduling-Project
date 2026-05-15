# Epic Overview — [EP-34] 횡단 공통 기능 (CO Requirements) ⭐

**Sprint**: S2~S4 분산 | **Priority**: Must ⭐⭐ (Phase 3 진입 게이트) | **SP**: 5 | **PD**: ~3.5 PD

---

## Epic 목적

> WBS §10 EP-34 인용: "마스터 dual-review (BR-X05) + MES 실적 수신·폴백 (BR-X06) + KST 시간 통일 (BR-X04)"
> SRS REQ-FUNC-CO-001~010 / BR-X04·X05·X06: "모든 공정·도메인에 공통 적용되는 규칙 — 시간·마스터·MES"

본 Epic은 **5 Sprint 횡단 공통 규칙**. 모든 기능 Epic이 본 Epic의 룰을 따라야 함. BR-X04 (KST) → 모든 timestamp · BR-X05 (dual-review) → 마스터 변경 · BR-X06 (MES 폴백) → 실적 수신.

**Why P1 Critical**:
- **EP-99 마스터 검증** → BR-X05 dual-review 의존
- **EP-41 ST-41-3 MES 장애 회복** → BR-X06 MES 폴백 의존
- **모든 audit·log** → BR-X04 KST 일관성 의존

---

## Story 목록

| Story | 제목 | SP | PD | 검증 |
|---|---|:--:|:--:|:--:|
| [ST-34-1](ST-34-1/_Story_Overview.md) | 마스터 dual-review (BR-X05) | 2 | ~1.4 | T-U + T-I + A |
| [ST-34-2](ST-34-2/_Story_Overview.md) | MES 실적 수신 + 장애 폴백 (BR-X06) | 2 | ~1.4 | T-U + T-I |
| [ST-34-3](ST-34-3/_Story_Overview.md) | KST 시간 기준 통일 (BR-X04) | 1 | ~0.7 | T-U + ArchUnit |

---

## Epic 레벨 DoD

- [ ] **`master_change_request` 테이블** — pending·approved·rejected
- [ ] **2명 승인자 검증** — 동일 actor 거부 (BR-X05)
- [ ] **MES 실적 수신 API** — 회전·shift 단위
- [ ] **1 shift 미수신 폴백** — 임시 카운트 (EP-41 ST-41-3 정합)
- [ ] **모든 timestamp KST** — ArchUnit 규칙 + DB column 검증
- [ ] **REQ-FUNC-CO-001~010** 정합

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §10 EP-34
- **SRS REQ-FUNC**: REQ-FUNC-CO-001~010
- **BR**: BR-X04 (KST), BR-X05 (dual-review), BR-X06 (MES 폴백)
- **선행**: EP-00, EP-11 (audit)
- **후행**: EP-99 (BR-X05 활용), EP-41 ST-41-3 (BR-X06 활용)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §10 EP-34 + BR-X04·X05·X06 |
