# Policy: Agent CLI 直达对话与 /tool 命令

## 变更信息

- 变更：`add-agent-direct-chat-tools`
- 状态：`Status: Approved`
- 批准者：用户（2026-08-28 对话指令：不要每次调用 Python，要直接对话并用 `/tool` 查看工具）
- 等级：Standard
- Owner：本次 change 单一实现者
- base_ref：`1574a299916ff72ca61866ee9a21323d767af071`
- Build 循环：L2

## 允许的文件

- `openspec/changes/add-agent-direct-chat-tools/**`
- `scripts/agent_cli.py`
- `agent`
- `tests/test_agent_cli.py`
- `.agents/notes/implemented/architecture/2026-08-28-add-agent-launcher-and-tool-command.md`

## 禁止的文件与行为

- `services/api/**`、`contracts/**`、`apps/android/**`、`apps/web/**`
- `docs/**` 当前事实文档
- 新增第三方 Python/Node 依赖
- 修改 PATH、安装系统级命令或写入仓库外文件

## 决策记录

入口方式与 `/tool` 展示策略记录于：

- `.agents/notes/implemented/architecture/2026-08-28-add-agent-launcher-and-tool-command.md`

## 验证计划

- `./agent --help`
- `python3 -m unittest discover -s tests -p 'test_agent_cli.py' -q`
- `python3 -m unittest discover -s tests -p 'test_workflow_scripts.py' -q`
- `python3 scripts/check_agent_notes.py`
- `python3 scripts/check_workflow_skill.py`
- `python3 scripts/check_suilearn_workflow.py --base-ref <base_ref>`
- mock Agent API 实跑 `./agent` 直达 chat 与 `/tool`

## 完成定义

- `./agent` 可直接启动 chat，无 scope 时能选择知识库或材料。
- `/tool` 与 `/tools` 展示工具信息。
- 全部测试与工作流检查通过。
