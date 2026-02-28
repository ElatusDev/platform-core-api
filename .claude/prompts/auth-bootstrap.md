# Auth Bootstrap Implementation — Claude Code Execution Prompt

**Target**: Claude Code CLI  
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`  
**E2E repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-api-e2e`  
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md` before starting.  
**Problem**: No public registration endpoint exists. The first user can never be created  
because all user-creation endpoints require JWT auth, but obtaining a JWT requires an  
existing user. This blocks both production tenant onboarding and E2E test execution.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 1 → 2 → 3 → 4).
2. Do NOT skip ahead. Each phase must compile before the next begins.
3. After EACH phase, run the specified verification command. Fix failures before proceeding.
4. All new files MUST include the ElatusDev copyright header.
5. All `public` classes and methods MUST have Javadoc.
6. Commit after each phase using the exact commit message provided.

---

## Phase 1: Register Endpoint (security module)

### Step 1.1: OpenAPI Contract

**Create file:** `security/src/main/resources/openapi/registration.yaml`

```yaml
openapi: 3.0.0
info:
  title: Registration API
  version: '1.0.0'
components:
  schemas:
    RegistrationRequest:
      type: object
      required:
        - organizationName
        - organizationEmail
        - organizationAddress
        - firstName
        - lastName
        - email
        - phoneNumber
        - address
        - zipCode
        - birthdate
        - username
        - password
      properties:
        organizationName:
          type: string
          maxLength: 200
        legalName:
          type: string
          maxLength: 200
        websiteUrl:
          type: string
          format: uri
          maxLength: 255
        organizationEmail:
          type: string
          format: email
          maxLength: 200
        organizationAddress:
          type: string
          maxLength: 200
        organizationPhone:
          type: string
          maxLength: 20
        description:
          type: string
        taxId:
          type: string
          maxLength: 50
        firstName:
          type: string
          maxLength: 200
        lastName:
          type: string
          maxLength: 200
        email:
          type: string
          format: email
          maxLength: 200
        phoneNumber:
          type: string
          maxLength: 20
        address:
          type: string
          maxLength: 200
        zipCode:
          type: string
          maxLength: 10
        birthdate:
          type: string
          format: date
        username:
          type: string
          maxLength: 500
        password:
          type: string
          format: password
          maxLength: 500
    RegistrationResponse:
      type: object
      required:
        - token
        - tenantId
      properties:
        token:
          type: string
          description: JWT authentication token
        tenantId:
          type: integer
          format: int64
          description: Created tenant ID
    ErrorResponse:
      type: object
paths:
  /register:
    post:
      summary: Register a new tenant with an admin user
      operationId: register
      description: >
        Atomic tenant onboarding endpoint. Creates Tenant + Admin Employee +
        InternalAuth in a single transaction and returns a JWT token.
        This is the authentication bootstrap — the only public endpoint
        that creates users without requiring prior authentication.
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
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '409':
          description: Conflict (duplicate username or organization)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

### Step 1.2: Wire into security-module.yaml

**Edit file:** `security/src/main/resources/openapi/security-module.yaml`

Add under `paths:`:
```yaml
  /register:
    $ref: './registration.yaml#/paths/~1register'
```

Add under `components.schemas:`:
```yaml
    RegistrationRequest:
      $ref: './registration.yaml#/components/schemas/RegistrationRequest'
    RegistrationResponse:
      $ref: './registration.yaml#/components/schemas/RegistrationResponse'
```

### Step 1.3: Regenerate DTOs

```bash
mvn generate-sources -pl security -am
```

Verify these classes exist in `security/target/generated-sources/`:
- `RegistrationRequestDTO`
- `RegistrationResponseDTO`
- `RegisterApi` interface with `register()` method

If the generated `RegisterApi` interface does not appear, check the `openapi-generator-maven-plugin`
configuration in `security/pom.xml` — it may only point to `security-module.yaml` as the input spec.
Ensure the `$ref` chain resolves correctly.

### Step 1.4: Add module dependencies to security/pom.xml

**Edit file:** `security/pom.xml`

Add these dependencies (security module currently has NO dependency on tenant-management
or user-management — verify with `grep`):

```xml
<dependency>
    <groupId>com.akademiaplus</groupId>
    <artifactId>tenant-management</artifactId>
    <version>1.0</version>
</dependency>
<dependency>
    <groupId>com.akademiaplus</groupId>
    <artifactId>user-management</artifactId>
    <version>1.0</version>
</dependency>
```

### Step 1.5: RegistrationUseCase

**Create file:** `security/src/main/java/com/akademiaplus/registration/usecases/RegistrationUseCase.java`

Implementation requirements:

1. **Inject** these constructor dependencies:
   - `TenantCreationUseCase` (from tenant-management)
   - `TenantContextHolder` (from multi-tenant-data)
   - `EmployeeCreationUseCase` (from user-management)
   - `JwtTokenProvider` (from security module)

2. **Method**: `public RegistrationResponseDTO register(RegistrationRequestDTO dto)`

3. **Flow** (all inside `@Transactional`):

   a. Build `TenantCreateRequestDTO` from registration fields:
      - `setOrganizationName(dto.getOrganizationName())`
      - `setEmail(dto.getOrganizationEmail())`
      - `setAddress(dto.getOrganizationAddress())`
      - `setPhone(dto.getOrganizationPhone())`
      - `setLegalName(dto.getLegalName())`
      - `setWebsiteUrl(URI.create(dto.getWebsiteUrl()))` — guard null with ternary
      - `setDescription(dto.getDescription())`
      - `setTaxId(dto.getTaxId())`

   b. Call `TenantCreationUseCase.create(tenantDto)` → get `TenantDTO`
      - Extract `tenantId` from response: `tenantDTO.getTenantId()`

   c. Set tenant context: `tenantContextHolder.setTenantId(tenantId)`
      — CRITICAL: without this, the `EntityIdAssigner` / `SequentialIDGenerator`
      cannot assign composite keys for the employee

   d. Build `EmployeeCreationRequestDTO`:
      - Map `firstName`, `lastName`, `email`, `phoneNumber`, `address`, `zipCode` directly
      - `setBirthdate(dto.getBirthdate())`
      - `setEntryDate(LocalDate.now())`
      - `setEmployeeType("ADMINISTRATOR")`
      - `setRole("ADMIN")`
      - `setUsername(dto.getUsername())`
      - `setPassword(dto.getPassword())`

      Check the `EmployeeCreationRequestDTO` constructor signature — it has a 12-arg
      constructor with required fields. Use `new EmployeeCreationRequestDTO(...)` or
      builder setters depending on the generated code.

   e. Call `EmployeeCreationUseCase.create(employeeDto)`
      — This triggers the full creation chain: PersonPII (with hashing) + InternalAuth
      (with AES encryption + username hash) + Employee entity

   f. Build JWT claims: `Map.of("Has role", "ADMIN")`
      — Must match the claim key used in `InternalAuthenticationUseCase.login()`

   g. Call `JwtTokenProvider.createToken(dto.getUsername(), tenantId, claims)`

   h. Return `new RegistrationResponseDTO(token, tenantId)`

**Annotations**: `@Service`, `@RequiredArgsConstructor`, method-level `@Transactional`

### Step 1.6: RegistrationController

**Create file:** `security/src/main/java/com/akademiaplus/registration/interfaceadapters/RegistrationController.java`

```java
@RestController
@RequestMapping("/v1/security")
public class RegistrationController implements RegisterApi {

    private final RegistrationUseCase registrationUseCase;

    public RegistrationController(RegistrationUseCase registrationUseCase) {
        this.registrationUseCase = registrationUseCase;
    }

    @Override
    public ResponseEntity<RegistrationResponseDTO> register(RegistrationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationUseCase.register(dto));
    }
}
```

### Step 1.7: SecurityConfig — permit register endpoint

**Edit file:** `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

In the `securityFilterChain` bean (dev/local profile), add after the login permitAll line:

```java
.requestMatchers("/v1/security/register").permitAll()
```

Also in `corsConfigurationSourceForLogin()`, add CORS for register:

```java
source.registerCorsConfiguration("/v1/security/register", loginCorsConfig);
```

### Step 1.8: Compile and verify

```bash
mvn compile -pl security -am
mvn package -pl application -am -DskipTests
```

Fix all compilation errors before proceeding. Common issues:
- `TenantDTO.getTenantId()` might be `getTenant_id()` (snake_case from OpenAPI)
- `EmployeeCreationRequestDTO` constructor argument order
- Missing import for `URI.create()`

### Step 1.9: Commit

```bash
git add -A
git commit -m "feat(security): add tenant registration endpoint

Introduce POST /v1/security/register as the authentication bootstrap
for new tenants. Creates Tenant + Admin Employee + InternalAuth
atomically in a single transaction and returns a JWT token.

Endpoint is permitAll to break the chicken-and-egg dependency where
creating the first user required prior authentication.

New module dependencies: security → tenant-management, user-management."
```

---

## Phase 2: Mock-Data Service — Seed Admin + Credentials Endpoint

### Step 2.1: SeedCredentialHolder

**Create file:** `mock-data-system/src/main/java/com/akademiaplus/config/SeedCredentialHolder.java`

```java
package com.akademiaplus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * In-memory holder for the seed admin's plaintext credentials.
 * <p>
 * Populated by {@link MockDataOrchestrator} during {@code generateAll()}
 * and exposed via the mock-data REST API for E2E test consumption.
 * Never persisted — exists only in the service's JVM lifetime.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Getter
@Setter
public class SeedCredentialHolder {
    private Long tenantId;
    private String username;
    private String password;
    private String role;
    private String firstName;
    private String lastName;
    private String email;
}
```

### Step 2.2: OpenAPI — seed-credentials endpoint

**Edit file:** `mock-data-system/src/main/resources/openapi/mock-data-system-module.yaml`

Add a new path under `paths:`:

```yaml
  /mock-data/seed-credentials:
    get:
      summary: Returns the seed admin credentials from the last generate-all run
      operationId: getSeedCredentials
      tags:
        - Mock Data
      description: >
        Returns plaintext credentials of the seed admin employee created during
        the most recent generateAll execution. Used by E2E test suites to
        authenticate against platform-core-api. Returns 404 if generate-all
        has not been called yet.
      responses:
        '200':
          description: Seed credentials available
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SeedCredentials'
        '404':
          description: No seed credentials available yet
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

Add under `components.schemas:`:

```yaml
    SeedCredentials:
      type: object
      required:
        - tenantId
        - username
        - password
        - role
      properties:
        tenantId:
          type: integer
          format: int64
        username:
          type: string
        password:
          type: string
        role:
          type: string
        firstName:
          type: string
        lastName:
          type: string
        email:
          type: string
```

### Step 2.3: Regenerate DTOs

```bash
mvn generate-sources -pl mock-data-system -am
```

Verify `SeedCredentialsDTO` and that `MockDataApi` now includes `getSeedCredentials()`.

### Step 2.4: MockDataOrchestrator — create seed admin

**Edit file:** `mock-data-system/src/main/java/com/akademiaplus/config/MockDataOrchestrator.java`

**Add constructor dependencies:**
- `EmployeeCreationUseCase` (already available — mock-data-system depends on user-management)
- `SeedCredentialHolder`

**Modify** `generateAll(int tenantCount, int entitiesPerTenant)`:

After `loadTenants(tenantCount)` and **before** `loadTenantScopedEntities(...)`, add:

```java
createSeedAdmin();
```

**New private method** `createSeedAdmin()`:

```java
/**
 * Creates a deterministic admin employee for the first tenant.
 * <p>
 * Credentials are captured in plaintext before the creation pipeline
 * applies hashing and encryption. The {@link SeedCredentialHolder}
 * makes them available via REST for E2E test authentication.
 */
private void createSeedAdmin() {
    Long firstTenantId = tenantRepository.findAll().stream()
            .map(TenantDataModel::getTenantId)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No tenants found after load"));

    tenantContextHolder.setTenantId(firstTenantId);

    String username = "seed.admin";
    String password = "SeedAdmin2025!$";
    String role = "ADMIN";
    String firstName = "Seed";
    String lastName = "Admin";
    String email = "seed.admin@akademiaplus.test";

    EmployeeCreationRequestDTO dto = new EmployeeCreationRequestDTO(
            "ADMINISTRATOR",          // employeeType
            LocalDate.of(1990, 1, 1), // birthdate
            LocalDate.now(),          // entryDate
            firstName,
            lastName,
            email,
            "+525500000000",          // phoneNumber
            "1 Seed Admin Blvd",      // address
            "00000",                  // zipCode
            username,
            password,
            role
    );

    employeeCreationUseCase.create(dto);
    log.info("Seed admin created for tenant {}: username={}", firstTenantId, username);

    seedCredentialHolder.setTenantId(firstTenantId);
    seedCredentialHolder.setUsername(username);
    seedCredentialHolder.setPassword(password);
    seedCredentialHolder.setRole(role);
    seedCredentialHolder.setFirstName(firstName);
    seedCredentialHolder.setLastName(lastName);
    seedCredentialHolder.setEmail(email);
}
```

**IMPORTANT**: Verify the exact `EmployeeCreationRequestDTO` constructor argument order by
reading the generated class. The 12-arg constructor order is defined by the `required` fields
in the OpenAPI spec. If the constructor signature doesn't match, use setter-based construction.

The seed admin must be created **inside a transaction** because `EmployeeCreationUseCase.create()`
calls `saveAndFlush()`. The `loadTenantScopedEntities` loop sets `tenantContextHolder` per-tenant,
so `createSeedAdmin()` must run before that loop. Wrap the call in `@Transactional(propagation = REQUIRES_NEW)`
if needed — check whether the orchestrator method itself is transactional.

### Step 2.5: MockDataController — expose seed credentials

**Edit file:** `mock-data-system/src/main/java/com/akademiaplus/interfaceadapters/MockDataController.java`

Add `SeedCredentialHolder` as a constructor dependency.

Implement the generated `getSeedCredentials()` method:

```java
@Override
public ResponseEntity<SeedCredentialsDTO> getSeedCredentials() {
    if (seedCredentialHolder.getUsername() == null) {
        return ResponseEntity.notFound().build();
    }
    SeedCredentialsDTO dto = new SeedCredentialsDTO();
    dto.setTenantId(seedCredentialHolder.getTenantId());
    dto.setUsername(seedCredentialHolder.getUsername());
    dto.setPassword(seedCredentialHolder.getPassword());
    dto.setRole(seedCredentialHolder.getRole());
    dto.setFirstName(seedCredentialHolder.getFirstName());
    dto.setLastName(seedCredentialHolder.getLastName());
    dto.setEmail(seedCredentialHolder.getEmail());
    return ResponseEntity.ok(dto);
}
```

### Step 2.6: Audit URL routing

**CRITICAL**: The mock-data-system has `server.servlet.context-path=/infra` in its
properties AND the controller uses `@RequestMapping("/v1/infra")`. This means all URLs
are prefixed `/infra/v1/infra/...` which is double-nested.

Check the actual URL for the existing generate-all endpoint:
```bash
# Start mock-data-system locally or check the docker logs for:
#   "Mapped \"{[POST /v1/infra/mock-data/generate/all]}\""
grep -rn "RequestMapping\|context-path" mock-data-system/src/main/
```

The correct full URL for seed-credentials will be:
```
http://mock-data-system:8180/infra/v1/infra/mock-data/seed-credentials
```

Confirm this by looking at how the e2e-runner currently calls `generate/all`. If the
working URL for generate is `/infra/v1/infra/mock-data/generate/all`, then seed-credentials
follows the same pattern. If the controller mapping is actually `/v1` (not `/v1/infra`),
adjust accordingly.

### Step 2.7: Compile and verify

```bash
mvn compile -pl mock-data-system -am
mvn package -pl mock-data-system -am -DskipTests
```

### Step 2.8: Commit

```bash
git add -A
git commit -m "feat(mock-data): create seed admin and expose credentials endpoint

MockDataOrchestrator now creates a deterministic seed admin employee
(seed.admin / SeedAdmin2025!\$) with ADMIN role during generateAll.
Plaintext credentials stored in SeedCredentialHolder (in-memory only)
and served via GET /mock-data/seed-credentials for E2E consumption."
```

---

## Phase 3: E2E Collection Rewrite

### Step 3.1: Add mockDataBaseUrl to environment files

**Edit file:** `../platform-api-e2e/environments/docker.postman_environment.json`

Add a new entry to the `values` array:
```json
{
  "key": "mockDataBaseUrl",
  "value": "http://mock-data-system:8180/infra",
  "type": "default",
  "enabled": true
}
```

**Edit file:** `../platform-api-e2e/environments/local.postman_environment.json`

Add:
```json
{
  "key": "mockDataBaseUrl",
  "value": "http://localhost:8180/infra",
  "type": "default",
  "enabled": true
}
```

### Step 3.2: Add collection variables

**Edit file:** `../platform-api-e2e/Postman Collections/platform-api-e2e.json`

Add to the `variable` array at the collection root:
```json
{"key": "mockDataBaseUrl", "value": "http://localhost:8180/infra"},
{"key": "seedUsername", "value": ""},
{"key": "seedPassword", "value": ""}
```

### Step 3.3: Replace Setup folder's first requests

In the collection JSON, locate `item[0]` (the "Setup" folder) → `item` array.

**Replace** the first request (`SetupLogin`) with `FetchSeedCredentials`:

```json
{
  "name": "FetchSeedCredentials",
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('Setup: seed credentials fetched', () => {",
          "    pm.response.to.have.status(200);",
          "});",
          "",
          "const json = pm.response.json();",
          "pm.collectionVariables.set('seedUsername', json.username);",
          "pm.collectionVariables.set('seedPassword', json.password);",
          "pm.collectionVariables.set('tenantId', json.tenantId);"
        ],
        "type": "text/javascript"
      }
    }
  ],
  "request": {
    "auth": {"type": "noauth"},
    "method": "GET",
    "header": [],
    "url": {
      "raw": "{{mockDataBaseUrl}}/v1/infra/mock-data/seed-credentials",
      "host": ["{{mockDataBaseUrl}}"],
      "path": ["v1", "infra", "mock-data", "seed-credentials"]
    }
  }
}
```

**After** `FetchSeedCredentials`, insert the login request as the second item:

```json
{
  "name": "SetupLogin",
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('Setup: login status is 200', () => {",
          "    pm.response.to.have.status(200);",
          "});",
          "",
          "const json = pm.response.json();",
          "pm.collectionVariables.set('authToken', json.token);"
        ],
        "type": "text/javascript"
      }
    }
  ],
  "request": {
    "auth": {"type": "noauth"},
    "method": "POST",
    "header": [
      {"key": "Content-Type", "value": "application/json"}
    ],
    "body": {
      "mode": "raw",
      "raw": "{\"username\": \"{{seedUsername}}\", \"password\": \"{{seedPassword}}\"}"
    },
    "url": {
      "raw": "{{baseUrl}}/v1/security/login/internal",
      "host": ["{{baseUrl}}"],
      "path": ["v1", "security", "login", "internal"]
    }
  }
}
```

**Remove** the `SetupTenant` request entirely. The tenant already exists from mock-data
generation. The `tenantId` variable is set by `FetchSeedCredentials`.

Keep all remaining Setup requests (SetupEmployee, SetupCollaborator, etc.) — they create
E2E-specific test entities and authenticate via the collection-level Bearer auth.

### Step 3.4: Docker Compose — e2e-runner entrypoint

**Edit file:** `docker-compose.dev.yml`

Replace the e2e-runner's `entrypoint` + arguments with a shell script that triggers
mock data generation before Newman runs:

```yaml
  e2e-runner:
    image: postman/newman:6-alpine
    container_name: e2e-runner
    profiles:
      - e2e
    depends_on:
      mock-data-system:
        condition: service_healthy
    networks:
      - akademia-internal
    volumes:
      - ../platform-api-e2e/Postman Collections:/etc/newman/collections:ro
      - ../platform-api-e2e/environments:/etc/newman/environments:ro
    entrypoint: /bin/sh
    command:
      - -c
      - |
        echo "Triggering mock data generation..."
        MOCK_URL="http://mock-data-system:8180/infra/v1/infra/mock-data/generate/all?count=5"
        until wget -qO- --post-data '' "$$MOCK_URL" 2>/dev/null; do
          echo "Waiting for mock-data-system..."
          sleep 2
        done
        echo "Mock data ready. Running E2E tests..."
        newman run \
          /etc/newman/collections/platform-api-e2e.json \
          -e /etc/newman/environments/docker.postman_environment.json \
          --reporters cli \
          --color on \
          --bail
```

**IMPORTANT**: Verify the `MOCK_URL` path by checking Phase 2 Step 2.6 results.
If the actual path is different (e.g., `/infra/v1/mock-data/generate/all` without
the double `/infra`), adjust here.

### Step 3.5: Commit

```bash
cd ../platform-api-e2e
git add -A
git commit -m "feat(e2e): bootstrap auth from mock-data seed credentials

E2E collection now fetches seed admin credentials from mock-data-service
before authenticating against platform-core-api. Removes hardcoded
user1/pass1 credentials and SetupTenant request. Docker entrypoint
triggers mock data generation before Newman execution."

cd ../platform-core-api
git add docker-compose.dev.yml
git commit -m "fix(docker): e2e-runner triggers mock-data before Newman

Replace static Newman entrypoint with shell script that POSTs to
mock-data generate/all and waits for 201 before running tests."
```

---

## Phase 4: Build, Test, Verify

### Step 4.1: Full Maven build

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
mvn clean package -DskipTests
```

Both `application` and `mock-data-system` must produce JARs successfully.

### Step 4.2: Docker stack

```bash
docker compose -f docker-compose.dev.yml down -v
docker network prune -f
docker compose -f docker-compose.dev.yml --profile e2e up --build \
    --abort-on-container-exit --exit-code-from e2e-runner 2>&1 | tee /tmp/e2e-run.log
```

### Step 4.3: Expected startup sequence

1. `trust-broker` → CA + JWKS ready
2. `multi_tenant_db` → MariaDB schema initialized
3. `platform-core-redis` → ready
4. `platform-core-api` → started, `/v1/security/register` + `/v1/security/login/internal` available
5. `mock-data-system` → started, health check passes
6. `e2e-runner` → `wget` POST to `/generate/all?count=5` → waits for 201
7. Mock-data generates: tenant → seed admin → all entity types
8. Newman starts:
   - `FetchSeedCredentials` → GET seed-credentials → 200 → sets `seedUsername`, `seedPassword`, `tenantId`
   - `SetupLogin` → POST login with seed credentials → 200 → sets `authToken`
   - `SetupEmployee` → POST employee → 201 → sets `employeeId`
   - (remaining Setup + CRUD tests execute with valid auth)

### Step 4.4: Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| 404 on POST `/register` | Path not in SecurityConfig permitAll | Add `.requestMatchers("/v1/security/register").permitAll()` |
| 500 on register — "No bean of type TenantCreationUseCase" | Missing dependency in security/pom.xml | Add tenant-management dependency |
| 500 on register — "No tenant context set" | Forgot `tenantContextHolder.setTenantId()` after tenant creation | Add the call between tenant and employee creation |
| 500 on register — EntityIdAssigner fails | TenantContextHolder not set before employee creation | Same as above |
| 404 on GET `/seed-credentials` | Wrong URL (double context-path nesting) | Audit mock-data-system routing per Step 2.6 |
| 401 on SetupLogin after FetchSeedCredentials succeeds | Encryption key mismatch | Ensure `ENCRYPTION_KEY` env var is identical for platform-core-api and mock-data-system in docker-compose |
| 401 on SetupLogin — password mismatch | `InternalAuthenticationUseCase.login()` compares plaintext → decrypted | Both services must use same AES key. The `@Convert(converter = StringEncryptor.class)` on InternalAuthDataModel decrypts on read |
| e2e-runner wget hangs | mock-data-system not reachable on network | Both containers must be on `akademia-internal` network |
| generate/all returns 500 | EmployeeCreationRequestDTO constructor args wrong | Read the generated constructor and match argument order |

---

## Architecture Notes

### Why register lives in the security module
The security module already owns the auth boundary (`/v1/security/*`). Registration is the
auth bootstrap — the first step before any authenticated operation. The new cross-module
dependencies (→ tenant-management, → user-management) are justified because registration
is inherently cross-cutting.

### Why mock-data stores plaintext credentials
`EmployeeCreationUseCase` applies SHA-256 hashing + AES-256-GCM encryption before persistence.
These are one-way from mock-data's perspective. `SeedCredentialHolder` (in-memory, never
persisted) is the only way to make credentials available for E2E. Acceptable because
mock-data-service runs only in dev/test environments, never exposed publicly.

### Password comparison in login
`InternalAuthenticationUseCase.login()` does `dto.getPassword().equals(user.getPassword())`
where `user.getPassword()` is the **decrypted** value via JPA `@Convert(StringEncryptor)`.
This works as long as both services share the same `ENCRYPTION_KEY` — which they do via
Docker secrets/env vars.
