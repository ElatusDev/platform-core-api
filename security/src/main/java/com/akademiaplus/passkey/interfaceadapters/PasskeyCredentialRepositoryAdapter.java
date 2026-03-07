/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.security.PasskeyCredentialDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter between Yubico's {@link CredentialRepository} interface and the JPA repository.
 *
 * <p>Translates WebAuthn credential lookups into JPA queries on the
 * passkey_credentials table. All lookups are tenant-scoped via Hibernate filters.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class PasskeyCredentialRepositoryAdapter implements CredentialRepository {

    /** Separator for transport values stored in the database. */
    public static final String TRANSPORT_SEPARATOR = ",";

    private final PasskeyCredentialJpaRepository jpaRepository;
    private final InternalAuthRepository internalAuthRepository;
    private final HashingService hashingService;

    /**
     * Constructs the adapter with required dependencies.
     *
     * @param jpaRepository          the passkey credential JPA repository
     * @param internalAuthRepository the internal auth repository for username resolution
     * @param hashingService         the hashing service for username hash lookup
     */
    public PasskeyCredentialRepositoryAdapter(PasskeyCredentialJpaRepository jpaRepository,
                                               InternalAuthRepository internalAuthRepository,
                                               HashingService hashingService) {
        this.jpaRepository = jpaRepository;
        this.internalAuthRepository = internalAuthRepository;
        this.hashingService = hashingService;
    }

    /**
     * Returns the credential IDs registered for a given username.
     *
     * @param username the username
     * @return set of credential descriptors
     */
    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return resolveUserId(username)
                .map(userId -> jpaRepository.findByUserId(userId).stream()
                        .map(cred -> PublicKeyCredentialDescriptor.builder()
                                .id(new ByteArray(cred.getCredentialId()))
                                .build())
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    /**
     * Returns the user handle for a given username.
     *
     * @param username the username
     * @return the user handle, if found
     */
    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return resolveUserId(username)
                .flatMap(userId -> jpaRepository.findByUserId(userId).stream()
                        .findFirst()
                        .map(cred -> new ByteArray(cred.getUserHandle())));
    }

    /**
     * Returns the username for a given user handle.
     *
     * @param userHandle the user handle bytes
     * @return the username, if found
     */
    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        List<PasskeyCredentialDataModel> credentials = jpaRepository.findByUserHandle(userHandle.getBytes());
        if (credentials.isEmpty()) {
            return Optional.empty();
        }
        Long userId = credentials.getFirst().getUserId();
        return resolveUsername(userId);
    }

    /**
     * Looks up a specific registered credential by credential ID and user handle.
     *
     * @param credentialId the credential ID
     * @param userHandle   the user handle
     * @return the registered credential, if found
     */
    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return jpaRepository.findByCredentialId(credentialId.getBytes())
                .filter(cred -> Arrays.equals(cred.getUserHandle(), userHandle.getBytes()))
                .map(this::toRegisteredCredential);
    }

    /**
     * Looks up all registered credentials with a given credential ID.
     *
     * @param credentialId the credential ID
     * @return set of registered credentials
     */
    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return jpaRepository.findByCredentialId(credentialId.getBytes())
                .map(this::toRegisteredCredential)
                .map(Set::of)
                .orElse(Collections.emptySet());
    }

    /**
     * Converts a JPA entity to a Yubico RegisteredCredential.
     *
     * @param entity the passkey credential entity
     * @return the registered credential
     */
    private RegisteredCredential toRegisteredCredential(PasskeyCredentialDataModel entity) {
        return RegisteredCredential.builder()
                .credentialId(new ByteArray(entity.getCredentialId()))
                .userHandle(new ByteArray(entity.getUserHandle()))
                .publicKeyCose(new ByteArray(entity.getPublicKey()))
                .signatureCount(entity.getSignCount())
                .build();
    }

    /**
     * Resolves a username to a user ID via the internal auth repository.
     *
     * @param username the username
     * @return the user ID, if found
     */
    private Optional<Long> resolveUserId(String username) {
        String usernameHash = hashingService.generateHash(username);
        return internalAuthRepository.findByUsernameHash(usernameHash)
                .map(InternalAuthDataModel::getInternalAuthId);
    }

    /**
     * Resolves a user ID to a username via the internal auth repository.
     *
     * @param userId the user ID
     * @return the username, if found
     */
    private Optional<String> resolveUsername(Long userId) {
        return internalAuthRepository.findByInternalAuthId(userId)
                .map(InternalAuthDataModel::getUsername);
    }
}
