## Context

The root Compose stack already defines local RabbitMQ, MinIO, PostgreSQL, and
processing defaults, while the host-run API override template only configures
PostgreSQL and AI. The missing values cause the API to use incompatible blank
RabbitMQ and MinIO credentials. The template also presents a deprecated retry
property.

The change is owned by Leader because it coordinates local startup templates
and workflow verification. It is Major because repairing a workflow checker
changes shared governance enforcement, even though it does not change backend
runtime code or Compose topology.

## Goals / Non-Goals

**Goals:**

- Make a copied `local.properties` sufficient for a host-run API to connect
  to the root Compose services with their documented local-only defaults.
- Make `.env.example` and Compose fallbacks use the same local-only
  PostgreSQL, RabbitMQ, and MinIO values.
- Document the current asynchronous processing, OCR, bounded retry,
  circuit-breaker, and cleanup controls in the local template.
- Use only the canonical adapter retry property with its current default of
  zero.

**Non-Goals:**

- Change production credentials, Compose topology, API contracts, or runtime
  defaults.
- Provide real AI credentials or enable AI features by default.
- Remove support for the deprecated environment variable at runtime.

## Decisions

### Use existing Compose fallbacks as the local credential source

The template and `.env.example` will use the current Compose fallback values:
`suilearn_dev_password` for PostgreSQL,
`suilearn_rabbitmq_dev_password` for RabbitMQ, and
`suilearn_minio_dev_password` for MinIO. This makes startup behavior
deterministic whether a developer starts Compose with or without a copied
`.env` file.

Alternative: use a new shared variable for all development passwords. Rejected
because it would expand configuration scope and reduce parity with the existing
service-specific fallback names.

### Make host connectivity explicit in local.properties

The API template will name `localhost` endpoints for RabbitMQ and MinIO and
will include the current application property values for durable processing.
Spring environment variables retain normal precedence over file properties.

Alternative: leave the values implicit and require environment variables.
Rejected because the file is explicitly a local-override onboarding template.

### Keep legacy retry compatibility out of examples

The template will document `suilearn.adapter.max-retries=0` only. Runtime
compatibility for the deprecated property remains unchanged in application
code, but examples must not trigger its diagnostic mapping.

### Read durable workflow requirements from their stable main spec

The workflow checker will read
`openspec/specs/efficient-batch-workflow/spec.md` rather than the original
change-local delta path. Main specs are durable after change archival; delta
specs are intentionally moved with their change.

Alternative: resolve the latest matching archive directory at runtime. Rejected
because archive organization is capability-domain based and a checker should
not depend on an arbitrary historical archive entry.

## Risks / Trade-offs

- [Local development credentials are visible in templates] → They are
  deliberately non-production values, labelled local-only, and do not include
  AI credentials.
- [Compose fallbacks can change later] → Residual scans and `docker compose
  config` are required acceptance checks; any fallback change must update both
  examples in the same change.
- [More settings can overwhelm new users] → Group settings by service and mark
  ordinary defaults as optional tuning values.

## Migration Plan

1. Developers can replace or merge their copied local template entries.
2. Existing `.env` files continue to override Compose values; the changed
   sample values affect only newly copied examples.
3. Rollback consists of restoring the two example files; application runtime
   behavior and existing local files are unchanged.

## Open Questions

None.
