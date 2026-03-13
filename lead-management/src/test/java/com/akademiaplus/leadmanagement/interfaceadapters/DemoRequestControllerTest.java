/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.interfaceadapters;

import com.akademiaplus.leadmanagement.usecases.DeleteDemoRequestUseCase;
import com.akademiaplus.leadmanagement.usecases.DemoRequestCreationUseCase;
import com.akademiaplus.leadmanagement.usecases.GetAllDemoRequestsUseCase;
import com.akademiaplus.leadmanagement.usecases.GetDemoRequestByIdUseCase;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationRequestDTO;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationResponseDTO;
import openapi.akademiaplus.domain.lead.management.dto.GetAllDemoRequests200ResponseDTO;
import openapi.akademiaplus.domain.lead.management.dto.GetDemoRequestResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DemoRequestController}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DemoRequestController")
@ExtendWith(MockitoExtension.class)
class DemoRequestControllerTest {

    private static final Long DEMO_REQUEST_ID = 1L;

    @Mock
    private DemoRequestCreationUseCase demoRequestCreationUseCase;

    @Mock
    private GetDemoRequestByIdUseCase getDemoRequestByIdUseCase;

    @Mock
    private GetAllDemoRequestsUseCase getAllDemoRequestsUseCase;

    @Mock
    private DeleteDemoRequestUseCase deleteDemoRequestUseCase;

    private DemoRequestController controller;

    @BeforeEach
    void setUp() {
        controller = new DemoRequestController(
                demoRequestCreationUseCase,
                getDemoRequestByIdUseCase,
                getAllDemoRequestsUseCase,
                deleteDemoRequestUseCase);
    }

    @Nested
    @DisplayName("Create Demo Request")
    class CreateDemoRequest {

        @Test
        @DisplayName("Should return 201 CREATED with response body")
        void shouldReturn201Created_withResponseBody() {
            // Given
            DemoRequestCreationRequestDTO request = new DemoRequestCreationRequestDTO();
            request.setEmail("test@example.com");

            DemoRequestCreationResponseDTO expectedResponse = new DemoRequestCreationResponseDTO();
            expectedResponse.setDemoRequestId(DEMO_REQUEST_ID);

            when(demoRequestCreationUseCase.create(request)).thenReturn(expectedResponse);

            // When
            ResponseEntity<DemoRequestCreationResponseDTO> response =
                    controller.createDemoRequest(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDemoRequestId()).isEqualTo(DEMO_REQUEST_ID);

            verify(demoRequestCreationUseCase, times(1)).create(request);
            verifyNoMoreInteractions(demoRequestCreationUseCase);
            verifyNoInteractions(getDemoRequestByIdUseCase, getAllDemoRequestsUseCase,
                    deleteDemoRequestUseCase);
        }
    }

    @Nested
    @DisplayName("Get Demo Request By ID")
    class GetDemoRequestById {

        @Test
        @DisplayName("Should return 200 OK with demo request")
        void shouldReturn200Ok_withDemoRequest() {
            // Given
            GetDemoRequestResponseDTO expectedDto = new GetDemoRequestResponseDTO();
            expectedDto.setDemoRequestId(DEMO_REQUEST_ID);

            when(getDemoRequestByIdUseCase.get(DEMO_REQUEST_ID)).thenReturn(expectedDto);

            // When
            ResponseEntity<GetDemoRequestResponseDTO> response =
                    controller.getDemoRequestById(DEMO_REQUEST_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDemoRequestId()).isEqualTo(DEMO_REQUEST_ID);

            verify(getDemoRequestByIdUseCase, times(1)).get(DEMO_REQUEST_ID);
            verifyNoMoreInteractions(getDemoRequestByIdUseCase);
            verifyNoInteractions(demoRequestCreationUseCase, getAllDemoRequestsUseCase,
                    deleteDemoRequestUseCase);
        }
    }

    @Nested
    @DisplayName("Get All Demo Requests")
    class GetAllDemoRequests {

        @Test
        @DisplayName("Should return 200 OK with wrapped demo requests list")
        void shouldReturn200Ok_withWrappedDemoRequestsList() {
            // Given
            GetDemoRequestResponseDTO dto = new GetDemoRequestResponseDTO();
            dto.setDemoRequestId(DEMO_REQUEST_ID);

            when(getAllDemoRequestsUseCase.getAll()).thenReturn(List.of(dto));

            // When
            ResponseEntity<GetAllDemoRequests200ResponseDTO> response =
                    controller.getAllDemoRequests();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDemoRequests()).hasSize(1);

            verify(getAllDemoRequestsUseCase, times(1)).getAll();
            verifyNoMoreInteractions(getAllDemoRequestsUseCase);
            verifyNoInteractions(demoRequestCreationUseCase, getDemoRequestByIdUseCase,
                    deleteDemoRequestUseCase);
        }
    }

    @Nested
    @DisplayName("Delete Demo Request")
    class DeleteDemoRequest {

        @Test
        @DisplayName("Should return 204 NO CONTENT and delegate to use case")
        void shouldReturn204NoContent_andDelegateToUseCase() {
            // When
            ResponseEntity<Void> response = controller.deleteDemoRequest(DEMO_REQUEST_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            verify(deleteDemoRequestUseCase, times(1)).delete(DEMO_REQUEST_ID);
            verifyNoMoreInteractions(deleteDemoRequestUseCase);
            verifyNoInteractions(demoRequestCreationUseCase, getDemoRequestByIdUseCase,
                    getAllDemoRequestsUseCase);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when create use case throws")
        void shouldPropagateException_whenCreateUseCaseThrows() {
            // Given
            DemoRequestCreationRequestDTO request = new DemoRequestCreationRequestDTO();
            request.setEmail("test@example.com");
            RuntimeException useCaseException = new RuntimeException("Creation failed");
            when(demoRequestCreationUseCase.create(request)).thenThrow(useCaseException);

            // When / Then
            assertThatThrownBy(() -> controller.createDemoRequest(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Creation failed");

            verify(demoRequestCreationUseCase, times(1)).create(request);
            verifyNoInteractions(getDemoRequestByIdUseCase, getAllDemoRequestsUseCase,
                    deleteDemoRequestUseCase);
        }

        @Test
        @DisplayName("Should propagate exception when get by id use case throws")
        void shouldPropagateException_whenGetByIdUseCaseThrows() {
            // Given
            RuntimeException useCaseException = new RuntimeException("Not found");
            when(getDemoRequestByIdUseCase.get(DEMO_REQUEST_ID)).thenThrow(useCaseException);

            // When / Then
            assertThatThrownBy(() -> controller.getDemoRequestById(DEMO_REQUEST_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not found");

            verify(getDemoRequestByIdUseCase, times(1)).get(DEMO_REQUEST_ID);
            verifyNoInteractions(demoRequestCreationUseCase, getAllDemoRequestsUseCase,
                    deleteDemoRequestUseCase);
        }

        @Test
        @DisplayName("Should propagate exception when get all use case throws")
        void shouldPropagateException_whenGetAllUseCaseThrows() {
            // Given
            RuntimeException useCaseException = new RuntimeException("DB error");
            when(getAllDemoRequestsUseCase.getAll()).thenThrow(useCaseException);

            // When / Then
            assertThatThrownBy(() -> controller.getAllDemoRequests())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");

            verify(getAllDemoRequestsUseCase, times(1)).getAll();
            verifyNoInteractions(demoRequestCreationUseCase, getDemoRequestByIdUseCase,
                    deleteDemoRequestUseCase);
        }

        @Test
        @DisplayName("Should propagate exception when delete use case throws")
        void shouldPropagateException_whenDeleteUseCaseThrows() {
            // Given
            RuntimeException useCaseException = new RuntimeException("Delete error");
            org.mockito.Mockito.doThrow(useCaseException)
                    .when(deleteDemoRequestUseCase).delete(DEMO_REQUEST_ID);

            // When / Then
            assertThatThrownBy(() -> controller.deleteDemoRequest(DEMO_REQUEST_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Delete error");

            verify(deleteDemoRequestUseCase, times(1)).delete(DEMO_REQUEST_ID);
            verifyNoInteractions(demoRequestCreationUseCase, getDemoRequestByIdUseCase,
                    getAllDemoRequestsUseCase);
        }
    }
}
