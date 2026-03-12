# AI-CODE-REF.md — AI Coding Standards & Review Reference

**Version**: 4.3
**Last Updated**: March 11, 2026
**Purpose**: Quick reference for AI code analysis, review, refactoring, and test creation
**See also**: [DESIGN.md](../design/DESIGN.md) for architecture and system design

---

## How to Use This Document
- **Detection Patterns**: Use regex/keywords to identify violations
- **Priority**: CRITICAL > HIGH > MEDIUM > LOW
- **Fix Templates**: Reusable patterns for corrections
- **Context-Based**: Rules organized by code review scenarios
- **Test Standards**: Comprehensive testing conventions including Given-When-Then, no any() matchers, and mock stubbing rules

---

## 1. CODE REVIEW DETECTION RULES

### 1.1 NAMING VIOLATIONS

#### RULE: Magic Numbers/Strings
**Detection**: Numeric/string literals used directly in code (except 0, 1, -1, true, false, null, "")
**Priority**: MEDIUM
**SonarQube**: S109, S1192
```java
// DETECT: if (age > 18) { ... }
// DETECT: throw new Exception("invalid email");
// FIX: Extract to named constant
private static final int LEGAL_AGE = 18;
private static final String ERROR_INVALID_EMAIL = "Invalid email address";
```

#### RULE: Poor Variable Names
**Detection**: Single letters (except loop counters), abbreviations, non-descriptive names
**Priority**: LOW
```java
// DETECT: int d, String str, List<User> lst
// FIX: int daysSinceCreation, String emailAddress, List<User> users
```

#### RULE: Inconsistent Naming
**Detection**: Multiple words for same concept (get/fetch/retrieve, data/info, manager/handler)
**Priority**: MEDIUM
```java
// DETECT: getUserData() + fetchUserInfo() + retrieveUserDetails()
// FIX: Choose one: getUser() across all methods
```

---

### 1.2 EXCEPTION HANDLING VIOLATIONS

#### RULE: Generic Exception Catching
**Detection**: `catch (Exception e)`, `catch (Throwable t)`
**Priority**: HIGH
**SonarQube**: S1181, S2221
```java
// DETECT:
try {
    phoneUtil.parse(phoneNumber, region);
} catch (Exception e) { // ❌
    throw new CustomException(e);
}

// FIX: Catch specific exception
try {
    phoneUtil.parse(phoneNumber, region);
} catch (NumberParseException e) { // ✅
    throw new CustomException("Failed to parse phone: " + phoneNumber, e);
}
```

#### RULE: Wrong Exception Type
**Detection**: Using collection exceptions (NoSuchElementException) for validation
**Priority**: HIGH
```java
// DETECT: throw new NoSuchElementException("Invalid input")
// FIX: throw new IllegalArgumentException("Invalid input")
// OR: Custom business exception
```

#### RULE: Swallowed Exceptions
**Detection**: Empty catch blocks, catch without logging/rethrowing
**Priority**: CRITICAL
**SonarQube**: S108, S1166
```java
// DETECT:
catch (IOException e) { } // ❌

// FIX:
catch (IOException e) {
    logger.error("Failed to read file: {}", filename, e);
    throw new DataAccessException("Cannot read file", e);
}
```

#### RULE: Exception Without Context
**Detection**: Exceptions without meaningful messages or original exception
**Priority**: MEDIUM
```java
// DETECT: throw new CustomException(e); // No context
// FIX: throw new CustomException("Failed to normalize phone: " + phone, e);
```

---

### 1.3 METHOD COMPLEXITY VIOLATIONS

#### RULE: Method Too Long
**Detection**: Method > 20 lines (excluding braces/whitespace)
**Priority**: MEDIUM
**SonarQube**: S138
```java
// FIX: Extract Method refactoring
// Break into smaller methods with single responsibility
```

#### RULE: Cyclomatic Complexity
**Detection**: CC > 10 (count decision points: if, for, while, case, &&, ||, ?:)
**Priority**: HIGH
**SonarQube**: S3776
```java
// FIX: 
// 1. Extract nested conditions to methods
// 2. Use polymorphism instead of conditionals
// 3. Apply Strategy/State pattern
```

#### RULE: Deep Nesting
**Detection**: Nesting level > 3
**Priority**: MEDIUM
```java
// DETECT:
if (a) {
    if (b) {
        if (c) {
            if (d) { // ❌ Too deep
                ...
            }
        }
    }
}

// FIX: Guard clauses, early returns, extract methods
if (!a) return;
if (!b) return;
if (!c) return;
processD();
```

---

### 1.4 CODE DUPLICATION VIOLATIONS

#### RULE: Duplicate Code Blocks
**Detection**: Same code appears 2+ times
**Priority**: HIGH
**SonarQube**: CPD (Copy-Paste Detector)
```java
// FIX:
// 1. Extract to method
// 2. Extract to utility class
// 3. Use template method pattern
```

#### RULE: Duplicate Validation Logic
**Detection**: Same validation repeated across methods
**Priority**: MEDIUM
```java
// DETECT:
public void method1(String email) {
    if (email == null || email.trim().isEmpty()) { ... }
}
public void method2(String email) {
    if (email == null || email.trim().isEmpty()) { ... }
}

// FIX: Extract validation method
private void validateEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
        throw new IllegalArgumentException(ERROR_EMAIL_REQUIRED);
    }
}
```

---

### 1.5 SOLID PRINCIPLE VIOLATIONS

#### RULE: Single Responsibility Violation
**Detection**: Class/method does multiple unrelated things
**Priority**: HIGH
**Indicators**:
- Class name contains "And", "Or", "Manager", "Util"
- Method > 20 lines
- Method has multiple reasons to change
```java
// DETECT: UserManagerAndValidator
// FIX: Split into UserManager + UserValidator
```

#### RULE: Open/Closed Violation
**Detection**: Switch statements on type codes, instanceof chains
**Priority**: MEDIUM
```java
// DETECT:
if (shape.type == CIRCLE) { ... }
else if (shape.type == SQUARE) { ... }

// FIX: Polymorphism
interface Shape { double area(); }
class Circle implements Shape { ... }
class Square implements Shape { ... }
```

#### RULE: Dependency Inversion Violation
**Detection**: High-level module depends on low-level concrete classes
**Priority**: MEDIUM
```java
// DETECT: class Service { MySQLDatabase db; } // Concrete dependency
// FIX: class Service { Database db; } // Abstract dependency
```

---

## 2. REFACTORING FIX TEMPLATES

### 2.1 Extract Constant
```java
// PATTERN:
// Before: if (count > 100) { ... }
private static final int MAX_RETRY_COUNT = 100;
// After: if (count > MAX_RETRY_COUNT) { ... }

// NAMING: Use UPPER_SNAKE_CASE for constants
// LOCATION: Top of class (after static variables)
```

### 2.2 Extract Method
```java
// PATTERN:
// Before: Long method with distinct sections
public void process() {
    // validation logic...
    // business logic...
    // persistence logic...
}

// After: Multiple focused methods
public void process() {
    validate();
    executeBusinessLogic();
    persist();
}

private void validate() { ... }
private void executeBusinessLogic() { ... }
private void persist() { ... }

// NAMING: Methods should be verbs
// SIZE: Target 1-10 lines per method
```

### 2.3 Extract Validation Method
```java
// PATTERN:
private void validateNotNullOrEmpty(String value, String fieldName) {
    if (value == null || value.trim().isEmpty()) {
        throw new IllegalArgumentException(
            String.format("%s cannot be null or empty", fieldName)
        );
    }
}

// USAGE:
validateNotNullOrEmpty(email, "Email");
validateNotNullOrEmpty(phone, "Phone number");
```

### 2.4 Replace Exception Type
```java
// PATTERN:
// Before: throw new NoSuchElementException("Invalid input")
// After: throw new IllegalArgumentException("Invalid input")

// GUIDELINES:
// IllegalArgumentException: Invalid method parameters
// IllegalStateException: Object in invalid state
// UnsupportedOperationException: Operation not supported
// Custom exceptions: Domain-specific errors
```

### 2.5 Add Javadoc
```java
// PATTERN:
/**
 * Brief description of what the method does.
 * 
 * <p>Additional details if needed, including:
 * - Expected behavior
 * - Side effects
 * - Algorithm explanation
 * </p>
 *
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When/why this exception is thrown
 * @since Version when added
 */
public ReturnType methodName(ParamType paramName) throws ExceptionType {
    ...
}

// REQUIRED FOR:
// - All public methods/classes
// - Complex private methods
// - Utility methods
```

---

## 3. SONARQUBE CRITICAL RULES

### 3.1 Bugs (Priority: CRITICAL)

| Rule | Detection | Fix |
|------|-----------|-----|
| **S1181** | catch (Throwable) | Catch specific exception |
| **S1215** | GC called explicitly | Remove System.gc() |
| **S2259** | Null pointer dereference | Add null check or use Optional |
| **S2583** | Always true/false condition | Fix logic error |
| **S1862** | Identical branches | Remove duplication |
| **S1854** | Dead store (unused assignment) | Remove unused code |

### 3.2 Vulnerabilities (Priority: CRITICAL)

| Rule | Detection | Fix |
|------|-----------|-----|
| **S2076** | OS command injection | Use parameterized commands |
| **S3649** | SQL injection | Use PreparedStatement |
| **S2068** | Hardcoded credentials | Use environment variables/secrets |
| **S4787** | Weak encryption | Use strong algorithms (AES-256) |

### 3.3 Code Smells (Priority: HIGH)

| Rule | Detection | Fix |
|------|-----------|-----|
| **S1192** | String literal duplication (3+) | Extract to constant |
| **S109** | Magic numbers | Extract to named constant |
| **S2221** | catch (Exception) | Catch specific exception |
| **S1141** | Nested try blocks | Refactor to separate methods |
| **S3776** | Cognitive complexity > 15 | Decompose method |
| **S138** | Method > 50 lines | Extract smaller methods |
| **S1479** | Switch > 30 branches | Use polymorphism/map |
| **S1200** | Class > 1000 lines | Split class |

---

## 4. TESTING STANDARDS

### 4.1 Test Structure (Given-When-Then Pattern)

**CRITICAL**: Always use Given-When-Then pattern, NOT Arrange-Act-Assert

```java
@Test
@DisplayName("Should return lowercase trimmed email when given valid email")
void shouldReturnLowercaseTrimmedEmail_whenGivenValidEmail() {
    // Given: Setup test data and dependencies
    String input = "test@example.com";
    when(repository.findByEmail(input)).thenReturn(Optional.of(user));
    
    // When: Execute the method under test
    Result result = service.processEmail(input);
    
    // Then: Verify the outcome
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue()).isEqualTo(expected);
}
```

**Test Naming Convention:**
- Method: `shouldDoX_whenGivenY()`
- DisplayName: "Should do X when given Y"
- Reads naturally: Focus on behavior first, then condition

### 4.2 Mock Stubbing Rules

**CRITICAL**: Mock with EXACT parameters that implementation passes, NOT transformed values

```java
// ❌ WRONG: Stubbing with transformed parameter
String inputWithWhitespace = "  value  ";
when(mock.parse(inputWithWhitespace.trim())).thenReturn(result);
service.process(inputWithWhitespace); // Calls parse(inputWithWhitespace) - FAILS!

// ✅ CORRECT: Stub with exact parameter
String inputWithWhitespace = "  value  ";
when(mock.parse(inputWithWhitespace)).thenReturn(result);
service.process(inputWithWhitespace); // Calls parse(inputWithWhitespace) - PASSES!
```

**Golden Rule**: Read the implementation to know what's actually passed to mocks

### 4.3 NO any() Matchers Rule

**CRITICAL**: NEVER use `any()`, `anyString()`, `anyInt()`, or any ArgumentMatchers in tests

**Why any() is Problematic:**
1. **Loss of Precision**: Accepts ANY parameter, even wrong ones
2. **Hidden Bugs**: Test passes even if implementation uses wrong values
3. **False Confidence**: Don't know what was actually called

```java
// ❌ WRONG: Using any() matchers
verify(service, never()).process(any());
verify(repository).save(anyString(), any());
when(validator.validate(any())).thenReturn(true);

// ✅ CORRECT: Use exact values or verifyNoMoreInteractions
verify(service, never()).process(expectedValue); // If needed
verifyNoMoreInteractions(service); // Better - verifies nothing else called
when(validator.validate(expectedInput)).thenReturn(true);
```

**Acceptable Alternatives:**
- Use exact values: `verify(mock).method(expectedValue)`
- Use `verifyNoMoreInteractions(mock)`: Comprehensive check
- Use `verifyNoInteractions(mock)`: For complete isolation
- Use ArgumentCaptor: When you need to verify complex objects

```java
// ✅ Using ArgumentCaptor for complex verification
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(repository).save(captor.capture());
User savedUser = captor.getValue();
assertThat(savedUser.getName()).isEqualTo("John");
```

### 4.4 Unit Test Assertion Rules

> **Principle**: If the implementation's structure changes, at least one test MUST fail.
> These rules ensure every unit test is sensitive to structural changes in the code under test.

#### Rule 1: Every test MUST have both state AND interaction assertions

```java
// Then — state: what came back
assertThat(result).isEqualTo(EXPECTED_USER);

// Then — interactions: what was called, in what order, and nothing else
InOrder inOrder = inOrder(validator, repository);
inOrder.verify(validator, times(1)).validate(VALID_EMAIL);
inOrder.verify(repository, times(1)).save(USER_ENTITY);
inOrder.verifyNoMoreInteractions();
```

State-only tests miss added/removed collaborator calls. Interaction-only tests miss wrong return values.

#### Rule 2: `verifyNoMoreInteractions()` on ALL mocks — mandatory

Every `@Test` method MUST end with this. If someone adds a new dependency call, the test fails immediately.

```java
verifyNoMoreInteractions(repository, validator, publisher);
```

#### Rule 3: `verifyNoInteractions()` for short-circuit paths

When validation fails early, explicitly assert that downstream mocks were never touched.

```java
@Test
void shouldThrowValidationException_whenInputIsNull() {
    assertThatThrownBy(() -> useCase.execute(null))
        .isInstanceOf(ValidationException.class)
        .hasMessage(INPUT_REQUIRED);

    verifyNoInteractions(repository, mapper, publisher);
}
```

#### Rule 4: Explicit `times(1)` — no implicit defaults

```java
verify(repository, times(1)).save(entity);  // ✅ intent is clear
verify(repository).save(entity);            // ❌ implicit times(1) hides intent
```

If a refactor changes a single call to a loop, `times(1)` catches it.

#### Rule 5: `InOrder` when sequence matters

If business logic requires validation before persistence, or persistence before event publishing — enforce it.

```java
InOrder inOrder = inOrder(validator, repository, publisher);
inOrder.verify(validator, times(1)).validate(input);
inOrder.verify(repository, times(1)).save(entity);
inOrder.verify(publisher, times(1)).publish(event);
inOrder.verifyNoMoreInteractions();
```

#### Rule 6: Exception assertions MUST verify type AND message

```java
assertThatThrownBy(() -> useCase.execute(input))
    .isInstanceOf(EntityNotFoundException.class)
    .hasMessage(ENTITY_NOT_FOUND_MESSAGE);  // shared constant
```

If someone changes the exception type or message, the test fails.

#### Rule 7: Every parameter MUST be tested for every invalid state

One test per invalid state — never combine multiple invalid inputs in one test.

| Type | Invalid states to test |
|------|----------------------|
| Object / Record | `null` |
| String | `null`, `""` (empty), `"   "` (blank), invalid format |
| Number (Long/Integer) | `null`, `0`, negative, boundary (`MAX_VALUE`) |
| Collection | `null`, empty `List.of()` |
| Enum | `null`, each value that triggers a different path |

Group all input validation tests in a dedicated `@Nested` class:

```java
@Nested
@DisplayName("Input validation")
class InputValidation {

    @Test
    @DisplayName("Should throw when input is null")
    void shouldThrowValidationException_whenInputIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
            .isInstanceOf(ValidationException.class)
            .hasMessage(INPUT_REQUIRED);

        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("Should throw when email is null")
    void shouldThrowValidationException_whenEmailIsNull() {
        assertThatThrownBy(() -> useCase.execute(inputWithEmail(null)))
            .isInstanceOf(ValidationException.class)
            .hasMessage(EMAIL_REQUIRED);

        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("Should throw when email is empty")
    void shouldThrowValidationException_whenEmailIsEmpty() {
        assertThatThrownBy(() -> useCase.execute(inputWithEmail("")))
            .isInstanceOf(ValidationException.class)
            .hasMessage(EMAIL_REQUIRED);

        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("Should throw when email is blank")
    void shouldThrowValidationException_whenEmailIsBlank() {
        assertThatThrownBy(() -> useCase.execute(inputWithEmail("   ")))
            .isInstanceOf(ValidationException.class)
            .hasMessage(EMAIL_REQUIRED);

        verifyNoInteractions(repository, mapper);
    }
}
```

#### Rule 8: Every line that throws MUST have a dedicated test

Read the implementation. For every `if`/`throw`, `switch` branch, guard clause, or early return — write a test that triggers it.

Given this implementation:
```java
public User execute(CreateUserInput input) {
    if (input == null) throw new ValidationException(INPUT_REQUIRED);           // line 1
    if (input.email() == null || input.email().isBlank())                       // line 2
        throw new ValidationException(EMAIL_REQUIRED);
    if (!emailValidator.isValid(input.email()))                                 // line 3
        throw new ValidationException(EMAIL_INVALID_FORMAT);
    if (repository.existsByEmail(input.email()))                                // line 4
        throw new EntityAlreadyExistsException(EMAIL_ALREADY_EXISTS);

    User user = mapper.toEntity(input);                                        // line 5
    return repository.save(user);                                              // line 6
}
```

Required test map — one per throwing line, plus happy path:
```
InputValidation/
  ├── shouldThrowValidationException_whenInputIsNull            → line 1
  ├── shouldThrowValidationException_whenEmailIsNull            → line 2 (null)
  ├── shouldThrowValidationException_whenEmailIsEmpty           → line 2 (empty)
  ├── shouldThrowValidationException_whenEmailIsBlank           → line 2 (blank)
  ├── shouldThrowValidationException_whenEmailFormatInvalid     → line 3
  └── shouldThrowAlreadyExistsException_whenEmailAlreadyExists  → line 4

HappyPath/
  └── shouldSaveAndReturnUser_whenValidInput                    → lines 5-6
```

#### Rule 9: Exception tests MUST verify the cutoff point

Each exception test asserts **two things**: the exact exception AND that nothing after the throwing line was called.

```java
@Test
void shouldThrowValidationException_whenEmailFormatInvalid() {
    // Given
    when(emailValidator.isValid(INVALID_EMAIL)).thenReturn(false);

    // When / Then
    assertThatThrownBy(() -> useCase.execute(inputWithEmail(INVALID_EMAIL)))
        .isInstanceOf(ValidationException.class)
        .hasMessage(EMAIL_INVALID_FORMAT);

    // Verify cutoff: validator was called, but nothing after
    verify(emailValidator, times(1)).isValid(INVALID_EMAIL);
    verifyNoInteractions(repository, mapper);
}
```

This proves the method stopped exactly where expected. If someone reorders the guards, the test breaks.

#### Rule 10: Collaborator exceptions MUST be tested for propagation

If a collaborator can throw, test what the method under test does with that exception.

```java
@Test
void shouldPropagateRepositoryException_whenSaveFails() {
    // Given
    when(emailValidator.isValid(VALID_EMAIL)).thenReturn(true);
    when(repository.existsByEmail(VALID_EMAIL)).thenReturn(false);
    when(mapper.toEntity(VALID_INPUT)).thenReturn(USER_ENTITY);
    when(repository.save(USER_ENTITY)).thenThrow(new DataAccessException(DB_ERROR));

    // When / Then
    assertThatThrownBy(() -> useCase.execute(VALID_INPUT))
        .isInstanceOf(DataAccessException.class)
        .hasMessage(DB_ERROR);

    InOrder inOrder = inOrder(emailValidator, repository, mapper);
    inOrder.verify(emailValidator, times(1)).isValid(VALID_EMAIL);
    inOrder.verify(repository, times(1)).existsByEmail(VALID_EMAIL);
    inOrder.verify(mapper, times(1)).toEntity(VALID_INPUT);
    inOrder.verify(repository, times(1)).save(USER_ENTITY);
    inOrder.verifyNoMoreInteractions();
}
```

#### What These Rules Catch

| Code Change | Catching Rule |
|-------------|---------------|
| Add a new dependency call | Rule 2: `verifyNoMoreInteractions` |
| Remove an existing call | Rule 4: `times(1)` |
| Change call parameters | §4.3: zero `any()` rule |
| Change call order | Rule 5: `InOrder` |
| Change call count (1→N) | Rule 4: `times(1)` |
| Change exception type | Rule 6: `isInstanceOf` |
| Change error message | Rule 6: `hasMessage` |
| Skip validation on error path | Rule 3: `verifyNoInteractions` |
| Add new guard clause without test | Rule 8: every throwing line tested |
| Reorder guard clauses | Rule 9: cutoff verification |
| Remove input validation | Rule 7: input coverage |
| Unhandled collaborator exception | Rule 10: propagation test |

#### Complete Unit Test Anatomy

```java
@Test
@DisplayName("Should save normalized user when valid input")
void shouldSaveNormalizedUser_whenValidInput() {
    // Given
    when(validator.validate(INPUT_EMAIL)).thenReturn(valid());
    when(repository.save(NORMALIZED_USER)).thenReturn(SAVED_USER);

    // When
    User result = useCase.execute(INPUT_EMAIL);

    // Then — state
    assertThat(result).isEqualTo(SAVED_USER);

    // Then — interactions (ordered)
    InOrder inOrder = inOrder(validator, repository);
    inOrder.verify(validator, times(1)).validate(INPUT_EMAIL);
    inOrder.verify(repository, times(1)).save(NORMALIZED_USER);
    inOrder.verifyNoMoreInteractions();
}
```

### 4.5 Variable Extraction Rules

**RULE: Extract to local variable if:**
1. Value used only in one test
2. Value is "magic" (not defined as constant)
3. Improves readability

**RULE: Use class-level constant if:**
1. Value used in multiple tests
2. Value has business meaning
3. Value represents test configuration

```java
// CLASS-LEVEL: Used in multiple tests
private static final String VALID_EMAIL = "test@example.com";
private static final int MAX_RETRIES = 3;
private static final String ERROR_MESSAGE_INVALID = "is invalid";

// METHOD-LEVEL: Used in single test only
@Test
@DisplayName("Should throw exception when given array exceeding limit")
void shouldThrowException_whenGivenArrayExceedingLimit() {
    // Given - Magic array used once
    Long[] tooManyIds = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
    
    // When & Then
    assertThatThrownBy(() -> service.process(tooManyIds))
        .isInstanceOf(IllegalArgumentException.class);
}

// ❌ DON'T extract if already a constant
@Test
void shouldUseExistingConstant() {
    // Given
    // No need: String expected = VALID_EMAIL; // Redundant!
    
    // When
    String result = service.normalize(VALID_EMAIL);
    
    // Then
    assertThat(result).isEqualTo(VALID_EMAIL); // Use constant directly
}
```

### 4.6 Test Organization

**Use @Nested classes for logical grouping:**
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {
    
    @Nested
    @DisplayName("User Creation Tests")
    class UserCreationTests {
        // All user creation tests here
    }
    
    @Nested
    @DisplayName("User Validation Tests")
    class UserValidationTests {
        // All validation tests here
    }
}
```

**Benefits:**
- Clear test organization
- Related tests grouped together
- Better test reports
- Easier to navigate

### 4.7 Test Method Naming Examples

```java
// ✅ GOOD: Natural language, behavior-focused
@DisplayName("Should return user when given valid ID")
void shouldReturnUser_whenGivenValidId()

@DisplayName("Should throw exception when given null email")
void shouldThrowException_whenGivenNullEmail()

@DisplayName("Should normalize to lowercase when given uppercase email")
void shouldNormalizeToLowercase_whenGivenUppercaseEmail()

// ❌ BAD: Method-centric, not behavior-focused
void testGetUser()
void testNullEmail()
void normalizeEmail_uppercase()
```

### 4.8 Exception Testing Patterns

> See §4.4 Rules 6, 8, 9, 10 for the full assertion requirements.

```java
// ✅ Comprehensive exception testing — verifies type, message, cause, and cutoff
@Test
@DisplayName("Should throw processing exception with context when parsing fails")
void shouldThrowProcessingException_whenParsingFails() throws Exception {
    // Given
    when(parser.parse(INVALID_INPUT))
        .thenThrow(new ParseException(PARSE_ERROR_MESSAGE));

    // When / Then — state: exception type + message + cause (Rule 6)
    assertThatThrownBy(() -> service.process(INVALID_INPUT))
        .isInstanceOf(ProcessingException.class)
        .hasMessage(PROCESSING_FAILED_MESSAGE)
        .hasCauseInstanceOf(ParseException.class);

    // Then — interactions: verify cutoff point (Rule 9)
    verify(parser, times(1)).parse(INVALID_INPUT);
    verifyNoInteractions(repository, publisher);  // nothing after parser was called
    verifyNoMoreInteractions(parser);
}
```

### 4.9 Test Data Builders

**Use helper methods for complex test data:**
```java
// ✅ Helper methods for reusable test data
private User createValidUser() {
    User user = new User();
    user.setName("John Doe");
    user.setEmail("john@example.com");
    user.setAge(30);
    return user;
}

private User createUserWithAge(int age) {
    User user = createValidUser();
    user.setAge(age);
    return user;
}

@Test
void shouldValidateUser_whenGivenValidAge() {
    // Given
    User user = createUserWithAge(25);
    
    // When & Then
    assertThat(validator.isValid(user)).isTrue();
}
```

### 4.10 Void Method Stubbing (Strict Stubbing)

**CRITICAL**: When a mock has both void and return overloads of the same method (e.g., `ModelMapper.map()`),
Mockito strict stubbing requires ALL invocations to be declared — otherwise `PotentialStubbingProblem` is thrown.

```java
// ❌ WRONG: Only stubs the return overload — strict stubbing flags the void overload
when(modelMapper.map(savedModel, TenantDTO.class)).thenReturn(expectedDto);
// transform() calls modelMapper.map(dto, prototypeModel) → PotentialStubbingProblem!

// ✅ CORRECT: Stub BOTH overloads explicitly
doNothing().when(modelMapper).map(dto, prototypeModel);                    // void overload in transform()
when(modelMapper.map(savedModel, TenantDTO.class)).thenReturn(expectedDto); // return overload in create()
```

**Rule**: When a method under test calls the same mock through multiple overloads,
stub every overload that will be invoked. Use `doNothing().when(mock).method(args)` for void methods.

### 4.11 E2E Test Assertion Rules

> **Principle**: If the API contract or user experience changes, at least one E2E test MUST fail.

#### 4.11.1 Backend E2E Rules (Newman)

##### Per-Request Assertions (every request, no exceptions)

| # | Rule | What It Catches |
|---|------|-----------------|
| E1 | **Exact HTTP status code** | Status code regressions (201→200, 404→500) |
| E2 | **Content-Type `application/json`** on every non-204 response | Serialization config changes |
| E3 | **Response time < 200ms** | Performance regressions |

```javascript
// ✅ Every request MUST have these three
pm.test("Status code is 201", () => {
    pm.response.to.have.status(201);
});

pm.test("Response time is less than 200ms", () => {
    pm.expect(pm.response.responseTime).to.be.below(200);
});

pm.test("Content-Type is application/json", () => {
    pm.response.to.have.header("Content-Type", /application\/json/);
});
```

##### Success Response Assertions (2xx)

| # | Rule | What It Catches |
|---|------|-----------------|
| E4 | **All fields present with correct types** — every DTO field asserted: numbers are numbers, strings non-empty, dates valid ISO, arrays are arrays | Schema drift, null fields, type coercion |
| E5 | **Entity IDs are positive numbers** — `pm.expect(json.entityId).to.be.above(0)` | ID generation failures |
| E6 | **Nested objects validated** — nested DTOs assert their fields too | Incomplete serialization |

```javascript
// ✅ Schema validation — assert every field
pm.test("Response has correct schema", () => {
    const json = pm.response.json();
    pm.expect(json.employeeId).to.be.a("number").and.to.be.above(0);
    pm.expect(json.firstName).to.be.a("string").and.to.not.be.empty;
    pm.expect(json.lastName).to.be.a("string").and.to.not.be.empty;
    pm.expect(json.email).to.be.a("string").and.to.include("@");
    pm.expect(json.createdAt).to.match(/^\d{4}-\d{2}-\d{2}/);
});
```

##### Error Response Assertions (4xx)

| # | Rule | What It Catches |
|---|------|-----------------|
| E7 | **`code` field matches exact error code** (`ENTITY_NOT_FOUND`, `DUPLICATE_ENTITY`, `VALIDATION_ERROR`, etc.) | Exception handler changes, wrong exception thrown |
| E8 | **`message` field is present and non-empty** | Missing error context |
| E9 | **400 `VALIDATION_ERROR` responses assert `details[]`** with field-level errors | Validation framework changes |

```javascript
// ✅ Error response validation
pm.test("Error code is ENTITY_NOT_FOUND", () => {
    const json = pm.response.json();
    pm.expect(json.code).to.eql("ENTITY_NOT_FOUND");
    pm.expect(json.message).to.be.a("string").and.to.not.be.empty;
});

// ✅ Validation error with field-level details
pm.test("Validation error has field details", () => {
    const json = pm.response.json();
    pm.expect(json.code).to.eql("VALIDATION_ERROR");
    pm.expect(json.details).to.be.an("array").and.to.not.be.empty;
});
```

##### CRUD Lifecycle Assertions (per entity)

| # | Rule | What It Catches |
|---|------|-----------------|
| E10 | **Create → GetById chain** — ID from Create used in GetById, response data matches | Data persistence failures |
| E11 | **GetAll includes created entity** — assert array contains the entity | Query/filter regressions |
| E12 | **Delete → GetById 404** — after 204, GetById returns 404 `ENTITY_NOT_FOUND` | Soft delete `@SQLRestriction` broken |
| E13 | **Duplicate creation → 409** — same unique field returns `DUPLICATE_ENTITY` | Unique constraint dropped |
| E14 | **Delete with dependents → 409** — returns `DELETION_CONSTRAINT_VIOLATION` or `DELETION_BUSINESS_RULE` | FK/business rule enforcement broken |

##### Cross-Cutting Assertions

| # | Rule | What It Catches |
|---|------|-----------------|
| E15 | **Auth enforcement** — requests without valid JWT return 401 | Security filter misconfiguration |
| E16 | **Tenant isolation** — requests without `X-Tenant-Id` are rejected | Tenant filter bypass |

##### Mandatory Test Scenarios Per Entity

Every entity MUST have these 9 requests minimum:

```
1. Create              → 201 + full schema + field types + ID > 0
2. Create duplicate    → 409 + DUPLICATE_ENTITY (per unique constraint)
3. GetById             → 200 + full schema + matches created data
4. GetById not found   → 404 + ENTITY_NOT_FOUND
5. GetAll              → 200 + array + contains created entity
6. Delete              → 204
7. Delete not found    → 404 + ENTITY_NOT_FOUND
8. Delete constrained  → 409 + DELETION_CONSTRAINT_VIOLATION or DELETION_BUSINESS_RULE
9. GetById post-delete → 404 + ENTITY_NOT_FOUND (proves soft delete)
```

#### 4.11.2 Frontend E2E Rules (Playwright)

##### Per-Page Assertions

| # | Rule | What It Catches |
|---|------|-----------------|
| P1 | **Content over URL** — every navigation asserts visible content (`getByRole`, `getByText`), never URL alone (AP16) | Page renders blank but URL correct |
| P2 | **Page heading visible** — every page asserts its primary heading renders | Component mount failure, i18n key missing |
| P3 | **Accessibility audit** — every new page runs axe-core, zero WCAG 2.1 AA violations | Accessibility regressions |

```typescript
// ✅ Content over URL — always assert visible content after navigation
await page.getByRole('link', { name: /panel/i }).click();
await expect(page).toHaveURL('/dashboard');
await expect(page.getByRole('heading', { name: /panel de control/i })).toBeVisible();

// ❌ URL-only assertion (AP16)
await page.getByRole('link', { name: /panel/i }).click();
await expect(page).toHaveURL('/dashboard');  // page could be blank
```

##### Form Validation Assertions

| # | Rule | What It Catches |
|---|------|-----------------|
| P4 | **Every required field tested empty** — submit with field empty, assert specific error message | Missing validation rules |
| P5 | **Invalid format tested** — email, phone, dates with bad format, assert specific error | Regex/validation changes |
| P6 | **Valid submission renders success state** — assert success message, redirect, or updated data | Form handler broken |

##### API-Driven Rendering Assertions

| # | Rule | What It Catches |
|---|------|-----------------|
| P7 | **Mock API → verify data renders** — mock with `page.route()`, assert specific values visible on screen | Data binding broken, wrong field |
| P8 | **Error state rendering** — mock 4xx/5xx, assert user-facing error message visible | Silent failures, missing error handling |
| P9 | **Loading state** — assert loading indicator before data resolves | Missing loading feedback |
| P10 | **Empty state** — mock empty array, assert empty state message visible | Missing empty state handling |

```typescript
// ✅ Mock API and verify data renders on screen
await page.route('**/api/v1/employees', async (route) => {
    await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ employeeId: 1, firstName: 'Ana', lastName: 'García' }]),
    });
});
await page.goto('/employees');
await expect(page.getByText('Ana García')).toBeVisible();

// ✅ Error state — mock failure and verify user-facing message
await page.route('**/api/v1/employees', async (route) => {
    await route.fulfill({ status: 500, body: JSON.stringify({ code: 'INTERNAL_ERROR' }) });
});
await page.goto('/employees');
await expect(page.getByText(/error al cargar/i)).toBeVisible();
```

##### Auth & Navigation Assertions

| # | Rule | What It Catches |
|---|------|-----------------|
| P11 | **Auth guard enforcement** — navigate to protected route without auth, assert redirect to login | Auth guard disabled |
| P12 | **Post-auth navigation** — after auth injection, protected routes render content (no redirect flash) | Auth race condition (D17) |

##### Multi-Step Flow Assertions

| # | Rule | What It Catches |
|---|------|-----------------|
| P13 | **Step indicator state** — at each step, assert active step highlighted, previous completed | Stepper state broken |
| P14 | **Step data persistence** — advance, go back, assert previous step data preserved | State lost on navigation |
| P15 | **Final submission** — complete all steps, submit, assert success state | End-to-end flow broken |

#### What These Rules Catch

| Contract Change | Catching Rule |
|----------------|---------------|
| Status code changed | E1 |
| Response field removed/renamed | E4 |
| Error code changed | E7 |
| Performance regression | E3 |
| Soft delete broken | E12 |
| Unique constraint dropped | E13 |
| Security filter removed | E15 |
| Tenant filter bypassed | E16 |
| Page renders blank | P1 (AP16) |
| Form validation removed | P4, P5 |
| API error not shown to user | P8 |
| Auth guard disabled | P11 |
| Wizard loses data | P14 |
| Accessibility regression | P3 |

### 4.12 Testing Checklist

**Before committing tests:**
- [ ] Uses Given-When-Then pattern (NOT AAA)
- [ ] Test names: `shouldDoX_whenGivenY()`
- [ ] DisplayNames in natural language
- [ ] Zero `any()` matchers
- [ ] Mock stubbing matches implementation (exact parameters)
- [ ] Every test has both state AND interaction assertions (§4.4 Rule 1)
- [ ] `verifyNoMoreInteractions()` on ALL mocks at end of every test (§4.4 Rule 2)
- [ ] `verifyNoInteractions()` on downstream mocks for short-circuit paths (§4.4 Rule 3)
- [ ] Explicit `times(1)` on every `verify()` call (§4.4 Rule 4)
- [ ] `InOrder` used when execution sequence matters (§4.4 Rule 5)
- [ ] Exception assertions verify type AND message with shared constants (§4.4 Rule 6)
- [ ] Every parameter tested for every invalid state — one test per state (§4.4 Rule 7)
- [ ] Every `if`/`throw` line in the implementation has a dedicated test (§4.4 Rule 8)
- [ ] Exception tests verify cutoff point — downstream mocks untouched (§4.4 Rule 9)
- [ ] Collaborator exceptions tested for propagation (§4.4 Rule 10)
- [ ] Input validation tests grouped in `@Nested` `InputValidation` class
- [ ] Local vars for test-specific values
- [ ] Class constants for shared values
- [ ] Helper methods for complex setup
- [ ] No commented-out tests

**E2E Tests — Backend (Newman):**
- [ ] Every request asserts exact HTTP status code (§4.11 E1)
- [ ] Every non-204 response asserts Content-Type `application/json` (§4.11 E2)
- [ ] Every request asserts response time < 200ms (§4.11 E3)
- [ ] Success responses assert all DTO fields with correct types (§4.11 E4)
- [ ] Entity IDs asserted as positive numbers (§4.11 E5)
- [ ] Error responses assert exact error code (§4.11 E7)
- [ ] Error responses assert non-empty message (§4.11 E8)
- [ ] 400 responses assert `details[]` array (§4.11 E9)
- [ ] Every entity has 9 mandatory scenarios (§4.11)
- [ ] Auth enforcement tested — 401 without JWT (§4.11 E15)
- [ ] Tenant isolation tested — rejected without X-Tenant-Id (§4.11 E16)

**E2E Tests — Frontend (Playwright):**
- [ ] Every navigation asserts visible content, never URL alone (§4.11 P1, AP16)
- [ ] Every page asserts primary heading visible (§4.11 P2)
- [ ] Every new page passes axe-core WCAG 2.1 AA audit (§4.11 P3)
- [ ] Every required form field tested empty (§4.11 P4)
- [ ] Invalid format tested for formatted fields (§4.11 P5)
- [ ] Valid submission asserts success state (§4.11 P6)
- [ ] API-dependent pages mock responses and assert data visible (§4.11 P7)
- [ ] API error responses render user-facing error message (§4.11 P8)
- [ ] Loading state asserted before data resolves (§4.11 P9)
- [ ] Empty state asserted for empty data (§4.11 P10)
- [ ] Auth guard redirects unauthenticated users (§4.11 P11)
- [ ] Multi-step flows verify step state, data persistence, final submission (§4.11 P13-P15)

### 4.13 TEST-SPECIFIC DETECTION KEYWORDS FOR AI

#### Critical Test Violations (Priority: CRITICAL)
**Detection Keywords:**
- `any()` - ArgumentMatchers usage
- `anyString()` - String matcher usage
- `anyInt()` - Primitive matcher usage
- `anyLong()` - Long matcher usage
- `anyObject()` - Object matcher usage
- `Arrange` comment - Wrong pattern (should be Given)
- `Act` comment - Wrong pattern (should be When)
- `Assert` comment - Wrong pattern (should be Then)
- `testMethodName` - Wrong naming pattern (should be shouldDoX_whenY)

#### High Priority Test Violations
**Detection Patterns:**
- Mock stubbing with transformed parameters (e.g., `.trim()`, `.toLowerCase()`)
- Missing `@DisplayName` annotations
- Test methods without `void shouldX_whenY()` pattern
- `verify(..., never()).method(any())` — should use `verifyNoMoreInteractions()`
- Multiple redundant `verify(mock, never())` calls
- Exception assertions without message verification
- `verify(mock).method(` without explicit `times(1)` — must use `verify(mock, times(1))`
- Tests with state assertions only — missing `verify()`/`verifyNoMoreInteractions()`
- Tests with interaction assertions only — missing `assertThat()`/`assertThatThrownBy()`
- Exception tests without cutoff verification (`verifyNoInteractions` on downstream mocks)
- Missing `verifyNoMoreInteractions()` at end of test method
- Guard clause / `if`/`throw` in implementation without corresponding test
- Multiple invalid inputs combined in a single test method

#### E2E Test Violations (Priority: HIGH)
**Detection Patterns (Newman):**
- Newman test without status code assertion
- Newman test without response time assertion (`pm.response.responseTime`)
- Newman test without Content-Type assertion (non-204)
- Newman success response without field type assertions (`to.be.a("number")`, `to.be.a("string")`)
- Newman error response without `code` field assertion (`json.code`)
- Newman error response without `message` field assertion
- Newman 400 response without `details[]` assertion
- Entity missing any of the 9 mandatory test scenarios
- Newman test asserting only status code — must also validate response body

**Detection Patterns (Playwright):**
- `toHaveURL` without subsequent content assertion (`getByRole`, `getByText`) — AP16 violation
- `waitForTimeout` usage — rely on auto-wait + `expect()` instead
- CSS selectors (`page.$('.class')`, `page.$('#id')`) — use semantic locators
- English text in locators — must use Spanish with `/i` regex
- Missing `page.route()` — frontend E2E must mock API, not hit live backend
- Protected route test without auth guard assertion
- Form test without empty field validation
- Page test without accessibility audit (`AxeBuilder`)

#### Test Code Smells
**Detection Patterns:**
- Redundant local variables for class constants
- Test classes without `@Nested` organization
- Helper methods that could be extracted
- Tests without clear Given-When-Then sections
- Input validation tests not grouped in `@Nested InputValidation` class
- Collaborator exception propagation not tested

**Fix Actions:**
```java
// DETECT: any() usage
verify(mock).method(any());
// FIX: Use exact value or verifyNoMoreInteractions
verify(mock).method(expectedValue);
verifyNoMoreInteractions(mock);

// DETECT: Arrange-Act-Assert
// Arrange
// Act  
// Assert
// FIX: Change to Given-When-Then
// Given
// When
// Then

// DETECT: Wrong test naming
void testUserCreation()
void test_ValidEmail()
// FIX: Use shouldDoX_whenY pattern
void shouldCreateUser_whenGivenValidData()
void shouldNormalizeEmail_whenGivenValidEmail()
```

---

## 5. DOCUMENTATION REQUIREMENTS

### 5.1 When Javadoc is REQUIRED
- [ ] All public classes
- [ ] All public methods
- [ ] All public constants/fields
- [ ] Complex private methods (> 10 lines or complex logic)
- [ ] Utility methods
- [ ] Exception classes

### 5.2 When Javadoc is OPTIONAL
- [ ] Simple getters/setters
- [ ] Obvious private helper methods
- [ ] Overridden methods (if parent has docs)
- [ ] Test methods (use descriptive names instead)

### 5.3 Javadoc Quality Checklist
- [ ] Brief summary (< 80 chars) in first sentence
- [ ] Explains WHAT and WHY (not HOW - that's in code)
- [ ] Documents all parameters
- [ ] Documents return value (if not void)
- [ ] Documents all thrown exceptions
- [ ] Includes examples for complex methods
- [ ] Uses `<p>` for paragraphs
- [ ] Uses `{@code}` for code snippets
- [ ] Uses `{@link}` for cross-references

---

## 6. CODE REVIEW CHECKLIST

### 6.1 Automated Checks (Can be detected programmatically)
- [ ] No magic numbers/strings
- [ ] No generic exception catching
- [ ] No swallowed exceptions
- [ ] Method length < 20 lines
- [ ] Cyclomatic complexity < 10
- [ ] No code duplication
- [ ] All public APIs have Javadoc
- [ ] Consistent naming conventions
- [ ] No commented-out code
- [ ] No System.out.println (use logger)

### 6.2 Manual Review (Requires human judgment)
- [ ] Correct algorithm/logic
- [ ] Proper error handling
- [ ] Security considerations
- [ ] Performance implications
- [ ] Thread safety if needed
- [ ] Transaction boundaries correct
- [ ] SOLID principles followed
- [ ] Appropriate design patterns
- [ ] Test coverage adequate
- [ ] Edge cases handled

---

## 7. REFACTORING DECISION TREE

```
CODE VIOLATION DETECTED
        |
        v
Is it a BUG or VULNERABILITY?
├─ YES → Fix immediately (CRITICAL priority)
└─ NO → Is it a CODE SMELL?
          ├─ YES → Check complexity/duplication
          │       ├─ HIGH (CC>15, duplication>3) → Refactor now
          │       └─ MEDIUM → Add to backlog
          └─ NO → Is it a style/documentation issue?
                  ├─ YES → Fix if touching file anyway
                  └─ NO → Leave as-is
```

---

## 8. COMMON REFACTORING PATTERNS

### Pattern 1: Long Method → Extract Method
```java
// BEFORE:
public void process(Order order) {
    // 50 lines of validation
    // 30 lines of calculation
    // 20 lines of persistence
}

// AFTER:
public void process(Order order) {
    validate(order);
    calculate(order);
    persist(order);
}
```

### Pattern 2: Duplicate Code → Extract Method/Utility
```java
// BEFORE: Same validation in multiple methods
// AFTER: Single validation method reused
```

### Pattern 3: Magic Values → Named Constants
```java
// BEFORE: if (status == 1)
// AFTER: if (status == STATUS_ACTIVE)
```

### Pattern 4: Complex Condition → Extract to Method
```java
// BEFORE: if (user.age > 18 && user.verified && !user.blocked)
// AFTER: if (isEligibleUser(user))
```

### Pattern 5: Switch on Type → Polymorphism
```java
// BEFORE: switch (shape.getType()) { case CIRCLE: ... }
// AFTER: shape.draw() // Each shape implements draw()
```

---

## 9. LANGUAGE-SPECIFIC GUIDELINES

### Java Best Practices
- Use `Optional` instead of returning null
- Use `Stream` API for collections (when readable)
- Use `@Override` annotation
- Use diamond operator `<>` for generics
- Use try-with-resources for AutoCloseable
- Use `StringBuilder` for string concatenation in loops
- Prefer `List<T>` over `T[]` for return types
- Use `BigDecimal` for money calculations

### Exception Hierarchy
```
Throwable
├─ Error (JVM errors - don't catch)
└─ Exception
   ├─ RuntimeException (Unchecked)
   │  ├─ IllegalArgumentException (bad input)
   │  ├─ IllegalStateException (invalid state)
   │  ├─ NullPointerException (null reference)
   │  └─ UnsupportedOperationException
   └─ Checked Exceptions
      ├─ IOException
      ├─ SQLException
      └─ CustomBusinessException
```

---

## 10. PRIORITY MATRIX

| Issue Type | Priority | Action |
|------------|----------|--------|
| Security vulnerability | CRITICAL | Fix immediately |
| Bug causing incorrect behavior | CRITICAL | Fix immediately |
| NullPointerException risk | HIGH | Fix in current sprint |
| Generic exception catching | HIGH | Fix in current sprint |
| Code duplication (3+ places) | HIGH | Fix in current sprint |
| Method > 30 lines | MEDIUM | Refactor if touching file |
| Missing Javadoc (public API) | MEDIUM | Add if touching file |
| Magic numbers | LOW | Fix if convenient |
| Naming inconsistency | LOW | Fix if convenient |

---

## 11. QUICK REFERENCE: SMELL → FIX

| Code Smell | Quick Fix |
|------------|-----------|
| Duplicated code | Extract method |
| Long method | Extract smaller methods |
| Large class | Extract class |
| Long parameter list | Parameter object |
| Divergent change | Extract class |
| Feature envy | Move method |
| Data clumps | Extract class |
| Primitive obsession | Value object |
| Switch statements | Polymorphism |
| Lazy class | Inline or merge |
| Speculative generality | Remove unused abstraction |
| Temporary field | Extract class |
| Message chains | Hide delegate |
| Middle man | Remove middle man |
| Inappropriate intimacy | Move or extract |
| Comments | Rename/extract to make obvious |

---

## 12. DETECTION KEYWORDS FOR AI

### High Priority Flags (Production Code)
- `catch (Exception`
- `catch (Throwable`
- `System.out.println`
- `System.err.println`
- Hardcoded IPs/URLs/credentials
- `TODO` without ticket reference
- Commented-out code blocks
- Methods > 30 lines

### High Priority Flags (Test Code)
- `any()` or `anyString()` or `anyInt()` etc.
- `ArgumentMatchers.` imports
- `Arrange` / `Act` / `Assert` comments
- `void test` method names (not following shouldX_whenY pattern)
- Missing `@DisplayName` on test methods
- Mock stubbing with transformed parameters

### Medium Priority Flags (Production Code)
- String/number literals repeated 3+ times
- Methods without Javadoc (public)
- Deep nesting (4+ levels)
- Long parameter lists (5+ params)
- Class names with "Manager", "Util", "Helper"
- `instanceof` chains

### Medium Priority Flags (Test Code)
- `verify(mock, never()).method(any())`
- Multiple `never()` verifications (use `verifyNoMoreInteractions()`)
- Redundant test verifications
- Test classes without `@Nested` organization
- Exception tests without message verification

### Refactoring Opportunities (Production Code)
- Duplicate code blocks
- Complex boolean conditions
- Switch statements on types
- Getter/setter only classes
- God classes (> 500 lines)

### Refactoring Opportunities (Test Code)
- Redundant local variables for constants
- Helper methods that could be extracted
- Tests without clear Given-When-Then structure
- Over-complex test setup

---

## 13. WORKFLOW & PROMPT DOCUMENTATION

### 13.1 When Required

Workflow + prompt documentation is required for:
- **Multi-file features** — 3+ new production files
- **Cross-module changes** — modifications spanning 2+ modules
- **New entity introduction** — any new JPA entity or entity cluster
- **New delivery channel / integration** — new strategy implementation or external service integration

### 13.2 Workflow Document Format

**Reference**: `docs/workflows/completed/sse-notification-delivery-workflow.md`

Required sections:

1. **Architecture Overview** — data flow diagrams, strategy patterns, module placement, transport decisions
2. **File Inventory** — new files table (file, package, responsibility), modified files table (file, change), OpenAPI changes, test files table — all with phase column
3. **Implementation Sequence** — phase dependency graph (ASCII), strict execution order
4. **Phase Details** — per phase: file paths, public API signatures (method tables), code snippets, constants, design decisions, test plan (@Nested + test method table), verification gate (`mvn` command)
5. **Key Design Decisions** — trade-off tables (Factor / Option A / Option B) with rationale
6. **Multi-Tenancy Considerations** — composite keys, tenant context, Hibernate filters, cross-tenant queries
7. **Future Extensibility** — deferred features, upgrade paths, scaling considerations
8. **Verification Checklist** — final `mvn` commands (compile, test, full build), convention compliance checks (`grep` for `any()`, AAA, generic catches), copyright header verification
9. **Critical Reminders** — project convention guardrails (prototype beans, ID assignment, named TypeMaps, constants, testing rules, copyright, commits, Long IDs, `@Transactional` placement)

### 13.3 Prompt Document Format

**Reference**: `docs/prompts/pending/oauth-social-login-prompt.md`

Required sections:

1. **Header block** — Target (`Claude Code CLI`), Repo (absolute path), Spec (link to workflow), Prerequisites (directives to read)
2. **EXECUTION RULES** — numbered, immutable constraints:
   - Sequential phase execution (no skipping)
   - "Read first" before writing code
   - Compile gate after each code phase
   - Test gate after each test phase
   - Copyright header on all new files
   - Javadoc on all public classes/methods
   - Test conventions (`shouldDoX_whenY`, `@DisplayName`, Given-When-Then, zero `any()`)
   - Constants for all string literals
   - `applicationContext.getBean()` for entity instantiation
   - Read existing files before modifying
   - Commit after each phase with provided message
3. **Phase N** — per phase:
   - "Read first" commands (bash `cat` / `grep` / `find`)
   - Step N.M with exact file paths, code snippets, and instructions
   - Verify Phase N (`mvn` command)
   - Commit Phase N (Conventional Commits message with scope)
4. **VERIFICATION CHECKLIST** — final end-to-end checks:
   - Full compilation (`mvn clean install -DskipTests`)
   - Module-specific test runs
   - Component test verification (`mvn verify`)
   - Convention compliance (copyright, Javadoc, constants, test patterns)
   - Architecture rules (Hard Rules #12, #13, #14 compliance)

### 13.4 Lifecycle

- New docs start in `pending/` subdirectory
- Move to `completed/` after all phases are implemented and verified
- Register both docs in `docs/MANIFEST.md` upon creation

### 13.5 Naming

- Workflow: `{feature-slug}-workflow.md`
- Prompt: `{feature-slug}-prompt.md`
- The `{feature-slug}` must match between paired workflow and prompt documents

---

**End of AI-CODE-REF.md v4.3**

*This document is structured for programmatic analysis and quick pattern matching during code reviews, refactoring sessions, and test creation. Formerly AI-code-ref.md v3.0. Renamed to AI-CODE-REF.md to avoid collision with the Claude Code CLAUDE.md convention.*