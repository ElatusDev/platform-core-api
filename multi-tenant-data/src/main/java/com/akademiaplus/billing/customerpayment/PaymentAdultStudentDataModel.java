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
import lombok.*;
import org.hibernate.annotations.SQLDelete;
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
@SQLDelete(sql = "UPDATE payment_adult_students SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId.class)
public class PaymentAdultStudentDataModel extends BasePayment {

    /**
     * Unique identifier for the adult student payment within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
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
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentAdultStudentCompositeId implements Serializable {
        private Integer tenantId;
        private Integer paymentAdultStudentId;
    }
}