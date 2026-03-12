/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet filter that reads customer profile claims from the JWT and
 * populates the {@link UserContextHolder} thread-local.
 *
 * <p>Runs after {@link com.akademiaplus.internal.interfaceadapters.jwt.JwtRequestFilter}
 * (order 3) to ensure the JWT has already been validated and the
 * {@code SecurityContext} is set. If the JWT contains {@code profile_type}
 * and {@code profile_id} claims (customer tokens only), they are stored
 * in the {@code UserContextHolder} for use by {@code /v1/my/*} endpoints.</p>
 *
 * <p>Internal user tokens (employees, collaborators) do not carry these
 * claims and are silently skipped — the UserContextHolder remains empty.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(4)
public class UserContextLoader extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(UserContextLoader.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieService cookieService;
    private final UserContextHolder userContextHolder;

    /**
     * Constructs the filter with required dependencies.
     *
     * @param jwtTokenProvider  the JWT token provider for claim extraction
     * @param cookieService     the cookie service for token extraction
     * @param userContextHolder the user context holder
     */
    public UserContextLoader(JwtTokenProvider jwtTokenProvider,
                             CookieService cookieService,
                             UserContextHolder userContextHolder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieService = cookieService;
        this.userContextHolder = userContextHolder;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                extractAndSetUserContext(request);
            }
            filterChain.doFilter(request, response);
        } finally {
            userContextHolder.clear();
        }
    }

    private void extractAndSetUserContext(HttpServletRequest request) {
        Optional<String> token = extractToken(request);
        if (token.isEmpty()) {
            return;
        }

        try {
            Claims claims = jwtTokenProvider.getClaims(token.get());
            String profileType = claims.get(JwtTokenProvider.PROFILE_TYPE_CLAIM, String.class);
            Object profileIdRaw = claims.get(JwtTokenProvider.PROFILE_ID_CLAIM);

            if (profileType != null && profileIdRaw != null) {
                Long profileId = ((Number) profileIdRaw).longValue();
                userContextHolder.set(profileType, profileId);
                LOG.debug("Set user context: profileType={}, profileId={}", profileType, profileId);
            }
        } catch (Exception e) {
            LOG.warn("Failed to extract user context from JWT: {}", e.getMessage());
        }
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        Optional<String> cookieToken = cookieService.extractAccessToken(request);
        if (cookieToken.isPresent()) {
            return cookieToken;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }
}
