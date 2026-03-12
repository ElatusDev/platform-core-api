# SonarCloud Remediation Workflow — AkademiaPlus

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and the root `pom.xml` before starting.
**Prompt**: `docs/prompts/pending/sonarcloud-remediation-prompt.md`
**Related**: `docs/workflows/pending/sonarqube-verification-workflow.md`, `docs/quality/sonarqube-java-rules.md`

---

## 1. Report Summary

First SonarCloud analysis of `platform-core-api` (project key: `ElatusDev-platform-core-api`).

| Metric             | Value | Rating |
|--------------------|-------|--------|
| Bugs               | 3     | C      |
| Vulnerabilities    | 0     | A      |
| Security Hotspots  | 24    | —      |
| Code Smells        | 191   | A      |
| Coverage           | —     | —      |
| Duplications       | —     | A      |
| **Quality Gate**   | **FAILED** | Reliability rating C |

The quality gate fails because 3 bugs yield a Reliability rating of C (threshold: A).

---

## 2. Bug Fixes (3) — MUST FIX

### Bug 1: S2119 — `SecureRandom` instance created per call

**File**: `certificate-authority/src/main/java/com/akademiaplus/usecases/domain/TokenManifest.java:142`

**Current code**:

```java
public synchronized void generateToken(String cn) {
    byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
    new SecureRandom().nextBytes(tokenBytes);  // S2119: new instance each call
    ...
}
```

**Root cause**: `SecureRandom` is expensive to seed. Creating a new instance per call wastes entropy and CPU. Sonar flags this as a reliability bug because the first call to a newly-created `SecureRandom` may have reduced entropy on some JVMs.

**Fix**: Extract to a `private static final` field so the instance is seeded once and reused.

```java
private static final SecureRandom SECURE_RANDOM = new SecureRandom();

public synchronized void generateToken(String cn) {
    byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
    SECURE_RANDOM.nextBytes(tokenBytes);
    ...
}
```

**Thread safety**: The method is already `synchronized`, and `SecureRandom` is itself thread-safe, so a static field is safe.

---

### Bug 2: S2142 — `InterruptedException` swallowed

**File**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwksRegistrationRunner.java:98`

**Current code**:

```java
} catch (Exception e) {
    // Non-fatal — trust broker may not be available in all environments
    log.warn("JWKS registration failed (non-fatal): {}", e.getMessage());
}
```

**Root cause**: `HttpClient.send()` throws `InterruptedException`. Catching it inside `catch (Exception)` without calling `Thread.currentThread().interrupt()` silently swallows the interrupt flag, breaking Java's cooperative interruption contract.

**Fix**: Split into two catch blocks — handle `InterruptedException` first with thread re-interruption.

```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    log.warn("JWKS registration interrupted: {}", e.getMessage());
} catch (Exception e) {
    // Non-fatal — trust broker may not be available in all environments
    log.warn("JWKS registration failed (non-fatal): {}", e.getMessage());
}
```

---

### Bug 3: S2583 — Always-true condition

**File**: `certificate-authority/src/main/java/com/akademiaplus/interfaceadapters/controllers/CertificateAuthorityController.java:87`

**Current code**:

```java
String callerIdentity = jwksRegistrationRequestDTO.getKid() != null
        ? jwksRegistrationRequestDTO.getKid()
        : "unknown";
```

**Root cause**: The `kid` field in `JwksRegistrationRequestDTO` is marked `@NotNull` in the OpenAPI schema, so the generated DTO enforces non-null at construction time. The null check is dead code.

**Fix**: Remove the null guard — use `getKid()` directly.

```java
registerJwksUseCase.register(jwksRegistrationRequestDTO,
        jwksRegistrationRequestDTO.getKid());
```

Remove the `callerIdentity` local variable entirely since it only served as null-safe indirection.

---

## 3. Security Hotspot Triage (24)

### 3.1 HIGH Priority (4)

#### S4502 — CSRF disabled (3 instances)

| # | File | Line |
|---|------|------|
| 1 | `CaSecurityConfiguration.java` | 33 |
| 2 | `SecurityConfig.java` | 36 |
| 3 | `SecurityConfig.java` | 107 |

**Assessment**: SAFE — Intentional.

- Instance 1 (`CaSecurityConfiguration`): Certificate Authority uses mTLS for authentication. No browser interaction, no cookies, no session — CSRF is not applicable. Stateless M2M API.
- Instance 2 (`SecurityConfig`, dev/local profile): Platform API uses stateless JWT Bearer tokens. CSRF protection is only relevant for cookie-based session authentication — not applicable here.
- Instance 3 (`SecurityConfig`, mock-data-service profile): Internal infrastructure service, never exposed to browsers.

**Action**: Add `@SuppressWarnings("java:S4502")` on each method with a justification comment.

```java
@SuppressWarnings("java:S4502") // CSRF disabled: stateless JWT Bearer auth, no cookies/sessions
```

#### S2077 — Dynamic SQL in `DataCleanUp`

**File**: `mock-data-service/src/main/java/com/akademiaplus/util/base/DataCleanUp.java:44`

**Current code**:

```java
String sql = "ALTER TABLE " + getTableName(dataModel) + " AUTO_INCREMENT = 1";
entityManager.createNativeQuery(sql).executeUpdate();
```

**Assessment**: LOW RISK — The table name comes from `@Table` annotation metadata, not user input. However, as belt-and-suspenders, add regex validation.

**Action**: Add a validation regex in `getTableName()` to reject non-identifier characters.

```java
private static final Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

private String getTableName(Class<?> entityClass) {
    Table tableAnnotation = entityClass.getAnnotation(Table.class);
    if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
        String name = tableAnnotation.name();
        if (!VALID_TABLE_NAME.matcher(name).matches()) {
            throw new UnprocessableDataModelException("Invalid table name: " + name);
        }
        return name;
    }
    throw new UnprocessableDataModelException(ANNOTATION_MISSING);
}
```

Then add `@SuppressWarnings("java:S2077")` on the `clean()` method with a comment explaining that the table name is validated and sourced from `@Table` annotation metadata only.

---

### 3.2 MEDIUM Priority (13)

#### S2245 — `java.util.Random` in mock-data generators (13 instances)

**Assessment**: SAFE — These are mock-data generators that produce randomized test data (names, addresses, amounts). They do not generate security-sensitive values (tokens, keys, passwords). `java.util.Random` is appropriate here; `SecureRandom` would add unnecessary overhead.

**Action**: Add `@SuppressWarnings("java:S2245")` on each class or method with a comment.

```java
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
```

---

### 3.3 LOW Priority (8)

#### S5122 — CORS wildcard (1 instance)

**File**: `SecurityConfig.java:129`

**Current code**:

```java
corsConfig.setAllowedOriginPatterns(List.of("*"));
```

**Assessment**: SAFE — This is the `mock-data-service` profile's CORS configuration. The mock-data service is internal CI/dev infrastructure, never exposed to the public internet.

**Action**: Add a comment documenting the justification. No code change needed — the existing Javadoc on `corsConfigurationSourceForMockDataService()` already explains it.

#### S7637 — Non-pinned GitHub Actions (3 instances)

**File**: `.github/workflows/docker-deploy-aws.yml`

| Action | Current | Pinned SHA |
|--------|---------|-----------|
| `actions/checkout@v4` | `@v4` | `@34e114876b0b11c390a56381ad16ebd13914f8d5` |
| `actions/setup-java@v4` | `@v4` | `@c1e323688fd81a25caa38c78aa6df2d33d3e20d9` |
| `appleboy/ssh-action@v1` | `@v1` | `@0ff4204d59e8e51228ff73bce53f80d53301dee2` |

**Action**: FIX — Pin all three actions to their full commit SHA with a version comment.

```yaml
- uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4
- uses: actions/setup-java@c1e323688fd81a25caa38c78aa6df2d33d3e20d9 # v4
- uses: appleboy/ssh-action@0ff4204d59e8e51228ff73bce53f80d53301dee2 # v1
```

#### S7636 — Secrets in `run` blocks (2 instances)

**File**: `.github/workflows/docker-deploy-aws.yml:30,55`

**Current code**:

```yaml
docker login -u ${{ secrets.DOCKERHUB_USER }} -p ${{ secrets.DOCKERHUB_TOKEN }} docker.io
```

**Risk**: Secrets used inline in `run` commands can leak into process tables and shell history.

**Action**: FIX — Use `env:` block and `--password-stdin`.

```yaml
- name: Build and push Docker image
  env:
    DOCKERHUB_USER: ${{ secrets.DOCKERHUB_USER }}
    DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
  run: |
    echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin docker.io
    ...
```

Apply the same pattern to the deploy job's `docker login` (line 55), passing secrets via `env:` in the `appleboy/ssh-action` `with:` block.

---

## 4. Code Smell Remediation (191)

### 4.1 S5853 — Multiple assertions should be chained (53 instances)

**Pattern**: Sonar flags consecutive `assertThat()` calls on the same subject as separate assertions.

**Fix**: Chain multiple assertions into a single `assertThat()` call using `satisfies()` or fluent methods.

```java
// BEFORE
assertThat(result.getName()).isEqualTo("foo");
assertThat(result.getAge()).isEqualTo(25);

// AFTER
assertThat(result)
    .extracting("name", "age")
    .containsExactly("foo", 25);

// OR for typed assertions:
assertThat(result).satisfies(r -> {
    assertThat(r.getName()).isEqualTo("foo");
    assertThat(r.getAge()).isEqualTo(25);
});
```

**Note**: If two `assertThat` calls are on *different* subjects, they should remain separate — Sonar only flags when the subject is the same or the assertions can be chained.

---

### 4.2 S1874 — Deprecated `Locale` constructor (26 instances)

**Pattern**: `new Locale("es")` and similar are deprecated since Java 19.

**Fix**: Replace with `Locale.of()`.

```java
// BEFORE
new Locale("es")
new Locale("en", "US")

// AFTER
Locale.of("es")
Locale.of("en", "US")
```

---

### 4.3 S1128 — Unused imports (21 instances)

**Fix**: Delete the import line. IDE auto-fix or manual removal.

---

### 4.4 S1130 — Declared checked exception not thrown (13 instances)

**Pattern**: Method declares `throws SomeException` but never actually throws it.

**Fix**: Remove the exception from the `throws` clause.

```java
// BEFORE
public void doThing() throws IOException { ... }

// AFTER (if IOException is never thrown)
public void doThing() { ... }
```

**Caution**: If the method overrides an interface method that declares the exception, the `throws` clause is inherited — check the parent signature. If the interface declares it, the override may keep it for compatibility.

---

### 4.5 S1068 — Unused private fields (11 instances)

**Fix**: Delete the field declaration. Verify no reflection-based access (e.g., `@JsonProperty`, `@Value`).

---

### 4.6 S7467 — Named catch variable unused (8 instances)

**Pattern**: `catch (SomeException e)` where `e` is never referenced.

**Fix**: Use the unnamed variable pattern (Java 22+).

```java
// BEFORE
catch (SomeException e) { ... }

// AFTER
catch (SomeException _) { ... }
```

---

### 4.7 S112 — Generic `throws Exception` (8 instances)

**Pattern**: Method signature uses `throws Exception` instead of specific exceptions.

**Fix**: Replace with the most specific exception(s) actually thrown, or remove if nothing is thrown.

```java
// BEFORE
public void process() throws Exception { ... }

// AFTER
public void process() throws IOException, IllegalArgumentException { ... }
```

**Note**: Some framework callbacks (e.g., Spring's `ApplicationRunner.run()`) legitimately declare `throws Exception` — in those cases, suppress with `@SuppressWarnings("java:S112")`.

---

### 4.8 S106 — `System.out.println` (7 instances)

**Fix**: Replace with SLF4J logger via `@Slf4j` annotation.

```java
// BEFORE
System.out.println("Processing: " + item);

// AFTER
log.info("Processing: {}", item);
```

---

### 4.9 S5838 — AssertJ style (5 instances)

**Pattern**: Use AssertJ's dedicated assertion methods instead of generic comparisons.

```java
// BEFORE
assertThat(list.size()).isGreaterThanOrEqualTo(5);

// AFTER
assertThat(list).hasSizeGreaterThanOrEqualTo(5);
```

---

### 4.10 S3011 — Reflection accessibility (4 instances)

**Assessment**: Justified — `HibernateStateUpdater` and `TenantPreInsertEventListener` require reflective access to Hibernate internals by design.

**Action**: Suppress with `@SuppressWarnings("java:S3011")` and justification comment.

---

### 4.11 S1481 — Unused local variables (4 instances)

**Fix**: Delete the variable. If the expression has side effects, keep the expression but remove the assignment.

---

### 4.12 S2589 — Always-true/false conditions (4 instances)

**Fix**: Remove the dead branch or fix the logic. Analyze each case individually — may indicate a real logic error.

---

### 4.13 S119 — Generic type parameter name too long (3 instances)

**Assessment**: Evaluate case-by-case. Descriptive names like `ENTITY` may be intentional for readability in the codebase.

**Action**: If the name is intentionally descriptive (e.g., `<ENTITY extends BaseModel>`), suppress with `@SuppressWarnings("java:S119")`. If it's just a long name with no added clarity, shorten to conventional single-letter (e.g., `T`, `E`).

---

### 4.14 S1854 — Dead stores (3 instances)

**Fix**: Remove the useless assignment. If the variable is used later, move the assignment to where it's first needed.

---

### 4.15 S5778 — Lambda with multiple throwables (2 instances)

**Action**: Extract the lambda body to a named method, or suppress if the throwables are framework-mandated.

---

### 4.16 S5961 — Too many assertions in test (2 instances)

**Assessment**: Justified — these are component-level tests that intentionally verify multiple aspects of a complex operation in a single test method.

**Action**: Suppress with `@SuppressWarnings("java:S5961")`.

---

### 4.17 S6201 — `instanceof` without pattern (2 instances)

**Fix**: Use pattern matching (Java 16+).

```java
// BEFORE
if (obj instanceof String) {
    String s = (String) obj;
    ...
}

// AFTER
if (obj instanceof String s) {
    ...
}
```

---

### 4.18 S1602 — Unnecessary lambda braces (2 instances)

**Fix**: Remove `{ }` around single-statement lambda.

```java
// BEFORE
list.forEach(item -> { process(item); });

// AFTER
list.forEach(item -> process(item));
```

---

### 4.19 S3776 — Cognitive complexity > 15 (2 instances)

**Fix**: Extract helper methods to reduce nesting depth and decision points. Target cognitive complexity < 10 per `AI-CODE-REF.md`.

---

### 4.20 Remaining Rules (6 individual instances)

| Rule | Count | Description | Action |
|------|-------|-------------|--------|
| S6877 | 1 | `Collections.reverse` | Use `.reversed()` (Java 21+) |
| S4488 | 1 | `@RequestMapping(POST)` | Replace with `@PostMapping` |
| S1192 | 1 | `"ES384"` duplicated 3x | Extract to `public static final String` constant |
| S107 | 1 | Constructor with too many params | Suppress — Spring DI controllers legitimately have many injected dependencies |
| S135 | 1 | Multiple loop break/continue | Refactor or suppress |
| S3012 | 1 | Return value ignored | Use or remove |
| S2386 | 1 | Mutable public field | Make private + accessor |
| S3457 | 1 | Printf format | Fix format string |
| S6068 | 1 | Call to deprecated method | Update to non-deprecated API |
| S1186 | 1 | Empty method body | Add comment or `// no-op` |
| S7027 | 1 | Package cycle | Acknowledged — legacy `com.makani` module |

---

## 5. GitHub Actions Hardening

### 5.1 Pin Actions to Full Commit SHA (S7637)

**File**: `.github/workflows/docker-deploy-aws.yml`

| Line | Current | Fixed |
|------|---------|-------|
| 16 | `actions/checkout@v4` | `actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4` |
| 21 | `actions/setup-java@v4` | `actions/setup-java@c1e323688fd81a25caa38c78aa6df2d33d3e20d9 # v4` |
| 46 | `appleboy/ssh-action@v1` | `appleboy/ssh-action@0ff4204d59e8e51228ff73bce53f80d53301dee2 # v1` |

### 5.2 Secrets via `env:` Block + `--password-stdin` (S7636)

**Build job (line 28-30)** — BEFORE:

```yaml
- name: Build and push Docker image
  id: docker-build-push
  run: |
    docker login -u ${{ secrets.DOCKERHUB_USER }} -p ${{ secrets.DOCKERHUB_TOKEN }} docker.io
```

**AFTER**:

```yaml
- name: Build and push Docker image
  id: docker-build-push
  env:
    DOCKERHUB_USER: ${{ secrets.DOCKERHUB_USER }}
    DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
  run: |
    echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin docker.io
```

**Deploy job (line 45-55)** — The `appleboy/ssh-action` `script` block runs on the remote EC2 instance. Secrets referenced inside `script:` are interpolated by GitHub Actions before being sent via SSH — they appear in the SSH command stream. The fix is to pass them as environment variables via the action's `envs` parameter:

```yaml
- name: Deploy to EC2
  uses: appleboy/ssh-action@0ff4204d59e8e51228ff73bce53f80d53301dee2 # v1
  with:
    host: ${{ secrets.SERVER_HOST }}
    username: ${{ secrets.SERVER_USER }}
    key: ${{ secrets.SERVER_SSH_KEY }}
    envs: DOCKERHUB_USER,DOCKERHUB_TOKEN
    script: |
      echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin docker.io
      docker pull ${{ needs.build-and-push.outputs.image_tag }}
      docker stop platform-core-api || true
      docker rm platform-core-api || true
      docker run -d --name platform-core-api -p 8080:8080 ${{ needs.build-and-push.outputs.image_tag }}
  env:
    DOCKERHUB_USER: ${{ secrets.DOCKERHUB_USER }}
    DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
```

### 5.3 Update JDK Version

The workflow currently uses JDK 24 (`java-version: '24'`). Update to 25 to match `build.yml` and `maven.compiler.release`.

---

## 6. Verification

### 6.1 Per-Phase Verification

Each phase in the prompt has a verification command. Run it before proceeding to the next phase.

### 6.2 Full Build

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

# All tests must pass
mvn clean verify -DskipITs
```

### 6.3 Re-run Sonar Analysis

```bash
export $(grep SONAR_TOKEN .env) && mvn clean verify -DskipITs \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=ElatusDev-platform-core-api
```

### 6.4 Dashboard Verification

Open https://sonarcloud.io/dashboard?id=ElatusDev-platform-core-api and verify:

- [ ] 0 bugs (Reliability rating A)
- [ ] 0 vulnerabilities (Security rating A)
- [ ] 0 unreviewed security hotspots
- [ ] Code smells reduced to suppressed-only remainder
- [ ] **Quality gate: PASSED**

### 6.5 Expected Outcome

| Metric | Before | After |
|--------|--------|-------|
| Bugs | 3 (C) | 0 (A) |
| Vulnerabilities | 0 (A) | 0 (A) |
| Security Hotspots | 24 unreviewed | 0 unreviewed |
| Code Smells | 191 | < 20 (suppressed only) |
| Quality Gate | FAILED | PASSED |
