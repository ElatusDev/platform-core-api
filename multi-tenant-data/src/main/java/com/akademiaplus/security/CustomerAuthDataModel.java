/*
 * Copyright (c) 2025 ElatusDev
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

/**
 * Entity representing customer authentication credentials for external platform users.
 * Stores OAuth tokens and provider information for adult students, tutors, and
 * minor students who authenticate through external providers.
 * <p>
 * Customer authentication supports various OAuth providers such as Google, Facebook,
 * Apple, and other social login services for user convenience and security.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "customer_auths")
@SQLDelete(sql = "UPDATE customer_auths SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND customer_auth_id = ?")
@IdClass(CustomerAuthDataModel.CustomerAuthCompositeId.class)
public class CustomerAuthDataModel extends TenantScoped {

    /**
     * Unique identifier for the customer authentication within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "customer_auth_id")
    private Long customerAuthId;

    /**
     * OAuth provider name for external authentication.
     * Identifies which service is used for user authentication.
     * <p>
     * Examples: "google", "facebook", "apple", "microsoft", "github"
     */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /**
     * OAuth token for authenticated access to external services.
     * Contains the authentication token received from the OAuth provider.
     * <p>
     * Stored as TEXT to accommodate various token formats and lengths.
     */
    @Lob
    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token;

    /**
     * Composite primary key class for CustomerAuth entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerAuthCompositeId implements Serializable {
        protected Long tenantId;
        protected Long customerAuthId;
    }
}