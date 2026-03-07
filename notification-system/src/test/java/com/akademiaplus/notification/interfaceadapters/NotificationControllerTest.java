/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.config.NotificationControllerAdvice;
import com.akademiaplus.notification.usecases.DeleteNotificationUseCase;
import com.akademiaplus.notification.usecases.GetAllNotificationsUseCase;
import com.akademiaplus.notification.usecases.GetNotificationByIdUseCase;
import com.akademiaplus.notification.usecases.MarkNotificationAsReadUseCase;
import com.akademiaplus.notification.usecases.NotificationCreationUseCase;
import com.akademiaplus.notification.usecases.NotificationDispatchService;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.notification.system.dto.GetNotificationResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationDispatchResponseDTO;
import org.modelmapper.ModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("NotificationController")
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final Long NOTIFICATION_ID = 100L;
    private static final String BASE_PATH = "/v1/notification-system/notifications";

    @Mock private NotificationCreationUseCase notificationCreationUseCase;
    @Mock private DeleteNotificationUseCase deleteNotificationUseCase;
    @Mock private GetAllNotificationsUseCase getAllNotificationsUseCase;
    @Mock private GetNotificationByIdUseCase getNotificationByIdUseCase;
    @Mock private MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    @Mock private NotificationDispatchService notificationDispatchService;
    @Mock private ModelMapper modelMapper;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationController controller = new NotificationController(
                notificationCreationUseCase, getAllNotificationsUseCase, getNotificationByIdUseCase,
                deleteNotificationUseCase, markNotificationAsReadUseCase, notificationDispatchService, modelMapper);
        NotificationControllerAdvice controllerAdvice = new NotificationControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
    }

    @Nested
    @DisplayName("GET /notifications")
    class GetAllNotifications {

        @Test
        @DisplayName("Should return 200 with empty list when no notifications exist")
        void shouldReturn200WithEmptyList_whenNoNotificationsExist() throws Exception {
            // Given
            when(getAllNotificationsUseCase.getAll(null)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(getAllNotificationsUseCase).getAll(null);
            verifyNoMoreInteractions(getAllNotificationsUseCase);
        }

        @Test
        @DisplayName("Should return 200 with notification list when notifications exist")
        void shouldReturn200WithNotificationList_whenNotificationsExist() throws Exception {
            // Given
            GetNotificationResponseDTO dto1 = new GetNotificationResponseDTO();
            GetNotificationResponseDTO dto2 = new GetNotificationResponseDTO();
            when(getAllNotificationsUseCase.getAll(null)).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(getAllNotificationsUseCase).getAll(null);
            verifyNoMoreInteractions(getAllNotificationsUseCase);
        }
    }

    @Nested
    @DisplayName("GET /notifications/{notificationId}")
    class GetNotificationById {

        @Test
        @DisplayName("Should return 200 with notification when found")
        void shouldReturn200WithNotification_whenFound() throws Exception {
            // Given
            GetNotificationResponseDTO dto = new GetNotificationResponseDTO();
            when(getNotificationByIdUseCase.get(NOTIFICATION_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{notificationId}", NOTIFICATION_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getNotificationByIdUseCase).get(NOTIFICATION_ID);
            verifyNoMoreInteractions(getNotificationByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when notification not found")
        void shouldReturn404_whenNotificationNotFound() throws Exception {
            // Given
            when(getNotificationByIdUseCase.get(NOTIFICATION_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.NOTIFICATION, String.valueOf(NOTIFICATION_ID)));
            when(messageService.getEntityNotFound(EntityType.NOTIFICATION, String.valueOf(NOTIFICATION_ID)))
                    .thenReturn("Notification not found: " + NOTIFICATION_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{notificationId}", NOTIFICATION_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(getNotificationByIdUseCase).get(NOTIFICATION_ID);
            verify(messageService).getEntityNotFound(EntityType.NOTIFICATION, String.valueOf(NOTIFICATION_ID));
            verifyNoMoreInteractions(getNotificationByIdUseCase, messageService);
        }
    }

    @Nested
    @DisplayName("PATCH /notifications/{notificationId}/read")
    class MarkNotificationAsRead {

        private static final Long USER_ID = 42L;
        private static final String READ_PATH = BASE_PATH + "/{notificationId}/read";

        @Test
        @DisplayName("Should return 200 when notification marked as read")
        void shouldReturn200_whenNotificationMarkedAsRead() throws Exception {
            // When & Then
            mockMvc.perform(patch(READ_PATH, NOTIFICATION_ID)
                            .param("userId", String.valueOf(USER_ID)))
                    .andExpect(status().isOk());

            verify(markNotificationAsReadUseCase).markAsRead(NOTIFICATION_ID, USER_ID);
            verifyNoMoreInteractions(markNotificationAsReadUseCase);
        }
    }

    @Nested
    @DisplayName("POST /notifications/{notificationId}/dispatch")
    class DispatchNotification {

        private static final Long DELIVERY_ID = 500L;
        private static final String DISPATCH_PATH = BASE_PATH + "/{notificationId}/dispatch";

        @Test
        @DisplayName("Should return 200 with dispatch response when notification dispatched")
        void shouldReturn200WithDispatchResponse_whenNotificationDispatched() throws Exception {
            // Given
            NotificationDataModel notification = new NotificationDataModel();
            NotificationDeliveryDataModel delivery = new NotificationDeliveryDataModel();
            NotificationDispatchResponseDTO responseDto = new NotificationDispatchResponseDTO();
            responseDto.setNotificationDeliveryId(DELIVERY_ID);

            when(getNotificationByIdUseCase.getEntity(NOTIFICATION_ID)).thenReturn(notification);
            when(notificationDispatchService.dispatch(notification)).thenReturn(delivery);
            when(modelMapper.map(delivery, NotificationDispatchResponseDTO.class)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(post(DISPATCH_PATH, NOTIFICATION_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.notificationDeliveryId").value(DELIVERY_ID));

            verify(getNotificationByIdUseCase).getEntity(NOTIFICATION_ID);
            verify(notificationDispatchService).dispatch(notification);
            verify(modelMapper).map(delivery, NotificationDispatchResponseDTO.class);
            verifyNoMoreInteractions(getNotificationByIdUseCase, notificationDispatchService, modelMapper);
        }

        @Test
        @DisplayName("Should return 404 when notification not found for dispatch")
        void shouldReturn404_whenNotificationNotFoundForDispatch() throws Exception {
            // Given
            when(getNotificationByIdUseCase.getEntity(NOTIFICATION_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.NOTIFICATION, String.valueOf(NOTIFICATION_ID)));
            when(messageService.getEntityNotFound(EntityType.NOTIFICATION, String.valueOf(NOTIFICATION_ID)))
                    .thenReturn("Notification not found: " + NOTIFICATION_ID);

            // When & Then
            mockMvc.perform(post(DISPATCH_PATH, NOTIFICATION_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(getNotificationByIdUseCase).getEntity(NOTIFICATION_ID);
            verify(messageService).getEntityNotFound(EntityType.NOTIFICATION, String.valueOf(NOTIFICATION_ID));
            verifyNoMoreInteractions(getNotificationByIdUseCase, notificationDispatchService, messageService);
        }
    }
}
