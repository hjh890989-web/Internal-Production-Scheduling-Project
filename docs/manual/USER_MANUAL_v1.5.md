# 사내 공정 스케줄링 시스템 — 사용자 매뉴얼 v1.5

**대상**: 송우산업 베타 사용자 8명 (PLANNER 3 + STK_USER 3 + IT_OPS 1 + READ_ONLY 1)
**작성일**: 2026-06-01 | **버전**: 1.5 | **참조**: Sprint 25 S25-A ST-PROD-3 (v1.4 Sprint 24 S24-A ST-FB-3 TK-FB-3-4 기반)

> 본 매뉴얼은 베타 운영 진입 시점의 4 role 별 표준 작업 절차(SOP). 시스템 변경 시 새 버전(v1.x+) 으로 분리 발행. 스크린샷은 `docs/manual/screenshots/` 폴더 별도.
> **v1.5 변경**: §3.7 IT_OPS Blue/Green 무중단 배포 가이드 신규 — Sprint 25 ST-PROD-3 활성. §3.7.1 배포 mode 선택 (수동 blue-green-switch.sh vs 자동 blue_green_deploy.sh Jenkins) + §3.7.2 첫 배포 전 green 사전 기동 (1회) + §3.7.3 정상 절차 5단계 (Harbor pull → switch → health → smoke → Grafana) + §3.7.4 rollback 3 시나리오 (auto / 수동 / missing 복구) + §3.7.5 트러블슈팅 4건 (NGINX reload / image pull / DB migration / healthcheck timeout) + §3.7.6 Grafana 배포 dashboard cross-ref (Sprint 21 deployment marker). §6 장애 대응 표에 `Blue/Green deploy 실패` 1줄 추가 — §3.7.4 rollback 참조만 (중복 회피).
> **v1.4 누적**: §3.3 Actuator + Grafana 모니터링 + alert 대응 (§3.3.1 dashboard 5종 + §3.3.2 baseline alert 4 rule Runbook + §3.3.3 Slack escalation 예약).
> **v1.3 누적**: §3.6 IT_OPS MES adapter 설정 가이드 — Sprint 23 EP-MES-ADAPTER-1 활성. config flag (`scheduling.mes.adapter`) + URL/Token 등록 + circuit breaker 트러블슈팅 (§3.2 Excel 폴백 연계). §6 에 MES adapter circuit OPEN 대응 1건.
> **v1.1 병행**: Sprint 20 EP-EXT-WEBHOOK 이 §6 에 Slack/Kakao webhook 장애 대응을 별도 추가 (병렬 진행, 완료 시 합본).

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

`마스터 데이터` 메뉴 → **MasterHubPage** 카드 진입점. 모든 변경은 audit_log 기록 (BR-X02 — 사번 + reason 자동 부착). 카드는 IT_OPS 만 보이며, 다른 role 직접 URL 접근 시 403.

| 카드 | 페이지 | 용도 |
|---|---|---|
| 사용자 관리 | UserAdminPage | 사번 추가 / role 변경 / PIN 재설정 / 잠금 해제 |
| 우선순위 | ProductPriorityPage | BR-V12 capacity overflow rank |
| KD 발주 | KdOrderPage | BR-V13 잔량 보충 |
| 품번 (47종) | ProductSpecPage | 품번 spec 조회 (read, write 는 Sprint 22+) |
| **장비 (LP/IC)** | VcMachineAdminPage | 가류기 5대 회전수 + active toggle |
| **셋팅 그룹 (1~8)** | SettingGroupAdminPage | setting_group 1~8 + active |
| **성형 제약 + 합금형** | VcConstraintAdminPage | 47 품번 composite_count·slot·mold_qty (BR-V14) |
| **라인 (line_type)** | LineAdminPage | line_type + product 호환 매핑 |
| **휴일 (HOLIDAY)** | HolidayAdminPage | 연도별 휴일 추가/삭제 (BR-X04 KST) |

#### 3.1.1 사용자 관리 (UserAdminPage)

- 사번 추가 / role 변경
- PIN 재설정 (사용자 잠금 해제)
- failed_attempts 초기화

#### 3.1.2 품번 우선순위 (ProductPriorityPage)

BR-V12 split 시점 rank — capacity overflow 시 채택 순서.

#### 3.1.3 KD 발주 (KdOrderPage)

BR-V13 잔량 보충 — KD 발주 등록.

#### 3.1.4 장비 관리 (VcMachineAdminPage) — Sprint 21 신규

LP-01~04 + IC-01 가류기 5대 목록. 회전수·가동 여부 운영 변경 시 사용.

**가류기 교체 / 회전수 변경 절차:**
1. `마스터 데이터` → `장비 (LP/IC)` 카드 진입
2. 대상 가류기 row `수정` → Modal
3. `dayRotations` (주간 회전, 기본 8) / `nightRotations` (야간 회전, 기본 10) 조정 → 회전수 합 = 주+야 (BR-V04 1~18)
4. `active` toggle — 가류기 정비/교체 시 OFF
   - **주의**: active=false 는 **신규 INSERT 만 차단**. 기존 vc_schedule row (machine_id FK) 는 보존 — 운영 차단 없음
   - 예: LP-04 정비 → active=false → 다음 build 부터 LP-04 슬롯 미배정. 기존 확정 일정은 유지
5. `저장` → audit_log row 영속 (사번 + 변경 전후 값)
- machine_type · total_slots (LP=8 고정) 은 수정 불가 (회색 표시)
- **신규 가류기 도입** (예: LP-05): 본 페이지 추가 + VcConstraint slot 가용성(§3.1.6) 동기화 필요

#### 3.1.5 셋팅 그룹 관리 (SettingGroupAdminPage) — Sprint 21 신규

setting_group 1~8 (BR-V12/V13 product_setting_group 연결).

**절차:**
1. `셋팅 그룹 (1~8)` 카드 진입
2. 신규: `추가` → groupId (1~8 범위 강제) + displayName
3. 수정: row `수정` → displayName 변경
4. `active` toggle (soft-delete) — 연결된 product_setting_group row 가 있으면 비활성 시 경고
- groupId 범위 외 (0 또는 9+) 입력 → 400 거부

#### 3.1.6 성형 제약 + 합금형 (VcConstraintAdminPage) — Sprint 21 신규

47 품번별 성형 제약 — composite_count (합금형) + slot 가용성 + mold 수량.

**합금형(composite_count) 변경 절차 (BR-V14):**
1. `성형 제약 + 합금형` 카드 진입 → 47 품번 Table (검색 필터 + 페이지 20)
2. 대상 품번 row `수정` → Drawer
3. `composite_count` — **1·2·3·6 만 허용** (BR-V14). 4·5·7 등 입력 시 거부
4. `lp/ic mold_qty` ≥ 0 (금형 보유 수량)
5. `slot1~slot7` eligibility 체크박스 (7 슬롯 가용성 boolean)
6. `저장` → audit_log 영속
- **신규 합금형 도입**: 해당 품번 composite_count 변경 + slot 가용성 재설정 → 다음 build 즉시 반영

#### 3.1.7 라인 관리 (LineAdminPage) — Sprint 21 신규

line_type (LP / IC / EX) + product 호환 매핑.

**절차:**
1. `라인 (line_type)` 카드 진입
2. 신규 라인: `추가` → lineCode + lineName + lineType
3. product 호환: row 선택 → Transfer/멀티셀렉트 로 호환 품번 매핑
4. `active` toggle — 비활성 시 다수 schedule 의존 차단 (확인 모달)

#### 3.1.8 휴일 관리 (HolidayAdminPage) — Sprint 21 신규

연도별 휴일 Calendar UI (BR-X04 KST 정합). WorkingCalendarService cache 와 연동 — 휴일 추가 시 다음 build CapacityLedger 가 해당 일자 영업일 제외.

**신년 휴일 일괄 갱신 절차 (매년 1회 권장):**
1. `휴일 (HOLIDAY)` 카드 진입 → Calendar
2. 우상단 연도 selector 로 대상 연도 선택 (예: 2027)
3. 법정공휴일 날짜 클릭 → `추가` Modal
   - holidayName (예: 설날) + holidayType (`법정공휴일` / `사내 휴일` / `정비일`) + description(선택)
4. 저장 → WorkingCalendar cache invalidate → 즉시 반영
5. 삭제: 기존 휴일 날짜 클릭 → `삭제` (Popconfirm)
   - **주의**: 영업일 → 휴일 전환 시 기존 schedule 의존 row 충돌 가능 — 영향 row 미리 표시 + confirm
- 모든 timestamp Asia/Seoul (BR-X04) — KST 기준 날짜로 저장

### 3.2 Excel 폴백 입력 — DegradedBanner 우상단 트리거

MES 미수신 1 shift 이상 시 우상단 빨강 배너의 `Excel 폴백 입력` 버튼:
- 가류기 선택 (LP-01~04, IC-01)
- shift_date / shift_no (1=주간전반 / 2=주간후반 / 3=야간전반 / 4=야간후반)
- planned_qty / actual_qty
- 입력 → POST `/api/v1/mes/shift/fallback` → mes_shift_event INSERT (source=EXCEL_FALLBACK)

### 3.3 Actuator + Grafana 모니터링 + alert 대응

- http://localhost:8080/api/actuator/health → 헬스 체크
- http://localhost:8080/api/actuator/prometheus → metric raw
- http://localhost:3000 → Grafana

#### 3.3.1 신규 dashboard (Sprint 18 ~ 24 누적)

| dashboard | 용도 | 추가 시점 |
|---|---|---|
| Scheduling Overview | 전체 시스템 핵심 지표 (수주/VC/EX/Capa) | Sprint 10~17 누적 |
| Application Overview | JVM heap·GC·thread·HTTP latency | Sprint 0 baseline |
| EP-NOTIFY Sprint 18 | WebSocket 연결·broadcast 지연·CRITICAL 알림 수 | Sprint 18 |
| Logs Overview | Loki 기반 log volume + ERROR rate | Sprint 19 |
| **MES adapter Sprint 23** (신규) | MES polling success rate / p95 fetch latency / circuit breaker state / DegradedModeService 6h gauge | **Sprint 24 ST-FB-3** |

#### 3.3.2 baseline alert 4 rule 대응 절차

`infrastructure/observability/grafana/alerts/baseline-alerts.yml` 참조. 4 rule 모두 Prometheus rule → Grafana alert → (Phase 5+) Slack escalation 흐름.

| rule | 발화 조건 | 1차 대응 |
|---|---|---|
| **MesRetryCriticalRate** | `mes_fetch_failed_without_retry_total / mes_fetch_attempts_total > 0.05` (5분 윈도우) | §3.6 트러블슈팅 표 — `MES_URL` / `MES_TOKEN` 미설정·오류 확인 → NSSM 환경변수 재확인 → Backend 재시작 |
| **MesFetchP95High** | MES `fetchShiftEvent` p95 latency > 800ms 10분 지속 | 사내 IT MES 서버 부하 확인 의뢰 (CPU/네트워크) + `scheduling.mes.http.timeout-seconds` 상향 검토 (§3.6.5 참조) |
| **MesCircuitOpen** | Resilience4j `mes` circuit state=1 (OPEN) 1분 지속 | §3.6.4 자동 HALF_OPEN 30초 대기 → 복귀 미발생 시 6h 시점 §3.2 Excel 폴백 진입 |
| **MesDegradedSixHours** | `DegradedModeService` degraded_machines gauge > 0 이 6시간 지속 | §3.2 Excel 폴백 즉시 입력 + IT_OPS 가 MES 시스템 점검 의뢰 (§6 장애 대응 표 ↔ 본 절 cross-reference) |

> 본 4 rule 대응은 §6 장애 대응 표의 `MES adapter circuit OPEN` / `DegradedBanner 빨강` row 와 정합 — §6 표는 사용자 관점 증상→대응 1줄, 본 §3.3.2 는 IT_OPS 관점 alert 규칙→Runbook 매핑. 중복 갱신 회피 — §6 표 유지 + 본 절 신규.

#### 3.3.3 Slack escalation 채널 (Phase 5+ 예약)

- 현재 alert 는 Grafana UI 내 알림 패널까지만 노출
- Sprint 20 ST-EXT-1 (EP-EXT-WEBHOOK) 마감 후 실 Slack webhook URL 발급 — `#scheduling-alerts` 채널 자동 push
- v1.4 시점에는 `infrastructure/observability/grafana/alerts/baseline-alerts.yml` 의 receiver 가 placeholder 상태 — Sprint 20 합본 시 본 절에 채널명·escalation 정책 (CRITICAL → @here / IMPORTANT → @channel) 추가 예정

### 3.4 Cutover 작업 (운영 진입 시 1회)

운영 진입 시점에 99999-SAMPLE 시드 제거:
```sql
SELECT * FROM app.cleanup_99999_samples();
```
docker exec scheduling-postgres psql -U app_user -d scheduling -c 로 실행.

### 3.5 Backend / Frontend 자동시작 — NSSM

`infrastructure/scripts/install-nssm-services.ps1` 관리자 권한 실행 — PC 재부팅 시 자동 기동. 자세한 절차는 [README-nssm.md](../../infrastructure/scripts/README-nssm.md).

### 3.6 MES adapter 설정 (Sprint 23 신규) — `scheduling.mes.adapter`

Sprint 23 EP-MES-ADAPTER-1 활성 — 실 MES (Manufacturing Execution System) REST API 와 60초 주기 polling 연결.
DEV/STG 는 `jpa` (DB stub) 모드, PROD 는 `http` (실 MES REST polling) 모드 운영.

#### 3.6.1 adapter 모드 선택

| 모드 | 동작 | 적용 환경 |
|---|---|---|
| `jpa` (default) | mes_shift_event 테이블 직접 read (DB stub) — 실 MES 없이 운영 가능 | DEV, STG, MES 미연동 PROD |
| `http` | MES REST `/api/mes/shift?machine=&date=&shift_no=` 60초 polling | PROD 실 MES 연동 시 |

#### 3.6.2 환경 변수 (config flag)

NSSM AppEnvironmentExtra 또는 application-prod.yml 환경 변수 등록:

| 변수 | 기본값 | 설명 |
|---|---|---|
| `MES_ADAPTER` | `jpa` | `jpa` 또는 `http` — adapter bean 선택 |
| `MES_URL` | (빈값) | `http` 모드 시 MES base URL (예: `https://mes.intranet/api`) |
| `MES_TOKEN` | (빈값) | `http` 모드 시 Bearer token (사내 IT 발급) |

추가 (application.yml 기본값 — 별도 환경변수 불필요):
- `scheduling.mes.http.timeout-seconds` — 10초 (REST 호출 timeout)
- `scheduling.mes.poll-interval-ms` — 60000ms (polling 주기)

#### 3.6.3 http 모드 적용 절차 (PROD 전환)

1. 사내 IT 에 MES REST endpoint URL + Bearer token 발급 의뢰
2. NSSM service 환경변수 추가:
   ```powershell
   nssm set Scheduling-Backend AppEnvironmentExtra `
     "MES_ADAPTER=http" `
     "MES_URL=https://mes.intranet/api" `
     "MES_TOKEN=<발급받은 token>"
   ```
3. Backend 재시작:
   ```powershell
   Restart-Service Scheduling-Backend
   ```
4. 첫 polling (60초 후) 결과 검증:
   - Grafana → MES adapter Sprint 23 dashboard → MES polling success rate (§3.3.1 참조)
   - Loki `{app="scheduling"} |= "MesPollingService"` 검색 → "MES polling success machine=... shift=..." 로그
   - http://localhost:8080/api/actuator/health → `mes` health indicator UP

#### 3.6.4 Resilience4j circuit breaker (자동 fallback)

`HttpMesShiftClient` 는 `@Retry(name=mes)` + `@CircuitBreaker(name=mes)` 자동 활성:
- 호출 실패 시 3회 retry (exponential backoff)
- 4회 연속 실패 → circuit OPEN → 즉시 fallback (예외 던지지 않고 빈 결과)
- 30초 후 자동 HALF_OPEN → 3 call permitted → 성공 시 CLOSED 복귀
- circuit OPEN 지속 시 `DegradedModeService` 가 6시간 미수신 감지 → DegradedBanner 빨강 노출 (§3.2 Excel 폴백 진입)

#### 3.6.5 트러블슈팅

| 증상 | 진단 | 1차 대응 |
|---|---|---|
| Grafana MES polling success rate 0% | `MES_URL` / `MES_TOKEN` 미설정 or 오류 | NSSM 환경변수 재확인 → Backend 재시작 |
| Loki `circuit breaker mes state=OPEN` 로그 | MES 서버 다운 or token 만료 | MES 서버 확인 + 사내 IT token 재발급 |
| DegradedBanner 6시간 빨강 지속 | MES adapter 장기 장애 | §3.2 Excel 폴백 즉시 입력 + IT_OPS 가 MES 시스템 점검 의뢰 |
| `MES_TOKEN` 변경 후 401 | Bearer token 갱신 안됨 | Backend 재시작 (token 은 startup 시 1회 주입) |
| timeout 빈번 (Loki `SocketTimeoutException`) | MES 서버 지연 | `scheduling.mes.http.timeout-seconds` 15~20초 상향 검토 |
| `jpa` 모드 복귀 (긴급) | http adapter 장애 시 빠른 운영 복귀 | `MES_ADAPTER=jpa` 환경변수 변경 + Backend 재시작 → mes_shift_event 테이블 직접 read |

#### 3.6.6 Phase 5+ carry-over

- 실 MES vendor spec DTO 교체 (현재 mock contract 기준)
- MQ / file 기반 adapter 확장 (현재 HTTP REST polling baseline)
- Grafana 패널 — MES polling success rate / circuit state visualization (Sprint 24 ST-FB-3 본 v1.4 에 §3.3.1 추가됨)

### 3.7 IT_OPS Blue/Green 무중단 배포 (Sprint 25 ST-PROD-3 활성)

Sprint 25 EP-PROD-DEPLOY ST-PROD-3 — Harbor image registry + NGINX upstream toggle 기반 blue/green 컨테이너 무중단 배포. 운영자 수동 실행 (`blue-green-switch.sh`) 또는 Jenkins CI/CD pipeline 자동 호출 (`blue_green_deploy.sh`) 두 mode 운영.

#### 3.7.1 배포 mode 선택

| mode | 트리거 | 사용 시점 |
|---|---|---|
| **수동** | `blue-green-switch.sh` — 운영자 직접 실행 (PowerShell 또는 bash) | 긴급 hotfix, 사후 검증 후 단계 진행 |
| **자동** | `blue_green_deploy.sh` — Jenkins CI/CD pipeline 자동 호출 | 정상 release tag push 시 (Harbor tag 발행 → pipeline 자동 deploy) |

#### 3.7.2 첫 배포 전 green 사전 기동 (1회)

`docker-compose.prod.yml` 의 depends_on reset 정책상 blue 만 명시되어 있어 — 최초 1회는 운영자가 수동으로 green 컨테이너를 사전 기동해야 한다.

```bash
docker compose -f infrastructure/docker-compose.prod.yml up -d backend-green
```

이후 부터는 blue ↔ green 토글이 자동 처리됨.

#### 3.7.3 정상 절차 5단계

1. **Harbor image tag pull** — 예: `docker pull harbor.intranet/scheduling/backend:v1.2.3`
2. **switch 실행** — `./blue-green-switch.sh green v1.2.3` (수동) 또는 Jenkins pipeline 자동
3. **health check 확인** — `curl http://localhost:8080/api/actuator/health` → `{"status":"UP"}` 응답 확인
4. **smoke test** — `curl http://localhost:8080/api/v1/schedule/vc/slots` → 401 응답 정상 (인증 필수 endpoint 가 정상 라우팅됨을 확인)
5. **Grafana 배포 dashboard 모니터링** — Sprint 21 deployment marker (annotations + deployment_started/completed event) 표시 확인 (§3.7.6 cross-ref)

#### 3.7.4 rollback 3 시나리오

| 시나리오 | 트리거 | 절차 |
|---|---|---|
| **healthcheck 실패 자동 rollback** | switch 후 health UP 실패 | `deploy.sh` 자동 처리 — 이전 색 컨테이너로 NGINX upstream 자동 복귀 (운영자 개입 불필요) |
| **운영자 수동 rollback** | smoke test 실패 / 비즈니스 검증 실패 | `./blue_green_rollback.sh` 실행 → NGINX upstream 이전 색 강제 복귀 |
| **이전 컨테이너 missing 복구** | 이전 색 컨테이너 중지/제거 상태 | `docker compose -f infrastructure/docker-compose.prod.yml up -d backend-{prev_color}` 선행 → 이후 `./blue_green_rollback.sh` |

#### 3.7.5 트러블슈팅

| 증상 | 원인 | 대응 |
|---|---|---|
| NGINX reload 실패 | conf syntax error | `nginx -t` 사전 확인 + rollback (§3.7.4) |
| image pull 실패 | Harbor 인증 만료 | `docker login harbor.intranet` 재인증 후 재시도 |
| DB migration 충돌 | Flyway version 중복 | `flyway repair` + 수동 인터벤션 (DBA 협의) |
| healthcheck timeout 30s | DB connection slow | `spring.datasource.hikari maxLifetime` 확인 + DB 연결 상태 점검 |

#### 3.7.6 Grafana 배포 dashboard cross-ref

Sprint 21 deployment marker — Grafana annotations API 기반 `deployment_started` / `deployment_completed` event marker 가 모든 dashboard 시계열 panel 위에 vertical line 으로 표시됨. 배포 직후 latency·error rate 변동을 시각적으로 추적 가능. 자세한 panel 정의는 §3.3.1 신규 dashboard 표 참조.

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
| **MES adapter circuit OPEN** (Loki `state=OPEN`) | §3.6 트러블슈팅 표 참조 → URL/Token 확인 후 Backend 재시작 | 30초 자동 HALF_OPEN 복귀 + 6h 지속 시 Excel 폴백 |
| **Blue/Green deploy 실패** | §3.7.4 rollback 3 시나리오 참조 (자동 / 수동 / missing 복구) | — |
| `BR-X07 D-2 hard 제약` 423 응답 | 추가 요청은 D-2 (2일) 이상 미래 시점만 가능 — 일정 조정 | — |
| `BR-X05 dual-review` 409 응답 | 본인 작성 row 는 다른 PLANNER 에게 승인 요청 | — |
| `BR-V07 D-0 락` 423 응답 | 당일 row 수정은 override_reason 입력 필수 | OverrideJustificationForm 작성 |
| `BR-V14 합금형` 400 응답 | composite_count 는 1·2·3·6 만 허용 (VcConstraintAdminPage) | — |
| 시뮬뷰 빈 화면 (Empty) | V039 sample 또는 운영 데이터 없음 | IT_OPS 수주 import 진행 |
| Backend 미응답 | Get-Service Scheduling-Backend → Status 확인 | Restart-Service Scheduling-Backend (관리자) |
| Frontend 미응답 | http://localhost:8080/api/actuator/health 직접 호출 | Get-Service Scheduling-Frontend / Restart |

> **§3.3.2 cross-reference**: 본 표의 `MES adapter circuit OPEN` / `DegradedBanner 빨강` 은 §3.3.2 baseline alert (MesCircuitOpen / MesDegradedSixHours) Runbook 과 정합 — 사용자 증상→대응 (본 §6) ↔ alert rule→Runbook (§3.3.2) 분리 운영 (중복 갱신 회피).
> **§3.7.4 cross-reference**: 본 표의 `Blue/Green deploy 실패` 는 §3.7.4 rollback 3 시나리오 (자동 healthcheck / 수동 / missing 복구) 와 정합 — 본 §6 은 증상→§3.7.4 참조 1줄, 상세 절차는 §3.7.4 표 유지 (중복 회피).
> **Sprint 20 EP-EXT-WEBHOOK 병행**: Slack/Kakao webhook 장애 대응 (circuit breaker 상태 확인 + 토큰 갱신) 은 USER_MANUAL_v1.1 §6 에서 별도 추가 예정 — 완료 시 본 표에 합본.

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-28 | Claude Code | 초안 — 4 role SOP (PLANNER + STK_USER + IT_OPS + READ_ONLY) + 베타 PIN 8건 + 장애 시 대응 9건. Sprint 10~18 누적 자산 기반. 스크린샷은 `docs/manual/screenshots/` 별도 (Phase 4 추가 권장). |
| 1.2 | 2026-05-29 | Claude Code | Sprint 21 EP-CRUD-MASTER-2 TK-CRUD-6-2 — §3.1 IT_OPS 마스터 데이터에 5 entity 자체 운영 절차 추가 (3.1.4 장비 교체/회전수, 3.1.5 셋팅그룹 1~8, 3.1.6 성형제약·합금형 BR-V14, 3.1.7 라인 호환, 3.1.8 휴일 신년 갱신). MasterHubPage 9 카드 진입 매핑 표. §6 에 BR-V14 합금형 400 대응 1건 추가. v1.1 (S20 webhook §6) 은 병렬 진행 — 완료 시 합본 명시. |
| 1.3 | 2026-06-01 | Claude Code | Sprint 23 EP-MES-ADAPTER-1 ST-MES-5 TK-MES-5-1 — §3.6 IT_OPS MES adapter 설정 신규 (jpa vs http + URL/Token NSSM 등록 + Resilience4j circuit breaker + 트러블슈팅 6 시나리오 + Phase 5+ carry-over). §6 장애 대응에 MES adapter circuit OPEN 1건 추가 (§3.6 ↔ §3.2 Excel 폴백 연계). v1.1 (S20 webhook §6) 합본은 여전히 별도 예약. |
| 1.4 | 2026-06-01 | Claude Code | Sprint 24 S24-A ST-FB-3 TK-FB-3-4 — §3.3 Actuator + Grafana 모니터링 절을 alert 대응 절차까지 확장. §3.3.1 신규 dashboard 5종 매핑 표 (MES adapter Sprint 23 신규 추가) + §3.3.2 baseline alert 4 rule (MesRetryCriticalRate / MesFetchP95High / MesCircuitOpen / MesDegradedSixHours) Runbook 매핑 (infrastructure/observability/grafana/alerts/baseline-alerts.yml 참조) + §3.3.3 Slack escalation 채널 placeholder (Sprint 20 ST-EXT-1 실 webhook 발급 후 활성). §6 장애 대응 표는 v1.3 유지 + §3.3.2 와 cross-reference 만 (중복 회피). §3.6.6 carry-over 에 Sprint 24 ST-FB-3 완료 marker 갱신. |
| **1.5** | **2026-06-01** | **Claude Code** | **Sprint 25 S25-A ST-PROD-3 — §3.7 IT_OPS Blue/Green 무중단 배포 가이드 신규. §3.7.1 배포 mode 선택 (수동 blue-green-switch.sh vs 자동 blue_green_deploy.sh Jenkins) + §3.7.2 첫 배포 전 green 사전 기동 1회 (depends_on reset 정책 — docker compose -f infrastructure/docker-compose.prod.yml up -d backend-green) + §3.7.3 정상 절차 5단계 (Harbor pull → switch → health UP → smoke 401 → Grafana 모니터링) + §3.7.4 rollback 3 시나리오 (healthcheck 실패 자동 / 운영자 수동 blue_green_rollback.sh / 이전 컨테이너 missing 복구) + §3.7.5 트러블슈팅 4건 (NGINX reload / image pull / DB migration / healthcheck timeout) + §3.7.6 Grafana 배포 dashboard cross-ref (Sprint 21 deployment marker annotations). §6 장애 대응 표에 `Blue/Green deploy 실패 → §3.7.4 rollback 참조` 1줄 추가 + §3.7.4 cross-reference 주석 (중복 회피).** |

> **버전 주석**: v1.1 은 Sprint 20 EP-EXT-WEBHOOK (Slack/Kakao webhook, S21 와 병렬) 의 §6 장애 대응 addendum 으로 예약. S21 (마스터 운영) → v1.2, S23 (MES adapter) → v1.3, S24 ST-FB-3 (Grafana alert) → v1.4, S25 ST-PROD-3 (Blue/Green deploy) → v1.5 순으로 마감. Sprint 20 마감 후 v1.6 합본 검토.
