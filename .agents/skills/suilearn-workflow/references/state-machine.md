# 状态机

```text
Explore -> Spec --[Approval Gate]--> Build -> Verify --[Sync Gate]--> Archive
             ^                           |          |
             +---- spec issue -----------+          |
             +---- verify fail: 小问题返 Build，范围/规格问题返 Spec
             +---- archive 前 sync 不通过返 Spec 或 Build
```

## Explore

允许阅读、比较、提问和风险分析；禁止写业务代码。

## Spec

- Light：创建 `tasks.md`。
- Standard：创建 `tasks.md`、`policy.md`；需要时补 `proposal.md`/`design.md`。
- Major：创建完整产物。

只有 Approval Gate 通过后进入 Build。

## Build

- L1 Light：Implement -> Verify。
- L2 Standard：Implement -> Test -> Review -> Fix。
- L2 Auto：一次批准后逐任务 TDD、逐任务提交；失败或高风险步骤暂停。
- L3 Major：批次实现 -> Test -> Spec Review -> Code Review -> Fix。

实现、规格或验收有歧义时返回 Spec。

## Verify / Archive

Verify 失败时小问题返 Build，范围/规格问题返 Spec。Sync Gate 通过后，使用 `scripts/archive_openspec_change.py` 扁平归档。
