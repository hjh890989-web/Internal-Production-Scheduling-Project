# Project

이 문서는 Claude Code가 작업 시작 시 자동으로 로드하는 프로젝트 컨텍스트입니다.

---

## 1. Project Overview

### Vision
프로젝트의 비전과 해결하려는 문제를 여기에 기술합니다. (템플릿 — 실제 프로젝트에 맞게 수정하세요.)

### Core Features
- Feature 1: …
- Feature 2: …
- Feature 3: …

### Target Audience
- Primary users: …
- Secondary users: …

### Project Philosophy
- 높은 유지보수성과 확장성 있는 아키텍처를 우선한다.
- 시니어 엔지니어링 관행과 표준을 따른다.
- 포괄적이고 최신 상태를 유지하는 문서를 남긴다.

---

## 2. Tech Stack

### Frontend
- Framework: React (Vite 템플릿)
- Styling: Tailwind CSS

### Backend
- Core: Spring Boot 3.x (Java 17+)
- Template Engine: Thymeleaf
- Caching: Redis (Lettuce / Redisson)
- Message Queue: Apache Kafka

### Authentication
- OAuth2 Providers: Google, KakaoTalk

### External Services
- AI/ML: Hugging Face API, OpenAI API

### Mobile (선택)
- Flutter + Riverpod + Supabase

---

## 3. Development Guidelines

### Version Control
- System: Git
- Repository: GitHub (GitHub APIs 활용)

### Development Priorities
1. 위 Tech Stack에 명시된 구성요소를 우선 활용한다.
2. 문서화와 커뮤니티가 튼튼한 프레임워크를 선호한다.
3. 검증된 디자인 패턴과 모범 사례를 적용한다.

### Architecture Principles
- 모듈화된 설계 (유지보수 용이성)
- 확장 가능한 인프라
- 안전한 데이터 처리
- 성능 최적화

### Code Comments
- 의미 있는 주석만 작성한다 (WHY 중심, WHAT는 코드로 표현).
- 사용되지 않거나 쓸모없어진 주석은 즉시 제거한다.
- 주석은 가독성과 유지보수성을 높여야 한다.

### Problem Solving
- 에러/예외 처리가 필요하면 `/fix-error` 슬래시 커맨드로 구조화된 7단계 진단을 수행한다.

---

## 4. Subagent & Command Routing

작업 성격에 따라 적합한 서브에이전트 또는 슬래시 커맨드가 자동으로 위임됩니다.
수동 호출이 필요하면 `> use the <agent-name> subagent` 또는 `/<command>` 형태로 지시하세요.

### Subagents (`.claude/agents/`)
| 에이전트 | 사용 시점 |
|---|---|
| `java-spring` | Java/Spring Boot 컨트롤러·서비스·리포지토리·REST API 작업 |
| `gradle` | `build.gradle` / Groovy 빌드 설정, 의존성·태스크 구성 |
| `jpa-querydsl` | JPA + QueryDSL 동적 쿼리, BooleanBuilder, 리포지토리 |
| `spring-redis` | Redis 클라이언트 선택(Lettuce vs Redisson), 캐싱·분산락 |
| `kafka-pipeline` | Kafka 토픽 설계, 파티셔닝, 컨슈머 멱등성 |
| `kafka-saga` | Kafka 기반 Saga 오케스트레이션, 보상 트랜잭션 |
| `react-frontend` | React + Vite + Tailwind 프론트엔드 구현 |
| `flutter-app` | Flutter + Riverpod + Supabase 모바일 앱 구현 |

### Slash Commands (`.claude/commands/`)
| 커맨드 | 목적 |
|---|---|
| `/fix-error` | 에러/예외 발생 시 7단계 구조화 진단·수정 |
| `/setup-env` | 빌드 프로세스 및 환경변수 설정 절차 |
| `/gitflow-commit` | Git Flow 준수 커밋·PR 자동화 |

---

## 5. 참고
- 새 규칙을 추가할 때: 항상 적용은 이 파일, 도메인 지식은 서브에이전트, 절차·프로세스는 슬래시 커맨드에 작성합니다.
