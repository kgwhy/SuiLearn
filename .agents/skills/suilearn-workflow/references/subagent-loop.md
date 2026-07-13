# 子 Agent Build 循环

使用足以保护任务的最小循环等级。只有所选循环等级需要时，才使用仅携带任务局部
上下文的新子 Agent。Build 默认采用风险自适应批次，避免为每个细任务重复建立完整审查循环。

```text
L1 Tiny: Implementer -> Verify
L2 Normal: Implementer -> Test -> Review -> Fix
L3 Major: Batch[Implementer + task-local tests] -> Test -> Spec Review -> Code Review -> Fix
```

## 批次规则

- 按依赖、共享文件和风险域组批；文件重叠时串行。
- 批次内每个行为变更仍执行 TDD/复现步骤和局部测试。
- 批次内任务只可标记“实现完成、待批次审查”；批次末统一独立 Test、Spec Review、Code Review。
- 契约/兼容、迁移、安全、并发/事务/幂等、跨模块公共接口或无法解释的测试失败触发即时审查。
- Fix 轮运行失败测试和受影响模块回归；批次关闭运行批次验收命令；最终 Verify 运行全量验证。

## 上下文与紧凑证据

默认输入只包含任务卡、相关规格摘录、允许/禁止路径、受影响文件或符号、当前 diff 和验证命令，不附完整对话或无关规格。

成功返回：命令、退出码、通过/失败计数、必要摘要。失败返回：首个根因、关键原始输出、失败用例和复现命令。只有间歇性问题、审计或继续诊断需要时才提供完整日志位置。

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

独立运行要求的命令，并按紧凑证据格式报告；失败时保留关键原始输出。

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
