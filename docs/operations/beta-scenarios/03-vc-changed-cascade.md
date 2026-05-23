# 베타 시나리오 3 — VC 변경 cascade (BR-X03 partial replan)

**시나리오 ID**: BS-03 | **페르소나**: Planner | **소요 시간**: ~15분
**관련 BR**: BR-X03 (수동 호출 0건) + BR-E11 (변경 영향 자동 분석) + REQ-FUNC-EX-014 (p95 ≤ 2초)

> Planner override → VcChangedEvent → ImpactedRowFinder → PartialReplanService →
> ExReplanCompletedEvent → STOMP /topic/extrusion-updates → 압출 매트릭스 자동 갱신.
> **REV-D-003 명시화 — 수동 호출 100% 제거**.

---

## 1. 사전 조건

- [x] 시나리오 1 (정상 horizon) 완료 — VC 확정 + EX cascade 완료
- [x] ExScheduleCandidate ~6,300 row SCHEDULED 상태

---

## 2. Planner override 절차 (긴급 수주 변경 가정)

```
1. /vc/simview 진입 → 회전 격자
2. 변경 대상 row 선택 (예: 2026-06-10 LP-01 slot 3 rotation 7)
3. 우측 panel [Override] 버튼 클릭 (Sprint 7+ UI)
   - 대안: 직접 POST /api/v1/schedule/vc/{id}/override
   - body: { newQty: 150, newDate: '2026-06-11', reason: '긴급 LOT 추가' }
4. VcSchedule.applyOverride 호출:
   - reason + overrideBy 비-NULL 필수 (BR-V07 + REQ-FUNC-VC-018)
   - V027 trigger pass (일중 락 — reason 비-NULL 통과)
5. 백엔드 VcChangedEvent 발행 (Modulith AFTER_COMMIT + Async)
```

---

## 3. cascade chain 자동 동작 (수동 호출 0건)

### 3.1 ImpactedRowFinder

```
이벤트 수신 → vc_row_id 직접 매핑 ExScheduleCandidate 1건
+ DATE 변경 시 hose 동일 horizon ±3일 인접 candidate 추가
→ 영향 candidate IDs 반환
```

### 3.2 PartialReplanService.replanWithContext

```
- QUANTITY 변경 → vcYield 갱신 + status PENDING
- DATE 변경 → deadline 재산출 (WorkingCalendar.subtractWorkingDays(date,1))
- MACHINE 변경 → grouping 재배치 트리거
- DELETED → status FAILED
- CONFIRMED candidate → 차단 (override 별도 흐름)
```

### 3.3 ExReplanCompletedEvent 발행

```
vcScheduleId + completedAt + triggeredCount + candidateIds[]
```

### 3.4 STOMP push (notify/ExReplanPushListener)

```
SimpMessagingTemplate.convertAndSend("/topic/extrusion-updates", event)
→ Frontend useExUpdates hook 수신 → useExMatrix invalidateQueries
→ AG Grid 자동 재렌더링 (Planner 액션 0)
```

---

## 4. UI 동시 검증 (2 모니터 권장)

| 모니터 | 화면 | 확인 |
|---|---|---|
| 왼쪽 | /vc/simview | override 직후 회전 격자 cell 색상 변경 (yellow override 마커) |
| 오른쪽 | /extrusion-matrix | STOMP "마지막 cascade: HH:mm:ss (N건 갱신)" 표시 |

---

## 5. 기대 결과 + 검증

| 항목 | 기대 | 검증 |
|---|---|---|
| VcChangedEvent 발행 | AFTER_COMMIT | 백엔드 로그 "VC changed event received" |
| ImpactedRowFinder | direct + horizon ±3일 candidate | 백엔드 로그 |
| PartialReplanService | triggered=N (CONFIRMED 차단 후) | @Auditable reason="BR-X03 partial replan" |
| audit row 자동 발행 | 100% (BR-X02) | `audit.schedule_audit_log` UPDATE row |
| **🌟 STOMP push p95** | ≤ 2,000ms (REQ-FUNC-EX-014) | Grafana scheduling-overview "STOMP push" panel |
| Frontend grid 갱신 | < 3초 | Planner 인지 (액션 0) |
| **🌟 수동 호출 0건** | BR-X03 | 백엔드 로그에 수동 PartialReplanController 호출 0 |

---

## 6. 실패 시 대처

| 증상 | 원인 | 대처 |
|---|---|---|
| VcChangedEvent 미수신 (Listener 0건 trigger) | spring-modulith-events-jpa 비활성 | V031 event_publication 테이블 존재 확인 |
| Listener 호출됨 but Replan 0 row | CONFIRMED candidate 만 — 차단 정상 | 로그 "CONFIRMED candidate skip" 확인 |
| STOMP push 미도달 (Frontend badge "disconnected") | Bearer JWT 만료 | 재로그인 + SockJS 재연결 (5초 자동) |
| p95 > 2초 (EP-EX14 NFR 미달) | Redis 부하 | Grafana resilience4j 대시 + JVM heap 확인 |

---

## 7. KPI 영향

- **BR-X03 자동화** — 100% 보장 (수동 호출 0건 monitor)
- **NS-S07** D-1 준수율 — cascade 후 deadline 갱신 정합

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — VC override → cascade chain SOP |
