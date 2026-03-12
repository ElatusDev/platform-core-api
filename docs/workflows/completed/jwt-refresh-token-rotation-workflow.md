# JWT Refresh Token Rotation Workflow

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.

---

## 1. Architecture Overview

### 1.1 Current State

The platform uses stateless JWT access tokens delivered via the `Authorization: Bearer` header. There are no refresh tokens, no Redis session store, and no server-side revocation mechanism. Logout is client-side only (discard the token). Access token validity is configured via `jwt.token.validity-ms`.

| Component | Location | State |
|-----------|----------|-------|
| `JwtTokenProvider` | `security/.../internal/interfaceadapters/jwt/` | `createToken(username, tenantId, claims)` -- signs with EC/RSA key pair, access tokens only |
| `JwtRequestFilter` | `security/.../internal/interfaceadapters/jwt/` | Reads `Authorization: Bearer` header, validates, sets `SecurityContextHolder` |
| `SecurityConfig` | `security/.../config/` | Single `SecurityFilterChain`, permits `/login/internal`, `/register`, Swagger, actuator |
| `InternalAuthController` | `security/.../internal/interfaceadapters/` | Returns `AuthTokenResponseDTO` with access token in JSON body |
| Redis | -- | Not present in the codebase |
| Refresh tokens | -- | Not implemented |
| Cookie-based delivery | -- | Not implemented |

### 1.2 What's Missing

1. **Refresh token generation**: No refresh token is issued alongside the access token
2. **Refresh token entity**: No `RefreshTokenDataModel` to persist token families for rotation tracking
3. **Redis session store**: No Redis integration for access token revocation or session metadata
4. **Cookie-based delivery**: Tokens are delivered in JSON body, not HttpOnly cookies
5. **Token rotation endpoint**: No `POST /v1/security/token/refresh` endpoint
6. **Reuse detection**: No family-based rotation tracking to detect token theft
7. **Logout endpoint**: No `POST /v1/security/logout` to revoke all tokens and clear cookies
8. **Cookie extraction in filter**: `JwtRequestFilter` only reads `Authorization` header, not cookies

### 1.3 Target Architecture

```
Login/OAuth POST /v1/security/login/*
  |
  +---> JwtTokenProvider.createAccessToken(username, tenantId, claims)
  |       |---> Short-lived JWT (15 min)
  |       |---> Stored in Redis as session metadata (jti -> userId+tenantId)
  |
  +---> JwtTokenProvider.createRefreshToken(username, tenantId, familyId)
  |       |---> Long-lived opaque token (30 days)
  |       |---> SHA-256 hash stored in RefreshTokenDataModel
  |       |---> familyId (UUID) links all tokens in a rotation chain
  |
  +---> Response: Set-Cookie headers
          |---> access_token: HttpOnly, Secure, SameSite=Strict, Path=/v1, Max-Age=900
          |---> refresh_token: HttpOnly, Secure, SameSite=Strict, Path=/v1/security/token, Max-Age=2592000

Token Refresh POST /v1/security/token/refresh
  |
  +---> Read refresh_token cookie
  +---> Hash token, lookup RefreshTokenDataModel by tokenHash
  +---> REUSE DETECTION: if token already consumed (revokedAt != null)
  |       |---> Revoke ALL tokens in family (breach detected)
  |       |---> Delete all Redis sessions for the user
  |       |---> Return 401
  +---> VALID: issue new access + refresh pair
  |       |---> Mark old refresh token as consumed (set revokedAt, replacedByTokenHash)
  |       |---> Create new RefreshTokenDataModel with same familyId
  |       |---> Set new cookies

Logout POST /v1/security/logout
  |
  +---> Read access_token cookie (or Authorization header)
  +---> Extract userId + tenantId from JWT
  +---> Revoke all RefreshTokenDataModel entries for userId + tenantId
  +---> Delete all Redis sessions for the user
  +---> Clear cookies (Max-Age=0)

JwtRequestFilter (modified)
  |
  +---> Try 1: Read access_token cookie
  +---> Try 2: Fall back to Authorization: Bearer header (migration support)
  +---> Validate JWT signature + expiration
  +---> Check Redis session store: jti must exist and not be revoked
  +---> Set SecurityContextHolder
```

### 1.4 Cookie Security Properties

| Property | Value | Rationale |
|----------|-------|-----------|
| `HttpOnly` | `true` | Prevents JavaScript access (XSS mitigation) |
| `Secure` | `true` | HTTPS only (production), relaxed in dev profile |
| `SameSite` | `Strict` | Prevents CSRF via cross-site requests |
| `Path` (access) | `/v1` | Available to all API endpoints |
| `Path` (refresh) | `/v1/security/token` | Only sent to token refresh endpoint |
| `Max-Age` (access) | `900` (15 min) | Short-lived, matches JWT expiry |
| `Max-Age` (refresh) | `2592000` (30 days) | Long-lived, stored hashed in DB |

### 1.5 Module Placement

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `RefreshTokenDataModel` | multi-tenant-data | `security/` | Entity with composite key (Hard Rule #13) |
| `RefreshTokenRepository` | security | `internal/interfaceadapters/` | Repository for refresh tokens |
| `AkademiaPlusRedisSessionStore` | security | `internal/interfaceadapters/session/` | Redis access token session management |
| `AkademiaPlusRedisConfig` | security | `config/` | Redis connection + template beans |
| `CookieService` | security | `internal/interfaceadapters/jwt/` | HttpOnly cookie creation/extraction |
| `TokenRefreshUseCase` | security | `internal/usecases/` | Token rotation business logic |
| `TokenRefreshController` | security | `internal/interfaceadapters/` | POST /v1/security/token/refresh |
| `LogoutUseCase` | security | `internal/usecases/` | Logout + revocation logic |
| `LogoutController` | security | `internal/interfaceadapters/` | POST /v1/security/logout |
| `TokenReuseDetectedException` | security | `internal/exceptions/` | Refresh token reuse exception |
| `RefreshTokenExpiredException` | security | `internal/exceptions/` | Expired refresh token exception |

---

## 2. File Inventory

### New files (18)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `multi-tenant-data/.../security/RefreshTokenDataModel.java` | multi-tenant-data | 2 |
| 2 | `security/.../internal/interfaceadapters/RefreshTokenRepository.java` | security | 2 |
| 3 | `security/.../config/AkademiaPlusRedisConfig.java` | security | 1 |
| 4 | `security/.../internal/interfaceadapters/session/AkademiaPlusRedisSessionStore.java` | security | 1 |
| 5 | `security/.../internal/interfaceadapters/jwt/CookieService.java` | security | 3 |
| 6 | `security/.../internal/usecases/TokenRefreshUseCase.java` | security | 4 |
| 7 | `security/.../internal/interfaceadapters/TokenRefreshController.java` | security | 4 |
| 8 | `security/.../internal/usecases/LogoutUseCase.java` | security | 6 |
| 9 | `security/.../internal/interfaceadapters/LogoutController.java` | security | 6 |
| 10 | `security/.../internal/exceptions/TokenReuseDetectedException.java` | security | 5 |
| 11 | `security/.../internal/exceptions/RefreshTokenExpiredException.java` | security | 5 |
| 12 | `security/test/.../internal/interfaceadapters/session/AkademiaPlusAkademiaPlusRedisSessionStoreTest.java` | security | 8 |
| 13 | `security/test/.../internal/interfaceadapters/jwt/CookieServiceTest.java` | security | 8 |
| 14 | `security/test/.../internal/usecases/TokenRefreshUseCaseTest.java` | security | 8 |
| 15 | `security/test/.../internal/usecases/LogoutUseCaseTest.java` | security | 8 |
| 16 | `security/test/.../internal/interfaceadapters/TokenRefreshControllerTest.java` | security | 8 |
| 17 | `security/test/.../internal/interfaceadapters/LogoutControllerTest.java` | security | 8 |
| 18 | DB schema migration file for `refresh_tokens` table | infra | 2 |

### Modified files (7)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `security/pom.xml` | Add `spring-boot-starter-data-redis` dependency | 1 |
| 2 | `security/.../internal/interfaceadapters/jwt/JwtTokenProvider.java` | Add refresh token generation, access token `jti` claim | 3 |
| 3 | `security/.../internal/interfaceadapters/jwt/JwtRequestFilter.java` | Read from cookie first, fall back to header; Redis session check | 7 |
| 4 | `security/.../config/SecurityConfig.java` | Permit `/v1/security/token/refresh` and `/v1/security/logout` | 4 |
| 5 | `security/.../internal/interfaceadapters/InternalAuthController.java` | Set cookies on response instead of returning token in body | 3 |
| 6 | `application/src/main/resources/application.properties` | Add Redis + cookie configuration properties | 1 |
| 7 | DB schema file | Add `refresh_tokens` table | 2 |

---

## 3. Implementation Sequence

### Phase Dependency Graph

```
Phase 1:  Redis infrastructure (dependencies, AkademiaPlusRedisConfig, AkademiaPlusRedisSessionStore)
    |
Phase 2:  RefreshTokenDataModel + RefreshTokenRepository + DB schema
    |
Phase 3:  JwtTokenProvider enhancements + CookieService + InternalAuthController cookie delivery
    |
Phase 4:  Token refresh endpoint (TokenRefreshController + SecurityConfig)
    |
Phase 5:  Reuse detection logic (exceptions + TokenRefreshUseCase enhancement)
    |
Phase 6:  Logout endpoint (LogoutUseCase + LogoutController)
    |
Phase 7:  JwtRequestFilter cookie + Redis session support
    |
Phase 8:  Unit tests (security module)
    |
Phase 9:  Component tests
    |
Phase 10: E2E tests
```

---

## 4. Phase-by-Phase Implementation

### Phase 1: Redis Infrastructure

#### Step 1.1: Add Redis dependency

**File**: `security/pom.xml`

Add:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### Step 1.2: Add Redis configuration properties

**File**: `application/src/main/resources/application.properties`

Add:
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

#### Step 1.3: Create AkademiaPlusRedisConfig

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

#### Step 1.4: Create AkademiaPlusRedisSessionStore

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/session/AkademiaPlusRedisSessionStore.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis-backed session store for access token revocation tracking.
 *
 * <p>Stores JWT session metadata keyed by JTI (JWT ID). Enables
 * server-side revocation of access tokens without waiting for
 * expiration. Also maintains a user-to-sessions index for bulk
 * revocation during logout.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class AkademiaPlusRedisSessionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkademiaPlusRedisSessionStore.class);

    public static final String SESSION_KEY_PREFIX = "session:";
    public static final String USER_SESSIONS_KEY_PREFIX = "user_sessions:";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_USERNAME = "username";

    private final RedisTemplate<String, String> akademiaPlusRedisTemplate;

    /**
     * Constructs a AkademiaPlusRedisSessionStore with the provided Redis template.
     *
     * @param akademiaPlusRedisTemplate the Redis template for session operations
     */
    public AkademiaPlusRedisSessionStore(RedisTemplate<String, String> akademiaPlusRedisTemplate) {
        this.akademiaPlusRedisTemplate = akademiaPlusRedisTemplate;
    }

    /**
     * Stores a new access token session in Redis.
     *
     * @param jti       the JWT ID (unique identifier for the access token)
     * @param username  the authenticated username
     * @param tenantId  the tenant ID
     * @param ttl       the time-to-live for the session entry
     */
    public void storeSession(String jti, String username, Long tenantId, Duration ttl) {
        String sessionKey = SESSION_KEY_PREFIX + jti;
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + tenantId + ":" + username;

        akademiaPlusRedisTemplate.opsForHash().put(sessionKey, FIELD_USERNAME, username);
        akademiaPlusRedisTemplate.opsForHash().put(sessionKey, FIELD_TENANT_ID, String.valueOf(tenantId));
        akademiaPlusRedisTemplate.expire(sessionKey, ttl);

        akademiaPlusRedisTemplate.opsForSet().add(userSessionsKey, jti);
        akademiaPlusRedisTemplate.expire(userSessionsKey, ttl);

        LOGGER.debug("Stored session for jti={} user={} tenant={}", jti, username, tenantId);
    }

    /**
     * Checks whether a session exists and is not revoked.
     *
     * @param jti the JWT ID to check
     * @return true if the session is valid, false otherwise
     */
    public boolean isSessionValid(String jti) {
        String sessionKey = SESSION_KEY_PREFIX + jti;
        return Boolean.TRUE.equals(akademiaPlusRedisTemplate.hasKey(sessionKey));
    }

    /**
     * Revokes a single session by deleting its Redis entry.
     *
     * @param jti the JWT ID to revoke
     */
    public void revokeSession(String jti) {
        String sessionKey = SESSION_KEY_PREFIX + jti;
        akademiaPlusRedisTemplate.delete(sessionKey);
        LOGGER.debug("Revoked session jti={}", jti);
    }

    /**
     * Revokes all sessions for a given user within a tenant.
     *
     * <p>Iterates over the user's session index and deletes each session
     * entry, then removes the index itself.</p>
     *
     * @param username the username whose sessions to revoke
     * @param tenantId the tenant ID
     */
    public void revokeAllSessionsForUser(String username, Long tenantId) {
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + tenantId + ":" + username;
        Set<String> jtis = akademiaPlusRedisTemplate.opsForSet().members(userSessionsKey);

        if (jtis != null) {
            for (String jti : jtis) {
                revokeSession(jti);
            }
        }

        akademiaPlusRedisTemplate.delete(userSessionsKey);
        LOGGER.info("Revoked all sessions for user={} tenant={}", username, tenantId);
    }
}
```

#### Step 1.5: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 1.6: Commit

```
feat(security): add Redis infrastructure for session management

Add spring-boot-starter-data-redis dependency, AkademiaPlusRedisConfig with
StringRedisSerializer template, and AkademiaPlusRedisSessionStore service for
access token session tracking and revocation.
```

---

### Phase 2: RefreshTokenDataModel + Repository

#### Step 2.1: Create RefreshTokenDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/security/RefreshTokenDataModel.java`

Entity with composite key (tenantId + refreshTokenId):

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.security;

/**
 * Persists refresh token metadata for rotation tracking and reuse detection.
 *
 * <p>Each refresh token belongs to a family identified by {@code familyId}.
 * When a token is rotated, the old token's {@code revokedAt} and
 * {@code replacedByTokenHash} are set, and a new token is created with the
 * same {@code familyId}. If a consumed token is reused, all tokens in the
 * family are revoked (breach detection).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenDataModel {

    public static final String COLUMN_TOKEN_HASH = "token_hash";
    public static final String COLUMN_FAMILY_ID = "family_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_EXPIRES_AT = "expires_at";
    public static final String COLUMN_REVOKED_AT = "revoked_at";
    public static final String COLUMN_REPLACED_BY_TOKEN_HASH = "replaced_by_token_hash";
    public static final String COLUMN_CREATED_AT = "created_at";

    @EmbeddedId
    private RefreshTokenCompositeId id;

    @Column(name = COLUMN_TOKEN_HASH, nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = COLUMN_FAMILY_ID, nullable = false, length = 36)
    private String familyId;

    @Column(name = COLUMN_USER_ID, nullable = false)
    private Long userId;

    @Column(name = COLUMN_USERNAME, nullable = false, length = 255)
    private String username;

    @Column(name = COLUMN_EXPIRES_AT, nullable = false)
    private Instant expiresAt;

    @Column(name = COLUMN_REVOKED_AT)
    private Instant revokedAt;

    @Column(name = COLUMN_REPLACED_BY_TOKEN_HASH, length = 64)
    private String replacedByTokenHash;

    @Column(name = COLUMN_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    // Getters and setters (Lombok or manual)
}
```

Use the same composite key pattern as other entities in the project. Read existing `CompositeId` classes first.

#### Step 2.2: Create RefreshTokenRepository

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/RefreshTokenRepository.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.security.RefreshTokenDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for refresh token persistence and family-based operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenDataModel, RefreshTokenCompositeId> {

    /**
     * Finds a refresh token by its SHA-256 hash.
     *
     * @param tokenHash the SHA-256 hash of the refresh token
     * @return the refresh token entity, if found
     */
    Optional<RefreshTokenDataModel> findByTokenHash(String tokenHash);

    /**
     * Revokes all non-revoked tokens in a family by setting their revokedAt timestamp.
     *
     * @param familyId  the family UUID
     * @param revokedAt the revocation timestamp
     */
    @Modifying
    @Query("UPDATE RefreshTokenDataModel r SET r.revokedAt = :revokedAt WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
    void revokeAllByFamilyId(@Param("familyId") String familyId, @Param("revokedAt") Instant revokedAt);

    /**
     * Revokes all non-revoked tokens for a user within a tenant.
     *
     * @param userId    the user ID
     * @param tenantId  the tenant ID
     * @param revokedAt the revocation timestamp
     */
    @Modifying
    @Query("UPDATE RefreshTokenDataModel r SET r.revokedAt = :revokedAt WHERE r.userId = :userId AND r.id.tenantId = :tenantId AND r.revokedAt IS NULL")
    void revokeAllByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("revokedAt") Instant revokedAt);
}
```

#### Step 2.3: Add DB schema

Add the `refresh_tokens` table to the schema file:

```sql
CREATE TABLE refresh_tokens (
    tenant_id       BIGINT       NOT NULL,
    refresh_token_id BIGINT      NOT NULL,
    token_hash      VARCHAR(64)  NOT NULL,
    family_id       VARCHAR(36)  NOT NULL,
    user_id         BIGINT       NOT NULL,
    username        VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMP(6) NOT NULL,
    revoked_at      TIMESTAMP(6) NULL,
    replaced_by_token_hash VARCHAR(64) NULL,
    created_at      TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (tenant_id, refresh_token_id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    INDEX idx_refresh_token_family (family_id),
    INDEX idx_refresh_token_user (tenant_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

#### Step 2.4: Compile check

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 2.5: Commit

```
feat(multi-tenant-data): add RefreshTokenDataModel for token rotation

Add refresh_tokens table with composite key (tenantId + refreshTokenId),
tokenHash (SHA-256, unique), familyId (UUID for rotation chain),
userId, expiresAt, revokedAt, replacedByTokenHash, createdAt.
Add RefreshTokenRepository with family-based revocation queries.
```

---

### Phase 3: JwtTokenProvider Enhancements + CookieService

#### Step 3.1: Enhance JwtTokenProvider

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java`

Add:

```java
public static final String JTI_CLAIM = "jti";
public static final String TOKEN_TYPE_CLAIM = "token_type";
public static final String TOKEN_TYPE_ACCESS = "access";
public static final String TOKEN_TYPE_REFRESH = "refresh";
public static final String FAMILY_ID_CLAIM = "family_id";

@Value("${jwt.refresh-token.validity-ms}")
private long refreshTokenValidityInMs;
```

Add method:

```java
/**
 * Creates a signed access token with a unique JTI claim for Redis session tracking.
 *
 * @param username         the subject of the token
 * @param tenantId         the tenant ID
 * @param additionalClaims additional claims to include
 * @return the signed JWT access token string
 */
public String createAccessToken(String username, Long tenantId, Map<String, Object> additionalClaims) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + validityInMs);

    Map<String, Object> claims = (additionalClaims != null) ? new HashMap<>(additionalClaims) : new HashMap<>();
    claims.put(TENANT_ID_CLAIM, tenantId);
    claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS);

    String jti = UUID.randomUUID().toString();

    return Jwts.builder()
            .id(jti)
            .subject(username)
            .claims(claims)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(keyPair.getPrivate())
            .compact();
}

/**
 * Creates a signed refresh token with a family ID for rotation tracking.
 *
 * @param username the subject of the token
 * @param tenantId the tenant ID
 * @param familyId the token family UUID for rotation chain tracking
 * @return the signed JWT refresh token string
 */
public String createRefreshToken(String username, Long tenantId, String familyId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + refreshTokenValidityInMs);

    Map<String, Object> claims = new HashMap<>();
    claims.put(TENANT_ID_CLAIM, tenantId);
    claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH);
    claims.put(FAMILY_ID_CLAIM, familyId);

    return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(username)
            .claims(claims)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(keyPair.getPrivate())
            .compact();
}

/**
 * Extracts the JTI (JWT ID) claim from a token.
 *
 * @param token the JWT string
 * @return the JTI value
 */
public String getJti(String token) {
    return getClaims(token).getId();
}

/**
 * Extracts the token type claim from a token.
 *
 * @param token the JWT string
 * @return the token type (access or refresh)
 */
public String getTokenType(String token) {
    return getClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
}

/**
 * Returns the access token validity in milliseconds.
 *
 * @return access token TTL in milliseconds
 */
public long getAccessTokenValidityInMs() {
    return validityInMs;
}
```

#### Step 3.2: Create CookieService

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/CookieService.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for creating and extracting HttpOnly cookies for JWT token delivery.
 *
 * <p>Manages access and refresh token cookies with Secure, HttpOnly,
 * and SameSite=Strict attributes. Supports cookie clearing for logout.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class CookieService {

    public static final String ACCESS_TOKEN_PATH = "/v1";
    public static final String REFRESH_TOKEN_PATH = "/v1/security/token";
    public static final String SAME_SITE_STRICT = "Strict";
    public static final String SET_COOKIE_HEADER = "Set-Cookie";

    @Value("${security.cookie.domain}")
    private String cookieDomain;

    @Value("${security.cookie.secure}")
    private boolean secureCookie;

    @Value("${security.cookie.access-token-name}")
    private String accessTokenCookieName;

    @Value("${security.cookie.refresh-token-name}")
    private String refreshTokenCookieName;

    @Value("${security.cookie.access-token-max-age-seconds}")
    private long accessTokenMaxAge;

    @Value("${security.cookie.refresh-token-max-age-seconds}")
    private long refreshTokenMaxAge;

    /**
     * Adds access and refresh token cookies to the HTTP response.
     *
     * @param response     the HTTP response
     * @param accessToken  the JWT access token
     * @param refreshToken the JWT refresh token
     */
    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = buildCookie(accessTokenCookieName, accessToken, ACCESS_TOKEN_PATH, accessTokenMaxAge);
        ResponseCookie refreshCookie = buildCookie(refreshTokenCookieName, refreshToken, REFRESH_TOKEN_PATH, refreshTokenMaxAge);

        response.addHeader(SET_COOKIE_HEADER, accessCookie.toString());
        response.addHeader(SET_COOKIE_HEADER, refreshCookie.toString());
    }

    /**
     * Clears access and refresh token cookies by setting Max-Age to 0.
     *
     * @param response the HTTP response
     */
    public void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = buildCookie(accessTokenCookieName, "", ACCESS_TOKEN_PATH, 0);
        ResponseCookie refreshCookie = buildCookie(refreshTokenCookieName, "", REFRESH_TOKEN_PATH, 0);

        response.addHeader(SET_COOKIE_HEADER, accessCookie.toString());
        response.addHeader(SET_COOKIE_HEADER, refreshCookie.toString());
    }

    /**
     * Extracts the access token from the request cookies.
     *
     * @param request the HTTP request
     * @return the access token value, or empty if not present
     */
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, accessTokenCookieName);
    }

    /**
     * Extracts the refresh token from the request cookies.
     *
     * @param request the HTTP request
     * @return the refresh token value, or empty if not present
     */
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, refreshTokenCookieName);
    }

    private ResponseCookie buildCookie(String name, String value, String path, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(SAME_SITE_STRICT)
                .path(path)
                .domain(cookieDomain)
                .maxAge(maxAge)
                .build();
    }

    private Optional<String> extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
```

#### Step 3.3: Modify InternalAuthController for cookie delivery

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/InternalAuthController.java`

Add `CookieService` and `AkademiaPlusRedisSessionStore` dependencies. After login succeeds, set cookies on the response in addition to returning the token in the body (backward compatibility during migration).

#### Step 3.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 3.5: Commit

```
feat(security): add refresh token generation and cookie-based delivery

Enhance JwtTokenProvider with createAccessToken (jti claim),
createRefreshToken (familyId claim), and token type claims.
Add CookieService for HttpOnly/Secure/SameSite=Strict cookie
management. Modify InternalAuthController to set token cookies.
```

---

### Phase 4: Token Refresh Endpoint + Use Case

#### Step 4.1: Create TokenRefreshUseCase

**File**: `security/src/main/java/com/akademiaplus/internal/usecases/TokenRefreshUseCase.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

/**
 * Handles refresh token rotation with reuse detection.
 *
 * <p>Validates the incoming refresh token, issues a new access + refresh
 * pair, and marks the old refresh token as consumed. If a previously
 * consumed token is reused (indicating possible theft), all tokens in
 * the family are revoked.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@Transactional
public class TokenRefreshUseCase {

    public static final String ERROR_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found";
    public static final String ERROR_REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
    public static final String ERROR_TOKEN_REUSE_DETECTED = "Token reuse detected — all tokens in family revoked";

    // Constructor: JwtTokenProvider, RefreshTokenRepository, AkademiaPlusRedisSessionStore,
    //              HashingService, ApplicationContext

    /**
     * Rotates a refresh token, issuing a new access + refresh pair.
     *
     * @param currentRefreshToken the current refresh token string
     * @return a record containing the new access token, refresh token, and username
     * @throws RefreshTokenExpiredException  if the refresh token has expired
     * @throws TokenReuseDetectedException   if the token was already consumed
     */
    public TokenRefreshResult refresh(String currentRefreshToken) {
        // 1. Hash the incoming token
        // 2. Lookup RefreshTokenDataModel by tokenHash
        // 3. If not found -> throw RefreshTokenExpiredException
        // 4. If revokedAt is not null -> REUSE DETECTED
        //    a. Revoke ALL tokens in the family
        //    b. Revoke all Redis sessions for the user
        //    c. Throw TokenReuseDetectedException
        // 5. If expiresAt < now -> throw RefreshTokenExpiredException
        // 6. Mark old token as consumed (set revokedAt)
        // 7. Generate new access token (with jti)
        // 8. Generate new refresh token (same familyId)
        // 9. Hash new refresh token, create new RefreshTokenDataModel
        // 10. Set replacedByTokenHash on old token
        // 11. Store new access token session in Redis
        // 12. Return TokenRefreshResult(newAccessToken, newRefreshToken, username)
    }
}
```

#### Step 4.2: Create TokenRefreshResult record

**File**: `security/src/main/java/com/akademiaplus/internal/usecases/domain/TokenRefreshResult.java`

```java
/**
 * Result of a successful token refresh operation.
 *
 * @param accessToken  the new JWT access token
 * @param refreshToken the new JWT refresh token
 * @param username     the authenticated username
 * @author ElatusDev
 * @since 1.0
 */
public record TokenRefreshResult(String accessToken, String refreshToken, String username) {}
```

#### Step 4.3: Create TokenRefreshController

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/TokenRefreshController.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

/**
 * REST controller for the token refresh endpoint.
 *
 * <p>Reads the refresh token from the HttpOnly cookie, delegates to
 * {@link TokenRefreshUseCase}, and sets new cookies on the response.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security/token")
public class TokenRefreshController {

    public static final String ERROR_NO_REFRESH_TOKEN = "No refresh token cookie present";

    // Constructor: TokenRefreshUseCase, CookieService

    /**
     * Refreshes the access and refresh tokens.
     *
     * <p>Reads the refresh token from the cookie, validates and rotates it,
     * then sets new access and refresh token cookies on the response.</p>
     *
     * @param request  the HTTP request containing the refresh token cookie
     * @param response the HTTP response where new cookies will be set
     * @return 200 OK with empty body (tokens are in cookies)
     */
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

#### Step 4.4: Update SecurityConfig

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add:
```java
.requestMatchers("/v1/security/token/refresh").permitAll()
.requestMatchers("/v1/security/logout").permitAll()
```

Also update CORS config to allow credentials (`allowCredentials(true)`) since cookies require it.

#### Step 4.5: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 4.6: Commit

```
feat(security): add token refresh endpoint with rotation

Add TokenRefreshUseCase with refresh token validation, rotation,
and new token pair issuance. Add TokenRefreshController that reads
refresh token from HttpOnly cookie and sets new cookies.
Update SecurityConfig to permit /v1/security/token/refresh and
/v1/security/logout endpoints.
```

---

### Phase 5: Reuse Detection Logic

#### Step 5.1: Create TokenReuseDetectedException

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

#### Step 5.2: Create RefreshTokenExpiredException

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

#### Step 5.3: Enhance TokenRefreshUseCase with reuse detection

The reuse detection logic is already outlined in Phase 4 step 4.1. The key logic:

```java
// Inside refresh() method, after lookup by tokenHash:
if (existingToken.getRevokedAt() != null) {
    // REUSE DETECTED: this token was already consumed
    LOGGER.warn("Token reuse detected for family={}, revoking all tokens", existingToken.getFamilyId());
    refreshTokenRepository.revokeAllByFamilyId(existingToken.getFamilyId(), Instant.now());
    redisSessionStore.revokeAllSessionsForUser(existingToken.getUsername(), existingToken.getId().getTenantId());
    throw new TokenReuseDetectedException(existingToken.getFamilyId());
}
```

#### Step 5.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 5.5: Commit

```
feat(security): add refresh token reuse detection

Add TokenReuseDetectedException and RefreshTokenExpiredException.
When a consumed refresh token is reused, revoke all tokens in the
family and invalidate all Redis sessions for the user.
```

---

### Phase 6: Logout Endpoint + Use Case

#### Step 6.1: Create LogoutUseCase

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

/**
 * Handles user logout by revoking all refresh tokens and Redis sessions.
 *
 * <p>Revokes all refresh tokens for the user within the tenant and
 * removes all Redis session entries. The controller is responsible
 * for clearing the cookies.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class LogoutUseCase {

    // Constructor: RefreshTokenRepository, AkademiaPlusRedisSessionStore, JwtTokenProvider

    /**
     * Revokes all tokens and sessions for the authenticated user.
     *
     * @param accessToken the current access token (to extract user identity)
     */
    @Transactional
    public void logout(String accessToken) {
        String username = jwtTokenProvider.getUsername(accessToken);
        Long tenantId = Long.valueOf(jwtTokenProvider.getTenantId(accessToken));

        // Revoke all refresh tokens for this user+tenant
        refreshTokenRepository.revokeAllByUserIdAndTenantId(userId, tenantId, Instant.now());

        // Revoke all Redis sessions
        redisSessionStore.revokeAllSessionsForUser(username, tenantId);
    }
}
```

#### Step 6.2: Create LogoutController

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

/**
 * REST controller for the logout endpoint.
 *
 * <p>Extracts the access token from the cookie or Authorization header,
 * delegates revocation to {@link LogoutUseCase}, and clears cookies.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security")
public class LogoutController {

    public static final String ERROR_NO_TOKEN_FOR_LOGOUT = "No access token available for logout";

    // Constructor: LogoutUseCase, CookieService

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
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }
}
```

#### Step 6.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 6.4: Commit

```
feat(security): add logout endpoint with full revocation

Add LogoutUseCase that revokes all refresh tokens and Redis sessions
for the user. Add LogoutController at POST /v1/security/logout that
clears HttpOnly cookies and delegates to LogoutUseCase.
```

---

### Phase 7: JwtRequestFilter Cookie Support

#### Step 7.1: Modify JwtRequestFilter

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java`

Add `CookieService` and `AkademiaPlusRedisSessionStore` as constructor dependencies.

Modify `doFilterInternal()`:

```java
@Override
protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain)
        throws ServletException, IOException {

    String jwtToken = null;
    String username = null;

    // Try 1: Extract from cookie
    Optional<String> cookieToken = cookieService.extractAccessToken(request);
    if (cookieToken.isPresent()) {
        jwtToken = cookieToken.get();
    }

    // Try 2: Fall back to Authorization header (migration support)
    if (jwtToken == null) {
        final String requestTokenHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (requestTokenHeader != null && requestTokenHeader.startsWith(BEARER_PREFIX)) {
            jwtToken = requestTokenHeader.substring(BEARER_PREFIX.length());
        }
    }

    // Validate token
    if (jwtToken != null) {
        try {
            if (jwtTokenProvider.validateToken(jwtToken)) {
                // Check Redis session (if jti claim is present)
                String jti = jwtTokenProvider.getJti(jwtToken);
                if (jti != null && !redisSessionStore.isSessionValid(jti)) {
                    // Session revoked server-side
                    chain.doFilter(request, response);
                    return;
                }
                username = jwtTokenProvider.getUsername(jwtToken);
            }
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = this.internalAuthorizationUseCase.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getUsername(), userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
    chain.doFilter(request, response);
}
```

Add constants:
```java
public static final String AUTHORIZATION_HEADER = "Authorization";
public static final String BEARER_PREFIX = "Bearer ";
```

#### Step 7.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 7.3: Commit

```
feat(security): add cookie and Redis session support to JwtRequestFilter

Modify JwtRequestFilter to read access token from HttpOnly cookie
first, then fall back to Authorization header for backward
compatibility. Add Redis session validation to reject revoked tokens.
```

---

### Phase 8: Unit Tests (Security Module)

All tests follow conventions: Given-When-Then, `shouldDoX_whenY()`, `@DisplayName`, `@Nested`, zero `any()` matchers, `public static final` constants.

#### Step 8.1: AkademiaPlusAkademiaPlusRedisSessionStoreTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/session/AkademiaPlusAkademiaPlusRedisSessionStoreTest.java`

| @Nested | Tests |
|---------|-------|
| `SessionStorage` | `shouldStoreSession_whenValidParametersProvided`, `shouldSetTtlOnSessionKey_whenStoring` |
| `SessionValidation` | `shouldReturnTrue_whenSessionExists`, `shouldReturnFalse_whenSessionDoesNotExist` |
| `SessionRevocation` | `shouldDeleteSessionKey_whenRevokingSingleSession`, `shouldDeleteAllSessionsForUser_whenRevokingByUser` |

#### Step 8.2: CookieServiceTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/jwt/CookieServiceTest.java`

| @Nested | Tests |
|---------|-------|
| `CookieCreation` | `shouldAddAccessTokenCookie_whenTokenProvided`, `shouldAddRefreshTokenCookie_whenTokenProvided`, `shouldSetHttpOnlyFlag_whenCreatingCookie`, `shouldSetSecureFlag_whenCreatingCookie`, `shouldSetSameSiteStrict_whenCreatingCookie` |
| `CookieClearing` | `shouldSetMaxAgeToZero_whenClearingCookies` |
| `CookieExtraction` | `shouldExtractAccessToken_whenCookiePresent`, `shouldReturnEmpty_whenNoCookiesPresent`, `shouldExtractRefreshToken_whenCookiePresent` |

#### Step 8.3: TokenRefreshUseCaseTest

**File**: `security/src/test/java/com/akademiaplus/internal/usecases/TokenRefreshUseCaseTest.java`

| @Nested | Tests |
|---------|-------|
| `SuccessfulRotation` | `shouldIssueNewAccessToken_whenRefreshTokenIsValid`, `shouldIssueNewRefreshToken_whenRefreshTokenIsValid`, `shouldMarkOldTokenAsConsumed_whenRotating`, `shouldCreateNewRefreshTokenEntity_whenRotating`, `shouldPreserveFamilyId_whenRotating`, `shouldStoreNewSessionInRedis_whenRotating` |
| `ReuseDetection` | `shouldRevokeAllFamilyTokens_whenConsumedTokenReused`, `shouldRevokeAllRedisSessionsForUser_whenReuseDetected`, `shouldThrowTokenReuseDetectedException_whenConsumedTokenReused` |
| `ExpiredToken` | `shouldThrowRefreshTokenExpiredException_whenTokenExpired`, `shouldThrowRefreshTokenExpiredException_whenTokenNotFound` |

#### Step 8.4: LogoutUseCaseTest

**File**: `security/src/test/java/com/akademiaplus/internal/usecases/LogoutUseCaseTest.java`

| @Nested | Tests |
|---------|-------|
| `SuccessfulLogout` | `shouldRevokeAllRefreshTokens_whenLoggingOut`, `shouldRevokeAllRedisSessions_whenLoggingOut` |

#### Step 8.5: TokenRefreshControllerTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/TokenRefreshControllerTest.java`

| @Nested | Tests |
|---------|-------|
| `RefreshEndpoint` | `shouldReturn200_whenRefreshSucceeds`, `shouldSetNewCookies_whenRefreshSucceeds`, `shouldReturn401_whenNoRefreshTokenCookie`, `shouldReturn401_whenTokenReuseDetected` |

#### Step 8.6: LogoutControllerTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/LogoutControllerTest.java`

| @Nested | Tests |
|---------|-------|
| `LogoutEndpoint` | `shouldReturn204_whenLogoutSucceeds`, `shouldClearCookies_whenLogoutSucceeds`, `shouldReadFromAuthorizationHeader_whenNoCookie` |

#### Step 8.7: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 8.8: Commit

```
test(security): add unit tests for refresh token rotation and logout

AkademiaPlusAkademiaPlusRedisSessionStoreTest, CookieServiceTest, TokenRefreshUseCaseTest,
LogoutUseCaseTest, TokenRefreshControllerTest, LogoutControllerTest.
Cover session management, cookie handling, token rotation, reuse
detection, and logout with full revocation.
```

---

### Phase 9: Component Tests

#### Step 9.1: TokenRotationComponentTest

**File**: `security/src/test/java/com/akademiaplus/usecases/TokenRotationComponentTest.java`

Full Spring context + Testcontainers MariaDB + embedded Redis. Tests the complete HTTP stack.

| @Nested | Tests |
|---------|-------|
| `LoginWithCookies` | `shouldSetAccessTokenCookie_whenLoginSucceeds`, `shouldSetRefreshTokenCookie_whenLoginSucceeds` |
| `TokenRefresh` | `shouldReturn200_whenRefreshingWithValidToken`, `shouldIssueNewTokenPair_whenRefreshing`, `shouldInvalidateOldRefreshToken_whenRefreshing` |
| `ReuseDetection` | `shouldReturn401_whenReplayingConsumedRefreshToken`, `shouldRevokeAllFamilyTokens_whenReuseDetected` |
| `Logout` | `shouldReturn204_whenLoggingOut`, `shouldInvalidateAllTokens_whenLoggingOut`, `shouldClearCookies_whenLoggingOut`, `shouldRejectAccessToken_afterLogout` |

#### Step 9.2: Compile + verify

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl security -am -f platform-core-api/pom.xml
```

#### Step 9.3: Commit

```
test(security): add token rotation component test

TokenRotationComponentTest — full Spring context + Testcontainers
MariaDB + embedded Redis. Covers login cookie delivery, token
refresh with rotation, reuse detection, logout with full revocation.
```

---

### Phase 10: E2E Tests

See separate E2E prompt. Add requests to the Postman collection:

| Request | Method | Expected |
|---------|--------|----------|
| `LoginWithCookies` | POST `/v1/security/login/internal` | 200 + Set-Cookie headers |
| `TokenRefresh` | POST `/v1/security/token/refresh` | 200 + new Set-Cookie headers |
| `TokenRefreshWithConsumedToken` | POST `/v1/security/token/refresh` | 401 (reuse detected) |
| `Logout` | POST `/v1/security/logout` | 204 + cleared cookies |
| `AccessAfterLogout` | GET `/v1/...` | 401 (session revoked) |

#### Commit

```
test(e2e): add refresh token rotation and logout E2E tests

Cover login with cookies, token refresh rotation, reuse detection
(replaying consumed token), logout, and post-logout access rejection.
```

---

## 5. Key Design Decisions

### Token Storage: Cookie vs. Header

| Aspect | Cookie (chosen) | Authorization Header |
|--------|-----------------|---------------------|
| XSS protection | HttpOnly prevents JS access | Token in JS memory, vulnerable to XSS |
| CSRF risk | Mitigated by SameSite=Strict | No CSRF risk (manual header) |
| Mobile support | Requires cookie jar support | Simpler for mobile clients |
| Migration | Dual support (cookie + header) during transition | Current approach |

### Refresh Token Storage: DB vs. Redis

| Aspect | DB (chosen) | Redis only |
|--------|-------------|------------|
| Durability | Survives Redis restart | Lost on restart |
| Family tracking | Relational queries for reuse detection | Complex with key patterns |
| Audit trail | Full rotation history preserved | Ephemeral |
| Performance | Slightly slower writes | Faster |

### Access Token Revocation: Redis vs. DB Blacklist

| Aspect | Redis (chosen) | DB blacklist |
|--------|----------------|-------------|
| Latency | Sub-millisecond lookup | DB query per request |
| Auto-cleanup | TTL-based expiry | Manual cleanup job needed |
| Scalability | Horizontal Redis scaling | DB connection pool pressure |

---

## 6. Multi-Tenancy Considerations

1. **RefreshTokenDataModel** uses composite key `(tenantId, refreshTokenId)` -- consistent with all entities
2. **Redis session keys** include tenant ID: `session:{jti}` stores `tenantId` as a hash field, `user_sessions:{tenantId}:{username}` enables per-tenant user session lookup
3. **Revocation queries** filter by `tenantId` -- a user in tenant A cannot revoke tokens in tenant B
4. **Token family** is scoped to a single user+tenant combination -- cross-tenant family collision is not possible because `familyId` is a UUID
5. **Hibernate tenant filter** applies to `RefreshTokenDataModel` queries automatically via `TenantContextLoader`

---

## 7. Future Extensibility

1. **Token binding** (anti-hijack): The `jti` claim and Redis session store provide a hook to bind tokens to device fingerprints in a future feature
2. **Sliding window expiry**: Redis TTL on sessions can be extended on each request for sliding session windows
3. **Per-device sessions**: Redis `user_sessions` set can track individual devices for selective revocation
4. **Rate limiting**: Token refresh endpoint can be rate-limited per `familyId` to prevent rotation storms
5. **Branching security**: Cookie paths can be differentiated per app origin (`/akademia/**` vs `/elatus/**`)

---

## 8. Verification Checklist

### Per-phase gates

After each phase, run the specified compile/test command. Fix all errors before proceeding.

### Final verification

1. `mvn clean compile -pl security -am -DskipTests` -- full compilation
2. `mvn test -pl security` -- all unit tests pass
3. `mvn verify -pl security` -- component tests pass
4. Manual: Login, verify Set-Cookie headers, refresh token, verify new cookies, logout, verify cookies cleared
5. Manual: Replay consumed refresh token, verify 401 + family revocation

---

## 9. Critical Reminders

1. **Existing `createToken()` method** -- keep it for backward compatibility during migration. New code should call `createAccessToken()` which adds the `jti` claim.
2. **Cookie `SameSite=Strict`** -- blocks cross-origin requests entirely. CORS `allowCredentials` must be `true` for same-origin cookie transmission.
3. **Refresh token is NOT stored** -- only the SHA-256 hash is stored in `RefreshTokenDataModel.tokenHash`. The raw token is only in the cookie.
4. **Redis unavailability** -- if Redis is down, the filter should degrade gracefully (skip session validation, log warning). Do NOT fail open for refresh/logout.
5. **`ApplicationContext.getBean()`** -- all entity instantiation must use prototype bean pattern, never `new RefreshTokenDataModel()`.
6. **Family ID** -- generated as `UUID.randomUUID().toString()` at first login. Preserved across all rotations in the chain.
7. **Reuse detection is critical** -- if an attacker steals a refresh token and the legitimate user refreshes first, the attacker's replay triggers family-wide revocation. This is the core security property.
8. **No `any()` matchers** -- all mock stubbing uses exact values or `ArgumentCaptor`.
9. **`Long` for all IDs** -- never `Integer`. The existing `getTenantId()` returns `Integer` -- the new code must handle the conversion or refactor to `Long`.
10. **Conventional Commits** -- no AI attribution in commit messages.
