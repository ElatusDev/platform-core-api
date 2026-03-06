# SonarCloud Remediation — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Workflow**: `docs/workflows/pending/sonarcloud-remediation-workflow.md`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md` before writing any code.
**Dependency**: SonarCloud analysis completed; `build.yml` CI pipeline configured.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7). Do NOT skip ahead.
2. After EVERY phase, run the specified verification command. Fix failures before proceeding.
3. Do NOT introduce new features, refactor beyond what is specified, or add comments to unchanged code.
4. All `@SuppressWarnings` annotations MUST include a justification comment on the same line or preceding line.
5. Commit after each phase using Conventional Commits format.
6. No `Co-Authored-By` or AI attribution in commit messages.
7. When in doubt about a suppression vs. fix, prefer fixing. Only suppress when the code is intentionally designed that way.

---

## Phase 0: Bug Fixes (3 files)

**Goal**: Fix the 3 bugs that cause the quality gate to fail (Reliability rating C → A).

### Step 0.1 — S2119: Extract `SecureRandom` to static field

**Edit file**: `certificate-authority/src/main/java/com/akademiaplus/usecases/domain/TokenManifest.java`

Add a static field near the top of the class (after existing constants):

```java
private static final SecureRandom SECURE_RANDOM = new SecureRandom();
```

Replace line 142:

**BEFORE**:

```java
new SecureRandom().nextBytes(tokenBytes);
```

**AFTER**:

```java
SECURE_RANDOM.nextBytes(tokenBytes);
```

### Step 0.2 — S2142: Restore interrupt flag on `InterruptedException`

**Edit file**: `security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwksRegistrationRunner.java`

**BEFORE** (line 98):

```java
} catch (Exception e) {
    // Non-fatal — trust broker may not be available in all environments
    log.warn("JWKS registration failed (non-fatal): {}", e.getMessage());
}
```

**AFTER**:

```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    log.warn("JWKS registration interrupted: {}", e.getMessage());
} catch (Exception e) {
    // Non-fatal — trust broker may not be available in all environments
    log.warn("JWKS registration failed (non-fatal): {}", e.getMessage());
}
```

### Step 0.3 — S2583: Remove always-true null check

**Edit file**: `certificate-authority/src/main/java/com/akademiaplus/interfaceadapters/controllers/CertificateAuthorityController.java`

**BEFORE** (lines 87-90):

```java
String callerIdentity = jwksRegistrationRequestDTO.getKid() != null
        ? jwksRegistrationRequestDTO.getKid()
        : "unknown";
registerJwksUseCase.register(jwksRegistrationRequestDTO, callerIdentity);
```

**AFTER**:

```java
registerJwksUseCase.register(jwksRegistrationRequestDTO, jwksRegistrationRequestDTO.getKid());
```

Remove the `callerIdentity` local variable entirely.

### Verify Phase 0

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
mvn test -pl certificate-authority,security -am
```

All tests must pass. The 3 bugs are now resolved.

### Commit Phase 0

```bash
git add certificate-authority/src/main/java/com/akademiaplus/usecases/domain/TokenManifest.java \
      security/src/main/java/com/akademiaplus/internal/interfaceadapters/jwt/JwksRegistrationRunner.java \
      certificate-authority/src/main/java/com/akademiaplus/interfaceadapters/controllers/CertificateAuthorityController.java
git commit -m "fix(quality): resolve 3 SonarCloud bugs — S2119, S2142, S2583

- Extract SecureRandom to static field in TokenManifest (S2119)
- Restore interrupt flag in JwksRegistrationRunner catch block (S2142)
- Remove always-true null check on @NotNull kid field (S2583)"
```

---

## Phase 1: Security Hotspots — Code (3 files)

**Goal**: Address HIGH-priority security hotspots in application code. CSRF/CORS are justified; DataCleanUp gets validation.

### Step 1.1 — S4502: CSRF suppression with justification

**Edit file**: `certificate-authority/src/main/java/com/akademiaplus/interfaceadapters/config/CaSecurityConfiguration.java`

Add `@SuppressWarnings` on the `caSecurityFilterChain` method:

```java
@SuppressWarnings("java:S4502") // CSRF disabled: mTLS M2M API, no browser/cookie interaction
@Bean
public SecurityFilterChain caSecurityFilterChain(HttpSecurity http) throws Exception {
```

**Edit file**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add `@SuppressWarnings` on the `securityFilterChain` method (dev/local profile):

```java
@SuppressWarnings("java:S4502") // CSRF disabled: stateless JWT Bearer auth, no cookies/sessions
@Bean
@Profile({"dev", "local"})
public SecurityFilterChain securityFilterChain(...) throws Exception {
```

Add `@SuppressWarnings` on the `securityFilterChainForMockDataService` method:

```java
@SuppressWarnings("java:S4502") // CSRF disabled: internal mock-data-service, stateless API
@Bean
@Profile({"mock-data-service"})
public SecurityFilterChain securityFilterChainForMockDataService(...) throws Exception {
```

### Step 1.2 — S2077: Dynamic SQL validation in DataCleanUp

**Edit file**: `mock-data-system/src/main/java/com/akademiaplus/util/base/DataCleanUp.java`

Add a validation regex constant at the class level:

```java
private static final Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
```

Add the required import:

```java
import java.util.regex.Pattern;
```

Update `getTableName()` to validate the name before returning:

**BEFORE**:

```java
private String getTableName(Class<?> entityClass) {
    Table tableAnnotation = entityClass.getAnnotation(Table.class);
    if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
        return tableAnnotation.name();
    }
    throw new UnprocessableDataModelException(ANNOTATION_MISSING);
}
```

**AFTER**:

```java
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

Add `@SuppressWarnings` on the `clean()` method:

```java
@SuppressWarnings("java:S2077") // Table name sourced from @Table annotation, validated by regex
@Transactional
public void clean() {
```

### Step 1.3 — S2245: Random in mock-data generators (13 instances)

Search for all `java.util.Random` usages in mock-data-system:

```bash
grep -rn "java\.util\.Random\|new Random()" mock-data-system/src/main/java/ --include="*.java"
```

For each file found, add `@SuppressWarnings("java:S2245")` at the class level:

```java
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
```

### Verify Phase 1

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
mvn compile -pl certificate-authority,security,mock-data-system -am -DskipTests
```

Compilation must pass.

### Commit Phase 1

```bash
git add -A
git commit -m "fix(quality): triage 24 security hotspots — S4502, S2077, S2245, S5122

- Suppress CSRF hotspots with justification (stateless JWT/mTLS APIs)
- Add regex validation for dynamic SQL table names in DataCleanUp
- Suppress Random in mock-data generators (non-security context)
- CORS wildcard justified via existing Javadoc (mock-data-service)"
```

---

## Phase 2: GitHub Actions Hardening (1 file)

**Goal**: Pin actions to full commit SHA, use `env:` for secrets, `--password-stdin` for Docker login.

### Step 2.1 — Pin actions to SHA

**Edit file**: `.github/workflows/docker-deploy-aws.yml`

**BEFORE** (line 16):

```yaml
        uses: actions/checkout@v4
```

**AFTER**:

```yaml
        uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4
```

**BEFORE** (line 20-21):

```yaml
      - name: Set up JDK 24
        uses: actions/setup-java@v4
        with:
          java-version: '24'
```

**AFTER**:

```yaml
      - name: Set up JDK 25
        uses: actions/setup-java@c1e323688fd81a25caa38c78aa6df2d33d3e20d9 # v4
        with:
          java-version: '25'
```

**BEFORE** (line 46):

```yaml
        uses: appleboy/ssh-action@v1
```

**AFTER**:

```yaml
        uses: appleboy/ssh-action@0ff4204d59e8e51228ff73bce53f80d53301dee2 # v1
```

### Step 2.2 — Secrets via `env:` block + `--password-stdin`

**Build job** — Replace the `run` block (lines 26-39):

**BEFORE**:

```yaml
      - name: Build and push Docker image
        id: docker-build-push
        run: |
          # Login to Docker Hub
          docker login -u ${{ secrets.DOCKERHUB_USER }} -p ${{ secrets.DOCKERHUB_TOKEN }} docker.io

          # Build the image (multi-stage Dockerfile handles Maven build internally)
          docker build -t ${{ secrets.DOCKERHUB_REPO }}:${{ github.sha }} .

          # Push the image to Docker Hub
          docker push ${{ secrets.DOCKERHUB_REPO }}:${{ github.sha }}

          # Export the image tag for the deploy job
          echo "image_tag=${{ secrets.DOCKERHUB_REPO }}:${{ github.sha }}" >> "$GITHUB_OUTPUT"
```

**AFTER**:

```yaml
      - name: Build and push Docker image
        id: docker-build-push
        env:
          DOCKERHUB_USER: ${{ secrets.DOCKERHUB_USER }}
          DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
          DOCKERHUB_REPO: ${{ secrets.DOCKERHUB_REPO }}
        run: |
          echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin docker.io

          docker build -t "$DOCKERHUB_REPO:${{ github.sha }}" .
          docker push "$DOCKERHUB_REPO:${{ github.sha }}"

          echo "image_tag=$DOCKERHUB_REPO:${{ github.sha }}" >> "$GITHUB_OUTPUT"
```

**Deploy job** — Replace the deploy step (lines 44-67):

**BEFORE**:

```yaml
      - name: Deploy to EC2
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SERVER_SSH_KEY }}
          script: |
            echo "Deploying image ${{ needs.build-and-push.outputs.image_tag }} to EC2..."

            # Log in to Docker Hub on the EC2 instance
            docker login -u ${{ secrets.DOCKERHUB_USER }} -p ${{ secrets.DOCKERHUB_TOKEN }} docker.io

            # Pull the image
            docker pull ${{ needs.build-and-push.outputs.image_tag }}

            # Stop and remove the old container (if running)
            docker stop platform-core-api || true
            docker rm platform-core-api || true

            # Run the new container
            docker run -d --name platform-core-api -p 8080:8080 ${{ needs.build-and-push.outputs.image_tag }}

            echo "Deployment complete."
```

**AFTER**:

```yaml
      - name: Deploy to EC2
        uses: appleboy/ssh-action@0ff4204d59e8e51228ff73bce53f80d53301dee2 # v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SERVER_SSH_KEY }}
          envs: DOCKERHUB_USER,DOCKERHUB_TOKEN,IMAGE_TAG
          script: |
            echo "Deploying image $IMAGE_TAG to EC2..."

            echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin docker.io

            docker pull "$IMAGE_TAG"

            docker stop platform-core-api || true
            docker rm platform-core-api || true

            docker run -d --name platform-core-api -p 8080:8080 "$IMAGE_TAG"

            echo "Deployment complete."
        env:
          DOCKERHUB_USER: ${{ secrets.DOCKERHUB_USER }}
          DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
          IMAGE_TAG: ${{ needs.build-and-push.outputs.image_tag }}
```

### Verify Phase 2

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

# Validate YAML syntax
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/docker-deploy-aws.yml'))" && echo "YAML valid" || echo "YAML INVALID"
```

### Commit Phase 2

```bash
git add .github/workflows/docker-deploy-aws.yml
git commit -m "ci(security): harden docker-deploy-aws.yml — pin actions, secure secrets

- Pin actions/checkout, actions/setup-java, appleboy/ssh-action to full SHA
- Use env: block + --password-stdin for Docker Hub credentials
- Pass secrets to SSH action via envs parameter
- Update JDK from 24 to 25"
```

---

## Phase 3: Code Smells — Imports & Unused (all modules)

**Goal**: Remove unused imports (S1128: 21), unused private fields (S1068: 11), unused local variables (S1481: 4), and dead stores (S1854: 3). Total: 39 items.

### Step 3.1 — S1128: Delete unused imports (21 instances)

Search and fix across all modules:

```bash
# Identify all Java files, then check for unused imports
# The Sonar report identifies exact files — fix each one
```

For each file flagged by Sonar, remove the unused import line. Common culprits:
- Imports left behind after refactoring
- IDE auto-imports that were never used
- Imports of classes that were later replaced

### Step 3.2 — S1068: Delete unused private fields (11 instances)

For each flagged file, verify the field is truly unused (not accessed via reflection, `@JsonProperty`, or Lombok-generated code). Delete the field declaration.

### Step 3.3 — S1481: Delete unused local variables (4 instances)

Delete the variable declaration. If the RHS expression has side effects (e.g., a method call), keep the expression but remove the assignment:

```java
// BEFORE
String unused = someMethod();

// AFTER (if someMethod has side effects)
someMethod();

// AFTER (if no side effects)
// Delete entirely
```

### Step 3.4 — S1854: Remove dead stores (3 instances)

Remove assignments to variables that are overwritten before being read.

```java
// BEFORE
String result = "default";
result = computeValue(); // dead store above

// AFTER
String result = computeValue();
```

### Verify Phase 3

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
mvn clean test -DskipITs
```

All tests must pass.

### Commit Phase 3

```bash
git add -A
git commit -m "refactor(quality): remove unused imports, fields, variables, dead stores

- S1128: remove 21 unused imports across all modules
- S1068: remove 11 unused private fields
- S1481: remove 4 unused local variables
- S1854: remove 3 dead store assignments"
```

---

## Phase 4: Code Smells — Exceptions (multi-module)

**Goal**: Fix exception-related smells — S1130 (13), S112 (8), S1874 (26). Total: 47 items.

### Step 4.1 — S1130: Remove undeclared exceptions from throws clause (13 instances)

For each method flagged, check whether the exception is:
1. Actually thrown → keep it
2. Thrown by a called method → keep it
3. Neither → remove from `throws` clause

**Caution**: If the method overrides an interface method, the parent may require the `throws` declaration. In that case, leave it.

### Step 4.2 — S112: Replace generic `throws Exception` (8 instances)

For each method:
1. Identify what exceptions the method body actually throws
2. Replace `throws Exception` with the specific exception types
3. If no exceptions are thrown, remove the clause entirely
4. If the method overrides a framework callback that declares `throws Exception` (e.g., `ApplicationRunner.run()`), add `@SuppressWarnings("java:S112")` with a comment

### Step 4.3 — S1874: Replace deprecated `Locale` constructor (26 instances)

Search and replace across the entire project:

```bash
grep -rn "new Locale(" --include="*.java" .
```

Replace all instances:

```java
// BEFORE
new Locale("es")
new Locale("en", "US")
new Locale("es", "MX", "")

// AFTER
Locale.of("es")
Locale.of("en", "US")
Locale.of("es", "MX")
```

### Verify Phase 4

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
mvn clean test -DskipITs
```

### Commit Phase 4

```bash
git add -A
git commit -m "refactor(quality): fix exception declarations and deprecated Locale — S1130, S112, S1874

- S1130: remove 13 unthrown exceptions from throws clauses
- S112: replace 8 generic throws Exception with specific types
- S1874: migrate 26 deprecated Locale constructors to Locale.of()"
```

---

## Phase 5: Code Smells — Test Quality (test sources)

**Goal**: Fix assertion style issues — S5853 (53), S5838 (5). Total: 58 items.

### Step 5.1 — S5853: Chain multiple assertions (53 instances)

For each test method flagged, identify consecutive `assertThat()` calls that can be chained.

**Strategy by case**:

**Case A — Same subject, different property checks**:

```java
// BEFORE
assertThat(result.getName()).isEqualTo("foo");
assertThat(result.getAge()).isEqualTo(25);

// AFTER — use satisfies()
assertThat(result).satisfies(r -> {
    assertThat(r.getName()).isEqualTo("foo");
    assertThat(r.getAge()).isEqualTo(25);
});
```

**Case B — Same subject, chainable assertions**:

```java
// BEFORE
assertThat(list).hasSize(3);
assertThat(list).contains("a");

// AFTER
assertThat(list).hasSize(3).contains("a");
```

**Case C — Different subjects** — leave as separate assertions.

### Step 5.2 — S5838: Use AssertJ dedicated methods (5 instances)

Replace generic comparisons with dedicated AssertJ methods:

```java
// BEFORE
assertThat(list.size()).isGreaterThanOrEqualTo(5);
assertThat(list.size()).isEqualTo(3);
assertThat(str.isEmpty()).isTrue();

// AFTER
assertThat(list).hasSizeGreaterThanOrEqualTo(5);
assertThat(list).hasSize(3);
assertThat(str).isEmpty();
```

### Verify Phase 5

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
mvn clean test -DskipITs
```

All tests must still pass with identical behavior.

### Commit Phase 5

```bash
git add -A
git commit -m "test(quality): chain assertions and use AssertJ dedicated methods — S5853, S5838

- S5853: chain 53 multi-assertion blocks into single assertThat chains
- S5838: replace 5 generic comparisons with dedicated AssertJ methods"
```

---

## Phase 6: Code Smells — Remaining (mixed)

**Goal**: Fix all remaining code smells. Total: ~47 items.

### Step 6.1 — S7467: Named catch variable unused (8 instances)

Replace unused exception variables with unnamed pattern:

```java
// BEFORE
catch (SomeException e) { log.warn("failed"); }

// AFTER
catch (SomeException _) { log.warn("failed"); }
```

### Step 6.2 — S106: Replace System.out with SLF4J (7 instances)

For each file:
1. Add `@Slf4j` annotation (Lombok) if not present
2. Add `import lombok.extern.slf4j.Slf4j;` if needed
3. Replace:

```java
// BEFORE
System.out.println("message: " + var);

// AFTER
log.info("message: {}", var);
```

Use `log.debug()` for verbose/diagnostic output, `log.info()` for operational messages.

### Step 6.3 — S3776: Reduce cognitive complexity (2 instances)

Identify the 2 methods with cognitive complexity > 15. Extract helper methods to reduce nesting.

Target: cognitive complexity < 10 (per `AI-CODE-REF.md` convention).

### Step 6.4 — S2589: Remove always-true/false conditions (4 instances)

Analyze each condition:
- If it's always true → remove the condition, keep the body
- If it's always false → remove the entire dead branch
- If it indicates a logic error → fix the logic

### Step 6.5 — S119: Generic type parameter name (3 instances)

Evaluate each case:
- If the name is intentionally descriptive (e.g., `ENTITY`) → suppress with `@SuppressWarnings("java:S119")`
- If it's just unnecessarily long → shorten to conventional single-letter

### Step 6.6 — S3011: Reflection accessibility (4 instances)

Suppress with justification — these are Hibernate internal integrations:

```java
@SuppressWarnings("java:S3011") // Reflective access required for Hibernate state management
```

### Step 6.7 — S6201: instanceof without pattern (2 instances)

```java
// BEFORE
if (obj instanceof String) { String s = (String) obj; ... }

// AFTER
if (obj instanceof String s) { ... }
```

### Step 6.8 — S1602: Unnecessary lambda braces (2 instances)

```java
// BEFORE
list.forEach(item -> { process(item); });

// AFTER
list.forEach(item -> process(item));
```

### Step 6.9 — Singleton fixes

| Rule | Fix |
|------|-----|
| S6877 (1) | `Collections.reverse(list)` → `list.reversed()` |
| S4488 (1) | `@RequestMapping(method = RequestMethod.POST)` → `@PostMapping` |
| S1192 (1) | Extract `"ES384"` to `public static final String ES384_ALGORITHM = "ES384"` |
| S5778 (2) | Extract lambda body to named method or suppress |
| S5961 (2) | Suppress with `@SuppressWarnings("java:S5961")` — intentionally comprehensive tests |
| S107 (1) | Suppress with `@SuppressWarnings("java:S107")` — Spring DI controller |
| S135 (1) | Refactor loop or suppress |
| S3012 (1) | Use or acknowledge return value |
| S2386 (1) | Make field private, add accessor |
| S3457 (1) | Fix printf format string |
| S6068 (1) | Update to non-deprecated API |
| S1186 (1) | Add `// no-op` comment or implementation |
| S7027 (1) | Acknowledged — legacy module, no action needed |

### Verify Phase 6

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
mvn clean test -DskipITs
```

### Commit Phase 6

```bash
git add -A
git commit -m "refactor(quality): resolve remaining code smells — S7467, S106, S3776, and others

- S7467: use unnamed catch variable pattern (8 instances)
- S106: replace System.out with SLF4J logger (7 instances)
- S3776: reduce cognitive complexity via method extraction (2 instances)
- S2589: remove dead conditional branches (4 instances)
- S119: evaluate/suppress generic type names (3 instances)
- S3011: suppress justified reflection access (4 instances)
- S6201, S1602, S6877, S4488, S1192, and others: miscellaneous fixes"
```

---

## Phase 7: Re-run Sonar Analysis

**Goal**: Verify all findings are resolved and the quality gate passes.

### Step 7.1 — Full build with coverage

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
mvn clean verify -DskipITs
```

All tests must pass.

### Step 7.2 — Run Sonar analysis

```bash
export $(grep SONAR_TOKEN .env) && mvn clean verify -DskipITs \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=ElatusDev-platform-core-api
```

### Step 7.3 — Dashboard verification

Open https://sonarcloud.io/dashboard?id=ElatusDev-platform-core-api and verify:

- [ ] 0 bugs (Reliability A)
- [ ] 0 vulnerabilities (Security A)
- [ ] 0 unreviewed security hotspots
- [ ] Code smells < 20 (only justified suppressions remain)
- [ ] **Quality gate: PASSED**

### No commit for Phase 7 — verification only.

---

## Critical Conventions Checklist

- [ ] All 3 bugs resolved (S2119, S2142, S2583)
- [ ] All 24 security hotspots reviewed (fixed or suppressed with justification)
- [ ] GitHub Actions pinned to full commit SHA
- [ ] Docker Hub secrets use `env:` block + `--password-stdin`
- [ ] No `any()` matchers introduced in modified test code
- [ ] `@DisplayName` preserved on all modified tests
- [ ] Given-When-Then comment pattern preserved in all modified tests
- [ ] All `@SuppressWarnings` have justification comments
- [ ] All commits use Conventional Commits format
- [ ] No `Co-Authored-By` or AI attribution in commit messages
- [ ] `mvn clean verify -DskipITs` passes after all phases complete
