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
**Repo path**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`

---

## Hard Rules

1. **Testing**: Given-When-Then (NEVER Arrange-Act-Assert), `shouldDoX_whenY()` naming, ZERO `any()` matchers
2. **Constants**: ALL string literals → `public static final`, shared between impl and tests as single source of truth
3. **IDs**: Always `Long`, never `Integer`
4. **Exceptions**: Catch specific types only — never `Exception` or `Throwable`
5. **Architecture**: Use-Case-Centric Modular Architecture — see `DESIGN.md` Section 3.2 for layer rules and placement guidelines
6. **Commits**: Conventional Commits — `type(scope): subject` in imperative mood, ≤72 chars. **NEVER** include `Co-Authored-By`, `Generated with`, or any AI-tool attribution in commit messages
7. **Methods**: < 20 lines, cyclomatic complexity < 10
8. **Javadoc**: Required on all public classes, methods, and constants
9. **Mock stubbing**: Stub with EXACT parameters the implementation passes (read the impl first)
10. **Test organization**: `@Nested` classes with `@DisplayName` for logical grouping
11. **Test coverage**: Every new feature MUST include all three test tiers — see [Test Coverage Requirements](#test-coverage-requirements)
12. **External service abstraction**: External integrations (OAuth providers, payment gateways, email services) MUST define a strategy interface in `usecases/` — implementations live alongside the interface, enabling testability without mocking HTTP clients
13. **Non-entity domain objects**: Records and value objects that are NOT JPA entities go in `usecases/domain/` within the feature package — never in `interfaceadapters/` or `config/`
14. **Cross-module boundaries**: Domain modules may import repositories from other domain modules but NEVER import use cases — use-case-to-use-case calls are restricted to the `application` module only
15. **Workflow + Prompt documentation**: Every multi-file feature (3+ new production files) MUST have a paired workflow (`docs/workflows/`) and prompt (`docs/prompts/`) authored BEFORE implementation begins. Workflows follow `platform/knowledge-base/templates/WORKFLOW-TEMPLATE.md` (11 sections). Prompts follow `platform/knowledge-base/templates/PROMPT-TEMPLATE.md` (Temporal-inspired, 8 sections with Activity Model steps). See `AI-CODE-REF.md` Section 13 for format guide.
16. **Execution retrospective**: After every prompt execution completes (or is aborted), write a retrospective in `docs/retrospectives/` following `platform/knowledge-base/templates/RETROSPECTIVE-TEMPLATE.md`. The retrospective MUST include the Document Updates phase (§8) — all identified workflow, prompt, template, and memory corrections applied before closing.

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
 * Copyright (c) 2026 ElatusDev
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
utilities                ← ZERO internal deps. Security services: AES-GCM, hashing, PII, ID generation
     ↑
infra-common             ← Base persistence: TenantScoped, Auditable, SoftDeletable, EntityIdAssigner
     ↑
multi-tenant-data        ← JPA entity models: all @Entity classes, composite keys, data relationships
     ↑
security                 ← Auth: JWT provider, filters, internal auth, module security configurators
     ↑
┌────┼──────────┬───────────────┬────────────────────┬─────────────────┬──────────────┐
│    │          │               │                    │                 │              │
user-mgmt  billing  course-mgmt  notification-system  tenant-mgmt  pos-system  lead-mgmt
│    │          │               │                    │                 │              │
└────┼──────────┴───────────────┴────────────────────┴─────────────────┴──────────────┘
     ↑          Cross-domain: billing → user-mgmt + course-mgmt
     ↑                        course-mgmt → user-mgmt
     ↑                        pos-system → user-mgmt
application              ← Spring Boot main class, assembles all modules

(Standalone services — each has own Maven profile, Spring profile, SecurityFilterChain, Dockerfile)
certificate-authority    ← JWKS trust broker (profile: ca-service)
mock-data-service        ← Test data seeding via DataFaker (profile: mock-data-service)
etl-service              ← Data pipeline: Excel/Word → MongoDB staging → MariaDB (profile: etl-service)
audit-service            ← Audit logging (placeholder)
```

#### Module Naming Convention
- **Domain modules**: named by domain boundary (`-management`, `-system`, or plain)
- **Standalone services**: named with `-service` suffix — each independently deployable with own profile

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

### User-Level Data Isolation

- **Two isolation layers**: tenant isolation (automatic via Hibernate filter) + user isolation (explicit via `UserContextHolder`)
- Customer-facing JWT (OAuth/magic-link) embeds `profile_type` (`ADULT_STUDENT` | `TUTOR`) and `profile_id` (adultStudentId/tutorId) as claims
- `UserContextLoader` filter (order 4, after `JwtRequestFilter`) reads claims into `UserContextHolder` (ThreadLocal, same pattern as `TenantContextHolder`)
- `/v1/my/*` endpoints derive the user's profile ID from `UserContextHolder.requireProfileId()` — **never** from request parameters
- Admin endpoints (`/v1/user-management/*`, `/v1/billing/*`) remain unchanged — they accept explicit IDs for staff use
- Internal users (employees/collaborators) do not get profile claims — they use admin endpoints only

### Security Layers

| Layer | Implementation |
|-------|---------------|
| Field-level encryption | `AESGCMEncryptionService` — AES-256-GCM with random IV per operation |
| PII normalization | `PiiNormalizer` — ReDoS-safe email regex, libphonenumber for phones |
| Data hashing | `HashingService` — SHA-256, salted hashing, constant-time comparison |
| JWT authentication | `JwtTokenProvider` + `JwtRequestFilter` |
| User data isolation | `UserContextHolder` + `UserContextLoader` — profile ID from JWT, never client-supplied |
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

| Profile | Spring Profile | Modules | Purpose |
|---------|----------------|---------|---------|
| `platform-core-api` (default) | `dev` / `local` / `prod` / `qa` | notification-system, course-management, user-management, billing, lead-management, pos-system, tenant-management, application, security, multi-tenant-data, infra-common, utilities | Main platform |
| `ca-service` | `ca-service` | certificate-authority | JWKS trust broker |
| `mock-data-service` | `mock-data-service` | mock-data-service, multi-tenant-data, utilities | Test data generation |
| `etl-service` | `etl-service` | etl-service, multi-tenant-data, utilities, security, infra-common, user-management, course-management, billing, pos-system, tenant-management | Data pipeline |

Each standalone service has a matching `application-{profile}.properties` auto-loaded by Spring Boot when the profile is active.

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

## Test Coverage Requirements

Every new feature, endpoint, or behavioral change MUST include all three test tiers before it is considered complete. No tier is optional.

### Three-Tier Test Pyramid

| Tier | Type | Location | Runner | What It Verifies |
|------|------|----------|--------|-----------------|
| 1 | **Unit Tests** | `platform-core-api/{module}/src/test/` | `mvn test -pl {module}` | Individual classes in isolation — use cases, strategies, registries, controllers (standalone MockMvc) |
| 2 | **Component Tests** | `platform-core-api/{module}/src/test/` | `mvn verify -pl {module}` | Full Spring context + Testcontainers MariaDB — HTTP → Controller → UseCase → Repository → DB |
| 3 | **E2E Tests** | `core-api-e2e/` | `newman run` | Real running server over the network — deployed API contract, auth flow, cross-module integration |

### Per-Tier Scope

**Tier 1 — Unit Tests** (`*Test.java`):
- One test class per production class: `*UseCaseTest`, `*StrategyTest`, `*RegistryTest`, `*ControllerTest`
- Cover: happy path, error/exception paths, edge cases, input validation
- Follow: `AI-CODE-REF.md` Section 4 (Given-When-Then, zero `any()`, `@DisplayName`, `@Nested`)

**Tier 2 — Component Tests** (`*ComponentTest.java`):
- One test class per entity or feature endpoint
- Cover: all CRUD operations + every exception path from the Entity Exception Matrix
- Follow: `docs/workflows/completed/component-test-workflow.md`

**Tier 3 — E2E Tests** (Postman/Newman):
- Requests added to `core-api-e2e/Postman Collections/core-api-e2e.json`
- Cover: Create, GetById, GetAll, Delete + error codes (404, 409, 400)
- Follow: `core-api-e2e/docs/workflows/completed/E2E-TEST-WORKFLOW.md`

### When Planning a Feature

Workflow and prompt documents MUST include test phases for all three tiers. A feature plan is incomplete if it only specifies unit tests.

### Applicability

| Feature Type | Tier 1 (Unit) | Tier 2 (Component) | Tier 3 (E2E) |
|-------------|:---:|:---:|:---:|
| New CRUD entity | Required | Required | Required |
| New non-CRUD endpoint (e.g., OAuth login) | Required | Required | Required |
| New strategy / service / domain logic | Required | Required (if exposed via endpoint) | Required (if exposed via endpoint) |
| Internal refactor (no API change) | Required | Verify existing pass | Verify existing pass |
| Bug fix | Required (regression test) | Verify existing pass | Verify existing pass |

---

## Pull Request Checklist

- [ ] Code compiles: `mvn clean install -DskipTests`
- [ ] Unit tests pass: `mvn test -pl {module}`
- [ ] Component tests pass: `mvn verify -pl {module}`
- [ ] E2E tests updated in `core-api-e2e` (if new/changed endpoints)
- [ ] New public APIs have Javadoc with `@param`, `@return`, `@throws`
- [ ] All string literals extracted to constants
- [ ] Tests follow Given-When-Then with proper naming
- [ ] No `any()` matchers anywhere in test code
- [ ] Copyright header present on all new files
- [ ] Commit messages follow Conventional Commits
- [ ] No tenant isolation bypass (if touching data layer)
- [ ] No user isolation bypass — `/v1/my/*` endpoints use `UserContextHolder`, never request params for user ID

---

## Essential Docs

| Document | Path | Purpose |
|----------|------|---------|
| AI-CODE-REF.md | `docs/directives/` | Coding standards, review rules, detection patterns |
| DESIGN.md | `docs/design/` | Architecture, module catalog, multi-tenancy model |
| MANIFEST.md | `docs/` | Documentation status tracker |
| WORKFLOW-TEMPLATE.md | `platform/knowledge-base/templates/` | Canonical workflow template (11 sections) — all workflows must follow |
| PROMPT-TEMPLATE.md | `platform/knowledge-base/templates/` | Canonical prompt template (Temporal-inspired, 8 sections) — all prompts must follow |
| RETROSPECTIVE-TEMPLATE.md | `platform/knowledge-base/templates/` | Execution retrospective template (10 sections) — required after every prompt execution |

---

## When Working on This Project

- Always read `AI-CODE-REF.md` before writing any code
- Check entity inheritance chain before modifying data models
- Verify tenant isolation is maintained in any new query or repository method
- Run `mvn test -pl {module}` after changes to verify test suite passes
- Use `public static final` constants for all error messages — never inline strings
- Use static imports for test assertions (`assertThat`, `assertThatThrownBy`)
- **Always commit changes** at the end of any workflow, prompt execution, or task — never leave work uncommitted. Split commits by feature when changes span multiple modules or concerns

---

## Knowledge Base (`platform/knowledge-base/`)

The knowledge base is the platform's institutional memory. **Consult it before building, update it after building.**

### Before Starting Work (Consult)

- **Before designing a workflow**: Read `patterns/{domain}.md` for proven approaches, `anti-patterns/catalog.md` for known failure modes, `decisions/log.md` for prior decisions on similar problems
- **Before writing a prompt**: Read `patterns/prompt-engineering.md` for step structures that work, `patterns/testing.md` for test setup patterns to reference
- **Before a BAR (Before Action Review)**: Search patterns for "what worked on similar features", anti-patterns for "what could go wrong", `retrospective-index.md` for the current Improvement Kata target

### After Completing Work (Update)

- **After every prompt execution**: Write a retrospective following `templates/RETROSPECTIVE-TEMPLATE.md`. Execute §8 Document Updates — transfer new patterns, anti-patterns, and decisions to the knowledge base
- **After discovering a reusable pattern**: Add it to `patterns/{domain}.md` immediately — don't wait for the retrospective
- **After hitting a failure caused by a known gap**: Add it to `anti-patterns/catalog.md` with root cause and alternative
- **After making an implementation decision**: Record it in `decisions/log.md` with rationale and alternatives considered
- **After closing a retrospective**: Update `retrospective-index.md` with metrics from §9
