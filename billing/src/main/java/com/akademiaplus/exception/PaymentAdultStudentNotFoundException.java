/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a payment adult student cannot be found by its identifier.
 */
public class PaymentAdultStudentNotFoundException extends RuntimeException {

    /**
     * Constructs a new PaymentAdultStudentNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the payment adult student identifier
     */
    public PaymentAdultStudentNotFoundException(String msg) {
        super(msg);
    }
}
