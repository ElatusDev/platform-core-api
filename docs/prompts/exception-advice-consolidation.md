# Exception Handling Consolidation — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Spec**: `docs/design/exception-advice-specification.md`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and the specification before starting.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 1 → 2 → 3 → ... → 10).
2. Do NOT skip ahead. Each phase depends on the previous one compiling.
3. After EACH phase, run the specified verification command. Fix failures before proceeding.
4. All new files MUST include the ElatusDev copyright header.
5. All new `public` classes and methods MUST have Javadoc.
6. All constants MUST be `public static final`.
7. All test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.

---

## Phase 1: EntityType Constants Class

**Goal**: Single source of truth for entity type identifiers. These are message property keys.

**Create file**: `utilities/src/main/java/com/akademiaplus/utilities/EntityType.java`

```java
package com.akademiaplus.utilities;

/**
 * Canonical entity type identifiers used in exception messages and error responses.
 * <p>
 * Each constant corresponds to a message property key in
 * {@code utilities_messages_es_MX.properties} that resolves to a localized
 * display name (e.g., {@code entity.employee} → "Empleado").
 */
public final class EntityType {
    private EntityType() {}

    // user-management
    public static final String EMPLOYEE = "entity.employee";
    public static final String COLLABORATOR = "entity.collaborator";
    public static final String ADULT_STUDENT = "entity.adult.student";
    public static final String TUTOR = "entity.tutor";
    public static final String MINOR_STUDENT = "entity.minor.student";

    // billing
    public static final String MEMBERSHIP = "entity.membership";
    public static final String MEMBERSHIP_ADULT_STUDENT = "entity.membership.adult.student";
    public static final String MEMBERSHIP_TUTOR = "entity.membership.tutor";
    public static final String PAYMENT_ADULT_STUDENT = "entity.payment.adult.student";
    public static final String PAYMENT_TUTOR = "entity.payment.tutor";
    public static final String COMPENSATION = "entity.compensation";

    // course-management
    public static final String COURSE = "entity.course";
    public static final String COURSE_EVENT = "entity.course.event";

    // pos-system
    public static final String STORE_PRODUCT = "entity.store.product";
    public static final String STORE_TRANSACTION = "entity.store.transaction";

    // notification
    public static final String NOTIFICATION = "entity.notification";

    // tenant
    public static final String TENANT = "entity.tenant";
    public static final String TENANT_SUBSCRIPTION = "entity.tenant.subscription";
    public static final String TENANT_BILLING_CYCLE = "entity.tenant.billing.cycle";
}
```

**Verify**:
```bash
mvn clean compile -pl utilities -am -DskipTests
```

---

## Phase 2: New Generic Exception Classes

**Goal**: Create 3 generic exceptions in `utilities` that replace 16 per-entity classes.

All exceptions go in: `utilities/src/main/java/com/akademiaplus/utilities/exceptions/`

### 2.1 EntityNotFoundException

**Create file**: `utilities/src/main/java/com/akademiaplus/utilities/exceptions/EntityNotFoundException.java`

```java
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when an entity lookup by composite key returns no result.
 * <p>
 * Replaces all per-entity NotFound exceptions (EmployeeNotFoundException,
 * CollaboratorNotFoundException, etc.) with a single generic class.
 * The {@code entityType} field holds a message property key from
 * {@link com.akademiaplus.utilities.EntityType} that resolves to a localized
 * display name via {@link com.akademiaplus.utilities.MessageService}.
 *
 * @see com.akademiaplus.utilities.EntityType
 */
public class EntityNotFoundException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    /**
     * Creates a new entity-not-found exception.
     *
     * @param entityType message property key (e.g., {@code EntityType.EMPLOYEE})
     * @param entityId   the ID that was not found
     */
    public EntityNotFoundException(String entityType, String entityId) {
        super("Entity not found: type=" + entityType + ", id=" + entityId);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
}
```

### 2.2 EntityDeletionNotAllowedException

**Create file**: `utilities/src/main/java/com/akademiaplus/utilities/exceptions/EntityDeletionNotAllowedException.java`

```java
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when an entity cannot be deleted due to a database constraint
 * violation or a business rule.
 * <p>
 * Two constructor variants exist:
 * <ul>
 *   <li><strong>Constraint variant</strong>: wraps a {@link Throwable} cause
 *       (typically {@code DataIntegrityViolationException}). The {@code reason}
 *       field will be {@code null}.</li>
 *   <li><strong>Business rule variant</strong>: carries a human-readable
 *       {@code reason} string (e.g., "Tutor has 3 active minor students").</li>
 * </ul>
 * The handler in {@code BaseControllerAdvice} checks {@link #getReason()} to
 * determine which HTTP response code to use:
 * {@code DELETION_CONSTRAINT_VIOLATION} vs {@code DELETION_BUSINESS_RULE}.
 *
 * @see com.akademiaplus.utilities.EntityType
 */
public class EntityDeletionNotAllowedException extends RuntimeException {

    private final String entityType;
    private final String entityId;
    private final String reason;

    /**
     * Constraint variant — wraps a database integrity violation.
     *
     * @param entityType message property key
     * @param entityId   the entity ID that cannot be deleted
     * @param cause      the underlying database exception
     */
    public EntityDeletionNotAllowedException(String entityType, String entityId, Throwable cause) {
        super("Deletion not allowed: type=" + entityType + ", id=" + entityId, cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.reason = null;
    }

    /**
     * Business rule variant — carries a descriptive reason.
     *
     * @param entityType message property key
     * @param entityId   the entity ID that cannot be deleted
     * @param reason     human-readable business rule description
     */
    public EntityDeletionNotAllowedException(String entityType, String entityId, String reason) {
        super("Deletion not allowed: type=" + entityType + ", id=" + entityId + ", reason=" + reason);
        this.entityType = entityType;
        this.entityId = entityId;
        this.reason = reason;
    }

    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getReason() { return reason; }
}
```

### 2.3 DuplicateEntityException

**Create file**: `utilities/src/main/java/com/akademiaplus/utilities/exceptions/DuplicateEntityException.java`

```java
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when a create or update operation violates a unique constraint
 * on a known field (e.g., email, phone number).
 * <p>
 * Replaces the pattern of parsing {@code DataIntegrityViolationException}
 * cause messages for "email" or "phone" substrings inside ControllerAdvice
 * handlers. The throwing service identifies the duplicate field and wraps
 * the original exception.
 *
 * @see com.akademiaplus.utilities.EntityType
 */
public class DuplicateEntityException extends RuntimeException {

    private final String entityType;
    private final String field;

    /**
     * Creates a new duplicate entity exception.
     *
     * @param entityType message property key
     * @param field      the field that violated uniqueness (e.g., "email", "phoneNumber")
     * @param cause      the underlying database exception
     */
    public DuplicateEntityException(String entityType, String field, Throwable cause) {
        super("Duplicate " + field + " for entity type=" + entityType, cause);
        this.entityType = entityType;
        this.field = field;
    }

    public String getEntityType() { return entityType; }
    public String getField() { return field; }
}
```

**Verify Phase 2**:
```bash
mvn clean compile -pl utilities -am -DskipTests
```

---

## Phase 3: Message Properties Updates

**Goal**: Add new message keys. Do NOT modify or remove any existing keys.

### 3.1 Add to `utilities/src/main/resources/messages/utilities_messages_es_MX.properties`

Append these lines at the end of the file:

```properties

# Tenant entity display names
entity.tenant=Organizacion
entity.tenant.subscription=Suscripcion de Organizacion
entity.tenant.billing.cycle=Ciclo de Facturacion
```

### 3.2 Add to `user-management/src/main/resources/messages/user_management_messages_es_MX.properties`

Append these lines at the end of the file:

```properties

# Generic deletion with business reason
entity.delete.not.allowed.reason=Eliminacion de {0} con ID: {1} no es posible: {2}
# Duplicate field constraint
entity.duplicate.field=Error: el campo {1} del {0} ya esta registrado
# Generic unclassified constraint violation
entity.constraint.violation=Operacion no permitida: conflicto de integridad de datos
# Invalid tenant context
invalid.tenant=Contexto de organizacion es requerido
```

**Verify Phase 3**:
```bash
mvn clean compile -pl utilities -am -DskipTests
```

---

## Phase 4: MessageService Refactoring

**Goal**: Add 4 new generic methods. Mark 20 old per-entity methods `@Deprecated`. Do NOT remove old methods yet.

**Edit file**: `utilities/src/main/java/com/akademiaplus/utilities/MessageService.java`

### 4.1 Add New Constants

Add after the existing `ENTITY_DELETE_NOT_ALLOWED` constant:

```java
    private static final String ENTITY_DELETE_NOT_ALLOWED_REASON = "entity.delete.not.allowed.reason";
    private static final String ENTITY_DUPLICATE_FIELD = "entity.duplicate.field";
    private static final String ENTITY_CONSTRAINT_VIOLATION = "entity.constraint.violation";
    private static final String INVALID_TENANT = "invalid.tenant";
```

### 4.2 Add New Generic Methods

Add these AFTER the existing `getEntityDeleteNotAllowed(String entityName)` private method. Make the existing private `getEntityNotFound(String entityName, String id)` and `getEntityDeleteNotAllowed(String entityName)` methods `public` and change their signatures to accept the entity type key and resolve it:

```java
    // ── NEW GENERIC METHODS ─────────────────────────────────────────────

    /**
     * Returns a localized "entity not found" message by resolving the entity
     * type key to a display name first, then formatting the not-found template.
     *
     * @param entityTypeKey message property key from {@link EntityType}
     * @param entityId      the ID that was not found
     * @return formatted message, e.g., "Empleado con ID: 42 no existe!"
     */
    public String getGenericEntityNotFound(String entityTypeKey, String entityId) {
        String entityName = messageSource.getMessage(entityTypeKey, null, locale);
        return messageSource.getMessage(ENTITY_NOT_FOUND, new Object[]{entityName, entityId}, locale);
    }

    /**
     * Returns a localized "deletion not allowed" message.
     *
     * @param entityTypeKey message property key from {@link EntityType}
     * @return formatted message, e.g., "Eliminacion de Empleado no esta permitida!..."
     */
    public String getGenericEntityDeleteNotAllowed(String entityTypeKey) {
        String entityName = messageSource.getMessage(entityTypeKey, null, locale);
        return messageSource.getMessage(ENTITY_DELETE_NOT_ALLOWED, new Object[]{entityName}, locale);
    }

    /**
     * Returns a localized "deletion not allowed" message with a business rule reason.
     *
     * @param entityTypeKey message property key from {@link EntityType}
     * @param entityId      the entity ID
     * @param reason        human-readable business rule description
     * @return formatted message
     */
    public String getEntityDeleteNotAllowedWithReason(String entityTypeKey, String entityId, String reason) {
        String entityName = messageSource.getMessage(entityTypeKey, null, locale);
        return messageSource.getMessage(ENTITY_DELETE_NOT_ALLOWED_REASON,
                new Object[]{entityName, entityId, reason}, locale);
    }

    /**
     * Returns a localized "duplicate field" message.
     *
     * @param entityTypeKey message property key from {@link EntityType}
     * @param field         the field that violated uniqueness
     * @return formatted message, e.g., "Error: el campo email del Empleado ya esta registrado"
     */
    public String getEntityDuplicateField(String entityTypeKey, String field) {
        String entityName = messageSource.getMessage(entityTypeKey, null, locale);
        return messageSource.getMessage(ENTITY_DUPLICATE_FIELD,
                new Object[]{entityName, field}, locale);
    }

    /**
     * Returns a generic constraint violation message.
     *
     * @return formatted message
     */
    public String getConstraintViolation() {
        return messageSource.getMessage(ENTITY_CONSTRAINT_VIOLATION, null, locale);
    }

    /**
     * Returns an "invalid tenant" message.
     *
     * @return formatted message
     */
    public String getInvalidTenant() {
        return messageSource.getMessage(INVALID_TENANT, null, locale);
    }
```

### 4.3 Deprecate Old Per-Entity Methods

Add `@Deprecated(since = "1.1", forRemoval = true)` to ALL 20 per-entity methods:

**NotFound methods to deprecate (17)**:
- `getAdultStudentNotFound(String id)`
- `getCollaboratorNotFound(String id)`
- `getEmployeeNotFound(String id)`
- `getTutorNotFound(String id)`
- `getMinorStudentNotFound(String id)`
- `getCourseNotFound(String id)`
- `getCourseEventNotFound(String id)`
- `getMembershipNotFound(String id)`
- `getMembershipAdultStudentNotFound(String id)`
- `getMembershipTutorNotFound(String id)`
- `getPaymentAdultStudentNotFound(String id)`
- `getPaymentTutorNotFound(String id)`
- `getCompensationNotFound(String id)`
- `getNotificationNotFound(String id)`
- `getStoreProductNotFound(String id)`
- `getStoreTransactionNotFound(String id)`
- private `getEntityNotFound(String entityName, String id)` — make it `@Deprecated` too

**DeletionNotAllowed methods to deprecate (3)**:
- `getAdultStudentDeleteNotAllowed()`
- `getCollaboratorDeleteNotAllowed()`
- `getEmployeeDeleteNotAllowed()`

Do NOT remove any of these methods. They must remain functional for the one-release deprecation cycle.

**Verify Phase 4**:
```bash
mvn clean compile -pl utilities -am -DskipTests
```

---

## Phase 5: BaseControllerAdvice

**Goal**: Create the abstract base class with 8 shared exception handlers in `utilities`.

**Create file**: `utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java`

Read the exact handler implementations from **Section 6.1** of `docs/design/exception-advice-specification.md`. The class must:

1. Be `public abstract class BaseControllerAdvice` (NOT annotated with `@ControllerAdvice` — subclasses do that)
2. Constructor-inject `MessageService` only
3. Expose `protected MessageService messageService()` accessor for subclasses
4. Contain exactly these 8 `@ExceptionHandler` methods:

| # | Exception | HTTP | Code | Handler Method Name |
|---|-----------|------|------|---------------------|
| 1 | `EntityNotFoundException` | 404 | `ENTITY_NOT_FOUND` | `handleEntityNotFound` |
| 2 | `EntityDeletionNotAllowedException` | 409 | `DELETION_CONSTRAINT_VIOLATION` or `DELETION_BUSINESS_RULE` (check `getReason()`) | `handleDeletionNotAllowed` |
| 3 | `DuplicateEntityException` | 409 | `DUPLICATE_ENTITY` | `handleDuplicateEntity` |
| 4 | `DataIntegrityViolationException` | 409 | `DATA_INTEGRITY_VIOLATION` | `handleDataIntegrityViolation` |
| 5 | `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` (populate `details` from field errors) | `handleValidation` |
| 6 | `InvalidTenantException` | 400 | `INVALID_TENANT` | `handleInvalidTenant` |
| 7 | `EncryptionFailureException` + `DecryptionFailureException` | 500 | `INTERNAL_ERROR` | `handleCryptoFailure` |
| 8 | `Exception` | 500 | `INTERNAL_ERROR` | `handleUnexpected` |

**CRITICAL implementation details**:
- Handler #2: Use `if (ex.getReason() != null)` to branch between constraint and business rule variants
- Handler #4: This is the FALLBACK for unclassified `DataIntegrityViolationException`. Do NOT parse cause messages. Use `messageService.getConstraintViolation()`
- Handler #5: Populate the `details` list with field-level errors from `BindingResult`
- Handler #8: This is the catch-all. Log at ERROR level.
- ALL handlers: Set BOTH `error.setMessage(...)` AND `error.setCode(...)` on ErrorResponseDTO

**Required imports**:
```java
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.*;
import com.akademiaplus.utilities.exceptions.security.DecryptionFailureException;
import com.akademiaplus.utilities.exceptions.security.EncryptionFailureException;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import openapi.akademiaplus.domain.utilities.dto.ErrorDetailDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
```

**Verify Phase 5**:
```bash
mvn clean compile -pl utilities -am -DskipTests
```

NOTE: This will require `openapi-generator` to have run for `utilities` module so that `ErrorResponseDTO` and `ErrorDetailDTO` exist in `target/generated-sources`. If compilation fails due to missing DTOs, run:
```bash
mvn clean generate-sources -pl utilities -am -DskipTests
```
Then retry the compile.

---

## Phase 6: Unit Tests for New Infrastructure

**Goal**: Test all 3 exception classes, the new MessageService methods, and all BaseControllerAdvice handlers.

### 6.1 Exception Unit Tests

**Create file**: `utilities/src/test/java/com/akademiaplus/utilities/exceptions/EntityNotFoundExceptionTest.java`

Test:
- Constructor stores `entityType` and `entityId` correctly
- `getMessage()` contains both values
- Getters return exact constructor arguments

**Create file**: `utilities/src/test/java/com/akademiaplus/utilities/exceptions/EntityDeletionNotAllowedExceptionTest.java`

Test:
- Constraint constructor: `getReason()` returns `null`, `getCause()` returns the Throwable
- Business rule constructor: `getReason()` returns the string, `getCause()` returns `null`
- Both constructors store `entityType` and `entityId`

**Create file**: `utilities/src/test/java/com/akademiaplus/utilities/exceptions/DuplicateEntityExceptionTest.java`

Test:
- Constructor stores `entityType`, `field`, and cause
- Getters return exact constructor arguments

### 6.2 BaseControllerAdvice Tests

**Create file**: `utilities/src/test/java/com/akademiaplus/utilities/web/BaseControllerAdviceTest.java`

Since `BaseControllerAdvice` is abstract, create a concrete inner subclass for testing:

```java
@DisplayName("BaseControllerAdvice")
@ExtendWith(MockitoExtension.class)
class BaseControllerAdviceTest {

    @Mock private MessageService messageService;

    private TestableControllerAdvice advice;

    /** Concrete subclass for testing the abstract base. */
    static class TestableControllerAdvice extends BaseControllerAdvice {
        TestableControllerAdvice(MessageService messageService) {
            super(messageService);
        }
    }

    @BeforeEach
    void setUp() {
        advice = new TestableControllerAdvice(messageService);
    }

    // ... nested test classes below
}
```

**Required test cases** (one `@Nested` class per handler):

```
@Nested @DisplayName("handleEntityNotFound")
  - shouldReturn404WithEntityNotFoundCode_whenEntityNotFoundExceptionThrown()
    Given: EntityNotFoundException with entityType + entityId, messageService returns localized msg
    When:  handleEntityNotFound(ex)
    Then:  ResponseEntity status=404, body.code="ENTITY_NOT_FOUND", body.message=localized msg
    Verify: messageService.getGenericEntityNotFound(entityType, entityId) called exactly once

@Nested @DisplayName("handleDeletionNotAllowed")
  - shouldReturn409WithConstraintCode_whenCauseIsThrowable()
    Given: EntityDeletionNotAllowedException(type, id, new RuntimeException()), reason is null
    Then:  status=409, code="DELETION_CONSTRAINT_VIOLATION"
    Verify: messageService.getGenericEntityDeleteNotAllowed(entityType) called

  - shouldReturn409WithBusinessRuleCode_whenReasonIsPresent()
    Given: EntityDeletionNotAllowedException(type, id, "has active students")
    Then:  status=409, code="DELETION_BUSINESS_RULE"
    Verify: messageService.getEntityDeleteNotAllowedWithReason(type, id, reason) called

@Nested @DisplayName("handleDuplicateEntity")
  - shouldReturn409WithDuplicateCode_whenDuplicateEntityExceptionThrown()
    Then:  status=409, code="DUPLICATE_ENTITY"
    Verify: messageService.getEntityDuplicateField(entityType, field) called

@Nested @DisplayName("handleDataIntegrityViolation")
  - shouldReturn409WithDataIntegrityCode_whenDataIntegrityViolationExceptionThrown()
    Then:  status=409, code="DATA_INTEGRITY_VIOLATION"
    Verify: messageService.getConstraintViolation() called

@Nested @DisplayName("handleValidation")
  - shouldReturn400WithDetailsArray_whenValidationFails()
    Given: mock MethodArgumentNotValidException with 2 field errors
    Then:  status=400, code="VALIDATION_ERROR", details.size()=2,
           details[0].field and details[0].message match field errors

@Nested @DisplayName("handleInvalidTenant")
  - shouldReturn400WithInvalidTenantCode_whenInvalidTenantExceptionThrown()
    Then:  status=400, code="INVALID_TENANT"
    Verify: messageService.getInvalidTenant() called

@Nested @DisplayName("handleCryptoFailure")
  - shouldReturn500_whenEncryptionFailureExceptionThrown()
  - shouldReturn500_whenDecryptionFailureExceptionThrown()
    Both: status=500, code="INTERNAL_ERROR"

@Nested @DisplayName("handleUnexpected")
  - shouldReturn500_whenUnhandledExceptionThrown()
    Then:  status=500, code="INTERNAL_ERROR"
```

**Test conventions**:
- Use `public static final` constants for test entity type, entity ID, field, reason, etc.
- Reference `EntityType.EMPLOYEE` (or similar) for entityType in tests
- Mock `MessageService` method calls with exact arguments, verify exact interactions
- Assert both HTTP status AND response body fields (code, message)
- For MethodArgumentNotValidException: mock `getBindingResult()`, `getFieldErrors()`, create `FieldError` objects

**Verify Phase 6**:
```bash
mvn clean test -pl utilities -am
```

---

## Phase 7: Refactor Module ControllerAdvice Classes

**Goal**: Each module's `ControllerAdvice` extends `BaseControllerAdvice`. Remove all handlers that are now inherited. Keep only domain-specific handlers.

Process each file IN THIS ORDER. After each file, run the module compile to catch errors immediately.

### 7.1 PeopleControllerAdvice (user-management)

**Edit file**: `user-management/src/main/java/com/akademiaplus/config/PeopleControllerAdvice.java`

**Before**: 8 explicit handlers (5 NotFound + 3 DeletionNotAllowed + DataIntegrity + Validation + Encryption)
**After**: 0 handlers — extends BaseControllerAdvice, all handlers inherited

Changes:
1. Add `extends BaseControllerAdvice`
2. Replace constructor body with `super(messageService);`
3. **DELETE** all 8 `@ExceptionHandler` methods entirely
4. Remove unused imports for per-entity exception classes
5. Keep the `@ControllerAdvice(basePackageClasses = {...})` annotation unchanged

```bash
mvn clean compile -pl user-management -am -DskipTests
```

### 7.2 BillingControllerAdvice (billing)

**Edit file**: `billing/src/main/java/com/akademiaplus/config/BillingControllerAdvice.java`

**Before**: 6 NotFound handlers
**After**: 0 handlers

Same pattern: extend BaseControllerAdvice, delete all handlers, super(messageService).

```bash
mvn clean compile -pl billing -am -DskipTests
```

### 7.3 CoordinationControllerAdvice (course-management)

**Edit file**: `course-management/src/main/java/com/akademiaplus/config/CoordinationControllerAdvice.java`

**Before**: 3 NotFound + ScheduleNotAvailable + CollaboratorNotFound handlers
**After**: 1 handler — `ScheduleNotAvailableException` only

Changes:
1. Add `extends BaseControllerAdvice`
2. Replace constructor body with `super(messageService);`
3. **DELETE** all NotFound and CollaboratorNotFound handlers
4. **KEEP** the `ScheduleNotAvailableException` handler but update it:
   - Set `error.setCode("SCHEDULE_CONFLICT")`
   - Use `messageService().getScheduleNotAvailable(ex.getMessage())` (use `messageService()` accessor from base)
5. Return `HttpStatus.CONFLICT` (409) — verify this is what it already uses

```bash
mvn clean compile -pl course-management -am -DskipTests
```

### 7.4 PosControllerAdvice (pos-system)

**Edit file**: `pos-system/src/main/java/com/akademiaplus/config/PosControllerAdvice.java`

**Before**: 2 NotFound handlers
**After**: 0 handlers

```bash
mvn clean compile -pl pos-system -am -DskipTests
```

### 7.5 NotificationControllerAdvice (notification-system)

**Edit file**: `notification-system/src/main/java/com/akademiaplus/config/NotificationControllerAdvice.java`

**Before**: 1 NotFound handler
**After**: 0 handlers

```bash
mvn clean compile -pl notification-system -am -DskipTests
```

### 7.6 SecurityControllerAdvice (security)

**Edit file**: `security/src/main/java/com/akademiaplus/config/SecurityControllerAdvice.java`

**Before**: 1 InvalidLogin handler returning 400
**After**: 1 InvalidLogin handler returning **401**

Changes:
1. Add `extends BaseControllerAdvice`
2. Replace constructor body with `super(messageService);`
3. **KEEP** the `InvalidLoginException` handler but update:
   - Set `error.setCode("INVALID_CREDENTIALS")`
   - Change `HttpStatus.BAD_REQUEST` → `HttpStatus.UNAUTHORIZED`
   - Use `messageService()` accessor from base

```bash
mvn clean compile -pl security -am -DskipTests
```

**Verify Phase 7 (full project)**:
```bash
mvn clean compile -DskipTests
```

---

## Phase 8: Migrate Exception Throw Sites

**Goal**: Replace all `throw new XxxNotFoundException(...)` and `throw new XxxDeletionNotAllowedException(...)` in service/use case classes with the generic exception equivalents.

**CRITICAL**: Do NOT touch any code in test files during this phase. Test updates happen in Phase 9.

### Strategy

Search for every throw site using these grep commands:

```bash
# Find all NotFound throws
grep -rn "throw new.*NotFoundException" --include="*.java" \
    user-management/src/main billing/src/main course-management/src/main \
    pos-system/src/main notification-system/src/main tenant-management/src/main

# Find all DeletionNotAllowed throws
grep -rn "throw new.*DeletionNotAllowedException" --include="*.java" \
    user-management/src/main billing/src/main
```

### Replacement Pattern

For EVERY throw site found:

**Before** (per-entity NotFound):
```java
throw new EmployeeNotFoundException(String.valueOf(employeeId));
```

**After** (generic):
```java
throw new EntityNotFoundException(EntityType.EMPLOYEE, String.valueOf(employeeId));
```

**Before** (per-entity DeletionNotAllowed — DB constraint):
```java
throw new EmployeeDeletionNotAllowedException(ex);
```

**After** (generic — constraint variant):
```java
throw new EntityDeletionNotAllowedException(EntityType.EMPLOYEE, String.valueOf(employeeId), ex);
```

**Before** (DataIntegrity email/phone in creation use cases):
```java
} catch (DataIntegrityViolationException ex) {
    String errorMessage = ex.getCause().getCause().getMessage();
    if (errorMessage != null && errorMessage.contains("email")) {
        // ...
    }
}
```

**After** (classify at throw site, not in advice):
```java
} catch (DataIntegrityViolationException ex) {
    String rootMsg = extractRootCauseMessage(ex);
    if (rootMsg != null && rootMsg.contains("email")) {
        throw new DuplicateEntityException(EntityType.EMPLOYEE, "email", ex);
    } else if (rootMsg != null && rootMsg.contains("phone")) {
        throw new DuplicateEntityException(EntityType.EMPLOYEE, "phoneNumber", ex);
    }
    throw ex; // Unknown constraint — let BaseControllerAdvice DataIntegrity handler catch it
}
```

Where `extractRootCauseMessage` is a null-safe helper:
```java
private String extractRootCauseMessage(DataIntegrityViolationException ex) {
    Throwable cause = ex.getCause();
    if (cause != null && cause.getCause() != null) {
        return cause.getCause().getMessage();
    }
    return cause != null ? cause.getMessage() : null;
}
```

NOTE: If `DataIntegrityViolationException` parsing currently lives ONLY in the `PeopleControllerAdvice` and NOT in individual use cases, move it to the relevant creation use cases that handle email/phone uniqueness (Employee, Collaborator, AdultStudent, Tutor). Read each creation use case to determine which ones catch `DataIntegrityViolationException` vs which rely on the advice handler. For use cases that currently let the exception bubble up to the advice, add the catch block in the use case's `create()` method.

### Required imports at each throw site

```java
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
// and/or:
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
```

### Module-by-Module Migration Order

Process in this order. After EACH module, compile:

**8.1 user-management** — most throw sites (Employee, Collaborator, AdultStudent, Tutor, MinorStudent)
```bash
grep -rn "throw new.*NotFoundException\|throw new.*DeletionNotAllowed" \
    --include="*.java" user-management/src/main/
# Migrate each file found
mvn clean compile -pl user-management -am -DskipTests
```

**8.2 billing** — (Membership, MembershipAdultStudent, MembershipTutor, PaymentAdultStudent, PaymentTutor, Compensation)
```bash
grep -rn "throw new.*NotFoundException" --include="*.java" billing/src/main/
mvn clean compile -pl billing -am -DskipTests
```

**8.3 course-management** — (Course, CourseEvent, Schedule)
```bash
grep -rn "throw new.*NotFoundException" --include="*.java" course-management/src/main/
mvn clean compile -pl course-management -am -DskipTests
```

**8.4 pos-system** — (StoreProduct, StoreTransaction)
```bash
grep -rn "throw new.*NotFoundException" --include="*.java" pos-system/src/main/
mvn clean compile -pl pos-system -am -DskipTests
```

**8.5 notification-system** — (Notification)
```bash
grep -rn "throw new.*NotFoundException" --include="*.java" notification-system/src/main/
mvn clean compile -pl notification-system -am -DskipTests
```

**8.6 tenant-management** — (check for any throws)
```bash
grep -rn "throw new.*NotFoundException" --include="*.java" tenant-management/src/main/
mvn clean compile -pl tenant-management -am -DskipTests
```

**Verify Phase 8 (full project)**:
```bash
mvn clean compile -DskipTests
```

---

## Phase 9: Update Existing Tests

**Goal**: Fix all test files that reference old per-entity exception classes to use the new generic ones.

### Strategy

```bash
# Find all test files referencing old exceptions
grep -rn "NotFoundException\|DeletionNotAllowedException" \
    --include="*.java" */src/test/
```

For EVERY test that imports or references a per-entity exception:

**Before**:
```java
import com.akademiaplus.exception.EmployeeNotFoundException;
// ...
when(...).thenThrow(new EmployeeNotFoundException(String.valueOf(TEST_EMPLOYEE_ID)));
```

**After**:
```java
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
// ...
when(...).thenThrow(new EntityNotFoundException(EntityType.EMPLOYEE, String.valueOf(TEST_EMPLOYEE_ID)));
```

**Before**:
```java
assertThatThrownBy(() -> useCase.delete(id))
    .isInstanceOf(EmployeeNotFoundException.class);
```

**After**:
```java
assertThatThrownBy(() -> useCase.delete(id))
    .isInstanceOf(EntityNotFoundException.class);
```

Same pattern for `DeletionNotAllowedException` → `EntityDeletionNotAllowedException`.

Process module by module in the same order as Phase 8. After each module:

```bash
mvn clean test -pl <module> -am
```

**Verify Phase 9 (full test suite)**:
```bash
mvn clean test
```

---

## Phase 10: Deprecate Old Exception Classes

**Goal**: Mark all 16 per-entity exception classes `@Deprecated`. Do NOT delete them yet.

### Files to annotate

Add `@Deprecated(since = "1.1", forRemoval = true)` to the class declaration of each:

**user-management** (`user-management/src/main/java/com/akademiaplus/exception/`):
- `EmployeeNotFoundException.java`
- `CollaboratorNotFoundException.java`
- `AdultStudentNotFoundException.java`
- `TutorNotFoundException.java`
- `MinorStudentNotFoundException.java`
- `EmployeeDeletionNotAllowedException.java`
- `CollaboratorDeletionNotAllowedException.java`
- `AdultStudentDeletionNotAllowedException.java`

**billing** (`billing/src/main/java/com/akademiaplus/exception/`):
- `MembershipNotFoundException.java`
- `MembershipAdultStudentNotFoundException.java`
- `MembershipTutorNotFoundException.java`
- `PaymentAdultStudentNotFoundException.java`
- `PaymentTutorNotFoundException.java`
- `CompensationNotFoundException.java`

**course-management** (`course-management/src/main/java/com/akademiaplus/exception/`):
- `CourseNotFoundException.java`
- `CourseEventNotFoundException.java`
- `ScheduleNotFoundException.java`

**pos-system** (`pos-system/src/main/java/com/akademiaplus/exception/`):
- `StoreProductNotFoundException.java`
- `StoreTransactionNotFoundException.java`

**notification-system** (`notification-system/src/main/java/com/akademiaplus/exception/`):
- `NotificationNotFoundException.java`

Add to each file:
```java
/**
 * @deprecated Use {@link com.akademiaplus.utilities.exceptions.EntityNotFoundException} instead.
 *             Scheduled for removal in version 1.2.
 */
@Deprecated(since = "1.1", forRemoval = true)
public class XxxNotFoundException extends RuntimeException { ... }
```

**Verify Phase 10**:
```bash
mvn clean compile -DskipTests
```

---

## Phase 11: OpenAPI Spec Updates

**Goal**: Add `409 Conflict` response to all delete endpoints across all modules.

### 11.1 Verify commons.yaml has Conflict response

The `Conflict` response reference already exists in:
`user-management/src/main/resources/openapi/commons.yaml` under `components.responses.Conflict`

Verify this. If it does not exist, add it:
```yaml
    Conflict:
      description: Conflict - resource already exists or conflicting state
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
```

### 11.2 Add 409 to delete endpoints

For each OpenAPI YAML file listed below, find the `delete:` operation and add:

```yaml
        '409':
          $ref: './commons.yaml#/components/responses/Conflict'
```

IMPORTANT: Modules other than user-management may reference their OWN commons or a shared error schema. Read each module's OpenAPI structure first to determine the correct `$ref` path. If a module does not have `commons.yaml`, inline the 409 response:

```yaml
        '409':
          description: Conflict - deletion not allowed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

**Files to update**:

**user-management** (commons.yaml reference available):
```
user-management/src/main/resources/openapi/employee-api.yaml          — add 409
user-management/src/main/resources/openapi/collaborator-api.yaml      — add 409
user-management/src/main/resources/openapi/adult-student-api.yaml     — add 409
user-management/src/main/resources/openapi/tutor-api.yaml             — CHANGE existing 400 → 409
user-management/src/main/resources/openapi/minor-student-api.yaml     — add 409
```

**billing**:
```
billing/src/main/resources/openapi/compensation.yaml                  — add 409
billing/src/main/resources/openapi/membership.yaml                    — add 409
billing/src/main/resources/openapi/membership-management.yaml         — add 409 (×2 delete endpoints)
billing/src/main/resources/openapi/payment-management.yaml            — add 409 (×2 delete endpoints)
```

**course-management**:
```
course-management/src/main/resources/openapi/course.yaml              — add 409
course-management/src/main/resources/openapi/schedule.yaml            — add 409
course-management/src/main/resources/openapi/course-event.yaml        — add 409
```

**pos-system**:
```
pos-system/src/main/resources/openapi/store-product.yaml              — add 409
pos-system/src/main/resources/openapi/store-transaction.yaml          — add 409
```

**tenant-management**:
```
tenant-management/src/main/resources/openapi/tenant-management-module.yaml — add 409 (×2 delete endpoints)
```

**notification-system**:
```
notification-system/src/main/resources/openapi/notification.yaml      — add 409
```

IMPORTANT: Before editing each file, `cat` it first to find the exact `delete:` operation and its `responses:` block. Some files may not have a delete endpoint — skip those.

**Special case — tutor-api.yaml**: The Tutor delete endpoint currently has a `'400'` response for "cannot delete tutor with active students". Change this to `'409'`. The description and schema stay the same; only the status code changes.

**Verify Phase 11** (regenerate sources to validate YAML syntax):
```bash
mvn clean generate-sources -DskipTests
```

If any YAML has syntax errors, the openapi-generator will fail. Fix and retry.

---

## Phase 12: Final Verification & Commit

### 12.1 Full Build

```bash
mvn clean install
```

This runs ALL unit tests across ALL modules. Every test must pass.

### 12.2 Verify No Remaining References to Old Exceptions in Non-Test Code

```bash
# Should return ZERO results in src/main (only deprecated class definitions allowed)
grep -rn "throw new.*NotFoundException\b" --include="*.java" */src/main/ | grep -v "@Deprecated"

# Should return ZERO results for old deletion exceptions in src/main (only deprecated class defs)
grep -rn "throw new.*DeletionNotAllowedException\b" --include="*.java" */src/main/ | grep -v "@Deprecated"
```

If any non-deprecated throw sites remain, go back and migrate them.

### 12.3 Verify ErrorResponseDTO.code is Set in ALL Handlers

```bash
# Every @ExceptionHandler in Base + module advices must set error.setCode(...)
grep -A 10 "@ExceptionHandler" utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java | grep "setCode"

grep -A 10 "@ExceptionHandler" course-management/src/main/java/com/akademiaplus/config/CoordinationControllerAdvice.java | grep "setCode"

grep -A 10 "@ExceptionHandler" security/src/main/java/com/akademiaplus/config/SecurityControllerAdvice.java | grep "setCode"
```

All handlers must show a `setCode(...)` call.

### 12.4 Commit

```bash
git add -A
git commit -m "refactor: consolidate exception handling with BaseControllerAdvice

BREAKING CHANGE: HTTP status codes changed for 2 endpoints:
- DeletionNotAllowed: 400 → 409 Conflict (RFC 9110)
- InvalidLogin: 400 → 401 Unauthorized

Infrastructure additions:
- EntityType constants class (20 entity type identifiers)
- EntityNotFoundException replaces 13 per-entity NFE classes
- EntityDeletionNotAllowedException replaces 3 per-entity DNA classes
- DuplicateEntityException replaces DataIntegrity cause parsing
- BaseControllerAdvice with 8 shared handlers (utilities module)
- 4 new generic methods in MessageService
- New message keys: entity.delete.not.allowed.reason,
  entity.duplicate.field, entity.constraint.violation, invalid.tenant

ControllerAdvice consolidation:
- PeopleControllerAdvice: 8 handlers → 0 (all inherited)
- BillingControllerAdvice: 6 → 0
- CoordinationControllerAdvice: 5 → 1 (ScheduleConflict)
- PosControllerAdvice: 2 → 0
- NotificationControllerAdvice: 1 → 0
- SecurityControllerAdvice: 1 → 1 (InvalidLogin, now 401)
- Total: 23 handlers → 9 (-14 removed)

ErrorResponseDTO now populates both 'message' and 'code' fields.
OpenAPI specs updated: 409 Conflict added to all delete endpoints.
16 per-entity exception classes + 20 MessageService methods deprecated."
```

---

## Summary: File Inventory

### New Files Created (Phase 1-6)

| # | File | Module |
|---|------|--------|
| 1 | `utilities/.../utilities/EntityType.java` | utilities |
| 2 | `utilities/.../exceptions/EntityNotFoundException.java` | utilities |
| 3 | `utilities/.../exceptions/EntityDeletionNotAllowedException.java` | utilities |
| 4 | `utilities/.../exceptions/DuplicateEntityException.java` | utilities |
| 5 | `utilities/.../web/BaseControllerAdvice.java` | utilities |
| 6 | `utilities/.../exceptions/EntityNotFoundExceptionTest.java` | utilities (test) |
| 7 | `utilities/.../exceptions/EntityDeletionNotAllowedExceptionTest.java` | utilities (test) |
| 8 | `utilities/.../exceptions/DuplicateEntityExceptionTest.java` | utilities (test) |
| 9 | `utilities/.../web/BaseControllerAdviceTest.java` | utilities (test) |

### Files Modified (Phase 3-11)

| # | File | Change |
|---|------|--------|
| 10 | `utilities_messages_es_MX.properties` | +3 entity name keys |
| 11 | `user_management_messages_es_MX.properties` | +4 message template keys |
| 12 | `MessageService.java` | +6 new methods, +20 @Deprecated |
| 13 | `PeopleControllerAdvice.java` | extends Base, delete 8 handlers |
| 14 | `BillingControllerAdvice.java` | extends Base, delete 6 handlers |
| 15 | `CoordinationControllerAdvice.java` | extends Base, keep 1 handler |
| 16 | `PosControllerAdvice.java` | extends Base, delete 2 handlers |
| 17 | `NotificationControllerAdvice.java` | extends Base, delete 1 handler |
| 18 | `SecurityControllerAdvice.java` | extends Base, keep 1 handler (401) |
| 19-38 | All service/use case files with throw sites | Migrate to generic exceptions |
| 39-58 | Corresponding test files | Update exception references |
| 59-78 | 20 per-entity exception classes | Add @Deprecated annotation |
| 79-97 | ~19 OpenAPI YAML files | Add 409 to delete endpoints |

---

## Critical Reminders

1. **Never delete old exception classes** — only deprecate. Consumers may exist outside this repo.
2. **ErrorResponseDTO.code must always be set** — it was empty before this work. Every handler MUST set it.
3. **DataIntegrity parsing moves to use cases** — the advice handler is now a dumb fallback that returns a generic message. Classification of email/phone violations happens at the throw site.
4. **The `reason` null-check** in `handleDeletionNotAllowed` determines the code field. NULL reason = constraint, non-null = business rule.
5. **commons.yaml is in user-management only** — other modules may need inline error response definitions in their OpenAPI specs.
6. **Test the MethodArgumentNotValidException handler** with mocked FieldError objects. This handler populates the `details[]` array which was previously never used.
7. **`messageService()` accessor** — subclass handlers use `messageService()` (protected method), not direct field access.
8. **BaseControllerAdvice is NOT @ControllerAdvice** — it's abstract. Only concrete subclasses carry the annotation.
