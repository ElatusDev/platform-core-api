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
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.interfaceadapters.session.RedisSessionStore;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for the logout endpoint.
 *
 * <p>Full Spring context with Testcontainers MariaDB. Redis is mocked
 * since these tests focus on the logout flow (cookie clearing + token
 * revocation via database), not the session store.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("LogoutComponentTest")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LogoutComponentTest extends AbstractIntegrationTest {

    public static final String LOGOUT_PATH = "/v1/security/logout";
    public static final String ACCESS_COOKIE_NAME = "access_token";
    public static final String REFRESH_COOKIE_NAME = "refresh_token";
    public static final String SET_COOKIE_HEADER = "Set-Cookie";
    public static final String TEST_USERNAME = "logout-test-user";
    public static final Long TEST_TENANT_ID = 1L;
    public static final Long TEST_USER_ID = 200L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TenantContextHolder tenantContextHolder;

    @MockitoBean private RedisSessionStore redisSessionStore;

    @BeforeEach
    void setUp() {
        tenantContextHolder.setTenantId(TEST_TENANT_ID);
    }

    @Nested
    @DisplayName("Valid Logout")
    class ValidLogout {

        @Test
        @DisplayName("Should return 204 and clear cookies when logging out with access token cookie")
        void shouldReturn204_andClearCookies_whenLoggingOutWithCookie() throws Exception {
            // Given — create a real signed access token
            Map<String, Object> claims = Map.of(JwtTokenProvider.USER_ID_CLAIM, TEST_USER_ID);
            String accessToken = jwtTokenProvider.createAccessToken(TEST_USERNAME, TEST_TENANT_ID, claims);
            Cookie accessCookie = new Cookie(ACCESS_COOKIE_NAME, accessToken);

            // When / Then
            mockMvc.perform(post(LOGOUT_PATH)
                            .cookie(accessCookie))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(SET_COOKIE_HEADER, containsString("Max-Age=0")));
        }

        @Test
        @DisplayName("Should return 204 when logging out with Bearer token header")
        void shouldReturn204_whenLoggingOutWithBearerHeader() throws Exception {
            // Given — create a real signed access token
            Map<String, Object> claims = Map.of(JwtTokenProvider.USER_ID_CLAIM, TEST_USER_ID);
            String accessToken = jwtTokenProvider.createAccessToken(TEST_USERNAME, TEST_TENANT_ID, claims);

            // When / Then
            mockMvc.perform(post(LOGOUT_PATH)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());
        }
    }
}
