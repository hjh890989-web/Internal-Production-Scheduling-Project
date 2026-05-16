# Project Instructions

This is the cross-tool global rules file (AGENTS.md) supported by Google Antigravity (v1.20.3+), Cursor, and Claude Code.

## Code Style

- Maintain clean, readable, and well-documented code.

---

## 🏗️ 001. Project Overview [아래 내용은 템플릿, 프로젝트에 맞게 항상 수정할 것]

**Vision:** Transform the complex business planning process into a data-driven decision-making journey that reduces failure rates. Act as an intelligent partner to help founders quickly pass funding gates (government grants, loans) and focus on sustainable growth.

**Core Features:**

- Submission Wizard: Step-by-step guide for government/bank forms (100% template compatibility).
- Financial Auto-Engine: Generates 3-year P&L and cash flow from key variables.
- AI Drafting: Context-aware writing assistance with Easy/Expert modes.
- PMF Diagnostic: Analyzes product-market fit and risks based on standard frameworks.
- Docs Export: One-click HWP/PDF export fully compliant with submission standards.

**Project Goals & Success Metrics:**

- Pass Funding Gates (Maximized acceptance rate > 65%).
- TTV < 30 mins to first "submit-ready" draft.
- Security First & Failure Avoidance.

---

## 🛠️ 002. Technical Stack

**Backend Core (Spring Boot):**

- Language: Java 21 (LTS)
- Framework: Spring Boot 4.0.0
- Build Tool: Gradle (Kotlin DSL recommended)
- Database: MySQL 8.x (InnoDB, utf8mb4)
- ORM: Spring Data JPA (Hibernate)

**AI & Document Engine (Python):**

- Language: Python 3.10+
- Framework: FastAPI
- AI Orchestration: LangChain
- LLM Provider: Google Gemini (via Internal Gateway)

**Infrastructure:**

- Docker, Swagger/OpenAPI 3.0, Git

---

## 📋 003. Development Guidelines

**Technology Constraints (C-TEC):**

- Frontend: Vite + React + TypeScript (SPA)
- Architecture: Micro-Service Ready (Core API and AI Engine communicate via REST)

**Performance Standards:**

- Wizard Latency: Step transitions < 800ms (p95)
- Doc Generation: Full draft creation < 10s (p95)

**Development Priorities:**

1. Accuracy: Financial engine logic must be flawless.
2. Compliance: Output formats (HWP/PDF) must match government templates 100%.
3. User Safety: Prevent data loss via aggressive Auto-save (<10s).

