# ============================================================
# 糖尿病健康监测系统 - 前端 Dockerfile（多阶段构建）
# ============================================================

# ---------- 阶段 1：Node 构建 ----------
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# ---------- 阶段 2：Nginx 运行时 ----------
FROM nginx:1.25-alpine
# 覆盖默认 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 拷贝构建产物
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
