# Agent Note: Agent CLI 根目录直达入口与 /tool 命令
Status: implemented

## Problem

Agent CLI 已可用，但用户每次都要输入 `python3 scripts/agent_cli.py chat ...`，且聊天内没有专门的工具查看命令。用户要求入口直达对话，并在对话中通过 `/tool` 查看工具。

## Decision

新增仓库根目录可执行文件 `agent`，通过 `SUILEARN_AGENT_PROG=agent exec python3 ...` 转发到 `scripts/agent_cli.py`，使用户只需 `./agent`。同时把 CLI 的默认子命令改为 `chat`，因此 `./agent --knowledge-base kb_1` 直接进入对话；未提供 scope 时交互式列出 `/knowledge-bases` 并允许选择编号、知识库 ID 或 `material:<id>`。聊天内新增 `/tool` 与 `/tools` 命令，复用 `GET /api/v2/agent/capabilities` 的工具段展示工具名称和描述。

## Alternatives considered

- **直接修改 PATH 或提供 install 脚本**：最接近“全局命令”，但会写仓库外路径，超出 SuiLearn 当前变更边界，且增加平台差异。
- **只改脚本默认命令**：仍需用户输入 `python3 scripts/...`，没有解决用户提出的入口痛点。
- **/tool 仅显示 ownedTools**：信息不完整；工具注册表返回的 `tools` 数组包含描述，更适合命令查看。

## Consequences

- 根目录新增一个无扩展名可执行脚本 `agent`；Unix 下 `./agent` 直接可用。
- 无 scope 启动会额外调用 `/api/v2/knowledge-bases`；该端点在 Agent 鉴权关闭/开启时均可用，失败时仍允许手动输入 ID。
- `scripts/agent_cli.py` 的 `ask`、`capabilities`、显式 `chat` 子命令保持兼容。
