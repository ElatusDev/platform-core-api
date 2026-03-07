/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import openapi.akademiaplus.domain.notification.system.dto.GetNotificationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetAllNotificationsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllNotificationsUseCaseTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllNotificationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllNotificationsUseCase(notificationRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no notifications exist")
        void shouldReturnEmptyList_whenNoNotificationsExist() {
            // Given
            when(notificationRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetNotificationResponseDTO> result = useCase.getAll(null);

            // Then
            assertThat(result).isEmpty();
            verify(notificationRepository).findAll();
            verifyNoMoreInteractions(notificationRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when notifications exist")
        void shouldReturnMappedDtos_whenNotificationsExist() {
            // Given
            NotificationDataModel notification1 = new NotificationDataModel();
            NotificationDataModel notification2 = new NotificationDataModel();
            GetNotificationResponseDTO dto1 = new GetNotificationResponseDTO();
            GetNotificationResponseDTO dto2 = new GetNotificationResponseDTO();

            when(notificationRepository.findAll()).thenReturn(List.of(notification1, notification2));
            when(modelMapper.map(notification1, GetNotificationResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(notification2, GetNotificationResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetNotificationResponseDTO> result = useCase.getAll(null);

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(notificationRepository).findAll();
            verify(modelMapper).map(notification1, GetNotificationResponseDTO.class);
            verify(modelMapper).map(notification2, GetNotificationResponseDTO.class);
            verifyNoMoreInteractions(notificationRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Filtered Retrieval")
    class FilteredRetrieval {

        @Test
        @DisplayName("Should return filtered results when targetUserId is provided")
        void shouldReturnFilteredResults_whenTargetUserIdProvided() {
            // Given
            Long targetUserId = 42L;
            NotificationDataModel notification = new NotificationDataModel();
            GetNotificationResponseDTO dto = new GetNotificationResponseDTO();

            when(notificationRepository.findByTargetUserId(targetUserId)).thenReturn(List.of(notification));
            when(modelMapper.map(notification, GetNotificationResponseDTO.class)).thenReturn(dto);

            // When
            List<GetNotificationResponseDTO> result = useCase.getAll(targetUserId);

            // Then
            assertThat(result).containsExactly(dto);
            verify(notificationRepository).findByTargetUserId(targetUserId);
            verifyNoMoreInteractions(notificationRepository);
        }
    }
}
