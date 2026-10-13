# 监控栈使用手册（Prometheus + Grafana）

配置与看板都在这个目录里，`docker-compose.monitoring.yml` 一起启动。
这份文档是给"打开看板之后看什么、怎么排查"用的。

## 1. 启动与停止

```bash
cd <项目根目录>

# 启动监控栈（需要 Docker Desktop 已运行）
docker compose -f docker-compose.monitoring.yml up -d

# 看状态 / 看日志
docker compose -f docker-compose.monitoring.yml ps
docker compose -f docker-compose.monitoring.yml logs -f prometheus

# 停止（加 -v 连数据卷一起删）
docker compose -f docker-compose.monitoring.yml down
```

三个入口：

| 入口 | 地址 | 用途 |
|---|---|---|
| Grafana 看板 | http://localhost:3000 （admin / admin，匿名只读也开着） | 日常看图 |
| Prometheus | http://localhost:9090 | 查采集状态、临时写 PromQL 验证 |
| 后端原始指标 | http://localhost:8088/actuator/prometheus | 排查"指标到底有没有产生" |

## 2. 三个入口分别解决什么问题

**Grafana（看趋势）**：先看这里。图是给人看的，用来判断"现在正常吗、什么时候开始不正常的"。

**Prometheus（查真相）**：看板某块图空着时来这里。

- `Status → Targets`：采集目标是不是 up。`backend:8088` 显示 down 是正常的——那是"后端跑在容器里"的场景；后端跑在宿主机时应看 `host.docker.internal:8088`。
- `Graph`：临时敲 PromQL 验证指标存在与否（例如直接查 `cache_access_total`）。
- `Status → Configuration`：确认 `scrape_interval` 与目标列表。

**/actuator/prometheus（查源头）**：如果 Prometheus 里查不到某指标，先 curl 这个端点。有 → 问题在采集链路；没有 → 问题在应用埋点或配置（比如端点没暴露、直方图没开）。

## 3. 看板 11 个面板怎么读

看板 uid：`diabetes-monitor-overview`，右上角时间范围建议先选「最近 6 小时」，刷新 30s。

| 面板 | 看什么 | 正常 | 异常说明 |
|---|---|---|---|
| 日聚合任务：距上次成功 | `time() - 上次成功时间戳` | 小于 24h | 超过 30h 变黄、48h 变红。**启动后没跑过任务时会显示一个巨大的数（时间戳还是 0）**，见第 6 节 |
| AI 调用速率 | 每秒 AI 调用次数 | 随使用波动 | 突然掉到 0 且服务有请求 → 调用链被上游挡住了 |
| 指标缓存命中率 | 命中 / (命中+未命中) | ≥ 0.6 | 长期 0 或没有曲线 = 缓存没生效（我们踩过） |
| HTTP 请求速率（按 uri） | 各接口的 QPS | 跟随业务 | 某个接口突然升高 → 可能是前端轮询或重试风暴 |
| HTTP P95 延迟（按 uri） | 95 分位响应时间 | < 0.5s | 升高先看是不是慢 SQL 或下游 AI 拖慢 |
| AI 调用 P95 延迟（按 scene） | 各 AI 场景耗时 | 秒级（模型生成本来就慢） | **只有几十毫秒 = 统计错了**（测的是提交耗时而不是流式耗时） |
| AI 调用错误率（按 scene） | 失败占比 | 0 | 非 0 时去日志看 403（额度/权限）还是 404（模型名过期） |
| 缓存命中 / 未命中 / 失败 | 三条曲线 | 失败恒为 0 | **失败曲线一旦非 0 必看**：缓存异常被 try/catch 静默降级，接口仍然是 200，只有这里能看见 |
| 日聚合任务：耗时与用户数 | 上一轮耗时、成功/失败用户数 | 失败为 0 | 单用户失败会计数但不中断整批 |
| JVM 堆内存 | 已用 vs 上限 | 有波动、不贴顶 | 持续贴顶且 GC 变频繁 → 内存泄漏排查 |
| JVM GC 暂停时间 | 每秒暂停秒数 | 接近 0 | 明显上升 → 影响接口 P99 |

## 4. 必会的 5 条 PromQL

```promql
# ① 每秒请求速率（按接口）
sum by (uri) (rate(http_server_requests_seconds_count[5m]))

# ② P95 延迟（直方图标准写法：先按 le 聚合再算分位）
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))

# ③ 缓存命中率（clamp_min 防止分母为 0 时出现 NaN）
sum(rate(cache_access_total{result="hit"}[5m]))
  / clamp_min(sum(rate(cache_access_total[5m])), 0.000001)

# ④ AI 错误率（按场景）
sum by (scene) (rate(ai_call_error_total[10m]))
  / clamp_min(sum by (scene) (rate(ai_call_seconds_count[10m])), 0.000001)

# ⑤ 任务新鲜度：距上次成功多久（配合阈值告警）
time() - daily_stat_last_success_timestamp_seconds
```

记住三个关键词就够用：`rate()` 算"每秒多少"、`sum by (标签)` 按维度聚合、`histogram_quantile(0.95, ...)` 算分位。

## 5. 面板空白 / 没数据时的排查顺序

1. **Prometheus 的 Targets 是 up 吗** → down 说明采集不到（后端没起、端口不通、路径不对）
2. **`/actuator/prometheus` 里有这个指标吗** → 没有就是应用侧问题（端点没暴露、埋点没触发、直方图没开）
3. **指标是"从没出现过"还是"出现过但为 0"** → 前者说明代码路径没走到；后者说明走到了但没 `increment()`（我们踩过）
4. **标签对得上吗** → 看板写 `result="hit"`、代码写 `.tag("op", ...)` 就永远查不到

## 6. 两个已知的语义坑（面试可讲）

**坑一：`gauge = 0` 会让"新鲜度"面板假报警。**
应用刚启动、定时任务还没跑过时，`daily_stat_last_success_timestamp_seconds` 是 0，
于是 `time() - 0` 显示成"距上次成功 56 年"，面板直接标红。
但真实含义是"还没有成功过"，不是"失败了"。两种改法：

- 启动时把 gauge 初始化成应用启动时间（语义变成"最晚可接受的成功时间"）
- 或让面板只在任务跑过之后才判断：`(time() - x) and (daily_stat_job_seconds_count > 0)`

**结论：「指标没有数据」和「指标是 0」是两件事。** 监控设计里必须区分。

**坑二：失败请求的耗时会被重试放大。**
`qwen-max` 额度耗尽时，调用先 403、SDK 自动重试，最终 P95 从 0.9s 涨到 5.3s。
"故障时延迟升高"不一定是性能问题，可能只是重试。

## 7. 面试演示脚本（3 分钟）

1. 打开看板，先指左上角：「这是业务指标，不是默认的 JVM 指标——AI 调用、缓存、定时任务都是我自己埋的。」
2. 点「指标缓存命中率」：「这条曲线长期为 0 或者没有数据，就说明缓存没生效。我们真踩过一次——Redis 序列化器缺 JSR310 模块，写缓存一直抛异常被 try/catch 吞掉，接口返回照样正确，只有指标和日志能发现。」
3. 点「缓存失败」：「所以我把失败单独做成一条曲线，而不是只写进日志。」
4. 点「AI 调用 P95（按 scene）」：「按场景拆开，才能看出是血糖分析慢还是普通对话慢。这里有个坑——流式接口不能用方法入口出口掐表，那样只测到提交耗时几十毫秒，必须用 `Timer.Sample` 等到流真正结束。」
5. 点「日聚合任务：距上次成功」：「定时任务最怕的是'悄悄不跑了'，所以我用时间戳做新鲜度指标，而不是只看它有没有报错。」

## 8. 生产化还需要补什么

- `backend:8088` 这个 down 的目标：按实际部署方式二选一，删掉用不到的那个
- `/actuator/**` 目前随业务端口开放且未鉴权，对外部署应挪到独立管理端口或加访问控制
- 加告警规则（`monitoring/prometheus/` 下放 `alerts.yml`）：缓存失败率、AI 错误率、任务新鲜度
- 单机 Prometheus + 本地存储只适合演示；生产要么用托管服务，要么上 Thanos/VictoriaMetrics 做长期存储
