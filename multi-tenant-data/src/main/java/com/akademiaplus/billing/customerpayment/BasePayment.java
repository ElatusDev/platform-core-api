/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.customerpayment;

import com.akademiaplus.infra.TenantScoped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abstract base class for all payment types in the multi-tenant platform.
 * Provides common payment attributes including date, amount, and payment method
 * that all concrete payment implementations inherit.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BasePayment extends TenantScoped {

    /**
     * Date when the payment was made.
     * Used for financial reporting and payment history tracking.
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * Amount of the payment.
     * Using BigDecimal for precise financial calculations.
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Method used for payment processing.
     * Examples: "credit_card", "bank_transfer", "cash", "paypal"
     */
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;
}