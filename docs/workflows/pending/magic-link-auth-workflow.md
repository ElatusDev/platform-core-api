# Magic Link Authentication Workflow — ElatusDev

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `docs/directives/CLAUDE.md`, `docs/directives/AI-CODE-REF.md`, and `docs/design/DESIGN.md` before starting. Also read the rate-limiting workflow (dependency for magic link request throttling).

---

## 1. Architecture Overview

### 1.1 What Exists

The platform supports internal authentication (username/password) via `InternalAuthenticationUseCase` and OAuth social login via `OAuthAuthenticationUseCase`. The notification system has email delivery infrastructure (`EmailDeliveryChannelStrategy`, `JavaMailSender`) already configured for sending HTML emails.

| Component | Location | State |
|-----------|----------|-------|
| `InternalAuthenticationUseCase` | `security/.../internal/usecases/` | Working — exchanges username/password for JWT |
| `JwtTokenProvider` | `security/.../internal/interfaceadapters/jwt/` | `createToken(username, tenantId, additionalClaims)` — signs with EC/RSA key |
| `SecurityConfig` | `security/.../config/` | Permits `/v1/security/login/internal`, `/v1/security/register` |
| `EmailDeliveryChannelStrategy` | `notification-system/.../notification/usecases/` | Delivers HTML emails via `JavaMailSender` (SMTP/SES) |
| `HashingService` | `utilities/.../security/` | SHA-256 hashing — `generateHash(String)` returns hex string |
| `TenantContextHolder` | `infra-common/.../persistence/config/` | `@RequestScope` — holds `tenantId` for current request |
| `CustomerAuthDataModel` | `multi-tenant-data/.../security/` | Has `provider` + `token` fields, composite key (tenantId, customerAuthId) |
| `PersonPIIDataModel` | `multi-tenant-data/.../users/base/` | Stores email (encrypted), emailHash (SHA-256) |
| `AdultStudentDataModel` | `multi-tenant-data/.../users/customer/` | Customer user entity |
| `RateLimiterService` | `security/.../ratelimit/usecases/` | Redis sliding window — `checkRateLimit(key, limit, windowMs)` (pending implementation) |

### 1.2 What's Missing

1. **MagicLinkTokenDataModel**: No entity to store single-use magic link tokens
2. **Token generation**: No code to generate cryptographically random URL-safe tokens
3. **Token hashing + storage**: No flow to hash tokens and persist them
4. **Magic link email**: No email template for magic link delivery
5. **Token verification**: No code to validate magic link tokens and issue JWTs
6. **Magic link endpoints**: No `POST /login/magic-link/request` or `POST /login/magic-link/verify`
7. **Rate limiting for magic links**: No per-email throttling to prevent abuse

### 1.3 Magic Link Authentication Flow

```
1. User enters email on ElatusDev login page
   │
   ├── POST /v1/security/login/magic-link/request
   │     Body: { email, tenantId }
   │
2. Server: MagicLinkRequestUseCase
   │  ├── Rate limit check: max 3 requests per email per hour
   │  ├── Generate 32-byte random token (SecureRandom)
   │  ├── Hash token with SHA-256
   │  ├── Store MagicLinkTokenDataModel:
   │  │     { tenantId, email, tokenHash, expiresAt=now+10min, usedAt=null }
   │  ├── Build magic link URL:
   │  │     {base-url}/auth/magic-link?token={urlSafeBase64Token}&tenant={tenantId}
   │  └── Send email via EmailDeliveryChannelStrategy
   │
   ├── Response: 200 (always — no email enumeration)
   │
3. User clicks link in email
   │
   ├── Frontend extracts token + tenantId from URL
   │
   ├── POST /v1/security/login/magic-link/verify
   │     Body: { token, tenantId }
   │
4. Server: MagicLinkVerificationUseCase (application module)
   │  ├── Set tenant context
   │  ├── Hash received token
   │  ├── Lookup MagicLinkTokenDataModel by tokenHash + tenantId
   │  ├── Validate: not expired, not already used
   │  ├── Mark token as used (set usedAt = now)
   │  ├── Lookup PersonPII by emailHash
   │  │     ├── Found → load AdultStudent → issue JWT
   │  │     └── Not found → create PersonPII + CustomerAuth + AdultStudent → issue JWT
   │  └── Return AuthTokenResponseDTO with platform JWT
   │
   └── Response: 200 { token: "jwt..." }
```

### 1.4 Security Properties

| Property | Value | Rationale |
|----------|-------|-----------|
| Token entropy | 256 bits (32 bytes, `SecureRandom`) | Exceeds OWASP minimum of 128 bits |
| Token format | URL-safe Base64 | Safe for URL query parameters |
| Storage | SHA-256 hash only — raw token never stored | Compromise of DB does not reveal valid tokens |
| Expiry | 10 minutes (configurable) | Short window minimizes exposure |
| Single-use | `usedAt` timestamp set on verification | Prevents replay attacks |
| Rate limit | 3 requests per email per hour | Prevents email flooding |
| No enumeration | Always returns 200 on request, even if email not found | Prevents email existence discovery |

---

## 2. Target Architecture

### 2.1 Entity: MagicLinkTokenDataModel

```
MagicLinkTokenDataModel extends TenantScoped
  @Id tenantId (Long)        — from TenantScoped
  @Id magicLinkTokenId (Long) — auto-generated per tenant
  email (String, VARCHAR 500)  — encrypted at rest
  tokenHash (String, VARCHAR 64) — SHA-256 hex, indexed
  expiresAt (Instant)          — token expiration timestamp
  usedAt (Instant, nullable)   — set when token is consumed
  createdAt (Instant)          — creation timestamp

Composite key: (tenantId, magicLinkTokenId)
Index: (tenant_id, token_hash, deleted_at) for lookup
```

### 2.2 Module Placement

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `MagicLinkTokenDataModel` | multi-tenant-data | `security/` | Entity (alongside `CustomerAuthDataModel`) |
| `MagicLinkTokenRepository` | security | `magiclink/interfaceadapters/` | Repository for the entity |
| `MagicLinkRequestUseCase` | security | `magiclink/usecases/` | Single-module use case — generates token, sends email (Hard Rule #12) |
| `MagicLinkVerificationUseCase` | application | `magiclink/usecases/` | Cross-module orchestrator — validates token, looks up/creates user, issues JWT (Hard Rule #14) |
| `MagicLinkController` | application | `magiclink/interfaceadapters/` | Controller for both endpoints |
| `MagicLinkControllerAdvice` | application | `magiclink/config/` | Exception handling |
| `MagicLinkProperties` | security | `config/` | Configuration (base URL, expiry) |
| `MagicLinkTokenExpiredException` | security | `magiclink/exceptions/` | Token expired |
| `MagicLinkTokenAlreadyUsedException` | security | `magiclink/exceptions/` | Token already consumed |
| `MagicLinkTokenNotFoundException` | security | `magiclink/exceptions/` | Token hash not found |
| OpenAPI spec | security | `src/main/resources/openapi/` | API contract |

### 2.3 Cross-Module Boundary

```
security module (single-module use case)
  ├── MagicLinkRequestUseCase
  │     ├── Generates token
  │     ├── Hashes and stores in DB (MagicLinkTokenRepository)
  │     └── Sends email (EmailDeliveryChannelStrategy)
  │
application module (cross-module orchestrator)
  └── MagicLinkVerificationUseCase
        ├── Validates token (MagicLinkTokenRepository — security)
        ├── Looks up user (PersonPIIRepository — user-management)
        ├── Creates user if needed (AdultStudentRepository — user-management)
        └── Issues JWT (JwtTokenProvider — security)
```

The request use case stays in the security module because it only interacts with security-module components (token storage, hashing) and notification-system (email). The verification use case lives in the application module because it orchestrates across security (token validation) and user-management (user lookup/creation).

### 2.4 Email Template

Simple HTML email with the magic link. No complex template engine — inline HTML string.

```html
<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
  <h2>Sign in to ElatusDev</h2>
  <p>Click the button below to sign in. This link expires in 10 minutes.</p>
  <a href="{magicLinkUrl}"
     style="display: inline-block; padding: 12px 24px; background-color: #4F46E5;
            color: white; text-decoration: none; border-radius: 6px; margin: 16px 0;">
    Sign In
  </a>
  <p style="color: #666; font-size: 14px;">
    If you didn't request this link, you can safely ignore this email.
  </p>
  <p style="color: #999; font-size: 12px;">
    Or copy this link: {magicLinkUrl}
  </p>
</body>
</html>
```

---

## 3. Execution Phases

### Phase Dependency Graph

```
Phase 1:  MagicLinkTokenDataModel + repository + DB schema
    ↓
Phase 2:  OpenAPI specification (magic-link-authentication.yaml)
    ↓
Phase 3:  SecurityConfig updates (permit magic link endpoints)
    ↓
Phase 4:  MagicLinkProperties configuration
    ↓
Phase 5:  Exceptions (expired, already-used, not-found)
    ↓
Phase 6:  MagicLinkRequestUseCase (security module — generates + emails)
    ↓
Phase 7:  MagicLinkVerificationUseCase (application module — validates + issues JWT)
    ↓
Phase 8:  MagicLinkController + MagicLinkControllerAdvice (application module)
    ↓
Phase 9:  Configuration properties (application.properties)
    ↓
Phase 10: Unit tests — security module (MagicLinkRequestUseCaseTest)
    ↓
Phase 11: Unit tests — application module (MagicLinkVerificationUseCaseTest, MagicLinkControllerTest)
    ↓
Phase 12: Component tests (MagicLinkComponentTest)
```

---

## 4. Phase-by-Phase Implementation

### Phase 1: MagicLinkTokenDataModel + Repository + DB Schema

#### Step 1.1: Create MagicLinkTokenDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/security/MagicLinkTokenDataModel.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.security;

import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Instant;

/**
 * Entity representing a single-use magic link token for passwordless
 * email-based authentication.
 *
 * <p>Tokens are stored as SHA-256 hashes — the raw token value is never
 * persisted. Each token expires after a configurable duration (default
 * 10 minutes) and can only be used once (verified by the {@code usedAt}
 * timestamp).
 *
 * @author ElatusDev
 * @since 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "magic_link_tokens")
@SQLDelete(sql = "UPDATE magic_link_tokens SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND magic_link_token_id = ?")
@IdClass(MagicLinkTokenDataModel.MagicLinkTokenCompositeId.class)
public class MagicLinkTokenDataModel extends TenantScoped {

    /**
     * Unique identifier for the magic link token within the tenant.
     */
    @Id
    @Column(name = "magic_link_token_id")
    private Long magicLinkTokenId;

    /**
     * The email address this magic link was sent to.
     * Encrypted at rest for PII protection.
     */
    @Column(name = "email", nullable = false, length = 500)
    private String email;

    /**
     * SHA-256 hash of the magic link token.
     * The raw token is never stored in the database.
     */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /**
     * Timestamp when this token expires and can no longer be used.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Timestamp when this token was consumed. Null if not yet used.
     * Once set, the token cannot be used again (single-use enforcement).
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * Timestamp when this token was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Composite primary key class for MagicLinkToken entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MagicLinkTokenCompositeId implements Serializable {
        protected Long tenantId;
        protected Long magicLinkTokenId;
    }
}
```

#### Step 1.2: Create MagicLinkTokenRepository

**File**: `security/src/main/java/com/akademiaplus/magiclink/interfaceadapters/MagicLinkTokenRepository.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.interfaceadapters;

import com.akademiaplus.infra.persistence.repository.TenantScopedRepository;
import com.akademiaplus.security.MagicLinkTokenDataModel;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for magic link token persistence operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface MagicLinkTokenRepository
        extends TenantScopedRepository<MagicLinkTokenDataModel, MagicLinkTokenDataModel.MagicLinkTokenCompositeId> {

    /**
     * Finds a magic link token by its SHA-256 hash.
     *
     * @param tokenHash the SHA-256 hash of the token
     * @return the matching token, if found
     */
    Optional<MagicLinkTokenDataModel> findByTokenHash(String tokenHash);
}
```

#### Step 1.3: DB schema

**File**: Add to `db_init/00-schema-dev.sql` (or equivalent)

```sql
CREATE TABLE IF NOT EXISTS magic_link_tokens (
    tenant_id      BIGINT       NOT NULL,
    magic_link_token_id BIGINT  NOT NULL,
    email          VARCHAR(500) NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL,
    expires_at     TIMESTAMP    NOT NULL,
    used_at        TIMESTAMP    NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP    NULL,
    PRIMARY KEY (tenant_id, magic_link_token_id),
    INDEX idx_magic_link_token_hash (tenant_id, token_hash, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Step 1.4: Compile check

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 1.5: Commit

```
feat(multi-tenant-data): add MagicLinkTokenDataModel entity

Add magic_link_tokens table and MagicLinkTokenDataModel entity
with tokenHash (SHA-256), expiresAt, usedAt for single-use
passwordless authentication. Add MagicLinkTokenRepository with
findByTokenHash query.
```

---

### Phase 2: OpenAPI Specification

#### Step 2.1: Create magic-link-authentication.yaml

**File**: `security/src/main/resources/openapi/magic-link-authentication.yaml`

Defines:
- `POST /login/magic-link/request`
  - Request: `MagicLinkRequest` — `email` (string, format: email), `tenantId` (integer, format: int64)
  - Response 200: empty body (no enumeration)
  - Response 429: `ErrorResponse` (rate limited)
- `POST /login/magic-link/verify`
  - Request: `MagicLinkVerifyRequest` — `token` (string), `tenantId` (integer, format: int64)
  - Response 200: `AuthTokenResponse` (reuses from internal-authentication.yaml)
  - Response 401: `ErrorResponse` (invalid, expired, or used token)

#### Step 2.2: Modify security-module.yaml

Add the magic link path and schema references.

#### Step 2.3: Regenerate DTOs

```bash
mvn clean generate-sources -pl security -am -DskipTests -f platform-core-api/pom.xml
```

Verify:
- `MagicLinkRequestDTO` generated with `getEmail()`, `getTenantId()`
- `MagicLinkVerifyRequestDTO` generated with `getToken()`, `getTenantId()`
- API interface generated with `requestMagicLink()` and `verifyMagicLink()` methods

#### Step 2.4: Commit

```
api(security): add magic link authentication OpenAPI specification

Add POST /login/magic-link/request and /login/magic-link/verify
endpoints. Request accepts email + tenantId, verify accepts
token + tenantId and returns AuthTokenResponse.
```

---

### Phase 3: SecurityConfig Updates

#### Step 3.1: Add permit rules

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add alongside existing permit rules:

```java
.requestMatchers("/v1/security/login/magic-link/request").permitAll()
.requestMatchers("/v1/security/login/magic-link/verify").permitAll()
```

#### Step 3.2: Add CORS rules

Add CORS configuration for both magic link endpoints using the same `loginCorsConfig` object.

#### Step 3.3: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 3.4: Commit

```
feat(security): permit magic link authentication endpoints

Add /v1/security/login/magic-link/request and /verify to
permitAll and CORS config alongside existing login rules.
```

---

### Phase 4: MagicLinkProperties

#### Step 4.1: Create MagicLinkProperties

**File**: `security/src/main/java/com/akademiaplus/config/MagicLinkProperties.java`

```java
/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for magic link authentication.
 *
 * @param baseUrl           the frontend base URL for magic link construction
 * @param tokenExpiryMinutes token validity duration in minutes
 * @param maxRequestsPerEmailPerHour maximum magic link requests per email per hour
 * @param emailSubject      the subject line for magic link emails
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "magic-link")
public record MagicLinkProperties(
        String baseUrl,
        int tokenExpiryMinutes,
        int maxRequestsPerEmailPerHour,
        String emailSubject
) {}
```

#### Step 4.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 4.3: Commit

```
feat(security): add MagicLinkProperties configuration

Add @ConfigurationProperties for magic-link.* prefix with
base URL, token expiry, rate limit, and email subject config.
```

---

### Phase 5: Exceptions

#### Step 5.1: MagicLinkTokenExpiredException

**File**: `security/src/main/java/com/akademiaplus/magiclink/exceptions/MagicLinkTokenExpiredException.java`

```java
public class MagicLinkTokenExpiredException extends RuntimeException {
    public static final String ERROR_TOKEN_EXPIRED = "Magic link token has expired";
    public static final String ERROR_CODE = "MAGIC_LINK_TOKEN_EXPIRED";

    public MagicLinkTokenExpiredException() {
        super(ERROR_TOKEN_EXPIRED);
    }
}
```

#### Step 5.2: MagicLinkTokenAlreadyUsedException

**File**: `security/src/main/java/com/akademiaplus/magiclink/exceptions/MagicLinkTokenAlreadyUsedException.java`

```java
public class MagicLinkTokenAlreadyUsedException extends RuntimeException {
    public static final String ERROR_TOKEN_ALREADY_USED = "Magic link token has already been used";
    public static final String ERROR_CODE = "MAGIC_LINK_TOKEN_ALREADY_USED";

    public MagicLinkTokenAlreadyUsedException() {
        super(ERROR_TOKEN_ALREADY_USED);
    }
}
```

#### Step 5.3: MagicLinkTokenNotFoundException

**File**: `security/src/main/java/com/akademiaplus/magiclink/exceptions/MagicLinkTokenNotFoundException.java`

```java
public class MagicLinkTokenNotFoundException extends RuntimeException {
    public static final String ERROR_TOKEN_NOT_FOUND = "Magic link token not found or invalid";
    public static final String ERROR_CODE = "MAGIC_LINK_TOKEN_NOT_FOUND";

    public MagicLinkTokenNotFoundException() {
        super(ERROR_TOKEN_NOT_FOUND);
    }
}
```

#### Step 5.4: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 5.5: Commit

```
feat(security): add magic link exception types

Add MagicLinkTokenExpiredException, MagicLinkTokenAlreadyUsedException,
and MagicLinkTokenNotFoundException for magic link verification
error handling.
```

---

### Phase 6: MagicLinkRequestUseCase

#### Step 6.1: Create MagicLinkRequestUseCase

**File**: `security/src/main/java/com/akademiaplus/magiclink/usecases/MagicLinkRequestUseCase.java`

This is a single-module use case (Hard Rule #12) — lives in the security module.

**Dependencies** (all constructor-injected):
- `MagicLinkTokenRepository`
- `HashingService`
- `RateLimiterService`
- `MagicLinkProperties`
- `EmailDeliveryChannelStrategy`
- `TenantContextHolder`
- `ApplicationContext`

**Constants**:
```java
public static final String RATE_LIMIT_KEY_PREFIX = "rate:magic-link:email:";
public static final int TOKEN_BYTE_LENGTH = 32;
public static final String MAGIC_LINK_URL_TEMPLATE = "%s/auth/magic-link?token=%s&tenant=%d";
public static final String EMAIL_SUBJECT = "Sign in to ElatusDev";
public static final String PROVIDER_MAGIC_LINK = "magic-link";
```

**Method `requestMagicLink(MagicLinkRequestDTO dto)`** — `@Transactional`:

1. `tenantContextHolder.setTenantId(dto.getTenantId())`
2. Rate limit check: `rateLimiterService.checkRateLimit(RATE_LIMIT_KEY_PREFIX + dto.getEmail(), properties.maxRequestsPerEmailPerHour(), 3_600_000L)`
3. If not allowed → return silently (no error to prevent enumeration)
4. Generate 32 random bytes via `SecureRandom`
5. Encode as URL-safe Base64: `Base64.getUrlEncoder().withoutPadding().encode(bytes)`
6. Hash token: `hashingService.generateHash(base64Token)`
7. Create `MagicLinkTokenDataModel` via `applicationContext.getBean()`:
   - Set `email`, `tokenHash`, `expiresAt = Instant.now().plusSeconds(properties.tokenExpiryMinutes() * 60)`, `createdAt = Instant.now()`
8. Save to repository
9. Build magic link URL: `String.format(MAGIC_LINK_URL_TEMPLATE, properties.baseUrl(), base64Token, dto.getTenantId())`
10. Build HTML email body (inline constant)
11. Send email via `EmailDeliveryChannelStrategy` — create `NotificationDataModel` with subject and HTML body
12. **Always return void/200** — never reveal whether the email exists

**IMPORTANT**: The email is sent regardless of whether the email address exists in the system. User lookup happens only at verification time. This prevents email enumeration attacks.

#### Step 6.2: Compile check

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 6.3: Commit

```
feat(security): implement MagicLinkRequestUseCase

Add single-module use case that generates a 32-byte random
token, stores SHA-256 hash in DB, and sends magic link email.
Includes per-email rate limiting (3/hour) and anti-enumeration
(always returns 200).
```

---

### Phase 7: MagicLinkVerificationUseCase

#### Step 7.1: Create MagicLinkVerificationUseCase

**File**: `application/src/main/java/com/akademiaplus/magiclink/usecases/MagicLinkVerificationUseCase.java`

This is a cross-module orchestrator (Hard Rule #14) — lives in the application module.

**Dependencies** (all constructor-injected):
- `MagicLinkTokenRepository` (security module)
- `PersonPIIRepository` (user-management module)
- `AdultStudentRepository` (user-management module)
- `CustomerAuthRepository` (user-management module)
- `JwtTokenProvider` (security module)
- `HashingService` (utilities module)
- `PiiNormalizer` (utilities module)
- `TenantContextHolder` (infra-common module)
- `ApplicationContext`

**Constants**:
```java
public static final String PLACEHOLDER_PHONE = "PENDING_UPDATE";
public static final String PLACEHOLDER_ADDRESS = "PENDING_UPDATE";
public static final String PLACEHOLDER_ZIP = "PENDING_UPDATE";
public static final String JWT_CLAIM_ROLE = "Has role";
public static final String ROLE_CUSTOMER = "CUSTOMER";
public static final String PROVIDER_MAGIC_LINK = "magic-link";
```

**Method `verifyMagicLink(MagicLinkVerifyRequestDTO dto)`** — `@Transactional`:

1. `tenantContextHolder.setTenantId(dto.getTenantId())`
2. Hash received token: `hashingService.generateHash(dto.getToken())`
3. Lookup: `magicLinkTokenRepository.findByTokenHash(tokenHash)`
4. If empty → throw `MagicLinkTokenNotFoundException`
5. If `token.getUsedAt() != null` → throw `MagicLinkTokenAlreadyUsedException`
6. If `Instant.now().isAfter(token.getExpiresAt())` → throw `MagicLinkTokenExpiredException`
7. Mark as used: `token.setUsedAt(Instant.now())`
8. Save token update
9. Lookup user by email:
   - `String emailHash = hashingService.generateHash(piiNormalizer.normalizeEmail(token.getEmail()))`
   - `Optional<PersonPIIDataModel> existingPii = personPIIRepository.findByEmailHash(emailHash)`
10. **Branch A — Existing user**: PersonPII found
    - Find `AdultStudentDataModel` via `adultStudentRepository.findByPersonPii(existingPii.get())`
    - Build JWT claims with role
    - `jwtTokenProvider.createToken(token.getEmail(), dto.getTenantId(), claims)`
11. **Branch B — New user**: PersonPII not found
    - Create `PersonPIIDataModel` via `applicationContext.getBean()` — set email, emailHash, placeholder phone/address/zip
    - Create `CustomerAuthDataModel` via `applicationContext.getBean()` — set provider = "magic-link"
    - Create `AdultStudentDataModel` via `applicationContext.getBean()` — set personPII, customerAuth
    - Save and flush
    - Build JWT and return
12. Return `AuthTokenResponseDTO` with platform JWT

#### Step 7.2: Compile check

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 7.3: Commit

```
feat(application): implement MagicLinkVerificationUseCase

Add cross-module orchestrator for magic link token verification.
Validates token hash, checks expiry and single-use, looks up
or creates user by email, and issues platform JWT. Supports
automatic account creation for new users.
```

---

### Phase 8: MagicLinkController + MagicLinkControllerAdvice

#### Step 8.1: Create MagicLinkController

**File**: `application/src/main/java/com/akademiaplus/magiclink/interfaceadapters/MagicLinkController.java`

- `@RestController @RequestMapping("/v1/security")`
- Implements generated magic link API interface
- Two methods:
  - `requestMagicLink(MagicLinkRequestDTO dto)` → delegates to `MagicLinkRequestUseCase`, returns 200 empty
  - `verifyMagicLink(MagicLinkVerifyRequestDTO dto)` → delegates to `MagicLinkVerificationUseCase`, returns 200 with JWT
- Thin controller — zero business logic

#### Step 8.2: Create MagicLinkControllerAdvice

**File**: `application/src/main/java/com/akademiaplus/magiclink/config/MagicLinkControllerAdvice.java`

- `@ControllerAdvice(basePackageClasses = MagicLinkController.class)`
- Extends `BaseControllerAdvice`
- `@ExceptionHandler(MagicLinkTokenNotFoundException.class)` → 401
- `@ExceptionHandler(MagicLinkTokenExpiredException.class)` → 401
- `@ExceptionHandler(MagicLinkTokenAlreadyUsedException.class)` → 401
- Use `public static final` error code constants

#### Step 8.3: Compile check

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 8.4: Commit

```
feat(application): add MagicLinkController and ControllerAdvice

Implement generated magic link API interface for request and
verify endpoints. Handle token-not-found, expired, and
already-used exceptions as 401 responses.
```

---

### Phase 9: Configuration Properties

#### Step 9.1: Add magic link properties

**File**: `application/src/main/resources/application.properties`

```properties
# Magic Link Authentication
magic-link.base-url=${MAGIC_LINK_BASE_URL:http://localhost:3000}
magic-link.token-expiry-minutes=${MAGIC_LINK_EXPIRY_MINUTES:10}
magic-link.max-requests-per-email-per-hour=${MAGIC_LINK_MAX_REQUESTS:3}
magic-link.email-subject=Sign in to ElatusDev
```

#### Step 9.2: Commit

```
feat(application): add magic link configuration properties

Add magic-link base URL, token expiry (10 min), max requests
per email per hour (3), and email subject configuration.
```

---

### Phase 10: Unit Tests — Security Module

#### Step 10.1: MagicLinkRequestUseCaseTest

**File**: `security/src/test/java/com/akademiaplus/magiclink/usecases/MagicLinkRequestUseCaseTest.java`

- `@ExtendWith(MockitoExtension.class)`
- All dependencies mocked
- Constants for test email, tenant ID, etc.

| @Nested | Tests |
|---------|-------|
| `TokenGeneration` | `shouldGenerateUrlSafeBase64Token_whenRequestingMagicLink`, `shouldHashTokenWithSha256_whenStoringInDatabase` |
| `TokenStorage` | `shouldStoreTokenHashInDatabase_whenRequestingMagicLink`, `shouldSetExpiryFromProperties_whenStoringToken`, `shouldSetCreatedAtToNow_whenStoringToken` |
| `EmailDelivery` | `shouldSendEmailWithMagicLink_whenRequestingMagicLink`, `shouldBuildCorrectMagicLinkUrl_whenSendingEmail` |
| `RateLimiting` | `shouldCheckRateLimit_whenRequestingMagicLink`, `shouldNotSendEmail_whenRateLimitExceeded`, `shouldStillReturn200_whenRateLimitExceeded` |
| `AntiEnumeration` | `shouldAlwaysSucceed_whenEmailDoesNotExist` |
| `TenantContext` | `shouldSetTenantId_whenRequestingMagicLink` |

#### Step 10.2: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 10.3: Commit

```
test(security): add MagicLinkRequestUseCase unit tests

Cover token generation, SHA-256 hashing, DB storage, email
delivery, rate limiting, anti-enumeration, and tenant context.
```

---

### Phase 11: Unit Tests — Application Module

#### Step 11.1: MagicLinkVerificationUseCaseTest

**File**: `application/src/test/java/com/akademiaplus/magiclink/usecases/MagicLinkVerificationUseCaseTest.java`

- `@ExtendWith(MockitoExtension.class)`
- All dependencies mocked
- `@Captor ArgumentCaptor` for entities

| @Nested | Tests |
|---------|-------|
| `ExistingUser` | `shouldIssueJwt_whenUserExistsByEmail`, `shouldMarkTokenAsUsed_whenVerificationSucceeds` |
| `NewUser` | `shouldCreateAdultStudent_whenEmailNotFound`, `shouldSetPlaceholderValues_whenCreatingNewUser`, `shouldIssueJwt_whenNewUserCreated`, `shouldSetProviderToMagicLink_whenCreatingCustomerAuth` |
| `TokenValidation` | `shouldThrowTokenNotFoundException_whenTokenHashNotFound`, `shouldThrowTokenExpiredException_whenTokenExpired`, `shouldThrowTokenAlreadyUsedException_whenTokenAlreadyUsed` |
| `TenantContext` | `shouldSetTenantId_whenVerifying` |
| `TokenHashing` | `shouldHashReceivedToken_whenVerifying` |

#### Step 11.2: MagicLinkControllerTest

**File**: `application/src/test/java/com/akademiaplus/magiclink/interfaceadapters/MagicLinkControllerTest.java`

- Standalone MockMvc
- `@Mock MagicLinkRequestUseCase`, `@Mock MagicLinkVerificationUseCase`

| @Nested | Tests |
|---------|-------|
| `RequestEndpoint` | `shouldReturn200_whenMagicLinkRequested` |
| `VerifyEndpoint` | `shouldReturn200WithToken_whenVerificationSucceeds`, `shouldReturn401_whenTokenNotFound`, `shouldReturn401_whenTokenExpired`, `shouldReturn401_whenTokenAlreadyUsed` |

#### Step 11.3: MagicLinkControllerAdviceTest

**File**: `application/src/test/java/com/akademiaplus/magiclink/config/MagicLinkControllerAdviceTest.java`

| @Nested | Tests |
|---------|-------|
| `TokenNotFound` | `shouldReturn401_whenMagicLinkTokenNotFoundExceptionThrown` |
| `TokenExpired` | `shouldReturn401_whenMagicLinkTokenExpiredExceptionThrown` |
| `TokenAlreadyUsed` | `shouldReturn401_whenMagicLinkTokenAlreadyUsedExceptionThrown` |

#### Step 11.4: Compile + test

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl application -am -f platform-core-api/pom.xml
```

#### Step 11.5: Commit

```
test(application): add magic link verification, controller, and advice unit tests

MagicLinkVerificationUseCaseTest — covers existing user, new user
creation, token validation (not found, expired, used), tenant context.
MagicLinkControllerTest — covers HTTP 200 and 401 responses.
MagicLinkControllerAdviceTest — covers exception-to-HTTP mapping.
```

---

### Phase 12: Component Tests

#### Step 12.1: MagicLinkComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/MagicLinkComponentTest.java`

- Extends `AbstractIntegrationTest`
- `@AutoConfigureMockMvc`
- `@MockitoBean EmailDeliveryChannelStrategy` — prevents real email sending
- `@MockitoBean RateLimiterService` — stub to always allow (or conditionally test rate limiting)
- `@ArgumentCaptor` to capture the email content and verify the magic link URL

| @Nested | Tests |
|---------|-------|
| `RequestAndVerify` | `shouldReturn200_whenRequestingMagicLink`, `shouldReturn200WithJwt_whenVerifyingValidToken`, `shouldCreateAdultStudentInDatabase_whenNewUserVerifies` |
| `ExistingUser` | `shouldReturn200WithJwt_whenExistingUserVerifiesMagicLink`, `shouldNotCreateDuplicateUser_whenExistingUserVerifies` |
| `TokenExpiry` | `shouldReturn401_whenTokenIsExpired` |
| `SingleUse` | `shouldReturn401_whenTokenIsUsedTwice` |
| `InvalidToken` | `shouldReturn401_whenTokenHashDoesNotExist` |

**Full flow test**:
1. POST `/v1/security/login/magic-link/request` with email
2. Capture the `NotificationDataModel` passed to `EmailDeliveryChannelStrategy.deliver()`
3. Extract the token from the captured magic link URL
4. POST `/v1/security/login/magic-link/verify` with the extracted token
5. Assert: 200 response with valid JWT
6. Assert: `MagicLinkTokenDataModel.usedAt` is not null in DB
7. Assert: `AdultStudentDataModel` exists in DB

#### Step 12.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

#### Step 12.3: Commit

```
test(application): add magic link authentication component tests

MagicLinkComponentTest — full Spring context + Testcontainers
MariaDB. Covers full request-verify flow, existing user login,
token expiry, single-use enforcement, and invalid token
handling. Email delivery mocked.
```

---

## 5. File Inventory

### New files (18)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `multi-tenant-data/.../security/MagicLinkTokenDataModel.java` | multi-tenant-data | 1 |
| 2 | `security/.../magiclink/interfaceadapters/MagicLinkTokenRepository.java` | security | 1 |
| 3 | `security/src/main/resources/openapi/magic-link-authentication.yaml` | security | 2 |
| 4 | `security/.../config/MagicLinkProperties.java` | security | 4 |
| 5 | `security/.../magiclink/exceptions/MagicLinkTokenExpiredException.java` | security | 5 |
| 6 | `security/.../magiclink/exceptions/MagicLinkTokenAlreadyUsedException.java` | security | 5 |
| 7 | `security/.../magiclink/exceptions/MagicLinkTokenNotFoundException.java` | security | 5 |
| 8 | `security/.../magiclink/usecases/MagicLinkRequestUseCase.java` | security | 6 |
| 9 | `application/.../magiclink/usecases/MagicLinkVerificationUseCase.java` | application | 7 |
| 10 | `application/.../magiclink/interfaceadapters/MagicLinkController.java` | application | 8 |
| 11 | `application/.../magiclink/config/MagicLinkControllerAdvice.java` | application | 8 |
| 12 | `security/test/.../magiclink/usecases/MagicLinkRequestUseCaseTest.java` | security | 10 |
| 13 | `application/test/.../magiclink/usecases/MagicLinkVerificationUseCaseTest.java` | application | 11 |
| 14 | `application/test/.../magiclink/interfaceadapters/MagicLinkControllerTest.java` | application | 11 |
| 15 | `application/test/.../magiclink/config/MagicLinkControllerAdviceTest.java` | application | 11 |
| 16 | `application/test/.../usecases/MagicLinkComponentTest.java` | application | 12 |
| 17 | DB schema migration file | infra | 1 |
| 18 | Email template HTML (inline constant in MagicLinkRequestUseCase) | security | 6 |

### Modified files (4)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `security/src/main/resources/openapi/security-module.yaml` | Add magic link refs | 2 |
| 2 | `security/.../config/SecurityConfig.java` | permitAll + CORS for magic link endpoints | 3 |
| 3 | `application/src/main/resources/application.properties` | Magic link config | 9 |
| 4 | DB schema file | Add magic_link_tokens table | 1 |

---

## 6. Key Design Decisions

### 6.1 Token Storage Trade-off

| Criterion | Hash-only (chosen) | Encrypted token | Raw token |
|-----------|-------------------|-----------------|-----------|
| DB compromise risk | Zero — hash is one-way | Low — encryption key compromise needed | Critical — all tokens exposed |
| Verification | Hash received token, compare | Decrypt stored token, compare | Direct comparison |
| Performance | Fast SHA-256 | Slower AES-GCM | Fastest |
| Compliance | Best practice (OWASP) | Acceptable | Not acceptable |
| Chosen because | DB compromise reveals no valid tokens; SHA-256 is fast enough for single verification | — | — |

### 6.2 Token Format Trade-off

| Criterion | URL-safe Base64 (chosen) | Hex encoding | UUID v4 |
|-----------|-------------------------|--------------|---------|
| Entropy (32 bytes) | 256 bits | 256 bits | 122 bits |
| URL safety | Native — no encoding needed | Safe | Safe (hyphens ok) |
| Length | 43 chars | 64 chars | 36 chars |
| Chosen because | Maximum entropy per character, no URL encoding needed | — | Insufficient entropy for security token |

### 6.3 Use Case Module Placement Trade-off

| Criterion | Split (chosen) | All in security | All in application |
|-----------|---------------|-----------------|-------------------|
| Request use case | security (single-module) | security | application |
| Verification use case | application (cross-module) | security (rule violation) | application |
| Hard Rule #14 compliance | Full | Violated — verification touches user-management | Over-abstraction for request |
| Chosen because | Request only needs security + notification; verification orchestrates security + user-management + JWT | — | — |

### 6.4 Anti-Enumeration Trade-off

| Criterion | Silent 200 (chosen) | Error on not-found | Delay response |
|-----------|---------------------|-------------------|----------------|
| Information leakage | None — always 200 | Reveals email existence | Timing side-channel possible |
| User experience | Slightly confusing if typo | Clear feedback | Slow |
| Security | Best — OWASP recommended | Poor | Medium |
| Chosen because | Prevents attackers from discovering valid email addresses via the magic link request endpoint | — | — |

---

## 7. Multi-Tenancy Considerations

1. **MagicLinkTokenDataModel**: Uses composite key `(tenantId, magicLinkTokenId)` — same pattern as all other entities. Tenant filter ensures tokens are scoped to their tenant.

2. **Token lookup**: `findByTokenHash` must include tenant scoping via the Hibernate tenant filter. The token hash alone is not globally unique — it is unique within a tenant.

3. **Tenant ID in magic link URL**: The magic link URL includes `tenant={tenantId}` as a query parameter. The frontend extracts this and sends it in the verify request body. This is necessary because the verify endpoint is unauthenticated and has no other way to determine the tenant context.

4. **Cross-tenant protection**: A token generated for tenant A cannot be used for tenant B because:
   - The `findByTokenHash` query is scoped by the Hibernate tenant filter
   - The tenant ID in the URL must match the tenant context set during verification

5. **Email delivery**: The `EmailDeliveryChannelStrategy` is tenant-agnostic — it sends to any email address. The tenant context is relevant only for token storage and user lookup.

---

## 8. Future Extensibility

1. **Configurable email templates**: Replace inline HTML with `EmailTemplateDataModel` + Thymeleaf rendering for branded per-tenant templates.
2. **Magic link for account linking**: Allow existing OAuth users to add email-based login via magic link verification.
3. **Device fingerprinting**: Store the requesting device fingerprint and validate it during verification to prevent token forwarding.
4. **Token revocation**: Add an endpoint to revoke all pending magic link tokens for an email (useful for password reset flows).
5. **Audit logging**: Log magic link requests and verifications to the audit system for compliance.
6. **SMS magic link**: Extend to support SMS-based magic links using the notification system's future SMS channel.

---

## 9. Verification Checklist

Run after all phases complete:

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — request use case tests green
- [ ] `mvn test -pl application -am -f platform-core-api/pom.xml` — verification use case + controller tests green
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests green
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, `shouldDoX_whenY()`, zero `any()` matchers
- [ ] `MagicLinkTokenDataModel` in multi-tenant-data with composite key
- [ ] `MagicLinkRequestUseCase` in security module (single-module, Hard Rule #12)
- [ ] `MagicLinkVerificationUseCase` in application module (cross-module, Hard Rule #14)
- [ ] No `new EntityDataModel()` — all via `applicationContext.getBean()`
- [ ] Request endpoint always returns 200 (anti-enumeration)
- [ ] Token stored as SHA-256 hash only — raw token never persisted
- [ ] Single-use enforcement via `usedAt` timestamp
- [ ] Token expiry configurable via properties
- [ ] Rate limiting: 3 magic link requests per email per hour
- [ ] Conventional Commits format, no AI attribution

---

## 10. Critical Reminders

1. **Raw token never stored** — only the SHA-256 hash is persisted. The raw token appears only in the email URL and the verify request.
2. **Always return 200 on request** — even if the email does not exist in the system, even if rate limited. This prevents email enumeration.
3. **`ApplicationContext.getBean()`** — all entity instantiation must use prototype bean pattern, never `new EntityDataModel()`.
4. **Tenant ID in URL** — the magic link URL must include `tenant={tenantId}` because the verify endpoint is unauthenticated.
5. **Email delivery is fire-and-forget** — the request use case sends the email and returns immediately. Email delivery failures are logged but do not fail the request.
6. **SecureRandom for token generation** — never use `Random` or `Math.random()`. The `SecureRandom` instance should be reused (it is thread-safe).
7. **Token hash as lookup key** — the `findByTokenHash` query is the primary lookup mechanism. The hash column must be indexed.
8. **No `any()` matchers** — all mock stubbing uses exact values or `ArgumentCaptor`.
9. **Rate limiting dependency** — `MagicLinkRequestUseCase` depends on `RateLimiterService` from the rate-limiting feature. If rate limiting is not yet implemented, use a stub or `@ConditionalOnBean` pattern.
10. **Notification system integration** — the email is sent via `EmailDeliveryChannelStrategy.deliver()`, which expects a `NotificationDataModel` and a recipient email string. Read the existing strategy before integrating.
