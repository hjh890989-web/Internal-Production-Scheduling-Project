# Story Overview — [EP-00] ST-00-1 Docker Compose 환경 구성

**Sprint**: S0 (Phase 0 사전 준비) | **Epic**: EP-00 인프라 기반 셋업 (Foundation) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD (3 SP × 0.7 PD)

---

## Story 목적

> WBS §5.2 EP-00 인용 + SAD ADR-013 인용: "단일 서버·30명 규모에 충분. Kubernetes는 과잉."

본 Story는 **개발(DEV) 환경의 단일 호스트 Docker Compose v2 인프라**를 구성한다. SAD §8 배포 아키텍처에서 정의한 5개 핵심 컨테이너 — **NGINX·PostgreSQL 16·Redis 7·Backend(Spring Boot)·Frontend(NGINX serve)** — 중 인프라 계층 3개(NGINX·PostgreSQL·Redis)와 통합 검증을 담당. Backend·Frontend 골격은 ST-00-2·ST-00-3에서 별도.

**Why 본 Story가 Phase 0 핵심 작업인가**:
- 모든 Sprint 1~5 개발 작업의 **로컬 실행 환경 전제** — 본 Story 미통과 시 EP-01~47 전체 차단 (WBS §12 의존성 DAG)
- Sprint 0 DoD 항목 1번: "`docker compose up`으로 DEV 환경 부팅 성공 (5개 컨테이너)" — 본 Story가 직접 달성
- SAD §8.x STG·PROD 환경 템플릿의 기반 — STG/PROD는 동일 compose 파일 + 환경별 `.env` 차이만

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-00-1-1](TK-00-1-1.md) | PostgreSQL 16 컨테이너 구성 + 초기 스키마 (`app`·`audit`·`master`) | 0.6 | DevOps + DBA | T-I + I | ☐ |
| [TK-00-1-2](TK-00-1-2.md) | Redis 7 컨테이너 구성 (세션·캐시·pub/sub) | 0.4 | DevOps | T-I + I | ☐ |
| [TK-00-1-3](TK-00-1-3.md) | NGINX 컨테이너 구성 (리버스 프록시·TLS termination·WebSocket·static serving) | 0.6 | DevOps | T-I + I | ☐ |
| [TK-00-1-4](TK-00-1-4.md) | `docker-compose.yml` v2 통합 + `docker compose up` health check 5개 부팅 검증 | 0.5 | DevOps | T-I + D | ☐ |

> **선행 의존**: 없음 (Phase 0 작업, EP-99와 병렬 가능)
> **후행 차단**: 모든 Sprint 1~5 개발 작업 (EP-01·02·03·04·...·47) — DEV 환경 전제

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] **`docker compose up -d` 한 번에 5개 컨테이너(PostgreSQL·Redis·NGINX + Backend·Frontend placeholder) 부팅 성공** — Sprint 0 DoD 항목 1
- [ ] **모든 컨테이너 healthcheck 통과** (PG `pg_isready`·Redis `PING`·NGINX HTTP 200)
- [ ] **`docker compose down` + `up` 재기동 시 데이터 보존** (PostgreSQL volume + Redis AOF 옵션)
- [ ] **개발자 노트북 사양 가이드** (≥8 GB RAM, ≥20 GB SSD) 문서화
- [ ] STG/PROD 환경 차이 명시 (환경별 `.env` 변수 + secrets 관리)
- [ ] **REQ-NF-SEC-001** (사내망 전용) 정합: NGINX `listen 0.0.0.0`이지만 DEV는 localhost only, STG/PROD는 사내망 IP 화이트리스트
- [ ] Sprint Review 데모: `docker compose up`부터 NGINX → BE → PG/Redis 통신 health check까지 시연

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-00 ST-00-1
- **SAD ADR**:
  - `Phase 2/3.SAD/SAD-001_Production_Scheduling_System_v1.0.md` §10 **ADR-010** (PostgreSQL 16 + Redis 7 — JSONB·LISTEN/NOTIFY·partial index·MVCC·100% OSS)
  - §10 **ADR-013** (Docker Compose — Kubernetes 미채택, 단일 서버 30명 규모 적정)
- **SAD §3 컨테이너 뷰**: 5개 컨테이너 정의 (NGINX·BE·FE·PG·Redis + 옵션 Keycloak·Prometheus 등)
- **SAD §5.6** 컨테이너·배포: 베이스 이미지 (`nginx:1.25-alpine`·`postgres:16-alpine`·`redis:7-alpine`), Compose v2 + Ansible playbook, Blue-Green 배포 패턴
- **SAD §8 배포 아키텍처**: DEV (개발자 노트북) · STG (수동 승인) · PROD (수동 승인) 환경 정의
- **SRS REQ-NF**:
  - REQ-NF-PER-004 (WebSocket PUSH p95 ≤2초) → NGINX WebSocket proxy 설정
  - REQ-NF-REL-001 (영업시간 가용성 ≥99.5%) → Docker healthcheck + restart policy
  - REQ-NF-REL-002 (ACID) → PG volume 영속성
  - REQ-NF-SEC-001 (사내망 전용) → NGINX listen 정책
  - REQ-NF-SEC-006 (TLS 1.2+) → NGINX TLS 1.3 + HSTS
  - REQ-NF-COS-001 (잉여 서버 우선) → 단일 호스트 Compose
- **SAD-RSK**:
  - SAD §11 **SAD-RSK-002** "Docker Compose 단일 서버 SPOF" → NAS 백업 + 분기 DR 드릴
  - SAD §11 **SAD-RSK-007** "잉여 서버 사양 부족" → Phase 0 부하 테스트 후 IT 예산 협의
- **연관 Story (병렬)**:
  - [ST-99-1](../../EP-99/ST-99-1/_Story_Overview.md) (성형 마스터 — 독립)
  - [ST-99-2](../../EP-99/ST-99-2/_Story_Overview.md) (압출 마스터 — 독립)
  - 후속: ST-00-2 (Spring Boot 골격 — 본 Story의 PG·Redis 사용), ST-00-3 (React 골격 — NGINX serve)

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | ST-99-1·99-2 패턴 재사용으로 초안 작성. 성격: 인프라(컨테이너) — 마스터 검증과 다른 도메인 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.2 EP-00 ST-00-1 + SAD ADR-010·013·§3·§5.6·§8 기반. Task 기반 분해 v1 세 번째 적용 (다른 성격 Story = 인프라). |
