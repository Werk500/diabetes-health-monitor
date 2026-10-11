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

## Observability

Prometheus + Grafana 的配置与看板已随仓库提供（`monitoring/` 与 `docker-compose.monitoring.yml`），
启动后无需手工导入数据源和看板：

```bash
# 需先启动 Docker Desktop
docker compose -f docker-compose.monitoring.yml up -d
# Prometheus: http://localhost:9090   Grafana: http://localhost:3000 (admin / admin)
```

后端通过 Actuator 暴露 Micrometer 指标（`GET /actuator/prometheus`）。除 JVM / HTTP 等默认指标外，
还埋了一组业务指标：

| Metric | Type | Tags | 含义 |
|---|---|---|---|
| `ai_call_seconds` | Timer | `scene` | AI 调用耗时（chat / 三类分析 / 食物识别） |
| `ai_call_error_total` | Counter | `scene` | AI 调用失败次数 |
| `cache_access_total` | Counter | `cache`, `result` | 缓存命中 / 未命中 |
| `cache_error_total` | Counter | `cache`, `op` | 缓存读 / 写 / 失效失败次数 |
| `daily_stat_job_seconds` | Timer | — | 日聚合任务整轮耗时 |
| `daily_stat_user_total` | Counter | `result` | 日聚合按用户成功 / 失败数 |
| `daily_stat_last_success_timestamp_seconds` | Gauge | — | 上次成功时间戳，用 `time() - x` 得到"距上次成功多久" |

`cache_error_total` 的存在是有原因的：缓存异常会被 `try/catch` 静默降级为查库，
业务结果依然正确，光看接口发现不了问题——必须有一条能看见失败的曲线。

> 生产注意：`/actuator/**` 目前随业务端口开放，且 `management` 未鉴权。
> 对外部署时应改到独立管理端口或加访问控制，避免指标与内部信息直接暴露。

## CI/CD

The GitHub Actions workflow in `.github/workflows/ci.yml` runs on every push to `main` and on pull requests:

- Backend: `mvn clean test`
- Frontend: `npm ci` and `npm run build`

## API Documentation

- Knife4j: `http://localhost:8088/doc.html`
- Swagger UI: `http://localhost:8088/swagger-ui.html`
