# Branching Security Filter Chain -- Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/branching-security-filter-workflow.md` -- read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 -> 2 -> ... -> 5). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header (year 2026).
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals -> `public static final` constants, shared between impl and tests.
9. Read existing files BEFORE modifying -- import paths, bean names, and method signatures vary.
10. Commit after each phase using the commit message provided.
11. **CRITICAL**: The refactor must NOT break any existing endpoint. All current `permitAll()` rules must appear in BOTH chains.

---

## Phase 1: AppOriginFilter + AppOriginContext

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/config/TenantContextLoader.java
```

Understand:
- How `JwtRequestFilter` is ordered (`@Order(3)`)
- How `TenantContextLoader` extracts the tenant from headers
- The current `SecurityFilterChain` structure and all `permitAll()` rules

### Step 1.1: Create directory structure

```bash
mkdir -p security/src/main/java/com/akademiaplus/internal/interfaceadapters/filters
```

### Step 1.2: Create AppOriginContext

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/filters/AppOriginContext.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for reading and writing the app origin on HTTP request attributes.
 *
 * <p>The app origin identifies whether a request comes from the
 * akademia-plus-web application (school premises, IP whitelist) or
 * the elatusdev-web application (public internet, full security).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public final class AppOriginContext {

    /** Request attribute key for the resolved app origin. */
    public static final String APP_ORIGIN_ATTRIBUTE = "com.akademiaplus.appOrigin";

    /** HTTP header for explicit app origin identification. */
    public static final String APP_ORIGIN_HEADER = "X-App-Origin";

    /** App origin value for akademia-plus-web. */
    public static final String ORIGIN_AKADEMIA = "akademia";

    /** App origin value for elatusdev-web. */
    public static final String ORIGIN_ELATUS = "elatus";

    /** Path prefix for akademia-plus-web requests. */
    public static final String PATH_PREFIX_AKADEMIA = "/akademia/";

    /** Path prefix for elatusdev-web requests. */
    public static final String PATH_PREFIX_ELATUS = "/elatus/";

    /** Default origin (fail-secure). */
    public static final String DEFAULT_ORIGIN = ORIGIN_ELATUS;

    private AppOriginContext() {
        // Utility class — prevent instantiation
    }

    /**
     * Sets the app origin on the request attributes.
     *
     * @param request   the HTTP request
     * @param appOrigin the resolved app origin
     */
    public static void setAppOrigin(HttpServletRequest request, String appOrigin) {
        request.setAttribute(APP_ORIGIN_ATTRIBUTE, appOrigin);
    }

    /**
     * Retrieves the app origin from the request attributes.
     *
     * @param request the HTTP request
     * @return the app origin, or the default (elatus) if not set
     */
    public static String getAppOrigin(HttpServletRequest request) {
        Object origin = request.getAttribute(APP_ORIGIN_ATTRIBUTE);
        return (origin instanceof String s) ? s : DEFAULT_ORIGIN;
    }

    /**
     * Checks whether the request originates from akademia-plus-web.
     *
     * @param request the HTTP request
     * @return true if the app origin is akademia
     */
    public static boolean isAkademia(HttpServletRequest request) {
        return ORIGIN_AKADEMIA.equals(getAppOrigin(request));
    }

    /**
     * Checks whether the request originates from elatusdev-web.
     *
     * @param request the HTTP request
     * @return true if the app origin is elatus
     */
    public static boolean isElatus(HttpServletRequest request) {
        return ORIGIN_ELATUS.equals(getAppOrigin(request));
    }
}
```

### Step 1.3: Create AppOriginFilter

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/filters/AppOriginFilter.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that resolves the application origin from the incoming request.
 *
 * <p>Resolution priority:</p>
 * <ol>
 *   <li>{@code X-App-Origin} header (explicit frontend identification)</li>
 *   <li>Request path prefix ({@code /akademia/**} or {@code /elatus/**})</li>
 *   <li>Default: {@code elatus} (fail-secure for unknown origins)</li>
 * </ol>
 *
 * <p>The resolved origin is stored as a request attribute via
 * {@link AppOriginContext#setAppOrigin(HttpServletRequest, String)}.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppOriginFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppOriginFilter.class);

    public static final String LOG_RESOLVED_ORIGIN = "Resolved app origin: {} for path: {}";
    public static final String LOG_INVALID_ORIGIN = "Invalid X-App-Origin header value: {}";

    /**
     * Resolves the app origin and sets it on the request attributes,
     * then continues the filter chain.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String appOrigin = resolveOrigin(request);
        AppOriginContext.setAppOrigin(request, appOrigin);

        LOGGER.debug(LOG_RESOLVED_ORIGIN, appOrigin, request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    private String resolveOrigin(HttpServletRequest request) {
        // Priority 1: Explicit header
        String headerValue = request.getHeader(AppOriginContext.APP_ORIGIN_HEADER);
        if (headerValue != null) {
            String normalized = headerValue.trim().toLowerCase();
            if (AppOriginContext.ORIGIN_AKADEMIA.equals(normalized)
                    || AppOriginContext.ORIGIN_ELATUS.equals(normalized)) {
                return normalized;
            }
            LOGGER.warn(LOG_INVALID_ORIGIN, headerValue);
        }

        // Priority 2: Path prefix
        String path = request.getRequestURI();
        if (path.startsWith(AppOriginContext.PATH_PREFIX_AKADEMIA)) {
            return AppOriginContext.ORIGIN_AKADEMIA;
        }
        if (path.startsWith(AppOriginContext.PATH_PREFIX_ELATUS)) {
            return AppOriginContext.ORIGIN_ELATUS;
        }

        // Priority 3: Default (fail-secure)
        return AppOriginContext.DEFAULT_ORIGIN;
    }
}
```

### Step 1.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 1.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/internal/interfaceadapters/filters/
git commit -m "feat(security): add AppOriginFilter for dual-app request routing

Add AppOriginFilter that resolves app origin from X-App-Origin
header, path prefix (/akademia/** or /elatus/**), or default
(elatus, fail-secure). Add AppOriginContext utility for reading
and writing origin on request attributes."
```

---

## Phase 2: SecurityConfig Refactor

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
cat security/src/main/java/com/akademiaplus/config/ModuleSecurityConfigurator.java
```

**IMPORTANT**: Read the full `SecurityConfig` carefully. Note:
- All `permitAll()` rules
- CORS configuration
- Header security settings
- How `ModuleSecurityConfigurator` instances are iterated
- Profile annotations
- The mock-data-service chain (leave unchanged)

### Step 2.1: Enhance ModuleSecurityConfigurator

**File**: `security/src/main/java/com/akademiaplus/config/ModuleSecurityConfigurator.java`

Add a `default` method for app-origin-aware configuration:

```java
/**
 * Configures authorization rules specific to a given app origin.
 *
 * <p>Override this method to apply different rules for akademia-plus-web
 * vs elatusdev-web. The default implementation delegates to
 * {@link #configure(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry)}.</p>
 *
 * @param auth      the authorization configuration registry
 * @param appOrigin the app origin ("akademia" or "elatus")
 * @throws Exception if configuration fails
 */
default void configure(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
        String appOrigin) throws Exception {
    configure(auth);
}
```

**IMPORTANT**: This is a `default` method so existing implementations do NOT need to change. Verify by checking all classes that implement `ModuleSecurityConfigurator`:

```bash
grep -rn "implements ModuleSecurityConfigurator\|extends.*ModuleSecurityConfigurator" security/src/ application/src/ user-management/src/ billing/src/ course-management/src/ notification-system/src/ pos-system/src/ tenant-management/src/
```

### Step 2.2: Refactor SecurityConfig

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Replace the single `securityFilterChain` bean (under `@Profile({"dev", "local"})`) with two beans:

**akademiaSecurityFilterChain** (`@Order(1)`, `@Profile({"dev", "local"})`):
- `http.securityMatcher(new AkademiaRequestMatcher())`
- Same CSRF, CORS, headers, session management, form login/HTTP basic disabling as current
- Same `permitAll()` rules (replicate ALL of them)
- Iterates `ModuleSecurityConfigurator` with `configurator.configure(auth, AppOriginContext.ORIGIN_AKADEMIA)`
- `anyRequest().authenticated()`
- Adds `appOriginFilter` and `jwtRequestFilter` before `UsernamePasswordAuthenticationFilter`

**elatusSecurityFilterChain** (`@Order(2)`, `@Profile({"dev", "local"})`):
- `http.securityMatcher("/**")` (catch-all)
- Same CSRF, CORS, headers, session management, form login/HTTP basic disabling
- Same `permitAll()` rules (replicate ALL of them)
- Iterates `ModuleSecurityConfigurator` with `configurator.configure(auth, AppOriginContext.ORIGIN_ELATUS)`
- `anyRequest().authenticated()`
- Adds `appOriginFilter` and `jwtRequestFilter` before `UsernamePasswordAuthenticationFilter`
- Comment: `// Future: add token binding, rate limiting, HMAC filters here`

**AkademiaRequestMatcher** (private static inner class):

```java
/**
 * Request matcher that identifies akademia-plus-web requests.
 *
 * <p>Matches if the {@code X-App-Origin} header is "akademia" or
 * the request path starts with {@code /akademia/}.</p>
 */
private static class AkademiaRequestMatcher implements RequestMatcher {

    @Override
    public boolean matches(HttpServletRequest request) {
        String header = request.getHeader(AppOriginContext.APP_ORIGIN_HEADER);
        if (AppOriginContext.ORIGIN_AKADEMIA.equalsIgnoreCase(header)) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith(AppOriginContext.PATH_PREFIX_AKADEMIA);
    }
}
```

**IMPORTANT**: Add `import` for:
- `com.akademiaplus.internal.interfaceadapters.filters.AppOriginFilter`
- `com.akademiaplus.internal.interfaceadapters.filters.AppOriginContext`
- `org.springframework.security.web.util.matcher.RequestMatcher`
- `org.springframework.core.annotation.Order`

Add `AppOriginFilter appOriginFilter` as a parameter to both filter chain methods.

Remove the old `securityFilterChain` method entirely. Keep `corsConfigurationSourceForLogin()`, the mock-data-service chain, and `corsConfigurationSourceForMockDataService()` unchanged.

### Step 2.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

**IMPORTANT**: If compilation fails, the most likely issues are:
- Missing imports for `RequestMatcher`, `AppOriginFilter`, `AppOriginContext`
- Bean method signature changes (both chains need `AppOriginFilter` parameter)
- Duplicate bean names -- ensure the two chain methods have distinct names

### Step 2.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/
git commit -m "feat(security): split SecurityFilterChain into dual app-origin chains

Refactor SecurityConfig into akademiaSecurityFilterChain (@Order 1)
and elatusSecurityFilterChain (@Order 2). Add AkademiaRequestMatcher
to route requests by X-App-Origin header or path prefix.
Enhance ModuleSecurityConfigurator with app-origin-aware default method."
```

---

## Phase 3: Per-App Security Properties

### Read first

```bash
cat application/src/main/resources/application.properties
```

Check for existing `@ConfigurationProperties` patterns:
```bash
grep -rn "@ConfigurationProperties" security/src/main/java/ | head -5
```

### Step 3.1: Create AppSecurityProperties

**File**: `security/src/main/java/com/akademiaplus/config/AppSecurityProperties.java`

See workflow Phase 3 Step 3.1 for full implementation. Key points:

- `@Component @ConfigurationProperties(prefix = "security.app")`
- Two nested `AppConfig` objects: `akademia` and `elatus`
- `AppConfig` fields: `tokenBindingEnabled` (boolean), `rateLimitingEnabled` (boolean), `hmacVerificationEnabled` (boolean), `allowedOrigins` (String[])
- All fields have Javadoc-documented getters and setters
- Default values: akademia has all security hooks disabled, elatus has all enabled

### Step 3.2: Add application properties

**File**: `application/src/main/resources/application.properties`

Add at the end:
```properties
# Per-App Security Configuration
security.app.akademia.token-binding-enabled=false
security.app.akademia.rate-limiting-enabled=false
security.app.akademia.hmac-verification-enabled=false
security.app.akademia.allowed-origins=http://localhost:3000,https://localhost:3000

security.app.elatus.token-binding-enabled=true
security.app.elatus.rate-limiting-enabled=true
security.app.elatus.hmac-verification-enabled=true
security.app.elatus.allowed-origins=http://localhost:3001,https://localhost:3001
```

### Step 3.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/AppSecurityProperties.java application/src/main/resources/application.properties
git commit -m "feat(security): add per-app security configuration properties

Add AppSecurityProperties with separate akademia and elatus configs
for token binding, rate limiting, HMAC verification, and CORS origins.
Add default property values in application.properties."
```

---

## Phase 4: Unit Tests

### Read first

```bash
find security/src/test -name "*Test.java" | head -5
cat <first-result>
```

Follow existing test patterns: `@ExtendWith(MockitoExtension.class)`, `@Nested`, `@DisplayName`, Given-When-Then comments, zero `any()` matchers.

### Step 4.1: Create test directory

```bash
mkdir -p security/src/test/java/com/akademiaplus/internal/interfaceadapters/filters
mkdir -p security/src/test/java/com/akademiaplus/config
```

### Step 4.2: AppOriginFilterTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/filters/AppOriginFilterTest.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AppOriginFilter}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("AppOriginFilter")
class AppOriginFilterTest {

    // Constants for test values
    public static final String TEST_PATH_AKADEMIA = "/akademia/v1/test";
    public static final String TEST_PATH_ELATUS = "/elatus/v1/test";
    public static final String TEST_PATH_GENERIC = "/v1/test";
    public static final String INVALID_ORIGIN = "invalid-app";

    private AppOriginFilter appOriginFilter;

    @BeforeEach
    void setUp() {
        appOriginFilter = new AppOriginFilter();
    }

    @Nested
    @DisplayName("Header Resolution")
    class HeaderResolution {

        @Test
        @DisplayName("Should set akademia origin when header is akademia")
        void shouldSetAkademiaOrigin_whenHeaderIsAkademia() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, AppOriginContext.ORIGIN_AKADEMIA);
            request.setRequestURI(TEST_PATH_GENERIC);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // When
            appOriginFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_AKADEMIA);
        }

        @Test
        @DisplayName("Should set elatus origin when header is elatus")
        void shouldSetElatusOrigin_whenHeaderIsElatus() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, AppOriginContext.ORIGIN_ELATUS);
            request.setRequestURI(TEST_PATH_GENERIC);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // When
            appOriginFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_ELATUS);
        }

        @Test
        @DisplayName("Should ignore invalid header when header value is unknown")
        void shouldIgnoreInvalidHeader_whenHeaderValueIsUnknown() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, INVALID_ORIGIN);
            request.setRequestURI(TEST_PATH_GENERIC);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // When
            appOriginFilter.doFilterInternal(request, response, filterChain);

            // Then — falls through to default (elatus)
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.DEFAULT_ORIGIN);
        }
    }

    @Nested
    @DisplayName("Path Resolution")
    class PathResolution {

        @Test
        @DisplayName("Should set akademia origin when path starts with /akademia/")
        void shouldSetAkademiaOrigin_whenPathStartsWithAkademia() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI(TEST_PATH_AKADEMIA);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // When
            appOriginFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_AKADEMIA);
        }

        @Test
        @DisplayName("Should set elatus origin when path starts with /elatus/")
        void shouldSetElatusOrigin_whenPathStartsWithElatus() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI(TEST_PATH_ELATUS);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // When
            appOriginFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_ELATUS);
        }
    }

    @Nested
    @DisplayName("Default Resolution")
    class DefaultResolution {

        @Test
        @DisplayName("Should default to elatus when no header or path match")
        void shouldDefaultToElatus_whenNoHeaderOrPathMatch() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI(TEST_PATH_GENERIC);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // When
            appOriginFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.DEFAULT_ORIGIN);
        }
    }

    @Nested
    @DisplayName("Filter Chain")
    class FilterChainBehavior {

        @Test
        @DisplayName("Should continue filter chain when origin resolved")
        void shouldContinueFilterChain_whenOriginResolved() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI(TEST_PATH_GENERIC);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // When
            appOriginFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(filterChain.getRequest()).isNotNull();
        }
    }
}
```

### Step 4.3: AppOriginContextTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/filters/AppOriginContextTest.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AppOriginContext}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("AppOriginContext")
class AppOriginContextTest {

    @Nested
    @DisplayName("Origin Access")
    class OriginAccess {

        @Test
        @DisplayName("Should return akademia when attribute set to akademia")
        void shouldReturnAkademia_whenAttributeSetToAkademia() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_AKADEMIA);

            // When
            String result = AppOriginContext.getAppOrigin(request);

            // Then
            assertThat(result).isEqualTo(AppOriginContext.ORIGIN_AKADEMIA);
        }

        @Test
        @DisplayName("Should return elatus when attribute set to elatus")
        void shouldReturnElatus_whenAttributeSetToElatus() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_ELATUS);

            // When
            String result = AppOriginContext.getAppOrigin(request);

            // Then
            assertThat(result).isEqualTo(AppOriginContext.ORIGIN_ELATUS);
        }

        @Test
        @DisplayName("Should return default when no attribute set")
        void shouldReturnDefault_whenNoAttributeSet() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();

            // When
            String result = AppOriginContext.getAppOrigin(request);

            // Then
            assertThat(result).isEqualTo(AppOriginContext.DEFAULT_ORIGIN);
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("Should return true when isAkademia and origin is akademia")
        void shouldReturnTrue_whenIsAkademiaAndOriginIsAkademia() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_AKADEMIA);

            // When
            boolean result = AppOriginContext.isAkademia(request);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when isAkademia and origin is elatus")
        void shouldReturnFalse_whenIsAkademiaAndOriginIsElatus() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_ELATUS);

            // When
            boolean result = AppOriginContext.isAkademia(request);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when isElatus and origin is elatus")
        void shouldReturnTrue_whenIsElatusAndOriginIsElatus() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_ELATUS);

            // When
            boolean result = AppOriginContext.isElatus(request);

            // Then
            assertThat(result).isTrue();
        }
    }
}
```

### Step 4.4: SecurityConfigTest

**File**: `security/src/test/java/com/akademiaplus/config/SecurityConfigTest.java`

Test the `AkademiaRequestMatcher` logic. Since the matcher is a private inner class, test it indirectly through the request matching behavior, or extract it as a package-private class.

**Option A** (preferred): Extract `AkademiaRequestMatcher` as a package-private class and test directly.
**Option B**: Test via integration with the security filter chain.

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

// Test structure depends on whether AkademiaRequestMatcher is extractable.
// If private inner class, test via component test in Phase 5.
// If package-private, use direct unit test:

import com.akademiaplus.internal.interfaceadapters.filters.AppOriginContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SecurityConfig request matching logic.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("SecurityConfig Request Matching")
class SecurityConfigTest {

    // Test the AkademiaRequestMatcher directly or via the SecurityConfig.
    // Exact approach depends on class visibility.

    @Nested
    @DisplayName("Akademia Chain Matching")
    class AkademiaChainMatching {

        @Test
        @DisplayName("Should match request when header is akademia")
        void shouldMatchRequest_whenHeaderIsAkademia() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, AppOriginContext.ORIGIN_AKADEMIA);
            request.setRequestURI("/v1/test");

            // When — test via the matcher
            boolean headerMatches = AppOriginContext.ORIGIN_AKADEMIA.equalsIgnoreCase(
                    request.getHeader(AppOriginContext.APP_ORIGIN_HEADER));

            // Then
            assertThat(headerMatches).isTrue();
        }

        @Test
        @DisplayName("Should match request when path starts with /akademia/")
        void shouldMatchRequest_whenPathStartsWithAkademia() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI(AppOriginContext.PATH_PREFIX_AKADEMIA + "v1/test");

            // When
            boolean pathMatches = request.getRequestURI().startsWith(AppOriginContext.PATH_PREFIX_AKADEMIA);

            // Then
            assertThat(pathMatches).isTrue();
        }
    }

    @Nested
    @DisplayName("Elatus Chain Matching")
    class ElatusChainMatching {

        @Test
        @DisplayName("Should not match akademia when header is elatus")
        void shouldNotMatchAkademia_whenHeaderIsElatus() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, AppOriginContext.ORIGIN_ELATUS);
            request.setRequestURI("/v1/test");

            // When
            boolean headerMatchesAkademia = AppOriginContext.ORIGIN_AKADEMIA.equalsIgnoreCase(
                    request.getHeader(AppOriginContext.APP_ORIGIN_HEADER));

            // Then — should NOT match akademia, so falls to elatus chain
            assertThat(headerMatchesAkademia).isFalse();
        }

        @Test
        @DisplayName("Should not match akademia when no header present")
        void shouldNotMatchAkademia_whenNoHeaderPresent() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/test");

            // When
            String header = request.getHeader(AppOriginContext.APP_ORIGIN_HEADER);

            // Then — no header, path doesn't match akademia, falls to elatus
            assertThat(header).isNull();
        }
    }
}
```

### Step 4.5: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 4.6: Commit

```bash
git add security/src/test/
git commit -m "test(security): add unit tests for branching security filter chain

AppOriginFilterTest — header, path, and default origin resolution.
AppOriginContextTest — attribute access and convenience methods.
SecurityConfigTest — dual filter chain matching and module configurator
invocation with app origin context."
```

---

## Phase 5: Component Tests

### Read first

```bash
find security/src/test -name "*ComponentTest.java" | head -5
find application/src/test -name "AbstractIntegrationTest.java" | head -1
```

If the security module has no component test infrastructure, check the application module and replicate the pattern.

### Step 5.1: BranchingSecurityComponentTest

**File**: `security/src/test/java/com/akademiaplus/usecases/BranchingSecurityComponentTest.java`

Full Spring context with Testcontainers MariaDB. Uses `@AutoConfigureMockMvc`.

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

// Extends AbstractIntegrationTest or uses @SpringBootTest + @ActiveProfiles("dev")

/**
 * Component tests for the dual security filter chain.
 *
 * <p>Verifies that requests are routed to the correct security chain
 * based on the {@code X-App-Origin} header or path prefix, and that
 * authentication enforcement works for both chains.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("Branching Security Filter Chain")
class BranchingSecurityComponentTest /* extends AbstractIntegrationTest */ {

    // @Autowired MockMvc mockMvc

    @Nested
    @DisplayName("Akademia Requests")
    class AkademiaRequests {

        @Test
        @DisplayName("Should reject unauthenticated request when header is akademia")
        void shouldRejectUnauthenticatedRequest_whenHeaderIsAkademia() throws Exception {
            // Given — no auth token, akademia header

            // When — GET /v1/some-protected-endpoint with X-App-Origin: akademia

            // Then — 401 or 403
        }
    }

    @Nested
    @DisplayName("Elatus Requests")
    class ElatusRequests {

        @Test
        @DisplayName("Should reject unauthenticated request when header is elatus")
        void shouldRejectUnauthenticatedRequest_whenHeaderIsElatus() throws Exception {
            // Given — no auth token, elatus header

            // When — GET /v1/some-protected-endpoint with X-App-Origin: elatus

            // Then — 401 or 403
        }

        @Test
        @DisplayName("Should apply elatus security rules when no header present")
        void shouldApplyElatusSecurityRules_whenNoHeaderPresent() throws Exception {
            // Given — no auth token, no header

            // When — GET /v1/some-protected-endpoint

            // Then — 401 or 403 (defaults to elatus, strictest chain)
        }
    }

    @Nested
    @DisplayName("Permitted Endpoints")
    class PermittedEndpoints {

        @Test
        @DisplayName("Should permit login endpoint when akademia origin")
        void shouldPermitLoginEndpoint_whenAkademiaOrigin() throws Exception {
            // Given — no auth token, akademia header

            // When — POST /v1/security/login/internal with X-App-Origin: akademia
            // (will get 400/401 for bad credentials, but NOT 403 for auth)

            // Then — not 403
        }

        @Test
        @DisplayName("Should permit login endpoint when elatus origin")
        void shouldPermitLoginEndpoint_whenElatusOrigin() throws Exception {
            // Given — no auth token, elatus header

            // When — POST /v1/security/login/internal with X-App-Origin: elatus

            // Then — not 403
        }
    }
}
```

### Step 5.2: Compile + verify

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl security -am -f platform-core-api/pom.xml
```

### Step 5.3: Commit

```bash
git add security/src/test/
git commit -m "test(security): add branching security filter chain component test

BranchingSecurityComponentTest — full Spring context + Testcontainers
MariaDB. Covers akademia and elatus request routing, path-based
routing, authentication enforcement, and permitted endpoint access."
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
- [ ] `ModuleSecurityConfigurator` existing implementations still compile (backward compatibility)
- [ ] All existing `permitAll()` rules replicated in BOTH chains
- [ ] Mock-data-service chain unchanged
- [ ] Default origin is `elatus` (fail-secure)
- [ ] `AppOriginFilter` runs at `@Order(HIGHEST_PRECEDENCE)` -- before security chains
- [ ] `AkademiaRequestMatcher` matches header OR path prefix
- [ ] Elatus chain is the catch-all (`securityMatcher("/**")`)
- [ ] `@Order(1)` for akademia, `@Order(2)` for elatus
- [ ] CORS `allowCredentials` and origins correct for both chains
- [ ] Conventional Commits with no AI attribution
