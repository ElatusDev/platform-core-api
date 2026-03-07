/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases;

import com.akademiaplus.hmac.exceptions.HmacSignatureException;
import com.akademiaplus.hmac.interfaceadapters.config.HmacProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Manages per-application HMAC signing keys.
 *
 * <p>Keys are loaded from configuration properties. For the initial implementation,
 * a single shared key is used for all ElatusDev clients.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class HmacKeyService {

    /** Default application identifier for ElatusDev web. */
    public static final String DEFAULT_APP_ID = "elatusdev-web";

    private final HmacProperties hmacProperties;

    /**
     * Constructs the service with HMAC properties.
     *
     * @param hmacProperties the HMAC configuration properties
     */
    public HmacKeyService(HmacProperties hmacProperties) {
        this.hmacProperties = hmacProperties;
    }

    /**
     * Resolves the signing key for the given application.
     *
     * @param appId the application identifier
     * @return the signing key as bytes
     * @throws HmacSignatureException if no key is configured for the given appId
     */
    public byte[] resolveKey(String appId) {
        String key = hmacProperties.getKeys().get(appId);
        if (key == null) {
            key = hmacProperties.getKeys().get(DEFAULT_APP_ID);
        }
        if (key == null) {
            throw new HmacSignatureException(
                    String.format(HmacSignatureException.ERROR_NO_KEY_CONFIGURED, appId));
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Resolves the default signing key (for single-app deployments).
     *
     * @return the default signing key as bytes
     */
    public byte[] resolveDefaultKey() {
        return resolveKey(DEFAULT_APP_ID);
    }
}
