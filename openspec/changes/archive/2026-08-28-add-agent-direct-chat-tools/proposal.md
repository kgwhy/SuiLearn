# Proposal: Agent CLI 直达对话与 /tool 命令

## 背景与动机

已归档的 `add-agent-cli` 提供了可用的 Agent CLI，但入口仍是 `python3 scripts/agent_cli.py chat ...`，每次使用都要输入 Python 命令。用户要求：

1. 可以直接对话，不再每次手动调用 Python。
2. 在对话中通过 `/tool` 命令查看工具。

## 变更内容

- 新增仓库根目录可执行入口 `agent`，直接转发到 `scripts/agent_cli.py`。
- `scripts/agent_cli.py` 不再强制子命令：无子命令时默认进入 `chat`，因此 `./agent --knowledge-base kb_1` 或 `./agent` 都能直接对话。
- `./agent` 未提供 scope 时，交互式列出知识库并让用户选择编号、知识库 ID 或 `material:<id>`。
- 对话内新增 `/tool` 命令（`/tools` 为别名），列出工具名称与描述；保留 `/capabilities`。
- `/help` 同步显示新命令。

## 非目标

- 不修改后端、OpenAPI、Web 或 Android。
- 不提供系统级 PATH 安装脚本；仓库根目录 `./agent` 是统一入口。
- 不新增第三方依赖。

## 验收标准

- `./agent --help`、`./agent capabilities`、`./agent ask ... --knowledge-base kb_1` 均可用。
- `./agent --knowledge-base kb_1` 直接进入 chat，`./agent` 能引导选择 scope 后进入 chat。
- chat 内输入 `/tool` 只列出工具，输入 `/tools` 等价。
- 原有 `python3 scripts/agent_cli.py ...` 用法保持兼容。
