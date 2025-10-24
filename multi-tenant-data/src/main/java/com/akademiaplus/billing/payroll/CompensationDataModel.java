/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.payroll;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
@Table(name = "compensations")
@SQLDelete(sql = "UPDATE compensations SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(CompensationDataModel.CompensationCompositeId.class)
public class CompensationDataModel extends TenantScoped {

    /**
     * Unique identifier for the compensation structure within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @Column(name = "compensation_id")
    private Long compensationId;

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
    private BigDecimal amount;

    /**
     * List of collaborators who are assigned this compensation structure.
     * Uses tenant-aware mapping to maintain data isolation.
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "compensation_collaborators",
            joinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id"),
                    @JoinColumn(name = "compensation_id", referencedColumnName = "compensation_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "collaborator_id", referencedColumnName = "collaborator_id")
            }
    )
    private List<CollaboratorDataModel> collaborators;

    /**
     * Composite primary key class for Compensation entity.
     * Combines tenant ID and compensation ID for uniqueness.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompensationCompositeId {
        private Integer tenantId;
        private Long compensationId;
    }
}