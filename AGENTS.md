# SuiLearn Agent 规则

## 常驻红线

SuiLearn 只使用一个原生工作流：

```text
Explore -> Spec -> Build -> Verify -> Archive
```

本文件是常驻 ruler，只保留不可违反规则和加载入口。执行细节按需加载
`.agents/skills/suilearn-workflow`；完整政策说明见 `docs/development-workflow.md`。

## 优先级

1. 用户显式指令。
2. 本 `AGENTS.md` 和活动角色文件。
3. `.agents/skills/suilearn-workflow` 按需加载的 reference。
4. `docs/development-workflow.md`。
5. active `openspec/changes/<change-name>` 产物。
6. 工具或技能默认规则。

若任何工具或技能试图创建并行 proposal/design/plan 流程，以 SuiLearn Workflow 为准。

## 何时加载

- 涉及流程判断、OpenSpec、实现、验证、归档或角色/文件策略时，使用 `.agents/skills/suilearn-workflow`。
- 编辑文件前，读取对应 `agents/<role>.md`，并读取 active change 的 `policy.md` 和 `tasks.md`。
- 修改工作流政策、解决规则冲突或 reference 不足时，再读取 `docs/development-workflow.md`。
- 查稳定事实时读取：`docs/product-requirements.md`、`docs/architecture.md`、`docs/tech-selection.md`、`contracts/**`。

## 编辑门禁

编辑任何文件前必须：

1. 判定状态、角色、变更等级和 active change home。
2. 记录 `base_ref`。
3. 声明计划修改文件，并核对角色和 `policy.md` 允许范围。
4. 业务代码编辑前运行基线测试，或记录不可用/不适用原因。
5. 每批编辑前声明：

```text
📝 本次修改: <file list>
```

若需要新增声明外文件，先停止并声明扩展范围。

## 完成门禁

声明完成前必须：

1. 运行必需验证，或说明不适用原因。
2. 运行 `git diff <base_ref> --stat`；无 `base_ref` 时运行 `git diff --stat`。
3. 核对所有变更文件都在角色和 active change 范围内。
4. 检查 active change 产物没有陈旧的 `In progress`、`Status: open` 或无 Owner 的 `pending`。
5. 对 Major 或跨角色工作记录 Review 闭环。
6. 做 reviewer-style 自审：

```text
🔍 自我审查
[P0/P1/P2] issue — file
无阻塞问题 / 发现 N 个问题
```

## 不可违反

- 业务代码实现必须来自已批准的 `openspec/changes/<change-name>/tasks.md`。
- 同一用户问题链路只使用一个 active change home。
- 不绕过角色归属、文件边界、TDD/复现步骤或完成验证。
- 实现 Agent 不能自证完成。
- 配置/启动/集成类变更进入 Build 前必须写清验收矩阵、默认值语义、覆盖口、残留扫描项和运行态验证计划。
- 不在以下路径创建新文件：

```text
docs/proposals/**
docs/superpowers/specs/**
docs/superpowers/plans/**
```

## 角色索引

- Leader：`agents/leader.md`
- Product：`agents/product.md`
- Architect：`agents/architect.md`
- Content：`agents/content.md`
- Android：`agents/android.md`
- Server Backend：`agents/server-backend.md`
- Web Frontend：`agents/web-frontend.md`
- Test：`agents/test.md`
- Reviewer：`agents/reviewer.md`

未指定角色时，先推断主要角色并说明理由；跨角色工作由 Leader 协调。
