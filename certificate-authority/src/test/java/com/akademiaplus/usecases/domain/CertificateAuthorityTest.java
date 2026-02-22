/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.domain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CertificateAuthority Domain Tests")
class CertificateAuthorityTest {

    private static final String PROVIDER_BC = "BC";
    private static final String EC_CURVE = "secp384r1";
    private static final String SIGNING_ALGORITHM = "SHA384withECDSA";
    private static final String CA_SUBJECT = "CN=Test CA,O=TestOrg";
    private static final long INITIAL_SERIAL = 2L;
    private static final String SERIAL_FILE_NAME = "serial.txt";

    @BeforeAll
    static void registerBouncyCastle() {
        if (Security.getProvider(PROVIDER_BC) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @TempDir
    Path tempDir;

    private CertificateAuthority buildTestCa(long initialSerial) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", PROVIDER_BC);
        keyGen.initialize(new ECGenParameterSpec(EC_CURVE));
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name name = new X500Name(CA_SUBJECT);
        Instant now = Instant.now();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE,
                Date.from(now), Date.from(now.plus(Duration.ofDays(1))),
                name, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM)
                .setProvider(PROVIDER_BC).build(keyPair.getPrivate());
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(PROVIDER_BC).getCertificate(builder.build(signer));

        Path serialFile = tempDir.resolve(SERIAL_FILE_NAME);
        Files.writeString(serialFile, String.valueOf(initialSerial));
        return new CertificateAuthority(keyPair, cert, initialSerial, serialFile);
    }

    @Nested
    @DisplayName("CA Key Pair and Certificate Tests")
    class CaKeyPairAndCertificateTests {

        @Test
        @DisplayName("Should expose ECDSA private key when CA is initialized")
        void shouldExposeEcdsaPrivateKey_whenCaIsInitialized() throws Exception {
            // Given
            CertificateAuthority ca = buildTestCa(INITIAL_SERIAL);

            // When
            var privateKey = ca.getPrivateKey();

            // Then
            assertThat(privateKey).isNotNull();
            assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
        }

        @Test
        @DisplayName("Should expose X509 certificate when CA is initialized")
        void shouldExposeX509Certificate_whenCaIsInitialized() throws Exception {
            // Given
            CertificateAuthority ca = buildTestCa(INITIAL_SERIAL);

            // When
            X509Certificate cert = ca.getCertificate();

            // Then
            assertThat(cert).isNotNull();
            assertThat(cert.getSubjectX500Principal().getName()).contains("CN=Test CA");
        }

        @Test
        @DisplayName("Should return public key matching the certificate when CA is initialized")
        void shouldReturnPublicKeyMatchingCertificate_whenCaIsInitialized() throws Exception {
            // Given
            CertificateAuthority ca = buildTestCa(INITIAL_SERIAL);

            // When
            var publicKey = ca.getPublicKey();

            // Then
            assertThat(publicKey).isEqualTo(ca.getCertificate().getPublicKey());
        }
    }

    @Nested
    @DisplayName("Serial Counter Tests")
    class SerialCounterTests {

        @Test
        @DisplayName("Should return initial serial when first nextSerial is called")
        void shouldReturnInitialSerial_whenFirstNextSerialIsCalled() throws Exception {
            // Given
            CertificateAuthority ca = buildTestCa(INITIAL_SERIAL);

            // When
            BigInteger serial = ca.nextSerial();

            // Then
            assertThat(serial).isEqualTo(BigInteger.valueOf(INITIAL_SERIAL));
        }

        @Test
        @DisplayName("Should increment serial number when cert is issued")
        void shouldIncrementSerialNumber_whenCertIssued() throws Exception {
            // Given
            CertificateAuthority ca = buildTestCa(INITIAL_SERIAL);

            // When
            BigInteger first = ca.nextSerial();
            BigInteger second = ca.nextSerial();
            BigInteger third = ca.nextSerial();

            // Then
            assertThat(second).isEqualTo(first.add(BigInteger.ONE));
            assertThat(third).isEqualTo(second.add(BigInteger.ONE));
        }

        @Test
        @DisplayName("Should persist updated serial to disk when nextSerial is called")
        void shouldPersistUpdatedSerial_whenNextSerialIsCalled() throws Exception {
            // Given
            Path serialFile = tempDir.resolve(SERIAL_FILE_NAME);
            Files.writeString(serialFile, String.valueOf(INITIAL_SERIAL));
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", PROVIDER_BC);
            keyGen.initialize(new ECGenParameterSpec(EC_CURVE));
            KeyPair keyPair = keyGen.generateKeyPair();
            X500Name name = new X500Name(CA_SUBJECT);
            Instant now = Instant.now();
            X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    name, BigInteger.ONE,
                    Date.from(now), Date.from(now.plus(Duration.ofDays(1))),
                    name, keyPair.getPublic());
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM)
                    .setProvider(PROVIDER_BC).build(keyPair.getPrivate());
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(PROVIDER_BC).getCertificate(builder.build(signer));
            CertificateAuthority ca = new CertificateAuthority(keyPair, cert, INITIAL_SERIAL, serialFile);

            // When
            ca.nextSerial();

            // Then — file should contain INITIAL_SERIAL + 1
            long persisted = Long.parseLong(Files.readString(serialFile).trim());
            assertThat(persisted).isEqualTo(INITIAL_SERIAL + 1);
        }
    }
}
