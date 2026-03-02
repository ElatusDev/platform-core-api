# E2E Bootstrap Workflow — Registration-Driven Auth + Tenant-Scoped Mock Data

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Prerequisite**: Read `docs/directives/CLAUDE.md`, `docs/directives/AI-CODE-REF.md` before starting.
**Dependency**: None — first workflow for Wave 6 (E2E Automation).

---

## Section 1 — Problem Statement

The E2E test suite (`platform-api-e2e`) fails at `SetupLogin` because no admin user exists.
`InternalAuthDataModel` is tenant-scoped, creating a chicken-and-egg problem:

1. All user-creation endpoints require a JWT (authenticated).
2. JWTs are issued by `POST /v1/security/login/internal`, which requires an existing user.
3. Users belong to a tenant — no tenant means no user.
4. Tenant creation (`POST /v1/tenant-management/tenants`) also requires a JWT.

**Solution**: A public `POST /v1/security/register` endpoint that atomically creates a tenant + admin
employee and returns a JWT — no prior auth required.

### Deviation from `auth-bootstrap-prompt.md` (completed)

The previous prompt (in `prompts/completed/`) took a different approach:

| Aspect | Previous approach | New approach |
|--------|-------------------|--------------|
| `RegistrationUseCase` location | `security` module (new cross-module deps) | `application` module (no new cross-module deps) |
| Admin seeding | mock-data creates admin + `GET /seed-credentials` | Self-service registration — no mock-data dependency for auth |
| Mock-data enhancement | None | New `POST /mock-data/generate/tenant/{tenantId}` (tenant-aware) |
| E2E flow | seed-credentials → login → tests | register → mock-data → tests (no hardcoded credentials) |
| Credential handling | Plaintext in `SeedCredentialHolder` | Registration returns JWT directly |

The previous prompt stays in `completed/` for historical reference.

---

## Section 2 — Architecture Analysis

### 2.1 — Module Dependency Graph (Relevant Subset)

```
application
├── security              (JWT filter, SecurityConfig, LoginUseCase)
├── user-management       (EmployeeCreationUseCase, InternalAuthDataModel)
├── tenant-management     (TenantCreationUseCase, TenantDataModel)
├── infra-common          (TenantContextLoader, TenantContextHolder)
└── utilities             (ModelMapperConfig, MessageService)

mock-data-system          (standalone — NOT a dep of application)
├── user-management
├── tenant-management
├── course-management
├── billing
├── notification-system
├── pos-system
└── infra-common

platform-api-e2e          (Newman/Postman — external to Maven)
```

### 2.2 — Why `application` Module

`RegistrationUseCase` needs `TenantCreationUseCase` (tenant-management) and `EmployeeCreationUseCase`
(user-management). The `application` module already depends on both — placing the use case here avoids
adding `tenant-management` and `user-management` as dependencies of `security`.

The `application` module has no OpenAPI spec today, so one must be created.

### 2.3 — Current `SecurityConfig` Permit List

```java
// Profile: dev | local
.requestMatchers("/actuator/**").permitAll()
.requestMatchers("/v1/security/login/internal").permitAll()
.requestMatchers("/v3/api-docs/**").permitAll()
.requestMatchers("/swagger-ui/**").permitAll()
.requestMatchers("/swagger-ui.html").permitAll()
// + ModuleSecurityConfigurator beans
```

The registration endpoint must be added to the permit list.

### 2.4 — Current `TenantContextLoader` Bypass

```java
return path.startsWith("/actuator")
        || path.startsWith("/v1/security/login");
```

The registration endpoint path (`/v1/security/register`) already matches via the
`/v1/security/` prefix — **but only if we use `/v1/security/register`**. If the path starts
with `/v1/security/`, the current prefix match handles it. However, the current check is
`/v1/security/login` (not `/v1/security/`), so we must explicitly add `/v1/security/register`.

### 2.5 — Current `MockDataOrchestrator` Flow

```
generateAll(tenantCount, entitiesPerTenant)
  → cleanAll()
  → loadTenants(tenantCount)                         // creates N tenants
  → loadTenantScopedEntities(plan, entitiesPerTenant) // for each tenant: set context, load entities
```

The new `generateForTenant(long tenantId, int count)` method skips `cleanAll()` and `loadTenants()`,
reusing the tenant created by registration. It:
1. Verifies the tenant exists via `tenantRepository`
2. Sets `tenantContextHolder.setTenantId(tenantId)`
3. Calls `loadTenantScopedEntities()` for that single tenant

### 2.6 — Current E2E Collection Structure

```
Setup/
├── SetupLogin              ← uses hardcoded user1/pass1 — FAILS
├── SetupTenant             ← creates tenant (redundant with registration)
├── SetupEmployee           ← seeds employee for tests
├── SetupCollaborator       ← seeds collaborator
├── ...8 more setup requests
Tests/ (Tenant Management, User Management, Billing, Security, Course, Notification, POS)
Teardown/ (19 delete requests)
```

---

## Section 3 — Execution Phases

### Phase 1: Registration Endpoint (security + application modules)

**Goal**: Public `POST /v1/security/register` that creates tenant + admin employee + returns JWT.

#### Step 1.1 — OpenAPI Spec (`registration.yaml`)

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
        lastName:
          type: string
        phone:
          type: string
        zipCode:
          type: string
        birthdate:
          type: string
          format: date
        username:
          type: string
        password:
          type: string
          format: password
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
    ErrorResponse:
      type: object
```

**Wire into `security-module.yaml`**:

```yaml
# Add to components.schemas:
    RegistrationRequest:
      $ref: './registration.yaml#/components/schemas/RegistrationRequest'
    RegistrationResponse:
      $ref: './registration.yaml#/components/schemas/RegistrationResponse'

# Add to paths:
  /register:
    $ref: './registration.yaml#/paths/~1register'
```

**DTO generation**: The OpenAPI generator produces `RegistrationRequestDTO`, `RegistrationResponseDTO`,
and `RegisterApi` interface in the security module's DTO package.

#### Step 1.2 — `RegistrationUseCase` (application module)

**File**: `application/src/main/java/com/akademiaplus/usecases/RegistrationUseCase.java`

```java
@Service
@RequiredArgsConstructor
public class RegistrationUseCase {

    private final TenantCreationUseCase tenantCreationUseCase;
    private final EmployeeCreationUseCase employeeCreationUseCase;
    private final TenantContextHolder tenantContextHolder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RegistrationResponseDTO register(RegistrationRequestDTO request) {
        // 1. Build TenantCreateRequestDTO from registration fields
        // 2. Call tenantCreationUseCase.create() → TenantDTO (tenantId)
        // 3. Set tenantContextHolder.setTenantId(tenantId)  ← CRITICAL
        // 4. Build EmployeeCreationRequestDTO (type=ADMINISTRATOR, role=ADMIN)
        // 5. Call employeeCreationUseCase.create()
        // 6. Build JWT claims, call jwtTokenProvider.createToken()
        // 7. Return RegistrationResponseDTO(token, tenantId)
    }
}
```

**Key concern**: `TenantContextHolder` must be set AFTER tenant creation and BEFORE employee creation,
because the employee repository uses `@FilterDef("tenantFilter")` — without a tenant context, the
entity won't be associated with the new tenant.

#### Step 1.3 — `RegistrationController` (application module)

**File**: `application/src/main/java/com/akademiaplus/interfaceadapters/controllers/RegistrationController.java`

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

Note: The controller maps at `/v1/security` + OpenAPI path `/register` = `/v1/security/register`.

#### Step 1.4 — SecurityConfig Update

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add to the `dev`/`local` profile bean, in the `requestMatchers` chain:

```java
.requestMatchers("/v1/security/register").permitAll()
```

#### Step 1.5 — TenantContextLoader Update

**File**: `infra-common/src/main/java/com/akademiaplus/infra/persistence/config/TenantContextLoader.java`

Update `shouldNotFilter()`:

```java
return path.startsWith("/actuator")
        || path.startsWith("/v1/security/login")
        || path.startsWith("/v1/security/register");
```

#### Step 1.6 — Unit Tests

**File**: `application/src/test/java/com/akademiaplus/usecases/RegistrationUseCaseTest.java`

```
@Nested @DisplayName("Registration")
class Registration {
    @Test @DisplayName("Should create tenant, employee, and return JWT when valid request")
    void shouldCreateTenantEmployeeAndReturnJwt_whenValidRequest()

    @Test @DisplayName("Should set tenant context before creating employee")
    void shouldSetTenantContext_whenTenantCreated()
}
```

Conventions: Given-When-Then comments, `@DisplayName` on every `@Test` and `@Nested`, zero `any()`
matchers, all string literals as `public static final` constants.

#### Verify Phase 1

```bash
mvn compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl application -am -f platform-core-api/pom.xml
```

#### Commit Phase 1

```
feat(security): add tenant registration endpoint
```

---

### Phase 2: Mock-Data Tenant-Scoped Endpoint (mock-data-system module)

**Goal**: `POST /mock-data/generate/tenant/{tenantId}?count=50` — generates mock data for an
existing tenant without creating a new one.

#### Step 2.1 — OpenAPI Spec Update

**File**: `mock-data-system/src/main/resources/openapi/mock-data-system-module.yaml`

Add new path:

```yaml
  /mock-data/generate/tenant/{tenantId}:
    post:
      operationId: generateMockDataForTenant
      summary: Generate mock data for an existing tenant
      description: >
        Skips tenant creation and clean-up. Loads all tenant-scoped entities
        for the specified tenant.
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

#### Step 2.2 — `MockDataOrchestrator` Enhancement

**File**: `mock-data-system/src/main/java/com/akademiaplus/config/MockDataOrchestrator.java`

New method:

```java
/**
 * Generate mock data for an existing tenant.
 * Skips tenant creation and clean-up — loads tenant-scoped entities only.
 *
 * @param tenantId the existing tenant's ID
 * @param entitiesPerTenant number of entities to generate per type
 * @throws EntityNotFoundException if the tenant does not exist
 */
public void generateForTenant(long tenantId, int entitiesPerTenant) {
    // 1. Verify tenant exists via tenantRepository.findById() → throw if absent
    // 2. Build MockDataExecutionPlan.forAll()
    // 3. Set tenantContextHolder.setTenantId(tenantId)
    // 4. Iterate plan.getLoadOrder(), skip TENANT, call loader + post-load hook
}
```

This method reuses the existing `loadTenantScopedEntities` loop logic but scoped to a single,
pre-existing tenant.

#### Step 2.3 — `MockDataController` Enhancement

**File**: `mock-data-system/src/main/java/com/akademiaplus/interfaceadapters/MockDataController.java`

Implement the new generated API method:

```java
@Override
public ResponseEntity<String> generateMockDataForTenant(Long tenantId, Integer count) {
    orchestrator.generateForTenant(tenantId, count);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body("Mock data generated: " + count + " records per entity type for tenant " + tenantId + ".");
}
```

#### Step 2.4 — Unit Tests

**File**: `mock-data-system/src/test/java/com/akademiaplus/config/MockDataOrchestratorTest.java`

```
@Nested @DisplayName("Generate for tenant")
class GenerateForTenant {
    @Test @DisplayName("Should load tenant-scoped entities when tenant exists")
    void shouldLoadTenantScopedEntities_whenTenantExists()

    @Test @DisplayName("Should throw EntityNotFoundException when tenant does not exist")
    void shouldThrowEntityNotFoundException_whenTenantDoesNotExist()

    @Test @DisplayName("Should set tenant context before loading entities")
    void shouldSetTenantContext_whenGeneratingForTenant()

    @Test @DisplayName("Should skip TENANT entity type in load order")
    void shouldSkipTenantEntityType_whenLoadingEntities()
}
```

#### Verify Phase 2

```bash
mvn compile -pl mock-data-system -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl mock-data-system -am -f platform-core-api/pom.xml
```

#### Commit Phase 2

```
feat(mock-data): add tenant-scoped mock data generation endpoint
```

---

### Phase 3: E2E Collection Rewrite (platform-api-e2e)

**Goal**: Replace hardcoded credentials flow with registration-driven bootstrap.

#### Step 3.1 — Environment Files

Add `mockDataBaseUrl` variable to all three environment files:

| File | Variable | Value |
|------|----------|-------|
| `environments/local.postman_environment.json` | `mockDataBaseUrl` | `http://localhost:8080/infra` |
| `environments/docker.postman_environment.json` | `mockDataBaseUrl` | `http://mock-data-service:8080/infra` |
| `environments/staging.postman_environment.json` | `mockDataBaseUrl` | `https://staging.akademiaplus.com/infra` |

Note: mock-data-system has context path `/infra` and controller mapping `/v1/infra`, so the
full URL for the new endpoint is `{{mockDataBaseUrl}}/v1/infra/mock-data/generate/tenant/{{tenantId}}`.

#### Step 3.2 — Replace `SetupLogin` with `SetupRegister`

**Old**: `SetupLogin` — `POST {{baseUrl}}/v1/security/login/internal` with `user1`/`pass1`

**New**: `SetupRegister` — `POST {{baseUrl}}/v1/security/register`

```json
{
  "name": "SetupRegister",
  "request": {
    "method": "POST",
    "url": "{{baseUrl}}/v1/security/register",
    "header": [
      { "key": "Content-Type", "value": "application/json" }
    ],
    "body": {
      "mode": "raw",
      "raw": {
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
    }
  }
}
```

**Post-response script** (extract and store):

```javascript
const response = pm.response.json();
pm.collectionVariables.set("authToken", response.token);
pm.collectionVariables.set("tenantId", response.tenantId);
```

#### Step 3.3 — Add `SetupMockData`

Insert after `SetupRegister`:

```json
{
  "name": "SetupMockData",
  "request": {
    "method": "POST",
    "url": "{{mockDataBaseUrl}}/v1/infra/mock-data/generate/tenant/{{tenantId}}?count=5",
    "header": []
  }
}
```

No auth header needed — mock-data-service runs with `anyRequest().permitAll()` in
the `mock-data-service` profile.

#### Step 3.4 — Remove `SetupTenant`

Delete the `SetupTenant` request from the Setup folder. Registration already creates the tenant.

#### Step 3.5 — Update Remaining Setup Requests

The remaining `SetupEmployee`, `SetupCollaborator`, etc. must use the `authToken` and `tenantId`
from `SetupRegister`. Verify all setup requests include:

```
Authorization: Bearer {{authToken}}
X-Tenant-Id: {{tenantId}}
```

#### Verify Phase 3

```bash
# Validate collection JSON syntax
node -e "JSON.parse(require('fs').readFileSync('platform-api-e2e/Postman Collections/platform-api-e2e.json', 'utf8')); console.log('Valid JSON')"
```

#### Commit Phase 3

```
feat(e2e): bootstrap auth via registration endpoint
```

---

### Phase 4: Docker Compose + Integration Verification

**Goal**: Ensure the full cold-start E2E flow works with the registration-based bootstrap.

#### Step 4.1 — Docker Compose Updates

Verify `e2e-runner` service in `docker-compose.yml` (or `docker-compose.e2e.yml`):

- **depends_on**: `platform-core-api` and `mock-data-service` must be healthy
- **entrypoint**: The runner script must NOT call `generate/all` before Newman — the E2E
  collection itself drives registration + mock-data generation

If the runner script currently calls `POST /mock-data/generate/all` before Newman, remove
that step. The E2E collection's `SetupRegister` + `SetupMockData` handles bootstrapping.

#### Step 4.2 — Cold-Start Verification

Full sequence:

```bash
# 1. Build all images
docker compose build

# 2. Start infrastructure (DB, etc.)
docker compose up -d mariadb
# Wait for MariaDB readiness

# 3. Start platform-core-api
docker compose up -d platform-core-api
# Wait for /actuator/health → UP

# 4. Start mock-data-service
docker compose up -d mock-data-service
# Wait for /actuator/health → UP

# 5. Run E2E
docker compose run --rm e2e-runner
```

Expected request flow:

```
1. SetupRegister    → POST /v1/security/register     → 201 (token + tenantId)
2. SetupMockData    → POST /mock-data/generate/tenant/{tenantId}?count=5 → 201
3. SetupEmployee    → POST /v1/user-management/employees → 201
4. ...remaining setup requests
5. ...test requests
6. ...teardown requests
```

#### Step 4.3 — Troubleshooting Table

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `SetupRegister` → 403 | Registration path not in SecurityConfig permitAll | Add `.requestMatchers("/v1/security/register").permitAll()` |
| `SetupRegister` → 400 "X-Tenant-Id required" | TenantContextLoader not bypassing `/v1/security/register` | Add path to `shouldNotFilter()` |
| `SetupRegister` → 409 Duplicate | Previous test run left data; DB not clean | Truncate tables or recreate DB container |
| `SetupMockData` → 404 Tenant not found | tenantId from register not propagated | Check Postman script sets `pm.collectionVariables.set("tenantId", ...)` |
| `SetupMockData` → Connection refused | mock-data-service not running or wrong URL | Verify `mockDataBaseUrl` env variable and service health |
| Employee creation → 400 | Missing `X-Tenant-Id` header | Ensure all post-register requests include `X-Tenant-Id: {{tenantId}}` |
| JWT expired during test run | Long test suite exceeds token TTL | Increase JWT expiry in test config or add token refresh |
| `SetupMockData` → 500 | Entity creation fails mid-batch | Check mock-data-service logs; likely a missing sequence or FK constraint |

#### Verify Phase 4

```bash
# Full E2E pass
./run-e2e.sh
# or: docker compose run --rm e2e-runner
```

#### Commit Phase 4

```
fix(docker): update e2e-runner for registration bootstrap flow
```

---

## Section 4 — File Inventory

### New Files

| File | Module | Purpose |
|------|--------|---------|
| `security/src/main/resources/openapi/registration.yaml` | security | Registration OpenAPI spec |
| `application/src/main/java/.../usecases/RegistrationUseCase.java` | application | Registration business logic |
| `application/src/main/java/.../interfaceadapters/controllers/RegistrationController.java` | application | REST controller for registration |
| `application/src/test/java/.../usecases/RegistrationUseCaseTest.java` | application | Unit tests |

### Modified Files

| File | Module | Change |
|------|--------|--------|
| `security/src/main/resources/openapi/security-module.yaml` | security | Wire registration.yaml refs |
| `security/src/main/java/.../config/SecurityConfig.java` | security | Add `/v1/security/register` permitAll |
| `infra-common/src/main/java/.../TenantContextLoader.java` | infra-common | Add register path to shouldNotFilter |
| `mock-data-system/src/main/resources/openapi/mock-data-system-module.yaml` | mock-data-system | Add tenant-scoped endpoint |
| `mock-data-system/src/main/java/.../config/MockDataOrchestrator.java` | mock-data-system | Add `generateForTenant()` method |
| `mock-data-system/src/main/java/.../interfaceadapters/MockDataController.java` | mock-data-system | Implement new API method |
| `platform-api-e2e/Postman Collections/platform-api-e2e.json` | e2e | Replace SetupLogin, add SetupMockData |
| `platform-api-e2e/environments/local.postman_environment.json` | e2e | Add `mockDataBaseUrl` |
| `platform-api-e2e/environments/docker.postman_environment.json` | e2e | Add `mockDataBaseUrl` |
| `platform-api-e2e/environments/staging.postman_environment.json` | e2e | Add `mockDataBaseUrl` |

---

## Section 5 — Dependency Graph

```
Phase 1 (Registration endpoint)
    │
    ├──→ Phase 2 (Mock-data tenant endpoint)  [independent of Phase 1 code,
    │                                          but Phase 1 must be understood]
    │
    └──→ Phase 3 (E2E collection rewrite)     [depends on Phase 1 + Phase 2 APIs]
              │
              └──→ Phase 4 (Docker + verify)  [depends on all prior phases]
```

Phases 1 and 2 can be developed in parallel but must both compile before Phase 3.

---

## Section 6 — Critical Reminders

1. **Prototype beans**: All entity creation must use `applicationContext.getBean(EntityDataModel.class)`, never `new EntityDataModel()`.
2. **TenantContextHolder timing**: In `RegistrationUseCase`, `setTenantId()` MUST be called after `TenantCreationUseCase.create()` returns and BEFORE `EmployeeCreationUseCase.create()`.
3. **Named TypeMaps**: Any new ModelMapper mappings must use named TypeMaps with `setImplicitMappingEnabled(false/true)` sandwich.
4. **Constants**: All string literals in production and test code → `public static final`.
5. **Test conventions**: Given-When-Then comments, `shouldDoX_whenY()` naming, `@DisplayName` on every `@Test` and `@Nested`, zero `any()` matchers.
6. **Copyright header**: All new `.java` files must include the ElatusDev copyright header.
7. **OpenAPI DTO suffix**: Schema names must NOT include `DTO` — the generator appends it via `modelNameSuffix=DTO`.
8. **`@Transactional` placement**: Only on the `create()`/`register()` method, never on the `transform()` method.
9. **ID type**: Always `Long`, never `Integer`.
10. **Conventional Commits**: One commit per phase with the specified message.
