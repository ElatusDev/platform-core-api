/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider  {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.keystore.path}")
    private String keystorePath;

    @Value("${jwt.keystore.password}")
    private String keystorePassword;

    @Value("${jwt.keystore.alias}")
    private String keyAlias;

    @Value("${jwt.token.validity-ms}")
    private long validityInMs;

    @Value("${spring.application.name:platform-core-api}")
    private String applicationName;

    public static final String TENANT_ID_CLAIM = "tenant_id";

    private KeyPair keyPair;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        try {
            this.keyPair = KeyLoader.loadKeyPair(keystorePath, keystorePassword, keyAlias);

            this.jwtParser = Jwts.parser()
                    .verifyWith(keyPair.getPublic())
                    .decryptWith(keyPair.getPrivate())
                    .build();

        } catch (Exception e) {
            throw new SecurityException("Failed to initialize JWT key pair", e);
        }
    }

    /**
     * Creates a signed JWT with a subject and a specific tenant ID.
     * Custom claims can also be added.
     *
     * @param username The subject of the token (e.g., user's ID).
     * @param tenantId The tenant ID for the user.
     * @param additionalClaims Additional claims to include in the payload. Can be null.
     * @return The signed JWT string.
     */
    public String createToken(String username, Long tenantId, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMs);

        // Prepare the claims, including the tenant ID
        Map<String, Object> claims = (additionalClaims != null) ? new HashMap<>(additionalClaims) : new HashMap<>();
        claims.put(TENANT_ID_CLAIM, tenantId);

        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(keyPair.getPrivate())
                .compact();
    }

    /**
     * Validates a JWT's signature and expiration time.
     * Does not check for tenant-specific authorization.
     *
     * @param token The JWT string to validate.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            LOGGER.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Parses the JWT to retrieve its claims (payload).
     *
     * @param token The JWT string.
     * @return The claims payload of the token.
     * @throws JwtException if the token is invalid (signature or expiration).
     */
    public Claims getClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    /**
     * Retrieves the username (subject) from a JWT.
     *
     * @param token The JWT string.
     * @return The username.
     * @throws JwtException if the token is invalid.
     */
    public String getUsername(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    /** Returns the loaded key pair (used by {@link JwksRegistrationRunner}). */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /** Returns the service identifier used as the JWKS kid (application name). */
    public String getServiceId() {
        return applicationName;
    }

    /**
     * Retrieves the tenant ID from a JWT.
     *
     * @param token The JWT string.
     * @return The tenant ID.
     * @throws JwtException if the token is invalid.
     */
    public Integer getTenantId(String token) {
        Claims claims = getClaims(token);
        return claims.get(TENANT_ID_CLAIM, Integer.class);
    }
}