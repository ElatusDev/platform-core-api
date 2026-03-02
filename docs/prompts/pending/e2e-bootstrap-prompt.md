# E2E Bootstrap — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Prerequisite**: Read `docs/directives/CLAUDE.md`, `docs/directives/AI-CODE-REF.md` before starting.
**Dependency**: None — first prompt for Wave 6 (E2E Automation).
**Spec reference**: `docs/workflows/pending/e2e-bootstrap-workflow.md`

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 1 → 2 → 3 → 4).
2. Do NOT skip ahead. Each phase must compile before the next begins.
3. After EACH phase, run the specified verification command.
4. All new files MUST include the ElatusDev copyright header.
5. All `public` classes and methods MUST have Javadoc.
6. Test conventions: Given-When-Then comments, `shouldDoX_whenY()` naming, `@DisplayName` on every `@Test` and `@Nested`, zero `any()` matchers, all string literals as `public static final` constants.
7. Read the target file with `grep` or `cat` BEFORE modifying it — never write blindly.
8. Commit after each phase using the specified Conventional Commit message.

---

## Phase 1: Registration Endpoint

### Step 1.1: Create OpenAPI spec for registration

**File**: `security/src/main/resources/openapi/registration.yaml`

```yaml
openapi: 3.0.0
info:
  title: Registration API
  version: '1.0.0'
components:
  schemas:
    RegistrationRequest:
      type: object
      properties:
        organizationName:
          type: string
          description: Tenant organization name
        email:
          type: string
          format: email
          description: Admin email address
        address:
          type: string
          description: Tenant address
        firstName:
          type: string
          description: Admin first name
        lastName:
          type: string
          description: Admin last name
        phone:
          type: string
          description: Admin phone number
        zipCode:
          type: string
          description: Tenant zip code
        birthdate:
          type: string
          format: date
          description: Admin birthdate
        username:
          type: string
          description: Admin login username
        password:
          type: string
          format: password
          description: Admin login password
      required:
        - organizationName
        - email
        - address
        - firstName
        - lastName
        - username
        - password
    RegistrationResponse:
      type: object
      properties:
        token:
          type: string
          description: JWT authentication token
        tenantId:
          type: integer
          format: int64
          description: Created tenant ID
      required:
        - token
        - tenantId
    ErrorResponse:
      type: object
paths:
  /register:
    post:
      summary: Register a new tenant with admin user
      operationId: register
      tags:
        - Register
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RegistrationRequest'
      responses:
        '201':
          description: Tenant and admin user created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RegistrationResponse'
        '400':
          description: Invalid request
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '409':
          description: Duplicate organization or username
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

### Step 1.2: Wire registration.yaml into security-module.yaml

**File**: `security/src/main/resources/openapi/security-module.yaml`

Add to `components.schemas`:

```yaml
    RegistrationRequest:
      $ref: './registration.yaml#/components/schemas/RegistrationRequest'
    RegistrationResponse:
      $ref: './registration.yaml#/components/schemas/RegistrationResponse'
```

Add to `paths`:

```yaml
  /register:
    $ref: './registration.yaml#/paths/~1register'
```

### Step 1.3: Regenerate DTOs and verify generated interfaces

```bash
mvn generate-sources -pl security -am -f platform-core-api/pom.xml
```

Verify these are generated (check `target/generated-sources/`):
- `RegistrationRequestDTO`
- `RegistrationResponseDTO`
- `RegisterApi` interface with method `register(RegistrationRequestDTO)`

### Step 1.4: Create RegistrationUseCase in application module

**File**: `application/src/main/java/com/akademiaplus/usecases/RegistrationUseCase.java`

**Package**: `com.akademiaplus.usecases`

**Dependencies** (constructor injection):
- `TenantCreationUseCase tenantCreationUseCase`
- `EmployeeCreationUseCase employeeCreationUseCase`
- `TenantContextHolder tenantContextHolder`
- `JwtTokenProvider jwtTokenProvider`

**Constants**:
```java
public static final String ADMIN_EMPLOYEE_TYPE = "ADMINISTRATOR";
public static final String ADMIN_ROLE = "ADMIN";
```

**Method**: `public RegistrationResponseDTO register(RegistrationRequestDTO request)`
- Annotation: `@Transactional`
- Flow:
  1. Build `TenantCreateRequestDTO` from `request`:
     - `organizationName` → `name`
     - `email` → `email`
     - `address` → `address`
     - `zipCode` → `zipCode`
  2. Call `tenantCreationUseCase.create(tenantDto)` → `TenantDTO` (extract `tenantId`)
  3. **CRITICAL**: Call `tenantContextHolder.setTenantId(tenantId)`
  4. Build `EmployeeCreationRequestDTO` from `request`:
     - `firstName`, `lastName`, `email`, `phone`, `zipCode`, `birthdate` → matching fields
     - `username`, `password` → matching fields
     - `employeeType` → `ADMIN_EMPLOYEE_TYPE`
     - `role` → `ADMIN_ROLE`
  5. Call `employeeCreationUseCase.create(employeeDto)` → `EmployeeCreationResponseDTO`
  6. Build JWT claims map: `Map.of("tenantId", tenantId, "role", ADMIN_ROLE, "username", request.getUsername())`
  7. Call `jwtTokenProvider.createToken(claims)` → `String token`
  8. Build and return `RegistrationResponseDTO`:
     - `token` → token
     - `tenantId` → tenantId

### Step 1.5: Create RegistrationController in application module

**File**: `application/src/main/java/com/akademiaplus/interfaceadapters/controllers/RegistrationController.java`

**Package**: `com.akademiaplus.interfaceadapters.controllers`

```java
@RestController
@RequestMapping("/v1/security")
@RequiredArgsConstructor
public class RegistrationController implements RegisterApi {

    private final RegistrationUseCase registrationUseCase;

    @Override
    public ResponseEntity<RegistrationResponseDTO> register(RegistrationRequestDTO request) {
        RegistrationResponseDTO response = registrationUseCase.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Step 1.6: Update SecurityConfig to permit registration endpoint

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Find the `dev`/`local` profile `SecurityFilterChain` bean. In the `requestMatchers` chain, add:

```java
.requestMatchers("/v1/security/register").permitAll()
```

Place it immediately after the existing `.requestMatchers("/v1/security/login/internal").permitAll()`.

### Step 1.7: Update TenantContextLoader to bypass registration path

**File**: `infra-common/src/main/java/com/akademiaplus/infra/persistence/config/TenantContextLoader.java`

In `shouldNotFilter()`, add the register path:

```java
return path.startsWith("/actuator")
        || path.startsWith("/v1/security/login")
        || path.startsWith("/v1/security/register");
```

### Step 1.8: Create RegistrationUseCase unit test

**File**: `application/src/test/java/com/akademiaplus/usecases/RegistrationUseCaseTest.java`

**Package**: `com.akademiaplus.usecases`

**Test class structure**:

```java
@ExtendWith(MockitoExtension.class)
class RegistrationUseCaseTest {

    // Constants shared between test and production code
    public static final String TEST_ORG_NAME = "Test Organization";
    public static final String TEST_EMAIL = "admin@test.com";
    public static final String TEST_ADDRESS = "123 Test St";
    public static final String TEST_FIRST_NAME = "John";
    public static final String TEST_LAST_NAME = "Doe";
    public static final String TEST_USERNAME = "johndoe";
    public static final String TEST_PASSWORD = "SecurePass1!";
    public static final Long TEST_TENANT_ID = 1L;
    public static final String TEST_TOKEN = "jwt-token-value";

    @Mock private TenantCreationUseCase tenantCreationUseCase;
    @Mock private EmployeeCreationUseCase employeeCreationUseCase;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private RegistrationUseCase registrationUseCase;

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Should create tenant, employee, and return JWT when valid request")
        void shouldCreateTenantEmployeeAndReturnJwt_whenValidRequest() {
            // Given
            // ... build RegistrationRequestDTO with constants
            // ... stub tenantCreationUseCase.create() to return TenantDTO with TEST_TENANT_ID
            // ... stub employeeCreationUseCase.create() to return EmployeeCreationResponseDTO
            // ... stub jwtTokenProvider.createToken() to return TEST_TOKEN

            // When
            // ... call registrationUseCase.register(request)

            // Then
            // ... assertThat(response.getToken()).isEqualTo(TEST_TOKEN)
            // ... assertThat(response.getTenantId()).isEqualTo(TEST_TENANT_ID)
            // ... verify tenantCreationUseCase.create() called once
            // ... verify employeeCreationUseCase.create() called once
        }

        @Test
        @DisplayName("Should set tenant context after tenant creation and before employee creation")
        void shouldSetTenantContext_whenTenantCreated() {
            // Given
            // ... same stubs as above

            // When
            // ... call registrationUseCase.register(request)

            // Then
            // ... use InOrder to verify:
            //     1. tenantCreationUseCase.create() called first
            //     2. tenantContextHolder.setTenantId(TEST_TENANT_ID) called second
            //     3. employeeCreationUseCase.create() called third
        }

        @Test
        @DisplayName("Should pass ADMINISTRATOR type and ADMIN role to employee creation")
        void shouldPassAdminTypeAndRole_whenCreatingEmployee() {
            // Given
            // ... same stubs

            // When
            // ... call registrationUseCase.register(request)

            // Then
            // ... capture EmployeeCreationRequestDTO via ArgumentCaptor
            // ... assertThat(captured.getEmployeeType()).isEqualTo(RegistrationUseCase.ADMIN_EMPLOYEE_TYPE)
            // ... assertThat(captured.getRole()).isEqualTo(RegistrationUseCase.ADMIN_ROLE)
        }
    }
}
```

**Rules**: Zero `any()` matchers — use exact values or `ArgumentCaptor`. `@DisplayName` on
every `@Test` and `@Nested`. All string literals as `public static final`.

### Verify Phase 1

```bash
mvn compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl application -am -f platform-core-api/pom.xml
```

Both commands must pass before proceeding.

### Commit Phase 1

```
feat(security): add tenant registration endpoint
```

---

## Phase 2: Mock-Data Tenant-Scoped Endpoint

### Step 2.1: Add tenant-scoped path to mock-data OpenAPI spec

**File**: `mock-data-system/src/main/resources/openapi/mock-data-system-module.yaml`

Add this path block after the existing `/mock-data/generate/all` path:

```yaml
  /mock-data/generate/tenant/{tenantId}:
    post:
      operationId: generateMockDataForTenant
      summary: Generate mock data for an existing tenant
      description: >
        Skips tenant creation and clean-up. Loads all tenant-scoped entities
        for the specified tenant. Does NOT create a new tenant.
      parameters:
        - name: tenantId
          in: path
          required: true
          schema:
            type: integer
            format: int64
        - name: count
          in: query
          required: false
          schema:
            type: integer
            default: 50
            minimum: 1
            maximum: 500
      responses:
        '201':
          description: Mock data generated for the specified tenant
          content:
            application/json:
              schema:
                type: string
                example: "Mock data generated: 50 records per entity type for tenant 1."
        '404':
          description: Tenant not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

### Step 2.2: Regenerate DTOs and verify generated interface

```bash
mvn generate-sources -pl mock-data-system -am -f platform-core-api/pom.xml
```

Verify `MockDataApi` interface now has method:
`ResponseEntity<String> generateMockDataForTenant(Long tenantId, Integer count)`

### Step 2.3: Add generateForTenant method to MockDataOrchestrator

**File**: `mock-data-system/src/main/java/com/akademiaplus/config/MockDataOrchestrator.java`

Add constants:

```java
public static final String ERROR_TENANT_NOT_FOUND = "Tenant not found with ID: ";
```

Add method:

```java
/**
 * Generates mock data for an existing tenant.
 * Skips tenant creation and clean-up — loads tenant-scoped entities only.
 *
 * @param tenantId           the existing tenant's ID
 * @param entitiesPerTenant  number of entities to generate per type
 * @throws EntityNotFoundException if the tenant does not exist
 */
public void generateForTenant(long tenantId, int entitiesPerTenant) {
    tenantRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException(
                    EntityType.TENANT, String.valueOf(tenantId)));

    MockDataExecutionPlan plan = MockDataExecutionPlan.forAll();

    tenantContextHolder.setTenantId(tenantId);
    for (MockEntityType entityType : plan.getLoadOrder()) {
        if (entityType == MockEntityType.TENANT) {
            continue;
        }
        mockDataLoaders.get(entityType).accept(entitiesPerTenant);
        if (mockDataPostLoadHooks.containsKey(entityType)) {
            mockDataPostLoadHooks.get(entityType).execute();
        }
    }
}
```

### Step 2.4: Implement new API method in MockDataController

**File**: `mock-data-system/src/main/java/com/akademiaplus/interfaceadapters/MockDataController.java`

Add method:

```java
@Override
public ResponseEntity<String> generateMockDataForTenant(Long tenantId, Integer count) {
    orchestrator.generateForTenant(tenantId, count);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body("Mock data generated: " + count + " records per entity type for tenant " + tenantId + ".");
}
```

### Step 2.5: Create/update MockDataOrchestrator unit test

**File**: `mock-data-system/src/test/java/com/akademiaplus/config/MockDataOrchestratorTest.java`

If the file already exists, add a new `@Nested` class. If not, create the full test class.

**Test class structure** (new nested class):

```java
@Nested
@DisplayName("Generate for tenant")
class GenerateForTenant {

    public static final Long EXISTING_TENANT_ID = 1L;
    public static final Long NON_EXISTENT_TENANT_ID = 999L;
    public static final int TEST_COUNT = 10;

    @Test
    @DisplayName("Should load tenant-scoped entities when tenant exists")
    void shouldLoadTenantScopedEntities_whenTenantExists() {
        // Given
        // ... stub tenantRepository.findById(EXISTING_TENANT_ID) → Optional.of(tenant)
        // ... stub mockDataLoaders for each entity type

        // When
        // ... call orchestrator.generateForTenant(EXISTING_TENANT_ID, TEST_COUNT)

        // Then
        // ... verify tenantContextHolder.setTenantId(EXISTING_TENANT_ID) called
        // ... verify each non-TENANT loader was called with TEST_COUNT
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when tenant does not exist")
    void shouldThrowEntityNotFoundException_whenTenantDoesNotExist() {
        // Given
        // ... stub tenantRepository.findById(NON_EXISTENT_TENANT_ID) → Optional.empty()

        // When / Then
        // ... assertThatThrownBy(() -> orchestrator.generateForTenant(NON_EXISTENT_TENANT_ID, TEST_COUNT))
        //     .isInstanceOf(EntityNotFoundException.class)
    }

    @Test
    @DisplayName("Should set tenant context before loading entities")
    void shouldSetTenantContext_whenGeneratingForTenant() {
        // Given
        // ... stub tenantRepository.findById(EXISTING_TENANT_ID) → Optional.of(tenant)

        // When
        // ... call orchestrator.generateForTenant(EXISTING_TENANT_ID, TEST_COUNT)

        // Then
        // ... use InOrder to verify setTenantId called before any loader
    }

    @Test
    @DisplayName("Should skip TENANT entity type in load order")
    void shouldSkipTenantEntityType_whenLoadingEntities() {
        // Given
        // ... stub tenantRepository.findById(EXISTING_TENANT_ID) → Optional.of(tenant)

        // When
        // ... call orchestrator.generateForTenant(EXISTING_TENANT_ID, TEST_COUNT)

        // Then
        // ... verify mockDataLoaders.get(MockEntityType.TENANT) was NEVER called
    }
}
```

### Verify Phase 2

```bash
mvn compile -pl mock-data-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl mock-data-system -am -f platform-core-api/pom.xml
```

Both commands must pass before proceeding.

### Commit Phase 2

```
feat(mock-data): add tenant-scoped mock data generation endpoint
```

---

## Phase 3: E2E Collection Rewrite

### Step 3.1: Add mockDataBaseUrl to environment files

**File**: `platform-api-e2e/environments/local.postman_environment.json`

Add variable:
```json
{ "key": "mockDataBaseUrl", "value": "http://localhost:8080/infra", "enabled": true }
```

**File**: `platform-api-e2e/environments/docker.postman_environment.json`

Add variable:
```json
{ "key": "mockDataBaseUrl", "value": "http://mock-data-service:8080/infra", "enabled": true }
```

**File**: `platform-api-e2e/environments/staging.postman_environment.json`

Add variable:
```json
{ "key": "mockDataBaseUrl", "value": "https://staging.akademiaplus.com/infra", "enabled": true }
```

### Step 3.2: Replace SetupLogin with SetupRegister

**File**: `platform-api-e2e/Postman Collections/platform-api-e2e.json`

Find the `SetupLogin` request object in the Setup folder. Replace it entirely with:

**Request**:
- Name: `SetupRegister`
- Method: `POST`
- URL: `{{baseUrl}}/v1/security/register`
- Header: `Content-Type: application/json`
- Body (raw JSON):

```json
{
    "organizationName": "E2E Test Organization",
    "email": "admin@e2e-test.com",
    "address": "123 Test Street",
    "firstName": "E2E",
    "lastName": "Admin",
    "username": "e2e-admin",
    "password": "E2eAdmin2026!$",
    "phone": "5551234567",
    "zipCode": "12345",
    "birthdate": "1990-01-15"
}
```

**Post-response test script** (exec array):

```javascript
pm.test("Registration successful", function () {
    pm.response.to.have.status(201);
    const response = pm.response.json();
    pm.collectionVariables.set("authToken", response.token);
    pm.collectionVariables.set("tenantId", response.tenantId);
});
```

### Step 3.3: Add SetupMockData request after SetupRegister

Insert a new request immediately after `SetupRegister` in the Setup folder:

**Request**:
- Name: `SetupMockData`
- Method: `POST`
- URL: `{{mockDataBaseUrl}}/v1/infra/mock-data/generate/tenant/{{tenantId}}?count=5`
- Headers: none required (mock-data-service has `anyRequest().permitAll()`)
- Body: none

**Post-response test script**:

```javascript
pm.test("Mock data generated successfully", function () {
    pm.response.to.have.status(201);
});
```

### Step 3.4: Remove SetupTenant request

Find and remove the `SetupTenant` request from the Setup folder. Registration already creates
the tenant. The `tenantId` collection variable is set by `SetupRegister`.

### Step 3.5: Verify remaining Setup requests use authToken and tenantId

Grep through all remaining Setup requests (`SetupEmployee`, `SetupCollaborator`, etc.) and
verify each includes:

```
Authorization: Bearer {{authToken}}
X-Tenant-Id: {{tenantId}}
```

If any Setup request references hardcoded credentials or a different auth mechanism, update it.

### Verify Phase 3

```bash
# Validate JSON syntax
node -e "JSON.parse(require('fs').readFileSync('platform-api-e2e/Postman Collections/platform-api-e2e.json', 'utf8')); console.log('Valid JSON')"
```

### Commit Phase 3

```
feat(e2e): bootstrap auth via registration endpoint
```

---

## Phase 4: Docker Compose + Integration Verification

### Step 4.1: Review and update Docker Compose e2e-runner

**File**: Find the relevant `docker-compose*.yml` in the repo root or `platform-api-e2e/`.

Check the `e2e-runner` service:

1. **depends_on**: Must include both `platform-core-api` and `mock-data-service` with
   `condition: service_healthy`.
2. **entrypoint/command**: If the runner script calls `POST /mock-data/generate/all` before
   Newman, **remove that step**. The E2E collection's `SetupRegister` + `SetupMockData` now
   handles bootstrapping.
3. **environment**: Ensure `mockDataBaseUrl` is set or the Docker environment file is
   mounted.

### Step 4.2: Update run-e2e.sh if it pre-seeds data

**File**: Find `run-e2e.sh` or equivalent script.

If it contains `curl` calls to `generate/all` before running Newman, remove them. The
collection drives all bootstrapping via `SetupRegister` → `SetupMockData`.

### Step 4.3: Full cold-start verification

```bash
# 1. Rebuild images
docker compose build

# 2. Start infrastructure
docker compose up -d mariadb
sleep 10  # Wait for MariaDB

# 3. Start platform-core-api
docker compose up -d platform-core-api
# Wait for: curl -s http://localhost:8080/api/actuator/health | grep UP

# 4. Start mock-data-service
docker compose up -d mock-data-service
# Wait for: curl -s http://localhost:8080/infra/actuator/health | grep UP

# 5. Run E2E
docker compose run --rm e2e-runner
```

### Step 4.4: Verify expected request flow

The Newman output should show:

```
1. SetupRegister         → 201 Created  (token + tenantId)
2. SetupMockData         → 201 Created  (mock data generated)
3. SetupEmployee         → 201 Created
4. SetupCollaborator     → 201 Created
5. SetupAdultStudent     → 201 Created
6. SetupTutor            → 201 Created
7. SetupCourse           → 201 Created
8. SetupSchedule         → 201 Created
9. SetupMembership       → 201 Created
10. SetupStoreProduct    → 201 Created
... all test folders pass ...
... teardown deletes all entities ...
```

### Step 4.5: Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `SetupRegister` → 403 | Registration path not in SecurityConfig permitAll | Add `.requestMatchers("/v1/security/register").permitAll()` in Phase 1 |
| `SetupRegister` → 400 "X-Tenant-Id required" | TenantContextLoader not bypassing register path | Add to `shouldNotFilter()` in Phase 1 |
| `SetupRegister` → 409 Duplicate | Previous test data not cleaned; DB not fresh | Recreate DB container: `docker compose down -v && docker compose up -d mariadb` |
| `SetupMockData` → 404 Tenant not found | `tenantId` variable not set by SetupRegister | Check post-response script: `pm.collectionVariables.set("tenantId", ...)` |
| `SetupMockData` → Connection refused | mock-data-service not running or wrong URL | Verify `mockDataBaseUrl` env variable and service health |
| Employee tests → 400 | Missing `X-Tenant-Id` header | Ensure all requests include `X-Tenant-Id: {{tenantId}}` |
| JWT expired during run | Long suite exceeds token TTL | Increase JWT expiry in test config |
| `SetupMockData` → 500 | Entity creation failure mid-batch | Check mock-data-service logs for FK constraint or sequence errors |

### Verify Phase 4

```bash
# All E2E requests must pass
./run-e2e.sh
# or: docker compose run --rm e2e-runner
```

### Commit Phase 4

```
fix(docker): update e2e-runner for registration bootstrap flow
```

---

## Summary of All Files

### New Files

| # | File | Module |
|---|------|--------|
| 1 | `security/src/main/resources/openapi/registration.yaml` | security |
| 2 | `application/src/main/java/com/akademiaplus/usecases/RegistrationUseCase.java` | application |
| 3 | `application/src/main/java/com/akademiaplus/interfaceadapters/controllers/RegistrationController.java` | application |
| 4 | `application/src/test/java/com/akademiaplus/usecases/RegistrationUseCaseTest.java` | application |

### Modified Files

| # | File | Module | Change |
|---|------|--------|--------|
| 1 | `security/src/main/resources/openapi/security-module.yaml` | security | Wire $ref to registration.yaml |
| 2 | `security/src/main/java/com/akademiaplus/config/SecurityConfig.java` | security | Add `/v1/security/register` permitAll |
| 3 | `infra-common/src/main/java/com/akademiaplus/infra/persistence/config/TenantContextLoader.java` | infra-common | Add register to shouldNotFilter |
| 4 | `mock-data-system/src/main/resources/openapi/mock-data-system-module.yaml` | mock-data-system | Add tenant-scoped endpoint |
| 5 | `mock-data-system/src/main/java/com/akademiaplus/config/MockDataOrchestrator.java` | mock-data-system | Add generateForTenant() |
| 6 | `mock-data-system/src/main/java/com/akademiaplus/interfaceadapters/MockDataController.java` | mock-data-system | Implement new API method |
| 7 | `platform-api-e2e/Postman Collections/platform-api-e2e.json` | e2e | Replace SetupLogin, add SetupMockData, remove SetupTenant |
| 8 | `platform-api-e2e/environments/local.postman_environment.json` | e2e | Add mockDataBaseUrl |
| 9 | `platform-api-e2e/environments/docker.postman_environment.json` | e2e | Add mockDataBaseUrl |
| 10 | `platform-api-e2e/environments/staging.postman_environment.json` | e2e | Add mockDataBaseUrl |
