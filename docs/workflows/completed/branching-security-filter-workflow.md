# Branching Security Filter Chain Workflow

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and `DESIGN.md` before starting.

---

## 1. Architecture Overview

### 1.1 Current State

The platform has a single `SecurityFilterChain` that applies the same security rules to all requests. However, the backend serves two distinct web applications with different security requirements:

- **akademia-plus-web**: School premises, IP whitelist, standard JWT validation
- **elatusdev-web**: Public internet, full security stack

Currently there is no mechanism to differentiate between requests from these two applications.

| Component | Location | State |
|-----------|----------|-------|
| `SecurityConfig` | `security/.../config/` | Single `SecurityFilterChain` (dev/local profile) + mock-data-service chain |
| `ModuleSecurityConfigurator` | `security/.../config/` | Interface for module-level auth rules -- not app-origin-aware |
| `JwtRequestFilter` | `security/.../internal/interfaceadapters/jwt/` | Single filter, no app-origin context |
| `TenantContextLoader` | `infra-common/.../config/` | Sets tenant from `X-Tenant-Id` header |
| App origin detection | -- | Not implemented |
| Dual filter chains | -- | Not implemented |
| Per-app security properties | -- | Not implemented |

### 1.2 What's Missing

1. **App origin detection**: No filter to determine whether a request originates from akademia-plus-web or elatusdev-web
2. **Dual filter chains**: Single `SecurityFilterChain` -- cannot apply different security rules per app
3. **Per-app security configuration**: No application properties for per-app security settings
4. **App-origin-aware module configurators**: `ModuleSecurityConfigurator` does not support app-origin context
5. **Security hook points**: No extension points for rate limiting, HMAC verification, or token binding (elatusdev-web only)

### 1.3 Target Architecture

```
Incoming Request
    |
    v
AppOriginFilter (@Order(1))
    |--- Extract app origin from:
    |    1. X-App-Origin header (explicit)
    |    2. Request path prefix (/akademia/** or /elatus/**)
    |    3. Default: "elatus" (fail-secure)
    |--- Set app origin in request attribute
    |
    +--- origin = "akademia"              +--- origin = "elatus"
    |                                     |
    v                                     v
AkademiaSecurityFilterChain (@Order(1))   ElatusSecurityFilterChain (@Order(2))
    |                                     |
    +-- JWT validation                    +-- JWT validation
    +-- Tenant context check              +-- Tenant context check
    +-- Standard auth rules               +-- Token binding verification (hook)
                                          +-- Rate limiting (hook)
                                          +-- HMAC verification (hook)
                                          +-- Stricter auth rules
```

### 1.4 App Origin Resolution Strategy

The app origin is determined by the following priority:

| Priority | Source | Value | Use Case |
|----------|--------|-------|----------|
| 1 | `X-App-Origin` header | `akademia` or `elatus` | Frontend explicitly identifies itself |
| 2 | Request path prefix | `/akademia/**` -> `akademia`, `/elatus/**` -> `elatus` | Path-based routing |
| 3 | Default | `elatus` | Fail-secure: unknown requests get full security |

### 1.5 Module Placement

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `AppOriginFilter` | security | `internal/interfaceadapters/filters/` | Request-scoped filter, runs before security chains |
| `AppOriginContext` | security | `internal/interfaceadapters/filters/` | Utility to read/write app origin on request attributes |
| `AppSecurityProperties` | security | `config/` | `@ConfigurationProperties` for per-app settings |
| `SecurityConfig` (refactored) | security | `config/` | Split into dual `SecurityFilterChain` beans |
| `ModuleSecurityConfigurator` (enhanced) | security | `config/` | Extended to support app-origin-aware configuration |

---

## 2. File Inventory

### New files (7)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `security/.../internal/interfaceadapters/filters/AppOriginFilter.java` | security | 1 |
| 2 | `security/.../internal/interfaceadapters/filters/AppOriginContext.java` | security | 1 |
| 3 | `security/.../config/AppSecurityProperties.java` | security | 3 |
| 4 | `security/test/.../internal/interfaceadapters/filters/AppOriginFilterTest.java` | security | 4 |
| 5 | `security/test/.../internal/interfaceadapters/filters/AppOriginContextTest.java` | security | 4 |
| 6 | `security/test/.../config/SecurityConfigTest.java` | security | 4 |
| 7 | `security/test/.../usecases/BranchingSecurityComponentTest.java` | security | 5 |

### Modified files (3)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `security/.../config/SecurityConfig.java` | Split into dual filter chains | 2 |
| 2 | `security/.../config/ModuleSecurityConfigurator.java` | Add app-origin-aware method | 2 |
| 3 | `application/src/main/resources/application.properties` | Add per-app security properties | 3 |

---

## 3. Implementation Sequence

### Phase Dependency Graph

```
Phase 1:  AppOriginFilter + AppOriginContext (extracts/validates app origin)
    |
Phase 2:  SecurityConfig refactor (dual filter chains + ModuleSecurityConfigurator enhancement)
    |
Phase 3:  Per-app security properties (AppSecurityProperties + application.properties)
    |
Phase 4:  Unit tests
    |
Phase 5:  Component tests
```

---

## 4. Phase-by-Phase Implementation

### Phase 1: AppOriginFilter

#### Step 1.1: Create AppOriginContext

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
        // Utility class
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

#### Step 1.2: Create AppOriginFilter

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
 * <p>The origin is determined by (in priority order):</p>
 * <ol>
 *   <li>{@code X-App-Origin} header (explicit frontend identification)</li>
 *   <li>Request path prefix ({@code /akademia/**} or {@code /elatus/**})</li>
 *   <li>Default: {@code elatus} (fail-secure for unknown origins)</li>
 * </ol>
 *
 * <p>The resolved origin is stored as a request attribute and can be
 * accessed via {@link AppOriginContext#getAppOrigin(HttpServletRequest)}.</p>
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
     * Resolves the app origin and sets it on the request attributes.
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

    /**
     * Resolves the app origin from the request using the priority chain:
     * header, path prefix, default.
     *
     * @param request the HTTP request
     * @return the resolved app origin
     */
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
            // Fall through to path-based resolution
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

#### Step 1.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 1.4: Commit

```
feat(security): add AppOriginFilter for dual-app request routing

Add AppOriginFilter that resolves app origin from X-App-Origin
header, path prefix (/akademia/** or /elatus/**), or default
(elatus, fail-secure). Add AppOriginContext utility for reading
and writing origin on request attributes.
```

---

### Phase 2: SecurityConfig Refactor

#### Step 2.1: Enhance ModuleSecurityConfigurator

**File**: `security/src/main/java/com/akademiaplus/config/ModuleSecurityConfigurator.java`

Add a default method for app-origin-aware configuration:

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Interface for modules to contribute their security rules to the
 * application's {@link org.springframework.security.web.SecurityFilterChain}.
 *
 * <p>Modules implement this interface to declare which endpoints they
 * expose and what authorization rules apply. The {@link SecurityConfig}
 * collects all implementations and applies them to the filter chain.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface ModuleSecurityConfigurator {

    /**
     * Configures authorization rules for this module's endpoints.
     *
     * <p>Called for both the akademia and elatus security filter chains.
     * Use this method for rules that apply regardless of app origin.</p>
     *
     * @param auth the authorization configuration registry
     * @throws Exception if configuration fails
     */
    void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception;

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
    default void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                           String appOrigin) throws Exception {
        configure(auth);
    }
}
```

#### Step 2.2: Refactor SecurityConfig

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Split the single `securityFilterChain` into two ordered beans:

```java
/**
 * Security filter chain for akademia-plus-web requests.
 *
 * <p>Matches requests with the {@code X-App-Origin: akademia} header
 * or the {@code /akademia/**} path prefix. Applies standard JWT
 * validation and tenant context verification.</p>
 *
 * @param moduleSecurityConfigurators the set of module security configurators
 * @param http                        the HTTP security builder
 * @param jwtRequestFilter            the JWT request filter
 * @param appOriginFilter             the app origin filter
 * @return the akademia security filter chain
 * @throws Exception if configuration fails
 */
@Bean
@Order(1)
@Profile({"dev", "local"})
public SecurityFilterChain akademiaSecurityFilterChain(
        Set<ModuleSecurityConfigurator> moduleSecurityConfigurators,
        HttpSecurity http,
        JwtRequestFilter jwtRequestFilter,
        AppOriginFilter appOriginFilter) throws Exception {

    http
        .securityMatcher(new AkademiaRequestMatcher())
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSourceForLogin()))
        .headers(/* same as current */)
        .authorizeHttpRequests(auth -> {
            // Same base rules as current
            auth.requestMatchers("/actuator/**").permitAll();
            auth.requestMatchers("/v1/security/login/internal").permitAll();
            auth.requestMatchers("/v1/security/register").permitAll();
            // ... other permits

            // Apply module configurators with akademia origin
            for (ModuleSecurityConfigurator configurator : moduleSecurityConfigurators) {
                configurator.configure(auth, AppOriginContext.ORIGIN_AKADEMIA);
            }
            auth.anyRequest().authenticated();
        })
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .addFilterBefore(appOriginFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

/**
 * Security filter chain for elatusdev-web requests.
 *
 * <p>Matches all requests that do not match the akademia chain.
 * Applies the full security stack: JWT validation, tenant context,
 * and hook points for token binding, rate limiting, and HMAC
 * verification (to be implemented in future features).</p>
 *
 * @param moduleSecurityConfigurators the set of module security configurators
 * @param http                        the HTTP security builder
 * @param jwtRequestFilter            the JWT request filter
 * @param appOriginFilter             the app origin filter
 * @return the elatus security filter chain
 * @throws Exception if configuration fails
 */
@Bean
@Order(2)
@Profile({"dev", "local"})
public SecurityFilterChain elatusSecurityFilterChain(
        Set<ModuleSecurityConfigurator> moduleSecurityConfigurators,
        HttpSecurity http,
        JwtRequestFilter jwtRequestFilter,
        AppOriginFilter appOriginFilter) throws Exception {

    http
        .securityMatcher("/**")
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSourceForLogin()))
        .headers(/* same as current */)
        .authorizeHttpRequests(auth -> {
            auth.requestMatchers("/actuator/**").permitAll();
            auth.requestMatchers("/v1/security/login/internal").permitAll();
            auth.requestMatchers("/v1/security/register").permitAll();
            // ... other permits

            // Apply module configurators with elatus origin
            for (ModuleSecurityConfigurator configurator : moduleSecurityConfigurators) {
                configurator.configure(auth, AppOriginContext.ORIGIN_ELATUS);
            }
            auth.anyRequest().authenticated();
        })
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .addFilterBefore(appOriginFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        // Future: add token binding filter, rate limiting filter, HMAC filter here

    return http.build();
}
```

#### Step 2.3: Create AkademiaRequestMatcher

Create an inner class or separate class that matches requests with the `X-App-Origin: akademia` header or `/akademia/**` path prefix:

```java
/**
 * Request matcher that identifies akademia-plus-web requests.
 *
 * <p>Matches if the {@code X-App-Origin} header is "akademia" or
 * the request path starts with {@code /akademia/}.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
private static class AkademiaRequestMatcher implements RequestMatcher {

    /**
     * Tests whether the request originates from akademia-plus-web.
     *
     * @param request the HTTP request
     * @return true if the request matches akademia criteria
     */
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

#### Step 2.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 2.5: Commit

```
feat(security): split SecurityFilterChain into dual app-origin chains

Refactor SecurityConfig into akademiaSecurityFilterChain (@Order 1)
and elatusSecurityFilterChain (@Order 2). Add AkademiaRequestMatcher
to route requests by X-App-Origin header or path prefix.
Enhance ModuleSecurityConfigurator with app-origin-aware default method.
```

---

### Phase 3: Per-App Security Properties

#### Step 3.1: Create AppSecurityProperties

**File**: `security/src/main/java/com/akademiaplus/config/AppSecurityProperties.java`

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
import org.springframework.stereotype.Component;

/**
 * Configuration properties for per-app security settings.
 *
 * <p>Provides separate configuration for the akademia-plus-web and
 * elatusdev-web applications, enabling different security postures
 * for school-premises vs public-internet traffic.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "security.app")
public class AppSecurityProperties {

    public static final String PREFIX = "security.app";

    private AppConfig akademia = new AppConfig();
    private AppConfig elatus = new AppConfig();

    /**
     * Returns the akademia-plus-web security configuration.
     *
     * @return the akademia app config
     */
    public AppConfig getAkademia() {
        return akademia;
    }

    /**
     * Sets the akademia-plus-web security configuration.
     *
     * @param akademia the akademia app config
     */
    public void setAkademia(AppConfig akademia) {
        this.akademia = akademia;
    }

    /**
     * Returns the elatusdev-web security configuration.
     *
     * @return the elatus app config
     */
    public AppConfig getElatus() {
        return elatus;
    }

    /**
     * Sets the elatusdev-web security configuration.
     *
     * @param elatus the elatus app config
     */
    public void setElatus(AppConfig elatus) {
        this.elatus = elatus;
    }

    /**
     * Per-application security configuration.
     *
     * @author ElatusDev
     * @since 1.0
     */
    public static class AppConfig {

        /** Whether token binding verification is enabled. */
        private boolean tokenBindingEnabled = false;

        /** Whether rate limiting is enabled. */
        private boolean rateLimitingEnabled = false;

        /** Whether HMAC request verification is enabled. */
        private boolean hmacVerificationEnabled = false;

        /** Allowed CORS origins for this app. */
        private String[] allowedOrigins = {};

        // Getters and setters with Javadoc

        /**
         * Returns whether token binding verification is enabled.
         *
         * @return true if token binding is enabled
         */
        public boolean isTokenBindingEnabled() {
            return tokenBindingEnabled;
        }

        /**
         * Sets whether token binding verification is enabled.
         *
         * @param tokenBindingEnabled the token binding flag
         */
        public void setTokenBindingEnabled(boolean tokenBindingEnabled) {
            this.tokenBindingEnabled = tokenBindingEnabled;
        }

        /**
         * Returns whether rate limiting is enabled.
         *
         * @return true if rate limiting is enabled
         */
        public boolean isRateLimitingEnabled() {
            return rateLimitingEnabled;
        }

        /**
         * Sets whether rate limiting is enabled.
         *
         * @param rateLimitingEnabled the rate limiting flag
         */
        public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
            this.rateLimitingEnabled = rateLimitingEnabled;
        }

        /**
         * Returns whether HMAC request verification is enabled.
         *
         * @return true if HMAC verification is enabled
         */
        public boolean isHmacVerificationEnabled() {
            return hmacVerificationEnabled;
        }

        /**
         * Sets whether HMAC request verification is enabled.
         *
         * @param hmacVerificationEnabled the HMAC verification flag
         */
        public void setHmacVerificationEnabled(boolean hmacVerificationEnabled) {
            this.hmacVerificationEnabled = hmacVerificationEnabled;
        }

        /**
         * Returns the allowed CORS origins for this app.
         *
         * @return the allowed origins array
         */
        public String[] getAllowedOrigins() {
            return allowedOrigins;
        }

        /**
         * Sets the allowed CORS origins for this app.
         *
         * @param allowedOrigins the allowed origins array
         */
        public void setAllowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
```

#### Step 3.2: Add application properties

**File**: `application/src/main/resources/application.properties`

Add:
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

#### Step 3.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 3.4: Commit

```
feat(security): add per-app security configuration properties

Add AppSecurityProperties with separate akademia and elatus configs
for token binding, rate limiting, HMAC verification, and CORS origins.
Add default property values in application.properties.
```

---

### Phase 4: Unit Tests

All tests follow conventions: Given-When-Then, `shouldDoX_whenY()`, `@DisplayName`, `@Nested`, zero `any()` matchers, `public static final` constants.

#### Step 4.1: AppOriginFilterTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/filters/AppOriginFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- Uses `MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain` from Spring Test

| @Nested | Tests |
|---------|-------|
| `HeaderResolution` | `shouldSetAkademiaOrigin_whenHeaderIsAkademia`, `shouldSetElatusOrigin_whenHeaderIsElatus`, `shouldIgnoreInvalidHeader_whenHeaderValueIsUnknown` |
| `PathResolution` | `shouldSetAkademiaOrigin_whenPathStartsWithAkademia`, `shouldSetElatusOrigin_whenPathStartsWithElatus` |
| `DefaultResolution` | `shouldDefaultToElatus_whenNoHeaderOrPathMatch`, `shouldDefaultToElatus_whenHeaderIsInvalidAndNoPathMatch` |
| `FilterChain` | `shouldContinueFilterChain_whenOriginResolved` |

#### Step 4.2: AppOriginContextTest

**File**: `security/src/test/java/com/akademiaplus/internal/interfaceadapters/filters/AppOriginContextTest.java`

| @Nested | Tests |
|---------|-------|
| `OriginAccess` | `shouldReturnAkademia_whenAttributeSetToAkademia`, `shouldReturnElatus_whenAttributeSetToElatus`, `shouldReturnDefault_whenNoAttributeSet` |
| `ConvenienceMethods` | `shouldReturnTrue_whenIsAkademiaAndOriginIsAkademia`, `shouldReturnFalse_whenIsAkademiaAndOriginIsElatus`, `shouldReturnTrue_whenIsElatusAndOriginIsElatus` |

#### Step 4.3: SecurityConfigTest

**File**: `security/src/test/java/com/akademiaplus/config/SecurityConfigTest.java`

This test verifies the dual filter chain configuration:

| @Nested | Tests |
|---------|-------|
| `AkademiaChain` | `shouldMatchRequest_whenHeaderIsAkademia`, `shouldMatchRequest_whenPathStartsWithAkademia` |
| `ElatusChain` | `shouldMatchRequest_whenHeaderIsElatus`, `shouldMatchRequest_whenNoHeaderPresent` |
| `ModuleConfigurators` | `shouldCallConfigureWithAkademiaOrigin_whenAkademiaChainBuilds`, `shouldCallConfigureWithElatusOrigin_whenElatusChainBuilds` |

#### Step 4.4: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 4.5: Commit

```
test(security): add unit tests for branching security filter chain

AppOriginFilterTest — header, path, and default origin resolution.
AppOriginContextTest — attribute access and convenience methods.
SecurityConfigTest — dual filter chain matching and module configurator
invocation with app origin context.
```

---

### Phase 5: Component Tests

#### Step 5.1: BranchingSecurityComponentTest

**File**: `security/src/test/java/com/akademiaplus/usecases/BranchingSecurityComponentTest.java`

Full Spring context + Testcontainers MariaDB. Tests the complete HTTP stack with the dual filter chains.

| @Nested | Tests |
|---------|-------|
| `AkademiaRequests` | `shouldAllowAuthenticatedRequest_whenHeaderIsAkademia`, `shouldRejectUnauthenticatedRequest_whenHeaderIsAkademia` |
| `ElatusRequests` | `shouldAllowAuthenticatedRequest_whenHeaderIsElatus`, `shouldRejectUnauthenticatedRequest_whenHeaderIsElatus`, `shouldApplyElatusSecurityRules_whenNoHeaderPresent` |
| `PathBasedRouting` | `shouldRouteToAkademiaChain_whenPathStartsWithAkademia`, `shouldRouteToElatusChain_whenPathStartsWithElatus` |
| `PermittedEndpoints` | `shouldPermitLoginEndpoint_whenAkademiaOrigin`, `shouldPermitLoginEndpoint_whenElatusOrigin` |

#### Step 5.2: Compile + verify

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl security -am -f platform-core-api/pom.xml
```

#### Step 5.3: Commit

```
test(security): add branching security filter chain component test

BranchingSecurityComponentTest — full Spring context + Testcontainers
MariaDB. Covers akademia and elatus request routing, path-based
routing, authentication enforcement, and permitted endpoint access.
```

---

## 5. Key Design Decisions

### Request Matcher Strategy: Header vs. Path vs. Both

| Aspect | Header only | Path only | Both (chosen) |
|--------|-------------|-----------|---------------|
| Frontend flexibility | Frontend must set header | URLs must include prefix | Maximum flexibility |
| API cleanliness | Clean URL paths | Prefix in every URL | Clean URLs with header, path as fallback |
| Reverse proxy support | Header can be set by proxy | Path routing native | Both supported |
| Migration risk | Existing clients need update | Existing paths break | No breaking changes |

### Default Origin: akademia vs. elatus

| Choice | Rationale |
|--------|-----------|
| Default to `elatus` (chosen) | **Fail-secure**: unknown requests get the strictest security stack |
| Default to `akademia` | Fail-open: unknown requests bypass enhanced security -- unacceptable |

### Filter Ordering: AppOriginFilter Position

| Position | Rationale |
|----------|-----------|
| `@Order(HIGHEST_PRECEDENCE)` (chosen) | Must run before `SecurityFilterChain` matching so the request matcher can read the attribute |
| After SecurityFilterChain | Too late -- the chain is already selected |

---

## 6. Multi-Tenancy Considerations

1. **App origin is independent of tenant**: A single tenant can have users on both akademia-plus-web and elatusdev-web
2. **Tenant context** is still set by `TenantContextLoader` from the `X-Tenant-Id` header -- unchanged
3. **Per-tenant app-origin overrides** are NOT supported in this feature -- all tenants use the same security rules per app origin
4. **Future**: If per-tenant security rules are needed, extend `AppSecurityProperties` to support tenant-level overrides

---

## 7. Future Extensibility

1. **Token binding filter**: Can be conditionally added to the elatus chain based on `AppSecurityProperties.getElatus().isTokenBindingEnabled()`
2. **Rate limiting filter**: Same conditional pattern for the elatus chain
3. **HMAC verification filter**: Same conditional pattern for the elatus chain
4. **IP whitelist filter**: Can be added to the akademia chain specifically
5. **Per-module security**: Modules can override `ModuleSecurityConfigurator.configure(auth, appOrigin)` to apply different rules per app
6. **Additional app origins**: New apps (e.g., mobile-api) can be added by extending `AppOriginContext` and adding a new filter chain

---

## 8. Verification Checklist

### Per-phase gates

After each phase, run the specified compile/test command. Fix all errors before proceeding.

### Final verification

1. `mvn clean compile -pl security -am -DskipTests` -- full compilation
2. `mvn test -pl security` -- all unit tests pass
3. `mvn verify -pl security` -- component tests pass
4. Manual: Send request with `X-App-Origin: akademia` header -- verify it routes to akademia chain
5. Manual: Send request with `X-App-Origin: elatus` header -- verify it routes to elatus chain
6. Manual: Send request with no header -- verify it defaults to elatus chain

---

## 9. Critical Reminders

1. **Existing endpoints must keep working**: The refactor must not break any existing endpoint. All current `permitAll()` rules must be replicated in BOTH chains.
2. **`ModuleSecurityConfigurator` backward compatibility**: The new `configure(auth, appOrigin)` default method delegates to the existing `configure(auth)` method -- existing implementations need NO changes.
3. **`@Order` matters**: The akademia chain MUST be `@Order(1)` (higher priority) and the elatus chain MUST be `@Order(2)`. Spring evaluates chains in order and uses the first match.
4. **`securityMatcher` vs `requestMatchers`**: The akademia chain uses `securityMatcher(new AkademiaRequestMatcher())` to select which requests go to this chain. The elatus chain uses `securityMatcher("/**")` as a catch-all.
5. **Mock-data-service chain**: The existing `@Profile("mock-data-service")` chain must remain unchanged -- it operates independently.
6. **CORS `allowCredentials`**: If cookie-based auth is implemented (from the refresh token rotation feature), CORS must set `allowCredentials(true)` and cannot use wildcard origins.
7. **No `any()` matchers** in tests -- all mock stubbing uses exact values or `ArgumentCaptor`.
8. **Conventional Commits** -- no AI attribution in commit messages.
