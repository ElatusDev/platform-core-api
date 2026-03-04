/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@DisplayName("ScheduledNotificationDispatcher")
@ExtendWith(MockitoExtension.class)
class ScheduledNotificationDispatcherTest {

    private static final Long TENANT_ID_1 = 1L;
    private static final Long TENANT_ID_2 = 2L;
    private static final Long NOTIFICATION_ID_1 = 10L;
    private static final Long NOTIFICATION_ID_2 = 20L;
    private static final Long TARGET_USER_ID = 100L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private ScheduledNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new ScheduledNotificationDispatcher(
                notificationRepository,
                notificationDispatchService,
                applicationContext
        );
    }

    private NotificationDataModel buildNotification(Long tenantId, Long notificationId) {
        NotificationDataModel notification = new NotificationDataModel();
        notification.setTenantId(tenantId);
        notification.setNotificationId(notificationId);
        notification.setTargetUserId(TARGET_USER_ID);
        return notification;
    }

    @Nested
    @DisplayName("Query")
    class Query {

        @Test
        @DisplayName("Should query pending notifications with current time")
        void shouldQueryPendingNotifications_whenDispatching() {
            // Given
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then
            verify(notificationRepository).findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Dispatch")
    class Dispatch {

        @Test
        @DisplayName("Should dispatch each pending notification")
        void shouldDispatchEachNotification_whenPendingExist() {
            // Given
            NotificationDataModel notification1 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_1);
            NotificationDataModel notification2 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_2);
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(List.of(notification1, notification2));
            when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then
            verify(notificationDispatchService).dispatch(notification1);
            verify(notificationDispatchService).dispatch(notification2);
        }

        @Test
        @DisplayName("Should not dispatch when no pending notifications")
        void shouldNotDispatch_whenNoPendingNotifications() {
            // Given
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then
            verifyNoInteractions(notificationDispatchService);
        }

        @Test
        @DisplayName("Should continue dispatching when one notification fails")
        void shouldContinueDispatching_whenOneNotificationFails() {
            // Given
            NotificationDataModel notification1 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_1);
            NotificationDataModel notification2 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_2);
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(List.of(notification1, notification2));
            when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);
            doThrow(new IllegalStateException("Dispatch failed")).when(notificationDispatchService).dispatch(notification1);

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then — second notification still dispatched despite first failure
            verify(notificationDispatchService).dispatch(notification2);
        }
    }

    @Nested
    @DisplayName("Tenant Context")
    class TenantContext {

        @Test
        @DisplayName("Should set tenant context for each tenant group")
        void shouldSetTenantContext_forEachTenantGroup() {
            // Given
            NotificationDataModel notification1 = buildNotification(TENANT_ID_1, NOTIFICATION_ID_1);
            NotificationDataModel notification2 = buildNotification(TENANT_ID_2, NOTIFICATION_ID_2);
            when(notificationRepository.findScheduledBefore(org.mockito.ArgumentMatchers.isA(LocalDateTime.class)))
                    .thenReturn(List.of(notification1, notification2));
            when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);

            // When
            dispatcher.dispatchScheduledNotifications();

            // Then
            verify(tenantContextHolder).setTenantId(TENANT_ID_1);
            verify(tenantContextHolder).setTenantId(TENANT_ID_2);
        }
    }
}
