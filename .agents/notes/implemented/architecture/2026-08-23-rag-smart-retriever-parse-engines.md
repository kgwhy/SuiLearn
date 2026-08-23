# Agent Note: SmartRetriever 先做查询改写与并行去重，复杂解析复用 material 边界
Status: implemented

## Problem

5a 只完成 pipeline/索引/text。5b 需多查询检索与 PDF/DOC/OCR 解析。

## Decision

- SmartRetriever 用 LlmClient 生成查询变体，虚拟线程并行，结果按 id 去重。
- PDF/Office 包装现有 DocumentParser；OCR 包装 TesseractOcrAdapter 临时文件路径。

## Alternatives considered

- **在 RAG 层重写 DocumentParser**：否决，material 安全边界已成熟。
- **多查询不做去重**：否决，引用与 evidence 会出现重复。
- **OCR 直接内嵌进程**：否决，违反既有 adapter 边界。

## Consequences

- 71 定向测试全绿；Docker 完整回归 379 tests 0 errors。
