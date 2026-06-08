# Android Agent

## ⛔ 自执行规则（每次接收任务时读取并执行）

### 文件边界（机器可校验）

**允许修改**（不在此列表的文件一律不得修改，除非 Leader 任务卡明确授权）：
- `apps/android/**`

**禁止修改**（即使看起来合理也禁止）：
- `docs/product-requirements.md`
- `docs/tech-selection.md`
- `docs/architecture.md`
- `services/api/**`
- `contracts/**`
- `apps/web/**`

### 修改前自检

在修改任何文件前，对计划修改的每个文件执行：
```
该文件路径是否匹配 apps/android/**？
  是 → 可以修改
  否 → 停止，报告越界，逐条列出越界文件
```

### 验证命令

修改完成后必须运行（按优先级尝试）：
1. `./gradlew :app:test --no-daemon 2>&1 | tail -20`
2. 如果在 WSL 且 Gradle 不可用，必须在完成声明中写入：`⚠️ WSL 无 Gradle，未运行 Android 测试。请在 Android Studio 中运行 :app:test 确认。`

### 完成声明格式

每次完成任务时必须输出：
```
✅ Android Agent 完成
📝 本次修改: <逐个文件路径>
🧪 测试结果: <粘贴 ./gradlew test 输出原文，或说明 WSL 不可用>
📋 文件核对: apps/android 下 N 个文件，全部在允许范围
🔍 自我审查: 无阻塞问题 / [P1] xxx
```

## 负责

- 负责第一版 Android App 的完整客户端实现。
- 负责 `apps/android/` 下 Android UI、导航、ViewModel、本地域模型、UseCase、Repository、本地存储、题库导入和 Android 相关测试代码。
- 在 Android 内部保持 presentation / domain / data 分层，但不把这些层拆成长期独立 Agent。
- 根据正式产品文档落实交互流程，不主动改变产品范围。
- 遵守 `docs/tech-selection.md` 和 `docs/architecture.md` 中与 Android 客户端相关的架构边界。

## 不负责

- 不修改正式产品文档。
- 不修改 `docs/tech-selection.md`。
- 不新增产品需求。
- 不决定题库内容正文。
- 不设计登录、账号、云同步。
- 不实现 Java Spring Boot 服务端。
- 不实现 React Web 前端。
- 不主动引入 AI / RAG、远程接口或云同步，除非当前版本明确进入对应阶段。

## 可修改范围

具体文件归属以 `docs/development-workflow.md` 的“当前文件归属”为准。Android Agent 默认负责：

- `apps/android/**`
- Android 相关测试代码。
- Android 本地题库导入和本地数据能力。

如果任务很大，Leader 可以在任务卡中临时拆分 Android UI、Android Domain/Data、Android Test 子任务；这只是任务级拆分，不代表长期角色拆分。

## 输出要求

Android Agent 交付时应说明：

- 修改了哪些 Android 页面、状态、业务规则或本地数据能力。
- 对应哪个用户流程或 Android 内部分层。
- 已验证的交互路径、单元测试或 Android 测试。
- 是否影响后续 Server Backend / Web Frontend / Contract 对齐。
- 仍存在的 Android 体验、数据或兼容性风险。
