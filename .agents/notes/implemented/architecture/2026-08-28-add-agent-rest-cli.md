# Agent Note: 新增 Agent REST CLI 而非 Web 页面
Status: implemented

## Problem

SuiLearn 已有 Agent REST/WebSocket 回合接口，但没有终端用户可直接使用的 Agent 入口。用户要求新增 Web 页面或 CLI，并要求选择实现更简单的一方。

## Decision

选择实现 Python 3 标准库 CLI `scripts/agent_cli.py`。CLI 只消费既有 REST 契约：后台线程启动同步回合，主线程轮询 `active-turn` 与 `events`，遇到 `WAITING_INPUT` 时调用 `reply` 恢复；`--non-interactive` 时调用 `cancel` 避免阻塞。不引入 WebSocket 客户端或第三方依赖，不修改后端/契约/前端。

## Alternatives considered

- **Web 页面**：复用 React 工作台可直接获得图形界面，但需要改 `apps/web/**`、准备真实运行截图证据并运行前端构建，改动面和验证成本明显更高。
- **WebSocket CLI**：可获得服务端推送和更低延迟，但 Python 标准库没有 WebSocket 客户端，需引入依赖或手写协议；REST 轮询已足够 CLI 场景。
- **仅同步一次调用**：实现最少，但无法在 `WAITING_INPUT` 回合取得 `turnId` 并恢复，会丢失 Agent 的关键交互能力。

## Consequences

- 新增 `scripts/agent_cli.py` 与 `tests/test_agent_cli.py`，仅依赖 Python 3.11+ 标准库。
- 流式体验为轮询近似，延迟由 `--poll-interval` 控制，适合本地 CLI 使用。
- 不修改现有角色长期文件边界；该工具由本变更的 policy 一次性授权。
