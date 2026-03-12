# ADR-0006: Integration Test Strategy

**Status**: Accepted
**Date**: 2026-02-18
**Deciders**: ElatusDev

## Context

The project has 95 unit tests (Mockito, no Spring context) covering business logic
in isolation. A Postman collection validates the deployed API contract externally.
Between these two layers there is a gap: no automated tests verify that Spring
wiring, Hibernate mappings, tenant filters, transaction propagation, and FK
constraints work correctly when assembled together against a real database.

Attempted component tests revealed a concrete blocker: Hibernate's
`SpringBeanContainer` instantiates JPA converters (e.g., `StringEncryptor` →
`AESGCMEncryptionService`) during `EntityManagerFactory` initialization, before
Spring `@Value` resolution completes in Boot 4.0.0-M3. A `TestSecurityConfiguration`
with a `@Primary` `AESGCMEncryptionService` bean exists as the workaround.

Infrastructure is partially in place:

- `AbstractIntegrationTest` base class (Testcontainers MariaDB, `@DynamicPropertySource`)
- `TestSecurityConfiguration` (`@Primary` AES-256-GCM bean with hardcoded test key)
- Test resources: `00-schema-dev.sql` init script, `test-keystore.p12`, profile properties
- No Maven Failsafe plugin — all tests currently run in the `test` phase via Surefire
- No naming convention distinguishes unit tests from Spring-context tests

## Decision

### 1. Test Taxonomy

Three test categories, each serving a distinct verification purpose:

| Category | What it proves | Spring context | Database | Runs with |
|----------|---------------|----------------|----------|-----------|
| **Unit** | Business logic in isolation | None | None | `mvn test` (Surefire) |
| **Component** | Vertical slice assembly — wiring, persistence, transactions | Full or slice | Testcontainers MariaDB | `mvn verify` (Failsafe) |
| **E2E** | Full deployed system — HTTP, auth flow, multi-request workflows, contract compliance | External | Running server + DB | Newman CLI / Postman |

The term **component test** is chosen deliberately over "integration test" because
"integration test" is semantically overloaded — it means different things to different
teams and tools. A component test exercises one assembled vertical slice of the
application (controller → use case → repository → DB) within the JVM boundary.

The term **E2E** (end-to-end) replaces "regression test" because "regression" describes
a *purpose* (catching regressions), not a *scope*. Any test category — unit, component,
or E2E — can serve as a regression check. E2E describes what makes these tests
structurally distinct: they exercise the full deployed system from external HTTP
boundary to database and back, with nothing simulated.

### 2. Naming and File Conventions

| Category | Class suffix / location | Example |
|----------|------------------------|---------|
| Unit | `*Test` in `src/test/java` | `MockDataOrchestratorTest` |
| Component | `*ComponentTest` in `src/test/java` | `MockDataOrchestratorComponentTest` |
| E2E | Postman collection in `platform-api-e2e/` | `platform-api-e2e.json` |

All test classes follow the existing GWT standard (ADR-0004). The `*ComponentTest`
suffix is recognized by Maven Failsafe's `<includes>` configuration so that
component tests never run during `mvn test` and unit tests never run during
`mvn verify -DskipTests`.

### 3. Maven Phase Separation

**Surefire** (bound to `test` phase): runs `*Test.java` — fast, no I/O, no Docker.
This is the developer inner-loop gate. Must stay under 30 seconds for the full reactor.

**Failsafe** (bound to `integration-test` / `verify` phase): runs `*ComponentTest.java` —
requires Docker Desktop running for Testcontainers. These are CI-gate tests that run on
every push but not necessarily on every local save.

Configuration lives in the parent POM so all modules inherit the same convention.
Individual modules opt in by having `*ComponentTest.java` classes on the test classpath;
modules with no component tests incur zero overhead.

### 4. Component Test Tiers

Tests are organized into three tiers of increasing scope and cost. Each tier
targets a specific class of bug that the tiers below cannot catch.

#### Tier 1 — Repository Slice Tests

**Scope**: Single repository + Hibernate + real MariaDB.
**Annotation**: `@DataJpaTest` + Testcontainers (via a shared base class or
`@Import` of the container configuration).
**Validates**:

- Schema-JPA alignment (column types, nullability, ENUM case, missing columns)
- Composite key round-trips (`save` → `findById` with `tenantId` + `entityId`)
- Tenant filter behavior (Hibernate `@Filter` applied, cross-tenant isolation)
- `EntityIdAssigner` lifecycle (sequential ID assignment, `@GeneratedValue` skip,
  `@EmbeddedId` skip)
- Soft-delete flag persistence and query exclusion
- Encrypted/hashed field round-trip (write encrypted → read decrypted)

**Why this tier exists**: The most frequent class of runtime failure in this project
is schema-JPA misalignment. Unit tests mock the repository boundary so they cannot
catch these. Repository slice tests are fast (thin Spring slice, no web layer) and
high-value.

**SpringBeanContainer consideration**: Slice tests that load entities with encrypted
fields will trigger the `StringEncryptor` → `AESGCMEncryptionService` chain. The
`TestSecurityConfiguration` `@Primary` bean must be `@Import`ed into the slice
context, or a dedicated `@TestConfiguration` registered via `@AutoConfigureTestDatabase`
replacement.

#### Tier 2 — Use Case Component Tests

**Scope**: Use case + repository + transaction manager + real MariaDB.
**Annotation**: `@SpringBootTest` (trimmed context via `classes` attribute or
`@ContextConfiguration`) + Testcontainers.
**Validates**:

- Transaction propagation correctness (`REQUIRES_NEW` in `SequentialIDGenerator`
  actually commits independently; `REQUIRED` in `DataLoader.load()` rolls back
  atomically on failure)
- Creation pipeline end-to-end: DTO → `transform()` → persist → response DTO
  (ModelMapper configuration, PII normalization, hashing, and encryption with
  real data flowing through)
- `ObjectProvider` scope-mismatch resolution (singleton listener accessing
  request-scoped `TenantContextHolder`)
- Prototype-scoped entity bean lifecycle (`ApplicationContext.getBean()` returns
  distinct instances per call)

**Why this tier exists**: Unit tests verify that `@Transactional(propagation =
REQUIRES_NEW)` is annotated, but cannot verify it actually opens an independent
transaction. These tests catch propagation bugs, scope-mismatch wiring failures,
and ModelMapper mis-mappings that only manifest with real bean assembly.

#### Tier 3 — Orchestration Component Tests

**Scope**: Full application context + real MariaDB.
**Annotation**: `@SpringBootTest(classes = MockDataApp.class)` + Testcontainers
(extends `AbstractIntegrationTest`).
**Validates**:

- `MockDataOrchestrator.generateAll(n)` produces correct row counts across all
  entity tables
- FK integrity: every minor student references a real tutor ID, every employee
  has valid tenant/auth/PII FKs
- Cleanup leaves zero rows and resets auto-increment counters
- Post-load hooks fire correctly (tutor IDs injected before minor student load)
- DAG ordering does not violate FK constraints under real database enforcement

**Why this tier exists**: Unit tests verify ordering with mock `InOrder` assertions
but never test against real FK constraints. This tier is the most expensive but is
the final proof that the orchestration pipeline is correct.

### 5. Shared Test Infrastructure

All component tests in the `mock-data-service` module extend `AbstractIntegrationTest`
(or import its container configuration). The base class owns:

- Testcontainers MariaDB lifecycle (singleton container strategy — one container per
  test class, reused across methods via `@Container` static field)
- `@DynamicPropertySource` for datasource, JWT keystore path
- `@Import(TestSecurityConfiguration.class)` for the `@Primary` AES bean

Tier 1 slice tests that use `@DataJpaTest` instead of `@SpringBootTest` need a
lighter-weight base or composition approach (`@Import` of a shared container
`@TestConfiguration`) since `@DataJpaTest` does not load the full context.

Future consideration: if component test count grows beyond ~20, migrate to the
Testcontainers **singleton container pattern** (shared static container across all
test classes in the module) to avoid container startup per class.

### 6. Relationship Between Component Tests and E2E Tests

Component tests and E2E tests are complementary, not overlapping. Component tests
catch wiring and persistence bugs early in the build. E2E tests validate the
deployed artifact behaves correctly as a black box.

E2E tests cover concerns that component tests cannot:

- Actual HTTP response codes from a running server (not MockMvc's simulated responses)
- Full authentication flow with real JWT token issuance and validation
- Multi-request workflows (create tenant → create employee → verify tenant filter
  isolation via GET)
- Response body shape against the OpenAPI contract
- Environment-specific issues (Docker networking, actual MariaDB configuration)

As the system grows beyond a single deployable (billing, notification, ETL modules),
E2E becomes the layer that validates cross-service flows — a boundary component tests
cannot cross.

| Concern | Component test | E2E test |
|---------|---------------|----------|
| Schema-JPA alignment | ✓ | — |
| Transaction propagation | ✓ | — |
| FK constraint enforcement | ✓ | — |
| Bean wiring and scope | ✓ | — |
| HTTP status codes | simulated (MockMvc) | ✓ (real server) |
| JWT auth flow | — | ✓ |
| Multi-request workflows | possible but verbose | ✓ (natural fit) |
| OpenAPI contract compliance | — | ✓ |
| Environment parity | partial (Testcontainers) | ✓ (real infra) |
| Cross-service flows | — | ✓ (future) |

### 7. Rollout Order

1. **Configure Maven Failsafe** in parent POM with `*ComponentTest.java` include
   pattern. Verify `mvn test` still runs only unit tests and `mvn verify` picks up
   both.
2. **Resolve SpringBeanContainer blocker** — verify `TestSecurityConfiguration`
   `@Primary` bean works with both `@SpringBootTest` and `@DataJpaTest` contexts.
3. **Tier 3 first** — write `MockDataOrchestratorComponentTest` extending
   `AbstractIntegrationTest`. This is the test that was previously attempted and
   removed. It validates the highest-risk area (orchestration + FK integrity) and
   exercises the full test infrastructure stack, flushing out any remaining
   configuration issues.
4. **Tier 1 repository slices** — one test class per repository, starting with
   entities that have the most complex mappings (encrypted fields, composite keys,
   nested auth/PII). These provide the broadest coverage per test.
5. **Tier 2 use case tests** — add as specific propagation or wiring bugs are
   discovered. These are the most expensive to write relative to their coverage
   and should be targeted rather than exhaustive.

## Alternatives Considered

1. **Single "integration test" category for all non-unit tests** — The most common
   approach. Rejected because it conflates two fundamentally different test types
   (in-JVM Spring tests vs. external HTTP tests) under one ambiguous name. This
   leads to confusion about what infrastructure is required, when tests should run,
   and what failures mean. The three-category taxonomy makes each test's purpose,
   cost, and infrastructure requirements explicit.

2. **`*IT` suffix for Failsafe** (Maven convention default) — Rejected because `IT`
   reinforces the "integration test" naming ambiguity this ADR aims to eliminate.
   `*ComponentTest` is self-documenting: it tests an assembled component, not an
   integration between systems.

3. **Separate Maven modules for component tests** (e.g., `mock-data-service-tests`) —
   Used by some projects to isolate slow tests. Rejected because the project already
   has 15 modules and the additional module overhead is not justified by the current
   test count. If component tests exceed ~50 classes, revisit this decision.

4. **Testcontainers with `@DataJpaTest` only (no `@SpringBootTest` tests)** —
   Tier 1 slice tests are cheaper and catch the highest-frequency bugs. Rejected as
   a complete strategy because slice tests cannot verify transaction propagation,
   scope-mismatch resolution, or full orchestration pipeline integrity. Tiers 2 and 3
   are needed for these specific concerns.

5. **Replace Postman E2E with in-JVM HTTP tests (`@SpringBootTest(webEnvironment =
   RANDOM_PORT)` + `TestRestTemplate`)** — Rejected because the Postman collection
   already exists, covers the deployed contract well, and tests environment-specific
   concerns that in-JVM tests cannot. The two approaches are complementary.

## Consequences

### Positive
- Clear vocabulary: "unit", "component", "E2E" each mean exactly one thing
- Maven phase separation prevents component tests from slowing the developer inner loop
- Tier structure prioritizes high-value tests (repository slices) over expensive ones
  (full orchestration)
- Existing `AbstractIntegrationTest` and `TestSecurityConfiguration` are reused directly
- E2E tests retain their role without duplication

### Negative
- Component tests require Docker Desktop running (Testcontainers dependency)
- Failsafe plugin adds build complexity and a second test execution phase
- Developers must remember `mvn verify` (not just `mvn test`) to run the full suite
- `@DataJpaTest` slice tests need their own container configuration approach separate
  from `AbstractIntegrationTest`'s `@SpringBootTest` base

### Neutral
- `AbstractIntegrationTest` base class may need refactoring to extract the container
  configuration into a reusable `@TestConfiguration` that both `@SpringBootTest` and
  `@DataJpaTest` tests can `@Import`
- AI-CODE-REF.md section 4 needs updating with the `*ComponentTest` naming convention
  and tier descriptions once this ADR is accepted
- The `*ComponentTest` suffix is a project convention, not a Maven or Spring convention;
  it must be documented in onboarding materials
- The `platform-api-regression` repository is renamed to `platform-api-e2e` to align
  with the taxonomy defined in this ADR
