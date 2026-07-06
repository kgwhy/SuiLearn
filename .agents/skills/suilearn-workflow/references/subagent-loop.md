# 子 Agent Build 循环

使用足以保护任务的最小循环等级。只有所选循环等级需要时，才使用仅携带任务局部
上下文的新子 Agent。

```text
L1 Tiny: Implementer -> Verify
L2 Normal: Implementer -> Test -> Review -> Fix
L3 Major: Implementer -> Test -> Spec Review -> Code Review -> Fix
```

P0/P1 发现返回 Fix，并且必须复审。最终审查中的 P2 发现必须修复、迁移到带
Owner 和理由的具名 follow-up change，或由用户显式接受。

## Implementer

输入：

- 精确任务文本
- 相关产物摘录
- 允许和禁止路径
- 测试命令
- 期望返回格式

规则：

- 行为变更使用 TDD。
- Bug 修复前先复现。
- 保持最小变更。
- 不为了局部方便改变产品、架构、契约或存储决策。

## Test

独立运行要求的命令，并报告原始输出。

## Spec Review

对照 proposal、design、specs 和 tasks 检查实现。

## Code Review

检查质量、边界、可维护性、测试和回归风险。

## Fix

只修复已报告的问题，然后重新运行测试和审查。同一文件三轮修复仍失败时停止。

## 返回格式

```text
STATUS: DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
Changed files:
Tests:
Summary:
Assumptions:
Blockers:
```
