# LLM Gateway — API 中转站

LLM API 聚合中转平台，包含后端中转服务和安卓客户端。支持多渠道 Key 池轮询、故障自动切换、用量统计、用户鉴权、在线聊天和模型大全。

## 项目结构

```
llm-gateway/
├── server/                 # 后端中转服务 (Node.js + Express + SQLite)
│   ├── src/
│   │   ├── server.js      # 入口
│   │   ├── config.js      # 配置
│   │   ├── db.js          # 数据库初始化
│   │   ├── middleware/     # 鉴权中间件
│   │   ├── routes/         # 路由（auth/chat/keys/stats）
│   │   └── services/       # Key池/转发/计费服务
│   └── package.json
├── android/                # 安卓客户端 (Kotlin + Jetpack Compose)
│   ├── app/src/main/
│   │   ├── java/com/llmgateway/app/
│   │   │   ├── data/       # 数据层（API/模型/存储）
│   │   │   └── ui/         # UI层（主题/屏幕/组件）
│   │   └── res/
│   └── build.gradle.kts
└── docs/
    └── DESIGN.md           # 设计规范
```

## 功能

| 模块 | 说明 |
|------|------|
| 在线聊天 | 多模型切换，流式/非流式对话 |
| 模型大全 | 20+ 主流大模型目录，按分类筛选搜索 |
| API Key 池 | 多上游渠道配置，加权轮询，故障自动跳过 |
| 用户鉴权 | 注册/登录/JWT，用户级 API Key 分发 |
| 用量统计 | 请求数/Token/费用，按日/周/月，按模型分组 |
| 请求日志 | 全量请求记录，状态/延迟/错误追踪 |
| 微调任务 | 微调任务创建与状态管理 |
| OpenAI 兼容 | `/v1/chat/completions` 标准接口，可直接替换 |

## 后端启动

```bash
cd server
cp .env.example .env   # 修改 JWT_SECRET 等
npm install
npm start
# 服务运行在 http://localhost:3000
# 默认管理员: admin / admin123
```

### 配置上游渠道

调用管理员接口添加渠道（也可直接操作 SQLite）：

```bash
curl -X POST http://localhost:3000/api/channels \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OpenAI 主渠道",
    "provider": "openai",
    "base_url": "https://api.openai.com/v1",
    "api_key": "sk-xxx",
    "models": ["gpt-4o", "gpt-4o-mini"],
    "weight": 1,
    "priority": 10
  }'
```

## 安卓构建

### 前置要求
- Android Studio Hedgehog+
- JDK 17
- Android SDK 34

### 步骤

1. 用 Android Studio 打开 `android/` 目录
2. 修改 `app/build.gradle.kts` 中的 `API_BASE_URL` 为你的后端地址
3. 生成签名密钥：
   ```bash
   keytool -genkey -v -keystore llmgateway.jks -keyalg RSA -keysize 2048 -validity 10000 -alias llmgateway
   ```
4. 在 `app/build.gradle.kts` 中配置签名：
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("llmgateway.jks")
               storePassword = "your_password"
               keyAlias = "llmgateway"
               keyPassword = "your_password"
           }
       }
       buildTypes {
           release { signingConfig = signingConfigs.getByName("release") }
       }
   }
   ```
5. 构建 APK：
   ```bash
   cd android
   ./gradlew assembleRelease
   # 输出: app/build/outputs/apk/release/app-release.apk
   ```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/me` | 当前用户 |
| POST | `/v1/chat/completions` | 聊天补全（OpenAI兼容） |
| GET | `/v1/models` | 可用模型列表 |
| GET | `/api/catalog` | 模型大全目录 |
| GET/POST/DELETE | `/api/` | API Key 管理 |
| GET/POST/PUT/DELETE | `/api/channels` | 渠道管理（管理员） |
| GET | `/api/usage` | 用量统计 |
| GET | `/api/logs` | 请求日志 |
| GET/POST | `/api/fine-tunes` | 微调任务 |

## 技术栈

**后端**
- Node.js + Express
- better-sqlite3（零配置数据库，可替换为 PostgreSQL）
- JWT 鉴权
- 原生 fetch 流式转发

**安卓**
- Kotlin + Jetpack Compose（Material3 自定义主题）
- Retrofit + OkHttp
- DataStore（Token 存储）
- Navigation Compose
- Coil（图片加载）

## 设计

浅色 + 黑色极简风格，详见 [docs/DESIGN.md](docs/DESIGN.md)。
