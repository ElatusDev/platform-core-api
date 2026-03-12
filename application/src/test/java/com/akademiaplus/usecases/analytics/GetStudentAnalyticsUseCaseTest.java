/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.StudentAnalyticsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("GetStudentAnalyticsUseCase")
@ExtendWith(MockitoExtension.class)
class GetStudentAnalyticsUseCaseTest {

    @Mock private AnalyticsQueryService queryService;

    private GetStudentAnalyticsUseCase useCase;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new GetStudentAnalyticsUseCase(queryService);
    }

    @Nested
    @DisplayName("Student Analytics")
    class StudentAnalytics {

        @Test
        @DisplayName("Should return student counts and enrollment trend")
        void shouldReturnStudentCounts_whenQueried() {
            // Given
            when(queryService.countAdultStudents(TENANT_ID)).thenReturn(20L);
            when(queryService.countMinorStudents(TENANT_ID)).thenReturn(15L);
            when(queryService.countNewStudentsThisMonth(TENANT_ID)).thenReturn(5L);
            List<Object[]> trend = List.<Object[]>of(
                    new Object[]{"2026-01", 3L},
                    new Object[]{"2026-02", 7L}
            );
            when(queryService.getEnrollmentTrend(TENANT_ID)).thenReturn(trend);

            // When
            StudentAnalyticsDTO result = useCase.get(TENANT_ID);

            // Then
            assertThat(result.getAdultCount()).isEqualTo(20);
            assertThat(result.getMinorCount()).isEqualTo(15);
            assertThat(result.getNewThisMonth()).isEqualTo(5);
            assertThat(result.getEnrollmentTrend()).hasSize(2);
            assertThat(result.getEnrollmentTrend().getFirst().getMonth()).isEqualTo("2026-01");
        }

        @Test
        @DisplayName("Should return empty trend when no historical data")
        void shouldReturnEmptyTrend_whenNoData() {
            // Given
            when(queryService.countAdultStudents(TENANT_ID)).thenReturn(0L);
            when(queryService.countMinorStudents(TENANT_ID)).thenReturn(0L);
            when(queryService.countNewStudentsThisMonth(TENANT_ID)).thenReturn(0L);
            when(queryService.getEnrollmentTrend(TENANT_ID)).thenReturn(Collections.emptyList());

            // When
            StudentAnalyticsDTO result = useCase.get(TENANT_ID);

            // Then
            assertThat(result.getEnrollmentTrend()).isEmpty();
        }
    }
}
