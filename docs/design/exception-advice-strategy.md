# Exception Handling & ControllerAdvice Specification — AkademiaPlus

## 1. Current State Inventory

### 1.1 Exception Classes Across All Modules (21 total)

```
exception/  (per module)
│
├── NotFound (13 classes, identical structure)
│   ├── user-management:   EmployeeNFE, CollaboratorNFE, AdultStudentNFE, TutorNFE, MinorStudentNFE
│   ├── billing:           MembershipNFE, MembershipAdultStudentNFE, MembershipTutorNFE,
│   │                      PaymentAdultStudentNFE, PaymentTutorNFE, CompensationNFE
│   ├── course-management: CourseNFE, CourseEventNFE, ScheduleNFE
│   ├── pos-system:        StoreProductNFE, StoreTransactionNFE
│   └── notification:      NotificationNFE
│
├── DeletionNotAllowed (3 classes, identical structure)
│   └── user-management:   EmployeeDNA, CollaboratorDNA, AdultStudentDNA
│
└── Domain-Specific (2 classes)
    ├── course-management: ScheduleNotAvailableException
    └── security:          InvalidLoginException
```

Every NotFound exception: `extends RuntimeException { constructor(String msg) { super(msg); } }`
Every DeletionNotAllowed exception: `extends RuntimeException { constructor(Exception ex) { super(ex); } }`

### 1.2 ControllerAdvice Coverage (6 classes)

| Advice Class | Module | Handles |
|---|---|---|
| `PeopleControllerAdvice` | user-management | 5 NotFound + 3 DeletionNotAllowed + DataIntegrityViolation + Validation + Encryption |
| `BillingControllerAdvice` | billing | 6 NotFound |
| `CoordinationControllerAdvice` | course-management | 3 NotFound + ScheduleNotAvailable + CollaboratorNotFound |
| `PosControllerAdvice` | pos-system | 2 NotFound |
| `NotificationControllerAdvice` | notification | 1 NotFound |
| `SecurityControllerAdvice` | security | InvalidLogin |

### 1.3 Message Keys (es_MX locale)

```properties
# utilities_messages_es_MX.properties — entity display names
entity.adult.student=Alumno
entity.employee=Empleado
entity.collaborator=Colaborador
entity.tutor=Tutor
entity.minor.student=Alumno Menor
entity.course=Curso
entity.course.event=Evento de Curso
entity.membership=Membresia
entity.membership.adult.student=Membresia de Alumno
entity.membership.tutor=Membresia de Tutor
entity.payment.adult.student=Pago de Alumno
entity.payment.tutor=Pago de Tutor
entity.compensation=Compensacion
entity.notification=Notificacion
entity.store.product=Producto de Tienda
entity.store.transaction=Transaccion de Tienda
internal.error.high.severity=Error interno en la applicacion...

# user_management_messages_es_MX.properties — message templates
entity.not.found={0} con ID: {1} no existe!
entity.delete.not.allowed=Eliminacion de {0} no esta permitida! dicha accion corromperia los datos de la organizacion
invalid.data.email.creation.request=Error, valor enviado en campo correo electronico ya esta registrado
invalid.data.phone.creation.request=Error, valor enviado en campo numero de celular ya esta registrado
invalid.unknown.data.request=Propiedad (email, cel, etc...) enviada no es reconocida!
```

### 1.4 ErrorResponseDTO (generated from OpenAPI)

The DTO has fields: `timestamp`, `message`, `path`, `traceId`, `code`, `details[]`.
Currently only `message` is being populated by the advice classes. The `code` field and `details[]` are never set.

---

## 2. Problems to Solve

### P1: 13 NotFound exception classes with identical structure → should be 1 generic
### P2: 3 DeletionNotAllowed exception classes → will grow to 19 → should be 1 generic
### P3: No DeletionNotAllowed exceptions exist for 16 entities that need deletion
### P4: HTTP 400 used for DeletionNotAllowed → should be 409 Conflict
### P5: `ex.getCause().getCause().getMessage()` in DataIntegrity handler → NPE risk
### P6: PeopleControllerAdvice has per-entity handler explosion (8 handlers for 5 entities)
### P7: OpenAPI specs missing 409 response definition on delete endpoints
### P8: `ErrorResponseDTO.code` field never populated → lost diagnostic value
### P9: MessageService has 17 per-entity `getXxxNotFound()` methods → should be 1 generic

---

## 3. Exception Taxonomy (Target State)

### 3.1 New Shared Exceptions — `utilities` module

```
com.akademiaplus.utilities.exceptions/
│
├── EntityNotFoundException                    ← replaces 13 per-entity NFE classes
│   ├── fields: entityType (String), entityId (String)
│   ├── constructor(String entityType, String entityId)
│   └── getMessage() → delegates to MessageService
│
├── EntityDeletionNotAllowedException          ← replaces 3 + prevents 16 more DNA classes
│   ├── fields: entityType (String), entityId (String)
│   ├── constructor(String entityType, String entityId, Throwable cause)  ← DB constraint
│   ├── constructor(String entityType, String entityId, String reason)    ← business rule
│   └── getMessage() → delegates to MessageService
│
├── DuplicateEntityException                   ← replaces DataIntegrity email/phone parsing
│   ├── fields: entityType (String), field (String)
│   ├── constructor(String entityType, String field, Throwable cause)
│   └── getMessage() → delegates to MessageService
│
└── (existing, unchanged)
    ├── DatabaseConnectionFailedException
    ├── InternalServerErrorException
    ├── InvalidTenantException
    └── security/
        ├── DecryptionFailureException
        ├── EncryptionFailureException
        ├── ErrorNormalizationException
        └── HashingFailureException
```

### 3.2 Domain-Specific Exceptions — kept in their modules

These are NOT generic entity operations. They encode domain-specific business rules and stay where they are:

| Exception | Module | Reason to Keep |
|---|---|---|
| `ScheduleNotAvailableException` | course-management | Schedule conflict is a domain concept, not a generic CRUD error |
| `InvalidLoginException` | security | Authentication failure is a security concern, not entity CRUD |

---

## 4. Exception-to-HTTP Mapping Specification

### 4.1 Complete Mapping Table

| Exception | HTTP Code | Code Field | Message Source | Used By |
|---|---|---|---|---|
| `EntityNotFoundException` | **404** Not Found | `ENTITY_NOT_FOUND` | `entity.not.found` with `{entityType, entityId}` | All modules |
| `EntityDeletionNotAllowedException` (DB constraint) | **409** Conflict | `DELETION_CONSTRAINT_VIOLATION` | `entity.delete.not.allowed` with `{entityType}` | All modules |
| `EntityDeletionNotAllowedException` (business rule) | **409** Conflict | `DELETION_BUSINESS_RULE` | Custom reason passed in constructor | user-management (Tutor) |
| `DuplicateEntityException` | **409** Conflict | `DUPLICATE_ENTITY` | `entity.duplicate.field` with `{entityType, field}` | user-management |
| `DataIntegrityViolationException` (unclassified) | **409** Conflict | `DATA_INTEGRITY_VIOLATION` | `invalid.unknown.data.request` | Fallback |
| `MethodArgumentNotValidException` | **400** Bad Request | `VALIDATION_ERROR` | Bean validation field errors | All modules |
| `EncryptionFailureException` | **500** Internal Server Error | `INTERNAL_ERROR` | `internal.error.high.severity` | All modules |
| `DecryptionFailureException` | **500** Internal Server Error | `INTERNAL_ERROR` | `internal.error.high.severity` | All modules |
| `InvalidTenantException` | **400** Bad Request | `INVALID_TENANT` | `invalid.tenant` | All modules |
| `ScheduleNotAvailableException` | **409** Conflict | `SCHEDULE_CONFLICT` | `schedule.not.available` | course-management |
| `InvalidLoginException` | **401** Unauthorized | `INVALID_CREDENTIALS` | `invalid.login` | security |
| `Exception` (fallback) | **500** Internal Server Error | `INTERNAL_ERROR` | `internal.error.high.severity` | All modules |

### 4.2 HTTP Status Code Rationale

| Code | Meaning (RFC 9110) | When to Use |
|---|---|---|
| **400** | Request syntax/framing error | Bean validation failures, malformed JSON, invalid tenant header |
| **401** | Missing/invalid authentication | Bad credentials, expired JWT |
| **404** | Resource does not exist | Entity not found by composite key |
| **409** | Request conflicts with resource state | FK constraint prevents deletion, duplicate unique key, schedule conflict |
| **500** | Server-side failure | Encryption/decryption errors, unhandled exceptions |

Key change: **DeletionNotAllowed moves from 400 → 409.** The client sent a valid request. The server understood it. But the resource's current state (has children, active references) prevents the operation. That is a 409.

Similarly, **InvalidLogin moves from 400 → 401.** The request is structurally valid; the credentials are wrong. That is an authentication failure = 401.

---

## 5. New Message Keys

### 5.1 Additions to `utilities_messages_es_MX.properties`

```properties
# Entity names already exist — no changes needed

# New: generic tenant message
entity.tenant=Organizacion
entity.tenant.subscription=Suscripcion de Organizacion
entity.tenant.billing.cycle=Ciclo de Facturacion
```

### 5.2 Additions to `user_management_messages_es_MX.properties`

```properties
# Existing — no changes
entity.not.found={0} con ID: {1} no existe!
entity.delete.not.allowed=Eliminacion de {0} no esta permitida! dicha accion corromperia los datos de la organizacion

# New: deletion with reason (business rule variant)
entity.delete.not.allowed.reason=Eliminacion de {0} con ID: {1} no es posible: {2}

# New: duplicate field violation
entity.duplicate.field=Error: el campo {1} del {0} ya esta registrado

# New: generic unclassified constraint
entity.constraint.violation=Operacion no permitida: conflicto de integridad de datos

# New: invalid tenant
invalid.tenant=Contexto de organizacion es requerido
```

### 5.3 MessageService New Methods

```java
// REPLACES: 17 per-entity getXxxNotFound() methods
public String getEntityNotFound(String entityType, String entityId) {
    return messageSource.getMessage(ENTITY_NOT_FOUND,
            new Object[]{entityType, entityId}, locale);
}

// REPLACES: 3 per-entity getXxxDeleteNotAllowed() methods
public String getEntityDeleteNotAllowed(String entityType) {
    return messageSource.getMessage(ENTITY_DELETE_NOT_ALLOWED,
            new Object[]{entityType}, locale);
}

// NEW: deletion blocked with business reason
public String getEntityDeleteNotAllowedWithReason(String entityType, String entityId, String reason) {
    return messageSource.getMessage(ENTITY_DELETE_NOT_ALLOWED_REASON,
            new Object[]{entityType, entityId, reason}, locale);
}

// NEW: duplicate field
public String getEntityDuplicateField(String entityType, String field) {
    return messageSource.getMessage(ENTITY_DUPLICATE_FIELD,
            new Object[]{entityType, field}, locale);
}
```

The 17 per-entity `getXxxNotFound()` methods and 3 `getXxxDeleteNotAllowed()` methods become `@Deprecated` and are retained for one release cycle until all consumers migrate.

---

## 6. ControllerAdvice Target State

### 6.1 Abstract Base Advice — `utilities` module

All 6 module-specific `ControllerAdvice` classes share the same handlers for generic exceptions. Rather than duplicating them 6 times, introduce a base class:

```java
package com.akademiaplus.utilities.web;

/**
 * Base exception handling for all module-specific ControllerAdvice classes.
 * Provides handlers for shared exception types that occur across all modules.
 * 
 * Module-specific ControllerAdvice classes extend this and add domain-specific handlers.
 */
public abstract class BaseControllerAdvice {

    private final MessageService messageService;

    protected BaseControllerAdvice(MessageService messageService) {
        this.messageService = messageService;
    }

    protected MessageService messageService() { return messageService; }

    // ── 404: Entity Not Found ──────────────────────────────────────────
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFound(EntityNotFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService.getEntityNotFound(ex.getEntityType(), ex.getEntityId()));
        error.setCode("ENTITY_NOT_FOUND");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ── 409: Deletion Not Allowed (constraint or business rule) ────────
    @ExceptionHandler(EntityDeletionNotAllowedException.class)
    public ResponseEntity<ErrorResponseDTO> handleDeletionNotAllowed(
            EntityDeletionNotAllowedException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        if (ex.getReason() != null) {
            // Business rule violation (e.g., Tutor has active MinorStudents)
            error.setMessage(messageService.getEntityDeleteNotAllowedWithReason(
                    ex.getEntityType(), ex.getEntityId(), ex.getReason()));
            error.setCode("DELETION_BUSINESS_RULE");
        } else {
            // DB constraint violation
            error.setMessage(messageService.getEntityDeleteNotAllowed(ex.getEntityType()));
            error.setCode("DELETION_CONSTRAINT_VIOLATION");
        }
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ── 409: Duplicate Entity (unique constraint on create/update) ─────
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateEntity(DuplicateEntityException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService.getEntityDuplicateField(
                ex.getEntityType(), ex.getField()));
        error.setCode("DUPLICATE_ENTITY");
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ── 409: Unclassified DataIntegrityViolation (fallback) ────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService.getConstraintViolation());
        error.setCode("DATA_INTEGRITY_VIOLATION");
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ── 400: Bean Validation ───────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        List<ErrorDetailDTO> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    ErrorDetailDTO d = new ErrorDetailDTO();
                    d.setField(fe.getField());
                    d.setMessage(fe.getDefaultMessage());
                    return d;
                })
                .toList();
        error.setMessage("Error de validacion en la solicitud");
        error.setCode("VALIDATION_ERROR");
        error.setDetails(details);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ── 400: Invalid Tenant Context ────────────────────────────────────
    @ExceptionHandler(InvalidTenantException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidTenant(InvalidTenantException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService.getInvalidTenant());
        error.setCode("INVALID_TENANT");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ── 500: Encryption/Decryption ─────────────────────────────────────
    @ExceptionHandler({EncryptionFailureException.class, DecryptionFailureException.class})
    public ResponseEntity<ErrorResponseDTO> handleCryptoFailure(RuntimeException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService.getInternalErrorHighSeverity());
        error.setCode("INTERNAL_ERROR");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── 500: Unhandled Fallback ────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(Exception ex) {
        // Log at ERROR level - this is a genuine bug
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService.getInternalErrorHighSeverity());
        error.setCode("INTERNAL_ERROR");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### 6.2 Module-Specific Advice Classes (Target State)

Each module extends `BaseControllerAdvice` and only adds handlers for domain-specific exceptions.

**PeopleControllerAdvice — after refactoring:**

```java
@ControllerAdvice(basePackageClasses = {EmployeeController.class, CollaboratorController.class,
        AdultStudentController.class, TutorController.class, MinorStudentController.class})
public class PeopleControllerAdvice extends BaseControllerAdvice {

    public PeopleControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    // No additional handlers needed.
    // All people exceptions (NotFound, DeletionNotAllowed, Duplicate, Validation, Encryption)
    // are handled by BaseControllerAdvice.
}
```

**BillingControllerAdvice — after refactoring:**

```java
@ControllerAdvice(basePackageClasses = {MembershipController.class, /* ... */})
public class BillingControllerAdvice extends BaseControllerAdvice {

    public BillingControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    // No additional handlers needed.
    // Current 6 per-entity NFE handlers replaced by one BaseControllerAdvice handler.
}
```

**CoordinationControllerAdvice — keeps 1 domain handler:**

```java
@ControllerAdvice(basePackageClasses = {CourseController.class, ScheduleController.class,
        CourseEventController.class})
public class CoordinationControllerAdvice extends BaseControllerAdvice {

    public CoordinationControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    // Domain-specific: schedule conflict is NOT a generic entity error
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

**SecurityControllerAdvice — fixes 400→401:**

```java
@ControllerAdvice(basePackageClasses = {InternalAuthController.class})
public class SecurityControllerAdvice extends BaseControllerAdvice {

    public SecurityControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidLogin(InvalidLoginException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService().getInvalidLogin());
        error.setCode("INVALID_CREDENTIALS");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
```

**PosControllerAdvice & NotificationControllerAdvice — become empty shells:**

```java
@ControllerAdvice(basePackageClasses = {StoreProductController.class, StoreTransactionController.class})
public class PosControllerAdvice extends BaseControllerAdvice {
    public PosControllerAdvice(MessageService messageService) { super(messageService); }
}

@ControllerAdvice(basePackageClasses = {NotificationController.class})
public class NotificationControllerAdvice extends BaseControllerAdvice {
    public NotificationControllerAdvice(MessageService messageService) { super(messageService); }
}
```

---

## 7. Handler Count: Before vs After

| Advice Class | Before (handlers) | After (handlers) | Removed |
|---|---|---|---|
| `PeopleControllerAdvice` | 8 explicit | 0 (all inherited) | -8 |
| `BillingControllerAdvice` | 6 explicit | 0 (all inherited) | -6 |
| `CoordinationControllerAdvice` | 5 explicit | 1 (ScheduleConflict) | -4 |
| `PosControllerAdvice` | 2 explicit | 0 (all inherited) | -2 |
| `NotificationControllerAdvice` | 1 explicit | 0 (all inherited) | -1 |
| `SecurityControllerAdvice` | 1 explicit | 1 (InvalidLogin) | 0 |
| `BaseControllerAdvice` | N/A | 7 shared | +7 |
| **Total** | **23** | **9** | **-14** |

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidLogin(InvalidLoginException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(messageService().getInvalidLogin());
        error.setCode("INVALID_CREDENTIALS");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
```

**PosControllerAdvice & NotificationControllerAdvice — become empty shells:**

```java
@ControllerAdvice(basePackageClasses = {StoreProductController.class, StoreTransactionController.class})
public class PosControllerAdvice extends BaseControllerAdvice {
    public PosControllerAdvice(MessageService messageService) { super(messageService); }
}

@ControllerAdvice(basePackageClasses = {NotificationController.class})
public class NotificationControllerAdvice extends BaseControllerAdvice {
    public NotificationControllerAdvice(MessageService messageService) { super(messageService); }
}
```

---

## 7. Handler Count: Before vs After

| Advice Class | Before (handlers) | After (handlers) | Removed |
|---|---|---|---|
| `PeopleControllerAdvice` | 8 explicit | 0 (all inherited) | -8 |
| `BillingControllerAdvice` | 6 explicit | 0 (all inherited) | -6 |
| `CoordinationControllerAdvice` | 5 explicit | 1 (ScheduleConflict) | -4 |
| `PosControllerAdvice` | 2 explicit | 0 (all inherited) | -2 |
| `NotificationControllerAdvice` | 1 explicit | 0 (all inherited) | -1 |
| `SecurityControllerAdvice` | 1 explicit | 1 (InvalidLogin) | 0 |
| `BaseControllerAdvice` | N/A | 7 shared | +7 |
| **Total** | **23** | **9** | **-14** |

---

## 8. Exception Flow Diagrams

### 8.1 Delete UseCase → HTTP Response

```
Controller.deleteEntity(entityId)
    │
    ▼
DeleteEntityUseCase.delete(entityId)
    │
    ├─ TenantContextHolder.requireTenantId()
    │   └─ throws InvalidTenantException ──────────► BaseAdvice → 400 INVALID_TENANT
    │
    ├─ repository.findById(compositeId)
    │   └─ empty → throws EntityNotFoundException ─► BaseAdvice → 404 ENTITY_NOT_FOUND
    │       message: "Empleado con ID: 42 no existe!"
    │
    ├─ [Optional: pre-delete business validation]
    │   └─ fails → throws EntityDeletionNotAllowedException(reason)
    │       ► BaseAdvice → 409 DELETION_BUSINESS_RULE
    │       message: "Eliminacion de Tutor con ID: 7 no es posible:
    │                 Tutor tiene 3 alumno(s) menor(es) activo(s)"
    │
    └─ repository.delete(entity)   ← triggers @SQLDelete soft-delete
        │
        ├─ success ────────────────► Controller → 204 No Content
        │
        └─ DataIntegrityViolationException
            └─ caught → throws EntityDeletionNotAllowedException(cause)
                ► BaseAdvice → 409 DELETION_CONSTRAINT_VIOLATION
                message: "Eliminacion de Empleado no esta permitida!
                          dicha accion corromperia los datos de la organizacion"
```

### 8.2 Create UseCase → HTTP Response (for context — DuplicateEntity)

```
Controller.createEntity(request)
    │
    ▼
CreateEntityUseCase.create(request)
    │
    ├─ validation fails ───────────► BaseAdvice → 400 VALIDATION_ERROR
    │
    ├─ repository.save(entity)
    │   │
    │   ├─ success ────────────────► Controller → 201 Created
    │   │
    │   └─ DataIntegrityViolationException
    │       │
    │       ├─ cause contains "email"
    │       │   → throws DuplicateEntityException("Employee", "email")
    │       │   ► BaseAdvice → 409 DUPLICATE_ENTITY
    │       │   message: "Error: el campo email del Empleado ya esta registrado"
    │       │
    │       ├─ cause contains "phone"
    │       │   → throws DuplicateEntityException("Employee", "phoneNumber")
    │       │   ► BaseAdvice → 409 DUPLICATE_ENTITY
    │       │
    │       └─ cause unknown ────── falls through to BaseAdvice DataIntegrity handler
    │           ► BaseAdvice → 409 DATA_INTEGRITY_VIOLATION
    │           message: "Operacion no permitida: conflicto de integridad de datos"
    │
    └─ encryption error ───────────► BaseAdvice → 500 INTERNAL_ERROR
```

---

## 9. ErrorResponseDTO Population Strategy

Currently only `message` is set. The `code` field provides machine-readable error categorization for API consumers. The `details` field provides structured field-level information for validation errors.

### 9.1 Response Body Examples

**404 — Entity Not Found:**
```json
{
  "message": "Empleado con ID: 42 no existe!",
  "code": "ENTITY_NOT_FOUND"
}
```

**409 — Deletion Blocked by DB Constraint:**
```json
{
  "message": "Eliminacion de Empleado no esta permitida! dicha accion corromperia los datos de la organizacion",
  "code": "DELETION_CONSTRAINT_VIOLATION"
}
```

**409 — Deletion Blocked by Business Rule:**
```json
{
  "message": "Eliminacion de Tutor con ID: 7 no es posible: Tutor tiene 3 alumno(s) menor(es) activo(s)",
  "code": "DELETION_BUSINESS_RULE"
}
```

**409 — Duplicate on Create:**
```json
{
  "message": "Error: el campo email del Empleado ya esta registrado",
  "code": "DUPLICATE_ENTITY"
}
```

**409 — Schedule Conflict:**
```json
{
  "message": "El horario del curso conflictua con uno o mas cursos existentes: Lunes 10:00-11:00",
  "code": "SCHEDULE_CONFLICT"
}
```

**400 — Validation Error:**
```json
{
  "message": "Error de validacion en la solicitud",
  "code": "VALIDATION_ERROR",
  "details": [
    { "field": "email", "message": "must not be blank" },
    { "field": "birthDate", "message": "must not be null" }
  ]
}
```

**400 — Invalid Tenant:**
```json
{
  "message": "Contexto de organizacion es requerido",
  "code": "INVALID_TENANT"
}
```

**401 — Invalid Login:**
```json
{
  "message": "nombre de usuario o contraseña no son validos",
  "code": "INVALID_CREDENTIALS"
}
```

**500 — Internal Error:**
```json
{
  "message": "Error interno en la applicacion, favor de contactar a soporte tecnico, email: david.martinez@elatus-dev.com",
  "code": "INTERNAL_ERROR"
}
```

---

## 10. OpenAPI Spec Updates Required

Every delete endpoint must add the `409` response. Example patch for `employee-api.yaml`:

```yaml
    delete:
      # ...existing...
      responses:
        '204':
          description: Employee deleted successfully
        '401':
          $ref: './commons.yaml#/components/responses/Unauthorized'
        '404':
          $ref: './commons.yaml#/components/responses/NotFound'
        '409':                                              # ← ADD
          $ref: './commons.yaml#/components/responses/Conflict'   # ← ADD
```

The `Conflict` response reference already exists in `commons.yaml`.

Tutor's delete endpoint already has a `400` response for active-students. Change it to `409`:

```yaml
    delete:
      # ...existing...
      responses:
        '204':
          description: Tutor deleted successfully
        '409':                                              # ← WAS '400'
          description: Cannot delete tutor with active students
          content:
            application/json:
              schema:
                $ref: './commons.yaml#/components/schemas/ErrorResponse'
        '401':
          $ref: './commons.yaml#/components/responses/Unauthorized'
        '404':
          $ref: './commons.yaml#/components/responses/NotFound'
```

**Files to update (19 delete endpoints across all modules):**
- `user-management/openapi/employee-api.yaml` — add 409
- `user-management/openapi/collaborator-api.yaml` — add 409
- `user-management/openapi/adult-student-api.yaml` — add 409
- `user-management/openapi/tutor-api.yaml` — change 400→409, add 409 ref
- `user-management/openapi/minor-student-api.yaml` — add 409
- `billing/openapi/compensation.yaml` — add 409
- `billing/openapi/membership.yaml` — add 409
- `billing/openapi/membership-management.yaml` — add 409 (×2 endpoints)
- `billing/openapi/payment-management.yaml` — add 409 (×2 endpoints)
- `course-management/openapi/course.yaml` — add 409
- `course-management/openapi/schedule.yaml` — add 409
- `course-management/openapi/course-event.yaml` — add 409
- `pos-system/openapi/store-product.yaml` — add 409
- `pos-system/openapi/store-transaction.yaml` — add 409
- `tenant-management/openapi/tenant-management-module.yaml` — add 409 (×2 endpoints)
- `notification-system/openapi/notification.yaml` — add 409

---

## 11. Deprecation Schedule

### Phase 1 (immediate): Mark @Deprecated

```java
@Deprecated(since = "1.1", forRemoval = true)
public class EmployeeNotFoundException extends RuntimeException { ... }
```

All 13 per-entity NotFound + 3 DeletionNotAllowed exception classes.

All 17 per-entity `getXxxNotFound()` methods and 3 `getXxxDeleteNotAllowed()` methods in `MessageService`.

### Phase 2 (next release): Remove

Delete all deprecated classes and methods once all modules have migrated to the generic exceptions.

---

## 12. Entity Type Constants

To avoid magic strings scattered across use cases, define a constants class:

```java
package com.akademiaplus.utilities;

/**
 * Canonical entity type identifiers used for exception messages.
 * Values correspond to message property keys in utilities_messages_es_MX.properties.
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

This means the generic `EntityNotFoundException` stores the message key, and `MessageService` resolves it:

```java
// In DeleteEmployeeUseCase:
throw new EntityNotFoundException(EntityType.EMPLOYEE, String.valueOf(employeeId));

// In MessageService.getEntityNotFound:
String entityName = messageSource.getMessage(entityTypeKey, null, locale);  // "Empleado"
return messageSource.getMessage(ENTITY_NOT_FOUND, new Object[]{entityName, entityId}, locale);
// → "Empleado con ID: 42 no existe!"
```

This keeps the use cases decoupled from the display strings and supports future i18n by swapping locale-specific property files.
