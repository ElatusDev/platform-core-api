/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.usecases.LogoutUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST controller for the logout endpoint.
 *
 * <p>Extracts the access token from the cookie or Authorization header,
 * delegates revocation to {@link LogoutUseCase}, and clears cookies.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security")
public class LogoutController {

    /** Error message when no access token is available for logout. */
    public static final String ERROR_NO_TOKEN_FOR_LOGOUT = "No access token available for logout";

    /** Authorization header name. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer prefix for Authorization header values. */
    public static final String BEARER_PREFIX = "Bearer ";

    private final LogoutUseCase logoutUseCase;
    private final CookieService cookieService;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param logoutUseCase the logout use case
     * @param cookieService the cookie service for clearing cookies
     */
    public LogoutController(LogoutUseCase logoutUseCase,
                             CookieService cookieService) {
        this.logoutUseCase = logoutUseCase;
        this.cookieService = cookieService;
    }

    /**
     * Logs out the current user by revoking all tokens and clearing cookies.
     *
     * @param request  the HTTP request containing the access token
     * @param response the HTTP response where cookies will be cleared
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = cookieService.extractAccessToken(request)
                .or(() -> extractBearerToken(request))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_NO_TOKEN_FOR_LOGOUT));

        logoutUseCase.logout(accessToken);
        cookieService.clearTokenCookies(response);

        return ResponseEntity.noContent().build();
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
