/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.usecases.InternalAuthenticationUseCase;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.tokenbinding.usecases.DeviceFingerprintService;
import com.akademiaplus.tokenbinding.usecases.domain.DeviceFingerprint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import openapi.akademiaplus.domain.security.dto.AuthTokenResponseDTO;
import openapi.akademiaplus.domain.security.dto.LoginRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
public class InternalAuthController {

    private final InternalAuthenticationUseCase internalAuthenticationUseCase;
    private final CookieService cookieService;
    private final HttpServletResponse httpServletResponse;
    private final HttpServletRequest httpServletRequest;
    private final DeviceFingerprintService deviceFingerprintService;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param internalAuthenticationUseCase the authentication use case
     * @param cookieService                the cookie service for token delivery
     * @param httpServletResponse          the HTTP response for setting cookies
     * @param httpServletRequest           the HTTP request for fingerprint computation
     * @param deviceFingerprintService     the device fingerprint service
     */
    public InternalAuthController(InternalAuthenticationUseCase internalAuthenticationUseCase,
                                   CookieService cookieService,
                                   HttpServletResponse httpServletResponse,
                                   HttpServletRequest httpServletRequest,
                                   DeviceFingerprintService deviceFingerprintService) {
        this.internalAuthenticationUseCase = internalAuthenticationUseCase;
        this.cookieService = cookieService;
        this.httpServletResponse = httpServletResponse;
        this.httpServletRequest = httpServletRequest;
        this.deviceFingerprintService = deviceFingerprintService;
    }

    @PostMapping("/login/internal")
    public ResponseEntity<AuthTokenResponseDTO> loginInternal(@RequestBody LoginRequestDTO loginRequestDTO) {
        DeviceFingerprint fingerprint = deviceFingerprintService.computeFingerprint(httpServletRequest);
        Map<String, Object> fingerprintClaims = Map.of(
                JwtTokenProvider.FINGERPRINT_CLAIM, fingerprint.fullHash(),
                JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, fingerprint.deviceOnlyHash()
        );

        LoginResult result = internalAuthenticationUseCase.login(
                loginRequestDTO.getUsername(),
                loginRequestDTO.getPassword(),
                fingerprintClaims
        );

        cookieService.addTokenCookies(httpServletResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(new AuthTokenResponseDTO(result.accessToken()));
    }
}
