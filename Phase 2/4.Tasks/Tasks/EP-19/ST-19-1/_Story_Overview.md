# Story Overview — [EP-19] ST-19-1 timestamp 선택 복원

**Sprint**: S5 | **Epic**: EP-19 마스터 복원 UI | **Priority**: Could
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-19-1-1](TK-19-1-1.md) | audit 기반 복원 쿼리 (`MasterTimeTravelService`) | 0.8 | Backend | T-U + T-I | ☐ |
| [TK-19-1-2](TK-19-1-2.md) | UI 시점 슬라이더 + 미리보기 | 0.7 | Frontend | T-U + T-I | ☐ |
| [TK-19-1-3](TK-19-1-3.md) | 5년 부하 테스트 (5초 이내) | 0.6 | QA + Backend | T-P + A | ☐ |

> **선행**: [EP-11](../../EP-11/)
> **후행**: 없음

---

## Story 레벨 DoD

- [ ] **`MasterTimeTravelService.snapshotAt(table, timestamp)`** — JSONB 역재생
- [ ] **`GET /api/v1/master/snapshot`** 인증 사용자 read-only
- [ ] **`POST /api/v1/master/restore`** Master Admin only (위험)
- [ ] **UI 시점 슬라이더** + 1주/1개월/1년 zoom
- [ ] **복원 미리보기** — diff 표시 (변경/추가/삭제 row)
- [ ] **5년 부하 (10^6+ audit row) p95 ≤ 5초**

---

## References

- **WBS**: §8 EP-19 ST-19-1
- **SRS REQ-FUNC**: OC-014, XT-002

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
