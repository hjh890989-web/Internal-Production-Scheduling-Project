# 베타 시나리오 4 — 임의 시점 마스터 복원 (forensic + 잠재 복원)

**시나리오 ID**: BS-04 | **페르소나**: IT_OPS + Planner | **소요 시간**: ~10분
**관련 BR**: REQ-FUNC-OC-014 (5초 이내) + NFR-SEC-004 (3년 보존 + immutable)

> 잘못된 vc_constraint 입력 → audit timeline 으로 변경 history 추적 → 특정 시점 row
> 상태 forensic 조회 → (필요 시 별도 confirm 흐름으로 복원).
> **Phase 4-D 의 DR 검증 시나리오와 분리** (본 시나리오는 application-level forensic).

---

## 1. 사전 조건

- [x] STG 안정 가동 — audit.schedule_audit_log 누적 record (~1주 운영)
- [x] V030 월별 RANGE 파티셔닝 활성 (2026-06 partition)
- [x] IT_OPS 또는 PLANNER role

---

## 2. 사전 입력 — 잘못된 마스터 변경 (재현용)

```sql
-- IT_OPS 권한으로 의도적 잘못된 변경 (재현 시)
UPDATE master.vc_constraint
SET composite_count = 9
WHERE hose_id = '29673-2R060';
-- 결과: BR-V14 위반 (composite_count IN (1,2,3,6) CHECK 통과 → application invariant 위반)
-- 다음 VC 스케줄 생성 시 yield 계산 오류
```

---

## 3. UI 복원 절차 — IT_OPS

```
1. /audit/restore 진입 → MasterRestorePage
2. 위험 방지 Alert 확인 ("audit forensic 조회 — 실제 복원은 별도 confirm 흐름")
3. 위젯 3종 설정:
   - Table select: vc_schedule / ex_schedule_candidate / order (master 변경은 별도 audit)
   - row PK 입력: UUID 또는 hose_id
   - DatePicker showTime: 잘못된 변경 직전 시점 (예: 2026-06-10 14:00:00)
4. snapshot Card 자동 표시:
   - rowExisted: true/false tag
   - lastAction: INSERT/UPDATE/DELETE tag
   - capturedAt: ISO 시각
   - JSON payload pre 태그 (해당 시점 row 상태)
5. timeline Divider — 전체 audit history Ascending Timeline 컴포넌트
   - INSERT (green) + UPDATE (blue) + DELETE (red)
   - 각 entry: action + actor + reason + occurred_at
```

---

## 4. 복원 결정 — Planner + IT_OPS 협의

본 시나리오 UI 는 **forensic 조회만** 제공. 실제 row 복원은:

### 4.1 RBAC dual-review (BR-X05)

- IT_OPS — audit timeline 분석 + Slack alert 발송
- Planner — 복원 적용 결정 (별도 confirm 흐름, Sprint 7+ UI)

### 4.2 복원 SQL (현재 — IT_OPS 수동, Phase 4-D PITR 시나리오와 분리)

```sql
-- snapshot UI 의 JSON payload 를 그대로 UPDATE
UPDATE master.vc_constraint
SET composite_count = 2  -- snapshot 의 정상 값
WHERE hose_id = '29673-2R060';
-- 본 UPDATE 도 audit 자동 발행 (V025 trigger) — actor='it_ops_사번' + reason
```

---

## 5. 기대 결과 + 검증

| 항목 | 기대 | 검증 |
|---|---|---|
| snapshot p95 | ≤ 100ms (단일 SQL + index) | Grafana scheduling-overview API panel |
| timeline 정확성 | INSERT → UPDATE → UPDATE | timeline 시간순 ASC + reason 일치 |
| audit immutability | UPDATE/DELETE/TRUNCATE 시도 → reject | V026 trigger |
| 복원 후 audit row | 새 UPDATE row 발행 (BR-X02) | `SELECT * FROM audit.schedule_audit_log ORDER BY audit_id DESC` |
| 3년 보존 | partition 2026-06~2029-06 활성 | V030 파티션 검색 plan |

---

## 6. 실패 시 대처

| 증상 | 원인 | 대처 |
|---|---|---|
| snapshot rowExisted=false (해당 시점에 row 없음) | timestamp 너무 과거 | DatePicker 시각 조정 |
| timeline empty | row_pk 오타 | UUID 또는 hose_id 정확 입력 |
| audit_log UPDATE 시도 → permission denied | V026 immutability 정상 동작 | 직접 UPDATE 시도 자체가 잘못 — 새 UPDATE 로 복원 |
| 복원 후 grid 미갱신 | 캐시 미invalidate | F5 또는 TanStack invalidateQueries |

---

## 7. PITR vs application-level 복원 비교

| 영역 | Application (BS-04) | PITR (Phase 4-D) |
|---|---|---|
| 단위 | 단일 row | 전체 DB |
| 영향 범위 | 점 (1 row) | 면 (모든 데이터) |
| 도구 | AuditSnapshotService | pg_basebackup + WAL replay |
| 시간 | < 1분 | 30분 ~ 1시간 |
| 위험 | 낮음 | 높음 (PROD downtime) |
| 사용 시점 | 단일 row 잘못 입력 | 대규모 데이터 손상 |

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — application-level forensic 복원 SOP (PITR 와 분리) |
