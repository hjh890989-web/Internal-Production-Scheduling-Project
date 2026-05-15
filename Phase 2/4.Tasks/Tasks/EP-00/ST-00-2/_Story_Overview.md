# Story Overview — [EP-00] ST-00-2 Spring Boot 모듈러 모놀리식 골격

**Sprint**: S0 (Phase 0 사전 준비) | **Epic**: EP-00 인프라 기반 셋업 (Foundation) | **Priority**: Must
**SP 합계**: 3 | **PD 추정**: ~2.1 PD (3 SP × 0.7 PD)

---

## Story 목적

> SAD §1.6 아키텍처 스타일 인용: "**모듈화 모놀리식 (Modular Monolith)** — 단일 백엔드 프로세스 내에 모듈 경계(`order`·`vc`·`ex`·`audit`·`notify`). 각 모듈은 별도 패키지·DB 스키마 분리, 추후 분할 용이."

본 Story는 **Backend 애플리케이션의 골격**을 셋업한다. SAD §4 컴포넌트 뷰 + ADR-008 (Java 21 LTS + Spring Boot 3.3 + Spring Modulith 1.2)에서 정의한 **7개 모듈** (`order`·`vc`·`ex`·`master`·`audit`·`notify`·`common`)을 Gradle 멀티모듈 + Spring Modulith로 구현하고, **ArchUnit으로 모듈 경계 위반을 빌드 타임에 차단**한다.

**Why 본 Story가 Phase 0 핵심 작업인가**:
- 모든 Sprint 1~5 백엔드 작업이 **본 Story의 모듈 구조 위에 구현**됨 — 골격 미완성 시 EP-01·02·03·04·...·47 전체 차단
- Sprint 0 DoD 항목 2번: "Spring Modulith ArchUnit 테스트 통과 (모듈 경계 위반 0)" — 본 Story가 직접 달성
- ADR-008 의사결정 검증 — Java 21 + Spring Boot 3.3 + Spring Modulith 조합이 실제로 사내 환경에서 빌드되는지 사전 확인
- NFR-COM-004 (API 전방 호환성 — Phase 2 MRP 결합 가능) 기반 — 모듈 경계가 향후 마이크로서비스 분할 옵션 보존

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-00-2-1](TK-00-2-1.md) | Gradle 8 멀티모듈 골격 + Spring Boot 3.3 + 의존성 BOM | 1.0 | 백엔드 | T-U + I | ☐ |
| [TK-00-2-2](TK-00-2-2.md) | Spring Modulith 1.2 모듈 경계 정의 (`order`·`vc`·`ex`·`master`·`audit`·`notify`·`common`) | 0.7 | 백엔드 | T-U + T-I | ☐ |
| [TK-00-2-3](TK-00-2-3.md) | ArchUnit 모듈 경계 테스트 + Spring Modulith 검증 통합 | 0.4 | 백엔드 + QA | T-U + A | ☐ |

> **선행 의존**: ST-00-1 (DEV 환경 — PG·Redis 컨테이너 필요)
> **후행 차단**: 모든 Sprint 1~5 백엔드 Epic (EP-01·02·03·04·...·47) — 본 Story의 모듈 구조 위에 구현

---

## Story 레벨 DoD (모든 Task 완료 후)

- [ ] 모든 Task DoD 통과 (각 TK 파일 `:checkered_flag:` 참조)
- [ ] `./gradlew build` 한 번에 성공 (clean build ≤3분, NFR 가이드)
- [ ] **7개 모듈 (`order`·`vc`·`ex`·`master`·`audit`·`notify`·`common`)** Gradle subproject로 분리 완료
- [ ] Spring Modulith 1.2 적용 + `Modulithic` 어노테이션 + 모듈 매니페스트 생성
- [ ] **ArchUnit 테스트 통과 — 모듈 경계 위반 0건** (Sprint 0 DoD 항목 2 직접 달성)
- [ ] 의존성 그래프 시각화 (`./gradlew :app:modulithDocs` 또는 동등) — SAD §4 다이어그램과 일치 확인
- [ ] `docker compose up` 후 backend 컨테이너가 placeholder가 아닌 **실제 Spring Boot 애플리케이션**으로 부팅 (ST-00-1 TK-00-1-4의 placeholder 교체)
- [ ] Spring Boot Actuator `/actuator/health` 200 OK
- [ ] Spring Modulith 모듈 매니페스트가 SAD §4 컴포넌트 뷰와 1:1 매핑됨
- [ ] Sprint Review 데모: `./gradlew build` + ArchUnit 통과 + `/actuator/health` + 의도적 모듈 경계 위반 시 빌드 실패 시연

---

## References (공통 — 모든 Task가 참조)

- **WBS Story**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §5.2 EP-00 ST-00-2
- **SAD ADR**:
  - **ADR-008** (Java 21 LTS + Spring Boot 3.3 + Spring Modulith — 한국 제조 사내 개발자 풀, Apache POI·Spring 생태계, LTS 8년)
- **SAD §4 컴포넌트 뷰**: Backend Modulith 내부 4 레이어 — Web Layer (REST·WebSocket·Security Filter), **Application Modules (`order`·`vc`·`ex`·`master`·`audit`·`notify`)**, Scheduling Engine Core (ExcelParser·RuleEngine·Optimizer·YieldCalculator), Infrastructure Layer (JPA Repos·Cache·EventBus·MES Adapter·FileSvc)
- **SAD §4.1 모듈 경계 원칙**:
  - 모듈 간 직접 호출 금지 (다른 모듈 내부 클래스 참조 불가) → **Spring Modulith 또는 ArchUnit 테스트**
  - 모듈 간 통신 = **Event Bus만** (비동기 이벤트 `OrderChangedEvent`·`VcConfirmedEvent`) → `@TransactionalEventListener`
  - 공통 도메인 = `common` 패키지 (DTO·Enum·Exception) → 의존성 그래프 순환 금지
  - Audit는 모든 커밋에 강제 (`@Auditable` AOP + DB 트리거) → BR-X02
- **SAD §5.1 백엔드 스택**:
  - 언어: Java 21 LTS
  - 프레임워크: Spring Boot 3.3.x
  - 빌드: Gradle 8.x (Kotlin DSL)
  - 모듈화: Spring Modulith 1.2.x
  - API 문서: springdoc-openapi 2.5.x
  - ORM: Spring Data JPA + Hibernate 6.x
  - 검증: Hibernate Validator 8.x (Jakarta Validation JSR-380)
  - 테스트: JUnit 5 + Mockito + Testcontainers
- **SRS REQ-NF**:
  - **REQ-NF-COM-004** (API 전방 호환성 — Phase 2 MRP 결합 가능) → Spring Modulith 모듈 경계 + API 버저닝
  - **REQ-NF-PER-002** (1주 성형 후보 ≤5분 p95) → Application Modules 분리로 병렬 최적화 가능 구조
- **연관 Story**:
  - 선행: [ST-00-1](../ST-00-1/_Story_Overview.md) (Docker Compose — PG·Redis 필요)
  - 후행: [ST-00-3](../ST-00-3/) (React 프론트엔드 — 본 Story의 `/api/*`·`/ws/*` 엔드포인트 호출)

---

## 진행 이력

| 일자 | Task | 상태 변경 | 비고 |
|---|---|---|---|
| 2026-05-15 | _Story_Overview | ☐ 신규 | 초안 작성. 성격: 백엔드 골격(Java + Gradle + Spring Modulith) — ST-99-1/99-2(데이터 검증)·ST-00-1(인프라 컨테이너)과 다른 4번째 도메인 |

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 작성 — WBS v1.2 §5.2 EP-00 ST-00-2 + SAD ADR-008·§4·§4.1·§5.1 기반. Task 기반 분해 v1 네 번째 적용 (백엔드 골격 — 양식 강건성 추가 검증) |
