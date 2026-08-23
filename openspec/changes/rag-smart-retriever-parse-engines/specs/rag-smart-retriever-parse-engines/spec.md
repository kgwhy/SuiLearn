## ADDED Requirements

### Requirement: SmartRetriever 必须并行改写与去重
系统 MUST 在 LlmClient 可用时生成最多 3 个查询变体，并行检索，并按结果 id 去重；改写失败时回退原始查询。

#### Scenario: 多查询
- **WHEN** fake LLM 返回两个变体
- **THEN** 原查询与变体并行执行，重复 id 只保留一个

### Requirement: 复杂 ParseEngine 必须复用 material 安全边界
PDF/Office 引擎 MUST 包装现有 DocumentParser；OCR 引擎 MUST 调用现有 TesseractOcrAdapter，且 MUST NOT 在解析失败时泄漏临时文件。

#### Scenario: OCR
- **WHEN** Tesseract 返回 SUCCEEDED
- **THEN** 返回文本与状态元数据
