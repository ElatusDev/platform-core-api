package com.akademiaplus.infra.exceptions;

public class InvalidTenantException extends RuntimeException {
    public InvalidTenantException(String msg) {
        super(msg);
    }
}
