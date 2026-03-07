/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import com.akademiaplus.internal.interfaceadapters.session.RedisSessionStore;
import com.akademiaplus.internal.usecases.InternalAuthorizationUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * JWT authentication filter that extracts and validates access tokens.
 *
 * <p>Reads the access token from the HttpOnly cookie first, then falls
 * back to the {@code Authorization: Bearer} header for backward
 * compatibility. Validates JWT signature and expiration, then checks
 * Redis session store for server-side revocation.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
@Order(3)
public class JwtRequestFilter extends OncePerRequestFilter {

    /** Authorization header name. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer prefix for Authorization header values. */
    public static final String BEARER_PREFIX = "Bearer ";

    private final InternalAuthorizationUseCase internalAuthorizationUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieService cookieService;
    private final RedisSessionStore redisSessionStore;

    /**
     * Constructs the filter with all required dependencies.
     *
     * @param internalAuthorizationUseCase the authorization use case
     * @param jwtTokenProvider             the JWT token provider
     * @param cookieService                the cookie service for token extraction
     * @param redisSessionStore            the Redis session store for revocation checks
     */
    public JwtRequestFilter(InternalAuthorizationUseCase internalAuthorizationUseCase,
                             JwtTokenProvider jwtTokenProvider,
                             CookieService cookieService,
                             RedisSessionStore redisSessionStore) {
        this.internalAuthorizationUseCase = internalAuthorizationUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieService = cookieService;
        this.redisSessionStore = redisSessionStore;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        String jwtToken = null;
        String username = null;

        Optional<String> cookieToken = cookieService.extractAccessToken(request);
        if (cookieToken.isPresent()) {
            jwtToken = cookieToken.get();
        }

        if (jwtToken == null) {
            String requestTokenHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (requestTokenHeader != null && requestTokenHeader.startsWith(BEARER_PREFIX)) {
                jwtToken = requestTokenHeader.substring(BEARER_PREFIX.length());
            }
        }

        if (jwtToken != null) {
            try {
                if (jwtTokenProvider.validateToken(jwtToken)) {
                    String jti = jwtTokenProvider.getJti(jwtToken);
                    if (jti != null && !redisSessionStore.isSessionValid(jti)) {
                        chain.doFilter(request, response);
                        return;
                    }
                    username = jwtTokenProvider.getUsername(jwtToken);
                }
            } catch (Exception e) {
                throw new SecurityException(e);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.internalAuthorizationUseCase.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, userDetails.getUsername(), userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }
}
