/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantSubscriptionRepository;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GetTenantSubscriptionByIdUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetTenantSubscriptionByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetTenantSubscriptionByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 42L;

    @Mock
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private ModelMapper modelMapper;

    private GetTenantSubscriptionByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTenantSubscriptionByIdUseCase(
                tenantSubscriptionRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when subscription found")
        void shouldReturnMappedDto_whenSubscriptionFound() {
            // Given
            TenantSubscriptionDataModel entity = new TenantSubscriptionDataModel();
            TenantSubscriptionDTO expectedDto = new TenantSubscriptionDTO();
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId compositeId =
                    new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(TENANT_ID, SUBSCRIPTION_ID);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(tenantSubscriptionRepository.findById(compositeId)).thenReturn(Optional.of(entity));
            when(modelMapper.map(entity, TenantSubscriptionDTO.class)).thenReturn(expectedDto);

            // When
            TenantSubscriptionDTO result = useCase.get(SUBSCRIPTION_ID);

            // Then
            assertThat(result).isSameAs(expectedDto);
            InOrder inOrder = inOrder(tenantContextHolder, tenantSubscriptionRepository, modelMapper);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(tenantSubscriptionRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(entity, TenantSubscriptionDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when subscription not found")
        void shouldThrowEntityNotFound_whenSubscriptionNotFound() {
            // Given
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId compositeId =
                    new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(TENANT_ID, SUBSCRIPTION_ID);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(tenantSubscriptionRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.get(SUBSCRIPTION_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TENANT_SUBSCRIPTION, String.valueOf(SUBSCRIPTION_ID)));
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(tenantSubscriptionRepository, times(1)).findById(compositeId);
            verifyNoInteractions(modelMapper);
            verifyNoMoreInteractions(tenantContextHolder, tenantSubscriptionRepository);
        }
    }

    @Nested
    @DisplayName("Missing Tenant Context")
    class MissingTenantContext {

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context missing")
        void shouldThrowIllegalArgument_whenTenantContextMissing() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.get(SUBSCRIPTION_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetTenantSubscriptionByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(tenantSubscriptionRepository, modelMapper);
            verifyNoMoreInteractions(tenantContextHolder);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when tenantContextHolder.getTenantId throws")
        void shouldPropagateException_whenGetTenantIdThrows() {
            // Given
            when(tenantContextHolder.getTenantId())
                    .thenThrow(new RuntimeException("Thread-local corrupted"));

            // When / Then
            assertThatThrownBy(() -> useCase.get(SUBSCRIPTION_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Thread-local corrupted");
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(tenantSubscriptionRepository, modelMapper);
        }

        @Test
        @DisplayName("Should propagate exception when repository.findById throws")
        void shouldPropagateException_whenFindByIdThrows() {
            // Given
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId compositeId =
                    new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(TENANT_ID, SUBSCRIPTION_ID);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(tenantSubscriptionRepository.findById(compositeId))
                    .thenThrow(new RuntimeException("DB connection lost"));

            // When / Then
            assertThatThrownBy(() -> useCase.get(SUBSCRIPTION_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");
            verify(tenantSubscriptionRepository, times(1)).findById(compositeId);
            verifyNoInteractions(modelMapper);
        }

        @Test
        @DisplayName("Should propagate exception when modelMapper.map throws")
        void shouldPropagateException_whenModelMapperThrows() {
            // Given
            TenantSubscriptionDataModel entity = new TenantSubscriptionDataModel();
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId compositeId =
                    new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(TENANT_ID, SUBSCRIPTION_ID);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(tenantSubscriptionRepository.findById(compositeId)).thenReturn(Optional.of(entity));
            when(modelMapper.map(entity, TenantSubscriptionDTO.class))
                    .thenThrow(new RuntimeException("Mapping configuration error"));

            // When / Then
            assertThatThrownBy(() -> useCase.get(SUBSCRIPTION_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping configuration error");
            verify(modelMapper, times(1)).map(entity, TenantSubscriptionDTO.class);
        }
    }
}
