# CLAUDE.md — Project Context & Coding Directives

> **Canonical directive for Claude Code CLI and Claude Project interactions.**
> Read this file, `AI-CODE-REF.md`, and `DESIGN.md` before writing any code.

---

## Identity

**Project**: AkademiaPlus — Multi-tenant SaaS educational platform
**Repository**: `platform-core-api`
**Owner**: ElatusDev
**Stack**: Java 24, Spring Boot 4.0.3, MariaDB, Maven multi-module
**License**: Proprietary — all files carry ElatusDev copyright header
**Repo path**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`

---

## Hard Rules

1. **Testing**: Given-When-Then (NEVER Arrange-Act-Assert), `shouldDoX_whenY()` naming, ZERO `any()` matchers
2. **Constants**: ALL string literals → `public static final`, shared between impl and tests as single source of truth
3. **IDs**: Always `Long`, never `Integer`
4. **Exceptions**: Catch specific types only — never `Exception` or `Throwable`
5. **Architecture**: `interfaceadapters/` (controllers + repos) and `usecases/` (services)
6. **Commits**: Conventional Commits — `type(scope): subject` in imperative mood, ≤72 chars
7. **Methods**: < 20 lines, cyclomatic complexity < 10
8. **Javadoc**: Required on all public classes, methods, and constants
9. **Mock stubbing**: Stub with EXACT parameters the implementation passes (read the impl first)
10. **Test organization**: `@Nested` classes with `@DisplayName` for logical grouping

### Constant Extraction Pattern
```java
// Implementation class
public static final String ERROR_INPUT_NULL = "Input cannot be null or empty";

// Test class — import and reference the constant directly
assertThatThrownBy(() -> service.process(null))
    .hasMessage(MyService.ERROR_INPUT_NULL);
```

### Copyright Header (required on ALL files)
```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
```

---

## Architecture

### Module Dependency Graph (build order, bottom-up)

```
infra-common          ← Base persistence: TenantScoped, Auditable, SoftDeletable, ID assignment
     ↑
utilities             ← Security services: AES-GCM encryption, hashing, PII normalization, ID generation
     ↑
multi-tenant-data     ← JPA entity models: all @Entity classes, composite keys, data relationships
     ↑
security              ← Auth: JWT provider, filters, internal auth, module security configurators
     ↑
┌────┼────────┬─────────────┬──────────────┬───────────────┬──────────────┐
│    │        │             │              │               │              │
user-mgmt  billing  course-mgmt  notification  tenant-mgmt  pos-system
│    │        │             │              │               │              │
└────┼────────┴─────────────┴──────────────┴───────────────┴──────────────┘
     ↑
application           ← Spring Boot main class, assembles all modules

(Standalone services)
certificate-authority ← Separate Spring Boot app — JWKS trust broker
mock-data-system      ← Separate Spring Boot app — test data seeding
audit-system          ← Placeholder for audit logging
```

### Clean Architecture Mapping

Each domain module follows this internal structure:
```
module/
├── config/                    ← Spring @Configuration, security config, controller advice
├── exception/                 ← Module-specific domain exceptions
├── {aggregate}/
│   ├── interfaceadapters/     ← Controllers (@RestController) + Repositories (JPA)
│   └── usecases/              ← Business logic (@Service, @Transactional)
```

### Multi-Tenancy Model

- All tenant-scoped entities extend `TenantScoped → SoftDeletable → Auditable`
- Composite primary keys: `tenantId` + entity-specific ID (using `@IdClass`)
- Hibernate filter `tenantFilter` enforces row-level isolation
- `TenantContextHolder` stores current tenant in thread-local
- `EntityIdAssigner` generates IDs via Hibernate `PreInsertEvent` listener
- Tenant ID is injected automatically — entities never set their own IDs

### Security Layers

| Layer | Implementation |
|-------|---------------|
| Field-level encryption | `AESGCMEncryptionService` — AES-256-GCM with random IV per operation |
| PII normalization | `PiiNormalizer` — ReDoS-safe email regex, libphonenumber for phones |
| Data hashing | `HashingService` — SHA-256, salted hashing, constant-time comparison |
| JWT authentication | `JwtTokenProvider` + `JwtRequestFilter` |
| Module security | Per-module `SecurityConfiguration` classes |
| Soft delete | `@SQLDelete` + `@SQLRestriction` — no physical deletes |

### API Contract (OpenAPI-First)

- Each module defines `src/main/resources/openapi/{module}-module.yaml`
- `openapi-generator-maven-plugin` generates DTOs and API interfaces
- Generated code in `target/generated-sources/openapi`
- Package convention: `openapi.akademiaplus.domain.{module}.api` / `.dto`
- DTO suffix: all generated models end with `DTO`

---

## Build & Run

```bash
# Full build
mvn clean install

# Run tests for specific module
mvn test -pl utilities

# Skip tests
mvn clean install -DskipTests

# Docker (dev stack with e2e)
docker compose -f docker-compose.dev.yml --profile e2e up --build

# Docker (infra only, for IDE debugging)
docker compose -f docker-compose.dev.yml up trust-broker multi_tenant_db platform-core-redis
```

### Maven Profiles

| Profile | Modules | Purpose |
|---------|---------|---------|
| `platform-core-api` (default) | notification, course, user, billing, app, security, multi-tenant, utilities | Main platform |
| `ca-service` | certificate-authority | JWKS trust broker |
| `mock-data-service` | mock-data, multi-tenant, utilities | Test data generation |

---

## Key Technical Decisions

1. **Spring Boot 4.0.3 GA** — stable Foundation layer stack
2. **Java 24** — latest LTS, `maven.compiler.release=24`
3. **Entity inheritance**: `Auditable → SoftDeletable → TenantScoped → AbstractUser → ConcreteUser`
4. **ID generation**: Custom `SequentialIDGenerator` per-tenant, assigned via Hibernate event listener
5. **PII protection**: Encrypted at rest + hash columns for indexed search without decryption
6. **OpenAPI-first**: Contracts defined before implementation, code generated
7. **Jackson 3**: `tools.jackson.databind.ObjectMapper` — migrated from `com.fasterxml`
8. **JUnit 6**: `junit-jupiter 6.0.3` with Boot 4.x `spring-boot-starter-webmvc-test`
9. **Plain HTTP internally**: TLS is an infrastructure concern (reverse proxy / service mesh)
10. **Trust broker JWKS**: Services register JWT public keys with CA on startup

---

## Pull Request Checklist

- [ ] Code compiles: `mvn clean install -DskipTests`
- [ ] Tests pass: `mvn test -pl {module}`
- [ ] New public APIs have Javadoc with `@param`, `@return`, `@throws`
- [ ] All string literals extracted to constants
- [ ] Tests follow Given-When-Then with proper naming
- [ ] No `any()` matchers anywhere in test code
- [ ] Copyright header present on all new files
- [ ] Commit messages follow Conventional Commits
- [ ] No tenant isolation bypass (if touching data layer)

---

## Essential Docs

| Document | Path | Purpose |
|----------|------|---------|
| AI-CODE-REF.md | `docs/directives/` | Coding standards, review rules, detection patterns |
| DESIGN.md | `docs/design/` | Architecture, module catalog, multi-tenancy model |
| MANIFEST.md | `docs/` | Documentation status tracker |

---

## When Working on This Project

- Always read `AI-CODE-REF.md` before writing any code
- Check entity inheritance chain before modifying data models
- Verify tenant isolation is maintained in any new query or repository method
- Run `mvn test -pl {module}` after changes to verify test suite passes
- Use `public static final` constants for all error messages — never inline strings
- Use static imports for test assertions (`assertThat`, `assertThatThrownBy`)
