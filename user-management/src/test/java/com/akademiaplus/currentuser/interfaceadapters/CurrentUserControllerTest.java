/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.currentuser.interfaceadapters;

import com.akademiaplus.currentuser.usecases.GetCurrentUserUseCase;
import openapi.akademiaplus.domain.user.management.dto.GetCurrentUserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CurrentUserController}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("CurrentUserController")
@ExtendWith(MockitoExtension.class)
class CurrentUserControllerTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUserUseCase;

    private CurrentUserController controller;

    @BeforeEach
    void setUp() {
        controller = new CurrentUserController(getCurrentUserUseCase);
    }

    @Nested
    @DisplayName("Get Current User")
    class GetCurrentUser {

        @Test
        @DisplayName("Should return 200 OK with current user profile")
        void shouldReturn200Ok_withCurrentUserProfile() {
            // Given
            GetCurrentUserResponseDTO expectedResponse = new GetCurrentUserResponseDTO();
            expectedResponse.setUserType(GetCurrentUserResponseDTO.UserTypeEnum.EMPLOYEE);
            expectedResponse.setUsername("john.doe");

            when(getCurrentUserUseCase.getCurrentUser()).thenReturn(expectedResponse);

            // When
            ResponseEntity<GetCurrentUserResponseDTO> response = controller.getCurrentUser();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserType())
                    .isEqualTo(GetCurrentUserResponseDTO.UserTypeEnum.EMPLOYEE);
            assertThat(response.getBody().getUsername()).isEqualTo("john.doe");

            verify(getCurrentUserUseCase, times(1)).getCurrentUser();
            verifyNoMoreInteractions(getCurrentUserUseCase);
        }
    }
}
