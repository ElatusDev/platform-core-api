/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantBillingCycleRepository;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GetAllTenantBillingCyclesUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetAllTenantBillingCyclesUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllTenantBillingCyclesUseCaseTest {

    @Mock
    private TenantBillingCycleRepository tenantBillingCycleRepository;

    @Mock
    private ModelMapper modelMapper;

    private GetAllTenantBillingCyclesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllTenantBillingCyclesUseCase(tenantBillingCycleRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return list of mapped DTOs when billing cycles exist")
        void shouldReturnListOfDtos_whenBillingCyclesExist() {
            // Given
            TenantBillingCycleDataModel entity1 = new TenantBillingCycleDataModel();
            TenantBillingCycleDataModel entity2 = new TenantBillingCycleDataModel();
            BillingCycleDTO dto1 = new BillingCycleDTO();
            BillingCycleDTO dto2 = new BillingCycleDTO();
            when(tenantBillingCycleRepository.findAll()).thenReturn(List.of(entity1, entity2));
            when(modelMapper.map(entity1, BillingCycleDTO.class)).thenReturn(dto1);
            when(modelMapper.map(entity2, BillingCycleDTO.class)).thenReturn(dto2);

            // When
            List<BillingCycleDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(tenantBillingCycleRepository).findAll();
            verify(modelMapper).map(entity1, BillingCycleDTO.class);
            verify(modelMapper).map(entity2, BillingCycleDTO.class);
            verifyNoMoreInteractions(tenantBillingCycleRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return empty list when no billing cycles exist")
        void shouldReturnEmptyList_whenNoBillingCyclesExist() {
            // Given
            when(tenantBillingCycleRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<BillingCycleDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(tenantBillingCycleRepository).findAll();
            verifyNoMoreInteractions(tenantBillingCycleRepository, modelMapper);
        }
    }
}
