# 任务

## 0. 变更分级

- 等级：Major
- 主 Owner：Leader Agent
- 架构 Owner：Architect Agent
- 后端 Owner：Server Backend Agent
- 测试 Owner：Test Agent
- 审查 Owner：Reviewer Agent
- Build 循环：L3

## 1. 架构与契约确认

- 状态：已完成
- Owner：Architect Agent
- 允许文件：
  - `openspec/changes/upgrade-text-only-rag-pipeline/**`
  - `contracts/**`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
- 禁止文件：
  - `apps/**`
  - `services/api/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 已确认 `RagAnswer` 新增兼容字段 `statements`，并保留旧 5 参数构造器。
  - 已确认 PostgreSQL FTS 为 text-only 候选召回路径，Java BM25 为 fallback。
  - 已确认本批不新增 chunk 表字段，旧 chunk 通过现有 `content`、`ordinal` 和 `materialId` 可继续检索，无需强制 reindex。
- 验证：
  - 审查 `design.md`、`specs/rag/spec.md` 和契约变更一致性。

## 2. 实现语义 Chunker

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/material/**`
  - `services/api/src/main/java/com/suilearn/api/model/MaterialChunk.java`
  - `services/api/src/test/java/com/suilearn/api/material/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 已升级 `DefaultMaterialChunker`。
  - 已支持 Markdown 标题、段落、列表、表格和代码块的块级处理。
  - 已实现约 `400-600 token` 目标窗口和 `80 token` overlap。
  - 本批通过 chunk content 保留标题路径，通过 `ordinal` 推导 previous/next；未新增持久化 offset/tokenCount 字段。
- 验证：
  - 覆盖标题不单独成 chunk。
  - 覆盖短段落合并。
  - 覆盖长段落拆分和 overlap。
  - 覆盖代码块不被中途破坏。

## 3. 实现 Text-only 索引和检索

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/retrieval/**`
  - `services/api/src/main/java/com/suilearn/api/persistence/**`
  - `services/api/src/main/resources/db/**`
  - `services/api/src/test/java/com/suilearn/api/retrieval/**`
  - `services/api/src/test/java/com/suilearn/api/persistence/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 已在 `KeywordRetriever` 内实现 query term normalization。
  - 已新增 PostgreSQL FTS 查询路径。
  - 已新增 Java BM25 fallback。
  - RAG evidence retrieval 已优先使用 text-only 候选和 BM25 排序，不依赖 embedding。
- 验证：
  - scope 过滤正确。
  - 删除资料不参与检索。
  - 中文和英文 query 都能命中。
  - FTS 不可用时 fallback 可用。

## 4. 实现 Evidence 扩展与 Context Packing

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/rag/**`
  - `services/api/src/main/java/com/suilearn/api/retrieval/**`
  - `services/api/src/test/java/com/suilearn/api/rag/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 已在 `KeywordRetriever` 中实现 Top 候选邻接扩展。
  - 已将 RAG evidence 上限从 Top3 调整为 Top5。
  - 已让 LLM 输入包含完整 evidence content。
  - 本批未单独引入 `ContextPacker` 类，仍由 evidence 数量上限控制上下文。
- 验证：
  - 命中 chunk 会带相邻 chunk。
  - 超预算时按分数和核心 chunk 裁剪。
  - 同一 chunk 不重复进入上下文。

## 5. 实现 Statement Citation 和 Validation

- 状态：已完成
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/ai/**`
  - `services/api/src/main/java/com/suilearn/api/rag/**`
  - `services/api/src/main/java/com/suilearn/api/model/**`
  - `services/api/src/test/java/com/suilearn/api/ai/**`
  - `services/api/src/test/java/com/suilearn/api/rag/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 已扩展 `AiProvider.answerQuestion` 输入 evidence content。
  - 已扩展输出 statement citations，并通过 `RagAnswer.statements` 返回。
  - 已新增 `CitationValidator`。
  - 已实现引用编号和 statement citation 基础校验。
  - 引用不合法时返回 `uncertain=true`。
- 验证：
  - 模型引用不存在编号时被拦截。
  - `uncertain=false` 但没有 citation 时被拦截。
  - statement 无 citation 时被拦截。
  - 无证据问题返回不确定回答。

## 6. 旧数据迁移与 Reindex

- 状态：不适用
- Owner：Server Backend Agent
- 允许文件：
  - `services/api/src/main/java/com/suilearn/api/material/**`
  - `services/api/src/main/java/com/suilearn/api/service/**`
  - `services/api/src/main/resources/db/**`
  - `services/api/src/test/java/com/suilearn/api/material/**`
- 禁止文件：
  - `apps/**`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- 任务：
  - 本批未新增必需持久化字段，旧资料可通过现有 `content` 直接参与 FTS/BM25 fallback。
  - 如后续引入真实 `search_text`、`heading_path_json` 或 `tsvector` 持久化字段，再单独创建 reindex change。
- 验证：
  - 旧资料可重新索引。
  - reindex 失败可记录任务错误。

## 7. API 客户端适配

- 状态：不适用
- Owner：Web Frontend Agent / Android Agent
- 前置条件：
  - 契约确认是否新增 `statements` 字段。
- 允许文件：
  - `apps/web/**`
  - `apps/android/**`
  - `contracts/**`
- 任务：
  - `RagAnswer.statements` 为新增字段，旧客户端读取原字段不受影响。
  - 客户端增强展示可作为后续独立任务。
- 验证：
  - 现有问答页面不因新增字段崩溃。
  - 引用展示仍可点击到资料片段。

## 8. 测试与审查

- 状态：已完成
- Owner：Test Agent / Reviewer Agent
- 验证命令：
  - `mvn -f services/api/pom.xml test -q`
  - `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef 3b8aababf1e49294a32a41eb8ed1780632364ad5 -ClosingChange upgrade-text-only-rag-pipeline`
- 审查：
  - Spec Review：确认 proposal、design、spec、tasks 一致。
  - Code Review：确认没有重新引入全量扫描主路径、没有把 excerpt 当完整上下文、没有绕过 citation validation。
