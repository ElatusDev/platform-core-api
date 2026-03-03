# Architecture — AkademiaPlus Platform Core API

> **See also**: [AI-CODE-REF.md](../directives/AI-CODE-REF.md) for coding standards, review rules, and AI-assisted development guidelines.

## 1. System Overview

AkademiaPlus is a multi-tenant SaaS platform for educational institutions. The `platform-core-api` is its backend monolith, structured as a Maven multi-module project following Clean Architecture principles with strong multi-tenancy guarantees and defense-in-depth security.

```
┌──────────────────────────────────────────────────────────┐
│                      Clients                             │
│          (Web App, Mobile App, Admin Dashboard)          │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS (TLS terminated at edge)
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
| `billing` | Financial operations | Membership, MembershipAdultStudent, MembershipTutor, PaymentAdultStudent, PaymentTutor, Compensation |
| `course-management` | Academic programs | Course, Schedule (program aggregate); CourseEvent (event aggregate) |
| `tenant-management` | Platform tenancy | Tenant, TenantSubscription, TenantBillingCycle |
| `notification-system` | Communications | Notification |
| `pos-system` | Point-of-sale | StoreProduct, StoreTransaction |

### 2.3 Standalone Services

| Module | Purpose | Runs As |
|--------|---------|---------|
| `certificate-authority` | Trust broker with dual role: (1) JWKS registry for JWT public key distribution, (2) internal PKI for certificate signing/enrollment. Docker service name: `trust-broker`. | Separate Spring Boot app (port 8082, internal only) |
| `mock-data-system` | Generates realistic test data using DataFaker | Separate Spring Boot app (port 8180) |
| `audit-system` | Audit event logging | Stub (501 Not Implemented — placeholder controller) |
| `application` | Main entry point — assembles all platform modules | Spring Boot main class (port 8080) |

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
│   ├── {Module}ModuleSecurityConfiguration.java    ← URL-level access rules (opt-in per module)
│   ├── {Module}ControllerAdvice.java               ← Extends BaseControllerAdvice (module-scoped)
│   └── MapperModelConfig.java                      ← ModelMapper bean definitions
├── {aggregate}/
│   ├── interfaceadapters/
│   │   ├── {Entity}Controller.java                 ← @RestController, delegates to use cases
│   │   └── {Entity}Repository.java                 ← Spring Data JPA repository interface
│   └── usecases/
│       ├── {Entity}CreationUseCase.java             ← @Service @Transactional
│       ├── Get{Entity}ByIdUseCase.java
│       ├── GetAll{Entities}UseCase.java
│       └── Delete{Entity}UseCase.java               ← Uses DeleteUseCaseSupport (composition)
```

**Key characteristics:**
- One use case class per operation (Command/Query separation at the class level)
- Controllers are thin — they delegate immediately to use cases
- Repositories are pure Spring Data interfaces — no custom implementations yet
- ModelMapper handles DTO ↔ Entity conversion
- Exception handling consolidated in `utilities` — `BaseControllerAdvice` provides generic handlers for `EntityNotFoundException`, `EntityDeletionNotAllowedException`, `DuplicateEntityException`, `InvalidTenantException`, `DataIntegrityViolationException`, `MethodArgumentNotValidException`, `EncryptionFailureException`/`DecryptionFailureException`, and a generic `Exception` fallback. Per-module ControllerAdvice classes extend it and add module-specific exception handlers only when needed (e.g., `ScheduleNotAvailableException` in course-management).

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

Modules that need custom URL-level access rules register a `{Module}ModuleSecurityConfiguration` class implementing the `ModuleSecurityConfigurator` interface. The central `SecurityConfig` in the `security` module composes all registered configurators.

**Modules with security configurations:**
- `user-management` → `PeopleModuleSecurityConfiguration`
- `billing` → `TreasuryModuleSecurityConfiguration`
- `course-management` → `CoordinationModuleSecurityConfiguration`
- `mock-data-system` → `MockDataServiceModuleSecurityConfiguration`

Modules without explicit security configurations (`notification-system`, `pos-system`, `tenant-management`) inherit the default deny-all policy from `SecurityConfig` and rely on the global authenticated-user requirement.

### 3.7 Test Patterns

- **Structure**: `@Nested` classes grouping by method under test
- **Naming**: `shouldDoX_whenGivenY()` with `@DisplayName`
- **Comments**: `// Given`, `// When`, `// Then` section markers
- **Assertions**: AssertJ fluent API (`assertThat`, `assertThatThrownBy`)
- **Mocking**: Mockito with exact parameter matching — zero `any()` usage
- **Error messages**: Verified against implementation's `public static final` constants
- **Edge cases**: Null, empty, whitespace, boundary values systematically covered

### 3.8 Prototype-Scoped Entity Pattern

All JPA data models are Spring-managed prototype beans:

```java
@Entity
@Component
@Scope("prototype")
@Table(name = "employees")
public class EmployeeDataModel extends AbstractUser { ... }
```

**Consequence**: Use cases must obtain fresh instances via `ApplicationContext`:

```java
EmployeeDataModel model = applicationContext.getBean(EmployeeDataModel.class);
```

Never use `new EmployeeDataModel()` — the prototype scope ensures Hibernate proxying,
lifecycle callbacks, and tenant filter integration work correctly.

### 3.9 Creation Use Case Pattern

All entity creation follows a two-method pattern separating transformation from persistence:

```java
@Service
@RequiredArgsConstructor
public class EmployeeCreationUseCase {
    private final ApplicationContext applicationContext;
    private final ModelMapper modelMapper;
    private final EmployeeRepository repository;

    @Transactional
    public EmployeeCreationResponseDTO create(RequestDTO dto) {
        return modelMapper.map(repository.save(transform(dto)), ResponseDTO.class);
    }

    public EmployeeDataModel transform(RequestDTO dto) {
        // 1. Get prototype beans from ApplicationContext
        // 2. ModelMapper maps DTO fields onto the beans
        // 3. Wire child entities (PII, auth) onto parent
        // 4. Normalize and hash PII fields
        return model;
    }
}
```

**Key design decisions:**
- `transform()` is `public` (not private) — the mock-data-system calls it directly via method reference as the `DataLoader` transformer function
- `create()` owns the `@Transactional` boundary — `transform()` is deliberately non-transactional
- Return type of `create()` is the OpenAPI response DTO, not the data model
- Tenant entity is simpler (no PII, no auth, no sequential ID):

```java
// Tenant: simple transform — just ModelMapper, no nested entities
public TenantDataModel transform(TenantCreateRequestDTO dto) {
    TenantDataModel model = applicationContext.getBean(TenantDataModel.class);
    modelMapper.map(dto, model);
    return model;
}

// People: complex transform — nested entities + PII hashing
public EmployeeDataModel transform(EmployeeCreationRequestDTO dto) {
    InternalAuthDataModel auth = applicationContext.getBean(InternalAuthDataModel.class);
    modelMapper.map(dto, auth);
    PersonPIIDataModel pii = applicationContext.getBean(PersonPIIDataModel.class);
    modelMapper.map(dto, pii);
    EmployeeDataModel model = applicationContext.getBean(EmployeeDataModel.class);
    modelMapper.map(dto, model, MAP_NAME);
    model.setPersonPII(pii);
    model.setInternalAuth(auth);
    // ... normalize + hash PII
    return model;
}
```

### 3.10 Composite Key Strategy

Two distinct ID strategies exist:

**Tenant-scoped entities** use `@IdClass` with composite key `(tenantId, entityId)`:
```java
@IdClass(EmployeeDataModel.EmployeeCompositeId.class)
public class EmployeeDataModel extends AbstractUser {
    @Id @Column(name = "employee_id")
    private Long employeeId;  // assigned by SequentialIDGenerator

    // tenantId @Id inherited from TenantScoped

    public static class EmployeeCompositeId implements Serializable {
        private Long tenantId;
        private Long employeeId;
    }
}
```

**Non-tenant-scoped entities** use DB auto-increment:
```java
public class TenantDataModel extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_id")
    private Long tenantId;  // DB generates via AUTO_INCREMENT
}
```

**`EntityIdAssigner` behavior:**
- `@GeneratedValue` → silently skipped (DB handles ID)
- `@EmbeddedId` → silently skipped (e.g., `TenantSequence`)
- All others → `SequentialIDGenerator` assigns per-tenant sequential ID

### 3.11 ModelMapper Configuration

Centralized in `utilities` module (`ModelMapperConfig.java`). All modules get the bean transitively.

**Registered converters:**

| Source | Target | Use Case |
|--------|--------|----------|
| `LocalDate` | `java.sql.Date` | JPA date columns |
| `java.sql.Date` | `LocalDate` | Reading dates back |
| `URI` | `String` | OpenAPI `format: uri` → JPA VARCHAR (e.g., `websiteUrl`) |
| `String` | `URI` | JPA VARCHAR → OpenAPI URI |
| `LocalDateTime` | `OffsetDateTime` | JPA audit fields → OpenAPI `format: date-time` |

**Two mapping modes:**
- `modelMapper.map(source, TargetClass.class)` — returns new instance (used in `create()` for response DTOs)
- `modelMapper.map(source, existingTarget)` — maps onto existing instance, returns `void` (used in `transform()` for prototype beans)

**Mockito implication:** When stubbing `ModelMapper` in tests, the void overload requires `doNothing().when(modelMapper).map(dto, model)` instead of `when().thenReturn()`. Strict stubbing will throw `PotentialStubbingProblem` if both overloads aren't stubbed explicitly.

### 3.12 Mock Data Pipeline

Four-layer architecture for generating test data:

```
DataGenerator → DataFactory → DataLoader → AbstractMockDataUseCase
```

**Layer responsibilities:**

| Layer | Scope | Pattern |
|-------|-------|---------|
| `DataGenerator` | Generates individual field values using DataFaker | `@Component`, stateless, one per entity domain |
| `DataFactory` | Assembles complete OpenAPI request DTOs | `@Component`, implements `DataFactory<D>`, may hold state (e.g., `availableTutorIds`) |
| `DataLoader` | Transforms DTOs → DataModels → persists | `@Component @Scope("prototype")`, configured via `@Bean` in `*DataLoaderConfiguration` |
| `AbstractMockDataUseCase` | Exposes `load(count)` and `clean()` | `@Service`, one per entity type, thin wrapper over DataLoader + DataCleanUp |

**Configuration pattern** — `DataLoader` beans are wired in `@Configuration` classes:
```java
@Bean
public DataLoader<EmployeeCreationRequestDTO, EmployeeDataModel, Long> employeeDataLoader(
        EmployeeRepository repository,
        DataFactory<EmployeeCreationRequestDTO> employeeFactory,
        EmployeeCreationUseCase employeeCreationUseCase) {
    return new DataLoader<>(repository, employeeCreationUseCase::transform, employeeFactory);
}
```

The transformer function is always `creationUseCase::transform` — this reuses the production transform logic for mock data generation.

**Inter-entity wiring** — when one entity depends on another's persisted IDs:
```java
// After tutors are loaded, collect their IDs and inject into minor student factory
List<Long> tutorIds = tutorRepository.findAll().stream()
        .map(TutorDataModel::getTutorId).toList();
minorStudentFactory.setAvailableTutorIds(tutorIds);
// Now minor students can be loaded with valid FK references
```

**Transaction boundaries:**
- `DataLoader.load()` — `@Transactional` (jakarta, defaults to `REQUIRED`)
- `LoadTenantMockDataUseCase.load()` — overrides with `@Transactional(propagation = REQUIRES_NEW)` because `SequentialIDGenerator` runs in independent transactions that need committed tenant rows
- `DataCleanUp.clean()` — `@Transactional` (jakarta) — `deleteAllInBatch()` + `ALTER TABLE AUTO_INCREMENT = 1`

### 3.13 Transaction Propagation Patterns

**Pattern 1: `REQUIRES_NEW` for committed visibility**
```java
// SequentialIDGenerator: independent transaction for ID allocation
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Long generateId(Long tenantId, String entityName) { ... }

// LoadTenantMockDataUseCase: tenant rows must be committed
// before SequentialIDGenerator's independent tx can see them
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void load(int count) { super.load(count); }
```

**Pattern 2: `@Lazy` for circular dependency resolution**
```java
// SequentialIDGenerator depends on TenantSequenceRepository
// TenantSequenceRepository triggers EntityIdAssigner (Hibernate listener)
// EntityIdAssigner depends on SequentialIDGenerator → circular
public SequentialIDGenerator(@Lazy TenantSequenceRepository repository) { ... }
```

**Pattern 3: `ObjectProvider` for scope mismatch**
```java
// Hibernate event listeners are singletons but need request-scoped TenantContextHolder
private final ObjectProvider<TenantContextHolder> tenantContextHolderProvider;

// Safe access inside event handler
TenantContextHolder holder = tenantContextHolderProvider.getObject();
```

### 3.14 Module Dependency Graph

```
                         ┌─────────────────┐
                         │   application   │ (assembles all modules)
                         └────────┬────────┘
                                  │
     ┌─────────────┬──────────────┼──────────────┬──────────────────┐
     │             │              │              │                  │
┌────┴─────┐ ┌────┴────┐ ┌──────┴──────┐ ┌────┴──────┐ ┌─────────┴────────┐
│ billing  │ │ course- │ │notification │ │  tenant-  │ │   mock-data-     │
│          │ │ mgmt    │ │  -system    │ │management │ │     system       │
└──┬───┬───┘ └──┬──────┘ └────────────┘ └───────────┘ └──────────────────┘
   │   │        │
   │   └────────┤ (cross-domain: billing → user-mgmt, course-mgmt)
   │            │ (cross-domain: course-mgmt → user-mgmt)
   │            │ (cross-domain: pos-system → user-mgmt)
   └────────────┤
                │
        ┌───────┴──────┐
        │user-management│
        └───────┬──────┘
                │
   ┌────────────┼────────────────────────────────┐
   │            │                                │
   ├── security            (JWT, filters)        │  depends on: multi-tenant-data, utilities
   ├── multi-tenant-data   (JPA @Entity models)  │  depends on: utilities, infra-common
   ├── infra-common        (persistence infra)   │  depends on: utilities
   └── utilities           (crypto, hashing, PII, ModelMapper, ID generation) │  ZERO internal deps
```

**Dependency rules:**
- All domain modules depend on `security`, `multi-tenant-data`, `infra-common`, and `utilities`
- **Cross-domain exceptions**: `billing` depends on `user-management` and `course-management`; `course-management` depends on `user-management`; `pos-system` depends on `user-management` — these are for FK-related operations
- `mock-data-system` depends on all domain modules for `CreationUseCase::transform` references
- `notification-system` is the leanest domain module — depends only on `multi-tenant-data` and `infra-common`
- `utilities` has ZERO internal project dependencies — it is the leaf of the dependency tree
- `infra-common` depends on `utilities` (for `SequentialIDGenerator` used by `EntityIdAssigner`)
- `multi-tenant-data` depends on `utilities` and `infra-common` (for entity base classes and encryption converters)

### 3.15 File Header Convention

All Java source files include the proprietary copyright header:
```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
```

### 3.16 OpenAPI DTO Naming Convention

Generated DTOs follow the pattern:
- `{Entity}CreationRequestDTO` — POST request body
- `{Entity}CreationResponseDTO` — POST 201 response (people entities)
- `{Entity}DTO` — general response (non-people entities like Tenant)
- `{Entity}CreateRequestDTO` — alternative naming (tenant module uses "Create" not "Creation")

Generated package: `openapi.akademiaplus.domain.{module}.dto`
Generated API interfaces: `openapi.akademiaplus.domain.{module}.api`

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

### 5.1 Observability

**Implemented:**
- **Structured logging**: Log4j2 with plain `PatternLayout` for all profiles; `CorrelationIdFilter` propagates `X-Correlation-Id` via MDC
- **Metrics**: Micrometer Prometheus registry exposed at `/actuator/prometheus`
- **Health checks**: Spring Boot Actuator with liveness/readiness probes (prod), extended endpoints (dev: metrics, mappings)

**Not yet implemented:**
- **ECS JSON log format**: Planned for prod/QA profiles (currently plain text across all profiles)
- **Tenant-tagged metrics**: `ObservationFilter` to tag metrics with `tenantId`
- **Distributed tracing**: OpenTelemetry for request flow across services
- **Business metrics**: Custom counters for domain events (enrollments, payments)

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

### 6.1 Docker Compose Stack (`docker-compose.dev.yml`)

All services communicate over plain HTTP on the isolated `akademia-internal`
Docker network (`internal: true` — no external traffic). TLS is NOT configured
at the application layer; it is an infrastructure concern handled by the
orchestrator (reverse proxy for edge in Docker, cert-manager/Istio in K8s).

```
┌──────────────────────────────────────────────────────────────────┐
│              Docker Compose (dev profile)                        │
│                                                                  │
│  trust-broker       ──→ :8082 (JWKS registry, internal only)    │
│  multi_tenant_db    ──→ :3306/:3307 (MariaDB, host-mapped)      │
│  platform-core-redis──→ :6379 (Redis 7, host-mapped)            │
│  platform-core-api  ──→ :8080 (Spring Boot, HTTP)               │
│  mock-data-system   ──→ :8180 (data seeder, HTTP)               │
│  e2e-runner         ──→ Newman (profile: e2e, runs then exits)  │
│                                                                  │
│  Networks: akademia-internal (bridge, internal)                  │
│            akademia-external (bridge)                            │
│  Volumes:  db_data, redis_data, trust_broker_data                │
└──────────────────────────────────────────────────────────────────┘
```

**Startup order** (enforced by `depends_on` + healthchecks):

```
trust-broker + multi_tenant_db + platform-core-redis
  └─► platform-core-api    (waits for all three healthy)
        └─► mock-data-system  (waits for API healthy, seeds DB)
              └─► e2e-runner   (profile-gated, waits for seeder healthy)
```

### 6.2 E2E Test Runner

The `e2e-runner` service is gated behind the `e2e` Docker Compose profile.
It mounts the Postman collection from the sibling `platform-api-e2e/` repo
and runs Newman against the Docker-internal network.

```bash
# Full stack + E2E:
docker compose -f docker-compose.dev.yml --profile e2e up --build \
  --abort-on-container-exit --exit-code-from e2e-runner

# E2E only (stack already running):
docker compose -f docker-compose.dev.yml --profile e2e run --rm e2e-runner

# Convenience script (from AkademiaPlus/ root):
./run-e2e.sh              # cold start
./run-e2e.sh --keep-data  # warm start
./run-e2e.sh --tests-only # stack already up
```

### 6.3 CI/CD

GitHub Actions → SonarQube analysis → Docker build → AWS deployment

---

## 7. Decision Records

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025 | Spring Boot 4.0.0-M3 (initial) | Early adoption for Jakarta EE 11, virtual threads readiness; upgraded to 4.0.3 GA in 2026-02 |
| 2025 | All IDs `Long` | Scalability — `Integer` overflow at ~2.1B records |
| 2025 | Custom ID generation | Multi-tenant isolation requires tenant-scoped sequences |
| 2025 | AES-GCM for field encryption | Authenticated encryption prevents both reading and tampering |
| 2025 | Possessive regex quantifiers | ReDoS prevention in PII validation |
| 2025 | OpenAPI-first | Contract-driven development, auto-generated DTOs |
| 2025 | Separate data model module | Entities shared across domain modules without circular deps |
| 2025-10-29 | Integer→Long migration | All entity IDs migrated to Long across all data models |
| 2026-02 | ModelMapper centralized | Moved from user-management to utilities module for cross-module access |
| 2026-02 | Mock data DAG orchestration | Enum-based dependency graph with topological sort for FK-safe load/cleanup ordering |
| 2026-02 | EntityIdAssigner skip logic | `@EmbeddedId` and `@GeneratedValue` entities silently skipped by ID assigner |
| 2026-02 | Spring Boot 4.0.3 + Java 24 | Upgraded from 4.0.0-M3/Java 21; Jackson 3, Hibernate 7.2.5 |
| 2026-02 | ECS structured logging | JSON format for prod/QA; human-readable for dev/local |
| 2026-02 | Correlation ID propagation | `X-Correlation-Id` header echoed and stored in MDC for tracing |
| 2026-02 | etl-system removed | Module removed from build — no current ETL requirements |
| 2026-02 | BaseControllerAdvice consolidation | Generic exception handlers extracted to `utilities`; per-module ControllerAdvice extends base |
| 2026-02 | DeleteUseCaseSupport composition | Shared find-or-throw → try-delete → catch-constraint pattern composed into all 20+ delete use cases |
| 2026-02 | Documentation reorganization | Canonical `docs/` structure: directives/, design/, prompts/, workflows/ with naming conventions |
