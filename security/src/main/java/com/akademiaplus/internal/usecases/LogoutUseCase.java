/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.RedisSessionStore;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles user logout by revoking all refresh tokens and Redis sessions.
 *
 * <p>Revokes all refresh tokens for the user within the tenant and
 * removes all Redis session entries. The controller is responsible
 * for clearing the cookies.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class LogoutUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutUseCase.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisSessionStore redisSessionStore;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructs the use case with all required dependencies.
     *
     * @param refreshTokenRepository the refresh token repository
     * @param redisSessionStore      the Redis session store
     * @param jwtTokenProvider       the JWT token provider
     */
    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository,
                          RedisSessionStore redisSessionStore,
                          JwtTokenProvider jwtTokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisSessionStore = redisSessionStore;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Revokes all tokens and sessions for the authenticated user.
     *
     * @param accessToken the current access token (to extract user identity)
     */
    @Transactional
    public void logout(String accessToken) {
        Claims claims = jwtTokenProvider.getClaims(accessToken);
        String username = claims.getSubject();
        Long tenantId = claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class);
        Long userId = claims.get(JwtTokenProvider.USER_ID_CLAIM, Long.class);

        if (userId != null && tenantId != null) {
            refreshTokenRepository.revokeAllByUserIdAndTenantId(userId, tenantId, Instant.now());
        }

        redisSessionStore.revokeAllSessionsForUser(username, tenantId);

        LOGGER.info("User {} logged out from tenant {}", username, tenantId);
    }
}
