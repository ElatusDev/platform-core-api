/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class KeyLoader {
    
    private KeyLoader(){}

    public static KeyPair loadKeyPair(String keystorePath, String password, String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12"); // or "JKS"
        try (FileInputStream in = new FileInputStream(keystorePath)) {
            keyStore.load(in, password.toCharArray());
        }

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
        Certificate cert = keyStore.getCertificate(alias);
        return new KeyPair(cert.getPublicKey(), privateKey);
    }
}