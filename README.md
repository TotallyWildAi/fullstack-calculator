# Full-Stack Calculator

A full-stack calculator application autonomously built by a swarm of AI coding agents.

## How This Was Built

This project was created entirely by an autonomous agent swarm system designed and built by **Dmitry Kislov**.

The system uses a multi-agent architecture with specialized roles:

- **Orchestrator Agent**: Manages the task graph, resolves dependencies, and dispatches work to specialized agents.
- **SE Worker Agent**: Executes the Investigation → Solve → Build loop. Reads the codebase, generates patches, compiles, tests, and iterates on failures with error context.
- **Code Review Agent**: Reviews each task's output against acceptance criteria. Can request rework if standards are not met.

Each task follows this lifecycle:

1. **Investigation**: Agent reads relevant files and understands the context.
2. **Solve**: Agent generates patches to implement the task.
3. **Build**: Agent compiles and runs tests to verify correctness.
4. **Code Review**: Agent validates output against acceptance criteria.
5. **Merge**: Approved changes are merged to main branch.

If a build fails, the agent retries with error context. If code review rejects the work, the task is reworked from scratch. The agent uses git worktrees for task isolation — each task works on its own branch. All agents use Claude (Anthropic) as the underlying LLM.

## Build Metrics

| Metric | Value |
| --- | --- |
| Total tasks completed | 22 |
| Total LLM calls | 127 |
| Total tokens consumed | 1,249,619 (~1.25M) |
| Pipeline wall-clock time | 144 seconds (2.4 minutes) |
| Average per task | 5.8 calls, 56,800 tokens, 47 seconds |
| First-try success rate | 95% |
| Human intervention | Zero |

## Project Overview

A full-stack calculator with the following components:

- **CLI**: Command-line interface for arithmetic operations
- **REST API**: Spring Boot backend with JWT authentication
- **React UI**: Modern single-page application with TypeScript
- **Persistence**: PostgreSQL database with calculation history
- **Security**: JWT-based stateless authentication with BCrypt password hashing

Supported operations: addition, subtraction, multiplication, division. All calculations are persisted with timestamps and user attribution.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA |
| Authentication | JWT (JSON Web Tokens), BCrypt |
| Frontend | React 19, TypeScript, Vite, RTK Query |
| Database | PostgreSQL 16 |
| Testing | JUnit 5, Testcontainers, Vitest, React Testing Library |
| Build & Deploy | Maven, Docker (multi-stage), Docker Compose |

## Architecture

```
┌─────────────┐
│   Browser   │
│ (React SPA) │
└──────┬──────┘
       │ HTTPS
       ▼
┌──────────────────────┐
│  Spring Boot REST    │
│  API (Port 8080)     │
│  - JWT Auth Filter   │
│  - Calculator Svc    │
│  - User Management   │
└──────────┬───────────┘
           │ JDBC
           ▼
┌──────────────────────┐
│   PostgreSQL 16      │
│   (Port 5432)        │
│   - Users table      │
│   - Calculations tbl │
└──────────────────────┘
```

**Security Model**: JWT-based stateless authentication. Passwords are hashed with BCrypt. Each request includes a Bearer token in the Authorization header. The JwtAuthFilter validates tokens on every request.

**Layered Architecture**:
- **Controller Layer**: REST endpoints, request/response mapping
- **Service Layer**: Business logic (Calculator service, User service)
- **Repository Layer**: Spring Data JPA for database access
- **Database Layer**: PostgreSQL with schema migrations

## Getting Started

### Quick Start with Docker Compose

```bash
docker compose up -d --build --wait
```

Then open http://localhost:8080 in your browser.

Default credentials:
- Username: `testuser`
- Password: `SecurePass123!`

### Manual Build

**Prerequisites**: Java 21, Maven 3.9+, Node.js 18+, PostgreSQL 16

```bash
# Build backend
mvn clean package

# Build frontend
cd frontend && npm install && npm run build

# Run backend (requires PostgreSQL running on localhost:5432)
java -jar target/calculator-app.jar
```

## API Reference

| Method | Path | Auth Required | Description |
| --- | --- | --- | --- |
| POST | `/auth/login` | No | Authenticate user, returns JWT token |
| POST | `/auth/register` | No | Register new user account |
| POST | `/api/calculate` | Yes | Perform arithmetic operation |
| GET | `/api/history` | Yes | Retrieve calculation history for user |
| DELETE | `/api/history/{id}` | Yes | Delete a calculation record |

All authenticated endpoints require `Authorization: Bearer <token>` header.

## Testing

### Backend Tests

```bash
mvn test
```

Runs unit tests, integration tests, and Testcontainers-based database tests. Coverage includes:
- Calculator service logic
- User authentication and JWT validation
- Repository layer with real PostgreSQL (via Testcontainers)
- REST controller endpoints

### Frontend Tests

```bash
cd frontend && npm test
```

Runs Vitest unit tests and React Testing Library component tests.

## Infrastructure

**Docker Build**: Multi-stage Dockerfile optimizes image size. Backend stage compiles with Maven, frontend stage builds with Vite, final stage runs Spring Boot application.

**Docker Compose**: Orchestrates two services:
- `app`: Spring Boot application (port 8080)
- `postgres`: PostgreSQL 16 database (port 5432)

Health checks ensure PostgreSQL is ready before the application starts. Volumes persist database data across container restarts.

