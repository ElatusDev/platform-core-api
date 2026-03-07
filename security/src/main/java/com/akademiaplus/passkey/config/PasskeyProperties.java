/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for WebAuthn/FIDO2 passkey authentication.
 *
 * <p>Defines the Relying Party identity, allowed origins, and challenge
 * time-to-live for the WebAuthn registration and authentication flows.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "security.passkey")
public class PasskeyProperties {

    /** Configuration properties prefix. */
    public static final String PREFIX = "security.passkey";

    /** Default challenge TTL in seconds (5 minutes). */
    public static final long DEFAULT_CHALLENGE_TTL_SECONDS = 300L;

    /** Relying Party ID — typically the domain (e.g., "akademiaplus.com"). */
    private String rpId;

    /** Relying Party display name shown to users during registration. */
    private String rpName;

    /** Allowed origins for WebAuthn operations. */
    private List<String> allowedOrigins = new ArrayList<>();

    /** Challenge time-to-live in seconds (default: 300 = 5 minutes). */
    private Long challengeTtlSeconds = DEFAULT_CHALLENGE_TTL_SECONDS;

    /**
     * Returns the Relying Party ID.
     *
     * @return the RP ID
     */
    public String getRpId() {
        return rpId;
    }

    /**
     * Sets the Relying Party ID.
     *
     * @param rpId the RP ID
     */
    public void setRpId(String rpId) {
        this.rpId = rpId;
    }

    /**
     * Returns the Relying Party display name.
     *
     * @return the RP name
     */
    public String getRpName() {
        return rpName;
    }

    /**
     * Sets the Relying Party display name.
     *
     * @param rpName the RP name
     */
    public void setRpName(String rpName) {
        this.rpName = rpName;
    }

    /**
     * Returns the allowed origins for WebAuthn operations.
     *
     * @return the list of allowed origins
     */
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Sets the allowed origins for WebAuthn operations.
     *
     * @param allowedOrigins the list of allowed origins
     */
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Returns the challenge time-to-live in seconds.
     *
     * @return the TTL in seconds
     */
    public Long getChallengeTtlSeconds() {
        return challengeTtlSeconds;
    }

    /**
     * Sets the challenge time-to-live in seconds.
     *
     * @param challengeTtlSeconds the TTL in seconds
     */
    public void setChallengeTtlSeconds(Long challengeTtlSeconds) {
        this.challengeTtlSeconds = challengeTtlSeconds;
    }
}
