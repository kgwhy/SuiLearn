# Tasks

## 1. Web import flow extracts knowledge points

- Status: done
- Owner: Web Frontend Agent
- Level: Tiny
- Allowed files:
  - `apps/web/src/App.tsx`
  - `openspec/changes/auto-extract-knowledge-points-on-import/**`
- Forbidden files:
  - `services/**`
  - `apps/android/**`
  - `contracts/**`
  - `docs/product-requirements.md`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- Implementation:
  - After a successful web material import, call the existing extract knowledge points API for the imported material.
  - Refresh the workbench after extraction so the knowledge point list/count is visible immediately.
- Baseline:
  - `passed`: `npm --prefix apps/web run build`
- Verification:
  - `npm --prefix apps/web run build`

