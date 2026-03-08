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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import openapi.akademiaplus.domain.security.dto.MagicLinkRequestDTO;
import openapi.akademiaplus.domain.security.dto.MagicLinkVerifyRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link MagicLinkController}.
 *
 * <p>Standalone MockMvc tests (no Spring context) verifying request
 * routing and response mapping.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("MagicLinkController")
@ExtendWith(MockitoExtension.class)
class MagicLinkControllerTest {

    private static final String TEST_EMAIL = "user@example.com";
    private static final Long TEST_TENANT_ID = 1L;
    private static final String TEST_RAW_TOKEN = "dGVzdC10b2tlbi12YWx1ZS1mb3ItdW5pdC10ZXN0cw";
    private static final String ACCESS_TOKEN = "jwt-access-token";
    private static final String REFRESH_TOKEN = "jwt-refresh-token";

    @Mock private MagicLinkRequestUseCase magicLinkRequestUseCase;
    @Mock private MagicLinkVerificationUseCase magicLinkVerificationUseCase;
    @Mock private CookieService cookieService;
    @Mock private HttpServletResponse httpServletResponse;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MagicLinkController controller = new MagicLinkController(
                magicLinkRequestUseCase, magicLinkVerificationUseCase,
                cookieService, httpServletResponse);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("Request Magic Link")
    class RequestMagicLink {

        @Test
        @DisplayName("Should return 200 when magic link requested")
        void shouldReturn200_whenMagicLinkRequested() throws Exception {
            // Given
            MagicLinkRequestDTO request = new MagicLinkRequestDTO(TEST_EMAIL, TEST_TENANT_ID);

            // When / Then
            mockMvc.perform(post("/v1/security/login/magic-link/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should delegate to use case when magic link requested")
        void shouldDelegateToUseCase_whenMagicLinkRequested() throws Exception {
            // Given
            MagicLinkRequestDTO request = new MagicLinkRequestDTO(TEST_EMAIL, TEST_TENANT_ID);

            // When
            mockMvc.perform(post("/v1/security/login/magic-link/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then
            verify(magicLinkRequestUseCase).requestMagicLink(
                    org.mockito.ArgumentMatchers.argThat(dto ->
                            TEST_EMAIL.equals(dto.getEmail()) && TEST_TENANT_ID.equals(dto.getTenantId())));
        }
    }

    @Nested
    @DisplayName("Verify Magic Link")
    class VerifyMagicLink {

        @Test
        @DisplayName("Should return 200 with access token when verification succeeds")
        void shouldReturn200WithAccessToken_whenVerificationSucceeds() throws Exception {
            // Given
            MagicLinkVerifyRequestDTO request = new MagicLinkVerifyRequestDTO(TEST_RAW_TOKEN, TEST_TENANT_ID);
            LoginResult loginResult = new LoginResult(ACCESS_TOKEN, REFRESH_TOKEN, TEST_EMAIL);
            when(magicLinkVerificationUseCase.verifyMagicLink(
                    org.mockito.ArgumentMatchers.argThat(dto ->
                            TEST_RAW_TOKEN.equals(dto.getToken()) && TEST_TENANT_ID.equals(dto.getTenantId()))))
                    .thenReturn(loginResult);

            // When / Then
            mockMvc.perform(post("/v1/security/login/magic-link/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(ACCESS_TOKEN));
        }

        @Test
        @DisplayName("Should set HttpOnly cookies when verification succeeds")
        void shouldSetHttpOnlyCookies_whenVerificationSucceeds() throws Exception {
            // Given
            MagicLinkVerifyRequestDTO request = new MagicLinkVerifyRequestDTO(TEST_RAW_TOKEN, TEST_TENANT_ID);
            LoginResult loginResult = new LoginResult(ACCESS_TOKEN, REFRESH_TOKEN, TEST_EMAIL);
            when(magicLinkVerificationUseCase.verifyMagicLink(
                    org.mockito.ArgumentMatchers.argThat(dto ->
                            TEST_RAW_TOKEN.equals(dto.getToken()) && TEST_TENANT_ID.equals(dto.getTenantId()))))
                    .thenReturn(loginResult);

            // When
            mockMvc.perform(post("/v1/security/login/magic-link/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Then
            verify(cookieService).addTokenCookies(httpServletResponse, ACCESS_TOKEN, REFRESH_TOKEN);
        }
    }
}
