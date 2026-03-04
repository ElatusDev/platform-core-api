# Update Use Case Workflow — User-Management Entities

> Reusable workflow for implementing PUT (update) endpoints for all 5 user-management entities.
> Each entity follows the same structural pattern with entity-specific field variations.
>
> **Status**: PersonPIIRepository prerequisite + AdultStudent — DONE. Remaining: Employee, Collaborator, Tutor, MinorStudent.

---

## 1. Field Registry

Fields derived from OpenAPI `*UpdateRequest` schemas in `user-management/src/main/resources/openapi/`.

### 1.1 Per-Entity Updatable Fields

| Entity | Entity-Specific Fields | PII Fields (shared) | Auth Fields | Immutable (never in UpdateRequest) |
|--------|----------------------|---------------------|-------------|-----------------------------------|
| **Employee** | `employeeType`, `birthdate`, `entryDate`, `profilePicture` | `firstName`, `lastName`, `email`, `phoneNumber`, `address`, `zipCode` | `role` | `username`, `password`, IDs, `tenantId`, `createdAt` |
| **Collaborator** | `skills`, `birthdate`, `entryDate`, `profilePicture` | (same) | `role` | `username`, `password`, IDs, `tenantId`, `createdAt` |
| **AdultStudent** | `birthdate`, `profilePicture` | (same) | — | `provider`, `token`, IDs, `tenantId`, `createdAt` |
| **Tutor** | `birthdate` | (same) | `provider` (nullable), `token` (nullable) | IDs, `tenantId`, `createdAt` |
| **MinorStudent** | `birthdate`, `tutorId`, `profilePicture` | (same) | — | `provider`, `token`, IDs, `tenantId`, `createdAt` |

### 1.2 Auth Model Mapping

| Entity | Auth Model | Auth Updatable Fields | Update Approach |
|--------|-----------|----------------------|-----------------|
| Employee | `InternalAuthDataModel` | `role` | Direct setter: `existing.getInternalAuth().setRole(dto.getRole())` |
| Collaborator | `InternalAuthDataModel` | `role` | Direct setter: `existing.getInternalAuth().setRole(dto.getRole())` |
| AdultStudent | `CustomerAuthDataModel` | — | No auth update needed |
| Tutor | `CustomerAuthDataModel` (optional) | `provider`, `token` | Direct setter on auth if non-null |
| MinorStudent | `CustomerAuthDataModel` | — | No auth update needed |

### 1.3 PII Rehashing Rules

When `email` or `phoneNumber` changes, the corresponding hash must be recomputed and duplicate-validated **excluding the entity's own PII record** (self-exclusion).

Fields requiring rehash on update:
- `email` → normalize → hash → set `emailHash` → validate via `existsByEmailHashAndPersonPiiIdNot`
- `phoneNumber` → normalize → hash → set `phoneHash` → validate via `existsByPhoneHashAndPersonPiiIdNot`

### 1.4 Response DTO Pattern

All `*UpdateResponseDTO` classes have two fields only: `{entity}Id` (Long) and `message` (String).
Construct the response **manually** (no ModelMapper) — simpler and avoids needing a TypeMap for a 2-field DTO.

```java
{Entity}UpdateResponseDTO response = new {Entity}UpdateResponseDTO();
response.set{Entity}Id({entity}Id);
response.setMessage(UPDATE_SUCCESS_MESSAGE);
return response;
```

---

## 2. One-Time Prerequisites (COMPLETED)

### 2.1 PersonPIIRepository — Self-Exclusion Methods

**File**: `user-management/src/main/java/com/akademiaplus/interfaceadapters/PersonPIIRepository.java`

Already added:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
boolean existsByEmailHashAndPersonPiiIdNot(String emailHash, Long personPiiId);

@Lock(LockModeType.PESSIMISTIC_WRITE)
boolean existsByPhoneHashAndPersonPiiIdNot(String phoneHash, Long personPiiId);
```

> **Why?** During creation, any duplicate is invalid. During update, the entity's _own_ existing email/phone must be excluded from the duplicate check. Spring Data JPA derives the query: `WHERE email_hash = ? AND person_pii_id != ?`.

---

## 3. Implementation Steps (Per Entity)

Follow these steps for each entity, substituting `{Entity}` with the concrete name (e.g., `Employee`).

### Step 1: Create `{Entity}UpdateUseCase.java`

**Location**: `user-management/src/main/java/com/akademiaplus/{entity-package}/usecases/{Entity}UpdateUseCase.java`

**Package mapping**:
| Entity | Package |
|--------|---------|
| Employee | `com.akademiaplus.employee.usecases` |
| Collaborator | `com.akademiaplus.collaborator.usecases` |
| AdultStudent | `com.akademiaplus.customer.adultstudent.usecases` |
| Tutor | `com.akademiaplus.customer.tutor.usecases` |
| MinorStudent | `com.akademiaplus.customer.minorstudent.usecases` |

**Canonical structure** (proven by AdultStudentUpdateUseCase implementation):

```java
@Service
@RequiredArgsConstructor
public class {Entity}UpdateUseCase {

    public static final String MAP_NAME = "{entity}UpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "{Entity} updated successfully";

    private final {Entity}Repository {entity}Repository;
    private final PersonPIIRepository personPIIRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;
    private final PiiNormalizer piiNormalizer;
    private final HashingService hashingService;
    // + TutorRepository for MinorStudent only

    @Transactional
    public {Entity}UpdateResponseDTO update(Long {entity}Id, {Entity}UpdateRequestDTO dto) {

        // 1. Load existing entity by composite key
        Long tenantId = tenantContextHolder.requireTenantId();
        {Entity}DataModel existing = {entity}Repository
                .findById(new {Entity}DataModel.{Entity}CompositeId(tenantId, {entity}Id))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.{ENTITY_CONSTANT}, String.valueOf({entity}Id)));

        // 2. Map entity-specific fields via named TypeMap (skips ID + nested objects)
        modelMapper.map(dto, existing, MAP_NAME);

        // 3. Update PII on existing PersonPII (unnamed void map + manual phoneNumber)
        PersonPIIDataModel pii = existing.getPersonPII();
        modelMapper.map(dto, pii);                    // unnamed map: DTO → PersonPII
        pii.setPhoneNumber(dto.getPhoneNumber());     // explicit set (matches creation pattern)

        // 4. Rehash and validate PII (self-exclusion)
        String normalizedEmail = piiNormalizer.normalizeEmail(pii.getEmail());
        String emailHash = hashingService.generateHash(normalizedEmail);
        pii.setEmailHash(emailHash);
        if (personPIIRepository.existsByEmailHashAndPersonPiiIdNot(emailHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.{ENTITY_CONSTANT}, PiiField.EMAIL);
        }

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber());
        String phoneHash = hashingService.generateHash(normalizedPhone);
        pii.setPhoneHash(phoneHash);
        if (personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(phoneHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.{ENTITY_CONSTANT}, PiiField.PHONE_NUMBER);
        }

        // 5. Update auth fields — DIRECT SETTER, no TypeMap needed
        //    Employee/Collaborator: existing.getInternalAuth().setRole(dto.getRole());
        //    Tutor: if (existing.getCustomerAuth() != null) { auth.setProvider(); auth.setToken(); }
        //    AdultStudent/MinorStudent: skip

        // 6. Validate cross-entity references (MinorStudent only)
        //    tutorRepository.findById(...).orElseThrow(...)

        // 7. Save and return manually-constructed response
        {entity}Repository.saveAndFlush(existing);

        {Entity}UpdateResponseDTO response = new {Entity}UpdateResponseDTO();
        response.set{Entity}Id({entity}Id);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }
}
```

**Key differences from creation use case**:
- No `transform()` method — update maps directly onto the existing loaded entity
- No `applicationContext.getBean()` — no new entities are created
- PII duplicate checks use `*AndPersonPiiIdNot` variants (self-exclusion)
- PII mapping uses **unnamed** void map (same as creation pattern: `modelMapper.map(dto, pii)`)
- Auth fields set via **direct setter** (no TypeMap needed for 1-2 fields)
- Response constructed manually (only 2 fields: entityId + message)
- `@Transactional` on the single `update()` method

### Step 2: Register Named TypeMap

**File**: `user-management/src/main/java/com/akademiaplus/config/PeopleModelMapperConfiguration.java`

Only **one** TypeMap per entity — the entity-level update map. PII mapping uses an unnamed auto-created TypeMap. Auth uses direct setters.

```java
private void register{Entity}UpdateMap() {
    modelMapper.createTypeMap(
            {Entity}UpdateRequestDTO.class,
            {Entity}DataModel.class,
            {Entity}UpdateUseCase.MAP_NAME
    ).addMappings(mapper -> {
        mapper.skip({Entity}DataModel::set{Entity}Id);
        mapper.skip({Entity}DataModel::setPersonPII);
        // skip auth setter:
        //   Employee/Collaborator: mapper.skip({Entity}DataModel::setInternalAuth);
        //   AdultStudent/Tutor: mapper.skip({Entity}DataModel::setCustomerAuth);
        //   MinorStudent: mapper.skip(::setCustomerAuth) + mapper.skip(::setTutor);
    }).implicitMappings();
}
```

Call it from `registerTypeMaps()` inside the `setImplicitMappingEnabled(false/true)` sandwich.

### Step 3: Wire into Controller

**File**: `user-management/src/main/java/com/akademiaplus/{entity-package}/interfaceadapters/{Entity}Controller.java`

1. Add `{Entity}UpdateUseCase` to constructor injection
2. Override the `update{Entity}()` method from the generated API interface:

```java
@Override
public ResponseEntity<{Entity}UpdateResponseDTO> update{Entity}(
        Long {entity}Id, {Entity}UpdateRequestDTO dto) {
    return ResponseEntity.ok({entity}UpdateUseCase.update({entity}Id, dto));
}
```

### Step 4: Create Unit Test

See Section 5 (Test Template) below.

---

## 4. Entity-Specific Variations

### Employee
- **Auth update**: `existing.getInternalAuth().setRole(dto.getRole())`
- **EntityType constant**: `EntityType.EMPLOYEE`
- **CompositeId class**: `EmployeeDataModel.EmployeeCompositeId`
- **Repository**: `EmployeeRepository`
- **TypeMap skips**: `setEmployeeId`, `setPersonPII`, `setInternalAuth`
- **Extra test**: Verify `setRole()` is called on InternalAuth

### Collaborator
- **Auth update**: `existing.getInternalAuth().setRole(dto.getRole())`
- **EntityType constant**: `EntityType.COLLABORATOR`
- **CompositeId class**: `CollaboratorDataModel.CollaboratorCompositeId`
- **Repository**: `CollaboratorRepository`
- **TypeMap skips**: `setCollaboratorId`, `setPersonPII`, `setInternalAuth`
- **Extra test**: Verify `setRole()` is called on InternalAuth

### AdultStudent (COMPLETED — reference implementation)
- **Auth update**: None — no auth fields in UpdateRequest
- **EntityType constant**: `EntityType.ADULT_STUDENT`
- **CompositeId class**: `AdultStudentDataModel.AdultStudentCompositeId`
- **Repository**: `AdultStudentRepository`
- **TypeMap skips**: `setAdultStudentId`, `setPersonPII`, `setCustomerAuth`
- **Files**:
  - `customer/adultstudent/usecases/AdultStudentUpdateUseCase.java`
  - `customer/adultstudent/usecases/AdultStudentUpdateUseCaseTest.java`

### Tutor
- **Auth update**: Direct setter on CustomerAuth if non-null:
  ```java
  CustomerAuthDataModel auth = existing.getCustomerAuth();
  if (auth != null) {
      auth.setProvider(dto.getProvider());
      auth.setToken(dto.getToken());
  }
  ```
- **EntityType constant**: `EntityType.TUTOR`
- **CompositeId class**: `TutorDataModel.TutorCompositeId`
- **Repository**: `TutorRepository` (at `com.akademiaplus.customer.interfaceadapters.TutorRepository`)
- **TypeMap skips**: `setTutorId`, `setPersonPII`, `setCustomerAuth`
- **Note**: `customerAuth` is `@OneToOne(optional = true)` — guard with null check
- **Extra test**: Verify provider/token set on auth when auth exists

### MinorStudent
- **Auth update**: None — no auth fields in UpdateRequest
- **Extra validation**: `tutorId` in UpdateRequest → validate tutor exists:
  ```java
  tutorRepository.findById(new TutorDataModel.TutorCompositeId(tenantId, dto.getTutorId()))
      .orElseThrow(() -> new EntityNotFoundException(EntityType.TUTOR, String.valueOf(dto.getTutorId())));
  ```
- **EntityType constant**: `EntityType.MINOR_STUDENT`
- **CompositeId class**: `MinorStudentDataModel.MinorStudentCompositeId`
- **Repository**: `MinorStudentRepository`
- **TypeMap skips**: `setMinorStudentId`, `setPersonPII`, `setCustomerAuth`, `setTutor`
- **Note**: `tutorId` (the FK column) IS mapped by the TypeMap via implicit matching. Only `setTutor` (the @OneToOne reference) is skipped.
- **Extra dependencies**: `TutorRepository` in constructor
- **Extra test**: Verify EntityNotFoundException(TUTOR) when tutorId references nonexistent tutor
- **Note**: MinorStudent creation is co-located in `TutorCreationUseCase`, but update is in its own `MinorStudentUpdateUseCase`

---

## 5. Test Template

**File**: `user-management/src/test/java/com/akademiaplus/{entity-package}/usecases/{Entity}UpdateUseCaseTest.java`

**Proven pattern** — see `AdultStudentUpdateUseCaseTest.java` for the reference implementation.

### Structure

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.{entity-package}.usecases;

// ... imports ...

@DisplayName("{Entity}UpdateUseCase")
@ExtendWith(MockitoExtension.class)
class {Entity}UpdateUseCaseTest {

    // ── Constants ──
    private static final Long TENANT_ID = 1L;
    private static final Long {ENTITY}_ID = 42L;
    private static final Long PERSON_PII_ID = 99L;
    private static final String TEST_EMAIL = "jdoe@example.com";
    private static final String TEST_PHONE = "5551234567";
    private static final String NORMALIZED_EMAIL = "jdoe@example.com";
    private static final String NORMALIZED_PHONE = "+525551234567";
    private static final String EMAIL_HASH = "hashed_email";
    private static final String PHONE_HASH = "hashed_phone";

    // ── Mocks ──
    @Mock private {Entity}Repository {entity}Repository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;
    @Mock private PiiNormalizer piiNormalizer;
    @Mock private HashingService hashingService;

    private {Entity}UpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new {Entity}UpdateUseCase(
                {entity}Repository, personPIIRepository,
                tenantContextHolder, modelMapper,
                piiNormalizer, hashingService);
    }

    // ── Helpers ──

    private {Entity}UpdateRequestDTO buildDto() {
        {Entity}UpdateRequestDTO dto = new {Entity}UpdateRequestDTO();
        // Set all updatable fields to test values
        dto.setBirthdate(LocalDate.of(1990, 1, 15));
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail(TEST_EMAIL);
        dto.setPhoneNumber(TEST_PHONE);
        dto.setAddress("123 Main St");
        dto.setZipCode("12345");
        // + entity-specific fields (employeeType, skills, role, tutorId, etc.)
        return dto;
    }

    private PersonPIIDataModel buildPersonPII() {
        PersonPIIDataModel pii = new PersonPIIDataModel();
        pii.setPersonPiiId(PERSON_PII_ID);
        pii.setEmail(TEST_EMAIL);
        pii.setPhoneNumber(TEST_PHONE);
        return pii;
    }

    private {Entity}DataModel buildExistingEntity(PersonPIIDataModel pii) {
        {Entity}DataModel entity = new {Entity}DataModel();
        entity.setPersonPII(pii);
        // For Employee/Collaborator: entity.setInternalAuth(new InternalAuthDataModel());
        // For Tutor: entity.setCustomerAuth(new CustomerAuthDataModel());
        return entity;
    }

    private void stubPiiRehash(PersonPIIDataModel pii) {
        when(piiNormalizer.normalizeEmail(pii.getEmail())).thenReturn(NORMALIZED_EMAIL);
        when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
        when(piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber())).thenReturn(NORMALIZED_PHONE);
        when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
    }

    // ── Test Groups ──

    @Nested
    @DisplayName("Entity Lookup")
    class EntityLookup {
        @Test
        @DisplayName("Should throw EntityNotFoundException when {entity} does not exist")
        void shouldThrowEntityNotFoundException_when{Entity}DoesNotExist() {
            // Given: stub tenantId + empty findById
            // When & Then: assertThatThrownBy → EntityNotFoundException
        }
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {
        @Test
        @DisplayName("Should update entity and return response when {entity} exists")
        void shouldUpdateEntityAndReturnResponse_when{Entity}Exists() {
            // Given: full happy-path stubs
            // When: useCase.update(...)
            // Then: verify saveAndFlush, assert response.get{Entity}Id() and response.getMessage()
        }
    }

    @Nested
    @DisplayName("PII Update")
    class PiiUpdate {
        @Test
        @DisplayName("Should rehash email and phone when PII fields are updated")
        void shouldRehashEmailAndPhone_whenPiiFieldsAreUpdated() {
            // Given: full stubs
            // When: useCase.update(...)
            // Then: verify normalizeEmail, normalizePhoneNumber, generateHash calls
        }
    }

    @Nested
    @DisplayName("Duplicate Validation")
    class DuplicateValidation {
        // Two tests: email duplicate + phone duplicate (see AdultStudentUpdateUseCaseTest)
    }

    // ── Entity-Specific Test Groups ──
    // Employee/Collaborator: @Nested "Auth Update" — verify role set on InternalAuth
    // Tutor: @Nested "Auth Update" — verify provider/token set on CustomerAuth
    // MinorStudent: @Nested "Tutor Validation" — verify EntityNotFoundException(TUTOR) for invalid tutorId
}
```

### Key stubbing patterns (from working AdultStudent implementation)

```java
// Named TypeMap (void 3-arg map) — entity-level fields
doNothing().when(modelMapper).map(dto, existing, {Entity}UpdateUseCase.MAP_NAME);

// Unnamed void map — PII fields
doNothing().when(modelMapper).map(dto, pii);

// Response is constructed manually, no ModelMapper stub needed for response mapping
```

---

## 6. Execution Prompt

> **Separate document**: [`docs/prompts/completed/update-usecase-prompt.md`](../prompts/completed/update-usecase-prompt.md)
>
> The prompt contains phased step-by-step execution instructions (AdultStudent → Employee → Collaborator → Tutor → MinorStudent), verification gates, and the conventions checklist.

---

## 7. Reference File Paths

### Entity Classes (multi-tenant-data)
| Entity | Path |
|--------|------|
| AbstractUser | `multi-tenant-data/.../users/base/AbstractUser.java` |
| PersonPIIDataModel | `multi-tenant-data/.../users/base/PersonPIIDataModel.java` |
| EmployeeDataModel | `multi-tenant-data/.../users/employee/EmployeeDataModel.java` |
| CollaboratorDataModel | `multi-tenant-data/.../users/collaborator/CollaboratorDataModel.java` |
| AdultStudentDataModel | `multi-tenant-data/.../users/customer/AdultStudentDataModel.java` |
| TutorDataModel | `multi-tenant-data/.../users/customer/TutorDataModel.java` |
| MinorStudentDataModel | `multi-tenant-data/.../users/customer/MinorStudentDataModel.java` |
| InternalAuthDataModel | `multi-tenant-data/.../security/InternalAuthDataModel.java` |
| CustomerAuthDataModel | `multi-tenant-data/.../security/CustomerAuthDataModel.java` |

### OpenAPI Specs
| Entity | Path |
|--------|------|
| Employee | `user-management/src/main/resources/openapi/employee-api.yaml` |
| Collaborator | `user-management/src/main/resources/openapi/collaborator-api.yaml` |
| AdultStudent | `user-management/src/main/resources/openapi/adult-student-api.yaml` |
| Tutor | `user-management/src/main/resources/openapi/tutor-api.yaml` |
| MinorStudent | `user-management/src/main/resources/openapi/minor-student-api.yaml` |
| Base schemas | `user-management/src/main/resources/openapi/base-components.yaml` |

### Completed Implementation (reference template)
| File | Path |
|------|------|
| AdultStudentUpdateUseCase | `user-management/.../customer/adultstudent/usecases/AdultStudentUpdateUseCase.java` |
| AdultStudentUpdateUseCaseTest | `user-management/.../customer/adultstudent/usecases/AdultStudentUpdateUseCaseTest.java` |

### Creation Use Cases (pattern reference)
| Entity | UseCase |
|--------|---------|
| Employee | `user-management/.../employee/usecases/EmployeeCreationUseCase.java` |
| Collaborator | `user-management/.../collaborator/usecases/CollaboratorCreationUseCase.java` |
| AdultStudent | `user-management/.../customer/adultstudent/usecases/AdultStudentCreationUseCase.java` |
| Tutor + MinorStudent | `user-management/.../customer/tutor/usecases/TutorCreationUseCase.java` |

### Controllers
| Entity | Path |
|--------|------|
| Employee | `user-management/.../employee/interfaceadapters/EmployeeController.java` |
| Collaborator | `user-management/.../collaborator/interfaceadapters/CollaboratorController.java` |
| AdultStudent | `user-management/.../customer/adultstudent/interfaceadapters/AdultStudentController.java` |
| Tutor | `user-management/.../customer/tutor/interfaceadapters/TutorController.java` |
| MinorStudent | `user-management/.../customer/minorstudent/interfaceadapters/MinorStudentController.java` |

### Shared Infrastructure
| File | Path |
|------|------|
| PeopleModelMapperConfiguration | `user-management/.../config/PeopleModelMapperConfiguration.java` |
| PersonPIIRepository | `user-management/.../interfaceadapters/PersonPIIRepository.java` |
| TutorRepository | `user-management/.../customer/interfaceadapters/TutorRepository.java` |
| EntityType | `utilities/.../utilities/EntityType.java` |
| PiiField | `utilities/.../utilities/PiiField.java` |
| EntityNotFoundException | `utilities/.../utilities/exceptions/EntityNotFoundException.java` |
| DuplicateEntityException | `utilities/.../utilities/exceptions/DuplicateEntityException.java` |
| HashingService | `utilities/.../utilities/security/HashingService.java` |
| PiiNormalizer | `utilities/.../utilities/security/PiiNormalizer.java` |
| TenantContextHolder | `infra-common/.../infra/persistence/config/TenantContextHolder.java` |

---

## 8. Implementation Order

Recommended order (simplest → most complex):

1. ~~**PersonPIIRepository** — add self-exclusion methods~~ DONE
2. ~~**AdultStudent** — simplest: no auth fields~~ DONE
3. **Employee** — adds `role` auth field + `employeeType`, `entryDate`
4. **Collaborator** — mirrors Employee with `skills` instead of `employeeType`
5. **Tutor** — adds `provider`/`token` auth fields, `customerAuth` is optional
6. **MinorStudent** — adds `tutorId` validation (must verify tutor exists)
