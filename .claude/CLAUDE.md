# AkademiaPlus Platform Core API — Claude Project Memory

## Identity

**Project**: Akademia Plus — Multi-tenant SaaS educational platform  
**Repository**: `platform-core-api`  
**Owner**: ElatusDev  
**Stack**: Java 21, Spring Boot 4.0.0-M3, MariaDB, Maven multi-module  
**License**: Proprietary — all files carry ElatusDev copyright header

---

## Coding Standards

**CRITICAL**: All code must follow the comprehensive standards in `AI-CODE-REF.md`.

### Non-Negotiable Rules

1. **Testing pattern**: Given-When-Then (NEVER Arrange-Act-Assert)
2. **No `any()` matchers**: Use exact values, `verifyNoMoreInteractions()`, or `ArgumentCaptor`
3. **Mock stubbing**: Stub with EXACT parameters the implementation passes (read the impl first)
4. **Test naming**: `shouldDoX_whenGivenY()` with `@DisplayName("Should do X when given Y")`
5. **Test organization**: `@Nested` classes with `@DisplayName` for logical grouping
6. **Constants**: Extract ALL string literals to `public static final` constants — shared between impl and tests as single source of truth
7. **Exceptions**: Catch specific exceptions only — never `Exception` or `Throwable`
8. **Methods**: < 20 lines, cyclomatic complexity < 10
9. **Javadoc**: Required on all public classes, methods, and constants
10. **IDs**: ALL IDs use `Long` (never `Integer`)

### Constant Extraction Pattern
```java
// Implementation class
public static final String ERROR_INPUT_NULL = "Input cannot be null or empty";

// Test class — import and reference the constant directly
assertThatThrownBy(() -> service.process(null))
    .hasMessage(MyService.ERROR_INPUT_NULL);
```

### Copyright Header (Required on ALL files)
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
certificate-authority ← Separate Spring Boot app for TLS/PKI
mock-data-system      ← Separate Spring Boot app for test data generation
etl-system            ← Placeholder for ETL pipelines
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

- **Interface Adapters** = Controllers + Repositories (outer ring)
- **Use Cases** = Application services (middle ring)
- **Entities** = `multi-tenant-data` module (inner ring, shared)

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

# Build specific profile
mvn clean install -P ca-service
mvn clean install -P mock-data-service

# Docker (dev)
docker compose -f docker-compose.dev.yml up --build
```

### Maven Profiles

| Profile | Modules | Purpose |
|---------|---------|---------|
| `platform-core-api` (default) | notification, course, user, billing, app, security, multi-tenant, utilities | Main platform |
| `ca-service` | certificate-authority | TLS/PKI standalone service |
| `mock-data-service` | mock-data, multi-tenant, utilities | Test data generation |

---

## Key Technical Decisions

1. **Spring Boot 4.0.0-M3**: Milestone release — expect API changes
2. **Entity inheritance**: `Auditable → SoftDeletable → TenantScoped → AbstractUser → ConcreteUser`
3. **ID generation**: Custom `SequentialIDGenerator` per-tenant, assigned via Hibernate event listener
4. **PII protection**: Encrypted at rest + hash columns for indexed search without decryption
5. **OpenAPI-first**: Contracts defined before implementation, code generated
6. **Dependency convergence**: `maven-enforcer-plugin` with `DependencyConvergence` + `RequireUpperBoundDeps`

---

## Known Technical Debt

1. **Dockerfile** references old module names (communication, coordination, datamodel, people, treasury) — needs update to current names
2. **docker-compose.dev.yml** references old Docker image `elatusdevops/makani-helpdesk-api:dev`
3. **SonarQube workflow** references `makani-helpdesk-api` project key — needs update
4. **Several modules are placeholders**: `etl-system`, `audit-system`, `pos-system` only contain `Main.java`
5. **notification-system** has OpenAPI specs but no Java implementation yet

---

## When Working on This Project

- Always read `AI-CODE-REF.md` before writing any code
- Check entity inheritance chain before modifying data models
- Verify tenant isolation is maintained in any new query or repository method
- Run `mvn test -pl {module}` after changes to verify test suite passes
- Use `public static final` constants for all error messages — never inline strings
- Use static imports for test assertions (`assertThat`, `assertThatThrownBy`)
