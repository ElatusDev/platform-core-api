/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.internal.exceptions.RefreshTokenExpiredException;
import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.usecases.TokenRefreshUseCase;
import com.akademiaplus.internal.usecases.domain.TokenRefreshResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the token refresh endpoint.
 *
 * <p>Reads the refresh token from the HttpOnly cookie, delegates to
 * {@link TokenRefreshUseCase}, and sets new cookies on the response.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security/token")
public class TokenRefreshController {

    /** Error message when no refresh token cookie is present. */
    public static final String ERROR_NO_REFRESH_TOKEN = "No refresh token cookie present";

    private final TokenRefreshUseCase tokenRefreshUseCase;
    private final CookieService cookieService;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param tokenRefreshUseCase the token refresh use case
     * @param cookieService       the cookie service for token delivery
     */
    public TokenRefreshController(TokenRefreshUseCase tokenRefreshUseCase,
                                   CookieService cookieService) {
        this.tokenRefreshUseCase = tokenRefreshUseCase;
        this.cookieService = cookieService;
    }

    /**
     * Refreshes the access and refresh tokens.
     *
     * <p>Reads the refresh token from the cookie, validates and rotates it,
     * then sets new access and refresh token cookies on the response.</p>
     *
     * @param request  the HTTP request containing the refresh token cookie
     * @param response the HTTP response where new cookies will be set
     * @return 200 OK with empty body (tokens are in cookies)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.extractRefreshToken(request)
                .orElseThrow(() -> new RefreshTokenExpiredException(ERROR_NO_REFRESH_TOKEN));

        TokenRefreshResult result = tokenRefreshUseCase.refresh(refreshToken);

        cookieService.addTokenCookies(response, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok().build();
    }
}
