# OAuth2 Social Login — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Spec**: `docs/workflows/pending/oauth-social-login-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 → 2 → ... → 14). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header.
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals → `public static final` constants, shared between impl and tests.
9. Use `applicationContext.getBean()` for all entity instantiation — never `new EntityDataModel()`.
10. Read existing files BEFORE modifying — field names, import paths, and CompositeId class names vary.
11. Commit after each phase using the commit message provided.

---

## Phase 1: Entity Enhancement

### Read first

```bash
cat multi-tenant-data/src/main/java/com/akademiaplus/security/CustomerAuthDataModel.java
cat utilities/src/main/java/com/akademiaplus/utilities/security/AESGCMEncryptionService.java
```

Find the `StringEncryptor` converter class:
```bash
grep -rn "class StringEncryptor\|class.*AttributeConverter.*String" multi-tenant-data/src/main/java/ utilities/src/main/java/
```

### Step 1.1: Modify CustomerAuthDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/security/CustomerAuthDataModel.java`

Add two new fields after the existing `token` field:

```java
@Convert(converter = StringEncryptor.class)
@Column(name = "encrypted_provider_user_id", length = 500)
private String providerUserId;

@Column(name = "provider_user_id_hash", length = 64)
private String providerUserIdHash;
```

**IMPORTANT**: Find the exact `StringEncryptor` class name by reading the codebase — it may be `StringAttributeConverter` or similar. Use the same converter pattern used by existing encrypted fields (e.g., in `PersonPIIDataModel`).

### Step 1.2: Modify DB schema

Find the schema file:
```bash
find . -name "00-schema*.sql" -o -name "*.sql" | grep -i "db_init\|schema\|migration" | head -10
```

Add the columns and index to the `customer_auths` table definition.

### Step 1.3: Compile

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
```

### Step 1.4: Commit

```bash
git add multi-tenant-data/ db_init/ infra-common/
git commit -m "feat(multi-tenant-data): add providerUserId fields to CustomerAuthDataModel

Add encrypted_provider_user_id (AES-GCM encrypted) and
provider_user_id_hash (SHA-256, indexed) columns for OAuth
returning-user lookup."
```

---

## Phase 2: OpenAPI Specification

### Read first

```bash
cat security/src/main/resources/openapi/internal-authentication.yaml
cat security/src/main/resources/openapi/security-module.yaml
```

Note the exact schema names for `AuthTokenResponse` and `ErrorResponse`, and how paths/schemas are referenced.

### Step 2.1: Create oauth-authentication.yaml

**File**: `security/src/main/resources/openapi/oauth-authentication.yaml`

```yaml
openapi: 3.1.0
info:
  title: OAuth Authentication API
  version: 1.0.0
  description: OAuth2 social login endpoints for customer-facing users

paths:
  /login/oauth:
    post:
      summary: Authenticate via OAuth2 provider
      description: >
        Exchanges an OAuth2 authorization code for a platform JWT.
        The frontend handles the OAuth redirect/consent flow and sends
        the authorization code to this endpoint.
      operationId: loginOauth
      tags:
        - oauth
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/OAuthLoginRequest'
      responses:
        '200':
          description: Authentication successful
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/AuthTokenResponse'
        '400':
          description: Unsupported provider or invalid request
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/ErrorResponse'
        '401':
          description: OAuth provider authentication failed
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/ErrorResponse'

components:
  schemas:
    OAuthLoginRequest:
      type: object
      properties:
        provider:
          type: string
          description: OAuth2 provider name
          enum:
            - google
            - facebook
        authorizationCode:
          type: string
          description: Authorization code from the OAuth2 provider
        redirectUri:
          type: string
          format: uri
          description: Redirect URI used in the OAuth2 authorization request
        tenantId:
          type: integer
          format: int64
          description: Tenant ID for the login context
      required:
        - provider
        - authorizationCode
        - redirectUri
        - tenantId
```

### Step 2.2: Modify security-module.yaml

Add the OAuth path and schema references following the same pattern as the existing `internal-authentication.yaml` references. Read the file first to match the exact structure.

### Step 2.3: Regenerate DTOs

```bash
mvn clean generate-sources -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 2.4: Verify generated code

```bash
find security/target/generated-sources -name "OAuthLoginRequestDTO.java" | head -1
find security/target/generated-sources -name "OauthApi.java" -o -name "OAuthApi.java" | head -1
```

Read both files and confirm:
- `OAuthLoginRequestDTO` has `getProvider()`, `getAuthorizationCode()`, `getRedirectUri()`, `getTenantId()`
- The API interface has a `loginOauth()` method

**IMPORTANT**: Note the exact generated interface name (could be `OauthApi` or `OAuthApi` depending on the generator config). Use this exact name in Phase 9.

### Step 2.5: Commit

```bash
git add security/src/main/resources/openapi/
git commit -m "api(security): add OAuth login OpenAPI specification

Add POST /login/oauth endpoint with OAuthLoginRequest schema
(provider enum, authorizationCode, redirectUri, tenantId).
Reuses AuthTokenResponse for 200 response."
```

---

## Phase 3: Security Config

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

Identify the exact line where `/v1/security/login/internal` is permitted and where CORS is configured.

### Step 3.1: Add permit rule

Add `.requestMatchers("/v1/security/login/oauth").permitAll()` on the line after (or alongside) the existing `/login/internal` permit rule.

### Step 3.2: Add CORS rule

Add `source.registerCorsConfiguration("/v1/security/login/oauth", loginCorsConfig)` alongside the existing CORS configuration for `/login/internal`. Use the same `CorsConfiguration` object.

### Step 3.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.4: Commit

```bash
git add security/src/main/java/
git commit -m "feat(security): permit OAuth login endpoint and add CORS rule

Add /v1/security/login/oauth to permitAll and CORS config
alongside the existing internal login rules."
```

---

## Phase 4: Domain Objects

### Step 4.1: Create directory structure

```bash
mkdir -p security/src/main/java/com/akademiaplus/oauth/usecases/domain
```

### Step 4.2: OAuthTokenResponse

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/domain/OAuthTokenResponse.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.usecases.domain;

/**
 * Represents the token response from an OAuth2 provider's token endpoint.
 *
 * @param accessToken the access token issued by the provider
 * @param tokenType   the token type (typically "Bearer")
 * @param expiresIn   token lifetime in seconds
 * @author ElatusDev
 * @since 1.0
 */
public record OAuthTokenResponse(String accessToken, String tokenType, long expiresIn) {
}
```

### Step 4.3: OAuthUserProfile

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/domain/OAuthUserProfile.java`

```java
/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.usecases.domain;

/**
 * Represents a user profile fetched from an OAuth2 provider.
 *
 * @param email          the user's email address
 * @param firstName      the user's first name
 * @param lastName       the user's last name
 * @param provider       the provider name (e.g., "google", "facebook")
 * @param providerUserId the unique user identifier assigned by the provider
 * @author ElatusDev
 * @since 1.0
 */
public record OAuthUserProfile(
        String email,
        String firstName,
        String lastName,
        String provider,
        String providerUserId
) {
}
```

### Step 4.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 4.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/oauth/
git commit -m "feat(security): add OAuth domain records

Add OAuthTokenResponse and OAuthUserProfile records
in oauth/usecases/domain/ for provider API responses."
```

---

## Phase 5: Provider Strategy Pattern

### Read first

```bash
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/DeliveryChannelStrategy.java
```

This is the existing strategy pattern to follow.

### Step 5.1: OAuthProviderStrategy

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/OAuthProviderStrategy.java`

Interface with three methods:
- `String getProviderName()`
- `OAuthTokenResponse exchangeCodeForToken(String authorizationCode, String redirectUri)`
- `OAuthUserProfile fetchUserProfile(String accessToken)`

### Step 5.2: GoogleOAuthProviderStrategy

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/GoogleOAuthProviderStrategy.java`

- `@Service`
- Constructor: `RestClient restClient`, `@Value("${oauth.google.client-id}") String clientId`, `@Value("${oauth.google.client-secret}") String clientSecret`
- `getProviderName()` returns constant `PROVIDER_NAME = "google"`
- `exchangeCodeForToken()`: POST to `https://oauth2.googleapis.com/token` with form-encoded body (`grant_type=authorization_code`, `code`, `redirect_uri`, `client_id`, `client_secret`). Parse JSON response for `access_token`, `token_type`, `expires_in`. Wrap errors in `OAuthProviderException`.
- `fetchUserProfile()`: GET `https://www.googleapis.com/oauth2/v3/userinfo` with `Authorization: Bearer {token}`. Parse JSON for `sub` (providerUserId), `email`, `given_name`, `family_name`. Wrap errors in `OAuthProviderException`.

**Constants**: All URL strings, error messages, and JSON field names must be `public static final`.

### Step 5.3: FacebookOAuthProviderStrategy

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/FacebookOAuthProviderStrategy.java`

Same pattern as Google with Facebook-specific endpoints:
- `exchangeCodeForToken()`: GET `https://graph.facebook.com/v19.0/oauth/access_token?client_id=...&client_secret=...&code=...&redirect_uri=...`
- `fetchUserProfile()`: GET `https://graph.facebook.com/v19.0/me?fields=id,first_name,last_name,email` with Bearer token
- Provider user ID from field `id` (not `sub`)

### Step 5.4: OAuthProviderRegistry

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/OAuthProviderRegistry.java`

- `@Service`
- Constructor: `Set<OAuthProviderStrategy> strategies` — Spring auto-injects all implementations
- Build `Map<String, OAuthProviderStrategy>` from the set using `getProviderName()` as key
- `resolve(String providerName)`: lookup or throw `UnsupportedProviderException`

### Step 5.5: OAuthConfiguration

**File**: `security/src/main/java/com/akademiaplus/oauth/interfaceadapters/config/OAuthConfiguration.java`

```java
@Configuration
public class OAuthConfiguration {
    @Bean
    public RestClient oAuthRestClient() {
        return RestClient.create();
    }
}
```

### Step 5.6: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 5.7: Commit

```bash
git add security/src/main/java/com/akademiaplus/oauth/
git commit -m "feat(security): implement OAuth provider strategy pattern

Add OAuthProviderStrategy interface with Google and Facebook
implementations. Add OAuthProviderRegistry for provider resolution.
Add RestClient bean for OAuth HTTP calls."
```

---

## Phase 6: Exceptions

### Step 6.1: OAuthProviderException

**File**: `security/src/main/java/com/akademiaplus/oauth/exceptions/OAuthProviderException.java`

```java
public class OAuthProviderException extends RuntimeException {
    public static final String ERROR_CODE_EXCHANGE_FAILED = "Failed to exchange authorization code with %s: %s";
    public static final String ERROR_PROFILE_FETCH_FAILED = "Failed to fetch user profile from %s: %s";
    // constructor with message + cause
}
```

### Step 6.2: UnsupportedProviderException

**File**: `security/src/main/java/com/akademiaplus/oauth/exceptions/UnsupportedProviderException.java`

```java
public class UnsupportedProviderException extends RuntimeException {
    public static final String ERROR_UNSUPPORTED_PROVIDER = "Unsupported OAuth provider: %s";
    // constructor with provider name
}
```

### Step 6.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 6.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/oauth/exceptions/
git commit -m "feat(security): add OAuth exception types

Add OAuthProviderException for provider communication failures
and UnsupportedProviderException for unknown provider names."
```

---

## Phase 7: Repository Methods

### Read first

```bash
cat user-management/src/main/java/com/akademiaplus/customer/interfaceadapters/CustomerAuthRepository.java
cat user-management/src/main/java/com/akademiaplus/customer/adultstudent/interfaceadapters/AdultStudentRepository.java
cat multi-tenant-data/src/main/java/com/akademiaplus/users/customer/AdultStudentDataModel.java
```

Check how `customerAuth` is stored on `AdultStudentDataModel` — is it a `@OneToOne` with a `customerAuthId` FK column, or is `CustomerAuthDataModel` embedded? This determines the correct repository query method.

### Step 7.1: CustomerAuthRepository

Add:
```java
Optional<CustomerAuthDataModel> findByProviderAndProviderUserIdHash(String provider, String providerUserIdHash);
```

### Step 7.2: PersonPIIRepository

**File**: `user-management/src/main/java/com/akademiaplus/customer/interfaceadapters/PersonPIIRepository.java`

Read the file first, then add:
```java
Optional<PersonPIIDataModel> findByEmailHash(String emailHash);
```

This enables the account-linking branch — when an OAuth email matches an existing internally-registered user, the system links the OAuth credentials to the existing account instead of creating a duplicate.

### Step 7.3: AdultStudentRepository

Add methods to find an AdultStudent by its associated CustomerAuth **and** by its PersonPII. The exact method signatures depend on how the relationships are mapped. Options:

**findByCustomerAuthId** (returning-user branch):
- If `AdultStudentDataModel` has a `customerAuth` field: `Optional<AdultStudentDataModel> findByCustomerAuth(CustomerAuthDataModel customerAuth)`
- If it has a FK column: `Optional<AdultStudentDataModel> findByCustomerAuth_CustomerAuthId(Long customerAuthId)` (Spring Data nested property traversal)
- If neither works, use `@Query`

**findByPersonPiiId** (account-linking branch):
- If `AdultStudentDataModel` has a `personPii` field: `Optional<AdultStudentDataModel> findByPersonPii(PersonPIIDataModel personPii)`
- If it has a FK column: `Optional<AdultStudentDataModel> findByPersonPii_PersonPiiId(Long personPiiId)` (Spring Data nested property traversal)
- If neither works, use `@Query`

**Read the entity first** to determine the correct approach for both methods.

### Step 7.4: Compile

```bash
mvn clean compile -pl user-management -am -DskipTests -f platform-core-api/pom.xml
```

### Step 7.5: Commit

```bash
git add user-management/src/main/java/
git commit -m "feat(user-management): add OAuth lookup repository methods

Add findByProviderAndProviderUserIdHash to CustomerAuthRepository,
findByEmailHash to PersonPIIRepository, and findByCustomerAuthId +
findByPersonPiiId to AdultStudentRepository for OAuth returning-user
lookup and account linking."
```

---

## Phase 8: OAuthAuthenticationUseCase

### Read first

```bash
cat user-management/src/main/java/com/akademiaplus/customer/adultstudent/usecases/AdultStudentCreationUseCase.java
cat security/src/main/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCase.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
cat utilities/src/main/java/com/akademiaplus/utilities/security/HashingService.java
cat utilities/src/main/java/com/akademiaplus/utilities/security/PiiNormalizer.java
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/config/TenantContextHolder.java
```

Understand:
- How `AdultStudentCreationUseCase.transform()` builds PersonPII + CustomerAuth + AdultStudent
- How `JwtTokenProvider.createToken(username, tenantId, claims)` works
- How `HashingService.generateHash()` works
- How `PiiNormalizer.normalizeEmail()` works
- How `TenantContextHolder.setTenantId()` works

### Step 8.1: Create OAuthAuthenticationUseCase

**File**: `application/src/main/java/com/akademiaplus/oauth/usecases/OAuthAuthenticationUseCase.java`

**Package**: `com.akademiaplus.oauth.usecases`

**Dependencies** (all constructor-injected):
- `OAuthProviderRegistry`
- `CustomerAuthRepository`
- `AdultStudentRepository`
- `PersonPIIRepository`
- `JwtTokenProvider`
- `HashingService`
- `PiiNormalizer`
- `TenantContextHolder`
- `ApplicationContext`

**Constants**:
```java
public static final String PLACEHOLDER_PHONE = "PENDING_UPDATE";
public static final String PLACEHOLDER_ADDRESS = "PENDING_UPDATE";
public static final String PLACEHOLDER_ZIP = "PENDING_UPDATE";
public static final String JWT_CLAIM_ROLE = "Has role";
public static final String ROLE_CUSTOMER = "CUSTOMER";
```

**Method `authenticate(OAuthLoginRequestDTO dto)`** — `@Transactional`:

1. `tenantContextHolder.setTenantId(dto.getTenantId())`
2. `OAuthProviderStrategy strategy = registry.resolve(dto.getProvider().getValue())`
3. `OAuthTokenResponse tokenResponse = strategy.exchangeCodeForToken(dto.getAuthorizationCode(), dto.getRedirectUri().toString())`
4. `OAuthUserProfile profile = strategy.fetchUserProfile(tokenResponse.accessToken())`
5. `String providerUserIdHash = hashingService.generateHash(profile.providerUserId())`
6. `Optional<CustomerAuthDataModel> existingAuth = customerAuthRepository.findByProviderAndProviderUserIdHash(profile.provider(), providerUserIdHash)`
7. If present → **Branch 1: returning OAuth user**
8. If empty → hash email, lookup `PersonPII` by emailHash
9. If PersonPII found → **Branch 2: existing email, new provider (account linking)**
10. If PersonPII not found → **Branch 3: brand new user**

**Branch 1 — Returning OAuth user** (CustomerAuth found by provider + providerUserIdHash):
- Update `existingAuth.token` with new access token
- Find `AdultStudentDataModel` by customerAuth
- Find `PersonPIIDataModel` from student → get email
- Build JWT claims map with role
- `jwtTokenProvider.createToken(email, dto.getTenantId(), claims)`

**Branch 2 — Existing email, new provider (account linking)** (CustomerAuth NOT found, but PersonPII exists by emailHash):
- `String emailHash = hashingService.generateHash(piiNormalizer.normalizeEmail(profile.email()))`
- `Optional<PersonPIIDataModel> existingPii = personPIIRepository.findByEmailHash(emailHash)`
- If present: find the existing `AdultStudentDataModel` via `adultStudentRepository.findByPersonPiiId(existingPii.get().getId())`
- Create new `CustomerAuthDataModel` via `applicationContext.getBean()` — set provider, token, providerUserId (encrypted), providerUserIdHash
- Link it to the existing `AdultStudentDataModel` (set `customerAuth` on the student, or save independently depending on entity mapping)
- Build JWT with the existing email and return

**Branch 3 — Brand new user** (neither CustomerAuth nor PersonPII found):
- Create `PersonPIIDataModel` via `applicationContext.getBean()` — set firstName, lastName, normalizedEmail, emailHash, placeholder phone/address/zip
- Create `CustomerAuthDataModel` via `applicationContext.getBean()` — set provider, token, providerUserId (encrypted), providerUserIdHash
- Create `AdultStudentDataModel` via `applicationContext.getBean()` — set personPII, customerAuth, entryDate
- The repository `saveAndFlush` cascades all three entities
- Build JWT and return

### Step 8.2: Compile

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

### Step 8.3: Commit

```bash
git add application/src/main/java/com/akademiaplus/oauth/
git commit -m "feat(application): implement OAuthAuthenticationUseCase

Add cross-module orchestrator for OAuth login flow with 3-branch logic:
- Returning OAuth user: update token, issue platform JWT
- Existing email, new provider: link OAuth credentials to existing account
- Brand new user: auto-create PersonPII + CustomerAuth + AdultStudent
- Exchange authorization code for access token via provider strategy
- Placeholder values for phone/address/zip (PENDING_UPDATE)"
```

---

## Phase 9: OAuthController

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/InternalAuthController.java
```

Follow the same pattern. Read the generated API interface to get the exact method signature.

```bash
find application/target/generated-sources security/target/generated-sources -name "OauthApi.java" -o -name "OAuthApi.java" 2>/dev/null
```

### Step 9.1: Create OAuthController

**File**: `application/src/main/java/com/akademiaplus/oauth/interfaceadapters/OAuthController.java`

- `@RestController @RequestMapping("/v1/security")`
- Implements the generated OAuth API interface (exact name from Phase 2 verification)
- Single `@Override` method delegates to `OAuthAuthenticationUseCase.authenticate(dto)`
- Returns `ResponseEntity.ok(result)`

### Step 9.2: Compile

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

### Step 9.3: Commit

```bash
git add application/src/main/java/com/akademiaplus/oauth/interfaceadapters/
git commit -m "feat(application): add OAuthController

Implement generated OauthApi interface, delegate to
OAuthAuthenticationUseCase for POST /v1/security/login/oauth."
```

---

## Phase 10: OAuthControllerAdvice

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityControllerAdvice.java
cat utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java
```

Follow the same pattern: extend `BaseControllerAdvice`, scope via `basePackageClasses`.

### Step 10.1: Create OAuthControllerAdvice

**File**: `application/src/main/java/com/akademiaplus/oauth/config/OAuthControllerAdvice.java`

- `@ControllerAdvice(basePackageClasses = OAuthController.class)`
- Extends `BaseControllerAdvice`
- `@ExceptionHandler(OAuthProviderException.class)` → 401
- `@ExceptionHandler(UnsupportedProviderException.class)` → 400
- Use `public static final` error code constants (e.g., `CODE_OAUTH_PROVIDER_ERROR`, `CODE_UNSUPPORTED_PROVIDER`)

### Step 10.2: Compile

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

### Step 10.3: Commit

```bash
git add application/src/main/java/com/akademiaplus/oauth/config/
git commit -m "feat(application): add OAuthControllerAdvice

Handle OAuthProviderException (401) and
UnsupportedProviderException (400) scoped to OAuthController."
```

---

## Phase 11: Configuration

### Read first

```bash
cat application/src/main/resources/application.properties
```

### Step 11.1: Add OAuth properties

Add at the end of the file:

```properties
# OAuth2 Provider Credentials
oauth.google.client-id=${GOOGLE_CLIENT_ID}
oauth.google.client-secret=${GOOGLE_CLIENT_SECRET}
oauth.facebook.client-id=${FACEBOOK_CLIENT_ID}
oauth.facebook.client-secret=${FACEBOOK_CLIENT_SECRET}
```

### Step 11.2: Commit

```bash
git add application/src/main/resources/application.properties
git commit -m "feat(application): add OAuth provider configuration properties

Add environment-variable-backed properties for Google and
Facebook OAuth client credentials."
```

---

## Phase 12: Unit Tests — Security Module

### Read first

```bash
cat notification-system/src/test/java/com/akademiaplus/notification/usecases/WebappDeliveryChannelStrategyTest.java
cat notification-system/src/test/java/com/akademiaplus/notification/usecases/SseEmitterRegistryTest.java
```

These are the existing strategy and registry test patterns to follow.

### Step 12.1: Create test directory

```bash
mkdir -p security/src/test/java/com/akademiaplus/oauth/usecases
```

### Step 12.2: GoogleOAuthProviderStrategyTest

**File**: `security/src/test/java/com/akademiaplus/oauth/usecases/GoogleOAuthProviderStrategyTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock RestClient restClient` + mock the fluent builder chain (`RestClient.RequestBodyUriSpec`, `RestClient.RequestHeadersSpec`, `RestClient.ResponseSpec`)
- Constants for all test values: client ID, client secret, auth code, redirect URI, access token, email, name, provider user ID
- `@Nested ProviderIdentity`: `shouldReturnGoogle_whenAskedForProviderName`
- `@Nested CodeExchange`: `shouldReturnTokenResponse_whenGoogleReturnsSuccess`, `shouldThrowOAuthProviderException_whenGoogleReturnsError`
- `@Nested ProfileFetch`: `shouldReturnUserProfile_whenGoogleReturnsValidProfile`, `shouldThrowOAuthProviderException_whenProfileFetchFails`

### Step 12.3: FacebookOAuthProviderStrategyTest

Same structure as Google, adapted for Facebook endpoints and response format.

### Step 12.4: OAuthProviderRegistryTest

**File**: `security/src/test/java/com/akademiaplus/oauth/usecases/OAuthProviderRegistryTest.java`

- No `@ExtendWith(MockitoExtension.class)` needed if using real strategy mocks inline
- Create mock strategies with `getProviderName()` returning "google" and "facebook"
- `@Nested Resolution`: `shouldReturnGoogleStrategy_whenProviderIsGoogle`, `shouldReturnFacebookStrategy_whenProviderIsFacebook`
- `@Nested UnknownProvider`: `shouldThrowUnsupportedProviderException_whenProviderIsUnknown`
- Verify exception message uses `UnsupportedProviderException.ERROR_UNSUPPORTED_PROVIDER`

### Step 12.5: OAuthConfigurationTest

**File**: `security/src/test/java/com/akademiaplus/oauth/interfaceadapters/config/OAuthConfigurationTest.java`

- `@ExtendWith(MockitoExtension.class)`
- Minimal: instantiate `OAuthConfiguration`, call `oAuthRestClient()`, assert not null
- `@Nested RestClientBean`:
  - `shouldReturnNonNullRestClient_whenBeanCreated`

This is a low-complexity test but satisfies CLAUDE.md Hard Rule #11 (one test class per production class).

### Step 12.6: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 12.7: Commit

```bash
git add security/src/test/
git commit -m "test(security): add OAuth provider strategy, registry, and config unit tests

GoogleOAuthProviderStrategyTest, FacebookOAuthProviderStrategyTest,
OAuthProviderRegistryTest, OAuthConfigurationTest — cover code exchange,
profile fetch, provider resolution, config bean creation, and error paths."
```

---

## Phase 13: Unit Tests — Application Module

### Read first

```bash
cat application/src/test/java/com/akademiaplus/usecases/RegistrationUseCaseTest.java
cat billing/src/test/java/com/akademiaplus/membership/interfaceadapters/MembershipControllerTest.java
```

Follow the RegistrationUseCaseTest pattern for the orchestrator use case test, and the MembershipControllerTest pattern for standalone MockMvc controller testing.

### Step 13.1: OAuthAuthenticationUseCaseTest

**File**: `application/src/test/java/com/akademiaplus/oauth/usecases/OAuthAuthenticationUseCaseTest.java`

- `@ExtendWith(MockitoExtension.class)`
- Constructor injection in `@BeforeEach`
- All dependencies mocked: `OAuthProviderRegistry`, `CustomerAuthRepository`, `AdultStudentRepository`, `PersonPIIRepository`, `JwtTokenProvider`, `HashingService`, `PiiNormalizer`, `TenantContextHolder`, `ApplicationContext`
- `buildDto()` helper method returning `OAuthLoginRequestDTO`
- `@Captor ArgumentCaptor` for entities passed to `saveAndFlush`

**@Nested classes**:

| @Nested | Tests |
|---------|-------|
| `ReturningUser` | `shouldIssueJwt_whenUserExistsByProviderHash`, `shouldUpdateToken_whenReturningUser` |
| `ExistingEmailNewProvider` | `shouldLinkOAuthToExistingAccount_whenEmailMatchesInternalRegistration`, `shouldCreateNewCustomerAuth_whenExistingEmailNewProvider`, `shouldIssueJwt_whenAccountLinked` |
| `NewUser` | `shouldCreateAdultStudent_whenFirstTimeOAuthLogin`, `shouldSetPlaceholderValues_whenCreatingNewUser`, `shouldIssueJwt_whenNewUserCreated` |
| `TenantContext` | `shouldSetTenantId_whenAuthenticating` |
| `ProviderResolution` | `shouldDelegateToRegistry_whenResolvingProvider` |
| `ErrorPaths` | `shouldPropagateOAuthProviderException_whenCodeExchangeFails`, `shouldPropagateUnsupportedProviderException_whenProviderUnknown` |

### Step 13.2: OAuthControllerTest

**File**: `application/src/test/java/com/akademiaplus/oauth/interfaceadapters/OAuthControllerTest.java`

- Standalone MockMvc: `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(advice).build()`
- `@Mock OAuthAuthenticationUseCase`
- `@Nested` per scenario:
  - `shouldReturn200WithToken_whenAuthenticationSucceeds`
  - `shouldReturn401_whenOAuthProviderExceptionThrown`
  - `shouldReturn400_whenUnsupportedProviderExceptionThrown`
- `verifyNoMoreInteractions(useCase)` after every test

### Step 13.3: OAuthControllerAdviceTest

**File**: `application/src/test/java/com/akademiaplus/oauth/config/OAuthControllerAdviceTest.java`

### Read first

```bash
cat utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java
cat security/src/test/java/com/akademiaplus/config/SecurityControllerAdviceTest.java
```

Follow the `BaseControllerAdviceTest` pattern (if it exists) or the `SecurityControllerAdviceTest` pattern:
- Create a concrete test subclass extending `OAuthControllerAdvice` (or test the advice directly)
- `@ExtendWith(MockitoExtension.class)`
- `@Mock MessageService` (if `BaseControllerAdvice` requires it)
- `@Nested OAuthProviderExceptionHandling`:
  - `shouldReturn401WithOAuthProviderErrorCode_whenOAuthProviderExceptionThrown`
  - Verify response status is 401 and error code matches `CODE_OAUTH_PROVIDER_ERROR`
- `@Nested UnsupportedProviderExceptionHandling`:
  - `shouldReturn400WithUnsupportedProviderCode_whenUnsupportedProviderExceptionThrown`
  - Verify response status is 400 and error code matches `CODE_UNSUPPORTED_PROVIDER`

This satisfies CLAUDE.md Hard Rule #11 (one test class per production class).

### Step 13.4: Compile + test

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl application -am -f platform-core-api/pom.xml
```

### Step 13.5: Commit

```bash
git add application/src/test/
git commit -m "test(application): add OAuth use case, controller, and advice unit tests

OAuthAuthenticationUseCaseTest — covers returning user, account linking,
new user auto-creation, tenant context, provider resolution, and error paths.
OAuthControllerTest — covers HTTP 200, 401, and 400 responses.
OAuthControllerAdviceTest — covers OAuthProviderException (401) and
UnsupportedProviderException (400) handling."
```

---

## Phase 14: Component Tests — Application Module

### Read first

```bash
find application/src/test -name "*ComponentTest.java" | head -5
cat <first-result>
find application/src/test -name "AbstractIntegrationTest.java" | head -1
cat <result>
```

Understand the component test infrastructure: `AbstractIntegrationTest`, `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles`, `MockMvc`.

### Step 14.1: OAuthComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/OAuthComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`, `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`
- `@MockitoBean` (or `@MockBean`) for `GoogleOAuthProviderStrategy` and `FacebookOAuthProviderStrategy` — prevents real HTTP calls
- Creates tenant + tenant sequences in `@BeforeEach`
- Stubs strategy mocks to return valid `OAuthTokenResponse` and `OAuthUserProfile`

**@Nested classes**:

| @Nested | Tests |
|---------|-------|
| `NewUserLogin` | `shouldReturn200WithJwt_whenNewUserLogsInViaOAuth`, `shouldCreateAdultStudentInDatabase_whenNewUserLogsIn` |
| `ReturningUserLogin` | `shouldReturn200WithJwt_whenReturningUserLogsIn`, `shouldNotCreateDuplicateUser_whenReturningUserLogsIn` |
| `ExistingEmailNewProvider` | `shouldReturn200WithJwt_whenOAuthEmailMatchesExistingInternalUser`, `shouldLinkOAuthCredentialsToExistingAdultStudent_whenEmailCollision`, `shouldNotCreateDuplicatePersonPII_whenOAuthEmailMatchesExistingUser` |
| `ConcurrentLogin` | `shouldCreateOnlyOneUser_whenTwoConcurrentOAuthLoginsWithSameProviderUserId` |
| `ErrorPaths` | `shouldReturn400_whenProviderIsUnsupported`, `shouldReturn401_whenProviderCommunicationFails` |

**ExistingEmailNewProvider setup**:
- In `@BeforeEach`, create an internally-registered user (PersonPII + CustomerAuth + AdultStudent) with a known email
- Stub the OAuth provider strategy to return a profile with the same email but a new providerUserId
- POST to `/v1/security/login/oauth` → should succeed with 200 + JWT
- Assert: no new PersonPII created, new CustomerAuth created, existing AdultStudent reused

**ConcurrentLogin setup**:
- Use `ExecutorService` with 2 threads + `CountDownLatch` for synchronized start
- Both threads submit `POST /v1/security/login/oauth` with the same providerUserId
- Assert: exactly 1 `PersonPII` + 1 `AdultStudent` + 1 `CustomerAuth` in DB
- The use case's `@Transactional` + pessimistic lock on `findByProviderAndProviderUserIdHash()` / `findByEmailHash()` prevents duplicate creation
- **Note**: If the use case does not yet use pessimistic locking, this test may fail — it serves as a regression test to ensure the lock is added

### Step 14.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

### Step 14.3: Commit

```bash
git add application/src/test/
git commit -m "test(application): add OAuth component test

OAuthComponentTest — full Spring context + Testcontainers MariaDB.
Covers new user login, returning user login, account linking
(existing email + new provider), concurrent login race condition,
unsupported provider, and provider communication failure.
External providers mocked."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — strategy + registry tests green
- [ ] `mvn test -pl application -am -f platform-core-api/pom.xml` — use case + controller tests green
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests green
- [ ] All new files have ElatusDev copyright header
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] Strategy interface in `usecases/` with implementations alongside (Hard Rule #12)
- [ ] Domain records in `usecases/domain/` (Hard Rule #13)
- [ ] Cross-module orchestrator in `application` module only (Hard Rule #14)
- [ ] No `new EntityDataModel()` — all via `applicationContext.getBean()`
