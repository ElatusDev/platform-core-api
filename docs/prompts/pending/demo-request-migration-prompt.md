# Demo Request Feature Migration — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/demo-request-migration-workflow.md`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 1 → 2 → 3 → 4 → 5).
2. Do NOT skip ahead. Each phase must compile before the next begins.
3. After EACH phase, run the specified verification command. Fix failures before proceeding.
4. All new files MUST include the ElatusDev copyright header.
5. All `public` classes and methods MUST have Javadoc.
6. ALL string literals → `public static final` constants, shared between impl and tests.
7. Commit after each phase using the exact commit message provided.

---

## Phase 1: Module Scaffolding + Database Schema

### Read first

```bash
cat pom.xml | head -80                          # Root POM structure
cat db_init/00-schema-qa.sql | tail -30         # End of schema file
cat tenant-management/pom.xml                   # Reference module POM
```

### Step 1.1: Create `lead-management/pom.xml`

**Create file:** `lead-management/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright (c) 2025 ElatusDev
  ~ All rights reserved.
  ~
  ~ This code is proprietary and confidential.
  ~ Unauthorized copying, distribution, or modification is strictly prohibited.
  -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.akademiaplus</groupId>
        <artifactId>platform-core-api</artifactId>
        <version>1.0</version>
    </parent>

    <artifactId>lead-management</artifactId>
    <name>lead-management</name>
    <description>Lead management module — demo requests and prospect capture</description>

    <dependencies>
        <!-- Internal modules -->
        <dependency>
            <groupId>com.akademiaplus</groupId>
            <artifactId>multi-tenant-data</artifactId>
            <version>1.0</version>
        </dependency>
        <dependency>
            <groupId>com.akademiaplus</groupId>
            <artifactId>utilities</artifactId>
            <version>1.0</version>
        </dependency>

        <!-- Spring -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- ModelMapper -->
        <dependency>
            <groupId>org.modelmapper</groupId>
            <artifactId>modelmapper</artifactId>
        </dependency>

        <!-- OpenAPI generated code dependencies -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- OpenAPI code generation -->
            <plugin>
                <groupId>org.openapitools</groupId>
                <artifactId>openapi-generator-maven-plugin</artifactId>
                <version>${openapi-generator.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <inputSpec>${project.basedir}/src/main/resources/openapi/lead-management-module.yaml</inputSpec>
                            <generatorName>spring</generatorName>
                            <library>spring-boot</library>
                            <apiPackage>openapi.akademiaplus.domain.leadmanagement.api</apiPackage>
                            <modelPackage>openapi.akademiaplus.domain.leadmanagement.dto</modelPackage>
                            <generateApiTests>false</generateApiTests>
                            <generateModelTests>false</generateModelTests>
                            <configOptions>
                                <interfaceOnly>true</interfaceOnly>
                                <useTags>true</useTags>
                                <openApiNullable>false</openApiNullable>
                                <useSpringBoot3>true</useSpringBoot3>
                                <modelNameSuffix>DTO</modelNameSuffix>
                                <skipDefaultInterface>true</skipDefaultInterface>
                            </configOptions>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- Delete generated test files -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-antrun-plugin</artifactId>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals><goal>run</goal></goals>
                        <configuration>
                            <target>
                                <delete dir="${project.build.directory}/generated-sources/openapi/src/test"/>
                            </target>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

**IMPORTANT**: Before writing this file, read an existing domain module's `pom.xml`
(e.g., `tenant-management/pom.xml`) to verify the exact plugin configuration, version
properties, and any additional dependencies or plugin settings that may have been added
since this prompt was authored. Match the existing pattern exactly.

### Step 1.2: Register module in root `pom.xml`

**Edit file:** `pom.xml` (root)

Add to the `<modules>` section:
```xml
<module>lead-management</module>
```

Add to the `platform-core-api` profile's `<modules>` section (if it uses an explicit module list)
or verify it is NOT in any profile's exclusion list.

### Step 1.3: Add `demo_requests` table to schema

**Edit file:** `db_init/00-schema-qa.sql`

Add at the end, before any `INSERT` statements:

```sql
-- ============================================================
-- LEAD MANAGEMENT
-- ============================================================

CREATE TABLE demo_requests (
    demo_request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    company_name    VARCHAR(200)  NOT NULL,
    message         TEXT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP     NULL,
    INDEX idx_demo_request_email (email, deleted_at),
    INDEX idx_demo_request_status (status, deleted_at)
);
```

### Step 1.4: Create directory structure

```bash
mkdir -p lead-management/src/main/java/com/akademiaplus/demorequest/interfaceadapters
mkdir -p lead-management/src/main/java/com/akademiaplus/demorequest/usecases
mkdir -p lead-management/src/main/java/com/akademiaplus/config
mkdir -p lead-management/src/main/resources/openapi
mkdir -p lead-management/src/test/java/com/akademiaplus/demorequest/usecases
mkdir -p lead-management/src/test/java/com/akademiaplus/demorequest/interfaceadapters
```

### Verify Phase 1

```bash
mvn validate -pl lead-management
```

The module must be recognized by the reactor. Fix any POM issues before proceeding.

### Commit Phase 1

```bash
git add -A
git commit -m "chore(lead-management): scaffold module and demo_requests schema

Add lead-management Maven module for public-facing lead capture.
Create demo_requests table (platform-level, non-tenant-scoped)
with auto-increment Long PK and soft-delete support."
```

---

## Phase 2: Entity (multi-tenant-data module)

### Read first

```bash
cat multi-tenant-data/src/main/java/com/akademiaplus/tenancy/TenantDataModel.java
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/model/SoftDeletable.java
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/model/Auditable.java
```

Verify the exact class signatures, annotations, and inheritance chain.

### Step 2.1: Create `DemoRequestDataModel.java`

**Create file:** `multi-tenant-data/src/main/java/com/akademiaplus/leadmanagement/DemoRequestDataModel.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement;

import com.akademiaplus.infra.persistence.model.SoftDeletable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Persistence model for demo requests submitted by prospective customers.
 * <p>
 * This is a <strong>platform-level</strong> entity — it is NOT tenant-scoped.
 * Prospects submitting demo requests are not yet tenants, so there is no
 * {@code tenantId} or composite key. Uses auto-increment {@code Long} PK.
 * <p>
 * Supports soft delete via {@link SoftDeletable} inheritance.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Entity
@Table(name = "demo_requests")
@SQLDelete(sql = "UPDATE demo_requests SET deleted_at = CURRENT_TIMESTAMP WHERE demo_request_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DemoRequestDataModel extends SoftDeletable {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "demo_request_id")
    private Long demoRequestId;

    /** Prospect's first name. */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** Prospect's last name. */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Prospect's email address — must be unique across all demo requests. */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Name of the prospect's organization or company. */
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    /** Optional free-text message from the prospect. */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * Current status of the demo request.
     * <p>
     * Valid values: {@code PENDING}, {@code CONTACTED}, {@code SCHEDULED},
     * {@code COMPLETED}, {@code REJECTED}.
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
```

**IMPORTANT**: After writing, verify that:
- `SoftDeletable` provides `deletedAt` and inherits `Auditable` (`createdAt`, `updatedAt`)
- The `@SQLRestriction` annotation exists in the project's Hibernate version (7.2.5). If the
  project uses `@Filter` + `@FilterDef` instead, match that pattern. Check `TenantDataModel`
  for the exact soft-delete annotation used.

### Verify Phase 2

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests
```

### Commit Phase 2

```bash
git add -A
git commit -m "feat(multi-tenant-data): add DemoRequestDataModel

Platform-level entity for demo request lead capture.
Extends SoftDeletable with auto-increment Long PK.
Not tenant-scoped — prospects are pre-tenant."
```

---

## Phase 3: OpenAPI Contract

### Read first

```bash
cat user-management/src/main/resources/openapi/employee-api.yaml
cat user-management/src/main/resources/openapi/user-management-module.yaml
```

Understand the exact schema composition pattern, path references, and $ref syntax.

### Step 3.1: Create `demo-request.yaml`

**Create file:** `lead-management/src/main/resources/openapi/demo-request.yaml`

```yaml
openapi: 3.0.0
info:
  title: Demo Request API
  version: '1.0.0'

components:
  schemas:
    BaseDemoRequest:
      type: object
      properties:
        firstName:
          type: string
          maxLength: 100
        lastName:
          type: string
          maxLength: 100
        email:
          type: string
          format: email
          maxLength: 255
        companyName:
          type: string
          maxLength: 200
        message:
          type: string
      required:
        - firstName
        - lastName
        - email
        - companyName

    DemoRequestCreationRequest:
      allOf:
        - $ref: '#/components/schemas/BaseDemoRequest'

    DemoRequestCreationResponse:
      type: object
      properties:
        demoRequestId:
          type: integer
          format: int64
          readOnly: true
      required:
        - demoRequestId

    GetDemoRequestResponse:
      allOf:
        - $ref: '#/components/schemas/BaseDemoRequest'
        - type: object
          properties:
            demoRequestId:
              type: integer
              format: int64
              readOnly: true
            status:
              type: string
              maxLength: 20
            createdAt:
              type: string
              format: date-time
          required:
            - demoRequestId
            - status

    GetAllDemoRequests200Response:
      type: object
      properties:
        demoRequests:
          type: array
          items:
            $ref: '#/components/schemas/GetDemoRequestResponse'

    ErrorResponse:
      type: object

paths:
  /demo-requests:
    post:
      operationId: createDemoRequest
      summary: Submit a new demo request
      description: >
        Public endpoint — no authentication required.
        Captures a demo request from a prospective customer.
      tags:
        - Demo Requests
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DemoRequestCreationRequest'
      responses:
        '201':
          description: Demo request created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DemoRequestCreationResponse'
        '400':
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '409':
          description: Email already submitted
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    get:
      operationId: getAllDemoRequests
      summary: List all demo requests
      description: >
        Requires authentication. Returns all active demo requests.
      tags:
        - Demo Requests
      responses:
        '200':
          description: List of demo requests
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GetAllDemoRequests200Response'

  /demo-requests/{demoRequestId}:
    get:
      operationId: getDemoRequestById
      summary: Get a demo request by ID
      tags:
        - Demo Requests
      parameters:
        - name: demoRequestId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Demo request found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/GetDemoRequestResponse'
        '404':
          description: Demo request not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    delete:
      operationId: deleteDemoRequest
      summary: Soft-delete a demo request
      tags:
        - Demo Requests
      parameters:
        - name: demoRequestId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '204':
          description: Demo request deleted
        '404':
          description: Demo request not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

### Step 3.2: Create `lead-management-module.yaml`

**Create file:** `lead-management/src/main/resources/openapi/lead-management-module.yaml`

```yaml
openapi: 3.0.0
info:
  title: Lead Management Module
  version: '1.0.0'
  description: Public-facing lead capture and demo request management

components:
  schemas:
    DemoRequestCreationRequest:
      $ref: './demo-request.yaml#/components/schemas/DemoRequestCreationRequest'
    DemoRequestCreationResponse:
      $ref: './demo-request.yaml#/components/schemas/DemoRequestCreationResponse'
    GetDemoRequestResponse:
      $ref: './demo-request.yaml#/components/schemas/GetDemoRequestResponse'
    GetAllDemoRequests200Response:
      $ref: './demo-request.yaml#/components/schemas/GetAllDemoRequests200Response'
    ErrorResponse:
      $ref: './demo-request.yaml#/components/schemas/ErrorResponse'

paths:
  /demo-requests:
    $ref: './demo-request.yaml#/paths/~1demo-requests'
  /demo-requests/{demoRequestId}:
    $ref: './demo-request.yaml#/paths/~1demo-requests~1{demoRequestId}'
```

### Step 3.3: Generate DTOs

```bash
mvn clean generate-sources -pl lead-management -am -DskipTests
```

Verify generated classes exist:

```bash
find lead-management/target/generated-sources -name "*DTO.java" -o -name "*Api.java" | sort
```

Expected:
- `DemoRequestCreationRequestDTO.java`
- `DemoRequestCreationResponseDTO.java`
- `GetDemoRequestResponseDTO.java`
- `GetAllDemoRequests200ResponseDTO.java`
- `DemoRequestsApi.java`

If the generated API interface name differs (e.g., `DemoRequestApi` vs `DemoRequestsApi`),
note it — the controller must implement the exact generated interface.

### Verify Phase 3

```bash
mvn clean compile -pl lead-management -am -DskipTests
```

### Commit Phase 3

```bash
git add -A
git commit -m "feat(lead-management): add OpenAPI contract for demo requests

Define CRUD endpoints for demo request lead capture:
POST (public), GET by ID, GET all, DELETE (authenticated).
Generate DTOs and API interface via openapi-generator."
```

---

## Phase 4: Business Logic + Controller

### Read first

```bash
# Reference use case patterns
cat tenant-management/src/main/java/com/akademiaplus/tenant/usecases/TenantCreationUseCase.java
cat tenant-management/src/main/java/com/akademiaplus/tenant/usecases/GetTenantByIdUseCase.java
cat tenant-management/src/main/java/com/akademiaplus/tenant/usecases/GetAllTenantsUseCase.java
cat tenant-management/src/main/java/com/akademiaplus/tenant/usecases/DeleteTenantUseCase.java

# Reference controller
cat tenant-management/src/main/java/com/akademiaplus/tenant/interfaceadapters/TenantController.java

# Reference security config
cat tenant-management/src/main/java/com/akademiaplus/config/TenantModuleSecurityConfiguration.java

# Reference controller advice
cat tenant-management/src/main/java/com/akademiaplus/config/TenantControllerAdvice.java

# Reference ModelMapper config
cat tenant-management/src/main/java/com/akademiaplus/config/TenantModelMapperConfiguration.java

# Check exception classes available in utilities
find utilities/src/main/java -name "*Exception*"
```

Read ALL of these files carefully before writing any code. Match patterns exactly.

### Step 4.1: `DemoRequestRepository`

**Create file:** `lead-management/src/main/java/com/akademiaplus/demorequest/interfaceadapters/DemoRequestRepository.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.demorequest.interfaceadapters;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link DemoRequestDataModel}.
 * <p>
 * Uses standard {@link JpaRepository} with {@code Long} PK — NOT
 * {@code TenantScopedRepository} because demo requests are platform-level.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface DemoRequestRepository extends JpaRepository<DemoRequestDataModel, Long> {

    /**
     * Checks if a demo request with the given email already exists.
     *
     * @param email the email to check
     * @return {@code true} if a demo request with this email exists
     */
    boolean existsByEmail(String email);
}
```

### Step 4.2: `DemoRequestCreationUseCase`

**Create file:** `lead-management/src/main/java/com/akademiaplus/demorequest/usecases/DemoRequestCreationUseCase.java`

Implementation requirements:

1. **Constants**:
   - `public static final String MAP_NAME = "demoRequestMap"`
   - `public static final String DEFAULT_STATUS = "PENDING"`
   - `public static final String ERROR_EMAIL_ALREADY_SUBMITTED = "A demo request with this email has already been submitted"`

2. **Constructor dependencies**:
   - `ApplicationContext applicationContext`
   - `DemoRequestRepository repository`
   - `ModelMapper modelMapper`

3. **Method**: `public DemoRequestCreationResponseDTO create(DemoRequestCreationRequestDTO dto)`
   - Check `repository.existsByEmail(dto.getEmail())` → throw `DuplicateEntityException` if true
   - Call `transform(dto)` → `repository.save(model)` → `modelMapper.map(saved, ResponseDTO.class)`
   - Annotate with `@Transactional`

4. **Method**: `public DemoRequestDataModel transform(DemoRequestCreationRequestDTO dto)`
   - `applicationContext.getBean(DemoRequestDataModel.class)` — prototype bean
   - `modelMapper.map(dto, model, MAP_NAME)` — named TypeMap
   - `model.setStatus(DEFAULT_STATUS)` — set initial status
   - return model

**IMPORTANT**: Check if `DuplicateEntityException` exists in the `utilities` module. If not,
check what exception class is used for 409 CONFLICT scenarios in the existing codebase. Use
that exact class. If no such exception exists, create one in the `lead-management` exception
package following the project's exception pattern.

### Step 4.3: `GetDemoRequestByIdUseCase`

**Create file:** `lead-management/src/main/java/com/akademiaplus/demorequest/usecases/GetDemoRequestByIdUseCase.java`

Implementation:
- **Constant**: `public static final String ERROR_NOT_FOUND = "Demo request not found with id: "`
- **Constructor deps**: `DemoRequestRepository`, `ModelMapper`
- **Method**: `public GetDemoRequestResponseDTO getById(Long id)`
  - `repository.findById(id).orElseThrow(() -> new EntityNotFoundException(ERROR_NOT_FOUND + id))`
  - `modelMapper.map(entity, GetDemoRequestResponseDTO.class)`
  - Annotate with `@Transactional(readOnly = true)`

**IMPORTANT**: Check what exception class is used for 404 NOT_FOUND. Look in `utilities/`
for an existing `EntityNotFoundException` or similar. Use that exact class.

### Step 4.4: `GetAllDemoRequestsUseCase`

**Create file:** `lead-management/src/main/java/com/akademiaplus/demorequest/usecases/GetAllDemoRequestsUseCase.java`

Implementation:
- **Constructor deps**: `DemoRequestRepository`, `ModelMapper`
- **Method**: `public GetAllDemoRequests200ResponseDTO getAll()`
  - `repository.findAll()` → map each to `GetDemoRequestResponseDTO` → wrap in response DTO
  - Annotate with `@Transactional(readOnly = true)`

Check the generated `GetAllDemoRequests200ResponseDTO` — verify how to set the list
(setter name, constructor, etc.).

### Step 4.5: `DeleteDemoRequestUseCase`

**Create file:** `lead-management/src/main/java/com/akademiaplus/demorequest/usecases/DeleteDemoRequestUseCase.java`

Implementation:
- **Constant**: `public static final String ERROR_NOT_FOUND = "Demo request not found with id: "`
  (or import from `GetDemoRequestByIdUseCase` if they share the constant — but per convention,
  each use case owns its own constants)
- **Constructor deps**: `DemoRequestRepository`
- **Method**: `public void delete(Long id)`
  - `findById(id).orElseThrow(...)` → `repository.delete(entity)`
  - The `@SQLDelete` annotation on the entity handles the soft delete
  - Annotate with `@Transactional`

### Step 4.6: `LeadManagementModelMapperConfiguration`

**Create file:** `lead-management/src/main/java/com/akademiaplus/config/LeadManagementModelMapperConfiguration.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.demorequest.usecases.DemoRequestCreationUseCase;
import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers lead-management TypeMaps for DTO → DataModel conversions.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
public class LeadManagementModelMapperConfiguration {

    private final ModelMapper modelMapper;

    /**
     * Registers all named TypeMaps for the lead-management module.
     */
    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);
        registerDemoRequestMap();
        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void registerDemoRequestMap() {
        // Look up the exact DTO class name from generated sources
        modelMapper.createTypeMap(
                // DemoRequestCreationRequestDTO.class,
                // DemoRequestDataModel.class,
                // DemoRequestCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(DemoRequestDataModel::setDemoRequestId);  // Auto-generated PK
            mapper.skip(DemoRequestDataModel::setStatus);          // Set explicitly in use case
        }).implicitMappings();
    }
}
```

**IMPORTANT**: The DTO class import depends on the generated package. Read the generated
`DemoRequestCreationRequestDTO` to get the correct fully-qualified class name. Fill in the
`createTypeMap()` arguments with the actual classes.

### Step 4.7: `DemoRequestController`

**Create file:** `lead-management/src/main/java/com/akademiaplus/demorequest/interfaceadapters/DemoRequestController.java`

Implementation:
- `@RestController`, `@RequestMapping("/v1/lead-management")`
- Implements the generated API interface (e.g., `DemoRequestsApi`)
- Constructor-inject all four use cases
- Thin delegation — no business logic:
  - `createDemoRequest(dto)` → `ResponseEntity.status(201).body(creationUseCase.create(dto))`
  - `getDemoRequestById(id)` → `ResponseEntity.ok(getByIdUseCase.getById(id))`
  - `getAllDemoRequests()` → `ResponseEntity.ok(getAllUseCase.getAll())`
  - `deleteDemoRequest(id)` → `deleteUseCase.delete(id)` → `ResponseEntity.noContent().build()`

### Step 4.8: `LeadManagementSecurityConfiguration`

**Create file:** `lead-management/src/main/java/com/akademiaplus/config/LeadManagementSecurityConfiguration.java`

Read an existing module's security configuration first (e.g., `PeopleModuleSecurityConfiguration`
in `user-management`) to match the exact pattern for `SecurityFilterChain` bean registration,
`@Order`, CORS configuration, and CSRF handling.

Key requirement: POST `/v1/lead-management/demo-requests` must be `permitAll()`.
All other endpoints under `/v1/lead-management/**` require authentication.

### Step 4.9: `LeadManagementControllerAdvice`

**Create file:** `lead-management/src/main/java/com/akademiaplus/config/LeadManagementControllerAdvice.java`

Read an existing module's controller advice (e.g., `TenantControllerAdvice`) to understand:
- Whether it extends a `BaseControllerAdvice`
- How it maps `EntityNotFoundException` → 404 and `DuplicateEntityException` → 409
- The exact `@ExceptionHandler` method signatures and return types

Match that pattern exactly.

### Step 4.10: Wire into `application/pom.xml`

**Edit file:** `application/pom.xml`

Add:
```xml
<dependency>
    <groupId>com.akademiaplus</groupId>
    <artifactId>lead-management</artifactId>
    <version>1.0</version>
</dependency>
```

### Verify Phase 4

```bash
mvn clean compile -pl lead-management -am -DskipTests
mvn clean compile -pl application -am -DskipTests
```

Both must compile without errors. Fix any issues before proceeding to tests.

### Commit Phase 4

```bash
git add -A
git commit -m "feat(lead-management): implement demo request CRUD use cases

Add DemoRequestCreationUseCase with email uniqueness validation,
GetDemoRequestByIdUseCase, GetAllDemoRequestsUseCase, and
DeleteDemoRequestUseCase. Controller delegates to use cases
following canonical thin-controller pattern.

POST /v1/lead-management/demo-requests is permitAll (public).
GET and DELETE endpoints require authentication."
```

---

## Phase 5: Unit Tests

### Read first

```bash
# Reference test patterns
cat tenant-management/src/test/java/com/akademiaplus/tenant/usecases/TenantCreationUseCaseTest.java
cat tenant-management/src/test/java/com/akademiaplus/tenant/usecases/GetTenantByIdUseCaseTest.java
cat tenant-management/src/test/java/com/akademiaplus/tenant/usecases/DeleteTenantUseCaseTest.java
```

Read ALL of these to understand the exact test conventions.

### Step 5.1: `DemoRequestCreationUseCaseTest`

**Create file:** `lead-management/src/test/java/com/akademiaplus/demorequest/usecases/DemoRequestCreationUseCaseTest.java`

Test coverage:

```
@DisplayName("DemoRequestCreationUseCase")
class DemoRequestCreationUseCaseTest {

    @Nested @DisplayName("create")
    class Create {
        @Test shouldCreateDemoRequest_whenGivenValidRequest()
        @Test shouldThrowDuplicateEntityException_whenEmailAlreadySubmitted()
    }

    @Nested @DisplayName("transform")
    class Transform {
        @Test shouldRetrievePrototypeBean_whenTransforming()
        @Test shouldDelegateToModelMapper_withNamedTypeMap()
        @Test shouldSetStatusToPending_whenTransforming()
    }
}
```

**Critical test rules**:
- Given-When-Then comments (never Arrange-Act-Assert)
- `shouldDoX_whenY()` naming with `@DisplayName`
- ZERO `any()` matchers — stub with exact values
- `doNothing().when(modelMapper).map(dto, model, MAP_NAME)` for void overload
- Use `DemoRequestCreationUseCase.MAP_NAME` constant in verify calls
- Use `DemoRequestCreationUseCase.DEFAULT_STATUS` in assertions
- Use `DemoRequestCreationUseCase.ERROR_EMAIL_ALREADY_SUBMITTED` in exception assertion
- Test constants as `private static final` at class level

### Step 5.2: `GetDemoRequestByIdUseCaseTest`

Test coverage:

```
@DisplayName("GetDemoRequestByIdUseCase")
class GetDemoRequestByIdUseCaseTest {

    @Nested @DisplayName("getById")
    class GetById {
        @Test shouldReturnDemoRequest_whenGivenValidId()
        @Test shouldThrowEntityNotFoundException_whenIdNotFound()
    }
}
```

### Step 5.3: `GetAllDemoRequestsUseCaseTest`

Test coverage:

```
@DisplayName("GetAllDemoRequestsUseCase")
class GetAllDemoRequestsUseCaseTest {

    @Nested @DisplayName("getAll")
    class GetAll {
        @Test shouldReturnAllDemoRequests_whenRequestsExist()
        @Test shouldReturnEmptyList_whenNoRequestsExist()
    }
}
```

### Step 5.4: `DeleteDemoRequestUseCaseTest`

Test coverage:

```
@DisplayName("DeleteDemoRequestUseCase")
class DeleteDemoRequestUseCaseTest {

    @Nested @DisplayName("delete")
    class Delete {
        @Test shouldDeleteDemoRequest_whenGivenValidId()
        @Test shouldThrowEntityNotFoundException_whenIdNotFound()
    }
}
```

### Step 5.5: `DemoRequestControllerTest`

Test coverage:

```
@DisplayName("DemoRequestController")
@WebMvcTest(DemoRequestController.class)
class DemoRequestControllerTest {

    @Nested @DisplayName("POST /v1/lead-management/demo-requests")
    class CreateDemoRequest {
        @Test shouldReturn201_whenGivenValidRequest()
        @Test shouldReturn409_whenEmailAlreadySubmitted()
    }

    @Nested @DisplayName("GET /v1/lead-management/demo-requests/{id}")
    class GetDemoRequestById {
        @Test shouldReturn200_whenRequestExists()
        @Test shouldReturn404_whenRequestNotFound()
    }

    @Nested @DisplayName("GET /v1/lead-management/demo-requests")
    class GetAllDemoRequests {
        @Test shouldReturn200_whenRequestsExist()
    }

    @Nested @DisplayName("DELETE /v1/lead-management/demo-requests/{id}")
    class DeleteDemoRequest {
        @Test shouldReturn204_whenRequestExists()
        @Test shouldReturn404_whenRequestNotFound()
    }
}
```

**IMPORTANT**: For `@WebMvcTest` tests, check the existing controller test pattern:
- How `@MockBean` / `@MockitoBean` is used for use case injection
- How `@WithMockUser` is applied for authenticated endpoints
- How the POST endpoint is tested WITHOUT `@WithMockUser` (since it's `permitAll`)
- The exact `MockMvc` request builder pattern (`mockMvc.perform(post(...))`)
- The security filter chain setup for tests

### Verify Phase 5

```bash
mvn test -pl lead-management -am
```

ALL tests must pass. Fix failures before committing.

Common issues:
- `PotentialStubbingProblem`: Missing `doNothing().when()` for void ModelMapper overload
- `SecurityContext` issues in controller tests: need proper security config for tests
- Generated DTO constructor/setter mismatches: read the generated class signatures

### Commit Phase 5

```bash
git add -A
git commit -m "test(lead-management): add unit tests for demo request use cases

Cover creation (happy path + duplicate email), getById (found + not found),
getAll (with data + empty), delete (found + not found), and controller
endpoints. All tests follow Given-When-Then, zero any() matchers."
```

---

## Final Verification

```bash
# Full project build with all tests
mvn clean install

# Convention compliance
grep -rn "any()" lead-management/src/test/ && echo "FAIL" || echo "PASS: no any() matchers"
grep -rL "Copyright" lead-management/src/main/java/com/akademiaplus/**/*.java 2>/dev/null && echo "FAIL" || echo "PASS: copyright headers"
grep -rn '".*not found.*"' lead-management/src/main/java/ | grep -v "static final" && echo "FAIL" || echo "PASS: no inline strings"
```

---

## Post-Migration Checklist

- [ ] `mvn clean install` passes (all modules)
- [ ] `mvn test -pl lead-management -am` passes (all tests)
- [ ] POST `/v1/lead-management/demo-requests` is public (no auth required)
- [ ] GET endpoints require authentication
- [ ] Email uniqueness returns 409 CONFLICT
- [ ] Missing ID returns 404 NOT_FOUND
- [ ] DELETE triggers soft delete (sets `deleted_at`, does not physically remove)
- [ ] All constants extracted — no inline string literals
- [ ] All public classes have Javadoc with `@param`, `@return`, `@throws`
- [ ] Copyright header on all files
- [ ] Tests use Given-When-Then with `@Nested` + `@DisplayName`
- [ ] Zero `any()` matchers in test code
- [ ] Commits follow Conventional Commits format
