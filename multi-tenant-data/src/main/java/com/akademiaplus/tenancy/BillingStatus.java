/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tenancy;

/**
 * Enumeration of billing cycle status values.
 * Represents the current state of billing and payment processing
 * for tenant subscription charges.
 */
public enum BillingStatus {
    /**
     * Billing cycle has been calculated but not yet processed.
     * Initial state when billing calculations are completed.
     */
    PENDING,

    /**
     * Bill has been generated and sent to the customer.
     * Indicates invoicing has been completed and payment is expected.
     */
    BILLED,

    /**
     * Payment has been received and processed successfully.
     * Final successful state of the billing cycle.
     */
    PAID,

    /**
     * Payment attempt failed or was rejected.
     * Requires follow-up action or retry attempts.
     */
    FAILED,

    /**
     * Billing cycle was cancelled due to account changes or adjustments.
     * Used for administrative corrections or subscription modifications.
     */
    CANCELLED
}