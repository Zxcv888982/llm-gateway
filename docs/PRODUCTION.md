# 生产级部署指南（国内 / 百人以上规模）

## 架构概览

```
                    ┌─────────────┐
                    │   域名/DNS   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │    Nginx     │  反向代理 + HTTPS + 限流 + Gzip
                    │  (443/80)   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  Node.js App │  Express + Knex 连接池
                    │   (:3000)    │  多实例可水平扩展
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  PostgreSQL  │  主库（可加只读副本）
                    │   (:5432)    │  定时备份 + PITR
                    └─────────────┘
```

全部容器化（Docker Compose），数据持久化卷，健康检查自动重启。

---

## 一、云服务器选型（国内）

### 推荐配置（百人 / 日请求 10 万内）

| 组件 | 配置 | 说明 |
|------|------|------|
| CPU | 4 核 | 并发请求处理 |
| 内存 | 8 GB | PostgreSQL + Node.js 各占约 3GB |
| 系统盘 | 80 GB SSD | 系统 + 日志 + 备份 |
| 带宽 | 5 Mbps 起步 | API 流量小，可按量弹性 |
| 操作系统 | Ubuntu 22.04 LTS | 稳定，文档全 |

### 云服务商对比

| 服务商 | 优势 | 4核8G 年付参考 |
|--------|------|----------------|
| 阿里云 ECS | 生态全、备案快、稳定 | 约 ¥1500-2500/年 |
| 腾讯云 CVM | 性价比高、网络好 | 约 ¥1200-2000/年 |
| 华为云 ECS | 政企客户多、安全合规 | 约 ¥1500-2500/年 |

**建议**：新用户首年常有 1-3 折优惠，先买一年试用。后续可升配。

### 数据库选项

- **自建 PostgreSQL（Docker）**：包含在上述服务器配置中，零额外成本，适合起步
- **云数据库 RDS（推荐生产）**：阿里云/腾讯云 PostgreSQL，自动备份、主从切换、监控告警。4核8G 约 ¥300-600/月。日请求超过 50 万建议上 RDS。

---

## 二、域名与备案

### 1. 购买域名
- 阿里云万网 / 腾讯云 DNSPod 购买，`.com` 约 ¥55-70/年
- 建议同时买 `.cn` 备用

### 2. ICP 备案（国内服务器必须）
- 在云服务商控制台提交备案，免费
- 需准备：身份证、手机号、邮箱、域名证书
- 个人备案约 7-20 个工作日，企业备案约 3-7 个工作日
- 备案期间域名无法解析到国内服务器（可用海外临时节点过渡）

### 3. DNS 解析
- 备案通过后，添加 A 记录指向服务器公网 IP
- 建议同时配置 `www` 和 `@` 两个记录

---

## 三、部署步骤

### 1. 服务器初始化

```bash
# SSH 登录服务器
ssh root@你的服务器IP

# 更新系统
apt update && apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com | bash
systemctl enable --now docker

# 安装 Docker Compose 插件（Docker 20.10+ 已内置）
docker compose version

# 创建非 root 用户（安全）
adduser deploy
usermod -aG docker deploy
```

### 2. 上传代码

```bash
# 方式一：Git（推荐）
# 在服务器上
git clone https://github.com/你的用户名/llm-gateway.git
cd llm-gateway/server

# 方式二：SCP 上传
# 在本地执行
scp -r llm-gateway/server root@服务器IP:/opt/llm-gateway
```

### 3. 配置环境变量

```bash
cd /opt/llm-gateway  # 或你的代码目录

# 复制配置模板
cp .env.production.example .env

# 生成强密码（复制输出填入 .env）
echo "POSTGRES_PASSWORD=$(openssl rand -hex 16)"
echo "JWT_SECRET=$(openssl rand -hex 32)"

# 编辑 .env，填入：
# - POSTGRES_PASSWORD（上面生成的）
# - JWT_SECRET（上面生成的）
# - ADMIN_PASSWORD（你的管理员密码）
# - DOMAIN（你的域名）
nano .env
```

### 4. 启动服务

```bash
# 构建并启动（首次约 3-5 分钟）
docker compose -f docker-compose.prod.yml up -d --build

# 查看状态
docker compose -f docker-compose.prod.yml ps

# 查看日志
docker compose -f docker-compose.prod.yml logs -f app

# 验证健康检查
curl http://localhost:3000/health
# 返回 {"ok":true,...} 即成功
```

### 5. 配置 Nginx + SSL

```bash
# 先修改 nginx/conf.d/default.conf，把 server_name 改成你的域名
# 暂时用 HTTP 验证（SSL 配置在 certbot 后取消注释）

# 安装 certbot（在宿主机或用 docker 镜像）
apt install certbot -y

# 生成 SSL 证书（需要域名已解析到本机）
certbot certonly --standalone -d 你的域名.com -d www.你的域名.com

# 复制证书到 nginx/certs 目录
cp /etc/letsencrypt/live/你的域名.com/fullchain.pem nginx/certs/
cp /etc/letsencrypt/live/你的域名.com/privkey.pem nginx/certs/

# 修改 nginx/conf.d/default.conf，取消 SSL 相关行的注释

# 重载 Nginx
docker compose -f docker-compose.prod.yml restart nginx

# 验证 HTTPS
curl https://你的域名.com/health
```

### 6. 配置自动续期（SSL 证书 90 天有效期）

```bash
# 添加 crontab，每月自动续期
crontab -e
# 添加：
0 3 1 * * certbot renew --quiet && cp /etc/letsencrypt/live/你的域名.com/*.pem /opt/llm-gateway/nginx/certs/ && cd /opt/llm-gateway && docker compose -f docker-compose.prod.yml restart nginx
```

---

## 四、备份策略

### 自动每日备份

```bash
# 添加 crontab，每天凌晨 3 点备份，保留 30 天
crontab -e
# 添加：
0 3 * * * cd /opt/llm-gateway && ./backup.sh 30 >> /var/log/llm-gateway-backup.log 2>&1
```

备份文件存在 `backups/` 目录，格式 `llmgateway_YYYYMMDD_HHMMSS.sql.gz`。

### 异地备份（强烈建议）

```bash
# 安装 ossutil / coscli，把备份同步到对象存储
# 阿里云 OSS / 腾讯云 COS，100GB 存储约 ¥10/月
# 示例（阿里云 OSS）：
0 4 * * * ossutil cp -r /opt/llm-gateway/backups/ oss://你的bucket/backups/
```

### 恢复

```bash
# 列出备份
ls -lh backups/

# 恢复指定备份（会停止应用、覆盖数据）
./restore.sh backups/llmgateway_20240101_030000.sql.gz
```

---

## 五、安全加固

### 1. 服务器安全

```bash
# 防火墙（只开放 80/443，SSH 改端口）
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 2222/tcp  # 如果你改了 SSH 端口
ufw enable

# SSH 禁用密码登录，只用密钥
sed -i 's/#PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
systemctl restart sshd

# 禁止 root 远程登录
sed -i 's/PermitRootLogin yes/PermitRootLogin no/' /etc/ssh/sshd_config
```

### 2. 应用安全

- JWT_SECRET 必须用 64 位随机字符串（已在部署步骤中生成）
- 管理员初始密码部署后立即修改（APP 内"我的→修改密码"）
- Nginx 已配置限流（10 req/s/IP，突发 20）和连接数限制（50/IP）
- 应用层 API 限流（300 次/15分钟/IP，可在 .env 调整）
- Helmet 安全头已启用
- Docker 容器以非 root 用户运行

### 3. 数据库安全

- PostgreSQL 不暴露公网端口（仅 internal 网络）
- 强密码（已在部署步骤中生成）
- 定期备份（已配置）

---

## 六、监控与日志

### 应用日志

```bash
# 实时查看应用日志
docker compose -f docker-compose.prod.yml logs -f app

# Nginx 访问日志
tail -f nginx/logs/access.log

# Nginx 错误日志
tail -f nginx/logs/error.log
```

### 健康检查

- 应用：`GET /health` 返回 `{"ok":true,"ts":...}`
- Docker 自动健康检查，异常自动重启（`restart: unless-stopped`）
- 可配置云服务商的健康监控（阿里云云监控 / 腾讯云云监控），免费

### 推荐监控工具（可选）

- **Uptime Kuma**：开源监控，Docker 一键部署，监控接口可用性和响应时间
- **Prometheus + Grafana**：专业监控，适合大规模
- **Sentry**：错误追踪，免费版够用

---

## 七、扩容方案

### 垂直扩容（先做这个）

- 服务器升配：4核8G → 8核16G → 16核32G（云服务商控制台一键升配，约5分钟停机）
- 数据库迁移到云 RDS（独立扩展，不影响应用）

### 水平扩容（日请求 > 50 万）

1. **应用多实例**：`docker-compose.prod.yml` 中 app 服务加 `deploy: replicas: 3`，Nginx 自动负载均衡
2. **数据库读写分离**：PostgreSQL 主从复制，读请求走只读副本
3. **Redis 缓存**：热门模型列表、渠道配置缓存，减少数据库查询
4. **消息队列**：请求日志异步写入（BullMQ / RabbitMQ），降低主链路延迟

### 静态资源 CDN

- APP 内的模型图标、静态资源可放 CDN（阿里云 CDN / 腾讯云 CDN）
- API 请求不走 CDN（动态内容）

---

## 八、成本估算（年付）

| 项目 | 起步配置 | 中等配置 |
|------|---------|---------|
| 云服务器（4核8G） | ¥1,500/年（新用户优惠） | ¥3,000/年（8核16G） |
| 域名 | ¥60/年 | ¥60/年 |
| SSL 证书 | 免费（Let's Encrypt） | 免费 |
| 对象存储备份（100GB） | ¥120/年 | ¥240/年 |
| 云数据库 RDS（可选） | - | ¥3,600/年（4核8G） |
| CDN（可选） | - | ¥500/年（按量） |
| **合计** | **约 ¥1,700/年** | **约 ¥7,400/年** |

---

## 九、上线检查清单

- [ ] 服务器安全组只开放 80/443
- [ ] SSH 密钥登录，禁用密码和 root
- [ ] 域名已备案并解析
- [ ] HTTPS 证书已配置并自动续期
- [ ] JWT_SECRET 为 64 位随机字符串
- [ ] 数据库强密码
- [ ] 管理员初始密码已修改
- [ ] 每日自动备份已配置
- [ ] 异地备份已配置
- [ ] 健康检查正常
- [ ] 上游 API 渠道已添加并测试
- [ ] APP 的 API_BASE_URL 已改为生产域名
- [ ] APP 已重新编译并分发

---

## 十、常用运维命令

```bash
# 进入项目目录
cd /opt/llm-gateway

# 启动/停止/重启
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml restart app

# 查看状态和日志
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app
docker compose -f docker-compose.prod.yml logs -f postgres

# 更新代码后重新部署
git pull
docker compose -f docker-compose.prod.yml up -d --build app

# 手动备份
./backup.sh 30

# 查看数据库
docker exec -it llm-gateway-db psql -U llmgateway -d llmgateway
```
