# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for the AkademiaPlus platform.

ADRs document significant technical decisions — the context, options considered, decision made,
and consequences. They are immutable once accepted: if a decision is reversed, a new ADR
supersedes the old one rather than editing it.

## Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [0000](0000-adr-template.md) | ADR Template | — | — |
| [0001](0001-composite-keys-for-multi-tenancy.md) | Composite keys for multi-tenancy | Accepted | 2025-01-01 |
| [0002](0002-openapi-first-api-design.md) | OpenAPI-first API design | Accepted | 2025-01-01 |
| [0003](0003-field-encryption-with-hash-columns.md) | AES-256-GCM field encryption with hash columns | Accepted | 2025-01-01 |
| [0004](0004-given-when-then-testing-standard.md) | Given-When-Then testing standard | Accepted | 2025-01-01 |
| [0005](0005-conventional-commits.md) | Conventional Commits | Accepted | 2025-02-17 |

## Creating a New ADR

1. Copy `0000-adr-template.md`
2. Number it sequentially (e.g., `0006-your-decision.md`)
3. Fill in all sections — Context is the most important
4. Set status to `Proposed`, then update to `Accepted` after review
5. Update the index table above
6. Commit with `docs(adr): record decision on {topic}`
