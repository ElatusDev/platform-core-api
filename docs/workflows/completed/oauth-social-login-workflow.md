# OAuth2 Social Login Workflow — Google & Facebook

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.

---

## 1. Current State Analysis

### 1.1 What Exists

The platform supports internal authentication (username/password) for employees via `InternalAuthenticationUseCase`. Customer-facing users (AdultStudent, Tutor, MinorStudent) have a `CustomerAuthDataModel` entity with `provider` and `token` fields, but no OAuth flow is implemented.

| Component | Location | State |
|-----------|----------|-------|
| `CustomerAuthDataModel` | `multi-tenant-data/.../security/` | Has `provider` (VARCHAR 50) + `token` (TEXT) — no `providerUserId` or hash column |
| `InternalAuthenticationUseCase` | `security/.../internal/usecases/` | Working — exchanges username/password for JWT |
| `InternalAuthController` | `security/.../internal/interfaceadapters/` | Implements `LoginApi`, mapped to `/v1/security/login/internal` |
| `JwtTokenProvider` | `security/.../internal/interfaceadapters/jwt/` | `createToken(username, tenantId, additionalClaims)` — signs with EC/RSA key |
| `SecurityConfig` | `security/.../config/` | Permits `/v1/security/login/internal` and `/v1/security/register` |
| `AdultStudentCreationUseCase` | `user-management/.../customer/adultstudent/usecases/` | Creates PersonPII + CustomerAuth + AdultStudent — sets `provider`/`token` from DTO |
| `CustomerAuthRepository` | `user-management/.../customer/interfaceadapters/` | Extends `TenantScopedRepository` — no provider lookup method |
| `AdultStudentRepository` | `user-management/.../customer/adultstudent/interfaceadapters/` | Extends `TenantScopedRepository` — no customerAuth lookup method |
| `RestClient` / `WebClient` | — | None exist in the codebase |

### 1.2 What's Missing

1. **Provider user identification**: `CustomerAuthDataModel` has no `providerUserId` or hash column — cannot identify returning OAuth users
2. **OAuth token exchange**: No code to exchange authorization codes for access tokens with Google/Facebook
3. **User profile fetch**: No code to retrieve user profiles from OAuth providers
4. **Auto-registration**: No flow to auto-create AdultStudent accounts for first-time OAuth users
5. **OAuth login endpoint**: No `POST /login/oauth` endpoint
6. **HTTP client**: No `RestClient` bean for external API calls
7. **E2E tests**: No OAuth login tests in `platform-api-e2e`

### 1.3 Token Exchange Pattern

The frontend handles the OAuth redirect/consent flow and obtains an authorization code. The API receives this code and:

1. Exchanges it with the provider for an access token
2. Uses the access token to fetch the user's profile
3. Looks up or creates the user in the platform
4. Returns a platform JWT

This pattern keeps OAuth complexity server-side and avoids exposing provider tokens to the frontend beyond the authorization code.

Facebook Login covers Instagram (Meta unified both platforms under the same OAuth flow).

---

## 2. Target Architecture

### 2.1 OAuth Login Flow

```
Client POST /v1/security/login/oauth
  ├── Request: { provider, authorizationCode, redirectUri, tenantId }
  │
  ├── OAuthAuthenticationUseCase.authenticate(dto)
  │     ├── 1. Set tenant context from dto.tenantId
  │     ├── 2. Resolve provider strategy via OAuthProviderRegistry
  │     ├── 3. Exchange authorization code for access token (provider API)
  │     ├── 4. Fetch user profile from provider (provider API)
  │     ├── 5. Hash providerUserId → lookup CustomerAuth by provider + hash
  │     ├── 6a. RETURNING OAUTH USER: update token, load email, issue JWT
  │     ├── 6b. EXISTING EMAIL, NEW PROVIDER: PersonPII found by emailHash
  │     │       → create CustomerAuth linked to existing AdultStudent, issue JWT
  │     └── 6c. BRAND NEW USER: create PersonPII + CustomerAuth + AdultStudent, issue JWT
  │
  └── Response: { token } (AuthTokenResponseDTO — reuses internal auth response)
```

### 2.2 Provider Strategy Pattern

```
OAuthProviderStrategy (interface in usecases/)
  ├── getProviderName(): String
  ├── exchangeCodeForToken(code, redirectUri): OAuthTokenResponse
  └── fetchUserProfile(accessToken): OAuthUserProfile

GoogleOAuthProviderStrategy   ──→ Google OAuth2 endpoints
FacebookOAuthProviderStrategy ──→ Facebook Graph API endpoints

OAuthProviderRegistry         ──→ Maps provider name → strategy instance
```

### 2.3 Provider Endpoints

| Provider | Token Endpoint | Profile Endpoint |
|----------|---------------|-----------------|
| Google | `POST https://oauth2.googleapis.com/token` | `GET https://www.googleapis.com/oauth2/v3/userinfo` |
| Facebook | `GET https://graph.facebook.com/v19.0/oauth/access_token` | `GET https://graph.facebook.com/v19.0/me?fields=id,first_name,last_name,email` |

### 2.4 Module Placement

Per CLAUDE.md Hard Rules #12, #13, #14 and DESIGN.md Section 3.2.8:

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `OAuthProviderStrategy` (interface) | security | `oauth/usecases/` | External service abstraction (Hard Rule #12) |
| `GoogleOAuthProviderStrategy` | security | `oauth/usecases/` | Strategy implementation alongside interface |
| `FacebookOAuthProviderStrategy` | security | `oauth/usecases/` | Strategy implementation alongside interface |
| `OAuthProviderRegistry` | security | `oauth/usecases/` | Use case helper — resolves strategy by name |
| `OAuthTokenResponse` (record) | security | `oauth/usecases/domain/` | Non-entity domain object (Hard Rule #13) |
| `OAuthUserProfile` (record) | security | `oauth/usecases/domain/` | Non-entity domain object (Hard Rule #13) |
| `OAuthConfiguration` (`RestClient` bean) | security | `oauth/interfaceadapters/config/` | Spring config bean (DESIGN.md 3.2.8) |
| `OAuthProviderException` | security | `oauth/exceptions/` | Module-specific exception |
| `UnsupportedProviderException` | security | `oauth/exceptions/` | Module-specific exception |
| `OAuthAuthenticationUseCase` | application | `oauth/usecases/` | Cross-module orchestrator (Hard Rule #14) — calls user-management repos |
| `OAuthController` | application | `oauth/interfaceadapters/` | Controller for the orchestrator |
| `OAuthControllerAdvice` | application | `oauth/config/` | Exception handling scoped to OAuthController |
| OpenAPI spec | security | `src/main/resources/openapi/` | API contract |

### 2.5 Entity Changes

`CustomerAuthDataModel` needs two new fields for returning-user lookup:

| Field | Type | Purpose |
|-------|------|---------|
| `providerUserId` | `String` (encrypted, VARCHAR 500) | Provider's unique user ID — encrypted at rest via `StringEncryptor` |
| `providerUserIdHash` | `String` (VARCHAR 64, indexed) | SHA-256 hash for indexed lookup without decryption |

Composite index: `(tenant_id, provider, provider_user_id_hash, deleted_at)` — enables fast returning-user lookup within tenant.

### 2.6 Placeholder Handling

OAuth providers don't supply phone, address, or zip code. New users created via OAuth will have:

| Field | Value | Constant |
|-------|-------|----------|
| Phone | `"PENDING_UPDATE"` | `OAuthAuthenticationUseCase.PLACEHOLDER_PHONE` |
| Address | `"PENDING_UPDATE"` | `OAuthAuthenticationUseCase.PLACEHOLDER_ADDRESS` |
| Zip code | `"PENDING_UPDATE"` | `OAuthAuthenticationUseCase.PLACEHOLDER_ZIP` |

The frontend should prompt users to complete their profile after first OAuth login.

---

## 3. Execution Phases

### Phase Dependency Graph

```
Phase 1:  Entity enhancement (CustomerAuthDataModel + DB schema)
    ↓
Phase 2:  OpenAPI specification (oauth-authentication.yaml)
    ↓
Phase 3:  Security config (permitAll + CORS for /login/oauth)
    ↓
Phase 4:  Domain objects (OAuthTokenResponse, OAuthUserProfile records)
    ↓
Phase 5:  Provider strategy pattern (interface + Google + Facebook + registry + RestClient)
    ↓
Phase 6:  Exceptions (OAuthProviderException, UnsupportedProviderException)
    ↓
Phase 7:  Repository methods (CustomerAuthRepo + AdultStudentRepo)
    ↓
Phase 8:  OAuthAuthenticationUseCase (application module — orchestrator)
    ↓
Phase 9:  OAuthController (application module)
    ↓
Phase 10: OAuthControllerAdvice (application module)
    ↓
Phase 11: Configuration (application.properties — OAuth client IDs/secrets)
    ↓
Phase 12: Unit tests (security module — strategies, registry)
    ↓
Phase 13: Unit tests (application module — use case, controller)
    ↓
Phase 14: Component tests (application module — full stack OAuth endpoint)
    ↓
Phase 15: E2E tests (platform-api-e2e — Postman/Newman)
```

---

## 4. Phase-by-Phase Implementation

### Phase 1: Entity Enhancement

#### Step 1.1: Modify CustomerAuthDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/security/CustomerAuthDataModel.java`

Add two new fields:

```java
@Convert(converter = StringEncryptor.class)
@Column(name = "encrypted_provider_user_id", length = 500)
private String providerUserId;

@Column(name = "provider_user_id_hash", length = 64)
private String providerUserIdHash;
```

#### Step 1.2: Modify DB schema

**File**: `db_init/00-schema-dev.sql` (or equivalent schema file)

```sql
ALTER TABLE customer_auths
  ADD COLUMN encrypted_provider_user_id VARCHAR(500),
  ADD COLUMN provider_user_id_hash VARCHAR(64);

CREATE INDEX idx_customer_auth_provider_lookup
  ON customer_auths (tenant_id, provider, provider_user_id_hash, deleted_at);
```

#### Step 1.3: Compile check

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 1.4: Commit

```
feat(multi-tenant-data): add providerUserId fields to CustomerAuthDataModel

Add encrypted_provider_user_id (AES-GCM encrypted) and
provider_user_id_hash (SHA-256, indexed) columns for OAuth
returning-user lookup. Add composite index on
(tenant_id, provider, provider_user_id_hash, deleted_at).
```

---

### Phase 2: OpenAPI Specification

#### Step 2.1: Create oauth-authentication.yaml

**File**: `security/src/main/resources/openapi/oauth-authentication.yaml`

Defines:
- `POST /login/oauth`
- Request schema: `OAuthLoginRequest` — `provider` (enum: google, facebook), `authorizationCode` (string), `redirectUri` (string, format: uri), `tenantId` (integer, format: int64)
- Response 200: reuses `AuthTokenResponse` from `internal-authentication.yaml`
- Response 401: `ErrorResponse`
- Response 400: `ErrorResponse`

#### Step 2.2: Modify security-module.yaml

**File**: `security/src/main/resources/openapi/security-module.yaml`

Add:
- `OAuthLoginRequest` schema ref
- `/login/oauth` path ref

#### Step 2.3: Regenerate DTOs

```bash
mvn clean generate-sources -pl security -am -DskipTests -f platform-core-api/pom.xml
```

Verify:
- `OAuthLoginRequestDTO` generated with `getProvider()`, `getAuthorizationCode()`, `getRedirectUri()`, `getTenantId()`
- `OauthApi` interface generated with `loginOauth()` method

#### Step 2.4: Commit

```
api(security): add OAuth login OpenAPI specification

Add POST /login/oauth endpoint with OAuthLoginRequest schema
(provider enum, authorizationCode, redirectUri, tenantId).
Reuses AuthTokenResponse for 200 response.
```

---

### Phase 3: Security Config

#### Step 3.1: Add permit rule

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add `.requestMatchers("/v1/security/login/oauth").permitAll()` alongside the existing `/login/internal` rule.

#### Step 3.2: Add CORS rule

Add `source.registerCorsConfiguration("/v1/security/login/oauth", loginCorsConfig)` alongside the existing CORS configuration for `/login/internal`.

#### Step 3.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 3.4: Commit

```
feat(security): permit OAuth login endpoint and add CORS rule

Add /v1/security/login/oauth to permitAll and CORS config
alongside the existing internal login rules.
```

---

### Phase 4: Domain Objects

**New files** in `security/src/main/java/com/akademiaplus/oauth/usecases/domain/`:

#### Step 4.1: OAuthTokenResponse

```java
public record OAuthTokenResponse(String accessToken, String tokenType, long expiresIn) {}
```

#### Step 4.2: OAuthUserProfile

```java
public record OAuthUserProfile(
    String email,
    String firstName,
    String lastName,
    String provider,
    String providerUserId
) {}
```

#### Step 4.3: Commit

```
feat(security): add OAuth domain records

Add OAuthTokenResponse and OAuthUserProfile records
in oauth/usecases/domain/ for provider API responses.
```

---

### Phase 5: Provider Strategy Pattern

#### Step 5.1: OAuthProviderStrategy interface

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/OAuthProviderStrategy.java`

```java
public interface OAuthProviderStrategy {
    String getProviderName();
    OAuthTokenResponse exchangeCodeForToken(String authorizationCode, String redirectUri);
    OAuthUserProfile fetchUserProfile(String accessToken);
}
```

#### Step 5.2: GoogleOAuthProviderStrategy

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/GoogleOAuthProviderStrategy.java`

- `@Service`, constructor-injected `RestClient`, `@Value` for client ID/secret
- `exchangeCodeForToken()`: POST to `https://oauth2.googleapis.com/token` with `grant_type=authorization_code`
- `fetchUserProfile()`: GET `https://www.googleapis.com/oauth2/v3/userinfo` with Bearer token
- Maps JSON responses to domain records
- Wraps HTTP errors in `OAuthProviderException`

#### Step 5.3: FacebookOAuthProviderStrategy

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/FacebookOAuthProviderStrategy.java`

- Same pattern as Google
- `exchangeCodeForToken()`: GET `https://graph.facebook.com/v19.0/oauth/access_token` with query params
- `fetchUserProfile()`: GET `https://graph.facebook.com/v19.0/me?fields=id,first_name,last_name,email` with Bearer token

#### Step 5.4: OAuthProviderRegistry

**File**: `security/src/main/java/com/akademiaplus/oauth/usecases/OAuthProviderRegistry.java`

- `@Service`, constructor-injected `Set<OAuthProviderStrategy>`
- `resolve(String providerName)`: returns strategy or throws `UnsupportedProviderException`
- Builds internal `Map<String, OAuthProviderStrategy>` from the injected set

#### Step 5.5: OAuthConfiguration

**File**: `security/src/main/java/com/akademiaplus/oauth/interfaceadapters/config/OAuthConfiguration.java`

- `@Configuration`
- `@Bean RestClient oAuthRestClient()`: default RestClient for OAuth HTTP calls

#### Step 5.6: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 5.7: Commit

```
feat(security): implement OAuth provider strategy pattern

Add OAuthProviderStrategy interface with Google and Facebook
implementations. Add OAuthProviderRegistry for provider resolution.
Add RestClient bean for OAuth HTTP calls.
```

---

### Phase 6: Exceptions

**New files** in `security/src/main/java/com/akademiaplus/oauth/exceptions/`:

- `OAuthProviderException.java` — provider communication failure (wraps HTTP errors)
- `UnsupportedProviderException.java` — unknown provider name

Both extend `RuntimeException` with `public static final` message constants.

#### Commit

```
feat(security): add OAuth exception types

Add OAuthProviderException for provider communication failures
and UnsupportedProviderException for unknown provider names.
```

---

### Phase 7: Repository Methods

#### Step 7.1: CustomerAuthRepository

**File**: `user-management/src/main/java/com/akademiaplus/customer/interfaceadapters/CustomerAuthRepository.java`

Add:
```java
Optional<CustomerAuthDataModel> findByProviderAndProviderUserIdHash(String provider, String providerUserIdHash);
```

#### Step 7.2: PersonPIIRepository

**File**: `user-management/src/main/java/com/akademiaplus/customer/interfaceadapters/PersonPIIRepository.java`

Add:
```java
Optional<PersonPIIDataModel> findByEmailHash(String emailHash);
```

This enables the account-linking branch — when an OAuth email matches an existing internally-registered user, the system links the OAuth credentials to the existing account instead of creating a duplicate.

#### Step 7.3: AdultStudentRepository

**File**: `user-management/src/main/java/com/akademiaplus/customer/adultstudent/interfaceadapters/AdultStudentRepository.java`

Add:
```java
Optional<AdultStudentDataModel> findByCustomerAuthId(Long customerAuthId);
Optional<AdultStudentDataModel> findByPersonPiiId(Long personPiiId);
```

- `findByCustomerAuthId`: used in the returning-user branch to load the AdultStudent from a known CustomerAuth
- `findByPersonPiiId`: used in the account-linking branch to find the existing AdultStudent when an OAuth email matches an existing PersonPII

Note: These query methods must account for the composite key — verify that Spring Data resolves the fields correctly, or use `@Query` if needed.

#### Step 7.4: Compile check

```bash
mvn clean compile -pl user-management -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 7.5: Commit

```
feat(user-management): add OAuth lookup repository methods

Add findByProviderAndProviderUserIdHash to CustomerAuthRepository,
findByEmailHash to PersonPIIRepository, and findByCustomerAuthId +
findByPersonPiiId to AdultStudentRepository for OAuth returning-user
lookup and account linking.
```

---

### Phase 8: OAuthAuthenticationUseCase

**File**: `application/src/main/java/com/akademiaplus/oauth/usecases/OAuthAuthenticationUseCase.java`

This is a cross-module orchestrator (Hard Rule #14 — lives in `application` module).

**Dependencies**: `OAuthProviderRegistry`, `CustomerAuthRepository`, `AdultStudentRepository`, `PersonPIIRepository`, `JwtTokenProvider`, `HashingService`, `PiiNormalizer`, `TenantContextHolder`, `ApplicationContext`

**Flow** (3-branch):
1. Set tenant context from `dto.getTenantId()`
2. Resolve provider strategy via registry
3. Exchange authorization code for access token
4. Fetch user profile from provider
5. Hash `providerUserId`, lookup `CustomerAuth` by provider + hash
6. **Branch 1 — Returning OAuth user**: `CustomerAuth` found → update token, load email from associated AdultStudent's PersonPII, issue JWT
7. **Branch 2 — Existing email, new provider (account linking)**: `CustomerAuth` NOT found → hash email, lookup `PersonPII` by emailHash → if found, create new `CustomerAuth` linked to the existing AdultStudent (via `AdultStudentRepository.findByPersonPiiId()`), issue JWT
8. **Branch 3 — Brand new user**: neither `CustomerAuth` nor `PersonPII` found → create PersonPII + CustomerAuth + AdultStudent (prototype beans via `applicationContext.getBean()`), issue JWT with email as subject
9. Return `AuthTokenResponseDTO` with platform JWT

**Account linking rationale**: When a user registers internally (username/password) and later logs in via OAuth with the same email, the system must link the OAuth credentials to the existing account rather than creating a duplicate PersonPII (which would violate the unique emailHash constraint and cause a `DuplicateEntityException` 409).

**Placeholder handling**: Use `PENDING_UPDATE` constants for phone/address/zip.

**`@Transactional`** on `authenticate()` method.

#### Commit

```
feat(application): implement OAuthAuthenticationUseCase

Add cross-module orchestrator for OAuth login flow with 3-branch logic:
- Returning OAuth user: update token, issue platform JWT
- Existing email, new provider: link OAuth credentials to existing account
- Brand new user: auto-create PersonPII + CustomerAuth + AdultStudent
- Exchange authorization code for access token via provider strategy
- Placeholder values for phone/address/zip (PENDING_UPDATE)
```

---

### Phase 9: OAuthController

**File**: `application/src/main/java/com/akademiaplus/oauth/interfaceadapters/OAuthController.java`

- `@RestController @RequestMapping("/v1/security")`
- Implements generated `OauthApi` interface
- Delegates to `OAuthAuthenticationUseCase.authenticate()`
- Thin controller — zero business logic

#### Commit

```
feat(application): add OAuthController

Implement generated OauthApi interface, delegate to
OAuthAuthenticationUseCase for POST /v1/security/login/oauth.
```

---

### Phase 10: OAuthControllerAdvice

**File**: `application/src/main/java/com/akademiaplus/oauth/config/OAuthControllerAdvice.java`

- `@ControllerAdvice(basePackageClasses = OAuthController.class)`
- Extends `BaseControllerAdvice`
- Handles `OAuthProviderException` → 401 with error code
- Handles `UnsupportedProviderException` → 400 with error code

#### Commit

```
feat(application): add OAuthControllerAdvice

Handle OAuthProviderException (401) and
UnsupportedProviderException (400) scoped to OAuthController.
```

---

### Phase 11: Configuration

**File**: `application/src/main/resources/application.properties`

Add:
```properties
oauth.google.client-id=${GOOGLE_CLIENT_ID}
oauth.google.client-secret=${GOOGLE_CLIENT_SECRET}
oauth.facebook.client-id=${FACEBOOK_CLIENT_ID}
oauth.facebook.client-secret=${FACEBOOK_CLIENT_SECRET}
```

#### Commit

```
feat(application): add OAuth provider configuration properties

Add environment-variable-backed properties for Google and
Facebook OAuth client credentials.
```

---

### Phase 12: Unit Tests — Security Module

All tests follow project conventions: Given-When-Then, `shouldDoX_whenY()`, `@DisplayName`, `@Nested`, zero `any()` matchers, string literal constants.

#### Step 12.1: GoogleOAuthProviderStrategyTest

**File**: `security/src/test/java/com/akademiaplus/oauth/usecases/GoogleOAuthProviderStrategyTest.java`

| @Nested | Tests |
|---------|-------|
| `CodeExchange` | `shouldReturnToken_whenGoogleReturnsValidResponse`, `shouldThrowOAuthProviderException_whenGoogleReturnsError` |
| `ProfileFetch` | `shouldReturnUserProfile_whenGoogleReturnsValidProfile`, `shouldThrowOAuthProviderException_whenProfileFetchFails` |
| `ProviderIdentity` | `shouldReturnGoogle_whenAskedForProviderName` |

#### Step 12.2: FacebookOAuthProviderStrategyTest

**File**: `security/src/test/java/com/akademiaplus/oauth/usecases/FacebookOAuthProviderStrategyTest.java`

Same structure as Google, with Facebook-specific endpoint assertions.

#### Step 12.3: OAuthProviderRegistryTest

**File**: `security/src/test/java/com/akademiaplus/oauth/usecases/OAuthProviderRegistryTest.java`

| @Nested | Tests |
|---------|-------|
| `Resolution` | `shouldReturnGoogleStrategy_whenProviderIsGoogle`, `shouldReturnFacebookStrategy_whenProviderIsFacebook` |
| `UnknownProvider` | `shouldThrowUnsupportedProviderException_whenProviderIsUnknown` |

#### Step 12.4: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 12.5: Commit

```
test(security): add OAuth provider strategy and registry unit tests

GoogleOAuthProviderStrategyTest, FacebookOAuthProviderStrategyTest,
OAuthProviderRegistryTest — cover code exchange, profile fetch,
provider resolution, and error paths.
```

---

### Phase 13: Unit Tests — Application Module

#### Step 13.1: OAuthAuthenticationUseCaseTest

**File**: `application/src/test/java/com/akademiaplus/oauth/usecases/OAuthAuthenticationUseCaseTest.java`

| @Nested | Tests |
|---------|-------|
| `ReturningUser` | `shouldIssueJwt_whenUserExistsByProviderHash`, `shouldUpdateToken_whenReturningUser` |
| `NewUser` | `shouldCreateAdultStudent_whenFirstTimeOAuthLogin`, `shouldSetPlaceholderValues_whenCreatingNewUser`, `shouldIssueJwt_whenNewUserCreated` |
| `TenantContext` | `shouldSetTenantId_whenAuthenticating` |
| `ProviderResolution` | `shouldDelegateToRegistry_whenResolvingProvider` |
| `ErrorPaths` | `shouldPropagateOAuthProviderException_whenCodeExchangeFails`, `shouldPropagateUnsupportedProviderException_whenProviderUnknown` |

#### Step 13.2: OAuthControllerTest

**File**: `application/src/test/java/com/akademiaplus/oauth/interfaceadapters/OAuthControllerTest.java`

Standalone MockMvc (no Spring context). Tests:
- `shouldReturn200WithToken_whenAuthenticationSucceeds`
- `shouldReturn401_whenOAuthProviderExceptionThrown`
- `shouldReturn400_whenUnsupportedProviderExceptionThrown`

#### Step 13.3: Compile + test

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl application -am -f platform-core-api/pom.xml
```

#### Step 13.4: Commit

```
test(application): add OAuth use case and controller unit tests

OAuthAuthenticationUseCaseTest — covers returning user, new user
auto-creation, tenant context, provider resolution, and error paths.
OAuthControllerTest — covers HTTP 200, 401, and 400 responses.
```

---

### Phase 14: Component Tests — Application Module

#### Step 14.1: OAuthComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/OAuthComponentTest.java`

Full Spring context + Testcontainers MariaDB via `AbstractIntegrationTest`. Tests the complete HTTP → Controller → UseCase → Repository → DB stack.

**Note**: Component tests for OAuth require mocking the external provider APIs. Use `@MockitoBean` (or `@MockBean`) for `OAuthProviderStrategy` implementations to avoid real HTTP calls to Google/Facebook during tests.

| @Nested | Tests |
|---------|-------|
| `NewUserLogin` | `shouldReturn200WithJwt_whenNewUserLogsInViaOAuth`, `shouldCreateAdultStudentInDatabase_whenNewUserLogsIn` |
| `ReturningUserLogin` | `shouldReturn200WithJwt_whenReturningUserLogsIn`, `shouldNotCreateDuplicateUser_whenReturningUserLogsIn` |
| `ErrorPaths` | `shouldReturn400_whenProviderIsUnsupported`, `shouldReturn401_whenProviderCommunicationFails` |

#### Step 14.2: Run component tests

```bash
mvn verify -pl application -am -f platform-core-api/pom.xml
```

#### Step 14.3: Commit

```
test(application): add OAuth component test

OAuthComponentTest — full Spring context + Testcontainers MariaDB.
Covers new user login, returning user login, unsupported provider,
and provider communication failure. External providers mocked.
```

---

### Phase 15: E2E Tests — platform-api-e2e

See separate prompt: `platform-api-e2e/docs/prompts/pending/oauth-e2e-prompt.md`

OAuth E2E tests require a running server with valid provider credentials. Add requests to the Postman collection:

| Request | Method | Expected |
|---------|--------|----------|
| `OAuthLoginGoogleResponse200` | POST `/v1/security/login/oauth` | 200 + JWT (requires valid Google auth code) |
| `OAuthLoginFacebookResponse200` | POST `/v1/security/login/oauth` | 200 + JWT (requires valid Facebook auth code) |
| `OAuthLoginUnsupportedProviderResponse400` | POST `/v1/security/login/oauth` | 400 + `UNSUPPORTED_PROVIDER` |
| `OAuthLoginInvalidCodeResponse401` | POST `/v1/security/login/oauth` | 401 + `OAUTH_PROVIDER_ERROR` |

**Note**: The happy-path E2E tests (200) require real provider credentials and are gated behind an environment variable. The error-path tests (400, 401) can run without credentials.

---

## 5. File Inventory

### New files (21)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `security/src/main/resources/openapi/oauth-authentication.yaml` | security | 2 |
| 2 | `security/.../oauth/usecases/domain/OAuthTokenResponse.java` | security | 4 |
| 3 | `security/.../oauth/usecases/domain/OAuthUserProfile.java` | security | 4 |
| 4 | `security/.../oauth/usecases/OAuthProviderStrategy.java` | security | 5 |
| 5 | `security/.../oauth/usecases/GoogleOAuthProviderStrategy.java` | security | 5 |
| 6 | `security/.../oauth/usecases/FacebookOAuthProviderStrategy.java` | security | 5 |
| 7 | `security/.../oauth/usecases/OAuthProviderRegistry.java` | security | 5 |
| 8 | `security/.../oauth/interfaceadapters/config/OAuthConfiguration.java` | security | 5 |
| 9 | `security/.../oauth/exceptions/OAuthProviderException.java` | security | 6 |
| 10 | `security/.../oauth/exceptions/UnsupportedProviderException.java` | security | 6 |
| 11 | `application/.../oauth/usecases/OAuthAuthenticationUseCase.java` | application | 8 |
| 12 | `application/.../oauth/interfaceadapters/OAuthController.java` | application | 9 |
| 13 | `application/.../oauth/config/OAuthControllerAdvice.java` | application | 10 |
| 14 | `security/test/.../oauth/usecases/GoogleOAuthProviderStrategyTest.java` | security | 12 |
| 15 | `security/test/.../oauth/usecases/FacebookOAuthProviderStrategyTest.java` | security | 12 |
| 16 | `security/test/.../oauth/usecases/OAuthProviderRegistryTest.java` | security | 12 |
| 17 | `application/test/.../oauth/usecases/OAuthAuthenticationUseCaseTest.java` | application | 13 |
| 18 | `application/test/.../oauth/interfaceadapters/OAuthControllerTest.java` | application | 13 |
| 19 | `application/test/.../usecases/OAuthComponentTest.java` | application | 14 |
| 20 | DB schema migration file | infra | 1 |
| 21 | `platform-api-e2e` collection update | e2e | 15 |

### Modified files (8)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `multi-tenant-data/.../security/CustomerAuthDataModel.java` | Add providerUserId fields | 1 |
| 2 | `security/src/main/resources/openapi/security-module.yaml` | Add OAuth refs | 2 |
| 3 | `security/.../config/SecurityConfig.java` | permitAll + CORS | 3 |
| 4 | `user-management/.../customer/interfaceadapters/CustomerAuthRepository.java` | Add query method | 7 |
| 5 | `user-management/.../customer/interfaceadapters/PersonPIIRepository.java` | Add `findByEmailHash` for account linking | 7 |
| 6 | `user-management/.../customer/adultstudent/interfaceadapters/AdultStudentRepository.java` | Add `findByCustomerAuthId` + `findByPersonPiiId` | 7 |
| 7 | `application/src/main/resources/application.properties` | OAuth config keys | 11 |
| 8 | DB schema file | Add columns + index | 1 |

---

## 6. Verification

### Per-phase gates

After each phase, run the specified compile/test command. Fix all errors before proceeding.

### Final verification

1. `mvn clean compile -pl security -am -DskipTests` — OpenAPI codegen produces `OauthApi` + `OAuthLoginRequestDTO`
2. `mvn test -pl security` — provider strategy + registry tests pass
3. `mvn clean compile -pl application -am -DskipTests` — full compilation
4. `mvn test -pl application` — use case + controller tests pass
5. `mvn verify -pl application` — component tests pass
6. Manual: Start app, POST to `/v1/security/login/oauth` with a valid Google auth code → receive JWT

---

## 7. Critical Reminders

1. **Authorization codes are single-use** — the provider invalidates them after exchange. Tests must account for this.
2. **`providerUserId` is PII** — must be encrypted at rest (Phase 1 adds `@Convert(converter = StringEncryptor.class)`).
3. **Tenant context** — OAuth login receives `tenantId` in the request body (not from `X-Tenant-Id` header) because the login endpoint is unauthenticated.
4. **JWT subject** — use the user's email as the JWT subject (consistent with internal auth which uses username).
5. **`ApplicationContext.getBean()`** — all entity instantiation must use prototype bean pattern, never `new Entity()`.
6. **Facebook token format** — Facebook Graph API v19.0 returns JSON (not form-encoded) for the token endpoint.
7. **Component test mocking** — mock the `OAuthProviderStrategy` implementations in component tests to avoid real HTTP calls.
8. **No `any()` matchers** — all mock stubbing uses exact values or `ArgumentCaptor`.
