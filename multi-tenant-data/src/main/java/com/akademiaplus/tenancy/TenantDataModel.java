/*
 * Copyright (c) 2025 ElatusDev
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
 * Entity representing a tenant organization in the multi-tenant platform.
 * This is the root entity that defines tenant boundaries and organizational information.
 * <p>
 * Note: This entity does not extend TenantScoped since it IS the tenant definition.
 * All other entities in the system will reference this through their tenant_id.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "tenants")
@SQLDelete(sql = "UPDATE tenants SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
public class TenantDataModel extends SoftDeletable {

    /**
     * Unique identifier for the tenant organization.
     * This ID is used as tenant_id in all other tenant-scoped entities.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * Display name of the organization.
     * Used in user interfaces and communications.
     */
    @Column(name = "organization_name", nullable = false, length = 200)
    private String organizationName;

    /**
     * Legal name of the organization.
     * Used for contracts, billing, and legal documentation.
     */
    @Column(name = "legal_name", length = 200)
    private String legalName;

    /**
     * Organization's website URL.
     * Used for branding and external references.
     */
    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    /**
     * Primary contact email for the organization.
     * Used for administrative communications and support.
     */
    @Column(name = "email", nullable = false, length = 200)
    private String email;

    /**
     * Physical address of the organization.
     * Used for billing, legal, and compliance purposes.
     */
    @Column(name = "address", nullable = false, length = 200)
    private String address;

    /**
     * Primary phone number for the organization.
     * Used for direct contact and support communications.
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Landline phone number for the organization.
     * Alternative contact method for business communications.
     */
    @Column(name = "landline", length = 20)
    private String landline;

    /**
     * Detailed description of the organization.
     * Used for profile information and marketing purposes.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Tax identification number for the organization.
     * Used for billing, invoicing, and tax compliance.
     */
    @Column(name = "tax_id", length = 50)
    private String taxId;
}