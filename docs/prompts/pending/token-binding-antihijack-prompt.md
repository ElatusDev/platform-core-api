# Token Binding & Anti-Hijack — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/token-binding-antihijack-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 -> 2 -> ... -> 8). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header (2026).
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals -> `public static final` constants, shared between impl and tests.
9. Use `applicationContext.getBean()` for all entity instantiation — never `new EntityDataModel()`.
10. Read existing files BEFORE modifying — field names, import paths, and class names vary.
11. Commit after each phase using the commit message provided.
12. Use `Long` not `Integer` for all numeric IDs.

---

## Phase 1: DeviceFingerprintService + Domain Records

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

Find the `HashingService`:
```bash
grep -rn "class HashingService" utilities/src/main/java/
cat <result>
```

### Step 1.1: Create directory structure

```bash
mkdir -p security/src/main/java/com/akademiaplus/tokenbinding/usecases/domain
mkdir -p security/src/main/java/com/akademiaplus/tokenbinding/interfaceadapters/config
mkdir -p security/src/main/java/com/akademiaplus/tokenbinding/exceptions
```

### Step 1.2: Create TokenBindingMode enum

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/domain/TokenBindingMode.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases.domain;

/**
 * Defines the strictness mode for token-to-device binding verification.
 *
 * <ul>
 *   <li>{@link #STRICT} — full match required: IP + device fingerprint</li>
 *   <li>{@link #RELAXED} — allow IP changes, reject device changes</li>
 *   <li>{@link #OFF} — token binding disabled</li>
 * </ul>
 *
 * @author ElatusDev
 * @since 1.0
 */
public enum TokenBindingMode {
    STRICT,
    RELAXED,
    OFF
}
```

### Step 1.3: Create DeviceFingerprint record

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/domain/DeviceFingerprint.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases.domain;

/**
 * Immutable representation of a device fingerprint derived from HTTP request context.
 *
 * <p>The {@code fullHash} includes all components (IP + device). The {@code deviceOnlyHash}
 * excludes the IP, enabling RELAXED mode to detect device changes while allowing IP changes.</p>
 *
 * @param fullHash       SHA-256 hash of clientIP + User-Agent + Accept-Language + X-Device-Id
 * @param deviceOnlyHash SHA-256 hash of User-Agent + Accept-Language + X-Device-Id (no IP)
 * @param clientIp       the resolved client IP address (for anomaly logging)
 * @author ElatusDev
 * @since 1.0
 */
public record DeviceFingerprint(String fullHash, String deviceOnlyHash, String clientIp) {
}
```

### Step 1.4: Create AnomalyEvent record

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/domain/AnomalyEvent.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases.domain;

/**
 * Represents a suspicious event detected during token binding verification.
 *
 * @param username     the JWT subject (user identifier)
 * @param eventType    the type of anomaly (IP_CHANGE, DEVICE_MISMATCH, FULL_MISMATCH)
 * @param expectedIp   the IP embedded in the token fingerprint (null if unavailable)
 * @param actualIp     the IP from the current request
 * @param tenantId     the tenant context
 * @param timestamp    the event timestamp (epoch millis)
 * @param details      additional context for logging
 * @author ElatusDev
 * @since 1.0
 */
public record AnomalyEvent(
        String username,
        String eventType,
        String expectedIp,
        String actualIp,
        Long tenantId,
        long timestamp,
        String details
) {

    /** Event type constant for IP address change within session. */
    public static final String EVENT_TYPE_IP_CHANGE = "IP_CHANGE";

    /** Event type constant for device-component fingerprint mismatch. */
    public static final String EVENT_TYPE_DEVICE_MISMATCH = "DEVICE_MISMATCH";

    /** Event type constant for full fingerprint mismatch (IP + device). */
    public static final String EVENT_TYPE_FULL_MISMATCH = "FULL_MISMATCH";
}
```

### Step 1.5: Create DeviceFingerprintService

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/DeviceFingerprintService.java`

- `@Service`
- Constructor-injected `HashingService`
- Constants: `HEADER_DEVICE_ID = "X-Device-Id"`, `HEADER_USER_AGENT = "User-Agent"`, `HEADER_ACCEPT_LANGUAGE = "Accept-Language"`, `HEADER_FORWARDED_FOR = "X-Forwarded-For"`, `FINGERPRINT_SEPARATOR = "|"`, `UNKNOWN_VALUE = "unknown"`
- `computeFingerprint(HttpServletRequest)` — resolves client IP (X-Forwarded-For first IP, else getRemoteAddr()), extracts headers, concatenates with separator, hashes via `HashingService`:
  - `fullHash = hashingService.generateHash(clientIp + "|" + userAgent + "|" + acceptLanguage + "|" + deviceId)`
  - `deviceOnlyHash = hashingService.generateHash(userAgent + "|" + acceptLanguage + "|" + deviceId)`
- `resolveClientIp(HttpServletRequest)` — private helper, prefers X-Forwarded-For first entry
- `defaultIfBlank(String)` — private helper, returns `UNKNOWN_VALUE` if null or blank

**IMPORTANT**: Read `HashingService` first to get the exact method name (`generateHash()`, `hash()`, etc.).

### Step 1.6: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 1.7: Commit

```bash
git add security/src/main/java/com/akademiaplus/tokenbinding/
git commit -m "feat(security): add DeviceFingerprintService and token binding domain records

Add DeviceFingerprint, TokenBindingMode, and AnomalyEvent domain
records. Add DeviceFingerprintService that computes SHA-256
fingerprint hashes from HTTP request context (IP, User-Agent,
Accept-Language, X-Device-Id)."
```

---

## Phase 2: JwtTokenProvider Enhancement

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
```

### Step 2.1: Add fingerprint claim constants

Add to `JwtTokenProvider.java`:

```java
/** JWT claim name for the full device fingerprint hash (IP + device components). */
public static final String FINGERPRINT_CLAIM = "fpr";

/** JWT claim name for the device-only fingerprint hash (no IP). */
public static final String DEVICE_FINGERPRINT_CLAIM = "dfpr";
```

**IMPORTANT**: The existing `createToken(String username, Long tenantId, Map<String, Object> additionalClaims)` method already accepts a claims map. Verify this by reading the file. The fingerprint will be passed as:

```java
additionalClaims.put(JwtTokenProvider.FINGERPRINT_CLAIM, fingerprint.fullHash());
additionalClaims.put(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, fingerprint.deviceOnlyHash());
```

No method signature change is needed unless `createToken()` does not accept `additionalClaims`.

### Step 2.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 2.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
git commit -m "feat(security): add fingerprint claim constants to JwtTokenProvider

Add FINGERPRINT_CLAIM (fpr) and DEVICE_FINGERPRINT_CLAIM (dfpr)
public static final constants for token binding. The existing
createToken() additionalClaims map is used to embed fingerprints."
```

---

## Phase 3: TokenBindingFilter + TokenBindingException

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
```

Note `JwtRequestFilter`'s `@Order(3)`. The `TokenBindingFilter` must be `@Order(4)` to run after JWT validation.

### Step 3.1: Create TokenBindingException

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/exceptions/TokenBindingException.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.exceptions;

/**
 * Thrown when a token binding verification fails — the JWT fingerprint
 * does not match the current request's device fingerprint.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class TokenBindingException extends RuntimeException {

    /** Error message for token binding mismatch. */
    public static final String ERROR_TOKEN_BINDING_MISMATCH =
            "Token binding mismatch: request fingerprint does not match token fingerprint";

    /** Error code returned in the 401 response body. */
    public static final String ERROR_CODE_TOKEN_BINDING = "TOKEN_BINDING_MISMATCH";

    /**
     * Creates a new TokenBindingException with the given message.
     *
     * @param message the detail message
     */
    public TokenBindingException(String message) {
        super(message);
    }
}
```

### Step 3.2: Create TokenBindingFilter

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/interfaceadapters/TokenBindingFilter.java`

- `@Component`, `@Order(4)`
- Extends `OncePerRequestFilter`
- Constructor-injected: `JwtTokenProvider`, `DeviceFingerprintService`, `AnomalyDetectionService`, `TokenBindingProperties`
- Constants: `ERROR_RESPONSE_TYPE = "application/json"`, `ERROR_RESPONSE_BODY = "{\"error\":\"TOKEN_BINDING_MISMATCH\",\"message\":\"Token binding verification failed\"}"`
- `AUTHORIZATION_HEADER = "Authorization"`, `BEARER_PREFIX = "Bearer "`

**`doFilterInternal()` logic**:

1. Read `mode` from `TokenBindingProperties`. If `OFF`, call `chain.doFilter()` and return.
2. If `SecurityContextHolder.getContext().getAuthentication() == null`, call `chain.doFilter()` and return (no JWT was validated).
3. Extract JWT from `Authorization: Bearer <token>` header. If null, call `chain.doFilter()` and return.
4. Get claims from JWT via `jwtTokenProvider.getClaims(token)`.
5. Read `fpr` claim (`JwtTokenProvider.FINGERPRINT_CLAIM`). If null (legacy token), call `chain.doFilter()` and return.
6. Read `dfpr` claim (`JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM`).
7. Compute current fingerprint via `deviceFingerprintService.computeFingerprint(request)`.
8. Extract `username` and `tenantId` from claims for anomaly logging.
9. **STRICT mode**:
   - If `fpr` claim != `currentFingerprint.fullHash()` -> log anomaly (FULL_MISMATCH), reject with 401.
10. **RELAXED mode**:
    - If `dfpr` claim != `currentFingerprint.deviceOnlyHash()` -> log anomaly (DEVICE_MISMATCH), reject with 401.
    - Else if `fpr` claim != `currentFingerprint.fullHash()` -> log anomaly (IP_CHANGE), but continue (allow).
11. Call `chain.doFilter()`.

**`rejectRequest(HttpServletResponse)`**: Set status 401, content type JSON, write error body.

**`extractToken(HttpServletRequest)`**: Private helper to extract Bearer token.

### Step 3.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/tokenbinding/
git commit -m "feat(security): add TokenBindingFilter with strict/relaxed/off modes

Add OncePerRequestFilter that verifies JWT fingerprint claim (fpr)
matches the current request device fingerprint. Supports STRICT
(exact match), RELAXED (allow IP change), and OFF modes. Returns
401 TOKEN_BINDING_MISMATCH on verification failure."
```

---

## Phase 4: AnomalyDetectionService

### Step 4.1: Create AnomalyDetectionService

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/AnomalyDetectionService.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases;

// imports...

/**
 * Logs suspicious patterns detected during token binding verification.
 *
 * <p>This is a logging-only service for the initial implementation.
 * Future iterations may integrate with geo-IP services for impossible-travel
 * detection, or push events to a SIEM/alerting system.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class AnomalyDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnomalyDetectionService.class);

    /** Log template for anomaly events. */
    public static final String LOG_TEMPLATE_ANOMALY =
            "Token binding anomaly detected: type={}, user={}, tenant={}, expectedIp={}, actualIp={}, details={}";

    /**
     * Logs an anomaly event at WARN level.
     *
     * @param event the anomaly event to log
     */
    public void logAnomaly(AnomalyEvent event) {
        LOGGER.warn(LOG_TEMPLATE_ANOMALY,
                event.eventType(),
                event.username(),
                event.tenantId(),
                event.expectedIp(),
                event.actualIp(),
                event.details());
    }
}
```

### Step 4.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 4.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/tokenbinding/usecases/AnomalyDetectionService.java
git commit -m "feat(security): add AnomalyDetectionService for token binding events

Add logging-only anomaly detection service that records IP changes,
device mismatches, and full fingerprint mismatches at WARN level.
Placeholder for future geo-IP impossible-travel detection."
```

---

## Phase 5: Configuration Properties

### Read first

```bash
cat application/src/main/resources/application.properties
# or
cat application/src/main/resources/application.yml
```

Check which format is used. Look for existing `@ConfigurationProperties` patterns:

```bash
grep -rn "@ConfigurationProperties" security/src/main/java/ application/src/main/java/
```

### Step 5.1: Create TokenBindingProperties

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/interfaceadapters/config/TokenBindingProperties.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.interfaceadapters.config;

import com.akademiaplus.tokenbinding.usecases.domain.TokenBindingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for token binding anti-hijack feature.
 *
 * <p>Bound to {@code security.elatus.token-binding.*} in application properties.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "security.elatus.token-binding")
public class TokenBindingProperties {

    /** Default token binding mode. */
    public static final String DEFAULT_MODE = "STRICT";

    /** The strictness mode for token binding verification. */
    private TokenBindingMode mode = TokenBindingMode.STRICT;

    /**
     * Returns the token binding strictness mode.
     *
     * @return the current mode
     */
    public TokenBindingMode getMode() {
        return mode;
    }

    /**
     * Sets the token binding strictness mode.
     *
     * @param mode the mode to set
     */
    public void setMode(TokenBindingMode mode) {
        this.mode = mode;
    }
}
```

### Step 5.2: Create TokenBindingConfiguration

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/interfaceadapters/config/TokenBindingConfiguration.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.interfaceadapters.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that enables token binding properties.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(TokenBindingProperties.class)
public class TokenBindingConfiguration {
}
```

### Step 5.3: Add application properties

Add to the application properties file (match the existing format — `.properties` or `.yml`):

**If `.properties`**:
```properties
# Token Binding Anti-Hijack (ElatusDev only)
security.elatus.token-binding.mode=STRICT
```

**If `.yml`**:
```yaml
security:
  elatus:
    token-binding:
      mode: STRICT
```

For local/dev profiles, add to `application-local.properties` (or equivalent):
```properties
security.elatus.token-binding.mode=OFF
```

### Step 5.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 5.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/tokenbinding/interfaceadapters/config/
git add application/src/main/resources/
git commit -m "feat(security): add token binding configuration properties

Add TokenBindingProperties bound to security.elatus.token-binding.*
with STRICT default mode. Add TokenBindingConfiguration to enable
property binding. Set mode=OFF for local/dev profiles."
```

---

## Phase 6: Integration with ElatusDev Filter Chain

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/InternalAuthController.java
cat security/src/main/java/com/akademiaplus/internal/usecases/InternalAuthenticationUseCase.java
```

Understand how login currently works and where the JWT is created. Find the exact method that calls `jwtTokenProvider.createToken()`.

### Step 6.1: Register TokenBindingFilter in SecurityConfig

Add `TokenBindingFilter` to the filter chain, after `JwtRequestFilter`:

```java
.addFilterAfter(tokenBindingFilter, JwtRequestFilter.class)
```

Add `TokenBindingFilter` as a constructor parameter of the `securityFilterChain()` method.

**If branching-security-filter is NOT yet implemented**: The `TokenBindingFilter` is already `@Component` + `@Order(4)`, and it checks `mode=OFF` to skip. The `@Order` ensures it runs after `JwtRequestFilter`. Manually adding it via `addFilterAfter()` in `SecurityConfig` ensures explicit ordering within the Spring Security filter chain.

### Step 6.2: Update login flow to embed fingerprint

Find the code that creates the JWT at login time. This is likely in `InternalAuthenticationUseCase` or `InternalAuthController`.

**Option A** — If the login use case creates the token:
- Inject `DeviceFingerprintService` into the use case
- The use case method needs access to `HttpServletRequest` — either inject it directly or pass the fingerprint from the controller

**Option B** — If the controller creates the token:
- Inject `DeviceFingerprintService` into the controller
- Compute fingerprint before calling `createToken()`

**The preferred pattern**: Compute fingerprint in the controller (which has access to `HttpServletRequest`) and pass the fingerprint hash via the `additionalClaims` map.

```java
// In the login controller or use case:
DeviceFingerprint fingerprint = deviceFingerprintService.computeFingerprint(request);
Map<String, Object> claims = new HashMap<>();
claims.put(JwtTokenProvider.FINGERPRINT_CLAIM, fingerprint.fullHash());
claims.put(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, fingerprint.deviceOnlyHash());
// ... any existing claims ...
String token = jwtTokenProvider.createToken(username, tenantId, claims);
```

**IMPORTANT**: Read the login flow carefully. If `additionalClaims` is already being passed with other claims (e.g., roles), merge the fingerprint claims into the existing map rather than replacing it.

**IMPORTANT**: Also update the OAuth login flow (`OAuthAuthenticationUseCase` / `OAuthController`) if it exists, to embed fingerprint claims in OAuth-issued tokens. Read the OAuth controller to determine where to inject `DeviceFingerprintService`.

### Step 6.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

### Step 6.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/
git add application/src/main/java/com/akademiaplus/
git commit -m "feat(security): integrate TokenBindingFilter with filter chain

Register TokenBindingFilter after JwtRequestFilter in SecurityConfig.
Update login flow to compute device fingerprint and embed fpr/dfpr
claims in issued JWTs."
```

---

## Phase 7: Unit Tests

### Read first

```bash
# Find existing test patterns in security module
find security/src/test -name "*Test.java" | head -5
cat <first-result>
```

### Step 7.1: Create test directory

```bash
mkdir -p security/src/test/java/com/akademiaplus/tokenbinding/usecases
mkdir -p security/src/test/java/com/akademiaplus/tokenbinding/interfaceadapters
```

### Step 7.2: DeviceFingerprintServiceTest

**File**: `security/src/test/java/com/akademiaplus/tokenbinding/usecases/DeviceFingerprintServiceTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock HashingService hashingService`
- Instantiate `DeviceFingerprintService` in `@BeforeEach`
- Use `MockHttpServletRequest` from `org.springframework.mock.web`
- Constants for test values: `TEST_IP = "192.168.1.100"`, `TEST_USER_AGENT = "Mozilla/5.0"`, `TEST_ACCEPT_LANGUAGE = "en-US"`, `TEST_DEVICE_ID = "device-uuid-123"`, `TEST_FORWARDED_IP = "10.0.0.1"`, `TEST_FORWARDED_CHAIN = "10.0.0.1, 172.16.0.1"`, `EXPECTED_FULL_HASH = "full-hash-abc"`, `EXPECTED_DEVICE_HASH = "device-hash-xyz"`

**@Nested classes**:

| @Nested | Test | Description |
|---------|------|-------------|
| `FingerprintComputation` | `shouldReturnFullHash_whenAllHeadersPresent` | Set all 4 headers, verify `hashingService.generateHash()` called with correct concatenated string, verify returned `DeviceFingerprint.fullHash()` matches |
| | `shouldUseUnknownDefault_whenDeviceIdHeaderMissing` | Omit X-Device-Id, verify "unknown" substituted in hash input |
| | `shouldUseUnknownDefault_whenUserAgentHeaderMissing` | Omit User-Agent, verify "unknown" substituted |
| | `shouldUseUnknownDefault_whenAcceptLanguageHeaderMissing` | Omit Accept-Language, verify "unknown" substituted |
| `ClientIpResolution` | `shouldUseRemoteAddr_whenNoForwardedHeader` | No X-Forwarded-For, verify remoteAddr used |
| | `shouldPreferXForwardedFor_whenBothPresent` | Set both X-Forwarded-For and remoteAddr, verify X-Forwarded-For first IP used |
| | `shouldUseFirstIp_whenXForwardedForContainsChain` | Set chain "10.0.0.1, 172.16.0.1", verify "10.0.0.1" used |
| `DeviceOnlyHash` | `shouldExcludeIpFromDeviceOnlyHash_whenComputing` | Verify the device-only hash input does NOT contain the IP |

Mock stubbing pattern (zero `any()` matchers):
```java
// Given
String expectedFullInput = TEST_IP + DeviceFingerprintService.FINGERPRINT_SEPARATOR
        + TEST_USER_AGENT + DeviceFingerprintService.FINGERPRINT_SEPARATOR
        + TEST_ACCEPT_LANGUAGE + DeviceFingerprintService.FINGERPRINT_SEPARATOR
        + TEST_DEVICE_ID;
when(hashingService.generateHash(expectedFullInput)).thenReturn(EXPECTED_FULL_HASH);

String expectedDeviceInput = TEST_USER_AGENT + DeviceFingerprintService.FINGERPRINT_SEPARATOR
        + TEST_ACCEPT_LANGUAGE + DeviceFingerprintService.FINGERPRINT_SEPARATOR
        + TEST_DEVICE_ID;
when(hashingService.generateHash(expectedDeviceInput)).thenReturn(EXPECTED_DEVICE_HASH);
```

### Step 7.3: TokenBindingFilterTest

**File**: `security/src/test/java/com/akademiaplus/tokenbinding/interfaceadapters/TokenBindingFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock JwtTokenProvider jwtTokenProvider`
- `@Mock DeviceFingerprintService deviceFingerprintService`
- `@Mock AnomalyDetectionService anomalyDetectionService`
- `@Mock TokenBindingProperties tokenBindingProperties`
- `MockHttpServletRequest request`, `MockHttpServletResponse response`, `MockFilterChain filterChain`
- Instantiate `TokenBindingFilter` in `@BeforeEach`

Constants:
```java
public static final String TEST_TOKEN = "eyJhbGciOiJSUzI1NiJ9.test";
public static final String TEST_FULL_HASH = "full-hash-match";
public static final String TEST_DEVICE_HASH = "device-hash-match";
public static final String TEST_DIFFERENT_FULL_HASH = "full-hash-different";
public static final String TEST_DIFFERENT_DEVICE_HASH = "device-hash-different";
public static final String TEST_USERNAME = "testuser@example.com";
public static final Long TEST_TENANT_ID = 1L;
public static final String TEST_IP = "192.168.1.100";
```

Helper: `setUpAuthentication()` — set a mock `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.

Helper: `setUpRequestWithToken()` — set `Authorization: Bearer TEST_TOKEN` header.

Helper: `mockClaimsWithFingerprint(String fpr, String dfpr)` — return `Claims` mock with fingerprint values.

**@Nested classes**:

| @Nested | Test |
|---------|------|
| `StrictMode` | `shouldContinueChain_whenFingerprintMatchesInStrictMode` |
| | `shouldReturn401_whenFingerprintMismatchesInStrictMode` |
| | `shouldLogAnomaly_whenFingerprintMismatchesInStrictMode` |
| `RelaxedMode` | `shouldContinueChain_whenOnlyIpChangesInRelaxedMode` |
| | `shouldReturn401_whenDeviceChangesInRelaxedMode` |
| | `shouldLogIpChangeAnomaly_whenIpChangesInRelaxedMode` |
| | `shouldLogDeviceMismatch_whenDeviceChangesInRelaxedMode` |
| `OffMode` | `shouldContinueChain_whenModeIsOff` |
| `NoAuthentication` | `shouldContinueChain_whenNoAuthenticationInContext` |
| `LegacyTokens` | `shouldContinueChain_whenTokenHasNoFingerprintClaim` |
| `NoToken` | `shouldContinueChain_whenNoAuthorizationHeader` |

**Verification pattern for 401 rejection**:
```java
// Then
assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
assertThat(response.getContentAsString()).isEqualTo(TokenBindingFilter.ERROR_RESPONSE_BODY);
assertThat(filterChain.getRequest()).isNull(); // chain was NOT called
```

**Verification pattern for chain continuation**:
```java
// Then
assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
assertThat(filterChain.getRequest()).isNotNull(); // chain WAS called
```

**IMPORTANT**: After each test, clear `SecurityContextHolder`:
```java
@AfterEach
void tearDown() {
    SecurityContextHolder.clearContext();
}
```

### Step 7.4: AnomalyDetectionServiceTest

**File**: `security/src/test/java/com/akademiaplus/tokenbinding/usecases/AnomalyDetectionServiceTest.java`

- No mocks needed — instantiate `AnomalyDetectionService` directly
- Verify `logAnomaly()` does not throw for each event type

| @Nested | Test |
|---------|------|
| `AnomalyLogging` | `shouldNotThrow_whenLoggingIpChangeAnomaly` |
| | `shouldNotThrow_whenLoggingDeviceMismatchAnomaly` |
| | `shouldNotThrow_whenLoggingFullMismatchAnomaly` |

### Step 7.5: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 7.6: Commit

```bash
git add security/src/test/
git commit -m "test(security): add token binding unit tests

DeviceFingerprintServiceTest — covers fingerprint computation,
header defaults, proxy IP resolution, and device-only hash.
TokenBindingFilterTest — covers strict/relaxed/off modes, legacy
tokens, and missing authentication. AnomalyDetectionServiceTest
— covers logging for all anomaly event types."
```

---

## Phase 8: Component Tests

### Read first

```bash
find application/src/test -name "AbstractIntegrationTest.java" | head -1
cat <result>
find application/src/test -name "*ComponentTest.java" | head -5
cat <first-result>
```

Understand the component test infrastructure: base class, `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles`, Testcontainers setup.

### Step 8.1: TokenBindingComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/TokenBindingComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`, `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`
- `@Autowired MockMvc mockMvc`
- `@Autowired JwtTokenProvider jwtTokenProvider`
- `@Autowired DeviceFingerprintService deviceFingerprintService`

**Setup**:
1. Create a test user (or use existing test data from `AbstractIntegrationTest`)
2. Login to get a JWT (POST `/v1/security/login/internal` with known credentials)
3. The JWT will contain `fpr` and `dfpr` claims matching the login request's headers

**Test flow for each scenario**:
- Set specific headers on the login request to establish a baseline fingerprint
- Make authenticated requests with the same or different headers
- Assert 200 (match) or 401 (mismatch) depending on the mode

**Overriding token binding mode per test**:
- Use `@TestPropertySource(properties = "security.elatus.token-binding.mode=STRICT")` at the class level
- Or use `@DynamicPropertySource` for per-nested-class overrides

| @Nested | Tests |
|---------|-------|
| `StrictModeVerification` | `shouldReturn200_whenFingerprintMatchesInStrictMode` |
| | `shouldReturn401_whenIpChangesInStrictMode` |
| | `shouldReturn401_whenUserAgentChangesInStrictMode` |
| `RelaxedModeVerification` | `shouldReturn200_whenOnlyIpChangesInRelaxedMode` |
| | `shouldReturn401_whenDeviceChangesInRelaxedMode` |
| `OffModeVerification` | `shouldReturn200_whenFingerprintMismatchesInOffMode` |
| `LegacyTokenCompatibility` | `shouldReturn200_whenTokenHasNoFingerprintClaim` |

**Legacy token test**: Manually create a JWT without `fpr`/`dfpr` claims using `jwtTokenProvider.createToken(username, tenantId, emptyMap)`. Use this token for an authenticated request with any headers. Assert 200 (filter skips verification for legacy tokens).

### Step 8.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

### Step 8.3: Commit

```bash
git add application/src/test/
git commit -m "test(application): add token binding component test

TokenBindingComponentTest — full Spring context + Testcontainers
MariaDB. Covers strict/relaxed/off modes, fingerprint verification,
IP change handling, device mismatch detection, and legacy token
backward compatibility."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — fingerprint + filter + anomaly tests green
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests pass
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] `shouldDoX_whenY` naming with `@DisplayName`, `@Nested`
- [ ] Domain records in `usecases/domain/` (Hard Rule #13)
- [ ] `Long` not `Integer` for tenant IDs
- [ ] Conventional Commits, no AI attribution
- [ ] Token binding only applies to ElatusDev filter chain (not AkademiaPlus)
- [ ] Legacy tokens without `fpr` claim handled gracefully (backward compatible)
- [ ] `mode=OFF` completely bypasses fingerprint verification
- [ ] `TokenBindingFilter` runs after `JwtRequestFilter` (`@Order(4)` after `@Order(3)`)
- [ ] Fingerprint includes X-Forwarded-For handling for reverse proxy deployments
