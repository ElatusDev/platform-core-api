/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of JWT signing public keys submitted by enrolled services.
 *
 * <p>Persisted as JSON to the {@code ca_certs} volume so registrations survive
 * CA restarts. The file is written atomically on every registration.
 *
 * <p>Each entry maps a {@code kid} (key ID, typically the service CN) to the
 * base64-encoded DER SubjectPublicKeyInfo used when building the JWKS response.
 */
@Slf4j
public class JwksRegistry {

    /** Wire representation stored on disk and returned via JWKS endpoint. */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JwksEntry {
        private String kid;
        private String alg;
        /** Base64-encoded DER SubjectPublicKeyInfo. */
        private String publicKeyBase64;

        public JwksEntry() {}

        public JwksEntry(String kid, String alg, String publicKeyBase64) {
            this.kid = kid;
            this.alg = alg;
            this.publicKeyBase64 = publicKeyBase64;
        }
    }

    private final Map<String, JwksEntry> entries = new ConcurrentHashMap<>();
    private final Path persistPath;
    private final ObjectMapper objectMapper;

    public JwksRegistry(Path persistPath, ObjectMapper objectMapper) {
        this.persistPath = persistPath;
        this.objectMapper = objectMapper;
    }

    /** Register or replace a key entry. Persists immediately. */
    public void register(String kid, String alg, String publicKeyBase64) {
        entries.put(kid, new JwksEntry(kid, alg, publicKeyBase64));
        log.info("JWKS registry: registered public key for kid={} alg={}", kid, alg);
        persist();
    }

    /** Returns all current entries as an immutable snapshot. */
    public List<JwksEntry> getEntries() {
        return List.copyOf(entries.values());
    }

    /** Decode raw DER bytes from a registry entry. */
    public byte[] decodePublicKey(JwksEntry entry) {
        return Base64.getDecoder().decode(entry.getPublicKeyBase64());
    }

    public void persist() {
        try {
            List<JwksEntry> snapshot = new ArrayList<>(entries.values());
            objectMapper.writeValue(persistPath.toFile(), snapshot);
        } catch (IOException e) {
            log.warn("Failed to persist JWKS registry to {}: {}", persistPath, e.getMessage());
        }
    }

    public static JwksRegistry load(Path persistPath, ObjectMapper objectMapper) {
        JwksRegistry registry = new JwksRegistry(persistPath, objectMapper);
        if (Files.exists(persistPath)) {
            try {
                JwksEntry[] loaded = objectMapper.readValue(persistPath.toFile(), JwksEntry[].class);
                for (JwksEntry entry : loaded) {
                    registry.entries.put(entry.getKid(), entry);
                }
                log.info("JWKS registry loaded {} entries from {}", loaded.length, persistPath);
            } catch (IOException e) {
                log.warn("Could not load JWKS registry from {} — starting empty: {}",
                        persistPath, e.getMessage());
            }
        }
        return registry;
    }
}
