/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.interfaceadapters;

import com.akademiaplus.config.RateLimitProperties;
import com.akademiaplus.ratelimit.usecases.RateLimiterService;
import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
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

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitingFilter}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("RateLimitingFilter")
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private static final String CLIENT_IP = "192.168.1.100";
    private static final String USERNAME = "testuser@example.com";
    private static final int LOGIN_LIMIT = 5;
    private static final long LOGIN_WINDOW_MS = 900000L;
    private static final int PUBLIC_LIMIT = 20;
    private static final long PUBLIC_WINDOW_MS = 60000L;
    private static final int AUTH_LIMIT = 100;
    private static final long AUTH_WINDOW_MS = 60000L;
    private static final long RESET_EPOCH = Instant.now().getEpochSecond() + 60;

    @Mock private RateLimiterService rateLimiterService;
    @Mock private FilterChain filterChain;

    private RateLimitingFilter filter;
    private final ObjectMapper objectMapper = new JsonMapper();

    private RateLimitProperties createEnabledProperties() {
        return new RateLimitProperties(true, Map.of(
                RateLimitingFilter.TIER_LOGIN, new RateLimitProperties.TierProperties(LOGIN_LIMIT, LOGIN_WINDOW_MS),
                RateLimitingFilter.TIER_PUBLIC, new RateLimitProperties.TierProperties(PUBLIC_LIMIT, PUBLIC_WINDOW_MS),
                RateLimitingFilter.TIER_AUTHENTICATED, new RateLimitProperties.TierProperties(AUTH_LIMIT, AUTH_WINDOW_MS)
        ));
    }

    private RateLimitProperties createDisabledProperties() {
        return new RateLimitProperties(false, Map.of());
    }

    @Nested
    @DisplayName("Bypass")
    class Bypass {

        @Test
        @DisplayName("Should bypass filter when path is actuator")
        void shouldBypassFilter_whenPathIsActuator() {
            // Given
            filter = new RateLimitingFilter(rateLimiterService, createEnabledProperties(), objectMapper);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/actuator/health");

            // When
            boolean result = filter.shouldNotFilter(request);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should bypass filter when path is swagger")
        void shouldBypassFilter_whenPathIsSwagger() {
            // Given
            filter = new RateLimitingFilter(rateLimiterService, createEnabledProperties(), objectMapper);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/swagger-ui/index.html");

            // When
            boolean result = filter.shouldNotFilter(request);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should pass through when rate limiting is disabled")
        void shouldPassThrough_whenRateLimitingDisabled() throws Exception {
            // Given
            filter = new RateLimitingFilter(rateLimiterService, createDisabledProperties(), objectMapper);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/security/login/internal");
            request.setRemoteAddr(CLIENT_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(rateLimiterService);
        }
    }

    @Nested
    @DisplayName("Tier Resolution")
    class TierResolution {

        @BeforeEach
        void setUp() {
            filter = new RateLimitingFilter(rateLimiterService, createEnabledProperties(), objectMapper);
        }

        @Test
        @DisplayName("Should use login tier when path is login")
        void shouldUseLoginTier_whenPathIsLogin() {
            // When
            String tier = filter.resolveTier("/v1/security/login/internal");

            // Then
            assertThat(tier).isEqualTo(RateLimitingFilter.TIER_LOGIN);
        }

        @Test
        @DisplayName("Should use login tier when path is passkey login")
        void shouldUseLoginTier_whenPathIsPasskeyLogin() {
            // When
            String tier = filter.resolveTier("/v1/security/passkey/login/options");

            // Then
            assertThat(tier).isEqualTo(RateLimitingFilter.TIER_LOGIN);
        }

        @Test
        @DisplayName("Should use public tier when path is register")
        void shouldUsePublicTier_whenPathIsRegister() {
            // When
            String tier = filter.resolveTier("/v1/security/register");

            // Then
            assertThat(tier).isEqualTo(RateLimitingFilter.TIER_PUBLIC);
        }

        @Test
        @DisplayName("Should use authenticated tier when path is other")
        void shouldUseAuthenticatedTier_whenPathIsOther() {
            // When
            String tier = filter.resolveTier("/v1/students");

            // Then
            assertThat(tier).isEqualTo(RateLimitingFilter.TIER_AUTHENTICATED);
        }
    }

    @Nested
    @DisplayName("Allowed Request")
    class AllowedRequest {

        @BeforeEach
        void setUp() {
            filter = new RateLimitingFilter(rateLimiterService, createEnabledProperties(), objectMapper);
        }

        @Test
        @DisplayName("Should continue filter chain when request is allowed")
        void shouldContinueFilterChain_whenRequestAllowed() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/security/login/internal");
            request.setRemoteAddr(CLIENT_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();

            RateLimitResult result = new RateLimitResult(true, LOGIN_LIMIT, 4, RESET_EPOCH);
            when(rateLimiterService.checkRateLimit(
                    RateLimiterService.KEY_PREFIX_IP + CLIENT_IP + ":" + RateLimitingFilter.TIER_LOGIN,
                    LOGIN_LIMIT, LOGIN_WINDOW_MS))
                    .thenReturn(result);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should set rate limit headers when request is allowed")
        void shouldSetRateLimitHeaders_whenRequestAllowed() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/security/login/internal");
            request.setRemoteAddr(CLIENT_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();

            RateLimitResult result = new RateLimitResult(true, LOGIN_LIMIT, 4, RESET_EPOCH);
            when(rateLimiterService.checkRateLimit(
                    RateLimiterService.KEY_PREFIX_IP + CLIENT_IP + ":" + RateLimitingFilter.TIER_LOGIN,
                    LOGIN_LIMIT, LOGIN_WINDOW_MS))
                    .thenReturn(result);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getHeader(RateLimitingFilter.HEADER_RATE_LIMIT)).isEqualTo(String.valueOf(LOGIN_LIMIT));
            assertThat(response.getHeader(RateLimitingFilter.HEADER_RATE_REMAINING)).isEqualTo("4");
            assertThat(response.getHeader(RateLimitingFilter.HEADER_RATE_RESET)).isEqualTo(String.valueOf(RESET_EPOCH));
        }
    }

    @Nested
    @DisplayName("Rejected Request")
    class RejectedRequest {

        @BeforeEach
        void setUp() {
            filter = new RateLimitingFilter(rateLimiterService, createEnabledProperties(), objectMapper);
        }

        @Test
        @DisplayName("Should return 429 when rate limit is exceeded")
        void shouldReturn429_whenRateLimitExceeded() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/security/login/internal");
            request.setRemoteAddr(CLIENT_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();

            RateLimitResult result = new RateLimitResult(false, LOGIN_LIMIT, 0, RESET_EPOCH);
            when(rateLimiterService.checkRateLimit(
                    RateLimiterService.KEY_PREFIX_IP + CLIENT_IP + ":" + RateLimitingFilter.TIER_LOGIN,
                    LOGIN_LIMIT, LOGIN_WINDOW_MS))
                    .thenReturn(result);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(429);
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("Should set Retry-After header when rate limit is exceeded")
        void shouldSetRetryAfterHeader_whenRateLimitExceeded() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/security/login/internal");
            request.setRemoteAddr(CLIENT_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();

            long futureReset = Instant.now().getEpochSecond() + 120;
            RateLimitResult result = new RateLimitResult(false, LOGIN_LIMIT, 0, futureReset);
            when(rateLimiterService.checkRateLimit(
                    RateLimiterService.KEY_PREFIX_IP + CLIENT_IP + ":" + RateLimitingFilter.TIER_LOGIN,
                    LOGIN_LIMIT, LOGIN_WINDOW_MS))
                    .thenReturn(result);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            String retryAfter = response.getHeader(RateLimitingFilter.HEADER_RETRY_AFTER);
            assertThat(retryAfter).isNotNull();
            assertThat(Long.parseLong(retryAfter)).isPositive();
        }

        @Test
        @DisplayName("Should write JSON error body when rate limit is exceeded")
        void shouldWriteJsonErrorBody_whenRateLimitExceeded() throws Exception {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/security/login/internal");
            request.setRemoteAddr(CLIENT_IP);
            MockHttpServletResponse response = new MockHttpServletResponse();

            RateLimitResult result = new RateLimitResult(false, LOGIN_LIMIT, 0, RESET_EPOCH);
            when(rateLimiterService.checkRateLimit(
                    RateLimiterService.KEY_PREFIX_IP + CLIENT_IP + ":" + RateLimitingFilter.TIER_LOGIN,
                    LOGIN_LIMIT, LOGIN_WINDOW_MS))
                    .thenReturn(result);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            String body = response.getContentAsString();
            assertThat(body).contains(RateLimitingFilter.ERROR_TOO_MANY_REQUESTS);
            assertThat(body).contains(RateLimitingFilter.ERROR_RATE_LIMIT_MESSAGE);
        }
    }

    @Nested
    @DisplayName("Key Resolution")
    class KeyResolution {

        @BeforeEach
        void setUp() {
            filter = new RateLimitingFilter(rateLimiterService, createEnabledProperties(), objectMapper);
        }

        @Test
        @DisplayName("Should use IP key when request is unauthenticated")
        void shouldUseIpKey_whenRequestIsUnauthenticated() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(CLIENT_IP);

            // When
            String key = filter.resolveKey(request, RateLimitingFilter.TIER_LOGIN);

            // Then
            assertThat(key).isEqualTo(RateLimiterService.KEY_PREFIX_IP + CLIENT_IP + ":" + RateLimitingFilter.TIER_LOGIN);
        }

        @Test
        @DisplayName("Should use user key when request is authenticated")
        void shouldUseUserKey_whenRequestIsAuthenticated() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(CLIENT_IP);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(USERNAME, USERNAME, Collections.emptyList()));

            // When
            String key = filter.resolveKey(request, RateLimitingFilter.TIER_AUTHENTICATED);

            // Then
            assertThat(key).isEqualTo(RateLimiterService.KEY_PREFIX_USER + USERNAME);
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should use X-Forwarded-For when header is present")
        void shouldUseXForwardedFor_whenHeaderPresent() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0.1");
            request.addHeader(RateLimitingFilter.HEADER_X_FORWARDED_FOR, CLIENT_IP + ", 10.0.0.1");

            // When
            String key = filter.resolveKey(request, RateLimitingFilter.TIER_LOGIN);

            // Then
            assertThat(key).isEqualTo(RateLimiterService.KEY_PREFIX_IP + CLIENT_IP + ":" + RateLimitingFilter.TIER_LOGIN);
        }

        @Test
        @DisplayName("Should fall back to IP key when authenticated tier has no auth context")
        void shouldFallBackToIpKey_whenAuthenticatedTierHasNoAuthContext() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(CLIENT_IP);
            SecurityContextHolder.clearContext();

            // When
            String key = filter.resolveKey(request, RateLimitingFilter.TIER_AUTHENTICATED);

            // Then
            assertThat(key).isEqualTo(RateLimiterService.KEY_PREFIX_IP + CLIENT_IP + ":" + RateLimitingFilter.TIER_AUTHENTICATED);
        }
    }
}
