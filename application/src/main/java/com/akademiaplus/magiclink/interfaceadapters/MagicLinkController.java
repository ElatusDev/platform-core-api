/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.magiclink.usecases.MagicLinkRequestUseCase;
import com.akademiaplus.magiclink.usecases.MagicLinkVerificationUseCase;
import jakarta.servlet.http.HttpServletResponse;
import openapi.akademiaplus.domain.security.dto.AuthTokenResponseDTO;
import openapi.akademiaplus.domain.security.dto.MagicLinkRequestDTO;
import openapi.akademiaplus.domain.security.dto.MagicLinkVerifyRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for magic link authentication endpoints.
 *
 * <p>Provides request (send email) and verify (validate token) endpoints
 * for passwordless authentication via email magic links.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/security")
public class MagicLinkController {

    private final MagicLinkRequestUseCase magicLinkRequestUseCase;
    private final MagicLinkVerificationUseCase magicLinkVerificationUseCase;
    private final CookieService cookieService;
    private final HttpServletResponse httpServletResponse;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param magicLinkRequestUseCase      the request use case
     * @param magicLinkVerificationUseCase the verification use case
     * @param cookieService                the cookie service for token delivery
     * @param httpServletResponse          the HTTP response for setting cookies
     */
    public MagicLinkController(MagicLinkRequestUseCase magicLinkRequestUseCase,
                                MagicLinkVerificationUseCase magicLinkVerificationUseCase,
                                CookieService cookieService,
                                HttpServletResponse httpServletResponse) {
        this.magicLinkRequestUseCase = magicLinkRequestUseCase;
        this.magicLinkVerificationUseCase = magicLinkVerificationUseCase;
        this.cookieService = cookieService;
        this.httpServletResponse = httpServletResponse;
    }

    /**
     * Requests a magic link for email-based login.
     *
     * <p>Always returns 200 regardless of whether the email exists,
     * to prevent email enumeration attacks.</p>
     *
     * @param request the magic link request with email and tenant ID
     * @return HTTP 200 (always)
     */
    @PostMapping("/login/magic-link/request")
    public ResponseEntity<Void> requestMagicLink(@RequestBody MagicLinkRequestDTO request) {
        magicLinkRequestUseCase.requestMagicLink(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies a magic link token and issues a JWT.
     *
     * @param request the verification request with token and tenant ID
     * @return HTTP 200 with the JWT access token
     */
    @PostMapping("/login/magic-link/verify")
    public ResponseEntity<AuthTokenResponseDTO> verifyMagicLink(@RequestBody MagicLinkVerifyRequestDTO request) {
        LoginResult result = magicLinkVerificationUseCase.verifyMagicLink(request);
        cookieService.addTokenCookies(httpServletResponse, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(new AuthTokenResponseDTO(result.accessToken()));
    }
}
