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
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.tokenbinding.usecases.DeviceFingerprintService;
import com.akademiaplus.tokenbinding.usecases.domain.DeviceFingerprint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import openapi.akademiaplus.domain.security.dto.PasskeyLoginCompleteRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyLoginOptionsRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyRegisterCompleteRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyRegisterOptionsRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Map;

import org.mockito.InOrder;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link PasskeyController}.
 *
 * <p>Standalone MockMvc tests (no Spring context) verifying request
 * routing and response mapping.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("PasskeyController")
@ExtendWith(MockitoExtension.class)
class PasskeyControllerTest {

    private static final String USERNAME = "testuser@example.com";
    private static final Long TENANT_ID = 1L;
    private static final String OPTIONS_JSON = "{\"challenge\":\"test-challenge\"}";
    private static final String DISPLAY_NAME = "My Passkey";
    private static final String ACCESS_TOKEN = "jwt-access-token";
    private static final String REFRESH_TOKEN = "jwt-refresh-token";
    private static final String FULL_HASH = "full-fingerprint-hash";
    private static final String DEVICE_HASH = "device-only-hash";
    private static final String CLIENT_IP = "127.0.0.1";

    @Mock private PasskeyAuthenticationUseCase passkeyAuthenticationUseCase;
    @Mock private CookieService cookieService;
    @Mock private HttpServletResponse httpServletResponse;
    @Mock private HttpServletRequest httpServletRequest;
    @Mock private DeviceFingerprintService deviceFingerprintService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PasskeyController controller = new PasskeyController(
                passkeyAuthenticationUseCase, cookieService, httpServletResponse,
                httpServletRequest, deviceFingerprintService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Nested
    @DisplayName("Register Options")
    class RegisterOptions {

        @Test
        @DisplayName("Should return 200 with options JSON when registration options requested")
        void shouldReturn200WithOptions_whenRegistrationOptionsRequested() throws Exception {
            // Given
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(USERNAME, USERNAME, Collections.emptyList()));
            when(passkeyAuthenticationUseCase.generateRegistrationOptions(USERNAME, TENANT_ID))
                    .thenReturn(OPTIONS_JSON);

            PasskeyRegisterOptionsRequestDTO request = new PasskeyRegisterOptionsRequestDTO(TENANT_ID);

            // When / Then
            mockMvc.perform(post("/v1/security/passkey/register/options")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optionsJson").value(OPTIONS_JSON));

            verify(passkeyAuthenticationUseCase, times(1)).generateRegistrationOptions(USERNAME, TENANT_ID);
            verifyNoMoreInteractions(passkeyAuthenticationUseCase, cookieService,
                    httpServletResponse, httpServletRequest, deviceFingerprintService);
            SecurityContextHolder.clearContext();
        }
    }

    @Nested
    @DisplayName("Register Complete")
    class RegisterComplete {

        @Test
        @DisplayName("Should return 200 with success when registration succeeds")
        void shouldReturn200_whenRegistrationSucceeds() throws Exception {
            // Given
            String responseJson = "{\"response\":{}}";
            when(passkeyAuthenticationUseCase.completeRegistration(responseJson, TENANT_ID, DISPLAY_NAME))
                    .thenReturn(DISPLAY_NAME);

            PasskeyRegisterCompleteRequestDTO request = new PasskeyRegisterCompleteRequestDTO(TENANT_ID, responseJson);
            request.setDisplayName(DISPLAY_NAME);

            // When / Then
            mockMvc.perform(post("/v1/security/passkey/register/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.displayName").value(DISPLAY_NAME));

            verify(passkeyAuthenticationUseCase, times(1)).completeRegistration(responseJson, TENANT_ID, DISPLAY_NAME);
            verifyNoMoreInteractions(passkeyAuthenticationUseCase, cookieService,
                    httpServletResponse, httpServletRequest, deviceFingerprintService);
        }
    }

    @Nested
    @DisplayName("Login Options")
    class LoginOptions {

        @Test
        @DisplayName("Should return 200 with options JSON when login options requested")
        void shouldReturn200WithOptions_whenLoginOptionsRequested() throws Exception {
            // Given
            when(passkeyAuthenticationUseCase.generateLoginOptions(TENANT_ID))
                    .thenReturn(OPTIONS_JSON);

            PasskeyLoginOptionsRequestDTO request = new PasskeyLoginOptionsRequestDTO(TENANT_ID);

            // When / Then
            mockMvc.perform(post("/v1/security/passkey/login/options")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optionsJson").value(OPTIONS_JSON));

            verify(passkeyAuthenticationUseCase, times(1)).generateLoginOptions(TENANT_ID);
            verifyNoMoreInteractions(passkeyAuthenticationUseCase, cookieService,
                    httpServletResponse, httpServletRequest, deviceFingerprintService);
        }
    }

    @Nested
    @DisplayName("Login Complete")
    class LoginComplete {

        @Test
        @DisplayName("Should return 200 with access token when login succeeds")
        void shouldReturn200WithAccessToken_whenLoginSucceeds() throws Exception {
            // Given
            String responseJson = "{\"response\":{}}";
            DeviceFingerprint fingerprint = new DeviceFingerprint(FULL_HASH, DEVICE_HASH, CLIENT_IP);
            when(deviceFingerprintService.computeFingerprint(httpServletRequest)).thenReturn(fingerprint);

            Map<String, Object> fingerprintClaims = Map.of(
                    JwtTokenProvider.FINGERPRINT_CLAIM, FULL_HASH,
                    JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, DEVICE_HASH);
            LoginResult loginResult = new LoginResult(ACCESS_TOKEN, REFRESH_TOKEN, USERNAME);
            when(passkeyAuthenticationUseCase.completeLogin(responseJson, TENANT_ID, fingerprintClaims))
                    .thenReturn(loginResult);

            PasskeyLoginCompleteRequestDTO request = new PasskeyLoginCompleteRequestDTO(TENANT_ID, responseJson);

            // When / Then
            mockMvc.perform(post("/v1/security/passkey/login/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(ACCESS_TOKEN));

            InOrder inOrder = inOrder(deviceFingerprintService, passkeyAuthenticationUseCase, cookieService);
            inOrder.verify(deviceFingerprintService, times(1)).computeFingerprint(httpServletRequest);
            inOrder.verify(passkeyAuthenticationUseCase, times(1)).completeLogin(responseJson, TENANT_ID, fingerprintClaims);
            inOrder.verify(cookieService, times(1)).addTokenCookies(httpServletResponse, ACCESS_TOKEN, REFRESH_TOKEN);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(httpServletResponse, httpServletRequest);
        }

        @Test
        @DisplayName("Should set HttpOnly cookies when login succeeds")
        void shouldSetHttpOnlyCookies_whenLoginSucceeds() throws Exception {
            // Given
            String responseJson = "{\"response\":{}}";
            DeviceFingerprint fingerprint = new DeviceFingerprint(FULL_HASH, DEVICE_HASH, CLIENT_IP);
            when(deviceFingerprintService.computeFingerprint(httpServletRequest)).thenReturn(fingerprint);

            Map<String, Object> fingerprintClaims = Map.of(
                    JwtTokenProvider.FINGERPRINT_CLAIM, FULL_HASH,
                    JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, DEVICE_HASH);
            LoginResult loginResult = new LoginResult(ACCESS_TOKEN, REFRESH_TOKEN, USERNAME);
            when(passkeyAuthenticationUseCase.completeLogin(responseJson, TENANT_ID, fingerprintClaims))
                    .thenReturn(loginResult);

            PasskeyLoginCompleteRequestDTO request = new PasskeyLoginCompleteRequestDTO(TENANT_ID, responseJson);

            // When
            mockMvc.perform(post("/v1/security/passkey/login/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then
            InOrder inOrder = inOrder(deviceFingerprintService, passkeyAuthenticationUseCase, cookieService);
            inOrder.verify(deviceFingerprintService, times(1)).computeFingerprint(httpServletRequest);
            inOrder.verify(passkeyAuthenticationUseCase, times(1)).completeLogin(responseJson, TENANT_ID, fingerprintClaims);
            inOrder.verify(cookieService, times(1)).addTokenCookies(httpServletResponse, ACCESS_TOKEN, REFRESH_TOKEN);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(httpServletResponse, httpServletRequest);
        }
    }
}
