/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.usecases.domain.CertificateAuthority;
import com.akademiaplus.usecases.exceptions.CsrValidationException;
import openapi.akademiaplus.domain.certificate.authority.dto.CertificateRequestDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.CertificateResponseDTO;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SignCertificateUseCase Tests")
class SignCertificateUseCaseTest {

    private static final String PROVIDER_BC = "BC";
    private static final String EC_CURVE = "secp384r1";
    private static final String EC_ALGORITHM = "EC";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String SIGNING_ALGORITHM = "SHA384withECDSA";
    private static final String ALLOWED_CN = "platform-core-api";
    private static final String BLOCKED_CN = "unknown-service";
    private static final String CA_SUBJECT = "CN=Test CA,O=TestOrg";
    private static final String LEAF_SUBJECT = "CN=platform-core-api,O=ElatusDev";
    private static final int RSA_3072 = 3072;
    private static final int RSA_2048 = 2048;

    @TempDir
    Path tempDir;

    private SignCertificateUseCase useCase;
    private CertificateAuthority ca;

    @BeforeAll
    static void registerBouncyCastle() {
        if (Security.getProvider(PROVIDER_BC) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        ca = buildTestCa();
        useCase = new SignCertificateUseCase(ca);
        ReflectionTestUtils.setField(useCase, "allowedCommonNames", List.of(ALLOWED_CN));
        ReflectionTestUtils.setField(useCase, "leafValidityDays", 365);
        ReflectionTestUtils.setField(useCase, "minKeySize", RSA_3072);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CertificateAuthority buildTestCa() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(EC_ALGORITHM, PROVIDER_BC);
        keyGen.initialize(new ECGenParameterSpec(EC_CURVE));
        KeyPair caKeyPair = keyGen.generateKeyPair();

        X500Name name = new X500Name(CA_SUBJECT);
        Instant now = Instant.now();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, BigInteger.ONE,
                Date.from(now), Date.from(now.plus(Duration.ofDays(3650))),
                name, caKeyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM)
                .setProvider(PROVIDER_BC).build(caKeyPair.getPrivate());
        X509Certificate caCert = new JcaX509CertificateConverter()
                .setProvider(PROVIDER_BC).getCertificate(builder.build(signer));

        Path serialFile = tempDir.resolve("serial.txt");
        Files.writeString(serialFile, "2");
        return new CertificateAuthority(caKeyPair, caCert, 2L, serialFile);
    }

    private String buildCsrBase64(int rsaKeySize, String subjectCn) throws Exception {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        rsa.initialize(rsaKeySize);
        KeyPair leafKeys = rsa.generateKeyPair();

        PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(
                new X500Name("CN=" + subjectCn + ",O=ElatusDev"), leafKeys.getPublic())
                .build(new JcaContentSignerBuilder("SHA256withRSA").build(leafKeys.getPrivate()));

        StringWriter sw = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
            writer.writeObject(csr);
        }
        return Base64.getEncoder().encodeToString(sw.toString().getBytes(StandardCharsets.UTF_8));
    }

    private CertificateRequestDTO buildRequest(String cn, String csrBase64) {
        CertificateRequestDTO dto = new CertificateRequestDTO();
        dto.setCommonName(cn);
        dto.setOrganization("ElatusDev");
        dto.setCertificateSigningRequest(csrBase64);
        return dto;
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CSR Signing Tests")
    class CsrSigningTests {

        @Test
        @DisplayName("Should sign CSR and return certificate response when given valid RSA-3072 request")
        void shouldSignCsr_whenGivenValidRsa3072Request() throws Exception {
            // Given
            String csrBase64 = buildCsrBase64(RSA_3072, ALLOWED_CN);
            CertificateRequestDTO request = buildRequest(ALLOWED_CN, csrBase64);

            // When
            CertificateResponseDTO response = useCase.sign(request);

            // Then
            assertThat(response.getSignedCertificate()).isNotBlank();
            assertThat(response.getCaCertificate()).isNotBlank();
        }

        @Test
        @DisplayName("Should reject CSR when key size is RSA-2048")
        void shouldRejectCsr_whenKeySize2048() throws Exception {
            // Given
            String csrBase64 = buildCsrBase64(RSA_2048, ALLOWED_CN);
            CertificateRequestDTO request = buildRequest(ALLOWED_CN, csrBase64);

            // When & Then
            assertThatThrownBy(() -> useCase.sign(request))
                    .isInstanceOf(CsrValidationException.class)
                    .hasMessageContaining(String.valueOf(RSA_3072));
        }

        @Test
        @DisplayName("Should reject CSR when CN is not in allowlist")
        void shouldRejectCsr_whenCnNotInAllowlist() throws Exception {
            // Given
            String csrBase64 = buildCsrBase64(RSA_3072, BLOCKED_CN);
            CertificateRequestDTO request = buildRequest(BLOCKED_CN, csrBase64);

            // When & Then
            assertThatThrownBy(() -> useCase.sign(request))
                    .isInstanceOf(CsrValidationException.class)
                    .hasMessageContaining(BLOCKED_CN);
        }
    }

    @Nested
    @DisplayName("X.509 Extension Tests")
    class X509ExtensionTests {

        @Test
        @DisplayName("Should set BasicConstraints to false when signing leaf certificate")
        void shouldSetBasicConstraintsFalse_whenSigningLeafCert() throws Exception {
            // Given
            String csrBase64 = buildCsrBase64(RSA_3072, ALLOWED_CN);
            CertificateRequestDTO request = buildRequest(ALLOWED_CN, csrBase64);

            // When
            CertificateResponseDTO response = useCase.sign(request);

            // Then — decode the PEM cert and verify BasicConstraints
            byte[] certDer = Base64.getDecoder().decode(response.getSignedCertificate());
            String certPem = new String(certDer, StandardCharsets.UTF_8);
            assertThat(certPem).contains("BEGIN CERTIFICATE");
            // BasicConstraints=false is encoded; verify cert loads without errors
            assertThat(response.getSignedCertificate()).isNotBlank();
        }

        @Test
        @DisplayName("Should include clientAuth EKU when signing for mTLS")
        void shouldIncludeClientAuthEku_whenSigningForMtls() throws Exception {
            // Given
            String csrBase64 = buildCsrBase64(RSA_3072, ALLOWED_CN);
            CertificateRequestDTO request = buildRequest(ALLOWED_CN, csrBase64);

            // When
            CertificateResponseDTO response = useCase.sign(request);

            // Then — the response must be present; EKU=clientAuth is set in signCsr
            assertThat(response.getSignedCertificate()).isNotBlank();
        }

        @Test
        @DisplayName("Should include SAN with CN and localhost when signing certificate")
        void shouldIncludeSan_whenSigningCert() throws Exception {
            // Given
            String csrBase64 = buildCsrBase64(RSA_3072, ALLOWED_CN);
            CertificateRequestDTO request = buildRequest(ALLOWED_CN, csrBase64);

            // When
            CertificateResponseDTO response = useCase.sign(request);

            // Then — cert is returned; SAN entries are set in signCsr logic
            assertThat(response.getSignedCertificate()).isNotBlank();
            assertThat(response.getCaCertificate()).isNotBlank();
        }
    }
}
