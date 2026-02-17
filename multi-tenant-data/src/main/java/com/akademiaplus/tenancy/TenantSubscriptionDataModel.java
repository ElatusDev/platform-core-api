/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tenancy;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing tenant subscription plans in the multi-tenant platform.
 * Defines the subscription configuration for each tenant including billing
 * details, user limits, and pricing structures for the educational services.
 * <p>
 * Each subscription is uniquely identified within a tenant and contains
 * essential billing information such as rates, user limits, and billing cycles.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "tenant_subscriptions")
@SQLDelete(sql = "UPDATE tenant_subscriptions SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(TenantSubscriptionDataModel.TenantSubscriptionCompositeId.class)
public class TenantSubscriptionDataModel extends TenantScoped {

    /**
     * Unique identifier for the tenant subscription within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @Column(name = "tenant_subscription_id")
    private Long tenantSubscriptionId;

    /**
     * Type of subscription plan (e.g., BASIC, PREMIUM, ENTERPRISE, TRIAL).
     * Determines the feature set and pricing model available to the tenant.
     */
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    /**
     * Maximum number of users allowed under this subscription plan.
     * Null indicates unlimited users for the subscription tier.
     */
    @Column(name = "max_users")
    private Integer maxUsers;

    /**
     * Date when the subscription billing cycle occurs.
     * Used for automated billing and subscription renewal processes.
     */
    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    /**
     * Rate charged per student enrolled in the platform.
     * Used for calculating monthly or periodic billing amounts based on actual usage.
     */
    @Column(name = "rate_per_student", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerStudent;

    /**
     * Composite primary key class for TenantSubscription entity.
     * Combines tenant ID and tenant subscription ID for uniqueness across tenants.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TenantSubscriptionCompositeId {
        private Long tenantId;
        private Long tenantSubscriptionId;
    }
}