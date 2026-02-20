/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a notification cannot be found by its identifier.
 */
public class NotificationNotFoundException extends RuntimeException {

    /**
     * Constructs a new NotificationNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the notification identifier
     */
    public NotificationNotFoundException(String msg) {
        super(msg);
    }
}
