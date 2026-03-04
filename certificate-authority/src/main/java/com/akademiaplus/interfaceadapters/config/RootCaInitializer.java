/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters.config;

import com.akademiaplus.usecases.domain.CertificateAuthority;
import com.akademiaplus.usecases.domain.TokenManifest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Post-startup initializer for the CA service.
 *
 * <p>Generates bootstrap enrollment tokens for any allowed CN that does not already
 * have an unused token in the manifest. Logs the CA fingerprint for operator verification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RootCaInitializer implements ApplicationRunner {

    private final CertificateAuthority certificateAuthority;
    private final TokenManifest tokenManifest;

    @Value("#{'${ca.allowed-common-names}'.split(',')}")
    private List<String> allowedCommonNames;

    @SuppressWarnings("java:S112") // ApplicationRunner.run() declares throws Exception
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logCaFingerprint();
        generateMissingTokens();
    }

    private void logCaFingerprint() throws java.security.GeneralSecurityException {
        byte[] encoded = certificateAuthority.getCertificate().getEncoded();
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(encoded);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02X:", b));
        }
        log.info("CA Service ready. Root CA SHA-256 fingerprint: {}",
                hex.toString().replaceAll(":$", ""));
        log.info("CA Subject: {}", certificateAuthority.getCertificate().getSubjectX500Principal());
    }

    private void generateMissingTokens() {
        boolean generated = false;
        for (String cn : allowedCommonNames) {
            String trimmedCn = cn.trim();
            if (!tokenManifest.hasUnusedTokenForCn(trimmedCn)) {
                tokenManifest.generateToken(trimmedCn);
                generated = true;
            }
        }
        if (generated) {
            tokenManifest.persist();
            log.info("Token manifest updated. Unused tokens available for all allowed CNs.");
        } else {
            log.info("Token manifest is complete. All allowed CNs have unused tokens.");
        }
    }
}
