# 免费部署指南（零成本 / 无需信用卡 / 无需自己管服务器）

用 **Render（免费应用托管）+ Supabase（免费 PostgreSQL）**，全程网页操作，不用 SSH、不用配服务器。

---

## 整体架构

```
用户 APP ──HTTPS──▶ Render（免费 Node.js 服务，自动部署）
                         │
                         ├──▶ Supabase（免费 PostgreSQL 数据库）
                         └──▶ 上游 LLM API（OpenAI/DeepSeek/通义等）
```

- Render：跑你的后端代码，给你一个 `https://xxx.onrender.com` 域名
- Supabase：托管 PostgreSQL 数据库，500MB 免费
- 两个都不用信用卡，不用管服务器运维

---

## 第一步：注册 Supabase（免费数据库）

### 1. 注册
- 打开 https://supabase.com
- 点「Start your project」，用 GitHub 账号登录（不用信用卡）

### 2. 创建项目
- 点「New project」
- Name：随便填，比如 `llm-gateway`
- Database Password：点「Generate a password」自动生成，**复制保存好**（后面要用）
- Region：选 `Southeast Asia (Singapore)`（离国内近，速度快）
- 点「Create new project」
- 等待约 2 分钟初始化完成

### 3. 获取数据库连接字符串
- 项目创建后，左侧菜单点「Project Settings」（齿轮图标）
- 左侧选「Database」
- 找到「Connection string」区域，选「URI」标签
- 复制那个以 `postgresql://` 开头的字符串
- **重要**：把字符串里的 `[YOUR-PASSWORD]` 替换成你刚才复制的数据库密码
- 最终格式类似：
  ```
  postgresql://postgres:abc123xyz@db.xxxx.supabase.co:5432/postgres
  ```
- 把这串保存好，后面 Render 配置要用

---

## 第二步：上传代码到 GitHub

### 1. 注册 GitHub
- 打开 https://github.com，注册账号（免费）

### 2. 创建仓库
- 点右上角「+」→「New repository」
- Repository name：`llm-gateway`
- 选 Public（公开，免费）
- 点「Create repository」

### 3. 上传代码
把你本地的 `llm-gateway` 文件夹里的所有文件上传到这个仓库：

**方式一：网页上传（最简单）**
- 在仓库页面点「uploading an existing file」
- 把本地 `llm-gateway` 文件夹里的所有文件和文件夹拖进去
- 点「Commit changes」

**方式二：Git 命令行**
```bash
cd llm-gateway
git init
git add .
git commit -m "init"
git branch -M main
git remote add origin https://github.com/你的用户名/llm-gateway.git
git push -u origin main
```

---

## 第三步：注册 Render 并部署

### 1. 注册
- 打开 https://render.com
- 点「Get Started」，用 GitHub 账号登录（不用信用卡）

### 2. 创建 Web Service
- 登录后点「New +」→「Web Service」
- 找到你刚才的 GitHub 仓库 `llm-gateway`，点「Connect」
- Render 会自动检测根目录的 `render.yaml`，点「Apply」应用配置

### 3. 配置环境变量（关键）
在部署页面找到「Environment Variables」区域，需要填 `DATABASE_URL`：
- Key：`DATABASE_URL`
- Value：粘贴你第一步从 Supabase 复制的连接字符串（已替换密码的）
- 点「Add Environment Variable」

其他变量（JWT_SECRET 等）render.yaml 已自动配置，不用管。

### 4. 开始部署
- 点「Create Web Service」
- 等待构建部署，约 3-5 分钟
- 部署成功后，页面上方会显示你的域名，类似：
  ```
  https://llm-gateway-xxxx.onrender.com
  ```

### 5. 验证
在浏览器打开：
```
https://你的域名.onrender.com/health
```
返回 `{"ok":true,...}` 即部署成功。

> **注意**：Render 免费实例 15 分钟无请求会休眠，第一次访问可能需要等 30-50 秒冷启动。之后有请求就会保持运行。

---

## 第四步：添加上游 API 渠道

部署完成后，需要添加你的 LLM API Key 才能使用聊天功能。

用 curl 命令（或 Postman）调用接口：

```bash
# 1. 登录获取 token（用你设置的管理员密码，默认 admin123）
curl -X POST https://你的域名.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 返回 {"token":"eyJhbG...","user":{...}}，复制 token

# 2. 添加渠道（示例：DeepSeek，便宜好用）
curl -X POST https://你的域名.onrender.com/api/channels \
  -H "Authorization: Bearer 你的token" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "DeepSeek",
    "provider": "deepseek",
    "base_url": "https://api.deepseek.com/v1",
    "api_key": "sk-你的DeepSeekKey",
    "models": ["deepseek-chat", "deepseek-coder"],
    "weight": 1,
    "priority": 10
  }'
```

支持任何 OpenAI 兼容接口的服务商：OpenAI、DeepSeek、通义千问、智谱、月之暗面、OpenRouter 等。

---

## 第五步：更新 APP 并安装

1. 打开 `android/app/build.gradle.kts`
2. 找到 `buildConfigField("String", "API_BASE_URL", ...)`
3. 改成你的 Render 域名：
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"https://你的域名.onrender.com\"")
   ```
   注意：**不要加末尾斜杠**
4. 重新编译 APK，安装到手机

---

## 免费额度说明

| 服务 | 免费额度 | 限制 |
|------|---------|------|
| Render | 750 小时/月（1个实例一直跑），512MB RAM | 15分钟无请求休眠，冷启动30-50秒 |
| Supabase | 500MB 数据库，2GB 带宽/月 | 项目7天无活动会暂停（手动恢复即可） |
| 总费用 | **0 元** | 无需信用卡 |

### 避免 Supabase 暂停
- Supabase 免费项目 7 天无数据库活动会被暂停
- 可以设置一个定时任务（比如用 cron-job.org 免费服务）每天访问一次 `/health`，保持活跃
- 或者直接在 Supabase 后台点「Restore」恢复（1分钟搞定）

---

## 常见问题

**Q: 国内访问 Render 慢吗？**
A: Render 服务器在海外，国内访问延迟约 200-500ms。API 中转对延迟不敏感（本身就要调海外 LLM API），可以接受。如果觉得慢，后续可以换国内轻量服务器。

**Q: 休眠了怎么办？**
A: 打开 APP 发第一条消息时会自动唤醒，等 30-50 秒即可。之后保持活跃。可以用 cron-job.org 设个每 10 分钟访问一次 `/health`，保持永不休眠。

**Q: 数据安全吗？**
A: Supabase 数据库在云端，有备份。Render 跑的是你的代码。建议定期用 `backup.sh` 思路导出数据库备份。

**Q: 后续想升级怎么办？**
A: 随时可以把 Render 免费实例升级为付费（$7/月起，不休眠），或者迁移到国内轻量服务器（4核8G 约 ¥200/年首年），代码不用改，只改环境变量。

---

## 快速检查清单

- [ ] Supabase 注册并创建项目，获取连接字符串
- [ ] 代码上传到 GitHub
- [ ] Render 注册并连接 GitHub 仓库
- [ ] Render 配置 DATABASE_URL 环境变量
- [ ] 部署成功，访问 /health 返回 ok
- [ ] 调用 /api/channels 添加上游 API 渠道
- [ ] APP 的 API_BASE_URL 改成 Render 域名
- [ ] 重新编译 APK 并安装
- [ ] APP 登录测试聊天功能
