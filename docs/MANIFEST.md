# Documentation Manifest — AkademiaPlus Platform Core API

> **Last updated**: 2026-03-06
> **Purpose**: Single source of truth for all project documentation status.

---

## Directory Structure

```
docs/
├── MANIFEST.md                          ← this file
├── design/                              ← architecture, specs, strategies, ADRs
│   └── adr/                             ← architecture decision records
├── directives/                          ← coding standards & Claude execution rules
├── prompts/                             ← executable Claude Code prompts
│   ├── pending/                         ← ready to execute or blocked
│   └── completed/                       ← executed prompts
└── workflows/                           ← step-by-step implementation plans
    ├── pending/                          ← ready to execute or blocked
    └── completed/                        ← executed workflows
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
| [SECURITY.md](design/SECURITY.md) | Security policy and vulnerability reporting | 🟢 Active reference |
| [beta-maturity-design.md](design/beta-maturity-design.md) | 11-wave rollout plan toward beta release | 🟡 In progress — Waves 1-5 done |
| [delete-usecase-strategy.md](design/delete-usecase-strategy.md) | Strategy for soft-delete across all entities | 🟢 Implemented |
| [exception-advice-strategy.md](design/exception-advice-strategy.md) | ControllerAdvice consolidation spec (21 exception classes) | 🟢 Implemented |

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
| ~~0007~~ | ~~Internal PKI with mTLS bootstrap enrollment~~ | ⚪ Superseded → [adr/0007](design/adr/0007-internal-pki-mtls-bootstrap-enrollment.md) |

---

## Prompts (`docs/prompts/`)

Executable prompts for Claude Code CLI.

### Pending (`docs/prompts/pending/`)

| Prompt | Purpose | Status |
|--------|---------|--------|
| [e2e-bootstrap-prompt.md](prompts/pending/e2e-bootstrap-prompt.md) | Registration-driven E2E bootstrap — register endpoint, tenant-scoped mock data, collection rewrite | 🟡 Ready to execute |
| [email-notification-delivery-prompt.md](prompts/pending/email-notification-delivery-prompt.md) | Email notification delivery via Jakarta Mail/SMTP — 10 phases, strategy + templates + controllers + tests | 🟡 Ready to execute |

### Completed (`docs/prompts/completed/`)

| Prompt | Original purpose | Completed |
|--------|-----------------|-----------|
| [auth-bootstrap-prompt.md](prompts/completed/auth-bootstrap-prompt.md) | Auth bootstrap — internal login endpoint and e2e test suite | Implemented |
| [delete-usecase-rollout-prompt.md](prompts/completed/delete-usecase-rollout-prompt.md) | Delete use case rollout across all entities | All entities implemented |
| [exception-advice-consolidation-prompt.md](prompts/completed/exception-advice-consolidation-prompt.md) | Exception handling consolidation | BaseControllerAdvice + generics implemented |
| [dependency-upgrade-prompt.md](prompts/completed/dependency-upgrade-prompt.md) | Boot 4.0.0-M3 → 4.0.3, Java 21 → 24 | Upgrade completed |
| [oauth-social-login-prompt.md](prompts/completed/oauth-social-login-prompt.md) | OAuth2 social login (Google + Facebook) — 14 phases | Implemented |
| [sonarcloud-remediation-prompt.md](prompts/completed/sonarcloud-remediation-prompt.md) | SonarCloud code quality remediation | Implemented |

---

## Workflows (`docs/workflows/`)

Step-by-step implementation plans. Referenced by prompts.

### Pending (`docs/workflows/pending/`)

| Document | Purpose | Status |
|----------|---------|--------|
| [e2e-bootstrap-workflow.md](workflows/pending/e2e-bootstrap-workflow.md) | Registration-driven auth + tenant-scoped mock data — architecture analysis, 4 phases, file inventory | 🟡 Ready to execute |
| [email-notification-delivery-workflow.md](workflows/pending/email-notification-delivery-workflow.md) | Email notification delivery via Jakarta Mail/SMTP — 10 phases, ~35 new files, template engine | 🟡 Ready to execute |

### Completed (`docs/workflows/completed/`)

| Document | Original purpose | Completed |
|----------|-----------------|-----------|
| [component-test-workflow.md](workflows/completed/component-test-workflow.md) | Canonical component test pattern per entity | 25 component tests across all modules |
| [delete-usecase-workflow.md](workflows/completed/delete-usecase-workflow.md) | Step-by-step delete use case implementation | 20+ delete use cases implemented |
| [exception-advice-workflow.md](workflows/completed/exception-advice-workflow.md) | Exception handling consolidation | BaseControllerAdvice + generics implemented |
| [creation-usecase-workflow.md](workflows/completed/creation-usecase-workflow.md) | Step-by-step creation use case for all entities | All entities implemented |
| [ca-trust-propagation-workflow.md](workflows/completed/ca-trust-propagation-workflow.md) | mTLS certificate enrollment workflow (8443/8081) | Superseded by plain HTTP |
| [controller-advice-audit-workflow.md](workflows/completed/controller-advice-audit-workflow.md) | Diagnostic audit of @ControllerAdvice across modules | Fed into exception-advice-strategy |
| [oauth-social-login-workflow.md](workflows/completed/oauth-social-login-workflow.md) | OAuth2 social login (Google + Facebook) — provider strategy, 3-branch auth | Implemented |
| [sonarcloud-remediation-workflow.md](workflows/completed/sonarcloud-remediation-workflow.md) | SonarCloud code quality remediation | Implemented |

---

## Root-Level Documents

| Document | Purpose |
|----------|---------|
| `CLAUDE.md` | Entry point — redirects to `docs/directives/CLAUDE.md` |
| `README.md` | Project overview and quick start |
