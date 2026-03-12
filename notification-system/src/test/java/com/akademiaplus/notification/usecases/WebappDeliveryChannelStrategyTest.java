/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("WebappDeliveryChannelStrategy")
@ExtendWith(MockitoExtension.class)
class WebappDeliveryChannelStrategyTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String RECIPIENT_IDENTIFIER = "100";
    private static final String TEST_TITLE = "Test Notification";
    private static final String IO_ERROR_MESSAGE = "Connection reset";

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    private WebappDeliveryChannelStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new WebappDeliveryChannelStrategy(sseEmitterRegistry);
    }

    private NotificationDataModel buildNotification() {
        NotificationDataModel notification = new NotificationDataModel();
        notification.setTenantId(TENANT_ID);
        notification.setTargetUserId(USER_ID);
        notification.setTitle(TEST_TITLE);
        return notification;
    }

    @Nested
    @DisplayName("Channel Identity")
    class ChannelIdentity {

        @Test
        @DisplayName("Should return WEBAPP as the delivery channel")
        void shouldReturnWebappChannel() {
            // Given — strategy instance

            // When
            DeliveryChannel channel = strategy.getChannel();

            // Then
            assertThat(channel).isEqualTo(DeliveryChannel.WEBAPP);
            verifyNoInteractions(sseEmitterRegistry);
        }
    }

    @Nested
    @DisplayName("Delivery")
    class Delivery {

        @Test
        @DisplayName("Should return SENT when emitter exists and send succeeds")
        void shouldReturnSent_whenEmitterExistsAndSendSucceeds() throws IOException {
            // Given
            NotificationDataModel notification = buildNotification();
            SseEmitter emitter = mock(SseEmitter.class);
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.of(emitter));

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
            assertThat(result.failureReason()).isNull();
            verify(sseEmitterRegistry, times(1)).getEmitter(TENANT_ID, USER_ID);
            verify(emitter, times(1)).send(org.mockito.ArgumentMatchers.isA(SseEmitter.SseEventBuilder.class));
            verifyNoMoreInteractions(sseEmitterRegistry, emitter);
        }

        @Test
        @DisplayName("Should call emitter send when emitter exists")
        void shouldCallEmitterSend_whenEmitterExists() throws IOException {
            // Given
            NotificationDataModel notification = buildNotification();
            SseEmitter emitter = mock(SseEmitter.class);
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.of(emitter));

            // When
            strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            verify(sseEmitterRegistry, times(1)).getEmitter(TENANT_ID, USER_ID);
            verify(emitter, times(1)).send(org.mockito.ArgumentMatchers.isA(SseEmitter.SseEventBuilder.class));
            verifyNoMoreInteractions(sseEmitterRegistry, emitter);
        }

        @Test
        @DisplayName("Should return FAILED when no emitter is registered for user")
        void shouldReturnFailed_whenNoEmitterRegistered() {
            // Given
            NotificationDataModel notification = buildNotification();
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(result.failureReason()).isEqualTo(WebappDeliveryChannelStrategy.ERROR_USER_NOT_CONNECTED);
            verify(sseEmitterRegistry, times(1)).getEmitter(TENANT_ID, USER_ID);
            verifyNoMoreInteractions(sseEmitterRegistry);
        }

        @Test
        @DisplayName("Should return FAILED when SSE send throws IOException")
        void shouldReturnFailed_whenSendThrowsIOException() throws IOException {
            // Given
            NotificationDataModel notification = buildNotification();
            SseEmitter emitter = mock(SseEmitter.class);
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.of(emitter));
            doThrow(new IOException(IO_ERROR_MESSAGE))
                    .when(emitter).send(org.mockito.ArgumentMatchers.isA(SseEmitter.SseEventBuilder.class));

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(result.failureReason()).isEqualTo(IO_ERROR_MESSAGE);
            verify(sseEmitterRegistry, times(1)).getEmitter(TENANT_ID, USER_ID);
            verify(emitter, times(1)).send(org.mockito.ArgumentMatchers.isA(SseEmitter.SseEventBuilder.class));
            verify(sseEmitterRegistry, times(1)).remove(TENANT_ID, USER_ID);
            verifyNoMoreInteractions(sseEmitterRegistry, emitter);
        }

        @Test
        @DisplayName("Should remove emitter from registry when send fails")
        void shouldRemoveEmitter_whenSendFails() throws IOException {
            // Given
            NotificationDataModel notification = buildNotification();
            SseEmitter emitter = mock(SseEmitter.class);
            when(sseEmitterRegistry.getEmitter(TENANT_ID, USER_ID)).thenReturn(Optional.of(emitter));
            doThrow(new IOException(IO_ERROR_MESSAGE))
                    .when(emitter).send(org.mockito.ArgumentMatchers.isA(SseEmitter.SseEventBuilder.class));

            // When
            strategy.deliver(notification, RECIPIENT_IDENTIFIER);

            // Then
            verify(sseEmitterRegistry, times(1)).getEmitter(TENANT_ID, USER_ID);
            verify(emitter, times(1)).send(org.mockito.ArgumentMatchers.isA(SseEmitter.SseEventBuilder.class));
            verify(sseEmitterRegistry, times(1)).remove(TENANT_ID, USER_ID);
            verifyNoMoreInteractions(sseEmitterRegistry, emitter);
        }
    }
}
