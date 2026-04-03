# Calculator Application

## Project Overview

A full-stack calculator web application with user authentication and calculation history. Users log in via JWT-based authentication, perform arithmetic operations (add, subtract, multiply, divide), and view their calculation history persisted to a PostgreSQL database.

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend | Java | 21 |
| Framework | Spring Boot | 3.4.0 |
| Security | Spring Security + JWT | 3.4.0 |
| Database | PostgreSQL | 16 |
| ORM | Spring Data JPA | 3.4.0 |
| Frontend | React | 19.0.0 |
| Build Tool (Frontend) | Vite | 5.0.8 |
| State Management | Redux Toolkit + RTK Query | 1.9.7 |
| Language (Frontend) | TypeScript | 5.3.3 |
| Containerization | Docker | Multi-stage build |
| Testing (Backend) | JUnit 5, Testcontainers | 1.20.4 |
| Testing (Frontend) | Vitest, React Testing Library | 2.0.0 |

## Getting Started

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Node.js 20+
- Docker (for Testcontainers and containerized deployment)

### Build and Run Locally

**Backend:**
```bash
mvn clean package
java -jar target/adder-1.0.0.jar
```
Backend runs on `http://localhost:8080`

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`

### Docker Build and Run

```bash
docker build -t calculator .
docker run -p 8080:8080 calculator
```
Access the application at `http://localhost:8080`

## API Reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | No | Authenticate user, return JWT token |
| GET | `/api/calculate?a=X&b=Y&op=OP` | Bearer Token | Perform calculation and record to history |

### POST /api/auth/login

**Request:**
```json
{
  "username": "user",
  "password": "password"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (401 Unauthorized):**
```json
{
  "error": "Invalid credentials"
}
```

### GET /api/calculate

**Query Parameters:**
- `a` (int, required): First operand
- `b` (int, required): Second operand
- `op` (string, optional, default='add'): Operation ('add', 'sub', 'mul', 'div')

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "a": 10,
  "b": 5,
  "op": "add",
  "result": 15
}
```

**Response (400 Bad Request):**
```json
{
  "error": "Unknown operation: xyz"
}
```

## Testing

### Run All Tests

```bash
mvn clean test
```

### Run Backend Tests Only

```bash
mvn test
```

### Run Frontend Tests Only

```bash
cd frontend && npm test
```

### Test Coverage

- **Backend:** Unit tests for Calculator, Adder, Subtractor, Multiplier, Divider; integration tests for AuthController and CalculatorController with Testcontainers PostgreSQL
- **Frontend:** Component tests for LoginForm, CalculatorForm, HistoryList using React Testing Library and Vitest
