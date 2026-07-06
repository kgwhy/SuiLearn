# 状态机

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |
             +---- spec issue found -----+
```

## Explore

用于澄清、比较、调查和风险发现。

- 允许：阅读文档、代码、测试、日志和 CodeGraph，上下文提问，比较方案。
- 禁止：写业务代码，或把讨论材料当成已确认需求。
- 退出条件：问题和预期结果已能清楚表述，或本轮工作停止。

## Spec

用于行为、工作流、架构、产品范围、契约或存储变化。创建或更新
`openspec/changes/<change-name>/**`。

- Tiny 最低产物：`tasks.md`、`policy.md`。
- Normal 最低产物：`proposal.md`、`design.md`、`tasks.md`、`policy.md`。
- Major 最低产物：proposal、design、specs、tasks、policy、verification、archive。

只有 Approval Gate 通过后才能退出。

## Build

用于已批准任务。根据变更等级选择的循环等级，协调实现、测试、审查和修复。

当范围、契约、架构、数据或验收标准存在歧义时，返回 Spec。

## Verify

用于任何完成声明之前。收集测试、diff stat、文件范围核对、任务状态和最终审查证据。

只有 Sync Gate 要求已满足，或已明确记录非发布状态后才能退出。

## Archive

用于关闭已完成变更。记录最终状态、实现引用、验证摘要、已同步事实、延期项和审查闭环。
