/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

public class EmployeeDeletionNotAllowedException extends RuntimeException {
    public EmployeeDeletionNotAllowedException(Exception ex) {
        super(ex);
    }
}
