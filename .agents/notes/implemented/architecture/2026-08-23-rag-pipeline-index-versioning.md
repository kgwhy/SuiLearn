# Agent Note: RAG 先建 pipeline 与索引签名，多查询合成放 5b
Status: implemented

## Problem

现有检索单一实现，换 embedding 模型无法提示重索引。

## Decision

- 5a 已实现 pipeline 工厂、pgvector-hybrid 包装、EmbeddingSignature、index_versions、text ParseEngine。
- 5b 实现 SmartRetriever 与 PDF/Tika/OCR 引擎。

## Alternatives considered

- **直接重写 KeywordRetriever**：否决，先包后替更稳。
- **引入向量库 SDK**：否决，pgvector 现有路径足够。
- **一次交付全部引擎**：否决，material 解析器迁移面大。

## Consequences

- 68 定向测试全绿；Docker 完整回归 376 tests 0 errors。
- PDF/Tika/OCR 仍走旧 material 路径，行为不变。
