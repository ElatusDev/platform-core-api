/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.internal.interfaceadapters.filters.AppOriginContext;
import com.akademiaplus.utilities.network.CidrMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Servlet filter that enforces IP-based access control for AkademiaPlus-origin requests.
 *
 * <p>This filter runs before the JWT filter so that requests from unauthorized
 * IP ranges are rejected immediately without consuming JWT validation resources.
 *
 * <p>Only requests identified as AkademiaPlus origin (via {@link AppOriginContext})
 * are subject to IP whitelisting. ElatusDev-origin requests pass through unconditionally.
 *
 * <p>The filter extracts the client IP from the {@code X-Forwarded-For} header
 * (first IP in the chain) for proxied requests, falling back to
 * {@code request.getRemoteAddr()} for direct connections.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class IpWhitelistFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(IpWhitelistFilter.class);

    /** HTTP header for forwarded client IP. */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /** Error message for rejected IP addresses. */
    public static final String ERROR_IP_NOT_ALLOWED = "Access denied: IP address not in whitelist";

    /** Machine-readable error code for IP rejection. */
    public static final String CODE_IP_REJECTED = "IP_NOT_WHITELISTED";

    /** JSON error field: error code. */
    public static final String JSON_FIELD_CODE = "code";

    /** JSON error field: error message. */
    public static final String JSON_FIELD_MESSAGE = "message";

    private static final String LOG_IP_REJECTED = "Rejected AkademiaPlus request from IP: {}";
    private static final String LOG_IP_ALLOWED = "Allowed AkademiaPlus request from IP: {}";

    private final IpWhitelistProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Creates the IP whitelist filter.
     *
     * @param properties    the CIDR configuration properties
     * @param objectMapper  Jackson object mapper for JSON error responses
     */
    public IpWhitelistFilter(IpWhitelistProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Bypasses the filter for health, actuator, and Swagger endpoints.
     *
     * @param request the HTTP request
     * @return {@code true} if the request targets a bypass path
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    /**
     * Applies IP whitelist validation for AkademiaPlus-origin requests.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!AppOriginContext.isAkademia(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);

        if (CidrMatcher.isAllowed(clientIp, properties.getAllowedCidrs())) {
            LOG.debug(LOG_IP_ALLOWED, clientIp);
            filterChain.doFilter(request, response);
            return;
        }

        LOG.warn(LOG_IP_REJECTED, clientIp);
        rejectRequest(response);
    }

    /**
     * Extracts the client IP address from the request.
     *
     * <p>Prefers the first IP in the {@code X-Forwarded-For} header chain
     * (set by reverse proxies). Falls back to {@code request.getRemoteAddr()}
     * for direct connections.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes a 403 Forbidden JSON response for rejected IP addresses.
     *
     * @param response the HTTP response
     * @throws IOException if writing the response fails
     */
    private void rejectRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> errorBody = Map.of(
                JSON_FIELD_CODE, CODE_IP_REJECTED,
                JSON_FIELD_MESSAGE, ERROR_IP_NOT_ALLOWED
        );

        objectMapper.writeValue(response.getWriter(), errorBody);
    }
}
