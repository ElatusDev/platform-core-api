/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.exceptions.InvalidLoginException;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.RedisSessionStore;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.security.RefreshTokenDataModel;
import com.akademiaplus.utilities.security.HashingService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles internal user authentication, issuing access and refresh token pairs.
 *
 * <p>On successful login, creates a short-lived access token (stored in Redis
 * for session tracking) and a long-lived refresh token (hashed and persisted
 * in the database for rotation tracking).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class InternalAuthenticationUseCase {

    private final InternalAuthRepository repository;
    private final JwtTokenProvider jwtTokenProvider;
    private final HashingService hashingService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisSessionStore redisSessionStore;
    private final ApplicationContext applicationContext;

    /**
     * Constructs the use case with all required dependencies.
     *
     * @param repository             the internal auth repository
     * @param jwtTokenProvider       the JWT token provider
     * @param hashingService         the hashing service for SHA-256 operations
     * @param refreshTokenRepository the refresh token repository
     * @param redisSessionStore      the Redis session store
     * @param applicationContext     the Spring application context for prototype beans
     */
    public InternalAuthenticationUseCase(InternalAuthRepository repository,
                                         JwtTokenProvider jwtTokenProvider,
                                         HashingService hashingService,
                                         RefreshTokenRepository refreshTokenRepository,
                                         RedisSessionStore redisSessionStore,
                                         ApplicationContext applicationContext) {
        this.repository = repository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.hashingService = hashingService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisSessionStore = redisSessionStore;
        this.applicationContext = applicationContext;
    }

    /**
     * Authenticates the user and issues an access + refresh token pair.
     *
     * @param username the username
     * @param password the password
     * @return a {@link LoginResult} containing the access token, refresh token, and username
     * @throws InvalidLoginException if credentials are invalid
     */
    @Transactional
    public LoginResult login(String username, String password) {
        return login(username, password, null);
    }

    /**
     * Authenticates the user and issues an access + refresh token pair,
     * merging optional fingerprint claims into the access token.
     *
     * @param username          the username
     * @param password          the password
     * @param fingerprintClaims optional fingerprint claims to embed in the JWT (may be null)
     * @return a {@link LoginResult} containing the access token, refresh token, and username
     * @throws InvalidLoginException if credentials are invalid
     */
    @Transactional
    public LoginResult login(String username, String password, Map<String, Object> fingerprintClaims) {
        String usernameHash = hashingService.generateHash(username);
        InternalAuthDataModel auth = repository.findByUsernameHash(usernameHash)
                .filter(user -> password.equals(user.getPassword()))
                .orElseThrow(InvalidLoginException::new);

        Map<String, Object> claims = new HashMap<>();
        claims.put("Has role", auth.getRole());
        claims.put(JwtTokenProvider.USER_ID_CLAIM, auth.getInternalAuthId());
        if (fingerprintClaims != null) {
            claims.putAll(fingerprintClaims);
        }

        String accessToken = jwtTokenProvider.createAccessToken(auth.getUsername(), auth.getTenantId(), claims);
        String jti = jwtTokenProvider.getJti(accessToken);

        redisSessionStore.storeSession(
                jti,
                auth.getUsername(),
                auth.getTenantId(),
                Duration.ofMillis(jwtTokenProvider.getAccessTokenValidityInMs())
        );

        String familyId = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(auth.getUsername(), auth.getTenantId(), familyId);

        String refreshTokenHash = hashingService.generateHash(refreshToken);
        RefreshTokenDataModel refreshTokenEntity = applicationContext.getBean(RefreshTokenDataModel.class);
        refreshTokenEntity.setTenantId(auth.getTenantId());
        refreshTokenEntity.setTokenHash(refreshTokenHash);
        refreshTokenEntity.setFamilyId(familyId);
        refreshTokenEntity.setUserId(auth.getInternalAuthId());
        refreshTokenEntity.setUsername(auth.getUsername());
        refreshTokenEntity.setExpiresAt(Instant.now().plusMillis(jwtTokenProvider.getAccessTokenValidityInMs())
                .plusMillis(jwtTokenProvider.getAccessTokenValidityInMs()));
        refreshTokenEntity.setExpiresAt(
                Instant.ofEpochMilli(jwtTokenProvider.getClaims(refreshToken).getExpiration().getTime()));

        refreshTokenRepository.save(refreshTokenEntity);

        return new LoginResult(accessToken, refreshToken, auth.getUsername());
    }
}
