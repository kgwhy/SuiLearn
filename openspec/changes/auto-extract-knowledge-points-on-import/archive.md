# Archive

Status: passed

## Change

`auto-extract-knowledge-points-on-import`

## Implementation Reference

Working tree changes from base ref `cc8b0c1c5172088229e37948fa2989f868f5a831`.

## Summary

- Updated the web material import flow to call the existing knowledge point extraction API after a successful import.
- Reused the existing extraction handler so the workbench refreshes and the knowledge point count/list becomes visible immediately.

## Verification

- `npm --prefix apps/web run build`: passed.
- `npm --prefix apps/web test`: passed.
- `powershell -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef cc8b0c1c5172088229e37948fa2989f868f5a831 -ClosingChange auto-extract-knowledge-points-on-import`: passed.

## Deferred Items

Deferred items: none

none

## Review

No P0/P1/P2 findings remain open.
