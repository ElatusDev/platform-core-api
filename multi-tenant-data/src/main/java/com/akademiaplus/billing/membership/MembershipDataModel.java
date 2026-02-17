/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.membership;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import com.akademiaplus.courses.program.CourseDataModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Entity representing membership types in the multi-tenant platform.
 * Defines membership plans with associated fees, descriptions, and
 * course access permissions for different user types.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "memberships")
@SQLDelete(sql = "UPDATE memberships SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(MembershipDataModel.MembershipCompositeId.class)
public class MembershipDataModel extends TenantScoped {

    /**
     * Unique identifier for the membership within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "membership_id")
    private Long membershipId;

    /**
     * Type classification of the membership.
     * Defines the category or level of membership.
     */
    @Column(name = "membership_type", nullable = false, length = 50)
    private String membershipType;

    /**
     * Fee charged for this membership type.
     * Using BigDecimal for precise financial calculations.
     */
    @Column(name = "fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    /**
     * Description of the membership benefits and features.
     * Used for marketing and enrollment information.
     */
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /**
     * List of courses included in this membership.
     * Many-to-many relationship managed through membership_courses table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "membership_courses",
            joinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false),
                    @JoinColumn(name = "membership_id", referencedColumnName = "membership_id", insertable=false, updatable=false)
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false),
                    @JoinColumn(name = "course_id", referencedColumnName = "course_id", insertable=false, updatable=false)
            }
    )
    private List<CourseDataModel> courses;

    /**
     * Composite primary key class for Membership entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MembershipCompositeId implements Serializable {
        private Long tenantId;
        private Long membershipId;
    }
}