/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.StaffAnalyticsDTO;
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

@DisplayName("GetStaffAnalyticsUseCase")
@ExtendWith(MockitoExtension.class)
class GetStaffAnalyticsUseCaseTest {

    @Mock private AnalyticsQueryService queryService;

    private GetStaffAnalyticsUseCase useCase;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new GetStaffAnalyticsUseCase(queryService);
    }

    @Nested
    @DisplayName("Staff Analytics")
    class StaffAnalytics {

        @Test
        @DisplayName("Should return staff counts and distribution by role")
        void shouldReturnStaffCounts_whenQueried() {
            // Given
            when(queryService.countEmployees(TENANT_ID)).thenReturn(5L);
            when(queryService.countTutors(TENANT_ID)).thenReturn(8L);
            when(queryService.countCollaborators(TENANT_ID)).thenReturn(3L);
            List<Object[]> distribution = List.<Object[]>of(
                    new Object[]{"MANAGER", 2L},
                    new Object[]{"INSTRUCTOR", 3L}
            );
            when(queryService.getEmployeeDistributionByRole(TENANT_ID)).thenReturn(distribution);

            // When
            StaffAnalyticsDTO result = useCase.get(TENANT_ID);

            // Then
            assertThat(result.getEmployeeCount()).isEqualTo(5);
            assertThat(result.getTutorCount()).isEqualTo(8);
            assertThat(result.getCollaboratorCount()).isEqualTo(3);
            assertThat(result.getDistributionByRole()).hasSize(2);
        }
    }
}
