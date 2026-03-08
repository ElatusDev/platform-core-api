/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.passkey.exceptions.PasskeyAuthenticationException;
import com.akademiaplus.passkey.usecases.PasskeyAuthenticationUseCase;
import com.akademiaplus.tokenbinding.usecases.DeviceFingerprintService;
import com.akademiaplus.tokenbinding.usecases.domain.DeviceFingerprint;
import tools.jackson.databind.ObjectMapper;
import openapi.akademiaplus.domain.security.dto.PasskeyLoginCompleteRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyLoginOptionsRequestDTO;
import openapi.akademiaplus.domain.security.dto.PasskeyRegisterOptionsRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for passkey authentication endpoints.
 *
 * <p>Full Spring context with Testcontainers MariaDB. The
 * {@link PasskeyAuthenticationUseCase} is mocked because WebAuthn
 * attestation/assertion objects require a FIDO2 authenticator or complex
 * test harness. Tests verify controller routing, cookie handling, and
 * error mapping.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("PasskeyComponentTest")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasskeyComponentTest extends AbstractIntegrationTest {

    public static final String PASSKEY_BASE = "/v1/security/passkey";
    public static final String REGISTER_OPTIONS_PATH = PASSKEY_BASE + "/register/options";
    public static final String REGISTER_COMPLETE_PATH = PASSKEY_BASE + "/register/complete";
    public static final String LOGIN_OPTIONS_PATH = PASSKEY_BASE + "/login/options";
    public static final String LOGIN_COMPLETE_PATH = PASSKEY_BASE + "/login/complete";

    public static final String SET_COOKIE_HEADER = "Set-Cookie";
    public static final Long TEST_TENANT_ID = 1L;
    public static final String TEST_USERNAME = "passkey-test-user";
    public static final String MOCK_OPTIONS_JSON = "{\"challenge\":\"dGVzdC1jaGFsbGVuZ2U\",\"rp\":{\"name\":\"Test\"}}";
    public static final String MOCK_ACCESS_TOKEN = "mock-access-token";
    public static final String MOCK_REFRESH_TOKEN = "mock-refresh-token";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantContextHolder tenantContextHolder;

    @MockitoBean private PasskeyAuthenticationUseCase passkeyAuthenticationUseCase;
    @MockitoBean private DeviceFingerprintService deviceFingerprintService;

    @BeforeEach
    void setUp() {
        tenantContextHolder.setTenantId(TEST_TENANT_ID);
        when(deviceFingerprintService.computeFingerprint(any()))
                .thenReturn(new DeviceFingerprint("full-hash-value", "device-hash-value", "127.0.0.1"));
    }

    @Nested
    @DisplayName("Registration Options")
    class RegistrationOptions {

        @Test
        @DisplayName("Should return 200 with challenge when requesting registration options")
        void shouldReturn200_withChallenge_whenRequestingRegistrationOptions() throws Exception {
            // Given
            setSecurityContext(TEST_USERNAME);
            when(passkeyAuthenticationUseCase.generateRegistrationOptions(
                    anyString(), eq(TEST_TENANT_ID)))
                    .thenReturn(MOCK_OPTIONS_JSON);

            PasskeyRegisterOptionsRequestDTO request = new PasskeyRegisterOptionsRequestDTO();
            request.setTenantId(TEST_TENANT_ID);

            // When / Then
            mockMvc.perform(post(REGISTER_OPTIONS_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optionsJson").value(MOCK_OPTIONS_JSON));
        }
    }

    @Nested
    @DisplayName("Registration Complete")
    class RegistrationComplete {

        @Test
        @DisplayName("Should return 200 with success when registration is verified")
        void shouldReturn200_withSuccess_whenRegistrationVerified() throws Exception {
            // Given
            setSecurityContext(TEST_USERNAME);
            when(passkeyAuthenticationUseCase.completeRegistration(
                    anyString(), eq(TEST_TENANT_ID), eq("My Passkey")))
                    .thenReturn("My Passkey");

            String body = """
                    {
                        "responseJson": "{\\"id\\":\\"test-credential-id\\"}",
                        "tenantId": %d,
                        "displayName": "My Passkey"
                    }
                    """.formatted(TEST_TENANT_ID);

            // When / Then
            mockMvc.perform(post(REGISTER_COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.displayName").value("My Passkey"));
        }
    }

    @Nested
    @DisplayName("Login Options")
    class LoginOptions {

        @Test
        @DisplayName("Should return 200 with challenge when requesting login options")
        void shouldReturn200_withChallenge_whenRequestingLoginOptions() throws Exception {
            // Given
            when(passkeyAuthenticationUseCase.generateLoginOptions(eq(TEST_TENANT_ID)))
                    .thenReturn(MOCK_OPTIONS_JSON);

            PasskeyLoginOptionsRequestDTO request = new PasskeyLoginOptionsRequestDTO();
            request.setTenantId(TEST_TENANT_ID);

            // When / Then
            mockMvc.perform(post(LOGIN_OPTIONS_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optionsJson").value(MOCK_OPTIONS_JSON));
        }
    }

    @Nested
    @DisplayName("Login Complete")
    class LoginComplete {

        @Test
        @DisplayName("Should return 200 with JWT and set cookies when login is verified")
        void shouldReturn200_withJwtAndCookies_whenLoginVerified() throws Exception {
            // Given
            LoginResult loginResult = new LoginResult(
                    MOCK_ACCESS_TOKEN, MOCK_REFRESH_TOKEN, TEST_USERNAME);
            when(passkeyAuthenticationUseCase.completeLogin(
                    anyString(), eq(TEST_TENANT_ID), any(Map.class)))
                    .thenReturn(loginResult);

            PasskeyLoginCompleteRequestDTO request = new PasskeyLoginCompleteRequestDTO();
            request.setResponseJson("{\"id\":\"test-credential-id\"}");
            request.setTenantId(TEST_TENANT_ID);

            // When / Then
            mockMvc.perform(post(LOGIN_COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(MOCK_ACCESS_TOKEN))
                    .andExpect(header().string(SET_COOKIE_HEADER, containsString("access_token")));
        }

        @Test
        @DisplayName("Should return 401 when login with unregistered credential fails")
        void shouldReturn401_whenUnregisteredCredentialFails() throws Exception {
            // Given
            when(passkeyAuthenticationUseCase.completeLogin(
                    anyString(), anyLong(), any(Map.class)))
                    .thenThrow(new PasskeyAuthenticationException(
                            PasskeyAuthenticationException.ERROR_CREDENTIAL_NOT_FOUND));

            PasskeyLoginCompleteRequestDTO request = new PasskeyLoginCompleteRequestDTO();
            request.setResponseJson("{\"id\":\"unknown-credential\"}");
            request.setTenantId(TEST_TENANT_ID);

            // When / Then
            mockMvc.perform(post(LOGIN_COMPLETE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("PASSKEY_AUTHENTICATION_FAILED"));
        }
    }

    private void setSecurityContext(String username) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
