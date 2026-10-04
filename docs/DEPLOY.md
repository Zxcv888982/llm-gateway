# 部署指南（零成本方案）

## 方案一：Render 免费托管（推荐）

Render 提供免费 Web Service，无需信用卡，自动 HTTPS，公网域名。

### 步骤

**1. 注册 GitHub 并上传代码**
- 注册 github.com（免费）
- 新建仓库，把 `llm-gateway/` 整个目录推上去

**2. 注册 Render**
- 打开 render.com，用 GitHub 账号登录（不需要绑信用卡）

**3. 部署**
- 控制台点「New +」→「Web Service」
- 选择你刚才的 GitHub 仓库
- Render 会自动检测根目录的 `render.yaml`，点「Apply」
- 等待 2-3 分钟构建完成
- 获得地址：`https://llm-gateway-xxx.onrender.com`

**4. 验证**
```bash
curl https://你的地址.onrender.com/health
# 返回 {"ok":true} 即成功
```

**5. 配置安卓 APP**
- 打开 `android/app/build.gradle.kts`
- 把 `API_BASE_URL` 改成你的 Render 地址（注意不要加末尾斜杠）
- 重新编译 APK

### 注意事项
- 免费实例 15 分钟无请求会休眠，下次请求需等 30-50 秒冷启动
- SQLite 数据存在实例内存中，**重新部署会重置**（个人使用可接受；重要数据建议定期调用日志接口备份）
- 每月 100GB 流量额度，个人用完全够
- 默认管理员：`admin` / `admin123`，登录后请及时修改密码

---

## 方案二：本地运行 + 内网穿透（完全零成本，数据本地）

如果你有一台常开的电脑/树莓派/旧手机，后端跑本地，用免费隧道暴露公网。

### 步骤

**1. 本地启动后端**
```bash
cd server
npm install
cp .env.example .env
npm start
# 本地运行在 http://localhost:3000
```

**2. 用 cpolar 免费版暴露公网**
- 注册 cpolar.com（免费，无需信用卡）
- 下载客户端，安装后运行：
```bash
cpolar http 3000
```
- 获得一个 `https://xxx.cpolar.io` 公网地址（免费版地址会定期变化）

**3. 配置 APP**
- 把 `API_BASE_URL` 改成 cpolar 地址，重新编译 APK

### 优缺点
- 优点：完全免费、数据本地持久化、无休眠、无流量限制
- 缺点：设备需 24 小时开机，免费版隧道地址会变（需定期更新 APP）

---

## 方案三：Oracle Cloud 永久免费（长期最佳，需信用卡验证）

Oracle 提供永久免费的 ARM 实例（4核24GB），适合长期生产使用。

- 注册 oracle.com/cloud（需信用卡验证，不扣费）
- 创建 Always Free ARM 实例
- 安装 Docker，用项目里的 `docker-compose.yml` 一键启动
- 配置域名 + Nginx + Let's Encrypt SSL

---

## 上游渠道配置（部署后必做）

部署完成后，需要添加你的 LLM API 渠道才能使用聊天功能：

```bash
# 先登录获取 token
curl -X POST https://你的地址/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 添加 OpenAI 渠道（示例）
curl -X POST https://你的地址/api/channels \
  -H "Authorization: Bearer 你的token" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OpenAI",
    "provider": "openai",
    "base_url": "https://api.openai.com/v1",
    "api_key": "sk-你的key",
    "models": ["gpt-4o", "gpt-4o-mini"],
    "weight": 1,
    "priority": 10
  }'
```

支持的渠道：OpenAI、Anthropic、DeepSeek、通义千问、智谱、月之暗面、Google Gemini 等任何 OpenAI 兼容接口。
