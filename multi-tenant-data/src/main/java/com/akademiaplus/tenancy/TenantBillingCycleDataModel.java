/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tenancy;

import com.akademiaplus.infra.TenantScoped;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

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
@IdClass(TenantBillingCycleDataModel.TenantBillingCycleCompositeId.class)
public class TenantBillingCycleDataModel extends TenantScoped {

    /**
     * Unique identifier for the billing cycle within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "billing_cycle_id")
    private Integer billingCycleId;

    /**
     * Year and month for this billing cycle.
     * Represents the service period being billed.
     * <p>
     * Format: YYYY-MM (e.g., 2025-01 for January 2025)
     */
    @Column(name = "billing_month", nullable = false)
    private YearMonth billingMonth;

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
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TenantBillingCycleCompositeId implements Serializable {
        private Integer tenantId;
        private Integer tenantBillingCycleId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TenantBillingCycleCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && tenantBillingCycleId.equals(that.tenantBillingCycleId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, tenantBillingCycleId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{tenantId=" + tenantId + ", tenantBillingCycleId=" + tenantBillingCycleId + "}";
        }
    }
}