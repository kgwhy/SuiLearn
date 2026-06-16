# SuiLearn Agent 规则

## 核心规则

SuiLearn 使用唯一原生工作流：

```text
Explore -> Spec -> Build -> Verify -> Archive
```

该工作流吸收 OpenSpec 风格的 SDD 生命周期、Superpowers 风格的子 Agent/TDD/调试/验证纪律，以及 SuiLearn 角色/文件策略。所有 Agent 必须遵守本文、`docs/development-workflow.md` 和当前活动角色文件。

## 优先级

1. 用户显式指令。
2. 本 `AGENTS.md` 和活动角色文件。
3. `docs/development-workflow.md`。
4. active `openspec/changes/<change-name>` 产物。
5. 工具或技能默认规则。

如果某个工具或技能试图创建并行设计或计划流程，以本项目工作流为准。

## 强制门禁

### Gate A：修改文件前

任何文件编辑前：

1. 读取活动角色文件 `agents/<role>.md`。
2. 列出计划修改文件，并逐项对照角色策略。
3. 记录 `base_ref`，通常为当前 `HEAD`。
4. 检查 `docs/development-workflow.md` 中的锁和 worktree 要求。
5. 业务代码编辑前运行相关基线测试。

纯文档、纯工作流和只读审查任务可以跳过模块测试，但必须说明测试不适用的原因。

### Gate B：编辑期间

每个编辑批次前声明：

```text
📝 本次修改: <file list>
```

如果需要新增声明外文件，先停止并声明扩展范围再编辑。若同一文件在同一任务中三轮修复仍失败，停止并请求重新拆分工作流或重置上下文。

### Gate C：完成前

声明完成前：

1. 运行必需验证命令，或说明不适用原因。
2. 运行 `git diff <base_ref> --stat`；如果没有 `base_ref`，运行 `git diff --stat`。
3. 对照活动角色策略和任务范围检查每个变更文件。
4. 对 Major 或跨角色工作，确认最终 Review Agent 发现已修复、已明确延期到具名 follow-up change，或已由用户显式接受。
5. 声明变更完成前，检查 active change 产物中是否仍有 `In progress`、`Status: open` 或无 Owner 的 `pending` 等陈旧关闭状态。
6. 检查本次触及的当前事实文档，确认除历史参考段落外，不再把新工作指向 `docs/proposals/**` 等退役流程。
7. 报告所有已运行命令的原始测试输出。

完成格式：

```text
✅ 完成
改了什么: <summary>
测试结果: <raw output or not-applicable reason>
文件核对: <N files, all in scope / out-of-scope files: X>
Review 闭环: <无发现 / 已修复 Pn / 已延期到 change id>
```

### Gate D：自我审查

任务结束时执行快速 reviewer-style 自审：

```text
🔍 自我审查
[P0/P1/P2] issue — file
无阻塞问题 / 发现 N 个问题
```

## 工作流入口

- Explore/spec/design/planning 工作属于 SuiLearn Workflow 的 `Explore` 和 `Spec` 状态。
- 业务代码实现必须来自已批准的 `openspec/changes/<change-name>/tasks.md` 任务。
- Fast Track 例外：低风险单角色变更在 `docs/development-workflow.md` 分类为 `Tiny` 时，可使用轻量任务说明代替完整 proposal/design 包。
- Bug 修复应由 OpenSpec change、现有 active task，或低风险且不改变产品/架构/契约/存储/跨角色行为的 Fast Track 任务说明承载。
- `docs/proposals/**` 已退役，不用于新工作。
- `docs/superpowers/specs/**` 和 `docs/superpowers/plans/**` 不是项目事实源。

## 文档规则

- 当前事实位于：
  - `docs/product-requirements.md`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
  - `contracts/**`
- 未来变更位于 `openspec/changes/<change-name>/**`。
- 已完成变更的稳定结论必须在归档前同步回当前事实文档。
- `docs/chat.md` 仅作为灵感和讨论材料，默认只读，不是实现依据。
- `docs/proposals/**` 仅作为历史迁移材料。

## 角色目录

- Leader Agent：`agents/leader.md`
- Product Agent：`agents/product.md`
- Architect Agent：`agents/architect.md`
- Content Agent：`agents/content.md`
- Android Agent：`agents/android.md`
- Server Backend Agent：`agents/server-backend.md`
- Web Frontend Agent：`agents/web-frontend.md`
- Test Agent：`agents/test.md`
- Reviewer Agent：`agents/reviewer.md`

未指定角色时，推断主要角色、说明理由，并将任务保持在该角色策略内。跨角色工作由 Leader 协调。

## 角色隔离

- 除非用户或 Leader 明确授权扩展范围，否则不得修改活动角色策略外的文件。
- 共享文件和契约需要串行执行或 worktree 隔离。
- 实现 Agent 不得为了局部方便改变产品范围、技术基线或契约。
- 如果文档和实现冲突，停止并询问是更新 spec 还是更新实现。

## 子 Agent 策略

在 `Build` 中，主 Agent 作为协调者。任务非平凡、跨角色、高风险或用户要求时，实现、测试、审查和修复应委派给新的子 Agent。

循环强度按风险选择：

```text
L1: Implementer -> Verify
L2: Implementer -> Test Agent -> Review
L3: Implementer -> Test Agent -> Spec Reviewer -> Code Reviewer -> Fix Agent
```

实现 Agent 不能自证完成。P0/P1 测试或审查问题返回修复循环。Spec 歧义、范围变化、架构冲突和越界编辑返回 `Spec` 或需要用户确认。

Major 变更必须在完成前进行最终审查。P2 发现必须修复、迁移到带 Owner 和理由的具名 follow-up change，或由用户显式接受；不允许静默遗留。

## 退役流程

不要在以下路径下创建新文件：

```text
docs/proposals/**
docs/superpowers/specs/**
docs/superpowers/plans/**
```

使用 `openspec/changes/<change-name>/**` 和 SuiLearn Workflow。
