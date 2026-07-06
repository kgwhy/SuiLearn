# 任务

## 1. 建立变更边界

- [x] 1.1 Owner: Leader Agent。创建本 change 的 proposal/design/tasks/policy/verification/archive/spec 产物，允许文件：`openspec/changes/progressive-load-agent-workflow/**`；测试：OpenSpec 校验、人工核对；审查重点：变更范围清晰，不创建退役流程文件。

## 2. 调整 workflow skill

- [x] 2.1 Owner: Leader Agent。将 `.agents/skills/suilearn-workflow/SKILL.md` 改为轻量路由器，允许文件：`.agents/skills/suilearn-workflow/SKILL.md`；测试：skill 结构校验、人工核对；审查重点：frontmatter 清晰，正文不承载完整政策。
- [x] 2.2 Owner: Leader Agent。补充 `.agents/skills/suilearn-workflow/references/**` 的渐进加载、状态、等级、验证细节，允许文件：`.agents/skills/suilearn-workflow/references/**`；测试：人工核对；审查重点：reference 一层直达、无深层追踪。

## 3. 调整 ruler/doc 入口

- [x] 3.1 Owner: Leader Agent。调整 `AGENTS.md` 中工作流入口和 Gate A 表述，允许文件：`AGENTS.md`；测试：人工核对、工作流检查器；审查重点：常驻规则保持短小且保留红线。
- [x] 3.2 Owner: Leader Agent。调整 `docs/development-workflow.md` 开头和加载策略说明，允许文件：`docs/development-workflow.md`；测试：人工核对、工作流检查器；审查重点：文档仍是完整事实说明，但不要求每次全量加载。

## 4. 验证与自审

- [x] 4.1 Owner: Leader Agent。运行 `openspec validate progressive-load-agent-workflow --strict`、工作流检查器、`git diff f220d65b475e3dc9f97d2bf31a0d94e186caa2c4 --stat`，允许文件：本 change 的 verification/archive；审查重点：记录原始输出和范围核对。
- [x] 4.2 Owner: Leader Agent。将 `.agents/skills/suilearn-workflow/**` 的 Skill 正文、reference 和 UI metadata 改为中文，允许文件：`.agents/skills/suilearn-workflow/**`、本 change 的 verification/archive/tasks；测试：OpenSpec 校验、Skill frontmatter/reference 检查、工作流检查器；审查重点：必要英文状态名和路径保留，说明文字中文化。
- [x] 4.3 Owner: Leader Agent。曾新增 Skill 质量标准 reference；后续确认其属于通用 Skill 作者方法论，不属于 `suilearn-workflow` 职责，已在 4.6 从 workflow skill 中移除。保留结论：workflow skill 只承载 SuiLearn 工作流行为。
- [x] 4.4 Owner: Leader Agent。补充真实任务触发示例、前向测试路径和 skill-local 确定性检查脚本，允许文件：`.agents/skills/suilearn-workflow/SKILL.md`、`.agents/skills/suilearn-workflow/references/usage-examples.md`、`.agents/skills/suilearn-workflow/references/forward-testing.md`、`.agents/skills/suilearn-workflow/scripts/check-skill.ps1`、本 change 的 verification/archive/tasks；测试：`scripts/check-skill.ps1`、OpenSpec 校验、工作流检查器；审查重点：示例驱动行为，脚本承接确定性检查，前向测试缺口如实记录。
- [x] 4.5 Owner: Leader Agent。压缩 `AGENTS.md` 为常驻 ruler，只保留红线、加载入口、最小门禁和角色索引，允许文件：`AGENTS.md`、本 change 的 verification/archive/tasks；测试：OpenSpec 校验、工作流检查器、diff stat、行数核对；审查重点：不丢失不可违反项，细节转由 Skill references 和 `docs/development-workflow.md` 承载。
- [x] 4.6 Owner: Leader Agent。从 `suilearn-workflow` 中移除非工作流职责 reference：`skill-quality.md` 和冗余的 `progressive-loading.md`，允许文件：`.agents/skills/suilearn-workflow/**`、本 change 的 verification/archive/tasks；测试：`scripts/check-skill.ps1`、OpenSpec 校验、工作流检查器、残留引用扫描；审查重点：workflow skill 只保留与 SuiLearn 工作流直接相关的 reference。
- [x] 4.7 Owner: Leader Agent。为 `agents/*.md` 补充简短身份定位，使每个 Agent 加载角色文件后先明确自身专业身份、判断姿态和职责边界；允许文件：`agents/*.md`、本 change 的 policy/tasks/verification/archive；测试：OpenSpec 校验、工作流检查器、diff check、范围核对；审查重点：只补身份，不扩写成长篇流程文档，不改变既有文件边界。
