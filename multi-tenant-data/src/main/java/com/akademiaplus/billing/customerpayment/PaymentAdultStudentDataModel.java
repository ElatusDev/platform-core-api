/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.customerpayment;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Entity representing payments made by adult students in the multi-tenant platform.
 * Extends BasePayment to inherit common payment attributes and adds
 * specific relationship to adult student memberships.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "payment_adult_students")
@IdClass(PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId.class)
public class PaymentAdultStudentDataModel extends BasePayment {

    /**
     * Unique identifier for the adult student payment within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_adult_student_id")
    private Integer paymentAdultStudentId;

    /**
     * Reference to the adult student membership this payment is for.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "membership_adult_student_id", referencedColumnName = "membership_adult_student_id")
    private MembershipAdultStudentDataModel membershipAdultStudent;

    /**
     * Composite primary key class for PaymentAdultStudent entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentAdultStudentCompositeId implements Serializable {
        private Integer tenantId;
        private Integer paymentAdultStudentId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PaymentAdultStudentCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && paymentAdultStudentId.equals(that.paymentAdultStudentId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, paymentAdultStudentId);
        }

        @Override
        public String toString() {
            return "PaymentAdultStudentCompositeId{tenantId=" + tenantId + ", paymentAdultStudentId=" + paymentAdultStudentId + "}";
        }
    }
}