/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CookieService}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("CookieService")
@ExtendWith(MockitoExtension.class)
class CookieServiceTest {

    private static final String ACCESS_TOKEN = "access-token-jwt";
    private static final String REFRESH_TOKEN = "refresh-token-jwt";
    private static final String ACCESS_TOKEN_NAME = "access_token";
    private static final String REFRESH_TOKEN_NAME = "refresh_token";
    private static final String COOKIE_DOMAIN = "localhost";
    private static final long ACCESS_TOKEN_MAX_AGE = 900L;
    private static final long REFRESH_TOKEN_MAX_AGE = 2592000L;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    private CookieService cookieService;

    @BeforeEach
    void setUp() throws Exception {
        cookieService = new CookieService();
        setField(cookieService, "cookieDomain", COOKIE_DOMAIN);
        setField(cookieService, "secureCookie", true);
        setField(cookieService, "accessTokenCookieName", ACCESS_TOKEN_NAME);
        setField(cookieService, "refreshTokenCookieName", REFRESH_TOKEN_NAME);
        setField(cookieService, "accessTokenMaxAge", ACCESS_TOKEN_MAX_AGE);
        setField(cookieService, "refreshTokenMaxAge", REFRESH_TOKEN_MAX_AGE);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("Cookie Creation")
    class CookieCreation {

        @Test
        @DisplayName("Should add access and refresh token cookies with correct attributes")
        void shouldAddTokenCookies_whenTokensProvided() {
            // Given — tokens ready

            // When
            cookieService.addTokenCookies(response, ACCESS_TOKEN, REFRESH_TOKEN);

            // Then
            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
            verify(response, times(2)).addHeader(
                    org.mockito.ArgumentMatchers.eq(CookieService.SET_COOKIE_HEADER),
                    headerCaptor.capture());

            String accessCookieHeader = headerCaptor.getAllValues().get(0);
            assertThat(accessCookieHeader).contains(ACCESS_TOKEN_NAME + "=" + ACCESS_TOKEN);
            assertThat(accessCookieHeader).contains("HttpOnly");
            assertThat(accessCookieHeader).contains("Secure");
            assertThat(accessCookieHeader).contains("SameSite=Strict");
            assertThat(accessCookieHeader).contains("Path=" + CookieService.ACCESS_TOKEN_PATH);

            String refreshCookieHeader = headerCaptor.getAllValues().get(1);
            assertThat(refreshCookieHeader).contains(REFRESH_TOKEN_NAME + "=" + REFRESH_TOKEN);
            assertThat(refreshCookieHeader).contains("Path=" + CookieService.REFRESH_TOKEN_PATH);
        }
    }

    @Nested
    @DisplayName("Cookie Clearing")
    class CookieClearing {

        @Test
        @DisplayName("Should set Max-Age to 0 when clearing cookies")
        void shouldSetMaxAgeToZero_whenClearingCookies() {
            // Given — no setup needed

            // When
            cookieService.clearTokenCookies(response);

            // Then
            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
            verify(response, times(2)).addHeader(
                    org.mockito.ArgumentMatchers.eq(CookieService.SET_COOKIE_HEADER),
                    headerCaptor.capture());

            for (String header : headerCaptor.getAllValues()) {
                assertThat(header).contains("Max-Age=0");
            }
        }
    }

    @Nested
    @DisplayName("Cookie Extraction")
    class CookieExtraction {

        @Test
        @DisplayName("Should extract access token when cookie is present")
        void shouldExtractAccessToken_whenCookiePresent() {
            // Given
            Cookie accessCookie = new Cookie(ACCESS_TOKEN_NAME, ACCESS_TOKEN);
            when(request.getCookies()).thenReturn(new Cookie[]{accessCookie});

            // When
            Optional<String> result = cookieService.extractAccessToken(request);

            // Then
            assertThat(result).isPresent().contains(ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Should extract refresh token when cookie is present")
        void shouldExtractRefreshToken_whenCookiePresent() {
            // Given
            Cookie refreshCookie = new Cookie(REFRESH_TOKEN_NAME, REFRESH_TOKEN);
            when(request.getCookies()).thenReturn(new Cookie[]{refreshCookie});

            // When
            Optional<String> result = cookieService.extractRefreshToken(request);

            // Then
            assertThat(result).isPresent().contains(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Should return empty when no cookies are present")
        void shouldReturnEmpty_whenNoCookiesPresent() {
            // Given
            when(request.getCookies()).thenReturn(null);

            // When
            Optional<String> result = cookieService.extractAccessToken(request);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
