/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.oauth.usecases.OAuthAuthenticationUseCase;
import jakarta.servlet.http.HttpServletResponse;
import openapi.akademiaplus.domain.security.dto.AuthTokenResponseDTO;
import openapi.akademiaplus.domain.security.dto.OAuthLoginRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for OAuth2 social login authentication.
 *
 * <p>Accepts an OAuth authorization code and provider, exchanges it for
 * a user profile, and returns a platform JWT.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security")
public class OAuthLoginController {

    private final OAuthAuthenticationUseCase oauthAuthenticationUseCase;
    private final CookieService cookieService;
    private final HttpServletResponse httpServletResponse;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param oauthAuthenticationUseCase the OAuth authentication use case
     * @param cookieService              the cookie service for token delivery
     * @param httpServletResponse        the HTTP response for setting cookies
     */
    public OAuthLoginController(OAuthAuthenticationUseCase oauthAuthenticationUseCase,
                                 CookieService cookieService,
                                 HttpServletResponse httpServletResponse) {
        this.oauthAuthenticationUseCase = oauthAuthenticationUseCase;
        this.cookieService = cookieService;
        this.httpServletResponse = httpServletResponse;
    }

    /**
     * Authenticates a user via OAuth2 social login.
     *
     * @param request the OAuth login request with provider, authorization code, redirect URI, and tenant ID
     * @return HTTP 200 with the JWT access token
     */
    @PostMapping("/login/oauth")
    public ResponseEntity<AuthTokenResponseDTO> loginOAuth(@RequestBody OAuthLoginRequestDTO request) {
        LoginResult result = oauthAuthenticationUseCase.loginWithOAuth(
                request.getProvider(),
                request.getAuthorizationCode(),
                request.getRedirectUri(),
                request.getTenantId()
        );

        cookieService.addTokenCookies(httpServletResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(new AuthTokenResponseDTO(result.accessToken()));
    }
}
