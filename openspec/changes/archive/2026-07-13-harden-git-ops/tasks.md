## 1. 提交预检扫描器

- [x] 1.1 Owner: Leader Agent。先在隔离临时 Git 仓库建立无命中、key/token 命中、未触及旧行命中和非文本 blob 的失败用例；允许文件：`.agents/skills/git-ops/scripts/scan-staged-secrets.ps1`、本 change 产物；禁止文件：业务代码、CI、Git hooks；测试：逐例运行扫描器并记录退出码和脱敏输出；审查重点：测试样本仅使用虚构凭据，且不输出匹配原文。
- [x] 1.2 Owner: Leader Agent。实现对待提交路径完整暂存 blob 的无回显扫描器；允许文件：`.agents/skills/git-ops/scripts/scan-staged-secrets.ps1`；禁止文件：`.git/hooks/**`、`apps/**`、`services/**`；测试：任务 1.1 的四个用例；审查重点：`git commit` 前可稳定阻断，路径与规则外不泄露任何匹配值。

## 2. 渐进式 Git 指引

- [x] 2.1 Owner: Leader Agent。将主 `SKILL.md` 压缩为触发、核心顺序和按场景的一层 reference 路由；允许文件：`.agents/skills/git-ops/SKILL.md`、`.agents/skills/git-ops/references/**`；禁止文件：`.agents/skills/suilearn-workflow/**`、`AGENTS.md`；测试：人工按只读、提交、发布三类 prompt 前向核对加载路径；审查重点：提交流程在 `git commit` 前加载并执行安全预检。
- [x] 2.2 Owner: Leader Agent。简化提交信息 reference 与模板，删除“验证”和“风险与备注”段落及强制要求；允许文件：`.agents/skills/git-ops/references/commit-messages.md`、`.agents/skills/git-ops/references/conventional-commits.md`；禁止文件：业务代码、Git 历史；测试：模板文本和残留短语扫描；审查重点：Conventional Commit 标题规则保留，交接报告与 commit body 责任清晰分离。
- [x] 2.3 Owner: Leader Agent。将提交前 key、secret 与 access token 检查的脚本命令写入主入口的固定规则和提交顺序；允许文件：`.agents/skills/git-ops/SKILL.md`、本 change 的 spec/tests；禁止文件：业务代码、CI、Git hooks；测试：文档测试先失败后通过；审查重点：入口在 `git commit` 前明确给出可执行检查命令和成功条件。

## 3. 验证与审查

- [x] 3.1 Owner: Test Agent。独立复跑扫描器矩阵与脱敏断言；允许文件：本 change 的 `verification.md`；禁止文件：`.agents/skills/git-ops/**`；测试：policy.md 的全部扫描器场景；审查重点：任何输出均不含 fixture 的凭据或子串。
- [x] 3.2 Owner: Reviewer Agent。对规格一致性、渐进加载边界、文件范围和安全阻断逻辑进行独立审查；允许文件：本 change 的 `verification.md`；禁止文件：实现文件；测试：`openspec validate harden-git-ops --strict` 与文件范围核对；审查重点：无 P0/P1 泄露路径或绕过路径。
- [x] 3.3 Owner: Leader Agent。运行 Skill 结构检查、OpenSpec 严格校验、diff 检查和统计，记录 reviewer-style 自审；允许文件：本 change 的 `verification.md`、`archive.md`；禁止文件：其他路径；测试：policy.md 所列验证命令；审查重点：所有变更均在 policy 范围内，任务没有陈旧的进行中或无 Owner pending 状态。
