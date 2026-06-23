# Tasks

## 1. Backend RAG-backed extraction cleanup

- Status: done
- Owner: Server Backend Agent
- Allowed files:
  - `services/api/src/main/java/com/suilearn/api/ai/AiProvider.java`
  - `services/api/src/main/java/com/suilearn/api/ai/FakeAiProvider.java`
  - `services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`
  - `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointCandidateExtractor.java`
  - `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointService.java`
  - `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
  - `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- Forbidden files:
  - `apps/**`
  - `contracts/**`
  - `docs/product-requirements.md`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- Baseline:
  - `passed`: `mvn -f services/api/pom.xml test -q`
- Verification:
  - `mvn -f services/api/pom.xml test -q`
- Review focus:
  - No punctuation-only, duplicate, or long sentence-fragment knowledge points.
  - Controller service and legacy facade use the same extraction rules.
  - Extraction uses material chunk evidence/source refs and AI provider output before heuristic fallback.

## 2. Web knowledge point detail

- Status: done
- Owner: Web Frontend Agent
- Allowed files:
  - `apps/web/src/App.tsx`
  - `apps/web/src/styles.css`
- Forbidden files:
  - `services/**`
  - `apps/android/**`
  - `contracts/**`
  - `docs/product-requirements.md`
  - `docs/architecture.md`
  - `docs/tech-selection.md`
  - `docs/proposals/**`
  - `docs/superpowers/**`
- Baseline:
  - `passed`: `npm --prefix apps/web run build`
- Verification:
  - `npm --prefix apps/web run build`
  - `npm --prefix apps/web test`
- Review focus:
  - Knowledge point chips can be opened from overview and material detail.
  - Text remains readable and does not resize or shift the layout unexpectedly.
