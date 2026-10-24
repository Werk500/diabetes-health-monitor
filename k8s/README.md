# Kubernetes 本地部署

## 前置条件

- Docker Desktop Engine 已启动。
- Docker Desktop 已启用 Kubernetes。
- `kubectl config current-context` 返回 `docker-desktop`。
- 本机 `30080` 端口空闲。
- 环境变量中存在真实 `SPRING_AI_DASHSCOPE_API_KEY`。

验证：

```powershell
docker info
kubectl get nodes
```

## 部署

在项目根目录执行：

```powershell
.\k8s\deploy.ps1
```

脚本会自动：

- 构建 `diabetes-monitor-backend:local`
- 构建 `diabetes-monitor-frontend:local`
- 创建数据库和 Redis Secret
- 从 `sql/init.sql` 创建 MySQL 初始化 ConfigMap
- 部署 MySQL、Redis、后端、前端、PVC、Service
- 等待所有工作负载 Ready

如果镜像已经构建：

```powershell
.\k8s\deploy.ps1 -SkipBuild
```

完全重建并删除所有数据：

```powershell
.\k8s\deploy.ps1 -ResetData
```

## 访问

- 前端：`http://localhost:30080`
- 后端健康检查仅限集群内：

```powershell
kubectl port-forward -n diabetes-monitor svc/diabetes-backend 9090:9090
```

然后访问 `http://localhost:9090/actuator/health`。

## 冒烟测试

```powershell
.\k8s\smoke.ps1
```

覆盖前端、登录、Access Token、Refresh Token 轮换和退出。

## 查看状态

```powershell
kubectl get pods,svc,pvc -n diabetes-monitor
kubectl get events -n diabetes-monitor --sort-by=.lastTimestamp
kubectl logs -n diabetes-monitor deployment/diabetes-backend --tail=200
kubectl logs -n diabetes-monitor statefulset/mysql --tail=200
kubectl logs -n diabetes-monitor statefulset/redis --tail=200
```

## 停止

保留 PVC 和数据库数据：

```powershell
.\k8s\stop.ps1
```

删除 Namespace 和全部数据：

```powershell
.\k8s\stop.ps1 -DeleteData
```

## 设计限制

- 后端固定为 1 个副本，因为导出文件目前写入本地 PVC。
- 要水平扩容后端，需要先把导出文件迁移到共享 PVC、MinIO 或对象存储。
- 本地 K8s 使用 HTTP，因此 `REFRESH_COOKIE_SECURE=false`。
- 公网 HTTPS 部署必须改为 `REFRESH_COOKIE_SECURE=true`。
- MySQL `init.sql` 仅会在空的数据卷上执行。修改初始化 SQL 后如需重建，请使用 `-ResetData`。
