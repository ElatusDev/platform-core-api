/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import com.akademiaplus.infra.config.TenantContextHolder;
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

@Component
@Order(1)
public class JwtRequestFilter extends OncePerRequestFilter {

    private final InternalAuthorizationUseCase internalAuthorizationUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final TenantContextHolder tenantContextHolder;

    public JwtRequestFilter(InternalAuthorizationUseCase internalAuthorizationUseCase,
                            JwtTokenProvider jwtTokenProvider,
                            TenantContextHolder tenantContextHolder) {
        this.internalAuthorizationUseCase = internalAuthorizationUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                if (jwtTokenProvider.validateToken(jwtToken)) {
                    username = jwtTokenProvider.getUsername(jwtToken);
                    Integer tenantId = jwtTokenProvider.getTenantId(jwtToken);
                    tenantContextHolder.setTenantId(tenantId);
                }
            } catch (Exception e) {
               throw new SecurityException(e);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            this.internalAuthorizationUseCase.setTenantContextHolder(tenantContextHolder);
            UserDetails userDetails = this.internalAuthorizationUseCase.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, userDetails.getUsername(), userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }
}