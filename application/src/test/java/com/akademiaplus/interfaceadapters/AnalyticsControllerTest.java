/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.usecases.analytics.GetCourseAnalyticsUseCase;
import com.akademiaplus.usecases.analytics.GetOverviewAnalyticsUseCase;
import com.akademiaplus.usecases.analytics.GetRevenueAnalyticsUseCase;
import com.akademiaplus.usecases.analytics.GetStaffAnalyticsUseCase;
import com.akademiaplus.usecases.analytics.GetStudentAnalyticsUseCase;
import openapi.akademiaplus.domain.analytics.dto.CourseAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.OverviewAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.RevenueAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.StaffAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.StudentAnalyticsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AnalyticsController")
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock private GetOverviewAnalyticsUseCase overviewUseCase;
    @Mock private GetStudentAnalyticsUseCase studentUseCase;
    @Mock private GetCourseAnalyticsUseCase courseUseCase;
    @Mock private GetStaffAnalyticsUseCase staffUseCase;
    @Mock private GetRevenueAnalyticsUseCase revenueUseCase;
    @Mock private TenantContextHolder tenantContextHolder;

    private AnalyticsController controller;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(
                overviewUseCase, studentUseCase, courseUseCase,
                staffUseCase, revenueUseCase, tenantContextHolder);
    }

    @Nested
    @DisplayName("Overview")
    class Overview {

        @Test
        @DisplayName("Should return 200 with overview analytics")
        void shouldReturn200_whenOverviewRequested() {
            // Given
            OverviewAnalyticsDTO dto = new OverviewAnalyticsDTO();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(overviewUseCase.get(TENANT_ID)).thenReturn(dto);

            // When
            ResponseEntity<OverviewAnalyticsDTO> response = controller.getOverviewAnalytics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(overviewUseCase, times(1)).get(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Students")
    class Students {

        @Test
        @DisplayName("Should return 200 with student analytics")
        void shouldReturn200_whenStudentsRequested() {
            // Given
            StudentAnalyticsDTO dto = new StudentAnalyticsDTO();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(studentUseCase.get(TENANT_ID)).thenReturn(dto);

            // When
            ResponseEntity<StudentAnalyticsDTO> response = controller.getStudentAnalytics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(studentUseCase, times(1)).get(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Courses")
    class Courses {

        @Test
        @DisplayName("Should return 200 with course analytics")
        void shouldReturn200_whenCoursesRequested() {
            // Given
            CourseAnalyticsDTO dto = new CourseAnalyticsDTO();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(courseUseCase.get(TENANT_ID)).thenReturn(dto);

            // When
            ResponseEntity<CourseAnalyticsDTO> response = controller.getCourseAnalytics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(courseUseCase, times(1)).get(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Staff")
    class Staff {

        @Test
        @DisplayName("Should return 200 with staff analytics")
        void shouldReturn200_whenStaffRequested() {
            // Given
            StaffAnalyticsDTO dto = new StaffAnalyticsDTO();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(staffUseCase.get(TENANT_ID)).thenReturn(dto);

            // When
            ResponseEntity<StaffAnalyticsDTO> response = controller.getStaffAnalytics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(staffUseCase, times(1)).get(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Revenue")
    class Revenue {

        @Test
        @DisplayName("Should return 200 with revenue analytics")
        void shouldReturn200_whenRevenueRequested() {
            // Given
            RevenueAnalyticsDTO dto = new RevenueAnalyticsDTO();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(revenueUseCase.get(TENANT_ID)).thenReturn(dto);

            // When
            ResponseEntity<RevenueAnalyticsDTO> response = controller.getRevenueAnalytics();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(revenueUseCase, times(1)).get(TENANT_ID);
        }
    }
}
