/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tenancy;

import com.akademiaplus.infra.persistence.model.SoftDeletable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Entity representing tenant branding configuration.
 * Stores visual identity settings such as school name, colors, logo, and fonts
 * that customize the application appearance per tenant.
 * <p>
 * Uses tenantId as the sole primary key since each tenant has exactly one
 * branding configuration (1:1 relationship with tenant). This entity does NOT
 * extend TenantScoped because it uses tenantId as its own PK with
 * {@code @GeneratedValue} absent — the tenantId is externally assigned.
 * <p>
 * The EntityIdAssigner will NOT process this entity because it has no secondary
 * {@code @Id} field alongside tenantId. The tenantId is set from TenantContextHolder
 * by the use case layer.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "tenant_branding")
@SQLDelete(sql = "UPDATE tenant_branding SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
public class TenantBrandingDataModel extends SoftDeletable {

    /**
     * Tenant identifier — also the primary key.
     * Each tenant has at most one branding configuration.
     */
    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /**
     * Display name of the school or institution.
     */
    @Column(name = "school_name", nullable = false, length = 200)
    private String schoolName;

    /**
     * Logo image URL for the tenant's branding.
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /**
     * Primary hex color for theming (e.g. "#FF5733").
     */
    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor;

    /**
     * Secondary hex color for theming.
     */
    @Column(name = "secondary_color", nullable = false, length = 7)
    private String secondaryColor;

    /**
     * CSS font family name for custom typography.
     */
    @Column(name = "font_family", length = 100)
    private String fontFamily;
}
