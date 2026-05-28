# 사내 공정 스케줄링 시스템 — 사용자 매뉴얼 v1.0

**대상**: 송우산업 베타 사용자 8명 (PLANNER 3 + STK_USER 3 + IT_OPS 1 + READ_ONLY 1)
**작성일**: 2026-05-28 | **버전**: 1.0 | **참조**: Sprint 19 EP-BETA-LAUNCH TK-BETA-4

> 본 매뉴얼은 베타 운영 진입 시점의 4 role 별 표준 작업 절차(SOP). 시스템 변경 시 새 버전(v1.1+) 으로 분리 발행. 스크린샷은 `docs/manual/screenshots/` 폴더 별도.

---

## 0. 공통

### 0.1 접속

| 항목 | URL / 정보 |
|---|---|
| Frontend | http://localhost:5173 (사내망) |
| Backend Health | http://localhost:8080/api/actuator/health → `{"status":"UP"}` |
| Grafana 대시보드 | http://localhost:3000 (admin / admin 초기) |

### 0.2 로그인

1. 사번 8자리 입력 (예: `00000001`)
2. PIN 4자리 입력 (예: `0001`)
3. `로그인` 클릭

**정책 (NFR-SEC-007):**
- 5회 연속 실패 → 10분 잠금 (잠금 해제는 IT_OPS 만 가능)
- 첫 로그인 후 PIN 변경 권장 (매뉴얼 §IT_OPS 참조)
- JWT 토큰 유효기간 8시간 — 자동 갱신 없음, 만료 시 재로그인

### 0.3 로그아웃

우상단 빨강 `로그아웃` 버튼 → JWT 토큰 무효화 + `/login` 이동

### 0.4 우상단 알림 센터 (Sprint 18 EP-NOTIFY)

- 🔔 종 아이콘 — 미읽음 알림 Badge (빨강 숫자)
- 클릭 → 우측 Drawer 슬라이드 in
- 알림 종류: CRITICAL (빨강) / IMPORTANT (주황) / STANDARD (파랑) / INFO (회색)
- 클릭 시 (link 있으면) deep-link 이동 + 읽음 처리
- `모두 읽음` / `비우기` 버튼

### 0.5 화면 nav

| role | 보이는 메뉴 |
|---|---|
| PLANNER | 홈 / 수주 통합 / 성형 스케줄 / Capa 큐+KD 보충 / 압출 스케줄 / 감사 로그 |
| STK_USER | 홈 / 수주 통합 / 성형 스케줄 / 압출 스케줄 |
| IT_OPS | 홈 / 마스터 데이터 / 감사 로그 / Capa 큐+KD 보충 |
| READ_ONLY | 홈 / 수주 통합 / 성형 스케줄 / Capa 큐+KD 보충 / 압출 스케줄 / 감사 로그 |

---

## 1. PLANNER (생산 계획 담당자) — 사번 00000001~3

### 1.1 수주 import — `수주 통합` 메뉴

1. **파일 업로드** — 영업/관리 엑셀 1~3개 동시 (각 ≤ 20MB)
2. `trackingId` 발급 → 자동 폴링 5초
3. 상태 추적:
   - `QUEUED` → `PARSING` → `PARSED` → `MAPPING` → `MAPPED` / `REVIEW_REQUIRED`
   - `REVIEW_REQUIRED` 시 자동 매핑 검토 모달 노출 (매핑 실패율 1% 이상)
4. 매핑 룰 보정 후 재시도

### 1.2 Diff 검토 + 확정 — `/orders/diff/{trackingId}` (수주 통합 후 자동 이동)

1. **severity 분류 확인** (BR-O02):
   - CRITICAL (빨강 highlight) — 납기/품번 변경, 수량 ±20% 이상, 신규/삭제
   - IMPORTANT (주황) — 수량 ±10~20%
   - STANDARD (파랑) — 그 외
2. `확정` 버튼 (`POST /api/v1/orders/{trackingId}/commit`)
   - 사유 필수 입력 (BR-X02 audit)
   - 확정 시 OrderCommittedEvent 발행 → 자동 chain 진입 (Phase 5+ 활성 예정)
3. `거절` 버튼 — 입력 파일 재요청

### 1.3 VC 시뮬뷰 + 확정 — `성형 스케줄` 메뉴

**상단:**
- DegradedBanner — MES 1 shift 미수신 시 빨강 Alert (PLANNER/IT_OPS Excel 폴백 버튼)
- STOMP 실시간 연결 Tag (초록 ● = 연결 / 회색 ○ = 미연결)
- `Capa 큐 + KD 보충 →` 버튼

**중간 — AG Grid 회전 격자 (BR-V04 1~18 회전):**
- row: (일자 · 머신·슬롯)
- col: D1~D8 (주간) + N1~N10 (야간)
- 셀: hose_id (BR-V07 일중 락 — 같은 row 모두 같은 angle)

**하단 — 확정 대기 CANDIDATE (Sprint 16/17):**
- Table — 상태 / Hose / 머신·슬롯 / 생산일 / 회전 / 수량 / 작성자 / 단건 확정
- **본인 작성 row** = 빨강 `본인 작성` 배지 + 체크박스/확정 버튼 disabled (BR-X05)
- `전체 선택 (본인 제외)` → `선택 일괄 확정 (N건)` → BatchConfirmModal 노출
  - 선택 건수 + 총수량 + 가류기/Hose 분포 + BR-X05 자동 제외 안내
  - 200 시 N건 일괄 CONFIRMED + 자동 갱신
  - 409 BR-X05 시 본인 작성 row 포함 안내
- 단건 `확정` 버튼 → ConfirmModal (5 분기 메시지)

### 1.4 일중 앵글 교체 override (BR-V07)

D-0 (당일) row 수정 시 override_reason + override_by 입력 필수. UI 는 Sprint 5 OverrideJustificationForm 사용 (드래그 시 자동 노출).

### 1.5 dual-review 흐름 (BR-X05)

- 본인이 작성한 row 는 확정 불가 → 다른 PLANNER 에게 승인 요청
- 본인 작성 → 다른 PLANNER 가 확정 → 정상 CONFIRMED
- 베타 운영 시 PLANNER 2명 이상 동시 활성 권장 (00000001 ↔ 00000002 또는 00000003)

### 1.6 압출 매트릭스 — `압출 스케줄` 메뉴

- 다중 후보 ranking 표시 (Sprint 5 EP-18)
- BR-E01 D-1 역산 자동 (vc_production_date - 1 day = extrusion_deadline)
- BR-E05 reference yield = 2531 (29673-2R060)
- Excel 다운로드 가능

### 1.7 Capa 큐 + KD 보충 — `Capa 큐 + KD 보충` 메뉴

BR-V12 capa 초과 시 PRODUCT_PRIORITY rank ASC 정렬 자동 채택 + 추가 요청 큐 → Planner 1클릭 승인/거절.

---

## 2. STK_USER (현장 STK 작업자) — 사번 00000004~6

### 2.1 시뮬뷰 read — `성형 스케줄` 메뉴

PLANNER 와 동일 화면, 확정/Modal 권한 없음 (read-only).

### 2.2 swap 제안 등록 (REQ-FUNC-VC-018)

- 시뮬뷰 하단 `현장 swap 제안` 영역
- 같은 (가류기, 슬롯, 일자) 안 두 회전 사이 swap 제안 (드래그)
- PLANNER 가 1클릭 수용/거절 (총량 보존 invariant)

### 2.3 압출 매트릭스 read

`압출 스케줄` 메뉴 — 다중 후보 조회 + Excel 다운로드.

---

## 3. IT_OPS (IT 운영 담당자) — 사번 00000007

### 3.1 마스터 데이터 — `마스터 데이터` 메뉴

- **사용자 관리 (UserAdminPage)**:
  - 사번 추가/role 변경
  - PIN 재설정 (사용자 잠금 해제)
  - failed_attempts 초기화
- **품번 우선순위 (ProductPriorityPage)** — BR-V12 split 시점 rank
- **KD 발주 (KdOrderPage)** — BR-V13 잔량 보충

### 3.2 Excel 폴백 입력 — DegradedBanner 우상단 트리거

MES 미수신 1 shift 이상 시 우상단 빨강 배너의 `Excel 폴백 입력` 버튼:
- 가류기 선택 (LP-01~04, IC-01)
- shift_date / shift_no (1=주간전반 / 2=주간후반 / 3=야간전반 / 4=야간후반)
- planned_qty / actual_qty
- 입력 → POST `/api/v1/mes/shift/fallback` → mes_shift_event INSERT (source=EXCEL_FALLBACK)

### 3.3 Actuator + Grafana 모니터링

- http://localhost:8080/api/actuator/health → 헬스 체크
- http://localhost:8080/api/actuator/prometheus → metric raw
- http://localhost:3000 → Grafana (대시보드: Scheduling Overview, Application Overview, EP-NOTIFY Sprint 18, Logs Overview 등)

### 3.4 Cutover 작업 (운영 진입 시 1회)

운영 진입 시점에 99999-SAMPLE 시드 제거:
```sql
SELECT * FROM app.cleanup_99999_samples();
```
docker exec scheduling-postgres psql -U app_user -d scheduling -c 로 실행.

### 3.5 Backend / Frontend 자동시작 — NSSM

`infrastructure/scripts/install-nssm-services.ps1` 관리자 권한 실행 — PC 재부팅 시 자동 기동. 자세한 절차는 [README-nssm.md](../../infrastructure/scripts/README-nssm.md).

---

## 4. READ_ONLY (감사 · 임원) — 사번 00000008

### 4.1 조회 권한만

- 시뮬뷰 / 압출 매트릭스 / Capa 큐 / 감사 로그 — 모두 read-only
- 수정/확정/제안 등록 권한 없음 (403 응답)

### 4.2 감사 로그 — `감사 로그` 메뉴

- 모든 BR-X02 mutation 영속 (audit.schedule_audit_log)
- 작성자 (actor) / 시각 / 사유 / 영향 row PK
- 변조 불가 (NFR-SEC-004 — UPDATE/DELETE 금지 trigger)

---

## 5. 베타 사용자 초기 PIN

| 사번 | 초기 PIN | role | 비고 |
|---|---|---|---|
| 00000001 | 0001 | PLANNER | 첫 PLANNER |
| 00000002 | 0002 | PLANNER | dual-review 짝 |
| 00000003 | 0003 | PLANNER | 백업 |
| 00000004 | 0004 | STK_USER | |
| 00000005 | 0005 | STK_USER | |
| 00000006 | 0006 | STK_USER | |
| 00000007 | 0007 | IT_OPS | 마스터 + 잠금 해제 |
| 00000008 | 0008 | READ_ONLY | 임원/감사 |

> **보안 권고**: 첫 로그인 후 PIN 변경 (UserAdminPage 통해 IT_OPS 가 일괄 갱신). 외부 공유 금지.

---

## 6. 장애 시 대응

| 증상 | 1차 대응 | 2차 대응 |
|---|---|---|
| 로그인 실패 5회 → 잠금 | 10분 대기 또는 IT_OPS 잠금 해제 | UserAdminPage → failed_attempts=0 |
| 우상단 종 아이콘 Badge 빨강 5+ | Drawer 열어 알림 확인 | Critical Diff 시 즉시 처리 |
| DegradedBanner 빨강 | MES 자동 수신 확인 / 즉시 Excel 폴백 입력 | IT_OPS 가 MES 시스템 점검 의뢰 |
| `BR-X07 D-2 hard 제약` 423 응답 | 추가 요청은 D-2 (2일) 이상 미래 시점만 가능 — 일정 조정 | — |
| `BR-X05 dual-review` 409 응답 | 본인 작성 row 는 다른 PLANNER 에게 승인 요청 | — |
| `BR-V07 D-0 락` 423 응답 | 당일 row 수정은 override_reason 입력 필수 | OverrideJustificationForm 작성 |
| 시뮬뷰 빈 화면 (Empty) | V039 sample 또는 운영 데이터 없음 | IT_OPS 수주 import 진행 |
| Backend 미응답 | Get-Service Scheduling-Backend → Status 확인 | Restart-Service Scheduling-Backend (관리자) |
| Frontend 미응답 | http://localhost:8080/api/actuator/health 직접 호출 | Get-Service Scheduling-Frontend / Restart |

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-28 | Claude Code | 초안 — 4 role SOP (PLANNER + STK_USER + IT_OPS + READ_ONLY) + 베타 PIN 8건 + 장애 시 대응 9건. Sprint 10~18 누적 자산 기반. 스크린샷은 `docs/manual/screenshots/` 별도 (Phase 4 추가 권장). |
