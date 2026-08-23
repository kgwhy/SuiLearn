# 归档记录

Status: archived

Owner: Leader

## 归档内容

- 实现引用：本 change 的 proposal、design、specs、tasks、policy、verification。
- 验证摘要：55 个新 runtime/契约/工具/loop/残留扫描测试全绿；Docker 完整回归 364 run / 唯一 error 为 Testcontainers 无 docker.sock；排除后 363 tests 0 errors BUILD SUCCESS。
- 当前事实同步：
  - `docs/**`：not affected。
  - `contracts/openapi/suilearn-v2.yaml`：已删除旧 `/api/v2/agents/study/**` 与 `StudyAgent*` schema。
- 决策记录：已迁移为 `.agents/notes/implemented/architecture/2026-08-23-agent-native-loop-retire-legacy.md`。

Deferred items:
- 旧 Prompt/Context/Memory 死代码清理 -> Phase 4/5。
- 真实模型质量 Eval -> 后续 change。
- Testcontainers WSL socket 集成 -> Docker Desktop WSL Integration 开启后补齐。

## 审查摘要

- review_mode: single-agent
- Spec Review 先行：删除边界、类型迁移、新基础设施配置与依赖删除均已对照计划复核。
- Code Review：OpenAPI 删除顺序错误曾误删新路径，已恢复并修正切片边界；Spring 装配、残留扫描、练习 helper 无 catalog/budget 均已检查。
- 最终 P0/P1: 0；P2: 0 未关闭（Testcontainers socket 为具名延期）。
