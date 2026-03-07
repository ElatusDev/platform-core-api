/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases.domain;

/**
 * Represents a suspicious event detected during token binding verification.
 *
 * @param username     the JWT subject (user identifier)
 * @param eventType    the type of anomaly (e.g., IP_CHANGE, DEVICE_MISMATCH)
 * @param expectedIp   the IP embedded in the token fingerprint (null if not available)
 * @param actualIp     the IP from the current request
 * @param tenantId     the tenant context
 * @param timestamp    the event timestamp (epoch millis)
 * @param details      additional context for logging
 * @author ElatusDev
 * @since 1.0
 */
public record AnomalyEvent(
        String username,
        String eventType,
        String expectedIp,
        String actualIp,
        Long tenantId,
        long timestamp,
        String details
) {

    /** Anomaly type: client IP changed mid-session. */
    public static final String EVENT_TYPE_IP_CHANGE = "IP_CHANGE";

    /** Anomaly type: device fingerprint does not match. */
    public static final String EVENT_TYPE_DEVICE_MISMATCH = "DEVICE_MISMATCH";

    /** Anomaly type: full fingerprint (IP + device) does not match. */
    public static final String EVENT_TYPE_FULL_MISMATCH = "FULL_MISMATCH";
}
