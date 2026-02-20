# Delete UseCase Implementation Workflow — AkademiaPlus

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, `docs/delete-usecase-strategy.md`, and `docs/exception-advice-specification.md` before starting.
**Dependency**: `docs/exception-advice-workflow.md` MUST be fully executed first — this workflow assumes `EntityNotFoundException`, `EntityDeletionNotAllowedException`, `EntityType`, `BaseControllerAdvice`, and generic `MessageService` methods already exist in `utilities`.

---

## Execution Phases

```
Phase 0: Fix @SQLDelete bug — all 29 entities in multi-tenant-data (URGENT)
Phase 1: Shared delete infrastructure — utilities module
Phase 2: Refactor user-management — 3 existing + 2 new delete use cases
Phase 3: billing — 6 new delete use cases
Phase 4: course-management — 3 new delete use cases
Phase 5: pos-system — 2 new delete use cases
Phase 6: tenant-management + notification-system — 3 new delete use cases
Phase 7: Integration tests — @SQLDelete verification + end-to-end
```

Each phase has a verification gate. Do NOT proceed to the next phase until the current one compiles and all tests pass.

---

## Phase 0: Fix @SQLDelete Bug (CRITICAL — silent data corruption)

**Problem:** Every `@SQLDelete` annotation uses `WHERE tenant_id = ?` only. With `@IdClass` composite keys, Hibernate binds parameters in `@Id` field declaration order. The missing entity ID column means `repository.delete(entity)` soft-deletes ALL rows for the entire tenant instead of the single target row.

**Scope:** `multi-tenant-data` module — all 29 entities with `@SQLDelete`.

### Step 0.1: Build the complete fix map

For each entity, you need the table name, the `@Id` fields in declaration order, and the column names.

```bash
grep -rn "@SQLDelete" multi-tenant-data/src/main/java/ | sort
```

The parameter binding order in `@SQLDelete` SQL must match the `@Id` field declaration order in the class hierarchy. For all entities that extend `TenantScoped`, `tenantId` is declared FIRST (in `TenantScoped`), then the entity-specific ID is declared SECOND (in the concrete class). Therefore the correct WHERE clause is always:

```
WHERE tenant_id = ? AND <entity_id_column> = ?
```

**Exception: `TenantDataModel`** — has only a single `@Id` (`tenantId`) with `@GeneratedValue`. Its `@SQLDelete` already uses `WHERE tenant_id = ?` and is CORRECT as-is. Do NOT modify it.

### Step 0.2: Fix each entity

For each of the 28 composite-key entities (all except `TenantDataModel`), read the file and identify the entity ID column name:

```bash
# For each file with @SQLDelete, extract the @Id column name:
grep -B5 "@Id" <entity-file> | grep "@Column"
```

**Complete fix map (28 entities):**

| Entity | Table | Current WHERE | Correct WHERE |
|---|---|---|---|
| `EmployeeDataModel` | `employees` | `tenant_id = ?` | `tenant_id = ? AND employee_id = ?` |
| `CollaboratorDataModel` | `collaborators` | `tenant_id = ?` | `tenant_id = ? AND collaborator_id = ?` |
| `AdultStudentDataModel` | `adult_students` | `tenant_id = ?` | `tenant_id = ? AND adult_student_id = ?` |
| `TutorDataModel` | `tutors` | `tenant_id = ?` | `tenant_id = ? AND tutor_id = ?` |
| `MinorStudentDataModel` | `minor_students` | `tenant_id = ?` | `tenant_id = ? AND minor_student_id = ?` |
| `PersonPIIDataModel` | `person_piis` | `tenant_id = ?` | `tenant_id = ? AND person_pii_id = ?` |
| `InternalAuthDataModel` | `internal_auths` | `tenant_id = ?` | `tenant_id = ? AND internal_auth_id = ?` |
| `CustomerAuthDataModel` | `customer_auths` | `tenant_id = ?` | `tenant_id = ? AND customer_auth_id = ?` |
| `MembershipDataModel` | `memberships` | `tenant_id = ?` | `tenant_id = ? AND membership_id = ?` |
| `MembershipAdultStudentDataModel` | `membership_adult_students` | `tenant_id = ?` | `tenant_id = ? AND membership_adult_student_id = ?` |
| `MembershipTutorDataModel` | `membership_tutors` | `tenant_id = ?` | `tenant_id = ? AND membership_tutor_id = ?` |
| `PaymentAdultStudentDataModel` | `payment_adult_students` | `tenant_id = ?` | `tenant_id = ? AND payment_adult_student_id = ?` |
| `PaymentTutorDataModel` | `payment_tutors` | `tenant_id = ?` | `tenant_id = ? AND payment_tutor_id = ?` |
| `CompensationDataModel` | `compensations` | `tenant_id = ?` | `tenant_id = ? AND compensation_id = ?` |
| `CardPaymentInfoDataModel` | `card_payment_infos` | `tenant_id = ?` | `tenant_id = ? AND card_payment_info_id = ?` |
| `CourseDataModel` | `courses` | `tenant_id = ?` | `tenant_id = ? AND course_id = ?` |
| `ScheduleDataModel` | `schedules` | `tenant_id = ?` | `tenant_id = ? AND schedule_id = ?` |
| `CourseEventDataModel` | `course_events` | `tenant_id = ?` | `tenant_id = ? AND course_event_id = ?` |
| `StoreProductDataModel` | `store_products` | `tenant_id = ?` | `tenant_id = ? AND store_product_id = ?` |
| `StoreTransactionDataModel` | `store_transactions` | `tenant_id = ?` | `tenant_id = ? AND store_transaction_id = ?` |
| `StoreSaleItemDataModel` | `store_sale_items` | `tenant_id = ?` | `tenant_id = ? AND store_sale_item_id = ?` |
| `NotificationDataModel` | `notifications` | `tenant_id = ?` | `tenant_id = ? AND notification_id = ?` |
| `NotificationDeliveryDataModel` | `notification_deliveries` | `tenant_id = ?` | `tenant_id = ? AND notification_delivery_id = ?` |
| `EmailDataModel` | `emails` | `tenant_id = ?` | `tenant_id = ? AND email_id = ?` |
| `EmailAttachmentDataModel` | `email_attachments` | `tenant_id = ?` | `tenant_id = ? AND email_attachment_id = ?` |
| `EmailRecipientDataModel` | `email_recipients` | `tenant_id = ?` | `tenant_id = ? AND email_recipient_id = ?` |
| `TenantSubscriptionDataModel` | `tenant_subscriptions` | `tenant_id = ?` | `tenant_id = ? AND tenant_subscription_id = ?` |
| `TenantBillingCycleDataModel` | `tenant_billing_cycles` | `tenant_id = ?` | `tenant_id = ? AND tenant_billing_cycle_id = ?` |

**CRITICAL: Verify each entity's actual `@Id` column name** before applying. The fix map above assumes conventional naming. Read each file to confirm:

```bash
# Example verification for EmployeeDataModel:
grep -A2 "@Id" multi-tenant-data/src/main/java/com/akademiaplus/users/employee/EmployeeDataModel.java
# Must show: @Column(name = "employee_id")
```

Apply the fix by replacing the `@SQLDelete` annotation on each file. Example:

```java
// Before:
@SQLDelete(sql = "UPDATE employees SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")

// After:
@SQLDelete(sql = "UPDATE employees SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND employee_id = ?")
```

### Step 0.3: Create reflective validation test

**File**: `multi-tenant-data/src/test/java/com/akademiaplus/infra/persistence/SQLDeleteValidationTest.java`

This test scans all `@Entity` classes at compile time and verifies every `@SQLDelete` annotation includes all `@Id` columns:

```java
package com.akademiaplus.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLDelete;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that every {@code @SQLDelete} annotation includes all {@code @Id}
 * columns in its WHERE clause. Prevents the silent data corruption bug where
 * a missing entity ID column causes tenant-wide soft-deletion.
 */
@DisplayName("@SQLDelete WHERE Clause Validation")
class SQLDeleteValidationTest {

    private static final String BASE_PACKAGE = "com.akademiaplus";

    @Test
    @DisplayName("Should include all @Id columns in @SQLDelete WHERE clause")
    void shouldIncludeAllIdColumns_whenSQLDeleteDeclared() {
        Reflections reflections = new Reflections(BASE_PACKAGE);
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);
        List<String> violations = new ArrayList<>();

        for (Class<?> entityClass : entityClasses) {
            SQLDelete sqlDelete = entityClass.getAnnotation(SQLDelete.class);
            if (sqlDelete == null) continue;

            String sql = sqlDelete.sql().toLowerCase();
            List<String> idColumns = collectIdColumns(entityClass);

            for (String column : idColumns) {
                if (!sql.contains(column.toLowerCase())) {
                    violations.add(entityClass.getSimpleName()
                        + ": @SQLDelete missing @Id column '" + column
                        + "' in WHERE clause. SQL: " + sqlDelete.sql());
                }
            }
        }

        assertThat(violations)
            .as("All @SQLDelete annotations must include all @Id columns")
            .isEmpty();
    }

    /**
     * Walks the class hierarchy collecting all @Id field column names.
     */
    private List<String> collectIdColumns(Class<?> clazz) {
        List<String> columns = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    Column col = field.getAnnotation(Column.class);
                    String colName = (col != null && !col.name().isEmpty())
                        ? col.name()
                        : field.getName();
                    columns.add(colName);
                }
            }
            current = current.getSuperclass();
        }
        return columns;
    }
}
```

**Dependency note**: The `reflections` library is needed. Check if it's already in the POM:
```bash
grep -rn "reflections" multi-tenant-data/pom.xml
```
If missing, add to `multi-tenant-data/pom.xml`:
```xml
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10.2</version>
    <scope>test</scope>
</dependency>
```

### Step 0.4: Run the validation test BEFORE fixing

```bash
mvn test -pl multi-tenant-data -am -Dtest="SQLDeleteValidationTest"
```

This MUST FAIL — confirming the bug exists for 28 entities. The output will list every violation.

### Step 0.5: Apply all 28 fixes

Fix each file per the fix map in Step 0.2. After fixing all 28 files, run the test again:

```bash
mvn test -pl multi-tenant-data -am -Dtest="SQLDeleteValidationTest"
```

This MUST PASS — confirming all 28 entities now have correct WHERE clauses.

### Step 0.6: Verify full build

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests
mvn test -pl multi-tenant-data -am
```

### Step 0.7: Commit

```bash
git add -A
git commit -m "fix(multi-tenant-data): include entity ID in @SQLDelete WHERE clause

BREAKING BUG: All 28 composite-key entities had @SQLDelete with only
WHERE tenant_id = ?, causing repository.delete() to soft-delete ALL
rows for the entire tenant instead of the target row.

Fix: Add entity-specific ID column to every @SQLDelete WHERE clause.
Add reflective SQLDeleteValidationTest to prevent regression."
```

---

## Phase 1: Shared Delete Infrastructure

### Step 1.1: Add requireTenantId() to TenantContextHolder

Locate the file:
```bash
find . -name "TenantContextHolder.java" -path "*/main/*"
```

The existing `getTenantId()` returns `Optional<Long>`. Every delete use case calls `.orElseThrow()` with a raw `IllegalArgumentException`. Add a convenience method that throws the correct exception:

```java
/**
 * Returns the current tenant ID or throws {@link InvalidTenantException}
 * if no tenant context is set.
 *
 * @return the current tenant ID, never null
 * @throws InvalidTenantException if tenant context is missing
 */
public Long requireTenantId() {
    return getTenantId()
        .orElseThrow(InvalidTenantException::new);
}
```

This replaces the scattered `tenantContextHolder.getTenantId().orElseThrow(() -> new IllegalArgumentException(...))` pattern.

### Step 1.2: Create DeleteUseCaseSupport utility

**File**: `utilities/src/main/java/com/akademiaplus/utilities/usecases/DeleteUseCaseSupport.java`

```java
package com.akademiaplus.utilities.usecases;

import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Shared soft-delete execution logic for all delete use cases.
 * <p>
 * Encapsulates the find-or-throw → try-delete → catch-constraint pattern
 * that is identical across all 19 delete use cases. Individual use cases
 * compose this utility rather than inheriting from it.
 * <p>
 * For entities with pre-delete business rules (e.g., Tutor with active
 * MinorStudents), the use case performs validation before calling
 * {@link #executeDelete}.
 */
public final class DeleteUseCaseSupport {

    private DeleteUseCaseSupport() {}

    /**
     * Executes soft-delete for a tenant-scoped entity.
     * <p>
     * Flow: findById → present? delete : throw 404
     *       delete succeeds? return : catch constraint → throw 409
     *
     * @param repository  the JPA repository for the entity
     * @param compositeId the composite key (tenantId + entityId)
     * @param entityType  message key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId    the entity ID as display string
     * @param <T>         the entity type
     * @param <ID>        the composite ID type
     */
    public static <T, ID> void executeDelete(
            JpaRepository<T, ID> repository,
            ID compositeId,
            String entityType,
            String entityId) {

        T entity = repository.findById(compositeId)
                .orElseThrow(() -> new EntityNotFoundException(entityType, entityId));

        try {
            repository.delete(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new EntityDeletionNotAllowedException(entityType, entityId, ex);
        }
    }

    /**
     * Finds an entity by composite key or throws 404.
     * <p>
     * Use this when pre-delete validation is needed before calling delete.
     * The use case performs business checks on the returned entity, then
     * calls {@link #executeDelete} or {@code repository.delete()} directly.
     *
     * @param repository  the JPA repository
     * @param compositeId the composite key
     * @param entityType  message key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId    the entity ID as display string
     * @param <T>         the entity type
     * @param <ID>        the composite ID type
     * @return the found entity, never null
     */
    public static <T, ID> T findOrThrow(
            JpaRepository<T, ID> repository,
            ID compositeId,
            String entityType,
            String entityId) {

        return repository.findById(compositeId)
                .orElseThrow(() -> new EntityNotFoundException(entityType, entityId));
    }
}
```

### Step 1.3: Unit test for DeleteUseCaseSupport

**File**: `utilities/src/test/java/com/akademiaplus/utilities/usecases/DeleteUseCaseSupportTest.java`

```java
@DisplayName("DeleteUseCaseSupport")
@ExtendWith(MockitoExtension.class)
class DeleteUseCaseSupportTest {

    private static final String ENTITY_TYPE = EntityType.EMPLOYEE;
    private static final String ENTITY_ID = "42";
    private static final Long COMPOSITE_ID = 42L;

    @Mock private JpaRepository<Object, Long> repository;

    @Nested
    @DisplayName("executeDelete")
    class ExecuteDelete {

        @Test
        @DisplayName("Should delete entity when found by composite ID")
        void shouldDeleteEntity_whenFoundByCompositeId() {
            // Given
            Object entity = new Object();
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.of(entity));

            // When
            DeleteUseCaseSupport.executeDelete(repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID);

            // Then
            verify(repository).delete(entity);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFound_whenEntityMissing() {
            // Given
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() ->
                DeleteUseCaseSupport.executeDelete(repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> {
                    EntityNotFoundException enfe = (EntityNotFoundException) ex;
                    assertThat(enfe.getEntityType()).isEqualTo(ENTITY_TYPE);
                    assertThat(enfe.getEntityId()).isEqualTo(ENTITY_ID);
                });
        }

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed when constraint violation occurs")
        void shouldThrowDeletionNotAllowed_whenConstraintViolation() {
            // Given
            Object entity = new Object();
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.of(entity));
            DataIntegrityViolationException cause =
                    new DataIntegrityViolationException("FK constraint");
            doThrow(cause).when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() ->
                DeleteUseCaseSupport.executeDelete(repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
                .isInstanceOf(EntityDeletionNotAllowedException.class)
                .satisfies(ex -> {
                    EntityDeletionNotAllowedException edna =
                            (EntityDeletionNotAllowedException) ex;
                    assertThat(edna.getEntityType()).isEqualTo(ENTITY_TYPE);
                    assertThat(edna.getEntityId()).isEqualTo(ENTITY_ID);
                    assertThat(edna.getReason()).isNull();
                    assertThat(edna.getCause()).isSameAs(cause);
                });
        }
    }

    @Nested
    @DisplayName("findOrThrow")
    class FindOrThrow {

        @Test
        @DisplayName("Should return entity when found")
        void shouldReturnEntity_whenFound() {
            // Given
            Object entity = new Object();
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.of(entity));

            // When
            Object result = DeleteUseCaseSupport.findOrThrow(
                    repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID);

            // Then
            assertThat(result).isSameAs(entity);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFound_whenNotFound() {
            // Given
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() ->
                DeleteUseCaseSupport.findOrThrow(
                    repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
```

### Step 1.4: Verify

```bash
mvn clean compile -pl utilities -am -DskipTests
mvn test -pl utilities -am
```

### Step 1.5: Commit

```bash
git add -A
git commit -m "feat(utilities): add DeleteUseCaseSupport and TenantContextHolder.requireTenantId

Add static utility for shared delete pattern: find-or-404 → delete →
catch-constraint-409. Provides executeDelete() for simple deletes and
findOrThrow() for use cases requiring pre-delete validation.

Add requireTenantId() convenience method to TenantContextHolder."
```

---

## Phase 2: Refactor user-management (3 existing + 2 new)

### Delete UseCase Template — Standard (no business rules)

All standard delete use cases follow this exact pattern. Do NOT deviate:

```java
package com.akademiaplus.<aggregate>.usecases;

import com.akademiaplus.<aggregate>.interfaceadapters.<Entity>Repository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.<datamodel-package>.<EntityDataModel>;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link <EntityDataModel>} by composite key.
 * <p>
 * Delegates to {@link DeleteUseCaseSupport#executeDelete} for the
 * find-or-404 → delete → catch-constraint-409 pattern.
 */
@Service
public class Delete<Entity>UseCase {

    private final <Entity>Repository repository;
    private final TenantContextHolder tenantContextHolder;

    public Delete<Entity>UseCase(<Entity>Repository repository,
                                  TenantContextHolder tenantContextHolder) {
        this.repository = repository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes the {@link <EntityDataModel>} identified by the given ID
     * within the current tenant context.
     *
     * @param entityId the entity-specific ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no entity exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long entityId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                repository,
                new <EntityDataModel>.<Entity>CompositeId(tenantId, entityId),
                EntityType.<ENTITY_CONSTANT>,
                String.valueOf(entityId));
    }
}
```

### Delete UseCase Test Template — Standard

```java
@DisplayName("Delete<Entity>UseCase")
@ExtendWith(MockitoExtension.class)
class Delete<Entity>UseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ENTITY_ID = 42L;

    @Mock private <Entity>Repository repository;
    @Mock private TenantContextHolder tenantContextHolder;

    private Delete<Entity>UseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new Delete<Entity>UseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete entity when found by composite key")
        void shouldSoftDeleteEntity_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            <EntityDataModel> entity = new <EntityDataModel>();
            <EntityDataModel>.<Entity>CompositeId compositeId =
                    new <EntityDataModel>.<Entity>CompositeId(TENANT_ID, ENTITY_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(ENTITY_ID);

            // Then
            verify(repository).delete(entity);
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when entity missing")
        void shouldThrowEntityNotFound_whenEntityMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            <EntityDataModel>.<Entity>CompositeId compositeId =
                    new <EntityDataModel>.<Entity>CompositeId(TENANT_ID, ENTITY_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(ENTITY_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> {
                    EntityNotFoundException enfe = (EntityNotFoundException) ex;
                    assertThat(enfe.getEntityType()).isEqualTo(EntityType.<ENTITY_CONSTANT>);
                    assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(ENTITY_ID));
                });
        }
    }

    @Nested
    @DisplayName("Constraint Violation")
    class ConstraintViolation {

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed when constraint violated")
        void shouldThrowDeletionNotAllowed_whenConstraintViolated() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            <EntityDataModel> entity = new <EntityDataModel>();
            <EntityDataModel>.<Entity>CompositeId compositeId =
                    new <EntityDataModel>.<Entity>CompositeId(TENANT_ID, ENTITY_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(ENTITY_ID))
                .isInstanceOf(EntityDeletionNotAllowedException.class)
                .satisfies(ex -> {
                    EntityDeletionNotAllowedException edna =
                            (EntityDeletionNotAllowedException) ex;
                    assertThat(edna.getEntityType())
                            .isEqualTo(EntityType.<ENTITY_CONSTANT>);
                    assertThat(edna.getReason()).isNull();
                });
        }
    }
}
```

### Step 2.1: Refactor existing DeleteEmployeeUseCase

Read the current file:
```bash
cat user-management/src/main/java/com/akademiaplus/employee/usecases/DeleteEmployeeUseCase.java
```

Replace the entire body with the standard template, using:
- `EntityType.EMPLOYEE`
- `EmployeeDataModel.EmployeeCompositeId`
- `EmployeeRepository`

Remove the import of `EmployeeDeletionNotAllowedException` and `EmployeeNotFoundException`.

### Step 2.2: Refactor existing DeleteCollaboratorUseCase

Same pattern — use `EntityType.COLLABORATOR`, `CollaboratorDataModel.CollaboratorCompositeId`.

### Step 2.3: Refactor existing DeleteAdultStudentUseCase

Same pattern — use `EntityType.ADULT_STUDENT`, `AdultStudentDataModel.AdultStudentCompositeId`.

### Step 2.4: Create DeleteTutorUseCase (SPECIAL — business rule)

**File**: `user-management/src/main/java/com/akademiaplus/customer/tutor/usecases/DeleteTutorUseCase.java`

The Tutor delete has a pre-delete business rule: a Tutor cannot be deleted if they have active (non-soft-deleted) MinorStudents. The OpenAPI spec already defines this constraint.

First, determine the relationship between Tutor and MinorStudent:
```bash
grep -n "minorStudent\|MinorStudent" multi-tenant-data/src/main/java/com/akademiaplus/users/customer/TutorDataModel.java
```

The Tutor→MinorStudent relationship determines how to count active students. If it's a `@OneToMany`, you can count via the collection. Otherwise, use a repository query.

```java
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.tutor.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link TutorDataModel}.
 * <p>
 * Business rule: a Tutor with active (non-deleted) MinorStudents
 * cannot be deleted. The pre-delete check prevents orphaned students.
 */
@Service
public class DeleteTutorUseCase {

    public static final String ACTIVE_MINOR_STUDENTS_REASON =
            "Tutor tiene %d alumno(s) menor(es) activo(s)";

    private final TutorRepository tutorRepository;
    private final MinorStudentRepository minorStudentRepository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteTutorUseCase(TutorRepository tutorRepository,
                              MinorStudentRepository minorStudentRepository,
                              TenantContextHolder tenantContextHolder) {
        this.tutorRepository = tutorRepository;
        this.minorStudentRepository = minorStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes a Tutor after verifying they have no active MinorStudents.
     *
     * @param tutorId the tutor's entity-specific ID
     * @throws EntityNotFoundException if no tutor exists with the given ID
     * @throws EntityDeletionNotAllowedException if tutor has active minor students
     *         (business rule) or a DB constraint prevents deletion
     */
    @Transactional
    public void delete(Long tutorId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        String entityId = String.valueOf(tutorId);

        TutorDataModel tutor = DeleteUseCaseSupport.findOrThrow(
                tutorRepository,
                new TutorDataModel.TutorCompositeId(tenantId, tutorId),
                EntityType.TUTOR,
                entityId);

        // Pre-delete business rule: check for active minor students
        long activeMinorStudents = countActiveMinorStudents(tenantId, tutorId);
        if (activeMinorStudents > 0) {
            throw new EntityDeletionNotAllowedException(
                    EntityType.TUTOR,
                    entityId,
                    String.format(ACTIVE_MINOR_STUDENTS_REASON, activeMinorStudents));
        }

        try {
            tutorRepository.delete(tutor);
        } catch (DataIntegrityViolationException ex) {
            throw new EntityDeletionNotAllowedException(
                    EntityType.TUTOR, entityId, ex);
        }
    }

    /**
     * Counts non-soft-deleted MinorStudents linked to this Tutor.
     * <p>
     * The {@code @SQLRestriction("deleted_at IS NULL")} on MinorStudentDataModel
     * ensures only active students are returned by JPA queries.
     */
    private long countActiveMinorStudents(Long tenantId, Long tutorId) {
        return minorStudentRepository.countByTenantIdAndTutorId(tenantId, tutorId);
    }
}
```

**Repository method needed** — add to `MinorStudentRepository`:
```java
long countByTenantIdAndTutorId(Long tenantId, Long tutorId);
```

Verify the MinorStudent→Tutor relationship to get the correct field name:
```bash
grep -n "tutor\|Tutor" multi-tenant-data/src/main/java/com/akademiaplus/users/customer/MinorStudentDataModel.java
```

The JPA derived query method name must match the field name in `MinorStudentDataModel`. If the FK field is `tutor` (a `TutorDataModel` object), the method is `countByTenantIdAndTutor_TutorId()`. If it's a scalar `tutorId` field, use `countByTenantIdAndTutorId()`. Read the entity to confirm.

### Step 2.5: Create DeleteMinorStudentUseCase

Standard template — use `EntityType.MINOR_STUDENT`, `MinorStudentDataModel.MinorStudentCompositeId`. No special business rules.

### Step 2.6: Create unit tests for all 5 use cases

For the 3 refactored use cases (Employee, Collaborator, AdultStudent) and the new MinorStudent use case, use the standard test template from above.

For DeleteTutorUseCase, add an extra `@Nested` class:

```java
@Nested
@DisplayName("Business Rule — Active Minor Students")
class ActiveMinorStudentsRule {

    @Test
    @DisplayName("Should throw business rule deletion error when tutor has active students")
    void shouldThrowDeletionNotAllowed_whenTutorHasActiveMinorStudents() {
        // Given
        when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
        TutorDataModel tutor = new TutorDataModel();
        when(tutorRepository.findById(compositeId)).thenReturn(Optional.of(tutor));
        when(minorStudentRepository.countByTenantIdAndTutorId(TENANT_ID, TUTOR_ID))
                .thenReturn(3L);

        // When / Then
        assertThatThrownBy(() -> useCase.delete(TUTOR_ID))
            .isInstanceOf(EntityDeletionNotAllowedException.class)
            .satisfies(ex -> {
                EntityDeletionNotAllowedException edna =
                        (EntityDeletionNotAllowedException) ex;
                assertThat(edna.getEntityType()).isEqualTo(EntityType.TUTOR);
                assertThat(edna.getReason()).contains("3");
                assertThat(edna.getCause()).isNull();
            });
    }

    @Test
    @DisplayName("Should proceed with deletion when tutor has zero active students")
    void shouldDeleteTutor_whenNoActiveMinorStudents() {
        // Given
        when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
        TutorDataModel tutor = new TutorDataModel();
        when(tutorRepository.findById(compositeId)).thenReturn(Optional.of(tutor));
        when(minorStudentRepository.countByTenantIdAndTutorId(TENANT_ID, TUTOR_ID))
                .thenReturn(0L);

        // When
        useCase.delete(TUTOR_ID);

        // Then
        verify(tutorRepository).delete(tutor);
    }
}
```

### Step 2.7: Wire controllers for Tutor and MinorStudent

Read the existing controllers:
```bash
find user-management/src/main/java -name "TutorController.java" -o -name "MinorStudentController.java"
```

Each controller implements a generated API interface with a `deleteTutor(Long)` / `deleteMinorStudent(Long)` method. Add the delete use case as a constructor dependency and delegate:

```java
@Override
public ResponseEntity<Void> deleteTutor(Long tutorId) {
    deleteTutorUseCase.delete(tutorId);
    return ResponseEntity.noContent().build();
}
```

### Step 2.8: Verify + commit

```bash
mvn clean compile -pl user-management -am -DskipTests
mvn test -pl user-management -am
git add -A
git commit -m "feat(user-management): implement all 5 delete use cases

Refactor Employee, Collaborator, AdultStudent to use
DeleteUseCaseSupport + generic exceptions.

Add DeleteTutorUseCase with pre-delete business rule: blocks deletion
when active MinorStudents exist (HTTP 409 DELETION_BUSINESS_RULE).

Add DeleteMinorStudentUseCase (standard pattern).

Unit tests for all 5 use cases with Given-When-Then."
```

---

## Phase 3: billing — 6 new delete use cases

All 6 follow the standard template. No special business rules.

### Step 3.1: Identify entity → composite ID mapping

For EACH entity, read the DataModel to get the CompositeId class name:
```bash
grep -A3 "class.*CompositeId" multi-tenant-data/src/main/java/com/akademiaplus/billing/**/*.java
```

### Step 3.2: Create delete use cases (one at a time)

| # | UseCase | EntityType Constant | CompositeId Class | Package |
|---|---------|--------------------|--------------------|---------|
| 3.1 | `DeleteCompensationUseCase` | `COMPENSATION` | `CompensationDataModel.CompensationCompositeId` | `billing.payroll.usecases` |
| 3.2 | `DeleteMembershipUseCase` | `MEMBERSHIP` | `MembershipDataModel.MembershipCompositeId` | `billing.membership.usecases` |
| 3.3 | `DeleteMembershipAdultStudentUseCase` | `MEMBERSHIP_ADULT_STUDENT` | `MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId` | `billing.membership.usecases` |
| 3.4 | `DeleteMembershipTutorUseCase` | `MEMBERSHIP_TUTOR` | `MembershipTutorDataModel.MembershipTutorCompositeId` | `billing.membership.usecases` |
| 3.5 | `DeletePaymentAdultStudentUseCase` | `PAYMENT_ADULT_STUDENT` | `PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId` | `billing.customerpayment.usecases` |
| 3.6 | `DeletePaymentTutorUseCase` | `PAYMENT_TUTOR` | `PaymentTutorDataModel.PaymentTutorCompositeId` | `billing.customerpayment.usecases` |

For each entity:
1. Apply the standard delete UseCase template
2. Apply the standard delete test template
3. Wire the controller (read existing controller, add use case dependency, delegate)
4. Compile: `mvn clean compile -pl billing -am -DskipTests`

### Step 3.3: Verify + commit

```bash
mvn test -pl billing -am
git add -A
git commit -m "feat(billing): implement 6 delete use cases

Add DeleteCompensationUseCase, DeleteMembershipUseCase,
DeleteMembershipAdultStudentUseCase, DeleteMembershipTutorUseCase,
DeletePaymentAdultStudentUseCase, DeletePaymentTutorUseCase.

All use DeleteUseCaseSupport + generic exceptions.
Unit tests for all 6 with Given-When-Then."
```

---

## Phase 4: course-management — 3 new delete use cases

### Step 4.1: Identify entity → composite ID mapping

```bash
grep -A3 "class.*CompositeId" multi-tenant-data/src/main/java/com/akademiaplus/courses/**/*.java
```

### Step 4.2: Create delete use cases

| # | UseCase | EntityType Constant | CompositeId Class | Package |
|---|---------|--------------------|--------------------|---------|
| 4.1 | `DeleteCourseUseCase` | `COURSE` | `CourseDataModel.CourseCompositeId` | `course.program.usecases` |
| 4.2 | `DeleteScheduleUseCase` | `COURSE` (verify) | `ScheduleDataModel.ScheduleCompositeId` | `course.program.usecases` |
| 4.3 | `DeleteCourseEventUseCase` | `COURSE_EVENT` | `CourseEventDataModel.CourseEventCompositeId` | `course.event.usecases` |

**Note on package paths**: Read the existing creation use cases to determine the correct package structure:
```bash
find course-management/src/main/java -name "*UseCase.java" -path "*/usecases/*"
```

For each entity: apply standard template, create test, wire controller.

### Step 4.3: Verify + commit

```bash
mvn test -pl course-management -am
git add -A
git commit -m "feat(course-management): implement 3 delete use cases

Add DeleteCourseUseCase, DeleteScheduleUseCase, DeleteCourseEventUseCase.
All use DeleteUseCaseSupport + generic exceptions.
Unit tests with Given-When-Then."
```

---

## Phase 5: pos-system — 2 new delete use cases

### Step 5.1: Identify entity → composite ID mapping

```bash
grep -A3 "class.*CompositeId" multi-tenant-data/src/main/java/com/akademiaplus/billing/store/*.java
```

### Step 5.2: Create delete use cases

| # | UseCase | EntityType Constant | CompositeId Class | Package |
|---|---------|--------------------|--------------------|---------|
| 5.1 | `DeleteStoreProductUseCase` | `STORE_PRODUCT` | `StoreProductDataModel.StoreProductCompositeId` | `store.product.usecases` |
| 5.2 | `DeleteStoreTransactionUseCase` | `STORE_TRANSACTION` | `StoreTransactionDataModel.StoreTransactionCompositeId` | `store.transaction.usecases` |

**Note on package paths**: The pos-system module may use different package conventions. Read existing files:
```bash
find pos-system/src/main/java -name "*UseCase.java" -path "*/usecases/*"
```

For each entity: apply standard template, create test, wire controller.

### Step 5.3: Verify + commit

```bash
mvn test -pl pos-system -am
git add -A
git commit -m "feat(pos-system): implement 2 delete use cases

Add DeleteStoreProductUseCase, DeleteStoreTransactionUseCase.
All use DeleteUseCaseSupport + generic exceptions.
Unit tests with Given-When-Then."
```

---

## Phase 6: tenant-management + notification-system — 3 new delete use cases

### Step 6.1: tenant-management

```bash
grep -A3 "class.*CompositeId" multi-tenant-data/src/main/java/com/akademiaplus/tenancy/*.java
```

| # | UseCase | EntityType Constant | Notes |
|---|---------|--------------------|-|
| 6.1 | `DeleteTenantSubscriptionUseCase` | `TENANT_SUBSCRIPTION` | Standard template. The OpenAPI operationId is `cancelTenantSubscription`. |

**Important**: The OpenAPI spec uses `cancelTenantSubscription` as the operationId, not `deleteTenantSubscription`. The generated API interface method will be `cancelTenantSubscription()`. Read the controller to confirm:
```bash
grep -n "cancel\|delete" tenant-management/src/main/java/com/akademiaplus/*/interfaceadapters/*Controller.java
```

**Note on TenantDataModel**: `TenantDataModel` uses `@GeneratedValue` with a SINGLE `@Id` (tenantId). It does NOT use `@IdClass`. Its delete use case passes `tenantId` directly as the repository ID — no composite key construction. However, tenant deletion is a high-risk operation that may require cascading soft-delete of all child entities. **For now, implement the standard pattern. Cascading tenant deletion is out of scope — document it as a future ADR.**

| 6.2 | `DeleteTenantUseCase` | `TENANT` | Single `@Id`, pass `tenantId` directly to `repository.findById(tenantId)` |

### Step 6.2: notification-system

| 6.3 | `DeleteNotificationUseCase` | `NOTIFICATION` | Standard template |

```bash
find notification-system/src/main/java -name "*UseCase.java" -path "*/usecases/*"
```

### Step 6.3: Verify + commit (tenant-management)

```bash
mvn test -pl tenant-management -am
git add -A
git commit -m "feat(tenant-management): implement 2 delete use cases

Add DeleteTenantUseCase (single @Id, no composite key) and
DeleteTenantSubscriptionUseCase (standard composite key pattern).
Unit tests with Given-When-Then.

Note: cascading tenant soft-delete is out of scope — future ADR."
```

### Step 6.4: Verify + commit (notification-system)

```bash
mvn test -pl notification-system -am
git add -A
git commit -m "feat(notification-system): implement DeleteNotificationUseCase

Standard pattern with DeleteUseCaseSupport + generic exceptions.
Unit test with Given-When-Then."
```

### Step 6.5: Full project build

```bash
mvn clean install
```

All 19 delete use cases now exist. All compile. All unit tests pass.

---

## Phase 7: Integration Tests

Integration tests verify the actual JPA + MariaDB behavior. They require Docker Desktop for Testcontainers.

### Step 7.1: @SQLDelete single-row verification test

**File**: `multi-tenant-data/src/test/java/com/akademiaplus/infra/persistence/SQLDeleteIntegrationTest.java`

This is a Testcontainers test that:
1. Creates 2 entities for the same tenant
2. Calls `repository.delete(entity1)`
3. Verifies entity1 has `deleted_at` set
4. Verifies entity2 has `deleted_at` still NULL

Use Employee as the test entity (simplest to set up).

```java
@SpringBootTest
@Testcontainers
@DisplayName("@SQLDelete Single-Row Verification")
class SQLDeleteIntegrationTest {

    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11")
            .withDatabaseName("akademiaplus_test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
    }

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("Should soft-delete only target entity, not all entities for tenant")
    void shouldSoftDeleteOnlyTarget_whenDeleteCalled() {
        // Given — two employees for the same tenant
        Long tenantId = 1L;
        // ... create and persist employee1 (id=1) and employee2 (id=2)
        // This requires setting up the tenant context and using the
        // EntityIdAssigner or manually setting IDs

        // When — delete employee1
        employeeRepository.delete(employee1);
        employeeRepository.flush();
        entityManager.clear();

        // Then — verify at the SQL level
        // Use native query to bypass @SQLRestriction:
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT employee_id, deleted_at FROM employees WHERE tenant_id = ?")
                .setParameter(1, tenantId)
                .getResultList();

        // employee1 should have deleted_at set
        // employee2 should have deleted_at NULL
    }
}
```

**Important integration test notes:**
- Read `docs/adr/` for any existing ADR on integration test strategy
- The Testcontainers setup may already exist in the project — check:
  ```bash
  find . -name "*IntegrationTest.java" -o -name "*IT.java" | head -10
  grep -rn "Testcontainers\|@Container" --include="*.java" */src/test/
  ```
- Follow the existing integration test configuration (application-test.yml, test profiles, etc.)
- If no integration test infrastructure exists yet, create a shared `AbstractIntegrationTest` base class

### Step 7.2: @SQLRestriction exclusion verification

Verify that after soft-delete, the entity is excluded from normal JPA queries:

```java
@Test
@DisplayName("Should exclude soft-deleted entity from findById")
void shouldExcludeSoftDeleted_whenFindByIdCalled() {
    // Given — create and soft-delete an employee
    // ...
    employeeRepository.delete(employee);
    employeeRepository.flush();
    entityManager.clear();

    // When
    Optional<EmployeeDataModel> result = employeeRepository.findById(compositeId);

    // Then — @SQLRestriction filters it out
    assertThat(result).isEmpty();
}
```

### Step 7.3: FK constraint → 409 end-to-end test

Verify that deleting an entity with active FK references results in HTTP 409:

```java
@Test
@DisplayName("Should return 409 when deleting entity with FK references")
void shouldReturn409_whenDeletingEntityWithFkReferences() {
    // Given — create a Course with Schedules referencing it

    // When — attempt to delete the Course
    // (use MockMvc or TestRestTemplate to call the DELETE endpoint)

    // Then
    // HTTP 409
    // Response body contains code: "DELETION_CONSTRAINT_VIOLATION"
}
```

### Step 7.4: Tutor business rule end-to-end test

```java
@Test
@DisplayName("Should return 409 with business rule reason when tutor has active students")
void shouldReturn409WithReason_whenTutorHasActiveStudents() {
    // Given — create a Tutor with 2 active MinorStudents

    // When — attempt to delete the Tutor via DELETE endpoint

    // Then
    // HTTP 409
    // Response body contains code: "DELETION_BUSINESS_RULE"
    // Response body message contains "alumno(s) menor(es) activo(s)"
}

@Test
@DisplayName("Should return 204 when tutor has no active students")
void shouldReturn204_whenTutorHasNoActiveStudents() {
    // Given — create a Tutor with zero MinorStudents

    // When — DELETE /tutors/{tutorId}

    // Then — HTTP 204
}
```

### Step 7.5: Verify + commit

```bash
mvn verify -pl <module> -am  # Runs integration tests via Maven Failsafe
git add -A
git commit -m "test: add integration tests for soft-delete behavior

Verify @SQLDelete single-row correctness (not tenant-wide).
Verify @SQLRestriction excludes soft-deleted from queries.
Verify FK constraint → HTTP 409 end-to-end.
Verify Tutor business rule → HTTP 409 with reason.

Uses Testcontainers with MariaDB 11."
```

---

## Final Verification Checklist

Run after ALL phases complete:

| Check | Command | Expected |
|-------|---------|----------|
| Full build | `mvn clean install` | BUILD SUCCESS |
| @SQLDelete fix count | `grep -rn "AND.*_id = ?" multi-tenant-data/src/main/java/ --include="*.java" \| wc -l` | 28 (all composite-key entities) |
| Delete UseCase count | `find . -name "Delete*UseCase.java" -path "*/main/*" \| wc -l` | 19 |
| Delete test count | `find . -name "Delete*UseCaseTest.java" -path "*/test/*" \| wc -l` | 19 |
| No old exception refs | `grep -rn "EmployeeDeletionNotAllowed\|CollaboratorDeletionNotAllowed\|AdultStudentDeletionNotAllowed" --include="*.java" */src/main/java/ \| wc -l` | 0 |
| No IllegalArgumentException for tenant | `grep -rn "IllegalArgumentException.*Tenant\|IllegalArgumentException.*tenant" --include="*.java" */src/main/java/ \| wc -l` | 0 |
| All controllers wired | `grep -rn "noContent().build()" --include="*.java" */src/main/java/ \| wc -l` | ≥ 19 |
| SQLDelete validation test | `mvn test -pl multi-tenant-data -Dtest="SQLDeleteValidationTest"` | PASS |

---

## Entity Inventory — Complete Delete Coverage

| # | Entity | Module | UseCase | Business Rule | Phase |
|---|--------|--------|---------|---------------|-------|
| 1 | Employee | user-management | Refactored | None | 2 |
| 2 | Collaborator | user-management | Refactored | None | 2 |
| 3 | AdultStudent | user-management | Refactored | None | 2 |
| 4 | Tutor | user-management | **New** | Active MinorStudents check | 2 |
| 5 | MinorStudent | user-management | **New** | None | 2 |
| 6 | Compensation | billing | **New** | None | 3 |
| 7 | Membership | billing | **New** | None | 3 |
| 8 | MembershipAdultStudent | billing | **New** | None | 3 |
| 9 | MembershipTutor | billing | **New** | None | 3 |
| 10 | PaymentAdultStudent | billing | **New** | None | 3 |
| 11 | PaymentTutor | billing | **New** | None | 3 |
| 12 | Course | course-management | **New** | None | 4 |
| 13 | Schedule | course-management | **New** | None | 4 |
| 14 | CourseEvent | course-management | **New** | None | 4 |
| 15 | StoreProduct | pos-system | **New** | None | 5 |
| 16 | StoreTransaction | pos-system | **New** | None | 5 |
| 17 | Tenant | tenant-management | **New** | Single @Id (no composite) | 6 |
| 18 | TenantSubscription | tenant-management | **New** | None | 6 |
| 19 | Notification | notification-system | **New** | None | 6 |

---

## Commit Summary (Expected Sequence)

```
1.  fix(multi-tenant-data): include entity ID in @SQLDelete WHERE clause
2.  feat(utilities): add DeleteUseCaseSupport and TenantContextHolder.requireTenantId
3.  feat(user-management): implement all 5 delete use cases
4.  feat(billing): implement 6 delete use cases
5.  feat(course-management): implement 3 delete use cases
6.  feat(pos-system): implement 2 delete use cases
7.  feat(tenant-management): implement 2 delete use cases
8.  feat(notification-system): implement DeleteNotificationUseCase
9.  test: add integration tests for soft-delete behavior
```

9 commits. Each atomic, each compilable, each with passing tests.

---

## Critical Reminders

1. **Phase 0 is URGENT** — the @SQLDelete bug causes tenant-wide data corruption. Fix it before any delete use case work.
2. **Hibernate @SQLDelete parameter order** matches `@Id` field declaration order in the class hierarchy. `tenantId` is always first (from `TenantScoped`), entity ID is always second (from concrete class). This means `WHERE tenant_id = ? AND entity_id = ?`.
3. **TenantDataModel is the only entity with a single @Id** — all 28 others use `@IdClass` composite keys. Its delete use case passes `tenantId` directly to `findById()`, not a composite ID.
4. **`DeleteUseCaseSupport` is composition, not inheritance** — use cases call static methods, they do NOT extend a base class. This keeps the use case classes simple and testable.
5. **`requireTenantId()`** replaces the scattered `getTenantId().orElseThrow(() -> new IllegalArgumentException(...))`. It throws `InvalidTenantException` which is handled by `BaseControllerAdvice` → 400.
6. **`String.valueOf(entityId)`** — `DeleteUseCaseSupport` takes `String entityId` for display. Always convert Long IDs.
7. **The Tutor business rule is the ONLY entity with a pre-delete check.** All other 18 entities use the simple `executeDelete()` path. If future entities need pre-delete validation, they use `findOrThrow()` + manual delete.
8. **Integration tests require Docker Desktop** — Testcontainers spins up MariaDB 11. The Maven Failsafe plugin runs `*IT.java` files during `mvn verify`.
9. **Read existing controller code** before wiring — the generated API interface method names come from OpenAPI `operationId`. Some use `delete`, others use `cancel` or other verbs.
10. **Exception infrastructure dependency** — this workflow assumes `docs/exception-advice-workflow.md` was executed first. If the generic exceptions don't exist yet, Phase 1 of THIS workflow will fail to compile.
