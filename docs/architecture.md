# 随心学 SuiLearn 第一版技术方案

## 0. 文档信息

| 项目 | 内容 |
|---|---|
| 文档版本 | v0.1 |
| 维护角色 | 架构 Agent |
| 依据文档 | `docs/product-requirements.md`、`docs/tech-selection.md` |
| 适用阶段 | 第一版 Android 本地学习 App |
| 目标读者 | Android Agent、Server Backend Agent、Web Frontend Agent、内容 Agent、测试 Agent |

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

## 3. 总体分层

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

## 16. Agent 实现分工

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

Web Frontend Agent 第一版不创建 Web 项目。第三版启动后，负责 React + TypeScript Web 前端。

第三版后负责：

- React 页面、路由、组件和浏览器端状态。
- 调用 Server Backend API。
- Web 端刷题、搜索、错题复习、知识点学习等核心流程。
- Web 前端测试和接口契约消费层。

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
