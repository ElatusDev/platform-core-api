/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import com.akademiaplus.hmac.exceptions.HmacSignatureException;
import com.akademiaplus.hmac.interfaceadapters.config.HmacProperties;
import com.akademiaplus.hmac.usecases.HmacKeyService;
import com.akademiaplus.hmac.usecases.HmacSignatureService;
import com.akademiaplus.hmac.usecases.NonceStore;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HmacSigningFilter}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("HmacSigningFilter")
@ExtendWith(MockitoExtension.class)
class HmacSigningFilterTest {

    private static final String VALID_SIGNATURE = "valid-hmac-signature-hex";
    private static final String VALID_TIMESTAMP = String.valueOf(System.currentTimeMillis() / 1000L);
    private static final String VALID_NONCE = "test-nonce-uuid";
    private static final String VALID_BODY_HASH = "body-hash-sha256";
    private static final String REQUEST_BODY = "{\"test\":true}";
    private static final byte[] SIGNING_KEY = "test-key".getBytes(StandardCharsets.UTF_8);
    private static final long TOLERANCE_SECONDS = 300L;

    @Mock private HmacSignatureService hmacSignatureService;
    @Mock private NonceStore nonceStore;
    @Mock private HmacKeyService hmacKeyService;
    @Mock private HmacProperties hmacProperties;
    @Mock private FilterChain filterChain;

    private HmacSigningFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = new HmacSigningFilter(hmacSignatureService, nonceStore, hmacKeyService, hmacProperties);
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/v1/courses");
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
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(hmacSignatureService);
        }

        @Test
        @DisplayName("should continue chain when no authentication")
        void shouldContinueChain_whenNoAuthentication() throws Exception {
            // Given
            when(hmacProperties.isEnabled()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(hmacSignatureService);
        }
    }

    @Nested
    @DisplayName("Missing headers")
    class MissingHeaders {

        @Test
        @DisplayName("should return 401 when signature header is missing")
        void shouldReturn401_whenSignatureHeaderMissing() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            request.addHeader(HmacSigningFilter.HEADER_TIMESTAMP, VALID_TIMESTAMP);
            request.addHeader(HmacSigningFilter.HEADER_NONCE, VALID_NONCE);
            request.addHeader(HmacSigningFilter.HEADER_BODY_HASH, VALID_BODY_HASH);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("should return 401 when all HMAC headers are missing")
        void shouldReturn401_whenAllHeadersMissing() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(401);
            Map<String, String> body = objectMapper.readValue(
                    response.getContentAsString(), Map.class);
            assertThat(body.get("code")).isEqualTo(HmacSignatureException.ERROR_CODE_HMAC);
        }
    }

    @Nested
    @DisplayName("Timestamp validation")
    class TimestampValidation {

        @Test
        @DisplayName("should return 401 when timestamp is outside tolerance")
        void shouldReturn401_whenTimestampOutsideTolerance() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            when(hmacProperties.getTimestampToleranceSeconds()).thenReturn(TOLERANCE_SECONDS);
            long expiredTimestamp = (System.currentTimeMillis() / 1000L) - 600L;
            setAllHeaders(VALID_SIGNATURE, String.valueOf(expiredTimestamp), VALID_NONCE, VALID_BODY_HASH);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(filterChain);
        }
    }

    @Nested
    @DisplayName("Nonce replay")
    class NonceReplay {

        @Test
        @DisplayName("should return 401 when nonce is already used")
        void shouldReturn401_whenNonceAlreadyUsed() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            when(hmacProperties.getTimestampToleranceSeconds()).thenReturn(TOLERANCE_SECONDS);
            setAllHeaders(VALID_SIGNATURE, VALID_TIMESTAMP, VALID_NONCE, VALID_BODY_HASH);
            when(nonceStore.exists(VALID_NONCE)).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(filterChain);
        }
    }

    @Nested
    @DisplayName("Body hash verification")
    class BodyHashVerification {

        @Test
        @DisplayName("should return 401 when body hash does not match")
        void shouldReturn401_whenBodyHashDoesNotMatchActualBody() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            when(hmacProperties.getTimestampToleranceSeconds()).thenReturn(TOLERANCE_SECONDS);
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));
            setAllHeaders(VALID_SIGNATURE, VALID_TIMESTAMP, VALID_NONCE, "wrong-body-hash");
            when(nonceStore.exists(VALID_NONCE)).thenReturn(false);
            when(hmacSignatureService.computeBodyHash(REQUEST_BODY.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn("actual-body-hash");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(filterChain);
        }
    }

    @Nested
    @DisplayName("Signature verification")
    class SignatureVerification {

        @Test
        @DisplayName("should continue chain when signature is valid")
        void shouldContinueChain_whenSignatureIsValid() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            when(hmacProperties.getTimestampToleranceSeconds()).thenReturn(TOLERANCE_SECONDS);
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));
            setAllHeaders(VALID_SIGNATURE, VALID_TIMESTAMP, VALID_NONCE, VALID_BODY_HASH);
            when(nonceStore.exists(VALID_NONCE)).thenReturn(false);
            when(hmacSignatureService.computeBodyHash(REQUEST_BODY.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(VALID_BODY_HASH);
            String stringToSign = "POST\n/v1/courses\n" + VALID_TIMESTAMP + "\n" + VALID_BODY_HASH + "\n" + VALID_NONCE;
            when(hmacSignatureService.buildRequestStringToSign(
                    "POST", "/v1/courses", VALID_TIMESTAMP, VALID_BODY_HASH, VALID_NONCE))
                    .thenReturn(stringToSign);
            when(hmacKeyService.resolveDefaultKey()).thenReturn(SIGNING_KEY);
            when(hmacSignatureService.verifySignature(SIGNING_KEY, stringToSign, VALID_SIGNATURE))
                    .thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(CachedBodyHttpServletRequest.class), eq(response));
            verify(nonceStore).store(VALID_NONCE, TOLERANCE_SECONDS);
        }

        @Test
        @DisplayName("should return 401 when signature is invalid")
        void shouldReturn401_whenSignatureIsInvalid() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            when(hmacProperties.getTimestampToleranceSeconds()).thenReturn(TOLERANCE_SECONDS);
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));
            setAllHeaders("bad-signature", VALID_TIMESTAMP, VALID_NONCE, VALID_BODY_HASH);
            when(nonceStore.exists(VALID_NONCE)).thenReturn(false);
            when(hmacSignatureService.computeBodyHash(REQUEST_BODY.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(VALID_BODY_HASH);
            String stringToSign = "POST\n/v1/courses\n" + VALID_TIMESTAMP + "\n" + VALID_BODY_HASH + "\n" + VALID_NONCE;
            when(hmacSignatureService.buildRequestStringToSign(
                    "POST", "/v1/courses", VALID_TIMESTAMP, VALID_BODY_HASH, VALID_NONCE))
                    .thenReturn(stringToSign);
            when(hmacKeyService.resolveDefaultKey()).thenReturn(SIGNING_KEY);
            when(hmacSignatureService.verifySignature(SIGNING_KEY, stringToSign, "bad-signature"))
                    .thenReturn(false);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(401);
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("should store nonce in request attribute on success")
        void shouldStoreNonce_whenVerificationSucceeds() throws Exception {
            // Given
            setAuthentication();
            when(hmacProperties.isEnabled()).thenReturn(true);
            when(hmacProperties.getTimestampToleranceSeconds()).thenReturn(TOLERANCE_SECONDS);
            request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));
            setAllHeaders(VALID_SIGNATURE, VALID_TIMESTAMP, VALID_NONCE, VALID_BODY_HASH);
            when(nonceStore.exists(VALID_NONCE)).thenReturn(false);
            when(hmacSignatureService.computeBodyHash(REQUEST_BODY.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(VALID_BODY_HASH);
            String stringToSign = "POST\n/v1/courses\n" + VALID_TIMESTAMP + "\n" + VALID_BODY_HASH + "\n" + VALID_NONCE;
            when(hmacSignatureService.buildRequestStringToSign(
                    "POST", "/v1/courses", VALID_TIMESTAMP, VALID_BODY_HASH, VALID_NONCE))
                    .thenReturn(stringToSign);
            when(hmacKeyService.resolveDefaultKey()).thenReturn(SIGNING_KEY);
            when(hmacSignatureService.verifySignature(SIGNING_KEY, stringToSign, VALID_SIGNATURE))
                    .thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(nonceStore).store(VALID_NONCE, TOLERANCE_SECONDS);
        }
    }

    @Nested
    @DisplayName("Missing headers list")
    class MissingHeadersList {

        @Test
        @DisplayName("should list all missing headers")
        void shouldListAllMissingHeaders() {
            // When
            String result = filter.buildMissingHeadersList(null, null, null, null);

            // Then
            assertThat(result).contains(HmacSigningFilter.HEADER_SIGNATURE);
            assertThat(result).contains(HmacSigningFilter.HEADER_TIMESTAMP);
            assertThat(result).contains(HmacSigningFilter.HEADER_NONCE);
            assertThat(result).contains(HmacSigningFilter.HEADER_BODY_HASH);
        }

        @Test
        @DisplayName("should list only missing headers")
        void shouldListOnlyMissingHeaders() {
            // When
            String result = filter.buildMissingHeadersList("present", null, "present", null);

            // Then
            assertThat(result).doesNotContain(HmacSigningFilter.HEADER_SIGNATURE);
            assertThat(result).contains(HmacSigningFilter.HEADER_TIMESTAMP);
            assertThat(result).doesNotContain(HmacSigningFilter.HEADER_NONCE);
            assertThat(result).contains(HmacSigningFilter.HEADER_BODY_HASH);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));
    }

    private void setAllHeaders(String signature, String timestamp, String nonce, String bodyHash) {
        request.addHeader(HmacSigningFilter.HEADER_SIGNATURE, signature);
        request.addHeader(HmacSigningFilter.HEADER_TIMESTAMP, timestamp);
        request.addHeader(HmacSigningFilter.HEADER_NONCE, nonce);
        request.addHeader(HmacSigningFilter.HEADER_BODY_HASH, bodyHash);
    }
}
