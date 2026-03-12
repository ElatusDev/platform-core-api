/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.CourseAnalyticsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("GetCourseAnalyticsUseCase")
@ExtendWith(MockitoExtension.class)
class GetCourseAnalyticsUseCaseTest {

    @Mock private AnalyticsQueryService queryService;

    private GetCourseAnalyticsUseCase useCase;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new GetCourseAnalyticsUseCase(queryService);
    }

    @Nested
    @DisplayName("Course Analytics")
    class CourseAnalytics {

        @Test
        @DisplayName("Should return course enrollment and capacity data")
        void shouldReturnCourseData_whenQueried() {
            // Given
            List<Object[]> enrollments = List.<Object[]>of(new Object[]{1L, "Math 101", 15L});
            List<Object[]> capacity = List.<Object[]>of(new Object[]{1L, "Math 101", 30, 15L});
            List<Object[]> schedule = List.<Object[]>of(new Object[]{"MONDAY", 3L});
            when(queryService.getCourseEnrollments(TENANT_ID)).thenReturn(enrollments);
            when(queryService.getCourseCapacity(TENANT_ID)).thenReturn(capacity);
            when(queryService.getScheduleByDayOfWeek(TENANT_ID)).thenReturn(schedule);

            // When
            CourseAnalyticsDTO result = useCase.get(TENANT_ID);

            // Then
            assertThat(result.getEnrollmentByCourse()).hasSize(1);
            assertThat(result.getEnrollmentByCourse().getFirst().getCourseName()).isEqualTo("Math 101");
            assertThat(result.getCapacityUtilization()).hasSize(1);
            assertThat(result.getCapacityUtilization().getFirst().getUtilizationPercent()).isEqualTo(50.0);
            assertThat(result.getScheduleByDayOfWeek()).hasSize(1);
        }
    }
}
