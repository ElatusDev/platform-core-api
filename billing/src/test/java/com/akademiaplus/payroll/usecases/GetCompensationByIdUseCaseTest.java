/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import java.util.Optional;
import openapi.akademiaplus.domain.billing.dto.GetCompensationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetCompensationByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetCompensationByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COMPENSATION_ID = 100L;

    @Mock private CompensationRepository compensationRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetCompensationByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCompensationByIdUseCase(compensationRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {
        @Test
        @DisplayName("Should return mapped DTO when entity found")
        void shouldReturnMappedDto_whenCompensationFound() {
            // Given
            CompensationDataModel compensation = new CompensationDataModel();
            GetCompensationResponseDTO expectedDto = new GetCompensationResponseDTO();
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(compensationRepository.findById(new CompensationDataModel.CompensationCompositeId(TENANT_ID, COMPENSATION_ID)))
                    .thenReturn(Optional.of(compensation));
            when(modelMapper.map(compensation, GetCompensationResponseDTO.class)).thenReturn(expectedDto);
            // When
            GetCompensationResponseDTO result = useCase.get(COMPENSATION_ID);
            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(compensationRepository, times(1)).findById(new CompensationDataModel.CompensationCompositeId(TENANT_ID, COMPENSATION_ID));
            verify(modelMapper, times(1)).map(compensation, GetCompensationResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, compensationRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {
        @Test
        @DisplayName("Should throw NotFoundException when entity not found")
        void shouldThrowEntityNotFoundException_whenCompensationNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(compensationRepository.findById(new CompensationDataModel.CompensationCompositeId(TENANT_ID, COMPENSATION_ID)))
                    .thenReturn(Optional.empty());
            // When & Then
            assertThatThrownBy(() -> useCase.get(COMPENSATION_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("entityType", EntityType.COMPENSATION)
                    .hasFieldOrPropertyWithValue("entityId", String.valueOf(COMPENSATION_ID));
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(compensationRepository, times(1)).findById(new CompensationDataModel.CompensationCompositeId(TENANT_ID, COMPENSATION_ID));
            verifyNoMoreInteractions(tenantContextHolder, compensationRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(COMPENSATION_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetCompensationByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, compensationRepository, modelMapper);
        }
    }
}
