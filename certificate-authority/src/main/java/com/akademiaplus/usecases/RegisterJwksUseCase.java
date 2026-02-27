/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.usecases.domain.JwksRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import openapi.akademiaplus.domain.certificate.authority.dto.JwksRegistrationRequestDTO;
import org.springframework.stereotype.Service;

/**
 * Accepts a JWT public key registration from an enrolled service and stores it
 * in the {@link JwksRegistry}.
 *
 * <p>This endpoint is protected by mTLS (port 8081). The caller's identity is
 * established by the CN in their client certificate, which is passed in by the
 * controller as {@code callerCn}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterJwksUseCase {

    private final JwksRegistry jwksRegistry;

    /**
     * Register a JWT signing public key for the calling service.
     *
     * @param dto       registration payload (kid, alg, publicKeyBase64)
     * @param callerCn  CN extracted from the mTLS client certificate by the controller
     */
    public void register(JwksRegistrationRequestDTO dto, String callerCn) {
        String effectiveKid = (dto.getKid() != null && !dto.getKid().isBlank())
                ? dto.getKid()
                : callerCn;

        jwksRegistry.register(effectiveKid, dto.getAlg(), dto.getPublicKeyBase64());
        log.info("JWKS registration accepted: kid={} alg={} caller={}", effectiveKid, dto.getAlg(), callerCn);
    }
}
