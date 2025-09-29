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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Table(name = "membership_tutor")
@IdClass(MembershipTutorDataModel.MembershipTutorCompositeId.class)
public class MembershipTutorDataModel extends MembershipAssociationBase {

    /**
     * Unique identifier for the membership-tutor association within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_tutor_id")
    private Integer membershipTutorId;

    /**
     * Reference to the tutor associated with this membership.
     * Part of the composite primary key and uses tenant-aware join.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "tutor_id", referencedColumnName = "tutor_id")
    private TutorDataModel tutor;

    /**
     * Composite primary key class for MembershipTutor entity.
     * Combines tenant ID and membership-tutor ID for uniqueness.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MembershipTutorCompositeId {

        private Integer tenantId;
        private Integer membershipTutorId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MembershipTutorCompositeId that)) return false;
            return tenantId.equals(that.tenantId) &&
                    membershipTutorId.equals(that.membershipTutorId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, membershipTutorId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() +
                    "{tenantId=" + tenantId +
                    ", membershipTutorId=" + membershipTutorId + "}";
        }
    }
}