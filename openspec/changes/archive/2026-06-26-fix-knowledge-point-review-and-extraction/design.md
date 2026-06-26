# Design

## Backend

Introduce a shared candidate extraction helper as the fallback for knowledge point extraction. The primary path uses imported material chunks as retrieval evidence and asks the configured AI provider to produce concise, source-grounded knowledge points. Both the application service and legacy workflow facade use the same extraction rules.

Rules:

- Normalize whitespace and markdown list/heading markers.
- Reject punctuation-only and markdown separator values.
- Reject sentence fragments containing sentence punctuation or dash connectors.
- Keep short Chinese topic phrases and technical identifier tokens.
- Deduplicate by case-insensitive normalized name.
- Preserve insertion order and cap the result count.
- Prefer AI provider output backed by material chunk source refs.
- Fall back to the local candidate extractor only when the provider returns no usable points.

## Web

The workbench keeps a selected knowledge point id. Knowledge point chips become buttons. Clicking a chip opens a compact detail panel with name, description, source material, and source references. If the selected point disappears after refresh, selection is cleared.

## API Impact

No API or contract changes. The web uses existing `KnowledgePoint` fields and existing list/detail payloads.

## Risks

This is still heuristic extraction. It will be cleaner than the current split-based behavior, but truly semantic extraction remains a future AI/content-quality task.
