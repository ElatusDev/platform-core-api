/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.usecases.InternalAuthenticationUseCase;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import jakarta.servlet.http.HttpServletResponse;
import openapi.akademiaplus.domain.security.api.LoginApi;
import openapi.akademiaplus.domain.security.dto.AuthTokenResponseDTO;
import openapi.akademiaplus.domain.security.dto.LoginRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for internal user authentication.
 *
 * <p>Delegates to {@link InternalAuthenticationUseCase} and sets HttpOnly
 * cookies on the response. Also returns the access token in the JSON body
 * for backward compatibility during migration.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security")
public class InternalAuthController implements LoginApi {

    private final InternalAuthenticationUseCase internalAuthenticationUseCase;
    private final CookieService cookieService;
    private final HttpServletResponse httpServletResponse;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param internalAuthenticationUseCase the authentication use case
     * @param cookieService                the cookie service for token delivery
     * @param httpServletResponse          the HTTP response for setting cookies
     */
    public InternalAuthController(InternalAuthenticationUseCase internalAuthenticationUseCase,
                                   CookieService cookieService,
                                   HttpServletResponse httpServletResponse) {
        this.internalAuthenticationUseCase = internalAuthenticationUseCase;
        this.cookieService = cookieService;
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public ResponseEntity<AuthTokenResponseDTO> loginInternal(LoginRequestDTO loginRequestDTO) {
        LoginResult result = internalAuthenticationUseCase.login(
                loginRequestDTO.getUsername(),
                loginRequestDTO.getPassword()
        );

        cookieService.addTokenCookies(httpServletResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(new AuthTokenResponseDTO(result.accessToken()));
    }
}
