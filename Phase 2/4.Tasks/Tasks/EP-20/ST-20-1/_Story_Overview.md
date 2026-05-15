# Story Overview — [EP-20] ST-20-1 watchdog 폴더 ingest

**Sprint**: S5 | **Epic**: EP-20 영업 폴더 watch | **Priority**: Could
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-20-1-1](TK-20-1-1.md) | fs close 이벤트 감지 (`FolderWatchService`) | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-20-1-2](TK-20-1-2.md) | 큐 등록 + audit | 0.5 | Backend | T-U + T-I | ☐ |
| [TK-20-1-3](TK-20-1-3.md) | 60초 SLA 회귀 + 통합 | 0.4 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-01](../../EP-01/)
> **후행**: 없음

---

## Story 레벨 DoD

- [ ] **`FolderWatchService`** — 영업 공유 폴더 감시 (config 경로)
- [ ] **fs close 감지** — 쓰기 완료 후 트리거 (Linux inotify CLOSE_WRITE)
- [ ] **`import_queue` 테이블** — file_path·status·queued_at·imported_at
- [ ] **큐 등록 ≤ 60초** SLA (TC-XT-003)
- [ ] **EP-01 Parser 연결** — 큐 row → import + audit

---

## References

- **WBS**: §8 EP-20 ST-20-1
- **SRS REQ-FUNC**: OC-015, XT-003

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
