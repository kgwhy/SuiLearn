# 随心学 SuiLearn 技术与版本基线

## 1. 文档职责

本文是 SuiLearn 的技术选型与版本基线真相源，由架构 Agent 维护。

本文回答：

- 当前使用哪些技术栈。
- 每项技术的最低版本、当前项目版本和升级约束。
- 哪些依赖、平台或基础设施暂不引入。
- 技术升级需要谁确认、修改哪些配置、运行哪些验证。

本文不回答：

- 代码目录如何组织、模块职责如何切分、数据如何流转。这些由 `docs/architecture.md` 维护。
- 产品范围、验收标准和阶段优先级。这些由 `docs/product-requirements.md` 维护。
- API 字段细节和跨端 schema。稳定契约由 `contracts/**` 维护。

## 2. 当前技术路线

SuiLearn 当前采用三端渐进路线：

| 范围 | 定位 | 技术选择 |
|---|---|---|
| Android App | 本地学习闭环，以及轻量消费 AI/知识库能力 | Native Android、Kotlin、Jetpack Compose、Room |
| Java Backend | AI 生成、知识库、资料导入、RAG、语义搜索和任务状态 | Java、Spring Boot、JPA、PostgreSQL、OpenAI-compatible Provider、Spring AI adapter 边界 |
| Web Frontend | 知识库工作台，承载资料导入、生成确认、搜索和问答 | React、TypeScript、Vite |
| Contracts | 跨端 API 单点真相 | OpenAPI |

当前不做 iOS，不做 Flutter，不做账号系统、云同步、社区和多租户权限。

## 3. 全局工程基线

| 项目 | 当前基线 | 约束 |
|---|---|---|
| 开发 JDK | 推荐 JDK 21 | 本地和 CI 可使用 JDK 21；Android 编译字节码目标仍保持 17 |
| Backend Java | Java 21 | 后端 source/target 目标基线为 21；升级工程配置需单独修改 `services/api/pom.xml` 并跑后端测试 |
| Android Kotlin JVM target | 17 | Android 源码语言为 Kotlin；Kotlin 编译目标保持 JVM 17，不使用 Java 21 字节码目标或专属 API |
| Git 分支 | `codex/` 前缀用于 Agent 分支 | 除非用户另有要求 |
| API 契约 | `contracts/openapi/suilearn-v2.yaml` | 服务端、Web 和 Android 远程能力必须按契约消费或适配 |
| 密钥管理 | 环境变量 / 本地配置文件 | API key、Authorization header 和原始密钥不得进入响应、任务表、日志或文档示例 |
| 测试命令 | 以 `AGENTS.md` 和 `docs/development-workflow.md` 为准 | Windows/PowerShell 优先使用 `.bat` 或 `npm --prefix` 命令 |

## 4. Android 基线

| 项目 | 当前版本 / 约束 |
|---|---|
| Gradle module | `:app`，物理目录 `apps/android` |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 2.0.21 |
| Compose compiler plugin | Kotlin Compose plugin 2.0.21 |
| KSP | 2.0.21-1.0.25 |
| compileSdk | 35 |
| targetSdk | 35 |
| minSdk | 26 |
| applicationId | `com.suilearn` |
| versionName / versionCode | 1.0.1 / 2 |
| 源码语言 | Kotlin |
| Kotlin JVM target | 17 |
| UI | Jetpack Compose + Material 3 |
| Compose BOM | 2024.06.00 |
| Navigation | Navigation Compose 2.7.7 |
| Lifecycle | Lifecycle 2.8.3 |
| Room | 2.7.2 |
| AndroidX Test Core | 1.6.1 |
| Robolectric | 4.13 |

Android 约束：

- 保持单 Android module，不因 package 增长提前拆 Gradle 多 module。
- Android 本地闭环必须在未配置服务端、未配置 AI Provider 或网络不可用时继续可用。
- 第一阶段正式发布前可以破坏性重建开发数据；正式发布后 Room schema 变化必须提供 Migration 和测试。
- 依赖注入当前保持手动注入，不引入 Hilt，除非架构 Agent 更新本文并说明收益。
- 本地搜索当前使用 Room 查询；题量扩大到明显影响体验前，不引入 SQLite FTS。
- Android 不直接实现复杂 AI / RAG 逻辑，只消费服务端已定义能力。
- Android 不提升到 Java 21 字节码目标；`compileOptions` 和 `kotlinOptions.jvmTarget` 保持 17。

推荐验证：

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

## 5. Backend 基线

| 项目 | 当前版本 / 约束 |
|---|---|
| 目录 | `services/api` |
| 语言 | Java 21 |
| Framework | Spring Boot 3.5.14 |
| Build | Maven |
| API | REST + JSON |
| Persistence | Spring Data JPA |
| 开发 / 测试数据库 | H2 runtime 可用于测试与本地轻量验证 |
| 目标数据库 | PostgreSQL |
| 向量检索 | pgvector 优先，当前可用 Fake Embedding / 关键词检索兜底 |
| AI Provider | 业务层依赖 `AiProvider`；实现包括 Fake 和 OpenAI-compatible |
| Spring AI | 预留 1.0.x 稳定线；首轮只建立 SuiLearn port 与 `ai/infrastructure/springai/**` adapter 边界，不启用 starter |
| 测试 | Spring Boot Test / JUnit |

Backend 约束：

- 后端 Java source/target 目标基线为 21；当前工程配置升级需由 Backend 任务修改 `services/api/pom.xml` 的 `java.version` 并运行测试确认。
- 业务层不得直接依赖具体 AI 厂商 SDK；必须通过 `AiProvider` 边界。
- 业务模块不得直接 import Spring AI 类型，例如 `ChatClient`、`ChatModel`、`EmbeddingModel`、`VectorStore`、`Advisor` 或 Tool Calling 类型。
- Spring AI 相关代码只允许位于 `services/api/src/main/java/com/suilearn/api/ai/infrastructure/springai/**`。
- 当前阶段不新增 Spring AI Maven 依赖；真正替换 OpenAI-compatible Provider 或启用 Spring AI starter 时，必须由架构 Agent 更新本文、修改 `services/api/pom.xml` 并运行后端测试。
- Provider 状态接口只能暴露脱敏配置，例如 base URL、模型名、超时、重试和 API key 环境变量名。
- 资料导入、embedding、生成内容必须有任务状态或可追踪结果，避免不可解释的后台副作用。
- RAG 回答必须受知识库或资料范围约束；证据不足时表达不确定。
- Redis、分布式队列、独立向量库、真实 PDF 二进制解析、OCR、Office 解析均为后置能力，不能在未确认时引入。

推荐验证：

```powershell
mvn -f services/api/pom.xml test -q
```

## 6. Web Frontend 基线

| 项目 | 当前版本 / 约束 |
|---|---|
| 目录 | `apps/web` |
| Framework | React 19.0.0 |
| Language | TypeScript 5.7.3 |
| Build | Vite 6.0.7 |
| React plugin | `@vitejs/plugin-react` 5.0.4 |
| Icons | `lucide-react` 0.475.0 |
| Node typings | `@types/node` 22.10.2 |
| API base | `VITE_API_BASE_URL`，默认 `/api/v2` |

Web 约束：

- 当前 Web 是知识库工作台，不承载完整刷题学习端。
- Web API client 必须集中在 `apps/web/src/api.ts`，共享类型集中在 `apps/web/src/types.ts`。
- 前端类型应与 OpenAPI 和服务端 DTO 保持一致；不为局部便利私自改变契约语义。
- 不引入复杂状态管理库；React 本地状态足够时不加 Zustand、Redux 或 TanStack Query。
- 不引入大型 UI 组件库，除非已有页面复杂度证明收益明确。

推荐验证：

```powershell
npm --prefix apps/web run build
```

## 7. Contracts 基线

| 项目 | 当前约定 |
|---|---|
| OpenAPI | `contracts/openapi/suilearn-v2.yaml` |
| JSON Schema | `contracts/schemas/**` 预留 |
| Owner | 架构 Agent |

Contracts 约束：

- 契约变更必须先由架构 Agent 完成，再派发 Backend、Android、Web 或 Content 的适配任务。
- 契约变更与消费端适配不得并行写代码。
- 服务端 Controller / DTO、Web `api.ts` / `types.ts`、Android 远程模型必须围绕同一契约对齐。

## 8. 暂不引入

| 类型 | 暂不引入项 | 触发重新评估的条件 |
|---|---|---|
| 平台 | iOS、Flutter | 产品明确需要跨平台移动端 |
| 账号 | 登录、账号、云同步、多租户权限 | 产品规格进入同步或多人场景 |
| Android | Hilt、多 Gradle module、SQLite FTS | 手动注入或 Room 查询成为明确瓶颈 |
| Backend | Redis、分布式 worker、Milvus、Tika、OCR、Office 解析 | 单机 PostgreSQL / 文本解析无法满足已确认需求 |
| Web | 完整刷题学习端、复杂状态管理、大型组件库 | Web 工作台之外的学习端进入当前规格 |
| AI | 多 Provider 路由、成本平台、模型评测系统 | 单 Provider 抽象不足以支撑已确认运营需求 |

## 9. 升级规则

任何技术或版本升级必须满足：

- 由架构 Agent 更新本文。
- 说明升级原因、影响范围、替代方案和回退方式。
- 同步修改对应配置文件，例如 `build.gradle.kts`、`apps/android/build.gradle.kts`、`services/api/pom.xml`、`apps/web/package.json`。
- 运行受影响模块的验证命令，并在交付中粘贴原始结果。
- 如果升级影响契约或跨端模型，先更新 `contracts/**`，再派发消费端适配。

常见升级门禁：

| 升级项 | 必跑验证 |
|---|---|
| AGP / Kotlin / Compose / Room | `.\gradlew.bat :app:testDebugUnitTest --no-daemon` + `.\gradlew.bat :app:assembleDebug --no-daemon` |
| Java / Spring Boot / JPA | `mvn -f services/api/pom.xml test -q` |
| Spring AI starter / model adapter | `mvn -f services/api/pom.xml test -q`，并检查业务模块无 Spring AI 类型 import |
| React / TypeScript / Vite | `npm --prefix apps/web run build` |
| OpenAPI 契约 | 契约 diff 审查 + Backend/Web/Android 相关适配测试 |
