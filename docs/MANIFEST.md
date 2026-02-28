# Documentation Manifest — AkademiaPlus Platform Core API

> **Last updated**: 2026-02-28
> **Purpose**: Single source of truth for all project documentation status.

---

## Directory Structure

```
docs/
├── MANIFEST.md                          ← this file
├── design/                              ← architecture, specs, strategies, ADRs
│   ├── completed/                       ← implemented / superseded designs
│   └── adr/                             ← architecture decision records
├── directives/                          ← coding standards & Claude execution rules
├── prompts/                             ← executable Claude Code prompts
│   ├── pending/                         ← ready to execute or blocked
│   └── completed/                       ← executed prompts (historical)
└── workflows/                           ← step-by-step implementation plans
    ├── pending/                          ← ready to execute or blocked
    └── completed/                       ← executed workflows (historical)
```

### Naming Conventions

| Directory | Postfix | Example |
|-----------|---------|---------|
| `design/` | `-strategy` or `-design` | `delete-usecase-strategy.md` |
| `workflows/` | `-workflow` | `exception-advice-workflow.md` |
| `prompts/` | `-prompt` | `auth-bootstrap-prompt.md` |
| `directives/` | domain name as-is | `AI-CODE-REF.md`, `CLAUDE.md` |

---

## Directives (`docs/directives/`)

Coding standards and execution rules. Read before writing any code.

| Document | Purpose |
|----------|---------|
| [CLAUDE.md](directives/CLAUDE.md) | Project context, hard rules, architecture, build commands |
| [AI-CODE-REF.md](directives/AI-CODE-REF.md) | Coding standards, review rules, detection patterns (v4.1) |
| [AGENT-STRATEGY.md](directives/AGENT-STRATEGY.md) | Claude Code workflow, automation strategy, execution constraints |

---

## Design (`docs/design/`)

Specifications, strategies, and architectural decisions.

| Document | Purpose | Status |
|----------|---------|--------|
| [DESIGN.md](design/DESIGN.md) | Architecture, module catalog, multi-tenancy model, security layers | 🟢 Active reference |
| [beta-maturity-design.md](design/beta-maturity-design.md) | 11-wave rollout plan toward beta release | 🟡 In progress — Waves 1-5 done |
| [delete-usecase-strategy.md](design/delete-usecase-strategy.md) | Strategy for soft-delete across all entities | 🔴 Blocked — @SQLDelete bug |
| [exception-advice-strategy.md](design/exception-advice-strategy.md) | ControllerAdvice consolidation spec (21 exception classes) | 🟡 Ready to implement |

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
| ~~0007~~ | ~~Internal PKI with mTLS bootstrap enrollment~~ | ⚪ Superseded → [completed/](design/completed/0007-internal-pki-mtls-bootstrap-enrollment.md) |

### Completed Designs (`docs/design/completed/`)

| Document | Original purpose | Completed |
|----------|-----------------|-----------|
| [controller-advice-audit-design.md](design/completed/controller-advice-audit-design.md) | Point-in-time audit of @ControllerAdvice across modules | 2026-02-21 |
| [0007-internal-pki-mtls-bootstrap-enrollment.md](design/completed/0007-internal-pki-mtls-bootstrap-enrollment.md) | ADR for mTLS and bootstrap token enrollment | Superseded by trust-broker JWKS |

---

## Prompts (`docs/prompts/`)

Executable prompts for Claude Code CLI.

### Pending (`docs/prompts/pending/`)

| Prompt | Executes | Spec | Workflow | Status |
|--------|----------|------|----------|--------|
| [auth-bootstrap-prompt.md](prompts/pending/auth-bootstrap-prompt.md) | Auth bootstrap for e2e test suite | — | — | 🟡 Ready |
| [delete-usecase-rollout-prompt.md](prompts/pending/delete-usecase-rollout-prompt.md) | Delete use case rollout | [strategy](design/delete-usecase-strategy.md) | [workflow](workflows/pending/delete-usecase-workflow.md) | 🔴 Blocked |
| [exception-advice-consolidation-prompt.md](prompts/pending/exception-advice-consolidation-prompt.md) | Exception handling consolidation | [strategy](design/exception-advice-strategy.md) | [workflow](workflows/pending/exception-advice-workflow.md) | 🟡 Ready |

### Completed (`docs/prompts/completed/`)

| Prompt | Original purpose | Completed |
|--------|-----------------|-----------|
| [dependency-upgrade-prompt.md](prompts/completed/dependency-upgrade-prompt.md) | Boot 4.0.0-M3 → 4.0.3, Java 21 → 24 | Upgrade completed |

---

## Workflows (`docs/workflows/`)

Step-by-step implementation plans. Referenced by prompts.

### Pending (`docs/workflows/pending/`)

| Document | Purpose | Status |
|----------|---------|--------|
| [component-test-workflow.md](workflows/pending/component-test-workflow.md) | Canonical component test pattern per entity | 🟢 Active reference |
| [delete-usecase-workflow.md](workflows/pending/delete-usecase-workflow.md) | Step-by-step delete use case implementation | 🔴 Blocked on strategy |
| [exception-advice-workflow.md](workflows/pending/exception-advice-workflow.md) | Step-by-step exception handling consolidation | 🟡 Ready to implement |

### Completed (`docs/workflows/completed/`)

| Document | Original purpose | Completed |
|----------|-----------------|-----------|
| [creation-usecase-workflow.md](workflows/completed/creation-usecase-workflow.md) | Step-by-step creation use case for all entities | All entities implemented |
| [ca-trust-propagation-workflow.md](workflows/completed/ca-trust-propagation-workflow.md) | mTLS certificate enrollment workflow (8443/8081) | Superseded by plain HTTP |

---

## Root-Level Documents

| Document | Purpose |
|----------|---------|
| `CLAUDE.md` | Entry point — redirects to `docs/directives/CLAUDE.md` |
| `SECURITY.md` | Security policy and vulnerability reporting |
| `README.md` | Project overview and quick start |
