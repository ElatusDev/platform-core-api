/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.tokenbinding.interfaceadapters.config.TokenBindingProperties;
import com.akademiaplus.tokenbinding.usecases.AnomalyDetectionService;
import com.akademiaplus.tokenbinding.usecases.DeviceFingerprintService;
import com.akademiaplus.tokenbinding.usecases.domain.AnomalyEvent;
import com.akademiaplus.tokenbinding.usecases.domain.DeviceFingerprint;
import com.akademiaplus.tokenbinding.usecases.domain.TokenBindingMode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet filter that verifies the JWT fingerprint claim matches the
 * current request's device fingerprint.
 *
 * <p>Runs AFTER {@code JwtRequestFilter} (which sets the SecurityContext)
 * and BEFORE business logic. Only activates when token binding mode is
 * not {@link TokenBindingMode#OFF}.</p>
 *
 * <p>Applies only to the ElatusDev filter chain (public internet).
 * AkademiaPlus (school, IP-whitelisted) requests bypass this filter.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class TokenBindingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TokenBindingFilter.class);

    /** JSON error response body for token binding mismatch. */
    public static final String ERROR_RESPONSE_CODE = "TOKEN_BINDING_MISMATCH";

    /** JSON error response message. */
    public static final String ERROR_RESPONSE_MESSAGE = "Token binding verification failed";

    /** Authorization header name. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer prefix for Authorization header values. */
    public static final String BEARER_PREFIX = "Bearer ";

    private static final String LOG_MISMATCH = "Token binding mismatch for user: {}, type: {}";

    private final JwtTokenProvider jwtTokenProvider;
    private final DeviceFingerprintService deviceFingerprintService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final TokenBindingProperties tokenBindingProperties;
    private final CookieService cookieService;

    /**
     * Constructs the filter with all required dependencies.
     *
     * @param jwtTokenProvider          the JWT provider for claim extraction
     * @param deviceFingerprintService  the fingerprint computation service
     * @param anomalyDetectionService   the anomaly logging service
     * @param tokenBindingProperties    the token binding configuration
     * @param cookieService             the cookie service for token extraction
     */
    public TokenBindingFilter(JwtTokenProvider jwtTokenProvider,
                               DeviceFingerprintService deviceFingerprintService,
                               AnomalyDetectionService anomalyDetectionService,
                               TokenBindingProperties tokenBindingProperties,
                               CookieService cookieService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.deviceFingerprintService = deviceFingerprintService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.tokenBindingProperties = tokenBindingProperties;
        this.cookieService = cookieService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        TokenBindingMode mode = tokenBindingProperties.getMode();

        if (mode == TokenBindingMode.OFF) {
            chain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        Claims claims = jwtTokenProvider.getClaims(token);
        String expectedFullHash = claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class);
        String expectedDeviceHash = claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class);

        // Legacy token without fingerprint — skip verification
        if (expectedFullHash == null) {
            chain.doFilter(request, response);
            return;
        }

        DeviceFingerprint currentFingerprint = deviceFingerprintService.computeFingerprint(request);
        String username = claims.getSubject();
        Long tenantId = claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class);

        if (mode == TokenBindingMode.STRICT) {
            if (!expectedFullHash.equals(currentFingerprint.fullHash())) {
                LOG.warn(LOG_MISMATCH, username, AnomalyEvent.EVENT_TYPE_FULL_MISMATCH);
                anomalyDetectionService.logAnomaly(buildAnomalyEvent(
                        username, AnomalyEvent.EVENT_TYPE_FULL_MISMATCH,
                        null, currentFingerprint.clientIp(), tenantId));
                rejectRequest(response);
                return;
            }
        } else if (mode == TokenBindingMode.RELAXED) {
            if (expectedDeviceHash != null
                    && !expectedDeviceHash.equals(currentFingerprint.deviceOnlyHash())) {
                LOG.warn(LOG_MISMATCH, username, AnomalyEvent.EVENT_TYPE_DEVICE_MISMATCH);
                anomalyDetectionService.logAnomaly(buildAnomalyEvent(
                        username, AnomalyEvent.EVENT_TYPE_DEVICE_MISMATCH,
                        null, currentFingerprint.clientIp(), tenantId));
                rejectRequest(response);
                return;
            }
            if (!expectedFullHash.equals(currentFingerprint.fullHash())) {
                anomalyDetectionService.logAnomaly(buildAnomalyEvent(
                        username, AnomalyEvent.EVENT_TYPE_IP_CHANGE,
                        null, currentFingerprint.clientIp(), tenantId));
            }
        }

        chain.doFilter(request, response);
    }

    private void rejectRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + ERROR_RESPONSE_CODE
                + "\",\"message\":\"" + ERROR_RESPONSE_MESSAGE + "\"}");
    }

    private String extractToken(HttpServletRequest request) {
        Optional<String> cookieToken = cookieService.extractAccessToken(request);
        if (cookieToken.isPresent()) {
            return cookieToken.get();
        }
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private AnomalyEvent buildAnomalyEvent(String username, String eventType,
                                            String expectedIp, String actualIp, Long tenantId) {
        return new AnomalyEvent(username, eventType, expectedIp, actualIp,
                tenantId, System.currentTimeMillis(), null);
    }
}
