/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.domain;

import com.akademiaplus.usecases.exceptions.InvalidBootstrapTokenException;
import com.akademiaplus.usecases.exceptions.TokenAlreadyUsedException;
import com.akademiaplus.usecases.exceptions.TokenCnMismatchException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Manages the lifecycle of bootstrap enrollment tokens.
 *
 * <p>Tokens are persisted as JSON on the {@code ca_certs} volume. On first startup,
 * the manifest is seeded from a Docker secret or generated fresh for each allowed CN.
 * On subsequent startups, the persisted manifest (which tracks used/unused state) is loaded.
 */
@Slf4j
public class TokenManifest {

    public static final String INVALID_TOKEN_MSG = "Bootstrap token not found or invalid";
    public static final String TOKEN_ALREADY_USED_MSG = "Bootstrap token has already been used";
    public static final String TOKEN_CN_MISMATCH_MSG = "Token bound CN does not match the requested common name";

    private static final int TOKEN_BYTE_LENGTH = 32;

    @Getter
    @JsonProperty("tokens")
    private List<BootstrapToken> tokens;

    private Path manifestPath;
    private ObjectMapper objectMapper;

    public TokenManifest() {
        this.tokens = new ArrayList<>();
    }

    public TokenManifest(Path manifestPath, ObjectMapper objectMapper) {
        this.tokens = new ArrayList<>();
        this.manifestPath = manifestPath;
        this.objectMapper = objectMapper;
    }

    /**
     * Loads a token manifest from the given JSON file path.
     *
     * @param filePath     path to the JSON manifest file
     * @param objectMapper Jackson ObjectMapper for deserialization
     * @return loaded manifest with transient fields initialized
     * @throws IOException if the file cannot be read or parsed
     */
    public static TokenManifest loadFromFile(Path filePath, ObjectMapper objectMapper) throws IOException {
        TokenManifestWrapper wrapper = objectMapper.readValue(filePath.toFile(), TokenManifestWrapper.class);
        TokenManifest manifest = new TokenManifest(filePath, objectMapper);
        manifest.tokens = wrapper.tokens != null ? wrapper.tokens : new ArrayList<>();
        return manifest;
    }

    /**
     * Initializes the manifest path and ObjectMapper after deserialization or creation.
     *
     * @param manifestPath path where this manifest should be persisted
     * @param objectMapper Jackson ObjectMapper for serialization
     */
    public void init(Path manifestPath, ObjectMapper objectMapper) {
        this.manifestPath = manifestPath;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates a token for the given common name.
     *
     * @param token token string to validate
     * @param cn    common name the requesting service claims
     * @return the matching {@link BootstrapToken} if valid
     * @throws InvalidBootstrapTokenException if the token does not exist
     * @throws TokenAlreadyUsedException      if the token has already been consumed
     * @throws TokenCnMismatchException       if the token's bound CN differs from {@code cn}
     */
    public synchronized BootstrapToken validate(String token, String cn) {
        BootstrapToken found = tokens.stream()
                .filter(t -> t.getToken().equals(token))
                .findFirst()
                .orElseThrow(() -> new InvalidBootstrapTokenException(INVALID_TOKEN_MSG));

        if (found.isUsed()) {
            throw new TokenAlreadyUsedException(TOKEN_ALREADY_USED_MSG);
        }
        if (!found.getBoundCN().equals(cn)) {
            throw new TokenCnMismatchException(TOKEN_CN_MISMATCH_MSG);
        }
        return found;
    }

    /**
     * Marks the given token as used and persists the updated manifest.
     *
     * @param token token string to invalidate
     */
    public synchronized void invalidate(String token) {
        tokens.stream()
                .filter(t -> t.getToken().equals(token))
                .findFirst()
                .ifPresent(t -> t.setUsed(true));
        persist();
    }

    /**
     * Returns true if there is at least one unused token bound to the given CN.
     *
     * @param cn common name to check
     * @return true if an unused token exists for this CN
     */
    public boolean hasUnusedTokenForCn(String cn) {
        return tokens.stream()
                .anyMatch(t -> t.getBoundCN().equals(cn) && !t.isUsed());
    }

    /**
     * Generates a new cryptographically random token bound to the given CN and
     * adds it to the manifest. Does NOT persist automatically — call {@link #persist()} afterwards.
     *
     * @param cn common name to bind the new token to
     */
    public synchronized void generateToken(String cn) {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        new SecureRandom().nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        tokens.add(new BootstrapToken(token, cn, Instant.now().toString(), false));
        log.info("Generated bootstrap token for CN: {}", cn);
    }

    /**
     * Persists the current manifest state to disk.
     */
    public synchronized void persist() {
        if (manifestPath == null || objectMapper == null) {
            log.warn("TokenManifest not initialized with path/objectMapper — skipping persist");
            return;
        }
        try {
            TokenManifestWrapper wrapper = new TokenManifestWrapper();
            wrapper.tokens = this.tokens;
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), wrapper);
        } catch (IOException e) {
            log.error("Failed to persist token manifest to {}: {}", manifestPath, e.getMessage());
        }
    }

    /**
     * Internal JSON wrapper matching the manifest file structure.
     */
    private static class TokenManifestWrapper {
        @JsonProperty("tokens")
        public List<BootstrapToken> tokens;
    }
}
