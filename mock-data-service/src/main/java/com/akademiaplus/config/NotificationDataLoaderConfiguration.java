/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.notification.interfaceadapters.EmailAttachmentRepository;
import com.akademiaplus.notification.interfaceadapters.EmailRecipientRepository;
import com.akademiaplus.notification.interfaceadapters.EmailRepository;
import com.akademiaplus.notification.interfaceadapters.NotificationDeliveryRepository;
import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notification.usecases.NotificationCreationUseCase;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationDeliveryDataModel;
import com.akademiaplus.notifications.email.EmailAttachmentDataModel;
import com.akademiaplus.notifications.email.EmailDataModel;
import com.akademiaplus.notifications.email.EmailRecipientDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.mock.notification.EmailAttachmentFactory.EmailAttachmentRequest;
import com.akademiaplus.util.mock.notification.EmailFactory.EmailRequest;
import com.akademiaplus.util.mock.notification.EmailRecipientFactory.EmailRecipientRequest;
import com.akademiaplus.util.mock.notification.NotificationDeliveryFactory.NotificationDeliveryRequest;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for notification-related mock data loader and cleanup beans.
 */
@Configuration
public class NotificationDataLoaderConfiguration {

    @Bean
    public DataLoader<NotificationCreationRequestDTO, NotificationDataModel, NotificationDataModel.NotificationCompositeId> notificationDataLoader(
            NotificationRepository repository,
            DataFactory<NotificationCreationRequestDTO> notificationFactory,
            NotificationCreationUseCase notificationCreationUseCase) {

        return new DataLoader<>(repository, notificationCreationUseCase::transform, notificationFactory);
    }

    @Bean
    public DataCleanUp<NotificationDataModel, NotificationDataModel.NotificationCompositeId> notificationDataCleanUp(
            EntityManager entityManager,
            NotificationRepository repository) {

        DataCleanUp<NotificationDataModel, NotificationDataModel.NotificationCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(NotificationDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── NotificationDelivery (no domain creation use case — direct mapping) ──

    @Bean
    public DataLoader<NotificationDeliveryRequest, NotificationDeliveryDataModel, NotificationDeliveryDataModel.NotificationDeliveryCompositeId>
            notificationDeliveryDataLoader(
                    NotificationDeliveryRepository repository,
                    DataFactory<NotificationDeliveryRequest> factory,
                    ApplicationContext applicationContext) {

        return new DataLoader<>(repository, dto -> {
            NotificationDeliveryDataModel model =
                    applicationContext.getBean(NotificationDeliveryDataModel.class);
            model.setNotificationId(dto.notificationId());
            model.setChannel(dto.channel());
            model.setRecipientIdentifier(dto.recipientIdentifier());
            model.setStatus(dto.status());
            model.setSentAt(dto.sentAt());
            model.setRetryCount(dto.retryCount());
            model.setExternalId(dto.externalId());
            return model;
        }, factory);
    }

    @Bean
    public DataCleanUp<NotificationDeliveryDataModel, NotificationDeliveryDataModel.NotificationDeliveryCompositeId> notificationDeliveryDataCleanUp(
            EntityManager entityManager,
            NotificationDeliveryRepository repository) {

        DataCleanUp<NotificationDeliveryDataModel, NotificationDeliveryDataModel.NotificationDeliveryCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(NotificationDeliveryDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Email (no domain creation use case — direct mapping) ──

    @Bean
    public DataLoader<EmailRequest, EmailDataModel, EmailDataModel.EmailCompositeId> emailDataLoader(
            EmailRepository repository,
            DataFactory<EmailRequest> factory,
            ApplicationContext applicationContext) {

        return new DataLoader<>(repository, dto -> {
            EmailDataModel model = applicationContext.getBean(EmailDataModel.class);
            model.setSubject(dto.subject());
            model.setBody(dto.body());
            model.setSender(dto.sender());
            return model;
        }, factory);
    }

    @Bean
    public DataCleanUp<EmailDataModel, EmailDataModel.EmailCompositeId> emailDataCleanUp(
            EntityManager entityManager,
            EmailRepository repository) {

        DataCleanUp<EmailDataModel, EmailDataModel.EmailCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(EmailDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── EmailRecipient (no domain creation use case — direct mapping) ──

    @Bean
    public DataLoader<EmailRecipientRequest, EmailRecipientDataModel, EmailRecipientDataModel.EmailRecipientCompositeId>
            emailRecipientDataLoader(
                    EmailRecipientRepository repository,
                    DataFactory<EmailRecipientRequest> factory,
                    ApplicationContext applicationContext) {

        return new DataLoader<>(repository, dto -> {
            EmailRecipientDataModel model =
                    applicationContext.getBean(EmailRecipientDataModel.class);
            model.setEmailId(dto.emailId());
            model.setRecipientEmail(dto.recipientEmail());
            return model;
        }, factory);
    }

    @Bean
    public DataCleanUp<EmailRecipientDataModel, EmailRecipientDataModel.EmailRecipientCompositeId> emailRecipientDataCleanUp(
            EntityManager entityManager,
            EmailRecipientRepository repository) {

        DataCleanUp<EmailRecipientDataModel, EmailRecipientDataModel.EmailRecipientCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(EmailRecipientDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── EmailAttachment (no domain creation use case — direct mapping) ──

    @Bean
    public DataLoader<EmailAttachmentRequest, EmailAttachmentDataModel, EmailAttachmentDataModel.EmailAttachmentCompositeId>
            emailAttachmentDataLoader(
                    EmailAttachmentRepository repository,
                    DataFactory<EmailAttachmentRequest> factory,
                    ApplicationContext applicationContext) {

        return new DataLoader<>(repository, dto -> {
            EmailAttachmentDataModel model =
                    applicationContext.getBean(EmailAttachmentDataModel.class);
            model.setEmailId(dto.emailId());
            model.setAttachmentUrl(dto.attachmentUrl());
            return model;
        }, factory);
    }

    @Bean
    public DataCleanUp<EmailAttachmentDataModel, EmailAttachmentDataModel.EmailAttachmentCompositeId> emailAttachmentDataCleanUp(
            EntityManager entityManager,
            EmailAttachmentRepository repository) {

        DataCleanUp<EmailAttachmentDataModel, EmailAttachmentDataModel.EmailAttachmentCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(EmailAttachmentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
