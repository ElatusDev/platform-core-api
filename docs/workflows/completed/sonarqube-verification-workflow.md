# SonarQube Verification Workflow — AkademiaPlus

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md`, and the root `pom.xml` before starting.
**Prompt**: `docs/prompts/pending/sonarqube-verification-prompt.md`
**Related**: `docs/quality/sonarqube-java-rules.md` (Java rules reference)

---

## 1. Architecture Overview

### SonarCloud Model

SonarCloud performs static analysis on every push/PR via a GitHub Actions workflow. The analysis pipeline is:

```
Source code → Maven build → JaCoCo agent (coverage) → Sonar Scanner → SonarCloud dashboard
```

### Multi-Module Coverage Strategy

Each Maven module produces its own `jacoco.xml` report in `target/site/jacoco/`. SonarCloud natively aggregates per-module reports — no separate aggregator module or `jacoco-report-aggregate` goal is needed.

```
platform-core-api/
├── utilities/target/site/jacoco/jacoco.xml
├── user-management/target/site/jacoco/jacoco.xml
├── billing/target/site/jacoco/jacoco.xml
├── course-management/target/site/jacoco/jacoco.xml
├── tenant-management/target/site/jacoco/jacoco.xml
├── notification-system/target/site/jacoco/jacoco.xml
├── pos-system/target/site/jacoco/jacoco.xml
├── security/target/site/jacoco/jacoco.xml
├── certificate-authority/target/site/jacoco/jacoco.xml
├── infra-common/target/site/jacoco/jacoco.xml
└── multi-tenant-data/target/site/jacoco/jacoco.xml
```

Modules without test sources (e.g., `application`, `etl-service`, `audit-service`) produce no report — this is expected.

### Data Flow

```
┌──────────────────────────────────────────────────────────────┐
│  GitHub Actions (build.yml)                                  │
│                                                              │
│  mvn clean verify -DskipITs                                  │
│    ├── compile ──► surefire (unit tests) ──► JaCoCo agent    │
│    │                                          │              │
│    │               jacoco.xml per module ◄────┘              │
│    │                                                         │
│    └── org.sonarsource.scanner.maven:sonar-maven-plugin:sonar│
│           │                                                  │
│           ▼                                                  │
│    SonarCloud API (sonar.projectKey, sonar.organization)     │
└──────────────────────────────────────────────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  SonarCloud Dashboard│
              │  - Quality Gate      │
              │  - Coverage %        │
              │  - Issues            │
              │  - Duplications      │
              └─────────────────────┘
```

---

## 2. JaCoCo Configuration

### 2.1 Plugin Declaration (root `pom.xml`)

Add the JaCoCo version property alongside other plugin versions:

```xml
<properties>
    <!-- ... existing properties ... -->
    <jacoco.maven.plugin.version>0.8.13</jacoco.maven.plugin.version>
</properties>
```

Add the JaCoCo plugin in `<build><pluginManagement><plugins>`:

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

Then activate it for all modules by adding to `<build><plugins>` (NOT inside `pluginManagement`):

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

### 2.2 `@{argLine}` Interop with Surefire and Failsafe

**Problem**: The existing surefire and failsafe plugins use a hard-coded `<argLine>` containing the byte-buddy agent. JaCoCo's `prepare-agent` goal sets the `argLine` Maven property with its own `-javaagent` flag. If the surefire `<argLine>` is hard-coded, JaCoCo's agent is silently dropped.

**Solution**: Use the `@{argLine}` late-binding placeholder to let JaCoCo prepend its agent to the existing arguments. The `@{argLine}` syntax (as opposed to `${argLine}`) resolves at test execution time, and resolves to an empty string if JaCoCo is not active.

**Current surefire `<argLine>`**:

```xml
<argLine>
    -XX:+EnableDynamicAgentLoading
    -javaagent:${settings.localRepository}/net/bytebuddy/byte-buddy-agent/${byte.buddy.agent.version}/byte-buddy-agent-${byte.buddy.agent.version}.jar
</argLine>
```

**Updated surefire `<argLine>`**:

```xml
<argLine>
    @{argLine}
    -XX:+EnableDynamicAgentLoading
    -javaagent:${settings.localRepository}/net/bytebuddy/byte-buddy-agent/${byte.buddy.agent.version}/byte-buddy-agent-${byte.buddy.agent.version}.jar
</argLine>
```

Apply the same change to the failsafe plugin.

### 2.3 `lombok.config`

Create `lombok.config` at the project root (`platform-core-api/lombok.config`):

```
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
```

This causes Lombok to annotate all generated code with `@lombok.Generated`. JaCoCo 0.8.0+ automatically excludes classes/methods annotated with any `@Generated` annotation from coverage reports. Without this, Lombok-generated constructors, getters, builders, etc. appear as uncovered lines, artificially lowering coverage.

---

## 3. Sonar Properties

Add these properties to the root `pom.xml` `<properties>` section alongside the existing `sonar.organization` and `sonar.host.url`:

```xml
<!-- SonarCloud configuration -->
<sonar.organization>elatusdev</sonar.organization>
<sonar.host.url>https://sonarcloud.io</sonar.host.url>
<sonar.projectKey>ElatusDev-platform-core-api</sonar.projectKey>

<!-- Exclude OpenAPI-generated DTOs and Lombok-generated code from analysis -->
<sonar.exclusions>
    **/generated-sources/**,
    **/openapi/akademiaplus/domain/**/dto/**
</sonar.exclusions>

<!-- Exclude generated code from coverage calculations -->
<sonar.coverage.exclusions>
    **/generated-sources/**,
    **/openapi/akademiaplus/domain/**/dto/**,
    **/config/**,
    **/Main.java
</sonar.coverage.exclusions>

<!-- JaCoCo report paths — SonarCloud auto-discovers per-module reports -->
<sonar.coverage.jacoco.xmlReportPaths>
    ${project.build.directory}/site/jacoco/jacoco.xml
</sonar.coverage.jacoco.xmlReportPaths>
```

### Property Explanation

| Property | Purpose |
|----------|---------|
| `sonar.projectKey` | Unique project identifier in SonarCloud (matches repository) |
| `sonar.exclusions` | Files excluded from all analysis (bugs, smells, vulnerabilities) |
| `sonar.coverage.exclusions` | Files excluded from coverage calculations only |
| `sonar.coverage.jacoco.xmlReportPaths` | Where SonarCloud finds JaCoCo XML reports |

### Why Exclude OpenAPI DTOs

The project uses `openapi-generator-maven-plugin` to generate DTOs in `openapi.akademiaplus.domain.{module}.dto` packages. These are machine-generated and:
- Cannot be meaningfully tested (no logic)
- Inflate duplication metrics (repeated getter/setter patterns)
- Artificially lower coverage percentages

---

## 4. Quality Gate

The project uses the **"Sonar way"** built-in quality gate with the following thresholds:

| Metric | Condition | Threshold |
|--------|-----------|-----------|
| Coverage on new code | is less than | 80.0% |
| Duplicated lines on new code | is greater than | 3.0% |
| Maintainability rating on new code | is worse than | A |
| Reliability rating on new code | is worse than | A |
| Security rating on new code | is worse than | A |
| Security hotspots reviewed on new code | is less than | 100% |

### Project-Specific Overrides (via AI-CODE-REF.md)

The project enforces stricter thresholds than Sonar defaults:

| Metric | Sonar Default | Project Convention | Source |
|--------|---------------|-------------------|--------|
| Method length | 50 lines (S138) | < 20 lines | AI-CODE-REF.md |
| Cognitive complexity | 15 (S3776) | < 10 | AI-CODE-REF.md |
| String literal duplication | 3+ occurrences (S1192) | Zero tolerance — ALL literals to constants | AI-CODE-REF.md |

---

## 5. CI/CD Integration

### Complete `build.yml` Template

```yaml
name: SonarQube

on:
  push:
    branches:
      - main
      - develop
  pull_request:
    types: [opened, synchronize, reopened]
  workflow_dispatch:

jobs:
  build:
    name: Build and analyze
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
          ref: ${{ github.event.inputs.branch || github.ref_name }}

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: 25
          distribution: 'corretto'
          cache: maven

      - name: Cache SonarQube packages
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar
          restore-keys: ${{ runner.os }}-sonar

      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: maven-${{ runner.os }}-${{ hashFiles('**/pom.xml') }}-${{ github.run_id }}
          restore-keys: |
            maven-${{ runner.os }}-

      - name: Build and analyze
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          mvn -B verify -DskipITs \
            org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
            -Dsonar.projectKey=ElatusDev-platform-core-api
```

### Design Decisions

| Decision | Rationale |
|----------|-----------|
| `-DskipITs` | Integration tests require Docker/TestContainers — not available in CI runner. Unit test coverage is sufficient for the quality gate. |
| JDK 25 | Matches the project's `maven.compiler.release=25` (see dependency-upgrade-prompt.md). |
| No `sonar-maven-plugin` in `pom.xml` | Invoked via CLI only — keeps the build clean for developers who don't need Sonar locally. |
| `sonar.projectKey` in CLI | Redundant with `pom.xml` property but kept for CI visibility and as a fallback. |
| `fetch-depth: 0` | Required for SonarCloud to compute new-code coverage diffs against the base branch. |

---

## 6. Local Development

### Run Unit Tests with Coverage

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api

# Full build with coverage (skip integration tests)
mvn clean verify -DskipITs

# Single module with coverage
mvn clean verify -pl user-management -am -DskipITs
```

### View Coverage Reports

JaCoCo generates HTML reports alongside the XML:

```bash
# Open HTML report for a specific module
open utilities/target/site/jacoco/index.html
open user-management/target/site/jacoco/index.html
open billing/target/site/jacoco/index.html
```

### Verify JaCoCo XML Reports Exist

```bash
find . -path "*/target/site/jacoco/jacoco.xml" | sort
```

Expected output — one line per module with tests:

```
./billing/target/site/jacoco/jacoco.xml
./certificate-authority/target/site/jacoco/jacoco.xml
./course-management/target/site/jacoco/jacoco.xml
./infra-common/target/site/jacoco/jacoco.xml
./multi-tenant-data/target/site/jacoco/jacoco.xml
./notification-system/target/site/jacoco/jacoco.xml
./pos-system/target/site/jacoco/jacoco.xml
./security/target/site/jacoco/jacoco.xml
./tenant-management/target/site/jacoco/jacoco.xml
./user-management/target/site/jacoco/jacoco.xml
./utilities/target/site/jacoco/jacoco.xml
```

### Run Sonar Analysis Locally (optional — requires token)

```bash
export SONAR_TOKEN=<your-sonarcloud-token>

mvn clean verify -DskipITs \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=ElatusDev-platform-core-api
```

---

## 7. Java Rules Reference

See `docs/quality/sonarqube-java-rules.md` for the complete reference mapping Sonar rules to project conventions.

### Summary by Category

| Category | Scope | Key Rules |
|----------|-------|-----------|
| Bugs | Logic errors, null dereference, resource leaks | S2259, S2583, S1862, S1854, S2095 |
| Vulnerabilities | Injection, crypto, credentials | S2076, S3649, S2068, S4787, S5145 |
| Security Hotspots | Requires manual review | S4790, S5344, S2245 |
| Code Smells | Maintainability, complexity, style | S1192, S109, S138, S3776, S1181, S2221, S108 |

### Project Convention Mapping

The project's `AI-CODE-REF.md` enforces rules that are **stricter** than the "Sonar way" defaults:

- **Zero-tolerance string literals** (vs Sonar's 3+ threshold for S1192)
- **< 20 line methods** (vs Sonar's 50-line threshold for S138)
- **Cognitive complexity < 10** (vs Sonar's 15 for S3776)
- **No `any()` matchers in tests** (no Sonar equivalent — project-specific)
- **`@DisplayName` required on all `@Test`** (no Sonar equivalent — project-specific)

---

## 8. Verification Checklist

After executing the prompt, verify all items pass:

- [ ] `lombok.config` exists at project root with `lombok.addLombokGeneratedAnnotation = true`
- [ ] Root `pom.xml` contains `jacoco.maven.plugin.version` property
- [ ] JaCoCo plugin declared in `<pluginManagement>` with `prepare-agent` and `report` executions
- [ ] JaCoCo plugin activated in `<build><plugins>` (outside `pluginManagement`)
- [ ] Surefire `<argLine>` starts with `@{argLine}` before the byte-buddy agent
- [ ] Failsafe `<argLine>` starts with `@{argLine}` before the byte-buddy agent
- [ ] `sonar.exclusions` covers `**/generated-sources/**` and DTO packages
- [ ] `sonar.coverage.exclusions` covers generated code, config classes, and `Main.java`
- [ ] `sonar.coverage.jacoco.xmlReportPaths` set to `${project.build.directory}/site/jacoco/jacoco.xml`
- [ ] `.github/workflows/build.yml` uses `-DskipITs` and has no trailing incomplete command
- [ ] `mvn clean verify -DskipITs` produces `jacoco.xml` in each module with tests
- [ ] All existing unit tests still pass (no `argLine` regression)
- [ ] `docs/quality/sonarqube-java-rules.md` exists with all rule categories
