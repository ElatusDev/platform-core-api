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
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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
            verify(demoRequestRepository).delete(model);
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
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
