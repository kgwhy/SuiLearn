# 变更等级

| 等级 | 适用 | 最低产物 | 循环 |
| --- | --- | --- | --- |
| Light | 单角色、无行为/架构/契约/存储影响，通常不超过 3 个文件 | `tasks.md` | L1 |
| Standard | 用户可见行为、多文件实现、普通 bug、常规配置 | `tasks.md`、`policy.md`；新功能或跨模块时补 `proposal.md` | L2 或 L2 Auto |
| Major | 跨角色、契约、存储、架构、工作流、安全或高风险 | proposal、design、specs、tasks、policy、verification、archive | L3 |

## 批准记录

- Light：在 `tasks.md` 顶部记录 `Status: Approved` 或 `状态：已批准`。
- Standard/Major：在 `policy.md` 记录批准状态；L2 Auto 必须额外在 `tasks.md` 写 `Mode: Auto`。

批准状态是 Build 的机器门禁。范围扩大时立即升级等级并重新批准。
