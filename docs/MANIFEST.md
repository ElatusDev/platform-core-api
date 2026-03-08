# Documentation Manifest — AkademiaPlus Platform Core API

> **Last updated**: 2026-03-08
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

_No pending prompts for core-api._

### Completed (`docs/prompts/completed/`)

| Prompt | Original purpose | Completed |
|--------|-----------------|-----------|
| [auth-bootstrap-prompt.md](prompts/completed/auth-bootstrap-prompt.md) | Auth bootstrap — internal login endpoint and e2e test suite | Implemented |
| [delete-usecase-rollout-prompt.md](prompts/completed/delete-usecase-rollout-prompt.md) | Delete use case rollout across all entities | All entities implemented |
| [exception-advice-consolidation-prompt.md](prompts/completed/exception-advice-consolidation-prompt.md) | Exception handling consolidation | BaseControllerAdvice + generics implemented |
| [dependency-upgrade-prompt.md](prompts/completed/dependency-upgrade-prompt.md) | Boot 4.0.0-M3 → 4.0.3, Java 21 → 24 | Upgrade completed |
| [oauth-social-login-prompt.md](prompts/completed/oauth-social-login-prompt.md) | OAuth2 social login (Google + Facebook) — 14 phases | Implemented |
| [sonarcloud-remediation-prompt.md](prompts/completed/sonarcloud-remediation-prompt.md) | SonarCloud code quality remediation | Implemented |
| [sonarqube-verification-prompt.md](prompts/completed/sonarqube-verification-prompt.md) | SonarQube verification | Implemented |
| [update-usecase-prompt.md](prompts/completed/update-usecase-prompt.md) | Update use case rollout | Implemented |
| [course-management-update-usecase-prompt.md](prompts/completed/course-management-update-usecase-prompt.md) | Course management update use cases | Implemented |
| [pos-business-logic-prompt.md](prompts/completed/pos-business-logic-prompt.md) | POS business logic implementation | Implemented |
| [email-notification-delivery-prompt.md](prompts/completed/email-notification-delivery-prompt.md) | Email notification delivery via Jakarta Mail/SMTP — 10 phases | Implemented |
| [jwt-refresh-token-rotation-prompt.md](prompts/completed/jwt-refresh-token-rotation-prompt.md) | JWT refresh token rotation with Redis | Implemented |
| [aws-ses-email-infra-prompt.md](prompts/completed/aws-ses-email-infra-prompt.md) | AWS SES email infrastructure | Implemented |
| [demo-request-migration-prompt.md](prompts/completed/demo-request-migration-prompt.md) | Demo request lead capture migration | Implemented |
| [mobile-api-gaps-prompt.md](prompts/completed/mobile-api-gaps-prompt.md) | Mobile API gaps — /me, SSE, push endpoints | Implemented |
| [branching-security-filter-prompt.md](prompts/completed/branching-security-filter-prompt.md) | Per-app branching SecurityFilterChain | Implemented |
| [passkey-authentication-prompt.md](prompts/completed/passkey-authentication-prompt.md) | WebAuthn/FIDO2 passkey authentication | Implemented |
| [ip-whitelist-filter-prompt.md](prompts/completed/ip-whitelist-filter-prompt.md) | IP whitelist filter for akademia-plus-web | Implemented |
| [token-binding-antihijack-prompt.md](prompts/completed/token-binding-antihijack-prompt.md) | Token binding anti-hijack | Implemented |
| [hmac-api-signing-prompt.md](prompts/completed/hmac-api-signing-prompt.md) | HMAC API request signing | Implemented |
| [rate-limiting-prompt.md](prompts/completed/rate-limiting-prompt.md) | Redis sliding window rate limiting | Implemented |
| [magic-link-auth-prompt.md](prompts/completed/magic-link-auth-prompt.md) | Magic link passwordless authentication | Implemented |
| [component-test-coverage-gaps-prompt.md](prompts/completed/component-test-coverage-gaps-prompt.md) | Component tests for Phase 1–4 endpoints (26 tests) | Implemented |

---

## Workflows (`docs/workflows/`)

Step-by-step implementation plans. Referenced by prompts.

### Pending (`docs/workflows/pending/`)

_No pending workflows for core-api._

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
| [sonarqube-verification-workflow.md](workflows/completed/sonarqube-verification-workflow.md) | SonarQube verification | Implemented |
| [sse-notification-delivery-workflow.md](workflows/completed/sse-notification-delivery-workflow.md) | SSE notification delivery (WebappDeliveryChannelStrategy) | Implemented |
| [course-management-update-usecase-workflow.md](workflows/completed/course-management-update-usecase-workflow.md) | Course management update use cases | Implemented |
| [pos-business-logic-workflow.md](workflows/completed/pos-business-logic-workflow.md) | POS business logic implementation | Implemented |
| [email-notification-delivery-workflow.md](workflows/completed/email-notification-delivery-workflow.md) | Email notification delivery via Jakarta Mail/SMTP — 10 phases, ~35 new files | Implemented |
| [jwt-refresh-token-rotation-workflow.md](workflows/completed/jwt-refresh-token-rotation-workflow.md) | JWT refresh token rotation with Redis sessions | Implemented |
| [aws-ses-email-infra-workflow.md](workflows/completed/aws-ses-email-infra-workflow.md) | AWS SES email infrastructure | Implemented |
| [demo-request-migration-workflow.md](workflows/completed/demo-request-migration-workflow.md) | Demo request lead capture migration | Implemented |
| [mobile-api-gaps-workflow.md](workflows/completed/mobile-api-gaps-workflow.md) | Mobile API gaps — /me, SSE, push endpoints | Implemented |
| [branching-security-filter-workflow.md](workflows/completed/branching-security-filter-workflow.md) | Per-app branching SecurityFilterChain | Implemented |
| [passkey-authentication-workflow.md](workflows/completed/passkey-authentication-workflow.md) | WebAuthn/FIDO2 passkey authentication | Implemented |
| [ip-whitelist-filter-workflow.md](workflows/completed/ip-whitelist-filter-workflow.md) | IP whitelist filter for akademia-plus-web | Implemented |
| [token-binding-antihijack-workflow.md](workflows/completed/token-binding-antihijack-workflow.md) | Token binding anti-hijack | Implemented |
| [hmac-api-signing-workflow.md](workflows/completed/hmac-api-signing-workflow.md) | HMAC API request signing | Implemented |
| [rate-limiting-workflow.md](workflows/completed/rate-limiting-workflow.md) | Redis sliding window rate limiting | Implemented |
| [magic-link-auth-workflow.md](workflows/completed/magic-link-auth-workflow.md) | Magic link passwordless authentication | Implemented |
| [component-test-coverage-gaps-workflow.md](workflows/completed/component-test-coverage-gaps-workflow.md) | Component tests for Phase 1–4 endpoints (26 tests, 3 prod fixes) | Implemented |

---

## Root-Level Documents

| Document | Purpose |
|----------|---------|
| `CLAUDE.md` | Entry point — redirects to `docs/directives/CLAUDE.md` |
| `README.md` | Project overview and quick start |
