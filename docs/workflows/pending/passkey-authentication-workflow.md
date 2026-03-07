# Passkey Authentication Workflow — WebAuthn/FIDO2

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Prerequisite**: Read `docs/directives/CLAUDE.md`, `docs/directives/AI-CODE-REF.md`, and `docs/design/DESIGN.md` before starting.
**Dependency**: Requires jwt-refresh-token-rotation feature (Redis infrastructure for challenge store). If not yet implemented, the Redis configuration must be added as part of this workflow.

---

## 1. Architecture Overview

### 1.1 Purpose

Passkey authentication (WebAuthn/FIDO2) enables passwordless login using biometric authenticators (fingerprint, face recognition), hardware security keys (YubiKey), or platform authenticators (Touch ID, Windows Hello). This feature works for both AkademiaPlus and ElatusDev web app users, providing a more secure and convenient alternative to password-based authentication.

### 1.2 WebAuthn Flow

```
┌────────────────────────────────────────────────────────────────────────────┐
│  REGISTRATION FLOW                                                        │
│                                                                            │
│  Client (Browser)                    core-api                              │
│  ─────────────────                   ────────                              │
│  1. POST /v1/security/passkey/register/options                             │
│     { tenantId }                                                           │
│     ←── { challenge, rp, user, pubKeyCredParams, ... }                     │
│                                                                            │
│  2. navigator.credentials.create(options)                                  │
│     ←── Authenticator creates key pair, signs challenge                    │
│                                                                            │
│  3. POST /v1/security/passkey/register/complete                            │
│     { attestationObject, clientDataJSON, tenantId, displayName }           │
│     ←── { success: true }                                                  │
│     Server: validates response, stores credential in DB                    │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│  AUTHENTICATION FLOW                                                       │
│                                                                            │
│  Client (Browser)                    core-api                              │
│  ─────────────────                   ────────                              │
│  1. POST /v1/security/passkey/login/options                                │
│     { tenantId }                                                           │
│     ←── { challenge, rpId, allowCredentials, ... }                         │
│                                                                            │
│  2. navigator.credentials.get(options)                                     │
│     ←── Authenticator signs challenge with private key                     │
│                                                                            │
│  3. POST /v1/security/passkey/login/complete                               │
│     { authenticatorData, clientDataJSON, signature, userHandle }           │
│     ←── { token } (platform JWT)                                           │
│     Server: validates signature, updates sign count, issues JWT            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  application module                                              │
│                                                                  │
│  PasskeyAuthenticationUseCase (cross-module orchestrator)         │
│    ├── Generates authentication options (challenge)              │
│    ├── Validates authentication response (signature)             │
│    ├── Updates sign count                                        │
│    ├── Issues platform JWT via JwtTokenProvider                  │
│    └── Delegates to PasskeyRegistrationUseCase for registration  │
│                                                                  │
│  PasskeyController (implements generated PasskeyApi)             │
│    ├── POST /register/options                                    │
│    ├── POST /register/complete                                   │
│    ├── POST /login/options                                       │
│    └── POST /login/complete                                      │
│                                                                  │
│  PasskeyControllerAdvice (extends BaseControllerAdvice)          │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  security module                                                 │
│                                                                  │
│  PasskeyRegistrationUseCase                                      │
│    ├── Generates registration options                            │
│    ├── Validates registration response                           │
│    └── Stores credential via PasskeyCredentialRepository         │
│                                                                  │
│  PasskeyChallengeStore (Redis-backed, 5-min TTL)                 │
│                                                                  │
│  PasskeyCredentialRepository (implements CredentialRepository)    │
│                                                                  │
│  RelyingPartyConfiguration (@Bean RelyingParty)                  │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  multi-tenant-data module                                        │
│                                                                  │
│  PasskeyCredentialDataModel (@Entity)                            │
│    ├── tenantId + credentialId (composite key)                   │
│    ├── userId, publicKey, signCount, transports                  │
│    ├── createdAt, lastUsedAt, displayName                        │
│    └── Extends TenantScoped                                      │
└─────────────────────────────────────────────────────────────────┘
```

### 1.4 Module Placement

Per CLAUDE.md Hard Rules #5, #12, #13, #14 and DESIGN.md Section 3.2.8:

| Component | Module | Package | Rationale |
|-----------|--------|---------|-----------|
| `PasskeyCredentialDataModel` | multi-tenant-data | `com.akademiaplus.security/` | JPA entity — lives with other security entities |
| `PasskeyRegistrationUseCase` | security | `com.akademiaplus.passkey/usecases/` | Security-specific use case (Hard Rule #12) — handles WebAuthn crypto |
| `PasskeyChallengeStore` | security | `com.akademiaplus.passkey/usecases/` | Challenge storage — security infrastructure |
| `PasskeyCredentialRepository` | security | `com.akademiaplus.passkey/interfaceadapters/` | Implements Yubico `CredentialRepository` — security adapter |
| `PasskeyCredentialJpaRepository` | security | `com.akademiaplus.passkey/interfaceadapters/` | Spring Data JPA repository for DB access |
| `RelyingPartyConfiguration` | security | `com.akademiaplus.passkey/config/` | Spring `@Configuration` bean |
| `PasskeyProperties` | security | `com.akademiaplus.passkey/config/` | `@ConfigurationProperties` for rpId, rpName, origins |
| `PasskeyAuthenticationUseCase` | application | `com.akademiaplus.passkey/usecases/` | Cross-module orchestrator (Hard Rule #14) — calls security + user repos |
| `PasskeyController` | application | `com.akademiaplus.passkey/interfaceadapters/` | Controller for orchestrator |
| `PasskeyControllerAdvice` | application | `com.akademiaplus.passkey/config/` | Exception handling scoped to PasskeyController |
| `PasskeyRegistrationException` | security | `com.akademiaplus.passkey/exceptions/` | Registration failure |
| `PasskeyAuthenticationException` | security | `com.akademiaplus.passkey/exceptions/` | Authentication failure |
| OpenAPI spec | security | `src/main/resources/openapi/` | API contract |

### 1.5 Relying Party Configuration

The WebAuthn Relying Party (RP) is the server entity that the authenticator trusts. Configuration varies per deployment:

| Property | Dev | Production (AkademiaPlus) | Production (ElatusDev) |
|----------|-----|--------------------------|----------------------|
| `rpId` | `localhost` | `akademiaplus.com` | `elatusdev.com` |
| `rpName` | `AkademiaPlus Dev` | `AkademiaPlus` | `ElatusDev` |
| `origins` | `http://localhost:3000` | `https://app.akademiaplus.com` | `https://elatusdev.com` |

The `rpId` determines which credentials are valid — credentials registered with `akademiaplus.com` cannot be used on `elatusdev.com`. Since both apps are served by the same API, the RP ID is resolved per-request based on the `Origin` header.

### 1.6 Challenge Store

WebAuthn challenges are single-use random values that prevent replay attacks. They must be stored server-side between the options request and the complete request. Redis provides:

- **5-minute TTL**: Challenges expire automatically
- **Atomic operations**: Prevents race conditions in concurrent registrations
- **Shared state**: Works across multiple API instances in production

Key format: `passkey:challenge:{challengeBase64}` → value: serialized challenge metadata (userId, tenantId, operation type).

---

## 2. File Inventory

### 2.1 New Files (20)

| # | File | Module | Phase |
|---|------|--------|-------|
| 1 | `multi-tenant-data/src/main/java/com/akademiaplus/security/PasskeyCredentialDataModel.java` | multi-tenant-data | 2 |
| 2 | DB schema: `passkey_credentials` table creation | infra | 2 |
| 3 | `security/src/main/resources/openapi/passkey-authentication.yaml` | security | 3 |
| 4 | `security/src/main/java/com/akademiaplus/passkey/config/PasskeyProperties.java` | security | 4 |
| 5 | `security/src/main/java/com/akademiaplus/passkey/config/RelyingPartyConfiguration.java` | security | 4 |
| 6 | `security/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialJpaRepository.java` | security | 4 |
| 7 | `security/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialRepositoryAdapter.java` | security | 4 |
| 8 | `security/src/main/java/com/akademiaplus/passkey/usecases/PasskeyChallengeStore.java` | security | 5 |
| 9 | `security/src/main/java/com/akademiaplus/passkey/exceptions/PasskeyRegistrationException.java` | security | 6 |
| 10 | `security/src/main/java/com/akademiaplus/passkey/exceptions/PasskeyAuthenticationException.java` | security | 6 |
| 11 | `security/src/main/java/com/akademiaplus/passkey/usecases/PasskeyRegistrationUseCase.java` | security | 6 |
| 12 | `application/src/main/java/com/akademiaplus/passkey/usecases/PasskeyAuthenticationUseCase.java` | application | 7 |
| 13 | `application/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyController.java` | application | 8 |
| 14 | `application/src/main/java/com/akademiaplus/passkey/config/PasskeyControllerAdvice.java` | application | 8 |
| 15 | `security/src/test/java/com/akademiaplus/passkey/usecases/PasskeyRegistrationUseCaseTest.java` | security | 10 |
| 16 | `security/src/test/java/com/akademiaplus/passkey/usecases/PasskeyChallengeStoreTest.java` | security | 10 |
| 17 | `security/src/test/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialRepositoryAdapterTest.java` | security | 10 |
| 18 | `application/src/test/java/com/akademiaplus/passkey/usecases/PasskeyAuthenticationUseCaseTest.java` | application | 11 |
| 19 | `application/src/test/java/com/akademiaplus/passkey/interfaceadapters/PasskeyControllerTest.java` | application | 11 |
| 20 | `application/src/test/java/com/akademiaplus/usecases/PasskeyComponentTest.java` | application | 12 |

### 2.2 Modified Files (5)

| # | File | Change | Phase |
|---|------|--------|-------|
| 1 | `security/pom.xml` | Add `java-webauthn-server` dependency | 1 |
| 2 | `security/src/main/resources/openapi/security-module.yaml` | Add passkey path + schema refs | 3 |
| 3 | `security/src/main/java/com/akademiaplus/config/SecurityConfig.java` | Permit passkey endpoints | 9 |
| 4 | `application/src/main/resources/application.properties` | Add passkey RP configuration | 4 |
| 5 | DB schema file | Add `passkey_credentials` table | 2 |

---

## 3. Implementation Sequence

### Phase Dependency Graph

```
Phase 1:  Dependencies (java-webauthn-server in security pom.xml)
    ↓
Phase 2:  PasskeyCredentialDataModel + DB schema (multi-tenant-data)
    ↓
Phase 3:  OpenAPI specifications (security module)
    ↓
Phase 4:  RelyingPartyConfiguration + PasskeyCredentialRepository (security module)
    ↓
Phase 5:  Challenge store — Redis (security module)
    ↓
Phase 6:  PasskeyRegistrationUseCase + exceptions (security module)
    ↓
Phase 7:  PasskeyAuthenticationUseCase (application module — cross-module)
    ↓
Phase 8:  Controllers + ControllerAdvice (application module)
    ↓
Phase 9:  SecurityConfig updates — permit passkey endpoints
    ↓
Phase 10: Unit tests (security module)
    ↓
Phase 11: Unit tests (application module)
    ↓
Phase 12: Component tests (application module)
```

---

## 4. Phase-by-Phase Implementation

### Phase 1: Dependencies

#### Step 1.1: Add java-webauthn-server to security/pom.xml

**File**: `security/pom.xml`

Add the Yubico WebAuthn server library:

```xml
<!-- WebAuthn/FIDO2 Passkey Support -->
<dependency>
    <groupId>com.yubico</groupId>
    <artifactId>webauthn-server-core</artifactId>
    <version>2.5.3</version>
</dependency>
```

Also add `spring-boot-starter-data-redis` if not already present (needed for challenge store):

```xml
<!-- Redis for WebAuthn challenge store -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### Step 1.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 1.3: Commit

```
feat(security): add java-webauthn-server and Redis dependencies

Add Yubico webauthn-server-core 2.5.3 for FIDO2/WebAuthn passkey
support. Add spring-boot-starter-data-redis for challenge storage.
```

---

### Phase 2: PasskeyCredentialDataModel + DB Schema

#### Step 2.1: Create PasskeyCredentialDataModel

**File**: `multi-tenant-data/src/main/java/com/akademiaplus/security/PasskeyCredentialDataModel.java`

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
 * Entity representing a WebAuthn/FIDO2 passkey credential.
 *
 * <p>Stores the public key and metadata for a registered passkey authenticator.
 * Each user can have multiple passkey credentials (e.g., fingerprint on phone,
 * YubiKey, Touch ID on laptop).
 *
 * <p>The composite primary key is {@code (tenantId, passkeyCredentialId)}.
 * The {@code credentialId} (from the authenticator) is a unique index within
 * the tenant for WebAuthn assertion lookup.
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
@Table(name = "passkey_credentials")
@SQLDelete(sql = "UPDATE passkey_credentials SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND passkey_credential_id = ?")
@IdClass(PasskeyCredentialDataModel.PasskeyCredentialCompositeId.class)
public class PasskeyCredentialDataModel extends TenantScoped {

    /**
     * Unique identifier for the passkey credential within the tenant.
     */
    @Id
    @Column(name = "passkey_credential_id")
    private Long passkeyCredentialId;

    /**
     * The user ID that owns this credential (references internal or customer auth).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The credential ID assigned by the authenticator (Base64URL-encoded).
     * Used during authentication to identify which credential to use.
     */
    @Lob
    @Column(name = "credential_id", nullable = false, columnDefinition = "BLOB")
    private byte[] credentialId;

    /**
     * The COSE public key from the authenticator (CBOR-encoded).
     * Used to verify assertion signatures during authentication.
     */
    @Lob
    @Column(name = "public_key", nullable = false, columnDefinition = "BLOB")
    private byte[] publicKey;

    /**
     * Signature counter — incremented by the authenticator on each use.
     * Detects cloned authenticators when the counter decreases.
     */
    @Column(name = "sign_count", nullable = false)
    private Long signCount;

    /**
     * Comma-separated list of supported transports (e.g., "usb,nfc,ble,internal").
     */
    @Column(name = "transports", length = 255)
    private String transports;

    /**
     * Timestamp when the credential was registered.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Timestamp of the last successful authentication with this credential.
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * Human-readable name for this credential (e.g., "MacBook Touch ID", "YubiKey 5").
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * The user handle (WebAuthn user.id) — Base64URL-encoded.
     * Used to identify the user during authentication without revealing the username.
     */
    @Lob
    @Column(name = "user_handle", nullable = false, columnDefinition = "BLOB")
    private byte[] userHandle;

    /**
     * Composite primary key class for PasskeyCredential entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PasskeyCredentialCompositeId implements Serializable {
        protected Long tenantId;
        protected Long passkeyCredentialId;
    }
}
```

#### Step 2.2: DB Schema

Add the `passkey_credentials` table to the schema file:

```sql
CREATE TABLE passkey_credentials (
    tenant_id         BIGINT       NOT NULL,
    passkey_credential_id BIGINT   NOT NULL,
    user_id           BIGINT       NOT NULL,
    credential_id     BLOB         NOT NULL,
    public_key        BLOB         NOT NULL,
    sign_count        BIGINT       NOT NULL DEFAULT 0,
    transports        VARCHAR(255),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at      TIMESTAMP,
    display_name      VARCHAR(255),
    user_handle       BLOB         NOT NULL,
    deleted_at        TIMESTAMP,
    PRIMARY KEY (tenant_id, passkey_credential_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_passkey_cred_tenant_credential
    ON passkey_credentials (tenant_id, credential_id(255));

CREATE INDEX idx_passkey_cred_tenant_user
    ON passkey_credentials (tenant_id, user_id);

CREATE INDEX idx_passkey_cred_tenant_user_handle
    ON passkey_credentials (tenant_id, user_handle(255));
```

#### Step 2.3: Compile

```bash
mvn clean compile -pl multi-tenant-data -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 2.4: Commit

```
feat(multi-tenant-data): add PasskeyCredentialDataModel entity

Add JPA entity for WebAuthn/FIDO2 passkey credentials with
composite key (tenantId, passkeyCredentialId). Stores credential ID,
public key, sign count, transports, and user handle. Add DB schema
with indexes for credential and user lookups.
```

---

### Phase 3: OpenAPI Specifications

#### Step 3.1: Create passkey-authentication.yaml

**File**: `security/src/main/resources/openapi/passkey-authentication.yaml`

Defines 4 endpoints:

```yaml
openapi: 3.1.0
info:
  title: Passkey Authentication API
  version: 1.0.0
  description: WebAuthn/FIDO2 passkey registration and authentication endpoints

paths:
  /passkey/register/options:
    post:
      summary: Generate passkey registration options
      description: >
        Returns PublicKeyCredentialCreationOptions for the browser's
        navigator.credentials.create() call. Requires an authenticated user.
      operationId: passkeyRegisterOptions
      tags:
        - passkey
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PasskeyRegisterOptionsRequest'
      responses:
        '200':
          description: Registration options generated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PasskeyRegisterOptionsResponse'
        '400':
          description: Invalid request
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/ErrorResponse'

  /passkey/register/complete:
    post:
      summary: Complete passkey registration
      description: >
        Validates the authenticator's registration response and stores the
        credential. Requires an authenticated user.
      operationId: passkeyRegisterComplete
      tags:
        - passkey
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PasskeyRegisterCompleteRequest'
      responses:
        '200':
          description: Registration successful
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PasskeyRegisterCompleteResponse'
        '400':
          description: Registration validation failed
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/ErrorResponse'

  /passkey/login/options:
    post:
      summary: Generate passkey authentication options
      description: >
        Returns PublicKeyCredentialRequestOptions for the browser's
        navigator.credentials.get() call. No authentication required.
      operationId: passkeyLoginOptions
      tags:
        - passkey
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PasskeyLoginOptionsRequest'
      responses:
        '200':
          description: Authentication options generated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PasskeyLoginOptionsResponse'

  /passkey/login/complete:
    post:
      summary: Complete passkey authentication
      description: >
        Validates the authenticator's assertion response, updates the
        sign count, and issues a platform JWT.
      operationId: passkeyLoginComplete
      tags:
        - passkey
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PasskeyLoginCompleteRequest'
      responses:
        '200':
          description: Authentication successful
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/AuthTokenResponse'
        '401':
          description: Authentication failed
          content:
            application/json:
              schema:
                $ref: './internal-authentication.yaml#/components/schemas/ErrorResponse'
```

Schemas include Base64URL-encoded fields for challenge, credential ID, attestation objects, and client data JSON. All request schemas include `tenantId` (Long).

#### Step 3.2: Modify security-module.yaml

Add passkey path and schema references following the same pattern as internal-authentication.yaml references.

#### Step 3.3: Regenerate DTOs

```bash
mvn clean generate-sources -pl security -am -DskipTests -f platform-core-api/pom.xml
```

Verify generated interfaces and DTOs.

#### Step 3.4: Commit

```
api(security): add passkey authentication OpenAPI specification

Add 4 passkey endpoints: register/options, register/complete,
login/options, login/complete. Define request/response schemas
with Base64URL-encoded WebAuthn fields.
```

---

### Phase 4: RelyingPartyConfiguration + PasskeyCredentialRepository

#### Step 4.1: PasskeyProperties

**File**: `security/src/main/java/com/akademiaplus/passkey/config/PasskeyProperties.java`

```java
@ConfigurationProperties(prefix = "security.passkey")
public class PasskeyProperties {

    /** Relying Party ID — typically the domain (e.g., "akademiaplus.com"). */
    private String rpId;

    /** Relying Party display name shown to users during registration. */
    private String rpName;

    /** Allowed origins for WebAuthn operations. */
    private List<String> allowedOrigins = List.of();

    /** Challenge time-to-live in seconds (default: 300 = 5 minutes). */
    private Long challengeTtlSeconds = 300L;

    // Getters and setters
}
```

#### Step 4.2: RelyingPartyConfiguration

**File**: `security/src/main/java/com/akademiaplus/passkey/config/RelyingPartyConfiguration.java`

```java
@Configuration
@EnableConfigurationProperties(PasskeyProperties.class)
public class RelyingPartyConfiguration {

    /**
     * Creates the WebAuthn RelyingParty bean with configurable RP ID and name.
     *
     * @param properties          passkey configuration properties
     * @param credentialRepository the credential repository adapter
     * @return configured RelyingParty instance
     */
    @Bean
    public RelyingParty relyingParty(PasskeyProperties properties,
                                      PasskeyCredentialRepositoryAdapter credentialRepository) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(properties.getRpId())
                .name(properties.getRpName())
                .build();

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(credentialRepository)
                .allowedOrigins(properties.getAllowedOrigins()
                        .stream().map(URI::create).collect(Collectors.toSet()))
                .build();
    }
}
```

#### Step 4.3: PasskeyCredentialJpaRepository

**File**: `security/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialJpaRepository.java`

```java
/**
 * Spring Data JPA repository for passkey credential persistence.
 */
public interface PasskeyCredentialJpaRepository
        extends TenantScopedRepository<PasskeyCredentialDataModel, PasskeyCredentialDataModel.PasskeyCredentialCompositeId> {

    /**
     * Finds all credentials for a given user within the tenant.
     */
    List<PasskeyCredentialDataModel> findByUserId(Long userId);

    /**
     * Finds a credential by its authenticator-assigned credential ID within the tenant.
     */
    Optional<PasskeyCredentialDataModel> findByCredentialId(byte[] credentialId);

    /**
     * Finds all credentials by user handle within the tenant.
     */
    List<PasskeyCredentialDataModel> findByUserHandle(byte[] userHandle);
}
```

#### Step 4.4: PasskeyCredentialRepositoryAdapter

**File**: `security/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialRepositoryAdapter.java`

Implements the Yubico `CredentialRepository` interface, delegating to `PasskeyCredentialJpaRepository`:

```java
/**
 * Adapter between Yubico's CredentialRepository interface and the JPA repository.
 *
 * <p>Translates WebAuthn credential lookups into JPA queries on the
 * passkey_credentials table. All lookups are tenant-scoped via Hibernate filters.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class PasskeyCredentialRepositoryAdapter implements CredentialRepository {

    private final PasskeyCredentialJpaRepository jpaRepository;

    // getCredentialIdsForUsername(String username)
    // getUserHandleForUsername(String username)
    // getUsernameForUserHandle(ByteArray userHandle)
    // lookup(ByteArray credentialId, ByteArray userHandle)
    // lookupAll(ByteArray credentialId)
}
```

#### Step 4.5: Add configuration to application.properties

```properties
# Passkey / WebAuthn Configuration
security.passkey.rp-id=localhost
security.passkey.rp-name=AkademiaPlus Dev
security.passkey.allowed-origins[0]=http://localhost:3000
security.passkey.challenge-ttl-seconds=300
```

#### Step 4.6: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 4.7: Commit

```
feat(security): add RelyingParty config and credential repository

Add PasskeyProperties, RelyingPartyConfiguration (@Bean RelyingParty),
PasskeyCredentialJpaRepository, and PasskeyCredentialRepositoryAdapter
implementing Yubico CredentialRepository interface.
```

---

### Phase 5: Challenge Store (Redis)

#### Step 5.1: PasskeyChallengeStore

**File**: `security/src/main/java/com/akademiaplus/passkey/usecases/PasskeyChallengeStore.java`

```java
/**
 * Redis-backed store for WebAuthn challenges with automatic expiration.
 *
 * <p>Challenges are stored with a 5-minute TTL to prevent replay attacks.
 * Key format: {@code passkey:challenge:{challengeBase64}}.
 *
 * <p>Each entry stores the challenge metadata (userId, tenantId, operation type)
 * as a JSON string for retrieval during the complete step.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class PasskeyChallengeStore {

    /** Redis key prefix for passkey challenges. */
    public static final String KEY_PREFIX = "passkey:challenge:";

    /** Error message when challenge is not found or expired. */
    public static final String ERROR_CHALLENGE_NOT_FOUND = "Challenge not found or expired";

    /** Error message when challenge storage fails. */
    public static final String ERROR_CHALLENGE_STORE_FAILED = "Failed to store challenge";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasskeyProperties properties;

    /**
     * Stores a challenge with its associated metadata.
     *
     * @param challengeBase64 the Base64URL-encoded challenge
     * @param metadata        the challenge metadata (userId, tenantId, operation)
     */
    public void store(String challengeBase64, ChallengeMetadata metadata) { ... }

    /**
     * Retrieves and removes a challenge (single-use).
     *
     * @param challengeBase64 the Base64URL-encoded challenge
     * @return the challenge metadata
     * @throws PasskeyAuthenticationException if the challenge is not found or expired
     */
    public ChallengeMetadata consumeChallenge(String challengeBase64) { ... }

    /**
     * Challenge metadata stored alongside the challenge value.
     *
     * @param userId    the user ID (null for login — user unknown before assertion)
     * @param tenantId  the tenant ID from the request
     * @param operation the operation type (REGISTER or LOGIN)
     */
    public record ChallengeMetadata(Long userId, Long tenantId, String operation) {}
}
```

#### Step 5.2: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 5.3: Commit

```
feat(security): add Redis-backed PasskeyChallengeStore

Add challenge store with 5-minute TTL for WebAuthn registration
and authentication flows. Supports single-use challenge consumption
with JSON-serialized metadata.
```

---

### Phase 6: PasskeyRegistrationUseCase + Exceptions

#### Step 6.1: Exceptions

**File**: `security/src/main/java/com/akademiaplus/passkey/exceptions/PasskeyRegistrationException.java`

```java
public class PasskeyRegistrationException extends RuntimeException {
    public static final String ERROR_REGISTRATION_FAILED = "Passkey registration failed: %s";
    public static final String ERROR_CREDENTIAL_ALREADY_EXISTS = "A passkey with this credential ID already exists";
    // Constructor with message, Constructor with message + cause
}
```

**File**: `security/src/main/java/com/akademiaplus/passkey/exceptions/PasskeyAuthenticationException.java`

```java
public class PasskeyAuthenticationException extends RuntimeException {
    public static final String ERROR_AUTHENTICATION_FAILED = "Passkey authentication failed: %s";
    public static final String ERROR_CREDENTIAL_NOT_FOUND = "No passkey credential found for the provided ID";
    public static final String ERROR_SIGN_COUNT_REGRESSION = "Authenticator sign count regression detected — possible cloned authenticator";
    // Constructor with message, Constructor with message + cause
}
```

#### Step 6.2: PasskeyRegistrationUseCase

**File**: `security/src/main/java/com/akademiaplus/passkey/usecases/PasskeyRegistrationUseCase.java`

```java
/**
 * Handles WebAuthn passkey registration within the security module.
 *
 * <p>Generates registration options (challenge, relying party info, user info)
 * and validates the authenticator's registration response. On successful
 * validation, stores the credential in the database.
 *
 * <p>This use case is security-scoped — it does not cross module boundaries.
 * The {@code PasskeyAuthenticationUseCase} in the application module
 * orchestrates cross-module concerns (user lookup, JWT issuance).
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class PasskeyRegistrationUseCase {

    private final RelyingParty relyingParty;
    private final PasskeyChallengeStore challengeStore;
    private final PasskeyCredentialJpaRepository credentialRepository;
    private final ApplicationContext applicationContext;

    /**
     * Generates registration options for a new passkey.
     *
     * @param userId    the authenticated user's ID
     * @param username  the user's display name / email
     * @param tenantId  the tenant ID
     * @return PublicKeyCredentialCreationOptions for the browser
     */
    @Transactional
    public PublicKeyCredentialCreationOptions generateRegistrationOptions(
            Long userId, String username, byte[] userHandle, Long tenantId) {
        // 1. Build UserIdentity from userId, username, userHandle
        // 2. Call relyingParty.startRegistration(StartRegistrationOptions)
        // 3. Store challenge in PasskeyChallengeStore with REGISTER operation
        // 4. Return PublicKeyCredentialCreationOptions
    }

    /**
     * Validates the registration response and stores the credential.
     *
     * @param responseJson the authenticator's registration response (JSON)
     * @param tenantId     the tenant ID
     * @return the stored credential's display name
     * @throws PasskeyRegistrationException if validation fails
     */
    @Transactional
    public String completeRegistration(String responseJson, Long tenantId, String displayName) {
        // 1. Parse PublicKeyCredential from JSON
        // 2. Consume challenge from PasskeyChallengeStore
        // 3. Call relyingParty.finishRegistration(FinishRegistrationOptions)
        // 4. Extract credential data (credentialId, publicKey, signCount, transports)
        // 5. Create PasskeyCredentialDataModel via applicationContext.getBean()
        // 6. Save to DB
        // 7. Return displayName
    }
}
```

#### Step 6.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 6.4: Commit

```
feat(security): implement PasskeyRegistrationUseCase

Add passkey registration use case with challenge generation,
authenticator response validation, and credential storage.
Add PasskeyRegistrationException and PasskeyAuthenticationException.
Uses RelyingParty from java-webauthn-server for cryptographic validation.
```

---

### Phase 7: PasskeyAuthenticationUseCase

**File**: `application/src/main/java/com/akademiaplus/passkey/usecases/PasskeyAuthenticationUseCase.java`

This is a **cross-module orchestrator** (Hard Rule #14 — lives in `application` module). It coordinates between the security module (WebAuthn validation) and user management (user lookup for JWT).

```java
/**
 * Cross-module orchestrator for passkey authentication.
 *
 * <p>Coordinates WebAuthn assertion validation (security module) with user
 * lookup (user-management module) and JWT issuance. Handles both the
 * options generation phase and the complete/validation phase.
 *
 * <p>Lives in the application module per Hard Rule #14 — cross-module
 * orchestration is restricted to this module.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class PasskeyAuthenticationUseCase {

    public static final String JWT_CLAIM_ROLE = "Has role";
    public static final String JWT_CLAIM_AUTH_METHOD = "auth_method";
    public static final String AUTH_METHOD_PASSKEY = "passkey";

    private final RelyingParty relyingParty;
    private final PasskeyChallengeStore challengeStore;
    private final PasskeyCredentialJpaRepository credentialRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Generates authentication options for passkey login.
     *
     * @param tenantId the tenant ID from the request
     * @return AssertionRequest containing challenge and allowed credentials
     */
    @Transactional(readOnly = true)
    public AssertionRequest generateLoginOptions(Long tenantId) {
        // 1. Set tenant context
        // 2. Call relyingParty.startAssertion(StartAssertionOptions)
        // 3. Store challenge in PasskeyChallengeStore with LOGIN operation
        // 4. Return AssertionRequest (serialized to JSON for client)
    }

    /**
     * Validates the assertion response, updates sign count, and issues a JWT.
     *
     * @param responseJson the authenticator's assertion response (JSON)
     * @param tenantId     the tenant ID from the request
     * @return AuthTokenResponseDTO containing the platform JWT
     * @throws PasskeyAuthenticationException if validation fails
     */
    @Transactional
    public AuthTokenResponseDTO completeLogin(String responseJson, Long tenantId) {
        // 1. Set tenant context
        // 2. Parse PublicKeyCredential from JSON
        // 3. Consume challenge from PasskeyChallengeStore
        // 4. Call relyingParty.finishAssertion(FinishAssertionOptions)
        // 5. Update sign count on credential
        // 6. Update lastUsedAt timestamp
        // 7. Lookup user by userHandle → resolve email/username
        // 8. Build JWT claims (role, auth_method=passkey)
        // 9. Create and return JWT via jwtTokenProvider.createToken()
    }
}
```

**Key interaction**: The `completeLogin` method needs to resolve the user identity from the `userHandle` stored in the credential. It queries the `PasskeyCredentialJpaRepository` by `userHandle` to find the credential, then uses the `userId` to look up the user's email/username for the JWT subject.

#### Commit

```
feat(application): implement PasskeyAuthenticationUseCase

Add cross-module orchestrator for passkey login flow:
- Generate authentication options with challenge
- Validate assertion response (signature verification)
- Update sign count and last-used timestamp
- Issue platform JWT with auth_method=passkey claim
```

---

### Phase 8: Controllers + ControllerAdvice

#### Step 8.1: PasskeyController

**File**: `application/src/main/java/com/akademiaplus/passkey/interfaceadapters/PasskeyController.java`

- `@RestController @RequestMapping("/v1/security")`
- Implements generated `PasskeyApi` interface
- 4 endpoints:
  - `POST /passkey/register/options` — authenticated (requires valid JWT)
  - `POST /passkey/register/complete` — authenticated
  - `POST /passkey/login/options` — unauthenticated (public)
  - `POST /passkey/login/complete` — unauthenticated (public)
- Delegates to `PasskeyRegistrationUseCase` and `PasskeyAuthenticationUseCase`
- Thin controller — zero business logic

#### Step 8.2: PasskeyControllerAdvice

**File**: `application/src/main/java/com/akademiaplus/passkey/config/PasskeyControllerAdvice.java`

```java
@ControllerAdvice(basePackageClasses = PasskeyController.class)
public class PasskeyControllerAdvice extends BaseControllerAdvice {

    /** Error code for passkey registration failure. */
    public static final String CODE_PASSKEY_REGISTRATION_FAILED = "PASSKEY_REGISTRATION_FAILED";

    /** Error code for passkey authentication failure. */
    public static final String CODE_PASSKEY_AUTHENTICATION_FAILED = "PASSKEY_AUTHENTICATION_FAILED";

    @ExceptionHandler(PasskeyRegistrationException.class)
    public ResponseEntity<ErrorResponseDTO> handleRegistrationFailure(PasskeyRegistrationException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_PASSKEY_REGISTRATION_FAILED);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PasskeyAuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationFailure(PasskeyAuthenticationException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_PASSKEY_AUTHENTICATION_FAILED);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}
```

#### Step 8.3: Compile

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 8.4: Commit

```
feat(application): add PasskeyController and ControllerAdvice

Implement generated PasskeyApi interface with 4 endpoints.
Add PasskeyControllerAdvice for registration (400) and
authentication (401) failure handling.
```

---

### Phase 9: SecurityConfig Updates

#### Step 9.1: Permit passkey login endpoints

**File**: `security/src/main/java/com/akademiaplus/config/SecurityConfig.java`

Add:
```java
.requestMatchers("/v1/security/passkey/login/**").permitAll()
```

Registration endpoints (`/passkey/register/**`) remain authenticated — only logged-in users can register new passkeys.

#### Step 9.2: Add CORS rules for passkey endpoints

Add:
```java
source.registerCorsConfiguration("/v1/security/passkey/login/**", loginCorsConfig);
```

#### Step 9.3: Compile

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
```

#### Step 9.4: Commit

```
feat(security): permit passkey login endpoints in SecurityConfig

Add /v1/security/passkey/login/** to permitAll and CORS config.
Registration endpoints remain authenticated.
```

---

### Phase 10: Unit Tests — Security Module

All tests follow project conventions: Given-When-Then, `shouldDoX_whenY()`, `@DisplayName`, `@Nested`, zero `any()` matchers, string literal constants.

#### Step 10.1: PasskeyRegistrationUseCaseTest

**File**: `security/src/test/java/com/akademiaplus/passkey/usecases/PasskeyRegistrationUseCaseTest.java`

| @Nested | Tests |
|---------|-------|
| `OptionsGeneration` | `shouldReturnCreationOptions_whenUserIsValid`, `shouldStoreChallenge_whenOptionsGenerated`, `shouldIncludeRpInfo_whenOptionsGenerated` |
| `RegistrationCompletion` | `shouldStoreCredential_whenRegistrationSucceeds`, `shouldSetSignCountToZero_whenNewCredential`, `shouldSetCreatedAtTimestamp_whenCredentialStored` |
| `ErrorPaths` | `shouldThrowPasskeyRegistrationException_whenValidationFails`, `shouldThrowPasskeyRegistrationException_whenChallengeExpired` |

#### Step 10.2: PasskeyChallengeStoreTest

**File**: `security/src/test/java/com/akademiaplus/passkey/usecases/PasskeyChallengeStoreTest.java`

| @Nested | Tests |
|---------|-------|
| `Storage` | `shouldStoreChallenge_whenCalledWithValidMetadata`, `shouldSetTtl_whenStoringChallenge` |
| `Consumption` | `shouldReturnMetadata_whenChallengeExists`, `shouldDeleteChallenge_whenConsumed`, `shouldThrowPasskeyAuthenticationException_whenChallengeNotFound` |

#### Step 10.3: PasskeyCredentialRepositoryAdapterTest

**File**: `security/src/test/java/com/akademiaplus/passkey/interfaceadapters/PasskeyCredentialRepositoryAdapterTest.java`

| @Nested | Tests |
|---------|-------|
| `CredentialLookup` | `shouldReturnCredentialIds_whenUserHasRegisteredPasskeys`, `shouldReturnEmptySet_whenUserHasNoPasskeys` |
| `UserHandleLookup` | `shouldReturnUserHandle_whenUsernameExists`, `shouldReturnEmpty_whenUsernameNotFound` |
| `AssertionLookup` | `shouldReturnRegisteredCredential_whenCredentialIdAndUserHandleMatch`, `shouldReturnEmpty_whenCredentialIdNotFound` |

#### Step 10.4: Compile + test

```bash
mvn clean compile -pl security -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl security -am -f platform-core-api/pom.xml
```

#### Step 10.5: Commit

```
test(security): add passkey registration, challenge store, and repository tests

PasskeyRegistrationUseCaseTest — covers options generation,
registration completion, and error paths.
PasskeyChallengeStoreTest — covers storage, consumption, and expiry.
PasskeyCredentialRepositoryAdapterTest — covers credential and
user handle lookups.
```

---

### Phase 11: Unit Tests — Application Module

#### Step 11.1: PasskeyAuthenticationUseCaseTest

**File**: `application/src/test/java/com/akademiaplus/passkey/usecases/PasskeyAuthenticationUseCaseTest.java`

| @Nested | Tests |
|---------|-------|
| `LoginOptionsGeneration` | `shouldReturnAssertionRequest_whenCalled`, `shouldStoreChallengeWithLoginOperation_whenOptionsGenerated`, `shouldSetTenantContext_whenGeneratingOptions` |
| `LoginCompletion` | `shouldIssueJwt_whenAssertionSucceeds`, `shouldUpdateSignCount_whenLoginCompletes`, `shouldUpdateLastUsedAt_whenLoginCompletes`, `shouldIncludePasskeyAuthMethod_whenIssuingJwt` |
| `SignCountValidation` | `shouldThrowPasskeyAuthenticationException_whenSignCountRegresses` |
| `ErrorPaths` | `shouldThrowPasskeyAuthenticationException_whenAssertionFails`, `shouldThrowPasskeyAuthenticationException_whenChallengeExpired`, `shouldThrowPasskeyAuthenticationException_whenCredentialNotFound` |

#### Step 11.2: PasskeyControllerTest

**File**: `application/src/test/java/com/akademiaplus/passkey/interfaceadapters/PasskeyControllerTest.java`

Standalone MockMvc (no Spring context):

| @Nested | Tests |
|---------|-------|
| `RegisterOptions` | `shouldReturn200WithOptions_whenRegistrationOptionsRequested` |
| `RegisterComplete` | `shouldReturn200_whenRegistrationSucceeds`, `shouldReturn400_whenRegistrationFails` |
| `LoginOptions` | `shouldReturn200WithOptions_whenLoginOptionsRequested` |
| `LoginComplete` | `shouldReturn200WithJwt_whenLoginSucceeds`, `shouldReturn401_whenLoginFails` |

#### Step 11.3: Compile + test

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn test -pl application -am -f platform-core-api/pom.xml
```

#### Step 11.4: Commit

```
test(application): add passkey authentication use case and controller tests

PasskeyAuthenticationUseCaseTest — covers login options, login
completion, JWT issuance, sign count update, and error paths.
PasskeyControllerTest — covers all 4 endpoints with success
and failure scenarios.
```

---

### Phase 12: Component Tests

#### Step 12.1: PasskeyComponentTest

**File**: `application/src/test/java/com/akademiaplus/usecases/PasskeyComponentTest.java`

Extends `AbstractIntegrationTest`. Full Spring context + Testcontainers MariaDB + embedded Redis.

| @Nested | Tests |
|---------|-------|
| `Registration` | `shouldReturn200WithCreationOptions_whenRegisterOptionsRequested`, `shouldStoreCredentialInDatabase_whenRegistrationCompletes` |
| `Authentication` | `shouldReturn200WithJwt_whenPasskeyLoginSucceeds`, `shouldUpdateSignCount_whenLoginCompletes` |
| `ChallengeExpiry` | `shouldReturn401_whenChallengeHasExpired` |
| `ErrorPaths` | `shouldReturn400_whenRegistrationResponseIsInvalid`, `shouldReturn401_whenAssertionResponseIsInvalid` |

**Note**: WebAuthn component tests require a mock authenticator. The tests must simulate the browser's `navigator.credentials.create()` and `navigator.credentials.get()` calls by directly constructing the `PublicKeyCredential` objects with valid attestation/assertion data. Use the `webauthn-server-core` test utilities or construct minimal valid CBOR-encoded responses.

#### Step 12.2: Compile + verify

```bash
mvn clean compile -pl application -am -DskipTests -f platform-core-api/pom.xml
mvn verify -pl application -am -f platform-core-api/pom.xml
```

#### Step 12.3: Commit

```
test(application): add passkey component test

PasskeyComponentTest — full Spring context + Testcontainers MariaDB
+ embedded Redis. Covers registration flow, authentication flow,
challenge expiry, and invalid response handling.
Simulates WebAuthn authenticator responses.
```

---

## 5. Key Design Decisions

### 5.1 Library Choice

| Option | Pros | Cons |
|--------|------|------|
| **java-webauthn-server (Yubico)** | Battle-tested, FIDO Alliance certified, active maintenance, handles all crypto | Additional dependency (~2MB) |
| Spring Security WebAuthn | Part of Spring ecosystem | Less mature, limited configuration options |
| Custom implementation | No dependencies | Extremely complex crypto, high risk of security vulnerabilities |

**Decision**: java-webauthn-server — the de facto standard for Java WebAuthn. Handles CBOR parsing, COSE key validation, attestation verification, and signature validation.

### 5.2 Challenge Storage

| Option | Pros | Cons |
|--------|------|------|
| **Redis with TTL** | Auto-expiry, atomic operations, shared across instances | Requires Redis infrastructure |
| In-memory `ConcurrentHashMap` | No external dependency | Lost on restart, not shared across instances |
| Database table | Persistent, tenant-scoped | Requires cleanup job, slower than Redis |
| HTTP session | Built-in expiry | Stateful — violates stateless JWT architecture |

**Decision**: Redis with 5-minute TTL. Consistent with the project's stateless architecture and reuses Redis infrastructure from jwt-refresh-token-rotation.

### 5.3 Credential ID Storage

| Option | Type | Pros | Cons |
|--------|------|------|------|
| **BLOB (raw bytes)** | `byte[]` | Direct WebAuthn format, no encoding overhead | Requires Base64URL encoding for JSON serialization |
| Base64URL VARCHAR | `String` | Human-readable, JSON-friendly | Encoding/decoding overhead on every operation |
| Hex VARCHAR | `String` | Debuggable | 2x storage compared to raw bytes |

**Decision**: BLOB — store raw bytes to avoid encoding overhead. The WebAuthn library operates on `ByteArray` objects, and storing raw bytes avoids unnecessary Base64URL encode/decode cycles on the hot path.

### 5.4 User Handle Strategy

| Option | Pros | Cons |
|--------|------|------|
| **Random 64-byte value** | Unlinkable to userId, privacy-preserving per spec | Requires mapping table or stored on credential |
| Encoded userId | Simple lookup | Violates WebAuthn privacy recommendations |
| UUID | Standard format | Still linkable if deterministic |

**Decision**: Random 64-byte value, stored on `PasskeyCredentialDataModel.userHandle`. Generated once per user at first registration, reused for subsequent registrations. Looked up via `findByUserHandle()` during authentication.

### 5.5 Module Boundary for Registration vs Authentication

| Component | Module | Rationale |
|-----------|--------|-----------|
| `PasskeyRegistrationUseCase` | security | Pure WebAuthn crypto — no cross-module calls needed. The authenticated user's info comes from the JWT context. |
| `PasskeyAuthenticationUseCase` | application | Cross-module orchestrator — needs to resolve userId to user entity for JWT subject, potentially across user-management module. |

**Decision**: Split per Hard Rule #14. Registration is self-contained in the security module (the user is already authenticated, and all needed info comes from the JWT). Authentication requires resolving the user identity from the credential, which may cross module boundaries.

---

## 6. Multi-Tenancy Considerations

1. **Tenant-scoped credentials**: `PasskeyCredentialDataModel` extends `TenantScoped` — all queries are automatically filtered by `tenantId` via Hibernate filters. A credential registered under tenant A cannot be used for authentication under tenant B.

2. **Challenge isolation**: Challenge keys in Redis include the tenant context implicitly via the challenge metadata (`ChallengeMetadata.tenantId`). The `consumeChallenge` method validates that the tenant ID matches.

3. **Relying Party per tenant (future)**: Currently, the RP ID is global (configured per environment). If different tenants need different RP IDs (e.g., custom domains), the `RelyingParty` bean would need to be request-scoped or resolved dynamically.

4. **Composite key**: The primary key is `(tenantId, passkeyCredentialId)`. The `credentialId` (from the authenticator) is indexed separately for WebAuthn assertion lookups.

5. **ID generation**: `passkeyCredentialId` uses the tenant-scoped sequential ID generator (`SequentialIDGenerator`) — consistent with other entities.

---

## 7. Future Extensibility

1. **Conditional UI**: Return whether the user has registered passkeys in the login options response, enabling the frontend to show "Sign in with passkey" only when applicable.
2. **Passkey management API**: CRUD endpoints for users to list, rename, and delete their registered passkeys.
3. **Cross-device authentication**: Support for hybrid transports (QR code-based cross-device WebAuthn).
4. **Attestation validation**: Validate authenticator attestation certificates to restrict registration to specific authenticator models (e.g., FIPS-certified keys only).
5. **Discoverable credentials (resident keys)**: Support usernameless login where the authenticator stores the user handle locally.
6. **MFA combination**: Use passkey as a second factor alongside password authentication.

---

## 8. Verification Checklist

- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full compilation passes
- [ ] `mvn test -pl security -am -f platform-core-api/pom.xml` — registration, challenge store, and repository tests green
- [ ] `mvn test -pl application -am -f platform-core-api/pom.xml` — authentication use case + controller tests green
- [ ] `mvn verify -pl application -am -f platform-core-api/pom.xml` — component tests green
- [ ] All new files have ElatusDev copyright header (2026)
- [ ] All public classes and methods have Javadoc
- [ ] All string literals extracted to `public static final` constants
- [ ] All tests use Given-When-Then, zero `any()` matchers
- [ ] `PasskeyRegistrationUseCase` in `security` module (no cross-module calls)
- [ ] `PasskeyAuthenticationUseCase` in `application` module (cross-module orchestrator)
- [ ] Domain records in `usecases/domain/` (Hard Rule #13) — if any
- [ ] No `new PasskeyCredentialDataModel()` — all via `applicationContext.getBean()`
- [ ] Composite key `(tenantId, passkeyCredentialId)` follows existing pattern
- [ ] Redis challenge store uses 5-minute TTL
- [ ] Login endpoints are `permitAll` in SecurityConfig
- [ ] Registration endpoints require authentication
- [ ] `java-webauthn-server` dependency added to security module only
- [ ] Sign count regression detected and throws exception

---

## 9. Critical Reminders

1. **WebAuthn is browser-only**: The `navigator.credentials` API is only available in secure contexts (HTTPS or localhost). All testing must account for this.

2. **Challenge single-use**: Each challenge MUST be consumed (deleted from Redis) after validation — never reusable. The `consumeChallenge` method uses Redis `GETDEL` (or `GET` + `DEL` atomically) to prevent replay attacks.

3. **Sign count validation**: The authenticator increments a counter on each use. If the server-stored counter is greater than the authenticator's counter, a cloned authenticator is detected. This MUST throw `PasskeyAuthenticationException` with `ERROR_SIGN_COUNT_REGRESSION`.

4. **User handle privacy**: The `userHandle` MUST be a random value — never derived from the userId or username. This prevents linking users across relying parties per the WebAuthn specification.

5. **RP ID matching**: The `rpId` in the configuration MUST match the domain the browser uses. Mismatched RP IDs cause the authenticator to reject the request silently.

6. **`applicationContext.getBean()`**: The `PasskeyCredentialDataModel` entity MUST be instantiated via `applicationContext.getBean()` — never `new PasskeyCredentialDataModel()`.

7. **BLOB indexing in MariaDB**: MariaDB BLOB columns cannot be fully indexed — use `credential_id(255)` prefix index. For exact-match lookups, the application layer handles byte comparison after index narrowing.

8. **No `any()` matchers**: All mock stubbing uses exact values or `ArgumentCaptor`.

9. **Conventional Commits**: No AI attribution in commit messages.

10. **Both apps**: This feature works for both AkademiaPlus and ElatusDev users. The RP ID is resolved per-environment, not per-app origin.
