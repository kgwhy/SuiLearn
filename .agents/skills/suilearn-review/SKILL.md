---
name: suilearn-review
description: 在 SuiLearn 中做实现后自审、关闭 change 前自审，或用户要求 review 时使用。单人项目默认执行 self-review，不依赖独立 Reviewer。
---

# SuiLearn 单人自审

只审，不改代码。默认 `review_mode: single-agent`：完成实现后开新会话或隔一段时间再审；用户确认也可作为 Review 证据。

## Blocking

- 契约变更是否先于消费端实现。
- 文件是否在 `policy.md` 允许范围内。
- 业务代码是否来自已批准任务。
- 测试是否验证外部行为，而不是只验证实现者自己的报告。
- 用户可见 UI 是否附真实运行截图/GIF/录屏和 commit SHA。
- 产品/架构/技术事实是否同步到当前事实文档。
- 长期取舍是否写入 `.agents/notes/`。

## Manual checks

- 事务、并发、幂等、重试、迁移。
- 配置默认值和环境变量覆盖语义。
- Android 生命周期、离线能力。
- Web a11y、键盘操作、44px、loading/error 状态。
- 测试是否走真实入口：Gradle/Maven/npm build，不是手工拼装。
- 测试是否会在目标回归上失败；只是绿色不代表场景正确。
- 文件范围与 `git diff <base_ref> --stat` 一致。
- P0/P1 必须修复并复审；P2 修复或写具名 follow-up。

## 输出

统一格式：

```text
STATUS: DONE / DONE_WITH_CONCERNS / NEEDS_CONTEXT / BLOCKED
Changed files:
Tests:
Summary:
Assumptions:
Blockers:
review_mode: single-agent
```
