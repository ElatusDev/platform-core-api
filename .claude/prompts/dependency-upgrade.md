# Dependency Upgrade — Claude Code Execution Prompt

**Target**: Claude Code CLI  
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`  
**Spec**: `.claude/prompts/upgrade-plan.md` (human-readable reference — read before starting)  
**Prerequisite**: Read `CLAUDE.md` and `AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 1 → 2 → 3). Do NOT skip ahead.
2. After EVERY phase, run the specified verification command. Fix failures before proceeding.
3. After EVERY module in Phase 3, run the module-specific compile command before moving on.
4. Do NOT modify test logic or business code — this prompt is purely a dependency and import migration.
5. Do NOT add, remove, or rename any Java classes beyond import statement rewrites in Phase 3.
6. Commit after each phase using Conventional Commits format.

---

## Phase 1: Spring Boot GA + Java 25 + Plugin Fix

**Goal**: Replace the milestone parent with `4.0.2` GA, target Java 25, and fix the
Spring Boot Maven Plugin version mismatch. This is a pure POM change — no Java source files
are modified.

### 1.1 — Root POM parent version

**Edit file**: `pom.xml`

```xml
<!-- BEFORE -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.0-M3</version>
    <relativePath/>
</parent>

<!-- AFTER -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.2</version>
    <relativePath/>
</parent>
```

### 1.2 — Java compiler target: source/target → release

**Edit file**: `pom.xml` — inside `<properties>`

```xml
<!-- REMOVE both of these -->
<maven.compiler.source>21</maven.compiler.source>
<maven.compiler.target>21</maven.compiler.target>

<!-- ADD this single property in their place -->
<maven.compiler.release>25</maven.compiler.release>
```

### 1.3 — Remove the Spring Boot Maven Plugin version pin

**Edit file**: `pom.xml` — inside `<properties>`

```xml
<!-- REMOVE this property entirely -->
<spring-boot-maven-plugin.version>3.5.4</spring-boot-maven-plugin.version>
```

Then in `<build><pluginManagement><plugins>`, find the `spring-boot-maven-plugin` entry
and remove its `<version>` tag so it inherits the version managed by `spring-boot-starter-parent`:

```xml
<!-- BEFORE -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <version>${spring-boot-maven-plugin.version}</version>
    <configuration>
        ...
    </configuration>
    ...
</plugin>

<!-- AFTER — remove the <version> line only, keep everything else unchanged -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        ...
    </configuration>
    ...
</plugin>
```

### 1.4 — Update CLAUDE.md to reflect new stack

**Edit file**: `.claude/CLAUDE.md`

Update the Stack line:

```
<!-- BEFORE -->
**Stack**: Java 21, Spring Boot 4.0.0-M3, MariaDB, Maven multi-module

<!-- AFTER -->
**Stack**: Java 25, Spring Boot 4.0.2, MariaDB, Maven multi-module
```

Update the key technical decision entry:

```
<!-- BEFORE -->
1. **Spring Boot 4.0.0-M3**: Milestone release — expect API changes

<!-- AFTER -->
1. **Spring Boot 4.0.2**: GA release — stable Foundation layer stack
```

### Verify Phase 1

```bash
# Full compile — all modules, no tests
mvn clean compile -DskipTests

# Confirm Java 25 class-file version is being produced
# (should print "class file version 69.0" for Java 25)
javap -verbose target/classes/com/akademiaplus/Main.class 2>/dev/null | grep "major version" \
  || echo "Run from application/ subdirectory if Main.class not found at root"

# Run unit tests — must be green before proceeding
mvn test -pl utilities -am
mvn test -pl user-management -am
mvn test -pl billing -am
```

If `mvn clean compile -DskipTests` fails due to enforcer `DependencyConvergence`, read the
conflict tree output and add explicit version overrides to `<dependencyManagement>` in the
root `pom.xml` to resolve the conflict. Do NOT disable the enforcer plugin.

### Commit Phase 1

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
git add pom.xml .claude/CLAUDE.md
git commit -m "build(deps): upgrade to Spring Boot 4.0.2 GA and Java 25 LTS

- Bump spring-boot-starter-parent from 4.0.0-M3 to 4.0.2 GA
- Replace maven.compiler.source/target with maven.compiler.release=25
- Remove spring-boot-maven-plugin.version pin (now inherited from parent)
- Transitively upgrades: Spring Framework 7.0.5, Hibernate 7.1.8.Final,
  Spring Security 7.0.3, Tomcat 11.0.18
- Java 25 LTS enables virtual thread pinning fix (JDK 24+) for Hibernate
  session boundaries under Spring virtual thread executor"
```

---

## Phase 2: springdoc-openapi 2.x → 3.x

**Goal**: Upgrade springdoc-openapi from 2.8.9 (Spring Boot 3 line) to 3.0.1 (Spring Boot 4
native line). Add the required property configuration to re-enable Swagger UI, which springdoc
3.0.x disables by default in favour of Scalar UI.

### 2.1 — Root POM version property

**Edit file**: `pom.xml` — inside `<properties>`

```xml
<!-- BEFORE -->
<springdoc-openapi-starter-webmvc-ui-version>2.8.9</springdoc-openapi-starter-webmvc-ui-version>

<!-- AFTER -->
<springdoc-openapi-starter-webmvc-ui-version>3.0.1</springdoc-openapi-starter-webmvc-ui-version>
```

### 2.2 — Re-enable Swagger UI in application properties

springdoc 3.0.x ships Scalar UI as default and disables `/swagger-ui.html` by default.
Re-enable both Swagger UI and API docs explicitly.

**Edit file**: `application/src/main/resources/application.properties`

Append these lines at the end of the file:

```properties
# springdoc-openapi 3.x: re-enable Swagger UI (disabled by default in 3.x, replaced by Scalar)
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
```

**Edit file**: `application/src/main/resources/application-dev.properties`

If this file has any springdoc properties, verify they remain compatible. If it has none,
no change is needed.

**Edit file**: `application/src/main/resources/application-local.properties`

Same check as dev — add the springdoc properties only if not already present.

### 2.3 — Verify there are no hardcoded springdoc version references

Run:

```bash
grep -rn "springdoc" --include="*.java" --include="*.xml" --include="*.properties" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api
```

If any Java source files import from `org.springdoc.*`, review them. The API surface of
springdoc 3.x is compatible with 2.x for standard `@OpenAPIDefinition`, `@Operation`,
`@ApiResponse` usage — no Java source changes are expected. If any incompatibilities appear
in the compiler output, resolve them before proceeding.

### Verify Phase 2

```bash
# Full compile
mvn clean compile -DskipTests

# Start the application locally (requires MariaDB + env vars — use local profile)
# Then verify the endpoints respond:
#   curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v3/api-docs
#   curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/swagger-ui/index.html
# Both should return 200. If 400 is returned, confirm springdoc.swagger-ui.enabled=true
# is loaded from the active profile's properties file.
echo "Manual server smoke test required — see comments above"

# Run full test suite
mvn test -DskipTests=false
```

### Commit Phase 2

```bash
git add pom.xml application/src/main/resources/
git commit -m "build(deps): upgrade springdoc-openapi 2.8.9 → 3.0.1 for Spring Boot 4

- springdoc v3.x is the Spring Boot 4 / Spring Framework 7 native branch
- v2.x receives no further Spring Boot 4 compatibility updates
- Re-enable Swagger UI in application.properties (disabled by default in v3.x)
- springdoc v3.0.0 skipped: known Jackson 3 setAll() conflict resolved in 3.0.1"
```

---

## Phase 3: Jackson 2 → Jackson 3 Import Migration

**Goal**: Rewrite all Java source import statements from the Jackson 2 package namespace
(`com.fasterxml.jackson.*`) to the Jackson 3 namespace (`tools.jackson.*`). The Maven
group IDs remain `com.fasterxml.jackson` — only the Java package names changed.

Remove explicit Jackson 2 version pins from the root POM so Boot 4.0.2 manages
Jackson 3 through its own BOM.

**CRITICAL**: This phase touches import statements ONLY. Do not modify method calls,
constructor calls, or any logic. The Jackson 3 API is semantically compatible with
Jackson 2 for the ObjectMapper, JsonNode, and annotation usage patterns present in
this codebase.

### 3.0 — Audit before touching anything

Run these greps and record the output. This is the migration checklist.

```bash
# All files with Jackson 2 imports (main sources)
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/*/src/main/ \
  | sort

# All files with Jackson 2 imports (test sources)
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/*/src/test/ \
  | sort

# What specific Jackson classes are imported across the whole project
grep -rh "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/ \
  | sort | uniq -c | sort -rn
```

Expected findings based on prior audit:
- `certificate-authority`: `TokenManifest.java`, `BootstrapToken.java`, `CertificateAuthorityConfig.java`
- `billing` (tests): multiple `*ControllerTest.java` files using `ObjectMapper` for MockMvc
- Other modules (tests): controller tests using `ObjectMapper`

If the grep reveals files not in this expected list, include them in the migration below.

### 3.1 — Root POM: remove Jackson 2 explicit version pins

**Edit file**: `pom.xml` — `<properties>` section

**REMOVE** these properties entirely:
```xml
<jackson.core.version>2.20.0</jackson.core.version>
<jackson.annotations.version>2.20</jackson.annotations.version>
<jackson.dataform.yaml.version>2.19.0</jackson.dataform.yaml.version>
```

**Edit file**: `pom.xml` — `<dependencyManagement>` section

**REMOVE** the explicit Jackson BOM import:
```xml
<dependency>
    <groupId>com.fasterxml.jackson</groupId>
    <artifactId>jackson-bom</artifactId>
    <version>2.20.0</version>
    <type>pom</type>
</dependency>
```

**REMOVE** all individual Jackson artifact entries that reference the removed version properties:
```xml
<!-- Remove all of these -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <version>${jackson.dataform.yaml.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>${jackson.core.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>${jackson.core.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jdk8</artifactId>
    <version>${jackson.core.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>${jackson.core.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-guava</artifactId>
    <version>${jackson.core.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-joda</artifactId>
    <version>${jackson.core.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
    <version>${jackson.annotations.version}</version>
</dependency>
```

After these removals, `spring-boot-starter-parent 4.0.2` manages all Jackson 3 versions
through its own BOM. Verify the dependency tree resolves correctly:

```bash
mvn dependency:tree -pl utilities -am | grep "jackson" | sort | uniq
```

Expect to see `tools.jackson.*` versions managed by Boot 4.0.2 (Jackson 3.x).

### 3.2 — Import rewrite reference table

For every file modified in steps 3.3 through 3.7, apply these substitutions:

| Jackson 2 import (remove) | Jackson 3 import (add) |
|---|---|
| `import com.fasterxml.jackson.databind.ObjectMapper;` | `import tools.jackson.databind.ObjectMapper;` |
| `import com.fasterxml.jackson.databind.JsonNode;` | `import tools.jackson.databind.JsonNode;` |
| `import com.fasterxml.jackson.databind.node.ObjectNode;` | `import tools.jackson.databind.node.ObjectNode;` |
| `import com.fasterxml.jackson.databind.node.ArrayNode;` | `import tools.jackson.databind.node.ArrayNode;` |
| `import com.fasterxml.jackson.annotation.JsonProperty;` | `import tools.jackson.annotation.JsonProperty;` |
| `import com.fasterxml.jackson.annotation.JsonIgnore;` | `import tools.jackson.annotation.JsonIgnore;` |
| `import com.fasterxml.jackson.annotation.JsonIgnoreProperties;` | `import tools.jackson.annotation.JsonIgnoreProperties;` |
| `import com.fasterxml.jackson.annotation.JsonCreator;` | `import tools.jackson.annotation.JsonCreator;` |
| `import com.fasterxml.jackson.core.JsonProcessingException;` | `import tools.jackson.core.JacksonException;` |
| `import com.fasterxml.jackson.core.type.TypeReference;` | `import tools.jackson.core.type.TypeReference;` |
| `import com.fasterxml.jackson.databind.DeserializationFeature;` | `import tools.jackson.databind.DeserializationFeature;` |
| `import com.fasterxml.jackson.databind.SerializationFeature;` | `import tools.jackson.databind.SerializationFeature;` |

**IMPORTANT NOTE on `JsonProcessingException`**: In Jackson 3, `JsonProcessingException` is
replaced by `JacksonException` as the base checked exception. If any catch clause catches
`JsonProcessingException`, change it to `JacksonException`. Verify the method signature
compiles after the rename.

### 3.3 — certificate-authority module

Files to migrate (main sources):

```
certificate-authority/src/main/java/com/akademiaplus/usecases/domain/TokenManifest.java
certificate-authority/src/main/java/com/akademiaplus/usecases/domain/BootstrapToken.java
certificate-authority/src/main/java/com/akademiaplus/interfaceadapters/config/CertificateAuthorityConfig.java
```

For each file:
1. Read the file to identify which specific Jackson 2 imports are present
2. Apply only the substitutions from the table in 3.2 that match what is actually imported
3. If `JsonProcessingException` is caught anywhere, rename to `JacksonException`

Files to migrate (test sources):

```
certificate-authority/src/test/java/com/akademiaplus/usecases/domain/TokenManifestTest.java
```

**Verify after 3.3**:

```bash
mvn clean compile -pl certificate-authority -am -DskipTests
mvn test -pl certificate-authority -am
```

### 3.4 — utilities module

Run the grep first to find all files with Jackson 2 imports in this module:

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/utilities/
```

Migrate every file found. Apply only the substitutions that match actual imports.

**Verify after 3.4**:

```bash
mvn clean compile -pl utilities -am -DskipTests
mvn test -pl utilities -am
```

### 3.5 — infra-common and multi-tenant-data modules

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/infra-common/ \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/multi-tenant-data/
```

Migrate every file found.

**Verify after 3.5**:

```bash
mvn clean compile -pl infra-common,multi-tenant-data -am -DskipTests
mvn test -pl infra-common -am
mvn test -pl multi-tenant-data -am
```

### 3.6 — security module

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/security/
```

Migrate every file found. Pay attention to JWT filter classes — these commonly use
`ObjectMapper` for serializing auth error responses.

**Verify after 3.6**:

```bash
mvn clean compile -pl security -am -DskipTests
mvn test -pl security -am
```

### 3.7 — Domain modules (in dependency order)

Process each module in this exact order. After EACH module: grep, migrate, compile, test.

**user-management**:

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/user-management/

# Migrate all found files, then:
mvn clean compile -pl user-management -am -DskipTests
mvn test -pl user-management -am
```

**billing** (most test files affected — multiple `*ControllerTest.java`):

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/billing/

# Migrate all found files, then:
mvn clean compile -pl billing -am -DskipTests
mvn test -pl billing -am
```

**course-management**:

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/course-management/

mvn clean compile -pl course-management -am -DskipTests
mvn test -pl course-management -am
```

**notification-system**:

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/notification-system/

mvn clean compile -pl notification-system -am -DskipTests
mvn test -pl notification-system -am
```

**tenant-management**:

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/tenant-management/

mvn clean compile -pl tenant-management -am -DskipTests
mvn test -pl tenant-management -am
```

**pos-system**:

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/pos-system/

mvn clean compile -pl pos-system -am -DskipTests
mvn test -pl pos-system -am
```

**mock-data-system**:

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/mock-data-system/

mvn clean compile -pl mock-data-system -am -DskipTests
mvn test -pl mock-data-system -am
```

**application** (aggregator — unlikely to have direct Jackson imports but verify):

```bash
grep -rln "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/application/

mvn clean compile -pl application -am -DskipTests
```

### 3.8 — Third-party library compatibility notes (do NOT modify)

These libraries still use Jackson 2 internally and are explicitly NOT migrated in this phase.
Spring Boot 4.x maintains a Jackson 2 compatibility bridge until Boot 4.3 — they continue
to function correctly through that bridge:

- **`jackson-databind-nullable` (0.2.6)** — used by openapi-generator generated code.
  Leave the `<version>` pin at `0.2.6` in `<dependencyManagement>`. Monitor for a 0.3.x
  release targeting Jackson 3.
- **`mercadopago sdk-java` (2.5.0)** — internal Jackson 2 usage in the billing module.
  Do not modify. Leave it as-is and rely on the compatibility bridge.

### Verify Phase 3 (full project)

```bash
# Confirm zero remaining Jackson 2 imports in project source (except managed third-party)
grep -rn "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/ \
  | grep -v "target/" \
  | grep -v "generated-sources/"
# Expected output: empty (no matches from hand-written source files)
# jackson-databind-nullable generated sources will still show Jackson 2 — that is expected

# Full build and test
mvn clean test -DskipTests=false
```

If any tests fail, read the failure message. The most likely causes are:
1. A `JsonProcessingException` catch that needs renaming to `JacksonException`
2. A `ObjectMapper` method that changed signature — consult the Jackson 3 migration guide
3. A test that serialises a DTO to JSON using `objectMapper.writeValueAsString()` where
   the DTO class still has Jackson 2 `@Json*` annotations on generated sources — these
   generated DTOs are managed by openapi-generator and will be regenerated with Jackson 3
   annotations once openapi-generator releases Jackson 3 support. Until then, they work
   through the compatibility bridge.

### Commit Phase 3

```bash
git add pom.xml
git add $(git diff --name-only --cached)  # pick up all modified Java files
git commit -m "build(deps): migrate Jackson 2 → Jackson 3 package namespace

- Remove explicit Jackson 2 BOM and version properties from root POM
- Jackson 3 now managed by spring-boot-starter-parent 4.0.2 BOM
- Rewrite all hand-written source imports: com.fasterxml.jackson.* → tools.jackson.*
- JsonProcessingException catch clauses renamed to JacksonException where applicable
- jackson-databind-nullable (0.2.6) and mercadopago SDK remain on Jackson 2
  compatibility bridge (supported until Spring Boot 4.3)"
```

---

## Post-Upgrade Checklist

Run these after all three phases are committed and clean:

```bash
# 1. Full clean build including integration tests
mvn clean verify

# 2. Confirm enforcer passes (no dependency convergence violations)
mvn validate -pl . -am

# 3. Confirm no Jackson 2 imports remain in hand-written sources
grep -rn "import com.fasterxml.jackson" --include="*.java" \
  /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api/ \
  | grep -v "target/" | grep -v "generated-sources/"

# 4. Confirm Java 25 is active
java -version
mvn --version

# 5. Confirm Spring Boot version in the built JAR manifest
jar tf application/target/application-1.0.jar | grep "BOOT-INF/lib/spring-boot-4" | head -3
```

If the Postman E2E suite (`platform-api-e2e`) is available and the server can be started
locally, run the full Newman collection as a final smoke test:

```bash
cd /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-api-e2e
newman run "Postman Collections/platform-api-e2e.json" \
  --env-var "baseUrl=http://localhost:8080/api" \
  --reporters cli \
  --bail
```
