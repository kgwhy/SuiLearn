# 策略

## 变更

- 名称：`persist-chinese-rag-search-index`
- 等级：Major
- 当前阶段：Verify 已通过（已实现并跑通全部后端测试，待用户决定提交与归档目录迁移）
- base_ref：`eccf04be3e399914cfb53da428380b6771d1009d`
- Worktree 模式：serial
- 主角色：Server Backend Agent
- 协调角色：Leader Agent

## 本轮允许修改路径

- `openspec/changes/persist-chinese-rag-search-index/**`

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

Build 阶段必须在 Approval Gate 后按任务逐项授权。预计涉及：

- `services/api/src/main/java/com/suilearn/api/retrieval/**`
- `services/api/src/main/java/com/suilearn/api/persistence/**`
- `services/api/src/main/java/com/suilearn/api/material/**`
- `services/api/src/main/java/com/suilearn/api/model/MaterialChunk.java`
- `services/api/src/test/java/com/suilearn/api/**`

## 与现有 active change 的关系

- 当前无其他 active change（历史变更均已归档至 `openspec/changes/archive/**`）。
- 本变更是 `upgrade-text-only-rag-pipeline` 归档时记录的具名 follow-up（持久化 `search_text`/`tsvector`/reindex）。

## 基线测试

- 本轮为纯 Spec 文档变更，不运行后端模块测试。
- Build 前后端任务必须记录基线测试。
- 预计基线命令：`mvn -f services/api/pom.xml test -q`
- 已知风险：完整测试需本地 PostgreSQL（`pgvector/pgvector:pg16`）与 `suilearn_test` 测试库；生成列与 GIN 索引依赖 PostgreSQL，H2/非 PG 路径走 fallback。

## 文件锁

本轮锁定：

- `openspec/changes/persist-chinese-rag-search-index/**`

Build 阶段共享文件必须串行执行，尤其是：

- `KeywordRetriever.java`
- `MaterialChunkJpaRepository.java`
- `MaterialChunkEntity.java`
- `MaterialChunk.java`
- `SuiLearnV2Store.java`
- 新增迁移组件 `PostgresChunkSearchIndexMigration.java`

## 关键设计约束

- 镜像 `pgvector/pgvector:pg16` 不含 pg_jieba/zhparser；本批用应用层 n-gram + `to_tsvector('simple')` + GIN，零扩展。
- 若 Approval Gate 决定改用 pg_jieba，需要额外授权自定义 Docker 镜像与部署变更，本变更范围与等级据此调整。

## 完成定义

Spec 阶段完成定义：

- `proposal.md`、`design.md`、`tasks.md`、`policy.md`、`verification.md`、`archive.md` 与 `specs/rag/spec.md` 已创建。
- 所有文档使用中文，代码标识与路径保留原文。
- 任务拆分包含 Owner、允许文件、禁止文件、验证命令与审查重点。
- 用户在 Approval Gate 批准后才进入 Build。
