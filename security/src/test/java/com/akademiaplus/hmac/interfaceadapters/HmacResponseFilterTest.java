/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import com.akademiaplus.hmac.interfaceadapters.config.HmacProperties;
import com.akademiaplus.hmac.usecases.HmacKeyService;
import com.akademiaplus.hmac.usecases.HmacSignatureService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.InOrder;

/**
 * Unit tests for {@link HmacResponseFilter}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("HmacResponseFilter")
@ExtendWith(MockitoExtension.class)
class HmacResponseFilterTest {

    private static final String REQUEST_NONCE = "request-nonce-uuid";
    private static final String RESPONSE_BODY = "{\"result\":\"ok\"}";
    private static final String RESPONSE_BODY_HASH = "response-body-hash";
    private static final String RESPONSE_SIGNATURE = "response-hmac-signature";
    private static final byte[] SIGNING_KEY = "test-key".getBytes(StandardCharsets.UTF_8);

    @Mock private HmacSignatureService hmacSignatureService;
    @Mock private HmacKeyService hmacKeyService;
    @Mock private HmacProperties hmacProperties;
    @Mock private FilterChain filterChain;

    private HmacResponseFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new HmacResponseFilter(hmacSignatureService, hmacKeyService, hmacProperties);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Skip conditions")
    class SkipConditions {

        @Test
        @DisplayName("should continue chain when HMAC is disabled")
        void shouldContinueChain_whenHmacDisabled() throws Exception {
            // Given
            when(hmacProperties.isEnabled()).thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            verify(hmacProperties, times(1)).isEnabled();
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(hmacSignatureService, hmacKeyService);
            verifyNoMoreInteractions(hmacSignatureService, hmacKeyService, hmacProperties, filterChain);
        }

        @Test
        @DisplayName("should continue chain when no authentication")
        void shouldContinueChain_whenNoAuthentication() throws Exception {
            // Given
            when(hmacProperties.isEnabled()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            verify(hmacProperties, times(1)).isEnabled();
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(hmacSignatureService, hmacKeyService);
            verifyNoMoreInteractions(hmacSignatureService, hmacKeyService, hmacProperties, filterChain);
        }

        @Test
        @DisplayName("should continue chain when no request nonce attribute")
        void shouldContinueChain_whenNoRequestNonceAttribute() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            verify(hmacProperties, times(1)).isEnabled();
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(hmacSignatureService, hmacKeyService);
            verifyNoMoreInteractions(hmacSignatureService, hmacKeyService, hmacProperties, filterChain);
        }
    }

    @Nested
    @DisplayName("Response signing")
    class ResponseSigning {

        @Test
        @DisplayName("should set response signature header")
        void shouldSetResponseSignatureHeader_whenResponseIsComplete() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            request.setAttribute(HmacSigningFilter.REQUEST_ATTR_NONCE, REQUEST_NONCE);

            doAnswer(invocation -> {
                CachedBodyHttpServletResponse cachedResponse = invocation.getArgument(1);
                cachedResponse.getWriter().write(RESPONSE_BODY);
                cachedResponse.getWriter().flush();
                return null;
            }).when(filterChain).doFilter(eq(request), any(CachedBodyHttpServletResponse.class));

            when(hmacSignatureService.computeBodyHash(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(RESPONSE_BODY_HASH);
            when(hmacSignatureService.buildResponseStringToSign(
                    eq("200"), eq(RESPONSE_BODY_HASH), anyString(), eq(REQUEST_NONCE)))
                    .thenReturn("string-to-sign");
            when(hmacKeyService.resolveDefaultKey()).thenReturn(SIGNING_KEY);
            when(hmacSignatureService.computeHmac(SIGNING_KEY, "string-to-sign"))
                    .thenReturn(RESPONSE_SIGNATURE);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getHeader(HmacResponseFilter.HEADER_RESPONSE_SIGNATURE))
                    .isEqualTo(RESPONSE_SIGNATURE);
            verify(hmacProperties, times(1)).isEnabled();
            verify(filterChain, times(1)).doFilter(eq(request), any(CachedBodyHttpServletResponse.class));
            verify(hmacSignatureService, times(1)).computeBodyHash(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8));
            verify(hmacSignatureService, times(1)).buildResponseStringToSign(
                    eq("200"), eq(RESPONSE_BODY_HASH), anyString(), eq(REQUEST_NONCE));
            verify(hmacKeyService, times(1)).resolveDefaultKey();
            verify(hmacSignatureService, times(1)).computeHmac(SIGNING_KEY, "string-to-sign");
            verifyNoMoreInteractions(hmacSignatureService, hmacKeyService, hmacProperties, filterChain);
        }

        @Test
        @DisplayName("should set response timestamp header")
        void shouldSetResponseTimestampHeader_whenResponseIsComplete() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            request.setAttribute(HmacSigningFilter.REQUEST_ATTR_NONCE, REQUEST_NONCE);

            doAnswer(invocation -> {
                CachedBodyHttpServletResponse cachedResponse = invocation.getArgument(1);
                cachedResponse.getWriter().write(RESPONSE_BODY);
                cachedResponse.getWriter().flush();
                return null;
            }).when(filterChain).doFilter(eq(request), any(CachedBodyHttpServletResponse.class));

            when(hmacSignatureService.computeBodyHash(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(RESPONSE_BODY_HASH);
            when(hmacSignatureService.buildResponseStringToSign(
                    eq("200"), eq(RESPONSE_BODY_HASH), anyString(), eq(REQUEST_NONCE)))
                    .thenReturn("string-to-sign");
            when(hmacKeyService.resolveDefaultKey()).thenReturn(SIGNING_KEY);
            when(hmacSignatureService.computeHmac(SIGNING_KEY, "string-to-sign"))
                    .thenReturn(RESPONSE_SIGNATURE);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getHeader(HmacResponseFilter.HEADER_RESPONSE_TIMESTAMP))
                    .isNotNull()
                    .matches("\\d+");
            verify(hmacProperties, times(1)).isEnabled();
            verify(filterChain, times(1)).doFilter(eq(request), any(CachedBodyHttpServletResponse.class));
            verify(hmacSignatureService, times(1)).computeBodyHash(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8));
            verify(hmacSignatureService, times(1)).buildResponseStringToSign(
                    eq("200"), eq(RESPONSE_BODY_HASH), anyString(), eq(REQUEST_NONCE));
            verify(hmacKeyService, times(1)).resolveDefaultKey();
            verify(hmacSignatureService, times(1)).computeHmac(SIGNING_KEY, "string-to-sign");
            verifyNoMoreInteractions(hmacSignatureService, hmacKeyService, hmacProperties, filterChain);
        }

        @Test
        @DisplayName("should write response body to output")
        void shouldWriteResponseBody_whenSigningIsComplete() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            request.setAttribute(HmacSigningFilter.REQUEST_ATTR_NONCE, REQUEST_NONCE);

            doAnswer(invocation -> {
                CachedBodyHttpServletResponse cachedResponse = invocation.getArgument(1);
                cachedResponse.getWriter().write(RESPONSE_BODY);
                cachedResponse.getWriter().flush();
                return null;
            }).when(filterChain).doFilter(eq(request), any(CachedBodyHttpServletResponse.class));

            when(hmacSignatureService.computeBodyHash(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(RESPONSE_BODY_HASH);
            when(hmacSignatureService.buildResponseStringToSign(
                    eq("200"), eq(RESPONSE_BODY_HASH), anyString(), eq(REQUEST_NONCE)))
                    .thenReturn("string-to-sign");
            when(hmacKeyService.resolveDefaultKey()).thenReturn(SIGNING_KEY);
            when(hmacSignatureService.computeHmac(SIGNING_KEY, "string-to-sign"))
                    .thenReturn(RESPONSE_SIGNATURE);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getContentAsString()).isEqualTo(RESPONSE_BODY);
            verify(hmacProperties, times(1)).isEnabled();
            verify(filterChain, times(1)).doFilter(eq(request), any(CachedBodyHttpServletResponse.class));
            verify(hmacSignatureService, times(1)).computeBodyHash(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8));
            verify(hmacSignatureService, times(1)).buildResponseStringToSign(
                    eq("200"), eq(RESPONSE_BODY_HASH), anyString(), eq(REQUEST_NONCE));
            verify(hmacKeyService, times(1)).resolveDefaultKey();
            verify(hmacSignatureService, times(1)).computeHmac(SIGNING_KEY, "string-to-sign");
            verifyNoMoreInteractions(hmacSignatureService, hmacKeyService, hmacProperties, filterChain);
        }
    }

    @Nested
    @DisplayName("Collaborator exception propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("should propagate exception when hmacKeyService throws during response signing")
        void shouldPropagateException_whenHmacKeyServiceThrows() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            request.setAttribute(HmacSigningFilter.REQUEST_ATTR_NONCE, REQUEST_NONCE);

            doAnswer(invocation -> {
                CachedBodyHttpServletResponse cachedResponse = invocation.getArgument(1);
                cachedResponse.getWriter().write(RESPONSE_BODY);
                cachedResponse.getWriter().flush();
                return null;
            }).when(filterChain).doFilter(eq(request), any(CachedBodyHttpServletResponse.class));

            when(hmacSignatureService.computeBodyHash(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(RESPONSE_BODY_HASH);
            when(hmacSignatureService.buildResponseStringToSign(
                    eq("200"), eq(RESPONSE_BODY_HASH), anyString(), eq(REQUEST_NONCE)))
                    .thenReturn("string-to-sign");
            RuntimeException cause = new RuntimeException("key resolution error");
            when(hmacKeyService.resolveDefaultKey()).thenThrow(cause);

            // When / Then
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("key resolution error");

            verify(hmacKeyService, times(1)).resolveDefaultKey();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));
    }
}
