# HMAC-SHA256 API Request & Response Signing Workflow

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.
**Scope**: ElatusDev web app only (public internet). Does NOT apply to AkademiaPlus (school/IP-whitelisted).

---

## 1. Architecture Overview

### 1.1 Problem Statement

Public-facing API endpoints are vulnerable to request tampering (man-in-the-middle modification of request bodies), replay attacks (re-sending captured valid requests), and response tampering (modifying server responses before they reach the client). TLS protects data in transit but does not provide application-layer integrity guarantees or protection against compromised intermediaries.

### 1.2 Solution: HMAC-SHA256 Request & Response Signing

Every ElatusDev authenticated API request carries an HMAC-SHA256 signature computed over the HTTP method, path, timestamp, body hash, and a nonce. The server verifies this signature before processing. After processing, the server signs the response body and returns its own HMAC-SHA256 signature, allowing the client to verify response integrity.

### 1.3 Request Signing Flow

```
Client prepares request:
  +-- 1. Generate nonce (UUID)
  +-- 2. Compute body hash: SHA-256(requestBody) -> hex
  +-- 3. Build string-to-sign: METHOD + "\n" + path + "\n" + timestamp + "\n" + bodyHash + "\n" + nonce
  +-- 4. Compute HMAC-SHA256(signingKey, stringToSign) -> hex
  +-- 5. Set headers:
  |     X-Signature: <hmac-hex>
  |     X-Timestamp: <epoch-seconds>
  |     X-Nonce: <uuid>
  |     X-Body-Hash: <sha256-hex>
  +-- 6. Send request

Server receives request:
  +-- HmacSigningFilter (OncePerRequestFilter)
  |     +-- 1. Read X-Signature, X-Timestamp, X-Nonce, X-Body-Hash headers
  |     +-- 2. Validate timestamp: |now - X-Timestamp| <= 300 seconds
  |     +-- 3. Check nonce uniqueness via NonceStore (reject replay)
  |     +-- 4. Read request body via CachedBodyHttpServletRequest
  |     +-- 5. Verify body hash: SHA-256(body) == X-Body-Hash
  |     +-- 6. Rebuild string-to-sign from request context
  |     +-- 7. Resolve signing key via HmacKeyService
  |     +-- 8. Compute expected HMAC, compare with X-Signature
  |     +-- 9a. MATCH: store nonce, chain.doFilter(cachedRequest, response)
  |     +-- 9b. MISMATCH: 401 HMAC_SIGNATURE_INVALID
  |
  +-- Business logic processes request
  |
  +-- HmacResponseFilter (OncePerRequestFilter)
        +-- 1. Capture response body via CachedBodyHttpServletResponse
        +-- 2. Build response string-to-sign: statusCode + "\n" + SHA-256(responseBody) + "\n" + timestamp + "\n" + requestNonce
        +-- 3. Compute HMAC-SHA256(signingKey, responseStringToSign) -> hex
        +-- 4. Set headers:
        |     X-Response-Signature: <hmac-hex>
        |     X-Response-Timestamp: <epoch-seconds>
        +-- 5. Write response body to actual output stream
```

### 1.4 Signature Components

#### Request Signature

| Component | Source | Purpose |
|-----------|--------|---------|
| HTTP Method | `request.getMethod()` | Prevents cross-method replay |
| Path | `request.getRequestURI()` | Prevents cross-endpoint replay |
| Timestamp | `X-Timestamp` header (epoch seconds) | Prevents old-request replay (5-minute window) |
| Body Hash | `SHA-256(request body)` | Ensures body integrity |
| Nonce | `X-Nonce` header (UUID) | Prevents exact-replay within timestamp window |

String-to-sign format:
```
POST\n/v1/courses\n1709834567\nSHA256HexOfBody\nnonce-uuid
```

#### Response Signature

| Component | Source | Purpose |
|-----------|--------|---------|
| Status Code | `response.getStatus()` | Binds signature to specific outcome |
| Response Body Hash | `SHA-256(response body)` | Ensures response integrity |
| Timestamp | Server current time (epoch seconds) | Freshness of response |
| Request Nonce | `X-Nonce` from original request | Binds response to specific request |

String-to-sign format:
```
200\nSHA256HexOfResponseBody\n1709834568\noriginal-request-nonce
```

### 1.5 Headers

| Header | Direction | Format | Required |
|--------|-----------|--------|----------|
| `X-Signature` | Request | HMAC-SHA256 hex string | Yes |
| `X-Timestamp` | Request | Unix epoch seconds (Long) | Yes |
| `X-Nonce` | Request | UUID string | Yes |
| `X-Body-Hash` | Request | SHA-256 hex string | Yes (even for empty body — hash of empty string) |
| `X-Response-Signature` | Response | HMAC-SHA256 hex string | Yes |
| `X-Response-Timestamp` | Response | Unix epoch seconds (Long) | Yes |

### 1.6 Module Placement

Per CLAUDE.md Hard Rules #12, #13, #14 and DESIGN.md Section 3.2.8:

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `HmacSignatureService` | security | `hmac/usecases/` | Use case — HMAC computation and verification |
| `HmacKeyService` | security | `hmac/usecases/` | Use case — signing key resolution per app |
| `NonceStore` | security | `hmac/usecases/` | Use case interface — nonce tracking |
| `AkademiaPlusRedisNonceStore` | security | `hmac/interfaceadapters/` | Interface adapter — Redis-backed implementation |
| `HmacSigningFilter` | security | `hmac/interfaceadapters/` | Servlet filter — request signature verification |
| `HmacResponseFilter` | security | `hmac/interfaceadapters/` | Servlet filter — response signing |
| `CachedBodyHttpServletRequest` | security | `hmac/interfaceadapters/` | Wrapper — re-readable request body |
| `CachedBodyHttpServletResponse` | security | `hmac/interfaceadapters/` | Wrapper — capturable response body |
| `HmacProperties` | security | `hmac/interfaceadapters/config/` | Spring `@ConfigurationProperties` |
| `HmacConfiguration` | security | `hmac/interfaceadapters/config/` | Spring `@Configuration` |
| `HmacSignatureException` | security | `hmac/exceptions/` | Module-specific exception |
| `HmacSignatureResult` (record) | security | `hmac/usecases/domain/` | Non-entity domain object (Hard Rule #13) |

---

## 2. Current State Analysis

### 2.1 What Exists

| Component | Location | State |
|-----------|----------|-------|
| `JwtTokenProvider` | `security/.../internal/interfaceadapters/jwt/` | Signs/validates JWTs — no HMAC |
| `JwtRequestFilter` | `security/.../internal/interfaceadapters/jwt/` | Validates JWT Bearer tokens — `@Order(3)` |
| `SecurityConfig` | `security/.../config/` | Single filter chain for dev/local — no HMAC filters |
| `HashingService` | `utilities/.../security/` | SHA-256 hashing utility — can be reused for body hashing |
| Redis | infrastructure | Used for other caching/session needs (verify availability) |

### 2.2 What's Missing

1. **HMAC signature computation**: No code to compute or verify HMAC-SHA256 signatures
2. **Nonce tracking**: No mechanism to prevent replay attacks
3. **Request body caching**: No `HttpServletRequestWrapper` that allows reading the body multiple times
4. **Response body capture**: No `HttpServletResponseWrapper` that captures the body for signing
5. **Signing key management**: No per-app signing key storage or resolution
6. **Configuration**: No `security.elatus.hmac.*` properties
7. **HMAC filters**: No request verification or response signing filters

### 2.3 Dependencies

- **Redis**: Required for `NonceStore` implementation. If Redis is not yet available in the test environment, provide an in-memory fallback for tests.
- **branching-security-filter**: The HMAC filters should only apply to ElatusDev authenticated endpoints. If the branching filter is not yet implemented, use `@ConditionalOnProperty` to control activation.

---

## 3. File Inventory

### New Files (16)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `security/.../hmac/interfaceadapters/CachedBodyHttpServletRequest.java` | security | 1 |
| 2 | `security/.../hmac/interfaceadapters/CachedBodyHttpServletResponse.java` | security | 1 |
| 3 | `security/.../hmac/usecases/domain/HmacSignatureResult.java` | security | 2 |
| 4 | `security/.../hmac/usecases/HmacSignatureService.java` | security | 2 |
| 5 | `security/.../hmac/usecases/NonceStore.java` | security | 3 |
| 6 | `security/.../hmac/interfaceadapters/AkademiaPlusRedisNonceStore.java` | security | 3 |
| 7 | `security/.../hmac/interfaceadapters/InMemoryNonceStore.java` | security | 3 |
| 8 | `security/.../hmac/usecases/HmacKeyService.java` | security | 4 |
| 9 | `security/.../hmac/interfaceadapters/HmacSigningFilter.java` | security | 5 |
| 10 | `security/.../hmac/interfaceadapters/HmacResponseFilter.java` | security | 6 |
| 11 | `security/.../hmac/interfaceadapters/config/HmacProperties.java` | security | 7 |
| 12 | `security/.../hmac/interfaceadapters/config/HmacConfiguration.java` | security | 7 |
| 13 | `security/.../hmac/exceptions/HmacSignatureException.java` | security | 5 |
| 14 | `security/test/.../hmac/usecases/HmacSignatureServiceTest.java` | security | 9 |
| 15 | `security/test/.../hmac/interfaceadapters/HmacSigningFilterTest.java` | security | 9 |
| 16 | `security/test/.../hmac/interfaceadapters/HmacResponseFilterTest.java` | security | 9 |
| 17 | `security/test/.../hmac/usecases/HmacKeyServiceTest.java` | security | 9 |
| 18 | `security/test/.../hmac/interfaceadapters/InMemoryNonceStoreTest.java` | security | 9 |
| 19 | `application/test/.../usecases/HmacSigningComponentTest.java` | application | 10 |

### Modified Files (3)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `security/.../config/SecurityConfig.java` | Register HMAC filters in ElatusDev filter chain | 8 |
| 2 | `application/src/main/resources/application.properties` | Add `security.elatus.hmac.*` properties | 7 |
| 3 | `application/src/main/resources/application-local.properties` | Disable HMAC for local dev | 7 |

---

## 4. Execution Phases

### Phase Dependency Graph

```
Phase 1:   CachedBodyHttpServletRequest + CachedBodyHttpServletResponse
    |
Phase 2:   HmacSignatureService + HmacSignatureResult record
    |
Phase 3:   NonceStore interface + AkademiaPlusRedisNonceStore + InMemoryNonceStore
    |
Phase 4:   HmacKeyService (signing key management)
    |
Phase 5:   HmacSigningFilter (request verification) + HmacSignatureException
    |
Phase 6:   HmacResponseFilter (response signing)
    |
Phase 7:   Configuration properties (HmacProperties + HmacConfiguration)
    |
Phase 8:   Integration with ElatusDev filter chain
    |
Phase 9:   Unit tests (HmacSignatureService, HmacSigningFilter, HmacResponseFilter, HmacKeyService, NonceStore)
    |
Phase 10:  Component tests (full Spring context)
```

---

## 5. Phase-by-Phase Implementation

### Phase 1: CachedBody Wrappers

#### Step 1.1: CachedBodyHttpServletRequest

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/CachedBodyHttpServletRequest.java`

```java
/**
 * HttpServletRequest wrapper that caches the request body in a byte array,
 * allowing it to be read multiple times.
 *
 * <p>The default {@link HttpServletRequest#getInputStream()} can only be read once.
 * This wrapper reads the body on construction and returns a fresh
 * {@link ByteArrayInputStream} on each subsequent call.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    /**
     * Creates a new CachedBodyHttpServletRequest by reading and caching the request body.
     *
     * @param request the original request
     * @throws IOException if the body cannot be read
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * Returns the cached body bytes.
     *
     * @return the request body as a byte array
     */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    // Inner class: CachedServletInputStream wrapping ByteArrayInputStream
}
```

#### Step 1.2: CachedBodyHttpServletResponse

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/CachedBodyHttpServletResponse.java`

```java
/**
 * HttpServletResponse wrapper that captures the response body in a byte array,
 * allowing it to be read for HMAC signing before being written to the client.
 *
 * <p>Wraps the output stream with a {@link ByteArrayOutputStream} that captures
 * all written bytes. After the response is complete, {@link #getCachedBody()}
 * returns the captured bytes and {@link #writeBodyToResponse()} flushes them
 * to the original output stream.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream cachedOutputStream = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    /**
     * Creates a new CachedBodyHttpServletResponse.
     *
     * @param response the original response
     */
    public CachedBodyHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new CachedServletOutputStream(cachedOutputStream);
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(cachedOutputStream, StandardCharsets.UTF_8));
        }
        return writer;
    }

    /**
     * Returns the captured response body bytes.
     *
     * @return the response body as a byte array
     */
    public byte[] getCachedBody() {
        if (writer != null) {
            writer.flush();
        }
        return cachedOutputStream.toByteArray();
    }

    /**
     * Writes the cached body to the original response output stream.
     *
     * @throws IOException if writing fails
     */
    public void writeBodyToResponse() throws IOException {
        byte[] body = getCachedBody();
        getResponse().setContentLength(body.length);
        getResponse().getOutputStream().write(body);
        getResponse().getOutputStream().flush();
    }

    // Inner class: CachedServletOutputStream wrapping ByteArrayOutputStream
}
```

#### Step 1.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 1.4: Commit

```
feat(security): add CachedBody request and response wrappers

Add CachedBodyHttpServletRequest (re-readable request body) and
CachedBodyHttpServletResponse (capturable response body) for
HMAC signature computation. Both extend standard servlet wrappers.
```

---

### Phase 2: HmacSignatureService

#### Step 2.1: Create HmacSignatureResult record

**File**: `security/src/main/java/com/akademiaplus/hmac/usecases/domain/HmacSignatureResult.java`

```java
/**
 * Result of an HMAC signature computation or verification.
 *
 * @param signature the computed HMAC-SHA256 hex string
 * @param valid     whether the signature matched the expected value (for verification)
 * @author ElatusDev
 * @since 1.0
 */
public record HmacSignatureResult(String signature, boolean valid) {

    /**
     * Creates a result for a newly computed signature (not yet verified).
     *
     * @param signature the computed signature
     * @return a new HmacSignatureResult
     */
    public static HmacSignatureResult computed(String signature) {
        return new HmacSignatureResult(signature, true);
    }
}
```

#### Step 2.2: Create HmacSignatureService

**File**: `security/src/main/java/com/akademiaplus/hmac/usecases/HmacSignatureService.java`

```java
/**
 * Computes and verifies HMAC-SHA256 signatures for API request and response integrity.
 *
 * <p>This service handles the cryptographic operations for both request verification
 * (client-to-server) and response signing (server-to-client). It does NOT manage
 * keys, nonces, or timestamps — those are handled by {@code HmacKeyService},
 * {@code NonceStore}, and the filters respectively.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class HmacSignatureService {

    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String STRING_TO_SIGN_SEPARATOR = "\n";
    public static final String EMPTY_BODY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // SHA-256 of ""

    /**
     * Computes the SHA-256 hash of the given bytes, returning a hex string.
     *
     * @param body the bytes to hash
     * @return the hex-encoded SHA-256 hash
     */
    public String computeBodyHash(byte[] body) {
        // Use MessageDigest SHA-256
        // Return hex string
    }

    /**
     * Builds the request string-to-sign from its components.
     *
     * @param method    HTTP method (GET, POST, etc.)
     * @param path      request URI path
     * @param timestamp epoch seconds as string
     * @param bodyHash  SHA-256 hex hash of the request body
     * @param nonce     unique request identifier
     * @return the concatenated string-to-sign
     */
    public String buildRequestStringToSign(String method, String path,
                                           String timestamp, String bodyHash, String nonce) {
        return method + STRING_TO_SIGN_SEPARATOR
                + path + STRING_TO_SIGN_SEPARATOR
                + timestamp + STRING_TO_SIGN_SEPARATOR
                + bodyHash + STRING_TO_SIGN_SEPARATOR
                + nonce;
    }

    /**
     * Builds the response string-to-sign from its components.
     *
     * @param statusCode    HTTP status code as string
     * @param bodyHash      SHA-256 hex hash of the response body
     * @param timestamp     epoch seconds as string
     * @param requestNonce  the nonce from the original request
     * @return the concatenated string-to-sign
     */
    public String buildResponseStringToSign(String statusCode, String bodyHash,
                                            String timestamp, String requestNonce) {
        return statusCode + STRING_TO_SIGN_SEPARATOR
                + bodyHash + STRING_TO_SIGN_SEPARATOR
                + timestamp + STRING_TO_SIGN_SEPARATOR
                + requestNonce;
    }

    /**
     * Computes the HMAC-SHA256 of the given string using the provided key.
     *
     * @param key            the signing key bytes
     * @param stringToSign   the data to sign
     * @return the hex-encoded HMAC-SHA256 signature
     */
    public String computeHmac(byte[] key, String stringToSign) {
        // Use javax.crypto.Mac with HmacSHA256
        // Return hex string
    }

    /**
     * Verifies that the provided signature matches the expected HMAC for the given string-to-sign.
     *
     * @param key            the signing key bytes
     * @param stringToSign   the data that was signed
     * @param providedSignature the signature to verify
     * @return true if the signatures match (constant-time comparison)
     */
    public boolean verifySignature(byte[] key, String stringToSign, String providedSignature) {
        String expectedSignature = computeHmac(key, stringToSign);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8));
    }
}
```

**IMPORTANT**: Use `MessageDigest.isEqual()` for constant-time comparison to prevent timing attacks.

#### Step 2.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 2.4: Commit

```
feat(security): add HmacSignatureService for HMAC-SHA256 computation

Add HmacSignatureService with computeBodyHash (SHA-256),
computeHmac (HMAC-SHA256), verifySignature (constant-time),
and string-to-sign builders for request and response signing.
Add HmacSignatureResult domain record.
```

---

### Phase 3: NonceStore

#### Step 3.1: Create NonceStore interface

**File**: `security/src/main/java/com/akademiaplus/hmac/usecases/NonceStore.java`

```java
/**
 * Stores and checks nonces to prevent replay attacks.
 *
 * <p>Implementations must ensure that a nonce can only be used once within
 * the configured TTL window (default 5 minutes).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface NonceStore {

    /**
     * Checks if the nonce has already been used.
     *
     * @param nonce the nonce to check
     * @return true if the nonce already exists (replay detected)
     */
    boolean exists(String nonce);

    /**
     * Stores the nonce with a TTL. After TTL expiry, the nonce may be reused.
     *
     * @param nonce the nonce to store
     * @param ttlSeconds the time-to-live in seconds
     */
    void store(String nonce, long ttlSeconds);
}
```

#### Step 3.2: Create AkademiaPlusRedisNonceStore

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/AkademiaPlusRedisNonceStore.java`

```java
/**
 * Redis-backed nonce store for replay attack prevention.
 *
 * <p>Uses Redis SET with TTL to automatically expire nonces after the
 * configured window (default 5 minutes). The key format is
 * {@code hmac:nonce:<nonce-value>}.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@ConditionalOnBean(StringRedisTemplate.class)
public class AkademiaPlusRedisNonceStore implements NonceStore {

    public static final String NONCE_KEY_PREFIX = "hmac:nonce:";
    public static final String NONCE_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    // Constructor injection

    @Override
    public boolean exists(String nonce) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(NONCE_KEY_PREFIX + nonce));
    }

    @Override
    public void store(String nonce, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                NONCE_KEY_PREFIX + nonce,
                NONCE_VALUE,
                ttlSeconds,
                TimeUnit.SECONDS);
    }
}
```

#### Step 3.3: Create InMemoryNonceStore

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/InMemoryNonceStore.java`

```java
/**
 * In-memory nonce store for testing and local development.
 *
 * <p>Uses a {@link ConcurrentHashMap} with scheduled cleanup. Not suitable
 * for production multi-instance deployments (nonces are not shared between instances).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@ConditionalOnMissingBean(AkademiaPlusRedisNonceStore.class)
public class InMemoryNonceStore implements NonceStore {

    private final ConcurrentHashMap<String, Long> nonceMap = new ConcurrentHashMap<>();

    @Override
    public boolean exists(String nonce) {
        Long expiry = nonceMap.get(nonce);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            nonceMap.remove(nonce);
            return false;
        }
        return true;
    }

    @Override
    public void store(String nonce, long ttlSeconds) {
        long expiryMillis = System.currentTimeMillis() + (ttlSeconds * 1000L);
        nonceMap.put(nonce, expiryMillis);
    }

    /**
     * Removes expired nonces. Called periodically by scheduled cleanup.
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void cleanExpiredNonces() {
        long now = System.currentTimeMillis();
        nonceMap.entrySet().removeIf(entry -> now > entry.getValue());
    }
}
```

#### Step 3.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 3.5: Commit

```
feat(security): add NonceStore interface with Redis and in-memory implementations

Add NonceStore interface for replay attack prevention. Add
AkademiaPlusRedisNonceStore (production, TTL-based) and InMemoryNonceStore
(testing/local, ConcurrentHashMap with scheduled cleanup).
```

---

### Phase 4: HmacKeyService

#### Step 4.1: Create HmacKeyService

**File**: `security/src/main/java/com/akademiaplus/hmac/usecases/HmacKeyService.java`

```java
/**
 * Manages per-application HMAC signing keys.
 *
 * <p>Keys are loaded from configuration properties. Each client application
 * (e.g., elatusdev-web) has a unique signing key. The key is identified
 * by the {@code X-App-Id} header or derived from the authenticated user's context.</p>
 *
 * <p>For the initial implementation, a single shared key is used for all
 * ElatusDev clients. Per-app keys can be added in a future iteration.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class HmacKeyService {

    public static final String DEFAULT_APP_ID = "elatusdev-web";

    private final HmacProperties hmacProperties;

    // Constructor injection

    /**
     * Resolves the signing key for the given application.
     *
     * @param appId the application identifier (from X-App-Id header or default)
     * @return the signing key as bytes
     * @throws HmacSignatureException if no key is configured for the given appId
     */
    public byte[] resolveKey(String appId) {
        String key = hmacProperties.getKeys().get(appId);
        if (key == null) {
            key = hmacProperties.getKeys().get(DEFAULT_APP_ID);
        }
        if (key == null) {
            throw new HmacSignatureException(
                    String.format(HmacSignatureException.ERROR_NO_KEY_CONFIGURED, appId));
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Resolves the default signing key (for single-app deployments).
     *
     * @return the default signing key as bytes
     */
    public byte[] resolveDefaultKey() {
        return resolveKey(DEFAULT_APP_ID);
    }
}
```

#### Step 4.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 4.3: Commit

```
feat(security): add HmacKeyService for signing key management

Add HmacKeyService that resolves per-application HMAC signing
keys from configuration properties. Initial implementation uses
a single shared key for elatusdev-web.
```

---

### Phase 5: HmacSigningFilter + HmacSignatureException

#### Step 5.1: Create HmacSignatureException

**File**: `security/src/main/java/com/akademiaplus/hmac/exceptions/HmacSignatureException.java`

```java
/**
 * Thrown when HMAC signature verification fails.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class HmacSignatureException extends RuntimeException {

    /** Error message for invalid HMAC signature. */
    public static final String ERROR_SIGNATURE_INVALID = "HMAC signature verification failed: %s";

    /** Error message for missing required HMAC headers. */
    public static final String ERROR_MISSING_HEADERS = "Missing required HMAC headers: %s";

    /** Error message for timestamp out of tolerance. */
    public static final String ERROR_TIMESTAMP_EXPIRED = "Request timestamp outside tolerance window: delta=%d seconds, max=%d seconds";

    /** Error message for replay attack (nonce reuse). */
    public static final String ERROR_NONCE_REPLAY = "Nonce has already been used (replay attack detected): %s";

    /** Error message for body hash mismatch. */
    public static final String ERROR_BODY_HASH_MISMATCH = "Request body hash does not match X-Body-Hash header";

    /** Error message for no signing key configured. */
    public static final String ERROR_NO_KEY_CONFIGURED = "No HMAC signing key configured for app: %s";

    /** Error code returned in the 401 response body. */
    public static final String ERROR_CODE_HMAC = "HMAC_SIGNATURE_INVALID";

    /**
     * Creates a new HmacSignatureException with the given message.
     *
     * @param message the detail message
     */
    public HmacSignatureException(String message) {
        super(message);
    }
}
```

#### Step 5.2: Create HmacSigningFilter

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/HmacSigningFilter.java`

```java
/**
 * Servlet filter that verifies HMAC-SHA256 signatures on incoming requests.
 *
 * <p>This filter runs AFTER {@link JwtRequestFilter} (authentication) and
 * AFTER {@link TokenBindingFilter} (if present). It only activates for
 * authenticated requests on the ElatusDev filter chain when HMAC is enabled.</p>
 *
 * <p>Verification steps:
 * <ol>
 *   <li>Extract X-Signature, X-Timestamp, X-Nonce, X-Body-Hash headers</li>
 *   <li>Validate timestamp is within tolerance (|now - timestamp| <= 300s)</li>
 *   <li>Check nonce uniqueness via {@link NonceStore}</li>
 *   <li>Verify body hash matches SHA-256 of actual body</li>
 *   <li>Rebuild string-to-sign and verify HMAC signature</li>
 *   <li>Store nonce on success</li>
 * </ol></p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(5) // After TokenBindingFilter (@Order(4))
public class HmacSigningFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HmacSigningFilter.class);

    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_BODY_HASH = "X-Body-Hash";
    public static final String ERROR_RESPONSE_TYPE = "application/json";
    public static final String ERROR_RESPONSE_TEMPLATE = "{\"error\":\"%s\",\"message\":\"%s\"}";
    public static final String REQUEST_ATTR_NONCE = "hmac.request.nonce";
    public static final String REQUEST_ATTR_CACHED_REQUEST = "hmac.cached.request";

    private final HmacSignatureService hmacSignatureService;
    private final NonceStore nonceStore;
    private final HmacKeyService hmacKeyService;
    private final HmacProperties hmacProperties;

    // Constructor injection

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        // If HMAC is disabled, skip
        if (!hmacProperties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        // Only verify authenticated requests
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            chain.doFilter(request, response);
            return;
        }

        // Extract HMAC headers
        String signature = request.getHeader(HEADER_SIGNATURE);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String bodyHash = request.getHeader(HEADER_BODY_HASH);

        // Check all required headers present
        if (signature == null || timestamp == null || nonce == null || bodyHash == null) {
            rejectRequest(response, HmacSignatureException.ERROR_CODE_HMAC,
                    String.format(HmacSignatureException.ERROR_MISSING_HEADERS,
                            buildMissingHeadersList(signature, timestamp, nonce, bodyHash)));
            return;
        }

        // Validate timestamp tolerance
        long requestTime = Long.parseLong(timestamp);
        long serverTime = System.currentTimeMillis() / 1000L;
        long delta = Math.abs(serverTime - requestTime);
        if (delta > hmacProperties.getTimestampToleranceSeconds()) {
            rejectRequest(response, HmacSignatureException.ERROR_CODE_HMAC,
                    String.format(HmacSignatureException.ERROR_TIMESTAMP_EXPIRED,
                            delta, hmacProperties.getTimestampToleranceSeconds()));
            return;
        }

        // Check nonce replay
        if (nonceStore.exists(nonce)) {
            rejectRequest(response, HmacSignatureException.ERROR_CODE_HMAC,
                    String.format(HmacSignatureException.ERROR_NONCE_REPLAY, nonce));
            return;
        }

        // Cache request body for re-reading
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        // Verify body hash
        String computedBodyHash = hmacSignatureService.computeBodyHash(cachedRequest.getCachedBody());
        if (!computedBodyHash.equals(bodyHash)) {
            rejectRequest(response, HmacSignatureException.ERROR_CODE_HMAC,
                    HmacSignatureException.ERROR_BODY_HASH_MISMATCH);
            return;
        }

        // Build string-to-sign and verify HMAC
        String stringToSign = hmacSignatureService.buildRequestStringToSign(
                cachedRequest.getMethod(),
                cachedRequest.getRequestURI(),
                timestamp, bodyHash, nonce);

        byte[] key = hmacKeyService.resolveDefaultKey();
        if (!hmacSignatureService.verifySignature(key, stringToSign, signature)) {
            rejectRequest(response, HmacSignatureException.ERROR_CODE_HMAC,
                    String.format(HmacSignatureException.ERROR_SIGNATURE_INVALID, "HMAC mismatch"));
            return;
        }

        // Signature valid — store nonce to prevent replay
        nonceStore.store(nonce, hmacProperties.getTimestampToleranceSeconds());

        // Store nonce in request attribute for response filter
        cachedRequest.setAttribute(REQUEST_ATTR_NONCE, nonce);

        chain.doFilter(cachedRequest, response);
    }

    private void rejectRequest(HttpServletResponse response, String errorCode, String message)
            throws IOException {
        LOGGER.warn("HMAC verification failed: {}", message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(ERROR_RESPONSE_TYPE);
        response.getWriter().write(String.format(ERROR_RESPONSE_TEMPLATE, errorCode, message));
    }

    private String buildMissingHeadersList(String signature, String timestamp,
                                           String nonce, String bodyHash) {
        // Build comma-separated list of missing header names
    }
}
```

#### Step 5.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 5.4: Commit

```
feat(security): add HmacSigningFilter for request signature verification

Add OncePerRequestFilter that verifies HMAC-SHA256 request signatures.
Validates timestamp tolerance, nonce uniqueness, body hash integrity,
and HMAC signature correctness. Returns 401 HMAC_SIGNATURE_INVALID
on any verification failure.
```

---

### Phase 6: HmacResponseFilter

#### Step 6.1: Create HmacResponseFilter

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/HmacResponseFilter.java`

```java
/**
 * Servlet filter that signs outgoing HTTP responses with HMAC-SHA256.
 *
 * <p>This filter wraps the response in a {@link CachedBodyHttpServletResponse}
 * to capture the response body, then computes an HMAC-SHA256 signature over
 * the status code, body hash, timestamp, and the original request nonce.</p>
 *
 * <p>Runs AFTER business logic. Must be registered BEFORE the {@link HmacSigningFilter}
 * in the filter chain so that it wraps the response on the way out.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(6) // After HmacSigningFilter (@Order(5))
public class HmacResponseFilter extends OncePerRequestFilter {

    public static final String HEADER_RESPONSE_SIGNATURE = "X-Response-Signature";
    public static final String HEADER_RESPONSE_TIMESTAMP = "X-Response-Timestamp";

    private final HmacSignatureService hmacSignatureService;
    private final HmacKeyService hmacKeyService;
    private final HmacProperties hmacProperties;

    // Constructor injection

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        // If HMAC is disabled, skip
        if (!hmacProperties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        // Only sign authenticated responses
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            chain.doFilter(request, response);
            return;
        }

        // Get the request nonce (stored by HmacSigningFilter)
        String requestNonce = (String) request.getAttribute(HmacSigningFilter.REQUEST_ATTR_NONCE);
        if (requestNonce == null) {
            // No nonce means the request was not HMAC-verified (unauthenticated endpoint)
            chain.doFilter(request, response);
            return;
        }

        // Wrap response to capture body
        CachedBodyHttpServletResponse cachedResponse = new CachedBodyHttpServletResponse(response);

        // Let business logic write to the cached response
        chain.doFilter(request, cachedResponse);

        // Compute response signature
        byte[] responseBody = cachedResponse.getCachedBody();
        String bodyHash = hmacSignatureService.computeBodyHash(responseBody);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String statusCode = String.valueOf(cachedResponse.getStatus());

        String stringToSign = hmacSignatureService.buildResponseStringToSign(
                statusCode, bodyHash, timestamp, requestNonce);

        byte[] key = hmacKeyService.resolveDefaultKey();
        String signature = hmacSignatureService.computeHmac(key, stringToSign);

        // Set response headers
        response.setHeader(HEADER_RESPONSE_SIGNATURE, signature);
        response.setHeader(HEADER_RESPONSE_TIMESTAMP, timestamp);

        // Write the cached body to the actual response
        cachedResponse.writeBodyToResponse();
    }
}
```

#### Step 6.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 6.3: Commit

```
feat(security): add HmacResponseFilter for response signing

Add OncePerRequestFilter that computes HMAC-SHA256 signature
over response status code, body hash, timestamp, and request
nonce. Sets X-Response-Signature and X-Response-Timestamp headers.
```

---

### Phase 7: Configuration Properties

#### Step 7.1: Create HmacProperties

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/config/HmacProperties.java`

```java
/**
 * Configuration properties for HMAC API signing.
 *
 * <p>Bound to {@code security.elatus.hmac.*} in application properties.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "security.elatus.hmac")
public class HmacProperties {

    /** Default timestamp tolerance in seconds (5 minutes). */
    public static final long DEFAULT_TIMESTAMP_TOLERANCE_SECONDS = 300L;

    /** Whether HMAC signing is enabled. */
    private boolean enabled = true;

    /** Maximum allowed difference between request timestamp and server time, in seconds. */
    private long timestampToleranceSeconds = DEFAULT_TIMESTAMP_TOLERANCE_SECONDS;

    /** Per-application signing keys. Map of appId -> secret key. */
    private Map<String, String> keys = new HashMap<>();

    /**
     * Returns whether HMAC signing is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Sets whether HMAC signing is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Returns the timestamp tolerance in seconds.
     *
     * @return tolerance in seconds
     */
    public long getTimestampToleranceSeconds() { return timestampToleranceSeconds; }

    /**
     * Sets the timestamp tolerance in seconds.
     *
     * @param timestampToleranceSeconds tolerance in seconds
     */
    public void setTimestampToleranceSeconds(long timestampToleranceSeconds) {
        this.timestampToleranceSeconds = timestampToleranceSeconds;
    }

    /**
     * Returns the per-application signing keys.
     *
     * @return map of appId to secret key
     */
    public Map<String, String> getKeys() { return keys; }

    /**
     * Sets the per-application signing keys.
     *
     * @param keys map of appId to secret key
     */
    public void setKeys(Map<String, String> keys) { this.keys = keys; }
}
```

#### Step 7.2: Create HmacConfiguration

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/config/HmacConfiguration.java`

```java
/**
 * Spring configuration that enables HMAC signing properties.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(HmacProperties.class)
public class HmacConfiguration {
}
```

#### Step 7.3: Add application properties

**File**: `application/src/main/resources/application.properties` (or `.yml`)

```properties
# HMAC API Signing (ElatusDev only)
security.elatus.hmac.enabled=true
security.elatus.hmac.timestamp-tolerance-seconds=300
security.elatus.hmac.keys.elatusdev-web=${HMAC_SIGNING_KEY_ELATUSDEV}
```

For local/dev:
```properties
# application-local.properties
security.elatus.hmac.enabled=false
```

#### Step 7.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 7.5: Commit

```
feat(security): add HMAC signing configuration properties

Add HmacProperties bound to security.elatus.hmac.* with enabled
flag, timestamp tolerance (300s default), and per-app signing keys.
Add HmacConfiguration to enable property binding. Disable HMAC
for local/dev profiles.
```

---

### Phase 8: Integration with ElatusDev Filter Chain

#### Step 8.1: Read current SecurityConfig

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

#### Step 8.2: Register HMAC filters

Add `HmacSigningFilter` and `HmacResponseFilter` to the SecurityConfig filter chain:

```java
.addFilterAfter(hmacSigningFilter, TokenBindingFilter.class) // or after JwtRequestFilter if no TokenBindingFilter
.addFilterAfter(hmacResponseFilter, HmacSigningFilter.class)
```

If `TokenBindingFilter` is not yet registered, place after `JwtRequestFilter`:

```java
.addFilterAfter(hmacSigningFilter, JwtRequestFilter.class)
.addFilterAfter(hmacResponseFilter, HmacSigningFilter.class)
```

**Filter chain order**:
```
JwtRequestFilter (@Order 3)
  -> TokenBindingFilter (@Order 4) [if present]
  -> HmacSigningFilter (@Order 5)
  -> HmacResponseFilter (@Order 6)
  -> Business logic
```

**IMPORTANT**: The `HmacSigningFilter` skips unauthenticated endpoints (login, register) because `SecurityContextHolder.getContext().getAuthentication()` is null before JWT validation. This is by design — login/register endpoints do not require HMAC signing.

#### Step 8.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 8.4: Commit

```
feat(security): integrate HMAC filters with ElatusDev filter chain

Register HmacSigningFilter and HmacResponseFilter in SecurityConfig
after JwtRequestFilter and TokenBindingFilter. HMAC verification
applies only to authenticated ElatusDev endpoints.
```

---

### Phase 9: Unit Tests

All tests follow project conventions: Given-When-Then, `shouldDoX_whenY()`, `@DisplayName`, `@Nested`, zero `any()` matchers, `public static final` constants.

#### Step 9.1: HmacSignatureServiceTest

**File**: `security/src/test/java/com/akademiaplus/hmac/usecases/HmacSignatureServiceTest.java`

- No mocks needed — `HmacSignatureService` is pure computation
- Constants: known key, known data, expected HMAC values (pre-computed)

| @Nested | Tests |
|---------|-------|
| `BodyHash` | `shouldReturnKnownSha256_whenHashingKnownBody`, `shouldReturnEmptyBodyHash_whenBodyIsEmpty` |
| `StringToSign` | `shouldBuildRequestStringToSign_whenAllComponentsProvided`, `shouldBuildResponseStringToSign_whenAllComponentsProvided` |
| `HmacComputation` | `shouldReturnKnownHmac_whenComputingWithKnownKeyAndData`, `shouldReturnDifferentHmac_whenKeyChanges`, `shouldReturnDifferentHmac_whenDataChanges` |
| `SignatureVerification` | `shouldReturnTrue_whenSignatureMatchesExpected`, `shouldReturnFalse_whenSignatureMismatches` |

#### Step 9.2: HmacSigningFilterTest

**File**: `security/src/test/java/com/akademiaplus/hmac/interfaceadapters/HmacSigningFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- Mocks: `HmacSignatureService`, `NonceStore`, `HmacKeyService`, `HmacProperties`
- `MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain`

| @Nested | Tests |
|---------|-------|
| `Disabled` | `shouldContinueChain_whenHmacDisabled` |
| `Unauthenticated` | `shouldContinueChain_whenNoAuthentication` |
| `MissingHeaders` | `shouldReturn401_whenSignatureHeaderMissing`, `shouldReturn401_whenTimestampHeaderMissing`, `shouldReturn401_whenNonceHeaderMissing`, `shouldReturn401_whenBodyHashHeaderMissing` |
| `TimestampValidation` | `shouldReturn401_whenTimestampOutsideTolerance`, `shouldContinueChain_whenTimestampWithinTolerance` |
| `NonceReplay` | `shouldReturn401_whenNonceAlreadyUsed`, `shouldStoreNonce_whenVerificationSucceeds` |
| `BodyHashVerification` | `shouldReturn401_whenBodyHashDoesNotMatchActualBody` |
| `SignatureVerification` | `shouldContinueChain_whenSignatureIsValid`, `shouldReturn401_whenSignatureIsInvalid` |

#### Step 9.3: HmacResponseFilterTest

**File**: `security/src/test/java/com/akademiaplus/hmac/interfaceadapters/HmacResponseFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- Mocks: `HmacSignatureService`, `HmacKeyService`, `HmacProperties`

| @Nested | Tests |
|---------|-------|
| `Disabled` | `shouldContinueChain_whenHmacDisabled` |
| `Unauthenticated` | `shouldContinueChain_whenNoAuthentication` |
| `NoNonce` | `shouldContinueChain_whenNoRequestNonceAttribute` |
| `ResponseSigning` | `shouldSetResponseSignatureHeader_whenResponseIsComplete`, `shouldSetResponseTimestampHeader_whenResponseIsComplete`, `shouldWriteResponseBody_whenSigningIsComplete` |

#### Step 9.4: HmacKeyServiceTest

**File**: `security/src/test/java/com/akademiaplus/hmac/usecases/HmacKeyServiceTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock HmacProperties`

| @Nested | Tests |
|---------|-------|
| `KeyResolution` | `shouldReturnKey_whenAppIdConfigured`, `shouldReturnDefaultKey_whenAppIdNotFound`, `shouldThrowException_whenNoKeyConfigured` |

#### Step 9.5: InMemoryNonceStoreTest

**File**: `security/src/test/java/com/akademiaplus/hmac/interfaceadapters/InMemoryNonceStoreTest.java`

- No mocks needed

| @Nested | Tests |
|---------|-------|
| `NonceTracking` | `shouldReturnFalse_whenNonceNotStored`, `shouldReturnTrue_whenNonceAlreadyStored`, `shouldReturnFalse_whenNonceExpired` |

#### Step 9.6: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 9.7: Commit

```
test(security): add HMAC signing unit tests

HmacSignatureServiceTest — covers body hashing, string-to-sign
building, HMAC computation, and signature verification.
HmacSigningFilterTest — covers disabled, unauthenticated, missing
headers, timestamp, nonce replay, body hash, and signature paths.
HmacResponseFilterTest — covers disabled, unauthenticated, no
nonce, and response signing. HmacKeyServiceTest — covers key
resolution and missing key errors. InMemoryNonceStoreTest —
covers nonce tracking and expiry.
```

---

### Phase 10: Component Tests

#### Step 10.1: Read existing component test infrastructure

```bash
find application/src/test -name "AbstractIntegrationTest.java" | head -1
cat <result>
find application/src/test -name "*ComponentTest.java" | head -5
cat <first-result>
```

#### Step 10.2: HmacSigningComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/HmacSigningComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`
- `@TestPropertySource(properties = {"security.elatus.hmac.enabled=true", "security.elatus.hmac.keys.elatusdev-web=test-hmac-key-32-chars-minimum!"})`
- `@Autowired MockMvc mockMvc`
- Helper method: `signRequest(MockHttpServletRequestBuilder, String body, String key)` — computes all HMAC headers and adds them to the request builder

**Setup**:
1. Create a test user and login to get a valid JWT
2. Use the JWT for authenticated requests
3. Compute HMAC signatures for each request using the test key

| @Nested | Tests |
|---------|-------|
| `ValidSignature` | `shouldReturn200_whenRequestSignatureIsValid`, `shouldIncludeResponseSignatureHeaders_whenRequestIsValid` |
| `InvalidSignature` | `shouldReturn401_whenSignatureIsWrong`, `shouldReturn401_whenTimestampIsExpired`, `shouldReturn401_whenNonceIsReused`, `shouldReturn401_whenBodyHashDoesNotMatch`, `shouldReturn401_whenHeadersMissing` |
| `ResponseVerification` | `shouldReturnValidResponseSignature_whenResponseIsSigned` |
| `UnauthenticatedEndpoints` | `shouldNotRequireHmac_whenEndpointIsUnauthenticated` |

**Response signature verification test**:
1. Send a validly-signed authenticated request
2. Extract `X-Response-Signature` and `X-Response-Timestamp` from response
3. Rebuild the response string-to-sign using the response body, status code, timestamp, and original request nonce
4. Verify the HMAC matches

#### Step 10.3: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

#### Step 10.4: Commit

```
test(application): add HMAC signing component test

HmacSigningComponentTest — full Spring context + Testcontainers
MariaDB. Covers valid/invalid signatures, timestamp expiry, nonce
replay, body hash mismatch, response signing verification, and
unauthenticated endpoint bypass.
```

---

## 6. Key Design Decisions

### 6.1 Why HMAC-SHA256 (not RSA or ECDSA)?

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| HMAC-SHA256 | Fast (symmetric), simple key management, suitable for server-to-known-client | Shared secret — key must be distributed securely | **Selected** |
| RSA/ECDSA | Asymmetric — no shared secret | Slower, more complex key management, overkill for API signing | Rejected |
| JWT-signed body | Leverages existing JWT infra | Adds significant overhead per request, not standard for request signing | Rejected |

HMAC-SHA256 is the industry standard for API request signing (AWS Signature V4, Stripe Webhooks, GitHub Webhooks).

### 6.2 Why sign responses?

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| Request signing only | Simpler, fewer filters | Client cannot verify response integrity | Rejected |
| Request + response signing | Full integrity — client can detect response tampering | Additional filter, response body capture overhead | **Selected** |
| Mutual TLS | Strongest transport security | Complex certificate management, client-side config | Future consideration |

Response signing prevents a compromised intermediary from modifying API responses (e.g., changing a payment amount in the response body).

### 6.3 Why Redis for nonce storage?

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| Redis (primary) | Fast, TTL-based auto-expiry, shared across instances | Infrastructure dependency | **Selected** |
| In-memory ConcurrentHashMap (fallback) | No infrastructure dependency, simple | Not shared across instances, memory growth | Fallback for tests/local |
| Database | Durable, queryable | Slower writes, needs cleanup job | Rejected |

Redis is ideal because nonces are short-lived (5-minute TTL) and must be shared across all API server instances to prevent cross-instance replay.

### 6.4 Why 5-minute timestamp tolerance?

| Tolerance | Risk | Decision |
|-----------|------|----------|
| 30 seconds | Clock skew between client and server causes false rejections | Too strict |
| 5 minutes | Good balance — allows moderate clock drift while limiting replay window | **Selected** |
| 15 minutes | Large replay window — attacker has more time to capture and replay | Too permissive |

5 minutes (300 seconds) is the standard tolerance used by AWS Signature V4.

### 6.5 Why `CachedBodyHttpServletRequest` (not Spring's `ContentCachingRequestWrapper`)?

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| Custom `CachedBodyHttpServletRequest` | Reads body on construction, always available | Additional class to maintain | **Selected** |
| Spring `ContentCachingRequestWrapper` | Built-in, less code | Body only available AFTER filter chain completes (too late for verification) | Rejected |

Spring's `ContentCachingRequestWrapper` caches the body lazily on first read, which may not happen before the HMAC filter needs it. The custom wrapper reads eagerly on construction.

---

## 7. Multi-Tenancy Considerations

- HMAC signing is tenant-agnostic. The signing key is per-application (e.g., `elatusdev-web`), not per-tenant. All tenants using the same application share the same signing key.
- The tenant ID is carried in the JWT (validated by `JwtRequestFilter`) and is not part of the HMAC string-to-sign. This keeps the signing protocol simple and avoids coupling HMAC verification to tenant resolution.
- Future iteration: per-tenant signing keys could be introduced for SaaS scenarios where each tenant has its own application credentials. This would require extending `HmacKeyService` to resolve keys by `(appId, tenantId)`.

---

## 8. Future Extensibility

1. **Per-tenant signing keys**: Extend `HmacKeyService` to resolve keys by `(appId, tenantId)` for multi-tenant SaaS.
2. **Key rotation**: Support multiple active keys per app with a `key-id` header, allowing zero-downtime key rotation.
3. **Request signing for webhooks**: Reuse `HmacSignatureService` to sign outgoing webhook payloads.
4. **Signing algorithm negotiation**: Allow clients to specify preferred algorithm (HMAC-SHA256, HMAC-SHA512) via a header.
5. **Audit trail**: Persist verified request signatures in an audit log for compliance.
6. **Rate limiting by nonce**: Extend `NonceStore` to track nonce frequency per client for anomaly detection.
7. **Selective endpoint signing**: Configure which endpoints require HMAC via path patterns (e.g., only `/v1/payments/**` requires signing).

---

## 9. Verification Checklist

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — all HMAC unit tests green
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests pass
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] `shouldDoX_whenY` naming with `@DisplayName`, `@Nested`
- [ ] Domain records in `usecases/domain/` (Hard Rule #13)
- [ ] `Long` not `Integer` for all numeric identifiers
- [ ] Conventional Commits, no AI attribution
- [ ] HMAC only applies to ElatusDev authenticated endpoints (not AkademiaPlus)
- [ ] Unauthenticated endpoints (login, register) bypass HMAC
- [ ] Nonce replay protection works (second request with same nonce returns 401)
- [ ] Timestamp tolerance is configurable and defaults to 300 seconds
- [ ] Body hash is verified (tampered body returns 401)
- [ ] Response signature headers are set on authenticated responses
- [ ] `CachedBodyHttpServletRequest` allows body re-reading
- [ ] `CachedBodyHttpServletResponse` captures body for signing
- [ ] InMemoryNonceStore used when Redis is unavailable (tests)
- [ ] Constant-time signature comparison (`MessageDigest.isEqual`) prevents timing attacks
- [ ] Empty request body produces hash of empty string (not null)

---

## 10. Critical Reminders

1. **ElatusDev only**: HMAC signing MUST NOT apply to AkademiaPlus (school, IP-whitelisted) requests. The filters check `hmacProperties.isEnabled()` and `SecurityContextHolder.getContext().getAuthentication()`.
2. **Login/register bypass**: Unauthenticated endpoints (login, register, OAuth) are NOT HMAC-signed. The filter checks for null authentication context.
3. **Constant-time comparison**: ALWAYS use `MessageDigest.isEqual()` for HMAC comparison. Never use `String.equals()` — it is vulnerable to timing attacks.
4. **Nonce uniqueness**: Each request MUST have a unique nonce (UUID). The `NonceStore` prevents reuse within the 5-minute window.
5. **Body hash for empty bodies**: GET requests with no body MUST still include `X-Body-Hash` — the hash of an empty string (`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`).
6. **Filter ordering**: `HmacSigningFilter` MUST run AFTER `JwtRequestFilter` (needs authentication context) and BEFORE business logic. `HmacResponseFilter` runs AFTER `HmacSigningFilter` to capture the response.
7. **Key security**: HMAC signing keys MUST be loaded from environment variables or secrets manager — never hardcoded in source. The `HmacProperties.keys` map uses `${HMAC_SIGNING_KEY_ELATUSDEV}` placeholder.
8. **Redis dependency**: Production requires Redis for nonce storage. Tests and local dev use `InMemoryNonceStore` fallback (`@ConditionalOnMissingBean`).
9. **Response signing uses request nonce**: The response signature includes the original request's nonce, binding the response to the specific request. This prevents response replay/substitution.
10. **No `any()` matchers**: All mock stubbing in tests uses exact values or `ArgumentCaptor`.
