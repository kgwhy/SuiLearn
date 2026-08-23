# RAG Pipeline/Index 设计（5a）

## Decisions

- PipelineFactory 用 Map 注册 pipeline，缺省 pgvector-hybrid。
- index_versions 由 JPA 创建，字段 kb_id/signature/version_no/storage_ref/ready。
- EmbeddingSignature 哈希使用 SHA-256。
- ParseEngineRegistry 首版只注册 text/markdown；PDF/Tika/OCR 在 5b 接现有 material 解析器。
