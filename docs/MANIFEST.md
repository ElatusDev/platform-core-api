# Documentation Manifest — AkademiaPlus Platform Core API

> **Last updated**: 2026-02-28
> **Purpose**: Single source of truth for all project documentation status.
> Completed work moves to `docs/completed/`. ADRs stay in `docs/adr/` with status annotations.

---

## Active Documents

### Planning & Tracking

| Document | Purpose | Status |
|----------|---------|--------|
| [BETA-MATURITY-PLAN.md](BETA-MATURITY-PLAN.md) | 11-wave rollout plan toward beta release | 🟡 In progress — Waves 1-5 done |
| [AGENT-STRATEGY.md](AGENT-STRATEGY.md) | Claude Code workflow and automation strategy | 🟢 Active reference |

### Specifications (pending implementation)

| Document | Purpose | Depends on | Status |
|----------|---------|------------|--------|
| [delete-usecase-strategy.md](delete-usecase-strategy.md) | Strategy for soft-delete across all entities | @SQLDelete bug fix | 🔴 Blocked |
| [delete-usecase-workflow.md](delete-usecase-workflow.md) | Step-by-step implementation plan for delete use cases | delete-usecase-strategy | 🔴 Blocked |
| [exception-advice-specification.md](exception-advice-specification.md) | ControllerAdvice consolidation spec (21 exception classes) | — | 🟡 Ready to implement |
| [exception-advice-workflow.md](exception-advice-workflow.md) | Step-by-step consolidation workflow | exception-advice-spec | 🟡 Ready to implement |

### Reference Workflows

| Document | Purpose | Status |
|----------|---------|--------|
| [workflows/COMPONENT-TEST-WORKFLOW.md](workflows/COMPONENT-TEST-WORKFLOW.md) | Canonical component test pattern per entity | 🟢 Active reference |

### Claude Code Prompts (`.claude/prompts/`)

| Prompt | Executes | Status |
|--------|----------|--------|
| [auth-bootstrap.md](../.claude/prompts/auth-bootstrap.md) | Auth bootstrap for e2e test suite | 🟡 Ready to execute |
| [delete-usecase-rollout.md](../.claude/prompts/delete-usecase-rollout.md) | Delete use case rollout across entities | 🔴 Blocked on strategy |
| [exception-advice-consolidation.md](../.claude/prompts/exception-advice-consolidation.md) | Exception handling consolidation | 🟡 Ready to execute |

---

## Architecture Decision Records (`docs/adr/`)

| ADR | Decision | Status |
|-----|----------|--------|
| [0000](adr/0000-adr-template.md) | ADR template | 🟢 Active |
| [0001](adr/0001-composite-keys-for-multi-tenancy.md) | Composite keys for multi-tenancy | 🟢 Implemented |
| [0002](adr/0002-openapi-first-api-design.md) | OpenAPI-first API design | 🟢 Implemented |
| [0003](adr/0003-field-encryption-with-hash-columns.md) | Field encryption with hash columns | 🟢 Implemented |
| [0004](adr/0004-given-when-then-testing-standard.md) | Given-When-Then testing standard | 🟢 Implemented |
| [0005](adr/0005-conventional-commits.md) | Conventional Commits | 🟢 Implemented |
| [0006](adr/0006-integration-test-strategy.md) | Integration test strategy (Testcontainers) | 🟢 Implemented |
| ~~0007~~ | ~~Internal PKI with mTLS bootstrap enrollment~~ | ⚪ Superseded → [completed/](completed/0007-internal-pki-mtls-bootstrap-enrollment.md) |

---

## Completed / Historical (`docs/completed/`)

| Document | Original purpose | Completed |
|----------|-----------------|-----------|
| [ca-trust-propagation-workflow.md](completed/ca-trust-propagation-workflow.md) | mTLS certificate enrollment workflow with ports 8443/8081 | Superseded by plain HTTP + trust-broker JWKS model |
| [CONTROLLER-ADVICE-AUDIT.md](completed/CONTROLLER-ADVICE-AUDIT.md) | Point-in-time audit of @ControllerAdvice across modules | 2026-02-21 |
| [creation-usecase-workflow.md](completed/creation-usecase-workflow.md) | Step-by-step creation use case implementation for all entities | All entities implemented |
| [0007-internal-pki-mtls-bootstrap-enrollment.md](completed/0007-internal-pki-mtls-bootstrap-enrollment.md) | ADR for mTLS and bootstrap token enrollment | Superseded by trust-broker JWKS |
| [dependency-upgrade.md](completed/dependency-upgrade.md) | Claude Code prompt for Boot 4.0.0-M3 → 4.0.3, Java 21 → 24 | Upgrade completed |

---

## Root-Level Documents

| Document | Purpose | Notes |
|----------|---------|-------|
| `CLAUDE.md` | Claude Code project context entry point | Keep in sync with .claude/CLAUDE.md |
| `AI-CODE-REF.md` | AI coding standards and review reference (v4.1) | Authoritative — consult before coding |
| `DESIGN.md` | Architecture, module catalog, multi-tenancy model | Update when modules change |
| `CONTRIBUTING.md` | Developer onboarding and contribution guide | — |
| `SECURITY.md` | Security policy and vulnerability reporting | — |
| `CHANGELOG.md` | Release notes (Conventional Commits) | Append per release |
| `README.md` | Project overview and quick start | — |
