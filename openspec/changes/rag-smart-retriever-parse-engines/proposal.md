# SmartRetriever 与 PDF/DOC/OCR ParseEngine（change-5b）

## Why

5a 已建立 pipeline/索引/签名/text engine。5b 补齐多查询检索与复杂文档解析。

## What Changes

- `SmartRetriever`：LlmClient 生成 ≤3 个查询变体，虚拟线程并行检索，按结果 id 去重合并。
- `PdfParseEngine`、`OfficeParseEngine` 包装现有 `DocumentParser`。
- `OcrParseEngine` 包装现有 `TesseractOcrAdapter`（临时文件执行）。
- `ParseEngineRegistry` 注册全部引擎。

## Acceptance Criteria

- SmartRetriever 在 fake LLM 下并行查询并去重。
- PDF/Office 引擎复用 DocumentParser；OCR 引擎输出 Tesseract 文本或稳定失败。
- 既有 68 定向测试通过。
