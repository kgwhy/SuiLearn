# Policy

## Change

- Name: `fix-knowledge-point-review-and-extraction`
- Level: Normal
- base_ref: `cc8b0c1c5172088229e37948fa2989f868f5a831`
- Workflow: Explore -> Spec -> Build -> Verify -> Archive
- Worktree mode: serial

## Roles

- Leader Agent coordinates the change record.
- Server Backend Agent owns extraction logic and backend regression tests under `services/api/**`.
- Web Frontend Agent owns knowledge point opening/review UI under `apps/web/**`.
- No Android, contract, product, architecture, or tech-selection changes are authorized.

## Allowed Paths

- `openspec/changes/fix-knowledge-point-review-and-extraction/**`
- `services/api/src/main/java/com/suilearn/api/ai/AiProvider.java`
- `services/api/src/main/java/com/suilearn/api/ai/FakeAiProvider.java`
- `services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`
- `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointCandidateExtractor.java`
- `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointService.java`
- `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
- `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- `apps/web/src/App.tsx`
- `apps/web/src/styles.css`

## Forbidden Paths

- `apps/android/**`
- `contracts/**`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/tech-selection.md`
- `docs/proposals/**`
- `docs/superpowers/**`

## File Locks

- `openspec/changes/fix-knowledge-point-review-and-extraction/**`
- `services/api/src/main/java/com/suilearn/api/ai/AiProvider.java`
- `services/api/src/main/java/com/suilearn/api/ai/FakeAiProvider.java`
- `services/api/src/main/java/com/suilearn/api/ai/OpenAiCompatibleAiProvider.java`
- `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointCandidateExtractor.java`
- `services/api/src/main/java/com/suilearn/api/knowledgepoint/application/KnowledgePointService.java`
- `services/api/src/main/java/com/suilearn/api/service/internal/SuiLearnV2Workflow.java`
- `services/api/src/test/java/com/suilearn/api/service/SuiLearnV2ServiceTest.java`
- `apps/web/src/App.tsx`
- `apps/web/src/styles.css`

## Baseline

Module baseline commands passed before business-code edits:

```powershell
mvn -f services/api/pom.xml test -q
npm --prefix apps/web run build
```

The backend output contained Spring Boot startup logs and Mockito/ByteBuddy dynamic agent warnings, but no failures. The web output contained TypeScript and Vite build success.
