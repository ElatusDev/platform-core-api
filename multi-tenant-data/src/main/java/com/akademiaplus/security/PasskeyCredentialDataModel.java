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
import java.time.LocalDateTime;

/**
 * Entity representing a WebAuthn/FIDO2 passkey credential.
 *
 * <p>Stores the public key and metadata for a registered passkey authenticator.
 * Each user can have multiple passkey credentials (e.g., fingerprint on phone,
 * YubiKey, Touch ID on laptop).
 *
 * <p>The composite primary key is {@code (tenantId, passkeyCredentialId)}.
 * The {@code credentialId} (from the authenticator) is indexed within
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
     * The user ID that owns this credential.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * The credential ID assigned by the authenticator (raw bytes).
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
     * Timestamp of the last successful authentication with this credential.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Human-readable name for this credential (e.g., "MacBook Touch ID", "YubiKey 5").
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * The user handle (WebAuthn user.id) — random bytes.
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

        /** Tenant identifier. */
        protected Long tenantId;

        /** Passkey credential identifier. */
        protected Long passkeyCredentialId;
    }
}
