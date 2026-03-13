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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("TenantCreationUseCase")
@ExtendWith(MockitoExtension.class)
class TenantCreationUseCaseTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ApplicationContext applicationContext;
    @Mock private ModelMapper modelMapper;

    private TenantCreationUseCase useCase;

    private static final String ORG_NAME = "Akademia Plus";
    private static final String LEGAL_NAME = "Akademia Plus S.A. de C.V.";
    private static final String WEBSITE_URL = "https://akademiaplus.com";
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
            TenantDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, tenantRepository);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            TenantCreateRequestDTO dto = buildDto();
            TenantDataModel prototypeModel = new TenantDataModel();
            when(applicationContext.getBean(TenantDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, tenantRepository);
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
            doNothing().when(modelMapper).map(dto, prototypeModel, TenantCreationUseCase.MAP_NAME);
            when(tenantRepository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, TenantDTO.class)).thenReturn(expectedDto);

            // When
            TenantDTO result = useCase.create(dto);

            // Then
            assertThat(result.getTenantId()).isEqualTo(1L);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantRepository);
            inOrder.verify(applicationContext, times(1)).getBean(TenantDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantCreationUseCase.MAP_NAME);
            inOrder.verify(tenantRepository, times(1)).save(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, TenantDTO.class);
            inOrder.verifyNoMoreInteractions();
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
            doNothing().when(modelMapper).map(dto, prototypeModel, TenantCreationUseCase.MAP_NAME);
            when(tenantRepository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, TenantDTO.class)).thenReturn(responseDto);

            // When
            TenantDTO result = useCase.create(dto);

            // Then — verify the exact object from transform() is what gets saved
            assertThat(result).isSameAs(responseDto);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantRepository);
            inOrder.verify(applicationContext, times(1)).getBean(TenantDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantCreationUseCase.MAP_NAME);
            inOrder.verify(tenantRepository, times(1)).save(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, TenantDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when applicationContext.getBean throws")
        void shouldPropagateException_whenGetBeanThrows() {
            // Given
            TenantCreateRequestDTO dto = buildDto();
            when(applicationContext.getBean(TenantDataModel.class))
                    .thenThrow(new RuntimeException("Bean creation failed"));

            // When / Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Bean creation failed");
            verifyNoInteractions(tenantRepository);
        }

        @Test
        @DisplayName("Should propagate exception when repository.save throws")
        void shouldPropagateException_whenSaveThrows() {
            // Given
            TenantCreateRequestDTO dto = buildDto();
            TenantDataModel prototypeModel = new TenantDataModel();
            when(applicationContext.getBean(TenantDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, TenantCreationUseCase.MAP_NAME);
            when(tenantRepository.save(prototypeModel))
                    .thenThrow(new RuntimeException("DB connection lost"));

            // When / Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");
            verifyNoMoreInteractions(tenantRepository);
        }

        @Test
        @DisplayName("Should propagate exception when modelMapper.map for response throws")
        void shouldPropagateException_whenResponseMappingThrows() {
            // Given
            TenantCreateRequestDTO dto = buildDto();
            TenantDataModel prototypeModel = new TenantDataModel();
            TenantDataModel savedModel = new TenantDataModel();
            when(applicationContext.getBean(TenantDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, TenantCreationUseCase.MAP_NAME);
            when(tenantRepository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, TenantDTO.class))
                    .thenThrow(new RuntimeException("Mapping configuration error"));

            // When / Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping configuration error");
        }
    }
}
