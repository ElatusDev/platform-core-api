/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a payment tutor cannot be found by its identifier.
 */
public class PaymentTutorNotFoundException extends RuntimeException {

    /**
     * Constructs a new PaymentTutorNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the payment tutor identifier
     */
    public PaymentTutorNotFoundException(String msg) {
        super(msg);
    }
}
