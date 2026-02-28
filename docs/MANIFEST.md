# Documentation Manifest — AkademiaPlus Platform Core API

> **Last updated**: 2026-02-28
> **Purpose**: Single source of truth for all project documentation status.

---

## Directory Structure

```
docs/
├── MANIFEST.md              ← this file
├── design/                  ← architecture, specs, strategies, ADRs
├── directives/              ← rules and constraints for Claude Code execution
├── prompts/                 ← executable Claude Code prompts
├── workflows/               ← step-by-step implementation plans
└── completed/               ← historical / superseded documents
```

---

## Design (`docs/design/`)

Specifications, strategies, and architectural decisions.

| Document | Purpose | Status |
|----------|---------|--------|
| [BETA-MATURITY-PLAN.md](design/BETA-MATURITY-PLAN.md) | 11-wave rollout plan toward beta release | 🟡 In progress — Waves 1-5 done |
| [delete-usecase-strategy.md](design/delete-usecase-strategy.md) | Strategy for soft-delete across all entities | 🔴 Blocked — @SQLDelete bug |
| [exception-advice-specification.md](design/exception-advice-specification.md) | ControllerAdvice consolidation spec (21 exception classes) | 🟡 Ready to implement |

### Architecture Decision Records (`docs/design/adr/`)

| ADR | Decision | Status |
|-----|----------|--------|
| [0000](design/adr/0000-adr-template.md) | ADR template | 🟢 Active |
| [0001](design/adr/0001-composite-keys-for-multi-tenancy.md) | Composite keys for multi-tenancy | 🟢 Implemented |
| [0002](design/adr/0002-openapi-first-api-design.md) | OpenAPI-first API design | 🟢 Implemented |
| [0003](design/adr/0003-field-encryption-with-hash-columns.md) | Field encryption with hash columns | 🟢 Implemented |
| [0004](design/adr/0004-given-when-then-testing-standard.md) | Given-When-Then testing standard | 🟢 Implemented |
| [0005](design/adr/0005-conventional-commits.md) | Conventional Commits | 🟢 Implemented |
| [0006](design/adr/0006-integration-test-strategy.md) | Integration test strategy (Testcontainers) | 🟢 Implemented |
| ~~0007~~ | ~~Internal PKI with mTLS bootstrap enrollment~~ | ⚪ Superseded → [completed/](completed/0007-internal-pki-mtls-bootstrap-enrollment.md) |

---

## Directives (`docs/directives/`)

Rules and constraints that govern how Claude Code executes work on this repository.

| Document | Purpose | Status |
|----------|---------|--------|
| [AGENT-STRATEGY.md](directives/AGENT-STRATEGY.md) | Claude Code workflow, automation strategy, and execution constraints | 🟢 Active reference |

---

## Prompts (`docs/prompts/`)

Executable prompts for Claude Code CLI. Each prompt references its specification in `design/` and its workflow in `workflows/`.

| Prompt | Executes | Spec | Workflow | Status |
|--------|----------|------|----------|--------|
| [auth-bootstrap.md](prompts/auth-bootstrap.md) | Auth bootstrap for e2e test suite | — | — | 🟡 Ready to execute |
| [delete-usecase-rollout.md](prompts/delete-usecase-rollout.md) | Delete use case rollout across entities | [strategy](design/delete-usecase-strategy.md) | [workflow](workflows/delete-usecase-workflow.md) | 🔴 Blocked on strategy |
| [exception-advice-consolidation.md](prompts/exception-advice-consolidation.md) | Exception handling consolidation | [spec](design/exception-advice-specification.md) | [workflow](workflows/exception-advice-workflow.md) | 🟡 Ready to execute |

---

## Workflows (`docs/workflows/`)

Step-by-step implementation plans. Referenced by prompts.

| Document | Purpose | Status |
|----------|---------|--------|
| [COMPONENT-TEST-WORKFLOW.md](workflows/COMPONENT-TEST-WORKFLOW.md) | Canonical component test pattern per entity | 🟢 Active reference |
| [delete-usecase-workflow.md](workflows/delete-usecase-workflow.md) | Step-by-step delete use case implementation | 🔴 Blocked on strategy |
| [exception-advice-workflow.md](workflows/exception-advice-workflow.md) | Step-by-step exception handling consolidation | 🟡 Ready to implement |

---

## Completed / Historical (`docs/completed/`)

Archived documents — work is done or architecture has been superseded.

| Document | Original purpose | Completed |
|----------|-----------------|-----------|
| [ca-trust-propagation-workflow.md](completed/ca-trust-propagation-workflow.md) | mTLS certificate enrollment workflow (ports 8443/8081) | Superseded by plain HTTP + trust-broker JWKS |
| [CONTROLLER-ADVICE-AUDIT.md](completed/CONTROLLER-ADVICE-AUDIT.md) | Point-in-time audit of @ControllerAdvice across modules | 2026-02-21 |
| [creation-usecase-workflow.md](completed/creation-usecase-workflow.md) | Step-by-step creation use case implementation for all entities | All entities implemented |
| [0007-internal-pki-mtls-bootstrap-enrollment.md](completed/0007-internal-pki-mtls-bootstrap-enrollment.md) | ADR for mTLS and bootstrap token enrollment | Superseded by trust-broker JWKS |
| [dependency-upgrade.md](completed/dependency-upgrade.md) | Claude Code prompt for Boot 4.0.0-M3 → 4.0.3, Java 21 → 24 | Upgrade completed |

---

## Root-Level Documents

These live at the repository root, not inside `docs/`.

| Document | Purpose | Notes |
|----------|---------|-------|
| `CLAUDE.md` | Claude Code project context entry point | Keep in sync with `.claude/CLAUDE.md` |
| `AI-CODE-REF.md` | AI coding standards and review reference (v4.1) | Authoritative — consult before coding |
| `DESIGN.md` | Architecture, module catalog, multi-tenancy model | Update when modules change |
| `CONTRIBUTING.md` | Developer onboarding and contribution guide | — |
| `SECURITY.md` | Security policy and vulnerability reporting | — |
| `CHANGELOG.md` | Release notes (Conventional Commits) | Append per release |
| `README.md` | Project overview and quick start | — |
