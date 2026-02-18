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
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
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
import org.springframework.context.ApplicationContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("TenantCreationUseCase")
@ExtendWith(MockitoExtension.class)
class TenantCreationUseCaseTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ApplicationContext applicationContext;
    @Mock private ModelMapper modelMapper;

    private TenantCreationUseCase useCase;

    private static final String ORG_NAME = "Akademia Plus";
    private static final String LEGAL_NAME = "Akademia Plus S.A. de C.V.";
    private static final URI WEBSITE_URL = URI.create("https://akademiaplus.com");
    private static final String EMAIL = "admin@akademiaplus.com";
    private static final String ADDRESS = "Av. Vallarta 1234, Zapopan";
    private static final String PHONE = "+523312345678";
    private static final String LANDLINE = "+523398765432";
    private static final String DESCRIPTION = "Educational platform";
    private static final String TAX_ID = "APL210101ABC";

    @BeforeEach
    void setUp() {
        useCase = new TenantCreationUseCase(tenantRepository, applicationContext, modelMapper);
    }

    private TenantCreateRequestDTO buildDto() {
        TenantCreateRequestDTO dto = new TenantCreateRequestDTO();
        dto.setOrganizationName(ORG_NAME);
        dto.setLegalName(LEGAL_NAME);
        dto.setWebsiteUrl(WEBSITE_URL);
        dto.setEmail(EMAIL);
        dto.setAddress(ADDRESS);
        dto.setPhone(PHONE);
        dto.setLandline(LANDLINE);
        dto.setDescription(DESCRIPTION);
        dto.setTaxId(TAX_ID);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype TenantDataModel from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            TenantCreateRequestDTO dto = buildDto();
            TenantDataModel prototypeModel = new TenantDataModel();
            when(applicationContext.getBean(TenantDataModel.class)).thenReturn(prototypeModel);

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(TenantDataModel.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with DTO and prototype model")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            TenantCreateRequestDTO dto = buildDto();
            TenantDataModel prototypeModel = new TenantDataModel();
            when(applicationContext.getBean(TenantDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel);
            assertThat(result).isSameAs(prototypeModel);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            TenantCreateRequestDTO dto = buildDto();
            TenantDataModel prototypeModel = new TenantDataModel();
            TenantDataModel savedModel = new TenantDataModel();
            savedModel.setTenantId(1L);
            TenantDTO expectedDto = new TenantDTO();
            expectedDto.setTenantId(1L);

            when(applicationContext.getBean(TenantDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel);
            when(tenantRepository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, TenantDTO.class)).thenReturn(expectedDto);

            // When
            TenantDTO result = useCase.create(dto);

            // Then
            verify(tenantRepository).save(prototypeModel);
            verify(modelMapper).map(savedModel, TenantDTO.class);
            assertThat(result.getTenantId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should pass transform result directly to repository save")
        void shouldPassTransformResultToSave_whenCreating() {
            // Given
            TenantCreateRequestDTO dto = buildDto();
            TenantDataModel prototypeModel = new TenantDataModel();
            TenantDataModel savedModel = new TenantDataModel();
            TenantDTO responseDto = new TenantDTO();

            when(applicationContext.getBean(TenantDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel);
            when(tenantRepository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, TenantDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then — verify the exact object from transform() is what gets saved
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantRepository);
            inOrder.verify(applicationContext).getBean(TenantDataModel.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel);
            inOrder.verify(tenantRepository).save(prototypeModel);
            inOrder.verify(modelMapper).map(savedModel, TenantDTO.class);
        }
    }
}
