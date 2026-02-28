# Delete UseCase Strategy — AkademiaPlus Platform

## 1. Current State Analysis

### 1.1 Soft Delete Infrastructure (✅ Solid Foundation)

The inheritance chain `SoftDeletable → TenantScoped → AbstractUser → Entity` provides a well-layered soft-delete mechanism:

| Layer | Annotation | Purpose |
|---|---|---|
| `SoftDeletable` | `@SQLRestriction("deleted_at IS NULL")` | Filters soft-deleted rows from all JPA queries |
| `SoftDeletable` | `@FilterDef("softDeleteFilter")` + `@Filter` | Hibernate session-level soft-delete filter |
| `TenantScoped` | `@FilterDef("tenantFilter")` + `@Filter` | Tenant isolation at Hibernate session level |
| Each Entity | `@SQLDelete(sql = "UPDATE ... SET deleted_at = CURRENT_TIMESTAMP WHERE ...")` | Intercepts `DELETE` SQL → converts to `UPDATE` |

Every `DataModel` entity (29 total) already has `@SQLDelete` declared, meaning `repository.delete(entity)` already performs a soft delete at the JPA level—it never issues a physical `DELETE`.

### 1.2 Existing Delete UseCases (3 of 29 entities)

Only `user-management` has delete implementations:

| Entity | UseCase | ControllerAdvice Handler | Exception | Test |
|---|---|---|---|---|
| Employee | `DeleteEmployeeUseCase` | ✅ Handled | `EmployeeDeletionNotAllowedException` | ❌ None |
| Collaborator | `DeleteCollaboratorUseCase` | ✅ Handled | `CollaboratorDeletionNotAllowedException` | ❌ None |
| AdultStudent | `DeleteAdultStudentUseCase` | ✅ Handled | `AdultStudentDeletionNotAllowedException` | ❌ None |
| Tutor | ❌ Missing | ❌ Missing | ❌ Missing | ❌ None |
| MinorStudent | ❌ Missing | ❌ Missing | ❌ Missing | ❌ None |

### 1.3 The Current Pattern (Repeated Verbatim x3)

```java
public void delete(Long entityId) {
    Long tenantId = tenantContextHolder.getTenantId()
            .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));
    Optional<EntityDataModel> found = repository.findById(
            new EntityDataModel.EntityCompositeId(tenantId, entityId));
    if (found.isPresent()) {
        try {
            repository.delete(found.get());
        } catch (DataIntegrityViolationException ex) {
            throw new EntityDeletionNotAllowedException(ex);
        }
    } else {
        throw new EntityNotFoundException(String.valueOf(entityId));
    }
}
```

### 1.4 OpenAPI Delete Endpoints Defined (Not Yet Implemented)

| Module | Entities with `delete:` in OpenAPI | Implementation Status |
|---|---|---|
| **user-management** | Employee, Collaborator, AdultStudent, Tutor, MinorStudent | 3/5 implemented |
| **billing** | Compensation, Membership, MembershipAdultStudent, MembershipTutor, PaymentAdultStudent, PaymentTutor | 0/6 |
| **course-management** | Course, Schedule, CourseEvent | 0/3 |
| **pos-system** | StoreProduct, StoreTransaction | 0/2 |
| **tenant-management** | Tenant, TenantSubscription | 0/2 |
| **notification-system** | Notification | 0/1 |

**Total: 3 implemented / 19 defined in OpenAPI / 29 total entities**

---

## 2. Critical Issues Found

### 🔴 BUG: `@SQLDelete` WHERE Clause Missing Entity ID

**Severity: CRITICAL — silent data corruption in production**

Every `@SQLDelete` annotation uses only `WHERE tenant_id = ?`, which with `@IdClass` composite keys means Hibernate passes the first key component (tenant_id) but the generated SQL soft-deletes **ALL rows for the entire tenant**, not just the target entity.

```java
// CURRENT (BROKEN)
@SQLDelete(sql = "UPDATE employees SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")

// CORRECT
@SQLDelete(sql = "UPDATE employees SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND employee_id = ?")
```

**Impact:** Calling `repository.delete(employee)` would set `deleted_at` on ALL employees for that tenant. This affects all 29 entities.

### 🟡 Issue: Exception Proliferation

Each entity creates its own `XxxDeletionNotAllowedException` class with identical structure. The trajectory leads to 19+ nearly identical exception classes.

### 🟡 Issue: Incorrect HTTP Status for Constraint Violations

`PeopleControllerAdvice` returns `HttpStatus.BAD_REQUEST` (400) for `DeletionNotAllowedException`. Per RFC 9110, a constraint violation during deletion is a **409 Conflict**.

### 🟡 Issue: Fragile Cause Chain Traversal

```java
String errorMessage = ex.getCause().getCause().getMessage();  // ← NPE risk
```

### 🟡 Issue: Zero Test Coverage for Deletion

No unit tests exist for any of the 3 existing delete use cases.

---

## 3. Proposed Strategy

### 3.1 Design Principles

1. **Single generic exception hierarchy** — one `EntityDeletionNotAllowedException` and one `EntityNotFoundException` in `utilities`, parameterized by entity name.
2. **HTTP 409 Conflict for constraint violations** — semantically correct per RFC 9110.
3. **Centralized constraint violation translation** — a single `@ExceptionHandler` per `ControllerAdvice`.
4. **Defensive cause extraction** — utility method for safe exception chain traversal.
5. **Shared utility over abstract class** — `DeleteUseCaseSupport` utility, not inheritance.

### 3.2 Exception Taxonomy

```
RuntimeException
├── EntityNotFoundException                    (utilities module)
│   └── constructor(String entityType, String entityId) → HTTP 404
├── EntityDeletionNotAllowedException          (utilities module)
│   └── constructor(String entityType, String entityId, Throwable cause) → HTTP 409
└── EntityConstraintViolationException         (utilities module — future)
    └── constructor(String entityType, String constraintName, Throwable cause) → HTTP 409
```

### 3.3 Delete UseCase Template — Option A (Recommended)

```java
public final class DeleteUseCaseSupport {
    public static <T, ID> void executeDelete(
            TenantScopedRepository<T, ID> repository,
            ID compositeId, String entityType, String entityId) {
        repository.findById(compositeId)
            .ifPresentOrElse(
                entity -> {
                    try { repository.delete(entity); }
                    catch (DataIntegrityViolationException ex) {
                        throw new EntityDeletionNotAllowedException(entityType, entityId, ex);
                    }
                },
                () -> { throw new EntityNotFoundException(entityType, entityId); }
            );
    }
}
```

### 3.4 Special Cases

- **Tutor Deletion:** Pre-delete check for active MinorStudents (business rule from OpenAPI spec).
- **Tenant Deletion:** Cascading soft-delete — separate ADR, out of scope.
- **Auth Entity Cascading:** `orphanRemoval = true` chain needs integration test verification.

---

## 4. Implementation Plan

### Phase 0: Fix @SQLDelete Bug (URGENT)
**Scope:** `multi-tenant-data` module — all 29 entities | **Effort:** ~2h

1. Audit every `@SQLDelete` to include all composite key columns.
2. Parameter order matches `@Id` field declaration order.
3. Reflective validation test scanning all `@Entity` classes.

### Phase 1: Shared Exception Infrastructure
**Scope:** `utilities` module | **Effort:** ~3h

1. `EntityNotFoundException` + `EntityDeletionNotAllowedException`
2. `DeleteUseCaseSupport` utility class
3. `requireTenantId()` on `TenantContextHolder`
4. Generic `MessageService` methods
5. Unit tests

### Phase 2: Refactor user-management
**Scope:** 3 existing + 2 new | **Effort:** ~4h

1. Refactor existing 3 to use `DeleteUseCaseSupport`
2. Implement `DeleteTutorUseCase` (with MinorStudent check)
3. Implement `DeleteMinorStudentUseCase`
4. Consolidate `PeopleControllerAdvice` (N handlers → 2 generic)
5. HTTP 400 → 409, fix cause chain
6. Unit tests for all 5 + advice

### Phase 3: Billing Module
**Scope:** 6 entities | **Effort:** ~4h

### Phase 4: Course + POS Modules
**Scope:** 5 entities | **Effort:** ~3h

### Phase 5: Tenant + Notification Modules
**Scope:** 3 entities | **Effort:** ~2h

### Phase 6: Integration Tests
**Scope:** Testcontainers + MariaDB | **Effort:** ~6h

1. `@SQLDelete` single-row verification
2. `@SQLRestriction` exclusion verification
3. `orphanRemoval` cascade verification
4. FK constraint → 409 end-to-end
5. Tutor business rule verification

### Phase 7: Cleanup & Documentation
**Effort:** ~2h

**Total estimated effort: ~26h**
