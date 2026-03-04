# SonarQube Java Rules Reference — AkademiaPlus

This document maps the **"Sonar way" Java quality profile** rules to the AkademiaPlus project conventions defined in `AI-CODE-REF.md`. Rules are organized by category with the most impactful rules listed first.

---

## 1. Bugs

Bugs represent code that is demonstrably wrong or will produce unexpected behavior at runtime.

| Rule ID | Name | Description | Severity |
|---------|------|-------------|----------|
| S2259 | Null pointer dereference | Null values should not be dereferenced. Use `Optional` or explicit null checks at boundaries. | Blocker |
| S2583 | Conditions always true/false | Conditions that are always true or always false indicate logic errors. Remove dead branches. | Major |
| S1862 | Identical `if`/`else` branches | Related `if`/`else if` statements should not have the same condition. Merge or fix the logic. | Major |
| S1854 | Dead stores | Useless assignments to local variables should be removed. A "dead store" is a value written but never read. | Major |
| S2095 | Resources should be closed | Resources (streams, connections, readers) must be closed in a `finally` block or `try-with-resources`. | Blocker |
| S2189 | Infinite loops | Loops should have an exit condition. Infinite loops indicate missing break/return logic. | Blocker |
| S2699 | Tests without assertions | Tests should include at least one assertion. A test without assertions verifies nothing. | Major |
| S3518 | Division by zero | Divisors should not be zero or potentially zero. Guard arithmetic operations. | Blocker |
| S2119 | Random without seed | `Random` objects should be reused. Creating a new `Random()` per call loses entropy. | Major |

---

## 2. Vulnerabilities

Vulnerabilities are code patterns that can be exploited by attackers.

| Rule ID | Name | Description | Severity |
|---------|------|-------------|----------|
| S2076 | OS command injection | User input must never be concatenated into OS commands. Use `ProcessBuilder` with parameterized arguments. | Blocker |
| S3649 | SQL injection | User input must never be concatenated into SQL queries. Use `PreparedStatement` or JPA named parameters. | Blocker |
| S2068 | Hardcoded credentials | Passwords and secrets must not appear in source code. Use environment variables or a secrets manager. | Blocker |
| S4787 | Weak cryptography | Use strong algorithms (AES-256-GCM). The project already uses `AESGCMEncryptionService`. Avoid DES, 3DES, RC4, MD5. | Critical |
| S5145 | Log injection | User-controlled data in log statements should be sanitized to prevent log forging. | Major |
| S5131 | HTTP response splitting | User input should not be used in HTTP headers without validation. | Major |
| S2755 | XML external entity (XXE) | XML parsers should not be vulnerable to XXE attacks. Disable external entity resolution. | Blocker |

---

## 3. Security Hotspots

Security hotspots require manual review to determine if the code is safe in context. They are not necessarily vulnerabilities.

| Rule ID | Name | Description | Review Guidance |
|---------|------|-------------|-----------------|
| S4790 | Hashing without salt | Using a cryptographic hash function without a salt is security-sensitive. Verify salting in password storage. | Check if BCrypt/SCrypt/Argon2 is used (they include salts automatically). |
| S5344 | Hardcoded passwords in config | Passwords appearing in configuration files or property files. | Verify these are development-only defaults, not production secrets. |
| S2245 | Pseudorandom number generators | Using `java.util.Random` is security-sensitive for security-critical operations. | Use `SecureRandom` for tokens, keys, and nonces. `Random` is acceptable for non-security uses. |
| S4423 | Weak TLS protocols | TLS versions < 1.2 should not be used. | Verify the certificate-authority module enforces TLS 1.2+ (already configured in `DualPortConfiguration`). |
| S2092 | Cookies without `Secure` flag | Cookies should have the `Secure` attribute to prevent transmission over HTTP. | Verify Spring Security cookie configuration. |

---

## 4. Code Smells

Code smells are maintainability issues that make code harder to understand, modify, or test.

### 4.1 Complexity and Length

| Rule ID | Name | Sonar Default | Project Convention | Notes |
|---------|------|---------------|-------------------|-------|
| S138 | Method too long | > 50 lines | **< 20 lines** | Project is 2.5x stricter. Extract helper methods aggressively. |
| S3776 | Cognitive complexity too high | > 15 | **< 10** | Project is 1.5x stricter. Prefer early returns, guard clauses, and strategy pattern. |
| S1200 | Class too large | > 1000 lines | Split before reaching this | Modular classes — single responsibility. |
| S1479 | Switch with too many cases | > 30 branches | Polymorphism / Map | Use enum dispatch or `Map<Key, Handler>` pattern. |

### 4.2 Exception Handling

| Rule ID | Name | Description | Project Convention |
|---------|------|-------------|-------------------|
| S1181 | Catch `Throwable` | Catching `Throwable` swallows `Error` subclasses. | **NEVER** catch `Throwable` — catch specific exception types only. |
| S2221 | Catch `Exception` | Catching `Exception` is too broad. | **NEVER** catch generic `Exception` — use the project's exception hierarchy (`EntityNotFoundException`, `EntityAlreadyExistsException`, etc.). |
| S108 | Empty catch block | Exceptions should not be swallowed silently. | Empty catch blocks are **forbidden**. Log or rethrow. |
| S1166 | Exception without context | Exceptions should not be created without a message or cause. | Always include a descriptive message and chain the original cause. |
| S1141 | Nested try blocks | Try statements should not be nested. | Refactor to separate methods. Each method handles one concern. |

### 4.3 Constants and Literals

| Rule ID | Name | Sonar Default | Project Convention | Notes |
|---------|------|---------------|-------------------|-------|
| S1192 | String literal duplication | Flag at 3+ occurrences | **Zero tolerance** | ALL string literals must be `public static final` constants. Shared between implementation and test classes. |
| S109 | Magic numbers | Flag numeric literals | Extract to named constants | Self-documenting code — numbers must have names. |

### 4.4 Code Quality

| Rule ID | Name | Description | Project Convention |
|---------|------|-------------|-------------------|
| S1172 | Unused method parameters | Parameters that are not used should be removed. | Remove dead parameters. If required by interface, annotate with `@SuppressWarnings`. |
| S1481 | Unused local variables | Variables that are declared but not used. | Remove immediately — dead stores waste reader attention. |
| S1135 | `TODO` comments | Track `TODO` comments and resolve them. | Acceptable in development; must be resolved before merge. |
| S106 | `System.out.println` | Standard outputs should not be used for logging. | Use SLF4J (`@Slf4j` via Lombok). Never `System.out` or `System.err`. |
| S1118 | Utility class constructor | Utility classes should not have public constructors. | Use `private` constructor. Pattern: `private ClassName() {}`. |

---

## 5. Project Convention Mapping

This table maps Sonar rules to the corresponding `AI-CODE-REF.md` sections, highlighting where the project enforces stricter standards.

| Sonar Rule | AI-CODE-REF Section | Project Standard | Sonar Default | Strictness |
|------------|-------------------|-----------------|---------------|------------|
| S1192 | Constants & Strings | ALL literals → `public static final` | 3+ occurrences | **Stricter** |
| S109 | Constants & Strings | Named constants for all numbers | Flag magic numbers | Same |
| S138 | Method Design | < 20 lines per method | < 50 lines | **Stricter** |
| S3776 | Method Design | Cognitive complexity < 10 | < 15 | **Stricter** |
| S1181 | Exception Handling | Never catch `Throwable` | Warn on catch `Throwable` | Same intent |
| S2221 | Exception Handling | Never catch generic `Exception` | Warn on catch `Exception` | Same intent |
| S108 | Exception Handling | Empty catch forbidden | Warn on empty catch | Same intent |
| S2076 | Security | Parameterized commands | Flag OS injection | Same |
| S3649 | Security | `PreparedStatement` / JPA params | Flag SQL injection | Same |
| S2068 | Security | Env vars / secrets manager | Flag hardcoded creds | Same |
| S4787 | Security | AES-256-GCM (AESGCMEncryptionService) | Flag weak crypto | Same |
| S106 | Logging | SLF4J via `@Slf4j` | Flag `System.out` | Same |

### Rules Without Sonar Equivalents (Project-Specific)

These project conventions from `AI-CODE-REF.md` are not covered by Sonar rules and must be enforced by code review:

| Convention | Description | Source |
|------------|-------------|--------|
| No `any()` matchers | Mockito `any()`, `anyString()`, `anyInt()` are **forbidden** in tests | AI-CODE-REF.md |
| `@DisplayName` required | Every `@Test` and `@Nested` class must have `@DisplayName` | AI-CODE-REF.md |
| Given/When/Then naming | Test methods: `shouldDoX_whenY()` with Given-When-Then structure | AI-CODE-REF.md |
| Prototype-scoped entities | Entities via `ApplicationContext.getBean()`, never `new Entity()` | AI-CODE-REF.md |
| ID type enforcement | All entity IDs must be `Long`, never `Integer` | AI-CODE-REF.md |
| Named TypeMaps | ModelMapper mappings use named TypeMaps with implicit mapping sandwich | AI-CODE-REF.md |

---

## 6. Quality Profile Summary

The **"Sonar way"** profile for Java includes approximately **600+ rules** across all categories. The rules listed in this document are the most impactful for the AkademiaPlus codebase. The full rule set is available at:

- https://sonarcloud.io/organizations/elatusdev/rules?languages=java
- https://rules.sonarsource.com/java/

### Rule Count by Category (Sonar way)

| Category | Approximate Count | Focus Area |
|----------|------------------|------------|
| Bugs | ~180 | Correctness, null safety, resource management |
| Vulnerabilities | ~40 | OWASP Top 10, injection, crypto |
| Security Hotspots | ~30 | Manual review triggers |
| Code Smells | ~350 | Maintainability, complexity, naming |
