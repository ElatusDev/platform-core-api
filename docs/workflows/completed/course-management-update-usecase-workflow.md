# Update Use Case Workflow — Course-Management Entities

> Reusable workflow for implementing PUT (update) endpoints for all 3 course-management entities.
> Each entity follows the same structural pattern with entity-specific field variations.
>
> **Status**: OpenAPI specs DONE. Implementation: pending.

---

## 1. Field Registry

Fields derived from OpenAPI `*UpdateRequest` schemas in `course-management/src/main/resources/openapi/`.

### 1.1 Per-Entity Updatable Fields

| Entity | Updatable Fields | Immutable | FK References (require validation) |
|--------|-----------------|-----------|-----------------------------------|
| **Course** | `name`, `description`, `maxCapacity` | `courseId`, `tenantId`, `createdAt` | `timeTableIds` (schedules), `availableCollaboratorIds` (collaborators) |
| **Schedule** | `scheduleDay`, `startTime`, `endTime` | `scheduleId`, `tenantId`, `createdAt` | `courseId` (course) |
| **CourseEvent** | `title`, `description` | `courseEventId`, `tenantId`, `createdAt`, `eventDate` (JPA `updatable=false`) | `scheduleId` (schedule), `courseId` (course), `instructorId` (collaborator), `adultAttendeeIds`, `minorAttendeeIds` |

### 1.2 Key Differences from User-Management Updates

- **No PII/Auth layers** — entities have direct fields, no `PersonPIIDataModel` or auth models
- **No duplicate validation** — no email/phone hashing or self-exclusion checks
- **FK validation required** — referenced entities (courses, schedules, collaborators) must exist
- **Many-to-Many collections** — Course has `availableCollaborators`, CourseEvent has `adultAttendees`/`minorAttendees`
- **Schedule availability** — during Course update, newly assigned schedules must not already belong to another course (reuse `CourseValidator.validateSchedulesAvailable()` with adaptation for update context)

### 1.3 Response DTO Pattern

All `*UpdateResponseDTO` classes have two fields: `{entity}Id` (Long) and `message` (String).
Construct the response **manually** (no ModelMapper).

```java
{Entity}UpdateResponseDTO response = new {Entity}UpdateResponseDTO();
response.set{Entity}Id({entity}Id);
response.setMessage(UPDATE_SUCCESS_MESSAGE);
return response;
```

### 1.4 Collection Handling

For entities with Many-to-Many relationships:

- **Course `availableCollaborators`**: Validate all collaborator IDs exist (via `CourseValidator.validateCollaboratorsExist()`), then replace the collection: `existing.setAvailableCollaborators(validatedCollaborators)`
- **Course `schedules` (timeTableIds)**: Schedules are linked via FK on the Schedule side (`schedule.courseId`). Update requires: (a) unlink old schedules not in new list, (b) validate & link new schedules. Consider reusing/adapting `CourseValidator.validateSchedulesAvailable()` — but for updates, schedules already assigned to THIS course are still valid.
- **CourseEvent `adultAttendees`/`minorAttendees`**: Validate attendee IDs exist, then replace collections.

---

## 2. Implementation Steps (Per Entity)

Follow these steps for each entity, substituting `{Entity}` with the concrete name.

### Step 1: Create `{Entity}UpdateUseCase.java`

**Location**: `course-management/src/main/java/com/akademiaplus/{entity-package}/usecases/{Entity}UpdateUseCase.java`

**Package mapping**:
| Entity | Package |
|--------|---------|
| Course | `com.akademiaplus.program.usecases` |
| Schedule | `com.akademiaplus.program.usecases` |
| CourseEvent | `com.akademiaplus.event.usecases` |

**Canonical structure**:

```java
@Service
@RequiredArgsConstructor
public class {Entity}UpdateUseCase {

    public static final String MAP_NAME = "{entity}UpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "{Entity} updated successfully";

    private final {Entity}Repository {entity}Repository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;
    // + entity-specific repositories for FK validation

    @Transactional
    public {Entity}UpdateResponseDTO update(Long {entity}Id, {Entity}UpdateRequestDTO dto) {

        // 1. Load existing entity by composite key
        Long tenantId = tenantContextHolder.requireTenantId();
        {Entity}DataModel existing = {entity}Repository
                .findById(new {Entity}DataModel.{Entity}CompositeId(tenantId, {entity}Id))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.{ENTITY_CONSTANT}, String.valueOf({entity}Id)));

        // 2. Map simple fields via named TypeMap (skips ID + nested objects)
        modelMapper.map(dto, existing, MAP_NAME);

        // 3. Validate FK references and update relationships
        //    (entity-specific — see Section 3)

        // 4. Save and return manually-constructed response
        {entity}Repository.saveAndFlush(existing);

        {Entity}UpdateResponseDTO response = new {Entity}UpdateResponseDTO();
        response.set{Entity}Id({entity}Id);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }
}
```

**Key differences from creation use case**:
- No `transform()` method — maps directly onto the loaded entity
- No `applicationContext.getBean()` — no new entities are created
- `@Transactional` on the single `update()` method
- Response constructed manually (only 2 fields: entityId + message)
- Uses `TenantContextHolder.requireTenantId()` (not `getTenantId().orElseThrow()`)
- Throws `EntityNotFoundException` (not `IllegalArgumentException`) for missing entities

### Step 2: Register Named TypeMap

**File**: `course-management/src/main/java/com/akademiaplus/config/CoordinationModelMapperConfiguration.java`

Add update TypeMaps inside the existing `setImplicitMappingEnabled(false/true)` sandwich.

```java
private void register{Entity}UpdateMap() {
    modelMapper.createTypeMap(
            {Entity}UpdateRequestDTO.class,
            {Entity}DataModel.class,
            {Entity}UpdateUseCase.MAP_NAME
    ).addMappings(mapper -> {
        mapper.skip({Entity}DataModel::set{Entity}Id);
        // skip all relationship setters (entity-specific)
    }).implicitMappings();
}
```

### Step 3: Wire into Controller

**File**: `course-management/src/main/java/com/akademiaplus/{entity-package}/interfaceadapters/{Entity}Controller.java`

1. Add `{Entity}UpdateUseCase` to constructor injection
2. Override the generated `update{Entity}()` method:

```java
@Override
public ResponseEntity<{Entity}UpdateResponseDTO> update{Entity}(
        Long {entity}Id, {Entity}UpdateRequestDTO dto) {
    return ResponseEntity.ok({entity}UpdateUseCase.update({entity}Id, dto));
}
```

### Step 4: Create Unit Test

See Section 4 (Test Template) below.

---

## 3. Entity-Specific Variations

### Course

- **EntityType constant**: `EntityType.COURSE`
- **CompositeId class**: `CourseDataModel.CourseCompositeId`
- **Repository**: `CourseRepository`
- **TypeMap skips**: `setCourseId`, `setSchedules`, `setAvailableCollaborators`
- **FK validation**:
  - `availableCollaboratorIds` → validate via `CourseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds())` → `existing.setAvailableCollaborators(validatedCollaborators)`
  - `timeTableIds` → handle schedule reassignment:
    1. Clear old schedule associations (set `courseId = null` on schedules no longer in list)
    2. Validate new schedules are available (not assigned to ANOTHER course — schedules already on THIS course are fine)
    3. Assign new schedules (`schedule.setCourseId(courseId)`)
- **Extra dependencies**: `CourseValidator`, `ScheduleRepository`
- **Extra tests**: Collaborator validation, schedule reassignment

### Schedule

- **EntityType constant**: `EntityType.SCHEDULE`
- **CompositeId class**: `ScheduleDataModel.ScheduleCompositeId`
- **Repository**: `ScheduleRepository`
- **TypeMap skips**: `setScheduleId`, `setCourse`
- **FK validation**:
  - `courseId` → validate course exists via `CourseRepository.findById()`
  - Note: `courseId` is a writable FK column mapped by the TypeMap. Only `setCourse` (the JPA relationship object) is skipped.
- **Extra dependencies**: `CourseRepository`
- **Extra test**: Verify EntityNotFoundException(COURSE) when courseId references nonexistent course

### CourseEvent

- **EntityType constant**: `EntityType.COURSE_EVENT`
- **CompositeId class**: `CourseEventDataModel.CourseEventCompositeId`
- **Repository**: `CourseEventRepository`
- **TypeMap skips**: `setCourseEventId`, `setCourse`, `setCollaborator`, `setSchedule`, `setAdultAttendees`, `setMinorAttendees`
- **TypeMap custom mappings** (field name differences DTO → entity):
  - `getTitle()` → `setEventTitle()`
  - `getDescription()` → `setEventDescription()`
- **Note**: `eventDate` is immutable (`updatable=false` in JPA, excluded from UpdateRequest DTO)
- **FK validation**:
  - `courseId` → validate course exists via `CourseRepository.findById()`
  - `instructorId` → validate collaborator exists via `CollaboratorRepository.findById()` → `existing.setCollaboratorId(dto.getInstructorId())`
  - `scheduleId` → validate schedule exists via `ScheduleRepository.findById()`
  - `adultAttendeeIds` / `minorAttendeeIds` → validate attendees exist, replace collections
- **Extra dependencies**: `CourseRepository`, `ScheduleRepository`, `CollaboratorRepository`, `AdultStudentRepository`, `MinorStudentRepository`
- **Extra tests**: Verify EntityNotFoundException for each missing FK reference

---

## 4. Test Template

**File**: `course-management/src/test/java/com/akademiaplus/{entity-package}/usecases/{Entity}UpdateUseCaseTest.java`

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

    // -- Constants --
    private static final Long TENANT_ID = 1L;
    private static final Long {ENTITY}_ID = 42L;
    // + entity-specific constants

    // -- Mocks --
    @Mock private {Entity}Repository {entity}Repository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;
    // + entity-specific mocks (CourseValidator, other repositories)

    private {Entity}UpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new {Entity}UpdateUseCase(/* constructor args */);
    }

    // -- Helpers --
    // buildDto(), buildExistingEntity(), etc.

    // -- Test Groups --

    @Nested
    @DisplayName("Entity Lookup")
    class EntityLookup {
        @Test
        @DisplayName("Should throw EntityNotFoundException when {entity} does not exist")
        void shouldThrowEntityNotFoundException_when{Entity}DoesNotExist() {
            // Given: stub tenantId + empty findById
            // When & Then: assertThatThrownBy -> EntityNotFoundException
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

    // -- Entity-Specific Test Groups --
    // Course: @Nested "Collaborator Validation", "Schedule Reassignment"
    // Schedule: @Nested "Course Validation"
    // CourseEvent: @Nested "FK Validation" (course, collaborator, schedule, attendees)
}
```

### Key stubbing patterns

```java
// Named TypeMap (void 3-arg map)
doNothing().when(modelMapper).map(dto, existing, {Entity}UpdateUseCase.MAP_NAME);

// Response is constructed manually, no ModelMapper stub needed for response mapping
```

---

## 5. Execution Prompt

> **Separate document**: [`docs/prompts/completed/course-management-update-usecase-prompt.md`](../../prompts/completed/course-management-update-usecase-prompt.md)
>
> The prompt contains phased step-by-step execution instructions (Schedule → Course → CourseEvent), verification gates, and the conventions checklist.

---

## 6. Reference File Paths

### Entity Classes (multi-tenant-data)
| Entity | Path |
|--------|------|
| CourseDataModel | `multi-tenant-data/.../courses/program/CourseDataModel.java` |
| ScheduleDataModel | `multi-tenant-data/.../courses/program/ScheduleDataModel.java` |
| CourseEventDataModel | `multi-tenant-data/.../courses/event/CourseEventDataModel.java` |
| AbstractEvent | `multi-tenant-data/.../courses/event/AbstractEvent.java` |

### OpenAPI Specs
| Entity | Path |
|--------|------|
| Course | `course-management/src/main/resources/openapi/course.yaml` |
| Schedule | `course-management/src/main/resources/openapi/schedule.yaml` |
| CourseEvent | `course-management/src/main/resources/openapi/course-event.yaml` |
| Module aggregator | `course-management/src/main/resources/openapi/course-management-module.yaml` |

### Creation Use Cases (pattern reference)
| Entity | Path |
|--------|------|
| Course | `course-management/.../program/usecases/CreateCourseUseCase.java` |
| Schedule | `course-management/.../program/usecases/ScheduleCreationUseCase.java` |
| CourseEvent | `course-management/.../event/usecases/CourseEventCreationUseCase.java` |

### Controllers
| Entity | Path |
|--------|------|
| Course | `course-management/.../program/interfaceadapters/CourseController.java` |
| Schedule | `course-management/.../program/interfaceadapters/ScheduleController.java` |
| CourseEvent | `course-management/.../event/interfaceadapters/CourseEventController.java` |

### Shared Infrastructure
| File | Path |
|------|------|
| CoordinationModelMapperConfiguration | `course-management/.../config/CoordinationModelMapperConfiguration.java` |
| CourseValidator | `course-management/.../program/application/CourseValidator.java` |
| CourseRepository | `course-management/.../program/interfaceadapters/CourseRepository.java` |
| ScheduleRepository | `course-management/.../program/interfaceadapters/ScheduleRepository.java` |
| CourseEventRepository | `course-management/.../event/interfaceadapters/CourseEventRepository.java` |
| CollaboratorRepository | `user-management/.../collaborator/interfaceadapters/CollaboratorRepository.java` |
| EntityType | `utilities/.../utilities/EntityType.java` |
| EntityNotFoundException | `utilities/.../utilities/exceptions/EntityNotFoundException.java` |
| TenantContextHolder | `infra-common/.../infra/persistence/config/TenantContextHolder.java` |

---

## 7. Implementation Order

Recommended order (simplest -> most complex):

1. **Schedule** — simplest: 4 direct fields + 1 FK (courseId)
2. **Course** — medium: 3 direct fields + M2M collaborators + schedule reassignment
3. **CourseEvent** — most complex: 2 direct fields + 3 FK refs + 2 M2M attendee collections + custom TypeMap mappings
