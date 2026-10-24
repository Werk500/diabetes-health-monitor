# 糖尿病健康监测系统架构复盘

## 1. 系统定位

这是一个以糖尿病日常健康管理为核心的全栈系统，重点不只是 CRUD，而是把以下能力串成闭环：

1. 用户记录血糖、身体指标、饮食和运动数据。
2. 后端用确定性代码计算 TIR、CV、GMI 等医疗相关指标。
3. 定时任务生成每日聚合数据，支持趋势查询和历史补算。
4. AI 负责解释数据和生成健康建议，不负责决定关键数值。
5. 支持 SSE 流式对话、食物图片识别和异步 Excel/PDF 导出。
6. 使用 Redis、短期 Token、权限校验、监控和任务补偿保证可用性与安全性。

当前部署形态是 **模块化单体**，不是微服务。所有后端模块打包在同一个 Spring Boot 进程中，但业务边界按 Auth、Records、Analytics、AI、Export、Admin 分开。

## 2. 总体架构

```text
┌──────────────────────────────────────────────────────────────┐
│ Vue 3 + Vite + Pinia + Element Plus                         │
│ 登录/健康记录/趋势图/AI 对话/导出中心/管理后台              │
└──────────────────────────────┬───────────────────────────────┘
                               │ REST + SSE
                               │ Authorization: Bearer accessToken
                               │ HttpOnly refresh_token Cookie
┌──────────────────────────────▼───────────────────────────────┐
│ Spring Boot 3.4.8                                            │
│                                                              │
│  Security/Auth   Records   Metrics   Dashboard               │
│  AI Orchestration   Async Export   Admin   Observability     │
│                                                              │
│  Controllers → Services → MyBatis-Plus Mappers               │
└───────────────┬──────────────────────┬───────────────────────┘
                │                      │
        ┌───────▼────────┐     ┌───────▼────────┐
        │ MySQL 8        │     │ Redis 7        │
        │ 业务主数据     │     │ Token/缓存/锁  │
        └────────────────┘     └────────────────┘
                │
        ┌───────▼──────────────────────────────────────────────┐
        │ DashScope / Spring AI                               │
        │ 文本对话、健康分析、多模态食物识别                  │
        └─────────────────────────────────────────────────────┘

        ┌─────────────────────────────────────────────────────┐
        │ Prometheus → Grafana                                │
        │ JVM / HTTP / AI / Cache / Job / Export 指标         │
        └─────────────────────────────────────────────────────┘
```

## 3. 模块边界

| 模块 | 主要职责 | 关键入口 |
|---|---|---|
| Auth | 登录、Refresh Token 轮换、登出、改密、角色认证 | `UserController`、`AuthTokenServiceImpl` |
| Records | 身体、血糖、饮食、运动记录 | `HealthRecordController` |
| Metrics | TIR/TAR/TBR、CV、GMI、聚合事件 | `BloodGlucoseMetricsCalculator` |
| Daily Analytics | 每日聚合、趋势、补算 | `DailyGlucoseStatJob`、`HealthDailyStatServiceImpl` |
| AI | 对话、三类健康分析、食物识别 | `AiServiceImpl`、`AiAnalysisServiceImpl` |
| Export | 异步 Excel/PDF 任务、重试、恢复、下载 | `ExportController`、`ExportTaskDispatchJob` |
| Admin | 用户、运动类型、文章管理 | `AdminController` |
| Observability | 业务指标与 Grafana 看板 | `BusinessMetrics` |

### 为什么暂不拆微服务

- 当前数据高度关联，单库事务可以简化一致性。
- 单模块部署和排错成本更低，适合个人项目。
- 业务边界已经拆清楚，后续可以按 Auth、Records、Analytics、AI、Export 拆分。
- 过早拆分会引入注册中心、配置中心、链路追踪、分布式事务和跨服务查询等额外复杂度。

## 4. 数据模型

### MySQL

核心表：

- `sys_user`：账号、角色、糖尿病类型。
- `health_record_blood_sugar`：血糖明细。
- `health_record_body`：身高、体重等身体数据。
- `health_record_diet`：饮食和营养数据。
- `health_record_exercise`：运动数据。
- `health_daily_glucose_stat`：按用户、日期保存的血糖聚合。
- `export_task`：异步导出任务。
- `ai_chat_history`：AI 对话持久化。
- `health_article`、`sys_user_article`：健康文章及用户关联。

### Redis

| Key | 用途 | TTL/特点 |
|---|---|---|
| `auth:refresh:{sha256}` | Refresh Token 到用户/version 的映射 | 7 天，只保存哈希 |
| `auth:user-refresh:{userId}` | 用户全部 Refresh Token 集合 | 用于清理和吊销 |
| `auth:user-version:{userId}` | 用户 Token 版本号 | 改密/禁用后递增 |
| `login:lock:{username}` | 登录失败次数 | 30 分钟 |
| `metrics:blood:{userId}:{days}:{date}:{version}` | 血糖指标缓存 | 10 分钟 |
| `metrics:blood:ver:{userId}` | 缓存版本号 | 数据变化时 INCR |
| `chat:context:{userId}` | AI 对话上下文 | 最多 20 条，30 分钟 |
| `chat:session:{userId}` | 当前 AI 会话 ID | 30 分钟 |
| `food:cache:{md5}` | 图片识别结果 | 24 小时 |
| `ai:rate:{userId}:{uri}` | AI 接口限流 | 滑动时间窗口 |
| `sms:limit:{phone}` | 短信发送限流 | 60 秒 |
| `sms:code:{phone}` | 短信验证码 | 5 分钟 |
| `lock:job:daily-glucose:{date}` | 日聚合分布式锁 | Redisson，30 分钟租期 |

## 5. 认证与授权流程

### 登录

```text
用户名 + RSA(password)
  → BCrypt 校验
  → 生成 15 分钟 Access JWT
  → 生成 7 天随机 Refresh Token
  → Redis 保存 Refresh Token 的 SHA-256
  → Refresh Token 写入 HttpOnly Cookie
  → accessToken + user 返回前端
```

### 请求认证

```text
Authorization: Bearer <accessToken>
  → JwtAuthFilter
  → 校验签名、type=access、exp
  → 将 userId 和 ROLE_* 写入 SecurityContext
  → CurrentUser 提供当前用户身份
```

### 自动刷新

```text
业务请求返回 401
  → 前端单飞调用 /api/user/refresh
  → Redis Lua 原子 GET+DEL 旧 Refresh Token
  → 校验 userId:version
  → 签发新 Access + 新 Refresh
  → 重试原请求一次
```

并发刷新同一 Token 时只有一个请求能成功，避免一次 Token 被换出多套会话。

### 主动失效

- 单个登出：删除当前 Refresh Token。
- 修改密码：更新 BCrypt 密码并递增用户 Token version。
- 管理员禁用用户：递增 version，旧 Refresh Token 无法再刷新。
- 已经签发的 Access Token 最长还能使用 15 分钟，这是短 Token 策略接受的时间窗口。

### 数据权限

- `CurrentUser.assertSelf(userId)` 阻止用户访问其他用户数据。
- 健康记录新增接口强制使用当前登录用户 ID，不信任请求体中的 `userId`。
- 删除记录先查询归属，再执行删除。
- 用户资料更新使用白名单 DTO，不允许修改 `role/status/password`。
- 管理员接口统一位于 `/api/admin/**`；用户启停和用户列表要求管理员角色。

## 6. 血糖指标引擎

### 输入

血糖记录被转换为 `GlucoseReading(value, measureType, measureTime)`。

有效范围：

```text
0 < value <= 33.3 mmol/L
```

时间窗口采用左闭右开：

```text
[windowFrom, windowTo)
```

### 输出

- 样本数、脏数据数量、样本是否足够。
- 平均血糖、空腹均值、餐后均值、其他均值。
- 样本标准差、CV。
- TIR、TAR、TBR。
- 高血糖次数、严重低血糖次数、低血糖事件数。
- 最大/最小血糖。
- GMI。
- TIR 是否达标。

### 关键设计

- 医疗数值由纯 Java 计算器负责，AI 只解释，不负责算术。
- 样本不足时返回 `null`，不用 0 伪装“正常”。
- 低血糖事件按时间归并，持续低血糖不会被重复计数。
- TIR/TAR/TBR 共用同一批有效样本，保证口径一致。
- 数据变化时递增 Redis version，旧缓存通过新 key 自然失效。

## 7. 每日聚合与趋势

每日 02:10 聚合前一天：

```text
Redisson 分布式锁
  → 查询当天有血糖记录的用户
  → 逐用户调用同一个指标计算器
  → MySQL ON DUPLICATE KEY UPDATE
  → 单用户失败不影响整批
```

趋势查询：

1. 读取历史日聚合表。
2. 今日数据不等待凌晨任务，使用实时指标缓存兜底。
3. 对无数据日期补空对象，前端日期轴不会错位。
4. 支持最多 90 天补算。

## 8. AI 流程

### 普通对话

```text
用户消息
  → Redis 加载最近上下文
  → System Prompt + 历史消息 + 当前消息
  → Spring AI ChatModel
  → SSE 流式输出
  → Redis 保存短期上下文
  → MySQL 保存长期历史
```

上下文窗口：

- Redis 最多保留 20 条。
- 每次只发送最近 10 条给模型。
- Redis TTL 30 分钟。

### 健康分析

- 血糖分析、饮食分析、每日小结分别使用独立的 scene 指标。
- 数据由后端查询并拼接，模型只做解释。
- SSE 使用 `Timer.Sample` 统计完整流式调用耗时，而不是只看请求提交耗时。

### 食物识别

- 图片先压缩，再构造多模态 Prompt。
- 结果解析为 `FoodRecognitionResult`。
- 基于图片 MD5 缓存 24 小时。
- 模型失败时返回可理解的降级结果。

### AI 边界

- 不做诊断、不开处方。
- 涉及医疗判断时提示咨询医生。
- 关键指标由确定性代码计算。
- AI 超时或持久化失败不阻塞主链路，失败通过指标和日志暴露。

## 9. 异步导出

### 状态机

```text
PENDING → RUNNING → SUCCESS
                  └→ FAILED → PENDING（手动重试，最多 3 次）
```

### 提交

```text
校验 type/日期范围
  → export_task 插入 PENDING
  → Dispatcher 投递线程池
  → 接口立即返回 taskId
```

### 执行

```text
条件更新 claim(PENDING→RUNNING)
  → 生成 Excel/PDF
  → 文件落盘
  → markSuccess / markFailed
```

### 可靠性

- 线程池使用 `AbortPolicy`，队列满时任务保持 PENDING。
- 每 30 秒扫描滞留 PENDING 并重新投递。
- 超过 10 分钟仍 RUNNING 的任务标记 FAILED。
- 条件更新保证多实例只有一个 worker 能领取任务。
- 文件保留 7 天，数据库任务记录同步清理。

### 当前限制

导出文件写在本地 `exports` 目录。单实例安全；多实例需要共享 PVC、MinIO 或对象存储。

## 10. 线程池

| 线程池 | 场景 | 策略 |
|---|---|---|
| `aiExecutor` | AI HTTP I/O | 4/10 线程，CallerRuns |
| `persistenceExecutor` | Redis/MySQL 持久化 | 2/4 线程，Discard |
| `exportTaskExecutor` | Excel/PDF | 2/4 线程，Abort，交给补偿任务 |
| `taskExecutor` | 其他并行查询 | 通用 I/O 线程池 |

设计原则：

- AI 调用是 I/O 密集，可适当提高并发。
- 非核心持久化失败不能拖垮 AI 主流程。
- 导出属于后台任务，队列满时不应占用 HTTP 线程。

## 11. 可观测性

业务指标：

- AI 调用耗时与错误数。
- 缓存命中、未命中、失败。
- HTTP QPS、P95。
- 日聚合耗时、成功/失败用户数、上次成功时间。
- 导出提交、完成、耗时、派发拒绝、积压。

监控设计重点：

- “没有指标”和“指标值为 0”必须区分。
- 缓存异常会被降级逻辑吞掉，因此单独记录 `cache_error_total`。
- 定时任务使用最后成功时间判断“任务是否悄悄不跑”。
- 导出任务必须有 backlog 和 rejection，否则异步队列故障不可见。

## 12. 部署形态

### 当前已验证

- 本地 Java + MySQL + Redis。
- Docker Compose 前后端与依赖服务。
- Prometheus + Grafana 监控栈。
- GitHub Actions：后端测试 + 前端构建。

### 生产准备

- `prod` Profile 强制外部 Secret。
- Swagger/Knife4j 在生产关闭。
- Actuator 独立到 `9090`，只暴露 health/prometheus。
- HTTP 入口需要 HTTPS，Refresh Cookie 使用 Secure。

### Kubernetes

仓库已经准备 K8s 清单：

- MySQL/Redis StatefulSet。
- 后端/前端 Deployment。
- Service、NodePort、PVC、ConfigMap、Secret。
- readiness/liveness/startup probes。

当前本地 Docker Desktop kind 集群启动受环境状态影响，部署验收需在集群 Ready 后进行。

## 13. 已知限制与演进方向

1. 导出文件应迁移到 MinIO 或对象存储，支持后端多副本。
2. 如需 Access Token 立即失效，可在 JwtAuthFilter 校验 Redis Token version。
3. AI 对话历史目前同步落 MySQL 为异步旁路，可引入消息队列提高可靠性。
4. 日聚合数据量增大后，可改为分片批处理或独立分析服务。
5. 多实例部署后，定时任务和缓存都应保持当前的条件更新与锁语义。
6. 公网部署需要域名、HTTPS、反向代理、Secret 管理和备份策略。
