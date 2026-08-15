## Why

`git-ops` 当前没有在提交前检查已暂存内容中的泄露密钥或 access token，并且把不再需要的“验证”和“风险与备注”段落强制写入提交模板。这会增加凭据误提交的风险，也让常规提交信息变得冗长。

## What Changes

- 新增提交前的敏感凭据预检：在 `git add` 后、`git commit` 前扫描全部已暂存文件的内容；发现疑似真实 key、secret 或 access token 时停止提交，并只报告安全的文件与规则定位信息。
- 将 `git-ops` 改为渐进式加载：主 Skill 只保留触发条件、核心顺序与按场景加载的索引；提交前检查、提交信息、分支与发布分别移至一层直达的 reference。
- 简化默认提交正文模板，只保留“变更摘要”；移除“验证”和“风险与备注”两个模板段落及其必填要求。
- 明确不在提交消息、终端输出或交接信息中回显疑似凭据的原文。

## Capabilities

### New Capabilities

- `secure-git-commit-preflight`: 对已暂存提交快照执行安全的凭据暴露预检，并在命中时阻止提交。
- `progressive-git-operations-guidance`: 按 Git 操作场景渐进加载 `git-ops` 指引，并提供简化的提交信息模板。

### Modified Capabilities

- 无；仓库当前没有对应的主 OpenSpec capability。

## Scope and Non-goals

范围限于 `.agents/skills/git-ops/**` 以及本 change 产物。不会修改业务代码、Git hooks、CI 配置、已有提交历史，且不会自动删除、掩码或修复检测到的凭据。

## Acceptance Criteria

- `git-ops` 要求每次提交前扫描已暂存文件的完整内容，包含新增文件和未在本次 diff 中改动的已暂存内容。
- 命中疑似凭据时，Skill 要求停止提交且不回显凭据值；明确例外处理必须由用户确认。
- `SKILL.md` 为轻量入口，所有详细流程位于从入口直接链接的一层 reference。
- 默认提交模板不再包含“验证”或“风险与备注”段落，也不要求这些内容。

## Impact

受影响文件为 `.agents/skills/git-ops/SKILL.md`、其 `references/**`，以及本 change 的设计、规格、任务、策略和验证记录。该变化不增加运行时依赖；凭据检测采用提交时可用的 Git 内容与明确记录的扫描规则。
