/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import com.akademiaplus.internal.interfaceadapters.session.AkademiaPlusRedisSessionStore;
import com.akademiaplus.internal.usecases.InternalAuthorizationUseCase;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
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

    private static final Logger LOG = LoggerFactory.getLogger(JwtRequestFilter.class);

    /** Authorization header name. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer prefix for Authorization header values. */
    public static final String BEARER_PREFIX = "Bearer ";

    private final InternalAuthorizationUseCase internalAuthorizationUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieService cookieService;
    private final AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore;

    /**
     * Constructs the filter with all required dependencies.
     *
     * @param internalAuthorizationUseCase the authorization use case
     * @param jwtTokenProvider             the JWT token provider
     * @param cookieService                the cookie service for token extraction
     * @param akademiaPlusRedisSessionStore            the Redis session store for revocation checks
     */
    public JwtRequestFilter(InternalAuthorizationUseCase internalAuthorizationUseCase,
                             JwtTokenProvider jwtTokenProvider,
                             CookieService cookieService,
                             AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore) {
        this.internalAuthorizationUseCase = internalAuthorizationUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieService = cookieService;
        this.akademiaPlusRedisSessionStore = akademiaPlusRedisSessionStore;
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
                    if (jti != null && !akademiaPlusRedisSessionStore.isSessionValid(jti)) {
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
            try {
                UserDetails userDetails = this.internalAuthorizationUseCase.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, userDetails.getUsername(), userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } catch (UsernameNotFoundException e) {
                authenticateCustomerToken(jwtToken, username, request);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Authenticates a customer JWT when the internal auth lookup fails.
     * Customer tokens carry a {@code profile_type} claim that distinguishes
     * them from internal user tokens.
     */
    private void authenticateCustomerToken(String jwtToken, String username, HttpServletRequest request) {
        Claims claims = jwtTokenProvider.getClaims(jwtToken);
        String profileType = claims.get(JwtTokenProvider.PROFILE_TYPE_CLAIM, String.class);

        if (profileType != null) {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    username, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            LOG.debug("Authenticated customer token for: {} ({})", username, profileType);
        } else {
            LOG.warn("JWT for '{}' not found in internal auth and has no profile_type claim", username);
        }
    }
}
