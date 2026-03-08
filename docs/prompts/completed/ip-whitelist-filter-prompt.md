# IP Whitelist Filter — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/ip-whitelist-filter-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 → 2 → ... → 6). Do NOT skip ahead.
2. Before writing any code, read the existing files listed in each phase's "Read first" section.
3. **Compile gate**: After each phase that produces code, run the specified verification command. Fix all errors before proceeding.
4. **Test gate**: After each phase that creates tests, run the specified test command. Fix all failures before proceeding.
5. All new files MUST include the ElatusDev copyright header (2026).
6. All `public` classes and methods MUST have Javadoc.
7. Test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals → `public static final` constants, shared between impl and tests.
9. Read existing files BEFORE modifying — field names, import paths, and class names vary.
10. Commit after each phase using the commit message provided.

---

## Phase 1: CidrMatcher Utility + Unit Tests

### Read first

```bash
cat utilities/src/main/java/com/akademiaplus/utilities/security/HashingService.java
cat utilities/src/main/java/com/akademiaplus/utilities/security/PiiNormalizer.java
ls utilities/src/main/java/com/akademiaplus/utilities/
ls utilities/src/test/java/com/akademiaplus/utilities/
```

Understand the existing utility class patterns — no Spring annotations, pure Java, `public static final` constants for error messages.

### Step 1.1: Create directory structure

```bash
mkdir -p utilities/src/main/java/com/akademiaplus/utilities/network
mkdir -p utilities/src/test/java/com/akademiaplus/utilities/network
```

### Step 1.2: Create CidrMatcher

**File**: `utilities/src/main/java/com/akademiaplus/utilities/network/CidrMatcher.java`

- `public final class` with `private` constructor (utility class pattern)
- No Spring dependencies — pure `java.net.InetAddress`
- `public static boolean isAllowed(String clientIp, List<String> cidrRanges)` — returns `true` if IP falls within any CIDR range
- Handles both IPv4 and IPv6 addresses
- Single IP without prefix treated as `/32` (IPv4) or `/128` (IPv6)
- Bit manipulation for prefix matching — compare `n` bytes fully, then mask remaining bits
- `public static final String ERROR_INVALID_IP` and `ERROR_INVALID_CIDR` constants
- Throws `IllegalArgumentException` for malformed IPs or CIDRs

### Step 1.3: Create CidrMatcherTest

**File**: `utilities/src/test/java/com/akademiaplus/utilities/network/CidrMatcherTest.java`

Constants for all test values — no raw string literals:

```java
public static final String CIDR_CLASS_C = "192.168.1.0/24";
public static final String CIDR_CLASS_A = "10.0.0.0/8";
public static final String CIDR_SINGLE_HOST = "172.16.0.1/32";
public static final String IP_WITHIN_CLASS_C = "192.168.1.100";
public static final String IP_OUTSIDE_CLASS_C = "192.168.2.1";
public static final String IP_WITHIN_CLASS_A = "10.255.255.255";
public static final String IP_SINGLE_HOST_MATCH = "172.16.0.1";
public static final String IP_SINGLE_HOST_NO_MATCH = "172.16.0.2";
public static final String IP_MALFORMED = "not.an.ip.address";
public static final String CIDR_MALFORMED = "192.168.1.0/abc";
```

`@Nested` classes:

| @Nested | Tests |
|---------|-------|
| `Ipv4Matching` | `shouldReturnTrue_whenIpIsWithinCidr24`, `shouldReturnFalse_whenIpIsOutsideCidr24`, `shouldReturnTrue_whenIpMatchesSingleHost`, `shouldReturnFalse_whenIpDoesNotMatchSingleHost`, `shouldReturnTrue_whenIpMatchesAnyRangeInList`, `shouldReturnFalse_whenNoRangesMatch`, `shouldReturnFalse_whenCidrListIsEmpty` |
| `ErrorHandling` | `shouldThrowIllegalArgumentException_whenIpIsMalformed`, `shouldThrowIllegalArgumentException_whenCidrIsMalformed` |

Given-When-Then pattern for each test:

```java
@Test
@DisplayName("Should return true when IP is within /24 CIDR range")
void shouldReturnTrue_whenIpIsWithinCidr24() {
    // Given
    List<String> cidrs = List.of(CIDR_CLASS_C);

    // When
    boolean result = CidrMatcher.isAllowed(IP_WITHIN_CLASS_C, cidrs);

    // Then
    assertThat(result).isTrue();
}
```

### Step 1.4: Compile + test

```bash
mvn clean compile -pl utilities -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl utilities -f platform-core-api/pom.xml
```

### Step 1.5: Commit

```bash
git add utilities/src/main/java/com/akademiaplus/utilities/network/ utilities/src/test/java/com/akademiaplus/utilities/network/
git commit -m "feat(utilities): add CidrMatcher utility for IP-to-CIDR validation

Add CidrMatcher utility class supporting IPv4 and IPv6 address
matching against CIDR ranges. Includes comprehensive unit tests
for range matching, single-host matching, and error handling."
```

---

## Phase 2: IpWhitelistProperties Configuration

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
cat application/src/main/resources/application.properties
cat application/src/main/resources/application-dev.properties
```

Look for existing `@ConfigurationProperties` classes in the security module:
```bash
grep -rn "@ConfigurationProperties" security/src/main/java/
```

### Step 2.1: Create IpWhitelistProperties

**File**: `security/src/main/java/com/akademiaplus/config/IpWhitelistProperties.java`

- `@ConfigurationProperties(prefix = "security.akademia")`
- Field: `private List<String> allowedCidrs = List.of()`
- Standard getter/setter
- Javadoc with example configuration
- Copyright header (2026)

### Step 2.2: Enable ConfigurationProperties scanning

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add `@EnableConfigurationProperties(IpWhitelistProperties.class)` to the class.

**IMPORTANT**: Read the file first — check if `@EnableConfigurationProperties` already exists with other classes. If so, add `IpWhitelistProperties.class` to the existing annotation's value array.

### Step 2.3: Add CIDR configuration to application properties

**File**: `application/src/main/resources/application.properties`

Add at the end:
```properties
# IP Whitelist — AkademiaPlus origin restriction
security.akademia.allowed-cidrs=
```

**File**: `application/src/main/resources/application-dev.properties`

Add at the end:
```properties
# Dev: allow all IPs for local development
security.akademia.allowed-cidrs[0]=0.0.0.0/0
security.akademia.allowed-cidrs[1]=::/0
```

### Step 2.4: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 2.5: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/IpWhitelistProperties.java security/src/main/java/com/akademiaplus/config/SecurityConfig.java application/src/main/resources/
git commit -m "feat(security): add IpWhitelistProperties configuration

Add @ConfigurationProperties for security.akademia.allowed-cidrs
binding. Configure dev profile to allow all IPs. Enable
configuration properties scanning in SecurityConfig."
```

---

## Phase 3: IpWhitelistFilter Implementation

### Read first

```bash
cat security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilter.java
cat infra-common/src/main/java/com/akademiaplus/infra/persistence/config/TenantContextLoader.java
cat security/src/main/java/com/akademiaplus/config/IpWhitelistProperties.java
```

Understand:
- How `JwtRequestFilter` extends `OncePerRequestFilter` and uses `@Order(3)`
- How `TenantContextLoader` uses `shouldNotFilter()` for bypass paths
- The filter chain ordering pattern

### Step 3.1: Create IpWhitelistFilter

**File**: `security/src/main/java/com/akademiaplus/config/IpWhitelistFilter.java`

- `@Component @Order(1)` — runs before JWT filter
- Extends `OncePerRequestFilter`
- Constructor-injected: `IpWhitelistProperties`, `ObjectMapper`
- `shouldNotFilter()`: bypass `/actuator`, `/v3/api-docs`, `/swagger-ui` paths
- `doFilterInternal()`:
  1. Check `isAkademiaPlusOrigin(request)` — if false, pass through
  2. Extract client IP via `extractClientIp(request)` — `X-Forwarded-For` first IP, or `getRemoteAddr()`
  3. Check `CidrMatcher.isAllowed(clientIp, properties.getAllowedCidrs())`
  4. If allowed → `chain.doFilter()`
  5. If rejected → `rejectRequest(response)` — 403 + JSON body

**Constants** (all `public static final`):
```java
public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
public static final String ATTR_APP_ORIGIN = "app-origin";
public static final String ORIGIN_AKADEMIA_PLUS = "akademia-plus";
public static final String ERROR_IP_NOT_ALLOWED = "Access denied: IP address not in whitelist";
public static final String JSON_FIELD_CODE = "code";
public static final String JSON_FIELD_MESSAGE = "message";
public static final String CODE_IP_REJECTED = "IP_NOT_WHITELISTED";
```

**Private methods**:
- `isAkademiaPlusOrigin(request)`: checks `request.getAttribute(ATTR_APP_ORIGIN)` equals `ORIGIN_AKADEMIA_PLUS`
- `extractClientIp(request)`: prefers `X-Forwarded-For` first IP, falls back to `getRemoteAddr()`
- `rejectRequest(response)`: writes 403 JSON response via `ObjectMapper`

### Step 3.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 3.3: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/IpWhitelistFilter.java
git commit -m "feat(security): implement IpWhitelistFilter for AkademiaPlus origin

Add OncePerRequestFilter at @Order(1) that enforces IP-based access
control for AkademiaPlus-origin requests. Extracts client IP from
X-Forwarded-For header. Returns 403 JSON error for rejected IPs.
Bypasses health, actuator, and Swagger endpoints."
```

---

## Phase 4: Integration with SecurityConfig

### Read first

```bash
cat security/src/main/java/com/akademiaplus/config/SecurityConfig.java
```

Locate the line:
```java
.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
```

### Step 4.1: Add IpWhitelistFilter parameter

Add `IpWhitelistFilter ipWhitelistFilter` to the `securityFilterChain` method signature (for the `dev` and `local` profile bean).

### Step 4.2: Register filter in chain

Add before the existing `addFilterBefore` line:

```java
.addFilterBefore(ipWhitelistFilter, JwtRequestFilter.class)
```

The final chain should be:
```java
.addFilterBefore(ipWhitelistFilter, JwtRequestFilter.class)
.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
```

### Step 4.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

### Step 4.4: Commit

```bash
git add security/src/main/java/com/akademiaplus/config/SecurityConfig.java
git commit -m "feat(security): register IpWhitelistFilter in SecurityConfig

Add IpWhitelistFilter before JwtRequestFilter in the Spring Security
filter chain, ensuring IP validation runs before JWT processing."
```

---

## Phase 5: Unit Tests (Filter + Properties)

### Read first

```bash
cat security/src/test/java/com/akademiaplus/config/SecurityControllerAdviceTest.java 2>/dev/null
cat security/src/test/java/com/akademiaplus/internal/interfaceadapters/jwt/JwtRequestFilterTest.java 2>/dev/null
ls security/src/test/java/com/akademiaplus/
```

Follow the existing test patterns in the security module.

### Step 5.1: Create test directory

```bash
mkdir -p security/src/test/java/com/akademiaplus/config
```

### Step 5.2: IpWhitelistFilterTest

**File**: `security/src/test/java/com/akademiaplus/config/IpWhitelistFilterTest.java`

- `@ExtendWith(MockitoExtension.class)`
- `@Mock IpWhitelistProperties properties`
- `@Mock ObjectMapper objectMapper`
- Mock `HttpServletRequest`, `HttpServletResponse`, `FilterChain` via `@Mock`
- Create filter instance in `@BeforeEach`

**Constants**:
```java
public static final String ALLOWED_IP = "192.168.1.100";
public static final String BLOCKED_IP = "203.0.113.50";
public static final String CIDR_RANGE = "192.168.1.0/24";
public static final String FORWARDED_CHAIN = "192.168.1.100, 10.0.0.1, 172.16.0.1";
public static final String PATH_ACTUATOR = "/actuator/health";
public static final String PATH_SWAGGER = "/swagger-ui/index.html";
public static final String PATH_API_DOCS = "/v3/api-docs/security";
public static final String PATH_API = "/v1/security/login/internal";
```

**@Nested classes**:

| @Nested | Tests |
|---------|-------|
| `AkademiaPlusAllowedIp` | `shouldPassFilter_whenIpIsWithinAllowedCidr`, `shouldExtractFirstIp_whenXForwardedForHasMultipleIps`, `shouldUseRemoteAddr_whenXForwardedForIsAbsent` |
| `AkademiaPlusBlockedIp` | `shouldReturn403_whenIpIsNotInAllowedCidrs`, `shouldWriteJsonErrorBody_whenIpIsRejected`, `shouldNotInvokeFilterChain_whenIpIsRejected` |
| `NonAkademiaPlusOrigin` | `shouldPassFilter_whenOriginIsNotAkademiaPlus`, `shouldPassFilter_whenOriginAttributeIsNotSet` |
| `BypassPaths` | `shouldSkipFilter_whenPathIsActuator`, `shouldSkipFilter_whenPathIsSwagger`, `shouldSkipFilter_whenPathIsApiDocs` |

**Mock setup for AkademiaPlus origin**:
```java
// Given
Mockito.when(request.getAttribute(IpWhitelistFilter.ATTR_APP_ORIGIN))
        .thenReturn(IpWhitelistFilter.ORIGIN_AKADEMIA_PLUS);
Mockito.when(request.getRemoteAddr()).thenReturn(ALLOWED_IP);
Mockito.when(properties.getAllowedCidrs()).thenReturn(List.of(CIDR_RANGE));
```

**IMPORTANT**: No `any()` matchers — use exact parameter values for all stubbing.

**Mock setup for bypass paths** — test `shouldNotFilter()`:
```java
// Given
Mockito.when(request.getRequestURI()).thenReturn(PATH_ACTUATOR);
Mockito.when(request.getContextPath()).thenReturn("");

// When
boolean result = filter.shouldNotFilter(request);

// Then
assertThat(result).isTrue();
```

Note: `shouldNotFilter` is `protected` — you may need to call it directly or use `doFilterInternal` and verify the filter chain is invoked.

### Step 5.3: IpWhitelistPropertiesTest

**File**: `security/src/test/java/com/akademiaplus/config/IpWhitelistPropertiesTest.java`

Minimal test — no Mockito needed:

```java
class IpWhitelistPropertiesTest {

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("Should return empty list when no CIDRs are configured")
        void shouldReturnEmptyList_whenNoCidrsConfigured() {
            // Given
            IpWhitelistProperties properties = new IpWhitelistProperties();

            // When
            List<String> result = properties.getAllowedCidrs();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Configured values")
    class ConfiguredValues {

        @Test
        @DisplayName("Should return configured CIDR list when CIDRs are set")
        void shouldReturnConfiguredCidrList_whenCidrsAreSet() {
            // Given
            IpWhitelistProperties properties = new IpWhitelistProperties();
            List<String> cidrs = List.of("192.168.1.0/24", "10.0.0.0/8");
            properties.setAllowedCidrs(cidrs);

            // When
            List<String> result = properties.getAllowedCidrs();

            // Then
            assertThat(result).containsExactly("192.168.1.0/24", "10.0.0.0/8");
        }
    }
}
```

### Step 5.4: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

### Step 5.5: Commit

```bash
git add security/src/test/java/com/akademiaplus/config/
git commit -m "test(security): add IP whitelist filter and properties unit tests

IpWhitelistFilterTest — covers allowed IP, blocked IP (403),
non-AkademiaPlus origin passthrough, X-Forwarded-For extraction,
and bypass path handling.
IpWhitelistPropertiesTest — covers default and configured values."
```

---

## Phase 6: Component Tests

### Read first

```bash
find application/src/test -name "*ComponentTest.java" | head -5
```

Read the first result to understand the component test infrastructure:
```bash
cat <first-result>
```

Also read:
```bash
find application/src/test -name "AbstractIntegrationTest.java" -o -name "AbstractComponentTest.java" | head -1
cat <result>
```

Understand: `AbstractIntegrationTest`, `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles`, `MockMvc`, `@TestMethodOrder`, Testcontainers setup.

### Step 6.1: IpWhitelistComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/IpWhitelistComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`, `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`
- Uses `@TestPropertySource` to set test CIDR ranges:
  ```java
  @TestPropertySource(properties = {
      "security.akademia.allowed-cidrs[0]=192.168.1.0/24",
      "security.akademia.allowed-cidrs[1]=10.0.0.0/8"
  })
  ```

**Note**: To simulate the `app-origin` request attribute (set by branching-security-filter), use `MockMvc`'s `requestAttr()`:

```java
mockMvc.perform(get("/v1/some-endpoint")
        .requestAttr(IpWhitelistFilter.ATTR_APP_ORIGIN, IpWhitelistFilter.ORIGIN_AKADEMIA_PLUS)
        .header("X-Forwarded-For", "192.168.1.100")
        .header("X-Tenant-Id", String.valueOf(tenantId))
        .header("Authorization", "Bearer " + validJwt))
        .andExpect(status().isOk());
```

**@Nested classes**:

| @Nested | Tests |
|---------|-------|
| `AllowedIp` | `shouldReturn200_whenIpIsInAllowedCidrRange` |
| `BlockedIp` | `shouldReturn403WithJsonError_whenIpIsNotInAllowedCidrRange`, `shouldIncludeIpNotWhitelistedCode_whenIpIsRejected` |
| `ElatusDevOrigin` | `shouldReturn200_whenOriginIsElatusDevRegardlessOfIp` |

**Test for blocked IP**:
```java
@Test
@DisplayName("Should return 403 with JSON error when IP is not in allowed CIDR range")
void shouldReturn403WithJsonError_whenIpIsNotInAllowedCidrRange() throws Exception {
    // Given
    // (AkademiaPlus origin, blocked IP)

    // When
    MvcResult result = mockMvc.perform(get("/v1/some-endpoint")
            .requestAttr(IpWhitelistFilter.ATTR_APP_ORIGIN, IpWhitelistFilter.ORIGIN_AKADEMIA_PLUS)
            .header("X-Forwarded-For", "203.0.113.50"))
            .andExpect(status().isForbidden())
            .andReturn();

    // Then
    String body = result.getResponse().getContentAsString();
    assertThat(body).contains(IpWhitelistFilter.CODE_IP_REJECTED);
    assertThat(body).contains(IpWhitelistFilter.ERROR_IP_NOT_ALLOWED);
}
```

**ElatusDev origin passthrough**:
```java
@Test
@DisplayName("Should return 200 regardless of IP when origin is ElatusDev")
void shouldReturn200_whenOriginIsElatusDevRegardlessOfIp() throws Exception {
    // Given
    // (ElatusDev origin, IP not in whitelist — should still pass)

    // When & Then
    mockMvc.perform(get("/v1/some-endpoint")
            .requestAttr(IpWhitelistFilter.ATTR_APP_ORIGIN, "elatusdev")
            .header("X-Forwarded-For", "203.0.113.50")
            .header("X-Tenant-Id", String.valueOf(tenantId))
            .header("Authorization", "Bearer " + validJwt))
            .andExpect(status().isOk());
}
```

**IMPORTANT**: The component test endpoint must be an existing authenticated endpoint. Read the existing component tests to find which endpoints they test and reuse one. If none are suitable, use an endpoint that returns 200 for authenticated requests.

### Step 6.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

### Step 6.3: Commit

```bash
git add application/src/test/java/com/akademiaplus/usecases/IpWhitelistComponentTest.java
git commit -m "test(application): add IP whitelist component test

IpWhitelistComponentTest — full Spring context + Testcontainers
MariaDB. Covers allowed IP, blocked IP (403 + JSON error body),
and ElatusDev origin passthrough (no IP restriction)."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl utilities -f platform-core-api/pom.xml` — CidrMatcher tests green
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — filter + properties tests green
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests green
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] `CidrMatcher` is a final utility class in `utilities` module with no Spring dependencies
- [ ] `IpWhitelistFilter` runs at `@Order(1)` — before JWT filter (`@Order(3)`)
- [ ] 403 response includes JSON body with `code` ("IP_NOT_WHITELISTED") and `message` fields
- [ ] `X-Forwarded-For` handling takes first IP in comma-separated chain
- [ ] Empty CIDR list blocks all AkademiaPlus requests (fail-secure)
- [ ] ElatusDev-origin requests pass through unconditionally (no IP check)
- [ ] Health/actuator/swagger endpoints bypass the filter
- [ ] No `any()` matchers in any test — all stubbing uses exact values
