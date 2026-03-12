# JWT Refresh Token Rotation -- Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/jwt-refresh-token-rotation-workflow.md` -- read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 -> 2 -> ... -> 10). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header (year 2026).
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals -> `public static final` constants, shared between impl and tests.
9. Use `applicationContext.getBean()` for all entity instantiation -- never `new EntityDataModel()`.
10. Read existing files BEFORE modifying -- field names, import paths, and CompositeId class names vary.
11. Commit after each phase using the commit message provided.
12. Use `Long` for ALL IDs -- never `Integer`.

---

## Phase 1: Redis Infrastructure

### Read first

```bash
cat security/pom.xml
cat application/src/main/resources/application.properties
```

Identify:
- Where to add the `spring-boot-starter-data-redis` dependency
- Existing property naming conventions

### Step 1.1: Add Redis dependency

**File**: `security/pom.xml`

Add to the `<dependencies>` section:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Step 1.2: Add configuration properties

**File**: `application/src/main/resources/application.properties`

Add at the end:
```properties
# Redis Configuration
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=2000ms

# Cookie Configuration
security.cookie.domain=${COOKIE_DOMAIN:localhost}
security.cookie.secure=${COOKIE_SECURE:true}
security.cookie.access-token-name=access_token
security.cookie.refresh-token-name=refresh_token
security.cookie.access-token-max-age-seconds=900
security.cookie.refresh-token-max-age-seconds=2592000

# Refresh Token Configuration
jwt.refresh-token.validity-ms=2592000000
jwt.refresh-token.keystore.alias=${JWT_REFRESH_KEY_ALIAS:refresh-key}
```

### Step 1.3: Create AkademiaPlusRedisConfig

**File**: `security/src/main/java/com/akademiaplus/config/AkademiaPlusRedisConfig.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for access token session management.
 *
 * <p>Provides a {@link RedisTemplate} configured with string serializers
 * for storing JWT session metadata (jti to userId+tenantId mappings).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class AkademiaPlusRedisConfig {

    /**
     * Creates a RedisTemplate with string key and value serializers.
     *
     * @param connectionFactory the Redis connection factory
     * @return configured RedisTemplate for session storage
     */
    @Bean
    public RedisTemplate<String, String> akademiaPlusRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

### Step 1.4: Create AkademiaPlusRedisSessionStore

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/session/AkademiaPlusRedisSessionStore.java`

```bash
mkdir -p security/src/main/java/com/akademiaplus/internal/interfaceadapters/session
```

Create the `AkademiaPlusRedisSessionStore` service. See the workflow document Phase 1 Step 1.4 for the full implementation.

Key methods:
- `storeSession(String jti, String username, Long tenantId, Duration ttl)` -- stores session metadata with TTL
- `isSessionValid(String jti)` -- checks if session key exists
- `revokeSession(String jti)` -- deletes single session
- `revokeAllSessionsForUser(String username, Long tenantId)` -- deletes all sessions for user+tenant

Constants:
```java
public static final String SESSION_KEY_PREFIX = "session:";
public static final String USER_SESSIONS_KEY_PREFIX = "user_sessions:";
public static final String FIELD_USER_ID = "userId";
public static final String FIELD_TENANT_ID = "tenantId";
public static final String FIELD_USERNAME = "username";
```

### Step 1.5: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 1.6: Commit

```bash
git add security/pom.xml security/src/main/java/com/akademiaplus/config/AkademiaPlusRedisConfig.java security/src/main/java/com/akademiaplus/internal/interfaceadapters/session/ application/src/main/resources/application.properties
git commit -m "feat(security): add Redis infrastructure for session management

Add spring-boot-starter-data-redis dependency, AkademiaPlusRedisConfig with
StringRedisSerializer template, and AkademiaPlusRedisSessionStore service for
access token session tracking and revocation."
```

---

## Phase 2: RefreshTokenDataModel + Repository

### Read first

```bash
cat multi-tenant-data/src/main/java/com/akademiaplus/security/CustomerAuthDataModel.java
```

Find existing composite ID patterns:
```bash
grep -rn "class.*CompositeId\|@EmbeddedId" multi-tenant-data/src/main/java/ | head -10
```

Find the schema file:
```bash
find . -name "*.sql" | grep -i "db_init\|schema\|migration" | head -10
```

Understand:
- How composite IDs are defined (field names, `@Embeddable` class structure)
- Which Lombok annotations entities use
- The existing entity pattern (soft delete, audit fields, etc.)

### Step 2.1: Create RefreshTokenCompositeId

Follow the same pattern as existing composite IDs. The ID should have `tenantId` (Long) and `refreshTokenId` (Long).

### Step 2.2: Create RefreshTokenDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/security/RefreshTokenDataModel.java`

Follow the entity pattern from `CustomerAuthDataModel`:
- `@EmbeddedId` with `RefreshTokenCompositeId`
- Fields: `tokenHash` (VARCHAR 64, unique), `familyId` (VARCHAR 36), `userId` (Long), `username` (VARCHAR 255), `expiresAt` (Instant), `revokedAt` (Instant, nullable), `replacedByTokenHash` (VARCHAR 64, nullable), `createdAt` (Instant)
- All column name strings as `public static final` constants
- Javadoc on the class

**IMPORTANT**: Check if entities use `@Scope("prototype")` and if they extend a base class. Match the existing pattern exactly.

### Step 2.3: Create RefreshTokenRepository

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/RefreshTokenRepository.java`

```java
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenDataModel, RefreshTokenCompositeId> {

    Optional<RefreshTokenDataModel> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenDataModel r SET r.revokedAt = :revokedAt WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
    void revokeAllByFamilyId(@Param("familyId") String familyId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("UPDATE RefreshTokenDataModel r SET r.revokedAt = :revokedAt WHERE r.userId = :userId AND r.id.tenantId = :tenantId AND r.revokedAt IS NULL")
    void revokeAllByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("revokedAt") Instant revokedAt);
}
```

**IMPORTANT**: Check if the existing repositories extend `TenantScopedRepository` or `JpaRepository`. Match the same base interface. Read `CustomerAuthRepository` for reference.

### Step 2.4: Add DB schema

Add the `refresh_tokens` table definition to the schema file:

```sql
CREATE TABLE refresh_tokens (
    tenant_id           BIGINT       NOT NULL,
    refresh_token_id    BIGINT       NOT NULL,
    token_hash          VARCHAR(64)  NOT NULL,
    family_id           VARCHAR(36)  NOT NULL,
    user_id             BIGINT       NOT NULL,
    username            VARCHAR(255) NOT NULL,
    expires_at          TIMESTAMP(6) NOT NULL,
    revoked_at          TIMESTAMP(6) NULL,
    replaced_by_token_hash VARCHAR(64) NULL,
    created_at          TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (tenant_id, refresh_token_id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    INDEX idx_refresh_token_family (family_id),
    INDEX idx_refresh_token_user (tenant_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

### Step 2.5: Compile

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 2.6: Commit

```bash
git add multi-tenant-data/src/main/java/com/akademiaplus/security/ security/src/main/java/com/akademiaplus/internal/interfaceadapters/RefreshTokenRepository.java db_init/
git commit -m "feat(multi-tenant-data): add RefreshTokenDataModel for token rotation

Add refresh_tokens table with composite key (tenantId + refreshTokenId),
tokenHash (SHA-256, unique), familyId (UUID for rotation chain),
userId, expiresAt, revokedAt, replacedByTokenHash, createdAt.
Add RefreshTokenRepository with family-based revocation queries."
```

---

## Phase 3: JwtTokenProvider Enhancements + CookieService

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/KeyLoader.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/InternalAuthController.java
cat security/src/main/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCase.java
```

Understand:
- How the existing `createToken()` method works
- How the key pair is loaded
- How InternalAuthController returns the token response
- How InternalAuthenticationUseCase builds the response DTO

### Step 3.1: Enhance JwtTokenProvider

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java`

Add constants:
```java
public static final String JTI_CLAIM = "jti";
public static final String TOKEN_TYPE_CLAIM = "token_type";
public static final String TOKEN_TYPE_ACCESS = "access";
public static final String TOKEN_TYPE_REFRESH = "refresh";
public static final String FAMILY_ID_CLAIM = "family_id";
```

Add `@Value("${jwt.refresh-token.validity-ms}")` field.

Add methods:
- `createAccessToken(String username, Long tenantId, Map<String, Object> additionalClaims)` -- same as `createToken` but adds `jti` (UUID) and `token_type=access` claims
- `createRefreshToken(String username, Long tenantId, String familyId)` -- longer validity, adds `family_id` and `token_type=refresh` claims
- `getJti(String token)` -- extracts JTI from claims
- `getTokenType(String token)` -- extracts token type from claims
- `getAccessTokenValidityInMs()` -- returns access token TTL

**IMPORTANT**: Keep the existing `createToken()` method unchanged for backward compatibility. The new `createAccessToken()` is an enhanced version.

### Step 3.2: Create CookieService

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/CookieService.java`

See workflow Phase 3 Step 3.2 for full implementation.

Key methods:
- `addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken)`
- `clearTokenCookies(HttpServletResponse response)`
- `extractAccessToken(HttpServletRequest request)` -> `Optional<String>`
- `extractRefreshToken(HttpServletRequest request)` -> `Optional<String>`

Constants:
```java
public static final String ACCESS_TOKEN_PATH = "/v1";
public static final String REFRESH_TOKEN_PATH = "/v1/security/token";
public static final String SAME_SITE_STRICT = "Strict";
public static final String SET_COOKIE_HEADER = "Set-Cookie";
```

Uses `ResponseCookie.from()` for building cookies with `httpOnly(true)`, `secure(configurable)`, `sameSite("Strict")`.

### Step 3.3: Modify InternalAuthController

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/InternalAuthController.java`

Add `CookieService` as a constructor dependency. After login, set cookies on the `HttpServletResponse`:

```java
@Override
public ResponseEntity<AuthTokenResponseDTO> loginInternal(LoginRequestDTO loginRequestDTO) {
    AuthTokenResponseDTO response = internalAuthenticationUseCase.login(loginRequestDTO);
    // Set cookies for the new token delivery mechanism
    // The access token and refresh token should be set as cookies
    // The JSON body response is kept for backward compatibility
    return ResponseEntity.ok(response);
}
```

**Note**: The `InternalAuthenticationUseCase` needs to be modified to also generate refresh tokens and store Redis sessions. However, if this creates too large a change, defer the full integration and keep the cookie setting logic in the controller for now.

Add `HttpServletResponse` as a method parameter -- this may require modifying the generated OpenAPI interface or using `@RequestMapping` directly. Read the generated interface first.

### Step 3.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/
git commit -m "feat(security): add refresh token generation and cookie-based delivery

Enhance JwtTokenProvider with createAccessToken (jti claim),
createRefreshToken (familyId claim), and token type claims.
Add CookieService for HttpOnly/Secure/SameSite=Strict cookie
management. Modify InternalAuthController to set token cookies."
```

---

## Phase 4: Token Refresh Endpoint + Use Case

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCase.java
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
cat utilities/src/main/java/com/akademiaplus/utilities/security/HashingService.java
```

### Step 4.1: Create TokenRefreshResult record

**File**: `security/src/main/java/com/akademiaplus/internal/usecases/domain/TokenRefreshResult.java`

```bash
mkdir -p security/src/main/java/com/akademiaplus/internal/usecases/domain
```

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases.domain;

/**
 * Result of a successful token refresh operation.
 *
 * @param accessToken  the new JWT access token
 * @param refreshToken the new JWT refresh token
 * @param username     the authenticated username
 * @author ElatusDev
 * @since 1.0
 */
public record TokenRefreshResult(String accessToken, String refreshToken, String username) {
}
```

### Step 4.2: Create TokenRefreshUseCase

**File**: `security/src/main/java/com/akademiaplus/internal/usecases/TokenRefreshUseCase.java`

Dependencies (constructor-injected):
- `JwtTokenProvider`
- `RefreshTokenRepository`
- `AkademiaPlusRedisSessionStore`
- `HashingService`
- `ApplicationContext`

Constants:
```java
public static final String ERROR_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found";
public static final String ERROR_REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
public static final String ERROR_TOKEN_REUSE_DETECTED = "Token reuse detected — all tokens in family revoked";
```

Method `refresh(String currentRefreshToken)`:
1. Hash the incoming refresh token: `hashingService.generateHash(currentRefreshToken)`
2. Lookup `RefreshTokenDataModel` by `tokenHash` -- if not found, throw `RefreshTokenExpiredException`
3. **Reuse check**: If `revokedAt != null`, the token was already consumed:
   - `refreshTokenRepository.revokeAllByFamilyId(existingToken.getFamilyId(), Instant.now())`
   - `redisSessionStore.revokeAllSessionsForUser(existingToken.getUsername(), existingToken.getId().getTenantId())`
   - Throw `TokenReuseDetectedException(existingToken.getFamilyId())`
4. **Expiry check**: If `expiresAt.isBefore(Instant.now())`, throw `RefreshTokenExpiredException`
5. Mark old token as consumed: `existingToken.setRevokedAt(Instant.now())`
6. Generate new access token: `jwtTokenProvider.createAccessToken(username, tenantId, claims)`
7. Generate new refresh token: `jwtTokenProvider.createRefreshToken(username, tenantId, existingToken.getFamilyId())`
8. Hash new refresh token, create new `RefreshTokenDataModel` via `applicationContext.getBean()`
9. Set `replacedByTokenHash` on old token
10. Store new Redis session: `redisSessionStore.storeSession(jti, username, tenantId, duration)`
11. Return `TokenRefreshResult(newAccessToken, newRefreshToken, username)`

### Step 4.3: Create TokenRefreshController

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/TokenRefreshController.java`

```java
@RestController
@RequestMapping("/v1/security/token")
public class TokenRefreshController {

    public static final String ERROR_NO_REFRESH_TOKEN = "No refresh token cookie present";

    private final TokenRefreshUseCase tokenRefreshUseCase;
    private final CookieService cookieService;

    // constructor

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.extractRefreshToken(request)
                .orElseThrow(() -> new RefreshTokenExpiredException(ERROR_NO_REFRESH_TOKEN));

        TokenRefreshResult result = tokenRefreshUseCase.refresh(refreshToken);
        cookieService.addTokenCookies(response, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok().build();
    }
}
```

### Step 4.4: Update SecurityConfig

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add to the `authorizeHttpRequests` block:
```java
.requestMatchers("/v1/security/token/refresh").permitAll()
.requestMatchers("/v1/security/logout").permitAll()
```

Update CORS config: set `allowCredentials(true)` on default CORS configuration to support cookie transmission.

### Step 4.5: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 4.6: Commit

```bash
git add security/src/main/java/com/akademiaplus/internal/ security/src/main/java/com/akademiaplus/config/SecurityConfig.java
git commit -m "feat(security): add token refresh endpoint with rotation

Add TokenRefreshUseCase with refresh token validation, rotation,
and new token pair issuance. Add TokenRefreshController that reads
refresh token from HttpOnly cookie and sets new cookies.
Update SecurityConfig to permit /v1/security/token/refresh and
/v1/security/logout endpoints."
```

---

## Phase 5: Reuse Detection Exceptions

### Step 5.1: Create exception directory

```bash
mkdir -p security/src/main/java/com/akademiaplus/internal/exceptions
```

### Step 5.2: Create TokenReuseDetectedException

**File**: `security/src/main/java/com/akademiaplus/internal/exceptions/TokenReuseDetectedException.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.exceptions;

/**
 * Thrown when a previously consumed refresh token is reused, indicating
 * a potential token theft. All tokens in the family are revoked.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class TokenReuseDetectedException extends RuntimeException {

    public static final String ERROR_MESSAGE = "Refresh token reuse detected for family %s — all tokens revoked";

    /**
     * Constructs a TokenReuseDetectedException for the given family.
     *
     * @param familyId the compromised token family ID
     */
    public TokenReuseDetectedException(String familyId) {
        super(String.format(ERROR_MESSAGE, familyId));
    }
}
```

### Step 5.3: Create RefreshTokenExpiredException

**File**: `security/src/main/java/com/akademiaplus/internal/exceptions/RefreshTokenExpiredException.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.exceptions;

/**
 * Thrown when a refresh token has expired or cannot be found.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class RefreshTokenExpiredException extends RuntimeException {

    public static final String ERROR_MESSAGE = "Refresh token expired or not found";

    /**
     * Constructs a RefreshTokenExpiredException with the default message.
     */
    public RefreshTokenExpiredException() {
        super(ERROR_MESSAGE);
    }

    /**
     * Constructs a RefreshTokenExpiredException with a custom message.
     *
     * @param message the detail message
     */
    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
```

### Step 5.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 5.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/internal/exceptions/
git commit -m "feat(security): add refresh token reuse detection exceptions

Add TokenReuseDetectedException for family-wide revocation on
token replay. Add RefreshTokenExpiredException for expired or
missing refresh tokens."
```

---

## Phase 6: Logout Endpoint + Use Case

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/CookieService.java
```

### Step 6.1: Create LogoutUseCase

**File**: `security/src/main/java/com/akademiaplus/internal/usecases/LogoutUseCase.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.AkademiaPlusRedisSessionStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles user logout by revoking all refresh tokens and Redis sessions.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AkademiaPlusRedisSessionStore redisSessionStore;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructs a LogoutUseCase with the required dependencies.
     *
     * @param refreshTokenRepository the refresh token repository
     * @param redisSessionStore      the Redis session store
     * @param jwtTokenProvider       the JWT token provider
     */
    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository,
                         AkademiaPlusRedisSessionStore redisSessionStore,
                         JwtTokenProvider jwtTokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisSessionStore = redisSessionStore;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Revokes all tokens and sessions for the authenticated user.
     *
     * @param accessToken the current access token (to extract user identity)
     */
    @Transactional
    public void logout(String accessToken) {
        String username = jwtTokenProvider.getUsername(accessToken);
        Long tenantId = Long.valueOf(jwtTokenProvider.getTenantId(accessToken));

        refreshTokenRepository.revokeAllByUserIdAndTenantId(
                /* userId extracted from claims */ 0L, tenantId, Instant.now());

        redisSessionStore.revokeAllSessionsForUser(username, tenantId);
    }
}
```

**IMPORTANT**: The userId extraction depends on whether we store userId as a claim in the JWT. Read `JwtTokenProvider.createToken()` to check. If userId is not a claim, you may need to look up the user by username first, or add a `user_id` claim to the token.

### Step 6.2: Create LogoutController

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/LogoutController.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.usecases.LogoutUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST controller for the logout endpoint.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security")
public class LogoutController {

    public static final String ERROR_NO_TOKEN_FOR_LOGOUT = "No access token available for logout";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final LogoutUseCase logoutUseCase;
    private final CookieService cookieService;

    /**
     * Constructs a LogoutController with the required dependencies.
     *
     * @param logoutUseCase the logout use case
     * @param cookieService the cookie service
     */
    public LogoutController(LogoutUseCase logoutUseCase, CookieService cookieService) {
        this.logoutUseCase = logoutUseCase;
        this.cookieService = cookieService;
    }

    /**
     * Logs out the current user by revoking all tokens and clearing cookies.
     *
     * @param request  the HTTP request containing the access token
     * @param response the HTTP response where cookies will be cleared
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = cookieService.extractAccessToken(request)
                .or(() -> extractBearerToken(request))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_NO_TOKEN_FOR_LOGOUT));

        logoutUseCase.logout(accessToken);
        cookieService.clearTokenCookies(response);

        return ResponseEntity.noContent().build();
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
```

### Step 6.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 6.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/internal/usecases/LogoutUseCase.java security/src/main/java/com/akademiaplus/internal/interfaceadapters/LogoutController.java
git commit -m "feat(security): add logout endpoint with full revocation

Add LogoutUseCase that revokes all refresh tokens and Redis sessions
for the user. Add LogoutController at POST /v1/security/logout that
clears HttpOnly cookies and delegates to LogoutUseCase."
```

---

## Phase 7: JwtRequestFilter Cookie + Redis Support

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
```

### Step 7.1: Modify JwtRequestFilter

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java`

Add constructor dependencies:
- `CookieService cookieService`
- `AkademiaPlusRedisSessionStore redisSessionStore`

Add constants:
```java
public static final String AUTHORIZATION_HEADER = "Authorization";
public static final String BEARER_PREFIX = "Bearer ";
```

Modify `doFilterInternal()`:

1. **Cookie first**: `Optional<String> cookieToken = cookieService.extractAccessToken(request)`
2. **Header fallback**: If no cookie, check `Authorization: Bearer` header
3. **Validate**: If token found, call `jwtTokenProvider.validateToken(jwtToken)`
4. **Redis check**: Extract `jti` via `jwtTokenProvider.getJti(jwtToken)`. If `jti` is present and `!redisSessionStore.isSessionValid(jti)`, skip authentication (session revoked). Handle the case where `jti` is null (old tokens without jti claim -- allow through for backward compatibility).
5. **Set context**: Same as current implementation

### Step 7.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 7.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
git commit -m "feat(security): add cookie and Redis session support to JwtRequestFilter

Modify JwtRequestFilter to read access token from HttpOnly cookie
first, then fall back to Authorization header for backward
compatibility. Add Redis session validation to reject revoked tokens."
```

---

## Phase 8: Unit Tests (Security Module)

### Read first

```bash
find security/src/test -name "*Test.java" | head -5
cat <first-result>
```

Follow existing test patterns: `@ExtendWith(MockitoExtension.class)`, `@Nested`, `@DisplayName`, Given-When-Then comments.

### Step 8.1: Create test directories

```bash
mkdir -p security/src/test/java/com/akademiaplus/internal/interfaceadapters/session
mkdir -p security/src/test/java/com/akademiaplus/internal/interfaceadapters/jwt
mkdir -p security/src/test/java/com/akademiaplus/internal/usecases
mkdir -p security/src/test/java/com/akademiaplus/internal/interfaceadapters
```

### Step 8.2: AkademiaPlusAkademiaPlusRedisSessionStoreTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/session/AkademiaPlusAkademiaPlusRedisSessionStoreTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock RedisTemplate<String, String>`
- `@Mock SetOperations<String, String>`, `@Mock HashOperations<String, String, String>`
- Constants for all test values (jti, username, tenantId)

| @Nested | Tests |
|---------|-------|
| `SessionStorage` | `shouldStoreSession_whenValidParametersProvided`, `shouldSetTtlOnSessionKey_whenStoring` |
| `SessionValidation` | `shouldReturnTrue_whenSessionExists`, `shouldReturnFalse_whenSessionDoesNotExist` |
| `SessionRevocation` | `shouldDeleteSessionKey_whenRevokingSingleSession`, `shouldDeleteAllSessionsForUser_whenRevokingByUser` |

### Step 8.3: CookieServiceTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/jwt/CookieServiceTest.java`

- Use `MockHttpServletRequest` and `MockHttpServletResponse` from Spring Test
- Set `@Value` fields via reflection or test constructor

| @Nested | Tests |
|---------|-------|
| `CookieCreation` | `shouldAddAccessTokenCookie_whenTokenProvided`, `shouldAddRefreshTokenCookie_whenTokenProvided`, `shouldSetHttpOnlyFlag_whenCreatingCookie`, `shouldSetSameSiteStrict_whenCreatingCookie` |
| `CookieClearing` | `shouldSetMaxAgeToZero_whenClearingCookies` |
| `CookieExtraction` | `shouldExtractAccessToken_whenCookiePresent`, `shouldReturnEmpty_whenNoCookiesPresent`, `shouldExtractRefreshToken_whenCookiePresent` |

### Step 8.4: TokenRefreshUseCaseTest

**File**: `security/src/test/java/com/akademiaplus/internal/usecases/TokenRefreshUseCaseTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock JwtTokenProvider`, `@Mock RefreshTokenRepository`, `@Mock AkademiaPlusRedisSessionStore`, `@Mock HashingService`, `@Mock ApplicationContext`

| @Nested | Tests |
|---------|-------|
| `SuccessfulRotation` | `shouldIssueNewAccessToken_whenRefreshTokenIsValid`, `shouldIssueNewRefreshToken_whenRefreshTokenIsValid`, `shouldMarkOldTokenAsConsumed_whenRotating`, `shouldCreateNewRefreshTokenEntity_whenRotating`, `shouldPreserveFamilyId_whenRotating`, `shouldStoreNewSessionInRedis_whenRotating` |
| `ReuseDetection` | `shouldRevokeAllFamilyTokens_whenConsumedTokenReused`, `shouldRevokeAllRedisSessionsForUser_whenReuseDetected`, `shouldThrowTokenReuseDetectedException_whenConsumedTokenReused` |
| `ExpiredToken` | `shouldThrowRefreshTokenExpiredException_whenTokenExpired`, `shouldThrowRefreshTokenExpiredException_whenTokenNotFound` |

### Step 8.5: LogoutUseCaseTest

**File**: `security/src/test/java/com/akademiaplus/internal/usecases/LogoutUseCaseTest.java`

| @Nested | Tests |
|---------|-------|
| `SuccessfulLogout` | `shouldRevokeAllRefreshTokens_whenLoggingOut`, `shouldRevokeAllRedisSessions_whenLoggingOut` |

### Step 8.6: TokenRefreshControllerTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/TokenRefreshControllerTest.java`

- Standalone MockMvc: `MockMvcBuilders.standaloneSetup(controller).build()`
- `@Mock TokenRefreshUseCase`, `@Mock CookieService`

| @Nested | Tests |
|---------|-------|
| `RefreshEndpoint` | `shouldReturn200_whenRefreshSucceeds`, `shouldReturn401_whenNoRefreshTokenCookie` |

### Step 8.7: LogoutControllerTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/LogoutControllerTest.java`

| @Nested | Tests |
|---------|-------|
| `LogoutEndpoint` | `shouldReturn204_whenLogoutSucceeds`, `shouldClearCookies_whenLogoutSucceeds`, `shouldReadFromAuthorizationHeader_whenNoCookie` |

### Step 8.8: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 8.9: Commit

```bash
git add security/src/test/
git commit -m "test(security): add unit tests for refresh token rotation and logout

AkademiaPlusAkademiaPlusRedisSessionStoreTest, CookieServiceTest, TokenRefreshUseCaseTest,
LogoutUseCaseTest, TokenRefreshControllerTest, LogoutControllerTest.
Cover session management, cookie handling, token rotation, reuse
detection, and logout with full revocation."
```

---

## Phase 9: Component Tests

### Read first

```bash
find security/src/test -name "*ComponentTest.java" -o -name "AbstractIntegration*" | head -5
cat <result>
```

If no component test infrastructure exists in security module, check the application module:
```bash
find application/src/test -name "AbstractIntegrationTest.java" | head -1
cat <result>
```

### Step 9.1: TokenRotationComponentTest

**File**: `security/src/test/java/com/akademiaplus/usecases/TokenRotationComponentTest.java`

Full Spring context with:
- Testcontainers MariaDB (via `AbstractIntegrationTest` or manual setup)
- Embedded Redis (use `@TestConfiguration` with `EmbeddedRedisServer` or `testcontainers/redis`)
- `@AutoConfigureMockMvc`

| @Nested | Tests |
|---------|-------|
| `LoginWithCookies` | `shouldSetAccessTokenCookie_whenLoginSucceeds`, `shouldSetRefreshTokenCookie_whenLoginSucceeds` |
| `TokenRefresh` | `shouldReturn200_whenRefreshingWithValidToken`, `shouldIssueNewTokenPair_whenRefreshing`, `shouldInvalidateOldRefreshToken_whenRefreshing` |
| `ReuseDetection` | `shouldReturn401_whenReplayingConsumedRefreshToken`, `shouldRevokeAllFamilyTokens_whenReuseDetected` |
| `Logout` | `shouldReturn204_whenLoggingOut`, `shouldInvalidateAllTokens_whenLoggingOut`, `shouldClearCookies_whenLoggingOut`, `shouldRejectAccessToken_afterLogout` |

### Step 9.2: Compile + verify

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl security -am -f platform-core-api/pom.xml
```

### Step 9.3: Commit

```bash
git add security/src/test/
git commit -m "test(security): add token rotation component test

TokenRotationComponentTest — full Spring context + Testcontainers
MariaDB + embedded Redis. Covers login cookie delivery, token
refresh with rotation, reuse detection, logout with full revocation."
```

---

## Phase 10: E2E Tests

See separate E2E prompt document. Defer to platform-api-e2e module.

### Commit

```bash
git commit -m "test(e2e): add refresh token rotation and logout E2E tests

Cover login with cookies, token refresh rotation, reuse detection
(replaying consumed token), logout, and post-logout access rejection."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` -- full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` -- all unit tests green
- [ ] `mvn verify -pl security -am -f platform-core-api/pom.xml` -- component tests green
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] All entity instantiation via `applicationContext.getBean()` -- no `new EntityDataModel()`
- [ ] All IDs are `Long` -- no `Integer`
- [ ] Redis connection properties use environment variable defaults
- [ ] Cookie `SameSite=Strict`, `HttpOnly=true`, `Secure=true` (configurable per profile)
- [ ] Refresh token hash stored -- never the raw token
- [ ] Reuse detection revokes entire token family
- [ ] `JwtRequestFilter` reads cookie first, then `Authorization` header
- [ ] Backward compatibility: existing `createToken()` method still works
- [ ] Conventional Commits with no AI attribution
