# Claude Code CLI Prompt — Creation UseCase Rollout

Copy `docs/creation-usecase-workflow.md` to your repo first, then use the prompt below.

---

## Prompt (copy everything inside the triple-backtick block)

```
Read docs/creation-usecase-workflow.md in full. This is your execution plan.

You are implementing creation use cases for every entity in the multi-tenant-data module, each placed in its corresponding domain module. The workflow document contains the canonical pattern extracted from EmployeeCreationUseCase and TutorCreationUseCaseTest, the entity-to-module mapping, the dependency-ordered execution phases, step-by-step instructions per entity, and unit test templates.

IMPORTANT CONSTRAINTS:
- Read CLAUDE.md, AI-CODE-REF.md before writing any code.
- Follow the 10-step per-entity process in Section 4 EXACTLY in sequence.
- Do NOT proceed to the next entity until the current one compiles and all tests pass.
- Every test MUST use Given-When-Then (never AAA), shouldDoX_whenY() naming, ZERO any() matchers, @DisplayName on every @Test and @Nested, and doNothing().when() for void ModelMapper overloads.
- Every UseCase MUST have: public static final String MAP_NAME, ApplicationContext.getBean() for prototype beans, named TypeMap in modelMapper.map(), @Transactional only on create(), public transform() for testability.
- Every ModelMapperConfiguration MUST wrap TypeMap registration in setImplicitMappingEnabled(false/true), skip entity ID setter + all nested object setters (FKs, M2M, OneToMany, PersonPII, auth objects).
- Use Long for all IDs. Never use new EntityDataModel() in production code.
- Copyright header on every new file (see CLAUDE.md).
- Commit after each entity using Conventional Commits format shown in the workflow.

EXECUTION ORDER:
Phase 0: Refactor CreateCourseUseCase to canonical pattern (Section 5)
Phase 1: tenant-management — TenantSubscription, TenantBillingCycle
Phase 2: course-management — Schedule, CourseEvent
Phase 3: billing — Compensation, Membership, PaymentAdultStudent, PaymentTutor, MembershipAdultStudent, MembershipTutor
Phase 4: pos-system — StoreProduct, StoreTransaction
Phase 5: notification-system — Notification

START WITH PHASE 0. For each entity:
1. Read the DataModel in multi-tenant-data to identify ID field, FKs, M2M, enums, inherited fields
2. Check/create OpenAPI YAML + register in module YAML
3. mvn clean generate-sources -pl <module> -am -DskipTests
4. Create Repository if missing
5. Create CreationUseCase following Variant C template (Section 4 Step 5)
6. Create/update ModelMapperConfiguration with skip rules (Section 4 Step 6, use the skip rule decision matrix)
7. mvn clean compile -pl <module> -am -DskipTests
8. Create unit test following the test template (Section 4 Step 8)
9. mvn test -pl <module> -am
10. Commit

After all phases: mvn clean install to verify no cross-module breakage.

Begin now with Phase 0 — refactor CreateCourseUseCase.
```

---

## Phase-by-Phase Alternative (Recommended)

If you prefer to run one phase at a time to review between phases, use these individual prompts:

### Phase 0

```
Read docs/creation-usecase-workflow.md, CLAUDE.md, and AI-CODE-REF.md.

Execute Phase 0 only: Refactor CreateCourseUseCase in course-management to the canonical pattern.

Follow Section 5 of the workflow exactly:
1. Add public static final String MAP_NAME = "courseMap"
2. Add ApplicationContext as constructor dependency
3. Extract transform() method using applicationContext.getBean(CourseDataModel.class) + named TypeMap
4. Create CoordinationModelMapperConfiguration with skip rules for courseId, schedules, availableCollaborators
5. Create CreateCourseUseCaseTest covering Transformation (prototype bean, named TypeMap, validator delegation) and Persistence (save + schedule save + response mapping)
6. Run: mvn test -pl course-management -am
7. Commit: refactor(course-management): align CreateCourseUseCase with canonical pattern
```

### Phase 1

```
Read docs/creation-usecase-workflow.md, CLAUDE.md, and AI-CODE-REF.md.

Execute Phase 1 only: tenant-management module.

Implement in order:
1. TenantSubscriptionCreationUseCase — read TenantSubscriptionDataModel first, create OpenAPI YAML, generate DTOs, create UseCase + test, add to existing TenantModelMapperConfiguration
2. TenantBillingCycleCreationUseCase — same process, has FK to TenantSubscription

Follow the 10-step process in Section 4 for each entity. Do not proceed to entity 2 until entity 1 compiles and tests pass. Commit after each entity.
```

### Phase 2

```
Read docs/creation-usecase-workflow.md, CLAUDE.md, and AI-CODE-REF.md.

Execute Phase 2 only: course-management module.

Implement in order:
1. ScheduleCreationUseCase — ScheduleDataModel has @ManyToOne course with insertable=false/updatable=false. Check if there's a separate scalar course_id column. If FK is read-only, the scalar column must be set instead of the object. Create OpenAPI YAML, add to CoordinationModelMapperConfiguration (created in Phase 0).
2. CourseEventCreationUseCase — extends AbstractEvent (read it first for inherited fields). Has FKs to Course and Collaborator, M2M attendees. DTO includes courseId + collaboratorId. Skip: courseEventId, course, collaborator, adultAttendees, minorAttendees.

Follow the 10-step process in Section 4 for each entity. Commit after each.
```

### Phase 3

```
Read docs/creation-usecase-workflow.md, CLAUDE.md, and AI-CODE-REF.md.

Execute Phase 3 only: billing module.

Implement in order:
1. CompensationCreationUseCase — OpenAPI already exists (compensation.yaml). Skip: compensationId, collaborators (M2M, separate association endpoint). Create BillingModelMapperConfiguration.
2. MembershipCreationUseCase — OpenAPI already exists (membership.yaml). Skip: membershipId, courses (M2M). Add to BillingModelMapperConfiguration.
3. PaymentAdultStudentCreationUseCase — OpenAPI exists (payment-management.yaml). Extends BasePayment (read it for inherited fields). Has FK to AdultStudent + Membership. Skip: entity ID + all FK object setters.
4. PaymentTutorCreationUseCase — same pattern as PaymentAdultStudent but for Tutor.
5. MembershipAdultStudentCreationUseCase — OpenAPI exists (membership-management.yaml). Association entity with FKs to Membership + AdultStudent.
6. MembershipTutorCreationUseCase — same pattern for Tutor.

Follow the 10-step process in Section 4 for each entity. Commit after each.
```

### Phase 4

```
Read docs/creation-usecase-workflow.md, CLAUDE.md, and AI-CODE-REF.md.

Execute Phase 4 only: pos-system module.

Implement in order:
1. StoreProductCreationUseCase — simple entity, no FKs. Create OpenAPI store-product.yaml. Skip: storeProductId. Create PosModelMapperConfiguration.
2. StoreTransactionCreationUseCase — has optional FK to Employee, cascades SaleItems via CascadeType.ALL. DTO includes optional employeeId and nested saleItems list. transform() resolves Employee if provided. Consider a transformSaleItem() helper. Skip: storeTransactionId, employee, saleItems. Create OpenAPI store-transaction.yaml.

Follow the 10-step process in Section 4 for each entity. Commit after each.
```

### Phase 5

```
Read docs/creation-usecase-workflow.md, CLAUDE.md, and AI-CODE-REF.md.

Execute Phase 5 only: notification-system module.

Implement:
1. NotificationCreationUseCase — has enum fields (NotificationType, NotificationPriority). ModelMapper handles enum conversion if DTO string values match enum names exactly. If they don't, register a custom converter. Skip: notificationId. Create OpenAPI notification.yaml and NotificationModelMapperConfiguration.

Follow the 10-step process in Section 4. Commit when done.

Then run: mvn clean install to verify full project integrity.
```

---

## Post-Completion Verification Prompt

```
Read docs/creation-usecase-workflow.md.

All phases are complete. Run final verification:
1. mvn clean install (full build with all tests)
2. List every *CreationUseCase.java in the repo and verify each has a corresponding *CreationUseCaseTest.java
3. List every *ModelMapperConfiguration.java and verify each registers TypeMaps for all creation use cases in its module
4. Verify no UseCase uses unnamed TypeMap (modelMapper.map(dto, EntityDataModel.class) without MAP_NAME)
5. Verify no UseCase uses new EntityDataModel() instead of applicationContext.getBean()
6. Report any gaps or inconsistencies found.
```