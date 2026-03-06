/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notification.usecases.EmailDeliveryManagementUseCase;
import com.akademiaplus.notification.usecases.EmailTemplateCreationUseCase;
import com.akademiaplus.notification.usecases.EmailTemplatePreviewUseCase;
import com.akademiaplus.notification.usecases.EmailTemplateUpdateUseCase;
import com.akademiaplus.notification.usecases.GetEmailTemplateByIdUseCase;
import com.akademiaplus.notification.usecases.GetNotificationByIdUseCase;
import com.akademiaplus.notification.usecases.ImmediateSendUseCase;
import com.akademiaplus.notification.usecases.ListEmailTemplatesUseCase;
import com.akademiaplus.notifications.NotificationDataModel;
import openapi.akademiaplus.domain.notification.system.api.EmailApi;
import openapi.akademiaplus.domain.notification.system.dto.CreateEmailDeliveryRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.CreateEmailTemplateRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.DeliveryStatusDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryDetailResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryListResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateListResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplatePreviewRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplatePreviewResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailDeliveryResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailNotificationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.UpdateEmailDeliveryStatusRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.UpdateEmailTemplateRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for all email notification operations including
 * delivery management, immediate send, and template CRUD.
 * <p>
 * Implements the generated {@link EmailApi} interface which aggregates
 * all email-related endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/notification-system")
public class EmailController implements EmailApi {

    private final EmailDeliveryManagementUseCase emailDeliveryManagementUseCase;
    private final ImmediateSendUseCase immediateSendUseCase;
    private final EmailTemplateCreationUseCase emailTemplateCreationUseCase;
    private final EmailTemplateUpdateUseCase emailTemplateUpdateUseCase;
    private final GetEmailTemplateByIdUseCase getEmailTemplateByIdUseCase;
    private final ListEmailTemplatesUseCase listEmailTemplatesUseCase;
    private final EmailTemplatePreviewUseCase emailTemplatePreviewUseCase;
    private final GetNotificationByIdUseCase getNotificationByIdUseCase;

    /**
     * Constructs the email controller with all required use case dependencies.
     *
     * @param emailDeliveryManagementUseCase the delivery management use case
     * @param immediateSendUseCase           the immediate send use case
     * @param emailTemplateCreationUseCase   the template creation use case
     * @param emailTemplateUpdateUseCase     the template update use case
     * @param getEmailTemplateByIdUseCase    the template retrieval use case
     * @param listEmailTemplatesUseCase      the template listing use case
     * @param emailTemplatePreviewUseCase    the template preview use case
     * @param getNotificationByIdUseCase     the notification retrieval use case
     */
    public EmailController(EmailDeliveryManagementUseCase emailDeliveryManagementUseCase,
                           ImmediateSendUseCase immediateSendUseCase,
                           EmailTemplateCreationUseCase emailTemplateCreationUseCase,
                           EmailTemplateUpdateUseCase emailTemplateUpdateUseCase,
                           GetEmailTemplateByIdUseCase getEmailTemplateByIdUseCase,
                           ListEmailTemplatesUseCase listEmailTemplatesUseCase,
                           EmailTemplatePreviewUseCase emailTemplatePreviewUseCase,
                           GetNotificationByIdUseCase getNotificationByIdUseCase) {
        this.emailDeliveryManagementUseCase = emailDeliveryManagementUseCase;
        this.immediateSendUseCase = immediateSendUseCase;
        this.emailTemplateCreationUseCase = emailTemplateCreationUseCase;
        this.emailTemplateUpdateUseCase = emailTemplateUpdateUseCase;
        this.getEmailTemplateByIdUseCase = getEmailTemplateByIdUseCase;
        this.listEmailTemplatesUseCase = listEmailTemplatesUseCase;
        this.emailTemplatePreviewUseCase = emailTemplatePreviewUseCase;
        this.getNotificationByIdUseCase = getNotificationByIdUseCase;
    }

    @Override
    public ResponseEntity<EmailDeliveryResponseDTO> createEmailDelivery(
            Integer notificationId, Integer xTenantID,
            CreateEmailDeliveryRequestDTO createEmailDeliveryRequestDTO) {
        NotificationDataModel notification =
                getNotificationByIdUseCase.getEntity(notificationId.longValue());
        List<EmailDeliveryResponseDTO> deliveries =
                emailDeliveryManagementUseCase.createDelivery(notification, createEmailDeliveryRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveries.isEmpty() ? null : deliveries.getFirst());
    }

    @Override
    public ResponseEntity<EmailDeliveryListResponseDTO> getEmailDeliveries(
            Integer notificationId, Integer xTenantID,
            DeliveryStatusDTO status, String recipientEmail,
            Integer page, Integer size) {
        List<EmailDeliveryResponseDTO> deliveries =
                emailDeliveryManagementUseCase.getDeliveriesByNotificationId(notificationId.longValue());

        EmailDeliveryListResponseDTO response = new EmailDeliveryListResponseDTO();
        response.setDeliveries(deliveries);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<EmailDeliveryDetailResponseDTO> getEmailDeliveryById(
            Integer deliveryId, Integer xTenantID) {
        return ResponseEntity.ok(
                emailDeliveryManagementUseCase.getDeliveryById(deliveryId.longValue()));
    }

    @Override
    public ResponseEntity<EmailDeliveryResponseDTO> updateEmailDeliveryStatus(
            Integer deliveryId, Integer xTenantID,
            UpdateEmailDeliveryStatusRequestDTO updateEmailDeliveryStatusRequestDTO) {
        return ResponseEntity.ok(
                emailDeliveryManagementUseCase.updateDeliveryStatus(
                        deliveryId.longValue(), updateEmailDeliveryStatusRequestDTO));
    }

    @Override
    public ResponseEntity<EmailDeliveryResponseDTO> retryEmailDelivery(
            Integer deliveryId, Integer xTenantID) {
        NotificationDeliveryLookup lookup = resolveNotificationForDelivery(deliveryId.longValue());
        return ResponseEntity.ok(
                emailDeliveryManagementUseCase.retryDelivery(
                        deliveryId.longValue(), lookup.notification));
    }

    @Override
    public ResponseEntity<ImmediateEmailDeliveryResponseDTO> sendImmediateEmailNotification(
            Integer xTenantID,
            ImmediateEmailNotificationRequestDTO immediateEmailNotificationRequestDTO) {
        return ResponseEntity.ok(immediateSendUseCase.send(immediateEmailNotificationRequestDTO));
    }

    @Override
    public ResponseEntity<EmailTemplateResponseDTO> createEmailTemplate(
            Integer xTenantID,
            CreateEmailTemplateRequestDTO createEmailTemplateRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emailTemplateCreationUseCase.create(createEmailTemplateRequestDTO));
    }

    @Override
    public ResponseEntity<EmailTemplateListResponseDTO> listEmailTemplates(
            Integer xTenantID, String category, Integer page, Integer size) {
        return ResponseEntity.ok(listEmailTemplatesUseCase.list(category));
    }

    @Override
    public ResponseEntity<EmailTemplateResponseDTO> getEmailTemplateById(
            String templateId, Integer xTenantID) {
        return ResponseEntity.ok(
                getEmailTemplateByIdUseCase.get(Long.parseLong(templateId)));
    }

    @Override
    public ResponseEntity<EmailTemplateResponseDTO> updateEmailTemplate(
            String templateId, Integer xTenantID,
            UpdateEmailTemplateRequestDTO updateEmailTemplateRequestDTO) {
        return ResponseEntity.ok(
                emailTemplateUpdateUseCase.update(
                        Long.parseLong(templateId), updateEmailTemplateRequestDTO));
    }

    @Override
    public ResponseEntity<EmailTemplatePreviewResponseDTO> previewEmailTemplate(
            String templateId, Integer xTenantID,
            EmailTemplatePreviewRequestDTO emailTemplatePreviewRequestDTO) {
        return ResponseEntity.ok(
                emailTemplatePreviewUseCase.preview(
                        Long.parseLong(templateId), emailTemplatePreviewRequestDTO));
    }

    /**
     * Resolves the parent notification for a delivery record (used in retry).
     * The delivery record contains the notificationId needed to look up the notification.
     */
    private NotificationDeliveryLookup resolveNotificationForDelivery(Long deliveryId) {
        EmailDeliveryDetailResponseDTO detail =
                emailDeliveryManagementUseCase.getDeliveryById(deliveryId);
        NotificationDataModel notification =
                getNotificationByIdUseCase.getEntity(detail.getNotificationId());
        return new NotificationDeliveryLookup(notification);
    }

    private record NotificationDeliveryLookup(NotificationDataModel notification) {
    }
}
