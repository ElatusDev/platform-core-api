/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.internal.exceptions.RefreshTokenExpiredException;
import com.akademiaplus.internal.exceptions.TokenReuseDetectedException;
import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.AkademiaPlusRedisSessionStore;
import com.akademiaplus.internal.usecases.domain.TokenRefreshResult;
import com.akademiaplus.security.RefreshTokenDataModel;
import com.akademiaplus.utilities.security.HashingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles refresh token rotation with reuse detection.
 *
 * <p>Validates the incoming refresh token, issues a new access + refresh
 * pair, and marks the old refresh token as consumed. If a previously
 * consumed token is reused (indicating possible theft), all tokens in
 * the family are revoked.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class TokenRefreshUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRefreshUseCase.class);

    /** Error message when the refresh token hash is not found in the database. */
    public static final String ERROR_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found";

    /** Error message when the refresh token has expired. */
    public static final String ERROR_REFRESH_TOKEN_EXPIRED = "Refresh token has expired";

    /** Error message when token reuse is detected. */
    public static final String ERROR_TOKEN_REUSE_DETECTED = "Token reuse detected — all tokens in family revoked";

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore;
    private final HashingService hashingService;
    private final ApplicationContext applicationContext;

    /**
     * Constructs the use case with all required dependencies.
     *
     * @param jwtTokenProvider       the JWT token provider
     * @param refreshTokenRepository the refresh token repository
     * @param akademiaPlusRedisSessionStore      the Redis session store
     * @param hashingService         the hashing service for SHA-256 operations
     * @param applicationContext     the Spring application context for prototype beans
     */
    public TokenRefreshUseCase(JwtTokenProvider jwtTokenProvider,
                                RefreshTokenRepository refreshTokenRepository,
                                AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore,
                                HashingService hashingService,
                                ApplicationContext applicationContext) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.akademiaPlusRedisSessionStore = akademiaPlusRedisSessionStore;
        this.hashingService = hashingService;
        this.applicationContext = applicationContext;
    }

    /**
     * Rotates a refresh token, issuing a new access + refresh pair.
     *
     * <p>Steps:
     * <ol>
     *   <li>Hash the incoming token and look up by hash</li>
     *   <li>If not found, throw {@link RefreshTokenExpiredException}</li>
     *   <li>If already revoked (revokedAt != null), revoke the entire family (reuse detection)</li>
     *   <li>If expired, throw {@link RefreshTokenExpiredException}</li>
     *   <li>Mark old token as consumed and issue new pair with same familyId</li>
     * </ol>
     *
     * @param currentRefreshToken the current refresh token string
     * @return a {@link TokenRefreshResult} containing the new token pair
     * @throws RefreshTokenExpiredException if the refresh token is expired or not found
     * @throws TokenReuseDetectedException  if the token was already consumed
     */
    @Transactional
    public TokenRefreshResult refresh(String currentRefreshToken) {
        String tokenHash = hashingService.generateHash(currentRefreshToken);

        RefreshTokenDataModel existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenExpiredException(ERROR_REFRESH_TOKEN_NOT_FOUND));

        if (existingToken.getRevokedAt() != null) {
            LOGGER.warn("Token reuse detected for family={}, revoking all tokens", existingToken.getFamilyId());
            refreshTokenRepository.revokeAllByFamilyId(existingToken.getFamilyId(), Instant.now());
            akademiaPlusRedisSessionStore.revokeAllSessionsForUser(existingToken.getUsername(), existingToken.getTenantId());
            throw new TokenReuseDetectedException(existingToken.getFamilyId());
        }

        if (existingToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException(ERROR_REFRESH_TOKEN_EXPIRED);
        }

        Instant now = Instant.now();
        existingToken.setRevokedAt(now);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtTokenProvider.USER_ID_CLAIM, existingToken.getUserId());

        String newAccessToken = jwtTokenProvider.createAccessToken(
                existingToken.getUsername(), existingToken.getTenantId(), claims);
        String newJti = jwtTokenProvider.getJti(newAccessToken);

        akademiaPlusRedisSessionStore.storeSession(
                newJti,
                existingToken.getUsername(),
                existingToken.getTenantId(),
                Duration.ofMillis(jwtTokenProvider.getAccessTokenValidityInMs())
        );

        String newRefreshToken = jwtTokenProvider.createRefreshToken(
                existingToken.getUsername(), existingToken.getTenantId(), existingToken.getFamilyId());
        String newRefreshTokenHash = hashingService.generateHash(newRefreshToken);

        existingToken.setReplacedByTokenHash(newRefreshTokenHash);

        RefreshTokenDataModel newTokenEntity = applicationContext.getBean(RefreshTokenDataModel.class);
        newTokenEntity.setTenantId(existingToken.getTenantId());
        newTokenEntity.setTokenHash(newRefreshTokenHash);
        newTokenEntity.setFamilyId(existingToken.getFamilyId());
        newTokenEntity.setUserId(existingToken.getUserId());
        newTokenEntity.setUsername(existingToken.getUsername());
        newTokenEntity.setExpiresAt(
                Instant.ofEpochMilli(jwtTokenProvider.getClaims(newRefreshToken).getExpiration().getTime()));

        refreshTokenRepository.save(newTokenEntity);

        return new TokenRefreshResult(newAccessToken, newRefreshToken, existingToken.getUsername());
    }
}
