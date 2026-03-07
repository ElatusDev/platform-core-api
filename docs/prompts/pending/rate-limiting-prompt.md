# Rate Limiting — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/rate-limiting-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 → 2 → ... → 11). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header (2026).
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals → `public static final` constants, shared between impl and tests.
9. Use `applicationContext.getBean()` for all entity instantiation — never `new EntityDataModel()`.
10. Read existing files BEFORE modifying — field names, import paths, and method signatures vary.
11. Commit after each phase using the commit message provided.

---

## Phase 1: RateLimitProperties

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
cat security/pom.xml
find security/src/main/java -name "*Properties.java" | head -5
```

### Step 1.1: Create RateLimitProperties

**File**: `security/src/main/java/com/akademiaplus/config/RateLimitProperties.java`

Create a `@ConfigurationProperties(prefix = "rate-limit")` record with:

- `boolean enabled` — global on/off switch
- `Map<String, TierProperties> tiers` — per-tier configuration
- Nested record `TierProperties(int limit, long windowMs)` — the max requests and window duration

Add `@EnableConfigurationProperties(RateLimitProperties.class)` annotation on the class, or register it in a `@Configuration` class.

### Step 1.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 1.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/RateLimitProperties.java
git commit -m "feat(security): add RateLimitProperties configuration

Add @ConfigurationProperties for rate-limit.* prefix with
per-tier limit and window configuration."
```

---

## Phase 2: RedisConfiguration + Dependency

### Read first

```bash
cat security/pom.xml
cat docker-compose.dev.yml
```

### Step 2.1: Add Spring Data Redis dependency

**File**: `security/pom.xml`

Add inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Step 2.2: Create RedisConfiguration

**File**: `security/src/main/java/com/akademiaplus/config/RedisConfiguration.java`

- `@Configuration`
- `@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true")`
- `@Bean StringRedisTemplate rateLimitRedisTemplate(RedisConnectionFactory connectionFactory)`

### Step 2.3: Add Redis to docker-compose.dev.yml (if not already present)

```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  command: redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru
```

### Step 2.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 2.5: Commit

```bash
git add security/pom.xml security/src/main/java/com/akademiaplus/config/RedisConfiguration.java docker-compose.dev.yml
git commit -m "feat(security): add Redis configuration for rate limiting

Add spring-boot-starter-data-redis dependency and
RedisConfiguration bean, conditional on rate-limit.enabled."
```

---

## Phase 3: RateLimitResult Domain Record

### Step 3.1: Create directory

```bash
mkdir -p security/src/main/java/com/akademiaplus/ratelimit/usecases/domain
```

### Step 3.2: Create RateLimitResult

**File**: `security/src/main/java/com/akademiaplus/ratelimit/usecases/domain/RateLimitResult.java`

Record with fields:
- `boolean allowed`
- `int limit`
- `int remaining`
- `long resetEpochSeconds`

### Step 3.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/ratelimit/
git commit -m "feat(security): add RateLimitResult domain record

Add RateLimitResult record in ratelimit/usecases/domain/
for rate limit check responses."
```

---

## Phase 4: RateLimiterService

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/RedisConfiguration.java
```

### Step 4.1: Create RateLimiterService

**File**: `security/src/main/java/com/akademiaplus/ratelimit/usecases/RateLimiterService.java`

- `@Service`
- Constructor: `StringRedisTemplate redisTemplate`
- Constants:
  - `public static final String KEY_PREFIX_IP = "rate:ip:"`
  - `public static final String KEY_PREFIX_USER = "rate:user:"`
- Method `checkRateLimit(String key, int limit, long windowMs)` returning `RateLimitResult`:
  1. `long nowMs = Instant.now().toEpochMilli()`
  2. `ZREMRANGEBYSCORE key 0 (nowMs - windowMs)` — remove expired
  3. `ZCARD key` — count current
  4. If `count >= limit` → return `RateLimitResult(false, limit, 0, resetEpoch)`
  5. `ZADD key nowMs (nowMs + ":" + UUID.randomUUID())` — add this request
  6. `EXPIRE key (windowMs/1000 + 1)` — auto-cleanup
  7. Return `RateLimitResult(true, limit, remaining, resetEpoch)`

### Step 4.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 4.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/ratelimit/usecases/RateLimiterService.java
git commit -m "feat(security): implement RateLimiterService with Redis sliding window

Add RateLimiterService using Redis sorted sets for sliding
window rate limiting. Supports per-IP and per-user keys with
configurable limits and window durations."
```

---

## Phase 5: RateLimitExceededException

### Step 5.1: Create exception

**File**: `security/src/main/java/com/akademiaplus/ratelimit/exceptions/RateLimitExceededException.java`

- Extends `RuntimeException`
- `public static final String ERROR_RATE_LIMIT_EXCEEDED = "Rate limit exceeded. Try again in %d seconds."`
- `public static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED"`
- Constructor: `RateLimitExceededException(long retryAfterSeconds)`

### Step 5.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 5.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/ratelimit/exceptions/
git commit -m "feat(security): add RateLimitExceededException

Add exception type for 429 Too Many Requests responses
with error code and retry-after message."
```

---

## Phase 6: RateLimitingFilter

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
```

Follow the same `OncePerRequestFilter` pattern but at `@Order(1)`.

### Step 6.1: Create RateLimitingFilter

**File**: `security/src/main/java/com/akademiaplus/ratelimit/interfaceadapters/RateLimitingFilter.java`

- `@Component @Order(1)`
- Extends `OncePerRequestFilter`
- Constructor: `RateLimiterService`, `RateLimitProperties`, `ObjectMapper`
- Constants for:
  - Header names: `HEADER_RATE_LIMIT = "X-RateLimit-Limit"`, `HEADER_RATE_REMAINING = "X-RateLimit-Remaining"`, `HEADER_RATE_RESET = "X-RateLimit-Reset"`, `HEADER_RETRY_AFTER = "Retry-After"`
  - Tier names: `TIER_LOGIN = "login"`, `TIER_PUBLIC = "public"`, `TIER_AUTHENTICATED = "authenticated"`
  - Error messages: `ERROR_TOO_MANY_REQUESTS`, `ERROR_RATE_LIMIT_MESSAGE`
  - JSON field names: `JSON_FIELD_STATUS`, `JSON_FIELD_ERROR`, `JSON_FIELD_MESSAGE`, `JSON_FIELD_RETRY_AFTER`
  - `HEADER_X_FORWARDED_FOR = "X-Forwarded-For"`
- Bypass paths: `Set.of("/actuator", "/health")`
- Login paths: `Set.of("/v1/security/login")`
- Public paths: `Set.of("/v1/security/register")`
- `doFilterInternal()`:
  1. If `!rateLimitProperties.enabled()` → pass through
  2. If bypassed path → pass through
  3. Resolve tier from path
  4. Get `TierProperties` from `rateLimitProperties.tiers().get(tier)`
  5. If null → pass through (no config for this tier)
  6. Resolve key: authenticated → `rate:user:{principal}`, unauthenticated → `rate:ip:{ip}:{tier}`
  7. Call `rateLimiterService.checkRateLimit(key, limit, windowMs)`
  8. Set `X-RateLimit-*` headers
  9. If not allowed → write 429 JSON response with `Retry-After`
  10. If allowed → `chain.doFilter()`
- Client IP resolution: check `X-Forwarded-For` header, use first IP, fallback to `getRemoteAddr()`
- 429 response: JSON body with `status`, `error`, `message`, `retryAfterSeconds`

### Step 6.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 6.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/ratelimit/interfaceadapters/
git commit -m "feat(security): implement RateLimitingFilter

Add OncePerRequestFilter at @Order(1) that enforces per-IP
and per-user rate limits using RateLimiterService. Sets
X-RateLimit-* headers and returns 429 with Retry-After when
limits are exceeded. Bypasses actuator/health endpoints."
```

---

## Phase 7: Integration with ElatusDev Filter Chain

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

### Step 7.1: Register RateLimitingFilter in SecurityConfig

Add `RateLimitingFilter` as a constructor parameter to `securityFilterChain()` and add:

```java
.addFilterBefore(rateLimitingFilter, JwtRequestFilter.class)
```

This places the rate limiting filter before JWT validation in the filter chain.

**IMPORTANT**: Only add this to the ElatusDev/dev/local filter chain, NOT to the mock-data-service chain.

### Step 7.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 7.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/SecurityConfig.java
git commit -m "feat(security): register RateLimitingFilter in security filter chain

Add RateLimitingFilter before JwtRequestFilter to enforce
rate limits before authentication processing."
```

---

## Phase 8: Configuration

### Read first

```bash
cat application/src/main/resources/application.properties
```

### Step 8.1: Add rate limiting and Redis properties

Append to `application/src/main/resources/application.properties`:

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

### Step 8.2: Commit

```bash
git add application/src/main/resources/application.properties
git commit -m "feat(application): add rate limiting configuration properties

Add rate-limit tier properties (login: 5/15min, public: 20/min,
authenticated: 100/min) and Redis connection config."
```

---

## Phase 9: Unit Tests — RateLimiterServiceTest

### Read first

```bash
cat security/src/main/java/com/akademiaplus/ratelimit/usecases/RateLimiterService.java
find security/src/test -name "*Test.java" | head -5
cat security/src/test/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCaseTest.java 2>/dev/null || echo "File not found"
```

### Step 9.1: Create test directory

```bash
mkdir -p security/src/test/java/com/akademiaplus/ratelimit/usecases
```

### Step 9.2: Create RateLimiterServiceTest

**File**: `security/src/test/java/com/akademiaplus/ratelimit/usecases/RateLimiterServiceTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock StringRedisTemplate redisTemplate`
- `@Mock ZSetOperations<String, String> zSetOperations`
- Constants:
  - `public static final String TEST_KEY = "rate:ip:192.168.1.1:login"`
  - `public static final int TEST_LIMIT = 5`
  - `public static final long TEST_WINDOW_MS = 900_000L`

**Setup**:
```java
@BeforeEach
void setUp() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    rateLimiterService = new RateLimiterService(redisTemplate);
}
```

**@Nested AllowedRequests**:

- `shouldAllowRequest_whenCountBelowLimit`:
  - Given: `zSetOperations.zCard(TEST_KEY)` returns `3L`
  - When: `checkRateLimit(TEST_KEY, TEST_LIMIT, TEST_WINDOW_MS)`
  - Then: `result.allowed()` is true, `result.remaining()` is 1

- `shouldSetRemainingToZero_whenCountReachesLimitMinusOne`:
  - Given: `zCard` returns `4L` (limit - 1)
  - When: `checkRateLimit`
  - Then: `result.allowed()` is true, `result.remaining()` is 0

**@Nested ExceededRequests**:

- `shouldRejectRequest_whenCountAtLimit`:
  - Given: `zCard` returns `5L` (equals limit)
  - When: `checkRateLimit`
  - Then: `result.allowed()` is false, `result.remaining()` is 0

- `shouldRejectRequest_whenCountAboveLimit`:
  - Given: `zCard` returns `10L`
  - When: `checkRateLimit`
  - Then: `result.allowed()` is false

**@Nested SlidingWindow**:

- `shouldRemoveExpiredEntries_whenCheckingRateLimit`:
  - Given: any setup
  - When: `checkRateLimit`
  - Then: verify `zSetOperations.removeRangeByScore(TEST_KEY, 0, expectedWindowStart)` was called

- `shouldSetKeyExpiry_whenRequestAllowed`:
  - Given: `zCard` returns `0L`
  - When: `checkRateLimit`
  - Then: verify `redisTemplate.expire(TEST_KEY, expectedTtl, TimeUnit.SECONDS)` was called

### Step 9.3: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 9.4: Commit

```bash
git add security/src/test/java/com/akademiaplus/ratelimit/usecases/
git commit -m "test(security): add RateLimiterService unit tests

Cover allowed requests, exceeded requests, sliding window
cleanup, key expiry, and key format validation."
```

---

## Phase 10: Unit Tests — RateLimitingFilterTest

### Read first

```bash
cat security/src/main/java/com/akademiaplus/ratelimit/interfaceadapters/RateLimitingFilter.java
```

### Step 10.1: Create test directory

```bash
mkdir -p security/src/test/java/com/akademiaplus/ratelimit/interfaceadapters
```

### Step 10.2: Create RateLimitingFilterTest

**File**: `security/src/test/java/com/akademiaplus/ratelimit/interfaceadapters/RateLimitingFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock RateLimiterService rateLimiterService`
- Use `MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain`
- Build `RateLimitProperties` manually with test tier configs
- Constants:
  - `public static final String TEST_IP = "10.0.0.1"`
  - `public static final String TEST_USER = "testuser@example.com"`
  - `public static final int TEST_LOGIN_LIMIT = 5`
  - `public static final long TEST_LOGIN_WINDOW_MS = 900_000L`
  - `public static final int TEST_PUBLIC_LIMIT = 20`
  - `public static final long TEST_PUBLIC_WINDOW_MS = 60_000L`

**@Nested Bypass**:

- `shouldBypassFilter_whenPathIsActuator`:
  - Given: request URI = "/actuator/health"
  - When: doFilterInternal
  - Then: filter chain was invoked, rateLimiterService never called

- `shouldBypassFilter_whenPathIsHealth`:
  - Given: request URI = "/health"
  - When: doFilterInternal
  - Then: filter chain was invoked

- `shouldBypassFilter_whenRateLimitingDisabled`:
  - Given: `RateLimitProperties.enabled()` = false
  - When: doFilterInternal
  - Then: filter chain was invoked, rateLimiterService never called

**@Nested TierResolution**:

- `shouldUseLoginTier_whenPathIsLogin`:
  - Given: request URI = "/v1/security/login/internal"
  - When: doFilterInternal
  - Then: `rateLimiterService.checkRateLimit` called with key containing "login" and login tier limits

- `shouldUsePublicTier_whenPathIsRegister`:
  - Given: request URI = "/v1/security/register"
  - When: doFilterInternal
  - Then: called with key containing "public"

- `shouldUseAuthenticatedTier_whenPathIsOther`:
  - Given: request URI = "/v1/courses/list", authenticated user in SecurityContext
  - When: doFilterInternal
  - Then: called with user key prefix

**@Nested AllowedRequest**:

- `shouldContinueFilterChain_whenRequestAllowed`:
  - Given: `rateLimiterService.checkRateLimit` returns `RateLimitResult(true, 5, 4, resetEpoch)`
  - When: doFilterInternal
  - Then: filter chain was invoked

- `shouldSetRateLimitHeaders_whenRequestAllowed`:
  - Given: allowed result
  - When: doFilterInternal
  - Then: response has `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` headers

**@Nested RejectedRequest**:

- `shouldReturn429_whenRateLimitExceeded`:
  - Given: `rateLimiterService.checkRateLimit` returns `RateLimitResult(false, 5, 0, resetEpoch)`
  - When: doFilterInternal
  - Then: response status is 429

- `shouldSetRetryAfterHeader_whenRateLimitExceeded`:
  - Given: exceeded result
  - When: doFilterInternal
  - Then: response has `Retry-After` header with positive value

- `shouldWriteJsonErrorBody_whenRateLimitExceeded`:
  - Given: exceeded result
  - When: doFilterInternal
  - Then: response body contains JSON with `status`, `error`, `message`, `retryAfterSeconds`

**@Nested KeyResolution**:

- `shouldUseIpKey_whenRequestIsUnauthenticated`:
  - Given: no SecurityContext authentication, request URI = "/v1/security/login/internal"
  - When: doFilterInternal
  - Then: called with key starting with `RateLimiterService.KEY_PREFIX_IP`

- `shouldUseUserKey_whenRequestIsAuthenticated`:
  - Given: authenticated user in SecurityContext, request URI = "/v1/courses/list"
  - When: doFilterInternal
  - Then: called with key starting with `RateLimiterService.KEY_PREFIX_USER`

- `shouldUseXForwardedFor_whenHeaderPresent`:
  - Given: `X-Forwarded-For: 203.0.113.50, 10.0.0.1`, request URI = "/v1/security/login/internal"
  - When: doFilterInternal
  - Then: called with key containing `203.0.113.50` (first IP)

### Step 10.3: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 10.4: Commit

```bash
git add security/src/test/java/com/akademiaplus/ratelimit/interfaceadapters/
git commit -m "test(security): add RateLimitingFilter unit tests

Cover bypass paths, tier resolution, allowed/rejected
requests, response headers, JSON error body, and key
resolution for IP vs authenticated user."
```

---

## Phase 11: Component Tests

### Read first

```bash
find security/src/test -name "*ComponentTest.java" -o -name "Abstract*Test.java" | head -5
find application/src/test -name "*ComponentTest.java" | head -5
cat $(find application/src/test -name "*ComponentTest.java" | head -1) 2>/dev/null
```

Understand the component test infrastructure (Testcontainers, `@SpringBootTest`, `AbstractIntegrationTest`).

### Step 11.1: Add test dependency for embedded Redis

**File**: `security/pom.xml` (test scope)

```xml
<dependency>
    <groupId>com.redis</groupId>
    <artifactId>testcontainers-redis</artifactId>
    <version>2.2.2</version>
    <scope>test</scope>
</dependency>
```

Or use `org.testcontainers:testcontainers` with a Redis `GenericContainer`.

### Step 11.2: Create RateLimitComponentTest

**File**: `security/src/test/java/com/akademiaplus/ratelimit/RateLimitComponentTest.java`

- `@SpringBootTest` with test properties:
  - `rate-limit.enabled=true`
  - `rate-limit.tiers.login.limit=3` (low limit for fast tests)
  - `rate-limit.tiers.login.window-ms=60000`
- Testcontainers Redis (`@Container GenericContainer redis`)
- `@DynamicPropertySource` to set Redis host/port
- `@AutoConfigureMockMvc`

**@Nested LoginEndpoint**:

- `shouldReturn429_whenLoginExceedsLimit`:
  - Given: 3 successful requests to `/v1/security/login/internal`
  - When: 4th request
  - Then: status 429, `Retry-After` header present, JSON error body

- `shouldReturnRateLimitHeaders_whenLoginRequest`:
  - Given: first request to login
  - When: perform request
  - Then: `X-RateLimit-Limit: 3`, `X-RateLimit-Remaining: 2`

**@Nested BypassEndpoints**:

- `shouldNotRateLimit_whenActuatorEndpoint`:
  - Given: many requests to `/actuator/health`
  - When: all requests
  - Then: none return 429

### Step 11.3: Compile + verify

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl security -am -f platform-core-api/pom.xml
```

### Step 11.4: Commit

```bash
git add security/src/test/java/com/akademiaplus/ratelimit/ security/pom.xml
git commit -m "test(security): add rate limiting component tests

Full Spring context + Testcontainers Redis. Cover login
rate limit enforcement, response headers, and actuator
bypass behavior."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — rate limit unit tests green
- [ ] `mvn verify -pl security -am -f platform-core-api/pom.xml` — component tests green
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, `shouldDoX_whenY()`, zero `any()` matchers
- [ ] `RateLimitResult` record in `usecases/domain/` (Hard Rule #13)
- [ ] `RateLimiterService` in `usecases/` (Hard Rule #12)
- [ ] `RateLimitingFilter` in `interfaceadapters/` (DESIGN.md 3.2.5)
- [ ] Rate limiting filter at `@Order(1)`, before JwtRequestFilter `@Order(3)`
- [ ] Actuator/health endpoints bypassed
- [ ] X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset headers on all rate-limited responses
- [ ] 429 response includes Retry-After header and JSON error body
- [ ] Redis dependency conditional on `rate-limit.enabled`
- [ ] No `any()` matchers in tests — all use exact values or `ArgumentCaptor`
- [ ] Conventional Commits format, no AI attribution
