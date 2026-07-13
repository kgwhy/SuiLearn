# 策略

## 等级

Major。原因：该变更调整所有 Agent 的 Git 提交安全行为与高频 Skill 的加载结构。

## Owner 与 base_ref

- Owner: Leader Agent。
- `base_ref`: `09972deabe46d7160e9f2f885b2007a27d412d88`。
- 文件锁：串行执行；开始 Build 前检查 `.agents/locks` 和工作区占用。

## 审批状态

用户已批准进入 Build；实现和验收已完成。用户后续补充需求可在同一 change 中更新规格、任务和实现。

## 允许修改文件

- `openspec/changes/harden-git-ops/**`
- `.agents/skills/git-ops/SKILL.md`
- `.agents/skills/git-ops/references/**`
- `.agents/skills/git-ops/scripts/scan-staged-secrets.ps1`
- `.agents/skills/git-ops/agents/openai.yaml`（仅在元数据与新入口不一致时）

## 禁止修改文件

- `apps/**`
- `services/**`
- `contracts/**`
- `docs/proposals/**`
- `docs/superpowers/**`
- `AGENTS.md`
- `.agents/skills/suilearn-workflow/**`
- 其他 active change 目录

## 验证

- 在隔离临时 Git 仓库中验证扫描器：无命中、命中 key/token、未触及旧行中的命中、二进制或不可读 blob。
- 验证扫描器输出不含 fixture 的 secret 值或其子串。
- 验证 `SKILL.md` 的 reference 仅一层直达，且只读操作不要求加载提交或发布 reference。
- 验证默认提交模板不含“验证”或“风险与备注”段落。
- 运行 `python C:\Users\youku\.codex\skills\.system\skill-creator\scripts\quick_validate.py .agents\skills\git-ops`；若本地缺少 `yaml` 依赖，记录原始失败并执行等价结构检查。
- 运行 `git diff --check`、`git diff 09972deabe46d7160e9f2f885b2007a27d412d88 --stat`、`openspec validate harden-git-ops --strict`。

## 测试说明

此变更只涉及 Agent Skill、PowerShell 扫描脚本和 OpenSpec 文档；不适用业务模块基线测试。Build 前以当前 `git status --short --branch`、临时仓库的扫描器失败用例和 Skill 前向测试替代。
