# Passkey Authentication (WebAuthn/FIDO2) — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/passkey-authentication-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 → 2 → ... → 12). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header (2026).
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals → `public static final` constants, shared between impl and tests.
9. Use `applicationContext.getBean()` for all entity instantiation — never `new EntityDataModel()`.
10. Read existing files BEFORE modifying — field names, import paths, and CompositeId class names vary.
11. Commit after each phase using the commit message provided.

---

## Phase 1: Dependencies

### Read first

```bash
cat security/pom.xml
cat pom.xml | grep -A5 "webauthn\|redis"
```

Check if `spring-boot-starter-data-redis` is already in the parent POM or any module:
```bash
grep -rn "spring-boot-starter-data-redis" */pom.xml pom.xml
```

### Step 1.1: Add java-webauthn-server dependency

**File**: `security/pom.xml`

Add in the `<dependencies>` section:

```xml
<!-- WebAuthn/FIDO2 Passkey Support -->
<dependency>
    <groupId>com.yubico</groupId>
    <artifactId>webauthn-server-core</artifactId>
    <version>2.5.3</version>
</dependency>
```

**IMPORTANT**: Check the latest stable version of `webauthn-server-core` in the parent POM's `<dependencyManagement>`. If it's managed there, omit the `<version>` tag.

### Step 1.2: Add Redis dependency (if not present)

Check if `spring-boot-starter-data-redis` exists anywhere:
```bash
grep -rn "spring-boot-starter-data-redis" */pom.xml pom.xml
```

If not present, add to `security/pom.xml`:

```xml
<!-- Redis for WebAuthn challenge store -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Step 1.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 1.4: Commit

```bash
git add security/pom.xml pom.xml
git commit -m "feat(security): add java-webauthn-server and Redis dependencies

Add Yubico webauthn-server-core 2.5.3 for FIDO2/WebAuthn passkey
support. Add spring-boot-starter-data-redis for challenge storage."
```

---

## Phase 2: PasskeyCredentialDataModel + DB Schema

### Read first

```bash
cat multi-tenant-data/src/main/java/com/akademiaplus/security/CustomerAuthDataModel.java
cat multi-tenant-data/src/main/java/com/akademiaplus/security/InternalAuthDataModel.java
```

Understand:
- How composite keys are defined (`@IdClass`, `CompositeId` inner class)
- How `TenantScoped` is extended
- How `@SQLDelete` is formatted with composite key parameters
- The `@Scope("prototype")` + `@Component` pattern

Also find the DB schema file:
```bash
find . -name "*.sql" | grep -i "schema\|db_init\|migration" | head -10
```

### Step 2.1: Create PasskeyCredentialDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/security/PasskeyCredentialDataModel.java`

Follow the EXACT pattern of `CustomerAuthDataModel`:
- `@Getter @Setter @AllArgsConstructor @NoArgsConstructor`
- `@Scope("prototype") @Component`
- `@Entity @Table(name = "passkey_credentials")`
- `@SQLDelete(sql = "UPDATE passkey_credentials SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND passkey_credential_id = ?")`
- `@IdClass(PasskeyCredentialDataModel.PasskeyCredentialCompositeId.class)`
- Extends `TenantScoped`
- Copyright header (2026)
- Full Javadoc on class and all fields

**Fields**:

```java
@Id
@Column(name = "passkey_credential_id")
private Long passkeyCredentialId;

@Column(name = "user_id", nullable = false)
private Long userId;

@Lob
@Column(name = "credential_id", nullable = false, columnDefinition = "BLOB")
private byte[] credentialId;

@Lob
@Column(name = "public_key", nullable = false, columnDefinition = "BLOB")
private byte[] publicKey;

@Column(name = "sign_count", nullable = false)
private Long signCount;

@Column(name = "transports", length = 255)
private String transports;

@Column(name = "created_at", nullable = false)
private Instant createdAt;

@Column(name = "last_used_at")
private Instant lastUsedAt;

@Column(name = "display_name", length = 255)
private String displayName;

@Lob
@Column(name = "user_handle", nullable = false, columnDefinition = "BLOB")
private byte[] userHandle;
```

**Composite ID inner class**:
```java
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public static class PasskeyCredentialCompositeId implements Serializable {
    protected Long tenantId;
    protected Long passkeyCredentialId;
}
```

### Step 2.2: Add DB schema

Find the schema file and add the `passkey_credentials` table:

```sql
CREATE TABLE passkey_credentials (
    tenant_id              BIGINT       NOT NULL,
    passkey_credential_id  BIGINT       NOT NULL,
    user_id                BIGINT       NOT NULL,
    credential_id          BLOB         NOT NULL,
    public_key             BLOB         NOT NULL,
    sign_count             BIGINT       NOT NULL DEFAULT 0,
    transports             VARCHAR(255),
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at           TIMESTAMP,
    display_name           VARCHAR(255),
    user_handle            BLOB         NOT NULL,
    deleted_at             TIMESTAMP,
    PRIMARY KEY (tenant_id, passkey_credential_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_passkey_cred_tenant_credential
    ON passkey_credentials (tenant_id, credential_id(255));

CREATE INDEX idx_passkey_cred_tenant_user
    ON passkey_credentials (tenant_id, user_id);

CREATE INDEX idx_passkey_cred_tenant_user_handle
    ON passkey_credentials (tenant_id, user_handle(255));
```

### Step 2.3: Compile

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
```

### Step 2.4: Commit

```bash
git add multi-tenant-data/src/main/java/com/akademiaplus/security/PasskeyCredentialDataModel.java
git add db_init/ infra-common/ multi-tenant-data/
git commit -m "feat(multi-tenant-data): add PasskeyCredentialDataModel entity

Add JPA entity for WebAuthn/FIDO2 passkey credentials with
composite key (tenantId, passkeyCredentialId). Stores credential ID,
public key, sign count, transports, and user handle. Add DB schema
with indexes for credential and user lookups."
```

---

## Phase 3: OpenAPI Specifications

### Read first

```bash
cat security/src/main/resources/openapi/internal-authentication.yaml
cat security/src/main/resources/openapi/security-module.yaml
ls security/src/main/resources/openapi/
```

Note:
- The exact schema names for `AuthTokenResponse` and `ErrorResponse`
- How paths and schemas are referenced between files
- The openapi version used

### Step 3.1: Create passkey-authentication.yaml

**File**: `security/src/main/resources/openapi/passkey-authentication.yaml`

Define 4 paths and their request/response schemas:

**Paths**:
- `POST /passkey/register/options` → `passkeyRegisterOptions`
- `POST /passkey/register/complete` → `passkeyRegisterComplete`
- `POST /passkey/login/options` → `passkeyLoginOptions`
- `POST /passkey/login/complete` → `passkeyLoginComplete`

**Request schemas**:

```yaml
PasskeyRegisterOptionsRequest:
  type: object
  properties:
    tenantId:
      type: integer
      format: int64
    displayName:
      type: string
      description: Human-readable name for this passkey (e.g., "MacBook Touch ID")
  required:
    - tenantId

PasskeyRegisterCompleteRequest:
  type: object
  properties:
    tenantId:
      type: integer
      format: int64
    credential:
      type: string
      description: Base64URL-encoded PublicKeyCredential JSON from navigator.credentials.create()
    displayName:
      type: string
      description: Human-readable name for this passkey
  required:
    - tenantId
    - credential

PasskeyLoginOptionsRequest:
  type: object
  properties:
    tenantId:
      type: integer
      format: int64
  required:
    - tenantId

PasskeyLoginCompleteRequest:
  type: object
  properties:
    tenantId:
      type: integer
      format: int64
    credential:
      type: string
      description: Base64URL-encoded PublicKeyCredential JSON from navigator.credentials.get()
  required:
    - tenantId
    - credential
```

**Response schemas**:

```yaml
PasskeyRegisterOptionsResponse:
  type: object
  properties:
    publicKeyCredentialCreationOptions:
      type: string
      description: JSON-serialized PublicKeyCredentialCreationOptions for the browser

PasskeyRegisterCompleteResponse:
  type: object
  properties:
    success:
      type: boolean
    displayName:
      type: string

PasskeyLoginOptionsResponse:
  type: object
  properties:
    publicKeyCredentialRequestOptions:
      type: string
      description: JSON-serialized PublicKeyCredentialRequestOptions for the browser
```

Login complete reuses `AuthTokenResponse` from internal-authentication.yaml.

### Step 3.2: Modify security-module.yaml

Add the passkey path and schema references following the same pattern as the existing `internal-authentication.yaml` references. Read the file first to match the exact structure.

### Step 3.3: Regenerate DTOs

```bash
mvn clean generate-sources -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.4: Verify generated code

```bash
find security/target/generated-sources -name "*Passkey*" -o -name "*passkey*" | head -10
```

Read each generated file and confirm:
- Request DTOs have correct getters
- The API interface has 4 methods (one per endpoint)

**IMPORTANT**: Note the exact generated interface name for use in Phase 8.

### Step 3.5: Commit

```bash
git add security/src/main/resources/openapi/
git commit -m "api(security): add passkey authentication OpenAPI specification

Add 4 passkey endpoints: register/options, register/complete,
login/options, login/complete. Define request/response schemas
with Base64URL-encoded WebAuthn fields."
```

---

## Phase 4: RelyingPartyConfiguration + PasskeyCredentialRepository

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/InternalAuthRepository.java
cat utilities/src/main/java/com/akademiaplus/utilities/persistence/repository/TenantScopedRepository.java
cat application/src/main/resources/application.properties
```

Understand:
- How `TenantScopedRepository` is extended
- The `@ConfigurationProperties` + `@EnableConfigurationProperties` pattern
- Existing property naming conventions

### Step 4.1: Create directory structure

```bash
mkdir -p security/src/main/java/com/akademiaplus/passkey/config
mkdir -p security/src/main/java/com/akademiaplus/passkey/interfaceadapters
mkdir -p security/src/main/java/com/akademiaplus/passkey/usecases
mkdir -p security/src/main/java/com/akademiaplus/passkey/exceptions
```

### Step 4.2: PasskeyProperties

**File**: `security/src/main/java/com/akademiaplus/passkey/config/PasskeyProperties.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * ...
 */
@ConfigurationProperties(prefix = "security.passkey")
public class PasskeyProperties {

    /** Relying Party ID — typically the domain. */
    private String rpId;

    /** Relying Party display name. */
    private String rpName;

    /** Allowed origins for WebAuthn operations. */
    private List<String> allowedOrigins = List.of();

    /** Challenge TTL in seconds (default: 300 = 5 minutes). */
    private Long challengeTtlSeconds = 300L;

    // Getters + setters for all fields
}
```

### Step 4.3: RelyingPartyConfiguration

**File**: `security/src/main/java/com/akademiaplus/passkey/config/RelyingPartyConfiguration.java`

- `@Configuration`
- `@EnableConfigurationProperties(PasskeyProperties.class)`
- `@Bean RelyingParty relyingParty(PasskeyProperties, PasskeyCredentialRepositoryAdapter)`
- Build `RelyingPartyIdentity` from properties
- Set `allowedOrigins` from properties
- Full Javadoc

**IMPORTANT**: Read the Yubico `RelyingParty.builder()` API:
```bash
find ~/.m2/repository -name "webauthn-server-core-*.jar" -path "*/2.5.*" 2>/dev/null | head -1
```

The builder requires `identity(RelyingPartyIdentity)` and `credentialRepository(CredentialRepository)`.

### Step 4.4: PasskeyCredentialJpaRepository

**File**: `security/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialJpaRepository.java`

```java
/**
 * Spring Data JPA repository for passkey credential persistence.
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface PasskeyCredentialJpaRepository
        extends TenantScopedRepository<PasskeyCredentialDataModel,
                PasskeyCredentialDataModel.PasskeyCredentialCompositeId> {

    /**
     * Finds all credentials for a given user within the tenant.
     *
     * @param userId the user ID
     * @return list of credentials
     */
    List<PasskeyCredentialDataModel> findByUserId(Long userId);

    /**
     * Finds a credential by its authenticator-assigned credential ID.
     *
     * @param credentialId the WebAuthn credential ID (raw bytes)
     * @return the credential, if found
     */
    Optional<PasskeyCredentialDataModel> findByCredentialId(byte[] credentialId);

    /**
     * Finds all credentials by user handle.
     *
     * @param userHandle the WebAuthn user handle (raw bytes)
     * @return list of credentials
     */
    List<PasskeyCredentialDataModel> findByUserHandle(byte[] userHandle);
}
```

### Step 4.5: PasskeyCredentialRepositoryAdapter

**File**: `security/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialRepositoryAdapter.java`

Implements `com.yubico.webauthn.CredentialRepository`:

```java
/**
 * Adapter between Yubico's CredentialRepository and the JPA repository.
 *
 * <p>Translates WebAuthn credential lookups into JPA queries. All lookups
 * are tenant-scoped via Hibernate filters.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class PasskeyCredentialRepositoryAdapter implements CredentialRepository {

    /** Error message when username resolution fails. */
    public static final String ERROR_USER_NOT_FOUND = "User not found for username: %s";

    private final PasskeyCredentialJpaRepository jpaRepository;

    public PasskeyCredentialRepositoryAdapter(PasskeyCredentialJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        // Lookup by userId (username is the string representation of userId)
        // Map each credential to PublicKeyCredentialDescriptor
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        // Lookup first credential by userId, return userHandle
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        // Lookup by userHandle, return userId as string
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        // Lookup by credentialId, verify userHandle matches
        // Map to RegisteredCredential
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        // Lookup by credentialId, return all matching credentials
    }
}
```

**IMPORTANT**: The `username` parameter in Yubico's `CredentialRepository` interface maps to the `userId` (as a String). Read the Yubico documentation to understand the contract.

### Step 4.6: Add passkey configuration to application.properties

**File**: `application/src/main/resources/application.properties`

```properties
# Passkey / WebAuthn Configuration
security.passkey.rp-id=localhost
security.passkey.rp-name=AkademiaPlus Dev
security.passkey.allowed-origins[0]=http://localhost:3000
security.passkey.challenge-ttl-seconds=300
```

### Step 4.7: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 4.8: Commit

```bash
git add security/src/main/java/com/akademiaplus/passkey/ application/src/main/resources/
git commit -m "feat(security): add RelyingParty config and credential repository

Add PasskeyProperties, RelyingPartyConfiguration (@Bean RelyingParty),
PasskeyCredentialJpaRepository, and PasskeyCredentialRepositoryAdapter
implementing Yubico CredentialRepository interface."
```

---

## Phase 5: Challenge Store (Redis)

### Read first

```bash
grep -rn "RedisTemplate\|StringRedisTemplate\|@EnableRedisRepositories" security/src/ application/src/ 2>/dev/null
cat application/src/main/resources/application.properties | grep -i redis
```

Check if Redis auto-configuration is already set up. If `StringRedisTemplate` is not available, Spring Boot auto-configures it when `spring-boot-starter-data-redis` is on the classpath.

### Step 5.1: Create PasskeyChallengeStore

**File**: `security/src/main/java/com/akademiaplus/passkey/usecases/PasskeyChallengeStore.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * ...
 */
@Service
public class PasskeyChallengeStore {

    public static final String KEY_PREFIX = "passkey:challenge:";
    public static final String ERROR_CHALLENGE_NOT_FOUND = "Challenge not found or expired";
    public static final String ERROR_CHALLENGE_STORE_FAILED = "Failed to store challenge";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasskeyProperties properties;

    public PasskeyChallengeStore(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  PasskeyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Stores a challenge with metadata and TTL.
     */
    public void store(String challengeBase64, ChallengeMetadata metadata) {
        // Given
        String key = KEY_PREFIX + challengeBase64;
        String value = serialize(metadata);

        // When
        redisTemplate.opsForValue().set(key, value,
                Duration.ofSeconds(properties.getChallengeTtlSeconds()));
    }

    /**
     * Retrieves and deletes a challenge (single-use).
     */
    public ChallengeMetadata consumeChallenge(String challengeBase64) {
        String key = KEY_PREFIX + challengeBase64;
        String value = redisTemplate.opsForValue().getAndDelete(key);

        if (value == null) {
            throw new PasskeyAuthenticationException(ERROR_CHALLENGE_NOT_FOUND);
        }

        return deserialize(value);
    }

    /**
     * Challenge metadata.
     */
    public record ChallengeMetadata(Long userId, Long tenantId, String operation) {}

    private String serialize(ChallengeMetadata metadata) { ... }
    private ChallengeMetadata deserialize(String value) { ... }
}
```

### Step 5.2: Add Redis configuration (if needed)

**File**: `application/src/main/resources/application-dev.properties`

Check if Redis connection properties exist. If not, add:
```properties
# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Step 5.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 5.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/passkey/usecases/PasskeyChallengeStore.java
git add application/src/main/resources/
git commit -m "feat(security): add Redis-backed PasskeyChallengeStore

Add challenge store with 5-minute TTL for WebAuthn registration
and authentication flows. Supports single-use challenge consumption
with JSON-serialized metadata."
```

---

## Phase 6: PasskeyRegistrationUseCase + Exceptions

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCase.java
cat security/src/main/java/com/akademiaplus/exceptions/InvalidLoginException.java
```

Understand:
- How existing security use cases are structured
- How exceptions are defined with `public static final` message constants

### Step 6.1: Create exceptions directory

```bash
mkdir -p security/src/main/java/com/akademiaplus/passkey/exceptions
```

### Step 6.2: PasskeyRegistrationException

**File**: `security/src/main/java/com/akademiaplus/passkey/exceptions/PasskeyRegistrationException.java`

```java
public class PasskeyRegistrationException extends RuntimeException {

    public static final String ERROR_REGISTRATION_FAILED = "Passkey registration failed: %s";
    public static final String ERROR_CREDENTIAL_ALREADY_EXISTS = "A passkey with this credential ID already exists";

    public PasskeyRegistrationException(String message) {
        super(message);
    }

    public PasskeyRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Step 6.3: PasskeyAuthenticationException

**File**: `security/src/main/java/com/akademiaplus/passkey/exceptions/PasskeyAuthenticationException.java`

```java
public class PasskeyAuthenticationException extends RuntimeException {

    public static final String ERROR_AUTHENTICATION_FAILED = "Passkey authentication failed: %s";
    public static final String ERROR_CREDENTIAL_NOT_FOUND = "No passkey credential found for the provided ID";
    public static final String ERROR_SIGN_COUNT_REGRESSION = "Authenticator sign count regression detected — possible cloned authenticator";

    public PasskeyAuthenticationException(String message) {
        super(message);
    }

    public PasskeyAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Step 6.4: PasskeyRegistrationUseCase

**File**: `security/src/main/java/com/akademiaplus/passkey/usecases/PasskeyRegistrationUseCase.java`

- `@Service`
- Constructor-injected: `RelyingParty`, `PasskeyChallengeStore`, `PasskeyCredentialJpaRepository`, `ApplicationContext`
- `public static final String OPERATION_REGISTER = "REGISTER"`

**Method `generateRegistrationOptions`**:
```java
@Transactional
public PublicKeyCredentialCreationOptions generateRegistrationOptions(
        Long userId, String username, byte[] userHandle, Long tenantId) {
    // 1. Build UserIdentity
    UserIdentity userIdentity = UserIdentity.builder()
            .name(username)
            .displayName(username)
            .id(new ByteArray(userHandle))
            .build();

    // 2. Start registration
    StartRegistrationOptions options = StartRegistrationOptions.builder()
            .user(userIdentity)
            .build();

    PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);

    // 3. Store challenge
    String challengeBase64 = creationOptions.getChallenge().getBase64Url();
    challengeStore.store(challengeBase64,
            new PasskeyChallengeStore.ChallengeMetadata(userId, tenantId, OPERATION_REGISTER));

    // 4. Return options for browser
    return creationOptions;
}
```

**Method `completeRegistration`**:
```java
@Transactional
public String completeRegistration(String responseJson, Long tenantId, String displayName) {
    // 1. Parse PublicKeyCredential
    // 2. Extract challenge from response → consume from store
    // 3. Build FinishRegistrationOptions
    // 4. Call relyingParty.finishRegistration() — validates attestation
    // 5. Create PasskeyCredentialDataModel via applicationContext.getBean()
    //    - Set credentialId, publicKey, signCount (0), transports
    //    - Set createdAt = Instant.now(), displayName, userHandle
    // 6. Generate passkeyCredentialId via ID generator
    // 7. Save to DB
    // 8. Return displayName
}
```

### Step 6.5: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 6.6: Commit

```bash
git add security/src/main/java/com/akademiaplus/passkey/
git commit -m "feat(security): implement PasskeyRegistrationUseCase

Add passkey registration use case with challenge generation,
authenticator response validation, and credential storage.
Add PasskeyRegistrationException and PasskeyAuthenticationException.
Uses RelyingParty from java-webauthn-server for cryptographic validation."
```

---

## Phase 7: PasskeyAuthenticationUseCase

### Read first

```bash
cat application/src/main/java/com/akademiaplus/oauth/usecases/OAuthAuthenticationUseCase.java 2>/dev/null
cat security/src/main/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCase.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/config/TenantContextHolder.java
```

Understand:
- How existing authentication use cases build JWT claims
- How `JwtTokenProvider.createToken()` is called
- How `TenantContextHolder.setTenantId()` is used

### Step 7.1: Create directory structure

```bash
mkdir -p application/src/main/java/com/akademiaplus/passkey/usecases
mkdir -p application/src/main/java/com/akademiaplus/passkey/interfaceadapters
mkdir -p application/src/main/java/com/akademiaplus/passkey/config
```

### Step 7.2: Create PasskeyAuthenticationUseCase

**File**: `application/src/main/java/com/akademiaplus/passkey/usecases/PasskeyAuthenticationUseCase.java`

- `@Service`
- Constructor-injected: `RelyingParty`, `PasskeyChallengeStore`, `PasskeyCredentialJpaRepository`, `JwtTokenProvider`, `TenantContextHolder`
- `public static final String JWT_CLAIM_ROLE = "Has role"`
- `public static final String JWT_CLAIM_AUTH_METHOD = "auth_method"`
- `public static final String AUTH_METHOD_PASSKEY = "passkey"`
- `public static final String OPERATION_LOGIN = "LOGIN"`

**Method `generateLoginOptions(Long tenantId)`**:
```java
@Transactional(readOnly = true)
public AssertionRequest generateLoginOptions(Long tenantId) {
    // 1. Set tenant context
    tenantContextHolder.setTenantId(tenantId);

    // 2. Start assertion (no username — discoverable credential flow)
    StartAssertionOptions options = StartAssertionOptions.builder().build();
    AssertionRequest assertionRequest = relyingParty.startAssertion(options);

    // 3. Store challenge
    String challengeBase64 = assertionRequest.getPublicKeyCredentialRequestOptions()
            .getChallenge().getBase64Url();
    challengeStore.store(challengeBase64,
            new PasskeyChallengeStore.ChallengeMetadata(null, tenantId, OPERATION_LOGIN));

    // 4. Return assertion request
    return assertionRequest;
}
```

**Method `completeLogin(String responseJson, Long tenantId)`**:
```java
@Transactional
public AuthTokenResponseDTO completeLogin(String responseJson, Long tenantId) {
    // 1. Set tenant context
    tenantContextHolder.setTenantId(tenantId);

    // 2. Parse PublicKeyCredential
    // 3. Extract challenge → consume from store
    // 4. Build FinishAssertionOptions
    // 5. Call relyingParty.finishAssertion() — validates signature
    // 6. Get AssertionResult
    // 7. Find credential by credentialId
    // 8. Validate sign count (must not regress)
    // 9. Update sign count + lastUsedAt
    // 10. Resolve username/email from userId
    // 11. Build JWT claims: role, auth_method=passkey
    // 12. Create JWT via jwtTokenProvider.createToken(username, tenantId, claims)
    // 13. Build and return AuthTokenResponseDTO
}
```

### Step 7.3: Compile

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

### Step 7.4: Commit

```bash
git add application/src/main/java/com/akademiaplus/passkey/
git commit -m "feat(application): implement PasskeyAuthenticationUseCase

Add cross-module orchestrator for passkey login flow:
- Generate authentication options with challenge
- Validate assertion response (signature verification)
- Update sign count and last-used timestamp
- Issue platform JWT with auth_method=passkey claim"
```

---

## Phase 8: Controllers + ControllerAdvice

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/InternalAuthController.java
cat security/src/main/java/com/akademiaplus/config/SecurityControllerAdvice.java
cat utilities/src/main/java/com/akademiaplus/utilities/web/BaseControllerAdvice.java
```

Read the generated API interface:
```bash
find application/target/generated-sources security/target/generated-sources -name "*Passkey*Api*" -o -name "*passkey*Api*" 2>/dev/null
```

### Step 8.1: PasskeyController

**File**: `application/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyController.java`

- `@RestController @RequestMapping("/v1/security")`
- Implements the generated Passkey API interface (exact name from Phase 3 verification)
- 4 `@Override` methods — each delegates to the appropriate use case
- Thin controller — zero business logic
- Copyright header + Javadoc

### Step 8.2: PasskeyControllerAdvice

**File**: `application/src/main/java/com/akademiaplus/passkey/config/PasskeyControllerAdvice.java`

Follow the exact pattern of `SecurityControllerAdvice`:
- `@ControllerAdvice(basePackageClasses = PasskeyController.class)`
- Extends `BaseControllerAdvice`
- Constructor: `MessageService messageService`
- `public static final String CODE_PASSKEY_REGISTRATION_FAILED = "PASSKEY_REGISTRATION_FAILED"`
- `public static final String CODE_PASSKEY_AUTHENTICATION_FAILED = "PASSKEY_AUTHENTICATION_FAILED"`
- `@ExceptionHandler(PasskeyRegistrationException.class)` → 400
- `@ExceptionHandler(PasskeyAuthenticationException.class)` → 401

### Step 8.3: Compile

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

### Step 8.4: Commit

```bash
git add application/src/main/java/com/akademiaplus/passkey/
git commit -m "feat(application): add PasskeyController and ControllerAdvice

Implement generated PasskeyApi interface with 4 endpoints.
Add PasskeyControllerAdvice for registration (400) and
authentication (401) failure handling."
```

---

## Phase 9: SecurityConfig Updates

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

### Step 9.1: Permit passkey login endpoints

Add alongside existing permitAll rules:
```java
.requestMatchers("/v1/security/passkey/login/**").permitAll()
```

Registration endpoints (`/passkey/register/**`) stay **authenticated** — only logged-in users register new passkeys.

### Step 9.2: Add CORS rule

Add:
```java
source.registerCorsConfiguration("/v1/security/passkey/login/**", loginCorsConfig);
```

### Step 9.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 9.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/SecurityConfig.java
git commit -m "feat(security): permit passkey login endpoints in SecurityConfig

Add /v1/security/passkey/login/** to permitAll and CORS config.
Registration endpoints remain authenticated."
```

---

## Phase 10: Unit Tests — Security Module

### Read first

```bash
cat security/src/test/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCaseTest.java 2>/dev/null
cat security/src/test/java/com/akademiaplus/internal/usecases/InternalAuthorizationUseCaseTest.java 2>/dev/null
ls security/src/test/java/com/akademiaplus/
```

Follow the existing security module test patterns.

### Step 10.1: Create test directories

```bash
mkdir -p security/src/test/java/com/akademiaplus/passkey/usecases
mkdir -p security/src/test/java/com/akademiaplus/passkey/interfaceadapters
```

### Step 10.2: PasskeyRegistrationUseCaseTest

**File**: `security/src/test/java/com/akademiaplus/passkey/usecases/PasskeyRegistrationUseCaseTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock RelyingParty relyingParty`
- `@Mock PasskeyChallengeStore challengeStore`
- `@Mock PasskeyCredentialJpaRepository credentialRepository`
- `@Mock ApplicationContext applicationContext`
- Create use case in `@BeforeEach`

Constants:
```java
public static final Long TEST_USER_ID = 1L;
public static final String TEST_USERNAME = "john.doe@test.com";
public static final Long TEST_TENANT_ID = 100L;
public static final String TEST_DISPLAY_NAME = "MacBook Touch ID";
public static final String TEST_CHALLENGE_BASE64 = "dGVzdC1jaGFsbGVuZ2U";
```

| @Nested | Tests |
|---------|-------|
| `OptionsGeneration` | `shouldReturnCreationOptions_whenUserIsValid`, `shouldStoreChallengeInRedis_whenOptionsGenerated`, `shouldIncludeUserIdentity_whenOptionsGenerated` |
| `RegistrationCompletion` | `shouldStoreCredential_whenRegistrationSucceeds`, `shouldUseApplicationContextGetBean_whenCreatingCredential`, `shouldSetCreatedAtTimestamp_whenCredentialStored` |
| `ErrorPaths` | `shouldThrowPasskeyRegistrationException_whenValidationFails`, `shouldThrowPasskeyRegistrationException_whenChallengeExpired` |

### Step 10.3: PasskeyChallengeStoreTest

**File**: `security/src/test/java/com/akademiaplus/passkey/usecases/PasskeyChallengeStoreTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock StringRedisTemplate redisTemplate`
- `@Mock ValueOperations<String, String> valueOperations`
- `@Mock ObjectMapper objectMapper`
- `@Mock PasskeyProperties properties`

| @Nested | Tests |
|---------|-------|
| `Storage` | `shouldStoreWithKeyPrefix_whenCalledWithChallenge`, `shouldSetTtlFromProperties_whenStoringChallenge` |
| `Consumption` | `shouldReturnMetadata_whenChallengeExists`, `shouldDeleteAfterRetrieving_whenConsuming`, `shouldThrowPasskeyAuthenticationException_whenChallengeNotFound` |

### Step 10.4: PasskeyCredentialRepositoryAdapterTest

**File**: `security/src/test/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialRepositoryAdapterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock PasskeyCredentialJpaRepository jpaRepository`

| @Nested | Tests |
|---------|-------|
| `CredentialIdsForUsername` | `shouldReturnCredentialDescriptors_whenUserHasPasskeys`, `shouldReturnEmptySet_whenUserHasNoPasskeys` |
| `UserHandleForUsername` | `shouldReturnUserHandle_whenUserExists`, `shouldReturnEmpty_whenUserNotFound` |
| `UsernameForUserHandle` | `shouldReturnUserId_whenUserHandleMatches`, `shouldReturnEmpty_whenUserHandleNotFound` |
| `Lookup` | `shouldReturnRegisteredCredential_whenCredentialIdAndUserHandleMatch`, `shouldReturnEmpty_whenCredentialNotFound` |
| `LookupAll` | `shouldReturnAllMatchingCredentials_whenCredentialIdFound`, `shouldReturnEmptySet_whenNoCredentialsMatch` |

### Step 10.5: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 10.6: Commit

```bash
git add security/src/test/java/com/akademiaplus/passkey/
git commit -m "test(security): add passkey registration, challenge store, and repository tests

PasskeyRegistrationUseCaseTest — covers options generation,
registration completion, and error paths.
PasskeyChallengeStoreTest — covers storage, consumption, and expiry.
PasskeyCredentialRepositoryAdapterTest — covers credential and
user handle lookups."
```

---

## Phase 11: Unit Tests — Application Module

### Read first

```bash
cat application/src/test/java/com/akademiaplus/usecases/RegistrationUseCaseTest.java 2>/dev/null
cat application/src/test/java/com/akademiaplus/oauth/usecases/OAuthAuthenticationUseCaseTest.java 2>/dev/null
ls application/src/test/java/com/akademiaplus/
```

Follow the existing application module test patterns.

### Step 11.1: Create test directories

```bash
mkdir -p application/src/test/java/com/akademiaplus/passkey/usecases
mkdir -p application/src/test/java/com/akademiaplus/passkey/interfaceadapters
mkdir -p application/src/test/java/com/akademiaplus/passkey/config
```

### Step 11.2: PasskeyAuthenticationUseCaseTest

**File**: `application/src/test/java/com/akademiaplus/passkey/usecases/PasskeyAuthenticationUseCaseTest.java`

- `@ExtendWith(MockitoExtension.class)`
- All dependencies mocked
- Constructor injection in `@BeforeEach`

Constants:
```java
public static final Long TEST_TENANT_ID = 100L;
public static final Long TEST_USER_ID = 1L;
public static final String TEST_USERNAME = "john.doe@test.com";
public static final String TEST_JWT = "eyJhbGciOiJFUzI1NiJ9.test.jwt";
public static final String TEST_CHALLENGE_BASE64 = "dGVzdC1jaGFsbGVuZ2U";
public static final Long INITIAL_SIGN_COUNT = 5L;
public static final Long UPDATED_SIGN_COUNT = 6L;
public static final Long REGRESSED_SIGN_COUNT = 3L;
```

| @Nested | Tests |
|---------|-------|
| `LoginOptionsGeneration` | `shouldReturnAssertionRequest_whenCalled`, `shouldSetTenantContext_whenGeneratingOptions`, `shouldStoreChallengeWithLoginOperation_whenOptionsGenerated` |
| `LoginCompletion` | `shouldIssueJwt_whenAssertionSucceeds`, `shouldUpdateSignCount_whenLoginCompletes`, `shouldUpdateLastUsedAt_whenLoginCompletes`, `shouldIncludePasskeyAuthMethodClaim_whenIssuingJwt` |
| `SignCountValidation` | `shouldThrowPasskeyAuthenticationException_whenSignCountRegresses` |
| `ErrorPaths` | `shouldThrowPasskeyAuthenticationException_whenAssertionFails`, `shouldThrowPasskeyAuthenticationException_whenChallengeExpired`, `shouldThrowPasskeyAuthenticationException_whenCredentialNotFound` |

### Step 11.3: PasskeyControllerTest

**File**: `application/src/test/java/com/akademiaplus/passkey/interfaceadapters/PasskeyControllerTest.java`

Standalone MockMvc — no Spring context:
- `@ExtendWith(MockitoExtension.class)`
- `@Mock PasskeyRegistrationUseCase registrationUseCase`
- `@Mock PasskeyAuthenticationUseCase authenticationUseCase`
- `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(advice).build()`

| @Nested | Tests |
|---------|-------|
| `RegisterOptions` | `shouldReturn200_whenRegistrationOptionsRequested` |
| `RegisterComplete` | `shouldReturn200_whenRegistrationSucceeds`, `shouldReturn400_whenRegistrationFails` |
| `LoginOptions` | `shouldReturn200_whenLoginOptionsRequested` |
| `LoginComplete` | `shouldReturn200WithJwt_whenLoginSucceeds`, `shouldReturn401_whenLoginFails` |

### Step 11.4: PasskeyControllerAdviceTest

**File**: `application/src/test/java/com/akademiaplus/passkey/config/PasskeyControllerAdviceTest.java`

### Read first

```bash
cat security/src/test/java/com/akademiaplus/config/SecurityControllerAdviceTest.java 2>/dev/null
```

Follow the existing controller advice test pattern:
- `@ExtendWith(MockitoExtension.class)`
- `@Mock MessageService messageService`

| @Nested | Tests |
|---------|-------|
| `RegistrationFailure` | `shouldReturn400WithRegistrationFailedCode_whenPasskeyRegistrationExceptionThrown` |
| `AuthenticationFailure` | `shouldReturn401WithAuthenticationFailedCode_whenPasskeyAuthenticationExceptionThrown` |

### Step 11.5: Compile + test

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl application -am -f platform-core-api/pom.xml
```

### Step 11.6: Commit

```bash
git add application/src/test/java/com/akademiaplus/passkey/
git commit -m "test(application): add passkey authentication use case, controller, and advice tests

PasskeyAuthenticationUseCaseTest — covers login options, login
completion, JWT issuance, sign count update, and error paths.
PasskeyControllerTest — covers all 4 endpoints with success
and failure scenarios.
PasskeyControllerAdviceTest — covers registration (400) and
authentication (401) failure handling."
```

---

## Phase 12: Component Tests

### Read first

```bash
find application/src/test -name "*ComponentTest.java" | head -5
cat <first-result>
find application/src/test -name "AbstractIntegrationTest.java" -o -name "AbstractComponentTest.java" | head -1
cat <result>
```

Understand: `AbstractIntegrationTest`, `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles`, Testcontainers setup.

### Step 12.1: PasskeyComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/PasskeyComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`, `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`
- Requires embedded Redis (use `@TestPropertySource` or Testcontainers Redis)
- May need `@MockitoBean` for `RelyingParty` to avoid real WebAuthn crypto in tests — or use `webauthn-server-core` test utilities to construct valid responses

**@Nested classes**:

| @Nested | Tests |
|---------|-------|
| `Registration` | `shouldReturn200WithCreationOptions_whenRegisterOptionsRequested`, `shouldStoreCredentialInDatabase_whenRegistrationCompletes` |
| `Authentication` | `shouldReturn200WithJwt_whenPasskeyLoginSucceeds`, `shouldUpdateSignCount_whenLoginCompletes` |
| `ChallengeExpiry` | `shouldReturn401_whenChallengeHasExpired` |
| `ErrorPaths` | `shouldReturn400_whenRegistrationResponseIsInvalid`, `shouldReturn401_whenAssertionResponseIsInvalid` |

**Note**: WebAuthn component tests are complex because they require simulating the authenticator's response. Two approaches:

1. **Mock RelyingParty**: Use `@MockitoBean RelyingParty` to return controlled `RegistrationResult` and `AssertionResult` objects. This tests the controller → use case → repository → DB stack without WebAuthn crypto.

2. **Real WebAuthn**: Use Yubico's test utilities to construct valid CBOR-encoded attestation/assertion objects. This tests the full stack including crypto validation but is significantly more complex.

Approach 1 is recommended for the initial implementation.

### Step 12.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

### Step 12.3: Commit

```bash
git add application/src/test/java/com/akademiaplus/usecases/PasskeyComponentTest.java
git commit -m "test(application): add passkey component test

PasskeyComponentTest — full Spring context + Testcontainers MariaDB
+ embedded Redis. Covers registration flow, authentication flow,
challenge expiry, and invalid response handling.
Simulates WebAuthn authenticator responses via mocked RelyingParty."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — registration, challenge store, repository tests green
- [ ] `mvn test -pl application -am -f platform-core-api/pom.xml` — authentication use case + controller + advice tests green
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests green
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] `PasskeyRegistrationUseCase` in `security` module (no cross-module calls)
- [ ] `PasskeyAuthenticationUseCase` in `application` module (cross-module orchestrator)
- [ ] No `new PasskeyCredentialDataModel()` — all via `applicationContext.getBean()`
- [ ] Composite key `(tenantId, passkeyCredentialId)` follows existing entity pattern
- [ ] Redis challenge store uses 5-minute TTL via `PasskeyProperties.challengeTtlSeconds`
- [ ] Challenge consumed (deleted) after use — single-use enforcement
- [ ] Login endpoints (`/passkey/login/**`) are `permitAll` in SecurityConfig
- [ ] Registration endpoints (`/passkey/register/**`) require authentication
- [ ] Sign count regression throws `PasskeyAuthenticationException`
- [ ] `java-webauthn-server` dependency in security module POM only
- [ ] No `any()` matchers in any test — all stubbing uses exact values
- [ ] JWT contains `auth_method=passkey` claim
