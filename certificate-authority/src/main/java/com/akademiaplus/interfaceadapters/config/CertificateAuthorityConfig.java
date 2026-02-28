/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters.config;

import com.akademiaplus.usecases.domain.CertificateAuthority;
import com.akademiaplus.usecases.domain.JwksRegistry;
import com.akademiaplus.usecases.domain.TokenManifest;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.bc.BcPKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Initializes the Certificate Authority on startup.
 *
 * <p>On first run: generates an ECDSA P-384 root CA key pair, self-signs a root
 * certificate, and creates PKCS12 keystore and truststore files on the {@code ca_certs}
 * volume. On subsequent runs: loads the existing key and certificate from disk.
 *
 * <p>Bouncy Castle is registered as a JCE provider in {@link #registerBouncyCastle()}
 * which runs before any {@code @Bean} methods on this class.
 */
@Slf4j
@Configuration
public class CertificateAuthorityConfig {

    private static final String PROVIDER_BC = "BC";
    private static final String EC_ALGORITHM = "EC";
    private static final String EC_CURVE = "secp384r1";
    private static final String SIGNING_ALGORITHM = "SHA384withECDSA";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String FILE_CA_KEY = "/akademiaplus-ca.key";
    private static final String FILE_CA_CRT = "/akademiaplus-ca.crt";
    private static final String FILE_KEYSTORE = "/akademiaplus-ca-keystore.p12";
    private static final String FILE_TRUSTSTORE = "/akademiaplus-truststore.p12";
    private static final String FILE_SERIAL = "/akademiaplus-serial.txt";
    private static final String FILE_TOKENS = "/akademiaplus-tokens.json";
    private static final String FILE_JWKS = "/akademiaplus-jwks.json";
    private static final String FILE_SECRET_TOKENS = "/run/secrets/ca_bootstrap_tokens";
    private static final String TRUSTSTORE_CA_ALIAS = "akademiaplus-ca";
    private static final long SERIAL_ROOT_CA = 1L;
    private static final long SERIAL_COUNTER_INITIAL = 2L;

    @PostConstruct
    public void registerBouncyCastle() {
        if (Security.getProvider(PROVIDER_BC) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("Bouncy Castle JCE provider registered");
        }
    }

    /**
     * Provides the {@link CertificateAuthority} bean, generating or loading the root CA.
     *
     * @param certPath    absolute path to the certificate volume (e.g. {@code /certs})
     * @param keystorePass passphrase for PKCS12 keystore and truststore files
     * @param caSubject   X.500 distinguished name for the root CA certificate
     * @param validityDays validity period for the root CA certificate in days
     * @return initialized {@link CertificateAuthority}
     */
    @Bean
    public CertificateAuthority certificateAuthority(
            @Value("${ca.cert-path}") String certPath,
            @Value("${CA_KEYSTORE_PASS}") String keystorePass,
            @Value("${ca.root.subject}") String caSubject,
            @Value("${ca.root.validity-days:3650}") int validityDays) throws Exception {

        Files.createDirectories(Path.of(certPath));

        Path caKeyPath = Path.of(certPath + FILE_CA_KEY);
        Path caCrtPath = Path.of(certPath + FILE_CA_CRT);
        Path serialPath = Path.of(certPath + FILE_SERIAL);

        if (!Files.exists(caKeyPath) || !Files.exists(caCrtPath)) {
            log.info("No existing CA found. Generating new ECDSA P-384 root CA...");
            return generateAndPersistRootCa(caKeyPath, caCrtPath, serialPath, certPath, keystorePass, caSubject, validityDays);
        }

        log.info("Loading existing root CA from {}", certPath);
        return loadExistingCa(caKeyPath, caCrtPath, serialPath);
    }

    /**
     * Provides the {@link TokenManifest} bean, loading from the persisted volume or
     * Docker secret on first startup.
     *
     * @param manifestPath absolute path to the tokens manifest JSON file
     * @return initialized {@link TokenManifest}
     */
    @Bean
    public TokenManifest tokenManifest(
            @Value("${ca.token-manifest}") String manifestPath) {

        ObjectMapper objectMapper = new ObjectMapper();
        Path persistedPath = Path.of(manifestPath);
        Path secretPath = Path.of(FILE_SECRET_TOKENS);

        if (Files.exists(persistedPath)) {
            log.info("Loading token manifest from persisted volume: {}", persistedPath);
            TokenManifest manifest = TokenManifest.loadFromFile(persistedPath, objectMapper);
            manifest.init(persistedPath, objectMapper);
            return manifest;
        }

        if (Files.exists(secretPath)) {
            log.info("Loading token manifest from Docker secret: {}", secretPath);
            TokenManifest manifest = TokenManifest.loadFromFile(secretPath, objectMapper);
            manifest.init(persistedPath, objectMapper);
            manifest.persist();
            return manifest;
        }

        log.info("No token manifest found. Starting with empty manifest (tokens will be generated by RootCaInitializer)");
        return new TokenManifest(persistedPath, objectMapper);
    }

    /**
     * Provides the {@link JwksRegistry} bean, loading persisted entries from the
     * {@code ca_certs} volume if the file exists.
     *
     * @param certPath absolute path to the certificate volume (e.g. {@code /certs})
     * @return initialized {@link JwksRegistry}
     */
    @Bean
    public JwksRegistry jwksRegistry(
            @Value("${ca.cert-path}") String certPath) {
        Path jwksPath = Path.of(certPath + FILE_JWKS);
        return JwksRegistry.load(jwksPath, new ObjectMapper());
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private CertificateAuthority generateAndPersistRootCa(Path caKeyPath, Path caCrtPath,
            Path serialPath, String certPath, String keystorePass,
            String caSubject, int validityDays) throws Exception {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(EC_ALGORITHM, PROVIDER_BC);
        keyGen.initialize(new ECGenParameterSpec(EC_CURVE), new SecureRandom());
        KeyPair caKeyPair = keyGen.generateKeyPair();

        X500Name issuer = new X500Name(caSubject);
        Instant now = Instant.now();

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, BigInteger.valueOf(SERIAL_ROOT_CA),
                Date.from(now), Date.from(now.plus(Duration.ofDays(validityDays))),
                issuer, caKeyPair.getPublic());

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM)
                .setProvider(PROVIDER_BC).build(caKeyPair.getPrivate());

        X509Certificate caCert = new JcaX509CertificateConverter()
                .setProvider(PROVIDER_BC).getCertificate(builder.build(signer));

        savePem(caKeyPair.getPrivate(), caKeyPath);
        savePem(caCert, caCrtPath);
        Files.writeString(serialPath, String.valueOf(SERIAL_COUNTER_INITIAL));

        createKeystore(certPath + FILE_KEYSTORE, caKeyPair, caCert, keystorePass);
        createTruststore(certPath + FILE_TRUSTSTORE, caCert, keystorePass);

        log.info("Root CA generated. Subject: {}", caSubject);
        return new CertificateAuthority(caKeyPair, caCert, SERIAL_COUNTER_INITIAL, serialPath);
    }

    private CertificateAuthority loadExistingCa(Path caKeyPath, Path caCrtPath, Path serialPath) throws Exception {
        PrivateKey privateKey = loadPrivateKey(caKeyPath);

        X509Certificate caCert;
        try (PEMParser certParser = new PEMParser(new FileReader(caCrtPath.toFile()))) {
            org.bouncycastle.cert.X509CertificateHolder holder =
                    (org.bouncycastle.cert.X509CertificateHolder) certParser.readObject();
            caCert = new JcaX509CertificateConverter().setProvider(PROVIDER_BC).getCertificate(holder);
        }

        KeyPair keyPair = new KeyPair(caCert.getPublicKey(), privateKey);

        long serial = Files.exists(serialPath)
                ? Long.parseLong(Files.readString(serialPath).trim())
                : SERIAL_COUNTER_INITIAL;

        log.info("Root CA loaded. Serial counter at: {}", serial);
        return new CertificateAuthority(keyPair, caCert, serial, serialPath);
    }

    private PrivateKey loadPrivateKey(Path caKeyPath) throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(caKeyPath.toFile()))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(PROVIDER_BC);
            if (obj instanceof PEMKeyPair pemKeyPair) {
                return converter.getKeyPair(pemKeyPair).getPrivate();
            }
            if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo pki) {
                return converter.getPrivateKey(pki);
            }
            throw new IllegalStateException("Unexpected PEM object type in CA key file: " + obj.getClass());
        }
    }

    private void savePem(Object obj, Path path) throws IOException {
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(path.toFile()))) {
            writer.writeObject(obj);
        }
    }

    private void createKeystore(String keystorePath, KeyPair keyPair,
                                X509Certificate cert, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER_BC);
        ks.load(null, password.toCharArray());
        ks.setKeyEntry(CertificateAuthority.CA_KEY_ALIAS, keyPair.getPrivate(),
                password.toCharArray(), new Certificate[]{cert});
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(keystorePath)) {
            ks.store(fos, password.toCharArray());
        }
        log.info("CA keystore written to {}", keystorePath);
    }

    private void createTruststore(String truststorePath, X509Certificate caCert, String password) throws Exception {
        KeyStore ts = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER_BC);
        ts.load(null, password.toCharArray());
        ts.setCertificateEntry(TRUSTSTORE_CA_ALIAS, caCert);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(truststorePath)) {
            ts.store(fos, password.toCharArray());
        }
        log.info("CA truststore written to {}", truststorePath);
    }
}
