/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a membership adult student cannot be found by its identifier.
 */
public class MembershipAdultStudentNotFoundException extends RuntimeException {

    /**
     * Constructs a new MembershipAdultStudentNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the membership adult student identifier
     */
    public MembershipAdultStudentNotFoundException(String msg) {
        super(msg);
    }
}
