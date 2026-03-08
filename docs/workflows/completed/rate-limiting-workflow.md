# Rate Limiting Workflow — ElatusDev Filter Chain

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `docs/directives/CLAUDE.md`, `docs/directives/AI-CODE-REF.md`, and `docs/design/DESIGN.md` before starting. Also read the branching-security-filter workflow (prerequisite feature).

---

## 1. Architecture Overview

### 1.1 What Exists

The platform currently has no rate limiting. All endpoints — authenticated or unauthenticated — accept unlimited requests. The ElatusDev frontend faces the public internet and is exposed to brute-force, credential-stuffing, and denial-of-service attacks.

| Component | Location | State |
|-----------|----------|-------|
| `SecurityConfig` | `security/.../config/` | Two filter chains: dev/local (JWT) and mock-data-service — no rate limiting |
| `JwtRequestFilter` | `security/.../internal/interfaceadapters/jwt/` | `@Order(3)` — validates JWT, sets `SecurityContext` |
| `ModuleSecurityConfigurator` | `security/.../config/` | Interface for per-module authorization rules |
| `TenantContextHolder` | `infra-common/.../persistence/config/` | `@RequestScope` — holds `tenantId` for current request |
| Redis | — | Not yet configured in the project |

### 1.2 What's Missing

1. **Redis dependency**: No Redis client or Spring Data Redis configured
2. **Rate limiting filter**: No `OncePerRequestFilter` that checks request rates
3. **Rate limiter service**: No service implementing sliding window algorithm
4. **Rate limit configuration**: No `@ConfigurationProperties` for per-endpoint rate limits
5. **Rate limit headers**: No response headers communicating limit status to clients
6. **429 response handling**: No standardized "Too Many Requests" response
7. **ElatusDev filter chain isolation**: Rate limiting must only apply to the ElatusDev filter chain (public internet), not the AkademiaPlus filter chain (school IP whitelist)

### 1.3 Rate Limiting Strategy: Sliding Window with Redis Sorted Sets

The sliding window algorithm provides smoother rate limiting than fixed windows by avoiding the boundary-burst problem. Uses Redis sorted sets (`ZSET`) where each request is scored by its timestamp.

```
Request arrives at timestamp T
  ├── Key: "rate:ip:{ip}:{endpoint}" or "rate:user:{userId}"
  │
  ├── 1. ZREMRANGEBYSCORE key 0 (T - windowSizeMs)    // Remove expired entries
  ├── 2. ZCARD key                                       // Count current entries
  ├── 3. If count >= limit → REJECT (429)
  ├── 4. ZADD key T memberId                             // Add this request
  ├── 5. EXPIRE key windowSizeSeconds                    // Set TTL for cleanup
  └── 6. Set response headers (X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset)
```

### 1.4 Rate Limit Tiers

| Tier | Scope | Key Format | Default Limit | Window | Applies To |
|------|-------|------------|---------------|--------|-----------|
| Login | Per-IP | `rate:ip:{ip}:login` | 5 requests | 15 minutes | `POST /v1/security/login/*` |
| Public | Per-IP | `rate:ip:{ip}:public` | 20 requests | 1 minute | `POST /v1/security/register`, passkey endpoints |
| Authenticated | Per-user | `rate:user:{userId}` | 100 requests | 1 minute | All authenticated endpoints |

### 1.5 ElatusDev-Only Isolation

Rate limiting applies **exclusively** to the ElatusDev filter chain (public internet). The AkademiaPlus filter chain (school network, IP-whitelisted) is exempt. This depends on the branching-security-filter feature which establishes separate `SecurityFilterChain` beans per deployment context.

```
ElatusDev Filter Chain (public internet)
  ├── RateLimitingFilter (@Order(1))      ← NEW
  ├── JwtRequestFilter  (@Order(3))
  └── ... other filters

AkademiaPlus Filter Chain (school network)
  ├── IpWhitelistFilter
  ├── JwtRequestFilter
  └── ... other filters (NO rate limiting)
```

---

## 2. Target Architecture

### 2.1 Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│  security module                                         │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  config/                                           │  │
│  │  ├── RateLimitProperties (@ConfigurationProperties)│  │
│  │  └── RedisConfiguration (@Configuration)           │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  ratelimit/usecases/                               │  │
│  │  └── RateLimiterService (@Service)                 │  │
│  │       ├── isAllowed(key, limit, windowMs): boolean  │  │
│  │       ├── getRemainingRequests(key, limit, windowMs)│  │
│  │       └── getResetTimestamp(key, windowMs): long     │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  ratelimit/usecases/domain/                        │  │
│  │  └── RateLimitResult (record)                      │  │
│  │       ├── allowed: boolean                          │  │
│  │       ├── limit: int                                │  │
│  │       ├── remaining: int                            │  │
│  │       └── resetEpochSeconds: long                   │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  ratelimit/interfaceadapters/                      │  │
│  │  └── RateLimitingFilter (OncePerRequestFilter)     │  │
│  │       ├── @Order(1)                                 │  │
│  │       ├── resolveKey(request): String               │  │
│  │       ├── resolveLimit(request): RateLimitTier      │  │
│  │       └── writeRateLimitHeaders(response, result)   │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  ratelimit/exceptions/                             │  │
│  │  └── RateLimitExceededException                    │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Request Flow

```
HTTP Request → ElatusDev SecurityFilterChain
  ├── RateLimitingFilter.doFilterInternal()
  │     ├── 1. Check if path is bypassed (actuator, health)
  │     │     └── Yes → chain.doFilter() immediately
  │     ├── 2. Determine rate limit tier from request path
  │     ├── 3. Resolve key:
  │     │     ├── Unauthenticated → "rate:ip:{clientIp}:{tier}"
  │     │     └── Authenticated → "rate:user:{userId}"
  │     ├── 4. Call RateLimiterService.checkRateLimit(key, limit, windowMs)
  │     ├── 5. Set response headers (X-RateLimit-*)
  │     ├── 6a. ALLOWED → chain.doFilter()
  │     └── 6b. EXCEEDED → 429 JSON response + Retry-After header
  │
  ├── JwtRequestFilter (existing @Order(3))
  └── ... remaining filter chain
```

### 2.3 Module Placement

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `RateLimitProperties` | security | `config/` | Spring config bean (DESIGN.md 3.2.8) |
| `RedisConfiguration` | security | `config/` | Infrastructure bean (DESIGN.md 3.2.8) |
| `RateLimiterService` | security | `ratelimit/usecases/` | Use case — single-module, no cross-module orchestration |
| `RateLimitResult` (record) | security | `ratelimit/usecases/domain/` | Non-entity domain object (Hard Rule #13) |
| `RateLimitingFilter` | security | `ratelimit/interfaceadapters/` | Interface adapter — HTTP filter (DESIGN.md 3.2.5) |
| `RateLimitExceededException` | security | `ratelimit/exceptions/` | Module-specific exception |
| `RateLimitingFilterTest` | security | test | Unit test |
| `RateLimiterServiceTest` | security | test | Unit test |
| `RateLimitComponentTest` | security | test | Component test with embedded Redis |

---

## 3. Execution Phases

### Phase Dependency Graph

```
Phase 1:  RateLimitProperties (@ConfigurationProperties)
    ↓
Phase 2:  RedisConfiguration + Spring Data Redis dependency
    ↓
Phase 3:  RateLimitResult domain record
    ↓
Phase 4:  RateLimiterService (Redis sliding window)
    ↓
Phase 5:  RateLimitExceededException
    ↓
Phase 6:  RateLimitingFilter (OncePerRequestFilter)
    ↓
Phase 7:  Integration with ElatusDev filter chain (SecurityConfig)
    ↓
Phase 8:  Configuration (application.properties)
    ↓
Phase 9:  Unit tests — RateLimiterServiceTest
    ↓
Phase 10: Unit tests — RateLimitingFilterTest
    ↓
Phase 11: Component tests — RateLimitComponentTest
```

---

## 4. Phase-by-Phase Implementation

### Phase 1: RateLimitProperties

#### Step 1.1: Create RateLimitProperties

**File**: `security/src/main/java/com/akademiaplus/config/RateLimitProperties.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Configuration properties for rate limiting.
 * Binds to {@code rate-limit.*} in application.properties.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Map<String, TierProperties> tiers
) {

    /**
     * Rate limit tier configuration for a specific endpoint pattern.
     *
     * @param limit     maximum number of requests allowed in the window
     * @param windowMs  sliding window duration in milliseconds
     */
    public record TierProperties(int limit, long windowMs) {}
}
```

#### Step 1.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 1.3: Commit

```
feat(security): add RateLimitProperties configuration

Add @ConfigurationProperties for rate-limit.* prefix with
per-tier limit and window configuration.
```

---

### Phase 2: RedisConfiguration + Dependency

#### Step 2.1: Add Spring Data Redis dependency

**File**: `security/pom.xml`

Add:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### Step 2.2: Create RedisConfiguration

**File**: `security/src/main/java/com/akademiaplus/config/RedisConfiguration.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis configuration for rate limiting.
 * Only active when rate limiting is enabled.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true")
public class RedisConfiguration {

    /**
     * Provides a StringRedisTemplate for rate limiting operations.
     *
     * @param connectionFactory the Redis connection factory
     * @return configured StringRedisTemplate
     */
    @Bean
    public StringRedisTemplate rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

#### Step 2.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 2.4: Commit

```
feat(security): add Redis configuration for rate limiting

Add spring-boot-starter-data-redis dependency and
RedisConfiguration bean, conditional on rate-limit.enabled.
```

---

### Phase 3: RateLimitResult Domain Record

#### Step 3.1: Create RateLimitResult

**File**: `security/src/main/java/com/akademiaplus/ratelimit/usecases/domain/RateLimitResult.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.usecases.domain;

/**
 * Result of a rate limit check. Contains the decision and metadata
 * for response headers.
 *
 * @param allowed            whether the request is within the rate limit
 * @param limit              the maximum number of requests in the window
 * @param remaining          remaining requests in the current window
 * @param resetEpochSeconds  epoch second when the window resets
 * @author ElatusDev
 * @since 1.0
 */
public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long resetEpochSeconds
) {}
```

#### Step 3.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 3.3: Commit

```
feat(security): add RateLimitResult domain record

Add RateLimitResult record in ratelimit/usecases/domain/
for rate limit check responses.
```

---

### Phase 4: RateLimiterService

#### Step 4.1: Create RateLimiterService

**File**: `security/src/main/java/com/akademiaplus/ratelimit/usecases/RateLimiterService.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.usecases;

import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implements sliding window rate limiting using Redis sorted sets.
 *
 * <p>Each request is added to a Redis sorted set with its timestamp as score.
 * Expired entries are pruned on each check, and the current count determines
 * whether the request is allowed.
 *
 * <p>Key format: {@code rate:ip:{ip}:{endpoint}} for per-IP limiting,
 * or {@code rate:user:{userId}} for per-user limiting.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class RateLimiterService {

    /** Prefix for per-IP rate limit keys. */
    public static final String KEY_PREFIX_IP = "rate:ip:";

    /** Prefix for per-user rate limit keys. */
    public static final String KEY_PREFIX_USER = "rate:user:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructs a RateLimiterService with the given Redis template.
     *
     * @param redisTemplate the Redis template for sorted set operations
     */
    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks whether a request identified by the given key is within
     * the rate limit, and records the request if allowed.
     *
     * <p>Uses the sliding window algorithm:
     * <ol>
     *   <li>Remove entries older than {@code windowMs} from the sorted set</li>
     *   <li>Count remaining entries</li>
     *   <li>If count &lt; limit, add new entry and allow</li>
     *   <li>If count &gt;= limit, reject</li>
     * </ol>
     *
     * @param key      the rate limit key (e.g., "rate:ip:192.168.1.1:login")
     * @param limit    maximum requests allowed in the window
     * @param windowMs window duration in milliseconds
     * @return a {@link RateLimitResult} with the decision and header metadata
     */
    public RateLimitResult checkRateLimit(String key, int limit, long windowMs) {
        long nowMs = Instant.now().toEpochMilli();
        long windowStartMs = nowMs - windowMs;
        long resetEpochSeconds = (nowMs + windowMs) / 1000L;

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 1. Remove expired entries
        zSetOps.removeRangeByScore(key, 0, windowStartMs);

        // 2. Count current entries
        Long currentCount = zSetOps.zCard(key);
        long count = (currentCount != null) ? currentCount : 0L;

        if (count >= limit) {
            // Exceeded — do not add entry
            return new RateLimitResult(false, limit, 0, resetEpochSeconds);
        }

        // 3. Add new entry with timestamp as score, UUID as member for uniqueness
        String member = nowMs + ":" + UUID.randomUUID();
        zSetOps.add(key, member, nowMs);

        // 4. Set TTL to auto-expire the key after the window
        long ttlSeconds = TimeUnit.MILLISECONDS.toSeconds(windowMs) + 1L;
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

        int remaining = (int) (limit - count - 1);
        return new RateLimitResult(true, limit, Math.max(remaining, 0), resetEpochSeconds);
    }
}
```

#### Step 4.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 4.3: Commit

```
feat(security): implement RateLimiterService with Redis sliding window

Add RateLimiterService using Redis sorted sets for sliding
window rate limiting. Supports per-IP and per-user keys with
configurable limits and window durations.
```

---

### Phase 5: RateLimitExceededException

#### Step 5.1: Create exception

**File**: `security/src/main/java/com/akademiaplus/ratelimit/exceptions/RateLimitExceededException.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.exceptions;

/**
 * Thrown when a client exceeds the configured rate limit for an endpoint.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class RateLimitExceededException extends RuntimeException {

    /** Error message for rate limit exceeded. */
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "Rate limit exceeded. Try again in %d seconds.";

    /** Error code for rate limit exceeded responses. */
    public static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

    /**
     * Constructs a RateLimitExceededException with a retry-after duration.
     *
     * @param retryAfterSeconds seconds until the client may retry
     */
    public RateLimitExceededException(long retryAfterSeconds) {
        super(String.format(ERROR_RATE_LIMIT_EXCEEDED, retryAfterSeconds));
    }
}
```

#### Step 5.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 5.3: Commit

```
feat(security): add RateLimitExceededException

Add exception type for 429 Too Many Requests responses
with error code and retry-after message.
```

---

### Phase 6: RateLimitingFilter

#### Step 6.1: Create RateLimitingFilter

**File**: `security/src/main/java/com/akademiaplus/ratelimit/interfaceadapters/RateLimitingFilter.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.interfaceadapters;

import com.akademiaplus.config.RateLimitProperties;
import com.akademiaplus.ratelimit.usecases.RateLimiterService;
import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Servlet filter that enforces rate limits on incoming HTTP requests.
 *
 * <p>This filter runs before the JWT filter ({@code @Order(1)}) and checks
 * rate limits using {@link RateLimiterService}. For unauthenticated endpoints,
 * rate limits are applied per client IP. For authenticated endpoints, rate
 * limits are applied per authenticated user.
 *
 * <p>Bypasses health and actuator endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    /** Response header: maximum requests allowed in the window. */
    public static final String HEADER_RATE_LIMIT = "X-RateLimit-Limit";

    /** Response header: remaining requests in the current window. */
    public static final String HEADER_RATE_REMAINING = "X-RateLimit-Remaining";

    /** Response header: epoch second when the window resets. */
    public static final String HEADER_RATE_RESET = "X-RateLimit-Reset";

    /** Response header: seconds until the client may retry after a 429. */
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    /** JSON field name for error response status. */
    public static final String JSON_FIELD_STATUS = "status";

    /** JSON field name for error response error type. */
    public static final String JSON_FIELD_ERROR = "error";

    /** JSON field name for error response message. */
    public static final String JSON_FIELD_MESSAGE = "message";

    /** JSON field name for error response retry-after value. */
    public static final String JSON_FIELD_RETRY_AFTER = "retryAfterSeconds";

    /** Error message for 429 response body. */
    public static final String ERROR_TOO_MANY_REQUESTS = "Too Many Requests";

    /** Error message detail for 429 response body. */
    public static final String ERROR_RATE_LIMIT_MESSAGE = "Rate limit exceeded. Please retry after the indicated time.";

    /** Rate limit tier name for login endpoints. */
    public static final String TIER_LOGIN = "login";

    /** Rate limit tier name for public (unauthenticated) endpoints. */
    public static final String TIER_PUBLIC = "public";

    /** Rate limit tier name for authenticated endpoints. */
    public static final String TIER_AUTHENTICATED = "authenticated";

    /** Header used to obtain the original client IP behind a reverse proxy. */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    private static final Set<String> BYPASS_PREFIXES = Set.of(
            "/actuator",
            "/health"
    );

    private static final Set<String> LOGIN_PATH_PREFIXES = Set.of(
            "/v1/security/login"
    );

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/v1/security/register"
    );

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a RateLimitingFilter.
     *
     * @param rateLimiterService   the service that performs rate limit checks
     * @param rateLimitProperties  the rate limit configuration properties
     * @param objectMapper         Jackson mapper for JSON error responses
     */
    public RateLimitingFilter(RateLimiterService rateLimiterService,
                              RateLimitProperties rateLimitProperties,
                              ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitProperties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Bypass health/actuator
        if (isBypassed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tier = resolveTier(path);
        RateLimitProperties.TierProperties tierConfig = rateLimitProperties.tiers().get(tier);

        if (tierConfig == null) {
            // No rate limit configured for this tier — allow
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request, tier);
        RateLimitResult result = rateLimiterService.checkRateLimit(
                key, tierConfig.limit(), tierConfig.windowMs());

        // Always set rate limit headers
        writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            writeRateLimitExceededResponse(response, result);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether the request path should bypass rate limiting.
     *
     * @param path the request URI
     * @return true if the path is exempt from rate limiting
     */
    private boolean isBypassed(String path) {
        return BYPASS_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Resolves the rate limit tier based on the request path.
     *
     * @param path the request URI
     * @return the tier name (login, public, or authenticated)
     */
    private String resolveTier(String path) {
        if (LOGIN_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            return TIER_LOGIN;
        }
        if (PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            return TIER_PUBLIC;
        }
        return TIER_AUTHENTICATED;
    }

    /**
     * Resolves the rate limit key for the request.
     * Unauthenticated endpoints use the client IP; authenticated endpoints
     * use the authenticated user principal.
     *
     * @param request the HTTP request
     * @param tier    the rate limit tier
     * @return the Redis key for rate limiting
     */
    private String resolveKey(HttpServletRequest request, String tier) {
        if (TIER_AUTHENTICATED.equals(tier)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                return RateLimiterService.KEY_PREFIX_USER + auth.getName();
            }
        }
        String clientIp = resolveClientIp(request);
        return RateLimiterService.KEY_PREFIX_IP + clientIp + ":" + tier;
    }

    /**
     * Resolves the client IP address, respecting the X-Forwarded-For header
     * for requests behind a reverse proxy.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes rate limit headers to the response.
     *
     * @param response the HTTP response
     * @param result   the rate limit check result
     */
    private void writeRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setIntHeader(HEADER_RATE_LIMIT, result.limit());
        response.setIntHeader(HEADER_RATE_REMAINING, result.remaining());
        response.setHeader(HEADER_RATE_RESET, String.valueOf(result.resetEpochSeconds()));
    }

    /**
     * Writes a 429 Too Many Requests JSON response with Retry-After header.
     *
     * @param response the HTTP response
     * @param result   the rate limit check result
     * @throws IOException if writing the response body fails
     */
    private void writeRateLimitExceededResponse(HttpServletResponse response, RateLimitResult result)
            throws IOException {
        long retryAfterSeconds = Math.max(1L, result.resetEpochSeconds() - Instant.now().getEpochSecond());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(JSON_FIELD_STATUS, HttpStatus.TOO_MANY_REQUESTS.value());
        body.put(JSON_FIELD_ERROR, ERROR_TOO_MANY_REQUESTS);
        body.put(JSON_FIELD_MESSAGE, ERROR_RATE_LIMIT_MESSAGE);
        body.put(JSON_FIELD_RETRY_AFTER, retryAfterSeconds);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
```

#### Step 6.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 6.3: Commit

```
feat(security): implement RateLimitingFilter

Add OncePerRequestFilter at @Order(1) that enforces per-IP
and per-user rate limits using RateLimiterService. Sets
X-RateLimit-* headers and returns 429 with Retry-After when
limits are exceeded. Bypasses actuator/health endpoints.
```

---

### Phase 7: Integration with ElatusDev Filter Chain

#### Step 7.1: Register in SecurityConfig

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

The `RateLimitingFilter` must be registered in the ElatusDev-specific `SecurityFilterChain` only. When the branching-security-filter feature is implemented, the filter will be added to the ElatusDev chain via `.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)`.

Until the branching filter is in place, add the filter to the existing dev/local filter chain:

```java
.addFilterBefore(rateLimitingFilter, JwtRequestFilter.class)
```

This ensures rate limiting runs before JWT validation.

#### Step 7.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 7.3: Commit

```
feat(security): register RateLimitingFilter in security filter chain

Add RateLimitingFilter before JwtRequestFilter to enforce
rate limits before authentication processing.
```

---

### Phase 8: Configuration

#### Step 8.1: Add rate limit properties

**File**: `application/src/main/resources/application.properties`

```properties
# Rate Limiting
rate-limit.enabled=${RATE_LIMIT_ENABLED:false}
rate-limit.tiers.login.limit=5
rate-limit.tiers.login.window-ms=900000
rate-limit.tiers.public.limit=20
rate-limit.tiers.public.window-ms=60000
rate-limit.tiers.authenticated.limit=100
rate-limit.tiers.authenticated.window-ms=60000

# Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
```

#### Step 8.2: Commit

```
feat(application): add rate limiting configuration properties

Add rate-limit tier properties (login: 5/15min, public: 20/min,
authenticated: 100/min) and Redis connection config.
```

---

### Phase 9: Unit Tests — RateLimiterServiceTest

#### Step 9.1: Create RateLimiterServiceTest

**File**: `security/src/test/java/com/akademiaplus/ratelimit/usecases/RateLimiterServiceTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock StringRedisTemplate` and `@Mock ZSetOperations`
- All test values as `public static final` constants

| @Nested | Tests |
|---------|-------|
| `AllowedRequests` | `shouldAllowRequest_whenCountBelowLimit`, `shouldSetRemainingToZero_whenCountReachesLimitMinusOne` |
| `ExceededRequests` | `shouldRejectRequest_whenCountAtLimit`, `shouldRejectRequest_whenCountAboveLimit` |
| `SlidingWindow` | `shouldRemoveExpiredEntries_whenCheckingRateLimit`, `shouldSetKeyExpiry_whenRequestAllowed` |
| `KeyFormat` | `shouldUseIpPrefix_whenKeyStartsWithIpPrefix`, `shouldUseUserPrefix_whenKeyStartsWithUserPrefix` |

#### Step 9.2: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 9.3: Commit

```
test(security): add RateLimiterService unit tests

Cover allowed requests, exceeded requests, sliding window
cleanup, key expiry, and key format validation.
```

---

### Phase 10: Unit Tests — RateLimitingFilterTest

#### Step 10.1: Create RateLimitingFilterTest

**File**: `security/src/test/java/com/akademiaplus/ratelimit/interfaceadapters/RateLimitingFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock RateLimiterService`, `@Mock RateLimitProperties`
- Use `MockHttpServletRequest` and `MockHttpServletResponse`
- Use `MockFilterChain`

| @Nested | Tests |
|---------|-------|
| `Bypass` | `shouldBypassFilter_whenPathIsActuator`, `shouldBypassFilter_whenPathIsHealth`, `shouldBypassFilter_whenRateLimitingDisabled` |
| `TierResolution` | `shouldUseLoginTier_whenPathIsLogin`, `shouldUsePublicTier_whenPathIsRegister`, `shouldUseAuthenticatedTier_whenPathIsOther` |
| `AllowedRequest` | `shouldContinueFilterChain_whenRequestAllowed`, `shouldSetRateLimitHeaders_whenRequestAllowed` |
| `RejectedRequest` | `shouldReturn429_whenRateLimitExceeded`, `shouldSetRetryAfterHeader_whenRateLimitExceeded`, `shouldWriteJsonErrorBody_whenRateLimitExceeded` |
| `KeyResolution` | `shouldUseIpKey_whenRequestIsUnauthenticated`, `shouldUseUserKey_whenRequestIsAuthenticated`, `shouldUseXForwardedFor_whenHeaderPresent` |

#### Step 10.2: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 10.3: Commit

```
test(security): add RateLimitingFilter unit tests

Cover bypass paths, tier resolution, allowed/rejected
requests, response headers, JSON error body, and key
resolution for IP vs authenticated user.
```

---

### Phase 11: Component Tests

#### Step 11.1: Create RateLimitComponentTest

**File**: `security/src/test/java/com/akademiaplus/ratelimit/RateLimitComponentTest.java`

- `@SpringBootTest` with embedded Redis (Testcontainers or embedded-redis)
- `@AutoConfigureMockMvc`
- Full Spring context — RateLimitingFilter, RateLimiterService, Redis

| @Nested | Tests |
|---------|-------|
| `LoginEndpoint` | `shouldReturn429_whenLoginExceedsLimit`, `shouldReturnRateLimitHeaders_whenLoginRequest`, `shouldResetCounter_whenWindowExpires` |
| `PublicEndpoint` | `shouldAllowUpToLimit_whenPublicEndpointCalled`, `shouldReturn429_whenPublicLimitExceeded` |
| `AuthenticatedEndpoint` | `shouldAllowUpToLimit_whenAuthenticatedEndpointCalled`, `shouldReturn429_whenAuthenticatedLimitExceeded` |
| `BypassEndpoints` | `shouldNotRateLimit_whenActuatorEndpoint`, `shouldNotRateLimit_whenHealthEndpoint` |

#### Step 11.2: Compile + verify

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl security -am -f platform-core-api/pom.xml
```

#### Step 11.3: Commit

```
test(security): add rate limiting component tests

Full Spring context + embedded Redis. Cover login, public,
and authenticated rate limits, bypass endpoints, response
headers, and window reset behavior.
```

---

## 5. File Inventory

### New files (10)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `security/src/main/java/com/akademiaplus/config/RateLimitProperties.java` | security | 1 |
| 2 | `security/src/main/java/com/akademiaplus/config/RedisConfiguration.java` | security | 2 |
| 3 | `security/src/main/java/com/akademiaplus/ratelimit/usecases/domain/RateLimitResult.java` | security | 3 |
| 4 | `security/src/main/java/com/akademiaplus/ratelimit/usecases/RateLimiterService.java` | security | 4 |
| 5 | `security/src/main/java/com/akademiaplus/ratelimit/exceptions/RateLimitExceededException.java` | security | 5 |
| 6 | `security/src/main/java/com/akademiaplus/ratelimit/interfaceadapters/RateLimitingFilter.java` | security | 6 |
| 7 | `security/src/test/java/com/akademiaplus/ratelimit/usecases/RateLimiterServiceTest.java` | security | 9 |
| 8 | `security/src/test/java/com/akademiaplus/ratelimit/interfaceadapters/RateLimitingFilterTest.java` | security | 10 |
| 9 | `security/src/test/java/com/akademiaplus/ratelimit/RateLimitComponentTest.java` | security | 11 |
| 10 | Docker Compose Redis service (docker-compose.dev.yml) | infra | 2 |

### Modified files (3)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `security/pom.xml` | Add spring-boot-starter-data-redis | 2 |
| 2 | `security/.../config/SecurityConfig.java` | Register RateLimitingFilter | 7 |
| 3 | `application/src/main/resources/application.properties` | Rate limit + Redis config | 8 |

---

## 6. Key Design Decisions

### 6.1 Algorithm Trade-off

| Criterion | Sliding Window (chosen) | Fixed Window | Token Bucket |
|-----------|------------------------|--------------|--------------|
| Burst protection | Smooth — no boundary spike | Allows 2x burst at window boundary | Allows configurable burst |
| Memory | O(n) per key (one entry per request) | O(1) per key (single counter) | O(1) per key |
| Accuracy | Exact count in any window | Approximate — boundary issue | Token-based — different semantics |
| Redis ops | 3-4 per check (ZREMRANGEBYSCORE, ZCARD, ZADD, EXPIRE) | 1-2 per check (INCR, EXPIRE) | 2-3 per check (GET, DECR, SET) |
| Implementation | Medium — sorted sets | Simple — INCR + EXPIRE | Medium — atomic refill logic |
| Chosen because | Best accuracy-to-complexity ratio; prevents the fixed-window boundary-burst problem without token bucket's semantic complexity | — | — |

### 6.2 Per-IP vs Per-User Trade-off

| Criterion | Per-IP (unauthenticated) | Per-User (authenticated) | Hybrid (chosen) |
|-----------|-------------------------|--------------------------|------------------|
| Scope | Broad — shared among users behind NAT | Precise — per individual | Best of both |
| Evasion | VPN/proxy rotation | Requires account churn | Multiple defenses |
| False positives | Possible for shared IPs (offices, schools) | Rare | Minimized for authenticated, accepted for unauthenticated |
| Use case fit | Login/register brute force | API abuse | Login: per-IP, API: per-user |

### 6.3 Redis vs In-Memory Trade-off

| Criterion | Redis (chosen) | ConcurrentHashMap | Caffeine/Guava |
|-----------|---------------|-------------------|----------------|
| Multi-instance | Shared across all app instances | Per-instance only | Per-instance only |
| Persistence | Survives restarts (optional) | Lost on restart | Lost on restart |
| Scalability | Horizontal — single source of truth | No coordination | No coordination |
| Latency | ~1ms network hop | ~0.01ms | ~0.01ms |
| Dependency | Requires Redis server | None | Library only |
| Chosen because | Platform runs multiple instances behind a load balancer; in-memory counters would allow N x limit requests across N instances | — | — |

---

## 7. Multi-Tenancy Considerations

Rate limiting operates **independently of tenant context**:

1. **Login endpoints**: Rate limiting runs before authentication, so no tenant context is available. Rate limits apply per-IP regardless of which tenant the user belongs to. This is intentional — brute-force attacks target the login endpoint itself, not a specific tenant.

2. **Authenticated endpoints**: Rate limiting uses the authenticated user's principal (from `SecurityContextHolder`), which inherently includes tenant isolation since users belong to specific tenants. The rate limit key (`rate:user:{userId}`) is unique per user, and users cannot access other tenants' data.

3. **No tenant-scoped rate limit keys**: Adding `tenantId` to rate limit keys would allow attackers to bypass per-IP limits by cycling through tenant IDs in login attempts. Per-IP limits must be tenant-agnostic.

4. **Tenant-specific rate limit configuration**: Future enhancement — `RateLimitProperties` could support per-tenant overrides (e.g., premium tenants get higher limits). Not implemented in this phase.

---

## 8. Future Extensibility

1. **Per-tenant rate limit overrides**: Extend `RateLimitProperties` with a `Map<Long, TierProperties> tenantOverrides` to allow premium tenants higher limits.
2. **Rate limit by API key**: For future public API consumers, add a `rate:apikey:{key}` tier.
3. **Distributed rate limiting with Lua scripts**: Combine ZREMRANGEBYSCORE + ZCARD + ZADD into a single atomic Lua script to eliminate race conditions between concurrent checks.
4. **Rate limit dashboard**: Expose `/actuator/ratelimit` endpoint with current rate limit statistics per tier.
5. **Dynamic rate adjustment**: Integration with anomaly detection to temporarily lower limits during suspected attacks.
6. **Graceful degradation**: If Redis is unavailable, fall back to in-memory rate limiting per instance with a warning log.

---

## 9. Verification Checklist

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — all rate limit tests green
- [ ] `mvn verify -pl security -am -f platform-core-api/pom.xml` — component tests green
- [ ] Redis starts and rate limiting works end-to-end locally
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, `shouldDoX_whenY()`, zero `any()` matchers
- [ ] `RateLimitResult` record in `usecases/domain/` (Hard Rule #13)
- [ ] `RateLimiterService` in `usecases/` (Hard Rule #12)
- [ ] `RateLimitingFilter` in `interfaceadapters/` (DESIGN.md 3.2.5)
- [ ] No `new EntityDataModel()` — all via `applicationContext.getBean()` (not applicable, no entities)
- [ ] Rate limiting only applies to ElatusDev filter chain
- [ ] Actuator/health endpoints are bypassed
- [ ] X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset headers present on all responses
- [ ] 429 response includes Retry-After header and JSON body
- [ ] Conventional Commits format, no AI attribution

---

## 10. Critical Reminders

1. **Redis must be running** — rate limiting depends on Redis. Add Redis to `docker-compose.dev.yml` if not already present.
2. **`@Order(1)`** — the filter must run before JwtRequestFilter (`@Order(3)`) to enforce rate limits before authentication.
3. **`@ConditionalOnProperty`** — RedisConfiguration only activates when `rate-limit.enabled=true`. In tests, ensure this property is set.
4. **X-Forwarded-For trust** — in production, only trust `X-Forwarded-For` from known reverse proxies. Consider adding a trusted-proxy allowlist to `RateLimitProperties`.
5. **UUID member uniqueness** — each ZADD uses `timestamp:UUID` as the member to prevent score collisions for concurrent requests at the same millisecond.
6. **Window expiry via EXPIRE** — the `EXPIRE` TTL is set to `windowMs + 1 second` to ensure the key survives slightly longer than the window for accurate counting at boundary.
7. **No `any()` matchers** — all mock stubbing uses exact values or `ArgumentCaptor`.
8. **Graceful degradation** — if Redis is unavailable, the filter should log an error and allow the request rather than blocking all traffic. Consider wrapping the Redis call in a try-catch with fallback.
