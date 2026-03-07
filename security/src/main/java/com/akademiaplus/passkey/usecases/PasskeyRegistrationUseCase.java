/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.usecases;

import com.akademiaplus.passkey.exceptions.PasskeyRegistrationException;
import com.akademiaplus.passkey.interfaceadapters.PasskeyCredentialJpaRepository;
import com.akademiaplus.security.PasskeyCredentialDataModel;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Handles WebAuthn passkey registration within the security module.
 *
 * <p>Generates registration options (challenge, relying party info, user info)
 * and validates the authenticator's registration response. On successful
 * validation, stores the credential in the database.</p>
 *
 * <p>This use case is security-scoped — it does not cross module boundaries.
 * The {@code PasskeyAuthenticationUseCase} in the application module
 * orchestrates cross-module concerns (user lookup, JWT issuance).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class PasskeyRegistrationUseCase {

    /** Error message when registration validation fails. */
    public static final String ERROR_REGISTRATION_VALIDATION_FAILED = "Registration validation failed";

    /** Error message when the registration response cannot be parsed. */
    public static final String ERROR_INVALID_RESPONSE = "Invalid registration response format";

    /** Error message when options serialization fails. */
    public static final String ERROR_OPTIONS_SERIALIZATION_FAILED = "Failed to serialize registration options";

    /** Error message when stored options cannot be deserialized. */
    public static final String ERROR_OPTIONS_DESERIALIZATION_FAILED = "Failed to deserialize stored registration options";

    private final RelyingParty relyingParty;
    private final PasskeyChallengeStore challengeStore;
    private final PasskeyCredentialJpaRepository credentialRepository;
    private final ApplicationContext applicationContext;

    /**
     * Constructs the registration use case.
     *
     * @param relyingParty        the WebAuthn relying party
     * @param challengeStore      the challenge store
     * @param credentialRepository the credential repository
     * @param applicationContext  the Spring application context for entity creation
     */
    public PasskeyRegistrationUseCase(RelyingParty relyingParty,
                                       PasskeyChallengeStore challengeStore,
                                       PasskeyCredentialJpaRepository credentialRepository,
                                       ApplicationContext applicationContext) {
        this.relyingParty = relyingParty;
        this.challengeStore = challengeStore;
        this.credentialRepository = credentialRepository;
        this.applicationContext = applicationContext;
    }

    /**
     * Generates registration options for a new passkey.
     *
     * @param userId     the authenticated user's ID
     * @param username   the user's display name / email
     * @param userHandle the random user handle bytes
     * @param tenantId   the tenant ID
     * @return PublicKeyCredentialCreationOptions serialized as JSON for the browser
     */
    @Transactional
    public String generateRegistrationOptions(Long userId, String username, byte[] userHandle, Long tenantId) {
        UserIdentity userIdentity = UserIdentity.builder()
                .name(username)
                .displayName(username)
                .id(new ByteArray(userHandle))
                .build();

        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(userIdentity)
                .build();

        PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);
        String challengeBase64 = creationOptions.getChallenge().getBase64Url();

        PasskeyChallengeStore.ChallengeMetadata metadata = new PasskeyChallengeStore.ChallengeMetadata(
                userId, tenantId, PasskeyChallengeStore.OPERATION_REGISTER);
        challengeStore.store(challengeBase64, metadata);

        try {
            String optionsJson = creationOptions.toCredentialsCreateJson();
            challengeStore.storeOptions(challengeBase64, creationOptions.toJson());
            return optionsJson;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new PasskeyRegistrationException(
                    String.format(PasskeyRegistrationException.ERROR_REGISTRATION_FAILED, ERROR_OPTIONS_SERIALIZATION_FAILED), e);
        }
    }

    /**
     * Validates the registration response and stores the credential.
     *
     * @param responseJson the authenticator's registration response (JSON)
     * @param tenantId     the tenant ID
     * @param displayName  human-readable name for the credential
     * @return the stored credential's display name
     * @throws PasskeyRegistrationException if validation fails
     */
    @Transactional
    public String completeRegistration(String responseJson, Long tenantId, String displayName) {
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc;
        try {
            pkc = PublicKeyCredential.parseRegistrationResponseJson(responseJson);
        } catch (IOException e) {
            throw new PasskeyRegistrationException(
                    String.format(PasskeyRegistrationException.ERROR_REGISTRATION_FAILED, ERROR_INVALID_RESPONSE), e);
        }

        String challengeBase64 = pkc.getResponse().getClientData().getChallenge().getBase64Url();
        PasskeyChallengeStore.ChallengeMetadata metadata = challengeStore.consumeChallenge(challengeBase64);
        String storedOptionsJson = challengeStore.consumeOptions(challengeBase64);

        PublicKeyCredentialCreationOptions creationOptions;
        try {
            creationOptions = PublicKeyCredentialCreationOptions.fromJson(storedOptionsJson);
        } catch (IOException e) {
            throw new PasskeyRegistrationException(
                    String.format(PasskeyRegistrationException.ERROR_REGISTRATION_FAILED, ERROR_OPTIONS_DESERIALIZATION_FAILED), e);
        }

        FinishRegistrationOptions finishOptions = FinishRegistrationOptions.builder()
                .request(creationOptions)
                .response(pkc)
                .build();

        RegistrationResult result;
        try {
            result = relyingParty.finishRegistration(finishOptions);
        } catch (RegistrationFailedException e) {
            throw new PasskeyRegistrationException(
                    String.format(PasskeyRegistrationException.ERROR_REGISTRATION_FAILED, ERROR_REGISTRATION_VALIDATION_FAILED), e);
        }

        PasskeyCredentialDataModel credential = applicationContext.getBean(PasskeyCredentialDataModel.class);
        credential.setUserId(metadata.userId());
        credential.setCredentialId(result.getKeyId().getId().getBytes());
        credential.setPublicKey(result.getPublicKeyCose().getBytes());
        credential.setSignCount(result.getSignatureCount());
        java.util.SortedSet<AuthenticatorTransport> transports = pkc.getResponse().getTransports();
        credential.setTransports(transports.isEmpty() ? null :
                transports.stream()
                        .map(AuthenticatorTransport::getId)
                        .collect(Collectors.joining(",")));
        credential.setLastUsedAt(null);
        credential.setDisplayName(displayName);
        credential.setUserHandle(creationOptions.getUser().getId().getBytes());

        credentialRepository.save(credential);
        return displayName;
    }
}
