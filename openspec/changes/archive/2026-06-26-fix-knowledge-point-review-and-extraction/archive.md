# Archive

Status: passed

## Change

`fix-knowledge-point-review-and-extraction`

## Implementation Reference

Working tree changes from base ref `cc8b0c1c5172088229e37948fa2989f868f5a831`.

## Summary

- Added AI/RAG-backed backend knowledge point extraction that uses material chunk evidence and the configured AI provider before heuristic fallback.
- Added a shared backend knowledge point candidate extractor that filters separators, sentence fragments, and duplicate terms for fallback and provider cleanup.
- Wired both backend extraction entry points to the same extraction strategy.
- Added a regression test for noisy extraction input similar to the reported screenshot.
- Added regression coverage for replaceable provider extraction and chunk-level source refs.
- Made web knowledge point chips clickable from overview and material detail.
- Added a compact detail view for the selected knowledge point.

## Verification

- `mvn -f services/api/pom.xml test -q`: passed.
- `npm --prefix apps/web run build`: passed.
- `npm --prefix apps/web test`: passed.
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef cc8b0c1c5172088229e37948fa2989f868f5a831 -ClosingChange fix-knowledge-point-review-and-extraction`: passed.

## Deferred Items

Deferred items: none

none

## Review

No P0/P1/P2 findings remain open.
