# SmartRetriever/ParseEngine 设计（5b）

## Decisions

- SmartRetriever 查询改写失败时回退原始查询；并行执行后按 SearchResult.id 去重。
- PDF/Office 复用 DocumentParser 的签名校验与安全边界。
- OCR 通过临时文件调用既有 TesseractOcrAdapter，finally 删除。

## Risks

- OCR 临时文件在异常路径可能短暂留存；finally 覆盖删除。
