# Creation UseCase Implementation Workflow — AkademiaPlus

**Target**: Claude Code CLI  
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`  
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.

---

## 1. Pattern Analysis Summary

Three structural variants exist in the codebase. All creation use cases MUST conform to **Variant A** (the canonical pattern). Variant C (`CreateCourseUseCase`) must be refactored.

### Variant A — Canonical (Internal Auth Entities): `EmployeeCreationUseCase`, `CollaboratorCreationUseCase`

```
Dependencies: ApplicationContext, ModelMapper, Repository, HashingService, PiiNormalizer
create():  @Transactional → modelMapper.map(repo.save(transform(dto)), ResponseDTO.class)
transform():
  1. applicationContext.getBean(InternalAuthDataModel.class)  → modelMapper.map(dto, auth)
  2. applicationContext.getBean(PersonPIIDataModel.class)      → modelMapper.map(dto, pii)
  3. applicationContext.getBean(EntityDataModel.class)          → modelMapper.map(dto, model, MAP_NAME)
  4. Wire nested: model.setPersonPII(pii), model.setInternalAuth(auth)
  5. Hash PII: normalize email/phone → generateHash → set emailHash/phoneHash
  6. Hash username
ModelMapper Config: Named TypeMap skipping ID + personPII + internalAuth
```

### Variant B — Canonical (Customer Auth Entities): `AdultStudentCreationUseCase`, `TutorCreationUseCase`

```
Same as Variant A but replaces InternalAuth with CustomerAuth:
  - CustomerAuth wired manually (provider + token) — not via ModelMapper
  - Sets entryDate = LocalDate.now()
  - TutorCreationUseCase owns both Tutor and MinorStudent lifecycle
  - MinorStudent.transform does repository lookup for tutor FK
```

### Variant C — Non-Person Entities (Simple): `TenantCreationUseCase`

```
Dependencies: ApplicationContext, ModelMapper, Repository (no PII, no auth)
create():  @Transactional → repo.save(transform(dto)) → modelMapper.map(saved, ResponseDTO.class)
transform():
  1. applicationContext.getBean(EntityDataModel.class)
  2. modelMapper.map(dto, model, MAP_NAME)
  3. return model
ModelMapper Config: Named TypeMap skipping entity ID field (+ any FK fields that could deep-match)
```

### Key Invariants (ALL variants)

1. **`public static final String MAP_NAME`** — referenced by both UseCase and ModelMapperConfiguration
2. **Prototype-scoped beans** via `applicationContext.getBean()` — never `new EntityDataModel()`
3. **Named TypeMap** in `modelMapper.map(dto, model, MAP_NAME)` — prevents deep-matching pollution
4. **Separate ModelMapperConfiguration class** per module with `@PostConstruct` registering TypeMaps
5. **Skip rules**: Always skip the entity ID setter + all nested object setters (personPII, auth, FK relationships)
6. **Implicit mapping toggle**: `setImplicitMappingEnabled(false)` before registration, `true` after
7. **Constructor injection** — `@RequiredArgsConstructor` or explicit constructor (no `@Autowired`)

---

## 2. Entity → Module Mapping & Classification

### Root Entities (get their own CreationUseCase)

| Entity | Module | Variant | OpenAPI Exists | UseCase Exists | Notes |
|--------|--------|---------|----------------|----------------|-------|
| `TenantDataModel` | tenant-management | C | ✅ | ✅ | Done |
| `EmployeeDataModel` | user-management | A | ✅ | ✅ | Done — reference impl |
| `CollaboratorDataModel` | user-management | A | ✅ | ✅ | Done |
| `AdultStudentDataModel` | user-management | B | ✅ | ✅ | Done |
| `TutorDataModel` + `MinorStudentDataModel` | user-management | B | ✅ | ✅ | Done — dual entity |
| `CourseDataModel` | course-management | C | ✅ | ⚠️ Refactor | Needs named TypeMap + prototype bean |
| `ScheduleDataModel` | course-management | C | ❌ Create | ❌ Create | FK to Course — skip `course` setter |
| `CourseEventDataModel` | course-management | C | ❌ Create | ❌ Create | FKs to Course + Collaborator — skip both |
| `MembershipDataModel` | billing | C | ✅ | ❌ Create | M2M with courses — skip `courses` setter |
| `CompensationDataModel` | billing | C | ✅ | ❌ Create | M2M with collaborators — skip `collaborators` |
| `StoreProductDataModel` | pos-system | C | ❌ Create | ❌ Create | Simple entity |
| `StoreTransactionDataModel` | pos-system | C | ❌ Create | ❌ Create | FK to Employee, cascades SaleItems |
| `NotificationDataModel` | notification-system | C | ❌ Create | ❌ Create | Enum fields (type, priority) |
| `TenantBillingCycleDataModel` | tenant-management | C | ❌ Create | ❌ Create | FK to TenantSubscription |
| `TenantSubscriptionDataModel` | tenant-management | C | ❌ Create | ❌ Create | FK to Tenant |

### Child Entities (created via cascade or parent UseCase)

| Entity | Parent | Strategy |
|--------|--------|----------|
| `StoreSaleItemDataModel` | `StoreTransactionDataModel` | Created via `CascadeType.ALL` on parent `saleItems` list |
| `EmailDataModel` | `NotificationDataModel` | Created in a composite NotificationCreationUseCase |
| `EmailAttachmentDataModel` | `EmailDataModel` | Cascade from Email |
| `EmailRecipientDataModel` | `EmailDataModel` | Cascade from Email |
| `NotificationDeliveryDataModel` | `NotificationDataModel` | System-generated at send time |
| `PaymentAdultStudentDataModel` | billing | Own UseCase (has OpenAPI) |
| `PaymentTutorDataModel` | billing | Own UseCase (has OpenAPI) |
| `MembershipAdultStudentDataModel` | billing | Own UseCase (has OpenAPI — association entity) |
| `MembershipTutorDataModel` | billing | Own UseCase (has OpenAPI — association entity) |
| `CardPaymentInfoDataModel` | billing | Part of payment flow |

---

## 3. Execution Order (Dependency Graph)

Execute in this order to respect compilation dependencies:

```
Phase 0: Refactor existing non-conformant UseCase
  └── 0.1  CreateCourseUseCase → Refactor to canonical pattern

Phase 1: tenant-management (no cross-module FK)
  ├── 1.1  TenantSubscriptionCreationUseCase
  └── 1.2  TenantBillingCycleCreationUseCase

Phase 2: course-management (depends on user-management entities via FK)
  ├── 2.1  ScheduleCreationUseCase
  └── 2.2  CourseEventCreationUseCase

Phase 3: billing (depends on user-management + course-management)
  ├── 3.1  CompensationCreationUseCase
  ├── 3.2  MembershipCreationUseCase
  ├── 3.3  PaymentAdultStudentCreationUseCase
  ├── 3.4  PaymentTutorCreationUseCase
  ├── 3.5  MembershipAdultStudentCreationUseCase
  └── 3.6  MembershipTutorCreationUseCase

Phase 4: pos-system
  ├── 4.1  StoreProductCreationUseCase
  └── 4.2  StoreTransactionCreationUseCase (includes SaleItems via cascade)

Phase 5: notification-system
  └── 5.1  NotificationCreationUseCase
```

---

## 4. Step-by-Step Implementation Per Entity

For EACH entity in the execution order above, perform these steps **sequentially**. Do NOT proceed to the next entity until the current one compiles and all tests pass.

### Step 1: Examine the DataModel

```bash
# Read the entity to identify:
# - Entity ID field name and setter (e.g., setCompensationId)
# - All @ManyToOne / @OneToMany / @ManyToMany relationships (these need skip rules)
# - All @Enumerated fields (may need TypeMap converter)
# - Parent class (TenantScoped, BasePayment, AbstractEvent, etc.)
# - Any @CreationTimestamp or auto-set fields
cat multi-tenant-data/src/main/java/com/akademiaplus/<path>/<EntityDataModel>.java
```

### Step 2: Check or Create OpenAPI Spec

If the entity's module already has an OpenAPI spec with creation DTOs, skip this step.
Otherwise, create the OpenAPI YAML following the existing pattern:

**File**: `<module>/src/main/resources/openapi/<entity-name>.yaml`

```yaml
openapi: 3.0.0
info:
  title: <Entity> Entity
  version: '1.0.0'
components:
  schemas:
    Base<Entity>:
      type: object
      properties:
        # ALL writable fields from DataModel (exclude: ID, tenantId, deletedAt, createdAt, updatedAt)
        # For FK references: use <entity>Id as integer/int64, NOT the full object
        # For enums: use string type with maxLength
        # For BigDecimal: use number format double
        # For LocalDate: use string format date
        # For LocalDateTime: use string format date-time
        # For LocalTime: use string format time
      required:
        # List all non-nullable fields
    <Entity>CreationRequest:
      allOf:
        - $ref: '#/components/schemas/Base<Entity>'
    <Entity>CreationResponse:
      type: object
      properties:
        <entityId>:
          type: integer
          format: int64
          readOnly: true
      required:
        - <entityId>
    Get<Entity>Response:
      allOf:
        - $ref: '#/components/schemas/Base<Entity>'
        - type: object
          properties:
            <entityId>:
              type: integer
              format: int64
              readOnly: true
          required:
            - <entityId>
    ErrorResponse:
      type: object
paths:
  '/<entities>':
    post:
      operationId: create<Entity>
      summary: Create a new <entity>
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/<Entity>CreationRequest'
      responses:
        '201':
          description: <Entity> created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/<Entity>CreationResponse'
        '400':
          description: Invalid request data
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

Then register in the module YAML (`<module>/src/main/resources/openapi/<module>-module.yaml`):

```yaml
# Add under components.schemas:
    <Entity>CreationRequest:
      $ref: './<entity-name>.yaml#/components/schemas/<Entity>CreationRequest'
    <Entity>CreationResponse:
      $ref: './<entity-name>.yaml#/components/schemas/<Entity>CreationResponse'
# Add under paths:
  '/<entities>':
    $ref: './<entity-name>.yaml#/paths/~1<entities>'
```

### Step 3: Generate DTOs

```bash
mvn clean generate-sources -pl <module-name> -am -DskipTests
```

Verify the generated DTOs exist:
```bash
find <module-name>/target/generated-sources -name "*<Entity>Creation*DTO.java"
```

### Step 4: Create Repository (if missing)

**File**: `<module>/src/main/java/com/akademiaplus/<subpackage>/interfaceadapters/<Entity>Repository.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.<subpackage>.interfaceadapters;

import com.akademiaplus.<datamodel-package>.<EntityDataModel>;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface <Entity>Repository extends JpaRepository<<EntityDataModel>, <EntityDataModel>.<CompositeIdClass>> {
}
```

### Step 5: Create the CreationUseCase

**File**: `<module>/src/main/java/com/akademiaplus/<subpackage>/usecases/<Entity>CreationUseCase.java`

Follow this template for **Variant C (non-person entities)**:

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.<subpackage>.usecases;

import com.akademiaplus.<datamodel-package>.<EntityDataModel>;
import com.akademiaplus.<subpackage>.interfaceadapters.<Entity>Repository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.<module>.dto.<Entity>CreationRequestDTO;
import openapi.akademiaplus.domain.<module>.dto.<Entity>CreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles <entity> creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into nested JPA relationships and ID fields.
 */
@Service
@RequiredArgsConstructor
public class <Entity>CreationUseCase {
    public static final String MAP_NAME = "<camelCaseEntity>Map";

    private final ApplicationContext applicationContext;
    private final <Entity>Repository repository;
    private final ModelMapper modelMapper;
    // Add FK repositories here if transform() needs to look up referenced entities

    @Transactional
    public <Entity>CreationResponseDTO create(<Entity>CreationRequestDTO dto) {
        <EntityDataModel> saved = repository.save(transform(dto));
        return modelMapper.map(saved, <Entity>CreationResponseDTO.class);
    }

    /**
     * Maps a {@link <Entity>CreationRequestDTO} to a persistence-ready data model.
     * <p>
     * Uses a named TypeMap to prevent deep-matching of DTO fields
     * into nested JPA relationships. FK associations are resolved via
     * repository lookups, not through ModelMapper.
     *
     * @param dto the creation request
     * @return populated data model ready for persistence
     */
    public <EntityDataModel> transform(<Entity>CreationRequestDTO dto) {
        final <EntityDataModel> model = applicationContext.getBean(<EntityDataModel>.class);
        modelMapper.map(dto, model, MAP_NAME);
        // Wire FK relationships manually:
        // e.g., model.setCourse(courseRepository.findById(dto.getCourseId()).orElseThrow(...));
        return model;
    }
}
```

**For entities with FK lookups** (CourseEvent, Schedule, StoreTransaction, Payments, etc.):
Add the FK repository as a constructor dependency and resolve in `transform()`:

```java
    // Example for CourseEventCreationUseCase:
    private final CourseRepository courseRepository;
    private final CollaboratorRepository collaboratorRepository;

    public CourseEventDataModel transform(CourseEventCreationRequestDTO dto) {
        final CourseEventDataModel model = applicationContext.getBean(CourseEventDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        CourseDataModel course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));
        model.setCourse(course);

        CollaboratorDataModel collaborator = collaboratorRepository.findById(dto.getCollaboratorId())
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found: " + dto.getCollaboratorId()));
        model.setCollaborator(collaborator);

        return model;
    }
```

### Step 6: Create or Update ModelMapperConfiguration

If the module already has a `*ModelMapperConfiguration.java`, add the new TypeMap registration to it.
Otherwise, create a new one.

**File**: `<module>/src/main/java/com/akademiaplus/config/<Module>ModelMapperConfiguration.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.<datamodel-package>.<EntityDataModel>;
import com.akademiaplus.<subpackage>.usecases.<Entity>CreationUseCase;
import openapi.akademiaplus.domain.<module>.dto.<Entity>CreationRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for <module> DTO → DataModel conversions.
 */
@Configuration
public class <Module>ModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public <Module>ModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        register<Entity>Map();
        // Add more registrations as entities are added

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void register<Entity>Map() {
        modelMapper.createTypeMap(
                <Entity>CreationRequestDTO.class,
                <EntityDataModel>.class,
                <Entity>CreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(<EntityDataModel>::set<EntityId>);       // Always skip entity ID
            // Skip ALL nested object setters:
            // mapper.skip(<EntityDataModel>::setCourse);         // FK relationships
            // mapper.skip(<EntityDataModel>::setCollaborators);  // M2M collections
            // mapper.skip(<EntityDataModel>::setSchedules);      // OneToMany collections
        }).implicitMappings();
    }
}
```

**Skip rule decision matrix:**

| DataModel field type | Action |
|---------------------|--------|
| `@Id` entity ID (e.g., `setCompensationId`) | **Always skip** |
| `@ManyToOne` FK object (e.g., `setCourse`) | **Always skip** — resolve via repository |
| `@OneToMany` collection (e.g., `setSchedules`) | **Always skip** |
| `@ManyToMany` collection (e.g., `setCourses`) | **Always skip** |
| `setTenantId` (from `TenantScoped`) | **Do NOT skip** — inherited, set by `EntityIdAssigner` |
| `setPersonPII` / `setInternalAuth` / `setCustomerAuth` | **Always skip** — wired manually |
| Scalar fields (String, int, BigDecimal, enums, dates) | **Do NOT skip** — let implicit mapping handle |

### Step 7: Compile and Verify

```bash
mvn clean compile -pl <module-name> -am -DskipTests
```

Fix any compilation errors before proceeding to tests.

### Step 8: Create Unit Test

**File**: `<module>/src/test/java/com/akademiaplus/<subpackage>/usecases/<Entity>CreationUseCaseTest.java`

```java
package com.akademiaplus.<subpackage>.usecases;

import com.akademiaplus.<datamodel-package>.<EntityDataModel>;
import com.akademiaplus.<subpackage>.interfaceadapters.<Entity>Repository;
import openapi.akademiaplus.domain.<module>.dto.<Entity>CreationRequestDTO;
import openapi.akademiaplus.domain.<module>.dto.<Entity>CreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("<Entity>CreationUseCase")
@ExtendWith(MockitoExtension.class)
class <Entity>CreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private <Entity>Repository repository;
    @Mock private ModelMapper modelMapper;
    // @Mock private <FkEntity>Repository fkRepository;  // if entity has FK lookups

    private <Entity>CreationUseCase useCase;

    // --- Shared test constants ---
    // Extract ALL values used across multiple tests as public static final
    // e.g.:
    // private static final String TEST_TYPE = "HOURLY";
    // private static final BigDecimal TEST_AMOUNT = new BigDecimal("50.00");

    @BeforeEach
    void setUp() {
        useCase = new <Entity>CreationUseCase(
                applicationContext,
                repository,
                modelMapper
                // fkRepository
        );
    }

    // Helper to build a standard DTO for tests
    private <Entity>CreationRequestDTO buildDto() {
        <Entity>CreationRequestDTO dto = new <Entity>CreationRequestDTO();
        // Set all required fields using constants
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype bean from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            <Entity>CreationRequestDTO dto = buildDto();
            <EntityDataModel> prototypeModel = new <EntityDataModel>();
            when(applicationContext.getBean(<EntityDataModel>.class)).thenReturn(prototypeModel);

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(<EntityDataModel>.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            <Entity>CreationRequestDTO dto = buildDto();
            <EntityDataModel> prototypeModel = new <EntityDataModel>();
            when(applicationContext.getBean(<EntityDataModel>.class)).thenReturn(prototypeModel);

            // When
            <EntityDataModel> result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel, <Entity>CreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
        }

        // --- FK lookup tests (if applicable) ---
        // @Test
        // @DisplayName("Should resolve FK entity via repository lookup")
        // void shouldResolveFkEntity_whenTransforming() { ... }

        // @Test
        // @DisplayName("Should throw exception when FK entity not found")
        // void shouldThrowException_whenFkEntityNotFound() { ... }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            <Entity>CreationRequestDTO dto = buildDto();
            <EntityDataModel> prototypeModel = new <EntityDataModel>();
            <EntityDataModel> savedModel = new <EntityDataModel>();
            <Entity>CreationResponseDTO expectedDto = new <Entity>CreationResponseDTO();

            when(applicationContext.getBean(<EntityDataModel>.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, <Entity>CreationUseCase.MAP_NAME);
            when(repository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, <Entity>CreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            <Entity>CreationResponseDTO result = useCase.create(dto);

            // Then
            verify(repository).save(prototypeModel);
            verify(modelMapper).map(savedModel, <Entity>CreationResponseDTO.class);
            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("Should pass transform result directly to repository save")
        void shouldPassTransformResultToSave_whenCreating() {
            // Given
            <Entity>CreationRequestDTO dto = buildDto();
            <EntityDataModel> prototypeModel = new <EntityDataModel>();
            <EntityDataModel> savedModel = new <EntityDataModel>();
            <Entity>CreationResponseDTO responseDto = new <Entity>CreationResponseDTO();

            when(applicationContext.getBean(<EntityDataModel>.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, <Entity>CreationUseCase.MAP_NAME);
            when(repository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, <Entity>CreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then — verify the exact object flow: getBean → map → save → map
            InOrder inOrder = inOrder(applicationContext, modelMapper, repository);
            inOrder.verify(applicationContext).getBean(<EntityDataModel>.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel, <Entity>CreationUseCase.MAP_NAME);
            inOrder.verify(repository).save(prototypeModel);
            inOrder.verify(modelMapper).map(savedModel, <Entity>CreationResponseDTO.class);
        }
    }
}
```

**Test conventions checklist (from AI-CODE-REF.md):**

- [ ] Given-When-Then comments (NEVER Arrange-Act-Assert)
- [ ] `shouldDoX_whenGivenY()` naming
- [ ] `@DisplayName` on every `@Test` and `@Nested` class
- [ ] ZERO `any()` matchers — use exact values
- [ ] `doNothing().when(modelMapper).map(dto, model, MAP_NAME)` for void overloads
- [ ] `@Mock` all dependencies, construct UseCase in `@BeforeEach`
- [ ] Class-level `private static final` constants for shared test values
- [ ] `assertThat()` from AssertJ, not JUnit `assertEquals`
- [ ] `@Nested` classes: `Transformation` and `Persistence` (add `FkResolution` if entity has FKs)

### Step 9: Run Tests

```bash
mvn test -pl <module-name> -am
```

Fix any failures. Common issues:
- `PotentialStubbingProblem`: Missing `doNothing().when()` for void ModelMapper overload
- DTO constructor mismatches: Check generated DTO constructor signatures
- Missing mock stubs for FK repository lookups

### Step 10: Commit

```bash
git add -A
git commit -m "feat(<module>): add <Entity>CreationUseCase with unit tests

Implement creation use case following canonical pattern:
- Prototype-scoped bean via ApplicationContext.getBean()
- Named TypeMap '<camelCaseEntity>Map' to prevent deep-matching
- ModelMapper skip rules for ID and nested object fields
- Unit tests with Given-When-Then, zero any() matchers"
```

---

## 5. Phase 0 — Refactor CreateCourseUseCase

The existing `CreateCourseUseCase` in `course-management` does NOT follow the canonical pattern. It uses:
- `modelMapper.map(dto, CourseDataModel.class)` — unnamed TypeMap, creates new instance
- No `ApplicationContext.getBean()` for prototype-scoped entity
- No named TypeMap → vulnerable to deep-matching pollution

### Refactor steps:

1. **Add** `public static final String MAP_NAME = "courseMap"` to `CreateCourseUseCase`
2. **Add** `ApplicationContext applicationContext` as constructor dependency
3. **Replace** `modelMapper.map(dto, CourseDataModel.class)` with:
   ```java
   final CourseDataModel model = applicationContext.getBean(CourseDataModel.class);
   modelMapper.map(dto, model, MAP_NAME);
   ```
4. **Extract** `transform()` method from `create()`:
   ```java
   public CourseDataModel transform(CourseCreationRequestDTO dto) {
       List<CollaboratorDataModel> existingCollaborators =
               courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds());
       List<ScheduleDataModel> existingSchedules =
               courseValidator.validateSchedulesAvailable(dto.getTimeTableIds());

       final CourseDataModel model = applicationContext.getBean(CourseDataModel.class);
       modelMapper.map(dto, model, MAP_NAME);
       model.setAvailableCollaborators(existingCollaborators);
       // Note: schedules saved separately in create()
       return model;
   }
   ```
5. **Create** `course-management/src/main/java/com/akademiaplus/config/CoordinationModelMapperConfiguration.java`:
   ```java
   // Register TypeMap with skip rules:
   mapper.skip(CourseDataModel::setCourseId);
   mapper.skip(CourseDataModel::setSchedules);
   mapper.skip(CourseDataModel::setAvailableCollaborators);
   ```
6. **Create** `CreateCourseUseCaseTest` following the test template above — must cover:
    - Transformation: prototype bean retrieval, named TypeMap delegation
    - Validation delegation to `CourseValidator`
    - Persistence: save + schedule save + response mapping
7. **Run**: `mvn test -pl course-management -am`
8. **Commit**: `refactor(course-management): align CreateCourseUseCase with canonical pattern`

---

## 6. Entity-Specific Implementation Notes

### Phase 1: tenant-management

**1.1 TenantSubscriptionCreationUseCase**
- DataModel: `TenantSubscriptionDataModel` — check fields
- OpenAPI: Create `tenant-subscription.yaml`
- Skip: `setSubscriptionId`, plus any FK to Tenant (check DataModel for relationship fields)
- Add to existing `TenantModelMapperConfiguration`

**1.2 TenantBillingCycleCreationUseCase**
- DataModel: `TenantBillingCycleDataModel` — check fields
- OpenAPI: Create `tenant-billing-cycle.yaml`
- Skip: `setBillingCycleId`, FK to TenantSubscription
- Add to existing `TenantModelMapperConfiguration`

### Phase 2: course-management

**2.1 ScheduleCreationUseCase**
- DataModel: `ScheduleDataModel` — has `@ManyToOne course`
- DTO should include `courseId` (Long) — NOT the full course object
- transform(): look up Course via CourseRepository, set `model.setCourse(course)` — wait, the JoinColumn is `insertable=false, updatable=false`, so the FK column must exist separately. Check the schema to determine if there's a separate `course_id` scalar column or if it's only through the relationship. If `insertable=false`, you may need to set the FK via a scalar field rather than the object.
- Skip: `setScheduleId`, `setCourse`
- Add to `CoordinationModelMapperConfiguration`

**2.2 CourseEventCreationUseCase**
- DataModel: `CourseEventDataModel` — extends `AbstractEvent`, has `course`, `collaborator`, `adultAttendees`, `minorAttendees`
- Read `AbstractEvent.java` to identify inherited fields
- DTO: include `courseId`, `collaboratorId`, plus event fields from AbstractEvent
- Skip: `setCourseEventId`, `setCourse`, `setCollaborator`, `setAdultAttendees`, `setMinorAttendees`
- Attendees are M2M — either skip in creation or accept ID lists and resolve
- Add to `CoordinationModelMapperConfiguration`

### Phase 3: billing

**3.1 CompensationCreationUseCase**
- OpenAPI already exists with `CompensationCreationRequestDTO`
- DataModel has M2M `collaborators` — skip it, collaborator association is a separate endpoint
- Skip: `setCompensationId`, `setCollaborators`
- Create `BillingModelMapperConfiguration`

**3.2 MembershipCreationUseCase**
- OpenAPI already exists with `MembershipCreationRequestDTO`
- DataModel has M2M `courses` — skip it (separate association)
- Skip: `setMembershipId`, `setCourses`
- Add to `BillingModelMapperConfiguration`

**3.3-3.6 Payment and MembershipAssociation UseCases**
- OpenAPI already exists for all four
- These are association/payment entities — check `BasePayment` for inherited fields
- PaymentAdultStudent/PaymentTutor extend BasePayment — have `paymentDate`, `amount`, `paymentMethod` from parent
- MembershipAdultStudent/MembershipTutor are join entities with FK to membership + student/tutor
- Each needs FK repository lookups in transform()
- Skip: entity ID + all FK object setters

### Phase 4: pos-system

**4.1 StoreProductCreationUseCase**
- Simple entity, no FKs
- Create OpenAPI: `store-product.yaml`
- Skip: `setStoreProductId`
- Create `PosModelMapperConfiguration`

**4.2 StoreTransactionCreationUseCase**
- Has FK to Employee (optional), cascades SaleItems
- DTO should include `employeeId` (optional) and `saleItems` as nested list
- transform(): resolve Employee via EmployeeRepository (if provided)
- SaleItems created via cascade — DTO includes nested SaleItem creation objects
- This is the most complex transform — may need a separate `transformSaleItem()` helper
- Skip: `setStoreTransactionId`, `setEmployee`, `setSaleItems`

### Phase 5: notification-system

**5.1 NotificationCreationUseCase**
- Has enum fields (`NotificationType`, `NotificationPriority`)
- DTO uses string representations — ModelMapper handles enum conversion if enum names match
- If enum names differ from DTO strings, register a custom converter
- Skip: `setNotificationId`
- Create `NotificationModelMapperConfiguration`

---

## 7. Verification Checklist (Per Entity)

Run after each entity implementation:

```bash
# 1. Module compiles
mvn clean compile -pl <module> -am -DskipTests

# 2. All unit tests pass
mvn test -pl <module> -am

# 3. Full project still builds (no cross-module breakage)
mvn clean install -DskipTests
```

After ALL phases complete:

```bash
# Full build with all tests
mvn clean install

# Verify no regressions
mvn test
```

---

## 8. File Inventory Template (Per Entity)

For each entity, you will create or modify these files:

| # | File | Action | Location |
|---|------|--------|----------|
| 1 | `<entity>.yaml` | Create/verify | `<module>/src/main/resources/openapi/` |
| 2 | `<module>-module.yaml` | Update refs | `<module>/src/main/resources/openapi/` |
| 3 | `<Entity>Repository.java` | Create if missing | `<module>/.../interfaceadapters/` |
| 4 | `<Entity>CreationUseCase.java` | Create | `<module>/.../usecases/` |
| 5 | `<Module>ModelMapperConfiguration.java` | Create/update | `<module>/.../config/` |
| 6 | `<Entity>CreationUseCaseTest.java` | Create | `<module>/src/test/.../usecases/` |

---

## 9. Critical Reminders

1. **NEVER use `new EntityDataModel()`** in production code — always `applicationContext.getBean()`
2. **NEVER use unnamed ModelMapper TypeMap** for DTO→Entity — always named
3. **ALWAYS skip entity ID setters** in TypeMap — IDs assigned by `EntityIdAssigner`
4. **ALWAYS skip nested object setters** — wire manually after mapping
5. **`setImplicitMappingEnabled(false/true)` sandwich** around TypeMap registration
6. **Long IDs everywhere** — never Integer for entity IDs
7. **`@Transactional` on `create()` only** — not on `transform()`
8. **`transform()` must be `public`** — allows direct unit testing without persistence
9. **Constants**: `MAP_NAME` must be `public static final` and referenced from both UseCase and Configuration
10. **Test void ModelMapper**: `doNothing().when(modelMapper).map(dto, model, MAP_NAME)` — strict stubbing requires this