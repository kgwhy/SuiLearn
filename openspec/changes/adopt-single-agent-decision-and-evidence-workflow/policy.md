# 单人决策记录与证据工作流策略

Status: Approved
批准者: 用户
批准日期: 2026-08-19

- Change: `adopt-single-agent-decision-and-evidence-workflow`
- 级别: Major
- base_ref: `99b25074d26581939d22f334aac02a9883ab8bd0`
- 当前阶段: Build -> Verify
- 执行模式: serial
- 决策记录: `.agents/notes/implemented/process/2026-08-19-adopt-single-agent-decision-and-evidence-workflow.md`

## 允许修改文件

- `AGENTS.md`
- `agents/reviewer.md`
- `docs/development-workflow.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/product-requirements.md`
- `.agents/notes/**`
- `.agents/skills/suilearn-workflow/**`
- `.agents/skills/suilearn-review/**`
- `scripts/change_scope.py`
- `scripts/check_agent_notes.py`
- `scripts/check_workflow_skill.py`
- `tests/test_workflow_scripts.py`
- `.gitignore`
- `openspec/changes/adopt-single-agent-decision-and-evidence-workflow/**`

## 禁止修改文件

- `apps/**`
- `services/**`
- `contracts/**`
- `docs/proposals/**`
- `docs/superpowers/**`
- 其他 active change 目录
- 其他 `openspec/specs/**` 文件
- `openspec/specs/single-agent-workflow/spec.md`

## 基线测试

- 业务模块基线测试不适用：本变更不修改 `apps/**`、`services/**`、`contracts/**`。
- 工作流基线：`python3 -m unittest discover -s tests -p 'test_workflow_scripts.py'` 在改动前为 9/9 通过。

## 验收矩阵

| 场景 | 期望 |
| --- | --- |
| 无 note 或格式错误 | `check_agent_notes.py` 退出码非 0 |
| implemented 目录出现 Proposal/Acceptance criteria | 校验失败 |
| 变更范围查询 | `change_scope.py` 输出 committed/staged/unstaged/untracked 四类 |
| workflow SKILL 缺新 reference 链接 | `check_workflow_skill.py` 失败 |
| 当前事实文档含“已批准 Build 目标” | 不通过评审，需迁移为当前事实 |
| UI 变更缺真实证据 | Reviewer/自审按 P1 处理 |

## 审查重点

- 单人规则是默认，不是降级。
- Agent Note 校验只做格式，不做语义；语义由自审负责。
- 当前事实迁移不得把未验证目标写成已实现事实。
- 不引入双语或团队流程。
