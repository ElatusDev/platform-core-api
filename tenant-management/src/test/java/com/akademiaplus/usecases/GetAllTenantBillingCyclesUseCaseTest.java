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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
            InOrder inOrder = inOrder(tenantBillingCycleRepository, modelMapper);
            inOrder.verify(tenantBillingCycleRepository, times(1)).findAll();
            inOrder.verify(modelMapper, times(1)).map(entity1, BillingCycleDTO.class);
            inOrder.verify(modelMapper, times(1)).map(entity2, BillingCycleDTO.class);
            inOrder.verifyNoMoreInteractions();
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
            verify(tenantBillingCycleRepository, times(1)).findAll();
            verifyNoMoreInteractions(tenantBillingCycleRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when repository.findAll throws")
        void shouldPropagateException_whenFindAllThrows() {
            // Given
            when(tenantBillingCycleRepository.findAll())
                    .thenThrow(new RuntimeException("DB connection lost"));

            // When / Then
            assertThatThrownBy(() -> useCase.getAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");
            verify(tenantBillingCycleRepository, times(1)).findAll();
            verifyNoInteractions(modelMapper);
        }

        @Test
        @DisplayName("Should propagate exception when modelMapper.map throws")
        void shouldPropagateException_whenModelMapperThrows() {
            // Given
            TenantBillingCycleDataModel entity = new TenantBillingCycleDataModel();
            when(tenantBillingCycleRepository.findAll()).thenReturn(List.of(entity));
            when(modelMapper.map(entity, BillingCycleDTO.class))
                    .thenThrow(new RuntimeException("Mapping configuration error"));

            // When / Then
            assertThatThrownBy(() -> useCase.getAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping configuration error");
            verify(modelMapper, times(1)).map(entity, BillingCycleDTO.class);
        }
    }
}
