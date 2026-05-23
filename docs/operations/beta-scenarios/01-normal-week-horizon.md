# 베타 시나리오 1 — 정상 1주 horizon 운영

**시나리오 ID**: BS-01 | **페르소나**: Planner | **소요 시간**: ~30분
**Phase**: 4-B (Phase-4_EntryPlan §5) | **빈도**: 매주 월요일 09:00

> 영업 수주 import → VC 자동 스케줄 생성 → Planner 확정 → EX cascade 자동 발생 →
> Excel export. **정상 흐름 — 베타 운영 시 매주 반복 검증**.

---

## 1. 사전 조건

- [x] STG 환경 정상 부팅 (`docker compose ps` 모든 service `healthy`)
- [x] Keycloak SSO 로그인 — role `PLANNER`
- [x] 베타 시드 데이터 import 완료 (`seed-stg-beta-data.sh`)
- [x] Grafana 대시 `scheduling-overview` + `business-kpi` open (모니터)

---

## 2. 단계별 절차

### 2.1 수주 Excel import

```
1. https://stg.scheduling.internal 진입 → Keycloak SSO
2. 좌측 메뉴 [수주 통합] 클릭 → /orders/import
3. xlsx 파일 drag&drop (월별 예상·주간·확정·KD 4종 워크북)
   - 최대 3 파일 동시 / 각 20MB 이하
4. 매핑 검토 모달 — 자동 매핑 성공률 표시
   - ≥ 95% — [다음: 변경 검토] 진행
   - < 95% — 별칭 추가 + 룰 저장 + 재시도
5. 변경 검토 — 신규/수정/삭제 row 확인 → 확정
```

### 2.2 VC 시뮬뷰 — 자동 스케줄 확인

```
1. 좌측 메뉴 [성형 스케줄] → /vc/simview
2. RangePicker — 다음 주 월~금 (2026-06-08 ~ 06-12) 선택
3. AG Grid 회전 격자 표시:
   - row = (date, machine·slot)
   - col = D1-8 (주간) + N1-10 (야간) = 18 회전
   - cell = hose_id (BR-V07 일중 락 — 같은 row 모든 cell 동일 angle)
4. 모든 row 가 CANDIDATE 상태 확인 (BR-X01)
```

### 2.3 Planner 확정 (BR-X01)

```
1. 회전 격자에서 row 선택 (Ctrl+click 다중 선택)
2. 우상단 [확정] 버튼 → POST /api/v1/schedule/vc/confirm-batch
3. 확인 dialog — N rows CANDIDATE → CONFIRMED
4. 성공 메시지 — "VC 확정 완료 (audit 자동 발행)"
   - 백엔드: V022 trigger pass + V025 audit row insert
   - Modulith: VcConfirmedEvent 발행 (AFTER_COMMIT + Async)
```

### 2.4 EX cascade 자동 발생 (BR-X03)

```
1. 좌측 메뉴 [압출 스케줄] → /extrusion-matrix
2. STOMP 상태 badge 'connected' 확인 (우상단)
3. 매트릭스 자동 갱신 — VC 확정 cascade
   - ExtrusionScheduleService 가 PENDING candidate 생성
   - D-1 영업일 deadline (BR-E01)
   - yield 자동 계산 (BR-E05 — 29673-2R060 = 2,531)
4. Tabs [매트릭스] / [다중 후보 ranking] 전환 확인
5. [Excel 다운로드] 클릭 → EX_MATRIX_2026-06-08_2026-06-12.xlsx
   - 시트명 `6월8일(압출)` ~ `6월12일(압출)` BR-E09 정규식 일치
```

---

## 3. 기대 결과 + 검증

| 항목 | 기대 | 검증 방법 |
|---|---|---|
| 수주 import 성공률 | ≥ 95% | UI 매핑 검토 모달 표시 |
| VC schedule 생성 row | ~6,300 (1주) | AG Grid 격자 dataset 표시 |
| Confirm → CONFIRMED 전이 | 100% | DB trigger pass 로그 |
| audit row 자동 발행 | 100% (BR-X02) | `SELECT count(*) FROM audit.schedule_audit_log` |
| EX cascade auto-generate | < 5초 | STOMP push lastUpdate badge |
| Excel 다운로드 시트명 | `\d+월\d+일(압출)` | 정규식 통과 |
| Grafana API p95 | matrix < 800ms | scheduling-overview 대시 |

---

## 4. 실패 시 대처

| 증상 | 원인 | 대처 |
|---|---|---|
| 매핑 성공률 < 50% | 영업 양식 변경 | 새 양식 1 row sample → STK-08 룰 추가 요청 |
| VC schedule 0 row | 마스터 미동기화 | `MASTER 갱신` 버튼 + 5초 대기 후 재시도 |
| Confirm 실패 — 403 | RBAC 권한 부족 | Keycloak realm role PLANNER 확인 |
| EX cascade 미발생 | STOMP 미연결 | F12 → Network WS 확인, 재로그인 |
| Excel 다운로드 시트명 불일치 | EP-12 정규식 회귀 실패 | docs/operations/idp-failover.md §장애 대응 |

---

## 5. KPI 영향

본 시나리오 정상 완료 시 Grafana business-kpi 대시 갱신:
- **NS-S07** D-1 압출 deadline 준수율 ↑ (목표 ≥ 98%)
- **K-V02** 가류기 가동률 ↑ (목표 ≥ 85%)

---

## 6. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — 정상 1주 horizon SOP |
