package com.akademiaplus.utilities.exceptions.idgeneration;

/**
 * Custom exceptions for ID generation operations
 */
public class IDGenerationException extends RuntimeException {

    public IDGenerationException(String message) {
        super(message);
    }

    public IDGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}