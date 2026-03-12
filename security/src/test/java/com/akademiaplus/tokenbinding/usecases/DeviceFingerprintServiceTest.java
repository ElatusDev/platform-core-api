/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases;

import com.akademiaplus.tokenbinding.usecases.domain.DeviceFingerprint;
import com.akademiaplus.utilities.security.HashingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeviceFingerprintService}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeviceFingerprintService")
@ExtendWith(MockitoExtension.class)
class DeviceFingerprintServiceTest {

    private static final String CLIENT_IP = "192.168.1.100";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String ACCEPT_LANGUAGE = "en-US";
    private static final String DEVICE_ID = "device-abc-123";
    private static final String FULL_HASH = "full-hash-sha256";
    private static final String DEVICE_ONLY_HASH = "device-only-hash-sha256";
    private static final String SEP = DeviceFingerprintService.FINGERPRINT_SEPARATOR;

    @Mock
    private HashingService hashingService;

    private DeviceFingerprintService service;

    @BeforeEach
    void setUp() {
        service = new DeviceFingerprintService(hashingService);
    }

    @Nested
    @DisplayName("computeFingerprint")
    class ComputeFingerprint {

        @Test
        @DisplayName("should compute full and device-only hashes from all headers")
        void shouldComputeHashes_whenAllHeadersPresent() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(CLIENT_IP);
            request.addHeader(DeviceFingerprintService.HEADER_USER_AGENT, USER_AGENT);
            request.addHeader(DeviceFingerprintService.HEADER_ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
            request.addHeader(DeviceFingerprintService.HEADER_DEVICE_ID, DEVICE_ID);

            String deviceComponents = USER_AGENT + SEP + ACCEPT_LANGUAGE + SEP + DEVICE_ID;
            String fullComponents = CLIENT_IP + SEP + deviceComponents;

            when(hashingService.generateHash(fullComponents)).thenReturn(FULL_HASH);
            when(hashingService.generateHash(deviceComponents)).thenReturn(DEVICE_ONLY_HASH);

            // When
            DeviceFingerprint result = service.computeFingerprint(request);

            // Then
            assertThat(result.fullHash()).isEqualTo(FULL_HASH);
            assertThat(result.deviceOnlyHash()).isEqualTo(DEVICE_ONLY_HASH);
            assertThat(result.clientIp()).isEqualTo(CLIENT_IP);
            verifyNoMoreInteractions(hashingService);
        }

        @Test
        @DisplayName("should use 'unknown' default when headers are missing")
        void shouldUseDefaults_whenHeadersMissing() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(CLIENT_IP);

            String unknown = DeviceFingerprintService.UNKNOWN_VALUE;
            String deviceComponents = unknown + SEP + unknown + SEP + unknown;
            String fullComponents = CLIENT_IP + SEP + deviceComponents;

            when(hashingService.generateHash(fullComponents)).thenReturn(FULL_HASH);
            when(hashingService.generateHash(deviceComponents)).thenReturn(DEVICE_ONLY_HASH);

            // When
            DeviceFingerprint result = service.computeFingerprint(request);

            // Then
            assertThat(result.fullHash()).isEqualTo(FULL_HASH);
            assertThat(result.deviceOnlyHash()).isEqualTo(DEVICE_ONLY_HASH);
            verify(hashingService, times(1)).generateHash(fullComponents);
            verify(hashingService, times(1)).generateHash(deviceComponents);
            verifyNoMoreInteractions(hashingService);
        }

        @Test
        @DisplayName("should use 'unknown' default when headers are blank")
        void shouldUseDefaults_whenHeadersBlank() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(CLIENT_IP);
            request.addHeader(DeviceFingerprintService.HEADER_USER_AGENT, "   ");
            request.addHeader(DeviceFingerprintService.HEADER_ACCEPT_LANGUAGE, "");

            String unknown = DeviceFingerprintService.UNKNOWN_VALUE;
            String deviceComponents = unknown + SEP + unknown + SEP + unknown;
            String fullComponents = CLIENT_IP + SEP + deviceComponents;

            when(hashingService.generateHash(fullComponents)).thenReturn(FULL_HASH);
            when(hashingService.generateHash(deviceComponents)).thenReturn(DEVICE_ONLY_HASH);

            // When
            DeviceFingerprint result = service.computeFingerprint(request);

            // Then
            assertThat(result.fullHash()).isEqualTo(FULL_HASH);
            verifyNoMoreInteractions(hashingService);
        }

        @Test
        @DisplayName("should extract client IP from X-Forwarded-For header")
        void shouldExtractIp_whenXForwardedForPresent() {
            // Given
            String proxyIp = "10.0.0.1";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("127.0.0.1");
            request.addHeader(DeviceFingerprintService.HEADER_FORWARDED_FOR, proxyIp + ", 10.0.0.2");
            request.addHeader(DeviceFingerprintService.HEADER_USER_AGENT, USER_AGENT);
            request.addHeader(DeviceFingerprintService.HEADER_ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
            request.addHeader(DeviceFingerprintService.HEADER_DEVICE_ID, DEVICE_ID);

            String deviceComponents = USER_AGENT + SEP + ACCEPT_LANGUAGE + SEP + DEVICE_ID;
            String fullComponents = proxyIp + SEP + deviceComponents;

            when(hashingService.generateHash(fullComponents)).thenReturn(FULL_HASH);
            when(hashingService.generateHash(deviceComponents)).thenReturn(DEVICE_ONLY_HASH);

            // When
            DeviceFingerprint result = service.computeFingerprint(request);

            // Then
            assertThat(result.clientIp()).isEqualTo(proxyIp);
            verify(hashingService, times(1)).generateHash(fullComponents);
            verify(hashingService, times(1)).generateHash(deviceComponents);
            verifyNoMoreInteractions(hashingService);
        }

        @Test
        @DisplayName("should use remoteAddr when X-Forwarded-For is absent")
        void shouldUseRemoteAddr_whenXForwardedForAbsent() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(CLIENT_IP);
            request.addHeader(DeviceFingerprintService.HEADER_USER_AGENT, USER_AGENT);
            request.addHeader(DeviceFingerprintService.HEADER_ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
            request.addHeader(DeviceFingerprintService.HEADER_DEVICE_ID, DEVICE_ID);

            String deviceComponents = USER_AGENT + SEP + ACCEPT_LANGUAGE + SEP + DEVICE_ID;
            String fullComponents = CLIENT_IP + SEP + deviceComponents;

            when(hashingService.generateHash(fullComponents)).thenReturn(FULL_HASH);
            when(hashingService.generateHash(deviceComponents)).thenReturn(DEVICE_ONLY_HASH);

            // When
            DeviceFingerprint result = service.computeFingerprint(request);

            // Then
            assertThat(result.clientIp()).isEqualTo(CLIENT_IP);
            verifyNoMoreInteractions(hashingService);
        }

        @Test
        @DisplayName("should produce different full hashes for different IPs with same device")
        void shouldProduceDifferentFullHashes_whenIpsDiffer() {
            // Given
            String ip1 = "10.0.0.1";
            String ip2 = "10.0.0.2";
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            request1.setRemoteAddr(ip1);
            request1.addHeader(DeviceFingerprintService.HEADER_USER_AGENT, USER_AGENT);
            request1.addHeader(DeviceFingerprintService.HEADER_ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
            request1.addHeader(DeviceFingerprintService.HEADER_DEVICE_ID, DEVICE_ID);

            MockHttpServletRequest request2 = new MockHttpServletRequest();
            request2.setRemoteAddr(ip2);
            request2.addHeader(DeviceFingerprintService.HEADER_USER_AGENT, USER_AGENT);
            request2.addHeader(DeviceFingerprintService.HEADER_ACCEPT_LANGUAGE, ACCEPT_LANGUAGE);
            request2.addHeader(DeviceFingerprintService.HEADER_DEVICE_ID, DEVICE_ID);

            String deviceComponents = USER_AGENT + SEP + ACCEPT_LANGUAGE + SEP + DEVICE_ID;
            String full1 = ip1 + SEP + deviceComponents;
            String full2 = ip2 + SEP + deviceComponents;

            when(hashingService.generateHash(full1)).thenReturn("hash-ip1");
            when(hashingService.generateHash(full2)).thenReturn("hash-ip2");
            when(hashingService.generateHash(deviceComponents)).thenReturn(DEVICE_ONLY_HASH);

            // When
            DeviceFingerprint result1 = service.computeFingerprint(request1);
            DeviceFingerprint result2 = service.computeFingerprint(request2);

            // Then
            assertThat(result1.fullHash()).isNotEqualTo(result2.fullHash());
            assertThat(result1.deviceOnlyHash()).isEqualTo(result2.deviceOnlyHash());
            verifyNoMoreInteractions(hashingService);
        }
    }
}
