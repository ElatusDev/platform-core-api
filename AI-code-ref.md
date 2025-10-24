# AI-Optimized Coding Standards Reference

**Version**: 3.0 (AI-Optimized with Comprehensive Test Conventions)  
**Last Updated**: October 24, 2025  
**Purpose**: Quick reference for AI code analysis, review, refactoring, and test creation

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

### 4.4 Verification Best Practices

**Rule 1: Don't over-verify**
```java
// ❌ Redundant verification
assertThatThrownBy(() -> service.process(null))
    .isInstanceOf(IllegalArgumentException.class);
verify(mock, never()).someMethod(any()); // Redundant!

// ✅ Exception proves control flow
assertThatThrownBy(() -> service.process(null))
    .isInstanceOf(IllegalArgumentException.class);
// No mock verification needed - exception proves early validation
```

**Rule 2: Use verifyNoMoreInteractions for comprehensive checks**
```java
// ❌ Multiple never() checks
verify(service).initialize();
verify(service, never()).process(any());
verify(service, never()).cleanup(any());

// ✅ Single comprehensive check
verify(service).initialize();
verifyNoMoreInteractions(service); // Verifies nothing else called
```

**Rule 3: Verify what matters**
```java
// ✅ Good verification
verify(repository).save(expectedEntity);
verify(eventPublisher).publish(expectedEvent);
verifyNoMoreInteractions(repository, eventPublisher);
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

```java
// ✅ Comprehensive exception testing
@Test
@DisplayName("Should throw exception with context when parsing fails")
void shouldThrowExceptionWithContext_whenParsingFails() throws Exception {
    // Given
    String invalidInput = "not-a-number";
    when(parser.parse(invalidInput))
        .thenThrow(new ParseException("Invalid format"));
    
    // When & Then
    assertThatThrownBy(() -> service.process(invalidInput))
        .isInstanceOf(ProcessingException.class)
        .hasMessageContaining("Failed to process")
        .hasMessageContaining(invalidInput)
        .hasCauseInstanceOf(ParseException.class);
    
    // Verify parse was called, nothing else
    verify(parser).parse(invalidInput);
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

### 4.10 Testing Checklist

**Before committing tests:**
- [ ] Uses Given-When-Then pattern (NOT AAA)
- [ ] Test names: `shouldDoX_whenGivenY()`
- [ ] DisplayNames in natural language
- [ ] Zero `any()` matchers
- [ ] Mock stubbing matches implementation
- [ ] Verifications are precise and necessary
- [ ] Local vars for test-specific values
- [ ] Class constants for shared values
- [ ] Helper methods for complex setup
- [ ] All edge cases covered
- [ ] Exception messages verified
- [ ] No commented-out tests

### 4.11 TEST-SPECIFIC DETECTION KEYWORDS FOR AI

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
- `verify(..., never()).method(any())` - Should use `verifyNoMoreInteractions()`
- Multiple redundant `verify(mock, never())` calls
- Exception assertions without message verification

#### Test Code Smells
**Detection Patterns:**
- Redundant local variables for class constants
- Test classes without `@Nested` organization
- Helper methods that could be extracted
- Tests without clear Given-When-Then sections
- Over-verification (checking mocks that can't be called)

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

**End of AI-Optimized Reference Document v3.0**

*This document is structured for programmatic analysis and quick pattern matching during code reviews, refactoring sessions, and test creation. Version 3.0 adds comprehensive test conventions including Given-When-Then pattern, no any() matchers rule, mock stubbing best practices, and test-specific detection keywords.*