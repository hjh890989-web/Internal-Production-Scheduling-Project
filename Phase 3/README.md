# Phase 3 — 개발 (Development)

본 폴더는 **Phase 3 개발 단계의 산출물 (Sprint 보고·운영 결정·데모 기록)** 을 보존한다.
실 코드는 `backend/`·`frontend/`·`infrastructure/`·`tools/` 등 표준 위치.

---

## 1. Phase 3 구조

| 디렉토리 | 내용 |
|---|---|
| [1.Sprint-Reports/](1.Sprint-Reports/) | Sprint별 완료 보고서 (Sprint 0 ~ N) |
| [2.Decisions/](2.Decisions/) | 개발 중 발생한 운영 결정 (NFR 변경·정책 재정의 등) |
| [3.Demos/](3.Demos/) | Sprint Review 데모 결과 + 시연 자료 |

---

## 2. Sprint 진행 현황

| Sprint | 기간 | 상태 | Epic | 산출 |
|---|---|---|---|---|
| **Sprint 0** | 2026-05-15 ~ 2026-05-19 | ✓ 완료 | EP-00·30·31·32·33·34·99 | 인프라 (Docker 12 서비스) + 인증(Keycloak) + 관측(Prometheus·Loki·Grafana·AlertManager) + CI/CD(Jenkins+Trivy+Sonar) + 백업(pg_basebackup+WAL) + KST·마스터 검증 |
| **Sprint 1** | 2026-05-20 ~ | 🔄 진행 중 | EP-01 (수주통합) + EP-30-2 (RBAC) | Apache POI streaming + SourceClassifier + Multipart 엔드포인트 (TK-01-1-1·2·3 완료, TK-01-1-4 남음) |
| Sprint 2 | TBD | ⏳ 대기 | EP-04·EP-21 (성형 슬롯·좌우·앵글) | — |
| Sprint 3+ | TBD | ⏳ 대기 | EP-EX·EP-NS·NFR 등 | — |

상세 — [1.Sprint-Reports/](1.Sprint-Reports/)

---

## 3. 운영 결정 (Decisions)

| ID | 일자 | 결정 | 영향 문서 |
|---|---|---|---|
| [DECISION-001](2.Decisions/DECISION-001_NFR-SEC-007_v1.5_2026-05-19.md) | 2026-05-19 | NFR-SEC-007 사번 8자리 + PIN 4자리 + 5회/10분 잠금 (v1.4 12자/3종 폐기) | SRS v1.5, Keycloak realm, TK-42-6-3 |

---

## 4. References

- **Phase 2 설계 산출물** — [../Phase 2/](../Phase%202/)
- **Sprint 0 진입 계획** — [../Phase 2/4.Tasks/PLAN-001_Sprint0_Entry_Plan_v1.0.md](../Phase%202/4.Tasks/PLAN-001_Sprint0_Entry_Plan_v1.0.md)
- **WBS v1.2** — [../Phase 2/4.Tasks/TASK-001_WBS_v1.2.md](../Phase%202/4.Tasks/TASK-001_WBS_v1.2.md)
- **GitHub Issue · Project Board** — `gh issue list` / Project #4

---

## 5. 운영 원칙

- Sprint 완료 시 본 폴더에 **완료 보고서 v1.0 발행** (작성자: PM/Tech Lead, 결재: STK-01 + STK-08)
- 운영 결정 발생 시 **DECISION-NNN_<항목>_<일자>.md** 신규 파일 + Index 업데이트
- 데모 결과는 **3.Demos/** 에 screenshot·녹화 링크·KPI 측정값 첨부
- 한국어 콘텐츠 + 영문 파일명 (메모리 rule)

---

## 개정 이력

| 버전 | 일자 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-20 | (Tech Lead) | 초안 — Phase 3 폴더 신설, Sprint 0 완료 후 Sprint 1 진행 시점 정합 |
