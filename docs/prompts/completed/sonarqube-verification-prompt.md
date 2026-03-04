# SonarQube Verification — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Workflow**: `docs/workflows/pending/sonarqube-verification-workflow.md`
**Prerequisite**: Read `CLAUDE.md` and `AI-CODE-REF.md` before writing any code.
**Dependency**: Root `pom.xml` must compile cleanly (`mvn clean compile -DskipTests`)

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 0 → 1 → 2 → 3 → 4 → 5). Do NOT skip ahead.
2. After EVERY phase, run the specified verification command. Fix failures before proceeding.
3. Do NOT modify business logic or test assertions — this prompt adds build infrastructure only.
4. Do NOT add the `sonar-maven-plugin` to the `pom.xml` — it is invoked via CLI only.
5. Follow `AI-CODE-REF.md` conventions for any new files (copyright header if applicable).
6. Commit after each phase using Conventional Commits format.

---

## Phase 0: JaCoCo Plugin + `lombok.config` + `@{argLine}` Interop

**Goal**: Add JaCoCo code coverage instrumentation to the build without breaking existing tests. The JaCoCo agent must coexist with the byte-buddy agent already configured in surefire/failsafe.

### Step 0.1 — Create `lombok.config`

**Create file**: `lombok.config` (project root, same level as `pom.xml`)

```
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
```

This tells Lombok to annotate generated code with `@lombok.Generated`, which JaCoCo 0.8.0+ auto-excludes from coverage.

### Step 0.2 — Add JaCoCo version property

**Edit file**: `pom.xml` — inside `<properties>`, alongside other plugin versions

Add after the `<byte.buddy.agent.version>` line:

```xml
<jacoco.maven.plugin.version>0.8.13</jacoco.maven.plugin.version>
```

### Step 0.3 — Add JaCoCo plugin to `<pluginManagement>`

**Edit file**: `pom.xml` — inside `<build><pluginManagement><plugins>`

Add after the existing `maven-failsafe-plugin` block:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.maven.plugin.version}</version>
    <executions>
        <execution>
            <id>jacoco-prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Step 0.4 — Activate JaCoCo for all modules

**Edit file**: `pom.xml` — inside `<build><plugins>` (NOT inside `<pluginManagement>`)

Add:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

If there is no `<build><plugins>` section outside `<pluginManagement>`, create one. Check the existing `pom.xml` structure — some projects declare the `spring-boot-maven-plugin` or `maven-enforcer-plugin` here.

### Step 0.5 — Add `@{argLine}` to surefire

**Edit file**: `pom.xml` — the `maven-surefire-plugin` configuration inside `<pluginManagement>`

**BEFORE**:

```xml
<argLine>
    -XX:+EnableDynamicAgentLoading
    -javaagent:${settings.localRepository}/net/bytebuddy/byte-buddy-agent/${byte.buddy.agent.version}/byte-buddy-agent-${byte.buddy.agent.version}.jar
</argLine>
```

**AFTER**:

```xml
<argLine>
    @{argLine}
    -XX:+EnableDynamicAgentLoading
    -javaagent:${settings.localRepository}/net/bytebuddy/byte-buddy-agent/${byte.buddy.agent.version}/byte-buddy-agent-${byte.buddy.agent.version}.jar
</argLine>
```

`@{argLine}` is a late-binding placeholder. When JaCoCo's `prepare-agent` runs, it sets the `argLine` property to its `-javaagent` flag. The `@{argLine}` resolves at test execution time and prepends JaCoCo's agent before the byte-buddy agent. If JaCoCo is not active (e.g., running with `-Djacoco.skip=true`), `@{argLine}` resolves to an empty string — no regression.

### Step 0.6 — Add `@{argLine}` to failsafe

**Edit file**: `pom.xml` — the `maven-failsafe-plugin` configuration inside `<pluginManagement>`

Apply the exact same `@{argLine}` prepend as Step 0.5.

### Verify Phase 0

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

# Must compile and all unit tests must pass
mvn clean test -DskipITs

# Verify JaCoCo XML reports were produced
find . -path "*/target/site/jacoco/jacoco.xml" | sort

# Expected: one jacoco.xml per module with tests (utilities, user-management, billing, etc.)
# If zero files found: JaCoCo plugin is not active — check Step 0.4 (build/plugins activation)
```

If tests fail with `argLine`-related errors (e.g., "Could not find or load main class @{argLine}"), the issue is that the `@{argLine}` literal is not being resolved. This typically means the JaCoCo `prepare-agent` goal did not run. Verify Step 0.3 and Step 0.4 are both applied.

### Commit Phase 0

```bash
git add pom.xml lombok.config
git commit -m "build(quality): add JaCoCo coverage plugin and lombok.config

- Add jacoco-maven-plugin 0.8.13 with prepare-agent and report executions
- Prepend @{argLine} to surefire and failsafe for JaCoCo agent interop
- Create lombok.config with addLombokGeneratedAnnotation for coverage exclusion
- Per-module jacoco.xml reports produced in target/site/jacoco/"
```

---

## Phase 1: Sonar Properties

**Goal**: Add SonarCloud configuration properties to the root `pom.xml` so the scanner can find coverage reports and exclude generated code.

### Step 1.1 — Add sonar properties

**Edit file**: `pom.xml` — inside `<properties>`, replace existing sonar properties block

**BEFORE**:

```xml
<sonar.organization>elatusdev</sonar.organization>
<sonar.host.url>https://sonarcloud.io</sonar.host.url>
```

**AFTER**:

```xml
<!-- SonarCloud configuration -->
<sonar.organization>elatusdev</sonar.organization>
<sonar.host.url>https://sonarcloud.io</sonar.host.url>
<sonar.projectKey>ElatusDev-platform-core-api</sonar.projectKey>
<sonar.exclusions>
    **/generated-sources/**,
    **/openapi/akademiaplus/domain/**/dto/**
</sonar.exclusions>
<sonar.coverage.exclusions>
    **/generated-sources/**,
    **/openapi/akademiaplus/domain/**/dto/**,
    **/config/**,
    **/Main.java
</sonar.coverage.exclusions>
<sonar.coverage.jacoco.xmlReportPaths>
    ${project.build.directory}/site/jacoco/jacoco.xml
</sonar.coverage.jacoco.xmlReportPaths>
```

### Verify Phase 1

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

# Verify properties are resolved correctly
mvn help:effective-pom -pl utilities | grep -A 2 "sonar\."

# Expected: all sonar.* properties appear with resolved values
```

### Commit Phase 1

```bash
git add pom.xml
git commit -m "build(quality): add SonarCloud properties for coverage and exclusions

- Add sonar.projectKey for SonarCloud project binding
- Exclude generated OpenAPI DTOs from analysis and coverage
- Exclude config classes and Main.java from coverage calculations
- Set JaCoCo XML report path for SonarCloud discovery"
```

---

## Phase 2: Fix `.github/workflows/build.yml`

**Goal**: Complete the truncated `mvn` command in the CI workflow and update the JDK version to match the project.

### Step 2.1 — Fix the workflow file

**Edit file**: `.github/workflows/build.yml`

**BEFORE** (lines 22-23 — JDK version):

```yaml
      - name: Set up JDK 24
        uses: actions/setup-java@v4
        with:
          java-version: 24
```

**AFTER**:

```yaml
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: 25
```

**BEFORE** (lines 48-53 — truncated mvn command):

```yaml
      - name: Build and analyze
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
            -Dsonar.projectKey=ElatusDev-platform-core-api \
```

**AFTER**:

```yaml
      - name: Build and analyze
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          mvn -B verify -DskipITs \
            org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
            -Dsonar.projectKey=ElatusDev-platform-core-api
```

Key changes:
- Add `-DskipITs` — integration tests need Docker/TestContainers, not available in CI
- Move `org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` to a new line for readability
- Remove trailing backslash on last line (was causing dangling continuation)
- Update JDK from 24 to 25 to match `maven.compiler.release=25`

### Verify Phase 2

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

# Validate YAML syntax (requires python3 + pyyaml)
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))" && echo "YAML valid" || echo "YAML INVALID"

# If python3/pyyaml not available, use a simple check:
cat .github/workflows/build.yml | head -55
# Manually verify the mvn command is complete and properly formatted
```

### Commit Phase 2

```bash
git add .github/workflows/build.yml
git commit -m "ci(sonar): fix build.yml — add -DskipITs and complete mvn command

- Add -DskipITs to skip TestContainers integration tests in CI
- Fix truncated mvn command (dangling backslash)
- Update JDK version from 24 to 25 to match maven.compiler.release"
```

---

## Phase 3: Create Java Rules Reference Document

**Goal**: Create the SonarQube Java rules reference at `docs/quality/sonarqube-java-rules.md`.

### Step 3.1 — Create the document

**Create file**: `docs/quality/sonarqube-java-rules.md`

Write the document with these sections (refer to the workflow document Section 7 for content guidance):

1. **Header and purpose** — "Sonar way" Java quality profile reference for AkademiaPlus
2. **Bugs** — S2259, S2583, S1862, S1854, S2095, S2189, S2699 with descriptions and rule IDs
3. **Vulnerabilities** — S2076, S3649, S2068, S4787, S5145, S5131 with descriptions
4. **Security Hotspots** — S4790, S5344, S2245, S4423 with descriptions
5. **Code Smells** — S1192, S109, S138, S3776, S1181, S2221, S108, S1166, S1141, S1479, S1200 with descriptions
6. **Project Convention Mapping** — Table mapping Sonar rules to AI-CODE-REF.md conventions, highlighting where the project is stricter than defaults

Use `docs/quality/sonarqube-java-rules.md` for the exact content — see the workflow document for guidance.

### Verify Phase 3

```bash
# File exists and has content
test -f docs/quality/sonarqube-java-rules.md && echo "EXISTS" || echo "MISSING"
wc -l docs/quality/sonarqube-java-rules.md
# Expected: 100+ lines
```

### Commit Phase 3

```bash
git add docs/quality/sonarqube-java-rules.md
git commit -m "docs(quality): add SonarQube Java rules reference

- Document Sonar way quality profile rules for Bugs, Vulnerabilities,
  Security Hotspots, and Code Smells
- Map Sonar rules to existing AI-CODE-REF.md conventions
- Highlight project-specific thresholds stricter than Sonar defaults"
```

---

## Phase 4: Verify JaCoCo Reports

**Goal**: Run a full build and confirm JaCoCo XML reports are produced for all modules with tests.

### Step 4.1 — Full build

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

mvn clean verify -DskipITs
```

All unit tests must pass. If any test fails, fix the failure before proceeding — do NOT skip tests.

### Step 4.2 — Verify reports

```bash
# List all JaCoCo reports
find . -path "*/target/site/jacoco/jacoco.xml" | sort

# Verify each report has coverage data (non-empty)
for f in $(find . -path "*/target/site/jacoco/jacoco.xml"); do
  lines=$(wc -l < "$f")
  echo "$f: $lines lines"
done
```

Expected: one report per module with test sources, each with 50+ lines of XML.

### Step 4.3 — Verify no argLine regression

```bash
# Run a single module's tests to confirm no agent loading issues
mvn test -pl utilities -am

# Run certificate-authority tests (has the most complex test setup)
mvn test -pl certificate-authority -am
```

### No commit for Phase 4 — this is a verification-only phase.

---

## Phase 5: Run Sonar Analysis (if token available)

**Goal**: Run the full SonarCloud analysis and verify results on the dashboard.

### Step 5.1 — Run analysis

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

# Set your SonarCloud token
export SONAR_TOKEN=<your-token>

mvn clean verify -DskipITs \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=ElatusDev-platform-core-api
```

### Step 5.2 — Verify on dashboard

Open https://sonarcloud.io/project/overview?id=ElatusDev-platform-core-api and verify:

- [ ] Coverage percentage is reported (non-zero)
- [ ] Generated code (DTOs) is excluded from coverage
- [ ] Quality gate status is displayed
- [ ] No false-positive issues on Lombok-generated code

### Skip Phase 5 if no SonarCloud token is available. The infrastructure is still correctly configured and will activate when CI runs with the `SONAR_TOKEN` secret.

---

## Final Verification

Run all checks:

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

# 1. Full build passes
mvn clean verify -DskipITs

# 2. JaCoCo reports exist
find . -path "*/target/site/jacoco/jacoco.xml" | wc -l
# Expected: 8+ files

# 3. lombok.config exists
cat lombok.config

# 4. Sonar properties in effective POM
mvn help:effective-pom -pl utilities | grep "sonar\." | head -10

# 5. YAML is valid
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))" 2>/dev/null && echo "YAML OK"

# 6. Rules doc exists
test -f docs/quality/sonarqube-java-rules.md && echo "Rules doc OK"
```

---

## Critical Conventions Checklist

- [ ] No business logic or test assertions modified
- [ ] `@{argLine}` uses `@` prefix (NOT `$`) for late-binding
- [ ] JaCoCo plugin is in BOTH `<pluginManagement>` AND `<build><plugins>`
- [ ] `sonar-maven-plugin` is NOT declared in `pom.xml` (CLI-only invocation)
- [ ] `sonar.exclusions` covers OpenAPI generated DTO packages
- [ ] `lombok.config` is at project root (same level as `pom.xml`)
- [ ] `.github/workflows/build.yml` has no trailing backslash on last mvn argument
- [ ] All commits use Conventional Commits format
- [ ] No `Co-Authored-By` or AI attribution in commit messages
