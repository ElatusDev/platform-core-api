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
import openapi.akademiaplus.domain.tenant.management.dto.TenantBrandingUpdateRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("UpdateTenantBrandingUseCase")
@ExtendWith(MockitoExtension.class)
class UpdateTenantBrandingUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final String SCHOOL_NAME = "Test Academy";
    private static final String PRIMARY_COLOR = "#FF5733";
    private static final String SECONDARY_COLOR = "#C70039";

    @Mock private ApplicationContext applicationContext;
    @Mock private TenantBrandingRepository tenantBrandingRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private UpdateTenantBrandingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTenantBrandingUseCase(
                applicationContext, tenantBrandingRepository, tenantContextHolder, modelMapper);
    }

    private TenantBrandingUpdateRequestDTO buildDto() {
        TenantBrandingUpdateRequestDTO dto = new TenantBrandingUpdateRequestDTO();
        dto.setSchoolName(SCHOOL_NAME);
        dto.setPrimaryColor(PRIMARY_COLOR);
        dto.setSecondaryColor(SECONDARY_COLOR);
        return dto;
    }

    @Nested
    @DisplayName("Update Existing Branding")
    class UpdateExisting {

        @Test
        @DisplayName("Should update existing branding when found for tenant")
        void shouldUpdateExistingBranding_whenFoundForTenant() {
            // Given
            TenantBrandingUpdateRequestDTO dto = buildDto();
            TenantBrandingDataModel existing = new TenantBrandingDataModel();
            existing.setTenantId(TENANT_ID);
            TenantBrandingDataModel saved = new TenantBrandingDataModel();
            GetTenantBrandingResponseDTO expectedResponse = new GetTenantBrandingResponseDTO();
            expectedResponse.setSchoolName(SCHOOL_NAME);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tenantBrandingRepository.findById(TENANT_ID)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, UpdateTenantBrandingUseCase.MAP_NAME);
            when(tenantBrandingRepository.saveAndFlush(existing)).thenReturn(saved);
            when(modelMapper.map(saved, GetTenantBrandingResponseDTO.class)).thenReturn(expectedResponse);

            // When
            GetTenantBrandingResponseDTO result = useCase.upsert(dto);

            // Then
            assertThat(result.getSchoolName()).isEqualTo(SCHOOL_NAME);
            InOrder inOrder = inOrder(tenantContextHolder, tenantBrandingRepository, modelMapper);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tenantBrandingRepository, times(1)).findById(TENANT_ID);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, UpdateTenantBrandingUseCase.MAP_NAME);
            inOrder.verify(tenantBrandingRepository, times(1)).saveAndFlush(existing);
            inOrder.verify(modelMapper, times(1)).map(saved, GetTenantBrandingResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Create New Branding (Upsert)")
    class CreateNew {

        @Test
        @DisplayName("Should create new branding when none exists for tenant")
        void shouldCreateNewBranding_whenNoneExistsForTenant() {
            // Given
            TenantBrandingUpdateRequestDTO dto = buildDto();
            TenantBrandingDataModel newEntity = new TenantBrandingDataModel();
            TenantBrandingDataModel saved = new TenantBrandingDataModel();
            GetTenantBrandingResponseDTO expectedResponse = new GetTenantBrandingResponseDTO();
            expectedResponse.setSchoolName(SCHOOL_NAME);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tenantBrandingRepository.findById(TENANT_ID)).thenReturn(Optional.empty());
            when(applicationContext.getBean(TenantBrandingDataModel.class)).thenReturn(newEntity);
            doNothing().when(modelMapper).map(dto, newEntity, UpdateTenantBrandingUseCase.MAP_NAME);
            when(tenantBrandingRepository.saveAndFlush(newEntity)).thenReturn(saved);
            when(modelMapper.map(saved, GetTenantBrandingResponseDTO.class)).thenReturn(expectedResponse);

            // When
            GetTenantBrandingResponseDTO result = useCase.upsert(dto);

            // Then
            assertThat(result.getSchoolName()).isEqualTo(SCHOOL_NAME);
            assertThat(newEntity.getTenantId()).isEqualTo(TENANT_ID);
            verify(applicationContext, times(1)).getBean(TenantBrandingDataModel.class);
        }
    }
}
