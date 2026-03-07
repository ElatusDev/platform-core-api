/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases.domain;

/**
 * Immutable representation of a device fingerprint derived from HTTP request context.
 *
 * <p>The {@code fullHash} includes all components (IP + device). The {@code deviceOnlyHash}
 * excludes the IP, enabling RELAXED mode to detect device changes while allowing IP changes.</p>
 *
 * @param fullHash       SHA-256 hash of clientIP + User-Agent + Accept-Language + X-Device-Id
 * @param deviceOnlyHash SHA-256 hash of User-Agent + Accept-Language + X-Device-Id (no IP)
 * @param clientIp       the resolved client IP address (for anomaly logging)
 * @author ElatusDev
 * @since 1.0
 */
public record DeviceFingerprint(String fullHash, String deviceOnlyHash, String clientIp) {
}
