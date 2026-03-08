# Magic Link Authentication — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/magic-link-auth-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 → 2 → ... → 12). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header (2026).
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals → `public static final` constants, shared between impl and tests.
9. Use `applicationContext.getBean()` for all entity instantiation — never `new EntityDataModel()`.
10. Read existing files BEFORE modifying — field names, import paths, and CompositeId class names vary.
11. Commit after each phase using the commit message provided.

---

## Phase 1: MagicLinkTokenDataModel + Repository + DB Schema

### Read first

```bash
cat multi-tenant-data/src/main/java/com/akademiaplus/security/CustomerAuthDataModel.java
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/model/TenantScoped.java
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/model/SoftDeletable.java
find infra-common/src/main/java -name "TenantScopedRepository.java" | head -1
cat $(find infra-common/src/main/java -name "TenantScopedRepository.java" | head -1)
```

Note the composite key pattern, `@IdClass`, `@SQLDelete`, and `TenantScopedRepository` generics.

### Step 1.1: Create MagicLinkTokenDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/security/MagicLinkTokenDataModel.java`

Follow the exact same pattern as `CustomerAuthDataModel`:
- `@Entity @Table(name = "magic_link_tokens")`
- `@IdClass(MagicLinkTokenDataModel.MagicLinkTokenCompositeId.class)`
- `@SQLDelete(sql = "UPDATE magic_link_tokens SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND magic_link_token_id = ?")`
- `extends TenantScoped`
- `@Scope("prototype") @Component`
- Fields:
  - `@Id @Column(name = "magic_link_token_id") Long magicLinkTokenId`
  - `@Column(name = "email", nullable = false, length = 500) String email`
  - `@Column(name = "token_hash", nullable = false, length = 64) String tokenHash`
  - `@Column(name = "expires_at", nullable = false) Instant expiresAt`
  - `@Column(name = "used_at") Instant usedAt`
  - `@Column(name = "created_at", nullable = false) Instant createdAt`
- Inner class `MagicLinkTokenCompositeId` with `Long tenantId`, `Long magicLinkTokenId`

### Step 1.2: Create MagicLinkTokenRepository

**File**: `security/src/main/java/com/akademiaplus/magiclink/interfaceadapters/MagicLinkTokenRepository.java`

- `@Repository`
- `extends TenantScopedRepository<MagicLinkTokenDataModel, MagicLinkTokenDataModel.MagicLinkTokenCompositeId>`
- Method: `Optional<MagicLinkTokenDataModel> findByTokenHash(String tokenHash)`

### Step 1.3: DB schema

Find the schema file:
```bash
find . -name "*.sql" | grep -i "schema\|db_init" | head -10
```

Add the `magic_link_tokens` table with:
- `tenant_id BIGINT NOT NULL`
- `magic_link_token_id BIGINT NOT NULL`
- `email VARCHAR(500) NOT NULL`
- `token_hash VARCHAR(64) NOT NULL`
- `expires_at TIMESTAMP NOT NULL`
- `used_at TIMESTAMP NULL`
- `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `deleted_at TIMESTAMP NULL`
- `PRIMARY KEY (tenant_id, magic_link_token_id)`
- `INDEX idx_magic_link_token_hash (tenant_id, token_hash, deleted_at)`

Also add the ID sequence entry if the project uses a tenant-scoped sequence table.

### Step 1.4: Compile

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 1.5: Commit

```bash
git add multi-tenant-data/src/main/java/com/akademiaplus/security/MagicLinkTokenDataModel.java security/src/main/java/com/akademiaplus/magiclink/ db_init/
git commit -m "feat(multi-tenant-data): add MagicLinkTokenDataModel entity

Add magic_link_tokens table and MagicLinkTokenDataModel entity
with tokenHash (SHA-256), expiresAt, usedAt for single-use
passwordless authentication. Add MagicLinkTokenRepository with
findByTokenHash query."
```

---

## Phase 2: OpenAPI Specification

### Read first

```bash
cat security/src/main/resources/openapi/internal-authentication.yaml
cat security/src/main/resources/openapi/security-module.yaml
```

Note the schema names for `AuthTokenResponse` and `ErrorResponse`, and the path reference pattern.

### Step 2.1: Create magic-link-authentication.yaml

**File**: `security/src/main/resources/openapi/magic-link-authentication.yaml`

```yaml
openapi: 3.1.0
info:
  title: Magic Link Authentication API
  version: 1.0.0
  description: Passwordless email-based authentication endpoints

paths:
  /login/magic-link/request:
    post:
      summary: Request a magic link for email-based login
      description: >
        Sends a magic link to the provided email address. Always returns
        200 regardless of whether the email exists (anti-enumeration).
      operationId: requestMagicLink
      tags:
        - magic-link
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MagicLinkRequest'
      responses:
        '200':
          description: Magic link request accepted (always succeeds)
        '429':
          description: Rate limit exceeded
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/ErrorResponse'

  /login/magic-link/verify:
    post:
      summary: Verify a magic link token and obtain a JWT
      description: >
        Validates the magic link token and returns a platform JWT.
        The token must be valid, not expired, and not previously used.
      operationId: verifyMagicLink
      tags:
        - magic-link
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MagicLinkVerifyRequest'
      responses:
        '200':
          description: Token verified, JWT issued
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/AuthTokenResponse'
        '401':
          description: Token invalid, expired, or already used
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/ErrorResponse'

components:
  schemas:
    MagicLinkRequest:
      type: object
      properties:
        email:
          type: string
          format: email
          description: Email address to send the magic link to
        tenantId:
          type: integer
          format: int64
          description: Tenant ID for the login context
      required:
        - email
        - tenantId

    MagicLinkVerifyRequest:
      type: object
      properties:
        token:
          type: string
          description: The magic link token from the email URL
        tenantId:
          type: integer
          format: int64
          description: Tenant ID for the login context
      required:
        - token
        - tenantId
```

### Step 2.2: Modify security-module.yaml

Add the magic link path and schema references following the same pattern as existing references.

### Step 2.3: Regenerate DTOs

```bash
mvn clean generate-sources -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 2.4: Verify generated code

```bash
find security/target/generated-sources -name "MagicLinkRequestDTO.java" | head -1
find security/target/generated-sources -name "MagicLinkVerifyRequestDTO.java" | head -1
find security/target/generated-sources -name "*MagicLink*Api.java" -o -name "*Magiclink*Api.java" | head -3
```

Read and confirm:
- `MagicLinkRequestDTO` has `getEmail()`, `getTenantId()`
- `MagicLinkVerifyRequestDTO` has `getToken()`, `getTenantId()`
- Note the exact generated API interface name(s) for Phase 8

### Step 2.5: Commit

```bash
git add security/src/main/resources/openapi/
git commit -m "api(security): add magic link authentication OpenAPI specification

Add POST /login/magic-link/request and /login/magic-link/verify
endpoints. Request accepts email + tenantId, verify accepts
token + tenantId and returns AuthTokenResponse."
```

---

## Phase 3: SecurityConfig Updates

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

### Step 3.1: Add permit rules

Add alongside existing permit rules:

```java
.requestMatchers("/v1/security/login/magic-link/request").permitAll()
.requestMatchers("/v1/security/login/magic-link/verify").permitAll()
```

### Step 3.2: Add CORS rules

Add CORS configuration for both endpoints using the same `loginCorsConfig` object:

```java
source.registerCorsConfiguration("/v1/security/login/magic-link/request", loginCorsConfig);
source.registerCorsConfiguration("/v1/security/login/magic-link/verify", loginCorsConfig);
```

### Step 3.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/SecurityConfig.java
git commit -m "feat(security): permit magic link authentication endpoints

Add /v1/security/login/magic-link/request and /verify to
permitAll and CORS config alongside existing login rules."
```

---

## Phase 4: MagicLinkProperties

### Step 4.1: Create MagicLinkProperties

**File**: `security/src/main/java/com/akademiaplus/config/MagicLinkProperties.java`

- `@ConfigurationProperties(prefix = "magic-link")`
- Record with: `String baseUrl`, `int tokenExpiryMinutes`, `int maxRequestsPerEmailPerHour`, `String emailSubject`

Register via `@EnableConfigurationProperties(MagicLinkProperties.class)` or an existing configuration class.

### Step 4.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 4.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/MagicLinkProperties.java
git commit -m "feat(security): add MagicLinkProperties configuration

Add @ConfigurationProperties for magic-link.* prefix with
base URL, token expiry, rate limit, and email subject config."
```

---

## Phase 5: Exceptions

### Read first

```bash
cat security/src/main/java/com/akademiaplus/exceptions/InvalidLoginException.java 2>/dev/null
find security/src/main/java -name "*Exception.java" | head -10
```

Follow the existing exception pattern.

### Step 5.1: MagicLinkTokenExpiredException

**File**: `security/src/main/java/com/akademiaplus/magiclink/exceptions/MagicLinkTokenExpiredException.java`

```java
public class MagicLinkTokenExpiredException extends RuntimeException {
    public static final String ERROR_TOKEN_EXPIRED = "Magic link token has expired";
    public static final String ERROR_CODE = "MAGIC_LINK_TOKEN_EXPIRED";

    public MagicLinkTokenExpiredException() {
        super(ERROR_TOKEN_EXPIRED);
    }
}
```

### Step 5.2: MagicLinkTokenAlreadyUsedException

**File**: `security/src/main/java/com/akademiaplus/magiclink/exceptions/MagicLinkTokenAlreadyUsedException.java`

Same pattern with `ERROR_TOKEN_ALREADY_USED` and `MAGIC_LINK_TOKEN_ALREADY_USED` error code.

### Step 5.3: MagicLinkTokenNotFoundException

**File**: `security/src/main/java/com/akademiaplus/magiclink/exceptions/MagicLinkTokenNotFoundException.java`

Same pattern with `ERROR_TOKEN_NOT_FOUND` and `MAGIC_LINK_TOKEN_NOT_FOUND` error code.

### Step 5.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 5.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/magiclink/exceptions/
git commit -m "feat(security): add magic link exception types

Add MagicLinkTokenExpiredException, MagicLinkTokenAlreadyUsedException,
and MagicLinkTokenNotFoundException for magic link verification
error handling."
```

---

## Phase 6: MagicLinkRequestUseCase

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCase.java
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailDeliveryChannelStrategy.java
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryChannelStrategy.java
cat utilities/src/main/java/com/akademiaplus/utilities/security/HashingService.java
cat security/src/main/java/com/akademiaplus/ratelimit/usecases/RateLimiterService.java 2>/dev/null || echo "RateLimiterService not yet implemented"
cat multi-tenant-data/src/main/java/com/akademiaplus/notifications/NotificationDataModel.java
```

Understand:
- How `EmailDeliveryChannelStrategy.deliver(NotificationDataModel, recipientEmail)` works
- How `HashingService.generateHash(String)` works
- How `NotificationDataModel` is constructed (check if it's a prototype bean)
- How `RateLimiterService.checkRateLimit(key, limit, windowMs)` works (if implemented)

### Step 6.1: Create MagicLinkRequestUseCase

**File**: `security/src/main/java/com/akademiaplus/magiclink/usecases/MagicLinkRequestUseCase.java`

- `@Service`
- Constructor-injected dependencies:
  - `MagicLinkTokenRepository`
  - `HashingService`
  - `MagicLinkProperties`
  - `TenantContextHolder`
  - `ApplicationContext`
  - `EmailDeliveryChannelStrategy` (or `DeliveryChannelStrategy` — check the interface)
  - `RateLimiterService` (if available — use `@Autowired(required = false)` if pending)

- Constants:
  - `public static final String RATE_LIMIT_KEY_PREFIX = "rate:magic-link:email:"`
  - `public static final int TOKEN_BYTE_LENGTH = 32`
  - `public static final String MAGIC_LINK_URL_TEMPLATE = "%s/auth/magic-link?token=%s&tenant=%d"`
  - `public static final String PROVIDER_MAGIC_LINK = "magic-link"`

- Method `requestMagicLink(MagicLinkRequestDTO dto)`:
  1. `tenantContextHolder.setTenantId(dto.getTenantId())`
  2. Rate limit check (if `rateLimiterService` not null):
     ```java
     RateLimitResult result = rateLimiterService.checkRateLimit(
         RATE_LIMIT_KEY_PREFIX + dto.getEmail(),
         properties.maxRequestsPerEmailPerHour(),
         3_600_000L);
     if (!result.allowed()) return; // Silent — anti-enumeration
     ```
  3. Generate token:
     ```java
     byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
     new SecureRandom().nextBytes(randomBytes);
     String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
     ```
  4. Hash token: `String tokenHash = hashingService.generateHash(rawToken)`
  5. Create entity via `applicationContext.getBean(MagicLinkTokenDataModel.class)`:
     - Set `tenantId`, `email`, `tokenHash`, `expiresAt`, `createdAt`
     - `expiresAt = Instant.now().plus(properties.tokenExpiryMinutes(), ChronoUnit.MINUTES)`
  6. Save entity
  7. Build magic link URL: `String.format(MAGIC_LINK_URL_TEMPLATE, properties.baseUrl(), rawToken, dto.getTenantId())`
  8. Build HTML email (use inline constant — see workflow for template)
  9. Create `NotificationDataModel` via `applicationContext.getBean()` — set title (subject), content (HTML)
  10. Call email delivery strategy: `emailDeliveryChannelStrategy.deliver(notification, dto.getEmail())`

**IMPORTANT**:
- `SecureRandom` instance should be a class field (thread-safe, reusable)
- Always return void — no exception thrown, even if email fails or rate limited
- The email HTML template is a `public static final String` constant

### Step 6.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 6.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/magiclink/usecases/
git commit -m "feat(security): implement MagicLinkRequestUseCase

Add single-module use case that generates a 32-byte random
token, stores SHA-256 hash in DB, and sends magic link email.
Includes per-email rate limiting (3/hour) and anti-enumeration
(always returns 200)."
```

---

## Phase 7: MagicLinkVerificationUseCase

### Read first

```bash
cat user-management/src/main/java/com/akademiaplus/customer/adultstudent/usecases/AdultStudentCreationUseCase.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
cat utilities/src/main/java/com/akademiaplus/utilities/security/PiiNormalizer.java
cat user-management/src/main/java/com/akademiaplus/customer/interfaceadapters/PersonPIIRepository.java
cat user-management/src/main/java/com/akademiaplus/customer/adultstudent/interfaceadapters/AdultStudentRepository.java
cat user-management/src/main/java/com/akademiaplus/customer/interfaceadapters/CustomerAuthRepository.java
cat multi-tenant-data/src/main/java/com/akademiaplus/users/base/PersonPIIDataModel.java
```

Understand:
- How `AdultStudentCreationUseCase` builds PersonPII + CustomerAuth + AdultStudent
- How `JwtTokenProvider.createToken(username, tenantId, claims)` works
- How `PiiNormalizer.normalizeEmail()` works
- The exact field names and relationships on `PersonPIIDataModel`, `AdultStudentDataModel`

### Step 7.1: Create MagicLinkVerificationUseCase

**File**: `application/src/main/java/com/akademiaplus/magiclink/usecases/MagicLinkVerificationUseCase.java`

- `@Service`
- Constructor-injected dependencies:
  - `MagicLinkTokenRepository`
  - `PersonPIIRepository`
  - `AdultStudentRepository`
  - `CustomerAuthRepository`
  - `JwtTokenProvider`
  - `HashingService`
  - `PiiNormalizer`
  - `TenantContextHolder`
  - `ApplicationContext`

- Constants:
  - `public static final String PLACEHOLDER_PHONE = "PENDING_UPDATE"`
  - `public static final String PLACEHOLDER_ADDRESS = "PENDING_UPDATE"`
  - `public static final String PLACEHOLDER_ZIP = "PENDING_UPDATE"`
  - `public static final String JWT_CLAIM_ROLE = "Has role"`
  - `public static final String ROLE_CUSTOMER = "CUSTOMER"`
  - `public static final String PROVIDER_MAGIC_LINK = "magic-link"`

- Method `verifyMagicLink(MagicLinkVerifyRequestDTO dto)` — `@Transactional`:
  1. `tenantContextHolder.setTenantId(dto.getTenantId())`
  2. `String tokenHash = hashingService.generateHash(dto.getToken())`
  3. `MagicLinkTokenDataModel token = magicLinkTokenRepository.findByTokenHash(tokenHash).orElseThrow(MagicLinkTokenNotFoundException::new)`
  4. `if (token.getUsedAt() != null) throw new MagicLinkTokenAlreadyUsedException()`
  5. `if (Instant.now().isAfter(token.getExpiresAt())) throw new MagicLinkTokenExpiredException()`
  6. `token.setUsedAt(Instant.now())` — mark as consumed
  7. `magicLinkTokenRepository.save(token)` (or rely on dirty checking)
  8. `String normalizedEmail = piiNormalizer.normalizeEmail(token.getEmail())`
  9. `String emailHash = hashingService.generateHash(normalizedEmail)`
  10. `Optional<PersonPIIDataModel> existingPii = personPIIRepository.findByEmailHash(emailHash)`

  **Branch A — Existing user** (PersonPII found):
  - Find AdultStudent via repository (read entity relationships to determine query)
  - Build JWT claims: `Map.of(JWT_CLAIM_ROLE, ROLE_CUSTOMER)`
  - `String jwt = jwtTokenProvider.createToken(token.getEmail(), dto.getTenantId(), claims)`
  - Return `new AuthTokenResponseDTO(jwt)`

  **Branch B — New user** (PersonPII not found):
  - Create `PersonPIIDataModel` via `applicationContext.getBean()`
    - Set firstName from email prefix (or "User"), lastName "PENDING_UPDATE"
    - Set email (normalized), emailHash
    - Set placeholders for phone, address, zip
  - Create `CustomerAuthDataModel` via `applicationContext.getBean()`
    - Set provider = `PROVIDER_MAGIC_LINK`
    - Set token = "magic-link" (no OAuth token needed)
  - Create `AdultStudentDataModel` via `applicationContext.getBean()`
    - Link to PersonPII and CustomerAuth
  - Save (check if cascade or explicit save needed)
  - Build JWT and return

### Step 7.2: Compile

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

### Step 7.3: Commit

```bash
git add application/src/main/java/com/akademiaplus/magiclink/
git commit -m "feat(application): implement MagicLinkVerificationUseCase

Add cross-module orchestrator for magic link token verification.
Validates token hash, checks expiry and single-use, looks up
or creates user by email, and issues platform JWT. Supports
automatic account creation for new users."
```

---

## Phase 8: MagicLinkController + MagicLinkControllerAdvice

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/InternalAuthController.java
cat utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java
find application/target/generated-sources security/target/generated-sources -name "*MagicLink*Api.java" -o -name "*Magiclink*Api.java" 2>/dev/null
```

Note the exact generated API interface name and method signatures.

### Step 8.1: Create MagicLinkController

**File**: `application/src/main/java/com/akademiaplus/magiclink/interfaceadapters/MagicLinkController.java`

- `@RestController @RequestMapping("/v1/security")`
- Implements the generated magic link API interface (exact name from Phase 2 verification)
- Two methods:
  - `requestMagicLink(MagicLinkRequestDTO)` → delegates to `MagicLinkRequestUseCase.requestMagicLink()`, returns `ResponseEntity.ok().build()`
  - `verifyMagicLink(MagicLinkVerifyRequestDTO)` → delegates to `MagicLinkVerificationUseCase.verifyMagicLink()`, returns `ResponseEntity.ok(result)`

### Step 8.2: Create MagicLinkControllerAdvice

**File**: `application/src/main/java/com/akademiaplus/magiclink/config/MagicLinkControllerAdvice.java`

- `@ControllerAdvice(basePackageClasses = MagicLinkController.class)`
- Extends `BaseControllerAdvice`
- `@ExceptionHandler(MagicLinkTokenNotFoundException.class)` → 401
- `@ExceptionHandler(MagicLinkTokenExpiredException.class)` → 401
- `@ExceptionHandler(MagicLinkTokenAlreadyUsedException.class)` → 401
- Constants: `CODE_TOKEN_NOT_FOUND`, `CODE_TOKEN_EXPIRED`, `CODE_TOKEN_ALREADY_USED`

### Step 8.3: Compile

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

### Step 8.4: Commit

```bash
git add application/src/main/java/com/akademiaplus/magiclink/
git commit -m "feat(application): add MagicLinkController and ControllerAdvice

Implement generated magic link API interface for request and
verify endpoints. Handle token-not-found, expired, and
already-used exceptions as 401 responses."
```

---

## Phase 9: Configuration Properties

### Read first

```bash
cat application/src/main/resources/application.properties
```

### Step 9.1: Add magic link properties

Append to `application/src/main/resources/application.properties`:

```properties
# Magic Link Authentication
magic-link.base-url=${MAGIC_LINK_BASE_URL:http://localhost:3000}
magic-link.token-expiry-minutes=${MAGIC_LINK_EXPIRY_MINUTES:10}
magic-link.max-requests-per-email-per-hour=${MAGIC_LINK_MAX_REQUESTS:3}
magic-link.email-subject=Sign in to ElatusDev
```

### Step 9.2: Commit

```bash
git add application/src/main/resources/application.properties
git commit -m "feat(application): add magic link configuration properties

Add magic-link base URL, token expiry (10 min), max requests
per email per hour (3), and email subject configuration."
```

---

## Phase 10: Unit Tests — Security Module

### Read first

```bash
cat security/src/main/java/com/akademiaplus/magiclink/usecases/MagicLinkRequestUseCase.java
find security/src/test -name "*Test.java" | head -10
```

### Step 10.1: Create test directory

```bash
mkdir -p security/src/test/java/com/akademiaplus/magiclink/usecases
```

### Step 10.2: Create MagicLinkRequestUseCaseTest

**File**: `security/src/test/java/com/akademiaplus/magiclink/usecases/MagicLinkRequestUseCaseTest.java`

- `@ExtendWith(MockitoExtension.class)`
- All dependencies mocked
- Constants:
  - `public static final String TEST_EMAIL = "user@example.com"`
  - `public static final Long TEST_TENANT_ID = 1L`
  - `public static final String TEST_TOKEN_HASH = "abc123def456..."`
  - `public static final String TEST_BASE_URL = "https://app.elatusdev.com"`

**@Nested TokenGeneration**:

- `shouldGenerateUrlSafeBase64Token_whenRequestingMagicLink`:
  - Given: valid DTO
  - When: `requestMagicLink(dto)`
  - Then: `ArgumentCaptor` captures `MagicLinkTokenDataModel` saved to repo; `tokenHash` is not null and has 64 chars (SHA-256 hex)

- `shouldHashTokenWithSha256_whenStoringInDatabase`:
  - Given: `hashingService.generateHash(tokenCaptor)` returns known hash
  - When: `requestMagicLink(dto)`
  - Then: stored entity has the expected hash

**@Nested TokenStorage**:

- `shouldStoreTokenHashInDatabase_whenRequestingMagicLink`:
  - Given: valid DTO
  - When: `requestMagicLink(dto)`
  - Then: `magicLinkTokenRepository.save()` called with entity containing tokenHash

- `shouldSetExpiryFromProperties_whenStoringToken`:
  - Given: `properties.tokenExpiryMinutes()` = 10
  - When: `requestMagicLink(dto)`
  - Then: saved entity `expiresAt` is approximately 10 minutes from now

- `shouldSetCreatedAtToNow_whenStoringToken`:
  - Given: valid DTO
  - When: `requestMagicLink(dto)`
  - Then: saved entity `createdAt` is approximately now

**@Nested EmailDelivery**:

- `shouldSendEmailWithMagicLink_whenRequestingMagicLink`:
  - Given: valid DTO
  - When: `requestMagicLink(dto)`
  - Then: `emailDeliveryChannelStrategy.deliver()` called with notification containing magic link URL

- `shouldBuildCorrectMagicLinkUrl_whenSendingEmail`:
  - Given: `properties.baseUrl()` = TEST_BASE_URL
  - When: `requestMagicLink(dto)`
  - Then: email content contains URL matching `MAGIC_LINK_URL_TEMPLATE` pattern

**@Nested RateLimiting**:

- `shouldCheckRateLimit_whenRequestingMagicLink`:
  - Given: `rateLimiterService` is available
  - When: `requestMagicLink(dto)`
  - Then: `rateLimiterService.checkRateLimit()` called with key `RATE_LIMIT_KEY_PREFIX + TEST_EMAIL`

- `shouldNotSendEmail_whenRateLimitExceeded`:
  - Given: `rateLimiterService.checkRateLimit()` returns `RateLimitResult(false, ...)`
  - When: `requestMagicLink(dto)`
  - Then: `emailDeliveryChannelStrategy.deliver()` never called

- `shouldStillReturn200_whenRateLimitExceeded`:
  - Given: rate limit exceeded
  - When: `requestMagicLink(dto)`
  - Then: no exception thrown (method returns normally)

**@Nested AntiEnumeration**:

- `shouldAlwaysSucceed_whenEmailDoesNotExist`:
  - Given: any email
  - When: `requestMagicLink(dto)`
  - Then: no exception thrown, email sent to provided address

**@Nested TenantContext**:

- `shouldSetTenantId_whenRequestingMagicLink`:
  - Given: DTO with tenantId = TEST_TENANT_ID
  - When: `requestMagicLink(dto)`
  - Then: `tenantContextHolder.setTenantId(TEST_TENANT_ID)` called

### Step 10.3: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 10.4: Commit

```bash
git add security/src/test/java/com/akademiaplus/magiclink/
git commit -m "test(security): add MagicLinkRequestUseCase unit tests

Cover token generation, SHA-256 hashing, DB storage, email
delivery, rate limiting, anti-enumeration, and tenant context."
```

---

## Phase 11: Unit Tests — Application Module

### Read first

```bash
cat application/src/main/java/com/akademiaplus/magiclink/usecases/MagicLinkVerificationUseCase.java
cat application/src/test/java/com/akademiaplus/oauth/usecases/OAuthAuthenticationUseCaseTest.java 2>/dev/null || echo "Not found"
```

### Step 11.1: Create test directories

```bash
mkdir -p application/src/test/java/com/akademiaplus/magiclink/usecases
mkdir -p application/src/test/java/com/akademiaplus/magiclink/interfaceadapters
mkdir -p application/src/test/java/com/akademiaplus/magiclink/config
```

### Step 11.2: Create MagicLinkVerificationUseCaseTest

**File**: `application/src/test/java/com/akademiaplus/magiclink/usecases/MagicLinkVerificationUseCaseTest.java`

- `@ExtendWith(MockitoExtension.class)`
- All dependencies mocked
- Constants for test values
- `@Captor ArgumentCaptor<MagicLinkTokenDataModel> tokenCaptor`
- `@Captor ArgumentCaptor<PersonPIIDataModel> piiCaptor`

**@Nested ExistingUser**:

- `shouldIssueJwt_whenUserExistsByEmail`:
  - Given: token found, not expired, not used; PersonPII found by emailHash
  - When: `verifyMagicLink(dto)`
  - Then: `jwtTokenProvider.createToken()` called, returns `AuthTokenResponseDTO`

- `shouldMarkTokenAsUsed_whenVerificationSucceeds`:
  - Given: valid token
  - When: `verifyMagicLink(dto)`
  - Then: `token.getUsedAt()` is not null

**@Nested NewUser**:

- `shouldCreateAdultStudent_whenEmailNotFound`:
  - Given: token found; PersonPII NOT found by emailHash
  - When: `verifyMagicLink(dto)`
  - Then: `applicationContext.getBean(AdultStudentDataModel.class)` called

- `shouldSetPlaceholderValues_whenCreatingNewUser`:
  - Given: new user
  - When: `verifyMagicLink(dto)`
  - Then: PersonPII captor shows `PLACEHOLDER_PHONE`, `PLACEHOLDER_ADDRESS`, `PLACEHOLDER_ZIP`

- `shouldIssueJwt_whenNewUserCreated`:
  - Given: new user path
  - When: `verifyMagicLink(dto)`
  - Then: `jwtTokenProvider.createToken()` called with email as subject

- `shouldSetProviderToMagicLink_whenCreatingCustomerAuth`:
  - Given: new user path
  - When: `verifyMagicLink(dto)`
  - Then: `CustomerAuthDataModel.provider` = `PROVIDER_MAGIC_LINK`

**@Nested TokenValidation**:

- `shouldThrowTokenNotFoundException_whenTokenHashNotFound`:
  - Given: `findByTokenHash()` returns empty
  - When/Then: throws `MagicLinkTokenNotFoundException`

- `shouldThrowTokenExpiredException_whenTokenExpired`:
  - Given: token found, `expiresAt` is in the past
  - When/Then: throws `MagicLinkTokenExpiredException`

- `shouldThrowTokenAlreadyUsedException_whenTokenAlreadyUsed`:
  - Given: token found, `usedAt` is not null
  - When/Then: throws `MagicLinkTokenAlreadyUsedException`

**@Nested TenantContext**:

- `shouldSetTenantId_whenVerifying`:
  - Then: `tenantContextHolder.setTenantId(dto.getTenantId())` called

**@Nested TokenHashing**:

- `shouldHashReceivedToken_whenVerifying`:
  - Then: `hashingService.generateHash(dto.getToken())` called before repository lookup

### Step 11.3: Create MagicLinkControllerTest

**File**: `application/src/test/java/com/akademiaplus/magiclink/interfaceadapters/MagicLinkControllerTest.java`

- Standalone MockMvc: `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(advice).build()`
- `@Mock MagicLinkRequestUseCase`, `@Mock MagicLinkVerificationUseCase`

**@Nested RequestEndpoint**:

- `shouldReturn200_whenMagicLinkRequested`:
  - POST `/v1/security/login/magic-link/request` with valid JSON
  - Assert: 200 status, empty body

**@Nested VerifyEndpoint**:

- `shouldReturn200WithToken_whenVerificationSucceeds`:
  - POST `/v1/security/login/magic-link/verify` with valid token
  - Mock use case returns `AuthTokenResponseDTO`
  - Assert: 200 with token in body

- `shouldReturn401_whenTokenNotFound`:
  - Mock use case throws `MagicLinkTokenNotFoundException`
  - Assert: 401

- `shouldReturn401_whenTokenExpired`:
  - Mock throws `MagicLinkTokenExpiredException`
  - Assert: 401

- `shouldReturn401_whenTokenAlreadyUsed`:
  - Mock throws `MagicLinkTokenAlreadyUsedException`
  - Assert: 401

### Step 11.4: Create MagicLinkControllerAdviceTest

**File**: `application/src/test/java/com/akademiaplus/magiclink/config/MagicLinkControllerAdviceTest.java`

Follow the `SecurityControllerAdviceTest` pattern:

**@Nested TokenNotFound**:
- `shouldReturn401_whenMagicLinkTokenNotFoundExceptionThrown`

**@Nested TokenExpired**:
- `shouldReturn401_whenMagicLinkTokenExpiredExceptionThrown`

**@Nested TokenAlreadyUsed**:
- `shouldReturn401_whenMagicLinkTokenAlreadyUsedExceptionThrown`

### Step 11.5: Compile + test

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl application -am -f platform-core-api/pom.xml
```

### Step 11.6: Commit

```bash
git add application/src/test/java/com/akademiaplus/magiclink/
git commit -m "test(application): add magic link verification, controller, and advice unit tests

MagicLinkVerificationUseCaseTest — covers existing user, new user
creation, token validation (not found, expired, used), tenant context.
MagicLinkControllerTest — covers HTTP 200 and 401 responses.
MagicLinkControllerAdviceTest — covers exception-to-HTTP mapping."
```

---

## Phase 12: Component Tests

### Read first

```bash
find application/src/test -name "*ComponentTest.java" | head -5
cat $(find application/src/test -name "*ComponentTest.java" | head -1)
find application/src/test -name "AbstractIntegrationTest.java" | head -1
cat $(find application/src/test -name "AbstractIntegrationTest.java" | head -1)
```

Understand the component test base class and Testcontainers setup.

### Step 12.1: Create MagicLinkComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/MagicLinkComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`
- `@MockitoBean EmailDeliveryChannelStrategy` — capture arguments
- `@MockitoBean RateLimiterService` — stub `checkRateLimit()` to return `RateLimitResult(true, 3, 2, futureEpoch)`
- `@Autowired MockMvc mockMvc`
- `@Autowired MagicLinkTokenRepository magicLinkTokenRepository`
- `@ArgumentCaptor<NotificationDataModel>` to capture email content

**@Nested RequestAndVerify** (full flow):

- `shouldReturn200_whenRequestingMagicLink`:
  - POST request endpoint with email + tenantId
  - Assert: 200 status

- `shouldReturn200WithJwt_whenVerifyingValidToken`:
  - POST request endpoint → capture email content → extract token from URL
  - POST verify endpoint with extracted token
  - Assert: 200 with `token` in JSON body

- `shouldCreateAdultStudentInDatabase_whenNewUserVerifies`:
  - Full flow (request + verify)
  - Assert: `AdultStudentDataModel` exists in DB for that email

**@Nested ExistingUser**:

- `shouldReturn200WithJwt_whenExistingUserVerifiesMagicLink`:
  - Pre-create PersonPII + CustomerAuth + AdultStudent in `@BeforeEach`
  - Request + verify magic link
  - Assert: 200 with JWT

- `shouldNotCreateDuplicateUser_whenExistingUserVerifies`:
  - Pre-create user, run magic link flow
  - Assert: still exactly 1 PersonPII, 1 AdultStudent

**@Nested TokenExpiry**:

- `shouldReturn401_whenTokenIsExpired`:
  - Directly insert `MagicLinkTokenDataModel` with `expiresAt` in the past
  - POST verify with that token hash
  - Assert: 401

**@Nested SingleUse**:

- `shouldReturn401_whenTokenIsUsedTwice`:
  - Full flow: request → verify (success) → verify again (same token)
  - Assert: first verify = 200, second verify = 401

**@Nested InvalidToken**:

- `shouldReturn401_whenTokenHashDoesNotExist`:
  - POST verify with random token
  - Assert: 401

### Step 12.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

### Step 12.3: Commit

```bash
git add application/src/test/java/com/akademiaplus/usecases/MagicLinkComponentTest.java
git commit -m "test(application): add magic link authentication component tests

MagicLinkComponentTest — full Spring context + Testcontainers
MariaDB. Covers full request-verify flow, existing user login,
token expiry, single-use enforcement, and invalid token
handling. Email delivery mocked."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — request use case tests green
- [ ] `mvn test -pl application -am -f platform-core-api/pom.xml` — verification use case + controller tests green
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests green
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, `shouldDoX_whenY()`, zero `any()` matchers
- [ ] `MagicLinkTokenDataModel` in multi-tenant-data with composite key (tenantId, magicLinkTokenId)
- [ ] `MagicLinkRequestUseCase` in security module (single-module, Hard Rule #12)
- [ ] `MagicLinkVerificationUseCase` in application module (cross-module, Hard Rule #14)
- [ ] No `new EntityDataModel()` — all via `applicationContext.getBean()`
- [ ] Request endpoint always returns 200 (anti-enumeration)
- [ ] Token stored as SHA-256 hash only — raw token never persisted
- [ ] Single-use enforcement via `usedAt` timestamp
- [ ] Token expiry configurable (default 10 minutes)
- [ ] Rate limiting: 3 requests per email per hour
- [ ] Magic link URL includes tenant ID
- [ ] Both endpoints permitted in SecurityConfig
- [ ] Conventional Commits format, no AI attribution
