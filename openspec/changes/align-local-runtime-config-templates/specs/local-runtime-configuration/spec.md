## ADDED Requirements

### Requirement: Local templates use one documented Compose credential set
The project SHALL document the same local-only PostgreSQL, RabbitMQ, and MinIO
credentials in `local.properties.example` and `.env.example` as in the
root Compose fallbacks. A host-run API SHALL use `localhost` endpoints for
the root Compose RabbitMQ and MinIO services. Example files MUST NOT contain
production or AI-provider credentials.

#### Scenario: Developer starts Compose without copying .env
- **WHEN** a developer starts the root Compose stack without a local `.env`
  file and copies `local.properties.example`
- **THEN** the host-run API template uses credentials and endpoints compatible
  with the rendered RabbitMQ, MinIO, and PostgreSQL services

#### Scenario: Developer overrides a local credential
- **WHEN** a developer supplies an environment variable for a documented local
  credential
- **THEN** the environment value overrides the copied template without
  requiring a template edit

### Requirement: Local API template covers durable processing configuration
The local API template SHALL document the current local settings for
asynchronous processing, OCR, file and page limits, concurrency, original-file
retention, knowledge-point generation, parser/OCR/LibreOffice timeouts,
RabbitMQ retry delays, MinIO cleanup, outbox dispatch, CORS, and AI
circuit-breaker controls.

#### Scenario: Developer tunes OCR locally
- **WHEN** a developer changes an OCR concurrency or timeout value in copied
  `local.properties`
- **THEN** the value is expressed using the corresponding current application
  property key and can be applied without adding an undocumented environment
  variable

### Requirement: Local template uses canonical adapter retry configuration
The local API template MUST document
`suilearn.adapter.max-retries=0` as the default adapter retry setting and
MUST NOT document `suilearn.ai.max-retries`.

#### Scenario: Template retry key scan
- **WHEN** the local template is scanned for retry settings
- **THEN** it contains the canonical adapter key with value `0` and no
  deprecated AI retry key

### Requirement: Template changes do not alter runtime topology
Updating example templates MUST NOT modify Compose service definitions,
application source code, runtime defaults, API contracts, or database schemas.

#### Scenario: Render the Compose stack after template update
- **WHEN** `docker compose config` is run after the template update
- **THEN** the root Compose service topology and default service endpoints
  remain unchanged
