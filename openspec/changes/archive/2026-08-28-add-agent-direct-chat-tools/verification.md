# Verification: Agent CLI 直达对话与 /tool 命令

Status: Verified（2026-08-28）
Review mode: single-agent（实现后延迟自审）

## 验证命令与结果

| 命令 | 结果 |
| --- | --- |
| `python3 -m py_compile scripts/agent_cli.py tests/test_agent_cli.py` | exit 0 |
| `./agent --help` | exit 0，usage 显示为 `agent` |
| `python3 -m unittest discover -s tests -p 'test_agent_cli.py' -q` | Ran 20 tests, OK |
| `python3 -m unittest discover -s tests -p 'test_workflow_scripts.py' -q` | Ran 12 tests, OK |
| `python3 scripts/check_agent_notes.py` | passed（16 notes） |
| `python3 scripts/check_workflow_skill.py` | passed |
| `python3 scripts/check_suilearn_workflow.py --base-ref 1574a299916ff72ca61866ee9a21323d767af071` | passed |
| `python3 scripts/change_scope.py --base 1574a299916ff72ca61866ee9a21323d767af071` | exit 0，见范围核对 |
| `git diff --check` + 新文件 `--no-index --check` | 无空白错误 |

## 运行态证据

使用本地 mock HTTP API 并通过根目录 `./agent` 实跑：

- `printf '/tool\n/quit\n' | ./agent --knowledge-base kb_1 --base-url http://127.0.0.1:<port>`：exit 0，打印 `Tools:` 与 `ask_user: Pause for user input.`。
- `printf '1\n/tools\n/quit\n' | ./agent --base-url http://127.0.0.1:<port>`：exit 0，先列出知识库并读取编号选择 `kb_1`，再通过 `/tools` 打印工具列表。

## 范围核对

本变更产物：

- `agent`
- `scripts/agent_cli.py`（修改）
- `tests/test_agent_cli.py`（修改）
- `.agents/notes/implemented/architecture/2026-08-28-add-agent-launcher-and-tool-command.md`
- `openspec/changes/add-agent-direct-chat-tools/**`

`README.md` 仍是对话开始前已存在的未提交修改，本变更未编辑。上一变更 `add-agent-cli` 的归档目录和脚本、测试文件仍未提交，属于历史未提交产物；本变更只在其上继续修改 `scripts/agent_cli.py` 与 `tests/test_agent_cli.py`。

## 审查摘要

- P0/P1/P2：无。
- 自审点：默认命令改为 `chat` 后，`ask` 仍强制 scope；无 scope 的 chat 通过 `/knowledge-bases` 选择或手动输入 `kb:<id>` / `material:<id>`；`/tool` 与 `/tools` 只读工具注册表，不执行 Agent 回合；根入口只转发参数，不改变 API 语义。
