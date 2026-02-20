/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.membership;

import com.akademiaplus.users.customer.AdultStudentDataModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Entity representing the association between a membership and an adult student
 * in the multi-tenant platform. This junction table maintains the many-to-many
 * relationship while ensuring proper tenant isolation and inheriting common
 * membership association properties like dates and course references.
 * <p>
 * Each association is uniquely identified by the combination of adult student ID
 * and tenant ID, with membership information inherited from the base class.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "membership_adult_students")
@SQLDelete(sql = "UPDATE membership_adult_students SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND membership_adult_student_id = ?")
@IdClass(MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId.class)
public class MembershipAdultStudentDataModel extends MembershipAssociationBase {

    /**
     * Unique identifier for the membership-adult student association within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @Column(name = "membership_adult_student_id")
    private Long membershipAdultStudentId;

    /**
     * Foreign key to the adult student associated with this membership.
     * Writable column used to persist the FK value during INSERT.
     */
    @Column(name = "adult_student_id")
    private Long adultStudentId;

    /**
     * Reference to the adult student associated with this membership.
     * Part of the composite primary key and uses tenant-aware join.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "adult_student_id", referencedColumnName = "adult_student_id", insertable=false, updatable=false)
    private AdultStudentDataModel adultStudent;

    /**
     * Composite primary key class for MembershipAdultStudent entity.
     * Combines tenant ID and membership-adult student ID for uniqueness.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MembershipAdultStudentCompositeId {
        private Long tenantId;
        private Long membershipAdultStudentId;
    }
}