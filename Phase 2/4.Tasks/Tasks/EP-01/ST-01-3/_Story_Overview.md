# Story Overview — [EP-01] ST-01-3 폴더 폴링 watcher (Could)

**Sprint**: S1 (수주 통합 기반) | **Epic**: EP-01 엑셀 통합 Parser (M-01) | **Priority**: **Could**
**SP 합계**: 3 | **PD 추정**: ~2.1 PD (3 SP × 0.7 PD)

---

## Story 목적

> WBS §5.1 EP-01 인용: "ST-01-3 — TK-01-3-1 watchdog 폴링 60s, TK-01-3-2 큐 등록 + audit, TK-01-3-3 fs close 이벤트 핸들"
> SRS REQ-FUNC-OC-015 인용: "시스템은 설정된 폴더에 영업이 드롭한 워크북을 폴링하여 수동 업로드 없이 ingest 할 수 있어야 한다. watch 폴더에 배치된 파일이 60초 이내 큐 등록."
> US-07 인용: "As a 영업·관리부서 직원, I want 기존 엑셀 작성 방식을 유지하면서 시스템에 자동 송신되기를, So that 새 도구를 배우지 않아도 통합 마스터가 갱신된다."

본 Story는 **영업·관리 부서가 사내 파일 공유 폴더(SMB·NAS 마운트)에 .xlsx를 drop하면 시스템이 자동으로 ingest**하는 watcher를 구현한다. 사용자(P1·P4)가 수동 업로드 단계를 거치지 않아도 됨 — **영업 부서의 기존 워크플로우 무변경**이 핵심 가치.

**왜 Could 등급인가** (WBS §17.3, SRS REQ-FUNC-OC-015):
- ST-01-1·1-2가 충족된 상태에서 **사용자 직접 업로드로 충분히 운영 가능** — watcher는 편의 자동화
- Sprint 1 DoD 필수 항목 아님 (Must 12 + Should 5에 미포함)
- Phase 1.1·1.2 안정화 후 도입해도 무리 없음
- **본 Story 작성은 EP-01 완결성과 향후 Sprint 5 또는 Phase 2 활성화 대비**

**도입 시 가치**:
- P1 김정훈 주임의 "월요일 아침 3번 클릭"을 0번으로 단축
- US-07 영업 부서 수용성 향상 (학습 비용 0)
- Phase 0 EXP-1 베이스라인 측정 시 추가 시간 단축 효과 측정 가능

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-01-3-1](TK-01-3-1.md) | WatchService NIO + Spring `@Scheduled` 60s 폴백 폴링 | 0.8 | Backend | T-I + T-S | ☐ |
| [TK-01-3-2](TK-01-3-2.md) | 파일 ingest 큐 등록 + audit 로그 + 중복 처리 | 0.7 | Backend | T-I + I | ☐ |
| [TK-01-3-3](TK-01-3-3.md) | fs close 이벤트 안정성 (파일 락·크기 안정화 검증) | 0.6 | Backend | T-I + A | ☐ |

> **선행 의존**: ST-01-1·1-2 (Import 엔드포인트·매핑 동작 — watcher가 내부 호출)
> **후행 차단**: 없음 (Could 기능)
> **병렬 가능**: ST-02-1·ST-03-1 (Sprint 1의 다른 Must Story들과 병렬 작업 가능)

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] **설정 폴더에 .xlsx drop 후 60초 이내 ingest 큐 등록** — REQ-FUNC-OC-015 / TC-OC-015
- [ ] **파일 쓰기 완료 전 ingest 방지** — fs close 이벤트 또는 파일 크기 2회 연속 동일 검증 (REQ-NF-REL-002 ACID 정신)
- [ ] **중복 파일 처리** — 같은 파일명·해시·크기 ingest 시도 시 첫 번째만 처리, 후속은 skip + audit
- [ ] **활성/비활성 토글** — `application.yaml`의 `scheduling.watcher.enabled` 플래그로 운영 중 on/off 가능
- [ ] **권한 분리** — watcher가 호출하는 내부 import는 시스템 계정 토큰(전용 service-account) 사용
- [ ] **장애 격리** — watcher 실패가 사용자 직접 업로드 경로(TK-01-1-3) 영향 없음
- [ ] 단위 + 통합 테스트 ≥ 80% 커버리지
- [ ] Sprint Review 데모 (선택, Could): 파일 drop → 60초 내 import 큐 + audit row 생성 시연

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.1 EP-01 ST-01-3
- **SAD**:
  - `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` §5.1 Backend — Spring `@Scheduled` + Shedlock (야간 배치)
  - §3.1 EXT-SYS-01 (영업·관리 엑셀 원본, HTTPS 파일 업로드)
  - §5.3 데이터 — 로컬 파일 저장소 bind-mount `/data/files`
  - §10 ADR-008 (Java 21 + Spring Boot)
- **SRS REQ-FUNC**:
  - **REQ-FUNC-OC-015** (Could — 폴더 폴링 자동 송신)
- **SRS REQ-NF**:
  - **REQ-NF-REL-002** (ACID — 중복 ingest 차단)
  - **REQ-NF-SEC-001** (사내망 전용 — watch 폴더 SMB 마운트 사내 한정)
  - **REQ-NF-OPS-001** (구조화 로그 — watcher 이벤트 JSON)
- **PDD-01**: §4 Activity A6 "역-Export" 인접 (양방향 자동화 후보)
- **US**: US-07 영업 자동 송신
- **TestPlan**:
  - **TC-OC-015** (Could — watch 폴더 60초 이내 큐 등록)
  - **TC-XT-003** (폴더 watch 60초 이내 큐) — 동일 기능
- **연관 Story**:
  - 선행: [ST-01-1](../ST-01-1/_Story_Overview.md), [ST-01-2](../ST-01-2/_Story_Overview.md)
- **외부 자료**:
  - Java NIO WatchService: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/WatchService.html
  - Spring `@Scheduled`: https://docs.spring.io/spring-framework/reference/integration/scheduling.html

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | EP-01 ST-01-3 초안 (Could 등급, EP-01 완결용) |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.1 EP-01 ST-01-3 + SAD §5.1·§3.1 + REQ-FUNC-OC-015 + US-07 + TC-OC-015·XT-003 기반 |
