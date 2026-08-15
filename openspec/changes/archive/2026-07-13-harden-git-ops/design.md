## Context

`git-ops` 是会在提交、推送与交接时触发的高频 Skill。当前正文承载了所有流程细节，且提交前没有凭据暴露检查。此次是跨所有使用者的工作流与安全行为变更，Owner 为 Leader Agent；实现文件归属 `.agents/skills/git-ops/**`，规格与验收归属本 change。

## Goals / Non-Goals

**Goals:**

- 在 `git add` 后、`git commit` 前检测每个待提交文件的完整暂存快照中的疑似 key、secret 与 access token。
- 检测过程和失败输出均不得回显匹配值。
- 把高频入口压缩为按场景选择 reference 的路由，并将可执行扫描逻辑封装为 Skill 内脚本。
- 将默认提交正文缩减为可选的“变更摘要”。

**Non-Goals:**

- 不替代组织级 secret scanner、Git hook 或 CI 检查。
- 不自动认可、删除、掩码、轮换或上传检测到的凭据。
- 不扫描工作区未暂存内容、历史提交或所有仓库文件。

## Decisions

### 1. 扫描已暂存文件的完整 index blob

通过 `git diff --cached --name-only -z --diff-filter=ACMR` 取得待提交路径，再由脚本读取每个路径的 `:<path>` 暂存 blob。这样会覆盖未触及的旧行，但不会检查与本次提交无关的文件。

备选方案是只扫描 diff；它会漏掉修改同一文件时遗留在其他行的凭据。扫描整个 index 则会扩大范围且阻塞与本次提交无关的历史问题。因此选择“待提交路径的完整暂存 blob”。

### 2. 使用无回显的 bundled PowerShell scanner

在 `git-ops/scripts/` 提供确定性的扫描器。它在进程内匹配高置信度 token/key 模式，输出仅包含规则标识与路径，并以非零状态终止。详细规则、人工确认例外与命令用法放入 `references/commit-preflight.md`。

备选方案为 `git grep --cached`，但匹配行会直接暴露凭据；或把复杂正则内联进 Skill，后者难以审计和跨任务复用。两者均不采用。

### 3. 分为四个一层直达 reference

主 `SKILL.md` 仅定义触发条件和固定顺序：状态检查、暂存审阅、安全预检、提交、发布。它链接到 `commit-preflight.md`、`commit-messages.md`、`branches-and-publishing.md` 与 `conventional-commits.md`；不使用二层文档跳转。

### 4. 简化提交模板而非移除提交说明

默认模板仅含可选的“变更摘要”正文；提交标题仍遵循 Conventional Commits。验证结果和风险说明应在交接报告中按需要陈述，但不属于 commit body 模板或提交前置条件。

## Risks / Trade-offs

- [正则无法识别所有凭据] → 将其定位为防线之一，保留用户确认例外，并不取代专用扫描器或凭据轮换。
- [测试 fixture 与示例触发误报] → 不提供静默忽略；要求用户明确确认例外后才继续，且不得把真实值写入忽略规则。
- [二进制或无法读取的暂存 blob] → 扫描器报告安全的路径级失败并阻止提交，用户需处理或明确调整范围。
- [PowerShell 仅适用于当前 Windows 开发环境] → 首版以项目运行环境为目标；将跨平台实现列为后续独立变更，不在本次隐式扩展。

## Migration Plan

1. 新增扫描器与渐进式 references，并重写入口。
2. 对安全命中、无命中、非文本 blob 和简化模板进行验证。
3. 发布后，所有由 `git-ops` 指导的提交均采用新预检；不改写历史提交。
4. 回滚只需还原 Skill 目录与删除新增扫描器，不涉及仓库数据迁移。

## Open Questions

无。高置信度模式、阻止行为、用户确认例外和输出脱敏策略均由本 change 的 specs 固化。
