# 随心学 SuiLearn 技术方案

## 0. 文档信息

| 项目 | 内容 |
|---|---|
| 文档版本 | v2.1 |
| 维护角色 | 架构 Agent |
| 依据文档 | `docs/product-requirements.md`、`docs/tech-selection.md` |
| 适用阶段 | 当前架构（Android 本地 + Java Spring Boot 后端 + React Web 知识库工作台） |
| 目标读者 | Android Agent、Server Backend Agent、Web Frontend Agent、内容 Agent、测试 Agent |

本文前半部分保留第一版 Android 本地方案，新增第二版架构设计用于承接 PRD v2。第一版能力必须继续离线可用；第二版通过 Java Spring Boot 服务端承载 AI、资料导入、语义搜索和 RAG，Web 前端作为第二版知识库工作台，Android 只消费必要的生成和确认能力。

> **注意：Section 1-20（V1 Android 本地架构）为历史设计文档。实际代码结构与设计文档存在差异，以下标注"实际"的段落反映当前代码的真实结构。V2 当前架构从 Section 21 开始。**

## 1. 技术决策摘要

第一版采用 Native Android 本地架构，不引入服务端、登录、云同步、AI/RAG 或多学习包切换能力。

核心技术栈：

| 层面 | 技术方案 |
|---|---|
| 平台 | Native Android |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 导航 | Navigation Compose |
| 状态管理 | ViewModel + UI State |
| 异步 | Kotlin Coroutines + Flow |
| 本地数据库 | Room |
| 配置存储 | DataStore |
| 内置题库 | `assets` JSON + 首次导入 Room |
| 搜索 | Room LIKE 查询，不引入 SQLite FTS |
| UI 状态 | ViewModel 暴露 StateFlow，单次事件使用 SharedFlow |
| 错误模型 | Repository / UseCase 返回自定义 sealed result |
| 依赖管理 | 第一版手动依赖注入 |
| 测试 | JUnit、AndroidX Test、Room Test、Compose UI Test |

第一版没有独立后端服务。Android module 保持 Gradle 名称 `:app`，物理目录为 `apps/android`。Android Agent 负责完整 Android 客户端实现，包括 UI、ViewModel、本地域模型、UseCase、Repository、Room、题库导入和 Android 相关测试代码。Server Backend Agent 在第一版不创建服务端项目，后续第二版再介入 Java Spring Boot。

## 2. 架构目标

第一版技术方案服务于三个产品闭环：

- 刷题闭环：顺序、随机、分类、知识点、错题重刷都能进入同一套练习流程。
- 复盘闭环：答错自动进入错题本，支持已掌握标记、收藏和知识点聚合。
- 主动学习闭环：本地搜索、知识点详情、关联题目和专项练习能互相跳转。

架构上需要保证：

- 题库内容和用户学习记录分离，更新题库不覆盖用户记录。
- 统计由原始答题记录和状态表计算或派生，不只保存不可追溯的结果。
- 题目、分类、知识点 ID 稳定，为后续 Java 后端和 Web 版本保留模型一致性。
- UI 不直接访问 Room，通过 UseCase / Repository 获取数据。
- “继续刷题”有明确可恢复的本地会话状态。
- 第一版可快速实现，但模型不为了 demo 牺牲后续演进。

## 3. 总体分层 (V1 设计)

> **与实际代码的差异：以下为 V1 设计稿中的分层结构。实际 Android 代码未按 `core/database/`、`core/repository/` 等 package 组织，而是采用更扁平的结构。以下分层作为后续重构的参考目标。**

推荐采用 Feature-first + 分层架构。

```text
app
├─ core
│  ├─ database
│  ├─ datastore
│  ├─ model
│  ├─ repository
│  ├─ import
│  └─ common
├─ feature
│  ├─ home
│  ├─ practice
│  ├─ category
│  ├─ wrongbook
│  ├─ favorite
│  ├─ search
│  ├─ knowledge
│  ├─ statistics
│  └─ settings
└─ navigation
```

第一版采用单 Android module，并按 package 划分上述结构，不拆 Gradle 多 module。

**实际代码结构（当前）：**

```text
apps/android/src/main/java/com/suilearn/
├─ MainActivity.kt
├─ SuiLearnApp.kt
├─ data/
│   └─ AssetQuestionPackSource.kt
├─ di/
│   └─ AppDependencies.kt
├─ feature/
│   ├─ ai/
│   ├─ categories/
│   ├─ common/
│   ├─ favorites/
│   ├─ home/
│   ├─ knowledge/
│   ├─ practice/
│   ├─ search/
│   ├─ settings/
│   ├─ statistics/
│   └─ wrongbook/
├─ core/remote/
├─ navigation/
├─ theme/
├─ ui/
└─ viewmodel/
```

取舍理由：

- 第一版只有一个 Android 本地 App，没有服务端和 Web 端并行实现。
- 题量为 50 道，业务域清晰，单 module 足以支撑实现和测试。
- 过早拆 module 会增加 Gradle 配置、依赖边界和构建维护成本。
- 通过 package 边界先保持结构清晰，后续代码规模明显增长后再评估拆分。

## 4. 模块职责

### 4.1 core/model

定义跨层领域模型，命名尽量贴近后续 Java 后端模型：

- `StudyPack`
- `Category`
- `KnowledgePoint`
- `Question`
- `QuestionOption`
- `AnswerRecord`
- `WrongQuestion`
- `FavoriteQuestion`
- `PracticeSession`
- `PracticeMode`
- `StatisticsSummary`
- `MasteryLevel`

领域模型不携带 Compose UI 状态，不直接等同 Room Entity。

### 4.2 core/database

负责 Room Entity、DAO、Database、Migration。

Room 只保存两类数据：

- 内置内容数据：学习包、分类、知识点、题目、选项、题目知识点关系。
- 用户学习数据：答题记录、错题状态、收藏状态、设置和导入状态。

### 4.3 core/import

负责从 `assets/question_pack_java_interview.json` 导入内置题库。

导入规则：

- App 启动时检查 `packId + packVersion`。
- 如果本地没有该学习包，则全量导入。
- 如果本地已有相同版本，则跳过。
- 如果本地已有旧版本，第一版正式发布前允许清库重导；正式发布后需要增量导入策略。
- 导入题库不得删除或覆盖用户答题记录、错题状态、收藏状态。

导入与启动体验：

- 题库导入在 IO dispatcher 后台执行，不在主线程阻塞 Compose 首屏。
- Home 在题库未就绪时展示轻量 loading，并禁用刷题入口。
- `InitializeQuestionPackUseCase` 完成后刷新 Home 数据，刷题入口可用。
- 第一版 50 道题要求从冷启动到进入第一题不超过 10 秒；该预算包含导入时间。
- 如果导入失败，Home 展示可重试错误状态，不进入空题库刷题页。

### 4.4 core/repository

Repository 是 UI 和数据源之间的边界。

建议拆分：

- `StudyPackRepository`：学习包、分类、知识点基础数据。
- `QuestionRepository`：题目查询、详情、题目列表。
- `PracticeRepository`：构建练习队列、提交答案、下一题。
- `PracticeSessionRepository`：保存和恢复继续刷题会话。
- `WrongQuestionRepository`：错题记录、已掌握标记、错题聚合。
- `FavoriteRepository`：收藏、取消收藏、收藏列表。
- `SearchRepository`：本地搜索题目和知识点。
- `StatisticsRepository`：首页、分类、知识点、总览统计。
- `SettingsRepository`：学习包信息、重置学习记录、版本信息。

### 4.5 feature/*

每个 feature 包含：

- `*Route`：接收导航参数，连接 ViewModel。
- `*Screen`：纯 UI 展示。
- `*ViewModel`：组织 UI State 和用户事件。
- `*UiState`：页面状态。
- `*Event` 或明确的方法：处理点击、提交、筛选等交互。

UI 层不直接持有 Entity，不直接调用 DAO。

ViewModel 约定：

- 页面连续状态使用 `StateFlow<UiState>` 暴露。
- Toast、Snackbar、导航这类单次事件使用 `SharedFlow<UiEvent>`。
- Repository 查询列表或详情时优先返回 `Flow`，一次性写入操作使用 `suspend`。
- UseCase 可以组合 `Flow` 或提供 `suspend` 方法，但不向 UI 暴露 Room Entity。

## 5. 页面与路由

第一版一级页面：

| 页面 | Route | 主要能力 |
|---|---|---|
| 首页 | `home` | 学习入口、今日统计、薄弱知识点 |
| 刷题 | `practice?mode=&targetId=&sessionId=` | 所有练习模式共用，支持恢复会话 |
| 分类 | `categories` | 分类列表、进度、正确率 |
| 错题本 | `wrongbook?knowledgePointId=` | 错题列表、已掌握筛选、重练 |
| 收藏 | `favorites` | 收藏题列表、取消收藏 |
| 搜索 | `search?query=` | 题目和知识点搜索 |
| 知识点 | `knowledge` | 知识点列表和分组 |
| 知识点详情 | `knowledge/{knowledgePointId}` | 说明、关联题、统计、专项练习 |
| 统计 | `statistics` | 总览、分类、知识点、最近记录 |
| 设置 | `settings` | 学习包信息、重置记录、版本 |

练习入口统一使用 `PracticeMode`：

```kotlin
enum class PracticeMode {
    SEQUENTIAL,
    RANDOM,
    CATEGORY,
    KNOWLEDGE_POINT,
    WRONG_QUESTION,
    FAVORITE
}
```

`targetId` 的含义由 `mode` 决定，例如分类 ID、知识点 ID；顺序和随机模式可以为空。

`sessionId` 用于继续刷题。首页“继续刷题”入口优先读取最近一个未完成的 `PracticeSession`，带 `sessionId` 进入刷题页。

## 6. 练习会话

### 6.1 会话目标

练习会话用于落地“继续刷题”入口。它只保存恢复刷题所需的最小状态，不承担统计事实来源；统计仍以 `AnswerRecord` 为准。

### 6.2 会话创建

进入任一练习模式时创建或复用一个 `PracticeSession`：

- 用户从首页继续刷题：恢复最近一个 `IN_PROGRESS` 会话。
- 用户从顺序、随机、分类、知识点、错题、收藏入口开始：创建新会话。
- 如果同一入口已有未完成会话，第一版仍创建新会话，避免入口语义复杂化。

### 6.3 会话状态

状态：

```text
IN_PROGRESS
COMPLETED
ABANDONED
```

保存字段：

- `session_id`
- `practice_mode`
- `target_id`
- `question_ids`
- `current_index`
- `status`
- `created_at`
- `updated_at`

`question_ids` 使用 JSON 字符串保存本次队列快照。随机刷题必须保存随机后的队列，恢复时不重新随机。

### 6.4 会话更新

- 每次进入下一题时更新 `current_index` 和 `updated_at`。
- 用户完成队列最后一题后标记 `COMPLETED`。
- 用户从其他入口开始新会话时，不强制废弃旧会话；首页继续刷题只取最近 `IN_PROGRESS` 会话。
- 如果会话中的题目后来被标记废弃，恢复时跳过废弃题，队列全部不可用则标记 `ABANDONED`。

## 7. 领域规则

### 7.1 题型

题型使用枚举：

```text
SINGLE_CHOICE
MULTIPLE_CHOICE
TRUE_FALSE
SHORT_ANSWER
```

判题规则：

- 单选题：用户答案等于标准答案。
- 多选题：用户答案集合与标准答案集合完全一致，不考虑顺序。
- 判断题：用户答案等于标准答案。
- 简答题：第一版不自动判语义正确。用户输入答案并提交后展示参考答案和解析，由用户自行判断“通过 / 未通过”。

简答题自评结果写入学习记录并参与统计：`PASSED` 计为正确，`NOT_PASSED` 计为错误；`NOT_PASSED` 同时进入错题本。

简答题自评枚举：

- `PASSED`
- `NOT_PASSED`

### 7.2 答题记录

单选题、多选题、判断题每次提交都写入一条 `AnswerRecord`，不覆盖历史记录。

简答题分两步处理：用户提交文本答案后，页面展示参考答案和解析；用户选择 `PASSED` 或 `NOT_PASSED` 后写入一条 `AnswerRecord`。简答题用户输入内容作为 `user_answer` 持久化，用于最近记录和后续统计追溯。

记录内容包括：

- 题目 ID。
- 练习模式。
- 用户答案。
- 是否正确。
- 答题时间。
- 用时。

统计以 `AnswerRecord` 为事实来源。

### 7.3 错题状态

答错后 upsert `WrongQuestion`。

状态：

```text
ACTIVE      默认错题，出现在错题本
MASTERED    已掌握，默认错题列表隐藏
```

规则：

- 客观题答错后进入 `ACTIVE`。
- 简答题用户选择 `NOT_PASSED` 后进入 `ACTIVE`；选择 `PASSED` 不进入错题本。
- 用户标记已掌握后改为 `MASTERED`，写入 `mastered_at`。
- 已掌握错题仍保留记录，可通过筛选查看。
- 已掌握题如果再次答错，状态恢复为 `ACTIVE`，`wrong_count` 继续累加，`last_wrong_at` 更新为本次时间，`mastered_at` 置空。
- 已掌握题如果再次答对，状态保持 `MASTERED`，不更新 `wrong_count`、`last_wrong_at` 和 `mastered_at`。
- 同一题在同一会话中重复提交时，每次完整作答都写入 `answer_records`；只有答错或简答题自评未通过时更新 `wrong_questions`。

### 7.4 收藏状态

收藏使用独立表 `FavoriteQuestion`，与错题互不影响。

规则：

- 收藏和取消收藏只改变收藏表。
- 题目是否废弃不影响已有收藏记录，但列表展示时需要提示题目已废弃。

### 7.5 进度、正确率和掌握情况

第一版不强制保存所有派生统计表，优先通过查询聚合生成。

建议统计口径：

- 已刷题数：至少有一条答题记录的去重题目数。
- 总正确率：提交正确次数 / 提交次数，同题重复提交会重复计入分子和分母；简答题通过计入正确，未通过计入错误。
- 分类进度：分类下已练习题目数 / 分类下未废弃题目总数。
- 知识点进度：知识点下已练习题目数 / 知识点下未废弃题目总数。
- 错题数：`WrongQuestion.status = ACTIVE` 的题目数。
- 薄弱知识点：按 ACTIVE 错题数降序，再按正确率升序。

知识点掌握情况是派生状态，不单独存表。前端消费 `MasteryLevel`：

```text
NOT_STARTED     该知识点下没有答题记录
WEAK            ACTIVE 错题数 > 0，或正确率 < 60%
LEARNING        无 ACTIVE 错题，且 60% <= 正确率 < 80%
MASTERED        无 ACTIVE 错题，且正确率 >= 80%，且已练习题目数 = 关联题目总数
```

当知识点下没有答题记录时，掌握状态为 `NOT_STARTED`。

### 7.6 连续学习天数

如果前端展示连续学习天数，切日规则统一按设备本地时区的 0 点划分自然日。

## 8. Room 数据模型

### 8.1 内容表

`study_packs`

| 字段 | 类型 | 说明 |
|---|---|---|
| pack_id | TEXT PK | 稳定业务 ID |
| name | TEXT | 学习包名称 |
| description | TEXT | 学习包说明 |
| pack_version | INTEGER | 内容版本 |
| schema_version | INTEGER | JSON schema 版本 |
| imported_at | INTEGER | 导入时间 |

`categories`

| 字段 | 类型 | 说明 |
|---|---|---|
| category_id | TEXT PK | 稳定业务 ID |
| pack_id | TEXT | 所属学习包 |
| name | TEXT | 分类名 |
| description | TEXT | 描述 |
| sort_order | INTEGER | 排序 |

`knowledge_points`

| 字段 | 类型 | 说明 |
|---|---|---|
| knowledge_point_id | TEXT PK | 稳定业务 ID |
| pack_id | TEXT | 所属学习包 |
| category_id | TEXT | 所属分类 |
| name | TEXT | 名称 |
| description | TEXT | 简短说明 |
| sort_order | INTEGER | 排序 |

`questions`

| 字段 | 类型 | 说明 |
|---|---|---|
| question_id | TEXT PK | 稳定业务 ID |
| pack_id | TEXT | 所属学习包 |
| category_id | TEXT | 所属分类 |
| type | TEXT | 题型 |
| stem | TEXT | 题干 |
| answer | TEXT | 标准答案，JSON 字符串 |
| explanation | TEXT | 解析 |
| difficulty | INTEGER | 难度 1-5 |
| is_deprecated | INTEGER | 是否废弃 |
| sort_order | INTEGER | 顺序刷题排序 |

`question_options`

| 字段 | 类型 | 说明 |
|---|---|---|
| option_id | TEXT PK | 可用 `questionId_optionKey` |
| question_id | TEXT | 所属题目 |
| option_key | TEXT | A/B/C/D 或 TRUE/FALSE |
| content | TEXT | 选项内容 |
| sort_order | INTEGER | 排序 |

`question_knowledge_points`

| 字段 | 类型 | 说明 |
|---|---|---|
| question_id | TEXT | 题目 ID |
| knowledge_point_id | TEXT | 知识点 ID |

联合主键：`question_id + knowledge_point_id`。

### 8.2 用户数据表

`answer_records`

| 字段 | 类型 | 说明 |
|---|---|---|
| record_id | TEXT PK | UUID |
| question_id | TEXT | 题目 ID |
| practice_mode | TEXT | 练习模式 |
| target_id | TEXT NULL | 模式目标 ID |
| user_answer | TEXT | 用户答案，JSON 字符串 |
| is_correct | INTEGER | 是否正确 |
| duration_ms | INTEGER | 用时 |
| answered_at | INTEGER | 答题时间 |

`answer_records` 记录单选题、多选题、判断题和已完成自评的简答题。简答题在用户选择通过 / 未通过后写入，`is_correct` 对应自评是否通过。

`practice_sessions`

| 字段 | 类型 | 说明 |
|---|---|---|
| session_id | TEXT PK | UUID |
| practice_mode | TEXT | 练习模式 |
| target_id | TEXT NULL | 模式目标 ID |
| question_ids | TEXT | 题目 ID 队列 JSON |
| current_index | INTEGER | 当前题目位置 |
| status | TEXT | IN_PROGRESS / COMPLETED / ABANDONED |
| created_at | INTEGER | 创建时间 |
| updated_at | INTEGER | 最近更新时间 |

`wrong_questions`

| 字段 | 类型 | 说明 |
|---|---|---|
| question_id | TEXT PK | 题目 ID |
| status | TEXT | ACTIVE / MASTERED |
| wrong_count | INTEGER | 累计答错次数 |
| first_wrong_at | INTEGER | 首次答错时间 |
| last_wrong_at | INTEGER | 最近答错时间 |
| mastered_at | INTEGER NULL | 标记掌握时间 |

`favorite_questions`

| 字段 | 类型 | 说明 |
|---|---|---|
| question_id | TEXT PK | 题目 ID |
| created_at | INTEGER | 收藏时间 |

`app_settings`

| 字段 | 类型 | 说明 |
|---|---|---|
| key | TEXT PK | 设置项 |
| value | TEXT | 设置值 |

简单设置也可以放 DataStore；如果设置需要参与查询，再进入 Room。

`app_settings` 第一版只保存全局配置和导入状态，不保存学习进度。建议 key：

- `current_pack_id`
- `last_imported_pack_version:{packId}`
- `last_imported_schema_version:{packId}`

### 8.3 索引建议

必须添加：

- `questions(pack_id)`
- `questions(category_id)`
- `questions(type)`
- `questions(is_deprecated)`
- `question_options(question_id)`
- `knowledge_points(category_id)`
- `question_knowledge_points(question_id)`
- `question_knowledge_points(knowledge_point_id)`
- `answer_records(question_id)`
- `answer_records(answered_at)`
- `practice_sessions(status)`
- `practice_sessions(updated_at)`
- `wrong_questions(status)`
- `favorite_questions(created_at)`

搜索对 `stem`、`explanation`、`answer` 做 LIKE 查询。第一版题量为 50 道，且题库不会频繁修改，不引入 FTS 表。

## 9. 题库 JSON 契约

文件建议：

```text
apps/android/src/main/assets/question_pack_java_interview.json
```

结构：

```json
{
  "schemaVersion": 1,
  "packId": "java_interview_v1",
  "packName": "Java 八股学习包",
  "packVersion": 1,
  "description": "面向 Java 后端面试复习的第一版内置学习包",
  "categories": [],
  "knowledgePoints": [],
  "questions": []
}
```

分类结构：

```json
{
  "categoryId": "java_concurrency",
  "name": "并发",
  "description": "Java 并发基础、线程池、锁和 JMM",
  "sortOrder": 4
}
```

第一版分类 ID 固定如下：

| 分类名 | categoryId |
|---|---|
| JVM | `jvm` |
| Java 基础 | `java_basics` |
| 集合 | `collections` |
| 并发 | `concurrency` |
| Spring / Spring Boot | `spring` |
| MySQL | `mysql` |
| Redis | `redis` |
| 计算机网络 | `computer_networks` |
| 操作系统 | `operating_system` |
| 设计模式 | `design_patterns` |
| 项目场景题 | `project_scenarios` |

ID 命名规范：

- 只使用小写英文字母、数字和下划线。
- 不使用中文、空格、斜杠或导入顺序。
- `knowledgePointId` 使用领域概念英文或常见缩写，例如 `jmm`、`volatile`、`hashmap`。
- `questionId` 建议格式为 `{categoryId}_{topic}_{seq}`，例如 `concurrency_volatile_001`。
- ID 一经进入正式题库不得为了文案调整而修改。

知识点结构：

```json
{
  "knowledgePointId": "volatile",
  "categoryId": "java_concurrency",
  "name": "volatile",
  "description": "可见性、有序性和不保证原子性的关键字",
  "sortOrder": 1
}
```

题目结构：

```json
{
  "questionId": "java_concurrency_volatile_001",
  "categoryId": "java_concurrency",
  "type": "SINGLE_CHOICE",
  "stem": "关于 volatile 的说法，哪一项是正确的？",
  "options": [
    { "key": "A", "content": "volatile 可以保证复合操作的原子性" },
    { "key": "B", "content": "volatile 可以保证可见性和一定的有序性" }
  ],
  "answer": ["B"],
  "explanation": "volatile 能保证变量写入对其他线程可见，并通过内存屏障约束重排序，但不能保证复合操作原子性。",
  "difficulty": 2,
  "knowledgePointIds": ["volatile", "java_memory_model"],
  "sortOrder": 1,
  "deprecated": false
}
```

简答题结构：

```json
{
  "questionId": "jvm_gc_roots_001",
  "categoryId": "jvm",
  "type": "SHORT_ANSWER",
  "stem": "简述 GC Roots 通常包括哪些对象。",
  "options": [],
  "answer": [
    "虚拟机栈中引用的对象、方法区静态属性引用的对象、方法区常量引用的对象、本地方法栈 JNI 引用的对象等。"
  ],
  "explanation": "GC Roots 是可达性分析的起点。回答时需要覆盖栈、本地方法栈、静态属性和常量等常见来源。",
  "difficulty": 3,
  "knowledgePointIds": ["gc_roots"],
  "sortOrder": 1,
  "deprecated": false
}
```

约束：

- `questionId`、`categoryId`、`knowledgePointId` 一经发布必须稳定。
- `answer` 统一使用数组，判断题可用 `["TRUE"]` / `["FALSE"]`，简答题可用一个或多个参考答案字符串。
- 多选答案数组不表达顺序。
- 内容 Agent 产出的题库需要通过 schema 校验后再进入 App。
- 题目的 `categoryId` 必须存在于分类表。
- 题目的 `knowledgePointIds` 必须存在于知识点表。
- 第一版 `knowledgePoint` 归属一个主分类；题目可以引用同分类或跨分类知识点。跨分类引用允许存在，用于表达 JMM、缓存、网络协议等横跨概念，但内容校验需要输出跨分类引用清单供内容 Agent 复核。

difficulty 标准：

| difficulty | 含义 |
|---|---|
| 1 | 记忆型基础概念，答案直接明确 |
| 2 | 常见面试基础题，需要区分相近概念 |
| 3 | 需要解释机制或流程，属于主流中等题 |
| 4 | 需要综合多个知识点或分析场景 |
| 5 | 高难度深入题，涉及源码、性能权衡或复杂排障 |

## 10. 核心 UseCase

建议将复杂业务规则收敛到 UseCase：

- `InitializeQuestionPackUseCase`
- `GetHomeSummaryUseCase`
- `BuildPracticeSessionUseCase`
- `ResumePracticeSessionUseCase`
- `SubmitAnswerUseCase`
- `EvaluateShortAnswerUseCase`
- `ToggleFavoriteQuestionUseCase`
- `MarkWrongQuestionMasteredUseCase`
- `SearchLearningContentUseCase`
- `GetKnowledgePointDetailUseCase`
- `GetStatisticsSummaryUseCase`
- `ResetLearningDataUseCase`

### 10.1 BuildPracticeSessionUseCase

输入：

- `PracticeMode`
- `targetId`

输出：

- 题目 ID 列表。
- 当前题目索引。
- 会话元信息。

不同模式规则：

- 顺序刷题：按 `questions.sort_order` 升序，过滤废弃题。
- 随机刷题：过滤废弃题后随机排序。
- 分类刷题：按 `category_id` 过滤。
- 知识点刷题：通过 `question_knowledge_points` 过滤。
- 错题重刷：取 `wrong_questions.status = ACTIVE`。
- 收藏练习：取 `favorite_questions`。

### 10.2 ResumePracticeSessionUseCase

步骤：

1. 查询最近一个 `status = IN_PROGRESS` 的会话，按 `updated_at` 倒序。
2. 校验 `question_ids` 中仍有未废弃题目。
3. 如果会话可恢复，返回队列、当前索引和当前题目。
4. 如果会话不可恢复，标记 `ABANDONED`，首页隐藏继续刷题入口。

### 10.3 SubmitAnswerUseCase

步骤：

1. 获取题目和标准答案。
2. 按题型计算结果。
3. 客观题写入 `answer_records`。
4. 客观题如果答错，upsert `wrong_questions` 为 `ACTIVE`。
5. 客观题如果答对且原来是错题，不自动移出错题本，除非用户明确标记已掌握。
6. 返回答案、解析、是否正确、错题状态和收藏状态。

第 5 点是刻意取舍：避免用户偶然答对一次就丢失复盘目标。

### 10.4 EvaluateShortAnswerUseCase

步骤：

1. 接收用户输入的简答题答案。
2. 展示参考答案和解析。
3. 接收用户选择的 `PASSED` 或 `NOT_PASSED`。
4. 写入 `answer_records`，`PASSED` 对应 `is_correct = true`，`NOT_PASSED` 对应 `is_correct = false`。
5. 如果用户选择 `NOT_PASSED`，upsert `wrong_questions` 为 `ACTIVE`。
6. 返回解析、参考答案、自评结果、错题状态和收藏状态。

## 11. 搜索方案

第一版采用本地关键词搜索。

搜索目标：

- 题干。
- 选项。
- 答案。
- 解析。
- 分类名称。
- 知识点名称。
- 知识点描述。

返回类型：

```text
QUESTION
KNOWLEDGE_POINT
```

结果字段：

- `id`
- `type`
- `title`
- `summary`
- `categoryName`
- `difficulty`
- `hasAnswered`
- `hasWrongRecord`
- `matchedFields`

查询策略：

- 输入去除首尾空格。
- 空关键词不发起搜索，展示空态。
- LIKE 查询使用转义后的 `%keyword%`。
- 用户输入中的 `%`、`_`、`\` 必须转义，SQL 使用 `ESCAPE '\'`。
- 题目结果和知识点结果可合并展示，也可用 Tab 分开展示。

不升级 FTS 的判断：

- 第一版正式范围是 50 道题。
- 搜索字段虽然覆盖题干、选项、答案、解析、分类和知识点，但数据量很小。
- 题库不会频繁修改，维护 FTS 同步表的收益不足。
- LIKE 查询能满足第一版验收标准，且实现和测试成本最低。

## 12. 统计方案

统计不要散落在 UI 层，统一由 `StatisticsRepository` 或 UseCase 输出。

首页统计：

- 总已刷题数。
- 总正确率。
- ACTIVE 错题数量。
- 错题最多的 3 个知识点。
- 最近一次练习时间。
- 最近一个可恢复练习会话。

分类统计：

- 分类题目总数。
- 已练习题目数。
- 正确率。
- ACTIVE 错题数。

知识点统计：

- 关联题目总数。
- 已练习题目数。
- 正确率。
- ACTIVE 错题数。
- `MasteryLevel` 掌握状态。

最近学习记录：

- 从 `answer_records` 按 `answered_at` 倒序取。
- 展示题干摘要、分类、结果和时间。

## 13. 设置与重置

设置页第一版只实现：

- 当前学习包信息。
- 重置本地学习记录。
- App 版本。

第一版不提供学习记录导出能力。

重置范围：

- 删除 `answer_records`。
- 删除 `wrong_questions`。
- 删除 `favorite_questions`。
- 删除 `practice_sessions`。
- 保留学习包、分类、知识点、题目、选项、题目知识点关系。
- 保留 `app_settings` 中的题库导入状态和当前学习包配置。

重置操作需要二次确认。

## 14. 错误处理

第一版错误分三类：

- 题库导入错误：阻塞核心体验，展示可重试的错误页或错误状态。
- 数据查询错误：展示空态和重试，不让 App 崩溃。
- 数据写入错误：保留当前页面状态，提示保存失败，可再次提交。

题库导入失败需要记录日志，方便开发阶段定位 JSON schema 或内容错误。

代码层错误返回统一使用自定义 sealed result，避免 UI 直接捕获底层异常：

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data class ImportError(val message: String, val cause: Throwable? = null) : AppError
    data class DataError(val message: String, val cause: Throwable? = null) : AppError
    data class ValidationError(val message: String) : AppError
}
```

Repository 和 UseCase 对 UI 暴露 `AppResult` 或包含错误字段的 `UiState`，日志记录底层异常，界面只展示可理解的错误文案和重试入口。

## 15. 测试策略

### 15.1 单元测试

覆盖：

- 客观题判题规则。
- 简答题两段式流程：提交后展示参考答案和解析，自评后写入学习记录。
- 错题状态流转。
- 收藏切换。
- 练习队列构建。
- 会话创建、恢复和完成。
- 统计口径。
- 搜索结果映射。
- LIKE 搜索关键词转义。

### 15.2 Room 测试

覆盖：

- 题库导入后基础表数量正确。
- 题目与知识点关系正确。
- 客观题答题记录写入正确。
- 简答题自评后答题记录写入正确。
- 错题 upsert 正确。
- 已掌握错题默认隐藏但可筛选。
- 重置学习记录不删除题库内容。
- `practice_sessions` 可保存和恢复。
- Migration 测试覆盖发布后的 schema 变更。

### 15.3 Compose UI 测试

覆盖验收主路径：

- 10 秒内从首页进入第一题。
- 题库导入中 Home 展示 loading，导入完成后刷题入口可用。
- 首页继续刷题能恢复最近会话。
- 单选题提交后展示答案和解析。
- 多选题支持多选。
- 判断题支持提交。
- 简答题支持输入后查看参考答案，并由用户选择通过或未通过。
- 答错后出现在错题本。
- 收藏后出现在收藏列表。
- 搜索关键词能进入题目或知识点详情，空关键词展示空态。

### 15.4 内容校验

内容 Agent 产出的 JSON 需要校验：

- 50 道题数量达标。
- 每题有分类、知识点、答案、解析。
- 题型只使用允许枚举。
- 题目引用的分类和知识点必须存在。
- 跨分类知识点引用需要输出校验清单。
- 单选和判断题答案数量为 1。
- 多选题答案数量大于 1。
- 简答题允许没有 options，但必须有参考答案和解析。
- difficulty 必须在 1-5 范围内，并按本文标准填写。

## 16. Agent 实现分工（当前实际状态）

> **V1 设计文档中的分工描述与实际状态已不同步。以下标注当前各 Agent 的实际职责边界。Section 16.1-16.6 为 V1 设计内容，保留作为参考。**

**当前实际状态：**

| Agent | 当前状态 | 实际负责模块 |
|---|---|---|
| Android Agent | 已交付 V1 本地闭环 + V2 远程入口 | `apps/android/**` |
| Server Backend Agent | 已交付 V2 REST API + AI/RAG 服务端 | `services/api/**`（controller, service, model, persistence, retrieval, ai, config） |
| Web Frontend Agent | 已交付 V2 知识库工作台 | `apps/web/**`（App.tsx, api.ts, types.ts） |
| 内容 Agent | 已交付题库 JSON | `apps/android/src/main/assets/question_pack_java_interview.json` |
| 架构 Agent | 维护 contracts + architecture doc | `contracts/openapi/suilearn-v2.yaml`、`docs/architecture.md`、`docs/tech-selection.md` |
| 测试 Agent | 后端测试覆盖 SuiLearnV2Service | `services/api/src/test/**` |

### 16.1 产品 Agent

第一版已无阻塞实现的产品待确认项。以下决策按本文执行，不再由实现 Agent 自行调整：

- 答对错题不自动移出错题本，必须由用户手动标记已掌握。
- 搜索空关键词展示空态。
- 简答题由用户自行判断通过或未通过，并按自评结果写入学习记录。
- 第一版不导出学习记录。

### 16.2 内容 Agent

内容 Agent 负责实现和维护内置题库内容文件，不实现 App 代码。

负责文件：

- `apps/android/src/main/assets/question_pack_java_interview.json`
- 如果项目新增内容校验脚本，内容 Agent 负责让题库通过该脚本。

交付内容：

- 分类。
- 知识点。
- 50 道题。
- 每题稳定 ID。
- 每题分类、知识点、答案、解析。
- 按固定分类 ID 和 difficulty 标准填写。

不得修改：

- Android UI、ViewModel、Repository、UseCase、Room Entity、DAO。
- `docs/tech-selection.md` 和本文架构决策，除非用户明确要求内容 Agent 跨角色处理。

### 16.3 Android Agent

Android Agent 是第一版 Android App 的完整实现者。Android 内部需要保持 UI / presentation、domain、data 分层，但这些层不拆成长期独立 Agent；只有大任务需要并行时，Leader 才在任务卡中临时拆分 Android UI、Android Domain/Data、Android Test 子任务。

负责实现：

- Android 项目结构、Gradle 配置、Version Catalog、R8 release 配置。
- Compose UI、Navigation、ViewModel、UiState、UiEvent。
- `core/model` 领域模型。
- `core/database` Room Entity、DAO、Database、Migration。
- `core/datastore` 和 `app_settings` 的本地配置读写。
- `core/import` 题库导入逻辑和导入失败重试。
- `core/repository` 所有本地 Repository。
- `core/usecase` 所有 UseCase，包括 `BuildPracticeSessionUseCase`、`ResumePracticeSessionUseCase`、`SubmitAnswerUseCase`。
- `feature/*` 下所有 Compose 页面、Route、ViewModel、UiState、UiEvent。
- Navigation Compose 路由，包括 `practice?mode=&targetId=&sessionId=`。
- 首页继续刷题入口和 `practice_sessions` 恢复逻辑。
- 本地 LIKE 搜索及 `%`、`_`、`\` 转义。
- 统计、知识点掌握状态、错题状态流转、收藏和重置学习记录。
- Android 端单元测试、Room 测试和 Compose UI 测试的代码落地。

实现约束：

- UI 层不得直接调用 DAO。
- 页面状态统一通过 `StateFlow<UiState>` 暴露。
- 单次事件使用 `SharedFlow<UiEvent>`。
- Repository / UseCase 对 UI 返回 `AppResult` 或映射后的 `UiState` 错误状态。
- 所有刷题模式必须进入同一个刷题页。
- 设置页重置学习记录必须二次确认。

不得实现：

- 服务端、登录、云同步、AI/RAG、远程接口调用。
- FTS 搜索。
- Gradle 多 module 拆分。
- React Web 前端。

### 16.4 Server Backend Agent

Server Backend Agent 第一版不实现服务端代码、不创建 Spring Boot 项目、不提供 REST API。第二版启动后，Server Backend Agent 负责 Java Spring Boot 服务端、数据库、API、AI / RAG 和服务端测试。

第一版可参与评审，但不拥有 Android 代码：

- Room 数据模型评审结论，确认 `StudyPack`、`Category`、`KnowledgePoint`、`Question`、`AnswerRecord`、`WrongQuestion`、`FavoriteQuestion`、`PracticeSession` 命名与后续 Java 后端模型兼容。
- DAO 查询口径评审，重点覆盖统计、搜索、错题聚合、知识点掌握状态和继续刷题会话恢复。
- 题库 JSON 契约评审，确认 ID 稳定性、版本字段、deprecated 规则和跨分类知识点引用校验可迁移到后续后端。

不得实现：

- 第一版 Android App 代码。
- React Web 前端。
- 内容题库正文。

第二版后负责：

- Spring Boot REST API。
- PostgreSQL、Redis、OpenAPI、RAG、AI 生成题。
- 服务端领域模型、数据库迁移、异步任务和服务端测试。

### 16.5 Web Frontend Agent

Web Frontend Agent 第一版不创建 Web 项目。第二版启动后，负责 React + TypeScript 知识库工作台；第三版再扩展完整 Web 学习端。

第二版负责：

- React 页面、路由、组件和浏览器端状态。
- 调用 Server Backend API。
- 知识库创建、列表、详情、重命名和删除。
- 资料导入、资料详情、资料删除、导入状态展示。
- AI 生成题、解释、复习建议和 RAG 问答结果的确认、保存、丢弃或删除。
- 语义搜索、资料问答和引用片段查看。
- 知识库详情中的题目列表和学习统计轻量查看。
- Web 前端测试和接口契约消费层。

第三版后新增负责：

- Web 端刷题、搜索、错题复习、收藏、学习记录、统计和知识点学习等完整学习流程。
- 将第二版知识库工作台沉淀的组件和 API 消费层扩展到完整学习端。

不得实现：

- 第一版 Android App 代码。
- Java Spring Boot 服务端。
- 内容题库正文。

### 16.6 测试 Agent

测试 Agent 负责把第一版验收标准转成可执行测试范围，并验证 Android Agent 与内容 Agent 的交付物。

负责交付：

- 测试计划和测试用例。
- 单元测试覆盖建议。
- Room 测试覆盖建议。
- Compose UI 测试覆盖建议。
- 手工验收 checklist。

必须覆盖：

- 刷题入口：顺序、随机、分类、知识点、错题、收藏。
- 继续刷题会话恢复。
- 四类题型，其中简答题在用户自评后写入学习记录。
- 本地持久化。
- 错题状态流转和收藏状态。
- 搜索空态、转义字符和普通关键词。
- 知识点掌握状态。
- 题库导入 loading、失败重试和 10 秒进入第一题。
- 重置学习记录边界。

不得修改：

- 产品需求范围。
- 架构决策。
- 题库内容正文，除非用户明确要求测试 Agent 修复测试数据。

## 17. 替代方案与取舍

### 17.1 Flutter vs Native Android

不选 Flutter。

原因：

- 第一版只做 Android，不需要跨平台收益。
- Kotlin + Compose 更贴近 Android 原生生态。
- 当前项目重点是领域建模和本地数据，不应引入额外跨平台复杂度。

### 17.2 纯 JSON 读取 vs Room

不采用纯 JSON 运行时读取。

原因：

- 错题、收藏、答题记录、统计和搜索都需要结构化查询。
- Room 能更清晰地区分内容数据和用户数据。
- 后续迁移到后端数据库时模型更接近。

### 17.3 FTS vs LIKE 查询

第一版使用 LIKE 查询，不引入 FTS。

原因：

- 初始题量 50 道，LIKE 足够。
- 题库不会频繁修改，不需要为 FTS 维护额外同步链路。
- FTS 会增加导入、同步和测试复杂度。
- 通过 `SearchRepository` 保留未来替换空间，但第一版不做。

### 17.4 Hilt vs 手动注入

第一版先手动注入。

原因：

- 当前依赖图简单。
- 减少学习和调试成本。
- 等 Repository、UseCase 数量稳定后再评估 Hilt。

### 17.5 答对错题自动移出 vs 手动标记已掌握

本方案选择手动标记已掌握。

原因：

- 产品需求明确有“标记已掌握”动作。
- 偶然答对一次不代表真正掌握。
- 错题本作为复盘工具，应避免过早移除薄弱题。

## 18. 后续演进边界

第一版发布前：

- Room 可以 destructive migration。
- JSON schema 可以调整。
- 开发数据可以清空重导。

第一版发布后：

- Room schema 必须递增。
- 表结构变化必须写 Migration。
- JSON schema 必须版本化。
- 题目、分类、知识点 ID 必须稳定。
- 题库增量更新不得破坏用户记录。

第二版 Java 后端启动时：

- `contracts/**` 由架构 Agent 负责，作为跨端契约单点真相。
- `contracts/openapi/**` 保存 OpenAPI 契约，定义 Server Backend、Android 和 Web 的 API 边界。
- `contracts/schemas/**` 保存 JSON schema 和跨端数据结构约束，例如题库内容 schema。
- 后端 DTO、Android 网络模型、React TypeScript 类型围绕 OpenAPI 对齐。
- 本地 `StudyPack`、`Question`、`KnowledgePoint` 等命名优先沿用。
- Server Backend Agent 负责实现契约；Android Agent、Web Frontend Agent 和 Content Agent 负责消费或遵守契约，不拥有契约变更权。

Gradle module 演进边界：

- 第一版保持单 Android module。
- 只有当代码规模明显增长、多人并行开发冲突增多、或第二版/第三版需要共享模型与契约时，再评估拆分。

## 19. 工程基线

第一版工程基线：

| 项目 | 决策 |
|---|---|
| minSdk | 26 |
| targetSdk | 使用项目创建时 Android Gradle Plugin 支持的最新稳定 targetSdk |
| R8 | release 构建开启 |
| Version Catalog | 使用 `libs.versions.toml` 管理依赖版本 |
| 日志 | Debug 使用 Android Logcat；Release 不接入远程日志和崩溃上报 |
| 崩溃上报 | 第一版不引入 Firebase Crashlytics 等外部服务 |

理由：

- 第一版是本地个人学习工具，不需要远程日志和崩溃平台。
- minSdk 26 覆盖足够广，同时减少过旧系统适配成本。
- Version Catalog 在项目早期使用成本低，后续依赖增长时收益明显。

## 20. 后续待确认风险

- 如果第一版题量从 50 道大幅扩展到数百题以上，搜索是否升级 FTS 需要重新评估。
- 如果 Android 实现阶段出现明显模块膨胀，再重新评估 Gradle module 拆分。

## 21. 第二版架构决策摘要

第二版以服务端优先落地，不把 AI / RAG 逻辑塞回 Android。核心边界如下：

| 范围 | 决策 |
|---|---|
| 服务端 | `services/api`，Java + Spring Boot REST API |
| API 契约 | `contracts/openapi/suilearn-v2.yaml` 作为跨端单点真相 |
| 数据库 | PostgreSQL，向量检索优先 pgvector |
| 异步任务 | 资料解析、embedding、题目生成使用任务模型；MVP 可先同步返回待确认结果 |
| 文档解析 | MVP 接收文本化内容和文件名/sourceType；Markdown / TXT 可直接解析，PDF 二进制解析由后续 Apache Tika 适配层接入 |
| AI 调用 | OpenAI-compatible Provider 接口封装，业务层不直接依赖具体厂商 SDK |
| Web 前端 | 第二版新增知识库工作台，承载资料导入、生成结果确认、问答和语义搜索 |
| Android | 保留第一版本地闭环；第二版只接入生成入口、任务状态和确认结果消费 |

第二版不引入账号、多用户、云同步和自动发布机制。服务端可以先按单用户本地部署或开发环境运行，所有 API 仍保留 `knowledgeBaseId`、`sourceId` 和追溯字段，避免后续扩展时重写核心模型。

第二版 MVP 与后置范围按以下边界执行：

| 能力 | MVP 先做 | 后置 |
|---|---|---|
| 持久化 | PostgreSQL 作为真实集成前的必选落点；核心业务表和状态机先建齐 | 多租户、账号隔离、复杂权限、跨端云同步 |
| 向量检索 | `material_chunks.embedding` 使用 pgvector；只索引资料片段 | 独立向量库、题目全量向量化、混合重排模型 |
| AI Provider | 业务层依赖 `AiProvider` 抽象；默认可用 Fake Provider；真实 OpenAI-compatible Provider 作为基础设施适配，配置只暴露非密钥字段 | 多 Provider 路由、成本预算面板、复杂模型评测 |
| 资料解析 | Markdown / TXT / 已文本化 PDF 内容进入解析链路 | 真实 PDF 二进制解析、OCR、Office 文档解析 |
| 任务模型 | 资料导入、embedding、生成题保留任务状态；MVP 可同步执行，但必须写入 `generation_tasks` 并通过 `/api/v2/tasks/{taskId}` 查询 | 分布式队列、后台 worker 集群、复杂重试调度 |
| Web 工作台 | 承载知识库、资料导入、生成确认、问答、语义搜索 | 完整 Web 刷题学习端 |
| Android 接入 | 保留第一版本地闭环，只接入生成入口、状态展示、确认结果消费 | Android 完整知识库工作台、端侧 RAG、端侧 embedding |

## 22. 第二版模块划分

**设计目标结构：**

```text
services/api
├─ api              REST Controller、请求/响应 DTO
├─ application      UseCase / Service，编排业务流程
├─ domain           KnowledgeBase、Material、Chunk、GeneratedContent 等领域模型
├─ infrastructure
│ ├─ persistence   PostgreSQL / pgvector Repository
│ ├─ document      Markdown、TXT、PDF 解析适配
│ ├─ ai            OpenAI-compatible 调用适配
│ └─ task          任务状态、重试、错误记录
└─ config           CORS、AI Provider、存储、任务配置
```

**实际代码结构（当前）：**

```text
services/api/src/main/java/com/suilearn/api/
├─ SuiLearnApiApplication.java
├─ ai/                   AI Provider 实现（FakeAiProvider, OpenAiCompatibleAiProvider）
├─ config/               配置类（AppConfig, SuiLearnAiProperties）
├─ controller/           REST Controller（KnowledgeBaseController, AiProviderController, TaskController）
├─ dto/                  请求/响应 DTO
├─ material/             资料解析
├─ model/                领域模型（KnowledgeBase, LearningMaterial, MaterialChunk, GeneratedContent 等）
├─ persistence/          JPA Entity + Repository + SuiLearnV2Store
├─ retrieval/            检索（KeywordRetriever, EmbeddingProvider, FakeEmbeddingProvider）
└─ service/              业务服务（SuiLearnV2Service, AiProviderStatusService）

contracts
├─ openapi/              suilearn-v2.yaml（REST API 契约）
└─ schemas/              （预留，当前为空）

apps/web/src/
├─ App.tsx               Web 知识库工作台主页面
├─ api.ts                API 客户端
├─ types.ts              TypeScript 类型定义
└─ styles.css

apps/android
└─ 第一版本地闭环 + 第二版远程能力入口（AiKnowledgeEntryScreen, AiRemoteApiClient）

服务端领域层必须保持与第一版核心命名一致：`StudyPack`、`Category`、`KnowledgePoint`、`Question`、`AnswerRecord`、`WrongQuestion`、`FavoriteQuestion`。第二版新增模型不替代第一版模型，而是围绕知识库和 AI 内容沉淀扩展：

- `KnowledgeBase`：知识库边界，隔离资料、问答、搜索和生成结果。
- `LearningMaterial`：用户导入资料，记录类型、状态、来源和所属知识库。
- `MaterialChunk`：资料切片，是 RAG 引用和语义搜索的最小单位。
- `GeneratedContent`：AI 生成内容的统一待确认池。
- `GeneratedQuestionDraft`：待确认题目草稿，保存后转换为正式 `Question`。
- `SavedAiNote`：用户保存的解释、复习建议或问答内容。
- `SourceRef`：生成来源，可指向知识点、错题、资料、资料片段或知识库。
- `GenerationTask`：资料导入、embedding 和 AI 生成的统一任务记录；即使同步执行也必须写最终状态，供 Web / Android 展示。

服务端实现边界：

- `domain` 只表达领域对象和状态，不依赖 Spring、JPA、AI SDK 或 pgvector 类型。
- `application` 编排导入、生成、问答、搜索和确认保存流程，只依赖 Repository、DocumentParser、Chunker、EmbeddingService、AiProvider 等接口。
- `infrastructure.persistence` 负责 PostgreSQL / pgvector 表映射、事务和查询，不向 Controller 暴露 Entity。
- `infrastructure.ai` 只实现 Provider 适配，不承载生成题、RAG 问答或内容保存业务规则。
- `api` DTO 以 `contracts/openapi/suilearn-v2.yaml` 为准；新增字段先改 OpenAPI，再实现 DTO。

## 23. 第二版核心流程

### 23.0 AI Provider 分层

AI 调用必须通过服务端内部接口封装，业务层不直接调用任何厂商 SDK。建议最小接口：

```text
AiProvider
├─ generateQuestion(request): GeneratedQuestionCandidate
├─ generateExplanation(request): AiNoteCandidate
├─ generateReviewSuggestion(request): AiNoteCandidate
├─ answerWithContext(request): RagAnswerCandidate
└─ embed(texts): List<EmbeddingVector>
```

Provider 分层：

- `FakeAiProvider`：MVP 和测试默认可用，返回结构稳定、可预测的生成题、解释、建议、RAG 回答和 embedding 向量。用于端到端开发、契约验证和无密钥环境。
- `OpenAiCompatibleProvider`：真实 Provider，封装 OpenAI API 或兼容 OpenAI 协议的模型服务。配置项只在 `config` / `infrastructure.ai` 中出现，不能泄漏到 `application` 或 Controller。
- `AiProviderProperties`：保存 `providerType`、`baseUrl`、`model`、`embeddingModel`、超时、重试次数等配置；API key 只从环境变量或本地开发配置读取，不写入仓库文档示例之外的代码。

Provider 配置字段契约：

| 字段 | MVP 语义 | 是否可返回给客户端 |
|---|---|---|
| `providerType` | `fake` 或 `openai-compatible` | 可返回为枚举 |
| `baseUrl` | OpenAI-compatible API 基础地址 | 可返回，但不应包含 token |
| `chatModel` | 生成题、解释、建议、RAG 使用的模型 | 可返回 |
| `embeddingModel` | chunk embedding 使用的模型 | 可返回 |
| `embeddingDimensions` | pgvector 维度，需与表结构和索引一致 | 可返回 |
| `timeoutMs` / `maxRetries` | 调用超时和重试上限 | 可返回 |
| `apiKeyEnvName` | API key 所在环境变量名，例如 `SUILEARN_AI_API_KEY` | 可返回变量名，不返回值 |
| `apiKey` / Authorization header | 真实密钥或派生密钥 | 禁止返回、禁止写入任务表、禁止进入日志 |

MVP 先使用 Fake Provider 打通业务状态机和前后端契约；真实 OpenAI-compatible Provider 属于 P0 基础设施适配，必须复用同一接口和测试用例。真实 Provider 返回内容必须经过结构校验、来源校验和状态机写入后才能暴露给客户端，不能直接把模型原始响应透传为正式题库内容。`/api/v2/ai/provider-status` 只返回脱敏后的可用性与非密钥配置，供端侧判断 AI 功能是否可用。

### 23.0.1 任务状态模型

第二版统一使用 `generation_tasks` 表记录资料导入、embedding 和 AI 生成任务。MVP 可以在同一个 HTTP 请求内同步执行，但执行前必须创建任务，执行后必须写入 `SUCCEEDED` 或 `FAILED`；客户端统一通过 `/api/v2/tasks/{taskId}` 查询最终状态。

任务类型：

```text
MATERIAL_IMPORT
EMBEDDING
QUESTION_GENERATION
KNOWLEDGE_POINT_EXTRACTION
EXPLANATION_GENERATION
REVIEW_SUGGESTION_GENERATION
```

任务生命周期：

```text
QUEUED
RUNNING
SUCCEEDED
FAILED
CANCELLED
```

任务记录只保存输入摘要、范围 ID、Provider 类型、模型名、进度、错误摘要和结果引用；不保存大段资料正文、模型原始响应、API key 或 Authorization header。同步 MVP 的最小写入顺序是：`QUEUED -> RUNNING -> SUCCEEDED`，失败路径是：`QUEUED -> RUNNING -> FAILED` 并写入可展示的 `errorCode` / `errorMessage`。

### 23.1 AI 生成题

1. 客户端从知识点、错题、资料或知识库发起生成请求。
2. 服务端创建 `GenerationTask`，记录来源、范围、题型偏好和 prompt 参数。
3. `application` 根据 `SourceRef` 读取上下文；资料来源必须来自未删除的 `MaterialChunk`，错题和知识点来源必须属于请求的知识库或学习范围。
4. AI Provider 返回题干、选项、答案、解析、分类、知识点和来源说明；真实 Provider 的原始响应只在适配层解析，不作为 API 响应或正式题库内容直接透传。
5. 服务端执行结构校验和基本质量校验，生成携带 `categoryId` / `categoryName` / `knowledgePointIds` 的 `GeneratedQuestionDraft`。
6. 草稿进入 `PENDING_REVIEW`，客户端展示确认页。
7. 用户选择保存、编辑后保存、删除或丢弃。
8. 保存后转换为保留同一分类与知识点字段的正式题目，后续可进入刷题、错题、收藏、搜索和统计。

### 23.2 资料导入与知识点提取

1. 用户在知识库中上传 Markdown、TXT 或 PDF 来源资料；MVP 契约接收文本化后的内容、文件名和 `sourceType`，不承诺已解析真实 PDF 二进制。
2. 服务端保存 `LearningMaterial`，状态从 `UPLOADED` 进入 `PARSING`。
3. Document Parser 输出纯文本与结构信息；Markdown 保留标题层级，TXT 按空行和长度分段，PDF 在 MVP 只消费已经文本化的内容。
4. Chunker 按标题、段落和长度切片，形成 `MaterialChunk`，每个 chunk 保存 `ordinal`、`titlePath`、`content`、`tokenEstimate` 和 `sourceRef`。
5. Embedding Service 对 chunk 生成向量并写入 `material_chunks.embedding`；Fake Provider 环境可写入确定性伪向量，保证检索链路可测试。
6. 状态依次流转为 `CHUNKING`、`INDEXING`、`READY`；任一步失败写入 `FAILED` 和错误摘要，保留重试入口。
7. Knowledge Extractor 基于 chunk 生成候选知识点，进入可编辑状态；用户可以编辑或删除提取出的知识点。

资料导入至少产生一个 `MATERIAL_IMPORT` 任务；进入 embedding 阶段时产生或关联一个 `EMBEDDING` 任务。`LearningMaterial.importTaskId` 必须存在，`embeddingTaskId` 在 chunk embedding 开始后写入。资料状态和任务状态不是同一个字段：资料状态表达资料是否可用于搜索/RAG，任务状态表达本次处理动作的执行结果。

资料删除采用软删除优先：`LearningMaterial` 标记为 `DELETED` 后，不再参与 RAG、语义搜索和新生成任务；关联的 `MaterialChunk` 与 embedding 失效或异步清理。已经保存为正式题目、AI 笔记、解释或复习建议的内容默认保留，但必须保留 `SourceRef`、设置 `SourceRef.deleted = true`，并在详情中提示来源资料已删除；仍处于 `PENDING_REVIEW` 的生成内容默认标记为 `DELETED`，避免用户继续保存不可追溯草稿。

### 23.3 RAG 问答

1. 用户必须指定知识库或单份资料范围提问，禁止隐式全局问答。
2. Query Service 对问题生成 query embedding，并在指定范围内检索 `READY` 且未删除资料的 chunk；查询 embedding 不入库，只用于本次检索。
3. 检索结果按 pgvector 相似度、资料状态和来源范围过滤；MVP 不做跨知识库全局召回，不做复杂重排。
4. RAG Service 只基于候选片段组织回答，prompt 中必须带 chunk id、资料标题和片段内容。
5. 如果证据不足，返回 `uncertain = true`、`uncertaintyReason` 和“不确定 / 资料中未找到明确依据”提示；不得编造没有 citation 的确定答案。
6. 回答必须携带引用片段，客户端可跳转到资料详情；用户选择保存后才写入 `ai_notes`。

### 23.4 语义搜索

语义搜索统一返回多类型结果：

- `QUESTION`
- `KNOWLEDGE_POINT`
- `MATERIAL_CHUNK`
- `GENERATED_CONTENT`

搜索必须显式限定范围，客户端至少传入 `knowledgeBaseId` 或 `materialId`，禁止隐式全局搜索。若同时传入二者，`materialId` 必须属于该 `knowledgeBaseId`，结果需要展示类型、标题或摘要、相关度 `score`、所属知识库、关联知识点和详情入口。

MVP 的语义向量只索引资料 chunk。题目、知识点和已保存 AI 内容可以通过 `SourceRef` 关联的 chunk、标题/正文关键词或后续摘要 embedding 进入结果集；在未实现对应向量化前，不得假装存在题目或知识点 embedding。embedding 不可用时允许降级关键词搜索，但返回结果仍必须受知识库/资料范围约束。

## 24. 第二版 API 边界

API 契约由 `contracts/openapi/suilearn-v2.yaml` 维护，第一批接口覆盖：

- 知识库：创建、列表、详情、重命名、删除。
- 资料：导入、列表、详情、删除、知识点提取。
- AI 生成：生成题、相似题、复习建议、结果确认、编辑保存、丢弃。
- 任务：按 `taskId` 查询资料导入、embedding 和生成任务状态；同步 MVP 也必须返回可查询任务。
- AI Provider：查询脱敏 Provider 可用性和非密钥配置，不暴露 API key。
- RAG：知识库问答、单资料问答。
- 语义搜索：题目、知识点、资料片段、已保存生成内容。
- 知识库工作台轻量学习视图：知识库下题目列表、知识库学习统计。

实现 Agent 不得绕过契约直接扩展端侧私有接口。新增接口先由架构 Agent 更新 OpenAPI，再由 Server Backend、Android 和 Web 分别消费。

端侧接入边界：

- Web 工作台是第二版主入口，必须完整消费知识库、资料、知识点提取、生成确认、RAG 问答和语义搜索接口。
- Web 工作台只做知识库详情中的题目列表和学习统计轻量查看，不实现完整刷题、错题复习、收藏和学习记录闭环。
- Android 第二版只在第一版页面上增加必要远程入口，例如从知识点、错题或题目详情发起生成、查看任务或生成结果、确认保存或丢弃。
- Android 不承载资料导入、资料详情、知识库批量管理、完整语义搜索工作台或 RAG 对话工作台；这些由 Web 工作台优先承载。
- Android 未配置服务端或 AI Provider 不可用时，第一版本地刷题、错题、收藏、统计和搜索必须完全可用。

## 25. 第二版数据表草案

服务端首批表：

- `knowledge_bases`
- `learning_materials`
- `material_chunks`
- `knowledge_points`
- `questions`
- `generated_contents`
- `generation_tasks`
- `ai_notes`
- `source_refs`

PostgreSQL 表模型边界：

| 表 | MVP 职责 | 不放入该表的内容 |
|---|---|---|
| `knowledge_bases` | 知识库元数据、软删除状态、创建/更新时间 | 用户账号、权限、同步设备信息 |
| `learning_materials` | 资料元数据、`sourceType`、原始文件名、文本化内容或存储引用、导入状态、错误摘要 | chunk 正文、embedding、生成结果正文 |
| `material_chunks` | 资料切片正文、顺序、标题路径、token 估算、pgvector embedding、索引状态 | AI 回答正文、正式题目、用户学习记录 |
| `knowledge_points` | 第二版知识库内提取或生成的知识点、说明、软删除状态 | 第一版 Android 本地 Room 记录的直接同步副本 |
| `questions` | 已保存进入正式题库的题目、题型、答案、解析、分类、知识点归属、来源追溯 | `PENDING_REVIEW` 草稿、模型原始响应 |
| `generated_contents` | AI 生成题、解释、建议、RAG 回答等待确认或已处理内容的状态池 | 已保存后的正式题目主数据、资料 chunk |
| `generation_tasks` | 生成、解析、embedding 等任务状态、输入摘要、错误、重试次数 | 大段资料正文、Provider 原始密钥 |
| `ai_notes` | 用户保存的解释、复习建议、RAG 回答 | 未经用户保存的一次性模型输出 |
| `source_refs` | 生成内容、题目、笔记与知识点/错题/资料/chunk/知识库的追溯关系 | 业务对象正文、跨表权限规则 |

首批不建表的内容：

- 不建 `users`、`accounts`、`subscriptions`、`devices`，第二版不做账号和商业化。
- 不建完整 Web 学习记录表；Web 完整刷题闭环留到第三版。
- 不把 Android 第一版 Room 数据做云同步镜像；后端只保存第二版服务端产生或用户导入的内容。
- 不引入独立向量库表结构；pgvector 直接挂在 `material_chunks.embedding`，后续确有规模压力再迁移。

向量字段仅存在于 `material_chunks` 和后续明确需要语义检索的摘要表中。业务表不直接依赖向量库语义；语义检索失败时仍可通过关键词搜索和资料列表降级。

pgvector / embedding 字段约束：

- `material_chunks.embedding` 使用 pgvector 类型，维度必须与 `suilearn.ai.embedding-dimensions` 或所选 embedding 模型一致。
- `material_chunks.embedding_model` 保存模型名，`embedding_status` 保存 `PENDING`、`INDEXING`、`READY`、`FAILED` 或 `INVALIDATED`。
- 删除资料或切换 embedding 模型后，旧 chunk embedding 必须从检索范围排除；可以异步清理，但不得继续用于 RAG。
- API 不返回原始向量，只返回 `embeddingStatus`、`embeddingModel`、`embeddingDimensions` 等非敏感元数据。

最小字段要求：

- 所有业务表使用稳定字符串 ID 或 UUID，保留 `created_at`、`updated_at`；软删除表增加 `deleted_at`。
- `learning_materials` 必须保存 `knowledge_base_id`、`title`、`file_name`、`source_type`、`status`、`error_message`。
- `material_chunks` 必须保存 `knowledge_base_id`、`material_id`、`ordinal`、`content`、`embedding`、`embedding_model`、`source_ref`。
- `generated_contents` 必须保存 `knowledge_base_id`、`type`、`status`、`payload_json`、`source_refs`、`saved_target_id`。
- `generation_tasks` 必须保存 `kind`、`status`、`knowledge_base_id`、`material_id`、`generated_content_id`、`provider_type`、`model`、`progress_percent`、`current_step`、`error_code`、`error_message`、`retry_count`、`result_ref`。
- `source_refs` 必须能表达 `type`、`source_id`、`knowledge_base_id`、`material_id`、`chunk_id`、`deleted` 和 `excerpt`。

知识库详情轻量统计字段语义：

- `materialCount`：未删除资料数量。
- `knowledgePointCount`：未删除、归属该知识库的知识点数量。
- `questionCount`：已保存正式题目数量，包含用户确认保存的 AI 题。
- `generatedContentCount`：未软删除的生成内容数量，包含待确认、已保存和已丢弃记录。
- `aiNoteCount`：用户保存的 AI 笔记数量。
- `answeredQuestionCount`：至少有一次答题记录的正式题目数量。
- `answerCount`：该知识库正式题目的答题尝试总数。
- `correctRate`：`correctAnswerCount / answerCount`，没有答题记录时返回 `null` 或省略，不返回占位 0。
- `wrongQuestionCount`：当前仍未掌握的错题数量。
- `weakKnowledgePointIds`：由错题或低正确率证据推导；没有证据时返回空数组。

AI 生成内容状态：

```text
PENDING_REVIEW
SAVED
DISCARDED
DELETED
```

资料导入状态：

```text
UPLOADED
PARSING
CHUNKING
INDEXING
READY
FAILED
DELETED
```

## 26. 第二版质量门禁

必须覆盖的测试：

- 知识库边界：搜索、问答、生成结果不会串到其他知识库。
- AI 内容可控：生成结果默认是 `PENDING_REVIEW`，不会自动进入正式题库。
- 追溯字段：生成题、解释、建议至少保留一个 `SourceRef`。
- 不确定性表达：资料不足时 RAG 返回 `uncertain = true`。
- 第一版兼容：未配置 AI 服务时，Android 本地刷题、错题、收藏和统计不受影响。
- Provider 分层：Fake Provider 和真实 Provider 使用同一接口，业务测试不依赖真实模型服务。
- 资料导入状态机：成功路径必须覆盖 `UPLOADED -> PARSING -> CHUNKING -> INDEXING -> READY`，失败路径必须写入 `FAILED` 和错误摘要。
- 任务查询：资料导入、embedding、生成题、解释和复习建议即使同步完成，也能通过 `/api/v2/tasks/{taskId}` 查询到 `SUCCEEDED` 或 `FAILED`。
- Provider 脱敏：`/api/v2/ai/provider-status` 不返回 API key、Authorization header 或原始密钥。
- 统计字段：知识库详情统计必须由持久化数据计算，不允许返回常量占位。

服务端 MVP 可先用内存 Repository 验证 API 和业务状态机，但进入真实集成前必须切换 PostgreSQL，并补充 repository / integration test。

## 27. 第二版本地运行配置

第二版本地运行默认遵循“无密钥可启动”的原则：后端默认使用 H2 文件库和 `FakeAiProvider`，开发者只有在需要验证 PostgreSQL / pgvector 或真实 AI Provider 时才显式覆盖配置。

### 27.1 配置文件边界

后端默认配置位于 `services/api/src/main/resources/application.properties`，只保留可提交的默认值和环境变量占位符。它额外导入 `services/api/config/local.properties`：

```properties
spring.config.import=optional:file:./config/local.properties
```

`config/local.properties` 只用于本机开发，不提交仓库；模板见 `services/api/config/local.properties.example`。该模板包含数据库连接、AI Provider 模式、OpenAI-compatible Provider 的 base URL、模型名和 API key 占位符，但不包含真实密钥。真实 API key 必须从环境变量读取，例如 `SUILEARN_AI_API_KEY`。

### 27.2 本地 PostgreSQL / pgvector

需要验证真实 PostgreSQL 或 pgvector 行为时，在 `services/api` 目录运行：

```bash
docker compose -f compose.local.yml up -d
```

首次启动后启用 pgvector 扩展：

```bash
docker compose -f compose.local.yml exec postgres psql -U suilearn -d suilearn -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

然后复制配置模板：

```bash
cp config/local.properties.example config/local.properties
```

Windows PowerShell 可使用：

```powershell
Copy-Item config/local.properties.example config/local.properties
```

模板默认连接：

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/suilearn
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=suilearn
spring.datasource.password=suilearn_dev_password
spring.jpa.hibernate.ddl-auto=update
```

`suilearn_dev_password` 只作为本地容器默认值，不用于远程、共享或生产环境。若需要自定义本地密码，可在启动 compose 前设置 `SUILEARN_POSTGRES_PASSWORD`，并同步更新 `config/local.properties`。

### 27.3 Fake / Real AI Provider 切换

默认模式：

```properties
suilearn.ai.provider=fake
```

`fake` 模式由 `FakeAiProvider` 提供稳定的生成题、解释和复习建议，不需要 API key，适合本地开发、契约验证和自动化测试。

真实 Provider 按 OpenAI-compatible 适配层预留配置：

```properties
suilearn.ai.provider=openai-compatible
suilearn.ai.base-url=https://api.openai.com/v1
suilearn.ai.api-key=${SUILEARN_AI_API_KEY}
suilearn.ai.chat-model=gpt-4.1-mini
suilearn.ai.embedding-model=text-embedding-3-small
suilearn.ai.timeout-ms=30000
suilearn.ai.max-retries=2
```

当前仓库已定义 `AiProvider` 接口和 `FakeAiProvider`，真实 `OpenAiCompatibleProvider` 尚未接入。因此本地运行应保持 `fake`；等真实适配层合入后，再通过 `suilearn.ai.provider=openai-compatible` 切换。无论使用哪种 Provider，业务层只能依赖 `AiProvider` 抽象，不能直接读取 API key 或调用厂商 SDK。

### 27.4 推荐启动顺序

1. 不需要 PostgreSQL 时，直接运行 `mvn -f services/api/pom.xml spring-boot:run`，使用默认 H2 + Fake Provider。
2. 需要 PostgreSQL / pgvector 时，进入 `services/api` 启动 compose，启用 `vector` 扩展，复制并检查 `config/local.properties`。
3. 需要真实 AI 时，仅在本机 shell 设置 `SUILEARN_AI_API_KEY`，不要把 key 写入模板、文档或提交历史。
