# 糖尿病健康监测系统

[English](README.md) | [中文文档](README.zh-CN.md)

基于 Spring Boot 3 与 Vue 3 的全栈糖尿病健康管理系统，集成阿里云百炼 DashScope / Spring AI，实现 AI 对话、血糖分析、健康报告和食物图片识别。

## 功能特性

- JWT 登录认证，用户/管理员角色权限
- 血糖、身体指标、饮食、运动记录
- AI 智能对话，支持 SSE 流式输出，使用 Redis 保存上下文
- AI 智能分析（血糖、饮食、每日健康小结）
- 食物拍照识别，使用 Redis 缓存结果
- 健康报告导出 Excel 和 PDF
- 短信验证码，Redis 限流与防刷
- 前端 RSA 密码加密
- 健康文章浏览
- 管理端：用户管理、运动类型管理、健康文章管理
- Docker Compose 一键部署
- GitHub Actions 持续集成

## 技术栈

- 后端：Java 17、Spring Boot 3.4.8、Spring AI Alibaba 1.0.0.4（DashScope）、Spring Security、JWT、MyBatis-Plus 3.5.17、MySQL 8、Redis 7、Knife4j、Apache POI、OpenPDF
- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router、ECharts、GSAP、Three.js
- 基础设施：Docker Compose、GitHub Actions

## 项目结构

```text
diabetes-health-monitor/
├── .github/workflows/        # GitHub Actions CI
├── diabetes-backend/         # Spring Boot 后端
├── diabetes-frontend/        # Vue 3 前端
├── sql/                      # 数据库初始化脚本
├── docker-compose.yml        # Docker Compose 编排
├── .env.example              # 环境变量示例
├── README.md                 # 英文文档
└── README.zh-CN.md           # 中文文档
```

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20+
- MySQL 8
- Redis 7
- 阿里云百炼 DashScope API Key

## 本地开发

### 1. 初始化数据库

```bash
mysql -uroot -p < sql/init.sql
```

默认数据库为 `diabetes_monitor`。

### 2. 配置 DashScope API Key

PowerShell：

```powershell
$env:SPRING_AI_DASHSCOPE_API_KEY="sk-your-api-key"
```

Linux / macOS：

```bash
export SPRING_AI_DASHSCOPE_API_KEY=sk-your-api-key
```

AI 模型在 `application.yml` 中配置：

- 文本对话：`qwen-max`（`spring.ai.dashscope.chat.options.model`）
- 食物识别：`qwen-vl-max`（代码中通过 `DashScopeChatOptions` 指定）

### 3. 启动后端

```bash
cd diabetes-backend
mvn spring-boot:run
```

后端地址：`http://localhost:8088`

### 4. 启动前端

```bash
cd diabetes-frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`

## 默认账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `123456` |
| 普通用户 | `test001` | `123456` |

## Docker Compose 部署

```bash
cp .env.example .env
# 编辑 .env，填写 SPRING_AI_DASHSCOPE_API_KEY
docker compose up --build
```

服务端口：

- 前端：`80`
- 后端：`8088`
- MySQL：`3307`
- Redis：`6380`

## 环境变量

| 变量 | 使用位置 | 说明 |
| --- | --- | --- |
| `SPRING_AI_DASHSCOPE_API_KEY` | Docker Compose / 本地 Spring Boot | 阿里云百炼 DashScope API Key |
| `MYSQL_ROOT_PASSWORD` | Docker Compose | MySQL root 密码 |
| `REDIS_PASSWORD` | Docker Compose | Redis 密码 |
| `JWT_SECRET` | Docker Compose / Spring Boot | JWT 签名密钥 |
| `DB_HOST` | 本地 Spring Boot | MySQL 主机，默认 `localhost` |
| `DB_PORT` | 本地 Spring Boot | MySQL 端口，默认 `3306` |
| `DB_USERNAME` | 本地 Spring Boot | MySQL 用户名，默认 `root` |
| `DB_PASSWORD` | 本地 Spring Boot | MySQL 密码，默认 `123456` |
| `REDIS_HOST` | 本地 Spring Boot | Redis 主机，默认 `localhost` |
| `REDIS_PORT` | 本地 Spring Boot | Redis 端口，默认 `6379` |
| `REDIS_PASSWORD` | 本地 Spring Boot | Redis 密码，默认 `123456` |

Spring Boot 的宽松绑定也支持通过 `SPRING_DATASOURCE_*` 和 `SPRING_DATA_REDIS_*` 环境变量覆盖对应配置。

## 测试

```bash
cd diabetes-backend
mvn test
```

## CI/CD

`.github/workflows/ci.yml` 会在 `main` 分支 push 或 PR 时自动运行：

- 后端：`mvn clean test`
- 前端：`npm ci` 和 `npm run build`

## API 文档

- Knife4j：`http://localhost:8088/doc.html`
- Swagger UI：`http://localhost:8088/swagger-ui.html`
