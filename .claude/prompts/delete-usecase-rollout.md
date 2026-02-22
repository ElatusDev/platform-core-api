# Delete UseCase Rollout — Claude Code Execution Prompt

**Target**: Claude Code CLI  
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`  
**Spec**: `docs/delete-usecase-strategy.md`, `docs/delete-usecase-workflow.md`  
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, `docs/delete-usecase-strategy.md` before starting.  
**Dependency**: `exception-advice-consolidation.md` MUST be fully executed first.  
`EntityNotFoundException`, `EntityDeletionNotAllowedException`, `EntityType`, and  
`BaseControllerAdvice` must already exist in the `utilities` module.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 0 → 1 → 2 → ... → 7).
2. Do NOT skip ahead. Each phase must compile and test before the next begins.
3. After EACH phase, run the specified verification command. Fix failures before proceeding.
4. All new files MUST include the ElatusDev copyright header.
5. All `public` classes and methods MUST have Javadoc.
6. Test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
7. Read existing DataModel files BEFORE writing any UseCase — field names and CompositeId class names vary.

---

## Phase 0: Fix @SQLDelete Bug (CRITICAL — silent data corruption)

**Scope**: `multi-tenant-data` module — 28 composite-key entities.  
**Problem**: Every `@SQLDelete` WHERE clause is `WHERE tenant_id = ?` only. With `@IdClass`
composite keys, Hibernate binds the entity ID as a second parameter — but it is never
used. Calling `repository.delete(entity)` soft-deletes ALL rows for the entire tenant.

**Note**: `TenantDataModel` is the ONLY entity with a single `@Id`. Its WHERE clause is
correct as-is. Do NOT modify it.

### Step 0.1: Audit all @SQLDelete annotations

```bash
grep -rn "@SQLDelete" multi-tenant-data/src/main/java/ | sort
```

Record every file path and current SQL. Confirm all composite-key entities use
`WHERE tenant_id = ?` only (the bug). Confirm `TenantDataModel` is the exception.

### Step 0.2: Read entity ID column names before fixing

For EACH entity, determine the exact `@Id` column name declared on the entity-specific
ID field (NOT `tenantId` — that is inherited from `TenantScoped`):

```bash
# Example — do this for every entity in the fix map below
grep -A5 "private Long.*Id" multi-tenant-data/src/main/java/com/akademiaplus/users/employee/EmployeeDataModel.java | grep -A3 "@Id\|@Column"
```

The fix map below assumes conventional naming. **Verify each one** before writing:

| Entity | Table | Entity ID column |
|--------|-------|-----------------|
| `EmployeeDataModel` | `employees` | `employee_id` |
| `CollaboratorDataModel` | `collaborators` | `collaborator_id` |
| `AdultStudentDataModel` | `adult_students` | `adult_student_id` |
| `TutorDataModel` | `tutors` | `tutor_id` |
| `MinorStudentDataModel` | `minor_students` | `minor_student_id` |
| `PersonPIIDataModel` | `person_piis` | `person_pii_id` |
| `InternalAuthDataModel` | `internal_auths` | `internal_auth_id` |
| `CustomerAuthDataModel` | `customer_auths` | `customer_auth_id` |
| `MembershipDataModel` | `memberships` | `membership_id` |
| `MembershipAdultStudentDataModel` | `membership_adult_students` | `membership_adult_student_id` |
| `MembershipTutorDataModel` | `membership_tutors` | `membership_tutor_id` |
| `PaymentAdultStudentDataModel` | `payment_adult_students` | `payment_adult_student_id` |
| `PaymentTutorDataModel` | `payment_tutors` | `payment_tutor_id` |
| `CompensationDataModel` | `compensations` | `compensation_id` |
| `CardPaymentInfoDataModel` | `card_payment_infos` | `card_payment_info_id` |
| `CourseDataModel` | `courses` | `course_id` |
| `ScheduleDataModel` | `schedules` | `schedule_id` |
| `CourseEventDataModel` | `course_events` | `course_event_id` |
| `StoreProductDataModel` | `store_products` | `store_product_id` |
| `StoreTransactionDataModel` | `store_transactions` | `store_transaction_id` |
| `StoreSaleItemDataModel` | `store_sale_items` | `store_sale_item_id` |
| `NotificationDataModel` | `notifications` | `notification_id` |
| `NotificationDeliveryDataModel` | `notification_deliveries` | `notification_delivery_id` |
| `EmailDataModel` | `emails` | `email_id` |
| `EmailAttachmentDataModel` | `email_attachments` | `email_attachment_id` |
| `EmailRecipientDataModel` | `email_recipients` | `email_recipient_id` |
| `TenantSubscriptionDataModel` | `tenant_subscriptions` | `tenant_subscription_id` |
| `TenantBillingCycleDataModel` | `tenant_billing_cycles` | `tenant_billing_cycle_id` |

### Step 0.3: Create the reflective validation test FIRST

Write the test before applying fixes so you can confirm the bug exists and then
confirm it is resolved.

**File**: `multi-tenant-data/src/test/java/com/akademiaplus/infra/persistence/SQLDeleteValidationTest.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
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
 * Validates that every {@code @SQLDelete} annotation in the codebase
 * includes all {@code @Id} columns in its WHERE clause.
 * <p>
 * Prevents the silent data-corruption bug where a missing entity ID column
 * causes {@code repository.delete(entity)} to soft-delete all rows for the
 * entire tenant instead of the single target row.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("@SQLDelete WHERE Clause Validation")
class SQLDeleteValidationTest {

    private static final String BASE_PACKAGE = "com.akademiaplus";

    @Test
    @DisplayName("Should include all @Id columns in every @SQLDelete WHERE clause")
    void shouldIncludeAllIdColumns_whenSQLDeleteDeclared() {
        // Given
        Reflections reflections = new Reflections(BASE_PACKAGE);
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);
        List<String> violations = new ArrayList<>();

        // When
        for (Class<?> entityClass : entityClasses) {
            SQLDelete sqlDelete = entityClass.getAnnotation(SQLDelete.class);
            if (sqlDelete == null) {
                continue;
            }

            String sql = sqlDelete.sql().toLowerCase();
            List<String> idColumns = collectIdColumns(entityClass);

            for (String column : idColumns) {
                if (!sql.contains(column.toLowerCase())) {
                    violations.add(entityClass.getSimpleName()
                            + ": @SQLDelete missing @Id column '"
                            + column + "' in WHERE clause. SQL: " + sqlDelete.sql());
                }
            }
        }

        // Then
        assertThat(violations)
                .as("All @SQLDelete annotations must include every @Id column. Violations:\n%s",
                        String.join("\n", violations))
                .isEmpty();
    }

    /**
     * Walks the full class hierarchy collecting all fields annotated with
     * {@code @Id} and returns their database column names.
     */
    private List<String> collectIdColumns(Class<?> clazz) {
        List<String> columns = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    Column col = field.getAnnotation(Column.class);
                    String columnName = (col != null && !col.name().isEmpty())
                            ? col.name()
                            : camelToSnake(field.getName());
                    columns.add(columnName);
                }
            }
            current = current.getSuperclass();
        }
        return columns;
    }

    private String camelToSnake(String camel) {
        return camel.replaceAll("([A-Z])", "_$1").toLowerCase().replaceFirst("^_", "");
    }
}
```

**Reflections library** — check if already present:
```bash
grep -rn "reflections" multi-tenant-data/pom.xml
```

If missing, add to `multi-tenant-data/pom.xml` `<dependencies>`:
```xml
<dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10.2</version>
    <scope>test</scope>
</dependency>
```

### Step 0.4: Run the validation test — it MUST FAIL

```bash
mvn test -pl multi-tenant-data -am -Dtest="SQLDeleteValidationTest"
```

The test output lists every violation. Confirm all 28 composite-key entities appear.
If the test passes, STOP — the bug may already be fixed or the entities are not being
scanned. Investigate before proceeding.

### Step 0.5: Apply all 28 fixes

For each entity in the fix map, find its `@SQLDelete` annotation and add the entity
ID column. The pattern is always:

```java
// Before:
@SQLDelete(sql = "UPDATE {table} SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")

// After:
@SQLDelete(sql = "UPDATE {table} SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND {entity_id_column} = ?")
```

Apply each fix using `edit_block` with exact old_string/new_string. Process all 28
entities before running the validation test again.

### Step 0.6: Run the validation test — it MUST PASS

```bash
mvn test -pl multi-tenant-data -am -Dtest="SQLDeleteValidationTest"
```

If any violations remain, fix them before proceeding.

### Step 0.7: Full compile

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests
```

### Step 0.8: Commit

```bash
git add -A
git commit -m "fix(multi-tenant-data): include entity ID in @SQLDelete WHERE clause

Critical bug: all 28 composite-key entities had @SQLDelete using only
WHERE tenant_id = ?, causing repository.delete() to soft-delete ALL rows
for the tenant instead of the single target row.

Fix: add entity-specific ID column to every @SQLDelete WHERE clause.
Add SQLDeleteValidationTest to prevent regression via reflection scan."
```

---

## Phase 1: Shared Delete Infrastructure

### Step 1.1: Add requireTenantId() to TenantContextHolder

Locate the file:
```bash
find . -name "TenantContextHolder.java" -path "*/main/*"
```

The existing `getTenantId()` returns `Optional<Long>`. Add:

```java
/**
 * Returns the current tenant ID or throws {@link InvalidTenantException}
 * if no tenant context has been set.
 * <p>
 * Convenience method that eliminates repeated
 * {@code .orElseThrow(() -> new InvalidTenantException())} calls
 * in every delete use case.
 *
 * @return the current tenant ID, never {@code null}
 * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
 *         if no tenant context is set
 */
public Long requireTenantId() {
    return getTenantId()
            .orElseThrow(InvalidTenantException::new);
}
```

Import: `import com.akademiaplus.utilities.exceptions.InvalidTenantException;`

Verify the `InvalidTenantException` exists in the `utilities` module (created by
the exception-advice-consolidation workflow). If it does not exist yet, this phase
must wait for that workflow to complete.

### Step 1.2: Create DeleteUseCaseSupport

**File**: `utilities/src/main/java/com/akademiaplus/utilities/usecases/DeleteUseCaseSupport.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.usecases;

import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Shared soft-delete execution logic for all delete use cases.
 * <p>
 * Encapsulates the find-or-throw-404 → try-delete → catch-constraint-409
 * pattern that is structurally identical across all 19 delete use cases.
 * Individual use cases compose this utility rather than inheriting from it.
 * <p>
 * For entities requiring pre-delete business rule checks (e.g., Tutor with
 * active MinorStudents), use {@link #findOrThrow} to retrieve the entity,
 * perform validation, then call {@code repository.delete()} directly.
 *
 * @author ElatusDev
 * @since 1.0
 */
public final class DeleteUseCaseSupport {

    private DeleteUseCaseSupport() {}

    /**
     * Executes soft-delete for a tenant-scoped entity with composite key.
     * <p>
     * Flow: findById(compositeId) → present? delete() : throw 404
     *       delete succeeds? return : catch DataIntegrityViolation → throw 409
     *
     * @param repository  the JPA repository for the entity
     * @param compositeId the composite key instance (tenantId + entityId)
     * @param entityType  message property key from
     *                    {@link com.akademiaplus.utilities.EntityType}
     * @param entityId    the entity-specific ID as a display string
     * @param <T>         the entity type
     * @param <ID>        the composite ID type
     * @throws EntityNotFoundException           if no entity matches the composite key
     * @throws EntityDeletionNotAllowedException if a FK constraint prevents deletion
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
     * Use when pre-delete validation is required before performing the delete.
     * After validation, call {@link JpaRepository#delete(Object)} inside a
     * try/catch for {@link DataIntegrityViolationException} and wrap with
     * {@link EntityDeletionNotAllowedException} if needed.
     *
     * @param repository  the JPA repository
     * @param compositeId the composite key instance
     * @param entityType  message property key from
     *                    {@link com.akademiaplus.utilities.EntityType}
     * @param entityId    the entity-specific ID as a display string
     * @param <T>         the entity type
     * @param <ID>        the composite ID type
     * @return the found entity, never {@code null}
     * @throws EntityNotFoundException if no entity matches the composite key
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
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.usecases;

import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("DeleteUseCaseSupport")
@ExtendWith(MockitoExtension.class)
class DeleteUseCaseSupportTest {

    private static final String ENTITY_TYPE = EntityType.EMPLOYEE;
    private static final String ENTITY_ID = "42";
    private static final Long COMPOSITE_ID = 42L;

    @Mock
    @SuppressWarnings("unchecked")
    private JpaRepository<Object, Long> repository;

    @Nested
    @DisplayName("executeDelete")
    class ExecuteDelete {

        @Test
        @DisplayName("Should call repository.delete when entity found")
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
                    DeleteUseCaseSupport.executeDelete(
                            repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(ENTITY_TYPE);
                        assertThat(enfe.getEntityId()).isEqualTo(ENTITY_ID);
                    });
        }

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed with cause when constraint violated")
        void shouldThrowDeletionNotAllowed_whenConstraintViolation() {
            // Given
            Object entity = new Object();
            DataIntegrityViolationException cause =
                    new DataIntegrityViolationException("FK constraint");
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.of(entity));
            doThrow(cause).when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() ->
                    DeleteUseCaseSupport.executeDelete(
                            repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
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
        @DisplayName("Should return entity when found by composite ID")
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
        @DisplayName("Should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFound_whenNotFound() {
            // Given
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() ->
                    DeleteUseCaseSupport.findOrThrow(
                            repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(ENTITY_TYPE);
                        assertThat(enfe.getEntityId()).isEqualTo(ENTITY_ID);
                    });
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

Add DeleteUseCaseSupport with executeDelete() (find-or-404 → delete →
catch-constraint-409) and findOrThrow() for pre-delete validation.

Add requireTenantId() to TenantContextHolder to replace scattered
orElseThrow(IllegalArgumentException::new) calls.

Unit tests for both methods with Given-When-Then."
```

---

## Phase 2: user-management — 5 Delete Use Cases

**Read these first** to get exact class names, package paths, and CompositeId names:
```bash
find user-management/src/main/java -name "Delete*UseCase.java" -o -name "*DataModel.java" | sort
find multi-tenant-data/src/main/java -name "*Employee*" -o -name "*Collaborator*" -o -name "*Tutor*" -o -name "*Minor*" -o -name "*Adult*" | sort
```

### Standard Delete UseCase Template

Use this template for all standard (no business rule) delete use cases.
Replace ALL placeholders (`<Entity>`, `<aggregate>`, `<datamodel-package>`, `<ENTITY_CONSTANT>`, `<CompositeId>`):

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.<aggregate>.usecases;

import com.akademiaplus.<aggregate>.interfaceadapters.<Entity>Repository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.<datamodel-package>.<Entity>DataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link <Entity>DataModel} by composite key.
 * <p>
 * Delegates the find-or-404 → delete → catch-constraint-409 pattern
 * to {@link DeleteUseCaseSupport#executeDelete}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class Delete<Entity>UseCase {

    private final <Entity>Repository repository;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Soft-deletes the {@link <Entity>DataModel} identified by the given ID
     * within the current tenant context.
     *
     * @param <entityId> the entity-specific ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no entity exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database FK constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long <entityId>) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                repository,
                new <Entity>DataModel.<CompositeId>(tenantId, <entityId>),
                EntityType.<ENTITY_CONSTANT>,
                String.valueOf(<entityId>));
    }
}
```

### Standard Delete UseCase Test Template

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.<aggregate>.usecases;

import com.akademiaplus.<aggregate>.interfaceadapters.<Entity>Repository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.<datamodel-package>.<Entity>DataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
        @DisplayName("Should call repository.delete when entity found by composite key")
        void shouldDeleteEntity_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            <Entity>DataModel entity = new <Entity>DataModel();
            <Entity>DataModel.<CompositeId> compositeId =
                    new <Entity>DataModel.<CompositeId>(TENANT_ID, ENTITY_ID);
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
        @DisplayName("Should throw EntityNotFoundException when entity does not exist")
        void shouldThrowEntityNotFound_whenEntityDoesNotExist() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            <Entity>DataModel.<CompositeId> compositeId =
                    new <Entity>DataModel.<CompositeId>(TENANT_ID, ENTITY_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(ENTITY_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType())
                                .isEqualTo(EntityType.<ENTITY_CONSTANT>);
                        assertThat(enfe.getEntityId())
                                .isEqualTo(String.valueOf(ENTITY_ID));
                    });
        }
    }

    @Nested
    @DisplayName("Constraint Violation")
    class ConstraintViolation {

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed when FK constraint violated")
        void shouldThrowDeletionNotAllowed_whenFkConstraintViolated() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            <Entity>DataModel entity = new <Entity>DataModel();
            <Entity>DataModel.<CompositeId> compositeId =
                    new <Entity>DataModel.<CompositeId>(TENANT_ID, ENTITY_ID);
            DataIntegrityViolationException cause =
                    new DataIntegrityViolationException("FK constraint");
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(cause).when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(ENTITY_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType())
                                .isEqualTo(EntityType.<ENTITY_CONSTANT>);
                        assertThat(edna.getReason()).isNull();
                        assertThat(edna.getCause()).isSameAs(cause);
                    });
        }
    }
}
```

### Step 2.1: Refactor DeleteEmployeeUseCase

Read the current file to understand existing structure:
```bash
find user-management/src/main/java -name "DeleteEmployeeUseCase.java"
cat <path>
```

Replace the entire implementation with the standard template:
- Package: read from existing file
- EntityType: `EntityType.EMPLOYEE`
- CompositeId: read `EmployeeDataModel` to find the exact inner class name

Remove old imports for `EmployeeNotFoundException`, `EmployeeDeletionNotAllowedException`.
Remove the `MessageService` dependency (no longer needed in the use case).

Create the unit test. The existing test (if any) references old exception types — rewrite it:
```bash
find user-management/src/test -name "DeleteEmployeeUseCaseTest.java"
```

### Step 2.2: Refactor DeleteCollaboratorUseCase

Same process. EntityType: `EntityType.COLLABORATOR`.

### Step 2.3: Refactor DeleteAdultStudentUseCase

Same process. EntityType: `EntityType.ADULT_STUDENT`.

### Step 2.4: Create DeleteTutorUseCase (SPECIAL — business rule)

Read the Tutor and MinorStudent DataModels first to understand the relationship:
```bash
cat multi-tenant-data/src/main/java/com/akademiaplus/users/customer/TutorDataModel.java
cat multi-tenant-data/src/main/java/com/akademiaplus/users/customer/MinorStudentDataModel.java
```

Identify:
1. The `@OneToMany` / FK relationship between Tutor and MinorStudent
2. Whether MinorStudent has a scalar `tutorId` field or a `@ManyToOne tutor` object field
3. The exact `@Id` inner class name on each

Add `countByTenantIdAndTutorId(Long tenantId, Long tutorId)` to `MinorStudentRepository`.
Adjust method name if the FK field is an object (use `Tutor_TutorId` suffix in derived query).

**File**: `user-management/src/main/java/com/akademiaplus/<tutor-package>/usecases/DeleteTutorUseCase.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.<tutor-package>.usecases;

import com.akademiaplus.<tutor-package>.interfaceadapters.TutorRepository;
import com.akademiaplus.<minorstudent-package>.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link TutorDataModel}.
 * <p>
 * Enforces the business rule that a Tutor with active (non-deleted)
 * MinorStudents cannot be deleted. The pre-delete check is performed
 * before the database operation.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class DeleteTutorUseCase {

    /**
     * Format string for the business rule deletion reason.
     * Contains the count of active minor students.
     * Referenced in tests using {@code String.format(ACTIVE_MINOR_STUDENTS_REASON, count)}.
     */
    public static final String ACTIVE_MINOR_STUDENTS_REASON =
            "Tutor tiene %d alumno(s) menor(es) activo(s)";

    private final TutorRepository tutorRepository;
    private final MinorStudentRepository minorStudentRepository;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Soft-deletes a Tutor after verifying they have no active MinorStudents.
     *
     * @param tutorId the tutor entity ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no tutor exists with the given composite key
     * @throws EntityDeletionNotAllowedException
     *         with business rule reason if active minor students exist,
     *         or with constraint cause if a FK prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
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

        long activeMinorStudents = minorStudentRepository
                .countByTenantIdAndTutorId(tenantId, tutorId);

        if (activeMinorStudents > 0) {
            throw new EntityDeletionNotAllowedException(
                    EntityType.TUTOR,
                    entityId,
                    String.format(ACTIVE_MINOR_STUDENTS_REASON, activeMinorStudents));
        }

        try {
            tutorRepository.delete(tutor);
        } catch (DataIntegrityViolationException ex) {
            throw new EntityDeletionNotAllowedException(EntityType.TUTOR, entityId, ex);
        }
    }
}
```

**Unit test for DeleteTutorUseCase** — use the standard template PLUS this additional `@Nested` class:

```java
    @Nested
    @DisplayName("Active Minor Students Business Rule")
    class ActiveMinorStudentsRule {

        @Test
        @DisplayName("Should throw business rule deletion error when tutor has active students")
        void shouldThrowBusinessRuleDeletion_whenTutorHasActiveMinorStudents() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            TutorDataModel tutor = new TutorDataModel();
            TutorDataModel.TutorCompositeId compositeId =
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);
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
                        assertThat(edna.getReason())
                                .isEqualTo(String.format(
                                        DeleteTutorUseCase.ACTIVE_MINOR_STUDENTS_REASON, 3));
                        assertThat(edna.getCause()).isNull();
                    });
        }

        @Test
        @DisplayName("Should delete tutor when no active minor students exist")
        void shouldDeleteTutor_whenNoActiveMinorStudents() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            TutorDataModel tutor = new TutorDataModel();
            TutorDataModel.TutorCompositeId compositeId =
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);
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

### Step 2.5: Create DeleteMinorStudentUseCase

Standard template. EntityType: `EntityType.MINOR_STUDENT`.

### Step 2.6: Wire controllers for Tutor and MinorStudent

Find the controllers:
```bash
find user-management/src/main/java -name "*Controller.java" | xargs grep -l "deleteTutor\|deleteMinorStudent"
```

For each controller, add the delete use case as a constructor dependency and delegate:

```java
@Override
public ResponseEntity<Void> deleteTutor(Long tutorId) {
    deleteTutorUseCase.delete(tutorId);
    return ResponseEntity.noContent().build();
}
```

Verify the exact method name from the generated API interface (OpenAPI operationId).

### Step 2.7: Compile + test + commit

```bash
mvn clean compile -pl user-management -am -DskipTests
mvn test -pl user-management -am
git add -A
git commit -m "feat(user-management): implement all 5 delete use cases

Refactor DeleteEmployeeUseCase, DeleteCollaboratorUseCase,
DeleteAdultStudentUseCase to use DeleteUseCaseSupport + generic exceptions.
Remove MessageService dependency from all three.

Add DeleteTutorUseCase with pre-delete business rule:
- Blocks deletion when active MinorStudents exist (409 DELETION_BUSINESS_RULE)
- Falls back to constraint check (409 DELETION_CONSTRAINT_VIOLATION)
- ACTIVE_MINOR_STUDENTS_REASON constant exposed for component test assertions

Add DeleteMinorStudentUseCase (standard pattern).

Wire TutorController and MinorStudentController delete methods.
Unit tests for all 5 use cases with Given-When-Then."
```

---

## Phase 3: billing — 6 Delete Use Cases

Read existing creation use cases to identify the correct package path for each entity:
```bash
find billing/src/main/java -name "*CreationUseCase.java" | sort
```

Apply the standard template for all 6. Read each DataModel for the exact `CompositeId` class name:
```bash
find multi-tenant-data/src/main/java -path "*/billing/*" -name "*DataModel.java" | xargs grep -l "CompositeId"
```

| UseCase | EntityType | Package (read from creation use case) |
|---------|-----------|--------------------------------------|
| `DeleteCompensationUseCase` | `COMPENSATION` | (read) |
| `DeleteMembershipUseCase` | `MEMBERSHIP` | (read) |
| `DeleteMembershipAdultStudentUseCase` | `MEMBERSHIP_ADULT_STUDENT` | (read) |
| `DeleteMembershipTutorUseCase` | `MEMBERSHIP_TUTOR` | (read) |
| `DeletePaymentAdultStudentUseCase` | `PAYMENT_ADULT_STUDENT` | (read) |
| `DeletePaymentTutorUseCase` | `PAYMENT_TUTOR` | (read) |

For each: create UseCase → create test → wire controller → compile.

```bash
mvn clean compile -pl billing -am -DskipTests
mvn test -pl billing -am
git add -A
git commit -m "feat(billing): implement 6 delete use cases

Add Delete*UseCase for Compensation, Membership, MembershipAdultStudent,
MembershipTutor, PaymentAdultStudent, PaymentTutor.

All use DeleteUseCaseSupport + generic exceptions.
Unit tests for all 6 with Given-When-Then."
```

---

## Phase 4: course-management — 3 Delete Use Cases

Read existing creation use cases for package paths:
```bash
find course-management/src/main/java -name "*CreationUseCase.java" | sort
```

| UseCase | EntityType | Notes |
|---------|-----------|-------|
| `DeleteCourseUseCase` | `COURSE` | Standard |
| `DeleteScheduleUseCase` | `COURSE` (verify — may need `SCHEDULE` constant) | Check EntityType |
| `DeleteCourseEventUseCase` | `COURSE_EVENT` | Standard |

**Schedule note**: Read `EntityType.java` to confirm whether a `SCHEDULE` constant exists.
If not, add it: `public static final String SCHEDULE = "entity.schedule";` and add the
message key to the relevant properties file.

For each: create UseCase → create test → wire controller → compile.

```bash
mvn clean compile -pl course-management -am -DskipTests
mvn test -pl course-management -am
git add -A
git commit -m "feat(course-management): implement 3 delete use cases

Add DeleteCourseUseCase, DeleteScheduleUseCase, DeleteCourseEventUseCase.
All use DeleteUseCaseSupport + generic exceptions.
Unit tests with Given-When-Then."
```

---

## Phase 5: pos-system — 2 Delete Use Cases

```bash
find pos-system/src/main/java -name "*CreationUseCase.java" | sort
```

| UseCase | EntityType |
|---------|-----------|
| `DeleteStoreProductUseCase` | `STORE_PRODUCT` |
| `DeleteStoreTransactionUseCase` | `STORE_TRANSACTION` |

For each: create UseCase → create test → wire controller → compile.

```bash
mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am
git add -A
git commit -m "feat(pos-system): implement 2 delete use cases

Add DeleteStoreProductUseCase, DeleteStoreTransactionUseCase.
Standard pattern with DeleteUseCaseSupport + generic exceptions.
Unit tests with Given-When-Then."
```

---

## Phase 6: tenant-management + notification-system — 3 Delete Use Cases

### tenant-management

Read the tenant entities:
```bash
find tenant-management/src/main/java -name "*CreationUseCase.java"
find multi-tenant-data/src/main/java -name "Tenant*DataModel.java" | xargs grep -l "@Id"
```

**TenantDataModel** uses `@GeneratedValue` with a SINGLE `@Id` (no `@IdClass`). Its
delete use case does NOT use a composite key:

```java
@Transactional
public void delete(Long tenantId) {
    DeleteUseCaseSupport.executeDelete(
            repository,
            tenantId,  // single Long — not a composite key object
            EntityType.TENANT,
            String.valueOf(tenantId));
}
```

**TenantSubscription** — check the OpenAPI for the operationId. It may be
`cancelTenantSubscription` (not `deleteTenantSubscription`). Read the controller:
```bash
grep -rn "cancel\|delete" tenant-management/src/main/java --include="*Controller.java"
```

Wire the controller method that matches the generated interface regardless of its name.

**Note on cascading tenant deletion**: Deleting a `Tenant` could cascade to all
tenant-scoped entities. For now, the standard soft-delete pattern applies — cascade
is out of scope and should be tracked as a future ADR. Add a Javadoc warning:

```java
/**
 * @implNote Cascade soft-deletion of child entities (users, memberships, etc.)
 *           on tenant deletion is not implemented. Future work: ADR-XXXX.
 */
```

```bash
mvn clean compile -pl tenant-management -am -DskipTests
mvn test -pl tenant-management -am
git add -A
git commit -m "feat(tenant-management): implement 2 delete use cases

Add DeleteTenantUseCase (single @Id, no composite key).
Add DeleteTenantSubscriptionUseCase (composite key, standard pattern).

Note: cascading tenant soft-deletion is out of scope.
Future ADR required for multi-entity tenant teardown."
```

### notification-system

```bash
find notification-system/src/main/java -name "*CreationUseCase.java"
```

Create `DeleteNotificationUseCase` (standard template), test, wire controller.

```bash
mvn clean compile -pl notification-system -am -DskipTests
mvn test -pl notification-system -am
git add -A
git commit -m "feat(notification-system): implement DeleteNotificationUseCase

Standard pattern with DeleteUseCaseSupport + generic exceptions.
Unit test with Given-When-Then."
```

### Full project build

```bash
mvn clean install
```

All 19 delete use cases exist. All compile. All unit tests pass.

---

## Phase 7: Integration Tests for Delete Behavior

Verify actual JPA + MariaDB behavior. Requires Docker Desktop running.

First, check existing integration test infrastructure:
```bash
find . -name "AbstractIntegrationTest.java" -path "*/test/*"
find . -name "*ComponentTest.java" -path "*/test/*"
grep -rn "@Testcontainers\|@Container\|MariaDBContainer" --include="*.java" */src/test/
```

Follow the exact setup pattern from existing integration tests (class annotations,
`@DynamicPropertySource`, tenant creation helpers, etc.).

### Step 7.1: @SQLDelete single-row verification

**File**: `user-management/src/test/java/com/akademiaplus/usecases/EmployeeSQLDeleteComponentTest.java`

This integrates with the full Spring context and verifies:

1. Create 2 Employee entities for the same tenant
2. Delete employee1 via repository
3. Verify via native SQL:
   - `employee1.deleted_at IS NOT NULL`
   - `employee2.deleted_at IS NULL`
4. Verify `employeeRepository.findById(employee1CompositeId)` returns empty
   (confirming `@SQLRestriction` excludes soft-deleted from JPA queries)

Use `EntityManager.createNativeQuery()` to bypass `@SQLRestriction`:
```java
String sql = "SELECT deleted_at FROM employees WHERE tenant_id = ? AND employee_id = ?";
```

### Step 7.2: FK constraint → HTTP 409 end-to-end

**File**: `course-management/src/test/java/com/akademiaplus/usecases/CourseDeleteConstraintComponentTest.java`

Setup:
1. Create a Course
2. Create a Schedule referencing the Course (FK)
3. Attempt `DELETE /courses/{courseId}`
4. Assert: HTTP 409, `$.code = "DELETION_CONSTRAINT_VIOLATION"`

### Step 7.3: Tutor business rule end-to-end

**File**: `user-management/src/test/java/com/akademiaplus/usecases/TutorDeleteBusinessRuleComponentTest.java`

Test 1 — blocked:
1. Create Tutor
2. Create 2 MinorStudents linked to Tutor
3. Attempt `DELETE /tutors/{tutorId}`
4. Assert: HTTP 409, `$.code = "DELETION_BUSINESS_RULE"`,
   `$.message` contains `"alumno(s) menor(es) activo(s)"`

Test 2 — allowed:
1. Create Tutor with no MinorStudents
2. `DELETE /tutors/{tutorId}`
3. Assert: HTTP 204

### Step 7.4: Verify + commit

```bash
mvn verify -pl user-management -am
mvn verify -pl course-management -am
git add -A
git commit -m "test(integration): add soft-delete behavior verification tests

EmployeeSQLDeleteComponentTest: verifies single-row @SQLDelete
correctness and @SQLRestriction exclusion of soft-deleted entities.

CourseDeleteConstraintComponentTest: verifies FK constraint → HTTP 409
DELETION_CONSTRAINT_VIOLATION end-to-end.

TutorDeleteBusinessRuleComponentTest: verifies active-minor-student
business rule → HTTP 409 DELETION_BUSINESS_RULE with reason message,
and confirms clean deletion when no active students exist."
```

---

## Final Verification Checklist

After all 7 phases complete, run these checks in sequence:

```bash
# 1. Full build with all tests
mvn clean install

# 2. @SQLDelete covers all 28 composite-key entities
grep -rn "AND.*_id = ?" multi-tenant-data/src/main/java/ --include="*.java" | wc -l
# Expected: 28

# 3. Delete use cases exist for all 19 entities
find . -name "Delete*UseCase.java" -path "*/main/*" | wc -l
# Expected: 19

# 4. Delete use case tests exist for all 19 entities
find . -name "Delete*UseCaseTest.java" -path "*/test/*" | wc -l
# Expected: 19

# 5. No old entity-specific deletion exceptions in production code
grep -rn "DeletionNotAllowedException" --include="*.java" */src/main/java/ | grep -v "Generic\|EntityDeletion" | wc -l
# Expected: 0

# 6. No IllegalArgumentException for missing tenant context
grep -rn "IllegalArgumentException.*[Tt]enant\|[Tt]enant.*IllegalArgumentException" --include="*.java" */src/main/java/ | wc -l
# Expected: 0

# 7. All controllers have delete response wired
grep -rn "noContent().build()" --include="*.java" */src/main/java/ | wc -l
# Expected: >= 19

# 8. @SQLDelete validation test still passes
mvn test -pl multi-tenant-data -am -Dtest="SQLDeleteValidationTest"
# Expected: BUILD SUCCESS, 1 test passed
```
