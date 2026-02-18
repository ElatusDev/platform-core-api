# Continuation Prompt: Mock Data DAG Orchestration

## Context
Repo: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**ALWAYS read `AI-CODE-REF.md` before writing any code.**

## Current State — All Green ✅

### Commit History (9 commits on `develop`)
```
f351662 refactor(tenant): use ModelMapper in TenantCreationUseCase
7e9e537 refactor(config): move ModelMapper bean to utilities module
a01f25e test(mock-data): add integration test infrastructure with Testcontainers
38fffd7 feat(mock-data): add tenant mock data pipeline
5da9ee1 feat(mock-data): add tutor and minor student mock data pipelines
aeadeed feat(tenant): implement TenantCreationUseCase with transform
3302322 build: update byte-buddy agent and add tenant-management build plugins
5fc71ac fix(infra): resolve startup failures and type mismatches
eb08bd7 fix(infra): skip ID assignment for @EmbeddedId and @GeneratedValue entities
```

### Test Results
- **59 unit tests passing** (infra-common, user-management, tenant-management, mock-data-system)
- **2 integration tests failing** (pre-existing: `SequentialIDGenerator` FK constraint
  under `@Transactional` rollback — will be fixed as part of this work)
- **Compilation clean** across all modules

### Working Tree
Only untracked: `.claude/prompts/` (dev tooling, excluded)

---

## What Was Completed (DO NOT REDO)

### Infrastructure Fixes
- `EntityIdAssigner`: skip `@EmbeddedId` and `@GeneratedValue` entities silently
- `TenantDataModel`: added `@GeneratedValue(IDENTITY)`
- `TenantRepository`: fixed ID type `Integer` → `Long`
- `SequentialIDGenerator`: `@Lazy` to break circular dependency
- `AESGCMEncryptionService`: `@Autowired` for constructor disambiguation
- `HibernateTenantFilter`: `Optional.ifPresent()` for null safety
- Byte Buddy agent updated, `-XX:+EnableDynamicAgentLoading` added

### ModelMapper Centralization
- `ModelMapperConfig` moved from `user-management` → `utilities` module
- Converters: `LocalDate ↔ java.sql.Date`, `URI ↔ String`, `LocalDateTime → OffsetDateTime`
- `MapperModelConfig` deleted from `user-management`
- All modules get the bean transitively via `utilities` dependency

### TenantCreationUseCase (tenant-management module)
- `transform()`: uses `ModelMapper` + prototype-scoped `TenantDataModel` from `ApplicationContext`
- `create()`: returns `TenantDTO` (matches OpenAPI 201 response schema)
- 4 unit tests passing

### Mock Data Pipelines (all entity types)
- **Tenant**: `TenantDataGenerator`, `TenantFactory`, `TenantDataLoaderConfiguration`,
  `LoadTenantMockDataUseCase` (with `REQUIRES_NEW` propagation)
- **Tutor**: `TutorDataGenerator`, `TutorFactory`, `LoadTutorMockDataUseCase`,
  `TutorCreationUseCase` (user-management)
- **MinorStudent**: `MinorStudentDataGenerator`, `MinorStudentFactory`,
  `LoadMinorStudentMockDataUseCase` (factory receives tutor IDs)
- **Employee, Collaborator, AdultStudent**: pre-existing, unchanged

### Current Orchestration (TO BE REPLACED)
- `MockDataController.generateAllMockData()` → hardcoded: clean people → clean tenant →
  load tenant → load people
- `LoadPeopleMockDataUseCase` → hardcoded: load employees/collaborators/adultStudents →
  load tutors → wire tutor IDs → load minorStudents. Cleanup: reverse FK order + shared tables
- **Problem**: ordering logic split across two classes, not composable for partial loads

### Integration Test Infrastructure
- `TestSecurityConfiguration`, `AbstractIntegrationTest` with Testcontainers MariaDB
- `test-keystore.p12`, `mock-data-service.properties`
- Spring context boots; tests fail on FK constraint (tenant not committed)

---

## THE TASK: Implement DAG-Based Mock Data Orchestration

### Problem Statement
The mock data system needs to:
1. Load ALL entities in FK-safe order (current use case)
2. Load a SUBSET of entities without breaking FK constraints (future use case)
3. Clean up in reverse FK order, including shared tables (auth, PII, sequences)

Ordering logic is currently hardcoded in two places. Adding new entities requires
modifying orchestration code. Subset loading is not possible without refactoring.

### Solution: Enum DAG + Topological Sort + Registry

### Entity Dependency Graph
```
Level 0: TENANT                    (no FK — root)
         TENANT_SEQUENCE           (FK → TENANT, cleanup-only, created by SequentialIDGenerator)
         PERSON_PII               (FK → TENANT, cleanup-only, created by transform())
         INTERNAL_AUTH            (FK → TENANT, cleanup-only, created by transform())
         CUSTOMER_AUTH            (FK → TENANT, cleanup-only, created by transform())
Level 1: EMPLOYEE                  (FK → TENANT; creates INTERNAL_AUTH + PERSON_PII inline)
         COLLABORATOR              (FK → TENANT; creates INTERNAL_AUTH + PERSON_PII inline)
         ADULT_STUDENT             (FK → TENANT; creates CUSTOMER_AUTH + PERSON_PII inline)
         TUTOR                     (FK → TENANT; creates CUSTOMER_AUTH + PERSON_PII inline)
Level 2: MINOR_STUDENT             (FK → TUTOR + TENANT; creates CUSTOMER_AUTH + PERSON_PII inline)
```

Cleanup order is the REVERSE of load order. Shared tables (PII, auth, sequences) appear
in cleanup between leaf entities and TENANT.

---

## Step-by-Step Implementation Plan

### Step 1: Create `MockEntityType` enum
**File:** `mock-data-system/src/main/java/com/akademiaplus/config/MockEntityType.java`

Each enum value declares:
- `loadable` (boolean): has a `DataLoader` + `DataFactory` + `AbstractMockDataUseCase`
- `cleanable` (boolean): has a `DataCleanUp` (all values are cleanable)
- `dependencies` (varargs `MockEntityType`): direct FK parents

```java
public enum MockEntityType {
    TENANT(true, true),
    TENANT_SEQUENCE(false, true,  TENANT),
    PERSON_PII(false, true,       TENANT),
    INTERNAL_AUTH(false, true,     TENANT),
    CUSTOMER_AUTH(false, true,     TENANT),
    EMPLOYEE(true, true,          INTERNAL_AUTH, PERSON_PII),
    COLLABORATOR(true, true,      INTERNAL_AUTH, PERSON_PII),
    ADULT_STUDENT(true, true,     CUSTOMER_AUTH, PERSON_PII),
    TUTOR(true, true,             CUSTOMER_AUTH, PERSON_PII),
    MINOR_STUDENT(true, true,     TUTOR);
```

Note: People entities depend on their auth/PII types (not directly on TENANT) because
cleanup must delete people BEFORE auth/PII. TENANT is reached transitively through auth/PII.

**Tests:**
- No circular dependencies in graph
- Every loadable entity transitively depends on TENANT
- MINOR_STUDENT transitively depends on TUTOR

### Step 2: Create `MockDataExecutionPlan`
**File:** `mock-data-system/src/main/java/com/akademiaplus/config/MockDataExecutionPlan.java`

Pure function — takes `Set<MockEntityType>`, computes:
1. Transitive closure (recursively collect all dependencies)
2. Topological sort via Kahn's algorithm → `loadOrder` (only `loadable == true`)
3. Reverse full closure → `cleanupOrder` (all `cleanable == true`)

```java
public class MockDataExecutionPlan {
    private final List<MockEntityType> loadOrder;
    private final List<MockEntityType> cleanupOrder;

    public static MockDataExecutionPlan forAll();
    public static MockDataExecutionPlan forEntities(Set<MockEntityType> requested);
}
```

`forAll()` includes ALL enum values. `forEntities({MINOR_STUDENT})` computes closure
`{TENANT, TENANT_SEQUENCE, PERSON_PII, CUSTOMER_AUTH, TUTOR, MINOR_STUDENT}` and returns
load order `[TENANT, TUTOR, MINOR_STUDENT]` plus cleanup order (reversed full closure).

**Tests:**
- `forAll()`: TENANT first in load, last in cleanup
- `forAll()`: MINOR_STUDENT after TUTOR in load order
- `forAll()`: shared tables (auth, PII, sequences) between people and TENANT in cleanup
- `forEntities({MINOR_STUDENT})`: transitive closure includes TENANT and TUTOR
- `forEntities({EMPLOYEE})`: includes TENANT, INTERNAL_AUTH, PERSON_PII
- Empty input → empty plans

### Step 3: Create `MockDataRegistry`
**File:** `mock-data-system/src/main/java/com/akademiaplus/config/MockDataRegistry.java`

Spring `@Configuration` that maps `MockEntityType` → beans:

```java
@Bean
public Map<MockEntityType, AbstractMockDataUseCase<?, ?, ?>> loaderRegistry(...)

@Bean
public Map<MockEntityType, DataCleanUp<?, ?>> cleanupOnlyRegistry(...)

@Bean
public Map<MockEntityType, MockDataPostLoadHook> postLoadHooks(...)
```

**New bean needed:** `DataCleanUp<TenantSequence, TenantSequenceId>` for `tenant_sequences`
table. Create in `TenantDataLoaderConfiguration`.

**Post-load hooks** — `Map<MockEntityType, MockDataPostLoadHook>`:
- After `TUTOR` loads → query tutor IDs → inject into `MinorStudentFactory`
- Future: after `EMPLOYEE` loads → inject into store transaction factory, etc.

```java
@FunctionalInterface
public interface MockDataPostLoadHook {
    void execute();
}
```

**Tests:**
- All loadable enum values present in loader registry
- All cleanup-only enum values present in cleanup registry

### Step 4: Create `MockDataOrchestrator`
**File:** `mock-data-system/src/main/java/com/akademiaplus/usecases/MockDataOrchestrator.java`

Single entry point replacing `LoadPeopleMockDataUseCase` orchestration logic.

```java
@Service
public class MockDataOrchestrator {
    public void execute(MockDataExecutionPlan plan, int count);
    public void executeAll(int count);
}
```

**`execute()` algorithm:**
1. For each entity in `plan.getCleanupOrder()`:
   - If entity has a loader → call `loader.clean()`
   - Else if entity has a standalone cleanup → call `cleanup.clean()`
2. For each entity in `plan.getLoadOrder()`:
   - Call `loader.load(count)`
   - If entity has a post-load hook → call `hook.execute()`

**Important:** No special-casing for TENANT. `LoadTenantMockDataUseCase.load()` already
has `@Transactional(propagation = REQUIRES_NEW)` — commits automatically before next entity.

**Tests:**
- `executeAll(5)`: InOrder verification of full cleanup then full load sequence
- Post-load hooks fire between correct entities
- TENANT loads before all people entities
- Shared tables cleaned between people entities and TENANT

### Step 5: Simplify `MockDataController`
**Refactor** to delegate to orchestrator:

```java
@Override
public ResponseEntity<String> generateAllMockData(Integer count) {
    orchestrator.executeAll(count);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body("Mock data generated: " + count + " records per entity type.");
}
```

Remove `LoadTenantMockDataUseCase` and `LoadPeopleMockDataUseCase` from controller injection.

### Step 6: Delete `LoadPeopleMockDataUseCase`
All its responsibilities are now split:
- Ordering → `MockDataExecutionPlan`
- Execution → `MockDataOrchestrator`
- Tutor ID wiring → post-load hook in `MockDataRegistry`
- Shared table cleanup → cleanup-only entries in `MockDataRegistry`

Delete `LoadPeopleMockDataUseCase.java` and `LoadPeopleMockDataUseCaseTest.java`.

---

## Commit Plan

| # | Message |
|---|---|
| 1 | `feat(mock-data): add MockEntityType enum with dependency graph` |
| 2 | `feat(mock-data): add MockDataExecutionPlan with topological sort` |
| 3 | `feat(mock-data): add TenantSequence DataCleanUp bean` |
| 4 | `feat(mock-data): add MockDataRegistry and post-load hooks` |
| 5 | `feat(mock-data): add MockDataOrchestrator for DAG-ordered execution` |
| 6 | `refactor(mock-data): simplify MockDataController to use orchestrator` |
| 7 | `refactor(mock-data): remove LoadPeopleMockDataUseCase` |

---

## Deferred (Do NOT Implement Now)

- **Selective loading API endpoint** — `forEntities()` works but no REST endpoint yet
- **TenantContextHolder injection** — after tenant load, set context holder with first
  generated tenant ID. Tied to integration test fix.
- **Integration test `@Transactional` fix** — remove `@Transactional` from test class,
  use `@AfterEach` cleanup instead. Blocked until orchestrator is in place.

---

## Key File Locations

### Production Code
| File | Location |
|---|---|
| `AbstractMockDataUseCase` | `mock-data-system/.../util/base/AbstractMockDataUseCase.java` |
| `DataLoader` | `mock-data-system/.../util/base/DataLoader.java` |
| `DataCleanUp` | `mock-data-system/.../util/base/DataCleanUp.java` |
| `DataFactory` | `mock-data-system/.../util/base/DataFactory.java` |
| `MockDataController` | `mock-data-system/.../interfaceadapters/MockDataController.java` |
| `LoadPeopleMockDataUseCase` | `mock-data-system/.../users/usecases/LoadPeopleMockDataUseCase.java` |
| `LoadTenantMockDataUseCase` | `mock-data-system/.../users/usecases/LoadTenantMockDataUseCase.java` |
| `LoadEmployeeMockDataUseCase` | `mock-data-system/.../users/usecases/LoadEmployeeMockDataUseCase.java` |
| `LoadTutorMockDataUseCase` | `mock-data-system/.../users/usecases/LoadTutorMockDataUseCase.java` |
| `LoadMinorStudentMockDataUseCase` | `mock-data-system/.../users/usecases/LoadMinorStudentMockDataUseCase.java` |
| `PeopleDataLoaderConfiguration` | `mock-data-system/.../config/PeopleDataLoaderConfiguration.java` |
| `TenantDataLoaderConfiguration` | `mock-data-system/.../config/TenantDataLoaderConfiguration.java` |
| `TenantSequence` | `utilities/.../idgeneration/interfaceadapters/TenantSequence.java` |
| `TenantSequenceRepository` | `utilities/.../idgeneration/interfaceadapters/TenantSequenceRepository.java` |
| `MinorStudentFactory` | `mock-data-system/.../util/mock/users/MinorStudentFactory.java` |
| `TutorRepository` (domain) | `user-management/.../customer/interfaceadapters/TenantRepository.java` |
| `TenantContextHolder` | `infra-common/.../persistence/config/TenantContextHolder.java` |

### Test Code
| File | Location |
|---|---|
| `LoadPeopleMockDataUseCaseTest` | `mock-data-system/.../users/usecases/LoadPeopleMockDataUseCaseTest.java` |
| `MockDataControllerIntegrationTest` | `mock-data-system/.../interfaceadapters/MockDataControllerIntegrationTest.java` |
| `AbstractIntegrationTest` | `mock-data-system/.../config/AbstractIntegrationTest.java` |
| `TestSecurityConfiguration` | `mock-data-system/.../config/TestSecurityConfiguration.java` |

---

## Verification Commands
```bash
# Compile
mvn compile -pl mock-data-system -am -q

# Unit tests (excludes integration)
mvn test -pl mock-data-system,user-management,tenant-management,infra-common -am

# Integration test only (requires Docker Desktop)
mvn test -pl mock-data-system -am -Dtest="MockDataControllerIntegrationTest"
```
