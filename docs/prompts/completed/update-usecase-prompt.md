# User-Management Update UseCase Rollout — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Workflow**: `docs/workflows/update-usecase-workflow.md`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and the workflow document before starting.
**Dependency**: `PersonPIIRepository` self-exclusion methods must already exist (`existsByEmailHashAndPersonPiiIdNot`, `existsByPhoneHashAndPersonPiiIdNot`).

---

## EXECUTION RULES

1. Execute entities **strictly in order**: AdultStudent (reference) → Employee → Collaborator → Tutor → MinorStudent.
2. Do NOT skip ahead. Each entity must compile and all tests pass before starting the next.
3. After EACH entity, run the verification command. Fix failures before proceeding.
4. All new files MUST include the ElatusDev 2025 copyright header.
5. Test methods: `shouldDoX_whenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
6. Read existing DataModel files and creation use cases BEFORE writing any update use case.
7. Refer to the workflow document for field registries, entity-specific variations, auth model mapping, and PII rehashing rules.

---

## Phase 0: Prerequisites (COMPLETED)

- PersonPIIRepository self-exclusion methods — DONE
- AdultStudent reference implementation — DONE

---

## Phase 1: Employee Update

### Step 1.1: Read reference files

```
Read these files for context:
- user-management/src/main/java/com/akademiaplus/customer/adultstudent/usecases/AdultStudentUpdateUseCase.java (TEMPLATE)
- user-management/src/test/java/com/akademiaplus/customer/adultstudent/usecases/AdultStudentUpdateUseCaseTest.java (TEMPLATE)
- multi-tenant-data/src/main/java/com/akademiaplus/users/employee/EmployeeDataModel.java
- multi-tenant-data/src/main/java/com/akademiaplus/security/InternalAuthDataModel.java
- user-management/src/main/java/com/akademiaplus/employee/usecases/EmployeeCreationUseCase.java
- user-management/src/main/java/com/akademiaplus/config/PeopleModelMapperConfiguration.java
- user-management/src/main/java/com/akademiaplus/employee/interfaceadapters/EmployeeController.java
- user-management/src/main/resources/openapi/employee-api.yaml
```

### Step 1.2: Create `EmployeeUpdateUseCase.java`

**Location**: `user-management/src/main/java/com/akademiaplus/employee/usecases/EmployeeUpdateUseCase.java`

Follow workflow document Section 3 + Section 4 (Employee-specific):
- Same structure as AdultStudentUpdateUseCase
- Add auth update: `existing.getInternalAuth().setRole(dto.getRole())`
- TypeMap skips: `setEmployeeId`, `setPersonPII`, `setInternalAuth`

### Step 1.3: Register TypeMap in `PeopleModelMapperConfiguration.java`

### Step 1.4: Wire into `EmployeeController.java`

### Step 1.5: Create `EmployeeUpdateUseCaseTest.java`

Required test groups:
- `@Nested EntityLookup` — 404 when employee not found
- `@Nested SuccessfulUpdate` — happy path
- `@Nested AuthUpdate` — verify `setRole()` is called on InternalAuth
- `@Nested PiiUpdate` — verify email/phone rehashing
- `@Nested DuplicateValidation` — email duplicate + phone duplicate

### Step 1.6: Verify

```bash
mvn clean test -f platform-core-api/pom.xml -pl user-management -am
```

---

## Phase 2: Collaborator Update

Mirrors Employee. Same structure with `skills` instead of `employeeType`.

### Step 2.1: Read reference files (CollaboratorDataModel, CollaboratorCreationUseCase, collaborator-api.yaml)

### Step 2.2: Create `CollaboratorUpdateUseCase.java`

- Auth update: `existing.getInternalAuth().setRole(dto.getRole())`
- TypeMap skips: `setCollaboratorId`, `setPersonPII`, `setInternalAuth`

### Step 2.3-2.4: Register TypeMap + Wire Controller

### Step 2.5: Create `CollaboratorUpdateUseCaseTest.java` (same groups as Employee)

### Step 2.6: Verify

```bash
mvn clean test -f platform-core-api/pom.xml -pl user-management -am
```

---

## Phase 3: Tutor Update

### Step 3.1: Read reference files (TutorDataModel, CustomerAuthDataModel, tutor-api.yaml)

### Step 3.2: Create `TutorUpdateUseCase.java`

- Auth update with null guard (CustomerAuth is `@OneToOne(optional = true)`):
  ```java
  CustomerAuthDataModel auth = existing.getCustomerAuth();
  if (auth != null) {
      if (dto.getProvider() != null && dto.getProvider().isPresent()) {
          auth.setProvider(dto.getProvider().get());
      }
      if (dto.getToken() != null && dto.getToken().isPresent()) {
          auth.setToken(dto.getToken().get());
      }
  }
  ```
- Note: `provider`/`token` are `JsonNullable<String>` in the generated DTO
- TypeMap skips: `setTutorId`, `setPersonPII`, `setCustomerAuth`

### Step 3.3-3.4: Register TypeMap + Wire Controller

### Step 3.5: Create `TutorUpdateUseCaseTest.java`

Required test groups:
- `@Nested EntityLookup`
- `@Nested SuccessfulUpdate`
- `@Nested AuthUpdate` — verify provider/token set when auth exists, verify skip when auth is null
- `@Nested PiiUpdate`
- `@Nested DuplicateValidation`

### Step 3.6: Verify

```bash
mvn clean test -f platform-core-api/pom.xml -pl user-management -am
```

---

## Phase 4: MinorStudent Update

### Step 4.1: Read reference files (MinorStudentDataModel, minor-student-api.yaml)

### Step 4.2: Create `MinorStudentUpdateUseCase.java`

- No auth update
- Add `TutorRepository` to constructor for tutor existence validation
- After entity-level mapping, validate tutor exists:
  ```java
  tutorRepository.findById(new TutorDataModel.TutorCompositeId(tenantId, dto.getTutorId()))
      .orElseThrow(() -> new EntityNotFoundException(EntityType.TUTOR, String.valueOf(dto.getTutorId())));
  ```
- TypeMap skips: `setMinorStudentId`, `setPersonPII`, `setCustomerAuth`, `setTutor`
- `tutorId` FK column IS mapped by TypeMap (only `setTutor` JPA ref is skipped)

### Step 4.3-4.4: Register TypeMap + Wire Controller

### Step 4.5: Create `MinorStudentUpdateUseCaseTest.java`

Required test groups:
- `@Nested EntityLookup`
- `@Nested TutorValidation` — 404 when tutorId references nonexistent tutor
- `@Nested SuccessfulUpdate`
- `@Nested PiiUpdate`
- `@Nested DuplicateValidation`

### Step 4.6: Verify

```bash
mvn clean test -f platform-core-api/pom.xml -pl user-management -am
```

---

## Final Verification

Run the full user-management test suite:

```bash
mvn clean test -f platform-core-api/pom.xml -pl user-management -am
```

Confirm all tests pass (existing + new). Report the final test count.

---

## Critical Conventions Checklist

- [ ] Copyright header on all new files (ElatusDev 2025)
- [ ] `@Transactional` only on the `update()` method
- [ ] `@Service` + `@RequiredArgsConstructor` on use case
- [ ] Named TypeMap constant: `public static final String MAP_NAME = "{entity}UpdateMap"`
- [ ] Success message constant: `public static final String UPDATE_SUCCESS_MESSAGE = "..."`
- [ ] Use `existsByEmailHashAndPersonPiiIdNot` / `existsByPhoneHashAndPersonPiiIdNot` (self-exclusion)
- [ ] PII map uses **unnamed** void map: `modelMapper.map(dto, pii)` (not named)
- [ ] Response constructed manually (no ModelMapper) — set entityId + message
- [ ] Auth fields set via **direct setter** (no TypeMap)
- [ ] All string literals → `public static final` constants
- [ ] Tests: Given-When-Then comments, `@DisplayName` on all `@Test` and `@Nested`
- [ ] Tests: ZERO `any()` matchers — stub with exact values
- [ ] Tests: `shouldDoX_whenY()` naming
- [ ] Tests: `doNothing().when(modelMapper).map(dto, model, MAP_NAME)` for void ModelMapper
- [ ] Tests: `doNothing().when(modelMapper).map(dto, pii)` for unnamed PII map
- [ ] No `new Entity()` in production code — but OK in tests
- [ ] IDs are always `Long`, never `Integer`
- [ ] Commit: Conventional Commits format, NO AI attribution
