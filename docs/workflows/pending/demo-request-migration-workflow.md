# Demo Request Feature Migration Workflow — platform-core-api

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Source**: `/Volumes/ElatusDev/ElatusDev/ElatusDevApp/ElatusDevPlatformApi` (feature: demo requests)
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.

---

## 1. Migration Context

### What is being migrated

The `DemoRequest` feature from `ElatusDevPlatformApi` — a public-facing endpoint that captures
product demo requests from potential customers visiting the AkademiaPlus website.

**Source implementation** (ElatusDevPlatformApi):
- Entity: `DemoRequest` (UUID id, firstName, lastName, email, companyName, message, status, timestamps)
- Repository: `DemoRequestRepository` (findByEmail, existsByEmail)
- Service: `DemoRequestService` (create, getById, getAll)
- Controller: `DemoRequestController` (POST public, GET/{id} auth, GET auth)
- DTOs: `CreateDemoRequestRequest`, `DemoRequestResponse` (Java records with validation)
- Mapper: `DemoRequestMapper` (manual @Component)
- Exception handling: `DuplicateEntityException` (409), `EntityNotFoundException` (404)
- Tests: Unit (service), Controller (MockMvc), Integration (Testcontainers)

### Adaptation decisions

| Source (ElatusDevPlatformApi) | Target (platform-core-api) | Rationale |
|-------------------------------|----------------------------|-----------|
| UUID id | `Long` auto-increment id | Hard rule #3: all IDs are `Long` |
| Java record DTOs | OpenAPI-generated DTOs | Contract-first design (OpenAPI YAML → code generation) |
| Single layered service | Separate use cases per operation | Use-case-centric architecture (Hard rule #5) |
| Manual `@Component` mapper | ModelMapper with named TypeMap | Canonical pattern (see creation-usecase-workflow.md) |
| `JpaRepository<DemoRequest, UUID>` | `JpaRepository<DemoRequestDataModel, Long>` | Platform-level entity — NOT tenant-scoped (no composite key) |
| PostgreSQL | MariaDB | Existing stack — no DB migration |
| Gradle single-module | Maven module: `lead-management` | New module in reactor |
| `@NotBlank` / `@Email` on records | OpenAPI `required` + `format: email` | Validation via generated DTOs |
| `status` as String | `status` as String with constants | Follows constant extraction pattern |

### Why a new module (`lead-management`)

Demo requests are pre-tenant, public-facing lead-capture. They don't belong in any existing
domain module:
- Not `user-management` (no tenant context, not a managed user)
- Not `tenant-management` (prospect, not yet a tenant)
- Not `security` (not authentication-related)

A dedicated `lead-management` module:
- Follows single-responsibility per module
- Can grow to include contact forms, newsletter signups, etc.
- Keeps the public API boundary separate from tenant-scoped APIs

### Entity classification

`DemoRequestDataModel` is a **platform-level entity** (like `TenantDataModel`):
- Extends `SoftDeletable` (soft delete + audit timestamps) — NOT `TenantScoped`
- Single `Long` PK with `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- No composite key, no tenant filter, no `EntityIdAssigner`
- Uses `@SQLDelete` with single-column WHERE clause

---

## 2. Architecture Overview

### Data Flow

```
Client (website form)
    │ POST /v1/lead-management/demo-requests (permitAll)
    ▼
DemoRequestController (implements DemoRequestsApi)
    │ delegates to
    ▼
DemoRequestCreationUseCase (@Service, @Transactional)
    │ validates email uniqueness via repository
    │ transforms DTO → DataModel via ModelMapper (named TypeMap)
    │ persists via repository
    │ maps saved entity → response DTO
    ▼
DemoRequestRepository (JpaRepository<DemoRequestDataModel, Long>)
    │
    ▼
MariaDB: demo_requests table (platform-level, no tenant_id)
```

### Module Placement in Dependency Graph

```
utilities → infra-common → multi-tenant-data → security
                                                   ↑
                                          lead-management  ← NEW (depends on: multi-tenant-data, utilities)
                                                   ↑
                                              application
```

`lead-management` depends on:
- `multi-tenant-data` — for the `DemoRequestDataModel` entity
- `utilities` — for shared exception types (if any)

It does NOT depend on `security` for JWT auth because the POST endpoint is `permitAll`.
The GET endpoints require auth, which is handled by the global Spring Security filter chain
configured in the `security` module — no explicit dependency needed.

---

## 3. File Inventory

### New Files

| # | File | Package / Location | Responsibility | Phase |
|---|------|--------------------|----------------|-------|
| 1 | `lead-management/pom.xml` | Module root | Maven module definition | 1 |
| 2 | `demo_requests` DDL | `db_init/00-schema-qa.sql` | Table definition | 1 |
| 3 | `DemoRequestDataModel.java` | `multi-tenant-data/.../leadmanagement/` | JPA entity | 2 |
| 4 | `demo-request.yaml` | `lead-management/.../openapi/` | OpenAPI spec | 3 |
| 5 | `lead-management-module.yaml` | `lead-management/.../openapi/` | Module aggregator spec | 3 |
| 6 | `DemoRequestRepository.java` | `lead-management/.../interfaceadapters/` | Spring Data JPA | 4 |
| 7 | `DemoRequestCreationUseCase.java` | `lead-management/.../usecases/` | Create logic | 4 |
| 8 | `GetDemoRequestByIdUseCase.java` | `lead-management/.../usecases/` | Get by ID logic | 4 |
| 9 | `GetAllDemoRequestsUseCase.java` | `lead-management/.../usecases/` | List all logic | 4 |
| 10 | `DeleteDemoRequestUseCase.java` | `lead-management/.../usecases/` | Soft delete logic | 4 |
| 11 | `LeadManagementModelMapperConfiguration.java` | `lead-management/.../config/` | TypeMap registration | 4 |
| 12 | `DemoRequestController.java` | `lead-management/.../interfaceadapters/` | REST controller | 4 |
| 13 | `LeadManagementSecurityConfiguration.java` | `lead-management/.../config/` | permitAll for POST | 4 |
| 14 | `LeadManagementControllerAdvice.java` | `lead-management/.../config/` | Exception → HTTP mapping | 4 |
| 15 | `DemoRequestCreationUseCaseTest.java` | `lead-management/src/test/` | Unit test: create | 5 |
| 16 | `GetDemoRequestByIdUseCaseTest.java` | `lead-management/src/test/` | Unit test: get by id | 5 |
| 17 | `GetAllDemoRequestsUseCaseTest.java` | `lead-management/src/test/` | Unit test: get all | 5 |
| 18 | `DeleteDemoRequestUseCaseTest.java` | `lead-management/src/test/` | Unit test: delete | 5 |
| 19 | `DemoRequestControllerTest.java` | `lead-management/src/test/` | Unit test: controller | 5 |

### Modified Files

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `pom.xml` (root) | Add `<module>lead-management</module>`, add to `platform-core-api` profile | 1 |
| 2 | `db_init/00-schema-qa.sql` | Add `demo_requests` table DDL | 1 |
| 3 | `application/pom.xml` | Add `lead-management` dependency | 4 |

---

## 4. Implementation Sequence

```
Phase 1: Module Scaffolding + DB Schema
  ├── 1.1  Create lead-management/pom.xml
  ├── 1.2  Register module in root pom.xml
  └── 1.3  Add demo_requests table to 00-schema-qa.sql

Phase 2: Entity (multi-tenant-data)
  └── 2.1  Create DemoRequestDataModel.java

Phase 3: OpenAPI Contract
  ├── 3.1  Create demo-request.yaml (CRUD spec)
  ├── 3.2  Create lead-management-module.yaml (aggregator)
  └── 3.3  Generate DTOs: mvn generate-sources -pl lead-management -am

Phase 4: Business Logic + Controller
  ├── 4.1  DemoRequestRepository
  ├── 4.2  DemoRequestCreationUseCase (with email uniqueness check)
  ├── 4.3  GetDemoRequestByIdUseCase
  ├── 4.4  GetAllDemoRequestsUseCase
  ├── 4.5  DeleteDemoRequestUseCase
  ├── 4.6  LeadManagementModelMapperConfiguration
  ├── 4.7  DemoRequestController (implements generated API interface)
  ├── 4.8  LeadManagementSecurityConfiguration (POST permitAll)
  ├── 4.9  LeadManagementControllerAdvice
  └── 4.10 Wire into application/pom.xml

Phase 5: Unit Tests
  ├── 5.1  DemoRequestCreationUseCaseTest
  ├── 5.2  GetDemoRequestByIdUseCaseTest
  ├── 5.3  GetAllDemoRequestsUseCaseTest
  ├── 5.4  DeleteDemoRequestUseCaseTest
  └── 5.5  DemoRequestControllerTest
```

---

## 5. Entity Design

### DemoRequestDataModel

```java
@Entity
@Table(name = "demo_requests")
@SQLDelete(sql = "UPDATE demo_requests SET deleted_at = CURRENT_TIMESTAMP WHERE demo_request_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DemoRequestDataModel extends SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "demo_request_id")
    private Long demoRequestId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
```

**Inheritance**: Extends `SoftDeletable` which provides:
- `deletedAt` (timestamp, null = active)
- Inherits from `Auditable`: `createdAt`, `updatedAt`

**ID Strategy**: Auto-increment (`IDENTITY`) — NOT composite key.
This entity is platform-level, not tenant-scoped.

**Status Values** (as constants in the use case):
- `PENDING` — initial state on creation
- `CONTACTED` — admin has contacted the prospect
- `SCHEDULED` — demo session scheduled
- `COMPLETED` — demo completed
- `REJECTED` — request declined

### SQL Schema

```sql
CREATE TABLE demo_requests (
    demo_request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    company_name    VARCHAR(200)  NOT NULL,
    message         TEXT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP     NULL,
    INDEX idx_demo_request_email (email, deleted_at),
    INDEX idx_demo_request_status (status, deleted_at)
);
```

---

## 6. OpenAPI Contract Design

### Endpoints

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| POST | `/v1/lead-management/demo-requests` | Public | 201 | Create demo request |
| GET | `/v1/lead-management/demo-requests/{id}` | Bearer | 200 | Get by ID |
| GET | `/v1/lead-management/demo-requests` | Bearer | 200 | List all |
| DELETE | `/v1/lead-management/demo-requests/{id}` | Bearer | 204 | Soft delete |

### DTO Schema

| DTO | Fields | Usage |
|-----|--------|-------|
| `DemoRequestCreationRequestDTO` | firstName, lastName, email, companyName, message | POST body |
| `DemoRequestCreationResponseDTO` | demoRequestId | 201 response |
| `GetDemoRequestResponseDTO` | demoRequestId, firstName, lastName, email, companyName, message, status, createdAt | GET single |
| `GetAllDemoRequests200ResponseDTO` | demoRequests (array of GetDemoRequestResponseDTO) | GET list |
| `ErrorResponseDTO` | code, message, timestamp, path | Error responses |

---

## 7. Exception Handling

### Exception Matrix

| Scenario | Exception | HTTP Status | Error Code |
|----------|-----------|-------------|------------|
| Email already submitted | `DuplicateEntityException` | 409 CONFLICT | `DEMO_001` |
| Demo request not found by ID | `EntityNotFoundException` | 404 NOT_FOUND | `DEMO_002` |

### Constants

```java
public static final String ERROR_EMAIL_ALREADY_SUBMITTED =
        "A demo request with this email has already been submitted";
public static final String ERROR_DEMO_REQUEST_NOT_FOUND =
        "Demo request not found with id: ";
```

These constants live in their respective use case classes and are referenced by tests.

---

## 8. Security Configuration

The POST endpoint must be `permitAll` (public website form submission).
GET and DELETE endpoints require authentication (admin review).

```java
@Configuration
public class LeadManagementSecurityConfiguration {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain leadManagementFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/v1/lead-management/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/v1/lead-management/demo-requests").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

---

## 9. Key Design Decisions

| Decision | Chosen | Alternative | Rationale |
|----------|--------|-------------|-----------|
| Module name | `lead-management` | `demo-requests` | Broader scope — module can grow for other lead types |
| Entity location | `multi-tenant-data` | Inside `lead-management` | Convention: ALL entities live in `multi-tenant-data` |
| Entity parent | `SoftDeletable` | `TenantScoped` | Not tenant-scoped — platform-level pre-tenant entity |
| ID type | `Long` auto-increment | UUID | Hard rule #3 |
| Email uniqueness | Repository `existsByEmail` check | DB unique constraint only | Provides clear business exception (409) vs opaque SQL error |
| Status field | String with constants | Enum | Matches existing pattern (e.g., billing status) and allows OpenAPI compatibility |

---

## 10. Verification Checklist

After all phases complete:

```bash
# Module compiles
mvn clean compile -pl lead-management -am -DskipTests

# Unit tests pass
mvn test -pl lead-management -am

# Full project builds
mvn clean install -DskipTests

# Full project tests pass
mvn clean install
```

### Convention compliance

```bash
# No any() matchers in tests
grep -rn "any()" lead-management/src/test/ && echo "FAIL: any() matchers found" || echo "PASS"

# All public classes have Javadoc
grep -rL "/\*\*" lead-management/src/main/java/com/akademiaplus/**/*.java && echo "FAIL: missing Javadoc" || echo "PASS"

# Copyright header on all files
grep -rL "Copyright (c)" lead-management/src/main/java/ lead-management/src/test/java/ && echo "FAIL: missing copyright" || echo "PASS"

# Constants for error messages (no inline strings in assertions)
grep -rn '".*not found.*"' lead-management/src/main/java/ | grep -v "static final" && echo "FAIL: inline strings" || echo "PASS"
```
