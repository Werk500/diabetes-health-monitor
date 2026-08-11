# ============================================================
# 糖尿病健康监测系统 - 后端 Dockerfile（多阶段构建）
# ============================================================

# ---------- 阶段 1：Maven 构建 ----------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
# 预先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ---------- 阶段 2：运行时镜像 ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 创建日志目录
RUN mkdir -p /app/logs
# 从构建阶段拷贝 jar
COPY --from=builder /app/target/*.jar app.jar
# 创建非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
