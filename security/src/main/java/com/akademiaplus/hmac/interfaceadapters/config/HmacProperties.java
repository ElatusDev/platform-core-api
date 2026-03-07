/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for HMAC API signing.
 *
 * <p>Bound to {@code security.elatus.hmac.*} in application properties.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "security.elatus.hmac")
public class HmacProperties {

    /** Default timestamp tolerance in seconds (5 minutes). */
    public static final long DEFAULT_TIMESTAMP_TOLERANCE_SECONDS = 300L;

    /** Whether HMAC signing is enabled. */
    private boolean enabled = true;

    /** Maximum allowed difference between request timestamp and server time, in seconds. */
    private long timestampToleranceSeconds = DEFAULT_TIMESTAMP_TOLERANCE_SECONDS;

    /** Per-application signing keys. Map of appId to secret key. */
    private Map<String, String> keys = new HashMap<>();

    /**
     * Returns whether HMAC signing is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether HMAC signing is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the timestamp tolerance in seconds.
     *
     * @return tolerance in seconds
     */
    public long getTimestampToleranceSeconds() {
        return timestampToleranceSeconds;
    }

    /**
     * Sets the timestamp tolerance in seconds.
     *
     * @param timestampToleranceSeconds tolerance in seconds
     */
    public void setTimestampToleranceSeconds(long timestampToleranceSeconds) {
        this.timestampToleranceSeconds = timestampToleranceSeconds;
    }

    /**
     * Returns the per-application signing keys.
     *
     * @return map of appId to secret key
     */
    public Map<String, String> getKeys() {
        return keys;
    }

    /**
     * Sets the per-application signing keys.
     *
     * @param keys map of appId to secret key
     */
    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }
}
