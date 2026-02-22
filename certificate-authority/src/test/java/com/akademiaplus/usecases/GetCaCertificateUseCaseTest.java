/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.usecases.domain.CertificateAuthority;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetCaCertificateUseCase Tests")
class GetCaCertificateUseCaseTest {

    private static final String PROVIDER_BC = "BC";
    private static final String EC_CURVE = "secp384r1";
    private static final String SIGNING_ALGORITHM = "SHA384withECDSA";
    private static final String CA_SUBJECT = "CN=AkademiaPlus CA,O=ElatusDev";
    private static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_FOOTER = "-----END CERTIFICATE-----";

    @TempDir
    Path tempDir;

    private GetCaCertificateUseCase useCase;

    @BeforeAll
    static void registerBouncyCastle() {
        if (Security.getProvider(PROVIDER_BC) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        CertificateAuthority ca = buildTestCa();
        useCase = new GetCaCertificateUseCase(ca);
    }

    private CertificateAuthority buildTestCa() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", PROVIDER_BC);
        keyGen.initialize(new ECGenParameterSpec(EC_CURVE));
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name name = new X500Name(CA_SUBJECT);
        Instant now = Instant.now();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE,
                Date.from(now), Date.from(now.plus(Duration.ofDays(3650))),
                name, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM)
                .setProvider(PROVIDER_BC).build(keyPair.getPrivate());
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(PROVIDER_BC).getCertificate(builder.build(signer));

        Path serialFile = tempDir.resolve("serial.txt");
        Files.writeString(serialFile, "2");
        return new CertificateAuthority(keyPair, cert, 2L, serialFile);
    }

    @Nested
    @DisplayName("CA Certificate Retrieval Tests")
    class CaCertificateRetrievalTests {

        @Test
        @DisplayName("Should return PEM-encoded CA certificate when CA is initialized")
        void shouldReturnPemEncodedCaCert_whenCaInitialized() {
            // When
            String pem = useCase.getPemEncodedCaCertificate();

            // Then
            assertThat(pem).isNotBlank();
            assertThat(pem).contains(PEM_HEADER);
            assertThat(pem).contains(PEM_FOOTER);
        }

        @Test
        @DisplayName("Should return certificate containing correct subject when CA is initialized")
        void shouldReturnCertificateWithCorrectSubject_whenCaInitialized() {
            // When
            String pem = useCase.getPemEncodedCaCertificate();

            // Then
            assertThat(pem).contains(PEM_HEADER);
            // PEM is valid and can be used as truststore root
            assertThat(pem.trim()).endsWith(PEM_FOOTER);
        }

        @Test
        @DisplayName("Should return same certificate on repeated calls when CA is initialized")
        void shouldReturnSameCertificate_whenCalledMultipleTimes() {
            // When
            String first = useCase.getPemEncodedCaCertificate();
            String second = useCase.getPemEncodedCaCertificate();

            // Then
            assertThat(first).isEqualTo(second);
        }
    }
}
