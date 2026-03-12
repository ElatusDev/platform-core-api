/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.OverviewAnalyticsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("GetOverviewAnalyticsUseCase")
@ExtendWith(MockitoExtension.class)
class GetOverviewAnalyticsUseCaseTest {

    @Mock private AnalyticsQueryService queryService;

    private GetOverviewAnalyticsUseCase useCase;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new GetOverviewAnalyticsUseCase(queryService);
    }

    @Nested
    @DisplayName("Overview Aggregation")
    class OverviewAggregation {

        @Test
        @DisplayName("Should aggregate total students from adult and minor counts")
        void shouldAggregateTotalStudents_whenQueried() {
            // Given
            when(queryService.countAdultStudents(TENANT_ID)).thenReturn(15L);
            when(queryService.countMinorStudents(TENANT_ID)).thenReturn(10L);
            when(queryService.getRevenueMTD(TENANT_ID)).thenReturn(new BigDecimal("5000.00"));
            when(queryService.getCourseUtilization(TENANT_ID)).thenReturn(75.0);
            when(queryService.countEmployees(TENANT_ID)).thenReturn(3L);
            when(queryService.countTutors(TENANT_ID)).thenReturn(2L);
            when(queryService.countCollaborators(TENANT_ID)).thenReturn(4L);
            when(queryService.getMembershipRenewalRate(TENANT_ID)).thenReturn(80.0);

            // When
            OverviewAnalyticsDTO result = useCase.get(TENANT_ID);

            // Then
            assertThat(result.getTotalStudents()).isEqualTo(25);
            assertThat(result.getRevenueMTD()).isEqualTo(5000.0);
            assertThat(result.getCourseUtilization()).isEqualTo(75.0);
            assertThat(result.getStaffCount()).isEqualTo(9);
            assertThat(result.getMembershipRenewalRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Should return zeros when no data exists")
        void shouldReturnZeros_whenNoData() {
            // Given
            when(queryService.countAdultStudents(TENANT_ID)).thenReturn(0L);
            when(queryService.countMinorStudents(TENANT_ID)).thenReturn(0L);
            when(queryService.getRevenueMTD(TENANT_ID)).thenReturn(BigDecimal.ZERO);
            when(queryService.getCourseUtilization(TENANT_ID)).thenReturn(0.0);
            when(queryService.countEmployees(TENANT_ID)).thenReturn(0L);
            when(queryService.countTutors(TENANT_ID)).thenReturn(0L);
            when(queryService.countCollaborators(TENANT_ID)).thenReturn(0L);
            when(queryService.getMembershipRenewalRate(TENANT_ID)).thenReturn(0.0);

            // When
            OverviewAnalyticsDTO result = useCase.get(TENANT_ID);

            // Then
            assertThat(result.getTotalStudents()).isZero();
            assertThat(result.getStaffCount()).isZero();
        }
    }
}
