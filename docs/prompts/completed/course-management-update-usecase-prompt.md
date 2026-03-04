# Course-Management Update UseCase Rollout — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Workflow**: `docs/workflows/pending/course-management-update-usecase-workflow.md`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and the workflow document before starting.
**Dependency**: OpenAPI update specs for all 3 entities MUST already exist (Course, Schedule, CourseEvent).

---

## EXECUTION RULES

1. Execute entities **strictly in order**: Schedule → Course → CourseEvent.
2. Do NOT skip ahead. Each entity must compile and all tests pass before starting the next.
3. After EACH entity, run the verification command. Fix failures before proceeding.
4. All new files MUST include the ElatusDev 2025 copyright header.
5. Test methods: `shouldDoX_whenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
6. Read existing DataModel files and creation use cases BEFORE writing any update use case.
7. Refer to the workflow document for field registries, entity-specific variations, and TypeMap patterns.

---

## Phase 1: Schedule Update (simplest — 4 direct fields + 1 FK)

### Step 1.1: Read reference files

```
Read these files for context:
- multi-tenant-data/src/main/java/com/akademiaplus/courses/program/ScheduleDataModel.java
- course-management/src/main/java/com/akademiaplus/program/usecases/ScheduleCreationUseCase.java
- course-management/src/main/java/com/akademiaplus/config/CoordinationModelMapperConfiguration.java
- course-management/src/main/java/com/akademiaplus/program/interfaceadapters/ScheduleController.java
- course-management/src/main/java/com/akademiaplus/program/interfaceadapters/ScheduleRepository.java
- course-management/src/main/java/com/akademiaplus/program/interfaceadapters/CourseRepository.java
- course-management/src/main/resources/openapi/schedule.yaml
```

### Step 1.2: Create `ScheduleUpdateUseCase.java`

**Location**: `course-management/src/main/java/com/akademiaplus/program/usecases/ScheduleUpdateUseCase.java`

Follow the workflow document Section 2 canonical structure:
- Load by composite key (`ScheduleDataModel.ScheduleCompositeId`)
- Map via named TypeMap (`scheduleUpdateMap`)
- Validate `courseId` FK: `CourseRepository.findById()` → throw `EntityNotFoundException(COURSE)` if missing
- Save and return manually-constructed `ScheduleUpdateResponseDTO`
- TypeMap skips: `setScheduleId`, `setCourse`
- `courseId` FK column IS mapped by TypeMap (only `setCourse` JPA ref is skipped)

### Step 1.3: Register TypeMap

**File**: `CoordinationModelMapperConfiguration.java`

Add `registerScheduleUpdateMap()` inside the `setImplicitMappingEnabled(false/true)` sandwich.

### Step 1.4: Wire into Controller

**File**: `ScheduleController.java`

Add `ScheduleUpdateUseCase` to constructor and override `updateSchedule()`.

### Step 1.5: Create `ScheduleUpdateUseCaseTest.java`

**Location**: `course-management/src/test/java/com/akademiaplus/program/usecases/ScheduleUpdateUseCaseTest.java`

Required test groups:
- `@Nested EntityLookup` — 404 when schedule not found
- `@Nested CourseValidation` — 404 when courseId references nonexistent course
- `@Nested SuccessfulUpdate` — happy path: verify `saveAndFlush`, assert response fields

Stubbing patterns:
- `doNothing().when(modelMapper).map(dto, existing, ScheduleUpdateUseCase.MAP_NAME)`
- Response is manually constructed — no ModelMapper stub for response

### Step 1.6: Verify

```bash
mvn clean test -f platform-core-api/pom.xml -pl course-management -am
```

All tests must pass. Fix any failures before proceeding.

---

## Phase 2: Course Update (medium — 3 direct fields + M2M collaborators + schedule reassignment)

### Step 2.1: Read reference files

```
Read these files for context:
- multi-tenant-data/src/main/java/com/akademiaplus/courses/program/CourseDataModel.java
- course-management/src/main/java/com/akademiaplus/program/usecases/CreateCourseUseCase.java
- course-management/src/main/java/com/akademiaplus/program/application/CourseValidator.java
- course-management/src/main/java/com/akademiaplus/program/interfaceadapters/CourseController.java
- course-management/src/main/resources/openapi/course.yaml
```

### Step 2.2: Create `CourseUpdateUseCase.java`

**Location**: `course-management/src/main/java/com/akademiaplus/program/usecases/CourseUpdateUseCase.java`

Follow workflow document Section 2 + Section 3 (Course-specific):
- Load by composite key (`CourseDataModel.CourseCompositeId`)
- Map simple fields via named TypeMap (`courseUpdateMap`)
- TypeMap skips: `setCourseId`, `setSchedules`, `setAvailableCollaborators`
- Validate and update collaborators: `CourseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds())` → `existing.setAvailableCollaborators(validatedCollaborators)`
- Handle schedule reassignment:
  1. Clear schedules no longer in `timeTableIds` (set `courseId = null`)
  2. Validate new schedules are available (not assigned to another course — already-assigned to THIS course is fine)
  3. Assign new schedules (set `courseId` to this course's ID)
- Save and return manually-constructed `CourseUpdateResponseDTO`

### Step 2.3: Register TypeMap

**File**: `CoordinationModelMapperConfiguration.java`

Add `registerCourseUpdateMap()` inside the `setImplicitMappingEnabled(false/true)` sandwich.

### Step 2.4: Wire into Controller

**File**: `CourseController.java`

Add `CourseUpdateUseCase` to constructor and override `updateCourse()`.

### Step 2.5: Create `CourseUpdateUseCaseTest.java`

**Location**: `course-management/src/test/java/com/akademiaplus/program/usecases/CourseUpdateUseCaseTest.java`

Required test groups:
- `@Nested EntityLookup` — 404 when course not found
- `@Nested CollaboratorValidation` — verify collaborator validation is called
- `@Nested ScheduleReassignment` — verify schedule reassignment logic
- `@Nested SuccessfulUpdate` — happy path

### Step 2.6: Verify

```bash
mvn clean test -f platform-core-api/pom.xml -pl course-management -am
```

All tests must pass. Fix any failures before proceeding.

---

## Phase 3: CourseEvent Update (most complex — 2 direct fields + 3 FKs + 2 M2M collections + custom TypeMap)

### Step 3.1: Read reference files

```
Read these files for context:
- multi-tenant-data/src/main/java/com/akademiaplus/courses/event/CourseEventDataModel.java
- multi-tenant-data/src/main/java/com/akademiaplus/courses/event/AbstractEvent.java
- course-management/src/main/java/com/akademiaplus/event/usecases/CourseEventCreationUseCase.java
- course-management/src/main/java/com/akademiaplus/event/interfaceadapters/CourseEventController.java
- course-management/src/main/java/com/akademiaplus/event/interfaceadapters/CourseEventRepository.java
- course-management/src/main/resources/openapi/course-event.yaml
```

### Step 3.2: Create `CourseEventUpdateUseCase.java`

**Location**: `course-management/src/main/java/com/akademiaplus/event/usecases/CourseEventUpdateUseCase.java`

Follow workflow document Section 2 + Section 3 (CourseEvent-specific):
- Load by composite key (`CourseEventDataModel.CourseEventCompositeId`)
- Map simple fields via named TypeMap (`courseEventUpdateMap`)
- TypeMap skips: `setCourseEventId`, `setCourse`, `setCollaborator`, `setSchedule`, `setAdultAttendees`, `setMinorAttendees`
- TypeMap custom mappings: `getTitle()` → `setEventTitle()`, `getDescription()` → `setEventDescription()`
- **`eventDate` is NOT updatable** (excluded from DTO, `updatable=false` in JPA)
- Validate all FK references:
  - `courseId` → `CourseRepository.findById()` → EntityNotFoundException(COURSE)
  - `instructorId` → `CollaboratorRepository.findById()` → EntityNotFoundException(COLLABORATOR) → `existing.setCollaboratorId(dto.getInstructorId())`
  - `scheduleId` → `ScheduleRepository.findById()` → EntityNotFoundException(SCHEDULE)
  - `adultAttendeeIds` → validate existence, replace collection
  - `minorAttendeeIds` → validate existence, replace collection
- Save and return manually-constructed `CourseEventUpdateResponseDTO`

### Step 3.3: Register TypeMap

**File**: `CoordinationModelMapperConfiguration.java`

Add `registerCourseEventUpdateMap()` inside the `setImplicitMappingEnabled(false/true)` sandwich.

### Step 3.4: Wire into Controller

**File**: `CourseEventController.java`

Add `CourseEventUpdateUseCase` to constructor and override `updateCourseEvent()`.

### Step 3.5: Create `CourseEventUpdateUseCaseTest.java`

**Location**: `course-management/src/test/java/com/akademiaplus/event/usecases/CourseEventUpdateUseCaseTest.java`

Required test groups:
- `@Nested EntityLookup` — 404 when course event not found
- `@Nested CourseValidation` — 404 when courseId references nonexistent course
- `@Nested InstructorValidation` — 404 when instructorId references nonexistent collaborator
- `@Nested ScheduleValidation` — 404 when scheduleId references nonexistent schedule
- `@Nested AttendeeValidation` — verify attendee validation and collection replacement
- `@Nested SuccessfulUpdate` — happy path

### Step 3.6: Verify

```bash
mvn clean test -f platform-core-api/pom.xml -pl course-management -am
```

All tests must pass.

---

## Final Verification

Run the full course-management test suite:

```bash
mvn clean test -f platform-core-api/pom.xml -pl course-management -am
```

Confirm all tests pass (existing + new). Report the final test count.

---

## Critical Conventions Checklist

- [ ] Copyright header on all new files (ElatusDev 2025)
- [ ] `@Transactional` only on the `update()` method
- [ ] `@Service` + `@RequiredArgsConstructor` on use case
- [ ] Named TypeMap constant: `public static final String MAP_NAME = "{entity}UpdateMap"`
- [ ] Success message constant: `public static final String UPDATE_SUCCESS_MESSAGE = "..."`
- [ ] Use `EntityNotFoundException` (not `IllegalArgumentException`) for missing entities
- [ ] Use `TenantContextHolder.requireTenantId()` (not `getTenantId().orElseThrow()`)
- [ ] Response constructed manually (no ModelMapper) — set entityId + message
- [ ] All string literals → `public static final` constants
- [ ] Tests: Given-When-Then comments, `@DisplayName` on all `@Test` and `@Nested`
- [ ] Tests: ZERO `any()` matchers — stub with exact values
- [ ] Tests: `shouldDoX_whenY()` naming
- [ ] Tests: `doNothing().when(modelMapper).map(dto, model, MAP_NAME)` for void ModelMapper
- [ ] No `new Entity()` in production code — but OK in tests
- [ ] IDs are always `Long`, never `Integer`
- [ ] Commit: Conventional Commits format, NO AI attribution
