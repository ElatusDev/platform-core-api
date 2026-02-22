/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.usecases.domain.BootstrapToken;
import com.akademiaplus.usecases.domain.TokenManifest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import openapi.akademiaplus.domain.certificate.authority.dto.CertificateResponseDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.EnrollmentRequestDTO;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Service;

/**
 * Handles first-time certificate enrollment authenticated by a bootstrap token.
 *
 * <p>Validates the token, parses and validates the CSR, delegates signing to
 * {@link SignCertificateUseCase}, and invalidates the token on success to
 * enforce single-use semantics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollServiceUseCase {

    private final TokenManifest tokenManifest;
    private final SignCertificateUseCase signCertificateUseCase;

    /**
     * Enrolls a service by validating its bootstrap token and signing its CSR.
     *
     * @param dto enrollment request with token, CN, and base64-encoded PEM CSR
     * @return signed certificate and CA certificate, both base64-encoded PEM
     * @throws com.akademiaplus.usecases.exceptions.InvalidBootstrapTokenException if token not found
     * @throws com.akademiaplus.usecases.exceptions.TokenAlreadyUsedException      if token already used
     * @throws com.akademiaplus.usecases.exceptions.TokenCnMismatchException       if CN does not match
     * @throws com.akademiaplus.usecases.exceptions.CsrValidationException         if CSR is invalid
     */
    public CertificateResponseDTO enroll(EnrollmentRequestDTO dto) {
        BootstrapToken token = tokenManifest.validate(dto.getBootstrapToken(), dto.getCommonName());

        PKCS10CertificationRequest csr = signCertificateUseCase.parseCsr(dto.getCertificateSigningRequest());
        CertificateResponseDTO response = signCertificateUseCase.signEnrollment(csr, dto.getCommonName());

        tokenManifest.invalidate(token.getToken());
        log.info("Enrollment successful for CN: {}", dto.getCommonName());

        return response;
    }
}
