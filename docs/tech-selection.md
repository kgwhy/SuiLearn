# 随心学 SuiLearn 技术选型

## 1. 当前结论

SuiLearn 当前按三阶段推进：

```text
第一版：Android 本地学习 App
第二版：Java 后端 AI / RAG 服务
第三版：React Web 版本
```

当前不考虑 iOS，不做双端跨平台，不做登录、账号、云同步。

## 2. 技术路线总览

| 阶段 | 目标 | 技术选择 |
|---|---|---|
| 第一版 | Android 本地学习工具，覆盖 V0 + V1 + V2 | Kotlin + Jetpack Compose |
| 第二版 | AI 生成题、RAG、文档解析、任务管理 | Java + Spring Boot |
| 第三版 | Web 端刷题、搜索、知识点学习 | React + TypeScript |

## 3. 第一版：Android 本地版

### 3.1 目标

第一版先做 Android 本地 App，完成：

- 内置 Java 八股题库。
- 刷题。
- 答案解析。
- 错题本。
- 本地进度。
- 本地搜索。
- 知识点学习地图。

第一版覆盖：

- V0：概念验证版。
- V1：可日用刷题版。
- V2：主动学习版。

### 3.2 技术选择

| 方向 | 选择 |
|---|---|
| 平台 | Native Android |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | 分层架构 + MVVM / Feature-first |
| 异步 | Kotlin Coroutines + Flow |
| 本地数据 | Room |
| 简单配置 | DataStore |
| 内置题库 | JSON assets + 首次导入 Room |
| 搜索 | Room 查询，后续按需升级 SQLite FTS |
| 导航 | Navigation Compose |
| 依赖注入 | 第一版手动注入，后续再评估 Hilt |
| 测试 | JUnit + AndroidX Test + Compose UI Test |

### 3.3 选择理由

虽然长期求职方向是 Java 后端，但第一版需要一个可真实使用的 Android 入口。Kotlin + Compose 是当前 Android 新项目更合适的路线。

本项目中 Kotlin 的定位是：

> 客户端够用即可，不把主要学习成本投入到 Android 深水区。

第一版重点不在客户端炫技，而在：

- 领域建模。
- 本地数据设计。
- 题库结构。
- 错题规则。
- 搜索。
- 知识点聚合。
- 测试。

这些能力和后端开发更容易迁移。

### 3.4 第一版不做

- iOS。
- Web。
- Flutter。
- 服务端。
- 登录 / 账号系统。
- 云同步。
- AI 生成题。
- RAG。
- 复杂 UI 动画。
- 高级客户端性能优化。

### 3.5 第一版数据结构原则

第一版正式发布目标覆盖 V0 + V1 + V2，因此第一版初始数据结构应直接支持 V2 能力。

V0 / V1 / V2 是内部开发里程碑，不是线上数据库版本演进顺序。

开发期规则：

- 第一版正式发布前，可以破坏性重建本地数据库。
- 第一版正式发布前，可以调整题库 JSON schema。
- 第一版正式发布前，可以清空开发数据重新导入。
- 不承诺兼容开发期旧数据。

发布后规则：

- Room schema version 必须递增。
- 表结构变化必须提供 Migration。
- 题库 JSON schema 必须版本化。
- 题目 ID 和知识点 ID 必须保持稳定。
- 题库更新不能破坏用户答题记录和错题记录。

第一版初始结构需要直接包含：

- 学习包。
- 分类。
- 知识点。
- 题目。
- 题目与知识点关联。
- 答题记录。
- 错题状态。
- 收藏状态。
- 学习统计所需原始记录。

答题记录口径：

- 单选题、多选题、判断题提交后立即写入答题记录。
- 简答题提交后先展示参考答案和解析；用户选择“通过 / 未通过”后写入答题记录。
- 简答题“通过”计为正确，“未通过”计为错误；未通过同时更新错题状态。
- 正确率、进度和薄弱知识点统计统一以 `AnswerRecord` 和 `WrongQuestion` 为事实来源，不按题型排除简答题。

### 3.6 题库 JSON Schema 原则

题库 JSON 需要从第一版开始版本化。

建议题库文件包含：

- schemaVersion。
- packId。
- packName。
- packVersion。
- categories。
- knowledgePoints。
- questions。
- questionKnowledgePointRelations。

ID 规则：

- packId 稳定。
- categoryId 稳定。
- knowledgePointId 稳定。
- questionId 稳定。
- 不使用导入顺序作为业务 ID。
- 修改题目内容时不随意更换 questionId。

题目更新规则：

- 修正文案、解析、标签时保留 questionId。
- 如果题目语义发生根本变化，应创建新 questionId。
- 删除题目时不应直接破坏历史答题记录，可以标记为 deprecated。
- 用户答题记录以 questionId 关联题目。

### 3.7 Room Migration 策略

第一版正式发布前：

- 允许 destructive migration。
- 允许清库重建。
- 允许重导内置题库。

第一版正式发布后：

- 禁止随意 destructive migration。
- 每次 schema 变化必须写 Migration。
- Migration 需要有测试覆盖。
- 题库导入逻辑需要识别 packVersion。
- 导入新题库时不能覆盖用户答题记录、错题状态、收藏状态。

### 3.8 跨阶段模型一致性

第二版引入 Java 后端、第三版引入 React Web 后，三端会出现三套模型：

- Android Kotlin model。
- Java backend model。
- React TypeScript type。

为了避免漂移，第二版开始需要引入 API 契约作为单点真相。

优先策略：

- 第二版后端提供 OpenAPI 文档。
- Android 和 Web 都以后端 API 契约为准。
- 后端 DTO、Android 网络模型、React TypeScript type 需要围绕同一套 API 契约对齐。

第一版本地模型不强行引入 OpenAPI，但命名应尽量贴近后续后端模型：

- StudyPack。
- Category。
- KnowledgePoint。
- Question。
- AnswerRecord。
- WrongQuestion。
- FavoriteQuestion。

## 4. 第二版：Java 后端 AI / RAG

### 4.1 目标

第二版引入后端服务，承载 AI 和 RAG 能力。

第二版覆盖：

- V3：AI 辅助版。
- V4：知识库 / RAG 版。

### 4.2 技术选择

| 方向 | 选择 |
|---|---|
| 语言 | Java |
| 框架 | Spring Boot |
| API | REST API |
| 数据库 | PostgreSQL |
| 缓存 / 任务状态 | Redis，按需引入 |
| 文档解析 | Apache Tika，按需引入 |
| AI 调用 | OpenAI API 或兼容 OpenAI 协议的模型服务 |
| 向量检索 | pgvector 优先，复杂后再评估 Milvus 等 |
| 测试 | JUnit + Spring Boot Test |

### 4.3 主要能力

- AI 生成题。
- 根据错题生成相似题。
- 根据知识点生成解释。
- 根据答题情况生成复习建议。
- 文档导入。
- 文档解析。
- 文本切片。
- embedding。
- 向量检索。
- RAG 问答。
- 生成结果保存前的人工确认。
- 生成结果删除和修正。

### 4.4 后端重点沉淀

第二版是最贴近后端求职的部分，重点沉淀：

- REST API 设计。
- 领域模型设计。
- 数据库表设计。
- 文件上传和解析。
- 异步任务。
- 任务状态管理。
- AI API 封装。
- Prompt 模板管理。
- Token 成本控制。
- 生成内容质量校验。
- 错误记录和失败重试。
- 单元测试和集成测试。

### 4.5 与 Android 的关系

Android App 不直接承担复杂 AI / RAG 逻辑。

Android 负责：

- 发起 AI 生成请求。
- 展示任务状态。
- 展示生成结果。
- 让用户确认、保存、删除或修正生成内容。
- 将确认后的内容保存到本地或同步到后端，具体策略第二版再定。

## 5. 第三版：React Web

### 5.1 目标

第三版支持 Web 网站，让用户可以在浏览器中使用核心学习能力。

第三版覆盖：

- Web 端刷题。
- Web 端搜索。
- Web 端错题复习。
- Web 端知识点学习。
- 后续多学习包扩展。

### 5.2 技术选择

| 方向 | 选择 |
|---|---|
| 前端框架 | React |
| 语言 | TypeScript |
| 构建工具 | Vite |
| 路由 | React Router |
| 状态管理 | 先用 React 内置状态，复杂后再评估 Zustand / TanStack Query |
| UI 组件 | 后续按设计需要选择 |
| API 通信 | 调用 Java Spring Boot 后端 |

### 5.3 选择理由

React 是常见 Web 前端技术栈，适合作为后续 Web 版本。

对后端求职来说，React 的意义不是深入前端，而是帮助理解：

- 前后端接口协作。
- API 设计是否易用。
- Web 端状态流转。
- 后端能力如何被真实用户界面消费。

Web 端不在第一版启动，等 Java 后端 API 稳定后再做。

## 6. 后端求职导向

这个项目虽然从 Android App 起步，但长期学习收益应向后端倾斜。

优先投入：

1. 领域建模。
2. 数据库设计。
3. 查询与搜索。
4. 业务规则。
5. 单元测试。
6. AI / RAG 后端架构。
7. API 设计。
8. 异步任务和错误处理。

控制投入：

1. 复杂 Android UI。
2. 客户端动画。
3. 深度平台适配。
4. 高级客户端性能优化。
5. 复杂前端状态管理。

## 7. 当前推荐组合

### 第一版 Android

```text
Kotlin
Jetpack Compose
Material 3
Navigation Compose
ViewModel
Coroutines + Flow
Room
DataStore
JSON assets
JUnit
Compose UI Test
```

### 第二版后端

```text
Java
Spring Boot
REST API
PostgreSQL
Redis
Apache Tika
OpenAI-compatible API
pgvector
JUnit
Spring Boot Test
```

### 第三版 Web

```text
React
TypeScript
Vite
React Router
Spring Boot API
```
