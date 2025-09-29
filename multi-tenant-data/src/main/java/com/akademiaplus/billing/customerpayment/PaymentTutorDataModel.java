/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.customerpayment;

import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Entity representing payments made by tutors in the multi-tenant platform.
 * Extends BasePayment to inherit common payment attributes and adds
 * specific relationship to tutor memberships.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "payment_tutors")
@IdClass(PaymentTutorDataModel.PaymentTutorCompositeId.class)
public class PaymentTutorDataModel extends BasePayment {

    /**
     * Unique identifier for the tutor payment within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_tutor_id")
    private Integer paymentTutorId;

    /**
     * Reference to the tutor membership this payment is for.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "membership_tutor_id", referencedColumnName = "membership_tutor_id")
    private MembershipTutorDataModel membershipTutor;

    /**
     * Composite primary key class for PaymentTutor entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentTutorCompositeId implements Serializable {
        private Integer tenantId;
        private Integer paymentTutorId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PaymentTutorCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && paymentTutorId.equals(that.paymentTutorId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, paymentTutorId);
        }

        @Override
        public String toString() {
            return "PaymentTutorCompositeId{tenantId=" + tenantId + ", paymentTutorId=" + paymentTutorId + "}";
        }
    }
}