package com.akademiaplus.infra.persistence.exceptions;

/**
 * Custom exception thrown when ID assignment fails
 */
public class IdAssignmentException extends RuntimeException {

    public IdAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdAssignmentException(String message) {
        super(message);
    }
}