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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import openapi.akademiaplus.domain.certificate.authority.dto.CertificateRequestDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.CertificateResponseDTO;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Core certificate signing service.
 *
 * <p>Signs RSA-3072 (or larger) CSRs using the ECDSA P-384 CA private key.
 * Enforces: CN allowlist, minimum RSA key size, and correct X.509v3 extensions
 * for mTLS compatibility (clientAuth EKU, SAN, BasicConstraints=false).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignCertificateUseCase {

    public static final String SIGNING_ALGORITHM = "SHA384withECDSA";

    private static final String RSA_OID = PKCSObjectIdentifiers.rsaEncryption.getId();
    private static final String SAN_LOCALHOST = "localhost";
    private static final String SAN_LOOPBACK_IP = "127.0.0.1";
    private static final String ERROR_CN_NOT_ALLOWED = "Common name is not in the CA allowlist: ";
    private static final String ERROR_KEY_NOT_RSA = "CSR public key must be RSA";
    private static final String ERROR_KEY_TOO_SMALL = "RSA key size must be >= ";
    private static final String ERROR_CSR_PARSE = "Invalid CSR: cannot parse as PEM-encoded PKCS#10";
    private static final String ERROR_CN_MISMATCH = "CSR subject CN does not match requested common name";

    private final CertificateAuthority certificateAuthority;

    @Value("#{'${ca.allowed-common-names}'.split(',')}")
    private List<String> allowedCommonNames;

    @Value("${ca.leaf.validity-days:365}")
    private int leafValidityDays;

    @Value("${ca.leaf.min-key-size:3072}")
    private int minKeySize;

    /**
     * Signs a CSR submitted via the mTLS-authenticated {@code POST /ca/sign-cert} endpoint.
     *
     * @param dto request containing the CN, organization, and base64-encoded PEM CSR
     * @return signed certificate and CA certificate, both base64-encoded PEM
     */
    public CertificateResponseDTO sign(CertificateRequestDTO dto) {
        PKCS10CertificationRequest csr = parseCsr(dto.getCertificateSigningRequest());
        validateAllowedCn(dto.getCommonName());
        validateCsrKey(csr);
        X509Certificate signed = signCsr(csr);
        return buildResponse(signed);
    }

    /**
     * Signs a raw CSR after the caller has already validated the bootstrap token.
     * Used by {@link EnrollServiceUseCase} during first-time enrollment.
     *
     * @param csr parsed PKCS#10 CSR
     * @param cn  common name already validated by the enrollment flow
     * @return signed certificate response
     */
    public CertificateResponseDTO signEnrollment(PKCS10CertificationRequest csr, String cn) {
        validateAllowedCn(cn);
        validateCsrKey(csr);
        X509Certificate signed = signCsr(csr);
        return buildResponse(signed);
    }

    /**
     * Parses a base64-encoded PEM CSR string into a {@link PKCS10CertificationRequest}.
     *
     * @param csrBase64 base64(PEM-formatted PKCS#10 CSR)
     * @return parsed CSR
     * @throws CsrValidationException if parsing fails
     */
    public PKCS10CertificationRequest parseCsr(String csrBase64) {
        byte[] decoded = Base64.getDecoder().decode(csrBase64);
        String csrPem = new String(decoded, StandardCharsets.UTF_8);
        try (PEMParser parser = new PEMParser(new StringReader(csrPem))) {
            Object obj = parser.readObject();
            if (obj instanceof PKCS10CertificationRequest csr) {
                return csr;
            }
            throw new CsrValidationException(ERROR_CSR_PARSE);
        } catch (IOException e) {
            throw new CsrValidationException(ERROR_CSR_PARSE + ": " + e.getMessage(), e);
        }
    }

    private void validateAllowedCn(String cn) {
        if (!allowedCommonNames.contains(cn.trim())) {
            throw new CsrValidationException(ERROR_CN_NOT_ALLOWED + cn);
        }
    }

    private void validateCsrKey(PKCS10CertificationRequest csr) {
        SubjectPublicKeyInfo spki = csr.getSubjectPublicKeyInfo();
        ASN1ObjectIdentifier keyAlgorithmOid = spki.getAlgorithm().getAlgorithm();

        if (!keyAlgorithmOid.getId().equals(RSA_OID)) {
            throw new CsrValidationException(ERROR_KEY_NOT_RSA);
        }
        try {
            RSAKeyParameters rsaKey = (RSAKeyParameters) PublicKeyFactory.createKey(spki);
            if (rsaKey.getModulus().bitLength() < minKeySize) {
                throw new CsrValidationException(ERROR_KEY_TOO_SMALL + minKeySize + " bits");
            }
        } catch (IOException e) {
            throw new CsrValidationException("Failed to extract RSA key from CSR: " + e.getMessage(), e);
        }
    }

    private X509Certificate signCsr(PKCS10CertificationRequest csr) {
        try {
            BigInteger serial = certificateAuthority.nextSerial();
            Instant now = Instant.now();
            X500Name issuer = X500Name.getInstance(
                    certificateAuthority.getCertificate().getSubjectX500Principal().getEncoded());
            X500Name subject = csr.getSubject();
            SubjectPublicKeyInfo csrPubKey = csr.getSubjectPublicKeyInfo();

            X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                    issuer, serial,
                    Date.from(now),
                    Date.from(now.plus(Duration.ofDays(leafValidityDays))),
                    subject, csrPubKey);

            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            builder.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(new KeyPurposeId[]{
                            KeyPurposeId.id_kp_serverAuth,
                            KeyPurposeId.id_kp_clientAuth
                    }));

            String cn = IETFUtils.valueToString(subject.getRDNs(BCStyle.CN)[0].getFirst().getValue());
            GeneralNames san = new GeneralNames(new GeneralName[]{
                    new GeneralName(GeneralName.dNSName, cn),
                    new GeneralName(GeneralName.dNSName, SAN_LOCALHOST),
                    new GeneralName(GeneralName.iPAddress, SAN_LOOPBACK_IP)
            });
            builder.addExtension(Extension.subjectAlternativeName, false, san);

            ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM)
                    .setProvider("BC")
                    .build(certificateAuthority.getPrivateKey());

            return new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(builder.build(signer));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign CSR: " + e.getMessage(), e);
        }
    }

    private CertificateResponseDTO buildResponse(X509Certificate leafCert) {
        try {
            CertificateResponseDTO response = new CertificateResponseDTO();
            response.setSignedCertificate(toPemBase64(leafCert));
            response.setCaCertificate(toPemBase64(certificateAuthority.getCertificate()));
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode signed certificate: " + e.getMessage(), e);
        }
    }

    private String toPemBase64(X509Certificate cert) throws IOException {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
            writer.writeObject(cert);
        }
        return Base64.getEncoder().encodeToString(sw.toString().getBytes(StandardCharsets.UTF_8));
    }
}
