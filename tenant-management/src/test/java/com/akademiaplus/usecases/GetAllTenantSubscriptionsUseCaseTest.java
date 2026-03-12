/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantSubscriptionRepository;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import openapi.akademiaplus.domain.tenant.management.dto.TenantSubscriptionDTO;
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
 * Unit tests for {@link GetAllTenantSubscriptionsUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetAllTenantSubscriptionsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllTenantSubscriptionsUseCaseTest {

    @Mock
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Mock
    private ModelMapper modelMapper;

    private GetAllTenantSubscriptionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllTenantSubscriptionsUseCase(tenantSubscriptionRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return list of mapped DTOs when subscriptions exist")
        void shouldReturnListOfDtos_whenSubscriptionsExist() {
            // Given
            TenantSubscriptionDataModel entity1 = new TenantSubscriptionDataModel();
            TenantSubscriptionDataModel entity2 = new TenantSubscriptionDataModel();
            TenantSubscriptionDTO dto1 = new TenantSubscriptionDTO();
            TenantSubscriptionDTO dto2 = new TenantSubscriptionDTO();
            when(tenantSubscriptionRepository.findAll()).thenReturn(List.of(entity1, entity2));
            when(modelMapper.map(entity1, TenantSubscriptionDTO.class)).thenReturn(dto1);
            when(modelMapper.map(entity2, TenantSubscriptionDTO.class)).thenReturn(dto2);

            // When
            List<TenantSubscriptionDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            InOrder inOrder = inOrder(tenantSubscriptionRepository, modelMapper);
            inOrder.verify(tenantSubscriptionRepository, times(1)).findAll();
            inOrder.verify(modelMapper, times(1)).map(entity1, TenantSubscriptionDTO.class);
            inOrder.verify(modelMapper, times(1)).map(entity2, TenantSubscriptionDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return empty list when no subscriptions exist")
        void shouldReturnEmptyList_whenNoSubscriptionsExist() {
            // Given
            when(tenantSubscriptionRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<TenantSubscriptionDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(tenantSubscriptionRepository, times(1)).findAll();
            verifyNoMoreInteractions(tenantSubscriptionRepository, modelMapper);
        }
    }
}
