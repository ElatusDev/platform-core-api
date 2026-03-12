/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.usecases;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestRepository;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationRequestDTO;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DemoRequestCreationUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DemoRequestCreationUseCase")
@ExtendWith(MockitoExtension.class)
class DemoRequestCreationUseCaseTest {

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@example.com";
    private static final String COMPANY_NAME = "Acme Corp";
    private static final String MESSAGE = "Interested in a demo";
    private static final Long SAVED_ID = 1L;

    @Mock
    private DemoRequestRepository demoRequestRepository;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ModelMapper modelMapper;

    private DemoRequestCreationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DemoRequestCreationUseCase(
                demoRequestRepository, applicationContext, modelMapper);
    }

    private DemoRequestCreationRequestDTO buildRequest() {
        DemoRequestCreationRequestDTO dto = new DemoRequestCreationRequestDTO();
        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setEmail(EMAIL);
        dto.setCompanyName(COMPANY_NAME);
        dto.setMessage(MESSAGE);
        return dto;
    }

    @Nested
    @DisplayName("Successful Creation")
    class SuccessfulCreation {

        @Test
        @DisplayName("Should return the saved demo request ID when creation succeeds")
        void shouldReturnSavedId_whenCreationSucceeds() {
            // Given
            DemoRequestCreationRequestDTO dto = buildRequest();
            DemoRequestDataModel model = new DemoRequestDataModel();
            DemoRequestDataModel savedModel = new DemoRequestDataModel();
            savedModel.setDemoRequestId(SAVED_ID);

            when(demoRequestRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(applicationContext.getBean(DemoRequestDataModel.class)).thenReturn(model);
            when(demoRequestRepository.save(model)).thenReturn(savedModel);

            // When
            DemoRequestCreationResponseDTO response = useCase.create(dto);

            // Then
            assertThat(response.getDemoRequestId()).isEqualTo(SAVED_ID);

            InOrder inOrder = inOrder(demoRequestRepository, applicationContext, modelMapper);
            inOrder.verify(demoRequestRepository, times(1)).existsByEmail(EMAIL);
            inOrder.verify(applicationContext, times(1)).getBean(DemoRequestDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, model,
                    DemoRequestCreationUseCase.MAP_NAME);
            inOrder.verify(demoRequestRepository, times(1)).save(model);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should set status to PENDING when creating a demo request")
        void shouldSetStatusToPending_whenCreating() {
            // Given
            DemoRequestCreationRequestDTO dto = buildRequest();
            DemoRequestDataModel model = new DemoRequestDataModel();
            DemoRequestDataModel savedModel = new DemoRequestDataModel();
            savedModel.setDemoRequestId(SAVED_ID);

            when(demoRequestRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(applicationContext.getBean(DemoRequestDataModel.class)).thenReturn(model);
            when(demoRequestRepository.save(model)).thenReturn(savedModel);

            // When
            useCase.create(dto);

            // Then
            ArgumentCaptor<DemoRequestDataModel> captor =
                    ArgumentCaptor.forClass(DemoRequestDataModel.class);
            verify(demoRequestRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getStatus())
                    .isEqualTo(DemoRequestCreationUseCase.STATUS_PENDING);

            InOrder inOrder = inOrder(demoRequestRepository, applicationContext, modelMapper);
            inOrder.verify(demoRequestRepository, times(1)).existsByEmail(EMAIL);
            inOrder.verify(applicationContext, times(1)).getBean(DemoRequestDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, model,
                    DemoRequestCreationUseCase.MAP_NAME);
            inOrder.verify(demoRequestRepository, times(1)).save(model);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should map DTO to model using named TypeMap")
        void shouldMapDtoToModel_usingNamedTypeMap() {
            // Given
            DemoRequestCreationRequestDTO dto = buildRequest();
            DemoRequestDataModel model = new DemoRequestDataModel();
            DemoRequestDataModel savedModel = new DemoRequestDataModel();
            savedModel.setDemoRequestId(SAVED_ID);

            when(demoRequestRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(applicationContext.getBean(DemoRequestDataModel.class)).thenReturn(model);
            when(demoRequestRepository.save(model)).thenReturn(savedModel);

            // When
            useCase.create(dto);

            // Then
            assertThat(savedModel.getDemoRequestId()).isEqualTo(SAVED_ID);

            InOrder inOrder = inOrder(demoRequestRepository, applicationContext, modelMapper);
            inOrder.verify(demoRequestRepository, times(1)).existsByEmail(EMAIL);
            inOrder.verify(applicationContext, times(1)).getBean(DemoRequestDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, model,
                    DemoRequestCreationUseCase.MAP_NAME);
            inOrder.verify(demoRequestRepository, times(1)).save(model);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Duplicate Email")
    class DuplicateEmail {

        @Test
        @DisplayName("Should throw DuplicateEntityException when email already exists")
        void shouldThrowDuplicateEntityException_whenEmailAlreadyExists() {
            // Given
            DemoRequestCreationRequestDTO dto = buildRequest();
            when(demoRequestRepository.existsByEmail(EMAIL)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(DuplicateEntityException.class);

            verify(demoRequestRepository, times(1)).existsByEmail(EMAIL);
            verifyNoMoreInteractions(demoRequestRepository);
            verifyNoInteractions(applicationContext, modelMapper);
        }
    }
}
