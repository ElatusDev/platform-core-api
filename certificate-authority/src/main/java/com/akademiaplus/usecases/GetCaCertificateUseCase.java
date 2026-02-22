/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.usecases.domain.CertificateAuthority;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.stereotype.Service;

import java.io.StringWriter;

/**
 * Returns the PEM-encoded root CA certificate for trust bootstrapping.
 *
 * <p>The {@code GET /ca/ca.crt} endpoint serves this response unauthenticated so that
 * enrolling services can fetch the CA root before they have a client certificate.
 */
@Service
@RequiredArgsConstructor
public class GetCaCertificateUseCase {

    private final CertificateAuthority certificateAuthority;

    /**
     * Returns the PEM-encoded root CA certificate.
     *
     * @return PEM string (includes BEGIN/END CERTIFICATE headers)
     */
    public String getPemEncodedCaCertificate() {
        try {
            StringWriter sw = new StringWriter();
            try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
                writer.writeObject(certificateAuthority.getCertificate());
            }
            return sw.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode CA certificate as PEM: " + e.getMessage(), e);
        }
    }
}
