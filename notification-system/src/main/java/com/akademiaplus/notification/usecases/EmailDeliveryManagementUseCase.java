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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import openapi.akademiaplus.domain.notification.system.dto.CreateEmailDeliveryRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryDetailResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryListResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.UpdateEmailDeliveryStatusRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages email delivery lifecycle including creation, retrieval,
 * status updates, and retry operations for notification deliveries
 * through the EMAIL channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDeliveryManagementUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    /** Error message when delivery is not in a retryable state. */
    public static final String ERROR_NOT_RETRYABLE = "Delivery %s is not in a retryable state (current: %s)";

    /** Error message when recipients are empty. */
    public static final String ERROR_RECIPIENTS_REQUIRED = "At least one recipient is required";

    /** Initial retry count for new delivery records. */
    public static final int INITIAL_RETRY_COUNT = 0;

    private final ApplicationContext applicationContext;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final TenantContextHolder tenantContextHolder;
    private final EmailDeliveryChannelStrategy emailDeliveryChannelStrategy;
    private final ModelMapper modelMapper;

    /**
     * Creates email delivery records for each recipient in the request.
     *
     * @param notification the parent notification
     * @param dto          the delivery creation request with recipients and config
     * @return list of created delivery response DTOs
     */
    @Transactional
    public List<EmailDeliveryResponseDTO> createDelivery(NotificationDataModel notification,
                                                         CreateEmailDeliveryRequestDTO dto) {
        List<EmailDeliveryResponseDTO> responses = new ArrayList<>();

        if (dto.getRecipients().getTo() != null) {
            for (String recipient : dto.getRecipients().getTo()) {
                responses.add(createSingleDelivery(notification, recipient));
            }
        }
        if (dto.getRecipients().getCc() != null) {
            for (String recipient : dto.getRecipients().getCc()) {
                responses.add(createSingleDelivery(notification, recipient));
            }
        }
        if (dto.getRecipients().getBcc() != null) {
            for (String recipient : dto.getRecipients().getBcc()) {
                responses.add(createSingleDelivery(notification, recipient));
            }
        }

        return responses;
    }

    /**
     * Retrieves all email delivery records for a notification.
     *
     * @param notificationId the notification identifier
     * @return list of delivery response DTOs
     */
    public List<EmailDeliveryResponseDTO> getDeliveriesByNotificationId(Long notificationId) {
        List<NotificationDeliveryDataModel> deliveries =
                notificationDeliveryRepository.findByNotificationIdAndChannel(notificationId, DeliveryChannel.EMAIL);
        return deliveries.stream()
                .map(d -> modelMapper.map(d, EmailDeliveryResponseDTO.class))
                .toList();
    }

    /**
     * Retrieves a single email delivery record by its ID.
     *
     * @param deliveryId the delivery identifier
     * @return the delivery detail response DTO
     * @throws EntityNotFoundException if no delivery is found
     */
    public EmailDeliveryDetailResponseDTO getDeliveryById(Long deliveryId) {
        NotificationDeliveryDataModel delivery = findDeliveryOrThrow(deliveryId);
        return modelMapper.map(delivery, EmailDeliveryDetailResponseDTO.class);
    }

    /**
     * Updates the status and tracking timestamps of a delivery record.
     *
     * @param deliveryId the delivery identifier
     * @param dto        the status update request
     * @return the updated delivery response DTO
     * @throws EntityNotFoundException if no delivery is found
     */
    @Transactional
    public EmailDeliveryResponseDTO updateDeliveryStatus(Long deliveryId,
                                                         UpdateEmailDeliveryStatusRequestDTO dto) {
        NotificationDeliveryDataModel delivery = findDeliveryOrThrow(deliveryId);

        if (dto.getStatus() != null) {
            delivery.setStatus(DeliveryStatus.valueOf(dto.getStatus().getValue()));
        }
        if (dto.getDeliveredAt() != null) {
            delivery.setDeliveredAt(dto.getDeliveredAt().toLocalDateTime());
        }
        if (dto.getFailureReason() != null) {
            delivery.setFailureReason(dto.getFailureReason());
        }
        if (dto.getExternalId() != null) {
            delivery.setExternalId(dto.getExternalId());
        }

        NotificationDeliveryDataModel saved = notificationDeliveryRepository.save(delivery);
        return modelMapper.map(saved, EmailDeliveryResponseDTO.class);
    }

    /**
     * Retries a failed email delivery by re-dispatching through the email channel strategy.
     *
     * @param deliveryId the delivery identifier
     * @param notification the parent notification for content
     * @return the updated delivery response DTO
     * @throws EntityNotFoundException   if no delivery is found
     * @throws IllegalStateException     if the delivery is not in a retryable state
     */
    @Transactional
    public EmailDeliveryResponseDTO retryDelivery(Long deliveryId, NotificationDataModel notification) {
        NotificationDeliveryDataModel delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getStatus() != DeliveryStatus.FAILED && delivery.getStatus() != DeliveryStatus.BOUNCED) {
            throw new IllegalStateException(
                    String.format(ERROR_NOT_RETRYABLE, deliveryId, delivery.getStatus()));
        }

        DeliveryResult result = emailDeliveryChannelStrategy.deliver(notification, delivery.getRecipientIdentifier());

        delivery.setStatus(result.status());
        delivery.setRetryCount(delivery.getRetryCount() + 1);
        if (result.status() == DeliveryStatus.SENT) {
            delivery.setSentAt(LocalDateTime.now());
        }
        if (result.failureReason() != null) {
            delivery.setFailureReason(result.failureReason());
        }

        NotificationDeliveryDataModel saved = notificationDeliveryRepository.save(delivery);
        return modelMapper.map(saved, EmailDeliveryResponseDTO.class);
    }

    private EmailDeliveryResponseDTO createSingleDelivery(NotificationDataModel notification,
                                                          String recipientEmail) {
        final NotificationDeliveryDataModel delivery =
                applicationContext.getBean(NotificationDeliveryDataModel.class);

        delivery.setNotificationId(notification.getNotificationId());
        delivery.setChannel(DeliveryChannel.EMAIL);
        delivery.setRecipientIdentifier(recipientEmail);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setRetryCount(INITIAL_RETRY_COUNT);

        NotificationDeliveryDataModel saved = notificationDeliveryRepository.save(delivery);
        return modelMapper.map(saved, EmailDeliveryResponseDTO.class);
    }

    private NotificationDeliveryDataModel findDeliveryOrThrow(Long deliveryId) {
        return notificationDeliveryRepository.findByNotificationDeliveryId(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.NOTIFICATION_DELIVERY, String.valueOf(deliveryId)));
    }
}
