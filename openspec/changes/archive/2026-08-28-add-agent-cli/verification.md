# Verification: Add Agent REST CLI

Status: Verified（2026-08-28）
Review mode: single-agent（实现后延迟自审）

## 验证命令与结果

| 命令 | 结果 |
| --- | --- |
| `python3 -m py_compile scripts/agent_cli.py tests/test_agent_cli.py` | exit 0 |
| `python3 -m unittest discover -s tests -p 'test_agent_cli.py' -q` | Ran 15 tests, OK |
| `python3 -m unittest discover -s tests -p 'test_workflow_scripts.py' -q` | Ran 12 tests, OK |
| `python3 scripts/check_agent_notes.py` | passed（15 notes） |
| `python3 scripts/check_workflow_skill.py` | passed |
| `python3 scripts/check_suilearn_workflow.py --base-ref 1574a299916ff72ca61866ee9a21323d767af071` | passed |
| `python3 scripts/change_scope.py --base 1574a299916ff72ca61866ee9a21323d767af071` | exit 0，见下方范围 |
| `git diff --check` + 新文件 `--no-index --check` | 无空白错误 |

## 运行态证据

使用本地标准库 mock Agent API 启动真实 HTTP server，并通过 CLI 子进程调用：

- `capabilities`：exit 0，打印 `study_agent` 能力与 `ask_user` 工具。
- `ask`（`WAITING_INPUT` 回合，stdin 输入 `A`）：exit 0，先打印 `[wait_for_input] Which answer do you want, A or B?`，回复后打印 `Mock answer: A.` 与 `status: COMPLETED`/usage 摘要。

## 范围核对

`change_scope.py` 输出：

```text
unstaged:
  README.md
untracked:
  .agents/notes/implemented/architecture/2026-08-28-add-agent-rest-cli.md
  openspec/changes/add-agent-cli/...
  scripts/agent_cli.py
  tests/test_agent_cli.py
```

- `README.md` 是对话开始前已存在的未提交修改，不是本变更产物；本变更未编辑 README。
- 本变更产物全部位于 policy 允许文件列表内。
- 未修改 `services/api/**`、`contracts/**`、`apps/**` 或当前事实文档，因此后端 Maven/OpenAPI/前端测试不适用。

## 审查摘要

- P0/P1/P2：无。
- 自审点：REST 轮询在 `--poll-interval` 下近似流式；`WAITING_INPUT` 去重避免重复提问；`--non-interactive` 会取消回合并返回 exit code 2；HTTP 错误保留服务端 `code`/`message`。
