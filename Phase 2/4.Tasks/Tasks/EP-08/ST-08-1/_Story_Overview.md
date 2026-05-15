# Story Overview — [EP-08] ST-08-1 4-shift 모델 + 75% 효율

**Sprint**: S3 | **Epic**: EP-08 압출 수식 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §6 EP-08 ST-08-1: "TK-08-1-1 shift 정의 마스터(`/api/v1/master/shifts`), TK-08-1-2 효율 75% 적용 (주간전반 = 180 min), TK-08-1-3 단위 테스트"
> SRS REQ-FUNC-EX-003·004 / BR-E03·E04: "4 shift (주간전반·후반·야간전반·후반), 1 shift 240 min × 75% 효율 = 180 min effective."

본 Story는 압출 시간 단위의 기준을 마스터로 외재화. shift 정의를 코드 상수가 아닌 DB 마스터로 두어 효율 조정·shift 시간 변경 시 즉시 반영. yield 수식 (ST-08-2)이 `effective_min` 컬럼을 직접 참조.

**Why Must**:
- 효율 75%는 BR-E04 hard 상수 — 향후 라인별·계절별 차별화 시 마스터 갱신만으로 적용 (코드 변경 없이)
- shift 추가/제거 시 코드 재배포 없이 운영 가능 (CON-02 빈도 갱신 가능성 대비)
- ST-08-2 yield 수식의 `min` 입력원

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-08-1-1](TK-08-1-1.md) | Shift 마스터 + `/api/v1/master/shifts` 엔드포인트 | 0.8 | Backend | T-U + T-I | ☐ |
| [TK-08-1-2](TK-08-1-2.md) | 효율 75% 적용 (`effective_min = nominal_min × efficiency`) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-08-1-3](TK-08-1-3.md) | 단위 테스트 (4-shift × 효율 매트릭스 ≥ 10 케이스) | 0.6 | Backend + QA | T-U | ☐ |

> **선행**: [EP-07 ST-07-1](../../EP-07/ST-07-1/_Story_Overview.md)
> **후행**: ST-08-2, ST-08-3, ST-09-1

---

## Story 레벨 DoD

- [ ] `master.shift` 테이블 + 4 row seed (주간전반·후반·야간전반·후반)
- [ ] **shift schema**: `shift_code`, `name`, `start_time`, `end_time`, `nominal_min`, `efficiency`, `effective_min`
- [ ] **효율 75%** — `nominal_min = 240, efficiency = 0.75, effective_min = 180` 자동 계산 (Generated column)
- [ ] **`/api/v1/master/shifts`** GET (인증) / POST·PUT·DELETE (IT_OPS)
- [ ] LISTEN/NOTIFY 캐시 무효화 (TK-21-2-3 패턴 재사용)
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-08 ST-08-1
- **PDD-03**: M-08 §4 A1 T1.1
- **SRS REQ-FUNC**: REQ-FUNC-EX-003·004
- **BR**: BR-E03 (4-shift), BR-E04 (75% 효율)
- **TestPlan**: TC-EX-003·004

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-08 ST-08-1 |
