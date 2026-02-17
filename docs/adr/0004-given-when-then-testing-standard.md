# ADR-0004: Given-When-Then Testing Standard

**Status**: Accepted
**Date**: 2025-01-01
**Deciders**: ElatusDev

## Context

Consistent test structure improves readability, makes reviews faster, and reduces onboarding
friction. We needed a standard that enforces clarity about preconditions, actions, and expected
outcomes across all test code in the project.

## Decision

Adopt Given-When-Then (GWT) as the mandatory test structure for all test classes:

- **Given**: Set up preconditions and test data (mock stubbing, input construction)
- **When**: Execute the method under test (exactly one call)
- **Then**: Assert outcomes and verify interactions

Additional rules enforced alongside GWT:

| Rule | Rationale |
|------|-----------|
| `shouldDoX_whenY()` naming | Reads as a specification; grep-friendly |
| `@DisplayName("Should do X when Y")` | Human-readable in IDE test runners |
| `@Nested` classes with `@DisplayName` | Logical grouping by scenario or method |
| Zero `any()` matchers | Forces exact value matching; catches regressions |
| `public static final` constants shared between impl and tests | Single source of truth |
| Static imports for assertions | Clean test code (`assertThat`, `assertThatThrownBy`) |

## Alternatives Considered

1. **Arrange-Act-Assert (AAA)** — The most common alternative. Rejected because it's
   semantically identical to GWT but lacks the domain-language readability. GWT maps directly
   to BDD-style thinking and is more expressive when tests serve as living documentation.

2. **No enforced standard (developer's choice)** — Rejected because inconsistency across
   modules makes reviews slower and increases cognitive load when switching between areas
   of the codebase.

## Consequences

### Positive
- All tests read as specifications — clear preconditions, action, and expectations
- `@Nested` grouping creates navigable test suites in IDE runners
- Zero `any()` matchers catch subtle regressions that flexible matchers would miss
- Shared constants prevent drift between implementation and test assertions

### Negative
- Slightly more verbose than minimal test styles
- `any()` ban requires reading the implementation to know exact parameter values
- New contributors must learn the convention before their first PR

### Neutral
- Test template is documented in AI-CODE-REF.md sections 4.1–4.11
