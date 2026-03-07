/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.security;

import com.akademiaplus.infra.persistence.model.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Instant;

/**
 * Entity representing a refresh token used for JWT token rotation.
 *
 * <p>Refresh tokens are grouped by {@code familyId} — a UUID that chains
 * all tokens in a rotation sequence. When a token is consumed (rotated),
 * {@code revokedAt} is set and {@code replacedByTokenHash} points to the
 * successor. If a consumed token is replayed, the entire family is revoked
 * (reuse detection).</p>
 *
 * <p>This entity uses a composite key ({@code tenantId} + {@code refreshTokenId})
 * but is <strong>not</strong> filtered by Hibernate tenant filter — refresh
 * tokens are looked up by {@code tokenHash} (unique, cross-tenant).</p>
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
@Table(name = "refresh_tokens")
@IdClass(RefreshTokenDataModel.RefreshTokenCompositeId.class)
public class RefreshTokenDataModel extends Auditable {

    /** Column name constant for tenant_id. */
    public static final String COLUMN_TENANT_ID = "tenant_id";

    /** Column name constant for refresh_token_id. */
    public static final String COLUMN_REFRESH_TOKEN_ID = "refresh_token_id";

    /** Column name constant for token_hash. */
    public static final String COLUMN_TOKEN_HASH = "token_hash";

    /** Column name constant for family_id. */
    public static final String COLUMN_FAMILY_ID = "family_id";

    /** Column name constant for user_id. */
    public static final String COLUMN_USER_ID = "user_id";

    /** Column name constant for username. */
    public static final String COLUMN_USERNAME = "username";

    /** Column name constant for expires_at. */
    public static final String COLUMN_EXPIRES_AT = "expires_at";

    /** Column name constant for revoked_at. */
    public static final String COLUMN_REVOKED_AT = "revoked_at";

    /** Column name constant for replaced_by_token_hash. */
    public static final String COLUMN_REPLACED_BY = "replaced_by_token_hash";

    /** Tenant identifier — part of composite primary key. */
    @Id
    @Column(name = COLUMN_TENANT_ID)
    private Long tenantId;

    /** Unique identifier for the refresh token within the tenant. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = COLUMN_REFRESH_TOKEN_ID)
    private Long refreshTokenId;

    /** SHA-256 hash of the raw refresh token — unique across all tenants. */
    @Column(name = COLUMN_TOKEN_HASH, nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** UUID grouping all tokens in a rotation chain. */
    @Column(name = COLUMN_FAMILY_ID, nullable = false, length = 36)
    private String familyId;

    /** Internal user ID that owns this token. */
    @Column(name = COLUMN_USER_ID, nullable = false)
    private Long userId;

    /** Username for quick lookup during revocation. */
    @Column(name = COLUMN_USERNAME, nullable = false)
    private String username;

    /** Expiration timestamp of this refresh token. */
    @Column(name = COLUMN_EXPIRES_AT, nullable = false)
    private Instant expiresAt;

    /** Timestamp when this token was consumed or revoked. Null means active. */
    @Column(name = COLUMN_REVOKED_AT)
    private Instant revokedAt;

    /** Hash of the successor token that replaced this one during rotation. */
    @Column(name = COLUMN_REPLACED_BY, length = 64)
    private String replacedByTokenHash;

    /**
     * Composite primary key for refresh tokens.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RefreshTokenCompositeId implements Serializable {

        /** Tenant identifier. */
        private Long tenantId;

        /** Refresh token identifier. */
        private Long refreshTokenId;
    }
}
