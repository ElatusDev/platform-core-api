/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.customerpayment;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Entity representing card payment information in the multi-tenant platform.
 * Stores secure payment card details including tokens and card type information
 * for processing credit and debit card transactions.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "card_payment_infos")
@SQLDelete(sql = "UPDATE card_payment_infos SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(CardPaymentInfoDataModel.CardPaymentInfoCompositeId.class)
public class CardPaymentInfoDataModel extends TenantScoped {

    /**
     * Unique identifier for the card payment info within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "card_payment_info_id")
    private Long cardPaymentInfoId;

    /**
     * External payment processor reference ID.
     * Links to payment gateway transactions for tracking and reconciliation.
     */
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    /**
     * Secure token representing the payment card.
     * Tokenized card information for PCI compliance and security.
     */
    @Lob
    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token;

    /**
     * Type of payment card used.
     * Examples: "visa", "mastercard", "amex", "discover"
     */
    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    /**
     * Composite primary key class for CardPaymentInfo entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CardPaymentInfoCompositeId implements Serializable {
        private Integer tenantId;
        private Long cardPaymentInfoId;
    }
}