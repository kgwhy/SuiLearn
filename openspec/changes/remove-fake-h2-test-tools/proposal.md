# Proposal

## Summary

Remove fake AI/embedding providers and H2 from the backend runtime/test baseline.

## Problem

The backend still defaults to fake AI and H2-backed local/test execution. That makes AI/RAG and persistence behavior look available even when the real OpenAI-compatible provider or PostgreSQL database is not configured.

## Scope

- Default backend configuration to PostgreSQL and OpenAI-compatible provider settings.
- Remove production fake AI and fake embedding provider implementations.
- Remove the H2 Maven dependency and H2 test datasource configuration.
- Update backend tests to use explicit deterministic test providers, not production fake beans.
- Update current architecture/tech baseline docs for the removed fake/H2 baseline.

## Non-goals

- No API contract shape changes.
- No Android or Web UI changes.
- No real API keys or secrets in checked-in config.

## Acceptance

- Production code has no `FakeAiProvider` or `FakeEmbeddingProvider` beans.
- H2 is no longer a backend dependency or default datasource.
- Missing OpenAI-compatible configuration is reported as unavailable instead of silently falling back to fake.
- Backend tests compile; full integration tests require a reachable PostgreSQL test database.
