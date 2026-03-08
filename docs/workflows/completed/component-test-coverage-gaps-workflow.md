# Workflow: Component Test Coverage Gaps

> **Target**: core-api
> **Prerequisite**: component-test-workflow (completed) — follow same patterns
> **Priority**: Must complete before moving to Phase 5.2+
> **Status**: ✅ COMPLETED — 2026-03-08 (26 tests, all passing)

---

## 1. Problem Statement

Phases 1–4 added several new controllers and endpoints to core-api, but component tests
(Testcontainers-based integration tests) were not written for all of them. Unit tests exist
for all features, but the middle tier of the test pyramid is incomplete.

---

## 2. Missing Component Tests

| # | Module | Controller / Feature | Endpoints | File to Create |
|---|--------|---------------------|-----------|----------------|
| 1 | security | TokenRefreshController | `POST /v1/security/token/refresh` | `TokenRefreshComponentTest.java` |
| 2 | security | LogoutController | `POST /v1/security/logout` | `LogoutComponentTest.java` |
| 3 | lead-management | DemoRequestController | `POST /v1/lead-management/demo-requests`, `GET /v1/lead-management/demo-requests/{id}`, `GET /v1/lead-management/demo-requests`, `DELETE /v1/lead-management/demo-requests/{id}` | `DemoRequestComponentTest.java` |
| 4 | user-management | CurrentUserController | `GET /v1/user-management/me` | `CurrentUserComponentTest.java` |
| 5 | application | PasskeyController | `POST /v1/security/passkey/register/options`, `POST /v1/security/passkey/register/verify`, `POST /v1/security/passkey/login/options`, `POST /v1/security/passkey/login/verify` | `PasskeyComponentTest.java` |

---

## 3. Test Architecture

Follow the established component test patterns from `component-test-workflow.md`:

- **Base class**: Extend from the existing abstract component test base (Testcontainers + MariaDB + Redis)
- **Format**: Given-When-Then comments on every test method
- **Naming**: `@Nested` classes per operation, `@DisplayName` on every `@Test` and `@Nested`
- **Constants**: `public static final` for all test data
- **No `any()` matchers**: Use `ArgumentCaptor` or exact values
- **Auth setup**: Use the bootstrap registration flow to get valid JWT tokens before testing

### 3.1 Token Refresh Tests

| Scenario | Method | Expected |
|----------|--------|----------|
| Valid refresh token | POST /v1/security/token/refresh | 200 + new access token + new refresh token |
| Expired refresh token | POST /v1/security/token/refresh | 401 |
| Reused (rotated) refresh token | POST /v1/security/token/refresh | 401 + all tokens revoked |
| Missing refresh token cookie | POST /v1/security/token/refresh | 400 |

### 3.2 Logout Tests

| Scenario | Method | Expected |
|----------|--------|----------|
| Valid logout | POST /v1/security/logout | 200 + cookies cleared |
| Unauthenticated logout | POST /v1/security/logout | 401 |

### 3.3 Demo Request Tests

| Scenario | Method | Expected |
|----------|--------|----------|
| Create valid demo request | POST | 201 |
| Create duplicate email | POST | 409 |
| Create missing required fields | POST | 400 |
| Get by ID (exists) | GET /{id} | 200 |
| Get by ID (not found) | GET /{id} | 404 |
| Get all (paginated) | GET | 200 + pagination |
| Delete (exists) | DELETE /{id} | 204 |
| Delete (not found) | DELETE /{id} | 404 |

**Note**: Demo requests are platform-level (NOT tenant-scoped). No `X-Tenant-Id` header needed.

### 3.4 Current User Tests

| Scenario | Method | Expected |
|----------|--------|----------|
| Get /me with valid token | GET /v1/user-management/me | 200 + user details |
| Get /me unauthenticated | GET /v1/user-management/me | 401 |
| Get /me for each user type | GET /v1/user-management/me | 200 + correct role-specific fields |

### 3.5 Passkey Tests

| Scenario | Method | Expected |
|----------|--------|----------|
| Request registration options | POST /register/options | 200 + challenge |
| Verify registration | POST /register/verify | 200 + credential stored |
| Request login options | POST /login/options | 200 + challenge |
| Verify login | POST /login/verify | 200 + JWT issued |
| Login with unregistered credential | POST /login/verify | 401 |

**Note**: Passkey tests require WebAuthn mocking — the FIDO2 attestation/assertion
objects need to be crafted programmatically. Use `com.yubico:webauthn-server-core` test
utilities if available, otherwise mock the WebAuthn verification service.

---

## 4. Implementation Phases

### Phase A — Infrastructure Check
Verify all test infrastructure (Testcontainers config, base classes, Failsafe plugin) is
already in place from prior component test work.

### Phase B — Security Module Tests (Token Refresh + Logout)
Create `TokenRefreshComponentTest.java` and `LogoutComponentTest.java` in the security
module's test directory.

### Phase C — Lead Management Tests
Create `DemoRequestComponentTest.java` in the lead-management module's test directory.
These are NOT tenant-scoped — simpler setup.

### Phase D — User Management Tests
Create `CurrentUserComponentTest.java` in the user-management module's test directory.

### Phase E — Passkey Tests
Create `PasskeyComponentTest.java` in the application module's test directory.
This is the most complex — may require WebAuthn test utilities.

---

## 5. Validation

```bash
# Run all component tests
mvn failsafe:integration-test failsafe:verify

# Run specific test
mvn failsafe:integration-test -Dit.test=TokenRefreshComponentTest
mvn failsafe:integration-test -Dit.test=DemoRequestComponentTest
mvn failsafe:integration-test -Dit.test=CurrentUserComponentTest
mvn failsafe:integration-test -Dit.test=PasskeyComponentTest
```

All tests must pass. Zero test failures.

---

## 6. Acceptance Criteria

- [x] 6 component test classes created (added MagicLinkComponentTest beyond original 5)
- [x] All scenarios from Section 3 tables covered (26 tests total)
- [x] Given-When-Then comments on every test method
- [x] `@DisplayName` on every `@Test` and `@Nested`
- [x] All tests pass: `mvn verify -pl application -Dfailsafe.includes="**/usecases/*ComponentTest.java"`
- [x] Copyright header (2026) on all new files

---

## 7. Production Fixes Discovered During Testing

| Fix | File | Issue |
|-----|------|-------|
| TenantContextHolder → ThreadLocal | `infra-common/.../TenantContextHolder.java` | `@RequestScope` CGLIB proxy broke tenant propagation in MockMvc |
| InternalAuthController decoupled from LoginApi | `security/.../InternalAuthController.java` | Default 501 methods conflicted with MagicLinkController |
| CurrentUserController added to PeopleControllerAdvice | `user-management/.../PeopleControllerAdvice.java` | EntityNotFoundException not handled → raw 500 |

## 8. Known Follow-Up

- `jackson-databind-nullable` 0.2.9 incompatible with Jackson 3 — `JsonNullable<T>` fields serialize as objects instead of unwrapped values (affects `/me` response `employeeId` field)
