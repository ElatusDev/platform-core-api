/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationDeliveryRepository;
import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("NotificationDispatchService")
@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long NOTIFICATION_ID = 42L;
    private static final Long TARGET_USER_ID = 100L;
    private static final String RECIPIENT_IDENTIFIER = "100";
    private static final String FAILURE_REASON = "User not connected via SSE";

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private DeliveryChannelStrategy webappStrategy;

    private NotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        when(webappStrategy.getChannel()).thenReturn(DeliveryChannel.WEBAPP);
        dispatchService = new NotificationDispatchService(
                applicationContext,
                notificationDeliveryRepository,
                List.of(webappStrategy)
        );
        dispatchService.initStrategyMap();
    }

    private NotificationDataModel buildNotification() {
        NotificationDataModel notification = new NotificationDataModel();
        notification.setTenantId(TENANT_ID);
        notification.setNotificationId(NOTIFICATION_ID);
        notification.setTargetUserId(TARGET_USER_ID);
        return notification;
    }

    @Nested
    @DisplayName("Strategy Resolution")
    class StrategyResolution {

        @Test
        @DisplayName("Should resolve WEBAPP strategy when registered")
        void shouldResolveStrategy_whenChannelHasRegisteredStrategy() {
            // Given — strategy map initialized in setUp

            // When
            DeliveryChannelStrategy resolved = dispatchService.resolveStrategy(DeliveryChannel.WEBAPP);

            // Then
            assertThat(resolved).isSameAs(webappStrategy);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when no strategy for channel")
        void shouldThrowException_whenNoStrategyForChannel() {
            // Given — no EMAIL strategy registered

            // When / Then
            assertThatThrownBy(() -> dispatchService.resolveStrategy(DeliveryChannel.EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format(
                            NotificationDispatchService.ERROR_NO_STRATEGY,
                            DeliveryChannel.EMAIL));
        }
    }

    @Nested
    @DisplayName("Dispatch")
    class Dispatch {

        @Test
        @DisplayName("Should create delivery record via prototype bean when dispatching")
        void shouldCreateDeliveryRecord_whenDispatching() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER)).thenReturn(DeliveryResult.sent());
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(deliveryModel);

            // When
            dispatchService.dispatch(notification);

            // Then
            verify(applicationContext).getBean(NotificationDeliveryDataModel.class);
        }

        @Test
        @DisplayName("Should set status to SENT when strategy delivery succeeds")
        void shouldSetStatusToSent_whenStrategySucceeds() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER)).thenReturn(DeliveryResult.sent());
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(deliveryModel);

            // When
            dispatchService.dispatch(notification);

            // Then
            assertThat(deliveryModel.getStatus()).isEqualTo(DeliveryStatus.SENT);
            assertThat(deliveryModel.getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set status to FAILED with reason when strategy fails")
        void shouldSetStatusToFailed_whenStrategyFails() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER))
                    .thenReturn(DeliveryResult.failed(FAILURE_REASON));
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(deliveryModel);

            // When
            dispatchService.dispatch(notification);

            // Then
            assertThat(deliveryModel.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(deliveryModel.getFailureReason()).isEqualTo(FAILURE_REASON);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when targetUserId is null")
        void shouldThrowException_whenTargetUserIdIsNull() {
            // Given
            NotificationDataModel notification = buildNotification();
            notification.setTargetUserId(null);

            // When / Then
            assertThatThrownBy(() -> dispatchService.dispatch(notification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(NotificationDispatchService.ERROR_TARGET_USER_REQUIRED);
        }

        @Test
        @DisplayName("Should return saved delivery record")
        void shouldReturnSavedDelivery() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            NotificationDeliveryDataModel savedModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER)).thenReturn(DeliveryResult.sent());
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(savedModel);

            // When
            NotificationDeliveryDataModel result = dispatchService.dispatch(notification);

            // Then
            assertThat(result).isSameAs(savedModel);
        }

        @Test
        @DisplayName("Should set notification ID, channel, and recipient on delivery record")
        void shouldSetNotificationIdAndChannel_whenDispatching() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);
            when(webappStrategy.deliver(notification, RECIPIENT_IDENTIFIER)).thenReturn(DeliveryResult.sent());
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(deliveryModel);

            // When
            dispatchService.dispatch(notification);

            // Then
            assertThat(deliveryModel.getNotificationId()).isEqualTo(NOTIFICATION_ID);
            assertThat(deliveryModel.getChannel()).isEqualTo(DeliveryChannel.WEBAPP);
            assertThat(deliveryModel.getRecipientIdentifier()).isEqualTo(RECIPIENT_IDENTIFIER);
            assertThat(deliveryModel.getRetryCount()).isEqualTo(NotificationDispatchService.INITIAL_RETRY_COUNT);
        }
    }
}
