## ADDED Requirements

### Requirement: Single Workflow Lifecycle
SuiLearn SHALL use one project-level workflow lifecycle for new changes:
Explore, Spec, Build, Verify, and Archive.

#### Scenario: New change starts
- **WHEN** an agent receives a request that changes product scope, architecture, workflow, contracts, or business code
- **THEN** the agent SHALL route the work through the SuiLearn Workflow instead of creating a parallel proposal or plan system

### Requirement: OpenSpec Change Home
New change artifacts MUST live under `openspec/changes/<change-name>/**`.

#### Scenario: Spec artifacts are created
- **WHEN** a change enters Spec
- **THEN** proposal, design, tasks, specs, policy, verification, and archive notes SHALL be created or updated under the active change directory

### Requirement: Risk-Based Change Class
SuiLearn SHALL classify changes as Tiny, Normal, or Major and use the smallest class that protects the work.

#### Scenario: Low-risk change uses Fast Track
- **WHEN** a change is single-role, low-risk, does not affect product, architecture, contracts, storage, or cross-role behavior, and normally touches no more than two files
- **THEN** the change MAY use Tiny Fast Track with tasks and policy artifacts instead of a full proposal/design package

#### Scenario: Scope grows
- **WHEN** a Tiny change reveals broader product, architecture, contract, storage, or cross-role impact
- **THEN** the change MUST return to Spec and be reclassified as Normal or Major

### Requirement: Subagent Build Loop
Build tasks SHALL use a risk-appropriate implementation loop.

#### Scenario: Task enters Build
- **WHEN** a Tiny task starts implementation
- **THEN** the coordinator SHALL use L1 Implementer and Verify unless new risk appears

#### Scenario: Major task enters Build
- **WHEN** a Major task starts implementation
- **THEN** the coordinator SHALL dispatch focused subagents for implementation, testing, spec review, code review, and fixes as needed

### Requirement: Implementation Discipline
Business-code behavior changes MUST use TDD or explicit reproduction-first bug fixing.

#### Scenario: Behavior change is implemented
- **WHEN** an implementer changes behavior
- **THEN** the implementer SHALL write or identify a failing test or reproduction before the production change

### Requirement: Completion Evidence
Agents MUST provide fresh verification evidence before claiming completion.

#### Scenario: Completion is claimed
- **WHEN** an agent says a task or change is complete
- **THEN** the agent SHALL provide test output, diff scope evidence, or a documented not-applicable reason

### Requirement: Retired Document Flows
The project MUST NOT create new change authority under `docs/proposals/**`, `docs/superpowers/specs/**`, or `docs/superpowers/plans/**`.

#### Scenario: Old flow path is introduced
- **WHEN** a new file is added under a retired path
- **THEN** workflow policy verification SHALL fail until the file is removed or moved to the active change home

### Requirement: Protected Change Detection
Workflow policy verification SHOULD detect implementation or current-fact changes that lack an active OpenSpec change.

#### Scenario: Protected file changes without active change
- **WHEN** a diff changes `apps/**`, `services/**`, `contracts/**`, or current-fact documents
- **THEN** workflow policy verification SHALL fail if no active change contains both `tasks.md` and `policy.md`
