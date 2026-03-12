/*
 * Copyright (c) 2026 ElatusDev
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
import java.util.UUID;

/**
 * Provides JWT token creation, validation, and claim extraction.
 *
 * <p>Supports both access tokens (short-lived, with JTI for Redis session
 * tracking) and refresh tokens (long-lived, with family ID for rotation
 * chain tracking).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class JwtTokenProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.keystore.path}")
    private String keystorePath;

    @Value("${jwt.keystore.password}")
    private String keystorePassword;

    @Value("${jwt.keystore.alias}")
    private String keyAlias;

    @Value("${jwt.token.validity-ms}")
    private long validityInMs;

    @Value("${jwt.refresh-token.validity-ms}")
    private long refreshTokenValidityInMs;

    @Value("${spring.application.name:platform-core-api}")
    private String applicationName;

    /** Claim key for the tenant identifier. */
    public static final String TENANT_ID_CLAIM = "tenant_id";

    /** Claim key for the token type (access or refresh). */
    public static final String TOKEN_TYPE_CLAIM = "token_type";

    /** Token type value for access tokens. */
    public static final String TOKEN_TYPE_ACCESS = "access";

    /** Token type value for refresh tokens. */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /** Claim key for the refresh token family identifier. */
    public static final String FAMILY_ID_CLAIM = "family_id";

    /** Claim key for the user ID. */
    public static final String USER_ID_CLAIM = "user_id";

    /** Claim key for the full device fingerprint hash (IP + device). */
    public static final String FINGERPRINT_CLAIM = "fpr";

    /** Claim key for the device-only fingerprint hash (no IP). */
    public static final String DEVICE_FINGERPRINT_CLAIM = "dfpr";

    /** Claim key for the customer profile type (ADULT_STUDENT or TUTOR). */
    public static final String PROFILE_TYPE_CLAIM = "profile_type";

    /** Claim key for the customer profile ID (adultStudentId or tutorId). */
    public static final String PROFILE_ID_CLAIM = "profile_id";

    /** Profile type value for adult students. */
    public static final String PROFILE_TYPE_ADULT_STUDENT = "ADULT_STUDENT";

    /** Profile type value for tutors. */
    public static final String PROFILE_TYPE_TUTOR = "TUTOR";

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
     * @deprecated Use {@link #createAccessToken(String, Long, Map)} instead.
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public String createToken(String username, Long tenantId, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMs);

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
     * Creates a signed access token with a unique JTI claim for Redis session tracking.
     *
     * @param username         the subject of the token
     * @param tenantId         the tenant ID
     * @param additionalClaims additional claims to include (may be null)
     * @return the signed JWT access token string
     */
    public String createAccessToken(String username, Long tenantId, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMs);

        Map<String, Object> claims = (additionalClaims != null) ? new HashMap<>(additionalClaims) : new HashMap<>();
        claims.put(TENANT_ID_CLAIM, tenantId);
        claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS);

        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(keyPair.getPrivate())
                .compact();
    }

    /**
     * Creates a signed refresh token with a family ID for rotation tracking.
     *
     * @param username the subject of the token
     * @param tenantId the tenant ID
     * @param familyId the token family UUID for rotation chain tracking
     * @return the signed JWT refresh token string
     */
    public String createRefreshToken(String username, Long tenantId, String familyId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityInMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put(TENANT_ID_CLAIM, tenantId);
        claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH);
        claims.put(FAMILY_ID_CLAIM, familyId);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(keyPair.getPrivate())
                .compact();
    }

    /**
     * Extracts the JTI (JWT ID) claim from a token.
     *
     * @param token the JWT string
     * @return the JTI value
     */
    public String getJti(String token) {
        return getClaims(token).getId();
    }

    /**
     * Extracts the token type claim from a token.
     *
     * @param token the JWT string
     * @return the token type ({@code access} or {@code refresh})
     */
    public String getTokenType(String token) {
        return getClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    /**
     * Returns the access token validity in milliseconds.
     *
     * @return access token TTL in milliseconds
     */
    public long getAccessTokenValidityInMs() {
        return validityInMs;
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