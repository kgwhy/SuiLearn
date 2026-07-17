## 1. Local configuration templates

- [x] 1.1 Align the local API override template with the root Compose local-only
  PostgreSQL, RabbitMQ, and MinIO values; document the complete durable
  processing, OCR, timeout, cleanup, CORS, and circuit-breaker configuration
  surface; replace the legacy retry property with
  `suilearn.adapter.max-retries=0`.
  - Owner: Leader Agent
  - Allowed files: `services/api/config/local.properties.example`,
    `openspec/changes/align-local-runtime-config-templates/**`
  - Forbidden files: `compose.yml`, `services/api/src/**`, `apps/**`,
    `contracts/**`, `docs/**`
  - Test command: `mvn -f services/api/pom.xml -Dtest=AdapterRetryConfigurationResolverTest,SuiLearnProcessingPropertiesTest,HealthLayeringConfigurationTest test -q`
  - Review focus: canonical retry key only; all keys match current application
    properties; template contains no real AI credential.

- [x] 1.2 Align `.env.example` development-only PostgreSQL, RabbitMQ, and
  MinIO values with Compose fallbacks, retaining environment-variable override
  behavior and existing service topology.
  - Owner: Leader Agent
  - Allowed files: `.env.example`,
    `openspec/changes/align-local-runtime-config-templates/**`
  - Forbidden files: `compose.yml`, `services/api/**`, `apps/**`,
    `contracts/**`, `docs/**`
  - Test command: `docker compose config`
  - Review focus: copied `.env` does not alter documented local credentials
    or service endpoints; no production secret is introduced.

## 2. Verification and review

- [x] 2.1 Run the configuration acceptance matrix: affected backend
  configuration tests, `docker compose config`, retry-key and credential
  residual scans, `openspec validate --strict`, workflow checking, and
  `git diff --check`.
  - Owner: Leader Agent
  - Allowed files: `openspec/changes/align-local-runtime-config-templates/**`
  - Forbidden files: all implementation files
  - Test command: commands listed in the change policy verification plan
  - Review focus: acceptance evidence covers defaults, overrides, residual
    legacy key removal, and rendered runtime configuration.
  - Resolution: the workflow checker now reads the synced stable main
    efficient-batch capability spec; normal and negative checker paths passed.

## 3. Workflow checker archive resilience

- [x] 3.1 Replace the checker's archived change-local efficient-batch delta
  path with the synced main capability spec path, including the negative
  self-test fixture.
  - Owner: Leader Agent
  - Allowed files: `scripts/check-suilearn-workflow.ps1`,
    `openspec/changes/align-local-runtime-config-templates/**`
  - Forbidden files: `services/**`, `apps/**`, `contracts/**`,
    `compose.yml`, `.env.example`
  - Test command: reproduce the pre-change workflow-check failure; then run
    `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-suilearn-workflow.ps1 -BaseRef d2926b7533765c88290d8ac729e820cb75224b49`
    and `-SelfTestEfficientBatchPolicy`
  - Review focus: stable main-spec path is used in both normal and negative
    paths; no archive-directory discovery or unrelated policy change.

## 4. Major-change closeout

- [x] 4.1 Record verification and review evidence, verify allowed-file scope,
  and complete the final task checklist.
  - Owner: Leader Agent
  - Allowed files: `openspec/changes/align-local-runtime-config-templates/**`
  - Forbidden files: all implementation files
  - Test command: full acceptance matrix, `git diff --check`, and
    `git diff d2926b7533765c88290d8ac729e820cb75224b49 --stat`
  - Review focus: no stale open state, all tasks have owners and evidence, and
    the configuration/template scope has no unrelated file changes.
