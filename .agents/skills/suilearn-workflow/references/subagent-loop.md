# Build 循环

```text
L1 Light:    Implement -> Verify
L2 Standard: Implement -> Test -> Review -> Fix
L2 Auto:     一次批准 -> 逐任务 TDD + 逐任务提交 -> 结束 Review
L3 Major:    Batch[Implement + local tests] -> Test -> Spec Review -> Code Review -> Fix
```

## 规则

- Light 由实现者完成局部验证，Leader 或用户 Verify。
- Standard 必须有独立 Test；Review 在单角色且低风险时可合并。
- L2 Auto 只适用于每任务可独立验证和提交的 Standard 变更；连续失败或用户取消时退回普通 L2。
- Major 的 Spec Review 和 Code Review 必须分离，Spec Review 在前。
- 契约、迁移、安全、并发/事务/幂等、跨模块接口和无法解释的失败必须即时审查。
- 子 Agent 只接收任务卡、规格摘录、允许/禁止路径、diff 和命令。
- 同一文件三轮修复仍失败，停止并返回 Spec。

## 单 Agent 环境

- Test：在干净 shell 独立运行命令，保留原始输出。
- Review：使用新会话或用户确认，并记录 `review_mode: single-agent`。
- Major 降级必须由用户明确批准。

## 统一返回格式

```text
STATUS: DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
Changed files:
Tests:
Summary:
Assumptions:
Blockers:
```
