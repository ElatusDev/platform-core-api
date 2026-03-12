/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.usecases;

import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimiterService}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("RateLimiterService")
@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    private static final String TEST_KEY = "rate:ip:192.168.1.1:login";
    private static final int LIMIT = 5;
    private static final long WINDOW_MS = 60000L;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ZSetOperations<String, String> zSetOps;

    private RateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RateLimiterService(redisTemplate);
    }

    @Nested
    @DisplayName("Allowed Requests")
    class AllowedRequests {

        @Test
        @DisplayName("Should allow request when count is below limit")
        void shouldAllowRequest_whenCountBelowLimit() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(2L);

            // When
            RateLimitResult result = service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            assertThat(result.allowed()).isTrue();
            assertThat(result.limit()).isEqualTo(LIMIT);
            assertThat(result.remaining()).isEqualTo(2);
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verify(zSetOps, times(1)).add(eq(TEST_KEY), anyString(), anyDouble());
            verify(redisTemplate, times(1)).expire(eq(TEST_KEY), anyLong(), eq(TimeUnit.SECONDS));
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }

        @Test
        @DisplayName("Should set remaining to zero when count reaches limit minus one")
        void shouldSetRemainingToZero_whenCountReachesLimitMinusOne() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(4L);

            // When
            RateLimitResult result = service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(0);
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verify(zSetOps, times(1)).add(eq(TEST_KEY), anyString(), anyDouble());
            verify(redisTemplate, times(1)).expire(eq(TEST_KEY), anyLong(), eq(TimeUnit.SECONDS));
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }

        @Test
        @DisplayName("Should allow request when count is zero")
        void shouldAllowRequest_whenCountIsZero() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(0L);

            // When
            RateLimitResult result = service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(4);
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verify(zSetOps, times(1)).add(eq(TEST_KEY), anyString(), anyDouble());
            verify(redisTemplate, times(1)).expire(eq(TEST_KEY), anyLong(), eq(TimeUnit.SECONDS));
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }
    }

    @Nested
    @DisplayName("Exceeded Requests")
    class ExceededRequests {

        @Test
        @DisplayName("Should reject request when count is at limit")
        void shouldRejectRequest_whenCountAtLimit() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(5L);

            // When
            RateLimitResult result = service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            assertThat(result.allowed()).isFalse();
            assertThat(result.remaining()).isEqualTo(0);
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }

        @Test
        @DisplayName("Should reject request when count is above limit")
        void shouldRejectRequest_whenCountAboveLimit() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(10L);

            // When
            RateLimitResult result = service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            assertThat(result.allowed()).isFalse();
            assertThat(result.remaining()).isEqualTo(0);
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }

        @Test
        @DisplayName("Should not add entry when request is rejected")
        void shouldNotAddEntry_whenRequestIsRejected() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(5L);

            // When
            service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            verify(zSetOps, never()).add(eq(TEST_KEY), anyString(), anyDouble());
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }
    }

    @Nested
    @DisplayName("Sliding Window")
    class SlidingWindow {

        @Test
        @DisplayName("Should remove expired entries when checking rate limit")
        void shouldRemoveExpiredEntries_whenCheckingRateLimit() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(0L);

            // When
            service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verify(zSetOps, times(1)).add(eq(TEST_KEY), anyString(), anyDouble());
            verify(redisTemplate, times(1)).expire(eq(TEST_KEY), anyLong(), eq(TimeUnit.SECONDS));
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }

        @Test
        @DisplayName("Should set key expiry when request is allowed")
        void shouldSetKeyExpiry_whenRequestAllowed() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(0L);

            // When
            service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            long expectedTtl = TimeUnit.MILLISECONDS.toSeconds(WINDOW_MS) + 1L;
            verify(redisTemplate, times(1)).expire(TEST_KEY, expectedTtl, TimeUnit.SECONDS);
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verify(zSetOps, times(1)).add(eq(TEST_KEY), anyString(), anyDouble());
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }

        @Test
        @DisplayName("Should add entry with timestamp score when request is allowed")
        void shouldAddEntryWithTimestampScore_whenRequestAllowed() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(0L);

            // When
            service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
            verify(zSetOps, times(1)).add(eq(TEST_KEY), anyString(), scoreCaptor.capture());
            assertThat(scoreCaptor.getValue()).isPositive();
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verify(redisTemplate, times(1)).expire(eq(TEST_KEY), anyLong(), eq(TimeUnit.SECONDS));
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }
    }

    @Nested
    @DisplayName("Null count handling")
    class NullCountHandling {

        @Test
        @DisplayName("Should treat null count as zero")
        void shouldTreatNullCountAsZero() {
            // Given
            when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
            when(zSetOps.zCard(TEST_KEY)).thenReturn(null);

            // When
            RateLimitResult result = service.checkRateLimit(TEST_KEY, LIMIT, WINDOW_MS);

            // Then
            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(4);
            verify(zSetOps, times(1)).removeRangeByScore(eq(TEST_KEY), eq(0.0), anyDouble());
            verify(zSetOps, times(1)).zCard(TEST_KEY);
            verify(zSetOps, times(1)).add(eq(TEST_KEY), anyString(), anyDouble());
            verify(redisTemplate, times(1)).expire(eq(TEST_KEY), anyLong(), eq(TimeUnit.SECONDS));
            verifyNoMoreInteractions(redisTemplate, zSetOps);
        }
    }
}
