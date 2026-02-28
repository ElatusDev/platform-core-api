# CLAUDE.md — Project Context for Claude Code

## Identity

**Project**: AkademiaPlus Platform Core API
**Stack**: Java 24, Spring Boot 4.0.3, MariaDB, Maven multi-module (15 modules)
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`

## Essential Docs (read before coding)

- `AI-CODE-REF.md` — Coding standards, review rules, detection patterns, test conventions
- `DESIGN.md` — Architecture, module catalog, multi-tenancy model, security layers
- `CONTRIBUTING.md` — Onboarding, workflow, commit conventions, PR checklist
- `.claude/CLAUDE.md` — Extended project memory (module graph, key decisions, known debt)

## Hard Rules

- **Testing**: Given-When-Then (never AAA), `shouldDoX_whenY()` naming, ZERO `any()` matchers
- **Constants**: ALL string literals → `public static final`, shared between impl and tests
- **IDs**: Always `Long`, never `Integer`
- **Exceptions**: Catch specific types only — never `Exception` or `Throwable`
- **Architecture**: `interfaceadapters/` (controllers + repos) and `usecases/` (services)
- **Commits**: Conventional Commits — `type(scope): subject` in imperative mood, ≤72 chars

## Build

```bash
mvn clean install              # full build with tests
mvn clean install -DskipTests  # skip tests
mvn test -pl utilities         # test single module
```

## Multi-Tenancy (critical context)

Every tenant-scoped entity uses composite keys (`tenantId` + `entityId`). Hibernate filter
`tenantFilter` enforces row-level isolation. `TenantContextHolder` stores current tenant in
thread-local. Entities never set their own IDs — `EntityIdAssigner` handles it via
`PreInsertEvent` listener.

## Copyright Header (required on all new files)

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
```
