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
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteDemoRequestUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteDemoRequestUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteDemoRequestUseCaseTest {

    private static final Long DEMO_REQUEST_ID = 1L;

    @Mock
    private DemoRequestRepository demoRequestRepository;

    private DeleteDemoRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteDemoRequestUseCase(demoRequestRepository);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should delete the demo request when it exists")
        void shouldDeleteDemoRequest_whenItExists() {
            // Given
            DemoRequestDataModel model = new DemoRequestDataModel();
            model.setDemoRequestId(DEMO_REQUEST_ID);

            when(demoRequestRepository.findById(DEMO_REQUEST_ID))
                    .thenReturn(Optional.of(model));

            // When
            useCase.delete(DEMO_REQUEST_ID);

            // Then
            assertThat(model.getDemoRequestId()).isEqualTo(DEMO_REQUEST_ID);

            InOrder inOrder = inOrder(demoRequestRepository);
            inOrder.verify(demoRequestRepository, times(1)).findById(DEMO_REQUEST_ID);
            inOrder.verify(demoRequestRepository, times(1)).delete(model);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when demo request does not exist")
        void shouldThrowEntityNotFoundException_whenDemoRequestDoesNotExist() {
            // Given
            when(demoRequestRepository.findById(DEMO_REQUEST_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(DEMO_REQUEST_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.DEMO_REQUEST, DEMO_REQUEST_ID.toString()));

            verify(demoRequestRepository, times(1)).findById(DEMO_REQUEST_ID);
            verifyNoMoreInteractions(demoRequestRepository);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when findById throws")
        void shouldPropagateException_whenFindByIdThrows() {
            // Given
            RuntimeException dbException = new RuntimeException("DB connection failed");
            when(demoRequestRepository.findById(DEMO_REQUEST_ID)).thenThrow(dbException);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(DEMO_REQUEST_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection failed");

            verify(demoRequestRepository, times(1)).findById(DEMO_REQUEST_ID);
            verifyNoMoreInteractions(demoRequestRepository);
        }

        @Test
        @DisplayName("Should propagate exception when delete throws")
        void shouldPropagateException_whenDeleteThrows() {
            // Given
            DemoRequestDataModel model = new DemoRequestDataModel();
            model.setDemoRequestId(DEMO_REQUEST_ID);
            RuntimeException deleteException = new RuntimeException("Delete failed");

            when(demoRequestRepository.findById(DEMO_REQUEST_ID))
                    .thenReturn(Optional.of(model));
            org.mockito.Mockito.doThrow(deleteException)
                    .when(demoRequestRepository).delete(model);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(DEMO_REQUEST_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Delete failed");

            verify(demoRequestRepository, times(1)).findById(DEMO_REQUEST_ID);
            verify(demoRequestRepository, times(1)).delete(model);
        }
    }
}
