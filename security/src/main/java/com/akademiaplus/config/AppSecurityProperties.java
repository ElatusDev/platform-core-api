/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for per-app security settings.
 *
 * <p>Provides separate configuration for the akademia-plus-web and
 * elatusdev-web applications, enabling different security postures
 * for school-premises vs public-internet traffic.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "security.app")
public class AppSecurityProperties {

    /** Configuration properties prefix. */
    public static final String PREFIX = "security.app";

    private AppConfig akademia = new AppConfig();
    private AppConfig elatus = new AppConfig();

    /**
     * Returns the akademia-plus-web security configuration.
     *
     * @return the akademia app config
     */
    public AppConfig getAkademia() {
        return akademia;
    }

    /**
     * Sets the akademia-plus-web security configuration.
     *
     * @param akademia the akademia app config
     */
    public void setAkademia(AppConfig akademia) {
        this.akademia = akademia;
    }

    /**
     * Returns the elatusdev-web security configuration.
     *
     * @return the elatus app config
     */
    public AppConfig getElatus() {
        return elatus;
    }

    /**
     * Sets the elatusdev-web security configuration.
     *
     * @param elatus the elatus app config
     */
    public void setElatus(AppConfig elatus) {
        this.elatus = elatus;
    }

    /**
     * Per-application security configuration.
     *
     * @author ElatusDev
     * @since 1.0
     */
    public static class AppConfig {

        /** Whether token binding verification is enabled. */
        private boolean tokenBindingEnabled = false;

        /** Whether rate limiting is enabled. */
        private boolean rateLimitingEnabled = false;

        /** Whether HMAC request verification is enabled. */
        private boolean hmacVerificationEnabled = false;

        /** Allowed CORS origins for this app. */
        private String[] allowedOrigins = {};

        /**
         * Returns whether token binding verification is enabled.
         *
         * @return true if token binding is enabled
         */
        public boolean isTokenBindingEnabled() {
            return tokenBindingEnabled;
        }

        /**
         * Sets whether token binding verification is enabled.
         *
         * @param tokenBindingEnabled the token binding flag
         */
        public void setTokenBindingEnabled(boolean tokenBindingEnabled) {
            this.tokenBindingEnabled = tokenBindingEnabled;
        }

        /**
         * Returns whether rate limiting is enabled.
         *
         * @return true if rate limiting is enabled
         */
        public boolean isRateLimitingEnabled() {
            return rateLimitingEnabled;
        }

        /**
         * Sets whether rate limiting is enabled.
         *
         * @param rateLimitingEnabled the rate limiting flag
         */
        public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
            this.rateLimitingEnabled = rateLimitingEnabled;
        }

        /**
         * Returns whether HMAC request verification is enabled.
         *
         * @return true if HMAC verification is enabled
         */
        public boolean isHmacVerificationEnabled() {
            return hmacVerificationEnabled;
        }

        /**
         * Sets whether HMAC request verification is enabled.
         *
         * @param hmacVerificationEnabled the HMAC verification flag
         */
        public void setHmacVerificationEnabled(boolean hmacVerificationEnabled) {
            this.hmacVerificationEnabled = hmacVerificationEnabled;
        }

        /**
         * Returns the allowed CORS origins for this app.
         *
         * @return the allowed origins array
         */
        public String[] getAllowedOrigins() {
            return allowedOrigins;
        }

        /**
         * Sets the allowed CORS origins for this app.
         *
         * @param allowedOrigins the allowed origins array
         */
        public void setAllowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
