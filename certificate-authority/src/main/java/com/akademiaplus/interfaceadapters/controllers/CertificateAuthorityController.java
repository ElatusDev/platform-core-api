/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters.controllers;

import com.akademiaplus.usecases.EnrollServiceUseCase;
import com.akademiaplus.usecases.GetCaCertificateUseCase;
import com.akademiaplus.usecases.SignCertificateUseCase;
import com.akademiaplus.usecases.exceptions.CsrValidationException;
import com.akademiaplus.usecases.exceptions.InvalidBootstrapTokenException;
import com.akademiaplus.usecases.exceptions.TokenAlreadyUsedException;
import com.akademiaplus.usecases.exceptions.TokenCnMismatchException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.certificate.authority.api.CaApi;
import openapi.akademiaplus.domain.certificate.authority.dto.CertificateRequestDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.CertificateResponseDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.EnrollmentRequestDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles all certificate authority operations:
 * <ul>
 *   <li>{@code POST /ca/sign-cert} — signs a CSR for an already-enrolled service (mTLS)</li>
 *   <li>{@code GET /ca/ca.crt} — returns the PEM-encoded root CA certificate (unauthenticated)</li>
 *   <li>{@code POST /ca/enroll} — one-time bootstrap enrollment via token (one-way TLS)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class CertificateAuthorityController implements CaApi {

    private final SignCertificateUseCase signCertificateUseCase;
    private final GetCaCertificateUseCase getCaCertificateUseCase;
    private final EnrollServiceUseCase enrollServiceUseCase;

    @Override
    public ResponseEntity<CertificateResponseDTO> signCertificate(CertificateRequestDTO certificateRequestDTO) {
        return ResponseEntity.ok(signCertificateUseCase.sign(certificateRequestDTO));
    }

    @Override
    public ResponseEntity<String> getCaCertificate() {
        String pem = getCaCertificateUseCase.getPemEncodedCaCertificate();
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(pem);
    }

    @Override
    public ResponseEntity<CertificateResponseDTO> enrollService(EnrollmentRequestDTO enrollmentRequestDTO) {
        return ResponseEntity.ok(enrollServiceUseCase.enroll(enrollmentRequestDTO));
    }

    @ExceptionHandler(CsrValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleCsrValidation(CsrValidationException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidBootstrapTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidToken(InvalidBootstrapTokenException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler({TokenAlreadyUsedException.class, TokenCnMismatchException.class})
    public ResponseEntity<ErrorResponseDTO> handleTokenForbidden(RuntimeException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
