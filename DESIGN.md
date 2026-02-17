# Architecture — AkademiaPlus Platform Core API

> **See also**: [CLAUDE.md](CLAUDE.md) for coding standards, review rules, and AI-assisted development guidelines.

## 1. System Overview

AkademiaPlus is a multi-tenant SaaS platform for educational institutions. The `platform-core-api` is its backend monolith, structured as a Maven multi-module project following Clean Architecture principles with strong multi-tenancy guarantees and defense-in-depth security.

```
┌──────────────────────────────────────────────────────────┐
│                      Clients                             │
│          (Web App, Mobile App, Admin Dashboard)          │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS (TLS via CA Service)
                         ▼
┌──────────────────────────────────────────────────────────┐
│               platform-core-api (Spring Boot)            │
│  ┌────────┐ ┌────────┐ ┌─────────┐ ┌─────────────────┐  │
│  │ User   │ │Billing │ │ Course  │ │  Notification   │  │
│  │ Mgmt   │ │        │ │  Mgmt   │ │    System       │  │
│  └───┬────┘ └───┬────┘ └────┬────┘ └────────┬────────┘  │
│      │          │           │               │            │
│  ┌───┴──────────┴───────────┴───────────────┴─────────┐  │
│  │              security module (JWT, filters)        │  │
│  ├────────────────────────────────────────────────────┤  │
│  │              utilities (crypto, hashing, PII)      │  │
│  ├────────────────────────────────────────────────────┤  │
│  │           infra-common (persistence, tenancy)      │  │
│  ├────────────────────────────────────────────────────┤  │
│  │         multi-tenant-data (JPA entities)           │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────┬───────────────────────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         ┌────────┐  ┌─────────┐  ┌─────────┐
         │MariaDB │  │  Redis  │  │MercadoP.│
         │(Multi- │  │ (Cache/ │  │(Payment │
         │ tenant)│  │ Session)│  │ Gateway)│
         └────────┘  └─────────┘  └─────────┘
```

---

## 2. Module Catalog

### 2.1 Foundation Modules (No Domain Logic)

| Module | Responsibility | Key Classes |
|--------|---------------|-------------|
| `infra-common` | Persistence infrastructure — entity base classes, Hibernate event listeners, tenant context, ID assignment | `TenantScoped`, `SoftDeletable`, `Auditable`, `EntityIdAssigner`, `TenantContextHolder` |
| `utilities` | Cross-cutting security services and shared utilities | `AESGCMEncryptionService`, `HashingService`, `PiiNormalizer`, `SequentialIDGenerator` |
| `multi-tenant-data` | All JPA `@Entity` data models — no business logic, no services | `AbstractUser`, `PersonPIIDataModel`, `AdultStudentDataModel`, `EmployeeDataModel`, `CourseDataModel`, `MembershipDataModel`, `TenantDataModel` |
| `security` | Authentication and authorization infrastructure | `JwtTokenProvider`, `JwtRequestFilter`, `SecurityConfig`, `InternalAuthenticationUseCase` |

### 2.2 Domain Modules (Business Logic)

| Module | Domain | Aggregates |
|--------|--------|------------|
| `user-management` | People management | Employee, AdultStudent, MinorStudent, Tutor, Collaborator |
| `billing` | Financial operations | Payment, Membership, Payroll, Store/POS transactions |
| `course-management` | Academic programs | Course, Schedule, CourseEvent |
| `tenant-management` | Platform tenancy | Tenant creation and lifecycle |
| `notification-system` | Communications | Email, SMS, Push (OpenAPI specs defined, implementation pending) |
| `pos-system` | Point-of-sale | Store transactions (placeholder) |

### 2.3 Standalone Services

| Module | Purpose | Runs As |
|--------|---------|---------|
| `certificate-authority` | TLS certificate generation and PKI management | Separate Spring Boot app (port 8081) |
| `mock-data-system` | Generates realistic test data using DataFaker | Separate Spring Boot app |
| `etl-system` | ETL pipelines | Placeholder |
| `audit-system` | Audit event logging | Placeholder |
| `application` | Main entry point — assembles all platform modules | Spring Boot main class |

---

## 3. Existing Patterns

### 3.1 Entity Inheritance Chain

```
Auditable (createdAt, updatedAt — JPA lifecycle callbacks)
    └── SoftDeletable (deletedAt — @SQLRestriction, @SQLDelete, Hibernate filters)
            └── TenantScoped (tenantId — @Id, Hibernate tenant filter)
                    └── AbstractUser (personPII, birthDate, entryDate)
                            ├── EmployeeDataModel
                            ├── AdultStudentDataModel
                            ├── MinorStudentDataModel
                            ├── TutorDataModel
                            └── CollaboratorDataModel
```

Every entity in the system inherits audit timestamps, soft-delete capability, and tenant isolation. The `@Id tenantId` is part of every composite primary key, making cross-tenant data access structurally impossible at the database level.

### 3.2 Module Internal Structure (Clean Architecture)

```
{module}/
├── config/
│   ├── {Module}ModuleSecurityConfiguration.java    ← URL-level access rules
│   ├── {Module}ControllerAdvice.java               ← Exception → HTTP response mapping
│   └── MapperModelConfig.java                      ← ModelMapper bean definitions
├── exception/
│   └── {Entity}NotFoundException.java              ← Domain-specific exceptions
├── {aggregate}/
│   ├── interfaceadapters/
│   │   ├── {Entity}Controller.java                 ← @RestController, delegates to use cases
│   │   └── {Entity}Repository.java                 ← Spring Data JPA repository interface
│   └── usecases/
│       ├── {Entity}CreationUseCase.java             ← @Service @Transactional
│       ├── Get{Entity}ByIdUseCase.java
│       ├── GetAll{Entities}UseCase.java
│       └── Delete{Entity}UseCase.java
```

**Key characteristics:**
- One use case class per operation (Command/Query separation at the class level)
- Controllers are thin — they delegate immediately to use cases
- Repositories are pure Spring Data interfaces — no custom implementations yet
- ModelMapper handles DTO ↔ Entity conversion

### 3.3 PII Protection Pipeline

```
Input (raw PII) → PiiNormalizer → AESGCMEncryptionService → Encrypted storage
                       ↓
                  HashingService → Hash columns (emailHash, phoneHash) for indexed lookup
```

The pattern separates concerns: normalization ensures consistency, encryption protects at rest, and hash columns enable search without decryption. This is implemented in creation use cases (e.g., `AdultStudentCreationUseCase`).

### 3.4 ID Generation Strategy

IDs are NOT database auto-incremented. Instead:

1. `SequentialIDGenerator` maintains per-tenant sequence counters in `tenant_sequence` table
2. `IdAssignationPreInsertEventListener` intercepts Hibernate's `PreInsertEvent`
3. `EntityIdAssigner` orchestrates: resolve metadata → check for pre-set IDs (log security warning) → generate → assign
4. `HibernateStateUpdater` patches the Hibernate persister state array

This design gives the application full control over ID allocation and prevents ID conflicts in multi-tenant scenarios.

### 3.5 OpenAPI-First Contract Design

```
src/main/resources/openapi/{module}-module.yaml
       ↓ (maven build phase: generate-sources)
target/generated-sources/openapi/
├── openapi/akademiaplus/domain/{module}/api/   ← Controller interfaces
└── openapi/akademiaplus/domain/{module}/dto/   ← Request/Response DTOs (*DTO suffix)
```

Controllers implement the generated interfaces. DTOs are generated — never hand-written.

### 3.6 Security Configuration Pattern

Each module registers its own security rules via a `{Module}ModuleSecurityConfiguration` class. The central `SecurityConfig` in the `security` module composes all module-level configurations using a `ModuleSecurityConfigurator` interface.

### 3.7 Test Patterns

- **Structure**: `@Nested` classes grouping by method under test
- **Naming**: `shouldDoX_whenGivenY()` with `@DisplayName`
- **Comments**: `// Given`, `// When`, `// Then` section markers
- **Assertions**: AssertJ fluent API (`assertThat`, `assertThatThrownBy`)
- **Mocking**: Mockito with exact parameter matching — zero `any()` usage
- **Error messages**: Verified against implementation's `public static final` constants
- **Edge cases**: Null, empty, whitespace, boundary values systematically covered

---

## 4. Suggested New Patterns

### 4.1 Domain Events (Event-Driven Decoupling)

**Problem**: Use cases currently call services directly across module boundaries. As the system grows, this creates tight coupling.

**Suggestion**: Introduce Spring's `ApplicationEventPublisher` for cross-module communication.

```java
// In user-management
public class StudentCreatedEvent extends ApplicationEvent {
    private final Long tenantId;
    private final Long studentId;
}

// In billing — listens without direct dependency on user-management
@EventListener
public void onStudentCreated(StudentCreatedEvent event) {
    membershipService.createDefaultMembership(event.getTenantId(), event.getStudentId());
}
```

**Benefits**: Modules become independently deployable. The notification-system can react to domain events instead of being explicitly called.

### 4.2 Specification Pattern for Complex Queries

**Problem**: As search/filter requirements grow, repositories accumulate many `findBy...` methods.

**Suggestion**: Use Spring Data JPA Specifications for composable, type-safe queries.

```java
public class StudentSpecifications {
    public static Specification<AdultStudentDataModel> hasEmailHash(String hash) {
        return (root, query, cb) -> cb.equal(root.get("personPII").get("emailHash"), hash);
    }
    
    public static Specification<AdultStudentDataModel> isActive() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
```

### 4.3 Result Type for Use Case Responses

**Problem**: Use cases either return a value or throw an exception. Callers must try-catch.

**Suggestion**: Introduce a `Result<T>` type for expected failure paths, keeping exceptions for truly exceptional conditions.

```java
public sealed interface Result<T> {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String code, String message) implements Result<T> {}
}
```

### 4.4 Integration Test Base Class

**Problem**: Integration tests with Testcontainers require repetitive boilerplate for MariaDB setup and tenant context initialization.

**Suggestion**: Create `@AbstractIntegrationTest` base class in a shared test module.

```java
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:latest");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) { ... }
}
```

### 4.5 Outbox Pattern for Reliable Event Publishing

**Problem**: When the notification-system becomes active, there's risk of inconsistency between database commits and message publishing.

**Suggestion**: Implement the Transactional Outbox pattern — write events to an `outbox` table in the same transaction, then publish asynchronously.

### 4.6 Circuit Breaker for External Services

**Problem**: The billing module integrates with MercadoPago. Network failures can cascade.

**Suggestion**: Wrap external calls with `spring-retry` (already a dependency) and consider adding Resilience4j for circuit breaker, rate limiting, and bulkhead patterns.

### 4.7 Mapstruct Migration

**Problem**: ModelMapper uses reflection-based mapping which is slow and error-prone at runtime.

**Suggestion**: Gradually migrate to MapStruct for compile-time-verified, zero-reflection DTO mapping. Start with new modules.

---

## 5. Cross-Cutting Concerns

### 5.1 Observability (Not Yet Implemented)

Recommended additions:
- **Structured logging**: MDC with `tenantId`, `requestId`, `userId` in every log line
- **Metrics**: Micrometer + Prometheus for JVM, HTTP, and business metrics
- **Distributed tracing**: OpenTelemetry for request flow across services
- **Health checks**: Spring Boot Actuator (already available via starter)

### 5.2 Data Governance

Current state: PII encrypted at rest, hash columns for search. Recommended additions:
- **Field-level audit logging**: Track who accessed/modified PII fields
- **Data retention policies**: Automated purging of soft-deleted records after retention period
- **GDPR/data export**: Endpoint to export all data for a given person

### 5.3 SonarQube — Mapping to Coding Standards

The project's coding standards overlap with and extend SonarQube's default Java rules. This section documents where they align, where the project is stricter, and where SonarQube rules should be interpreted through the lens of project conventions.

**Constants & String Literals**

| Project Standard | Related SonarQube Rules |
|------------------|------------------------|
| All error messages, regex patterns, and repeated strings extracted to `public static final` constants | `java:S1192` (string literals should not be duplicated) — project is **stricter**: even single-use strings in validation messages must be constants for test-assertion sharing |
| Constants shared between implementation and test code as single source of truth | No direct SonarQube equivalent — enforced by code review |

**Test Conventions**

| Project Standard | Related SonarQube Rules |
|------------------|------------------------|
| Given-When-Then comments (never AAA) | `java:S5960` (assertions in tests) — project adds structural requirements beyond what SonarQube checks |
| `shouldDoX_whenGivenY()` naming | `java:S3577` (test class/method naming) — project convention is more specific than the default rule |
| Zero `any()` matchers — exact value matching only | No SonarQube rule — enforced by code review. Rationale: `any()` masks broken argument contracts |
| `@Nested` + `@DisplayName` structure | No SonarQube rule — structural convention |
| AssertJ over JUnit assertions | `java:S5785` (use AssertJ) — aligned |

**Security Patterns**

| Project Standard | Related SonarQube Rules |
|------------------|------------------------|
| Possessive regex quantifiers for ReDoS prevention | `java:S5852` (ReDoS-vulnerable regex) — aligned, project goes further by requiring possessive quantifiers proactively |
| Constant-time comparisons for hash/token equality | `java:S5547` (strong cipher algorithms) — adjacent; project adds timing-attack resistance beyond SonarQube's scope |
| AES-GCM for field encryption | `java:S5542` (encryption algorithm should be robust) — aligned |
| No hashing algorithms for password storage (documented warning) | `java:S5344` (passwords should not be stored with reversible encryption) — aligned |

**Code Structure**

| Project Standard | Related SonarQube Rules |
|------------------|------------------------|
| One use case per class (CQS at class level) | `java:S1820` (too many methods in class) — naturally satisfied by convention |
| Thin controllers — immediate delegation to use cases | `java:S1200` (classes coupling) — aligned in spirit |
| Input validation on all public entry points | `java:S5145` (log injection), `java:S2083` (path traversal) — project requires broader validation than security-only rules |

**Where SonarQube is insufficient — review-enforced rules:**
- Exact matcher policy in tests (no `any()`, `anyString()`, `anyLong()`)
- Constant extraction for _all_ strings, not just duplicated ones
- Given-When-Then comment structure
- `@Nested` test organization with `@DisplayName`
- `static import` usage for test constants and assertion methods

### 5.4 Performance Considerations

- **N+1 query risk**: AbstractUser → PersonPII is `@OneToOne` with default eager fetch. Monitor with Hibernate statistics.
- **ID generation contention**: `SequentialIDGenerator` uses database sequences per tenant. Under high concurrency, consider batch allocation.
- **Encryption overhead**: AES-GCM per field adds latency. Consider encrypted views or database-level TDE for bulk operations.

---

## 6. Deployment Topology

```
┌─────────────────────────────────────────┐
│             Docker Compose              │
│                                         │
│  platform-core-api ──→ :8443 (HTTPS)   │
│  ca-service        ──→ :8081 (HTTPS)   │
│  multi_tenant_db   ──→ :3306 (MariaDB) │
│  platform-core-redis→ :6379 (Redis)    │
│                                         │
│  Volumes: db_data, redis_data, ca_certs │
└─────────────────────────────────────────┘
```

**CI/CD**: GitHub Actions → SonarQube analysis → Docker build → AWS deployment

---

## 7. Decision Records

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025 | Spring Boot 4.0.0-M3 | Early adoption for Jakarta EE 11, virtual threads readiness |
| 2025 | All IDs `Long` | Scalability — `Integer` overflow at ~2.1B records |
| 2025 | Custom ID generation | Multi-tenant isolation requires tenant-scoped sequences |
| 2025 | AES-GCM for field encryption | Authenticated encryption prevents both reading and tampering |
| 2025 | Possessive regex quantifiers | ReDoS prevention in PII validation |
| 2025 | OpenAPI-first | Contract-driven development, auto-generated DTOs |
| 2025 | Separate data model module | Entities shared across domain modules without circular deps |
| 2025-10-29 | Integer→Long migration | All entity IDs migrated to Long across all data models |
