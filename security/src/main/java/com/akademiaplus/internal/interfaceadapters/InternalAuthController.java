/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.internal.usecases.InternalAuthenticationUseCase;
import openapi.akademiaplus.domain.security.api.LoginApi;
import openapi.akademiaplus.domain.security.dto.AuthTokenResponseDTO;
import openapi.akademiaplus.domain.security.dto.LoginRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/security")
public class InternalAuthController implements LoginApi {

    private final InternalAuthenticationUseCase internalAuthenticationUseCase;

    public InternalAuthController(InternalAuthenticationUseCase internalAuthenticationUseCase) {
        this.internalAuthenticationUseCase = internalAuthenticationUseCase;
    }

    @Override
    public ResponseEntity<AuthTokenResponseDTO> loginInternal(LoginRequestDTO loginRequestDTO) {
        return ResponseEntity.ok(internalAuthenticationUseCase.login(loginRequestDTO));
    }
}

