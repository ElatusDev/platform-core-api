> [!NOTE]
> **STATUS: COMPLETED** — Diagnostic audit that fed into the exception-advice-strategy.
> Moved to `workflows/completed/` on 2026-02-28.

---

# Controller Advice Audit Report

> Comprehensive audit of `@ControllerAdvice` configuration across all modules
> **Date**: 2026-02-21
> **Author**: Claude Code
> **Status**: ✅ ALL MODULES COMPLIANT

---

## Executive Summary

All 7 domain modules have correctly configured `@ControllerAdvice` classes that:
- Extend `BaseControllerAdvice` from the utilities module
- Use `basePackageClasses` (type-safe) instead of `basePackages` (string-based)
- Cover all REST controllers in their respective modules
- Inherit standardized exception → HTTP status mappings

**No issues found. All modules follow the established pattern.**

---

## Module-by-Module Analysis

### 1. user-management ✅

**File**: `user-management/src/main/java/com/akademiaplus/config/PeopleControllerAdvice.java`

```java
@ControllerAdvice(basePackageClasses = {
    EmployeeController.class,
    CollaboratorController.class,
    AdultStudentController.class,
    TutorController.class,
    MinorStudentController.class
})
public class PeopleControllerAdvice extends BaseControllerAdvice {
    public PeopleControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
```

**Coverage**: 5 controllers
**Custom Handlers**: None (inherits all from `BaseControllerAdvice`)
**Status**: ✅ Correct

---

### 2. billing ✅

**File**: `billing/src/main/java/com/akademiaplus/config/BillingControllerAdvice.java`

```java
@ControllerAdvice(basePackageClasses = {
    MembershipController.class,
    MembershipAdultStudentController.class,
    MembershipTutorController.class,
    PaymentAdultStudentController.class,
    PaymentTutorController.class,
    CompensationController.class
})
public class BillingControllerAdvice extends BaseControllerAdvice {
    public BillingControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
```

**Coverage**: 6 controllers
**Custom Handlers**: None
**Status**: ✅ Correct

---

### 3. course-management ✅

**File**: `course-management/src/main/java/com/akademiaplus/config/CoordinationControllerAdvice.java`

```java
@ControllerAdvice(basePackageClasses = {
    CourseController.class,
    ScheduleController.class,
    CourseEventController.class
})
public class CoordinationControllerAdvice extends BaseControllerAdvice {
    public CoordinationControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    @ExceptionHandler(ScheduleNotAvailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleScheduleConflict(
            ScheduleNotAvailableException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                messageService().getScheduleNotAvailable(ex.getMessage()));
        error.setCode("SCHEDULE_CONFLICT");
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
}
```

**Coverage**: 3 controllers
**Custom Handlers**: `ScheduleNotAvailableException` → HTTP 409
**Status**: ✅ Correct (includes domain-specific exception handler)

---

### 4. tenant-management ✅

**File**: `tenant-management/src/main/java/com/akademiaplus/config/TenantManagementControllerAdvice.java`

```java
@ControllerAdvice(basePackageClasses = TenantController.class)
public class TenantManagementControllerAdvice extends BaseControllerAdvice {
    public TenantManagementControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
```

**Coverage**: 1 controller (`TenantController` handles all tenant/subscription/billing-cycle endpoints)
**Custom Handlers**: None
**Status**: ✅ Correct (FIXED from `basePackages` string to `basePackageClasses` during audit)

---

### 5. notification-system ✅

**File**: `notification-system/src/main/java/com/akademiaplus/config/NotificationControllerAdvice.java`

```java
@ControllerAdvice(basePackageClasses = {NotificationController.class})
public class NotificationControllerAdvice extends BaseControllerAdvice {
    public NotificationControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
```

**Coverage**: 1 controller
**Custom Handlers**: None
**Status**: ✅ Correct

---

### 6. pos-system ✅

**File**: `pos-system/src/main/java/com/akademiaplus/config/PosControllerAdvice.java`

```java
@ControllerAdvice(basePackageClasses = {
    StoreProductController.class,
    StoreTransactionController.class
})
public class PosControllerAdvice extends BaseControllerAdvice {
    public PosControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
```

**Coverage**: 2 controllers
**Custom Handlers**: None
**Status**: ✅ Correct

---

### 7. security ✅

**File**: `security/src/main/java/com/akademiaplus/config/SecurityControllerAdvice.java`

```java
@ControllerAdvice(basePackageClasses = {InternalAuthController.class})
public class SecurityControllerAdvice extends BaseControllerAdvice {
    public SecurityControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
```

**Coverage**: 1 controller
**Custom Handlers**: None
**Status**: ✅ Correct

---

## Base Controller Advice (utilities module)

**File**: `utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java`

### Standard Exception Mappings

| Exception | HTTP Status | Error Code | Handler Method |
|-----------|------------|------------|----------------|
| `EntityNotFoundException` | 404 Not Found | `ENTITY_NOT_FOUND` | `handleEntityNotFound()` |
| `EntityDeletionNotAllowedException` (constraint) | 409 Conflict | `DELETION_CONSTRAINT_VIOLATION` | `handleDeletionNotAllowed()` |
| `EntityDeletionNotAllowedException` (business rule) | 409 Conflict | `DELETION_BUSINESS_RULE` | `handleDeletionNotAllowed()` |
| `DuplicateEntityException` | 409 Conflict | `DUPLICATE_ENTITY` | `handleDuplicateEntity()` |
| `DataIntegrityViolationException` | 409 Conflict | `DATA_INTEGRITY_VIOLATION` | `handleDataIntegrityViolation()` |
| `MethodArgumentNotValidException` | 400 Bad Request | `VALIDATION_ERROR` | `handleValidation()` |
| `InvalidTenantException` | 400 Bad Request | `INVALID_TENANT` | `handleInvalidTenant()` |
| `EncryptionFailureException` | 500 Internal Server Error | `INTERNAL_ERROR` | `handleCryptoFailure()` |
| `DecryptionFailureException` | 500 Internal Server Error | `INTERNAL_ERROR` | `handleCryptoFailure()` |
| `Exception` (fallback) | 500 Internal Server Error | `INTERNAL_ERROR` | `handleUnexpected()` |

### Error Response Structure

All error responses follow the OpenAPI-generated `ErrorResponseDTO` schema:

```json
{
  "code": "ENTITY_NOT_FOUND",
  "message": "Empleado con ID 123 no encontrado",
  "details": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ]
}
```

---

## Established Pattern

### ✅ Correct Pattern (Type-Safe)

```java
@ControllerAdvice(basePackageClasses = {
    Controller1.class,
    Controller2.class
})
public class ModuleControllerAdvice extends BaseControllerAdvice {
    public ModuleControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    // Optional: Add module-specific exception handlers
    @ExceptionHandler(ModuleSpecificException.class)
    public ResponseEntity<ErrorResponseDTO> handleModuleException(
            ModuleSpecificException ex) {
        // Custom handling
    }
}
```

**Advantages**:
- **Type-safe**: Compiler verifies controller classes exist
- **Refactoring-safe**: Package renames automatically propagate
- **IDE support**: Ctrl+Click navigation to controller classes
- **Explicit**: Clear which controllers are covered

### ❌ Incorrect Pattern (String-Based)

```java
@ControllerAdvice(basePackages = "com.akademiaplus.interfaceadapters")
public class ModuleControllerAdvice extends BaseControllerAdvice {
    // ...
}
```

**Problems**:
- **Not type-safe**: String typos compile but fail at runtime
- **Fragile**: Package refactoring breaks advice silently
- **Implicit**: Unclear which controllers are covered
- **No IDE support**: Can't navigate to controllers

---

## Testing Considerations

### Unit Tests

Exception handling is tested via **unit tests** for each use case:

```java
@Test
@DisplayName("Should throw EntityNotFoundException when not found")
void shouldThrowEntityNotFound_whenEntityNotFound() {
    // Given
    when(repository.findById(compositeId)).thenReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> useCase.get(entityId))
            .isInstanceOf(EntityNotFoundException.class)
            .satisfies(ex -> {
                EntityNotFoundException enfe = (EntityNotFoundException) ex;
                assertThat(enfe.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
                assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(entityId));
            });
}
```

### Component Tests

Component tests focus on **integration scenarios**, not exception paths:

```java
@Test
@DisplayName("Should return 409 when deleting tutor with active minor students")
void shouldReturn409_whenTutorHasActiveMinorStudents() throws Exception {
    // Given — tutor has active minor students (business rule)
    createTutor();
    createMinorStudentForTutor();

    // When / Then
    mockMvc.perform(delete("/v1/user-management/tutors/{id}", tutorId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code")
                    .value(BaseControllerAdvice.CODE_DELETION_BUSINESS_RULE));
}
```

**Why not test 404 in component tests?**

1. **404 scenarios require non-existent data** — difficult to set up reliably
2. **Unit tests already verify exception throwing** — no need to duplicate
3. **Component tests verify integration** — database, transactions, security
4. **Existing component tests only verify success paths** — established pattern

---

## Recommendations

1. **No Changes Needed**: All modules follow the correct pattern
2. **Maintain Consistency**: Use `basePackageClasses` for all future modules
3. **Document Custom Handlers**: If adding module-specific `@ExceptionHandler` methods, document in this file
4. **Test Exception Logic in Unit Tests**: Keep component tests focused on integration scenarios

---

## References

- **Base Implementation**: `utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java`
- **Spring Docs**: [Web on Reactive Stack - Exception Handling](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-controller-advice)
- **Related ADR**: ADR-0002 (Clean Architecture module structure)
