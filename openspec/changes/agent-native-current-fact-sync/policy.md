# Agent-Native 改造当前事实收口策略

Status: Approved
批准者: 用户
批准日期: 2026-08-24
批准依据: 用户指令“本次忽略安卓完成剩余部分改造”，要求完成改造计划剩余部分与最终 Sync Gate 收口。

- Change: `agent-native-current-fact-sync`
- 级别: Standard
- base_ref: `d450780`
- 执行模式: L2（单人执行）

## 允许修改文件

- `openspec/changes/agent-native-current-fact-sync/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`

## 禁止修改文件

- `apps/**`（Android 明确延后；Web not affected）
- `services/**`、`contracts/**`
- `.agents/notes/**`（本 change 不产生新取舍记录）
- 其他 `docs/**`

## 范围

- 只把 change-1 至 change-6b 已实现并验证的结论写入当前事实文档。
- 不写 Phase 8（鉴权/learner 隔离/技能 Prompt），不写 Android 新协议客户端。
- 不修改业务代码、契约或测试。
