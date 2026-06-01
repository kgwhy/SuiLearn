# SuiLearn

SuiLearn（随心学）是一个面向个人学习和刷题复盘的学习工具项目。当前路线按三阶段推进：

| 阶段 | 目标 | 状态 |
|---|---|---|
| 第一版 | Android 本地学习 App，覆盖内置题库、刷题、错题本、收藏、搜索、知识点和统计 | 已有 Android 工程 |
| 第二版 | Java 后端承载 AI 生成、知识库、资料导入、RAG 和语义搜索；React Web 作为知识库工作台 | 已有后端和 Web 工程 |
| 第三版 | 扩展完整 React Web 学习端 | 后续规划 |

项目当前不做账号、多用户、云同步和 iOS。第一版 Android 保持本地可用；第二版通过后端和 Web 工作台补齐 AI / RAG 能力。

## 项目结构

```text
SuiLearn
├─ apps/android          Native Android 本地学习 App
├─ apps/web              React + TypeScript 知识库工作台
├─ services/api          Java + Spring Boot 后端 API
├─ contracts/openapi     第二版跨端 REST API 契约
├─ docs                  产品、技术选型、架构和协作流程文档
├─ agents                多 Agent 角色边界
├─ build.gradle.kts      Android 根 Gradle 配置
└─ settings.gradle.kts   Gradle module 配置，`:app` 指向 `apps/android`
```

## 技术栈

### Android App

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- ViewModel + StateFlow / SharedFlow
- Coroutines + Flow
- Room
- JSON assets 内置题库
- JUnit、AndroidX Test、Robolectric、Compose UI Test

### Backend API

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- pgvector
- H2 本地默认降级数据库
- `AiProvider` 抽象，默认 `FakeAiProvider`
- OpenAI-compatible Provider 配置预留
- JUnit + Spring Boot Test

### Web 工作台

- React
- TypeScript
- Vite
- lucide-react
- REST API client

### 契约与文档

- OpenAPI：`contracts/openapi/suilearn-v2.yaml`
- 产品需求：`docs/product-requirements.md`
- 技术选型：`docs/tech-selection.md`
- 架构设计：`docs/architecture.md`
- 文档索引：`docs/index.md`

## 启动方式

### 1. Android App

构建 Debug 包：

```powershell
.\gradlew.bat :app:assembleDebug
```

运行单元测试：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

也可以用 Android Studio 打开项目根目录 `SuiLearn`，选择 `app` module 运行。

### 2. Backend API

默认使用 H2 文件库 + Fake AI Provider，不需要 PostgreSQL 或 API Key：

```powershell
mvn -f services/api/pom.xml spring-boot:run
```

运行后端测试：

```powershell
mvn -f services/api/pom.xml test -q
```

本地覆盖配置模板：

```powershell
Copy-Item services/api/config/local.properties.example services/api/config/local.properties
```

不要把真实 API Key 写入模板、文档或提交历史；真实密钥只放在本机环境变量中。

### 3. Local PostgreSQL / pgvector

需要验证 PostgreSQL 或 pgvector 时：

```powershell
cd services/api
docker compose -f compose.local.yml up -d
docker compose -f compose.local.yml exec postgres psql -U suilearn -d suilearn -c "CREATE EXTENSION IF NOT EXISTS vector;"
Copy-Item config/local.properties.example config/local.properties
cd ../..
```

`services/api/config/local.properties.example` 默认连接：

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/suilearn
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=suilearn
spring.datasource.password=suilearn_dev_password
```

这是本地开发默认值，不用于远程、共享或生产环境。

### 4. Fake / Real AI Provider

默认配置：

```properties
suilearn.ai.provider=fake
```

`fake` 模式不需要 API Key，适合本地开发和自动化测试。

真实 Provider 目前是 OpenAI-compatible 配置预留。接入真实适配层后，可在本地配置中切换：

```properties
suilearn.ai.provider=openai-compatible
suilearn.ai.base-url=https://api.openai.com/v1
suilearn.ai.api-key=${SUILEARN_AI_API_KEY}
suilearn.ai.chat-model=gpt-4.1-mini
suilearn.ai.embedding-model=text-embedding-3-small
```

当前仓库已实现 `AiProvider` 接口和 `FakeAiProvider`；真实 `OpenAiCompatibleProvider` 尚未接入。

### 5. Web 工作台

```powershell
cd apps/web
npm install
npm run dev
```

默认 Vite 开发服务监听 `127.0.0.1`。如需构建：

```powershell
npm run build
```

## 常用检查

```powershell
# Android 单元测试
.\gradlew.bat :app:testDebugUnitTest

# 后端测试
mvn -f services/api/pom.xml test -q

# Web 构建
cd apps/web
npm run build
```

## 开发约定

- 先阅读 `AGENTS.md` 和对应 `agents/*.md`，遵守文件归属和质量门禁。
- `docs/chat.md` 是灵感材料，不是正式 PRD。
- `docs/product-requirements.md` 是正式产品需求。
- `docs/tech-selection.md` 和 `docs/architecture.md` 是技术和架构事实源。
- 新增或变更第二版接口时，先更新 OpenAPI 契约，再同步后端和前端实现。
- 本地密钥、`.env`、`config/local.properties` 等开发私有配置不得提交。
