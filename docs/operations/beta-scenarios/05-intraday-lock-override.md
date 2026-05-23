# 베타 시나리오 5 — BR-V07 일중 락 override

**시나리오 ID**: BS-05 | **페르소나**: Planner | **소요 시간**: ~10분
**관련 BR**: BR-V07 (당일 락) + BR-X02 (audit) + REQ-FUNC-VC-014 (override + 사유)

> 같은 (machine, slot, date) 안 다른 angle 입력 시도 → V027 trigger reject → Planner
> override 사유 + actor 입력 → DB trigger pass → audit row 자동 발행 + DO-04 영업일 경계 키.
> **Sprint 4 EP-13 v1.4 거버넌스 핵심 시나리오**.

---

## 1. 사전 조건

- [x] 시나리오 1 완료 — 1주 horizon CANDIDATE row 존재
- [x] PLANNER role 로그인
- [x] V027 enforce_vc_intra_day_lock trigger 활성

---

## 2. BR-V07 위반 시도 (의도적 — 베타 검증)

### 2.1 첫 row 정상 입력

```
1. /vc/simview → 회전 격자
2. 2026-06-10 LP-01 slot 3 rotation 5 → angle_id = "ANG-A"
3. 정상 CANDIDATE 생성 (BR-V07 통과 — slot 첫 row)
```

### 2.2 두 번째 row 다른 angle 시도 (위반)

```
1. 같은 (LP-01, slot 3, 2026-06-10) 의 rotation 7 위치
2. angle_id = "ANG-B" 로 변경 시도
3. POST 백엔드 → V027 trigger 자동 reject:
   "BR-V07 일중 앵글 교체 차단: machine=LP-01 slot=3 date=2026-06-10
    existing_angle=ANG-A new_angle=ANG-B (override_reason 강제 필수)"
4. UI 에 한국어 에러 모달 표시:
   "일중 앵글 교체는 BR-V07 위반입니다. Override 시 사유를 입력하세요."
```

---

## 3. Override 사유 입력 → 통과

### 3.1 OverrideJustificationForm 모달 (Sprint 5 vc-scheduling)

```
1. 에러 모달 → [Override 사유 입력] 버튼 클릭
2. 사유 텍스트 area (REQ-FUNC-VC-014):
   "예: 긴급 LOT 추가, 고객 우선순위 변경, 설비 점검 후 재투입"
3. POST /api/v1/schedule/vc/{id}/override
   - body: { reason: "긴급 LOT 추가 (영업 14:00 통보)", overrideBy: "planner-001" }
4. 백엔드 IntraDayOverrideService.applyOverride @Auditable:
   - VcSchedule.applyOverride(reason, overrideActor, now)
     - reason blank 차단 (도메인 invariant)
     - overrideBy blank 차단
   - DB save → V027 trigger 통과 (reason + by 비-NULL)
5. 성공 메시지 — "Override 적용 완료 (audit 자동 발행)"
```

### 3.2 DO-04 영업일 경계 키 (BR-V07)

```
백엔드 로그:
  "BR-V07 override applied — id=uuid, planner=planner-001,
   boundary=2026-06-10_END"

→ 현장 작업자 안내 — "본 셋팅은 2026-06-10_END (다음 영업일 시작 전) 까지 유지"
```

---

## 4. audit 영속 검증

```sql
-- audit row 자동 발행 확인 (BR-X02)
SELECT action, actor, reason, occurred_at
FROM audit.schedule_audit_log
WHERE table_name = 'vc_schedule'
  AND row_pk = '<vc_schedule_id>'
ORDER BY occurred_at DESC LIMIT 3;

-- 기대 결과:
--   1. INSERT (actor=system, reason=NULL)               -- CANDIDATE 생성
--   2. UPDATE (actor=planner-001, reason="긴급 LOT 추가") -- override
```

---

## 5. 기대 결과 + 검증

| 항목 | 기대 | 검증 |
|---|---|---|
| BR-V07 trigger reject | 100% (reason 누락 시) | V027 RAISE 메시지 |
| Override reason 강제 | 100% | 도메인 invariant + DB trigger 이중 |
| Override actor (RBAC) | 100% | @PreAuthorize PLANNER |
| audit row 자동 발행 | 100% | V025 trigger AFTER UPDATE |
| DO-04 영업일 경계 키 | `YYYY-MM-DD_END` | BusinessDayBoundaryFormatter 단위 통과 |
| 회전 격자 시각화 | override row yellow mark | Sprint 5 UI |
| **🌟 일중 락 위반 0건** | 베타 1주 운영 0 | Grafana business-kpi K-V04 |

---

## 6. Override 남용 모니터링 (Phase 4-D 진입 게이트)

- Slack alert — override 1일 5회 이상 발생 시 IT_OPS 통보
- 1주 운영 누적 override 수 ≥ 10 → Planner 사유 분석 + 마스터 데이터 보강 검토
- Grafana K-V04 (BR-V07 위반) 대시 — 일별 0 (lower target) 유지

---

## 7. 실패 시 대처

| 증상 | 원인 | 대처 |
|---|---|---|
| reason 누락 → 400 Bad Request | 도메인 invariant 정상 | 사용자에 reason 입력 안내 |
| reason 입력 후 여전히 reject | overrideBy 미포함 | RBAC actor (Principal) 자동 주입 확인 |
| Override 적용 후 cascade 미발생 | VcChangedEvent 미발행 | Sprint 5 carry — 시나리오 3 의 publisher 확인 |
| BR-V07 위반 0건 임계값 초과 (K-V04 ≥ 1) | Planner override 남용 | Slack alert + 마스터 보강 회의 |

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — BR-V07 일중 락 override SOP + DO-04 boundary |
