/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.payroll;

import com.akademiaplus.infra.TenantScoped;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Entity representing compensation structures in the multi-tenant platform.
 * Defines various types of compensation packages that can be assigned to
 * collaborators, including salary, hourly rates, commission structures, etc.
 * <p>
 * Each compensation structure is uniquely identified within a tenant and
 * can be associated with multiple collaborators who share the same
 * compensation arrangement.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "compensation")
@IdClass(CompensationDataModel.CompensationCompositeId.class)
public class CompensationDataModel extends TenantScoped {

    /**
     * Unique identifier for the compensation structure within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "compensation_id")
    private Integer compensationId;

    /**
     * Type of compensation (e.g., SALARY, HOURLY, COMMISSION, BONUS).
     * Determines how the compensation amount should be interpreted and applied.
     */
    @Column(name = "compensation_type", nullable = false, length = 50)
    private String compensationType;

    /**
     * Monetary amount for this compensation structure.
     * Interpretation depends on compensation type (annual for salary, per hour for hourly, etc.).
     */
    @Column(name = "amount", nullable = false)
    private Double amount;

    /**
     * List of collaborators who are assigned this compensation structure.
     * Uses tenant-aware mapping to maintain data isolation.
     */
    @OneToMany(mappedBy = "compensation", fetch = FetchType.LAZY)
    private List<CollaboratorDataModel> collaborators;

    /**
     * Composite primary key class for Compensation entity.
     * Combines tenant ID and compensation ID for uniqueness.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompensationCompositeId {

        private Integer tenantId;
        private Integer compensationId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CompensationCompositeId that)) return false;
            return tenantId.equals(that.tenantId) &&
                    compensationId.equals(that.compensationId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, compensationId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() +
                    "{tenantId=" + tenantId +
                    ", compensationId=" + compensationId + "}";
        }
    }
}