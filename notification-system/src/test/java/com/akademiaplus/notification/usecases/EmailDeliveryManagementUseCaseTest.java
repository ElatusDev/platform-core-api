/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.notification.interfaceadapters.NotificationDeliveryRepository;
import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.notification.system.dto.CreateEmailDeliveryRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.DeliveryStatusDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryDetailResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailRecipientsDTO;
import openapi.akademiaplus.domain.notification.system.dto.UpdateEmailDeliveryStatusRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("EmailDeliveryManagementUseCase")
@ExtendWith(MockitoExtension.class)
class EmailDeliveryManagementUseCaseTest {

    private static final Long NOTIFICATION_ID = 10L;
    private static final Long DELIVERY_ID = 50L;
    private static final String RECIPIENT_EMAIL = "user@example.com";
    private static final String FAILURE_REASON = "Connection timeout";
    private static final String EXTERNAL_ID = "ext-abc-123";

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private EmailDeliveryChannelStrategy emailDeliveryChannelStrategy;

    @Mock
    private ModelMapper modelMapper;

    private EmailDeliveryManagementUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EmailDeliveryManagementUseCase(
                applicationContext,
                notificationDeliveryRepository,
                tenantContextHolder,
                emailDeliveryChannelStrategy,
                modelMapper
        );
    }

    private NotificationDataModel buildNotification() {
        NotificationDataModel notification = new NotificationDataModel();
        notification.setNotificationId(NOTIFICATION_ID);
        return notification;
    }

    private NotificationDeliveryDataModel buildDelivery(DeliveryStatus status) {
        NotificationDeliveryDataModel delivery = new NotificationDeliveryDataModel();
        delivery.setNotificationDeliveryId(DELIVERY_ID);
        delivery.setNotificationId(NOTIFICATION_ID);
        delivery.setChannel(DeliveryChannel.EMAIL);
        delivery.setRecipientIdentifier(RECIPIENT_EMAIL);
        delivery.setStatus(status);
        delivery.setRetryCount(EmailDeliveryManagementUseCase.INITIAL_RETRY_COUNT);
        return delivery;
    }

    @Nested
    @DisplayName("CreateDelivery")
    class CreateDelivery {

        @Test
        @DisplayName("Should create delivery record when recipients are provided")
        void shouldCreateDeliveryRecord_whenRecipientsProvided() {
            // Given
            NotificationDataModel notification = buildNotification();
            EmailRecipientsDTO recipients = new EmailRecipientsDTO();
            recipients.setTo(List.of(RECIPIENT_EMAIL));
            CreateEmailDeliveryRequestDTO dto = new CreateEmailDeliveryRequestDTO();
            dto.setRecipients(recipients);

            NotificationDeliveryDataModel deliveryModel = new NotificationDeliveryDataModel();
            when(applicationContext.getBean(NotificationDeliveryDataModel.class)).thenReturn(deliveryModel);

            NotificationDeliveryDataModel savedDelivery = buildDelivery(DeliveryStatus.PENDING);
            when(notificationDeliveryRepository.save(deliveryModel)).thenReturn(savedDelivery);

            EmailDeliveryResponseDTO responseDTO = new EmailDeliveryResponseDTO();
            when(modelMapper.map(savedDelivery, EmailDeliveryResponseDTO.class)).thenReturn(responseDTO);

            // When
            List<EmailDeliveryResponseDTO> result = useCase.createDelivery(notification, dto);

            // Then
            assertThat(result).hasSize(1);
            verify(applicationContext, times(1)).getBean(NotificationDeliveryDataModel.class);
            verify(notificationDeliveryRepository, times(1)).save(deliveryModel);
            verify(modelMapper, times(1)).map(savedDelivery, EmailDeliveryResponseDTO.class);
            assertThat(deliveryModel.getNotificationId()).isEqualTo(NOTIFICATION_ID);
            assertThat(deliveryModel.getChannel()).isEqualTo(DeliveryChannel.EMAIL);
            assertThat(deliveryModel.getRecipientIdentifier()).isEqualTo(RECIPIENT_EMAIL);
            assertThat(deliveryModel.getStatus()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(deliveryModel.getRetryCount()).isEqualTo(EmailDeliveryManagementUseCase.INITIAL_RETRY_COUNT);
            verifyNoMoreInteractions(applicationContext, notificationDeliveryRepository,
                    tenantContextHolder, emailDeliveryChannelStrategy, modelMapper);
        }
    }

    @Nested
    @DisplayName("GetDeliveries")
    class GetDeliveries {

        @Test
        @DisplayName("Should return deliveries when found for notification")
        void shouldReturnDeliveries_whenFoundForNotification() {
            // Given
            NotificationDeliveryDataModel delivery = buildDelivery(DeliveryStatus.SENT);
            when(notificationDeliveryRepository.findByNotificationIdAndChannel(NOTIFICATION_ID, DeliveryChannel.EMAIL))
                    .thenReturn(List.of(delivery));

            EmailDeliveryResponseDTO responseDTO = new EmailDeliveryResponseDTO();
            when(modelMapper.map(delivery, EmailDeliveryResponseDTO.class)).thenReturn(responseDTO);

            // When
            List<EmailDeliveryResponseDTO> result = useCase.getDeliveriesByNotificationId(NOTIFICATION_ID);

            // Then
            assertThat(result).hasSize(1);
            verify(notificationDeliveryRepository, times(1)).findByNotificationIdAndChannel(NOTIFICATION_ID, DeliveryChannel.EMAIL);
            verify(modelMapper, times(1)).map(delivery, EmailDeliveryResponseDTO.class);
            verifyNoMoreInteractions(applicationContext, notificationDeliveryRepository,
                    tenantContextHolder, emailDeliveryChannelStrategy, modelMapper);
        }
    }

    @Nested
    @DisplayName("GetById")
    class GetById {

        @Test
        @DisplayName("Should return delivery detail when found")
        void shouldReturnDeliveryDetail_whenFound() {
            // Given
            NotificationDeliveryDataModel delivery = buildDelivery(DeliveryStatus.DELIVERED);
            when(notificationDeliveryRepository.findByNotificationDeliveryId(DELIVERY_ID))
                    .thenReturn(Optional.of(delivery));

            EmailDeliveryDetailResponseDTO detailDTO = new EmailDeliveryDetailResponseDTO();
            when(modelMapper.map(delivery, EmailDeliveryDetailResponseDTO.class)).thenReturn(detailDTO);

            // When
            EmailDeliveryDetailResponseDTO result = useCase.getDeliveryById(DELIVERY_ID);

            // Then
            assertThat(result).isEqualTo(detailDTO);
            verify(notificationDeliveryRepository, times(1)).findByNotificationDeliveryId(DELIVERY_ID);
            verify(modelMapper, times(1)).map(delivery, EmailDeliveryDetailResponseDTO.class);
            verifyNoMoreInteractions(applicationContext, notificationDeliveryRepository,
                    tenantContextHolder, emailDeliveryChannelStrategy, modelMapper);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFound_whenNotFound() {
            // Given
            when(notificationDeliveryRepository.findByNotificationDeliveryId(DELIVERY_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.getDeliveryById(DELIVERY_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.NOTIFICATION_DELIVERY, String.valueOf(DELIVERY_ID)));
            verify(notificationDeliveryRepository, times(1)).findByNotificationDeliveryId(DELIVERY_ID);
            verifyNoInteractions(modelMapper);
            verifyNoMoreInteractions(applicationContext, notificationDeliveryRepository,
                    tenantContextHolder, emailDeliveryChannelStrategy);
        }
    }

    @Nested
    @DisplayName("UpdateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("Should update status when delivery is found")
        void shouldUpdateStatus_whenDeliveryFound() {
            // Given
            NotificationDeliveryDataModel delivery = buildDelivery(DeliveryStatus.PENDING);
            when(notificationDeliveryRepository.findByNotificationDeliveryId(DELIVERY_ID))
                    .thenReturn(Optional.of(delivery));

            UpdateEmailDeliveryStatusRequestDTO dto = new UpdateEmailDeliveryStatusRequestDTO();
            dto.setStatus(DeliveryStatusDTO.DELIVERED);
            dto.setFailureReason(FAILURE_REASON);
            dto.setExternalId(EXTERNAL_ID);

            NotificationDeliveryDataModel savedDelivery = buildDelivery(DeliveryStatus.DELIVERED);
            when(notificationDeliveryRepository.save(delivery)).thenReturn(savedDelivery);

            EmailDeliveryResponseDTO responseDTO = new EmailDeliveryResponseDTO();
            when(modelMapper.map(savedDelivery, EmailDeliveryResponseDTO.class)).thenReturn(responseDTO);

            // When
            EmailDeliveryResponseDTO result = useCase.updateDeliveryStatus(DELIVERY_ID, dto);

            // Then
            assertThat(result).isEqualTo(responseDTO);
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            assertThat(delivery.getFailureReason()).isEqualTo(FAILURE_REASON);
            assertThat(delivery.getExternalId()).isEqualTo(EXTERNAL_ID);
            verify(notificationDeliveryRepository, times(1)).findByNotificationDeliveryId(DELIVERY_ID);
            verify(notificationDeliveryRepository, times(1)).save(delivery);
            verify(modelMapper, times(1)).map(savedDelivery, EmailDeliveryResponseDTO.class);
            verifyNoMoreInteractions(applicationContext, notificationDeliveryRepository,
                    tenantContextHolder, emailDeliveryChannelStrategy, modelMapper);
        }
    }

    @Nested
    @DisplayName("RetryDelivery")
    class RetryDelivery {

        @Test
        @DisplayName("Should retry and update record when delivery has failed")
        void shouldRetryAndUpdateRecord_whenDeliveryFailed() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel delivery = buildDelivery(DeliveryStatus.FAILED);
            when(notificationDeliveryRepository.findByNotificationDeliveryId(DELIVERY_ID))
                    .thenReturn(Optional.of(delivery));

            DeliveryResult deliveryResult = DeliveryResult.sent();
            when(emailDeliveryChannelStrategy.deliver(notification, RECIPIENT_EMAIL))
                    .thenReturn(deliveryResult);

            NotificationDeliveryDataModel savedDelivery = buildDelivery(DeliveryStatus.SENT);
            when(notificationDeliveryRepository.save(delivery)).thenReturn(savedDelivery);

            EmailDeliveryResponseDTO responseDTO = new EmailDeliveryResponseDTO();
            when(modelMapper.map(savedDelivery, EmailDeliveryResponseDTO.class)).thenReturn(responseDTO);

            // When
            EmailDeliveryResponseDTO result = useCase.retryDelivery(DELIVERY_ID, notification);

            // Then
            assertThat(result).isEqualTo(responseDTO);
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SENT);
            assertThat(delivery.getRetryCount()).isEqualTo(1);
            verify(notificationDeliveryRepository, times(1)).findByNotificationDeliveryId(DELIVERY_ID);
            verify(emailDeliveryChannelStrategy, times(1)).deliver(notification, RECIPIENT_EMAIL);
            verify(notificationDeliveryRepository, times(1)).save(delivery);
            verify(modelMapper, times(1)).map(savedDelivery, EmailDeliveryResponseDTO.class);
            verifyNoMoreInteractions(applicationContext, notificationDeliveryRepository,
                    tenantContextHolder, emailDeliveryChannelStrategy, modelMapper);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when delivery is not retryable")
        void shouldThrowIllegalState_whenDeliveryNotRetryable() {
            // Given
            NotificationDataModel notification = buildNotification();
            NotificationDeliveryDataModel delivery = buildDelivery(DeliveryStatus.SENT);
            when(notificationDeliveryRepository.findByNotificationDeliveryId(DELIVERY_ID))
                    .thenReturn(Optional.of(delivery));

            // When / Then
            assertThatThrownBy(() -> useCase.retryDelivery(DELIVERY_ID, notification))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format(
                            EmailDeliveryManagementUseCase.ERROR_NOT_RETRYABLE,
                            DELIVERY_ID, DeliveryStatus.SENT));
            verify(notificationDeliveryRepository, times(1)).findByNotificationDeliveryId(DELIVERY_ID);
            verifyNoInteractions(emailDeliveryChannelStrategy, modelMapper);
            verifyNoMoreInteractions(applicationContext, notificationDeliveryRepository, tenantContextHolder);
        }
    }
}
