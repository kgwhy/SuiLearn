# 策略

## 变更

- 名称：`upgrade-text-only-rag-pipeline`
- 等级：Major
- 当前阶段：Spec
- base_ref：`3b8aababf1e49294a32a41eb8ed1780632364ad5`
- Worktree 模式：serial
- 主角色：Architect Agent
- 协调角色：Leader Agent

## 本轮允许修改路径

- `openspec/changes/upgrade-text-only-rag-pipeline/**`

## 本轮禁止修改路径

- `apps/**`
- `services/api/**`
- `contracts/**`
- `docs/proposals/**`
- `docs/superpowers/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`

## Build 阶段预计允许路径

Build 阶段必须在 Approval Gate 后按任务逐项授权。预计会涉及：

- `services/api/src/main/java/com/suilearn/api/material/**`
- `services/api/src/main/java/com/suilearn/api/retrieval/**`
- `services/api/src/main/java/com/suilearn/api/rag/**`
- `services/api/src/main/java/com/suilearn/api/ai/**`
- `services/api/src/main/java/com/suilearn/api/model/**`
- `services/api/src/main/java/com/suilearn/api/persistence/**`
- `services/api/src/main/resources/db/**`
- `services/api/src/test/java/com/suilearn/api/**`
- `contracts/**`
- 如契约新增字段需要客户端展示，另行授权 `apps/web/**` 和 `apps/android/**`。

## 与现有 active change 的关系

- `enable-text-only-rag`：作为前置基础，不在本变更中修改其文件。
- `implement-grounded-rag-answer`：作为前置基础，不在本变更中修改其文件。
- `improve-rag-retrieval`：本变更将其 Tiny 级排序优化吸收为正式 Major 级 RAG 架构方案；Build 时应避免同时修改同一检索文件。

## 基线测试

- 本轮为纯 Spec 文档变更，不运行后端模块测试。
- Build 前后端任务必须记录基线测试。
- 预计基线命令：`mvn -f services/api/pom.xml test -q`
- 当前已知风险：本地 PostgreSQL 测试库可能不可用；若不可用，需记录原始失败输出，并补充可运行的单元测试或 test-compile。

## 文件锁

本轮锁定：

- `openspec/changes/upgrade-text-only-rag-pipeline/**`

Build 阶段共享文件必须串行执行，尤其是：

- `MaterialChunk.java`
- `RagService.java`
- `AiProvider.java`
- `OpenAiCompatibleAiProvider.java`
- `KeywordRetriever.java`
- 数据库迁移文件

## 完成定义

Spec 阶段完成定义：

- `proposal.md`、`design.md`、`tasks.md`、`policy.md`、`verification.md`、`archive.md` 和 `specs/rag/spec.md` 已创建。
- 所有文档使用中文，代码标识和路径保留原文。
- 任务拆分包含 Owner、允许文件、禁止文件、验证命令和审查重点。
- 用户批准后才进入 Build。
