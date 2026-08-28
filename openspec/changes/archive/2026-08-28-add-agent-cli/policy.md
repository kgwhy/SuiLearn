# Policy: Add Agent REST CLI

## 变更信息

- 变更：`add-agent-cli`
- 状态：`Status: Approved`
- 批准者：用户（2026-08-28 对话指令“加一个 web 页面或者命令行界面可以使用 Agent 功能，哪个简单做哪个”，选择实现更简单的 CLI）
- 等级：Standard
- Owner：Leader Agent 协调；CLI 客户端实现按本 policy 授权由单一实现者完成，测试由独立命令验证。
- base_ref：`1574a299916ff72ca61866ee9a21323d767af071`
- Build 循环：L2（实现 -> 测试 -> 单 Agent 延迟自审）

## 角色与文件边界

CLI 是新的 API 消费端工具，不属于现有 Server Backend / Web Frontend 文件范围。本变更经用户显式指令授权，将以下文件视为本次 Standard change 的允许实现范围，不据此扩大任何角色的长期边界。

## 允许的文件

- `openspec/changes/add-agent-cli/**`
- `scripts/agent_cli.py`
- `tests/test_agent_cli.py`
- `.agents/notes/implemented/architecture/2026-08-28-add-agent-rest-cli.md`

## 禁止的文件与行为

- `services/api/**`、`contracts/**`、`apps/android/**`、`apps/web/**`
- `docs/**` 当前事实文档（本变更不改变稳定产品/架构事实）
- `compose.yml`、`.env.example`、根 Gradle/Maven 构建文件
- 新增第三方 Python 依赖或要求后端安装额外组件
- 修改既有 workflow scripts 的语义

## 决策记录

CLI vs Web 页面属于范围取舍，决策记录落在：

- `.agents/notes/implemented/architecture/2026-08-28-add-agent-rest-cli.md`

## 验收矩阵

| 范围 | 默认值/语义 | 必需验证 |
| --- | --- | --- |
| Base URL | `http://127.0.0.1:8080`；环境变量 `SUILEARN_API_BASE_URL` 优先，`--base-url` 最高优先 | 单元测试覆盖归一化与优先级 |
| Scope | `ask`/`chat` 必须提供 `--knowledge-base` 或 `--material` | CLI 参数测试/运行 help 检查 |
| 回合恢复 | `WAITING_INPUT` 时调用 `POST /reply`，`--non-interactive` 时调用 `cancel` | fake client 测试 |
| 认证 | `SUILEARN_AGENT_TOKEN` / `--token` 写入 `Authorization: Bearer` | HTTP server 测试 |
| 错误 | 读取 Agent 错误 envelope 的 `code`/`message` | HTTP 503 错误测试 |
| 依赖 | 仅 Python 3.11+ 标准库 | 直接执行，无需 pip install |

## 验证计划

- `python3 scripts/agent_cli.py --help`
- `python3 -m unittest discover -s tests -p 'test_agent_cli.py' -q`
- `python3 -m unittest discover -s tests -p 'test_workflow_scripts.py' -q`
- `python3 scripts/check_agent_notes.py`
- `python3 scripts/check_workflow_skill.py`
- `python3 scripts/change_scope.py --base 1574a299916ff72ca61866ee9a21323d767af071`
- `git diff 1574a299916ff72ca61866ee9a21323d767af071 --stat`
- 后端 Maven 测试不适用：本变更未修改 `services/api/**`；仓库环境无可用 Maven，按 policy 记录原因。

## 完成定义

- 所有允许文件存在且内容可运行。
- 单元测试通过且覆盖验收矩阵中的关键路径。
- 文件范围只包含允许文件；`README.md` 的未提交修改为对话开始前既有改动，不属于本变更。
