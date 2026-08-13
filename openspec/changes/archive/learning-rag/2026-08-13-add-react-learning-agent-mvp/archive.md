# 归档记录

状态：计划中；变更尚不具备归档资格。
负责人：Leader Agent。

## 预期归档领域

主技术领域：`backend`；若归档 taxonomy 支持，则将 `ai-agent` 和 `knowledge-retrieval` 记录为关联能力。

## 关闭条件

- 用户 Artifact Approval Gate 和所有 Build 任务已完成。
- 所有 capability spec 均已满足，并同步为稳定事实或明确标记为不受影响。
- 完整验证矩阵通过，并具有真实运行态证据。
- 独立 Spec Review 与 Code Review 不存在未关闭 P0/P1/P2。
- 每个修改文件均在 `policy.md` 范围内，且所有任务 owner/evidence 已完成。
- 不存在陈旧 `In progress`、`Status: open` 或无 owner 的 pending 项。

## 延后事项

- Web/Android UI、认证和多用户安全。
- Run Ledger、checkpoint/resume/fork、流式和长任务调度。
- 通用 Policy/HITL 平台、session lane scheduler 和完整 OTel tracing。
- MCP、plugins、Shell/browser tools、sandbox、第三/深层 SubAgent 和 Agent Teams。
- 动态模型路由及线上 Skill/Prompt 自我演进。

## 审查

最终审查证据只会在 Verify 通过后写入。本计划性归档文件不是实现或完成证据。
