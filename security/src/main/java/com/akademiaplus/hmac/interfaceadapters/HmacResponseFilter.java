/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import com.akademiaplus.hmac.interfaceadapters.config.HmacProperties;
import com.akademiaplus.hmac.usecases.HmacKeyService;
import com.akademiaplus.hmac.usecases.HmacSignatureService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that signs outgoing HTTP responses with HMAC-SHA256.
 *
 * <p>Wraps the response in a {@link CachedBodyHttpServletResponse} to capture
 * the response body, then computes an HMAC-SHA256 signature over the status code,
 * body hash, timestamp, and the original request nonce.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class HmacResponseFilter extends OncePerRequestFilter {

    /** Response header for the HMAC signature. */
    public static final String HEADER_RESPONSE_SIGNATURE = "X-Response-Signature";

    /** Response header for the timestamp (epoch seconds). */
    public static final String HEADER_RESPONSE_TIMESTAMP = "X-Response-Timestamp";

    private final HmacSignatureService hmacSignatureService;
    private final HmacKeyService hmacKeyService;
    private final HmacProperties hmacProperties;

    /**
     * Constructs the filter with all required dependencies.
     *
     * @param hmacSignatureService the HMAC signature computation service
     * @param hmacKeyService       the signing key resolution service
     * @param hmacProperties       the HMAC configuration properties
     */
    public HmacResponseFilter(HmacSignatureService hmacSignatureService,
                               HmacKeyService hmacKeyService,
                               HmacProperties hmacProperties) {
        this.hmacSignatureService = hmacSignatureService;
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

        String requestNonce = (String) request.getAttribute(HmacSigningFilter.REQUEST_ATTR_NONCE);
        if (requestNonce == null) {
            chain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletResponse cachedResponse = new CachedBodyHttpServletResponse(response);

        chain.doFilter(request, cachedResponse);

        byte[] responseBody = cachedResponse.getCachedBody();
        String bodyHash = hmacSignatureService.computeBodyHash(responseBody);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String statusCode = String.valueOf(cachedResponse.getStatus());

        String stringToSign = hmacSignatureService.buildResponseStringToSign(
                statusCode, bodyHash, timestamp, requestNonce);

        byte[] key = hmacKeyService.resolveDefaultKey();
        String signature = hmacSignatureService.computeHmac(key, stringToSign);

        response.setHeader(HEADER_RESPONSE_SIGNATURE, signature);
        response.setHeader(HEADER_RESPONSE_TIMESTAMP, timestamp);

        cachedResponse.writeBodyToResponse();
    }
}
