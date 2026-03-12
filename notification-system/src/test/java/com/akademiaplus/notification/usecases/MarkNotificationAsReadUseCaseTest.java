/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationReadStatusRepository;
import com.akademiaplus.notifications.NotificationReadStatusDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("MarkNotificationAsReadUseCase")
@ExtendWith(MockitoExtension.class)
class MarkNotificationAsReadUseCaseTest {

    private static final Long NOTIFICATION_ID = 100L;
    private static final Long USER_ID = 42L;

    @Mock private NotificationReadStatusRepository notificationReadStatusRepository;

    private MarkNotificationAsReadUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MarkNotificationAsReadUseCase(notificationReadStatusRepository);
    }

    @Nested
    @DisplayName("Mark as Read")
    class MarkAsRead {

        @Test
        @DisplayName("Should create read status when not already read")
        void shouldCreateReadStatus_whenNotAlreadyRead() {
            // Given
            when(notificationReadStatusRepository.existsByNotificationIdAndUserId(NOTIFICATION_ID, USER_ID))
                    .thenReturn(false);

            // When
            useCase.markAsRead(NOTIFICATION_ID, USER_ID);

            // Then
            ArgumentCaptor<NotificationReadStatusDataModel> captor =
                    ArgumentCaptor.forClass(NotificationReadStatusDataModel.class);
            InOrder inOrder = inOrder(notificationReadStatusRepository);
            inOrder.verify(notificationReadStatusRepository, times(1))
                    .existsByNotificationIdAndUserId(NOTIFICATION_ID, USER_ID);
            inOrder.verify(notificationReadStatusRepository, times(1)).save(captor.capture());
            inOrder.verifyNoMoreInteractions();
            NotificationReadStatusDataModel saved = captor.getValue();
            assertThat(saved.getNotificationId()).isEqualTo(NOTIFICATION_ID);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not create duplicate read status when already read")
        void shouldNotCreateDuplicate_whenAlreadyRead() {
            // Given
            when(notificationReadStatusRepository.existsByNotificationIdAndUserId(NOTIFICATION_ID, USER_ID))
                    .thenReturn(true);

            // When
            useCase.markAsRead(NOTIFICATION_ID, USER_ID);

            // Then
            verify(notificationReadStatusRepository, times(1)).existsByNotificationIdAndUserId(NOTIFICATION_ID, USER_ID);
            verifyNoMoreInteractions(notificationReadStatusRepository);
        }
    }
}
