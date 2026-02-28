# ADR-0002: OpenAPI-First API Design

**Status**: Accepted
**Date**: 2025-01-01
**Deciders**: ElatusDev

## Context

The platform exposes REST APIs consumed by web, mobile, and admin clients. We needed a strategy
that ensures API contracts are well-defined, consistent, and that frontend and backend teams can
work in parallel without blocking each other.

## Decision

Adopt contract-first (OpenAPI-first) API design:

- Each module defines its API contract in `src/main/resources/openapi/{module}-module.yaml`
- `openapi-generator-maven-plugin` generates Java interfaces and DTOs during Maven build
- Generated code lands in `target/generated-sources/openapi`
- Package convention: `openapi.akademiaplus.domain.{module}.api` / `.dto`
- All generated models use the `DTO` suffix
- Controllers implement the generated interfaces — they do not define their own endpoints

## Alternatives Considered

1. **Code-first with Springdoc** — Write controllers first, generate OpenAPI from annotations.
   Rejected because the contract becomes a second-class citizen derived from implementation
   details. Harder to review API changes in PRs (spread across annotations vs. a single YAML).

2. **Manual DTOs** — Hand-write all request/response objects. Rejected because it's error-prone,
   leads to drift between documentation and implementation, and duplicates effort.

## Consequences

### Positive
- API contract is the single source of truth — reviewable in one file
- Frontend teams can mock from the spec before backend implementation exists
- Generated DTOs guarantee consistency between spec and code
- Breaking API changes are visible in YAML diffs during PR review

### Negative
- openapi-generator produces verbose code with its own style (not always idiomatic)
- Build is slightly slower due to code generation phase
- Generated test files must be cleaned up (handled by `maven-antrun-plugin`)

### Neutral
- New endpoints require updating the YAML spec first, then implementing
