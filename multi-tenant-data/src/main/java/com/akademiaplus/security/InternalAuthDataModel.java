/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.security;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import com.akademiaplus.utilities.security.StringEncryptor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Entity representing internal authentication credentials for platform users.
 * Stores encrypted authentication data for employees and collaborators who
 * require internal system access.
 * <p>
 * All sensitive authentication fields are encrypted at rest for security compliance.
 * Username hashes are used for indexing and uniqueness checks without decryption.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "internal_auths")
@SQLDelete(sql = "UPDATE internal_auths SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(InternalAuthDataModel.InternalAuthCompositeId.class)
public class InternalAuthDataModel extends TenantScoped {

    /**
     * Unique identifier for the internal authentication within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "internal_auth_id")
    private Long internalAuthId;

    /**
     * Encrypted username for internal authentication.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Used for login identification along with password verification.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_username", nullable = false, length = 500)
    private String username;

    /**
     * Encrypted password for internal authentication.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Should be properly hashed before encryption for security best practices.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_password", nullable = false, length = 500)
    private String password;

    /**
     * Encrypted role information for authorization.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Defines access permissions and system capabilities for the user.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_role", nullable = false, length = 500)
    private String role;

    /**
     * Hash of the username for indexing and uniqueness checks.
     * Allows searching and ensuring uniqueness without decrypting the actual username.
     * <p>
     * Used in database constraints and login lookups for performance.
     */
    @Column(name = "username_hash", length = 64, nullable = false)
    private String usernameHash;

    /**
     * Composite primary key class for InternalAuth entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InternalAuthCompositeId implements Serializable {
        private Integer tenantId;
        private Long internalAuthId;
    }
}