# ===== 构建阶段 =====
FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json ./
RUN npm install --production=false

COPY src ./src

# ===== 运行阶段 =====
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000

# 仅复制生产依赖
COPY package.json ./
RUN npm install --production=true && npm cache clean --force

# 复制源码
COPY --from=builder /app/src ./src

# 非 root 用户运行
RUN addgroup -g 1001 -S nodejs && adduser -S nodejs -u 1001
RUN mkdir -p /app/data && chown -R nodejs:nodejs /app
USER nodejs

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget --spider -q http://localhost:3000/health || exit 1

CMD ["node", "src/server.js"]
