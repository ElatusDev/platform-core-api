# IP Whitelist Filter Workflow — AkademiaPlus-Only

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `docs/directives/CLAUDE.md`, `docs/directives/AI-CODE-REF.md`, and `docs/design/DESIGN.md` before starting.
**Dependency**: Requires branching-security-filter feature (determines AkademiaPlus vs ElatusDev origin). If branching-security-filter is not yet implemented, the filter must detect AkademiaPlus origin via request attribute or header set by that filter.

---

## 1. Architecture Overview

### 1.1 Purpose

The IP Whitelist Filter restricts API access for AkademiaPlus-origin requests to a configurable set of CIDR ranges. Schools deploy the AkademiaPlus web app on premises, and the API must only accept requests from known school IP ranges. Requests from ElatusDev-origin (public internet) are never subject to IP filtering.

### 1.2 Filter Chain Position

```
Incoming HTTP Request
  │
  ├── @Order(-105) RequestContextFilter       (Spring built-in)
  ├── @Order(-100) FilterChainProxy           (Spring Security)
  │     └── SecurityFilterChain
  │           ├── IpWhitelistFilter @Order(1)  ← NEW: IP whitelist check
  │           ├── BranchingSecurityFilter @Order(2)  (prerequisite feature)
  │           └── JwtRequestFilter @Order(3)   (existing)
  ├── @Order(-50)  TenantContextLoader        (existing)
  │
  └── DispatcherServlet → Controller
```

The `IpWhitelistFilter` runs at `@Order(1)` — **before** the JWT filter (`@Order(3)`) — so rejected IPs never consume JWT validation resources. It only applies when the request is identified as AkademiaPlus origin (via request attribute or header set by the branching-security-filter).

### 1.3 Request Flow

```
IpWhitelistFilter.doFilterInternal(request, response, chain)
  │
  ├── shouldNotFilter(request)?
  │     ├── YES (health/actuator/swagger) → chain.doFilter() → SKIP
  │     └── NO → continue
  │
  ├── isAkademiaPlusOrigin(request)?
  │     ├── NO (ElatusDev origin) → chain.doFilter() → PASS
  │     └── YES → continue
  │
  ├── Extract client IP:
  │     ├── X-Forwarded-For header present?
  │     │     ├── YES → take FIRST IP in comma-separated chain
  │     │     └── NO → request.getRemoteAddr()
  │     │
  │     └── clientIp
  │
  ├── CidrMatcher.isAllowed(clientIp, allowedCidrs)?
  │     ├── YES → chain.doFilter() → PASS
  │     └── NO → 403 Forbidden JSON error body → REJECT
  │
  └── END
```

### 1.4 Module Placement

Per CLAUDE.md Hard Rules #5, #12, #13 and DESIGN.md Section 3.2.8:

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `CidrMatcher` | utilities | `com.akademiaplus.utilities.network/` | Reusable utility — no Spring dependency |
| `CidrMatcherTest` | utilities | `com.akademiaplus.utilities.network/` | Unit tests for utility |
| `IpWhitelistProperties` | security | `com.akademiaplus.config/` | Spring `@ConfigurationProperties` for `security.akademia.allowed-cidrs` |
| `IpWhitelistFilter` | security | `com.akademiaplus.config/` | `OncePerRequestFilter` — security infrastructure |
| `IpWhitelistFilterTest` | security | `com.akademiaplus.config/` | Unit tests for filter |
| `IpWhitelistPropertiesTest` | security | `com.akademiaplus.config/` | Unit tests for config properties |
| `IpWhitelistComponentTest` | application | `com.akademiaplus.usecases/` | Component test — full Spring context |

---

## 2. File Inventory

### 2.1 New Files (7)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `utilities/src/main/java/com/akademiaplus/utilities/network/CidrMatcher.java` | utilities | 1 |
| 2 | `utilities/src/test/java/com/akademiaplus/utilities/network/CidrMatcherTest.java` | utilities | 1 |
| 3 | `security/src/main/java/com/akademiaplus/config/IpWhitelistProperties.java` | security | 2 |
| 4 | `security/src/main/java/com/akademiaplus/config/IpWhitelistFilter.java` | security | 3 |
| 5 | `security/src/test/java/com/akademiaplus/config/IpWhitelistFilterTest.java` | security | 5 |
| 6 | `security/src/test/java/com/akademiaplus/config/IpWhitelistPropertiesTest.java` | security | 5 |
| 7 | `application/src/test/java/com/akademiaplus/usecases/IpWhitelistComponentTest.java` | application | 6 |

### 2.2 Modified Files (3)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `security/src/main/java/com/akademiaplus/config/SecurityConfig.java` | Register `IpWhitelistFilter` before JWT filter | 4 |
| 2 | `application/src/main/resources/application.properties` | Add `security.akademia.allowed-cidrs` list | 2 |
| 3 | `application/src/main/resources/application-dev.properties` | Add dev-mode CIDR ranges (e.g., `0.0.0.0/0` for local dev) | 2 |

---

## 3. Implementation Sequence

### Phase Dependency Graph

```
Phase 1:  CidrMatcher utility (utilities module) + unit tests
    ↓
Phase 2:  IpWhitelistProperties configuration (security module)
    ↓
Phase 3:  IpWhitelistFilter implementation (security module)
    ↓
Phase 4:  Integration with SecurityConfig filter chain
    ↓
Phase 5:  Unit tests (filter + properties — security module)
    ↓
Phase 6:  Component tests (application module)
```

---

## 4. Phase-by-Phase Implementation

### Phase 1: CidrMatcher Utility

#### Step 1.1: Create CidrMatcher

**File**: `utilities/src/main/java/com/akademiaplus/utilities/network/CidrMatcher.java`

Pure utility class — no Spring dependencies. Uses `java.net.InetAddress` for IP parsing and bit manipulation for CIDR matching.

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Validates whether an IP address falls within one or more CIDR ranges.
 *
 * <p>Supports both IPv4 and IPv6 addresses. Each CIDR range is specified
 * in standard notation (e.g., {@code "192.168.1.0/24"}, {@code "10.0.0.0/8"}).
 * A single IP without a prefix length is treated as {@code /32} (IPv4)
 * or {@code /128} (IPv6).
 *
 * @author ElatusDev
 * @since 1.0
 */
public final class CidrMatcher {

    /** Error message when the IP address format is invalid. */
    public static final String ERROR_INVALID_IP = "Invalid IP address: %s";

    /** Error message when the CIDR notation is invalid. */
    public static final String ERROR_INVALID_CIDR = "Invalid CIDR notation: %s";

    private CidrMatcher() {
        // Utility class — no instantiation
    }

    /**
     * Checks whether the given IP address is contained in any of the specified CIDR ranges.
     *
     * @param clientIp    the IP address to check (IPv4 or IPv6)
     * @param cidrRanges  the list of CIDR ranges to match against
     * @return {@code true} if the IP falls within at least one CIDR range
     * @throws IllegalArgumentException if the IP or any CIDR range is malformed
     */
    public static boolean isAllowed(String clientIp, List<String> cidrRanges) {
        InetAddress clientAddress = parseAddress(clientIp);
        byte[] clientBytes = clientAddress.getAddress();

        for (String cidr : cidrRanges) {
            if (matchesCidr(clientBytes, cidr)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCidr(byte[] clientBytes, String cidr) {
        String[] parts = cidr.split("/");
        InetAddress networkAddress = parseAddress(parts[0]);
        byte[] networkBytes = networkAddress.getAddress();

        if (clientBytes.length != networkBytes.length) {
            return false; // IPv4 vs IPv6 mismatch
        }

        int prefixLength = (parts.length == 2)
                ? parsePrefixLength(parts[1], cidr)
                : clientBytes.length * 8;

        return prefixMatches(clientBytes, networkBytes, prefixLength);
    }

    private static boolean prefixMatches(byte[] clientBytes, byte[] networkBytes, int prefixLength) {
        int fullBytes = prefixLength / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (clientBytes[i] != networkBytes[i]) {
                return false;
            }
        }
        int remainingBits = prefixLength % 8;
        if (remainingBits > 0) {
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return (clientBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        }
        return true;
    }

    private static InetAddress parseAddress(String ip) {
        try {
            return InetAddress.getByName(ip.trim());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(String.format(ERROR_INVALID_IP, ip), e);
        }
    }

    private static int parsePrefixLength(String prefixStr, String cidr) {
        try {
            return Integer.parseInt(prefixStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(ERROR_INVALID_CIDR, cidr), e);
        }
    }
}
```

#### Step 1.2: Create CidrMatcherTest

**File**: `utilities/src/test/java/com/akademiaplus/utilities/network/CidrMatcherTest.java`

```java
@ExtendWith(MockitoExtension.class)
class CidrMatcherTest {

    // Constants for test values
    public static final String CIDR_CLASS_C = "192.168.1.0/24";
    public static final String CIDR_CLASS_A = "10.0.0.0/8";
    public static final String CIDR_SINGLE_HOST = "172.16.0.1/32";

    @Nested
    @DisplayName("IPv4 matching")
    class Ipv4Matching {

        @Test
        @DisplayName("Should return true when IP is within /24 CIDR range")
        void shouldReturnTrue_whenIpIsWithinCidr24() { ... }

        @Test
        @DisplayName("Should return false when IP is outside /24 CIDR range")
        void shouldReturnFalse_whenIpIsOutsideCidr24() { ... }

        @Test
        @DisplayName("Should return true when IP matches /32 single host")
        void shouldReturnTrue_whenIpMatchesSingleHost() { ... }

        @Test
        @DisplayName("Should return true when IP matches any range in list")
        void shouldReturnTrue_whenIpMatchesAnyRangeInList() { ... }

        @Test
        @DisplayName("Should return false when no ranges match")
        void shouldReturnFalse_whenNoRangesMatch() { ... }

        @Test
        @DisplayName("Should return false when CIDR list is empty")
        void shouldReturnFalse_whenCidrListIsEmpty() { ... }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw IllegalArgumentException when IP is malformed")
        void shouldThrowIllegalArgumentException_whenIpIsMalformed() { ... }

        @Test
        @DisplayName("Should throw IllegalArgumentException when CIDR is malformed")
        void shouldThrowIllegalArgumentException_whenCidrIsMalformed() { ... }
    }
}
```

#### Step 1.3: Compile + test

```bash
mvn clean compile -pl utilities -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl utilities -f platform-core-api/pom.xml
```

#### Step 1.4: Commit

```
feat(utilities): add CidrMatcher utility for IP-to-CIDR validation

Add CidrMatcher utility class supporting IPv4 and IPv6 address
matching against CIDR ranges. Includes comprehensive unit tests
for range matching, single-host matching, and error handling.
```

---

### Phase 2: IpWhitelistProperties Configuration

#### Step 2.1: Create IpWhitelistProperties

**File**: `security/src/main/java/com/akademiaplus/config/IpWhitelistProperties.java`

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

import java.util.List;

/**
 * Configuration properties for the IP whitelist filter.
 *
 * <p>Binds to {@code security.akademia.allowed-cidrs} in application.yml
 * or application.properties. Provides the list of CIDR ranges that are
 * permitted to access the AkademiaPlus web app endpoints.
 *
 * <p>Example configuration:
 * <pre>
 * security.akademia.allowed-cidrs[0]=192.168.1.0/24
 * security.akademia.allowed-cidrs[1]=10.0.0.0/8
 * </pre>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "security.akademia")
public class IpWhitelistProperties {

    /** The list of CIDR ranges allowed for AkademiaPlus requests. */
    private List<String> allowedCidrs = List.of();

    /**
     * Returns the list of allowed CIDR ranges.
     *
     * @return the allowed CIDR ranges, never null
     */
    public List<String> getAllowedCidrs() {
        return allowedCidrs;
    }

    /**
     * Sets the list of allowed CIDR ranges.
     *
     * @param allowedCidrs the CIDR ranges to allow
     */
    public void setAllowedCidrs(List<String> allowedCidrs) {
        this.allowedCidrs = allowedCidrs;
    }
}
```

#### Step 2.2: Enable ConfigurationProperties scanning

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add `@EnableConfigurationProperties(IpWhitelistProperties.class)` to the `SecurityConfig` class.

#### Step 2.3: Add CIDR configuration

**File**: `application/src/main/resources/application.properties`

```properties
# IP Whitelist — AkademiaPlus origin restriction
# Production values set per-environment via application-{profile}.properties
security.akademia.allowed-cidrs=
```

**File**: `application/src/main/resources/application-dev.properties`

```properties
# Dev: allow all IPs for local development
security.akademia.allowed-cidrs[0]=0.0.0.0/0
security.akademia.allowed-cidrs[1]=::/0
```

#### Step 2.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 2.5: Commit

```
feat(security): add IpWhitelistProperties configuration

Add @ConfigurationProperties for security.akademia.allowed-cidrs
binding. Configure dev profile to allow all IPs. Enable
configuration properties scanning in SecurityConfig.
```

---

### Phase 3: IpWhitelistFilter Implementation

#### Step 3.1: Create IpWhitelistFilter

**File**: `security/src/main/java/com/akademiaplus/config/IpWhitelistFilter.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.utilities.network.CidrMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Servlet filter that enforces IP-based access control for AkademiaPlus-origin requests.
 *
 * <p>This filter runs at {@code @Order(1)} — before the JWT filter — so that
 * requests from unauthorized IP ranges are rejected immediately without
 * consuming JWT validation resources.
 *
 * <p>Only requests identified as AkademiaPlus origin (via the branching-security-filter)
 * are subject to IP whitelisting. ElatusDev-origin requests pass through unconditionally.
 *
 * <p>The filter extracts the client IP from the {@code X-Forwarded-For} header
 * (first IP in the chain) for proxied requests, falling back to
 * {@code request.getRemoteAddr()} for direct connections.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(1)
public class IpWhitelistFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(IpWhitelistFilter.class);

    /** HTTP header for forwarded client IP. */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /** Request attribute set by branching-security-filter to identify app origin. */
    public static final String ATTR_APP_ORIGIN = "app-origin";

    /** Value indicating AkademiaPlus origin. */
    public static final String ORIGIN_AKADEMIA_PLUS = "akademia-plus";

    /** Error message for rejected IP addresses. */
    public static final String ERROR_IP_NOT_ALLOWED = "Access denied: IP address not in whitelist";

    /** JSON error field: error code. */
    public static final String JSON_FIELD_CODE = "code";

    /** JSON error field: error message. */
    public static final String JSON_FIELD_MESSAGE = "message";

    /** Machine-readable error code for IP rejection. */
    public static final String CODE_IP_REJECTED = "IP_NOT_WHITELISTED";

    /** Log message for rejected IPs. */
    private static final String LOG_IP_REJECTED = "Rejected AkademiaPlus request from IP: {}";

    /** Log message for allowed IPs. */
    private static final String LOG_IP_ALLOWED = "Allowed AkademiaPlus request from IP: {}";

    private final IpWhitelistProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Creates the IP whitelist filter.
     *
     * @param properties    the CIDR configuration properties
     * @param objectMapper  Jackson object mapper for JSON error responses
     */
    public IpWhitelistFilter(IpWhitelistProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Bypasses the filter for health, actuator, and Swagger endpoints.
     *
     * @param request the HTTP request
     * @return {@code true} if the request targets a bypass path
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    /**
     * Applies IP whitelist validation for AkademiaPlus-origin requests.
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

        if (!isAkademiaPlusOrigin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);

        if (CidrMatcher.isAllowed(clientIp, properties.getAllowedCidrs())) {
            LOG.debug(LOG_IP_ALLOWED, clientIp);
            filterChain.doFilter(request, response);
            return;
        }

        LOG.warn(LOG_IP_REJECTED, clientIp);
        rejectRequest(response);
    }

    /**
     * Determines whether the request originates from the AkademiaPlus web app.
     *
     * <p>Checks the {@code app-origin} request attribute set by the
     * branching-security-filter. Returns {@code false} for ElatusDev-origin
     * requests and any request where the attribute is not set.
     *
     * @param request the HTTP request
     * @return {@code true} if the request is identified as AkademiaPlus origin
     */
    private boolean isAkademiaPlusOrigin(HttpServletRequest request) {
        Object origin = request.getAttribute(ATTR_APP_ORIGIN);
        return ORIGIN_AKADEMIA_PLUS.equals(origin);
    }

    /**
     * Extracts the client IP address from the request.
     *
     * <p>Prefers the first IP in the {@code X-Forwarded-For} header chain
     * (set by reverse proxies). Falls back to {@code request.getRemoteAddr()}
     * for direct connections.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes a 403 Forbidden JSON response for rejected IP addresses.
     *
     * @param response the HTTP response
     * @throws IOException if writing the response fails
     */
    private void rejectRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> errorBody = Map.of(
                JSON_FIELD_CODE, CODE_IP_REJECTED,
                JSON_FIELD_MESSAGE, ERROR_IP_NOT_ALLOWED
        );

        objectMapper.writeValue(response.getWriter(), errorBody);
    }
}
```

#### Step 3.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 3.3: Commit

```
feat(security): implement IpWhitelistFilter for AkademiaPlus origin

Add OncePerRequestFilter at @Order(1) that enforces IP-based access
control for AkademiaPlus-origin requests. Extracts client IP from
X-Forwarded-For header. Returns 403 JSON error for rejected IPs.
Bypasses health, actuator, and Swagger endpoints.
```

---

### Phase 4: Integration with SecurityConfig

#### Step 4.1: Register IpWhitelistFilter in SecurityConfig

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add the `IpWhitelistFilter` to the filter chain before the `JwtRequestFilter`:

```java
.addFilterBefore(ipWhitelistFilter, JwtRequestFilter.class)
.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
```

Add `IpWhitelistFilter` as a parameter to the `securityFilterChain` method:

```java
public SecurityFilterChain securityFilterChain(
        Set<ModuleSecurityConfigurator> moduleSecurityConfigurators,
        HttpSecurity http,
        JwtRequestFilter jwtRequestFilter,
        IpWhitelistFilter ipWhitelistFilter) throws Exception {
```

#### Step 4.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 4.3: Commit

```
feat(security): register IpWhitelistFilter in SecurityConfig

Add IpWhitelistFilter before JwtRequestFilter in the Spring Security
filter chain, ensuring IP validation runs before JWT processing.
```

---

### Phase 5: Unit Tests

#### Step 5.1: IpWhitelistFilterTest

**File**: `security/src/test/java/com/akademiaplus/config/IpWhitelistFilterTest.java`

```java
@ExtendWith(MockitoExtension.class)
class IpWhitelistFilterTest {

    // Constants
    public static final String ALLOWED_IP = "192.168.1.100";
    public static final String BLOCKED_IP = "203.0.113.50";
    public static final String CIDR_RANGE = "192.168.1.0/24";

    @Mock private IpWhitelistProperties properties;
    @Mock private ObjectMapper objectMapper;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private IpWhitelistFilter filter;

    @Nested
    @DisplayName("AkademiaPlus origin — allowed IP")
    class AkademiaPlusAllowedIp {

        @Test
        @DisplayName("Should pass filter when IP is within allowed CIDR range")
        void shouldPassFilter_whenIpIsWithinAllowedCidr() { ... }

        @Test
        @DisplayName("Should extract first IP from X-Forwarded-For header")
        void shouldExtractFirstIp_whenXForwardedForHasMultipleIps() { ... }

        @Test
        @DisplayName("Should use remoteAddr when X-Forwarded-For is absent")
        void shouldUseRemoteAddr_whenXForwardedForIsAbsent() { ... }
    }

    @Nested
    @DisplayName("AkademiaPlus origin — blocked IP")
    class AkademiaPlusBlockedIp {

        @Test
        @DisplayName("Should return 403 when IP is not in allowed CIDR ranges")
        void shouldReturn403_whenIpIsNotInAllowedCidrs() { ... }

        @Test
        @DisplayName("Should write JSON error body when IP is rejected")
        void shouldWriteJsonErrorBody_whenIpIsRejected() { ... }

        @Test
        @DisplayName("Should not invoke filter chain when IP is rejected")
        void shouldNotInvokeFilterChain_whenIpIsRejected() { ... }
    }

    @Nested
    @DisplayName("Non-AkademiaPlus origin")
    class NonAkademiaPlusOrigin {

        @Test
        @DisplayName("Should pass filter when origin is not AkademiaPlus")
        void shouldPassFilter_whenOriginIsNotAkademiaPlus() { ... }

        @Test
        @DisplayName("Should pass filter when origin attribute is not set")
        void shouldPassFilter_whenOriginAttributeIsNotSet() { ... }
    }

    @Nested
    @DisplayName("Bypass paths")
    class BypassPaths {

        @Test
        @DisplayName("Should skip filter for actuator endpoints")
        void shouldSkipFilter_whenPathIsActuator() { ... }

        @Test
        @DisplayName("Should skip filter for swagger endpoints")
        void shouldSkipFilter_whenPathIsSwagger() { ... }

        @Test
        @DisplayName("Should skip filter for api-docs endpoints")
        void shouldSkipFilter_whenPathIsApiDocs() { ... }
    }
}
```

#### Step 5.2: IpWhitelistPropertiesTest

**File**: `security/src/test/java/com/akademiaplus/config/IpWhitelistPropertiesTest.java`

```java
class IpWhitelistPropertiesTest {

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("Should return empty list when no CIDRs are configured")
        void shouldReturnEmptyList_whenNoCidrsConfigured() { ... }
    }

    @Nested
    @DisplayName("Configured values")
    class ConfiguredValues {

        @Test
        @DisplayName("Should return configured CIDR list")
        void shouldReturnConfiguredCidrList_whenCidrsAreSet() { ... }
    }
}
```

#### Step 5.3: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 5.4: Commit

```
test(security): add IP whitelist filter and properties unit tests

IpWhitelistFilterTest — covers allowed IP, blocked IP (403),
non-AkademiaPlus origin passthrough, X-Forwarded-For extraction,
and bypass path handling.
IpWhitelistPropertiesTest — covers default and configured values.
```

---

### Phase 6: Component Tests

#### Step 6.1: IpWhitelistComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/IpWhitelistComponentTest.java`

Extends `AbstractIntegrationTest`. Full Spring context + Testcontainers MariaDB.

```java
@Nested
@DisplayName("AkademiaPlus origin with whitelisted IP")
class AllowedIp {

    @Test
    @DisplayName("Should return 200 when IP is in allowed CIDR range")
    void shouldReturn200_whenIpIsInAllowedCidrRange() { ... }
}

@Nested
@DisplayName("AkademiaPlus origin with non-whitelisted IP")
class BlockedIp {

    @Test
    @DisplayName("Should return 403 with JSON error when IP is not in allowed CIDR range")
    void shouldReturn403WithJsonError_whenIpIsNotInAllowedCidrRange() { ... }

    @Test
    @DisplayName("Should include IP_NOT_WHITELISTED error code in response")
    void shouldIncludeIpNotWhitelistedCode_whenIpIsRejected() { ... }
}

@Nested
@DisplayName("ElatusDev origin — no IP restriction")
class ElatusDevOrigin {

    @Test
    @DisplayName("Should return 200 regardless of IP when origin is ElatusDev")
    void shouldReturn200_whenOriginIsElatusDevRegardlessOfIp() { ... }
}
```

**Note**: Component tests require setting the `app-origin` request attribute to simulate the branching-security-filter behavior. Use `MockMvc`'s `requestAttr()` method or a test-only filter to set the attribute.

#### Step 6.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

#### Step 6.3: Commit

```
test(application): add IP whitelist component test

IpWhitelistComponentTest — full Spring context + Testcontainers
MariaDB. Covers allowed IP, blocked IP (403 + JSON error body),
and ElatusDev origin passthrough (no IP restriction).
```

---

## 5. Key Design Decisions

### 5.1 Filter Order

| Option | @Order | Pros | Cons |
|--------|--------|------|------|
| **Before JWT (@Order(1))** | 1 | Rejects unauthorized IPs before JWT parsing — lower resource consumption | Requires branching-security-filter to also run before JWT |
| After JWT (@Order(4)) | 4 | Simpler — can access authenticated user context | Wastes JWT validation on rejected IPs |

**Decision**: `@Order(1)` — fail-fast pattern. IP whitelist is a network-level control that should reject traffic as early as possible.

### 5.2 X-Forwarded-For Trust

| Option | Pros | Cons |
|--------|------|------|
| **Take first IP (leftmost)** | Standard convention — original client IP | Can be spoofed if reverse proxy doesn't strip/overwrite |
| Take last IP (rightmost) | Set by the last trusted proxy | Requires knowing the number of trusted proxies |
| Custom header (e.g., X-Real-IP) | Unambiguous | Non-standard, requires proxy configuration |

**Decision**: Take the first IP from `X-Forwarded-For`. The deployment architecture uses a single reverse proxy (AWS ALB/Nginx) that overwrites the header, making the first IP reliable.

### 5.3 CidrMatcher Placement

| Option | Module | Pros | Cons |
|--------|--------|------|------|
| **utilities** | utilities | Reusable across modules, no Spring dependency | Adds a class to the base module |
| security | security | Co-located with the filter | Not reusable if other modules need IP matching |
| infra-common | infra-common | Infrastructure utility | Wrong abstraction level |

**Decision**: `utilities` module — `CidrMatcher` is a pure utility with no Spring dependencies, consistent with other utilities like `HashingService` and `PiiNormalizer`.

### 5.4 Empty CIDR List Behavior

| Option | Behavior | Pros | Cons |
|--------|----------|------|------|
| **Block all** | No CIDRs configured → all AkademiaPlus requests rejected | Fail-secure — forces explicit configuration | May confuse during initial setup |
| Allow all | No CIDRs configured → all requests pass | Easier initial setup | Insecure default |

**Decision**: Block all (fail-secure). An empty `allowedCidrs` list means `CidrMatcher.isAllowed()` returns `false` for every IP. This ensures that AkademiaPlus IP whitelisting is never accidentally disabled by missing configuration.

---

## 6. Multi-Tenancy Considerations

The IP whitelist filter operates **before** tenant context is established (it runs at `@Order(1)`, before `TenantContextLoader` at `@Order(-50)` in the servlet filter chain, and before `JwtRequestFilter` at `@Order(3)` in the security filter chain). Therefore:

1. **CIDR ranges are global** — they apply to all tenants served by the AkademiaPlus web app. A single school deployment serves one tenant, but the API-level CIDR list applies uniformly.

2. **Per-tenant CIDR ranges** (future): If different schools need different IP ranges, this would require:
   - Extracting `tenantId` from a request header or JWT claim before the whitelist check
   - Storing CIDR ranges per tenant in the database
   - Moving the filter after JWT validation or using a lightweight tenant extraction mechanism

3. **Current design**: All AkademiaPlus schools share the same CIDR configuration. This is acceptable for the initial deployment where all schools connect through a single VPN or known set of IP ranges.

---

## 7. Future Extensibility

1. **Per-tenant CIDR ranges**: Store CIDR ranges in a `TenantIpWhitelist` table and lookup by tenant ID at filter time.
2. **Dynamic reload**: Use `@RefreshScope` or a scheduled task to reload CIDR ranges without restart.
3. **Rate limiting integration**: Combine IP whitelist with rate limiting — whitelisted IPs get higher rate limits.
4. **GeoIP blocking**: Extend `CidrMatcher` with MaxMind GeoIP2 for country-level blocking.
5. **Audit trail**: Log all rejected IPs to the audit system for security monitoring.

---

## 8. Verification Checklist

- [ ] `mvn clean compile -pl utilities -am -DskipTests` — CidrMatcher compiles
- [ ] `mvn test -pl utilities` — CidrMatcher tests pass
- [ ] `mvn clean compile -pl security -am -DskipTests` — filter + properties compile
- [ ] `mvn test -pl security` — filter + properties tests pass
- [ ] `mvn clean compile -pl application -am -DskipTests` — full compilation
- [ ] `mvn verify -pl application` — component tests pass
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] `CidrMatcher` is in `utilities` module with no Spring dependencies
- [ ] Filter runs at `@Order(1)` — before JWT filter
- [ ] 403 response includes JSON body with `code` and `message` fields
- [ ] `X-Forwarded-For` handling takes first IP in chain
- [ ] Empty CIDR list blocks all AkademiaPlus requests (fail-secure)
- [ ] ElatusDev-origin requests pass through unconditionally
- [ ] Health/actuator/swagger endpoints bypass the filter

---

## 9. Critical Reminders

1. **Branching-security-filter dependency**: This filter depends on the `app-origin` request attribute being set by the branching-security-filter. If that feature is not yet implemented, the filter will pass all requests through (since `isAkademiaPlusOrigin()` returns `false` when the attribute is absent). This is safe — the filter only activates when the branching-security-filter is deployed.

2. **X-Forwarded-For spoofing**: The reverse proxy (ALB/Nginx) MUST be configured to overwrite (not append to) the `X-Forwarded-For` header. Without this, clients can spoof the header to bypass the whitelist.

3. **IPv6 support**: `CidrMatcher` supports IPv6 ranges. Ensure the deployment infrastructure forwards IPv6 addresses correctly if IPv6 traffic is expected.

4. **Dev profile safety**: The `application-dev.properties` sets `0.0.0.0/0` and `::/0` to allow all IPs during development. These values MUST NOT be used in production profiles.

5. **Filter registration**: The filter is `@Component` + `@Order(1)` and is also registered in `SecurityConfig.addFilterBefore()`. The `@Order` annotation controls priority within the security filter chain. Verify that the filter does not run twice (once as a servlet filter and once as a security filter).

6. **`ObjectMapper` injection**: The `IpWhitelistFilter` requires a Jackson `ObjectMapper` for JSON error responses. Spring Boot auto-configures one — do not create a new instance.

7. **No `any()` matchers**: All mock stubbing in tests uses exact parameter values or `ArgumentCaptor`.

8. **Entity instantiation**: This feature creates no entities — `applicationContext.getBean()` is not needed.
