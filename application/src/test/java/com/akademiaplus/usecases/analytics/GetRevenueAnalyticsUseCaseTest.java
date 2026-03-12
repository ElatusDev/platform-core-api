/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.RevenueAnalyticsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("GetRevenueAnalyticsUseCase")
@ExtendWith(MockitoExtension.class)
class GetRevenueAnalyticsUseCaseTest {

    @Mock private AnalyticsQueryService queryService;

    private GetRevenueAnalyticsUseCase useCase;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new GetRevenueAnalyticsUseCase(queryService);
    }

    @Nested
    @DisplayName("Revenue Analytics")
    class RevenueAnalytics {

        @Test
        @DisplayName("Should return revenue trend, breakdown, and outstanding payments")
        void shouldReturnRevenueData_whenQueried() {
            // Given
            List<Object[]> trend = List.<Object[]>of(
                    new Object[]{"2026-01", new BigDecimal("10000.00")},
                    new Object[]{"2026-02", new BigDecimal("12000.00")}
            );
            List<Object[]> breakdown = List.<Object[]>of(
                    new Object[]{"credit_card", new BigDecimal("8000.00")},
                    new Object[]{"cash", new BigDecimal("4000.00")}
            );
            when(queryService.getRevenueTrend(TENANT_ID)).thenReturn(trend);
            when(queryService.getRevenueByType(TENANT_ID)).thenReturn(breakdown);
            when(queryService.getOutstandingPayments(TENANT_ID)).thenReturn(new BigDecimal("3500.00"));

            // When
            RevenueAnalyticsDTO result = useCase.get(TENANT_ID);

            // Then
            assertThat(result.getRevenueTrend()).hasSize(2);
            assertThat(result.getBreakdownByType()).hasSize(2);
            assertThat(result.getOutstandingPayments()).isEqualTo(3500.0);
        }
    }
}
