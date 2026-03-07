/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that resolves the application origin from the incoming request.
 *
 * <p>The origin is determined by (in priority order):</p>
 * <ol>
 *   <li>{@code X-App-Origin} header (explicit frontend identification)</li>
 *   <li>Request path prefix ({@code /akademia/**} or {@code /elatus/**})</li>
 *   <li>Default: {@code elatus} (fail-secure for unknown origins)</li>
 * </ol>
 *
 * <p>The resolved origin is stored as a request attribute and can be
 * accessed via {@link AppOriginContext#getAppOrigin(HttpServletRequest)}.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppOriginFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppOriginFilter.class);

    /** Log message for resolved origin. */
    public static final String LOG_RESOLVED_ORIGIN = "Resolved app origin: {} for path: {}";

    /** Log message for invalid header value. */
    public static final String LOG_INVALID_ORIGIN = "Invalid X-App-Origin header value: {}";

    /**
     * Resolves the app origin and sets it on the request attributes.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String appOrigin = resolveOrigin(request);
        AppOriginContext.setAppOrigin(request, appOrigin);

        LOGGER.debug(LOG_RESOLVED_ORIGIN, appOrigin, request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the app origin from the request using the priority chain:
     * header, path prefix, default.
     *
     * @param request the HTTP request
     * @return the resolved app origin
     */
    private String resolveOrigin(HttpServletRequest request) {
        String headerValue = request.getHeader(AppOriginContext.APP_ORIGIN_HEADER);
        if (headerValue != null) {
            String normalized = headerValue.trim().toLowerCase();
            if (AppOriginContext.ORIGIN_AKADEMIA.equals(normalized)
                    || AppOriginContext.ORIGIN_ELATUS.equals(normalized)) {
                return normalized;
            }
            LOGGER.warn(LOG_INVALID_ORIGIN, headerValue);
        }

        String path = request.getRequestURI();
        if (path.startsWith(AppOriginContext.PATH_PREFIX_AKADEMIA)) {
            return AppOriginContext.ORIGIN_AKADEMIA;
        }
        if (path.startsWith(AppOriginContext.PATH_PREFIX_ELATUS)) {
            return AppOriginContext.ORIGIN_ELATUS;
        }

        return AppOriginContext.DEFAULT_ORIGIN;
    }
}
