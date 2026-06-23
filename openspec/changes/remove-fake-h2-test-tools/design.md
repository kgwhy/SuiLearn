# Design

## Backend Runtime

`application.properties` uses PostgreSQL defaults and `openai-compatible` as the provider mode. Local development should start PostgreSQL from `services/api/compose.local.yml` or provide equivalent `SUILEARN_DB_*` values.

The fake AI and fake embedding provider classes are removed from main source. `SuiLearnAiProperties` treats the provider as OpenAI-compatible, and provider status no longer exposes a fake available mode.

## Tests

Tests keep deterministic in-test implementations of `AiProvider` and `EmbeddingProvider` so unit behavior remains repeatable without restoring production fake beans. The test datasource no longer uses H2; it points to PostgreSQL via `SUILEARN_TEST_DB_*` defaults.

Because the current machine cannot access Docker or local PostgreSQL, full `mvn test` may be environment-blocked until PostgreSQL is provided.

## Docs

`docs/tech-selection.md` and `docs/architecture.md` are updated so the stable baseline does not continue to describe H2/fake as available defaults.
