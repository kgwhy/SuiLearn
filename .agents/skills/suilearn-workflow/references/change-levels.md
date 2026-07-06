# 变更等级

使用足以保护工作的最小等级。范围扩大时立即向上重分类。

| 等级 | 适用场景 | 最低产物 | 默认循环 |
| --- | --- | --- | --- |
| Tiny | 单角色、无产品/架构/契约/存储变化，通常不超过两个文件 | `tasks.md`、`policy.md` | L1 |
| Normal | 用户可见行为、多文件实现或有意义的测试工作 | `proposal.md`、`design.md`、`tasks.md`、`policy.md` | L2 |
| Major | 跨角色、共享文件、契约、存储、架构、工作流或高风险 | proposal、design、specs、tasks、policy、verification、archive | L3 |

Fast Track 只适用于 Tiny 工作。Tiny 仍需记录 `base_ref`、允许文件、禁止文件、
验证命令和最终 diff/文件范围证据。

工作流规则变更默认为 Major，除非用户明确限定为不改变行为的小型措辞修正。
