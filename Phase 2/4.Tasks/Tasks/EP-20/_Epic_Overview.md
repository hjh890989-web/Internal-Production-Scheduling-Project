# Epic Overview — [EP-20] 영업 폴더 watch 자동 송신 (C-03)

**Sprint**: S5 | **Priority**: Could | **SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Epic 목적

> WBS §8 EP-20 인용: "ST-20-1 watchdog 폴더 ingest (60초 큐)"
> SRS REQ-FUNC-OC-015·XT-003: "영업 공유 폴더에 .xlsx 파일 drop → 60초 SLA 내 자동 ingest → 수주 import queue."

본 Epic은 EP-01 Parser의 자동화 (사용자가 폴더에 파일 drop만으로 수주 등록). file system watcher + 60초 SLA. audit + Slack 통보. 운영 친화 — 영업팀이 별도 UI 학습 불필요.

**Why Sprint 5 Could**:
- 운영 효율 — 영업 ↔ 생산 자동 연결
- C-03 (Could) 라벨 — 정상 운영은 수동 import도 OK
- Phase 2+ 분기별 검토

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-20-1](ST-20-1/_Story_Overview.md) | watchdog 폴더 ingest (60초 큐) | 2 | ~1.4 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **`FolderWatchService`** — Spring `Path WatchService` 또는 `commons-io FileAlterationMonitor`
- [ ] **fs close 이벤트 감지** — 파일 쓰기 완료 후 처리 (partial write 회피)
- [ ] **큐 등록 + audit** — `import_queue` 테이블 (id·filePath·status·queuedAt)
- [ ] **60초 SLA**: 파일 drop → 큐 등록 ≤ 60초
- [ ] **EP-01 Parser와 연결** — 큐 row → `MasterImportService.importAll()`
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §8 EP-20
- **PDD**: C-03
- **SRS REQ-FUNC**: REQ-FUNC-OC-015, REQ-FUNC-XT-003
- **TestPlan**: TC-XT-003
- **선행**: EP-01

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
