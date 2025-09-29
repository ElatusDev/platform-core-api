/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.customer;

import com.akademiaplus.users.base.AbstractUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Entity representing a minor student in the multi-tenant platform.
 * Extends AbstractUser to inherit common user functionality and adds
 * minor student-specific attributes such as tutor relationship and customer authentication.
 * <p>
 * Minor students are external users under the age of majority who enroll in courses
 * through a responsible tutor. They require tutor oversight for enrollments and
 * use customer authentication with limited access rights.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "minor_students")
@SQLDelete(sql = "UPDATE minor_students SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(MinorStudentDataModel.MinorStudentCompositeId.class)
public class MinorStudentDataModel extends AbstractUser {

    /**
     * Unique identifier for the minor student within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "minor_student_id")
    private Integer minorStudentId;

    /**
     * Reference to the tutor responsible for this minor student.
     * Uses composite foreign key to maintain tenant isolation.
     * <p>
     * Required relationship as minors cannot enroll or manage accounts independently.
     * The tutor acts as guardian for educational decisions and account management.
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "tutor_id", referencedColumnName = "tutor_id")
    private TutorDataModel tutor;

    /**
     * Reference to the student's customer authentication credentials.
     * Uses composite foreign key to maintain tenant isolation.
     * <p>
     * Minor students authenticate through external providers but with limited access
     * and tutor oversight for account activities.
     */
    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "customer_auth_id", referencedColumnName = "customer_auth_id")
    private CustomerAuthDataModel customerAuth;

    /**
     * Composite primary key class for MinorStudent entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MinorStudentCompositeId implements Serializable {
        protected Integer tenantId;
        protected Integer minorStudentId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MinorStudentCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && minorStudentId.equals(that.minorStudentId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, minorStudentId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{tenantId=" + tenantId + ", minorStudentId=" + minorStudentId + "}";
        }
    }
}