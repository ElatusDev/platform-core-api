/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a store product cannot be found by its identifier.
 */
public class StoreProductNotFoundException extends RuntimeException {

    /**
     * Constructs a new StoreProductNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the store product identifier
     */
    public StoreProductNotFoundException(String msg) {
        super(msg);
    }
}
