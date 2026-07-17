# 验证记录

状态：计划中；尚未批准 Build。
负责人：Test Agent，由 Leader Agent 协调。

## Spec 基线

- 基线引用：`6f1434ef849bd8e467cc8e0e1c68c37fa9e998c4`。
- `openspec validate add-react-learning-agent-mvp --strict`：Spec 创建期间已通过。
- 业务/模块基线测试：Spec 阶段只编辑新的 change artifacts，因此不适用。
- proposal 创建时的工作区：除新 active change 外干净；Git 仅输出用户级 ignore 权限警告，未产生仓库变更。

## 完成前所需证据

- 依赖收敛和最小 Agent Context 启动证据。
- OpenAPI 校验及实现一致性。
- Context/Prompt、memory、orchestration、Controller/metrics 和 Agent Eval 的测试数量/结果。
- 完整后端测试结果及任何合理的排除项。
- Compose 渲染以及 disabled/enabled 运行态探针。
- Redis 不可用、AI 不可用、非法配置、无证据、跨 scope、记忆隔离和删除失败证据。
- `policy.md` 定义的残留扫描。
- 稳定事实同步 diff，以及 Android/Web/既有正式题目流程未受影响的证据。
- 独立 Spec Review 与 Code Review，且所有 P0/P1/P2 已关闭。
- `git diff --check`、工作流检查、严格 OpenSpec 校验和基线 diff stat/文件范围。

## 计划中的运行态探针

| 探针 | 预期结果 |
|---|---|
| Agent disabled、Redis 缺失 | 既有 Backend 启动；Agent 端点不可用/功能禁用；其他 readiness 不变。 |
| Agent enabled、依赖健康 | Agent readiness 健康；scoped 请求返回 schema 有效结果。 |
| Redis 停止 | Agent readiness 降级；运行返回明确的 session-memory unavailable 错误。 |
| ChatModel 缺失 | Agent readiness 降级；运行返回明确的 model unavailable 错误。 |
| 非法预算/TTL/范围 | 配置绑定/启动拒绝该值。 |
| 空/跨 scope evidence | 返回不确定或校验错误；不得伪造 citation/practice。 |
| 两个 learner/session | 不发生 session 或 semantic-memory 跨读。 |
| semantic-memory 写入失败 | 回答可返回 `PERSIST_FAILED`；不得伪造“已记住”状态。 |
| 记忆删除 | 仅删除目标 learner 的 Agent memory。 |

## 审查状态

- Spec Review：待执行，负责人 Reviewer Agent。
- Code Review：待实现和独立测试完成后执行，负责人 Reviewer Agent。
- 最终 reviewer-style 自审：待 Verify 阶段执行。
