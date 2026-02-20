/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetScheduleResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetScheduleByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetScheduleByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long SCHEDULE_ID = 100L;

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetScheduleByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetScheduleByIdUseCase(scheduleRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when schedule is found")
        void shouldReturnMappedDto_whenScheduleFound() {
            // Given
            ScheduleDataModel schedule = new ScheduleDataModel();
            GetScheduleResponseDTO expectedDto = new GetScheduleResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID)))
                    .thenReturn(Optional.of(schedule));
            when(modelMapper.map(schedule, GetScheduleResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetScheduleResponseDTO result = useCase.get(SCHEDULE_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(scheduleRepository).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            verify(modelMapper).map(schedule, GetScheduleResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, scheduleRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when schedule not found")
        void shouldThrowEntityNotFoundException_whenScheduleNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(SCHEDULE_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(exception -> {
                        EntityNotFoundException ex = (EntityNotFoundException) exception;
                        assertThat(ex.getEntityType()).isEqualTo(EntityType.SCHEDULE);
                        assertThat(ex.getEntityId()).isEqualTo(String.valueOf(SCHEDULE_ID));
                    });
            verify(tenantContextHolder).getTenantId();
            verify(scheduleRepository).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            verifyNoMoreInteractions(tenantContextHolder, scheduleRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Tenant context")
    class TenantContext {

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(SCHEDULE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetScheduleByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, scheduleRepository, modelMapper);
        }
    }
}
