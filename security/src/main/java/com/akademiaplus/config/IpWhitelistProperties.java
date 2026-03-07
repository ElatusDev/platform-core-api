/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the IP whitelist filter.
 *
 * <p>Binds to {@code security.akademia.allowed-cidrs} in application.properties.
 * Provides the list of CIDR ranges that are permitted to access the
 * AkademiaPlus web app endpoints.
 *
 * <p>An empty list results in fail-secure behavior: all AkademiaPlus
 * requests are blocked.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "security.akademia")
public class IpWhitelistProperties {

    /** The list of CIDR ranges allowed for AkademiaPlus requests. */
    private List<String> allowedCidrs = List.of();

    /**
     * Returns the list of allowed CIDR ranges.
     *
     * @return the allowed CIDR ranges, never null
     */
    public List<String> getAllowedCidrs() {
        return allowedCidrs;
    }

    /**
     * Sets the list of allowed CIDR ranges.
     *
     * @param allowedCidrs the CIDR ranges to allow
     */
    public void setAllowedCidrs(List<String> allowedCidrs) {
        this.allowedCidrs = allowedCidrs;
    }
}
