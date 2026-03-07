/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.interfaceadapters.config;

import com.akademiaplus.tokenbinding.usecases.domain.TokenBindingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for token binding anti-hijack feature.
 *
 * <p>Bound to {@code security.elatus.token-binding.*} in application.properties.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "security.elatus.token-binding")
public class TokenBindingProperties {

    /** The strictness mode for token binding verification. */
    private TokenBindingMode mode = TokenBindingMode.STRICT;

    /**
     * Returns the token binding mode.
     *
     * @return the current mode
     */
    public TokenBindingMode getMode() {
        return mode;
    }

    /**
     * Sets the token binding mode.
     *
     * @param mode the mode to set
     */
    public void setMode(TokenBindingMode mode) {
        this.mode = mode;
    }
}
