# ADR-0005: Conventional Commits

**Status**: Accepted
**Date**: 2025-02-17
**Deciders**: ElatusDev

## Context

As the project grows with multiple modules and contributors (human and AI), we need a commit
history that is parseable, searchable, and can drive automated tooling like changelog generation,
semantic versioning, and release notes.

## Decision

Adopt [Conventional Commits](https://www.conventionalcommits.org/) as the mandatory commit
message format:

```
type(scope): subject in imperative mood (≤72 chars)

Body explains WHY and WHAT changed. Wrap at 72 chars.
```

**Types**: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `perf`, `chore`

**Scopes** map to modules or cross-cutting concerns: `build`, `security`, `api`, `test`,
`docs`, `user`, `billing`, `course`, `tenant`, `notification`, `pos`, `ca`, `mock-data`,
`etl`, `audit`, `infra`, `utilities`, `multi-tenant`

**Breaking changes**: Append `!` after scope — `feat(api)!: remove legacy auth endpoint`

## Alternatives Considered

1. **Free-form messages** — No enforced format. Rejected because `git log` becomes unsearchable
   and changelog generation requires manual curation.

2. **Gitmoji** — Emoji-prefixed commits. Rejected because emoji adds visual noise in terminals,
   isn't grep-friendly, and doesn't map well to semantic versioning.

3. **Angular convention** — Very similar to Conventional Commits but predates the formal spec.
   We chose Conventional Commits because it's the standardized superset with broader tooling
   support.

## Consequences

### Positive
- `git log --oneline` is immediately scannable by type and scope
- CHANGELOG.md can be generated or verified against commit history
- Semantic versioning can be automated (`feat` → minor, `fix` → patch, `!` → major)
- Scopes make it easy to filter history per module (`git log --grep="billing"`)

### Negative
- Requires discipline (rejected commits if format is wrong)
- Scope list must be maintained as modules are added or renamed

### Neutral
- Full convention documented in CONTRIBUTING.md
