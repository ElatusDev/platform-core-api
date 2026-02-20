/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a store transaction cannot be found by its identifier.
 */
public class StoreTransactionNotFoundException extends RuntimeException {

    /**
     * Constructs a new StoreTransactionNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the store transaction identifier
     */
    public StoreTransactionNotFoundException(String msg) {
        super(msg);
    }
}
