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
 * Entity representing a magic link authentication token.
 *
 * <p>Stores a SHA-256 hash of the random token sent to the user's email.
 * Tokens are single-use (tracked via {@code usedAt}) and expire after a
 * configurable duration (tracked via {@code expiresAt}).</p>
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
     * Email address the magic link was sent to.
     */
    @Column(name = "email", nullable = false, length = 500)
    private String email;

    /**
     * SHA-256 hash of the random token. The raw token is never stored.
     */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /**
     * Timestamp when the token expires and can no longer be used.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Timestamp when the token was consumed. Null if not yet used.
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * Composite primary key class for MagicLinkToken entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MagicLinkTokenCompositeId implements Serializable {

        /** Tenant identifier. */
        protected Long tenantId;

        /** Magic link token identifier. */
        protected Long magicLinkTokenId;
    }
}
