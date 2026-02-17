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

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a billing cycle for a tenant organization.
 * Tracks monthly billing calculations, user counts, and payment status
 * for subscription-based tenant charges.
 * <p>
 * Each billing cycle represents one month of service usage and
 * corresponding charges based on active user count and subscription rates.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "tenant_billing_cycles")
@SQLDelete(sql = "UPDATE tenant_billing_cycles SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(TenantBillingCycleDataModel.TenantBillingCycleCompositeId.class)
public class TenantBillingCycleDataModel extends TenantScoped {

    /**
     * Unique identifier for the billing cycle within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "tenant_billing_cycle_id")
    private Long tenantBillingCycleId;

    /**
     * Year and month for this billing cycle.
     * Represents the service period being billed.
     * <p>
     * Format: YYYY-MM (e.g., 2025-01 for January 2025)
     */
    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    /**
     * Date when billing calculations were performed.
     * Used for audit trail and billing reconciliation.
     */
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    /**
     * Number of active users during this billing period.
     * Used for calculating usage-based charges.
     */
    @Column(name = "user_count", nullable = false)
    private Integer userCount;

    /**
     * Total amount charged for this billing cycle.
     * Calculated based on user count and subscription rates.
     * <p>
     * Precision: 12 digits total, 2 decimal places for currency accuracy.
     */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Current status of the billing cycle.
     * Tracks progression from calculation through payment.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", length = 20)
    private BillingStatus billingStatus = BillingStatus.PENDING;

    /**
     * Timestamp when the bill was generated and sent.
     * Null if billing has not yet been processed.
     */
    @Column(name = "billed_at")
    private LocalDateTime billedAt;

    /**
     * Timestamp when payment was received for this cycle.
     * Null if payment has not yet been received.
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Additional notes or comments about this billing cycle.
     * Used for special circumstances, adjustments, or administrative notes.
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Composite primary key class for TenantBillingCycle entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TenantBillingCycleCompositeId implements Serializable {
        private Long tenantId;
        private Long tenantBillingCycleId;
    }
}