# Phase 4 (베타 운영) 진입 계획 v1.1 — Sprint 7 carry-over 풀 스택 마감 반영

**Phase**: 4 (베타 운영) | **목표 기간**: 2026-05-24 ~ 2026-06-30 (~5주)
**상태**: 🔄 진입 게이트 (게이트 9/9 통과) | **작성**: 2026-05-23
**상위 참조**: [Phase-3_Completion_v1.0](../Phase%203/2.Phase-Completion/Phase-3_Completion_v1.0.md) | **전판**: [v1.0](Phase-4_EntryPlan_v1.0.md)

> v1.0 (Sprint 0~6 + Phase 4 인계 자산) 이후 **Sprint 7 carry-over 풀 스택 마감 (BR-V12·V13 REST + Planner UI + REST IT + tooling)** 반영.
> 진입 게이트 9/9 통과 — 최종 승인 가능.

---

## 1. v1.0 → v1.1 변경 요지

| 항목 | v1.0 | v1.1 |
|---|---|---|
| Phase 3 commit | 153 | **~175** (+22 — Sprint 7 carry-over 6 + 마감 cleanup + 보고서 갱신) |
| Phase 3 산출 | Sprint 0~6 | Sprint 0~6 + **Sprint 7 carry-over 풀 스택** |
| 진입 게이트 | 5개 | **9개** (+ VSCode 문제 탭 0 + Frontend vitest +4 + BR-V12·V13 풀 스택 + REST IT) |
| 5 베타 시나리오 | BS-01~05 | BS-01~05 + **BR-V12·V13 capacity-queue UI 진입 가능 명시** (BS-06 후보) |
| Carry-over 표 | BR-V12·V13 마감 후 활성 | BR-V12 추가 요청 큐 승인 워크플로우 + BR-V13 Grafana panel (Sprint 8+) |

---

## 2. Phase 4 목표 (v1.0 그대로 유지)

> "사내 사용자 (~10명) 대상 베타 운영. PROD cutover 전 5주 stress test + 사용자 피드백 +
>  성능 NFR 실 측정 + DR 검증."

핵심 마일스톤 — STG 부팅 + Keycloak SSO + 베타 5 시나리오 + k6 실 측정 + PROD cutover 게이트.

---

## 3. 5 phase 마일스톤 (v1.0 그대로)

| Phase | 기간 | 항목 | 종료 게이트 |
|---|---|---|---|
| **4-A** STG 부팅 | Week 1 (5d) | Docker Compose Blue/Green STG + Keycloak SSO + 시드 데이터 + k6 실 측정 | STG 가동 + IT_OPS 접근 |
| **4-B** 베타 시나리오 | Week 2 (5d) | 5 사용자 시나리오 (정상·예외·복원·override·cascade) + **BR-V12·V13 capacity-queue UI 진입** | 시나리오 100% 통과 |
| **4-C** 사용자 교육 | Week 3 (5d) | Planner / STK_USER 페르소나 별 운영 매뉴얼 + 영상 + Q&A | 4 페르소나 교육 |
| **4-D** DR + 보안 검증 | Week 4 (5d) | pg_basebackup PITR + 사내 IdP LDAP/AD sync + Alertmanager Slack | DR 시나리오 통과 |
| **4-E** PROD 진입 결정 | Week 5 (5d) | Cutover 게이트 회의 + 운영팀 인수 + Phase 5 (PROD) 진입 | 승인 |

---

## 4. Phase 4 진입 게이트 충족 (Phase 3 → Phase 4 — v1.1 갱신)

- [x] Phase 3 Sprint 0~6 100% 완료 + **Sprint 7 carry-over 풀 스택 마감**
- [x] 47 Epic 거버넌스 + 안정성 + 관측성 완비
- [x] 9 Modulith 모듈 + **34 Flyway** (+V033 PRODUCT_PRIORITY + KD_ORDER) + 19 KPI 영속
- [x] **~175 commit** · 머지 충돌 0 · AI harness 안정
- [x] Backend 회귀 **788 tests** + Frontend **58 vitest** (+4 capacityOverflow.types) + Playwright 226 등록
- [x] **9 핵심 BR + 2 deferred BR-V12·V13 UI 진입점 마감** (`/vc/capacity-queue` 라우트 활성)
- [x] **REST IT 보강** — CapacityOverflowControllerIT (PLANNER RBAC + 401/403 + happy path)
- [x] **VSCode 문제 탭 0** (.markdownlint + .cspell config 적용)
- [x] **outward 문서 동기화** — README v1.0.1 + CHANGELOG v1.0.1 + Sprint-7 v1.1 + PERF-002 v1.1

→ **Phase 4 진입 승인 가능 — 9/9 게이트 통과**.

---

## 5. STG 환경 명세 (Phase 4-A) — v1.0 그대로

(Docker Compose Blue/Green + env var 명세는 v1.0 §4 참조.)

### 신규 STG 검증 — BR-V12·V13 capacity-queue UI

```bash
# Phase 4-A STG 부팅 후 — Planner 페르소나로 진입
curl -X POST https://schedule.intranet/api/v1/schedule/vc/capacity-overflow/split \
    -H "Authorization: Bearer ${PLANNER_JWT}" \
    -H "Content-Type: application/json" \
    -d '{"required": {"29673-2R060": 60, "28422-2M800": 50}, "dailyCapa": 90}'
# 응답 — accepted + requestQueue + totalAccepted + totalQueued

curl -X POST https://schedule.intranet/api/v1/schedule/vc/capacity-overflow/supplement \
    -H "Authorization: Bearer ${PLANNER_JWT}" \
    -H "Content-Type: application/json" \
    -d '{"hoseId": "29673-2R060", "shortage": 80}'
# 응답 — supplemented + consumed[] (audit 자동, @Auditable)

# 또는 브라우저 — https://schedule.intranet/vc/capacity-queue (Planner 로 로그인 후)
```

활성 조건 — DI-07 PRODUCT_PRIORITY + DI-08 KD_ORDER 마스터 입력 (Phase 4-B 초반).

---

## 6. 5 베타 시나리오 (Phase 4-B) — v1.0 그대로 + BS-06 후보

| # | 시나리오 | 페르소나 | 검증 |
|---|---|---|---|
| 1 | **정상 1주 horizon** — 수주 import → VC 자동 스케줄 → Planner confirm → EX cascade | Planner | 1500 row 30 col 정상 생성 + Excel export 동일 |
| 2 | **충돌 + alternative** — 마스터 충돌 입력 → ≥3 distinct alternative + Planner 선택 | Planner + STK_USER | 충돌 분류 100% + ranking 정렬 |
| 3 | **VC 변경 cascade** — Planner override → vc.changed → ex partial replan → STOMP push → 매트릭스 갱신 | Planner | p95 ≤ 2초 + audit 자동 |
| 4 | **마스터 복원** — 잘못된 vc_constraint 입력 → audit timeline forensic → 시점 snapshot 확인 | IT_OPS | JSONB 역재생 정확 |
| 5 | **일중 락 override** — BR-V07 위반 시도 → trigger reject → override 사유 입력 → audit 캡쳐 | Planner | reason+actor 강제 + DO-04 영업일 키 |
| **6 (후보)** | **🆕 BR-V12·V13 capa 큐 + KD 보충** — DI-07/08 입력 → Planner `/vc/capacity-queue` Tab1 split 미리보기 → Tab2 1클릭 보충 → audit 자동 | Planner + IT_OPS | priority rank ASC + KD remaining_qty 감소 + status 자동 전이 |

→ BS-06 은 활성 조건 충족 시 (Phase 4-B 후반) 검토.

---

## 7. DR + 보안 검증 (Phase 4-D) — v1.0 그대로

(v1.0 §6 참조 — DR 시나리오 5개 + 보안 6항목.)

---

## 8. PROD Cutover 게이트 (Phase 4-E) — v1.0 그대로

(v1.0 §7 참조 — 11 게이트.)

---

## 9. Phase 4 후속 — Phase 5 (PROD) 진입 사전 작업 (v1.0 그대로)

(v1.0 §8 참조.)

---

## 10. 위험 + 완화 전략 (v1.0 그대로 + V12·V13 항목)

| 리스크 | 영향 | 완화 |
|---|---|---|
| Keycloak SAML 통합 실패 | 인증 차단 | local fallback (사내 임시 ID 매핑) + OIDC 우선 |
| k6 STG 실 측정 p95 미달 | PROD 진입 차단 | QueryDSL projection + EntityGraph N+1 진단 |
| 베타 사용자 학습 곡선 | Sprint 5 UI 사용도 ↓ | 페르소나별 영상 + Q&A 세션 × 4회 |
| pg_basebackup NAS 용량 | DR 백업 실패 | 90일 보존 + 압축 + 월별 incremental |
| 사용자 BR-V07 override 남용 | 일중 락 의미 손실 | Slack alert (override 1일 5회↑ 발생 시 IT_OPS 통보) |
| AG Grid Enterprise 라이센스 만료 | 빌드 차단 | 라이센스 키 만료일 모니터 + Renew Q3 |
| 🆕 BR-V12·V13 활성 조건 미입력 | UI 진입 시 빈 결과 | Phase 4-B 초반 DI-07/08 입력 절차 명시 — BS-06 게이트 |
| 🆕 capacity-overflow REST RBAC 우회 시도 | Planner 외 승인 | CapacityOverflowControllerIT 401/403 회귀 + Keycloak role 매핑 점검 |

---

## 11. Phase 4 종료 후 — Phase 5 (PROD) + Phase 6 (운영) 로드맵 (v1.1 갱신)

| Phase | 기간 | 핵심 |
|---|---|---|
| **Phase 5** PROD cutover | Q3 2026 | Blue → Green 실 절환 + PROD 사용자 인계 |
| **Phase 6** 운영 + carry-over | Q4 2026 ~ | Sprint 8+ — **BR-V12 추가 요청 큐 승인 워크플로우** (UI commit/reject + endpoint + audit), **BR-V13 Grafana panel** (IT_OPS KD remaining_qty 시각화), ML 추천, 모바일 |

---

## 12. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Phase 4 (STG 베타 5주, 5 phase mile stone) 진입 계획 + PROD cutover 게이트 |
| 1.1 | 2026-05-23 | Claude Code | Sprint 7 carry-over 풀 스택 마감 반영 — 게이트 5→9, BS-06 후보 (capacity-queue UI), REST IT 보강, Phase 6 carry-over 갱신 (V12 승인 워크플로우 + V13 Grafana) |
