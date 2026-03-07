/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.passkey.usecases.PasskeyAuthenticationUseCase;
import jakarta.servlet.http.HttpServletResponse;
import openapi.akademiaplus.domain.security.api.PasskeyApi;
import openapi.akademiaplus.domain.security.dto.AuthTokenResponseDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyLoginCompleteRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyLoginOptionsRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyLoginOptionsResponseDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyRegisterCompleteRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyRegisterCompleteResponseDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyRegisterOptionsRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyRegisterOptionsResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for passkey registration and authentication.
 *
 * <p>Implements the generated {@link PasskeyApi} interface with 4 endpoints.
 * Registration endpoints require an authenticated user; login endpoints are
 * public. Delegates all business logic to
 * {@link PasskeyAuthenticationUseCase}.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security")
public class PasskeyController implements PasskeyApi {

    private final PasskeyAuthenticationUseCase passkeyAuthenticationUseCase;
    private final CookieService cookieService;
    private final HttpServletResponse httpServletResponse;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param passkeyAuthenticationUseCase the passkey authentication use case
     * @param cookieService               the cookie service for token delivery
     * @param httpServletResponse         the HTTP response for setting cookies
     */
    public PasskeyController(PasskeyAuthenticationUseCase passkeyAuthenticationUseCase,
                              CookieService cookieService,
                              HttpServletResponse httpServletResponse) {
        this.passkeyAuthenticationUseCase = passkeyAuthenticationUseCase;
        this.cookieService = cookieService;
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public ResponseEntity<PasskeyRegisterOptionsResponseDTO> passkeyRegisterOptions(
            PasskeyRegisterOptionsRequestDTO request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String optionsJson = passkeyAuthenticationUseCase.generateRegistrationOptions(
                username, request.getTenantId());

        PasskeyRegisterOptionsResponseDTO response = new PasskeyRegisterOptionsResponseDTO();
        response.setOptionsJson(optionsJson);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PasskeyRegisterCompleteResponseDTO> passkeyRegisterComplete(
            PasskeyRegisterCompleteRequestDTO request) {

        String displayName = passkeyAuthenticationUseCase.completeRegistration(
                request.getResponseJson(), request.getTenantId(), request.getDisplayName());

        PasskeyRegisterCompleteResponseDTO response = new PasskeyRegisterCompleteResponseDTO();
        response.setSuccess(true);
        response.setDisplayName(displayName);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PasskeyLoginOptionsResponseDTO> passkeyLoginOptions(
            PasskeyLoginOptionsRequestDTO request) {

        String optionsJson = passkeyAuthenticationUseCase.generateLoginOptions(request.getTenantId());

        PasskeyLoginOptionsResponseDTO response = new PasskeyLoginOptionsResponseDTO();
        response.setOptionsJson(optionsJson);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AuthTokenResponseDTO> passkeyLoginComplete(
            PasskeyLoginCompleteRequestDTO request) {

        LoginResult result = passkeyAuthenticationUseCase.completeLogin(
                request.getResponseJson(), request.getTenantId());

        cookieService.addTokenCookies(httpServletResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(new AuthTokenResponseDTO(result.accessToken()));
    }
}
