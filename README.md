# Diabetes Health Monitor

[English](README.md) | [中文文档](README.zh-CN.md)

A full-stack diabetes health management system built with Spring Boot 3 and Vue 3. It integrates Alibaba Cloud DashScope / Spring AI for AI chat, blood sugar analysis, health reports, and food image recognition.

## Features

- JWT authentication with user/admin roles
- Blood sugar, body, diet, and exercise records
- AI chat with SSE streaming and Redis conversation context
- AI health analysis (blood sugar, diet, and daily report)
- Food image recognition with Redis caching
- Health report export to Excel and PDF
- SMS verification with Redis rate limiting
- Frontend RSA password encryption
- Health article browsing
- Admin dashboard, user management, exercise type management, and article management
- Docker Compose deployment
- GitHub Actions CI

## Tech Stack

- Backend: Java 17, Spring Boot 3.4.8, Spring AI Alibaba 1.0.0.4 (DashScope), Spring Security, JWT, MyBatis-Plus 3.5.17, MySQL 8, Redis 7, Knife4j, Apache POI, OpenPDF
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

The AI models are configured in `application.yml`:

- Text chat: `qwen-max` (`spring.ai.dashscope.chat.options.model`)
- Food recognition: `qwen-vl-max` (set in code via `DashScopeChatOptions`)

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
# edit .env and set SPRING_AI_DASHSCOPE_API_KEY
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
| `SPRING_AI_DASHSCOPE_API_KEY` | Docker Compose / Local Spring Boot | Alibaba Cloud DashScope API key |
| `MYSQL_ROOT_PASSWORD` | Docker Compose | MySQL root password |
| `REDIS_PASSWORD` | Docker Compose | Redis password |
| `JWT_SECRET` | Docker Compose / Spring Boot | JWT signing secret |
| `DB_HOST` | Local Spring Boot | MySQL host, default `localhost` |
| `DB_PORT` | Local Spring Boot | MySQL port, default `3306` |
| `DB_USERNAME` | Local Spring Boot | MySQL username, default `root` |
| `DB_PASSWORD` | Local Spring Boot | MySQL password, default `123456` |
| `REDIS_HOST` | Local Spring Boot | Redis host, default `localhost` |
| `REDIS_PORT` | Local Spring Boot | Redis port, default `6379` |
| `REDIS_PASSWORD` | Local Spring Boot | Redis password, default `123456` |

Spring Boot relaxed binding also allows `SPRING_DATASOURCE_*` and `SPRING_DATA_REDIS_*` environment variables to override the corresponding settings.

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
