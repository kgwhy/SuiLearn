# 归档记录

Deferred items: none

## 状态

未归档。当前 change 已通过验证，可进入最终归档检查。

## 延期项

- none
- 无必须阻塞归档的延期项。
- 客户端 statement-level citation 展示为非阻塞增强：后端已兼容返回 `statements` 字段，Web/Android 展示增强可另行处理。
- 持久化 `search_text`、`heading_path_json`、`tsvector` 和 reindex 任务为非阻塞增强：本批使用现有 `content`、`ordinal`、PostgreSQL FTS 查询和 Java BM25 fallback，未强制迁移旧数据。

## 归档前必须补充

- 最终实现引用：commit、PR 或 working tree 引用。
- 验证摘要。
- 同步到当前事实文档的条目。
- 延期项及 Owner。
- 最终审查摘要，包括 P0/P1/P2 发现及关闭方式。
