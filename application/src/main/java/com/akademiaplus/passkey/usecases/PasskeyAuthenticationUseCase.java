/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.usecases;

import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.internal.interfaceadapters.RefreshTokenRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.AkademiaPlusRedisSessionStore;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.passkey.exceptions.PasskeyAuthenticationException;
import com.akademiaplus.passkey.interfaceadapters.PasskeyCredentialJpaRepository;
import com.akademiaplus.passkey.usecases.PasskeyChallengeStore;
import com.akademiaplus.passkey.usecases.PasskeyRegistrationUseCase;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.security.PasskeyCredentialDataModel;
import com.akademiaplus.security.RefreshTokenDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.exception.AssertionFailedException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-module orchestrator for passkey authentication.
 *
 * <p>Coordinates WebAuthn assertion validation (security module) with user
 * lookup and JWT issuance. Handles login options generation, login completion,
 * and registration orchestration (delegating to {@link PasskeyRegistrationUseCase}).</p>
 *
 * <p>Lives in the application module per Hard Rule #14 — cross-module
 * orchestration is restricted to this module.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class PasskeyAuthenticationUseCase {

    /** JWT claim key for the user's role. */
    public static final String JWT_CLAIM_ROLE = "Has role";

    /** JWT claim key for the authentication method. */
    public static final String JWT_CLAIM_AUTH_METHOD = "auth_method";

    /** Authentication method value for passkey-based login. */
    public static final String AUTH_METHOD_PASSKEY = "passkey";

    /** Error message when the assertion response cannot be parsed. */
    public static final String ERROR_INVALID_RESPONSE = "Invalid assertion response format";

    /** Error message when assertion validation fails. */
    public static final String ERROR_ASSERTION_VALIDATION_FAILED = "Assertion validation failed";

    /** Error message when stored options cannot be deserialized. */
    public static final String ERROR_OPTIONS_DESERIALIZATION_FAILED = "Failed to deserialize stored assertion options";

    /** Error message when assertion options serialization fails. */
    public static final String ERROR_OPTIONS_SERIALIZATION_FAILED = "Failed to serialize assertion options";

    /** Error message when the user cannot be resolved from the credential. */
    public static final String ERROR_USER_NOT_FOUND = "User not found for credential";

    /** Length in bytes for generated user handles. */
    private static final int USER_HANDLE_LENGTH = 64;

    private final RelyingParty relyingParty;
    private final PasskeyChallengeStore challengeStore;
    private final PasskeyCredentialJpaRepository credentialRepository;
    private final PasskeyRegistrationUseCase registrationUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final InternalAuthRepository internalAuthRepository;
    private final HashingService hashingService;
    private final AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationContext applicationContext;

    /**
     * Constructs the authentication use case with all required dependencies.
     *
     * @param relyingParty           the WebAuthn relying party
     * @param challengeStore         the Redis-backed challenge store
     * @param credentialRepository   the passkey credential repository
     * @param registrationUseCase    the registration use case for delegation
     * @param jwtTokenProvider       the JWT token provider
     * @param internalAuthRepository the internal auth repository for user lookup
     * @param hashingService         the hashing service for username hash lookup
     * @param akademiaPlusRedisSessionStore      the Redis session store
     * @param refreshTokenRepository the refresh token repository
     * @param applicationContext     the Spring application context for prototype beans
     */
    public PasskeyAuthenticationUseCase(RelyingParty relyingParty,
                                         PasskeyChallengeStore challengeStore,
                                         PasskeyCredentialJpaRepository credentialRepository,
                                         PasskeyRegistrationUseCase registrationUseCase,
                                         JwtTokenProvider jwtTokenProvider,
                                         InternalAuthRepository internalAuthRepository,
                                         HashingService hashingService,
                                         AkademiaPlusRedisSessionStore akademiaPlusRedisSessionStore,
                                         RefreshTokenRepository refreshTokenRepository,
                                         ApplicationContext applicationContext) {
        this.relyingParty = relyingParty;
        this.challengeStore = challengeStore;
        this.credentialRepository = credentialRepository;
        this.registrationUseCase = registrationUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
        this.internalAuthRepository = internalAuthRepository;
        this.hashingService = hashingService;
        this.akademiaPlusRedisSessionStore = akademiaPlusRedisSessionStore;
        this.refreshTokenRepository = refreshTokenRepository;
        this.applicationContext = applicationContext;
    }

    // ── Registration orchestration ────────────────────────────────────────

    /**
     * Orchestrates passkey registration options generation.
     *
     * <p>Resolves the authenticated user's ID from the username, looks up
     * or generates a user handle, and delegates to
     * {@link PasskeyRegistrationUseCase}.</p>
     *
     * @param username the authenticated user's username
     * @param tenantId the tenant ID
     * @return JSON-serialized PublicKeyCredentialCreationOptions for the browser
     */
    @Transactional
    public String generateRegistrationOptions(String username, Long tenantId) {
        String usernameHash = hashingService.generateHash(username);
        InternalAuthDataModel auth = internalAuthRepository.findByUsernameHash(usernameHash)
                .orElseThrow(() -> new PasskeyAuthenticationException(
                        String.format(PasskeyAuthenticationException.ERROR_AUTHENTICATION_FAILED, ERROR_USER_NOT_FOUND)));

        byte[] userHandle = resolveOrGenerateUserHandle(auth.getInternalAuthId());

        return registrationUseCase.generateRegistrationOptions(
                auth.getInternalAuthId(), username, userHandle, tenantId);
    }

    /**
     * Orchestrates passkey registration completion by delegating to
     * {@link PasskeyRegistrationUseCase}.
     *
     * @param responseJson the authenticator's registration response (JSON)
     * @param tenantId     the tenant ID
     * @param displayName  human-readable name for the credential
     * @return the stored credential's display name
     */
    @Transactional
    public String completeRegistration(String responseJson, Long tenantId, String displayName) {
        return registrationUseCase.completeRegistration(responseJson, tenantId, displayName);
    }

    // ── Login flow ────────────────────────────────────────────────────────

    /**
     * Generates authentication options for passkey login.
     *
     * @param tenantId the tenant ID from the request
     * @return JSON-serialized PublicKeyCredentialRequestOptions for the browser
     */
    @Transactional(readOnly = true)
    public String generateLoginOptions(Long tenantId) {
        AssertionRequest assertionRequest = relyingParty.startAssertion(
                StartAssertionOptions.builder().build());

        String challengeBase64 = assertionRequest.getPublicKeyCredentialRequestOptions()
                .getChallenge().getBase64Url();

        PasskeyChallengeStore.ChallengeMetadata metadata = new PasskeyChallengeStore.ChallengeMetadata(
                null, tenantId, PasskeyChallengeStore.OPERATION_LOGIN);
        challengeStore.store(challengeBase64, metadata);

        try {
            String optionsJson = assertionRequest.toCredentialsGetJson();
            challengeStore.storeOptions(challengeBase64, assertionRequest.toJson());
            return optionsJson;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new PasskeyAuthenticationException(
                    String.format(PasskeyAuthenticationException.ERROR_AUTHENTICATION_FAILED,
                            ERROR_OPTIONS_SERIALIZATION_FAILED), e);
        }
    }

    /**
     * Validates the assertion response, updates sign count, and issues a JWT
     * without fingerprint claims.
     *
     * @param responseJson the authenticator's assertion response (JSON)
     * @param tenantId     the tenant ID from the request
     * @return a {@link LoginResult} containing the access token, refresh token, and username
     * @throws PasskeyAuthenticationException if validation fails
     */
    @Transactional
    public LoginResult completeLogin(String responseJson, Long tenantId) {
        return completeLogin(responseJson, tenantId, null);
    }

    /**
     * Validates the assertion response, updates sign count, and issues a JWT,
     * merging optional fingerprint claims into the access token.
     *
     * @param responseJson      the authenticator's assertion response (JSON)
     * @param tenantId          the tenant ID from the request
     * @param fingerprintClaims optional fingerprint claims to embed in the JWT (may be null)
     * @return a {@link LoginResult} containing the access token, refresh token, and username
     * @throws PasskeyAuthenticationException if validation fails
     */
    @Transactional
    public LoginResult completeLogin(String responseJson, Long tenantId, Map<String, Object> fingerprintClaims) {
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc;
        try {
            pkc = PublicKeyCredential.parseAssertionResponseJson(responseJson);
        } catch (IOException e) {
            throw new PasskeyAuthenticationException(
                    String.format(PasskeyAuthenticationException.ERROR_AUTHENTICATION_FAILED,
                            ERROR_INVALID_RESPONSE), e);
        }

        String challengeBase64 = pkc.getResponse().getClientData().getChallenge().getBase64Url();
        challengeStore.consumeChallenge(challengeBase64);
        String storedOptionsJson = challengeStore.consumeOptions(challengeBase64);

        AssertionRequest assertionRequest;
        try {
            assertionRequest = AssertionRequest.fromJson(storedOptionsJson);
        } catch (IOException e) {
            throw new PasskeyAuthenticationException(
                    String.format(PasskeyAuthenticationException.ERROR_AUTHENTICATION_FAILED,
                            ERROR_OPTIONS_DESERIALIZATION_FAILED), e);
        }

        FinishAssertionOptions finishOptions = FinishAssertionOptions.builder()
                .request(assertionRequest)
                .response(pkc)
                .build();

        AssertionResult result;
        try {
            result = relyingParty.finishAssertion(finishOptions);
        } catch (AssertionFailedException e) {
            throw new PasskeyAuthenticationException(
                    String.format(PasskeyAuthenticationException.ERROR_AUTHENTICATION_FAILED,
                            ERROR_ASSERTION_VALIDATION_FAILED), e);
        }

        if (!result.isSuccess()) {
            throw new PasskeyAuthenticationException(
                    String.format(PasskeyAuthenticationException.ERROR_AUTHENTICATION_FAILED,
                            ERROR_ASSERTION_VALIDATION_FAILED));
        }

        PasskeyCredentialDataModel credential = credentialRepository
                .findByCredentialId(result.getCredentialId().getBytes())
                .orElseThrow(() -> new PasskeyAuthenticationException(
                        PasskeyAuthenticationException.ERROR_CREDENTIAL_NOT_FOUND));

        if (result.getSignatureCount() > 0
                && result.getSignatureCount() <= credential.getSignCount()) {
            throw new PasskeyAuthenticationException(
                    PasskeyAuthenticationException.ERROR_SIGN_COUNT_REGRESSION);
        }

        credential.setSignCount(result.getSignatureCount());
        credential.setLastUsedAt(LocalDateTime.now());
        credentialRepository.save(credential);

        InternalAuthDataModel auth = internalAuthRepository
                .findByInternalAuthId(credential.getUserId())
                .orElseThrow(() -> new PasskeyAuthenticationException(
                        String.format(PasskeyAuthenticationException.ERROR_AUTHENTICATION_FAILED,
                                ERROR_USER_NOT_FOUND)));

        Map<String, Object> claims = new HashMap<>();
        claims.put(JWT_CLAIM_ROLE, auth.getRole());
        claims.put(JwtTokenProvider.USER_ID_CLAIM, auth.getInternalAuthId());
        claims.put(JWT_CLAIM_AUTH_METHOD, AUTH_METHOD_PASSKEY);
        if (fingerprintClaims != null) {
            claims.putAll(fingerprintClaims);
        }

        String accessToken = jwtTokenProvider.createAccessToken(auth.getUsername(), tenantId, claims);
        String jti = jwtTokenProvider.getJti(accessToken);

        akademiaPlusRedisSessionStore.storeSession(
                jti,
                auth.getUsername(),
                tenantId,
                Duration.ofMillis(jwtTokenProvider.getAccessTokenValidityInMs()));

        String familyId = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(auth.getUsername(), tenantId, familyId);

        String refreshTokenHash = hashingService.generateHash(refreshToken);
        RefreshTokenDataModel refreshTokenEntity = applicationContext.getBean(RefreshTokenDataModel.class);
        refreshTokenEntity.setTenantId(tenantId);
        refreshTokenEntity.setTokenHash(refreshTokenHash);
        refreshTokenEntity.setFamilyId(familyId);
        refreshTokenEntity.setUserId(auth.getInternalAuthId());
        refreshTokenEntity.setUsername(auth.getUsername());
        refreshTokenEntity.setExpiresAt(
                Instant.ofEpochMilli(jwtTokenProvider.getClaims(refreshToken).getExpiration().getTime()));

        refreshTokenRepository.save(refreshTokenEntity);

        return new LoginResult(accessToken, refreshToken, auth.getUsername());
    }

    /**
     * Resolves an existing user handle from the user's passkey credentials,
     * or generates a new random one if no credentials exist.
     *
     * @param userId the user ID
     * @return the user handle bytes
     */
    private byte[] resolveOrGenerateUserHandle(Long userId) {
        List<PasskeyCredentialDataModel> existing = credentialRepository.findByUserId(userId);
        if (!existing.isEmpty()) {
            return existing.getFirst().getUserHandle();
        }
        byte[] userHandle = new byte[USER_HANDLE_LENGTH];
        new SecureRandom().nextBytes(userHandle);
        return userHandle;
    }
}
