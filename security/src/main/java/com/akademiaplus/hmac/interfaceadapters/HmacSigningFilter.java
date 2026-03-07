/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import com.akademiaplus.hmac.exceptions.HmacSignatureException;
import com.akademiaplus.hmac.interfaceadapters.config.HmacProperties;
import com.akademiaplus.hmac.usecases.HmacKeyService;
import com.akademiaplus.hmac.usecases.HmacSignatureService;
import com.akademiaplus.hmac.usecases.NonceStore;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet filter that verifies HMAC-SHA256 signatures on incoming requests.
 *
 * <p>Runs AFTER {@code JwtRequestFilter} and {@code TokenBindingFilter}.
 * Only activates for authenticated requests when HMAC is enabled.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class HmacSigningFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(HmacSigningFilter.class);

    /** Request header for the HMAC signature. */
    public static final String HEADER_SIGNATURE = "X-Signature";

    /** Request header for the timestamp (epoch seconds). */
    public static final String HEADER_TIMESTAMP = "X-Timestamp";

    /** Request header for the nonce (UUID). */
    public static final String HEADER_NONCE = "X-Nonce";

    /** Request header for the body hash (SHA-256 hex). */
    public static final String HEADER_BODY_HASH = "X-Body-Hash";

    /** Request attribute key for storing the nonce for response signing. */
    public static final String REQUEST_ATTR_NONCE = "hmac.request.nonce";

    private static final String LOG_HMAC_FAILED = "HMAC verification failed: {}";

    private final HmacSignatureService hmacSignatureService;
    private final NonceStore nonceStore;
    private final HmacKeyService hmacKeyService;
    private final HmacProperties hmacProperties;

    /**
     * Constructs the filter with all required dependencies.
     *
     * @param hmacSignatureService the HMAC signature computation service
     * @param nonceStore           the nonce store for replay prevention
     * @param hmacKeyService       the signing key resolution service
     * @param hmacProperties       the HMAC configuration properties
     */
    public HmacSigningFilter(HmacSignatureService hmacSignatureService,
                              NonceStore nonceStore,
                              HmacKeyService hmacKeyService,
                              HmacProperties hmacProperties) {
        this.hmacSignatureService = hmacSignatureService;
        this.nonceStore = nonceStore;
        this.hmacKeyService = hmacKeyService;
        this.hmacProperties = hmacProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        if (!hmacProperties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            chain.doFilter(request, response);
            return;
        }

        String signature = request.getHeader(HEADER_SIGNATURE);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String bodyHash = request.getHeader(HEADER_BODY_HASH);

        if (signature == null || timestamp == null || nonce == null || bodyHash == null) {
            String missing = buildMissingHeadersList(signature, timestamp, nonce, bodyHash);
            rejectRequest(response,
                    String.format(HmacSignatureException.ERROR_MISSING_HEADERS, missing));
            return;
        }

        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            rejectRequest(response,
                    String.format(HmacSignatureException.ERROR_TIMESTAMP_EXPIRED, 0L,
                            hmacProperties.getTimestampToleranceSeconds()));
            return;
        }

        long serverTime = System.currentTimeMillis() / 1000L;
        long delta = Math.abs(serverTime - requestTime);
        if (delta > hmacProperties.getTimestampToleranceSeconds()) {
            rejectRequest(response,
                    String.format(HmacSignatureException.ERROR_TIMESTAMP_EXPIRED,
                            delta, hmacProperties.getTimestampToleranceSeconds()));
            return;
        }

        if (nonceStore.exists(nonce)) {
            rejectRequest(response,
                    String.format(HmacSignatureException.ERROR_NONCE_REPLAY, nonce));
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        String computedBodyHash = hmacSignatureService.computeBodyHash(cachedRequest.getCachedBody());
        if (!computedBodyHash.equals(bodyHash)) {
            rejectRequest(response, HmacSignatureException.ERROR_BODY_HASH_MISMATCH);
            return;
        }

        String stringToSign = hmacSignatureService.buildRequestStringToSign(
                cachedRequest.getMethod(),
                cachedRequest.getRequestURI(),
                timestamp, bodyHash, nonce);

        byte[] key = hmacKeyService.resolveDefaultKey();
        if (!hmacSignatureService.verifySignature(key, stringToSign, signature)) {
            rejectRequest(response,
                    String.format(HmacSignatureException.ERROR_SIGNATURE_INVALID, "HMAC mismatch"));
            return;
        }

        nonceStore.store(nonce, hmacProperties.getTimestampToleranceSeconds());
        cachedRequest.setAttribute(REQUEST_ATTR_NONCE, nonce);

        chain.doFilter(cachedRequest, response);
    }

    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        LOG.warn(LOG_HMAC_FAILED, message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + HmacSignatureException.ERROR_CODE_HMAC
                + "\",\"message\":\"" + message + "\"}");
    }

    String buildMissingHeadersList(String signature, String timestamp,
                                   String nonce, String bodyHash) {
        List<String> missing = new ArrayList<>();
        if (signature == null) missing.add(HEADER_SIGNATURE);
        if (timestamp == null) missing.add(HEADER_TIMESTAMP);
        if (nonce == null) missing.add(HEADER_NONCE);
        if (bodyHash == null) missing.add(HEADER_BODY_HASH);
        return String.join(", ", missing);
    }
}
