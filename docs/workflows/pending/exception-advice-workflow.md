# Exception Handling Consolidation Workflow — AkademiaPlus

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `docs/design/exception-advice-strategy.md` before starting.
**Specification**: All decisions, HTTP mappings, message keys, and code structures are defined in `docs/design/exception-advice-strategy.md`. Do NOT deviate.

---

## Execution Phases

```
Phase 1: Foundation — EntityType constants + new message keys
Phase 2: Generic exceptions — 3 new classes in utilities
Phase 3: MessageService refactoring — generic methods + deprecation
Phase 4: BaseControllerAdvice — abstract base in utilities
Phase 5: Module advice migration — 6 modules extend base
Phase 6: UseCase migration — swap per-entity exceptions for generics
Phase 7: OpenAPI updates — add 409 to all delete endpoints
Phase 8: Cleanup — remove deprecated classes and methods
```

Each phase has a verification gate. Do NOT proceed to the next phase until the current one compiles and all tests pass.

---

## Phase 1: EntityType Constants + Message Keys

### Step 1.1: Create EntityType constants class

**File**: `utilities/src/main/java/com/akademiaplus/utilities/EntityType.java`

```java
package com.akademiaplus.utilities;

/**
 * Canonical entity type identifiers used for exception messages.
 * <p>
 * Values correspond to message property keys in
 * {@code utilities_messages_es_MX.properties} that resolve
 * to localized display names (e.g., "entity.employee" → "Empleado").
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

### Step 1.2: Add new message keys to utilities_messages_es_MX.properties

Locate the file:
```bash
find . -name "utilities_messages_es_MX.properties" -type f
```

Append these keys (do NOT modify existing keys):
```properties
# Entity display names (new additions — verify existing ones are present)
entity.tenant=Organizacion
entity.tenant.subscription=Suscripcion de Organizacion
entity.tenant.billing.cycle=Ciclo de Facturacion
```

### Step 1.3: Add new message keys to user_management_messages_es_MX.properties

Locate the file:
```bash
find . -name "user_management_messages_es_MX.properties" -type f
```

Append these keys (do NOT modify existing keys):
```properties
# New: deletion with business reason
entity.delete.not.allowed.reason=Eliminacion de {0} con ID: {1} no es posible: {2}

# New: duplicate field violation
entity.duplicate.field=Error: el campo {1} del {0} ya esta registrado

# New: generic unclassified constraint
entity.constraint.violation=Operacion no permitida: conflicto de integridad de datos

# New: invalid tenant context
invalid.tenant=Contexto de organizacion es requerido
```

### Step 1.4: Verify

```bash
mvn clean compile -pl utilities -am -DskipTests
```

Confirm `EntityType.java` compiles. Confirm both `.properties` files are well-formed (no trailing whitespace breaking keys, no duplicate keys).

---

## Phase 2: Generic Exception Classes

Create 3 new exception classes in the `utilities` module. All extend `RuntimeException`.

### Step 2.1: Create EntityNotFoundException

**File**: `utilities/src/main/java/com/akademiaplus/utilities/exceptions/EntityNotFoundException.java`

```java
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when a tenant-scoped entity cannot be found by its composite key.
 * <p>
 * The {@code entityType} field holds a message property key
 * (e.g., {@link com.akademiaplus.utilities.EntityType#EMPLOYEE})
 * that resolves to a localized display name via {@code MessageService}.
 * <p>
 * Handled by {@code BaseControllerAdvice} → HTTP 404 Not Found.
 */
public class EntityNotFoundException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    /**
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId   the entity ID that was not found (as String for display)
     */
    public EntityNotFoundException(String entityType, String entityId) {
        super(entityType + " with ID " + entityId + " not found");
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
}
```

### Step 2.2: Create EntityDeletionNotAllowedException

**File**: `utilities/src/main/java/com/akademiaplus/utilities/exceptions/EntityDeletionNotAllowedException.java`

```java
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when an entity cannot be deleted due to either a database
 * constraint violation or a business rule.
 * <p>
 * Two construction paths:
 * <ul>
 *   <li>DB constraint: {@code new EntityDeletionNotAllowedException(type, id, cause)}
 *       — {@code reason} will be {@code null}</li>
 *   <li>Business rule: {@code new EntityDeletionNotAllowedException(type, id, reason)}
 *       — {@code reason} describes why deletion is blocked</li>
 * </ul>
 * Handled by {@code BaseControllerAdvice} → HTTP 409 Conflict.
 */
public class EntityDeletionNotAllowedException extends RuntimeException {

    private final String entityType;
    private final String entityId;
    private final String reason;

    /**
     * DB constraint violation — the cause is the original
     * {@link org.springframework.dao.DataIntegrityViolationException}.
     *
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId   the entity ID whose deletion was attempted
     * @param cause      the database exception
     */
    public EntityDeletionNotAllowedException(String entityType, String entityId, Throwable cause) {
        super("Deletion of " + entityType + " with ID " + entityId + " not allowed", cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.reason = null;
    }

    /**
     * Business rule violation — a human-readable reason explains why.
     *
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId   the entity ID whose deletion was attempted
     * @param reason     business rule description (e.g., "Tutor tiene 3 alumno(s) menor(es) activo(s)")
     */
    public EntityDeletionNotAllowedException(String entityType, String entityId, String reason) {
        super("Deletion of " + entityType + " with ID " + entityId + " not allowed: " + reason);
        this.entityType = entityType;
        this.entityId = entityId;
        this.reason = reason;
    }

    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getReason() { return reason; }
}
```

### Step 2.3: Create DuplicateEntityException

**File**: `utilities/src/main/java/com/akademiaplus/utilities/exceptions/DuplicateEntityException.java`

```java
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when a create or update operation violates a unique constraint
 * on a known field (email, phone number, etc.).
 * <p>
 * This replaces the fragile pattern of parsing
 * {@code DataIntegrityViolationException.getCause().getCause().getMessage()}
 * to extract the duplicate field name.
 * <p>
 * Handled by {@code BaseControllerAdvice} → HTTP 409 Conflict.
 */
public class DuplicateEntityException extends RuntimeException {

    private final String entityType;
    private final String field;

    /**
     * @param entityType message property key from {@link com.akademiaplus.utilities.EntityType}
     * @param field      the field that has a duplicate value (e.g., "email", "phoneNumber")
     * @param cause      the original {@link org.springframework.dao.DataIntegrityViolationException}
     */
    public DuplicateEntityException(String entityType, String field, Throwable cause) {
        super("Duplicate " + field + " for " + entityType, cause);
        this.entityType = entityType;
        this.field = field;
    }

    public String getEntityType() { return entityType; }
    public String getField() { return field; }
}
```

### Step 2.4: Verify

```bash
mvn clean compile -pl utilities -am -DskipTests
```

All 3 new exception classes must compile. Verify they are in the correct package:
```bash
find utilities/src/main/java -name "Entity*Exception.java" -o -name "DuplicateEntityException.java"
```

Expected output:
```
utilities/src/main/java/.../exceptions/EntityNotFoundException.java
utilities/src/main/java/.../exceptions/EntityDeletionNotAllowedException.java
utilities/src/main/java/.../exceptions/DuplicateEntityException.java
```

### Step 2.5: Unit tests for new exceptions

**File**: `utilities/src/test/java/com/akademiaplus/utilities/exceptions/EntityNotFoundExceptionTest.java`

Test coverage required:
- Constructor stores `entityType` and `entityId` correctly
- `getMessage()` contains both values
- Getters return the exact values passed to constructor

**File**: `utilities/src/test/java/com/akademiaplus/utilities/exceptions/EntityDeletionNotAllowedExceptionTest.java`

Test coverage required:
- DB constraint constructor: `reason` is `null`, `cause` is stored
- Business rule constructor: `reason` is stored, `cause` is `null`
- Both constructors store `entityType` and `entityId` correctly
- `getMessage()` contains entity info for both variants

**File**: `utilities/src/test/java/com/akademiaplus/utilities/exceptions/DuplicateEntityExceptionTest.java`

Test coverage required:
- Constructor stores `entityType`, `field`, and `cause`
- `getMessage()` contains both entity type and field name
- Getters return exact values

Follow all testing conventions from `AI-CODE-REF.md`:
- Given-When-Then comments
- `shouldDoX_whenGivenY()` naming
- `@Nested` + `@DisplayName`
- Zero `any()` matchers
- `public static final` constants for test values
- AssertJ assertions

### Step 2.6: Run tests

```bash
mvn test -pl utilities -am
```

### Step 2.7: Commit

```bash
git add -A
git commit -m "feat(utilities): add generic exception classes and EntityType constants

Add EntityNotFoundException, EntityDeletionNotAllowedException, and
DuplicateEntityException to replace 16 per-entity exception classes.

Add EntityType constants class with message property keys for all 19
entity types. Add new message keys for deletion-with-reason, duplicate
field, constraint violation, and invalid tenant messages.

Includes unit tests for all 3 exception classes."
```

---

## Phase 3: MessageService Refactoring

### Step 3.1: Locate MessageService

```bash
find . -name "MessageService.java" -path "*/main/*"
```

Read the file. Identify:
1. All `getXxxNotFound()` methods (expect ~17)
2. All `getXxxDeleteNotAllowed()` methods (expect ~3)
3. The existing `messageSource` field and `locale` field
4. Existing message key constants

### Step 3.2: Add message key constants

Add these constants to `MessageService` (or to a companion constants class if one exists):

```java
// Generic message keys — used by BaseControllerAdvice
public static final String ENTITY_NOT_FOUND = "entity.not.found";
public static final String ENTITY_DELETE_NOT_ALLOWED = "entity.delete.not.allowed";
public static final String ENTITY_DELETE_NOT_ALLOWED_REASON = "entity.delete.not.allowed.reason";
public static final String ENTITY_DUPLICATE_FIELD = "entity.duplicate.field";
public static final String ENTITY_CONSTRAINT_VIOLATION = "entity.constraint.violation";
public static final String INVALID_TENANT = "invalid.tenant";
```

### Step 3.3: Add generic methods

Add these methods to `MessageService`:

```java
/**
 * Resolves a localized "entity not found" message.
 * <p>
 * First resolves the entity type key (e.g., "entity.employee" → "Empleado"),
 * then formats into the "entity.not.found" template.
 *
 * @param entityTypeKey message key from {@link EntityType}
 * @param entityId      the ID that was not found
 * @return formatted message, e.g., "Empleado con ID: 42 no existe!"
 */
public String getEntityNotFound(String entityTypeKey, String entityId) {
    String entityName = messageSource.getMessage(entityTypeKey, null, locale);
    return messageSource.getMessage(ENTITY_NOT_FOUND,
            new Object[]{entityName, entityId}, locale);
}

/**
 * Resolves a localized "deletion not allowed" message for DB constraint violations.
 *
 * @param entityTypeKey message key from {@link EntityType}
 * @return formatted message, e.g., "Eliminacion de Empleado no esta permitida!..."
 */
public String getEntityDeleteNotAllowed(String entityTypeKey) {
    String entityName = messageSource.getMessage(entityTypeKey, null, locale);
    return messageSource.getMessage(ENTITY_DELETE_NOT_ALLOWED,
            new Object[]{entityName}, locale);
}

/**
 * Resolves a localized "deletion not allowed" message for business rule violations.
 *
 * @param entityTypeKey message key from {@link EntityType}
 * @param entityId      the entity ID whose deletion was attempted
 * @param reason        human-readable business rule description
 * @return formatted message, e.g., "Eliminacion de Tutor con ID: 7 no es posible: ..."
 */
public String getEntityDeleteNotAllowedWithReason(
        String entityTypeKey, String entityId, String reason) {
    String entityName = messageSource.getMessage(entityTypeKey, null, locale);
    return messageSource.getMessage(ENTITY_DELETE_NOT_ALLOWED_REASON,
            new Object[]{entityName, entityId, reason}, locale);
}

/**
 * Resolves a localized "duplicate field" message.
 *
 * @param entityTypeKey message key from {@link EntityType}
 * @param field         the field name that has a duplicate value
 * @return formatted message, e.g., "Error: el campo email del Empleado ya esta registrado"
 */
public String getEntityDuplicateField(String entityTypeKey, String field) {
    String entityName = messageSource.getMessage(entityTypeKey, null, locale);
    return messageSource.getMessage(ENTITY_DUPLICATE_FIELD,
            new Object[]{entityName, field}, locale);
}

/**
 * Resolves the generic constraint violation message.
 *
 * @return "Operacion no permitida: conflicto de integridad de datos"
 */
public String getConstraintViolation() {
    return messageSource.getMessage(ENTITY_CONSTRAINT_VIOLATION, null, locale);
}

/**
 * Resolves the invalid tenant context message.
 *
 * @return "Contexto de organizacion es requerido"
 */
public String getInvalidTenant() {
    return messageSource.getMessage(INVALID_TENANT, null, locale);
}
```

### Step 3.4: Deprecate per-entity methods

Mark ALL existing per-entity methods with `@Deprecated`:

```java
@Deprecated(since = "1.1", forRemoval = true)
public String getEmployeeNotFound(Long employeeId) {
    // existing implementation unchanged
}
```

Apply `@Deprecated(since = "1.1", forRemoval = true)` to every `getXxxNotFound()` and `getXxxDeleteNotAllowed()` method. Do NOT change their implementations — existing code still calls them.

### Step 3.5: Unit tests for new methods

Locate existing `MessageServiceTest.java`:
```bash
find . -name "MessageServiceTest.java" -path "*/test/*"
```

Add a new `@Nested` class for the generic methods:

```java
@Nested
@DisplayName("Generic Entity Messages")
class GenericEntityMessages {

    @Test
    @DisplayName("Should format entity not found message with resolved entity name")
    void shouldFormatEntityNotFound_whenGivenEntityTypeAndId() {
        // Given
        when(messageSource.getMessage("entity.employee", null, LOCALE))
                .thenReturn("Empleado");
        when(messageSource.getMessage(MessageService.ENTITY_NOT_FOUND,
                new Object[]{"Empleado", "42"}, LOCALE))
                .thenReturn("Empleado con ID: 42 no existe!");

        // When
        String result = messageService.getEntityNotFound(
                EntityType.EMPLOYEE, "42");

        // Then
        assertThat(result).isEqualTo("Empleado con ID: 42 no existe!");
    }

    @Test
    @DisplayName("Should format entity delete not allowed message for DB constraint")
    void shouldFormatDeleteNotAllowed_whenGivenEntityType() {
        // Given
        when(messageSource.getMessage("entity.employee", null, LOCALE))
                .thenReturn("Empleado");
        when(messageSource.getMessage(MessageService.ENTITY_DELETE_NOT_ALLOWED,
                new Object[]{"Empleado"}, LOCALE))
                .thenReturn("Eliminacion de Empleado no esta permitida!...");

        // When
        String result = messageService.getEntityDeleteNotAllowed(EntityType.EMPLOYEE);

        // Then
        assertThat(result).isEqualTo("Eliminacion de Empleado no esta permitida!...");
    }

    @Test
    @DisplayName("Should format entity delete not allowed message with business reason")
    void shouldFormatDeleteNotAllowedWithReason_whenGivenReasonString() {
        // Given
        String reason = "Tutor tiene 3 alumno(s) menor(es) activo(s)";
        when(messageSource.getMessage("entity.tutor", null, LOCALE))
                .thenReturn("Tutor");
        when(messageSource.getMessage(MessageService.ENTITY_DELETE_NOT_ALLOWED_REASON,
                new Object[]{"Tutor", "7", reason}, LOCALE))
                .thenReturn("Eliminacion de Tutor con ID: 7 no es posible: " + reason);

        // When
        String result = messageService.getEntityDeleteNotAllowedWithReason(
                EntityType.TUTOR, "7", reason);

        // Then
        assertThat(result).startsWith("Eliminacion de Tutor con ID: 7 no es posible:");
    }

    @Test
    @DisplayName("Should format duplicate entity field message")
    void shouldFormatDuplicateField_whenGivenEntityTypeAndField() {
        // Given
        when(messageSource.getMessage("entity.employee", null, LOCALE))
                .thenReturn("Empleado");
        when(messageSource.getMessage(MessageService.ENTITY_DUPLICATE_FIELD,
                new Object[]{"Empleado", "email"}, LOCALE))
                .thenReturn("Error: el campo email del Empleado ya esta registrado");

        // When
        String result = messageService.getEntityDuplicateField(
                EntityType.EMPLOYEE, "email");

        // Then
        assertThat(result).isEqualTo(
                "Error: el campo email del Empleado ya esta registrado");
    }

    @Test
    @DisplayName("Should return constraint violation message")
    void shouldReturnConstraintViolation_whenCalled() {
        // Given
        when(messageSource.getMessage(MessageService.ENTITY_CONSTRAINT_VIOLATION, null, LOCALE))
                .thenReturn("Operacion no permitida: conflicto de integridad de datos");

        // When
        String result = messageService.getConstraintViolation();

        // Then
        assertThat(result).isEqualTo(
                "Operacion no permitida: conflicto de integridad de datos");
    }

    @Test
    @DisplayName("Should return invalid tenant message")
    void shouldReturnInvalidTenantMessage_whenCalled() {
        // Given
        when(messageSource.getMessage(MessageService.INVALID_TENANT, null, LOCALE))
                .thenReturn("Contexto de organizacion es requerido");

        // When
        String result = messageService.getInvalidTenant();

        // Then
        assertThat(result).isEqualTo("Contexto de organizacion es requerido");
    }
}
```

### Step 3.6: Run tests

```bash
mvn test -pl utilities -am
```

### Step 3.7: Commit

```bash
git add -A
git commit -m "refactor(utilities): add generic MessageService methods, deprecate per-entity

Add getEntityNotFound(), getEntityDeleteNotAllowed(),
getEntityDeleteNotAllowedWithReason(), getEntityDuplicateField(),
getConstraintViolation(), and getInvalidTenant() to MessageService.

Deprecate 17 per-entity getXxxNotFound() and 3 getXxxDeleteNotAllowed()
methods for removal in next release."
```

---

## Phase 4: BaseControllerAdvice

### Step 4.1: Identify existing ErrorResponseDTO structure

```bash
find . -name "ErrorResponseDTO.java" -path "*/generated-sources/*" | head -3
cat $(find . -name "ErrorResponseDTO.java" -path "*/generated-sources/*" | head -1)
```

Identify the exact setter names: `setMessage()`, `setCode()`, `setDetails()`, etc. The generated DTO field names dictate what we call. Do NOT guess — read the generated class.

Also check if `ErrorDetailDTO` exists (for validation error details array):
```bash
find . -name "ErrorDetailDTO.java" -path "*/generated-sources/*"
```

If `ErrorDetailDTO` does not exist, you must add it to the OpenAPI `commons.yaml` before proceeding:

```yaml
# In commons.yaml components.schemas:
ErrorDetail:
  type: object
  properties:
    field:
      type: string
    message:
      type: string
```

And reference it from `ErrorResponse`:
```yaml
ErrorResponse:
  type: object
  properties:
    # ...existing fields...
    details:
      type: array
      items:
        $ref: '#/components/schemas/ErrorDetail'
```

Then regenerate: `mvn clean generate-sources -DskipTests`

### Step 4.2: Create BaseControllerAdvice

**File**: `utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java`

Read the full implementation from `docs/design/exception-advice-strategy.md`, Section 6.1. Copy the complete class verbatim. Adapt setter names to match the actual generated `ErrorResponseDTO` (from Step 4.1).

**Critical implementation notes:**
- Do NOT annotate the class with `@ControllerAdvice` — it is abstract. Subclasses carry the annotation.
- The `messageService()` accessor is `protected` — subclasses use it for domain-specific handlers.
- The `@ExceptionHandler({EncryptionFailureException.class, DecryptionFailureException.class})` handler uses a multi-exception annotation — both crypto exceptions share one handler.
- The `Exception.class` fallback handler MUST log at ERROR level. Use `private static final Logger LOG = LoggerFactory.getLogger(BaseControllerAdvice.class);` and `LOG.error("Unhandled exception", ex);` before building the response.
- The DataIntegrityViolation handler does NOT attempt to parse the cause chain (solving P5).

### Step 4.3: Verify compilation

```bash
mvn clean compile -pl utilities -am -DskipTests
```

Resolve any import issues. The `ErrorResponseDTO` import will come from a generated-sources package — verify the exact package path from Step 4.1.

### Step 4.4: Unit test for BaseControllerAdvice

**File**: `utilities/src/test/java/com/akademiaplus/utilities/web/BaseControllerAdviceTest.java`

Since `BaseControllerAdvice` is abstract, create a concrete test subclass:

```java
@DisplayName("BaseControllerAdvice")
@ExtendWith(MockitoExtension.class)
class BaseControllerAdviceTest {

    // Concrete test subclass — no additional handlers
    private static class TestControllerAdvice extends BaseControllerAdvice {
        TestControllerAdvice(MessageService messageService) {
            super(messageService);
        }
    }

    @Mock private MessageService messageService;
    private TestControllerAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new TestControllerAdvice(messageService);
    }
```

**Required test coverage (one `@Nested` class per handler):**

```
@Nested EntityNotFoundHandling
  shouldReturn404_whenEntityNotFoundThrown()
  shouldSetCodeToEntityNotFound_whenHandled()
  shouldDelegateMessageToMessageService_whenHandled()

@Nested DeletionNotAllowedHandling
  shouldReturn409WithConstraintCode_whenCauseIsPresent()
  shouldReturn409WithBusinessRuleCode_whenReasonIsPresent()
  shouldDelegateConstraintMessage_whenCauseVariant()
  shouldDelegateReasonMessage_whenReasonVariant()

@Nested DuplicateEntityHandling
  shouldReturn409_whenDuplicateEntityThrown()
  shouldSetCodeToDuplicateEntity_whenHandled()

@Nested DataIntegrityViolationHandling
  shouldReturn409_whenDataIntegrityViolationThrown()
  shouldNotThrowNPE_whenCauseChainIsNull()

@Nested ValidationHandling
  shouldReturn400_whenValidationFails()
  shouldPopulateDetailsArray_whenFieldErrorsExist()

@Nested InvalidTenantHandling
  shouldReturn400_whenInvalidTenantThrown()

@Nested CryptoFailureHandling
  shouldReturn500_whenEncryptionFailureThrown()
  shouldReturn500_whenDecryptionFailureThrown()

@Nested FallbackHandling
  shouldReturn500_whenUnexpectedExceptionThrown()
```

**Testing notes:**
- Mock `MessageService` to return predictable strings
- Use `EntityType.EMPLOYEE` for test entity type constants
- For `MethodArgumentNotValidException`: construct using `mock(BindingResult.class)` with stubbed `getFieldErrors()`
- For `DataIntegrityViolationException`: construct with `new DataIntegrityViolationException("msg")` — no cause chain
- Verify both `message` and `code` fields on every response DTO
- Verify HTTP status code via `ResponseEntity.getStatusCode()`

### Step 4.5: Run tests

```bash
mvn test -pl utilities -am
```

### Step 4.6: Commit

```bash
git add -A
git commit -m "feat(utilities): add BaseControllerAdvice with shared exception handlers

Implement abstract BaseControllerAdvice with 7 handlers:
- EntityNotFoundException → 404 ENTITY_NOT_FOUND
- EntityDeletionNotAllowedException → 409 DELETION_CONSTRAINT_VIOLATION
  or DELETION_BUSINESS_RULE
- DuplicateEntityException → 409 DUPLICATE_ENTITY
- DataIntegrityViolationException → 409 DATA_INTEGRITY_VIOLATION
- MethodArgumentNotValidException → 400 VALIDATION_ERROR with details
- InvalidTenantException → 400 INVALID_TENANT
- EncryptionFailure/DecryptionFailure → 500 INTERNAL_ERROR
- Exception fallback → 500 INTERNAL_ERROR

Populates ErrorResponseDTO.code field for machine-readable errors."
```

---

## Phase 5: Module Advice Migration

Migrate all 6 module-specific `ControllerAdvice` classes to extend `BaseControllerAdvice`. Process ONE module at a time. After each module: compile, test, verify.

### Migration template

For each module advice class:

1. **Read** the current advice class to catalog all handlers
2. **Change** the class to `extends BaseControllerAdvice`
3. **Add** constructor that calls `super(messageService)`
4. **Remove** all handlers that are now inherited from `BaseControllerAdvice`:
   - Any `EntityNotFoundException` handler (now uses generic)
   - Wait — the current code catches per-entity exceptions like `EmployeeNotFoundException`, NOT the new `EntityNotFoundException`. **The per-entity exceptions are still in use by the use cases.** You cannot remove these handlers until Phase 6 migrates the use cases.

**IMPORTANT ORDERING CONSTRAINT**: Phases 5 and 6 must be interleaved PER MODULE. You cannot strip handlers globally before migrating use cases. The correct sequence per module is:

```
For each module:
  5.A  Extend BaseControllerAdvice (add inheritance, keep old handlers)
  6.A  Migrate all use cases in that module to throw generic exceptions
  5.B  Remove old per-entity handlers (now unreachable)
  Compile + test
  Commit
```

### Step 5.1: user-management (PeopleControllerAdvice)

#### 5.1.A: Extend BaseControllerAdvice

Read current file:
```bash
find . -name "PeopleControllerAdvice.java" -path "*/main/*"
cat <path>
```

Modify:
1. Add `extends BaseControllerAdvice`
2. Change constructor to call `super(messageService)`
3. **Keep all existing handlers** — they still handle old exception types

```java
@ControllerAdvice(basePackageClasses = {EmployeeController.class, /* ... */})
public class PeopleControllerAdvice extends BaseControllerAdvice {

    public PeopleControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    // KEEP all existing handlers for now — Phase 6 will migrate use cases
    // After Phase 6 migration, these handlers become dead code and get removed
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEmployeeNotFound(/*...*/) { /* existing */ }
    // ... etc
}
```

Compile: `mvn clean compile -pl user-management -am -DskipTests`

#### 5.1.B: Migrate use cases (Phase 6 work for this module)

For EACH use case in user-management that throws a per-entity exception, replace:

**Find all throw sites:**
```bash
grep -rn "throw new EmployeeNotFoundException\|throw new CollaboratorNotFoundException\|throw new AdultStudentNotFoundException\|throw new TutorNotFoundException\|throw new MinorStudentNotFoundException" user-management/src/main/java/
```

**Replace each throw site:**

Before:
```java
import com.akademiaplus.usermanagement.exception.EmployeeNotFoundException;
// ...
throw new EmployeeNotFoundException(
    messageService.getEmployeeNotFound(employeeId));
```

After:
```java
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
// ...
throw new EntityNotFoundException(EntityType.EMPLOYEE, String.valueOf(employeeId));
```

**Similarly for DeletionNotAllowed:**

Before:
```java
throw new EmployeeDeletionNotAllowedException(ex);
```

After:
```java
throw new EntityDeletionNotAllowedException(
    EntityType.EMPLOYEE, String.valueOf(employeeId), ex);
```

**Repeat for every throw site** in user-management. Use `grep -rn` to ensure zero remaining references to old exception classes in `src/main/java/`.

#### 5.1.C: Update unit tests

For EACH migrated use case test:
1. Change the expected exception type from `EmployeeNotFoundException.class` to `EntityNotFoundException.class`
2. Update assertions on the exception: verify `getEntityType()` and `getEntityId()` instead of `getMessage()`
3. Remove mock stubs for `messageService.getEmployeeNotFound()` — the generic exception does not call MessageService directly

```java
// Before:
assertThatThrownBy(() -> useCase.delete(employeeId))
    .isInstanceOf(EmployeeNotFoundException.class)
    .hasMessage("Empleado con ID: 42 no existe!");

// After:
assertThatThrownBy(() -> useCase.delete(employeeId))
    .isInstanceOf(EntityNotFoundException.class)
    .satisfies(ex -> {
        EntityNotFoundException enfe = (EntityNotFoundException) ex;
        assertThat(enfe.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(EMPLOYEE_ID));
    });
```

#### 5.1.D: Remove old handlers from PeopleControllerAdvice

After ALL use cases in user-management are migrated, verify zero references to old exceptions:
```bash
grep -rn "EmployeeNotFoundException\|CollaboratorNotFoundException\|AdultStudentNotFoundException\|TutorNotFoundException\|MinorStudentNotFoundException\|EmployeeDeletionNotAllowedException\|CollaboratorDeletionNotAllowedException\|AdultStudentDeletionNotAllowedException" user-management/src/main/java/
```

If zero hits (excluding the deprecated exception class files themselves), remove ALL per-entity handlers from `PeopleControllerAdvice`. The class becomes an empty shell:

```java
@ControllerAdvice(basePackageClasses = {EmployeeController.class, CollaboratorController.class,
        AdultStudentController.class, TutorController.class, MinorStudentController.class})
public class PeopleControllerAdvice extends BaseControllerAdvice {
    public PeopleControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
```

#### 5.1.E: Compile + test + commit

```bash
mvn clean compile -pl user-management -am -DskipTests
mvn test -pl user-management -am
git add -A
git commit -m "refactor(user-management): migrate to generic exceptions

Replace EmployeeNFE, CollaboratorNFE, AdultStudentNFE, TutorNFE,
MinorStudentNFE, EmployeeDNA, CollaboratorDNA, AdultStudentDNA with
EntityNotFoundException and EntityDeletionNotAllowedException.

PeopleControllerAdvice now extends BaseControllerAdvice with zero
module-specific handlers. HTTP 409 replaces 400 for deletion errors."
```

---

### Step 5.2: billing (BillingControllerAdvice)

Same sequence:
1. Extend `BaseControllerAdvice`, keep old handlers
2. Find all throw sites:
   ```bash
   grep -rn "throw new.*NotFoundException\|throw new.*DeletionNotAllowedException" billing/src/main/java/
   ```
3. Replace with `EntityNotFoundException(EntityType.XXX, id)`
4. Update unit tests
5. Verify zero old exception references in `src/main/java/`
6. Remove old handlers from `BillingControllerAdvice`
7. Compile + test + commit

```bash
git commit -m "refactor(billing): migrate to generic exceptions

Replace MembershipNFE, MembershipAdultStudentNFE, MembershipTutorNFE,
PaymentAdultStudentNFE, PaymentTutorNFE, CompensationNFE with
EntityNotFoundException.

BillingControllerAdvice extends BaseControllerAdvice with zero
module-specific handlers."
```

### Step 5.3: course-management (CoordinationControllerAdvice)

Same sequence, BUT this module keeps one domain-specific handler:

```java
@ControllerAdvice(basePackageClasses = {CourseController.class, ScheduleController.class,
        CourseEventController.class})
public class CoordinationControllerAdvice extends BaseControllerAdvice {

    public CoordinationControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    @ExceptionHandler(ScheduleNotAvailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleScheduleConflict(
            ScheduleNotAvailableException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService().getScheduleNotAvailable(ex.getMessage()));
        error.setCode("SCHEDULE_CONFLICT");
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
}
```

Find throw sites:
```bash
grep -rn "throw new.*NotFoundException" course-management/src/main/java/
```

Replace, update tests, remove old handlers, compile, test, commit:
```bash
git commit -m "refactor(course-management): migrate to generic exceptions

Replace CourseNFE, CourseEventNFE, ScheduleNFE with
EntityNotFoundException.

CoordinationControllerAdvice extends BaseControllerAdvice, retains
ScheduleNotAvailableException handler (domain-specific)."
```

### Step 5.4: pos-system (PosControllerAdvice)

```bash
grep -rn "throw new.*NotFoundException" pos-system/src/main/java/
```

Replace, update tests, strip handlers. Result: empty shell.

```bash
git commit -m "refactor(pos-system): migrate to generic exceptions

Replace StoreProductNFE, StoreTransactionNFE with EntityNotFoundException.
PosControllerAdvice extends BaseControllerAdvice with zero handlers."
```

### Step 5.5: notification-system (NotificationControllerAdvice)

```bash
grep -rn "throw new.*NotFoundException" notification-system/src/main/java/
```

Replace, update tests, strip handlers. Result: empty shell.

```bash
git commit -m "refactor(notification-system): migrate to generic exceptions

Replace NotificationNFE with EntityNotFoundException.
NotificationControllerAdvice extends BaseControllerAdvice with zero handlers."
```

### Step 5.6: security (SecurityControllerAdvice)

This module keeps its `InvalidLoginException` handler BUT fixes the HTTP status:

```java
@ControllerAdvice(basePackageClasses = {InternalAuthController.class})
public class SecurityControllerAdvice extends BaseControllerAdvice {

    public SecurityControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidLogin(
            InvalidLoginException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService().getInvalidLogin());
        error.setCode("INVALID_CREDENTIALS");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED); // WAS HttpStatus.BAD_REQUEST
    }
}
```

**Key change**: HTTP 400 → 401 for `InvalidLoginException`.

Update tests to assert `HttpStatus.UNAUTHORIZED` instead of `BAD_REQUEST`.

```bash
mvn test -pl security -am
git add -A
git commit -m "refactor(security): extend BaseControllerAdvice, fix InvalidLogin to 401

SecurityControllerAdvice extends BaseControllerAdvice, retains
InvalidLoginException handler. Fix HTTP status from 400 → 401
per RFC 9110 (authentication failure, not malformed request)."
```

### Step 5.7: Full project verification

```bash
mvn clean install
```

All modules must compile and all tests must pass. If there are cross-module test failures, investigate: a module-specific test may be asserting the OLD HTTP status code (400) for deletion errors that now return 409.

---

## Phase 6: OpenAPI Specification Updates

### Step 6.1: Verify Conflict response exists in commons.yaml

```bash
find . -name "commons.yaml" -path "*/openapi/*"
```

Read and confirm a `Conflict` response reference exists. If not, add:

```yaml
# In components.responses:
Conflict:
  description: Request conflicts with current resource state
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/ErrorResponse'
```

### Step 6.2: Add 409 to all delete endpoints

For EACH file listed below, find the `delete` operation and add the `'409'` response.

**Files to update (process sequentially, one at a time):**

```bash
# user-management (5 files)
find user-management/src/main/resources/openapi -name "*.yaml" | sort

# billing (4 files, some have 2 delete endpoints)
find billing/src/main/resources/openapi -name "*.yaml" | sort

# course-management (3 files)
find course-management/src/main/resources/openapi -name "*.yaml" | sort

# pos-system (2 files)
find pos-system/src/main/resources/openapi -name "*.yaml" | sort

# notification-system (1 file)
find notification-system/src/main/resources/openapi -name "*.yaml" | sort

# tenant-management (1 file, 2 delete endpoints)
find tenant-management/src/main/resources/openapi -name "*.yaml" | sort
```

**For each delete endpoint, add:**

```yaml
        '409':
          $ref: './commons.yaml#/components/responses/Conflict'
```

**Special case — tutor-api.yaml:** Change existing `'400'` response to `'409'`:

Before:
```yaml
        '400':
          description: Cannot delete tutor with active students
```

After:
```yaml
        '409':
          description: Cannot delete tutor with active students
          content:
            application/json:
              schema:
                $ref: './commons.yaml#/components/schemas/ErrorResponse'
```

### Step 6.3: Regenerate DTOs and verify

```bash
mvn clean generate-sources -DskipTests
mvn clean compile -DskipTests
```

### Step 6.4: Commit

```bash
git add -A
git commit -m "docs(openapi): add 409 Conflict response to all delete endpoints

Add 409 response to 19 delete endpoints across all 6 modules.
Change tutor delete from 400 → 409 for active-students constraint.
Aligns API contracts with actual exception handling behavior."
```

---

## Phase 7: Deprecated Code Cleanup

### Step 7.1: Verify zero usages of old exception classes

Run these checks from the repo root. Each must return ZERO hits in `src/main/java/` (hits in `src/test/java/` or in the deprecated class file itself are acceptable at this stage):

```bash
# NotFound exceptions — should all be replaced by EntityNotFoundException
grep -rn "EmployeeNotFoundException\|CollaboratorNotFoundException\|AdultStudentNotFoundException\|TutorNotFoundException\|MinorStudentNotFoundException" --include="*.java" */src/main/java/

grep -rn "MembershipNotFoundException\|MembershipAdultStudentNotFoundException\|MembershipTutorNotFoundException\|PaymentAdultStudentNotFoundException\|PaymentTutorNotFoundException\|CompensationNotFoundException" --include="*.java" */src/main/java/

grep -rn "CourseNotFoundException\|CourseEventNotFoundException\|ScheduleNotFoundException" --include="*.java" */src/main/java/

grep -rn "StoreProductNotFoundException\|StoreTransactionNotFoundException" --include="*.java" */src/main/java/

grep -rn "NotificationNotFoundException" --include="*.java" */src/main/java/

# DeletionNotAllowed exceptions — should all be replaced
grep -rn "EmployeeDeletionNotAllowedException\|CollaboratorDeletionNotAllowedException\|AdultStudentDeletionNotAllowedException" --include="*.java" */src/main/java/

# Deprecated MessageService methods
grep -rn "getEmployeeNotFound\|getCollaboratorNotFound\|getAdultStudentNotFound\|getTutorNotFound\|getMinorStudentNotFound\|getMembershipNotFound\|getCompensationNotFound\|getCourseNotFound\|getScheduleNotFound\|getCourseEventNotFound\|getStoreProductNotFound\|getStoreTransactionNotFound\|getNotificationNotFound\|getPaymentAdultStudentNotFound\|getPaymentTutorNotFound\|getMembershipAdultStudentNotFound\|getMembershipTutorNotFound" --include="*.java" */src/main/java/
```

If ANY of the above return hits in `src/main/java/` (outside the deprecated class files), those sites were missed in Phase 5. Go back and fix them before proceeding.

### Step 7.2: Delete deprecated exception classes

```bash
# Find all per-entity exception files
find . -name "*NotFoundException.java" -path "*/exception/*" ! -name "EntityNotFoundException.java"
find . -name "*DeletionNotAllowedException.java" -path "*/exception/*" ! -name "EntityDeletionNotAllowedException.java"
```

Delete each file. Then verify compilation:
```bash
mvn clean compile -DskipTests
```

If compilation fails, a reference was missed. The error message will show exactly which file and line.

### Step 7.3: Remove deprecated MessageService methods

Open `MessageService.java`. Remove ALL methods marked `@Deprecated(since = "1.1", forRemoval = true)`. Verify no remaining callers:

```bash
mvn clean compile -DskipTests
```

### Step 7.4: Update test files

Remove test coverage for deleted exception classes and deleted MessageService methods. Run:

```bash
mvn test
```

Fix any remaining test failures — these will be tests that still import or reference the deleted classes.

### Step 7.5: Commit

```bash
git add -A
git commit -m "chore: remove deprecated per-entity exceptions and MessageService methods

Delete 13 per-entity NotFoundException classes, 3 DeletionNotAllowed
classes, and 20 per-entity MessageService methods.

All modules now use EntityNotFoundException,
EntityDeletionNotAllowedException, and generic MessageService methods."
```

---

## Phase 8: Final Verification

### Step 8.1: Full clean build

```bash
mvn clean install
```

### Step 8.2: Exception class inventory (post-migration)

```bash
find . -name "*Exception.java" -path "*/main/*" | sort
```

**Expected result:**
```
utilities/.../exceptions/DatabaseConnectionFailedException.java
utilities/.../exceptions/DuplicateEntityException.java
utilities/.../exceptions/EntityDeletionNotAllowedException.java
utilities/.../exceptions/EntityNotFoundException.java
utilities/.../exceptions/InternalServerErrorException.java
utilities/.../exceptions/InvalidTenantException.java
utilities/.../exceptions/security/DecryptionFailureException.java
utilities/.../exceptions/security/EncryptionFailureException.java
utilities/.../exceptions/security/ErrorNormalizationException.java
utilities/.../exceptions/security/HashingFailureException.java
course-management/.../exception/ScheduleNotAvailableException.java
security/.../exception/InvalidLoginException.java
```

That is 12 exception classes. Down from 21.

### Step 8.3: Handler count verification

```bash
grep -rn "@ExceptionHandler" --include="*.java" */src/main/java/ | sort
```

**Expected result:**
```
utilities/.../BaseControllerAdvice.java:  @ExceptionHandler(EntityNotFoundException.class)
utilities/.../BaseControllerAdvice.java:  @ExceptionHandler(EntityDeletionNotAllowedException.class)
utilities/.../BaseControllerAdvice.java:  @ExceptionHandler(DuplicateEntityException.class)
utilities/.../BaseControllerAdvice.java:  @ExceptionHandler(DataIntegrityViolationException.class)
utilities/.../BaseControllerAdvice.java:  @ExceptionHandler(MethodArgumentNotValidException.class)
utilities/.../BaseControllerAdvice.java:  @ExceptionHandler(InvalidTenantException.class)
utilities/.../BaseControllerAdvice.java:  @ExceptionHandler({EncryptionFailureException.class, DecryptionFailureException.class})
utilities/.../BaseControllerAdvice.java:  @ExceptionHandler(Exception.class)
course-management/.../CoordinationControllerAdvice.java:  @ExceptionHandler(ScheduleNotAvailableException.class)
security/.../SecurityControllerAdvice.java:  @ExceptionHandler(InvalidLoginException.class)
```

That is 10 handler declarations (8 in base + 2 domain-specific). Down from 23.

### Step 8.4: Verify ErrorResponseDTO.code population

Quick spot-check — every handler must set `error.setCode(...)`:

```bash
grep -A3 "@ExceptionHandler" utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java | grep "setCode"
```

Every handler must have a corresponding `setCode()` call.

### Step 8.5: Final commit (if any fixups)

```bash
git add -A
git commit -m "chore: post-migration verification cleanup"
```

---

## Verification Checklist

Run after ALL phases complete:

| Check | Command | Expected |
|-------|---------|----------|
| Full build | `mvn clean install` | BUILD SUCCESS |
| Exception count | `find . -name "*Exception.java" -path "*/main/*" \| wc -l` | 12 |
| Handler count | `grep -rn "@ExceptionHandler" --include="*.java" */src/main/java/ \| wc -l` | 10 |
| No old NFE refs | `grep -rn "EmployeeNotFoundException\|CollaboratorNotFoundException\|..." --include="*.java" */src/main/java/ \| wc -l` | 0 |
| No old DNA refs | `grep -rn "EmployeeDeletionNotAllowed\|..." --include="*.java" */src/main/java/ \| wc -l` | 0 |
| Code field set | `grep -c "setCode" BaseControllerAdvice.java` | ≥ 8 |
| 409 on delete specs | `grep -rn "'409'" --include="*.yaml" */src/main/resources/openapi/ \| wc -l` | ≥ 19 |

---

## Critical Reminders

1. **Read `docs/design/exception-advice-strategy.md` FIRST** — it contains the complete mapping table, message templates, and code structures. Do not deviate.
2. **Read `AI-CODE-REF.md` BEFORE writing any code** — testing conventions, naming, constants, Javadoc requirements.
3. **Process one module at a time** — extend advice → migrate use cases → remove old handlers → compile → test → commit. Never batch multiple modules.
4. **Verify with `grep`** before deleting anything — zero references in `src/main/java/` to old exception classes before removing them.
5. **The `ErrorResponseDTO` is generated** — read the actual generated class to get exact setter names. Do not assume.
6. **`EntityType` constants are message property keys** — they resolve to display names via `MessageService`. They are NOT display strings themselves.
7. **Two-step message resolution** in `MessageService.getEntityNotFound()`:
   - Step 1: `messageSource.getMessage("entity.employee", null, locale)` → `"Empleado"`
   - Step 2: `messageSource.getMessage("entity.not.found", new Object[]{"Empleado", "42"}, locale)` → `"Empleado con ID: 42 no existe!"`
8. **`BaseControllerAdvice` is NOT annotated with `@ControllerAdvice`** — it is abstract. Only subclasses carry the annotation with `basePackageClasses`.
9. **HTTP status changes**: DeletionNotAllowed 400→409, InvalidLogin 400→401. Update ALL assertions in tests.
10. **`String.valueOf(entityId)`** — use cases pass `Long` IDs, but `EntityNotFoundException` takes `String entityId`. Always convert.

---

## Commit Summary (Expected Sequence)

```
1. feat(utilities): add generic exception classes and EntityType constants
2. refactor(utilities): add generic MessageService methods, deprecate per-entity
3. feat(utilities): add BaseControllerAdvice with shared exception handlers
4. refactor(user-management): migrate to generic exceptions
5. refactor(billing): migrate to generic exceptions
6. refactor(course-management): migrate to generic exceptions
7. refactor(pos-system): migrate to generic exceptions
8. refactor(notification-system): migrate to generic exceptions
9. refactor(security): extend BaseControllerAdvice, fix InvalidLogin to 401
10. docs(openapi): add 409 Conflict response to all delete endpoints
11. chore: remove deprecated per-entity exceptions and MessageService methods
```

11 commits. Each atomic, each compilable, each with passing tests.
