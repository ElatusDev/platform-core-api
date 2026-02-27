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
import com.akademiaplus.usecases.GetJwksUseCase;
import com.akademiaplus.usecases.RegisterJwksUseCase;
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
import openapi.akademiaplus.domain.certificate.authority.dto.JwksDocumentDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.JwksRegistrationRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles all certificate authority operations:
 * <ul>
 *   <li>{@code POST /ca/sign-cert}          — renews a cert for an enrolled service (reserved for future use)</li>
 *   <li>{@code GET  /ca/ca.crt}             — returns PEM root CA cert (unauthenticated)</li>
 *   <li>{@code POST /ca/enroll}             — one-time bootstrap enrollment (reserved for future use)</li>
 *   <li>{@code POST /ca/jwks/register}      — service registers its JWT public key (internal network trust)</li>
 *   <li>{@code GET  /ca/.well-known/jwks.json} — returns JWKS document (unauthenticated)</li>
 * </ul>
 *
 * <p>In the current architecture, all endpoints run on a single HTTP port. Transport
 * security (TLS) is delegated to the infrastructure layer (reverse proxy in Docker,
 * cert-manager/Istio in Kubernetes). Service identity for JWKS registration is
 * established by the {@code kid} field in the request body, trusted because the
 * {@code akademia-internal} network is isolated.
 */
@RestController
@RequiredArgsConstructor
public class CertificateAuthorityController implements CaApi {

    private final SignCertificateUseCase signCertificateUseCase;
    private final GetCaCertificateUseCase getCaCertificateUseCase;
    private final EnrollServiceUseCase enrollServiceUseCase;
    private final RegisterJwksUseCase registerJwksUseCase;
    private final GetJwksUseCase getJwksUseCase;

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

    /**
     * Registers the caller's JWT signing public key.
     *
     * <p>Service identity is established by the {@code kid} field in the request
     * body, trusted because this endpoint is only reachable on the isolated
     * {@code akademia-internal} Docker network. In production, Kubernetes
     * NetworkPolicy or a service mesh replaces network-level isolation.
     */
    @Override
    public ResponseEntity<Void> registerJwks(JwksRegistrationRequestDTO jwksRegistrationRequestDTO) {
        String callerIdentity = jwksRegistrationRequestDTO.getKid() != null
                ? jwksRegistrationRequestDTO.getKid()
                : "unknown";
        registerJwksUseCase.register(jwksRegistrationRequestDTO, callerIdentity);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<JwksDocumentDTO> getJwks() {
        return ResponseEntity.ok(getJwksUseCase.getJwks());
    }

    // ─── Exception handlers ──────────────────────────────────────────────────

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
