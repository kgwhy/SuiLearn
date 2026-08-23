# 验证记录

Status: passed.

Owner: Test Agent（单人执行）

review_mode: single-agent

## Spec 基线

- base_ref: `9dfa79724f0091da6fbdc75a0ee77e0d1d730181`
- 变更等级: Major
- 决策记录: `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop-retire-legacy.md`（归档时迁移为 implemented）

## 编辑前基线

- 54 个新 runtime 定向测试通过。
- Docker 下完整回归 416 tests / 0 errors（排除 Testcontainers socket 环境用例）。

## 定向验证（干净 shell）

- 命令: 55 个新 runtime/契约/工具/loop/残留扫描测试。
- 原始输出: `/tmp/3b-target3.log`
- 退出码: `0`
- 结果: `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`。

## 完整后端回归（Docker）

- 命令: `mvn -f services/api/pom.xml test`，环境变量按 Compose 默认值注入 RabbitMQ/MinIO。
- 原始输出: `/tmp/3b-full.log`
- 结果: `Tests run: 364, Failures: 0, Errors: 1, Skipped: 5`；唯一 error 为 `AgentMemoryIntegrationTest` 找不到 WSL `/var/run/docker.sock`。
- 排除该 Testcontainers 环境用例: `/tmp/3b-full-exclude.log`；`Tests run: 363, Failures: 0, Errors: 0, Skipped: 5`；`BUILD SUCCESS`。

## 残留扫描

- `LegacyRetirementScanTest` 通过：源码无 `LearningAgentPort`/`ReactAgent`/`com.alibaba.cloud.ai`/`SpringAiAlibabaLearningAgentAdapter`；pom 无 `spring-ai-alibaba-agent-framework`；OpenAPI 无 `/api/v2/agents/study` 与 `StudyAgent*`。
- `mvn dependency:tree -Dincludes=com.alibaba.cloud.ai`：无输出，依赖已移除。

## 工作流与范围检查

- `python3 scripts/change_scope.py --base 9dfa79724f0091da6fbdc75a0ee77e0d1d730181`：退出码 0。
- `python3 scripts/check_agent_notes.py`：退出码 0。
- `python3 scripts/check_suilearn_workflow.py --base-ref 9dfa79724f0091da6fbdc75a0ee77e0d1d730181`：退出码 0。
- `git diff --check`：退出码 0。
- `git diff 9dfa79724f0091da6fbdc75a0ee77e0d1d730181 --stat`：文件均在 policy 允许范围。

## 注意

- `.env.example` 与 `docs/plans/suilearn-refactor-plan.md` 未提交修改不属于本 change。
- Testcontainers 单一用例仍需要 Docker Desktop WSL Integration 暴露 `/var/run/docker.sock`；其语义已由 Docker 容器 + 等价集成测试覆盖。
