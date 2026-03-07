/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases;

import com.akademiaplus.tokenbinding.usecases.domain.DeviceFingerprint;
import com.akademiaplus.utilities.security.HashingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Computes device fingerprints from HTTP request context.
 *
 * <p>Extracts client IP, User-Agent, Accept-Language, and X-Device-Id from the
 * request, concatenates them with a separator, and produces SHA-256 hashes for
 * both full (IP-inclusive) and device-only (IP-exclusive) fingerprints.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeviceFingerprintService {

    /** Custom header for stable client device identification. */
    public static final String HEADER_DEVICE_ID = "X-Device-Id";

    /** Standard User-Agent header. */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /** Standard Accept-Language header. */
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

    /** X-Forwarded-For header for proxy-aware IP resolution. */
    public static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    /** Separator between fingerprint components. */
    public static final String FINGERPRINT_SEPARATOR = "|";

    /** Default value when a header is missing or blank. */
    public static final String UNKNOWN_VALUE = "unknown";

    private final HashingService hashingService;

    /**
     * Constructs the service with a hashing dependency.
     *
     * @param hashingService the SHA-256 hashing service
     */
    public DeviceFingerprintService(HashingService hashingService) {
        this.hashingService = hashingService;
    }

    /**
     * Computes a {@link DeviceFingerprint} from the given HTTP request.
     *
     * @param request the current HTTP servlet request
     * @return the computed device fingerprint with full and device-only hashes
     */
    public DeviceFingerprint computeFingerprint(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        String userAgent = defaultIfBlank(request.getHeader(HEADER_USER_AGENT));
        String acceptLanguage = defaultIfBlank(request.getHeader(HEADER_ACCEPT_LANGUAGE));
        String deviceId = defaultIfBlank(request.getHeader(HEADER_DEVICE_ID));

        String deviceComponents = userAgent + FINGERPRINT_SEPARATOR
                + acceptLanguage + FINGERPRINT_SEPARATOR
                + deviceId;

        String fullComponents = clientIp + FINGERPRINT_SEPARATOR + deviceComponents;

        String fullHash = hashingService.generateHash(fullComponents);
        String deviceOnlyHash = hashingService.generateHash(deviceComponents);

        return new DeviceFingerprint(fullHash, deviceOnlyHash, clientIp);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String defaultIfBlank(String value) {
        return (value == null || value.isBlank()) ? UNKNOWN_VALUE : value;
    }
}
