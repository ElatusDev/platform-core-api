/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a membership tutor cannot be found by its identifier.
 */
public class MembershipTutorNotFoundException extends RuntimeException {

    /**
     * Constructs a new MembershipTutorNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the membership tutor identifier
     */
    public MembershipTutorNotFoundException(String msg) {
        super(msg);
    }
}
