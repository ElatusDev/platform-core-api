# Prompt: Component Test Coverage Gaps

> **Workflow**: [component-test-coverage-gaps-workflow.md](../../workflows/completed/component-test-coverage-gaps-workflow.md)
> **Target**: core-api
> **Prerequisite**: Read the completed `component-test-workflow.md` for patterns and base class details
> **Status**: ✅ COMPLETED — 2026-03-08

---

## EXECUTION RULES

1. Read the workflow file first — understand all 5 missing test classes
2. Read existing component tests in `completed/` to understand base class, imports, and patterns
3. Execute phases sequentially: A → B → C → D → E
4. Run `mvn failsafe:integration-test` after each phase to verify
5. Commit after each phase passes (Conventional Commits, NO AI attribution)
6. Given-When-Then comments on EVERY test method
7. `@DisplayName` on EVERY `@Test` and `@Nested`
8. ZERO `any()` matchers
9. Copyright year: 2025

---

## Phase A — Infrastructure Check

1. Read the existing component test base class and Testcontainers configuration
2. Verify Failsafe plugin is configured in the relevant module POMs
3. Identify the package structure and import patterns used by existing component tests
4. If any module is missing Failsafe config, add it before proceeding

---

## Phase B — Security Module: Token Refresh + Logout

### Step 1: Read existing security module tests
- Find the security module test directory
- Read any existing component tests for pattern reference

### Step 2: Create `TokenRefreshComponentTest.java`
- Test valid token refresh → 200 + new tokens
- Test expired refresh token → 401
- Test reused (rotated) refresh token → 401 + revocation
- Test missing refresh token cookie → 400

### Step 3: Create `LogoutComponentTest.java`
- Test valid logout → 200 + cookies cleared
- Test unauthenticated logout → 401

### Step 4: Verify
```bash
mvn failsafe:integration-test -pl security -Dit.test="TokenRefreshComponentTest,LogoutComponentTest"
```

### Step 5: Commit
```
test(security): add component tests for token refresh and logout
```

---

## Phase C — Lead Management: Demo Requests

### Step 1: Read the DemoRequestController and its OpenAPI contract
- Note: demo requests are platform-level, NOT tenant-scoped
- No `X-Tenant-Id` header needed

### Step 2: Create `DemoRequestComponentTest.java`
- Test create valid demo request → 201
- Test create duplicate email → 409
- Test create missing required fields → 400
- Test get by ID (exists) → 200
- Test get by ID (not found) → 404
- Test get all (paginated) → 200
- Test delete (exists) → 204
- Test delete (not found) → 404

### Step 3: Verify
```bash
mvn failsafe:integration-test -pl lead-management -Dit.test=DemoRequestComponentTest
```

### Step 4: Commit
```
test(lead-management): add component tests for demo request CRUD
```

---

## Phase D — User Management: Current User /me

### Step 1: Read the CurrentUserController and /me endpoint implementation
- Understand which user types are supported and what fields are returned per type

### Step 2: Create `CurrentUserComponentTest.java`
- Test GET /me with valid employee token → 200 + employee fields
- Test GET /me with valid student token → 200 + student fields
- Test GET /me unauthenticated → 401

### Step 3: Verify
```bash
mvn failsafe:integration-test -pl user-management -Dit.test=CurrentUserComponentTest
```

### Step 4: Commit
```
test(user-management): add component tests for /me endpoint
```

---

## Phase E — Application Module: Passkey Authentication

### Step 1: Read the PasskeyController and WebAuthn configuration
- Understand how FIDO2 attestation/assertion objects are validated
- Check if `com.yubico:webauthn-server-core` provides test utilities
- If WebAuthn mocking is too complex, mock the verification service layer

### Step 2: Create `PasskeyComponentTest.java`
- Test registration options request → 200 + challenge
- Test registration verify → 200 + credential stored
- Test login options request → 200 + challenge
- Test login verify → 200 + JWT issued
- Test login with unregistered credential → 401

### Step 3: Verify
```bash
mvn failsafe:integration-test -pl application -Dit.test=PasskeyComponentTest
```

### Step 4: Commit
```
test(application): add component tests for passkey authentication
```

---

## Summary of All New Files

| File | Module | Tests |
|------|--------|-------|
| `TokenRefreshComponentTest.java` | security | 4 |
| `LogoutComponentTest.java` | security | 2 |
| `DemoRequestComponentTest.java` | lead-management | 8 |
| `CurrentUserComponentTest.java` | user-management | 3 |
| `PasskeyComponentTest.java` | application | 5 |
| **Total** | | **22** |

---

## Final Verification

```bash
mvn failsafe:integration-test failsafe:verify
```

All 22 tests must pass. Move workflow and prompt from `pending/` to `completed/` when done.
