# Component Test Workflow — One Per Entity Strategy

> Canonical reference for writing, organizing, and maintaining component tests
> across all domain modules in the AkademiaPlus Platform Core API.

## Table of Contents

1. [Strategy Overview](#1-strategy-overview)
2. [Architecture](#2-architecture)
3. [Entity Exception Matrix](#3-entity-exception-matrix)
4. [File Naming and Organization](#4-file-naming-and-organization)
5. [Infrastructure Setup Checklist](#5-infrastructure-setup-checklist)
6. [Test Structure Template](#6-test-structure-template)
7. [Patterns and Best Practices](#7-patterns-and-best-practices)
8. [Execution Order](#8-execution-order)

---

## 1. Strategy Overview

**One component test class per entity.** Each file covers all CRUD operations
and every exception path for a single entity, keeping tests focused, maintainable,
and independently runnable.

### Why One Per Entity

| Concern | One-per-entity | One-per-module |
|---------|---------------|----------------|
| File size | ~200-400 lines | ~1000+ lines |
| Failure locality | Immediately clear which entity broke | Must scan entire file |
| Parallel execution | Failsafe can parallelize entity tests | Single serial file |
| Maintainability | Add/remove entity without touching siblings | Every entity change touches the same file |
| Code review | Small, focused diffs | Large, noisy diffs |

### What Component Tests Verify

Component tests boot the **full Spring context** against a **Testcontainers MariaDB**
instance. They exercise the complete stack from controller to database:

```
HTTP Request → Controller → Use Case → Repository → MariaDB (Testcontainers)
     ↑                                                       |
     └───────────── HTTP Response ←───────────────────────────┘
```

They verify:
- REST endpoint returns correct HTTP status codes
- Response body structure matches OpenAPI contract
- Error codes from `BaseControllerAdvice` are correct
- Entity persistence and soft-delete behavior
- Multi-tenancy isolation (composite keys, tenant context)
- Business rule enforcement (e.g., tutor deletion with active minor students)

---

## 2. Architecture

### Inheritance Chain

```
AbstractIntegrationTest (infra-common test-jar)
  └── AbstractIntegrationTest (module-local, extends infra-common version)
      └── EntityComponentTest (one per entity)
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `AbstractIntegrationTest` | `infra-common/src/test/java/.../infra/testing/` | Testcontainers MariaDB, dynamic properties |
| Module `AbstractIntegrationTest` | `{module}/src/test/java/.../config/` | `@SpringBootTest` with module TestApp |
| `{Module}TestApp` | `{module}/src/test/java/com/akademiaplus/` | Minimal `@SpringBootApplication`, excludes `TenantContextHolder` |
| `00-schema-dev.sql` | `infra-common/src/test/resources/` | Full database schema for Testcontainers init |
| `BaseControllerAdvice` | `utilities/.../web/` | Centralized exception → HTTP mapping |

### Module `AbstractIntegrationTest` Pattern

Each module has its own `AbstractIntegrationTest` in `src/test/java/.../config/`
that adds `@SpringBootTest`:

```java
@SpringBootTest(classes = UserManagementTestApp.class)
public abstract class AbstractIntegrationTest
        extends com.akademiaplus.infra.testing.AbstractIntegrationTest {
}
```

### TestApp Pattern

```java
@SpringBootApplication
@ComponentScan(
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = TenantContextHolder.class
    )
)
public class BillingTestApp {
}
```

The `TenantContextHolder` exclusion prevents `@RequestScope` CGLIB proxy issues
in the test context. Tests inject `TenantContextHolder` directly and call
`setTenantId()` manually.

---

## 3. Entity Exception Matrix

Every component test must cover **all exception paths** for each operation.
The matrix below defines what each entity's test class must verify.

### HTTP Status Code Reference

| Exception | HTTP Status | Error Code | When |
|-----------|------------|------------|------|
| `EntityNotFoundException` | 404 | `ENTITY_NOT_FOUND` | getById / delete with non-existent ID |
| `EntityDeletionNotAllowedException` (constraint) | 409 | `DELETION_CONSTRAINT_VIOLATION` | Delete blocked by FK constraint |
| `EntityDeletionNotAllowedException` (business) | 409 | `DELETION_BUSINESS_RULE` | Delete blocked by business rule |
| `DuplicateEntityException` | 409 | `DUPLICATE_ENTITY` | Create with duplicate email/phone |
| `InvalidTenantException` | 400 | `INVALID_TENANT` | Missing tenant context |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` | Bean validation failure |
| `DataIntegrityViolationException` | 409 | `DATA_INTEGRITY_VIOLATION` | Unclassified DB constraint |

### Per-Entity Coverage Requirements

#### user-management (5 entities)

| Entity | Create | GetById | GetAll | Delete | Special |
|--------|--------|---------|--------|--------|---------|
| **Employee** | 201, 409 dup email, 409 dup phone | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | PII merge in getById |
| **Collaborator** | 201, 409 dup email, 409 dup phone | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | PII merge in getById |
| **AdultStudent** | 201, 409 dup email, 409 dup phone | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | PII merge in getById |
| **Tutor** | 201, 409 dup email, 409 dup phone | 200, 404 | 200, 200 empty | 204, 404, 409 business rule | Has MinorStudent business rule |
| **MinorStudent** | 201 (via TutorCreationUseCase) | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | Created through tutor endpoint |

#### billing (6 entities)

| Entity | Create | GetById | GetAll | Delete |
|--------|--------|---------|--------|--------|
| **Membership** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |
| **Compensation** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |
| **PaymentAdultStudent** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |
| **PaymentTutor** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |
| **MembershipAdultStudent** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |
| **MembershipTutor** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |

#### course-management (3 entities)

| Entity | Create | GetById | GetAll | Delete | Special |
|--------|--------|---------|--------|--------|---------|
| **Course** | 201, 400 validation | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | CourseValidator custom validation |
| **Schedule** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | — |
| **CourseEvent** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | — |

#### tenant-management (3 entities)

| Entity | Create | GetById | GetAll | Delete | Special |
|--------|--------|---------|--------|--------|---------|
| **Tenant** | 201 | 200, 404 | 200 | 204, 404, 409 constraint | No composite key (simple Long ID) |
| **TenantSubscription** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | Composite key with TenantContextHolder |
| **TenantBillingCycle** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint | Composite key with TenantContextHolder |

#### notification-system (1 entity)

| Entity | Create | GetById | GetAll | Delete |
|--------|--------|---------|--------|--------|
| **Notification** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |

#### pos-system (2 entities)

| Entity | Create | GetById | GetAll | Delete |
|--------|--------|---------|--------|--------|
| **StoreProduct** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |
| **StoreTransaction** | 201 | 200, 404 | 200, 200 empty | 204, 404, 409 constraint |

---

## 4. File Naming and Organization

### Naming Convention

```
{EntityName}ComponentTest.java
```

Examples:
- `EmployeeComponentTest.java`
- `TenantComponentTest.java`
- `MembershipAdultStudentComponentTest.java`

### Directory Structure

```
{module}/src/test/java/com/akademiaplus/
├── {Module}TestApp.java                    ← Minimal @SpringBootApplication
├── config/
│   └── AbstractIntegrationTest.java        ← Extends infra-common base, adds @SpringBootTest
└── usecases/
    ├── {Entity1}ComponentTest.java
    ├── {Entity2}ComponentTest.java
    └── ...
```

### Existing Tests to Refactor

The following existing component tests should be refactored into per-entity files:

| Current File | Refactor Into |
|-------------|---------------|
| `user-management/.../SoftDeleteComponentTest.java` | `EmployeeComponentTest.java` + `TutorComponentTest.java` (merge delete scenarios into full CRUD tests) |
| `tenant-management/.../TenantCreationComponentTest.java` | `TenantComponentTest.java` (merge creation into full CRUD test) |
| `course-management/.../SoftDeleteComponentTest.java` | `CourseComponentTest.java` (merge soft-delete into full CRUD test) |

---

## 5. Infrastructure Setup Checklist

Three modules currently **lack** integration test infrastructure and need setup
before component tests can be written:

- **billing**
- **notification-system**
- **pos-system**

### Per-Module Setup Steps

For each module missing infrastructure, create:

#### 1. pom.xml Dependencies

Add to the module's `pom.xml`:

```xml
<!-- Test dependencies -->
<dependency>
    <groupId>com.akademiaplus</groupId>
    <artifactId>infra-common</artifactId>
    <version>${project.version}</version>
    <type>test-jar</type>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mariadb</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

#### 2. Failsafe Plugin

Add the `maven-failsafe-plugin` to run `*ComponentTest.java` files:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*ComponentTest.java</include>
        </includes>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 3. TestApp Class

Create `{Module}TestApp.java` in `src/test/java/com/akademiaplus/`:

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Test-only application class for {module} integration tests.
 *
 * @author ElatusDev
 * @since 1.0
 */
@SpringBootApplication
@ComponentScan(
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = TenantContextHolder.class
    )
)
public class {Module}TestApp {
}
```

#### 4. Module AbstractIntegrationTest

Create in `src/test/java/com/akademiaplus/config/`:

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.{Module}TestApp;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Module-specific integration test base for {module}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@SpringBootTest(classes = {Module}TestApp.class)
public abstract class AbstractIntegrationTest
        extends com.akademiaplus.infra.testing.AbstractIntegrationTest {
}
```

---

## 6. Test Structure Template

### Standard Entity Component Test (tenant-scoped, composite key)

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for {@code {Entity}} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints and exception paths for the {Entity} entity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("{Entity} — Component Test")
class {Entity}ComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/{module}/{entities}";

    // ── Tenant setup ──────────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Test Tenant";
    private static final String TENANT_EMAIL = "tenant@test.com";
    private static final String TENANT_ADDRESS = "123 Test St";

    private static final String[] ENTITY_TABLE_NAMES = {
            "{entity_table}", "person_piis" /* if PII entity */
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long entityId;

    @BeforeEach
    void setUpTestDataOnce() throws Exception {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        tenantContextHolder.setTenantId(tenantId);
        dataCreated = true;
    }

    // ── Create ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Create")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Create {

        @Test
        @Order(1)
        @DisplayName("Should return 201 when given valid request")
        void shouldReturn201_whenGivenValidRequest() throws Exception {
            // Given
            tenantContextHolder.setTenantId(tenantId);
            String body = """
                    { ... }
                    """;

            // When
            MvcResult result = mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Then
            entityId = ((Number) com.jayway.jsonpath.JsonPath
                    .read(result.getResponse().getContentAsString(), "$.entityId"))
                    .longValue();
            assertThat(entityId).isPositive();
        }

        @Test
        @Order(2)
        @DisplayName("Should return 409 when duplicate email")
        void shouldReturn409_whenDuplicateEmail() throws Exception {
            // Given — same entity from Order(1) already exists
            tenantContextHolder.setTenantId(tenantId);
            String body = """
                    { ... same email ... }
                    """;

            // When / Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code")
                            .value(BaseControllerAdvice.CODE_DUPLICATE_ENTITY));
        }
    }

    // ── GetById ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("GetById")
    class GetById {

        @Test
        @DisplayName("Should return 200 with entity details when found")
        void shouldReturn200_whenEntityFound() throws Exception {
            // Given
            tenantContextHolder.setTenantId(tenantId);

            // When / Then
            mockMvc.perform(get(BASE_PATH + "/{id}", entityId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entityId").value(entityId));
        }

        @Test
        @DisplayName("Should return 404 when entity not found")
        void shouldReturn404_whenEntityNotFound() throws Exception {
            // Given
            tenantContextHolder.setTenantId(tenantId);

            // When / Then
            mockMvc.perform(get(BASE_PATH + "/{id}", 999999L)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code")
                            .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
        }
    }

    // ── GetAll ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GetAll")
    class GetAll {

        @Test
        @DisplayName("Should return 200 with list when entities exist")
        void shouldReturn200WithList_whenEntitiesExist() throws Exception {
            // Given
            tenantContextHolder.setTenantId(tenantId);

            // When / Then
            mockMvc.perform(get(BASE_PATH)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        }
    }

    // ── Delete ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Delete")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Delete {

        @Test
        @Order(1)
        @DisplayName("Should return 404 when deleting non-existent entity")
        void shouldReturn404_whenDeletingNonExistentEntity() throws Exception {
            // Given
            tenantContextHolder.setTenantId(tenantId);

            // When / Then
            mockMvc.perform(delete(BASE_PATH + "/{id}", 999999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code")
                            .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
        }

        @Test
        @Order(2)
        @DisplayName("Should return 204 when deleting existing entity")
        void shouldReturn204_whenDeletingExistingEntity() throws Exception {
            // Given
            tenantContextHolder.setTenantId(tenantId);

            // When
            mockMvc.perform(delete(BASE_PATH + "/{id}", entityId))
                    .andExpect(status().isNoContent());

            // Then — verify soft-deleted via native SQL
            entityManager.clear();
            assertThat(nativeDeletedAt("{entity_table}", "{entity_id_column}", entityId))
                    .as("Entity should have deleted_at set after deletion")
                    .isNotNull();
        }

        @Test
        @Order(3)
        @DisplayName("Should return 404 when requesting soft-deleted entity by ID")
        void shouldReturn404_whenRequestingSoftDeletedEntityById() throws Exception {
            // Given — entity was soft-deleted in Order(2)
            tenantContextHolder.setTenantId(tenantId);

            // When / Then
            mockMvc.perform(get(BASE_PATH + "/{id}", entityId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code")
                            .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
        }
    }

    // ── Data Creation Helpers ─────────────────────────────────────────

    private Long createTenant(TransactionTemplate tx) {
        return tx.execute(status -> {
            TenantDataModel tenant = new TenantDataModel();
            tenant.setOrganizationName(TENANT_ORG_NAME);
            tenant.setEmail(TENANT_EMAIL);
            tenant.setAddress(TENANT_ADDRESS);
            entityManager.persist(tenant);
            entityManager.flush();
            return tenant.getTenantId();
        });
    }

    private void createTenantSequences(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            for (String tableName : ENTITY_TABLE_NAMES) {
                entityManager.createNativeQuery(
                                "INSERT INTO tenant_sequences "
                                        + "(tenant_id, entity_name, next_value, version) "
                                        + "VALUES (:tenantId, :entityName, 1, 0)")
                        .setParameter("tenantId", tenantId)
                        .setParameter("entityName", tableName)
                        .executeUpdate();
            }
        });
    }

    // ── Native SQL Helpers ────────────────────────────────────────────

    private Object nativeDeletedAt(String tableName, String idColumn, Long entityId) {
        return entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM " + tableName
                                + " WHERE tenant_id = :tenantId AND " + idColumn + " = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", entityId)
                .getSingleResult();
    }
}
```

---

## 7. Patterns and Best Practices

### 7.1 Test Data Setup

**Use `@BeforeEach` with static flag guard** — not `@BeforeAll` — so that the
Spring context and Testcontainers are fully initialized before data creation:

```java
private static boolean dataCreated;

@BeforeEach
void setUpTestDataOnce() throws Exception {
    if (dataCreated) {
        return;
    }
    // ... create test data ...
    dataCreated = true;
}
```

### 7.2 Tenant and Sequence Setup

Every component test for tenant-scoped entities must:

1. Create a `TenantDataModel` via `EntityManager.persist()` (AUTO_INCREMENT ID)
2. Insert `tenant_sequences` rows for all entity tables the test will use
3. Set the tenant context via `tenantContextHolder.setTenantId(tenantId)`

```java
private void createTenantSequences(TransactionTemplate tx) {
    tx.executeWithoutResult(status -> {
        for (String tableName : ENTITY_TABLE_NAMES) {
            entityManager.createNativeQuery(
                    "INSERT INTO tenant_sequences "
                    + "(tenant_id, entity_name, next_value, version) "
                    + "VALUES (:tenantId, :entityName, 1, 0)")
                .setParameter("tenantId", tenantId)
                .setParameter("entityName", tableName)
                .executeUpdate();
        }
    });
}
```

### 7.3 Native SQL for Verification

Use native SQL to bypass `@SQLRestriction` when verifying soft-delete state:

```java
private Object nativeDeletedAt(String tableName, String idColumn, Long entityId) {
    return entityManager.createNativeQuery(
            "SELECT deleted_at FROM " + tableName
            + " WHERE tenant_id = :tenantId AND " + idColumn + " = :entityId")
        .setParameter("tenantId", tenantId)
        .setParameter("entityId", entityId)
        .getSingleResult();
}
```

### 7.4 Test Ordering

Use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` with `@Order` at
the class level when tests have dependencies (e.g., create before getById,
delete before verify-soft-deleted):

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Employee — Component Test")
class EmployeeComponentTest extends AbstractIntegrationTest {
    // ...
}
```

Within `@Nested` classes, use `@Order` to sequence dependent tests while keeping
independent groups in separate nested classes.

### 7.5 Error Code Assertions

Always assert on the `$.code` field from `BaseControllerAdvice`, not on the
message (which may be localized):

```java
// Correct — assert on code
.andExpect(jsonPath("$.code").value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));

// Avoid — assert on localized message
.andExpect(jsonPath("$.message").value("Empleado con ID 999 no encontrado"));
```

Exception: when testing business rule messages that include dynamic data
(e.g., active minor student count), use `containsString` with the constant:

```java
.andExpect(jsonPath("$.message", containsString(
        String.format(DeleteTutorUseCase.ACTIVE_MINOR_STUDENTS_REASON, count))));
```

### 7.6 Constants

All test data literals should be `private static final` constants at the top
of the test class:

```java
private static final String EMPLOYEE1_EMAIL = "emp1@test.com";
private static final String EMPLOYEE1_PHONE = "+525512345601";
```

### 7.7 Given-When-Then Comments

Always structure test methods with `// Given`, `// When`, `// Then` comments.
Never use Arrange-Act-Assert terminology:

```java
@Test
void shouldReturn404_whenEntityNotFound() throws Exception {
    // Given
    tenantContextHolder.setTenantId(tenantId);

    // When / Then
    mockMvc.perform(get(BASE_PATH + "/{id}", 999999L)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
}
```

### 7.8 Entity Creation via REST vs Direct

| Method | When to Use |
|--------|-------------|
| **MockMvc POST** | Entity has a REST creation endpoint |
| **Direct use case call** | Entity is created through a parent (e.g., MinorStudent via Tutor) |
| **EntityManager persist** | Infrastructure entities (Tenant, tenant_sequences) |

### 7.9 Extracting JSON Values from Responses

Use jayway JsonPath to extract IDs from creation responses:

```java
MvcResult result = mockMvc.perform(post(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andReturn();

Long id = ((Number) com.jayway.jsonpath.JsonPath
        .read(result.getResponse().getContentAsString(), "$.entityId"))
        .longValue();
```

### 7.10 Clearing EntityManager

Always call `entityManager.clear()` after writes to ensure subsequent reads
hit the database rather than the persistence context cache:

```java
mockMvc.perform(delete(BASE_PATH + "/{id}", entityId))
        .andExpect(status().isNoContent());

entityManager.clear();

assertThat(nativeDeletedAt("employees", "employee_id", entityId))
        .isNotNull();
```

---

## 8. Execution Order

### Recommended Implementation Order

Implement component tests in dependency order — entities that other entities
depend on should be tested first:

| Phase | Module | Entities | Reason |
|-------|--------|----------|--------|
| 1 | tenant-management | Tenant, TenantSubscription, TenantBillingCycle | Root entity, no dependencies |
| 2 | user-management | Employee, Collaborator, AdultStudent, Tutor, MinorStudent | People entities needed by billing/courses |
| 3 | course-management | Course, Schedule, CourseEvent | Depends on Collaborator |
| 4 | billing | Membership, Compensation, Payment*, Membership* | Depends on user entities |
| 5 | notification-system | Notification | Independent |
| 6 | pos-system | StoreProduct, StoreTransaction | Independent |

### Running Component Tests

```bash
# Run all component tests in a specific module
mvn verify -pl tenant-management

# Run a single component test
mvn verify -pl tenant-management -Dit.test=TenantComponentTest

# Run all component tests across all modules
mvn verify
```

### Refactoring Existing Tests

Before writing new per-entity tests, migrate existing component tests:

1. **Copy** the relevant test logic from the old file into the new per-entity file
2. **Verify** the new test passes: `mvn verify -pl {module} -Dit.test={Entity}ComponentTest`
3. **Delete** the old file only after all its scenarios are covered by new per-entity tests
4. **Verify** full module: `mvn verify -pl {module}`
