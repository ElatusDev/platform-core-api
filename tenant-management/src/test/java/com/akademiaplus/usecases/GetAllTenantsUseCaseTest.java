/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDTO;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GetAllTenantsUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetAllTenantsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllTenantsUseCaseTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ModelMapper modelMapper;

    private GetAllTenantsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllTenantsUseCase(tenantRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return list of mapped DTOs when tenants exist")
        void shouldReturnListOfDtos_whenTenantsExist() {
            // Given
            TenantDataModel entity1 = new TenantDataModel();
            TenantDataModel entity2 = new TenantDataModel();
            TenantDTO dto1 = new TenantDTO();
            TenantDTO dto2 = new TenantDTO();
            when(tenantRepository.findAll()).thenReturn(List.of(entity1, entity2));
            when(modelMapper.map(entity1, TenantDTO.class)).thenReturn(dto1);
            when(modelMapper.map(entity2, TenantDTO.class)).thenReturn(dto2);

            // When
            List<TenantDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            InOrder inOrder = inOrder(tenantRepository, modelMapper);
            inOrder.verify(tenantRepository, times(1)).findAll();
            inOrder.verify(modelMapper, times(1)).map(entity1, TenantDTO.class);
            inOrder.verify(modelMapper, times(1)).map(entity2, TenantDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return empty list when no tenants exist")
        void shouldReturnEmptyList_whenNoTenantsExist() {
            // Given
            when(tenantRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<TenantDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(tenantRepository, times(1)).findAll();
            verifyNoMoreInteractions(tenantRepository, modelMapper);
        }
    }
}
