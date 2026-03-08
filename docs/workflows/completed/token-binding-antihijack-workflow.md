# Token Binding & Anti-Hijack Workflow

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.
**Scope**: ElatusDev web app only (public internet). Does NOT apply to AkademiaPlus (school/IP-whitelisted).

---

## 1. Architecture Overview

### 1.1 Problem Statement

JWT bearer tokens, once stolen (via XSS, man-in-the-middle, or local storage compromise), can be replayed from any device or network. The platform needs a mechanism to bind access tokens to the originating device/session so that stolen tokens are useless on a different device.

### 1.2 Solution: Device Fingerprint Token Binding

At login time, the server computes a fingerprint hash from the client's request context (IP, User-Agent, Accept-Language, X-Device-Id header). This hash is embedded as an `fpr` (fingerprint) claim in the JWT. On every subsequent request, a filter recomputes the fingerprint from the current request and compares it to the `fpr` claim. A mismatch indicates the token is being used from a different device or network, triggering a 401 rejection.

### 1.3 Target Flow

```
Login Request
  +-- DeviceFingerprintService.computeFingerprint(HttpServletRequest)
  |     +-- hash(clientIP + User-Agent + Accept-Language + X-Device-Id)
  |     +-- returns: SHA-256 hex string
  |
  +-- JwtTokenProvider.createToken(username, tenantId, claims)
  |     +-- additionalClaims includes "fpr" = fingerprintHash
  |     +-- returns: signed JWT with embedded fingerprint
  |
  +-- Response: { token: "eyJ...fpr=abc123..." }

Subsequent Authenticated Request (ElatusDev filter chain only)
  +-- TokenBindingFilter.doFilterInternal(request, response, chain)
  |     +-- 1. Extract JWT from Authorization header
  |     +-- 2. Extract "fpr" claim from JWT
  |     +-- 3. Recompute fingerprint from current request
  |     +-- 4. Compare: fpr_claim vs fpr_current
  |     +-- 5a. MATCH or mode=OFF: chain.doFilter() (continue)
  |     +-- 5b. MISMATCH + mode=STRICT: 401 TOKEN_BINDING_MISMATCH
  |     +-- 5c. MISMATCH(IP only) + mode=RELAXED: log anomaly, continue
  |     +-- 5d. MISMATCH(device) + mode=RELAXED: 401 TOKEN_BINDING_MISMATCH
  |
  +-- AnomalyDetectionService.logAnomaly(event)
        +-- Logs: IP change mid-session, device mismatch, impossible travel (future)
```

### 1.4 Strictness Modes

| Mode | IP Change | Device Change | Use Case |
|------|-----------|---------------|----------|
| `STRICT` | Reject (401) | Reject (401) | Maximum security — default for production |
| `RELAXED` | Log + allow | Reject (401) | Mobile users who switch networks frequently |
| `OFF` | Allow | Allow | Development/testing or gradual rollout |

### 1.5 Fingerprint Components

| Component | Source | Rationale |
|-----------|--------|-----------|
| Client IP | `HttpServletRequest.getRemoteAddr()` (or `X-Forwarded-For` behind proxy) | Network-level binding |
| User-Agent | `User-Agent` header | Browser/device identification |
| Accept-Language | `Accept-Language` header | Locale fingerprint — changes rarely |
| X-Device-Id | `X-Device-Id` custom header | Client-generated stable device identifier |

### 1.6 Module Placement

Per CLAUDE.md Hard Rules #12, #13, #14 and DESIGN.md Section 3.2.8:

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `DeviceFingerprint` (record) | security | `tokenbinding/usecases/domain/` | Non-entity domain object (Hard Rule #13) |
| `DeviceFingerprintService` | security | `tokenbinding/usecases/` | Use case — computes fingerprint from request |
| `TokenBindingFilter` | security | `tokenbinding/interfaceadapters/` | Servlet filter — interface adapter |
| `TokenBindingProperties` | security | `tokenbinding/interfaceadapters/config/` | Spring `@ConfigurationProperties` |
| `TokenBindingConfiguration` | security | `tokenbinding/interfaceadapters/config/` | Spring `@Configuration` — registers filter bean |
| `AnomalyDetectionService` | security | `tokenbinding/usecases/` | Use case — logs suspicious patterns |
| `AnomalyEvent` (record) | security | `tokenbinding/usecases/domain/` | Non-entity domain object (Hard Rule #13) |
| `TokenBindingMode` (enum) | security | `tokenbinding/usecases/domain/` | Configuration enum |
| `TokenBindingException` | security | `tokenbinding/exceptions/` | Module-specific exception |

---

## 2. Current State Analysis

### 2.1 What Exists

| Component | Location | State |
|-----------|----------|-------|
| `JwtTokenProvider` | `security/.../internal/interfaceadapters/jwt/` | `createToken(username, tenantId, additionalClaims)` — accepts `Map<String, Object>` for custom claims |
| `JwtRequestFilter` | `security/.../internal/interfaceadapters/jwt/` | Extracts JWT, validates, sets SecurityContext — `@Order(3)` |
| `SecurityConfig` | `security/.../config/` | Single filter chain for dev/local profiles — no ElatusDev-specific branching yet |
| `TenantContextHolder` | `infra-common/.../persistence/config/` | Thread-local tenant context |
| `HashingService` | `utilities/.../security/` | SHA-256 hashing utility |

### 2.2 What's Missing

1. **Device fingerprint computation**: No code to extract and hash device-identifying request attributes
2. **Token binding claim**: JWT tokens do not contain a fingerprint claim
3. **Token binding filter**: No filter to verify fingerprint on subsequent requests
4. **Anomaly detection**: No logging/alerting for suspicious request patterns
5. **Configuration**: No `security.elatus.token-binding.*` properties
6. **ElatusDev filter chain separation**: SecurityConfig does not yet branch between AkademiaPlus and ElatusDev filter chains (depends on branching-security-filter workflow)

### 2.3 Dependencies

This workflow depends on the **branching-security-filter** workflow which separates the SecurityConfig into two filter chains (AkademiaPlus vs ElatusDev). The `TokenBindingFilter` must be registered only in the ElatusDev filter chain.

If the branching-security-filter is not yet implemented, the `TokenBindingFilter` can be conditionally activated via `@ConditionalOnProperty(name = "security.elatus.token-binding.mode", havingValue = "OFF", matchIfMissing = false)` and manually registered in the filter chain.

---

## 3. File Inventory

### New Files (10)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `security/.../tokenbinding/usecases/domain/DeviceFingerprint.java` | security | 1 |
| 2 | `security/.../tokenbinding/usecases/domain/TokenBindingMode.java` | security | 1 |
| 3 | `security/.../tokenbinding/usecases/domain/AnomalyEvent.java` | security | 1 |
| 4 | `security/.../tokenbinding/usecases/DeviceFingerprintService.java` | security | 1 |
| 5 | `security/.../tokenbinding/interfaceadapters/config/TokenBindingProperties.java` | security | 5 |
| 6 | `security/.../tokenbinding/interfaceadapters/config/TokenBindingConfiguration.java` | security | 5 |
| 7 | `security/.../tokenbinding/interfaceadapters/TokenBindingFilter.java` | security | 3 |
| 8 | `security/.../tokenbinding/usecases/AnomalyDetectionService.java` | security | 4 |
| 9 | `security/.../tokenbinding/exceptions/TokenBindingException.java` | security | 3 |
| 10 | `security/test/.../tokenbinding/usecases/DeviceFingerprintServiceTest.java` | security | 7 |
| 11 | `security/test/.../tokenbinding/interfaceadapters/TokenBindingFilterTest.java` | security | 7 |
| 12 | `security/test/.../tokenbinding/usecases/AnomalyDetectionServiceTest.java` | security | 7 |
| 13 | `application/test/.../usecases/TokenBindingComponentTest.java` | application | 8 |

### Modified Files (3)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `security/.../internal/interfaceadapters/jwt/JwtTokenProvider.java` | Add `FINGERPRINT_CLAIM` constant; update `createToken()` to accept optional fingerprint | 2 |
| 2 | `security/.../config/SecurityConfig.java` | Register `TokenBindingFilter` in ElatusDev filter chain (after `JwtRequestFilter`) | 6 |
| 3 | `application/src/main/resources/application.yml` (or `application.properties`) | Add `security.elatus.token-binding.*` properties | 5 |

---

## 4. Execution Phases

### Phase Dependency Graph

```
Phase 1:  DeviceFingerprintService + domain records
    |
Phase 2:  JwtTokenProvider enhancement (fpr claim)
    |
Phase 3:  TokenBindingFilter + TokenBindingException
    |
Phase 4:  AnomalyDetectionService (logging-only)
    |
Phase 5:  Configuration properties (TokenBindingProperties + TokenBindingConfiguration)
    |
Phase 6:  Integration with ElatusDev filter chain
    |
Phase 7:  Unit tests (DeviceFingerprintService, TokenBindingFilter, AnomalyDetectionService)
    |
Phase 8:  Component tests (full Spring context)
```

---

## 5. Phase-by-Phase Implementation

### Phase 1: DeviceFingerprintService + Domain Records

#### Step 1.1: Create TokenBindingMode enum

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/domain/TokenBindingMode.java`

```java
/**
 * Defines the strictness mode for token-to-device binding verification.
 *
 * @author ElatusDev
 * @since 1.0
 */
public enum TokenBindingMode {
    /** Full match required: IP + device fingerprint. */
    STRICT,
    /** Allow IP changes, reject device changes. */
    RELAXED,
    /** Token binding disabled — no fingerprint verification. */
    OFF
}
```

#### Step 1.2: Create DeviceFingerprint record

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/domain/DeviceFingerprint.java`

```java
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

#### Step 1.3: Create AnomalyEvent record

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/domain/AnomalyEvent.java`

```java
/**
 * Represents a suspicious event detected during token binding verification.
 *
 * @param username     the JWT subject (user identifier)
 * @param eventType    the type of anomaly (e.g., IP_CHANGE, DEVICE_MISMATCH)
 * @param expectedIp   the IP embedded in the token fingerprint (null if not available)
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

    public static final String EVENT_TYPE_IP_CHANGE = "IP_CHANGE";
    public static final String EVENT_TYPE_DEVICE_MISMATCH = "DEVICE_MISMATCH";
    public static final String EVENT_TYPE_FULL_MISMATCH = "FULL_MISMATCH";
}
```

#### Step 1.4: Create DeviceFingerprintService

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/DeviceFingerprintService.java`

```java
/**
 * Computes device fingerprints from HTTP request context.
 *
 * <p>Extracts client IP, User-Agent, Accept-Language, and X-Device-Id from the
 * request, concatenates them with a separator, and produces SHA-256 hashes for
 * both full (IP-inclusive) and device-only (IP-exclusive) fingerprints.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeviceFingerprintService {

    public static final String HEADER_DEVICE_ID = "X-Device-Id";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    public static final String FINGERPRINT_SEPARATOR = "|";
    public static final String UNKNOWN_VALUE = "unknown";

    private final HashingService hashingService;

    // Constructor injection

    /**
     * Computes a {@link DeviceFingerprint} from the given HTTP request.
     *
     * @param request the current HTTP servlet request
     * @return the computed device fingerprint with full and device-only hashes
     */
    public DeviceFingerprint computeFingerprint(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        String userAgent = defaultIfBlank(request.getHeader(HEADER_USER_AGENT));
        String acceptLanguage = defaultIfBlank(request.getHeader(HEADER_ACCEPT_LANGUAGE));
        String deviceId = defaultIfBlank(request.getHeader(HEADER_DEVICE_ID));

        String deviceComponents = userAgent + FINGERPRINT_SEPARATOR
                + acceptLanguage + FINGERPRINT_SEPARATOR
                + deviceId;

        String fullComponents = clientIp + FINGERPRINT_SEPARATOR + deviceComponents;

        String fullHash = hashingService.generateHash(fullComponents);
        String deviceOnlyHash = hashingService.generateHash(deviceComponents);

        return new DeviceFingerprint(fullHash, deviceOnlyHash, clientIp);
    }

    /**
     * Resolves the client IP, preferring X-Forwarded-For when behind a reverse proxy.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            // Take the first IP (client IP) from the chain
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String defaultIfBlank(String value) {
        return (value == null || value.isBlank()) ? UNKNOWN_VALUE : value;
    }
}
```

#### Step 1.5: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 1.6: Commit

```
feat(security): add DeviceFingerprintService and token binding domain records

Add DeviceFingerprint, TokenBindingMode, and AnomalyEvent domain
records. Add DeviceFingerprintService that computes SHA-256
fingerprint hashes from HTTP request context (IP, User-Agent,
Accept-Language, X-Device-Id).
```

---

### Phase 2: JwtTokenProvider Enhancement

#### Step 2.1: Read existing JwtTokenProvider

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtTokenProvider.java
```

#### Step 2.2: Add fingerprint claim constant

Add to `JwtTokenProvider`:

```java
public static final String FINGERPRINT_CLAIM = "fpr";
public static final String DEVICE_FINGERPRINT_CLAIM = "dfpr";
```

The `fpr` claim stores the full fingerprint hash (IP + device). The `dfpr` claim stores the device-only hash (no IP) for RELAXED mode comparison.

#### Step 2.3: Verify createToken signature

The existing `createToken(String username, Long tenantId, Map<String, Object> additionalClaims)` already accepts arbitrary claims. No method signature change is needed. The fingerprint claims will be passed in via `additionalClaims`:

```java
Map<String, Object> claims = new HashMap<>();
claims.put(JwtTokenProvider.FINGERPRINT_CLAIM, fingerprint.fullHash());
claims.put(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, fingerprint.deviceOnlyHash());
jwtTokenProvider.createToken(username, tenantId, claims);
```

**IMPORTANT**: If `createToken()` does not accept `additionalClaims`, add an overloaded method that does. Read the file first to verify.

#### Step 2.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 2.5: Commit

```
feat(security): add fingerprint claim constants to JwtTokenProvider

Add FINGERPRINT_CLAIM ("fpr") and DEVICE_FINGERPRINT_CLAIM ("dfpr")
public static final constants for token binding. The existing
createToken() additionalClaims map is used to embed fingerprints.
```

---

### Phase 3: TokenBindingFilter + TokenBindingException

#### Step 3.1: Create TokenBindingException

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/exceptions/TokenBindingException.java`

```java
/**
 * Thrown when a token binding verification fails — the JWT fingerprint
 * does not match the current request's device fingerprint.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class TokenBindingException extends RuntimeException {

    public static final String ERROR_TOKEN_BINDING_MISMATCH = "Token binding mismatch: request fingerprint does not match token fingerprint";
    public static final String ERROR_CODE_TOKEN_BINDING = "TOKEN_BINDING_MISMATCH";

    public TokenBindingException(String message) {
        super(message);
    }
}
```

#### Step 3.2: Create TokenBindingFilter

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/interfaceadapters/TokenBindingFilter.java`

```java
/**
 * Servlet filter that verifies the JWT fingerprint claim matches the
 * current request's device fingerprint.
 *
 * <p>This filter runs AFTER {@link JwtRequestFilter} (which sets the
 * SecurityContext) and BEFORE business logic. It only activates when
 * the token binding mode is not {@link TokenBindingMode#OFF}.</p>
 *
 * <p>Applies only to the ElatusDev filter chain (public internet).
 * AkademiaPlus (school, IP-whitelisted) requests bypass this filter.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(4) // After JwtRequestFilter (@Order(3))
public class TokenBindingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBindingFilter.class);

    public static final String ERROR_RESPONSE_TYPE = "application/json";
    public static final String ERROR_RESPONSE_BODY = "{\"error\":\"TOKEN_BINDING_MISMATCH\",\"message\":\"Token binding verification failed\"}";

    private final JwtTokenProvider jwtTokenProvider;
    private final DeviceFingerprintService deviceFingerprintService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final TokenBindingProperties tokenBindingProperties;

    // Constructor injection

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        TokenBindingMode mode = tokenBindingProperties.getMode();

        // If OFF, skip verification entirely
        if (mode == TokenBindingMode.OFF) {
            chain.doFilter(request, response);
            return;
        }

        // Only verify if authentication is present (JWT was validated by JwtRequestFilter)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        Claims claims = jwtTokenProvider.getClaims(token);
        String expectedFullHash = claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class);
        String expectedDeviceHash = claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class);

        // If no fingerprint in token (legacy token), skip verification
        if (expectedFullHash == null) {
            chain.doFilter(request, response);
            return;
        }

        DeviceFingerprint currentFingerprint = deviceFingerprintService.computeFingerprint(request);
        String username = claims.getSubject();
        Long tenantId = claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class);

        if (mode == TokenBindingMode.STRICT) {
            if (!expectedFullHash.equals(currentFingerprint.fullHash())) {
                anomalyDetectionService.logAnomaly(buildAnomalyEvent(
                        username, AnomalyEvent.EVENT_TYPE_FULL_MISMATCH,
                        null, currentFingerprint.clientIp(), tenantId));
                rejectRequest(response);
                return;
            }
        } else if (mode == TokenBindingMode.RELAXED) {
            // RELAXED: allow IP changes, reject device changes
            if (expectedDeviceHash != null
                    && !expectedDeviceHash.equals(currentFingerprint.deviceOnlyHash())) {
                anomalyDetectionService.logAnomaly(buildAnomalyEvent(
                        username, AnomalyEvent.EVENT_TYPE_DEVICE_MISMATCH,
                        null, currentFingerprint.clientIp(), tenantId));
                rejectRequest(response);
                return;
            }
            // IP changed but device same — log but allow
            if (!expectedFullHash.equals(currentFingerprint.fullHash())) {
                anomalyDetectionService.logAnomaly(buildAnomalyEvent(
                        username, AnomalyEvent.EVENT_TYPE_IP_CHANGE,
                        null, currentFingerprint.clientIp(), tenantId));
            }
        }

        chain.doFilter(request, response);
    }

    private void rejectRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(ERROR_RESPONSE_TYPE);
        response.getWriter().write(ERROR_RESPONSE_BODY);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private AnomalyEvent buildAnomalyEvent(String username, String eventType,
                                            String expectedIp, String actualIp, Long tenantId) {
        return new AnomalyEvent(username, eventType, expectedIp, actualIp,
                tenantId, System.currentTimeMillis(), null);
    }
}
```

#### Step 3.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 3.4: Commit

```
feat(security): add TokenBindingFilter with strict/relaxed/off modes

Add OncePerRequestFilter that verifies JWT fingerprint claim ("fpr")
matches the current request's device fingerprint. Supports STRICT
(exact match), RELAXED (allow IP change), and OFF modes. Returns
401 TOKEN_BINDING_MISMATCH on verification failure.
```

---

### Phase 4: AnomalyDetectionService

#### Step 4.1: Create AnomalyDetectionService

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/usecases/AnomalyDetectionService.java`

```java
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

    public static final String LOG_TEMPLATE_ANOMALY = "Token binding anomaly detected: type={}, user={}, tenant={}, expectedIp={}, actualIp={}, details={}";

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

#### Step 4.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 4.3: Commit

```
feat(security): add AnomalyDetectionService for token binding events

Add logging-only anomaly detection service that records IP changes,
device mismatches, and full fingerprint mismatches at WARN level.
Placeholder for future geo-IP impossible-travel detection.
```

---

### Phase 5: Configuration Properties

#### Step 5.1: Create TokenBindingProperties

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/interfaceadapters/config/TokenBindingProperties.java`

```java
/**
 * Configuration properties for token binding anti-hijack feature.
 *
 * <p>Bound to {@code security.elatus.token-binding.*} in application.yml.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "security.elatus.token-binding")
public class TokenBindingProperties {

    public static final String DEFAULT_MODE = "STRICT";

    /** The strictness mode for token binding verification. */
    private TokenBindingMode mode = TokenBindingMode.STRICT;

    /** Getters and setters */
    public TokenBindingMode getMode() { return mode; }
    public void setMode(TokenBindingMode mode) { this.mode = mode; }
}
```

#### Step 5.2: Create TokenBindingConfiguration

**File**: `security/src/main/java/com/akademiaplus/tokenbinding/interfaceadapters/config/TokenBindingConfiguration.java`

```java
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

#### Step 5.3: Add application properties

**File**: `application/src/main/resources/application.properties` (or `application.yml`)

Add:

```properties
# Token Binding Anti-Hijack (ElatusDev only)
security.elatus.token-binding.mode=STRICT
```

For local/dev profiles, consider setting `OFF`:

```properties
# application-local.properties
security.elatus.token-binding.mode=OFF
```

#### Step 5.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 5.5: Commit

```
feat(security): add token binding configuration properties

Add TokenBindingProperties bound to security.elatus.token-binding.*
with STRICT default mode. Add TokenBindingConfiguration to enable
property binding.
```

---

### Phase 6: Integration with ElatusDev Filter Chain

#### Step 6.1: Read current SecurityConfig

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

#### Step 6.2: Register TokenBindingFilter

The `TokenBindingFilter` uses `@Component` and `@Order(4)`, which places it after `JwtRequestFilter` (`@Order(3)`) in the Spring Security filter chain. If the ElatusDev filter chain branching is already in place, register the filter only in the ElatusDev chain.

**Option A**: If branching-security-filter is implemented, add to the ElatusDev `SecurityFilterChain`:

```java
.addFilterAfter(tokenBindingFilter, JwtRequestFilter.class)
```

**Option B**: If branching-security-filter is NOT yet implemented, the `@Order(4)` annotation and `@Component` registration will auto-register it. The filter's `doFilterInternal()` already skips processing when `mode=OFF`, so it is safe to register globally and control activation via configuration.

#### Step 6.3: Update login flow

The login endpoint (or `OAuthAuthenticationUseCase`, `InternalAuthenticationUseCase`) must inject `DeviceFingerprintService` and pass the fingerprint to `JwtTokenProvider.createToken()` via `additionalClaims`:

```java
// In the login use case / controller where token is created:
DeviceFingerprint fingerprint = deviceFingerprintService.computeFingerprint(request);
Map<String, Object> claims = new HashMap<>();
claims.put(JwtTokenProvider.FINGERPRINT_CLAIM, fingerprint.fullHash());
claims.put(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, fingerprint.deviceOnlyHash());
String token = jwtTokenProvider.createToken(username, tenantId, claims);
```

**IMPORTANT**: The login controller must have access to `HttpServletRequest`. If the current login controller does not inject `HttpServletRequest`, add it as a method parameter (Spring MVC auto-injects it).

#### Step 6.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 6.5: Commit

```
feat(security): integrate TokenBindingFilter with filter chain

Register TokenBindingFilter after JwtRequestFilter. Update login
flow to compute device fingerprint and embed fpr/dfpr claims
in issued JWTs.
```

---

### Phase 7: Unit Tests

All tests follow project conventions: Given-When-Then, `shouldDoX_whenY()`, `@DisplayName`, `@Nested`, zero `any()` matchers, `public static final` constants.

#### Step 7.1: DeviceFingerprintServiceTest

**File**: `security/src/test/java/com/akademiaplus/tokenbinding/usecases/DeviceFingerprintServiceTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock HashingService hashingService`
- Constants for all test values: IP, User-Agent, Accept-Language, X-Device-Id, expected hashes

| @Nested | Tests |
|---------|-------|
| `FingerprintComputation` | `shouldReturnFullHash_whenAllHeadersPresent`, `shouldUseUnknownDefault_whenHeaderMissing`, `shouldUseXForwardedFor_whenProxyHeaderPresent`, `shouldUseFirstIp_whenXForwardedForContainsChain` |
| `ClientIpResolution` | `shouldUseRemoteAddr_whenNoForwardedHeader`, `shouldPreferXForwardedFor_whenBothPresent` |
| `DeviceOnlyHash` | `shouldExcludeIpFromDeviceOnlyHash_whenComputing` |

#### Step 7.2: TokenBindingFilterTest

**File**: `security/src/test/java/com/akademiaplus/tokenbinding/interfaceadapters/TokenBindingFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock JwtTokenProvider`, `@Mock DeviceFingerprintService`, `@Mock AnomalyDetectionService`, `@Mock TokenBindingProperties`
- `MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain`

| @Nested | Tests |
|---------|-------|
| `StrictMode` | `shouldContinueChain_whenFingerprintMatches`, `shouldReturn401_whenFingerprintMismatches`, `shouldLogAnomaly_whenFingerprintMismatchesInStrictMode` |
| `RelaxedMode` | `shouldContinueChain_whenOnlyIpChanges`, `shouldReturn401_whenDeviceChanges`, `shouldLogIpChangeAnomaly_whenIpChangesInRelaxedMode` |
| `OffMode` | `shouldContinueChain_whenModeIsOff` |
| `NoAuthentication` | `shouldContinueChain_whenNoAuthenticationInContext` |
| `LegacyTokens` | `shouldContinueChain_whenTokenHasNoFingerprintClaim` |
| `NoToken` | `shouldContinueChain_whenNoAuthorizationHeader` |

#### Step 7.3: AnomalyDetectionServiceTest

**File**: `security/src/test/java/com/akademiaplus/tokenbinding/usecases/AnomalyDetectionServiceTest.java`

- No mocks needed — test that `logAnomaly()` does not throw
- Use `@ExtendWith(MockitoExtension.class)` if verifying logger output via mock appender, otherwise keep simple

| @Nested | Tests |
|---------|-------|
| `AnomalyLogging` | `shouldNotThrow_whenLoggingIpChangeAnomaly`, `shouldNotThrow_whenLoggingDeviceMismatchAnomaly`, `shouldNotThrow_whenLoggingFullMismatchAnomaly` |

#### Step 7.4: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 7.5: Commit

```
test(security): add token binding unit tests

DeviceFingerprintServiceTest — covers fingerprint computation,
header defaults, proxy IP resolution, and device-only hash.
TokenBindingFilterTest — covers strict/relaxed/off modes, legacy
tokens, and missing authentication. AnomalyDetectionServiceTest
— covers logging for all anomaly event types.
```

---

### Phase 8: Component Tests

#### Step 8.1: Read existing component test infrastructure

```bash
find application/src/test -name "AbstractIntegrationTest.java" | head -1
cat <result>
find application/src/test -name "*ComponentTest.java" | head -5
cat <first-result>
```

#### Step 8.2: TokenBindingComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/TokenBindingComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`
- Full Spring context + Testcontainers MariaDB
- Test flow: login → get JWT with fingerprint → make requests with matching/mismatching headers

| @Nested | Tests |
|---------|-------|
| `StrictModeVerification` | `shouldReturn200_whenFingerprintMatchesInStrictMode`, `shouldReturn401_whenIpChangesInStrictMode`, `shouldReturn401_whenUserAgentChangesInStrictMode` |
| `RelaxedModeVerification` | `shouldReturn200_whenOnlyIpChangesInRelaxedMode`, `shouldReturn401_whenDeviceChangesInRelaxedMode` |
| `OffModeVerification` | `shouldReturn200_whenFingerprintMismatchesInOffMode` |
| `LegacyTokenCompatibility` | `shouldReturn200_whenTokenHasNoFingerprintClaim` |

**Setup**: Create a user, login (obtaining JWT with fingerprint), then make authenticated requests with modified headers to simulate device/IP changes.

#### Step 8.3: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

#### Step 8.4: Commit

```
test(application): add token binding component test

TokenBindingComponentTest — full Spring context + Testcontainers
MariaDB. Covers strict/relaxed/off modes, fingerprint verification,
IP change handling, device mismatch detection, and legacy token
backward compatibility.
```

---

## 6. Key Design Decisions

### 6.1 Why hash-based fingerprinting (not raw values in JWT)?

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| Raw values in JWT | Easy to debug, readable | Exposes IP/User-Agent in token, increases token size | Rejected |
| SHA-256 hash in JWT | Small, fixed-size claim, no PII in token | Cannot decompose on mismatch to say which component changed | **Selected** |
| Encrypted values in JWT | Reversible, debuggable | Larger token, adds encryption overhead per request | Rejected |

Mitigation for the "cannot decompose" downside: store both `fpr` (full hash) and `dfpr` (device-only hash) to distinguish IP-only changes from device changes.

### 6.2 Why two hash claims (fpr + dfpr)?

RELAXED mode needs to distinguish "IP changed but device is the same" from "device changed". Storing both hashes avoids recomputing and comparing individual components. This is a single SHA-256 call at filter time.

### 6.3 Why `@Order(4)` after `JwtRequestFilter` (`@Order(3)`)?

The `TokenBindingFilter` needs the JWT to be already validated and the SecurityContext populated. It reads claims from the JWT and checks if the SecurityContext authentication is present. Placing it after `JwtRequestFilter` ensures this state is available.

### 6.4 Why logging-only AnomalyDetectionService?

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| Logging only | Simple, no infrastructure dependency, auditable via log aggregation | No real-time alerting | **Selected (Phase 1)** |
| Database persistence | Queryable, enables rate-based blocking | Adds write load per request, needs schema | Future phase |
| SIEM/webhook integration | Real-time alerting, external monitoring | Infrastructure dependency, configuration complexity | Future phase |

---

## 7. Multi-Tenancy Considerations

- The fingerprint is computed from HTTP request headers, which are tenant-agnostic. The fingerprint hash does not contain tenant-specific data.
- The `fpr` and `dfpr` claims are embedded in the JWT, which already carries `tenant_id`. The `TokenBindingFilter` reads the tenant from the JWT claims for anomaly logging but does not use it for fingerprint verification.
- Fingerprint verification applies equally to all tenants on the ElatusDev platform. There is no per-tenant configuration for token binding mode — it is a platform-level setting.

---

## 8. Future Extensibility

1. **Geo-IP impossible travel detection**: Extend `AnomalyDetectionService` with a geo-IP database (MaxMind) to compute distance/time between sequential IPs. Flag if a user's IP geolocates to a different continent within minutes.
2. **Anomaly persistence**: Store `AnomalyEvent` records in the database for query and reporting.
3. **Progressive enforcement**: Start with `OFF` or `RELAXED` mode in production, monitor anomaly logs, then switch to `STRICT` when false-positive rate is acceptable.
4. **WebSocket binding**: Extend fingerprint verification to WebSocket upgrade requests.
5. **Refresh token binding**: When refresh token rotation is implemented, bind refresh tokens to the same fingerprint — a stolen refresh token cannot be used from a different device.
6. **Per-tenant mode override**: Allow specific tenants to override the platform-level token binding mode via a tenant configuration table.

---

## 9. Verification Checklist

Run after all phases complete:

- [ ] `mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — fingerprint + filter + anomaly tests green
- [ ] `mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml` — application module compiles
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests pass
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] `shouldDoX_whenY` naming with `@DisplayName`, `@Nested`
- [ ] Domain records in `usecases/domain/` (Hard Rule #13)
- [ ] No `new EntityDataModel()` — all via `applicationContext.getBean()` (if applicable)
- [ ] `Long` not `Integer` for tenant IDs and numeric identifiers
- [ ] Conventional Commits, no AI attribution
- [ ] Token binding only applies to ElatusDev filter chain (not AkademiaPlus)
- [ ] Legacy tokens without `fpr` claim are handled gracefully (backward compatibility)
- [ ] `mode=OFF` configuration verified to completely bypass fingerprint verification

---

## 10. Critical Reminders

1. **ElatusDev only**: This feature MUST NOT apply to AkademiaPlus (school, IP-whitelisted) requests. The filter is gated by the ElatusDev filter chain or `mode=OFF` for non-ElatusDev environments.
2. **Backward compatibility**: Tokens issued before this feature is deployed will not have `fpr` claims. The filter MUST skip verification for such tokens (check for null `fpr` claim).
3. **Proxy awareness**: Behind a reverse proxy (ALB, Nginx), the client IP comes from `X-Forwarded-For`, not `getRemoteAddr()`. The `DeviceFingerprintService` handles this, but the proxy must be configured to set `X-Forwarded-For`.
4. **X-Device-Id generation**: The frontend must generate a stable device identifier (UUID stored in localStorage or similar) and send it as `X-Device-Id`. Without this header, the fingerprint relies solely on User-Agent + Accept-Language, which is weaker.
5. **Mobile networks**: Mobile devices frequently change IP addresses (WiFi to cellular, roaming). RELAXED mode accommodates this by only rejecting device-component changes.
6. **STRICT mode default**: Production should default to `STRICT`. Use `RELAXED` as a fallback if false-positive rates are too high during initial rollout.
7. **Filter ordering**: `TokenBindingFilter` MUST run after `JwtRequestFilter` — it depends on the SecurityContext being populated and the JWT being validated.
8. **No `any()` matchers**: All mock stubbing in tests uses exact values or `ArgumentCaptor`.
