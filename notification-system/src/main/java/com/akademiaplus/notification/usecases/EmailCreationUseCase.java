/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.EmailRepository;
import com.akademiaplus.notifications.email.EmailAttachmentDataModel;
import com.akademiaplus.notifications.email.EmailDataModel;
import com.akademiaplus.notifications.email.EmailRecipientDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.EmailAttachmentDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailRecipientsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles creation of {@link EmailDataModel} entities by transforming
 * request data into the persistence data model.
 * <p>
 * Uses the two-method pattern: {@link #create} is transactional and persists,
 * while {@link #transform} builds the entity graph without side effects.
 */
@Service
@RequiredArgsConstructor
public class EmailCreationUseCase {

    /** Error message when subject is missing. */
    public static final String ERROR_SUBJECT_REQUIRED = "Email subject is required";

    /** Error message when body is missing. */
    public static final String ERROR_BODY_REQUIRED = "Email body is required";

    /** Error message when recipients are missing. */
    public static final String ERROR_RECIPIENTS_REQUIRED = "At least one recipient is required";

    private final ApplicationContext applicationContext;
    private final EmailRepository emailRepository;

    @Value("${akademia.email.from-address}")
    private String defaultFromAddress;

    /**
     * Creates and persists an email entity with recipients and attachments.
     *
     * @param subject     the email subject line
     * @param body        the email HTML body
     * @param sender      the sender email address (uses default if null)
     * @param recipients  the email recipients (to, cc, bcc)
     * @param attachments the email attachments (may be null or empty)
     * @return the persisted email entity
     */
    @Transactional
    public EmailDataModel create(String subject, String body, String sender,
                                 EmailRecipientsDTO recipients,
                                 List<EmailAttachmentDTO> attachments) {
        return emailRepository.saveAndFlush(transform(subject, body, sender, recipients, attachments));
    }

    /**
     * Transforms input data into an {@link EmailDataModel} entity graph.
     * <p>
     * Creates the email entity as a prototype bean, then builds recipient
     * and attachment child entities. Child entities share the email's tenant
     * context via cascade persistence.
     *
     * @param subject     the email subject line
     * @param body        the email HTML body
     * @param sender      the sender email address
     * @param recipients  the email recipients
     * @param attachments the email attachments
     * @return a populated email data model ready for persistence
     */
    public EmailDataModel transform(String subject, String body, String sender,
                                    EmailRecipientsDTO recipients,
                                    List<EmailAttachmentDTO> attachments) {
        final EmailDataModel email = applicationContext.getBean(EmailDataModel.class);
        email.setSubject(subject);
        email.setBody(body);
        email.setSender(sender != null ? sender : defaultFromAddress);

        email.setRecipients(buildRecipients(recipients, email));
        email.setAttachments(buildAttachments(attachments, email));

        return email;
    }

    private List<EmailRecipientDataModel> buildRecipients(EmailRecipientsDTO recipients,
                                                          EmailDataModel email) {
        List<EmailRecipientDataModel> recipientModels = new ArrayList<>();

        if (recipients.getTo() != null) {
            for (String toAddress : recipients.getTo()) {
                recipientModels.add(createRecipient(toAddress, email));
            }
        }
        if (recipients.getCc() != null) {
            for (String ccAddress : recipients.getCc()) {
                recipientModels.add(createRecipient(ccAddress, email));
            }
        }
        if (recipients.getBcc() != null) {
            for (String bccAddress : recipients.getBcc()) {
                recipientModels.add(createRecipient(bccAddress, email));
            }
        }

        return recipientModels;
    }

    private EmailRecipientDataModel createRecipient(String emailAddress, EmailDataModel email) {
        final EmailRecipientDataModel recipient = applicationContext.getBean(EmailRecipientDataModel.class);
        recipient.setRecipientEmail(emailAddress);
        recipient.setEmail(email);
        return recipient;
    }

    private List<EmailAttachmentDataModel> buildAttachments(List<EmailAttachmentDTO> attachments,
                                                            EmailDataModel email) {
        List<EmailAttachmentDataModel> attachmentModels = new ArrayList<>();

        if (attachments != null) {
            for (EmailAttachmentDTO attachment : attachments) {
                final EmailAttachmentDataModel model = applicationContext.getBean(EmailAttachmentDataModel.class);
                model.setAttachmentUrl(attachment.getUrl().toString());
                model.setEmail(email);
                attachmentModels.add(model);
            }
        }

        return attachmentModels;
    }
}
