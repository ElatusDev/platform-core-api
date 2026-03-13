/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billingcycle.domain.exception;

import com.akademiaplus.tenancy.BillingStatus;

/**
 * Thrown when a billing cycle transition violates the state machine.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class InvalidBillingTransitionException extends RuntimeException {

    /** Error message for invalid billing status transition. */
    public static final String ERROR_MESSAGE = "Invalid billing status transition";

    /**
     * Creates a new exception with the from and to statuses appended.
     *
     * @param from the current billing status
     * @param to   the attempted target status
     */
    public InvalidBillingTransitionException(BillingStatus from, BillingStatus to) {
        super(ERROR_MESSAGE + ": " + from + " → " + to);
    }
}
