/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantBrandingRepository;
import com.akademiaplus.tenancy.TenantBrandingDataModel;
import openapi.akademiaplus.domain.tenant.management.dto.GetTenantBrandingResponseDTO;
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
import static org.mockito.Mockito.*;

@DisplayName("GetTenantBrandingUseCase")
@ExtendWith(MockitoExtension.class)
class GetTenantBrandingUseCaseTest {

    private static final Long TENANT_ID = 1L;

    @Mock private TenantBrandingRepository tenantBrandingRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetTenantBrandingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTenantBrandingUseCase(tenantBrandingRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Branding Exists")
    class BrandingExists {

        @Test
        @DisplayName("Should return mapped DTO when branding is found for tenant")
        void shouldReturnMappedDto_whenBrandingFoundForTenant() {
            // Given
            TenantBrandingDataModel entity = new TenantBrandingDataModel();
            entity.setTenantId(TENANT_ID);
            entity.setSchoolName("Test Academy");
            entity.setPrimaryColor("#333333");
            entity.setSecondaryColor("#666666");

            GetTenantBrandingResponseDTO expectedDto = new GetTenantBrandingResponseDTO();
            expectedDto.setSchoolName("Test Academy");

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tenantBrandingRepository.findById(TENANT_ID)).thenReturn(Optional.of(entity));
            when(modelMapper.map(entity, GetTenantBrandingResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetTenantBrandingResponseDTO result = useCase.get();

            // Then
            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getSchoolName()).isEqualTo("Test Academy");
            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(tenantBrandingRepository, times(1)).findById(TENANT_ID);
            verify(modelMapper, times(1)).map(entity, GetTenantBrandingResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, tenantBrandingRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Branding Not Found — Defaults")
    class BrandingNotFound {

        @Test
        @DisplayName("Should return default branding when no branding exists for tenant")
        void shouldReturnDefaults_whenNoBrandingExistsForTenant() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tenantBrandingRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

            // When
            GetTenantBrandingResponseDTO result = useCase.get();

            // Then
            assertThat(result.getSchoolName()).isEqualTo("My School");
            assertThat(result.getPrimaryColor()).isEqualTo("#1976D2");
            assertThat(result.getSecondaryColor()).isEqualTo("#FF9800");
            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(tenantBrandingRepository, times(1)).findById(TENANT_ID);
            verifyNoInteractions(modelMapper);
        }
    }
}
