# Diabetes Health Monitor

[English](README.md) | [中文文档](README.zh-CN.md)

A full-stack diabetes health management system built with Spring Boot 3 and Vue 3. It integrates Alibaba Cloud DashScope / Spring AI for AI chat, blood sugar analysis, health reports, and food image recognition.

## Features

- JWT authentication with user/admin roles
- Blood sugar, body, diet, and exercise records
- AI chat with SSE streaming and Redis conversation context
- Food image recognition with Redis caching
- Health report export to Excel and PDF
- Admin dashboard, user management, exercise type management, and article management
- Docker Compose deployment
- GitHub Actions CI

## Tech Stack

- Backend: Java 17, Spring Boot 3.1.12, Spring Security, JWT, MyBatis-Plus, MySQL 8, Redis 7, DashScope Java SDK, Knife4j, Apache POI, OpenPDF
- Frontend: Vue 3, Vite, Element Plus, Pinia, Vue Router, ECharts, GSAP, Three.js
- Infrastructure: Docker Compose, GitHub Actions

## Project Structure

```text
diabetes-health-monitor/
├── .github/workflows/        # GitHub Actions CI
├── diabetes-backend/         # Spring Boot backend
├── diabetes-frontend/        # Vue 3 frontend
├── sql/                      # Database initialization SQL
├── docker-compose.yml        # Docker Compose stack
├── .env.example              # Environment variable template
└── README.md
```

## Prerequisites

- JDK 17
- Maven 3.9+
- Node.js 20+
- MySQL 8
- Redis 7
- Alibaba Cloud DashScope API key

## Local Development

### 1. Initialize Database

```bash
mysql -uroot -p < sql/init.sql
```

The default database is `diabetes_monitor`.

### 2. Set the DashScope API Key

PowerShell:

```powershell
$env:SPRING_AI_DASHSCOPE_API_KEY="sk-your-api-key"
```

Linux / macOS:

```bash
export SPRING_AI_DASHSCOPE_API_KEY=sk-your-api-key
```

### 3. Start the Backend

```bash
cd diabetes-backend
mvn spring-boot:run
```

The backend runs at `http://localhost:8088`.

### 4. Start the Frontend

```bash
cd diabetes-frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`.

## Default Accounts

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `123456` |
| User | `test001` | `123456` |

## Docker Compose

```bash
cp .env.example .env
# edit .env and set DASHSCOPE_API_KEY
docker compose up --build
```

Service ports:

- Frontend: `80`
- Backend: `8088`
- MySQL: `3307`
- Redis: `6380`

## Environment Variables

| Variable | Used by | Description |
| --- | --- | --- |
| `DASHSCOPE_API_KEY` | Docker Compose | Passed to the backend as `SPRING_AI_DASHSCOPE_API_KEY` |
| `SPRING_AI_DASHSCOPE_API_KEY` | Local Spring Boot | Alibaba Cloud DashScope API key |

MySQL and Redis settings can also be overridden with standard Spring Boot environment variables such as `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, and `SPRING_DATA_REDIS_PASSWORD`.

## Testing

```bash
cd diabetes-backend
mvn test
```

## CI/CD

The GitHub Actions workflow in `.github/workflows/ci.yml` runs on every push to `main` and on pull requests:

- Backend: `mvn clean test`
- Frontend: `npm ci` and `npm run build`

## API Documentation

- Knife4j: `http://localhost:8088/doc.html`
- Swagger UI: `http://localhost:8088/swagger-ui.html`
