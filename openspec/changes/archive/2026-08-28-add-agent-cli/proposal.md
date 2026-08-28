# Proposal: Add Agent REST CLI

## 背景与动机

SuiLearn Backend 已提供可用的 Agent-Native 回合能力：

- `GET /api/v2/agent/capabilities`
- `POST /api/v2/agent/turns`
- `GET /api/v2/agent/turns/{turnId}/events`
- `POST /api/v2/agent/turns/{turnId}/reply`
- `POST /api/v2/agent/turns/{turnId}/cancel`
- `GET /api/v2/agent/sessions/{sessionId}/active-turn`

但当前仓库没有面向终端用户的 Agent 使用入口：Web 工作台只承载知识库管理，Android 端 Agent 协议客户端明确延后。用户要求增加一个 Web 页面或命令行界面来使用 Agent 功能，并指定“哪个简单做哪个”。

比较后选择 **CLI**：

- CLI 只需消费已有 REST 契约，不需要改 React 页面、构建前端或准备浏览器运行证据。
- REST 可通过“后台启动回合 + 轮询 active-turn/events + reply”获得近实时输出并处理 `WAITING_INPUT`，无需引入 WebSocket 客户端依赖。
- 单个 Python 3 标准库脚本即可实现，测试可用标准库 mock HTTP server 完成。

## 变更内容

- 新增 `scripts/agent_cli.py`，提供：
  - `capabilities`：列出 Agent 能力与工具。
  - `ask <message>`：单轮提问，流式打印事件，支持 Agent 等待输入时回复。
  - `chat`：交互式会话，复用 `sessionId`，支持连续多轮。
- 使用现有 REST 契约；不修改后端、OpenAPI、Web 或 Android。
- 新增 `tests/test_agent_cli.py`，覆盖 URL 归一化、API 客户端、快速回合和等待输入回合。

## 非目标

- 不新增或修改 `services/api/**`、`contracts/**`、`apps/**`。
- 不实现 WebSocket 客户端；REST 轮询对 CLI 已足够。
- 不引入第三方 Python 依赖。
- 不修改当前事实文档；本变更不产生新的稳定产品/架构事实。

## 验收标准

- 默认 base URL 为 `http://127.0.0.1:8080`，可用 `SUILEARN_API_BASE_URL` 或 `--base-url` 覆盖。
- `capabilities` 能展示能力/工具，并支持 `--json` 输出。
- `ask` 能在 `--knowledge-base` 或 `--material` 范围内启动回合，打印流式事件和 usage 摘要。
- Agent 发出 `WAITING_INPUT` 时，交互模式可输入回复并通过 `reply` 恢复回合。
- `--non-interactive` 遇到等待输入时取消回合，不会无限阻塞。
- API 错误显示服务端返回的 `code`/`message`，无第三方依赖。
