/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.interfaceadapters;

import com.akademiaplus.internal.interfaceadapters.jwt.CookieService;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.tokenbinding.interfaceadapters.config.TokenBindingProperties;
import com.akademiaplus.tokenbinding.usecases.AnomalyDetectionService;
import com.akademiaplus.tokenbinding.usecases.DeviceFingerprintService;
import com.akademiaplus.tokenbinding.usecases.domain.AnomalyEvent;
import com.akademiaplus.tokenbinding.usecases.domain.DeviceFingerprint;
import com.akademiaplus.tokenbinding.usecases.domain.TokenBindingMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TokenBindingFilter}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("TokenBindingFilter")
@ExtendWith(MockitoExtension.class)
class TokenBindingFilterTest {

    private static final String USERNAME = "testuser";
    private static final Long TENANT_ID = 1L;
    private static final String ACCESS_TOKEN = "jwt-access-token";
    private static final String FULL_HASH = "full-hash-sha256";
    private static final String DEVICE_ONLY_HASH = "device-only-hash-sha256";
    private static final String DIFFERENT_FULL_HASH = "different-full-hash";
    private static final String DIFFERENT_DEVICE_HASH = "different-device-hash";
    private static final String CLIENT_IP = "192.168.1.100";

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private DeviceFingerprintService deviceFingerprintService;
    @Mock private AnomalyDetectionService anomalyDetectionService;
    @Mock private TokenBindingProperties tokenBindingProperties;
    @Mock private CookieService cookieService;
    @Mock private FilterChain filterChain;
    @Mock private Claims claims;

    private TokenBindingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = new TokenBindingFilter(
                jwtTokenProvider, deviceFingerprintService,
                anomalyDetectionService, tokenBindingProperties, cookieService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Skip conditions")
    class SkipConditions {

        @Test
        @DisplayName("should pass through when mode is OFF")
        void shouldPassThrough_whenModeOff() throws Exception {
            // Given
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.OFF);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService, cookieService, claims);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should pass through when no authentication in SecurityContext")
        void shouldPassThrough_whenNoAuthentication() throws Exception {
            // Given
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.STRICT);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService, cookieService, claims);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should pass through when no token found in request")
        void shouldPassThrough_whenNoToken() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.STRICT);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.empty());

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            verify(cookieService, times(1)).extractAccessToken(request);
            verifyNoInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService, claims);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should pass through when token has no fingerprint claim (legacy token)")
        void shouldPassThrough_whenNoFingerprintClaim() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.STRICT);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
            when(claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class)).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            verify(claims, times(1)).get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class);
            verify(claims, times(1)).get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class);
            verifyNoInteractions(deviceFingerprintService, anomalyDetectionService);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }
    }

    @Nested
    @DisplayName("Token extraction")
    class TokenExtraction {

        @Test
        @DisplayName("should extract token from cookie first")
        void shouldExtractToken_fromCookie() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.STRICT);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            stubMatchingFingerprint();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(cookieService, times(1)).extractAccessToken(request);
            verify(jwtTokenProvider, times(1)).getClaims(ACCESS_TOKEN);
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should fall back to Bearer header when no cookie")
        void shouldExtractToken_fromBearerHeader() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.STRICT);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.empty());
            request.addHeader(TokenBindingFilter.AUTHORIZATION_HEADER,
                    TokenBindingFilter.BEARER_PREFIX + ACCESS_TOKEN);
            stubMatchingFingerprint();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(jwtTokenProvider, times(1)).getClaims(ACCESS_TOKEN);
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }
    }

    @Nested
    @DisplayName("STRICT mode")
    class StrictMode {

        @Test
        @DisplayName("should allow request when full hash matches")
        void shouldAllow_whenFullHashMatches() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.STRICT);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            stubMatchingFingerprint();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
            verifyNoInteractions(anomalyDetectionService);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should reject request when full hash mismatches")
        void shouldReject_whenFullHashMismatches() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.STRICT);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            stubMismatchingFullFingerprint();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verifyNoInteractions(filterChain);
            assertThat(response.getStatus()).isEqualTo(401);
            Map<String, String> body = objectMapper.readValue(
                    response.getContentAsString(), Map.class);
            assertThat(body.get("code")).isEqualTo(TokenBindingFilter.ERROR_RESPONSE_CODE);
            assertThat(body.get("message")).isEqualTo(TokenBindingFilter.ERROR_RESPONSE_MESSAGE);
            verify(anomalyDetectionService, times(1)).logAnomaly(any(AnomalyEvent.class));
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should log anomaly when full hash mismatches")
        void shouldLogAnomaly_whenFullHashMismatches() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.STRICT);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            stubMismatchingFullFingerprint();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            ArgumentCaptor<AnomalyEvent> captor = ArgumentCaptor.forClass(AnomalyEvent.class);
            verify(anomalyDetectionService).logAnomaly(captor.capture());
            AnomalyEvent event = captor.getValue();
            assertThat(event.username()).isEqualTo(USERNAME);
            assertThat(event.eventType()).isEqualTo(AnomalyEvent.EVENT_TYPE_FULL_MISMATCH);
            assertThat(event.actualIp()).isEqualTo(CLIENT_IP);
            assertThat(event.tenantId()).isEqualTo(TENANT_ID);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }
    }

    @Nested
    @DisplayName("RELAXED mode")
    class RelaxedMode {

        @Test
        @DisplayName("should allow and log IP change when only IP differs")
        void shouldAllowAndLog_whenOnlyIpChanges() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.RELAXED);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
            when(claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class)).thenReturn(DIFFERENT_FULL_HASH);
            when(claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class)).thenReturn(DEVICE_ONLY_HASH);
            when(claims.getSubject()).thenReturn(USERNAME);
            when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
            when(deviceFingerprintService.computeFingerprint(request))
                    .thenReturn(new DeviceFingerprint(FULL_HASH, DEVICE_ONLY_HASH, CLIENT_IP));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            ArgumentCaptor<AnomalyEvent> captor = ArgumentCaptor.forClass(AnomalyEvent.class);
            verify(anomalyDetectionService, times(1)).logAnomaly(captor.capture());
            assertThat(captor.getValue().eventType()).isEqualTo(AnomalyEvent.EVENT_TYPE_IP_CHANGE);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should reject when device hash mismatches")
        void shouldReject_whenDeviceHashMismatches() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.RELAXED);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
            when(claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class)).thenReturn(FULL_HASH);
            when(claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class)).thenReturn(DIFFERENT_DEVICE_HASH);
            when(claims.getSubject()).thenReturn(USERNAME);
            when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
            when(deviceFingerprintService.computeFingerprint(request))
                    .thenReturn(new DeviceFingerprint(FULL_HASH, DEVICE_ONLY_HASH, CLIENT_IP));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verifyNoInteractions(filterChain);
            assertThat(response.getStatus()).isEqualTo(401);
            verify(anomalyDetectionService, times(1)).logAnomaly(any(AnomalyEvent.class));
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should log device mismatch anomaly")
        void shouldLogAnomaly_whenDeviceHashMismatches() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.RELAXED);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
            when(claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class)).thenReturn(FULL_HASH);
            when(claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class)).thenReturn(DIFFERENT_DEVICE_HASH);
            when(claims.getSubject()).thenReturn(USERNAME);
            when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
            when(deviceFingerprintService.computeFingerprint(request))
                    .thenReturn(new DeviceFingerprint(FULL_HASH, DEVICE_ONLY_HASH, CLIENT_IP));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            ArgumentCaptor<AnomalyEvent> captor = ArgumentCaptor.forClass(AnomalyEvent.class);
            verify(anomalyDetectionService, times(1)).logAnomaly(captor.capture());
            assertThat(captor.getValue().eventType()).isEqualTo(AnomalyEvent.EVENT_TYPE_DEVICE_MISMATCH);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should allow when both hashes match")
        void shouldAllow_whenBothHashesMatch() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.RELAXED);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
            when(claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class)).thenReturn(FULL_HASH);
            when(claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class)).thenReturn(DEVICE_ONLY_HASH);
            when(claims.getSubject()).thenReturn(USERNAME);
            when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
            when(deviceFingerprintService.computeFingerprint(request))
                    .thenReturn(new DeviceFingerprint(FULL_HASH, DEVICE_ONLY_HASH, CLIENT_IP));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(anomalyDetectionService);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }

        @Test
        @DisplayName("should skip device check when dfpr claim is null")
        void shouldSkipDeviceCheck_whenDfprClaimNull() throws Exception {
            // Given
            setAuthentication();
            when(tokenBindingProperties.getMode()).thenReturn(TokenBindingMode.RELAXED);
            when(cookieService.extractAccessToken(request)).thenReturn(Optional.of(ACCESS_TOKEN));
            when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
            when(claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class)).thenReturn(FULL_HASH);
            when(claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class)).thenReturn(null);
            when(claims.getSubject()).thenReturn(USERNAME);
            when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
            when(deviceFingerprintService.computeFingerprint(request))
                    .thenReturn(new DeviceFingerprint(FULL_HASH, DEVICE_ONLY_HASH, CLIENT_IP));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(anomalyDetectionService);
            verifyNoMoreInteractions(jwtTokenProvider, deviceFingerprintService, anomalyDetectionService,
                    tokenBindingProperties, cookieService, filterChain, claims);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USERNAME, null, Collections.emptyList()));
    }

    private void stubMatchingFingerprint() {
        when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
        when(claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class)).thenReturn(FULL_HASH);
        when(claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class)).thenReturn(DEVICE_ONLY_HASH);
        when(claims.getSubject()).thenReturn(USERNAME);
        when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
        when(deviceFingerprintService.computeFingerprint(request))
                .thenReturn(new DeviceFingerprint(FULL_HASH, DEVICE_ONLY_HASH, CLIENT_IP));
    }

    private void stubMismatchingFullFingerprint() {
        when(jwtTokenProvider.getClaims(ACCESS_TOKEN)).thenReturn(claims);
        when(claims.get(JwtTokenProvider.FINGERPRINT_CLAIM, String.class)).thenReturn(DIFFERENT_FULL_HASH);
        when(claims.get(JwtTokenProvider.DEVICE_FINGERPRINT_CLAIM, String.class)).thenReturn(DEVICE_ONLY_HASH);
        when(claims.getSubject()).thenReturn(USERNAME);
        when(claims.get(JwtTokenProvider.TENANT_ID_CLAIM, Long.class)).thenReturn(TENANT_ID);
        when(deviceFingerprintService.computeFingerprint(request))
                .thenReturn(new DeviceFingerprint(FULL_HASH, DEVICE_ONLY_HASH, CLIENT_IP));
    }
}
