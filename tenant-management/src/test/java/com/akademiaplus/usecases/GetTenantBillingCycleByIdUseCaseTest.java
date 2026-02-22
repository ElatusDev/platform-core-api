/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantBillingCycleRepository;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleDetailsDTO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GetTenantBillingCycleByIdUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetTenantBillingCycleByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetTenantBillingCycleByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long BILLING_CYCLE_ID = 10L;

    @Mock
    private TenantBillingCycleRepository tenantBillingCycleRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private ModelMapper modelMapper;

    private GetTenantBillingCycleByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTenantBillingCycleByIdUseCase(
                tenantBillingCycleRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when billing cycle found")
        void shouldReturnMappedDto_whenBillingCycleFound() {
            // Given
            TenantBillingCycleDataModel entity = new TenantBillingCycleDataModel();
            BillingCycleDetailsDTO expectedDto = new BillingCycleDetailsDTO();
            TenantBillingCycleDataModel.TenantBillingCycleCompositeId compositeId =
                    new TenantBillingCycleDataModel.TenantBillingCycleCompositeId(TENANT_ID, BILLING_CYCLE_ID);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(tenantBillingCycleRepository.findById(compositeId)).thenReturn(Optional.of(entity));
            when(modelMapper.map(entity, BillingCycleDetailsDTO.class)).thenReturn(expectedDto);

            // When
            BillingCycleDetailsDTO result = useCase.get(BILLING_CYCLE_ID);

            // Then
            assertThat(result).isSameAs(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(tenantBillingCycleRepository).findById(compositeId);
            verify(modelMapper).map(entity, BillingCycleDetailsDTO.class);
            verifyNoMoreInteractions(tenantBillingCycleRepository, tenantContextHolder, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when billing cycle not found")
        void shouldThrowEntityNotFound_whenBillingCycleNotFound() {
            // Given
            TenantBillingCycleDataModel.TenantBillingCycleCompositeId compositeId =
                    new TenantBillingCycleDataModel.TenantBillingCycleCompositeId(TENANT_ID, BILLING_CYCLE_ID);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(tenantBillingCycleRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.get(BILLING_CYCLE_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.TENANT_BILLING_CYCLE);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(BILLING_CYCLE_ID));
                    });
            verifyNoMoreInteractions(modelMapper);
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
            assertThatThrownBy(() -> useCase.get(BILLING_CYCLE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetTenantBillingCycleByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verifyNoMoreInteractions(tenantBillingCycleRepository, modelMapper);
        }
    }
}
