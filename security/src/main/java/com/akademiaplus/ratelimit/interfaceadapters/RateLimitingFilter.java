/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.interfaceadapters;

import com.akademiaplus.config.RateLimitProperties;
import com.akademiaplus.ratelimit.usecases.RateLimiterService;
import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Servlet filter that enforces rate limits on incoming HTTP requests.
 *
 * <p>This filter runs before the JWT filter and checks rate limits using
 * {@link RateLimiterService}. For unauthenticated endpoints, rate limits
 * are applied per client IP. For authenticated endpoints, rate limits
 * are applied per authenticated user.
 *
 * <p>Only applied to the ElatusDev filter chain (public internet).
 * The AkademiaPlus filter chain (school network) is exempt.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** Response header: maximum requests allowed in the window. */
    public static final String HEADER_RATE_LIMIT = "X-RateLimit-Limit";

    /** Response header: remaining requests in the current window. */
    public static final String HEADER_RATE_REMAINING = "X-RateLimit-Remaining";

    /** Response header: epoch second when the window resets. */
    public static final String HEADER_RATE_RESET = "X-RateLimit-Reset";

    /** Response header: seconds until the client may retry after a 429. */
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    /** JSON field name for error response status. */
    public static final String JSON_FIELD_STATUS = "status";

    /** JSON field name for error response error type. */
    public static final String JSON_FIELD_ERROR = "error";

    /** JSON field name for error response message. */
    public static final String JSON_FIELD_MESSAGE = "message";

    /** JSON field name for error response retry-after value. */
    public static final String JSON_FIELD_RETRY_AFTER = "retryAfterSeconds";

    /** Error message for 429 response body. */
    public static final String ERROR_TOO_MANY_REQUESTS = "Too Many Requests";

    /** Error message detail for 429 response body. */
    public static final String ERROR_RATE_LIMIT_MESSAGE = "Rate limit exceeded. Please retry after the indicated time.";

    /** Rate limit tier name for login endpoints. */
    public static final String TIER_LOGIN = "login";

    /** Rate limit tier name for public (unauthenticated) endpoints. */
    public static final String TIER_PUBLIC = "public";

    /** Rate limit tier name for authenticated endpoints. */
    public static final String TIER_AUTHENTICATED = "authenticated";

    /** Header used to obtain the original client IP behind a reverse proxy. */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    private static final String LOG_RATE_LIMITED = "Rate limit exceeded for key: {}";

    private static final Set<String> BYPASS_PREFIXES = Set.of(
            "/actuator",
            "/health",
            "/v3/api-docs",
            "/swagger-ui"
    );

    private static final Set<String> LOGIN_PATH_PREFIXES = Set.of(
            "/v1/security/login",
            "/v1/security/passkey/login"
    );

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/v1/security/register"
    );

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a RateLimitingFilter.
     *
     * @param rateLimiterService   the service that performs rate limit checks
     * @param rateLimitProperties  the rate limit configuration properties
     * @param objectMapper         Jackson mapper for JSON error responses
     */
    public RateLimitingFilter(RateLimiterService rateLimiterService,
                              RateLimitProperties rateLimitProperties,
                              ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = getPathWithoutContext(request);
        return BYPASS_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitProperties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = getPathWithoutContext(request);
        String tier = resolveTier(path);
        RateLimitProperties.TierProperties tierConfig = rateLimitProperties.tiers().get(tier);

        if (tierConfig == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request, tier);
        RateLimitResult result = rateLimiterService.checkRateLimit(
                key, tierConfig.limit(), tierConfig.windowMs());

        writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            LOG.warn(LOG_RATE_LIMITED, key);
            writeRateLimitExceededResponse(response, result);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the rate limit tier based on the request path.
     *
     * @param path the request URI without context path
     * @return the tier name (login, public, or authenticated)
     */
    String resolveTier(String path) {
        if (LOGIN_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            return TIER_LOGIN;
        }
        if (PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            return TIER_PUBLIC;
        }
        return TIER_AUTHENTICATED;
    }

    /**
     * Resolves the rate limit key for the request.
     *
     * @param request the HTTP request
     * @param tier    the rate limit tier
     * @return the Redis key for rate limiting
     */
    String resolveKey(HttpServletRequest request, String tier) {
        if (TIER_AUTHENTICATED.equals(tier)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
                return RateLimiterService.KEY_PREFIX_USER + auth.getName();
            }
        }
        String clientIp = resolveClientIp(request);
        return RateLimiterService.KEY_PREFIX_IP + clientIp + ":" + tier;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getPathWithoutContext(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }
        return path;
    }

    private void writeRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setIntHeader(HEADER_RATE_LIMIT, result.limit());
        response.setIntHeader(HEADER_RATE_REMAINING, result.remaining());
        response.setHeader(HEADER_RATE_RESET, String.valueOf(result.resetEpochSeconds()));
    }

    private void writeRateLimitExceededResponse(HttpServletResponse response, RateLimitResult result)
            throws IOException {
        long retryAfterSeconds = Math.max(1L, result.resetEpochSeconds() - Instant.now().getEpochSecond());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(JSON_FIELD_STATUS, HttpStatus.TOO_MANY_REQUESTS.value());
        body.put(JSON_FIELD_ERROR, ERROR_TOO_MANY_REQUESTS);
        body.put(JSON_FIELD_MESSAGE, ERROR_RATE_LIMIT_MESSAGE);
        body.put(JSON_FIELD_RETRY_AFTER, retryAfterSeconds);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
