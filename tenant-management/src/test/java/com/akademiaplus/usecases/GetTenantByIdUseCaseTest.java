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
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDetailsDTO;
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
 * Unit tests for {@link GetTenantByIdUseCase}.
 *
 * <p>Tenant uses a simple {@code Long} primary key — no composite key
 * and no {@code TenantContextHolder} required.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetTenantByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetTenantByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ModelMapper modelMapper;

    private GetTenantByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTenantByIdUseCase(tenantRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when tenant found")
        void shouldReturnMappedDto_whenTenantFound() {
            // Given
            TenantDataModel entity = new TenantDataModel();
            TenantDetailsDTO expectedDto = new TenantDetailsDTO();
            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(entity));
            when(modelMapper.map(entity, TenantDetailsDTO.class)).thenReturn(expectedDto);

            // When
            TenantDetailsDTO result = useCase.get(TENANT_ID);

            // Then
            assertThat(result).isSameAs(expectedDto);
            InOrder inOrder = inOrder(tenantRepository, modelMapper);
            inOrder.verify(tenantRepository, times(1)).findById(TENANT_ID);
            inOrder.verify(modelMapper, times(1)).map(entity, TenantDetailsDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when tenant not found")
        void shouldThrowEntityNotFound_whenTenantNotFound() {
            // Given
            when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.get(TENANT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.TENANT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(TENANT_ID));
                    });
            verify(tenantRepository, times(1)).findById(TENANT_ID);
            verifyNoInteractions(modelMapper);
            verifyNoMoreInteractions(tenantRepository);
        }
    }
}
