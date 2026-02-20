/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a membership cannot be found by its identifier.
 */
public class MembershipNotFoundException extends RuntimeException {

    /**
     * Constructs a new MembershipNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the membership identifier
     */
    public MembershipNotFoundException(String msg) {
        super(msg);
    }
}
