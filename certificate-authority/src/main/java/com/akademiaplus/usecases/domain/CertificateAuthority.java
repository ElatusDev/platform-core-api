/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.domain;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Domain object representing the Certificate Authority state.
 *
 * <p>Holds the ECDSA P-384 key pair, self-signed root certificate, and monotonic
 * serial counter. The serial counter is persisted to disk after each issuance to
 * survive CA service restarts.
 */
@Slf4j
public class CertificateAuthority {

    public static final String CA_KEY_ALIAS = "ca-key";

    private final KeyPair keyPair;
    /**
     * -- GETTER --
     *  Returns the self-signed root CA X.509 certificate.
     *
     * @return root CA certificate
     */
    @Getter
    private final X509Certificate certificate;
    private final AtomicLong serialCounter;
    private final Path serialFile;

    public CertificateAuthority(KeyPair keyPair, X509Certificate certificate,
                                long initialSerial, Path serialFile) {
        this.keyPair = keyPair;
        this.certificate = certificate;
        this.serialCounter = new AtomicLong(initialSerial);
        this.serialFile = serialFile;
    }

    /**
     * Returns the next monotonically increasing serial number and persists the
     * updated counter to disk.
     *
     * @return next serial number for use in a signed certificate
     */
    public synchronized BigInteger nextSerial() {
        long next = serialCounter.getAndIncrement();
        persistSerial(serialCounter.get());
        return BigInteger.valueOf(next);
    }

    /**
     * Returns the CA's ECDSA P-384 private key used for signing certificates.
     *
     * @return CA private key
     */
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    /**
     * Returns the CA's ECDSA P-384 public key.
     *
     * @return CA public key
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    private void persistSerial(long value) {
        try {
            Files.writeString(serialFile, String.valueOf(value));
        } catch (IOException e) {
            log.warn("Failed to persist serial counter to {}: {}", serialFile, e.getMessage());
        }
    }
}
