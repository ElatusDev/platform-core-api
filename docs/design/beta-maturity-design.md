# AkademiaPlus Platform — Beta Maturity Plan

> **Living document.** Update the status column as waves complete.  
> **Owner**: ElatusDev  
> **Goal**: Enterprise-grade multi-tenant SaaS API ready for beta tenants  
> **Last updated**: 2026-02-26

---

## How to Read This Document

Each wave is a self-contained execution unit. A wave may not start until its
predecessor's **exit criteria** are fully met. Each wave lists:

- **Entry criteria** — what must already be true before starting
- **Artifacts** — docs, prompts, and code produced
- **Exit criteria** — verifiable conditions that confirm the wave is done
- **Claude Code prompt** — path to the executable automation file, or `NEEDED`

Status values: `✅ Done` | `🔄 In Progress` | `⬜ Pending` | `🔴 Blocked`

---

## Dependency Graph (Wave Order)

```
Wave 0 — Dependency Upgrade
    │
    ▼
Wave 1 — Exception Advice Consolidation
    │
    ├──────────────────────────┐
    ▼                          ▼
Wave 2 — Creation UseCases   Wave 3 — Delete UseCases
    │                          │
    └──────────┬───────────────┘
               ▼
Wave 4 — Component Tests (ADR-0006)
               │
               ▼
Wave 5 — @SQLDelete Bug Fix + Delete UseCase Rollout
               │
               ▼
Wave 6 — E2E Test Suite
               │
               ▼
Wave 7 — CI/CD Pipeline Repairs
               │
               ▼
Wave 8 — Database Migration Strategy (Flyway)
               │
               ▼
Wave 9 — Observability Baseline
               │
               ▼
Wave 10 — Security Hardening
               │
               ▼
Wave 11 — API Documentation for Consumers
               │
               ▼
          [ BETA RELEASE ]
```

---

## Wave 0 — Dependency Upgrade

**Status**: ✅ Done
**Effort**: ~4h
**Completed**: 2026-02-27

### Why First

Spring Boot 4.0.0-M3 is a milestone. The known `SpringBeanContainer` timing
race with JPA converters and `@Value` resolution is fixed in the GA. Hibernate
7.2.5.Final (shipped with Boot 4.0.3) resolves `PreInsertEvent` ordering issues
that have caused runtime crashes in the EntityIdAssigner. Everything else builds
on a stable foundation.

### Entry Criteria

- Docker Desktop running (Testcontainers dependency)
- All existing tests green on current stack

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| Human-readable upgrade plan | N/A (upgrade documented in commit history) _(generate from prompt)_ | ⬜ |
| Claude Code execution prompt | `docs/prompts/completed/dependency-upgrade-prompt.md` | ✅ Written |

### Work

| Phase | Description |
|-------|-------------|
| 1 | `spring-boot-starter-parent` 4.0.0-M3 → 4.0.3 GA (Hibernate 7.2.5) |
| 1 | Java `--release` 21 → 24, JUnit Jupiter 5.13.4 → 6.0.3 |
| 1 | Remove `spring-boot-maven-plugin` version pin |
| 2 | `springdoc-openapi` 2.8.9 → 3.0.1 |
| 2 | Re-enable Swagger UI in `application.properties` |
| 3 | Jackson 2 → 3 import namespace rewrite across all modules |
| 3 | Remove explicit Jackson 2 BOM and version pins |

### Exit Criteria

- `mvn clean install` green with zero test failures
- No remaining `import com.fasterxml.jackson` in hand-written source (grep verify)
- `/v3/api-docs` endpoint returns 200 with aggregated OpenAPI JSON
- `CLAUDE.md` Stack line updated to `Java 25, Spring Boot 4.0.2`

---

## Wave 1 — Exception Advice Consolidation

**Status**: ✅ Done
**Effort**: ~6h (completed)
**Blocks**: Waves 2, 3, 4, 6 (E2E `$.code` assertions depend on consistent error codes)

### Why This Order

Every downstream workflow — Delete UseCases, Component Tests, E2E Tests — asserts
on the `$.code` field in error responses. If `PeopleControllerAdvice` returns
`400` for constraint violations and `BillingControllerAdvice` returns `409`, the
E2E assertion layer breaks. This must be consistent before any test coverage
is written.

### Entry Criteria

- Wave 0 complete (stable Boot 4.0.2 stack)

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| Exception advice specification | `docs/design/exception-advice-strategy.md` | ✅ Exists |
| Exception advice workflow (human) | `docs/workflows/completed/exception-advice-workflow.md` | ✅ Exists |
| Claude Code execution prompt | `docs/prompts/completed/exception-advice-consolidation-prompt.md` | ✅ Exists |
| Controller advice audit | `docs/workflows/completed/controller-advice-audit-workflow.md` | ✅ Exists |

### Work (10 phases in prompt)

| Phase | Description |
|-------|-------------|
| 1 | `EntityType` constants class in `utilities` |
| 2 | 3 generic exceptions: `EntityNotFoundException`, `EntityDeletionNotAllowedException`, `DuplicateEntityException` |
| 3 | Message properties — new keys, no removals |
| 4 | `MessageService` — 6 generic methods, 20 methods deprecated |
| 5 | `BaseControllerAdvice` abstract class — 8 shared handlers |
| 6 | Unit tests for all new infrastructure |
| 7 | Module `ControllerAdvice` classes extend `BaseControllerAdvice` |
| 8 | Exception throw sites migrated to generic exceptions (all modules) |
| 9 | Test files updated to reference generic exceptions |
| 10 | Old per-entity exception classes marked `@Deprecated` |

### Exit Criteria

- `mvn clean test` green across all modules
- Zero `@ExceptionHandler` for `EntityNotFoundException` or `XxxNotFoundException` outside `BaseControllerAdvice`
- All error responses carry both `message` and `code` fields
- `grep -rn "throw new.*NotFoundException" */src/main/` returns zero hits for old per-entity types
- HTTP status audit: duplicates/deletions return 409, not 400

---

## Wave 2 — Creation UseCase Rollout

**Status**: ✅ Complete (per `creation-usecase-workflow.md` Phase map)  
**Effort**: ~0h remaining

All 15 root entities have `CreationUseCase` implementations per the Phase 0–5
execution plan. The `CreateCourseUseCase` was refactored to the canonical pattern.
All 6 billing association entities have their own use cases.

### Verification

```bash
# Confirm all creation use cases exist
find . -name "*CreationUseCase.java" -path "*/main/*" | sort

# Full test suite
mvn clean test
```

### Exit Criteria (verify before proceeding to Wave 4)

- `*CreationUseCase.java` exists for all 15 root entities
- All use cases follow the canonical pattern (named TypeMap, `ApplicationContext.getBean()`)
- Unit tests exist for every use case: `*CreationUseCaseTest.java`
- `mvn clean test` green

---

## Wave 3 — Delete UseCase Infrastructure

**Status**: ✅ Done
**Effort**: ~26h (completed)
**Depends on**: Wave 1 (generic exceptions must exist first)

### Why Before Component Tests

Component tests assert on deletion behavior — 204, 404, and 409 responses. Without
delete use cases the component test matrix is incomplete and tests would need to be
written twice.

### Entry Criteria

- Wave 1 complete (`EntityNotFoundException`, `EntityDeletionNotAllowedException` exist)
- `DeleteUseCaseSupport` utility stub not yet needed — exception classes suffice

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| Delete strategy | `docs/design/delete-usecase-strategy.md` | ✅ Exists |
| Delete workflow (human) | `docs/workflows/completed/delete-usecase-workflow.md` | ✅ Exists |
| Claude Code execution prompt | `docs/prompts/completed/delete-usecase-rollout-prompt.md` | ✅ Exists |

### Work (completed)

| Phase | Description | Scope | Status |
|-------|-------------|-------|--------|
| 0 | Fix `@SQLDelete` WHERE clause bug — all 29 entities | `multi-tenant-data` | ✅ `48d844c` |
| 1 | `DeleteUseCaseSupport` utility + `TenantContextHolder.requireTenantId()` | `utilities` | ✅ `babc1a2` |
| 2 | Refactor 3 existing + add Tutor + MinorStudent delete use cases | `user-management` | ✅ `b4a26f2` |
| 3 | 6 billing delete use cases | `billing` | ✅ `9948334` |
| 4 | Course, Schedule, CourseEvent | `course-management` | ✅ `9825a7a` |
| 5 | StoreProduct, StoreTransaction | `pos-system` | ✅ `5a55a93` |
| 6 | Tenant, TenantSubscription, Notification | `tenant-management`, `notification-system` | ✅ `38e4fbb`, `653ff42` |
| 7 | Integration tests — `@SQLDelete` single-row verification | all | ✅ `067eb37` |

### Exit Criteria (verified)

- ✅ `@SQLDelete` on all 29 entities includes both `tenant_id` and `entity_id` in WHERE clause
- ✅ Delete use case exists for every entity exposed in OpenAPI
- ✅ Unit tests for every delete use case (Given-When-Then, zero `any()`)
- ✅ Integration test verifies single-row soft-delete
- ✅ `mvn clean verify` green

---

## Wave 4 — Component Test Coverage (ADR-0006)

**Status**: ✅ Done
**Effort**: ~20h (completed)
**Depends on**: Waves 1, 2, 3

### Why

Component tests are the primary confidence layer between unit tests and E2E. They
verify multi-tenant isolation, `@SQLRestriction` behaviour, Hibernate filter
composition, composite key correctness, and the full exception → HTTP mapping
chain. Without them, bugs are found in production or by the E2E suite — both
too late.

### Entry Criteria

- Wave 1 done (exception codes consistent)
- Wave 2 done (creation use cases exist)
- Wave 3 done (delete use cases exist, `@SQLDelete` fixed)
- Docker Desktop running (Testcontainers)

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| Component test workflow | `docs/workflows/completed/component-test-workflow.md` | ✅ Exists |
| Claude Code execution prompt | N/A (executed inline) | N/A (executed directly) |

### Work (completed)

All 19 entities have component tests across 6 modules (`225eb40`). Integration test
infrastructure added (`067eb37`).

| Phase | Entities | Module | Status |
|-------|----------|--------|--------|
| 1 | Tenant, TenantSubscription, TenantBillingCycle | `tenant-management` | ✅ |
| 2 | Employee, Collaborator, AdultStudent, Tutor, MinorStudent | `user-management` | ✅ |
| 3 | Course, Schedule, CourseEvent | `course-management` | ✅ |
| 4 | Membership, Compensation, PaymentAdultStudent, PaymentTutor, MembershipAdultStudent, MembershipTutor | `billing` | ✅ |
| 5 | Notification | `notification-system` | ✅ |
| 6 | StoreProduct, StoreTransaction | `pos-system` | ✅ |

### Exit Criteria (verified)

- ✅ One `*ComponentTest.java` per entity (19 total)
- ✅ Every entity covers: Create 201, Create conflict 409, GetById 200/404, GetAll 200, Delete 204/404/409
- ✅ `@SQLRestriction` exclusion verified post-delete
- ✅ `mvn verify` green across all modules

---

## Wave 5 — Docker / CI Fixes

**Status**: ✅ Done
**Effort**: ~3h
**Depends on**: Wave 0 ✅
**Completed**: 2026-02-27

### Why This Order

No other wave depends on CI — but CI must work before beta because every merged PR
needs a reliable path to a deployable artifact. This is deliberately placed after the
hard implementation waves so it doesn't block them, but it must complete before beta.

### Completed

- ✅ `Dockerfile` module references — already aligned with current artifact IDs
- ✅ `mock-data-service/Dockerfile` — correctly references all dependency modules
- ✅ `certificate-authority/Dockerfile` — correctly references all reactor POMs
- ✅ `docker-compose.dev.yml` — 5 services with healthchecks, proper `depends_on` ordering
- ✅ `docker-compose.dev.yml` — E2E runner service added (profile-gated)
- ✅ Docker environment file for Newman (`platform-api-e2e/environments/docker.postman_environment.json`)
- ✅ `run-e2e.sh` convenience script at AkademiaPlus root

### Remaining Work — All Complete

| Item | File | Change | Status |
|------|------|--------|--------|
| `docker-compose.qa.yml` port fix | root | `8443:8443` → `8080:8080` | ✅ |
| SonarQube project key | `.github/workflows/build.yml` | Already correct: `ElatusDev_platform-core-api` | ✅ |
| Java version in CI workflows | `.github/workflows/*.yml` | Updated to `java-version: '24'` (Corretto) | ✅ |
| Deploy port fix | `.github/workflows/docker-deploy-aws.yml` | `8443:8443` → `8080:8080` | ✅ |

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| Claude Code execution prompt | `docs/prompts/pending/ci-fixes-prompt.md` (not yet created) | 🔴 NEEDED (CI-only items) |
| E2E Docker runner | `docker-compose.dev.yml` (e2e-runner service) | ✅ Done |
| Docker Newman env | `platform-api-e2e/environments/docker.postman_environment.json` | ✅ Done |
| E2E convenience script | `AkademiaPlus/run-e2e.sh` | ✅ Done |

### Exit Criteria

- ✅ `docker compose -f docker-compose.dev.yml build` succeeds with zero errors
- ✅ `docker compose --profile e2e` runs Newman against Docker-internal stack
- ✅ GitHub Actions SonarQube workflow — project key already correct (`ElatusDev_platform-core-api`)
- ✅ Java 24 (Corretto) set in all CI build steps

### Claude Code Prompt

> **N/A** — Wave 5 was executed directly without a formal prompt. Docker and CI fixes
> were applied across the session that produced the Spring Boot 4.0.3 + Java 24 upgrade.

---

## Wave 6 — E2E Test Suite

**Status**: ⬜ Pending  
**Effort**: ~6h  
**Depends on**: Wave 1 (error codes), Wave 2 (creation), Wave 3 (deletion)

### Entry Criteria

- Wave 1 done (all `$.code` values consistent)
- Wave 2 done (all entities have creation endpoints)
- Wave 3 done (all entities have deletion endpoints)
- Platform running locally (Docker + mock data)

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| E2E test workflow (human) | `platform-api-e2e/docs/E2E-TEST-WORKFLOW.md` | ✅ Exists |
| Claude Code execution prompt | `platform-api-e2e/.claude/prompts/e2e-test-workflow.md` | ✅ Written |
| Docker E2E runner service | `docker-compose.dev.yml` (profile: e2e) | ✅ Done |
| Docker Newman environment | `platform-api-e2e/environments/docker.postman_environment.json` | ✅ Done |
| E2E convenience script | `AkademiaPlus/run-e2e.sh` | ✅ Done |

### Work (6 phases in prompt)

| Phase | Description |
|-------|-------------|
| 1 | Audit — parse collection JSON + read all OpenAPI specs |
| 2 | Refactor — rename folders, fix 400→409, add `$.code` assertions, define variables |
| 3 | User Management completion — Tutor + MinorStudent tests |
| 4a | Tenant Management — 3 entities |
| 4b | Billing — 6 entities |
| 4c | Course Management — Schedule + CourseEvent + complete Course |
| 4d | Notification — 1 entity |
| 4e | POS — 2 entities |
| 5 | Variable dependency audit script |
| 6 | Newman dry-run + environment files + final commit |

### Exit Criteria

- `newman run --dry-run` passes (zero structural errors)
- ~182 total requests (34 existing refactored + ~148 new)
- All `$.code` assertions present on error responses
- `environments/local.postman_environment.json` created
- `environments/docker.postman_environment.json` created (Docker-internal `baseUrl`)
- Live run against local server: all tests pass
- `./run-e2e.sh` passes end-to-end against Docker stack

---

## Wave 7 — Database Migration Strategy

**Status**: ⬜ Pending  
**Effort**: ~8h  
**Depends on**: Wave 4 (component tests confirm schema is stable)

### Why Before Beta

`spring.jpa.hibernate.ddl-auto=validate` means deploying a schema change to a
live tenant database is a manual, risky operation. Flyway provides version-controlled,
repeatable, rollback-capable migrations. Beta is the last moment to introduce this
without existing production data to worry about.

### Entry Criteria

- Wave 4 done (schema validated by component tests — no more schema changes expected from test discoveries)
- All `@SQLDelete` WHERE clauses fixed (Wave 3 Phase 0)

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| ADR-0008 (Flyway adoption) | `docs/design/adr/0008-flyway-migration-strategy.md` | 🔴 NEEDED |
| Flyway baseline workflow | `docs/workflows/FLYWAY-BASELINE-WORKFLOW.md` | 🔴 NEEDED |
| Claude Code execution prompt | `docs/prompts/pending/flyway-baseline-prompt.md` (not yet created) | 🔴 NEEDED |

### Work

| Step | Description |
|------|-------------|
| 1 | Add `spring-boot-starter-flyway` to root `pom.xml` `<dependencyManagement>` |
| 1 | Add Flyway dependency to `application` module |
| 2 | Set `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=0` for first run |
| 2 | Set `spring.jpa.hibernate.ddl-auto=none` (remove `validate` — Flyway owns schema now) |
| 3 | Generate `V1__baseline.sql` from current MariaDB schema (`mysqldump --no-data`) |
| 3 | Place in `application/src/main/resources/db/migration/` |
| 4 | Write `V2__fix_sqldel_where_clauses.sql` for `@SQLDelete` fixes if not already applied |
| 5 | Update Testcontainers schema init: point to Flyway migration directory (replace `00-schema-dev.sql`) |
| 6 | Run `mvn verify` — Flyway applies migrations, component tests validate schema |
| 7 | Write rollback script for V1 (baseline rollback is always a manual procedure — document it) |

### Exit Criteria

- `spring.jpa.hibernate.ddl-auto=none` in all profiles
- `V1__baseline.sql` committed, Flyway history table populated on first run
- `mvn verify` green (Testcontainers applies Flyway migrations, not static SQL)
- `application-dev.properties` and `application-local.properties` include Flyway config
- ADR-0008 written and committed

### Docs Needed

**ADR-0008**: Decision to adopt Flyway, rationale (schema drift risk, rollback capability,
tenant deployment safety), consequences, alternatives considered (Liquibase).

**FLYWAY-BASELINE-WORKFLOW.md**: Step-by-step guide for generating future migrations,
naming conventions (`V{n}__{description}.sql`), testing migrations locally, and
rollback procedure for each migration.

**Claude Code Prompt**: Auto-generate the baseline SQL from the current schema,
insert Flyway dependency, configure properties, update Testcontainers setup.

---

## Wave 8 — Observability Baseline

**Status**: ✅ Done
**Effort**: ~4h
**Depends on**: Wave 5 ✅
**Completed**: 2026-02-27

### Why Before Beta

A multi-tenant system with no health endpoints cannot be deployed to any container
orchestration layer (ECS, GKE, Railway, Render). Without metrics and structured
logging, debugging a production incident requires access to raw container logs —
unacceptable for enterprise clients.

### Entry Criteria

- Wave 5 done (Docker build works)
- Target observability stack decided (Prometheus/Grafana vs Datadog vs CloudWatch)

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| ADR-0009 (Observability stack) | `docs/design/adr/0009-observability-strategy.md` | ⬜ Not created (implemented without ADR) |
| Claude Code execution prompt | N/A | ✅ Executed directly |

### Work

| Item | Change | File |
|------|--------|------|
| Actuator endpoints | Enable `health`, `health/liveness`, `health/readiness`, `metrics`, `info` | `application.properties` |
| Remove `mappings` from prod exposure | `mappings` is dev-only | `application.properties` |
| Structured JSON logging | Configure `log4j2.xml` for JSON output in non-local profiles | `log4j2.xml` |
| MDC correlation ID | `CorrelationIdFilter` — generate UUID per request, add to MDC, propagate to response header | New `@Component` |
| Micrometer Prometheus | Add `micrometer-registry-prometheus` dependency | root `pom.xml` |
| Custom tenant metric tag | Tag all metrics with `tenantId` via `MeterFilter` bean | New `@Configuration` |
| Health indicator | Custom `DatabaseHealthIndicator` verifying MariaDB + tenant sequence table | New `@Component` |
| docker-compose healthcheck | Add `healthcheck` stanza to services in `docker-compose.dev.yml` | `docker-compose.dev.yml` |

### Exit Criteria

- `GET /api/actuator/health` returns `{"status":"UP"}`
- `GET /api/actuator/health/liveness` and `/readiness` return 200
- `GET /api/actuator/metrics` returns Prometheus-format text
- Every log line in non-local profiles is valid JSON with `correlationId` field
- `X-Correlation-Id` response header present on all API responses
- `mappings` endpoint NOT exposed in `application.properties` (prod profile)

---

## Wave 9 — Security Hardening

**Status**: 🔄 In Progress (~4h remaining)
**Effort**: ~6h
**Depends on**: Wave 8 ✅

### Entry Criteria

- Wave 8 done (health/liveness endpoints active)
- Target deployment architecture decided (behind reverse proxy vs embedded rate limiting)

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| ADR-0010 (Rate limiting strategy) | `docs/design/adr/0010-rate-limiting-strategy.md` | 🔴 NEEDED |
| ADR-0011 (Secrets management) | `docs/design/adr/0011-secrets-management.md` | 🔴 NEEDED |
| Claude Code execution prompt | `docs/prompts/pending/security-hardening-prompt.md` (not yet created) | 🔴 NEEDED |

### Work

**Rate Limiting** (Bucket4j in-process — no gateway dependency):

| Item | Description |
|------|-------------|
| Add `bucket4j-spring-boot-starter` | Root `pom.xml` |
| Auth endpoint rate limit | `/v1/auth/**` — 10 requests/minute per IP |
| Global tenant rate limit | All endpoints — 1000 requests/minute per `tenantId` |
| 429 response body | `{"code":"RATE_LIMIT_EXCEEDED","message":"..."}` via `RateLimitExceededHandler` |
| Rate limit headers | `X-RateLimit-Remaining`, `X-RateLimit-Reset` on all responses |

**Secrets Management** (Spring Cloud Config + environment-specific):

| Item | Description |
|------|-------------|
| Document rotation policy | `ENCRYPTION_KEY`, `KEYSTORE_PASS`, `MP_ACCESS_TOKEN` — rotation cadence and procedure |
| AWS Secrets Manager integration | Optional for prod: `spring-cloud-starter-aws-secrets-manager` |
| Remove secrets from docker-compose | Replace plaintext values with `${ENV_VAR}` references |
| `.env.example` file | Document all required environment variables with placeholder values |
| Validate secrets at startup | `@PostConstruct` assertion that required env vars are non-null/non-empty |

**Security Headers**:

| Header | Value |
|--------|-------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Content-Security-Policy` | `default-src 'none'` (API-only, no HTML) |

**Token Refresh** (currently missing):

| Item | Description |
|------|-------------|
| `POST /v1/auth/refresh` endpoint | Accept refresh token, return new access token |
| Refresh token storage | Short-lived JWT signed with separate key, stored server-side hash |
| Token blacklist on logout | `POST /v1/auth/logout` invalidates current access + refresh token |

### Exit Criteria

- Auth endpoint rejects 11th request/minute from same IP with 429
- `/api/actuator/**` not rate-limited (excluded)
- All security headers present on API responses (verify via curl)
- No plaintext secrets in docker-compose files
- `.env.example` committed to repo root
- Token refresh flow works end-to-end

---

## Wave 10 — Placeholder Module Decision

**Status**: ✅ Done (Option A for etl, Option C for audit)
**Effort**: ~1h
**Depends on**: None
**Completed**: 2026-02-27

### Context

`etl-service` and `audit-service` are Maven modules containing only `Main.java`.
They appear in the build graph, inflate the module count, and imply capabilities
that don't exist.

### Decision Required

| Option | Effort | Consequence |
|--------|--------|-------------|
| **A: Remove from parent POM** | ~1h | Modules no longer build, Docker images not produced |
| **B: Implement audit MVP** | ~20h | Audit log of all write operations per tenant |
| **C: Stub to structured placeholder** | ~2h | Module compiles but returns 501 on all endpoints |

**Recommendation**: Option A for `etl-service` (genuinely out of scope for beta).
Option C for `audit-service` (enterprise clients expect audit trails — surface the
endpoint so the contract is established, even if the implementation is deferred).

Audit MVP scope (if Option B chosen for `audit-service`):
- `AuditEvent` entity: `tenantId`, `entityType`, `entityId`, `action` (CREATE/UPDATE/DELETE), `actorId`, `timestamp`, `before`, `after` (JSON)
- Spring AOP `@Around` advice on all `@Service` methods annotated with `@Audited`
- `GET /v1/audit?entityType=&entityId=&from=&to=` paginated query

### Exit Criteria

- `mvn clean install` does not include stub modules in main artifact (Option A) OR
- `GET /v1/audit` returns `501 Not Implemented` with `{"code":"NOT_IMPLEMENTED"}` (Option C) OR
- Audit log populated by all write operations (Option B)

---

## Wave 11 — API Documentation for Consumers

**Status**: 🔄 In Progress (~4h remaining)
**Effort**: ~6h
**Depends on**: Wave 6 (E2E tests confirm API contract is stable before documenting it)

### Entry Criteria

- Wave 6 done (E2E suite green — API contract is verified and stable)
- springdoc 3.0.1 configured (Wave 0)

### Artifacts

| Artifact | Location | Status |
|----------|----------|--------|
| Consumer onboarding guide | `docs/consumer-onboarding.md` | 🔴 NEEDED |
| Error code catalogue | `docs/error-codes.md` | 🔴 NEEDED |
| OpenAPI aggregation config | `application/src/main/resources/application.properties` | 🔴 NEEDED |

### Work

| Item | Description |
|------|-------------|
| Aggregate OpenAPI document | Configure springdoc to merge all module YAMLs into `/v3/api-docs` |
| Swagger UI global auth | Pre-configure `SecurityScheme` so Swagger UI shows "Authorize" button with JWT |
| `@OpenAPIDefinition` on TestApp | Title, description, version, contact, license |
| Error code catalogue | Markdown table of all `$.code` values with description, HTTP status, resolution |
| Consumer onboarding guide | How to create a tenant, get a JWT, make first API call, understand pagination |
| Postman collection export | Export final Postman collection as consumer-facing sample collection |
| Rate limit documentation | Document `X-RateLimit-*` headers and limits per endpoint group |

### Exit Criteria

- `GET /api/v3/api-docs` returns single merged OpenAPI document containing all module paths
- Swagger UI at `/api/swagger-ui/index.html` shows "Authorize" button, token persists across requests
- `docs/error-codes.md` lists every `$.code` value from `BaseControllerAdvice` + domain-specific codes
- `docs/consumer-onboarding.md` contains a working curl walkthrough from tenant creation to first entity creation

---

## Summary Table

| Wave | Name | Status | Effort | Prompt |
|------|------|--------|--------|--------|
| 0 | Dependency Upgrade | ✅ | ~4h | `docs/prompts/completed/dependency-upgrade-prompt.md` ✅ |
| 1 | Exception Advice Consolidation | ✅ | — | `docs/prompts/completed/exception-advice-consolidation-prompt.md` ✅ |
| 2 | Creation UseCase Rollout | ✅ | — | `docs/workflows/completed/creation-usecase-workflow.md` ✅ |
| 3 | Delete UseCase Infrastructure | ✅ | — | `docs/prompts/completed/delete-usecase-rollout-prompt.md` ✅ |
| 4 | Component Test Coverage | ✅ | — | N/A (executed directly) |
| 5 | Docker / CI Fixes | ✅ | — | Done (no prompt needed) |
| 6 | E2E Test Suite | ⬜ | ~6h | `platform-api-e2e/.claude/prompts/e2e-test-workflow.md` ✅ |
| 7 | Database Migration (Flyway) | ⬜ | ~8h | `docs/prompts/pending/flyway-baseline-prompt.md` (not yet created) 🔴 NEEDED |
| 8 | Observability Baseline | ✅ | — | Done (direct implementation) |
| 9 | Security Hardening | 🔄 | ~4h remaining | Security headers + .env.example done, rate limiting + refresh pending |
| 10 | Placeholder Modules Decision | ✅ | ~1h | Done (etl removed, audit stubbed to 501) |
| 11 | API Documentation | 🔄 | ~4h remaining | springdoc + Swagger UI + JWT auth done, docs pending |

**Total remaining effort**: ~20h
**Waves completed**: 0, 1, 2, 3, 4, 5, 8, 10
**Waves in progress**: 9 (~4h), 11 (~4h)
**Waves pending**: 6 (~6h, needs Docker), 7 (~8h, needs Docker for schema dump)
**Prompts needed before next execution**: Wave 7 (Flyway)

---

## Prompt Creation Backlog

These Claude Code prompts need to be written before their wave can be executed.

| Priority | Prompt | References | Notes |
|----------|--------|------------|-------|
| 1 | `docs/prompts/pending/flyway-baseline-prompt.md` | MariaDB schema, Boot Flyway auto-config | Wave 7 blocker |

> **Resolved (executed without prompt):** Wave 5 CI fixes, Wave 8 Observability, Wave 9 Security hardening (partial)

---

## ADR Backlog

Architectural decisions that need to be recorded before their wave executes.

| ADR | Title | Needed By |
|-----|-------|-----------|
| 0008 | Flyway Migration Strategy | Wave 7 |
| 0009 | Observability Stack Selection | Wave 8 |
| 0010 | Rate Limiting Strategy | Wave 9 |
| 0011 | Secrets Management | Wave 9 |

---

## Beta Release Checklist

All of the following must be true to declare beta readiness:

**Code Quality**
- [ ] `mvn clean verify` green (unit + component tests)
- [ ] Zero `@Deprecated` calls in production source (`-Xlint:deprecation` clean)
- [ ] SonarQube quality gate passing (zero blocker/critical issues)
- [ ] No `import com.fasterxml.jackson` in hand-written source
- [ ] Exception advice consolidated — all modules use `BaseControllerAdvice`

**Schema & Data**
- [ ] Flyway migration history table present in all environments
- [ ] `@SQLDelete` WHERE clauses include both `tenant_id` and entity ID
- [ ] `spring.jpa.hibernate.ddl-auto=none` in all profiles

**Testing**
- [ ] One `*ComponentTest.java` per entity (19 total)
- [ ] E2E suite ~182 requests, Newman passes against local server
- [ ] `@SQLRestriction` soft-delete exclusion verified in component tests
- [ ] `./run-e2e.sh` passes end-to-end against Docker stack

**Operations**
- [ ] `GET /api/actuator/health/liveness` returns 200
- [ ] `GET /api/actuator/health/readiness` returns 200
- [ ] `GET /api/actuator/metrics` returns Prometheus text
- [ ] Structured JSON log output in non-local profiles
- [ ] `X-Correlation-Id` header on all responses
- [ ] `docker compose -f docker-compose.dev.yml up --build` works cleanly

**Security**
- [ ] Auth endpoint rate-limited (429 after threshold)
- [ ] Security headers present (`HSTS`, `X-Content-Type-Options`, `X-Frame-Options`)
- [ ] No plaintext secrets in version-controlled files
- [ ] Token refresh endpoint operational
- [ ] `.env.example` committed

**Documentation**
- [ ] `/api/v3/api-docs` returns merged OpenAPI document
- [ ] `docs/error-codes.md` lists all `$.code` values
- [ ] `docs/consumer-onboarding.md` has working curl walkthrough
- [ ] All ADRs 0001–0011 committed
