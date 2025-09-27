/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus;

import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Provides access to the current tenant ID for a given request.
 * This class is designed to be immutable once the tenant ID is set.
 */

@Getter
@Component
@Scope("request")
public final class TenantContextHolder {

    private final Integer tenantId;

    /**
     * Private constructor to ensure that a tenant context can only be created
     * internally by a trusted factory or provider.
     *
     * @param tenantId The tenant ID for the current request. Must not be null.
     */
    private TenantContextHolder(Integer tenantId) {
        Objects.requireNonNull(tenantId, "Tenant ID cannot be null.");
        this.tenantId = tenantId;
    }
}