/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.internal.interfaceadapters.filters.AppOriginContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.mockito.InOrder;

/**
 * Unit tests for {@link IpWhitelistFilter}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("IpWhitelistFilter")
@ExtendWith(MockitoExtension.class)
class IpWhitelistFilterTest {

    private static final String ALLOWED_IP = "192.168.1.100";
    private static final String BLOCKED_IP = "203.0.113.50";
    private static final String CIDR_RANGE = "192.168.1.0/24";

    @Mock private IpWhitelistProperties properties;
    @Mock private FilterChain filterChain;

    private IpWhitelistFilter filter;
    private final ObjectMapper objectMapper = new JsonMapper();

    @BeforeEach
    void setUp() {
        filter = new IpWhitelistFilter(properties, objectMapper);
    }

    private MockHttpServletRequest createAkademiaRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_AKADEMIA);
        return request;
    }

    @Nested
    @DisplayName("AkademiaPlus origin — allowed IP")
    class AkademiaPlusAllowedIp {

        @Test
        @DisplayName("Should pass filter when IP is within allowed CIDR range")
        void shouldPassFilter_whenIpIsWithinAllowedCidr() throws Exception {
            // Given
            MockHttpServletRequest request = createAkademiaRequest(ALLOWED_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(properties.getAllowedCidrs()).thenReturn(List.of(CIDR_RANGE));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            InOrder inOrder = inOrder(properties, filterChain);
            inOrder.verify(properties, times(1)).getAllowedCidrs();
            inOrder.verify(filterChain, times(1)).doFilter(request, response);
            verifyNoMoreInteractions(properties, filterChain);
        }

        @Test
        @DisplayName("Should extract first IP from X-Forwarded-For header")
        void shouldExtractFirstIp_whenXForwardedForHasMultipleIps() throws Exception {
            // Given
            MockHttpServletRequest request = createAkademiaRequest("10.0.0.1");
            request.addHeader(IpWhitelistFilter.HEADER_X_FORWARDED_FOR, ALLOWED_IP + ", 10.0.0.1, 172.16.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(properties.getAllowedCidrs()).thenReturn(List.of(CIDR_RANGE));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            InOrder inOrder = inOrder(properties, filterChain);
            inOrder.verify(properties, times(1)).getAllowedCidrs();
            inOrder.verify(filterChain, times(1)).doFilter(request, response);
            verifyNoMoreInteractions(properties, filterChain);
        }

        @Test
        @DisplayName("Should use remoteAddr when X-Forwarded-For is absent")
        void shouldUseRemoteAddr_whenXForwardedForIsAbsent() throws Exception {
            // Given
            MockHttpServletRequest request = createAkademiaRequest(ALLOWED_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(properties.getAllowedCidrs()).thenReturn(List.of(CIDR_RANGE));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            InOrder inOrder = inOrder(properties, filterChain);
            inOrder.verify(properties, times(1)).getAllowedCidrs();
            inOrder.verify(filterChain, times(1)).doFilter(request, response);
            verifyNoMoreInteractions(properties, filterChain);
        }
    }

    @Nested
    @DisplayName("AkademiaPlus origin — blocked IP")
    class AkademiaPlusBlockedIp {

        @Test
        @DisplayName("Should return 403 when IP is not in allowed CIDR ranges")
        void shouldReturn403_whenIpIsNotInAllowedCidrs() throws Exception {
            // Given
            MockHttpServletRequest request = createAkademiaRequest(BLOCKED_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(properties.getAllowedCidrs()).thenReturn(List.of(CIDR_RANGE));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            verify(properties, times(1)).getAllowedCidrs();
            verifyNoMoreInteractions(properties, filterChain);
        }

        @Test
        @DisplayName("Should write JSON error body when IP is rejected")
        void shouldWriteJsonErrorBody_whenIpIsRejected() throws Exception {
            // Given
            MockHttpServletRequest request = createAkademiaRequest(BLOCKED_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(properties.getAllowedCidrs()).thenReturn(List.of(CIDR_RANGE));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            String body = response.getContentAsString();
            assertThat(body).contains(IpWhitelistFilter.CODE_IP_REJECTED);
            assertThat(body).contains(IpWhitelistFilter.ERROR_IP_NOT_ALLOWED);
            verify(properties, times(1)).getAllowedCidrs();
            verifyNoMoreInteractions(properties, filterChain);
        }

        @Test
        @DisplayName("Should not invoke filter chain when IP is rejected")
        void shouldNotInvokeFilterChain_whenIpIsRejected() throws Exception {
            // Given
            MockHttpServletRequest request = createAkademiaRequest(BLOCKED_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(properties.getAllowedCidrs()).thenReturn(List.of(CIDR_RANGE));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            verify(properties, times(1)).getAllowedCidrs();
            verifyNoMoreInteractions(properties, filterChain);
        }

        @Test
        @DisplayName("Should return 403 when CIDR list is empty (fail-secure)")
        void shouldReturn403_whenCidrListIsEmpty() throws Exception {
            // Given
            MockHttpServletRequest request = createAkademiaRequest(ALLOWED_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(properties.getAllowedCidrs()).thenReturn(List.of());

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            verify(properties, times(1)).getAllowedCidrs();
            verifyNoMoreInteractions(properties, filterChain);
        }
    }

    @Nested
    @DisplayName("Non-AkademiaPlus origin")
    class NonAkademiaPlusOrigin {

        @Test
        @DisplayName("Should pass filter when origin is elatus")
        void shouldPassFilter_whenOriginIsElatus() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(BLOCKED_IP);
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_ELATUS);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(properties);
            verifyNoMoreInteractions(properties, filterChain);
        }

        @Test
        @DisplayName("Should pass filter when origin attribute is not set")
        void shouldPassFilter_whenOriginAttributeIsNotSet() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(BLOCKED_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            verify(filterChain, times(1)).doFilter(request, response);
            verifyNoInteractions(properties);
            verifyNoMoreInteractions(properties, filterChain);
        }
    }

    @Nested
    @DisplayName("Bypass paths")
    class BypassPaths {

        @Test
        @DisplayName("Should skip filter for actuator endpoints")
        void shouldSkipFilter_whenPathIsActuator() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/actuator/health");

            // When
            boolean result = filter.shouldNotFilter(request);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should skip filter for swagger endpoints")
        void shouldSkipFilter_whenPathIsSwagger() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/swagger-ui/index.html");

            // When
            boolean result = filter.shouldNotFilter(request);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should skip filter for api-docs endpoints")
        void shouldSkipFilter_whenPathIsApiDocs() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v3/api-docs/swagger-config");

            // When
            boolean result = filter.shouldNotFilter(request);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should not skip filter for regular API endpoints")
        void shouldNotSkipFilter_whenPathIsRegularApi() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/security/login/internal");

            // When
            boolean result = filter.shouldNotFilter(request);

            // Then
            assertThat(result).isFalse();
        }
    }
}
