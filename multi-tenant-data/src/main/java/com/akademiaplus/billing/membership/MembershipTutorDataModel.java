/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.membership;

import com.akademiaplus.users.customer.TutorDataModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Entity representing the association between a membership and a tutor
 * in the multi-tenant platform. This junction table maintains the many-to-many
 * relationship while ensuring proper tenant isolation and inheriting common
 * membership association properties like dates and course references.
 * <p>
 * Each association is uniquely identified by the combination of tutor ID
 * and tenant ID, with membership information inherited from the base class.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "membership_tutors")
@SQLDelete(sql = "UPDATE membership_tutors SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(MembershipTutorDataModel.MembershipTutorCompositeId.class)
public class MembershipTutorDataModel extends MembershipAssociationBase {

    /**
     * Unique identifier for the membership-tutor association within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @Column(name = "membership_tutor_id")
    private Integer membershipTutorId;

    /**
     * Reference to the tutor associated with this membership.
     * Part of the composite primary key and uses tenant-aware join.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "tutor_id", referencedColumnName = "tutor_id", insertable=false, updatable=false)
    private TutorDataModel tutor;

    /**
     * Composite primary key class for MembershipTutor entity.
     * Combines tenant ID and membership-tutor ID for uniqueness.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MembershipTutorCompositeId {
        private Integer tenantId;
        private Integer membershipTutorId;
    }
}