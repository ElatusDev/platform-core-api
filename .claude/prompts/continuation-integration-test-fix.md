# Continuation Prompt: Add Tenant Mock Data Pipeline + Complete Integration Test

## Context
Repo: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**ALWAYS read `AI-CODE-REF.md` before writing any code.**

## What's Already Done (DO NOT REDO)

### EntityIdAssigner Fix — COMPLETE ✅
Four production files changed, one test updated. All `infra-common` tests pass (41/41).

| File | Change |
|---|---|
| `EntityIntrospector` | Added `hasEmbeddedId()` and `hasGeneratedValue()` methods |
| `EntityMetadataResolver` | `buildMetadata()` checks `@EmbeddedId` → skip, `@GeneratedValue` → skip |
| `EntityIdAssigner` | `metadata.isSkip()` → `return` instead of `throw` |
| `TenantDataModel` | Added `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `EntityIdAssignerTest` | `shouldSkipIdAssignment_whenMetadataIndicatesSkip()` asserts silent return |

### Tutor + MinorStudent Mock Data — COMPLETE ✅ (uncommitted)
30/30 unit tests passing. See `git status` for full file list.

### Integration Test Infrastructure — COMPLETE ✅
- `TestSecurityConfiguration` with `@Bean @Primary AESGCMEncryptionService`
- `AbstractIntegrationTest` with Testcontainers MariaDB + `@DynamicPropertySource`
- `test-keystore.p12` + shadowed `mock-data-service.properties`
- Spring context boots successfully

### MockTenantRepository — ALREADY CREATED ✅
At `mock-data-system/src/main/java/com/akademiaplus/interfaceadapters/MockTenantRepository.java`
```java
public interface MockTenantRepository extends JpaRepository<@NonNull TenantDataModel, @NonNull Long> {}
```

---

## Current Blocker: Integration Test Fails

```
UnexpectedRollbackException → SequentialIDGenerator.generateId()
  → initializeSequence() → saveAndFlush(TenantSequence)
  → FK: tenant_sequences.tenant_id → tenants.tenant_id
  → No tenant row committed → rollback
```

**Root cause**: No tenant exists in the DB. `SequentialIDGenerator.generateId()` uses
`Propagation.REQUIRES_NEW` (independent transaction). The `tenant_sequences` table has
FK to `tenants`. Without a committed tenant row, the FK check fails.

---

## Task 1: Tenant Mock Data Pipeline

Build the full Factory → DataLoader → UseCase pipeline following the existing pattern.

### Key Differences from People Entities
1. **Tenant is NOT tenant-scoped** — it doesn't extend `TenantScoped`, it IS the tenant
2. **DB generates ID** — `@GeneratedValue(IDENTITY)` + AUTO_INCREMENT, `EntityIdAssigner` skips it
3. **No creation UseCase exists** — `TenantCreationUseCase` in `tenant-management` is a placeholder (empty class). The DataLoader transformer can map directly from DTO → DataModel (ModelMapper or manual)
4. **OpenAPI DTO exists** — `TenantCreateRequestDTO` generated from `tenant-management/src/main/resources/openapi/tenant-management-module.yaml`. Required fields: `organizationName`, `email`, `address`. Optional: `legalName`, `websiteUrl`, `phone`, `landline`, `description`, `taxId`

### Files to Create (follow existing pattern in `util/mock/users/`)

#### `util/mock/tenants/TenantDataGenerator.java`
- Generates fake data for all `TenantCreateRequestDTO` fields
- Use `net.datafaker.Faker` (already a dependency)
- Organization names, legal names, emails, addresses, phone numbers, tax IDs, etc.

#### `util/mock/tenants/TenantFactory.java`
- Implements `DataFactory<TenantCreateRequestDTO>`
- Uses `TenantDataGenerator`
- Follow `EmployeeFactory`/`TutorFactory` pattern exactly

#### `users/usecases/LoadTenantMockDataUseCase.java`
- Extends `AbstractMockDataUseCase<TenantCreateRequestDTO, TenantDataModel, Long>`
- Constructor takes `DataLoader` + `DataCleanUp`
- Follow `LoadEmployeeMockDataUseCase` pattern

### Configuration: `PeopleDataLoaderConfiguration.java`
Add tenant beans following the same pattern as Employee/Tutor:
- `tenantDataLoader` bean — needs a transformer `Function<TenantCreateRequestDTO, TenantDataModel>`. Since `TenantCreationUseCase` is empty, either:
  - Implement `TenantCreationUseCase.transform()` in `tenant-management` module, OR
  - Use ModelMapper directly in the configuration, OR
  - Write a simple lambda/method reference in the config
- `tenantDataCleanUp` bean

### Dependency: `pom.xml`
mock-data-system may need `tenant-management` dependency if using `TenantCreationUseCase`. Check if `TenantDataModel` is already accessible via `multi-tenant-data` (it is — already a dependency).

### Transaction Constraint ⚠️
Tenant data MUST be committed before people data runs. `SequentialIDGenerator` uses
`REQUIRES_NEW`, so it can only see committed rows. Two options:

**Option A**: Change `DataLoader.load()` for tenant to use `Propagation.REQUIRES_NEW`
(separate DataLoader bean, or a flag, or a specialized subclass)

**Option B**: Create a specialized `TenantDataLoader` that uses `REQUIRES_NEW` 
and saves + flushes each tenant individually

The key invariant: by the time `loadEmployeeMockDataUseCase.load(count)` runs, the
tenant rows MUST be visible to `SequentialIDGenerator`'s independent transactions.

---

## Task 2: Wire Tenant into Orchestration

### `LoadPeopleMockDataUseCase.java`
- Inject `LoadTenantMockDataUseCase`
- In `load(int count)`: call `loadTenantMockDataUseCase.load(count)` **FIRST**, before any people entity
- In `cleanUp()`: clean tenant **LAST** (after PII, since all entities FK → tenant)
- Update constructor accordingly

### `LoadPeopleMockDataUseCaseTest.java`
- Already needs updating from old 3-entity to current 5-entity constructor
- Now needs to add tenant as a 6th entity
- Verify tenant loads first in ordering test

---

## Task 3: Integration Test Adjustments

### `MockDataControllerIntegrationTest.java`
- **Remove `@Transactional` from the test class**. Reason: the test creates tenants via
  `REQUIRES_NEW` which commits independently. The test's `@Transactional` rollback can't
  undo committed `REQUIRES_NEW` transactions. Instead, use `@AfterEach` + cleanup or
  `@DirtiesContext`.
- Alternative: `@Sql` annotation to seed a tenant before tests, but this conflicts with
  the pipeline approach.

### Dead file cleanup
Delete `mock-data-system/src/test/resources/application-test.properties` — profile is
`mock-data-service`, this file is never loaded.

---

## Task 4: Fix Known Compile Errors

### `TutorFactory.java`
Has a compile error requiring `JsonNullable` wrapping for `provider` and `token` fields.
Check current state — may already be fixed in previous session.

### `LoadPeopleMockDataUseCaseTest.java`
Needs updating from old 3-entity constructor to current 5-entity version (now 6 with tenant).

---

## Task 5: Verify All Tests Pass

```bash
# Compile check
mvn compile -pl mock-data-system -am -q

# Unit tests
mvn test -pl mock-data-system,user-management -am

# Integration test specifically
mvn test -pl mock-data-system -am -Dtest="MockDataControllerIntegrationTest"
```

Docker Desktop must be running for Testcontainers.

---

## Task 6: Commit

Use Conventional Commits format. Suggested split:

**Commit 1** — EntityIdAssigner fix:
```
fix(infra): skip ID assignment for @EmbeddedId and @GeneratedValue entities

- EntityIntrospector: add hasEmbeddedId() and hasGeneratedValue()
- EntityMetadataResolver: check @EmbeddedId/@GeneratedValue → skip
- EntityIdAssigner: return silently on skip instead of throwing
- TenantDataModel: add @GeneratedValue(IDENTITY) for AUTO_INCREMENT
- Update EntityIdAssignerTest for new skip behavior
```

**Commit 2** — Tenant + Tutor + MinorStudent mock data + integration test:
```
feat(mock-data): add tenant, tutor, and minor student mock data pipeline

- Add TenantFactory, TenantDataGenerator, LoadTenantMockDataUseCase
- Add TutorFactory, TutorDataGenerator, LoadTutorMockDataUseCase  
- Add MinorStudentFactory, MinorStudentDataGenerator, LoadMinorStudentMockDataUseCase
- Add TutorCreationUseCase in user-management
- Wire tenant-first ordering in LoadPeopleMockDataUseCase
- Add MockTenantRepository
- Fix MockDataControllerIntegrationTest with TestSecurityConfiguration
- Add test-keystore.p12 and test properties
```

---

## Key Reference: Existing Patterns to Follow

### Factory pattern (`TutorFactory.java`):
```java
@Component
@RequiredArgsConstructor
public class TutorFactory implements DataFactory<TutorCreationRequestDTO> {
    private final TutorDataGenerator generator;
    @Override
    public List<TutorCreationRequestDTO> generate(int count) { ... }
}
```

### DataLoader configuration (`PeopleDataLoaderConfiguration.java`):
```java
@Bean
public DataLoader<EmployeeCreationRequestDTO, EmployeeDataModel, Long> employeeDataLoader(
        EmployeeRepository repository,
        DataFactory<EmployeeCreationRequestDTO> employeeFactory,
        EmployeeCreationUseCase employeeCreationUseCase) {
    return new DataLoader<>(repository, employeeCreationUseCase::transform, employeeFactory);
}
```

### DataLoader transaction (`DataLoader.java`):
```java
@Transactional  // jakarta.transaction.Transactional — defaults to REQUIRED
public void load(int count) {
    List<M> models = factory.generate(count).stream().map(transformer).toList();
    models.forEach(e -> { repository.save(e); repository.flush(); });
}
```

### TenantDataModel required fields:
- `organizationName` (NOT NULL, VARCHAR 200)
- `email` (NOT NULL, VARCHAR 200)
- `address` (NOT NULL, VARCHAR 200)
- `tenantId` — AUTO_INCREMENT, DB-generated

### Encryption test values:
- AES-256 key: `zZhnG8Pe0W9bOHWNDrqTNHC0sDIdVHEsCW/jJWPt1cI=`
- Keystore: `classpath:test-keystore.p12`, password=`testpassword`, alias=`test-jwt`
