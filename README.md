# SuiLearn 随心学

SuiLearn（随心学）是一个面向个人学习者的本地刷题、错题复盘和 AI 知识库工作台项目。项目先用 Android App 打通离线 Java 面试题学习闭环，再用 Java 后端和 React Web 工作台承载资料导入、AI 生成题、RAG 问答和语义搜索。

第一阶段重点是打开即用：无需登录、无需服务端，也能围绕内置 Java 八股学习包完成刷题、收藏、错题本、搜索、知识点学习和统计。第二阶段在不破坏本地闭环的前提下，引入可确认、可追溯的 AI / RAG 能力。

![SuiLearn 首页示意图](docs/ui-mockups/suilearn-home-v3.png)

## 项目亮点

- Android 离线优先，本地内置 Java 面试学习包。
- 支持单选、多选、判断和简答题四种题型。
- 本地保存答题记录、错题、收藏、搜索、知识点和统计数据。
- Java Spring Boot 后端支持知识库、资料导入、AI 生成草稿、RAG 和语义搜索。
- React + TypeScript Web 工作台承载较重的知识库管理流程。
- 使用 OpenAPI 维护第二版后端与 Web 的跨端接口契约。
- AI 生成内容默认进入待确认状态，用户查看、编辑、保存、丢弃或删除后才会进入正式学习内容。

## 当前状态

| 模块 | 状态 |
| --- | --- |
| Android 本地学习 App | 核心本地闭环已实现 |
| Java 八股学习包 | 已内置第一版 50 道题 |
| Backend AI / RAG API | MVP 已实现，默认 H2 + Fake AI Provider |
| Web 知识库工作台 | MVP 已实现 |
| 完整 Web 学习端 | 后续规划 |
| 登录、云同步、多用户 | 当前阶段不做 |

## 项目结构

```text
SuiLearn
├─ apps/android          Native Android 本地学习 App
├─ apps/web              React + TypeScript 知识库工作台
├─ services/api          Java + Spring Boot 后端 API
├─ contracts/openapi     第二版 OpenAPI 契约
├─ docs                  产品、架构、技术选型和协作流程文档
├─ agents                多 Agent 角色边界和执行规则
├─ build.gradle.kts      Android 根 Gradle 配置
└─ settings.gradle.kts   Gradle module 配置，:app 指向 apps/android
```

## 技术栈

| 模块 | 技术 |
| --- | --- |
| Android | Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel、Coroutines、Flow、Room |
| Backend | Java、Spring Boot、Spring Web、Spring Data JPA、H2、PostgreSQL、pgvector-ready 持久化模型 |
| AI / RAG | `AiProvider` 抽象，默认 `FakeAiProvider`，预留 OpenAI-compatible Provider 配置 |
| Web | React、TypeScript、Vite、lucide-react |
| 契约 | [contracts/openapi/suilearn-v2.yaml](contracts/openapi/suilearn-v2.yaml) |
| 测试 | JUnit、AndroidX Test、Robolectric、Spring Boot Test、TypeScript build checks |

## 快速开始

### 环境要求

- JDK 17 或更高版本。
- Android Studio 或 Android SDK，用于运行 Android App。
- Maven，用于运行后端服务。
- Node.js 和 npm，用于运行 Web 工作台。
- Docker 仅在本地验证 PostgreSQL / pgvector 时需要。

### Android App

构建 Debug 包：

```powershell
.\gradlew.bat :app:assembleDebug
```

运行 Android 单元测试：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

也可以用 Android Studio 打开项目根目录，直接运行 `app` module。

### Backend API

后端默认使用 H2 文件库和 `FakeAiProvider`，不需要数据库服务或 API Key：

```powershell
mvn -f services/api/pom.xml spring-boot:run
```

运行后端测试：

```powershell
mvn -f services/api/pom.xml test -q
```

默认 API 地址：

```text
http://localhost:8080/api/v2
```

### Web 工作台

安装依赖并启动 Vite：

```powershell
cd apps/web
npm install
npm run dev
```

Vite 开发服务默认把 `/api` 代理到 `http://localhost:8080`。构建 Web 应用：

```powershell
npm run build
```

## 本地 PostgreSQL / pgvector

普通本地开发使用 H2 即可。需要验证 PostgreSQL 或 pgvector 行为时：

```powershell
cd services/api
docker compose -f compose.local.yml up -d
docker compose -f compose.local.yml exec postgres psql -U suilearn -d suilearn -c "CREATE EXTENSION IF NOT EXISTS vector;"
Copy-Item config/local.properties.example config/local.properties
cd ../..
```

配置模板使用本地默认值：

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/suilearn
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=suilearn
spring.datasource.password=suilearn_dev_password
```

不要提交真实 API Key、`.env` 文件或 `services/api/config/local.properties`。

## AI Provider 配置

默认配置：

```properties
suilearn.ai.provider=fake
```

`fake` 模式会返回稳定可预测的生成题、解释、复习建议和 embedding，适合本地开发、自动化测试和无密钥的契约联调。

真实 OpenAI-compatible Provider 目前是预留配置，后续接入适配层后可切换：

```properties
suilearn.ai.provider=openai-compatible
suilearn.ai.base-url=https://api.openai.com/v1
suilearn.ai.api-key=${SUILEARN_AI_API_KEY}
suilearn.ai.chat-model=gpt-4.1-mini
suilearn.ai.embedding-model=text-embedding-3-small
```

## 常用检查

提交或交付前建议运行：

```powershell
# Android 单元测试
.\gradlew.bat :app:testDebugUnitTest

# 后端测试
mvn -f services/api/pom.xml test -q

# Web 生产构建
cd apps/web
npm run build
```

## 文档入口

- 产品规格：[docs/product-requirements.md](docs/product-requirements.md)
- 架构设计：[docs/architecture.md](docs/architecture.md)
- 技术选型：[docs/tech-selection.md](docs/tech-selection.md)
- 开发流程：[docs/development-workflow.md](docs/development-workflow.md)
- 文档索引：[docs/index.md](docs/index.md)
- 第二版 API 契约：[contracts/openapi/suilearn-v2.yaml](contracts/openapi/suilearn-v2.yaml)

`docs/chat.md` 是灵感讨论材料，不是正式产品事实源。

## 开发约定

- 修改代码前先阅读 [AGENTS.md](AGENTS.md) 和对应 `agents/*.md` 角色文件。
- 遵守文件归属和角色边界，保持改动范围清晰。
- 第二版 API 变更先更新 OpenAPI，再同步后端和 Web 消费端。
- AI 生成内容未经用户确认，不得进入正式学习内容。
- 更新内置题包时必须保留 Android 本地学习记录。
- 不提交本地密钥、生成凭据或机器私有配置。

## 路线图

| 阶段 | 重点 |
| --- | --- |
| v1 | Android 离线 Java 面试学习闭环 |
| v2 | AI 生成草稿、知识库、资料导入、RAG、语义搜索和 Web 工作台 |
| v3 | 完整 Web 学习端，包括刷题、复盘、搜索和统计 |

## License

当前仓库尚未声明开源许可证。若要作为可复用开源项目发布，请先补充 License。
