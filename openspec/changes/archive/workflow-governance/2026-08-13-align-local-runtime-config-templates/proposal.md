## Why

The local API override template predates the durable asynchronous pipeline. It
uses a deprecated retry key and omits the RabbitMQ, MinIO, processing, OCR, and
circuit-breaker settings required when the API runs on the host against the
root Compose services. Its development credentials also diverge from
`.env.example` and the Compose fallbacks.

## What Changes

- Align `local.properties.example`, `.env.example`, and existing Compose
  fallbacks on one documented, local-only credential set.
- Replace the deprecated `suilearn.ai.max-retries` template entry with the
  canonical `suilearn.adapter.max-retries=0`.
- Add the local RabbitMQ, MinIO, asynchronous processing, OCR, bounded
  processing, retry-delay, circuit-breaker, CORS, and cleanup overrides needed
  for a host-run API to use the root Compose stack.
- Document precedence and the local-only nature of the sample credentials.
- Repair the workflow checker so its durable-policy assertions read the synced
  main specification after the original delta spec has been archived.
- Preserve production behavior: this change does not alter Compose service
  topology, production secrets, API contracts, or application defaults.

## Capabilities

### New Capabilities

- `local-runtime-configuration`: provide internally consistent, complete
  local runtime configuration templates for the API and root Compose stack.

### Modified Capabilities

- None.

## Impact

Affected files are `services/api/config/local.properties.example`,
`.env.example`, and `scripts/check-suilearn-workflow.ps1`. Validation
covers property-key consistency, retry-key semantics, Compose rendering, and
the workflow checker's archived-change regression. No runtime code, dependency,
API, or database-schema change is expected.
