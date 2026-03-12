# HMAC-SHA256 API Signing — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/hmac-api-signing-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 -> 2 -> ... -> 10). Do NOT skip ahead.
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

## Phase 1: CachedBody Wrappers

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

### Step 1.1: Create directory structure

```bash
mkdir -p security/src/main/java/com/akademiaplus/hmac/interfaceadapters/config
mkdir -p security/src/main/java/com/akademiaplus/hmac/usecases/domain
mkdir -p security/src/main/java/com/akademiaplus/hmac/exceptions
```

### Step 1.2: CachedBodyHttpServletRequest

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/CachedBodyHttpServletRequest.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * HttpServletRequest wrapper that caches the request body in a byte array,
 * allowing it to be read multiple times.
 *
 * <p>The default {@link HttpServletRequest#getInputStream()} can only be read once.
 * This wrapper reads the entire body on construction and returns a fresh
 * {@link ByteArrayInputStream} on each subsequent {@link #getInputStream()} call.</p>
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

    /** {@inheritDoc} */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("setReadListener not supported");
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    /** {@inheritDoc} */
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
}
```

### Step 1.3: CachedBodyHttpServletResponse

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/CachedBodyHttpServletResponse.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

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

    /** {@inheritDoc} */
    @Override
    public ServletOutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    throw new UnsupportedOperationException("setWriteListener not supported");
                }

                @Override
                public void write(int b) {
                    cachedOutputStream.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    cachedOutputStream.write(b, off, len);
                }
            };
        }
        return outputStream;
    }

    /** {@inheritDoc} */
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
}
```

### Step 1.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 1.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/hmac/
git commit -m "feat(security): add CachedBody request and response wrappers

Add CachedBodyHttpServletRequest (re-readable request body) and
CachedBodyHttpServletResponse (capturable response body) for
HMAC signature computation. Both extend standard servlet wrappers."
```

---

## Phase 2: HmacSignatureService

### Read first

```bash
grep -rn "class HashingService" utilities/src/main/java/
cat <result>
```

Understand the existing `HashingService` to see if SHA-256 hashing can be reused. The `HmacSignatureService` may delegate body hashing to `HashingService` or implement its own (since it needs byte[] input, not String).

### Step 2.1: Create HmacSignatureResult record

**File**: `security/src/main/java/com/akademiaplus/hmac/usecases/domain/HmacSignatureResult.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases.domain;

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
     * @return a new HmacSignatureResult with valid=true
     */
    public static HmacSignatureResult computed(String signature) {
        return new HmacSignatureResult(signature, true);
    }
}
```

### Step 2.2: Create HmacSignatureService

**File**: `security/src/main/java/com/akademiaplus/hmac/usecases/HmacSignatureService.java`

- `@Service`
- No constructor dependencies (pure computation)
- Constants: `HMAC_ALGORITHM = "HmacSHA256"`, `HASH_ALGORITHM = "SHA-256"`, `STRING_TO_SIGN_SEPARATOR = "\n"`, `EMPTY_BODY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"`

**Methods**:

- `computeBodyHash(byte[] body)`: `MessageDigest.getInstance("SHA-256").digest(body)` -> hex string. Use `HexFormat.of().formatHex()` (Java 17+) for hex encoding.
- `buildRequestStringToSign(String method, String path, String timestamp, String bodyHash, String nonce)`: Concatenate with `"\n"` separator.
- `buildResponseStringToSign(String statusCode, String bodyHash, String timestamp, String requestNonce)`: Concatenate with `"\n"` separator.
- `computeHmac(byte[] key, String stringToSign)`: `Mac.getInstance("HmacSHA256")`, init with `SecretKeySpec(key, "HmacSHA256")`, `doFinal(stringToSign.getBytes(UTF_8))` -> hex string.
- `verifySignature(byte[] key, String stringToSign, String providedSignature)`: Compute expected, compare with `MessageDigest.isEqual()`. **CRITICAL**: use constant-time comparison.

### Step 2.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 2.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/hmac/
git commit -m "feat(security): add HmacSignatureService for HMAC-SHA256 computation

Add HmacSignatureService with computeBodyHash (SHA-256),
computeHmac (HMAC-SHA256), verifySignature (constant-time),
and string-to-sign builders for request and response signing.
Add HmacSignatureResult domain record."
```

---

## Phase 3: NonceStore

### Read first

Check if Redis is available in the project:

```bash
grep -rn "spring-data-redis\|RedisTemplate\|StringRedisTemplate" security/pom.xml application/pom.xml
grep -rn "spring.redis\|spring.data.redis" application/src/main/resources/
```

### Step 3.1: Create NonceStore interface

**File**: `security/src/main/java/com/akademiaplus/hmac/usecases/NonceStore.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases;

/**
 * Stores and checks nonces to prevent replay attacks.
 *
 * <p>Implementations must ensure that a nonce can only be used once within
 * the configured TTL window (default 5 minutes). After TTL expiry,
 * the nonce may be reused (timestamp validation provides the primary
 * replay window; nonce provides uniqueness within that window).</p>
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

### Step 3.2: Create AkademiaPlusRedisNonceStore

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/AkademiaPlusRedisNonceStore.java`

- `@Service`, `@ConditionalOnBean(StringRedisTemplate.class)`
- Constructor-injected `StringRedisTemplate`
- Constants: `NONCE_KEY_PREFIX = "hmac:nonce:"`, `NONCE_VALUE = "1"`
- `exists(String nonce)`: `redisTemplate.hasKey(NONCE_KEY_PREFIX + nonce)`
- `store(String nonce, long ttlSeconds)`: `redisTemplate.opsForValue().set(NONCE_KEY_PREFIX + nonce, NONCE_VALUE, ttlSeconds, TimeUnit.SECONDS)`

**IMPORTANT**: If `StringRedisTemplate` is not in the classpath, this bean will not be created and `InMemoryNonceStore` will activate via `@ConditionalOnMissingBean`.

### Step 3.3: Create InMemoryNonceStore

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/InMemoryNonceStore.java`

- `@Service`, `@ConditionalOnMissingBean(AkademiaPlusRedisNonceStore.class)`
- Uses `ConcurrentHashMap<String, Long>` — key is nonce, value is expiry timestamp (epoch millis)
- `exists(String nonce)`: Check map, return false if null or expired (remove expired entry)
- `store(String nonce, long ttlSeconds)`: Put `nonce -> System.currentTimeMillis() + (ttlSeconds * 1000L)`
- `@Scheduled(fixedRate = 60000) cleanExpiredNonces()`: Remove entries where `now > expiry`

**IMPORTANT**: If `@Scheduled` is not enabled, add `@EnableScheduling` to `HmacConfiguration` (or verify it's already enabled elsewhere).

### Step 3.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/hmac/
git commit -m "feat(security): add NonceStore interface with Redis and in-memory implementations

Add NonceStore interface for replay attack prevention. Add
AkademiaPlusRedisNonceStore (production, TTL-based) and InMemoryNonceStore
(testing/local, ConcurrentHashMap with scheduled cleanup)."
```

---

## Phase 4: HmacKeyService

### Read first

```bash
cat application/src/main/resources/application.properties
```

Check how other secrets are configured (environment variable pattern).

### Step 4.1: Create HmacKeyService

**File**: `security/src/main/java/com/akademiaplus/hmac/usecases/HmacKeyService.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases;

import com.akademiaplus.hmac.exceptions.HmacSignatureException;
import com.akademiaplus.hmac.interfaceadapters.config.HmacProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Manages per-application HMAC signing keys.
 *
 * <p>Keys are loaded from configuration properties. Each client application
 * (e.g., elatusdev-web) has a unique signing key identified by its app ID.</p>
 *
 * <p>For the initial implementation, a single shared key is used for all
 * ElatusDev clients. Per-app keys can be added in a future iteration.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class HmacKeyService {

    /** Default application identifier for ElatusDev web app. */
    public static final String DEFAULT_APP_ID = "elatusdev-web";

    private final HmacProperties hmacProperties;

    /**
     * Creates a new HmacKeyService.
     *
     * @param hmacProperties the HMAC configuration properties
     */
    public HmacKeyService(HmacProperties hmacProperties) {
        this.hmacProperties = hmacProperties;
    }

    /**
     * Resolves the signing key for the given application.
     *
     * <p>Falls back to the default app key if no key is configured for the
     * specific app ID.</p>
     *
     * @param appId the application identifier
     * @return the signing key as bytes
     * @throws HmacSignatureException if no key is configured
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
     * @throws HmacSignatureException if no default key is configured
     */
    public byte[] resolveDefaultKey() {
        return resolveKey(DEFAULT_APP_ID);
    }
}
```

### Step 4.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 4.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/hmac/usecases/HmacKeyService.java
git commit -m "feat(security): add HmacKeyService for signing key management

Add HmacKeyService that resolves per-application HMAC signing
keys from configuration properties. Initial implementation uses
a single shared key for elatusdev-web."
```

---

## Phase 5: HmacSigningFilter + HmacSignatureException

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
```

Note the filter pattern: `@Component`, `@Order`, `extends OncePerRequestFilter`, `doFilterInternal()`.

### Step 5.1: Create HmacSignatureException

**File**: `security/src/main/java/com/akademiaplus/hmac/exceptions/HmacSignatureException.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.exceptions;

/**
 * Thrown when HMAC signature verification fails.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class HmacSignatureException extends RuntimeException {

    /** Error message template for invalid HMAC signature. */
    public static final String ERROR_SIGNATURE_INVALID =
            "HMAC signature verification failed: %s";

    /** Error message template for missing required HMAC headers. */
    public static final String ERROR_MISSING_HEADERS =
            "Missing required HMAC headers: %s";

    /** Error message template for timestamp out of tolerance. */
    public static final String ERROR_TIMESTAMP_EXPIRED =
            "Request timestamp outside tolerance window: delta=%d seconds, max=%d seconds";

    /** Error message template for replay attack (nonce reuse). */
    public static final String ERROR_NONCE_REPLAY =
            "Nonce has already been used (replay attack detected): %s";

    /** Error message for body hash mismatch. */
    public static final String ERROR_BODY_HASH_MISMATCH =
            "Request body hash does not match X-Body-Hash header";

    /** Error message template for no signing key configured. */
    public static final String ERROR_NO_KEY_CONFIGURED =
            "No HMAC signing key configured for app: %s";

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

### Step 5.2: Create HmacSigningFilter

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/HmacSigningFilter.java`

- `@Component`, `@Order(5)` (after `TokenBindingFilter @Order(4)`, or after `JwtRequestFilter @Order(3)` if token binding not present)
- Extends `OncePerRequestFilter`
- Constructor-injected: `HmacSignatureService`, `NonceStore`, `HmacKeyService`, `HmacProperties`
- Constants: `HEADER_SIGNATURE = "X-Signature"`, `HEADER_TIMESTAMP = "X-Timestamp"`, `HEADER_NONCE = "X-Nonce"`, `HEADER_BODY_HASH = "X-Body-Hash"`, `ERROR_RESPONSE_TYPE = "application/json"`, `ERROR_RESPONSE_TEMPLATE = "{\"error\":\"%s\",\"message\":\"%s\"}"`, `REQUEST_ATTR_NONCE = "hmac.request.nonce"`

**`doFilterInternal()` logic** — implement step by step:

1. `if (!hmacProperties.isEnabled())` -> `chain.doFilter()`, return
2. `if (SecurityContextHolder.getContext().getAuthentication() == null)` -> `chain.doFilter()`, return
3. Extract headers: `X-Signature`, `X-Timestamp`, `X-Nonce`, `X-Body-Hash`
4. If any header is null -> `rejectRequest(response, ERROR_CODE_HMAC, ERROR_MISSING_HEADERS.formatted(...))`
5. Parse timestamp: `Long.parseLong(timestamp)`. Catch `NumberFormatException` -> reject
6. Compute delta: `Math.abs(System.currentTimeMillis() / 1000L - requestTime)`. If `> hmacProperties.getTimestampToleranceSeconds()` -> reject with `ERROR_TIMESTAMP_EXPIRED`
7. If `nonceStore.exists(nonce)` -> reject with `ERROR_NONCE_REPLAY`
8. Wrap request: `CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request)`
9. Compute body hash: `hmacSignatureService.computeBodyHash(cachedRequest.getCachedBody())`. If not equal to `bodyHash` header -> reject with `ERROR_BODY_HASH_MISMATCH`
10. Build string-to-sign: `hmacSignatureService.buildRequestStringToSign(method, path, timestamp, bodyHash, nonce)`
11. Resolve key: `hmacKeyService.resolveDefaultKey()`
12. Verify: `hmacSignatureService.verifySignature(key, stringToSign, signature)`. If false -> reject with `ERROR_SIGNATURE_INVALID`
13. Store nonce: `nonceStore.store(nonce, hmacProperties.getTimestampToleranceSeconds())`
14. Set request attribute: `cachedRequest.setAttribute(REQUEST_ATTR_NONCE, nonce)`
15. `chain.doFilter(cachedRequest, response)`

**`rejectRequest(HttpServletResponse, String, String)`**: Set status 401, content type JSON, write formatted error body, log at WARN.

**`buildMissingHeadersList(String, String, String, String)`**: Build comma-separated list of null header names for error message.

### Step 5.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 5.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/hmac/
git commit -m "feat(security): add HmacSigningFilter for request signature verification

Add OncePerRequestFilter that verifies HMAC-SHA256 request signatures.
Validates timestamp tolerance, nonce uniqueness, body hash integrity,
and HMAC signature correctness. Returns 401 HMAC_SIGNATURE_INVALID
on any verification failure. Add HmacSignatureException."
```

---

## Phase 6: HmacResponseFilter

### Step 6.1: Create HmacResponseFilter

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/HmacResponseFilter.java`

- `@Component`, `@Order(6)` (after `HmacSigningFilter @Order(5)`)
- Extends `OncePerRequestFilter`
- Constructor-injected: `HmacSignatureService`, `HmacKeyService`, `HmacProperties`
- Constants: `HEADER_RESPONSE_SIGNATURE = "X-Response-Signature"`, `HEADER_RESPONSE_TIMESTAMP = "X-Response-Timestamp"`

**`doFilterInternal()` logic**:

1. `if (!hmacProperties.isEnabled())` -> `chain.doFilter()`, return
2. `if (SecurityContextHolder.getContext().getAuthentication() == null)` -> `chain.doFilter()`, return
3. Get request nonce: `request.getAttribute(HmacSigningFilter.REQUEST_ATTR_NONCE)`. If null -> `chain.doFilter()`, return (not HMAC-verified)
4. Wrap response: `CachedBodyHttpServletResponse cachedResponse = new CachedBodyHttpServletResponse(response)`
5. `chain.doFilter(request, cachedResponse)` — business logic writes to cached response
6. Get cached body: `cachedResponse.getCachedBody()`
7. Compute body hash: `hmacSignatureService.computeBodyHash(responseBody)`
8. Get timestamp: `String.valueOf(System.currentTimeMillis() / 1000L)`
9. Build string-to-sign: `hmacSignatureService.buildResponseStringToSign(statusCode, bodyHash, timestamp, requestNonce)`
10. Resolve key: `hmacKeyService.resolveDefaultKey()`
11. Compute signature: `hmacSignatureService.computeHmac(key, stringToSign)`
12. Set response headers: `response.setHeader(HEADER_RESPONSE_SIGNATURE, signature)`, `response.setHeader(HEADER_RESPONSE_TIMESTAMP, timestamp)`
13. `cachedResponse.writeBodyToResponse()`

**IMPORTANT**: Set response headers on the ORIGINAL `response` (not `cachedResponse`), then call `writeBodyToResponse()` which writes to the original output stream.

### Step 6.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 6.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/hmac/interfaceadapters/HmacResponseFilter.java
git commit -m "feat(security): add HmacResponseFilter for response signing

Add OncePerRequestFilter that computes HMAC-SHA256 signature
over response status code, body hash, timestamp, and request
nonce. Sets X-Response-Signature and X-Response-Timestamp headers."
```

---

## Phase 7: Configuration Properties

### Read first

```bash
cat application/src/main/resources/application.properties
# Check for existing @ConfigurationProperties patterns
grep -rn "@ConfigurationProperties" security/src/main/java/ application/src/main/java/
# Check for @EnableScheduling
grep -rn "@EnableScheduling" security/src/main/java/ application/src/main/java/
```

### Step 7.1: Create HmacProperties

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/config/HmacProperties.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

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

    /** Per-application signing keys. Map of appId to secret key string. */
    private Map<String, String> keys = new HashMap<>();

    /**
     * Returns whether HMAC signing is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether HMAC signing is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the timestamp tolerance in seconds.
     *
     * @return tolerance in seconds
     */
    public long getTimestampToleranceSeconds() {
        return timestampToleranceSeconds;
    }

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
    public Map<String, String> getKeys() {
        return keys;
    }

    /**
     * Sets the per-application signing keys.
     *
     * @param keys map of appId to secret key
     */
    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }
}
```

### Step 7.2: Create HmacConfiguration

**File**: `security/src/main/java/com/akademiaplus/hmac/interfaceadapters/config/HmacConfiguration.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring configuration that enables HMAC signing properties and scheduling.
 *
 * <p>{@code @EnableScheduling} is required for {@code InMemoryNonceStore}'s
 * periodic cleanup of expired nonces.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(HmacProperties.class)
@EnableScheduling
public class HmacConfiguration {
}
```

**IMPORTANT**: Check if `@EnableScheduling` is already present elsewhere. If so, do not duplicate it.

### Step 7.3: Add application properties

**File**: `application/src/main/resources/application.properties` (or `.yml`)

Add:
```properties
# HMAC API Signing (ElatusDev only)
security.elatus.hmac.enabled=true
security.elatus.hmac.timestamp-tolerance-seconds=300
security.elatus.hmac.keys.elatusdev-web=${HMAC_SIGNING_KEY_ELATUSDEV}
```

For local/dev profiles, add to `application-local.properties`:
```properties
security.elatus.hmac.enabled=false
```

### Step 7.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 7.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/hmac/interfaceadapters/config/
git add application/src/main/resources/
git commit -m "feat(security): add HMAC signing configuration properties

Add HmacProperties bound to security.elatus.hmac.* with enabled
flag, timestamp tolerance (300s default), and per-app signing keys.
Add HmacConfiguration with @EnableScheduling for nonce cleanup.
Disable HMAC for local/dev profiles."
```

---

## Phase 8: Integration with ElatusDev Filter Chain

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

### Step 8.1: Register HMAC filters in SecurityConfig

Add `HmacSigningFilter` and `HmacResponseFilter` as constructor parameters to the `securityFilterChain()` method. Register them after `JwtRequestFilter`:

```java
.addFilterAfter(hmacSigningFilter, JwtRequestFilter.class)
.addFilterAfter(hmacResponseFilter, HmacSigningFilter.class)
```

If `TokenBindingFilter` is already registered, place HMAC filters after it:

```java
.addFilterAfter(hmacSigningFilter, TokenBindingFilter.class)
.addFilterAfter(hmacResponseFilter, HmacSigningFilter.class)
```

The `@Order` annotations on the filter classes ensure correct ordering, but explicit `addFilterAfter()` makes the chain explicit in `SecurityConfig`.

### Step 8.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 8.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/SecurityConfig.java
git commit -m "feat(security): integrate HMAC filters with ElatusDev filter chain

Register HmacSigningFilter and HmacResponseFilter in SecurityConfig
after JwtRequestFilter. HMAC verification applies only to
authenticated ElatusDev endpoints."
```

---

## Phase 9: Unit Tests

### Read first

```bash
find security/src/test -name "*Test.java" | head -5
cat <first-result>
```

### Step 9.1: Create test directories

```bash
mkdir -p security/src/test/java/com/akademiaplus/hmac/usecases
mkdir -p security/src/test/java/com/akademiaplus/hmac/interfaceadapters
```

### Step 9.2: HmacSignatureServiceTest

**File**: `security/src/test/java/com/akademiaplus/hmac/usecases/HmacSignatureServiceTest.java`

- No mocks — pure computation tests
- Pre-computed constants: use known HMAC values from an external reference (e.g., `echo -n "test" | openssl dgst -sha256 -hmac "key"`)

Constants:
```java
public static final String TEST_KEY = "test-hmac-key-for-unit-tests-32ch";
public static final String TEST_BODY = "{\"name\":\"test\"}";
public static final String TEST_METHOD = "POST";
public static final String TEST_PATH = "/v1/courses";
public static final String TEST_TIMESTAMP = "1709834567";
public static final String TEST_NONCE = "550e8400-e29b-41d4-a716-446655440000";
public static final String TEST_STATUS_CODE = "200";
```

| @Nested | Test |
|---------|------|
| `BodyHash` | `shouldReturnKnownSha256_whenHashingKnownBody` |
| | `shouldReturnEmptyBodyHash_whenBodyIsEmpty` |
| | `shouldReturnConsistentHash_whenCalledMultipleTimes` |
| `StringToSign` | `shouldBuildRequestStringToSign_whenAllComponentsProvided` |
| | `shouldBuildResponseStringToSign_whenAllComponentsProvided` |
| | `shouldSeparateComponentsWithNewline_whenBuildingStringToSign` |
| `HmacComputation` | `shouldReturnConsistentHmac_whenComputingWithSameKeyAndData` |
| | `shouldReturnDifferentHmac_whenKeyChanges` |
| | `shouldReturnDifferentHmac_whenDataChanges` |
| `SignatureVerification` | `shouldReturnTrue_whenSignatureMatchesExpected` |
| | `shouldReturnFalse_whenSignatureMismatches` |

### Step 9.3: HmacSigningFilterTest

**File**: `security/src/test/java/com/akademiaplus/hmac/interfaceadapters/HmacSigningFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- Mocks: `HmacSignatureService`, `NonceStore`, `HmacKeyService`, `HmacProperties`
- `MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain`
- Set up authentication in `SecurityContextHolder` for authenticated test cases
- `@AfterEach`: clear `SecurityContextHolder`

Constants:
```java
public static final String TEST_SIGNATURE = "valid-hmac-signature";
public static final String TEST_TIMESTAMP = "1709834567";
public static final String TEST_NONCE = "test-nonce-uuid";
public static final String TEST_BODY_HASH = "body-sha256-hash";
public static final String TEST_BODY = "{\"name\":\"test\"}";
public static final byte[] TEST_KEY_BYTES = "test-key".getBytes(StandardCharsets.UTF_8);
public static final long TEST_TOLERANCE = 300L;
```

| @Nested | Test |
|---------|------|
| `Disabled` | `shouldContinueChain_whenHmacDisabled` |
| `Unauthenticated` | `shouldContinueChain_whenNoAuthentication` |
| `MissingHeaders` | `shouldReturn401_whenSignatureHeaderMissing` |
| | `shouldReturn401_whenTimestampHeaderMissing` |
| | `shouldReturn401_whenNonceHeaderMissing` |
| | `shouldReturn401_whenBodyHashHeaderMissing` |
| `TimestampValidation` | `shouldReturn401_whenTimestampOutsideTolerance` |
| | `shouldContinueChain_whenTimestampWithinTolerance` |
| `NonceReplay` | `shouldReturn401_whenNonceAlreadyUsed` |
| | `shouldStoreNonce_whenVerificationSucceeds` |
| `BodyHashVerification` | `shouldReturn401_whenBodyHashDoesNotMatchActualBody` |
| `SignatureVerification` | `shouldContinueChain_whenSignatureIsValid` |
| | `shouldReturn401_whenSignatureIsInvalid` |
| `RequestAttribute` | `shouldSetNonceAttribute_whenVerificationSucceeds` |

**Mock stubbing pattern** (zero `any()`):
```java
// Given
when(hmacProperties.isEnabled()).thenReturn(true);
when(hmacProperties.getTimestampToleranceSeconds()).thenReturn(TEST_TOLERANCE);
when(hmacSignatureService.computeBodyHash(TEST_BODY.getBytes(StandardCharsets.UTF_8)))
        .thenReturn(TEST_BODY_HASH);
when(hmacSignatureService.buildRequestStringToSign(
        TEST_METHOD, TEST_PATH, TEST_TIMESTAMP, TEST_BODY_HASH, TEST_NONCE))
        .thenReturn("string-to-sign");
when(hmacKeyService.resolveDefaultKey()).thenReturn(TEST_KEY_BYTES);
when(hmacSignatureService.verifySignature(TEST_KEY_BYTES, "string-to-sign", TEST_SIGNATURE))
        .thenReturn(true);
when(nonceStore.exists(TEST_NONCE)).thenReturn(false);
```

### Step 9.4: HmacResponseFilterTest

**File**: `security/src/test/java/com/akademiaplus/hmac/interfaceadapters/HmacResponseFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- Mocks: `HmacSignatureService`, `HmacKeyService`, `HmacProperties`
- `@AfterEach`: clear `SecurityContextHolder`

| @Nested | Test |
|---------|------|
| `Disabled` | `shouldContinueChain_whenHmacDisabled` |
| `Unauthenticated` | `shouldContinueChain_whenNoAuthentication` |
| `NoNonce` | `shouldContinueChain_whenNoRequestNonceAttribute` |
| `ResponseSigning` | `shouldSetResponseSignatureHeader_whenResponseIsComplete` |
| | `shouldSetResponseTimestampHeader_whenResponseIsComplete` |
| | `shouldWriteResponseBody_whenSigningIsComplete` |

### Step 9.5: HmacKeyServiceTest

**File**: `security/src/test/java/com/akademiaplus/hmac/usecases/HmacKeyServiceTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock HmacProperties hmacProperties`

| @Nested | Test |
|---------|------|
| `KeyResolution` | `shouldReturnKey_whenAppIdConfigured` |
| | `shouldReturnDefaultKey_whenAppIdNotFound` |
| | `shouldThrowHmacSignatureException_whenNoKeyConfigured` |

### Step 9.6: InMemoryNonceStoreTest

**File**: `security/src/test/java/com/akademiaplus/hmac/interfaceadapters/InMemoryNonceStoreTest.java`

- No mocks needed

| @Nested | Test |
|---------|------|
| `NonceTracking` | `shouldReturnFalse_whenNonceNotStored` |
| | `shouldReturnTrue_whenNonceAlreadyStored` |
| | `shouldReturnFalse_whenNonceExpired` |
| `Cleanup` | `shouldRemoveExpiredNonces_whenCleanupRuns` |

**Nonce expiry test**: Store a nonce with TTL of 1 second, wait briefly (or manipulate the stored expiry time), then verify `exists()` returns false.

### Step 9.7: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 9.8: Commit

```bash
git add security/src/test/
git commit -m "test(security): add HMAC signing unit tests

HmacSignatureServiceTest — covers body hashing, string-to-sign
building, HMAC computation, and signature verification.
HmacSigningFilterTest — covers disabled, unauthenticated, missing
headers, timestamp, nonce replay, body hash, and signature paths.
HmacResponseFilterTest — covers disabled, unauthenticated, no
nonce, and response signing. HmacKeyServiceTest — covers key
resolution and missing key. InMemoryNonceStoreTest — covers
nonce tracking, expiry, and cleanup."
```

---

## Phase 10: Component Tests

### Read first

```bash
find application/src/test -name "AbstractIntegrationTest.java" | head -1
cat <result>
find application/src/test -name "*ComponentTest.java" | head -5
cat <first-result>
```

### Step 10.1: HmacSigningComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/HmacSigningComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`
- `@TestPropertySource(properties = {"security.elatus.hmac.enabled=true", "security.elatus.hmac.keys.elatusdev-web=component-test-hmac-key-32chars!", "security.elatus.hmac.timestamp-tolerance-seconds=300"})`
- `@Autowired MockMvc mockMvc`

**Helper method**: `signRequest(MockHttpServletRequestBuilder builder, String body, String key)`:
1. Generate nonce (UUID)
2. Get current timestamp (epoch seconds)
3. Compute body hash (SHA-256 of body bytes, or SHA-256 of empty string for GET)
4. Build string-to-sign: `method + "\n" + path + "\n" + timestamp + "\n" + bodyHash + "\n" + nonce`
5. Compute HMAC-SHA256 with key
6. Add headers: `X-Signature`, `X-Timestamp`, `X-Nonce`, `X-Body-Hash`

**Setup**:
1. Create a test user and login to get a valid JWT
2. Use JWT for all authenticated requests

| @Nested | Test |
|---------|------|
| `ValidSignature` | `shouldReturn200_whenRequestSignatureIsValid` |
| | `shouldIncludeResponseSignatureHeaders_whenRequestIsValid` |
| `InvalidSignature` | `shouldReturn401_whenSignatureIsWrong` |
| | `shouldReturn401_whenTimestampIsExpired` |
| | `shouldReturn401_whenNonceIsReused` |
| | `shouldReturn401_whenBodyHashDoesNotMatch` |
| | `shouldReturn401_whenHeadersMissing` |
| `ResponseVerification` | `shouldReturnValidResponseSignature_whenResponseIsSigned` |
| `UnauthenticatedEndpoints` | `shouldNotRequireHmac_whenEndpointIsUnauthenticated` |

**Response signature verification test**:
```java
// Given — send a valid signed request
MvcResult result = mockMvc.perform(signedGet("/v1/some-endpoint", jwt, key))
        .andExpect(status().isOk())
        .andReturn();

// Then — verify response signature
String responseSignature = result.getResponse().getHeader("X-Response-Signature");
String responseTimestamp = result.getResponse().getHeader("X-Response-Timestamp");
String responseBody = result.getResponse().getContentAsString();
String statusCode = String.valueOf(result.getResponse().getStatus());
String responseBodyHash = computeSha256(responseBody.getBytes());
String responseStringToSign = statusCode + "\n" + responseBodyHash + "\n"
        + responseTimestamp + "\n" + originalNonce;
String expectedSignature = computeHmac(key, responseStringToSign);

assertThat(responseSignature).isEqualTo(expectedSignature);
```

### Step 10.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

### Step 10.3: Commit

```bash
git add application/src/test/
git commit -m "test(application): add HMAC signing component test

HmacSigningComponentTest — full Spring context + Testcontainers
MariaDB. Covers valid/invalid signatures, timestamp expiry, nonce
replay, body hash mismatch, response signing verification, and
unauthenticated endpoint bypass."
```

---

## VERIFICATION CHECKLIST

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
- [ ] Nonce replay returns 401
- [ ] Timestamp tolerance configurable, defaults to 300 seconds
- [ ] Body hash verified (tampered body returns 401)
- [ ] Response signatures set on authenticated responses
- [ ] `CachedBodyHttpServletRequest` allows body re-reading
- [ ] `CachedBodyHttpServletResponse` captures body for signing
- [ ] InMemoryNonceStore used when Redis unavailable
- [ ] Constant-time comparison via `MessageDigest.isEqual()`
- [ ] Empty body hash = SHA-256 of empty string (not null)
- [ ] HMAC signing keys from environment variables (not hardcoded)
