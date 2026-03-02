/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters.controllers;

import com.akademiaplus.usecases.RegistrationUseCase;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.security.api.RegisterApi;
import openapi.akademiaplus.domain.security.dto.RegistrationRequestDTO;
import openapi.akademiaplus.domain.security.dto.RegistrationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tenant registration.
 * <p>
 * Exposes {@code POST /v1/security/register} as a public endpoint
 * that creates a tenant and admin employee in a single request.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security")
@RequiredArgsConstructor
public class RegistrationController implements RegisterApi {

    private final RegistrationUseCase registrationUseCase;

    @Override
    public ResponseEntity<RegistrationResponseDTO> register(RegistrationRequestDTO registrationRequestDTO) {
        RegistrationResponseDTO response = registrationUseCase.register(registrationRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
