/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.NotificationDataModel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailDeliveryResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailNotificationRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates immediate (fire-and-forget) email delivery without
 * creating a notification or delivery records in the database.
 * <p>
 * Builds and sends a {@link MimeMessage} to each recipient,
 * collecting per-recipient results into an aggregated response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImmediateSendUseCase {

    /** Success message for the response. */
    public static final String MESSAGE_SENT = "Email delivery completed";

    /** Error message template for send failures. */
    public static final String ERROR_SEND_FAILED = "Failed to send email to %s: %s";

    private final JavaMailSender javaMailSender;
    private final ApplicationContext applicationContext;

    @Value("${akademia.email.from-address}")
    private String defaultFromAddress;

    @Value("${akademia.email.from-name}")
    private String defaultFromName;

    @Value("${akademia.email.ses-configuration-set:}")
    private String sesConfigurationSet;

    /**
     * Sends an email immediately to all specified recipients.
     * <p>
     * Each recipient receives an independent delivery attempt. Failures for
     * individual recipients do not prevent delivery to other recipients.
     *
     * @param dto the immediate email request containing subject, body, and recipients
     * @return aggregated delivery response with per-recipient results
     */
    public ImmediateEmailDeliveryResponseDTO send(ImmediateEmailNotificationRequestDTO dto) {
        String fromEmail = defaultFromAddress;
        String fromName = defaultFromName;

        if (dto.getConfig() != null) {
            if (dto.getConfig().getFromEmail() != null) {
                fromEmail = dto.getConfig().getFromEmail();
            }
            if (dto.getConfig().getFromName() != null) {
                fromName = dto.getConfig().getFromName();
            }
        }

        List<ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO> results = new ArrayList<>();

        List<String> allRecipients = collectRecipients(dto);

        for (String recipientEmail : allRecipients) {
            results.add(sendToRecipient(dto.getSubject(), dto.getBody(), fromEmail, fromName, recipientEmail));
        }

        ImmediateEmailDeliveryResponseDTO response = new ImmediateEmailDeliveryResponseDTO();
        response.setMessage(MESSAGE_SENT);
        response.setDeliveryResults(results);
        return response;
    }

    private List<String> collectRecipients(ImmediateEmailNotificationRequestDTO dto) {
        List<String> allRecipients = new ArrayList<>();
        if (dto.getRecipients().getTo() != null) {
            allRecipients.addAll(dto.getRecipients().getTo());
        }
        if (dto.getRecipients().getCc() != null) {
            allRecipients.addAll(dto.getRecipients().getCc());
        }
        if (dto.getRecipients().getBcc() != null) {
            allRecipients.addAll(dto.getRecipients().getBcc());
        }
        return allRecipients;
    }

    private ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO sendToRecipient(
            String subject, String body, String fromEmail, String fromName, String recipientEmail) {
        ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO result =
                new ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO();
        result.setRecipientEmail(recipientEmail);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            if (sesConfigurationSet != null && !sesConfigurationSet.isBlank()) {
                mimeMessage.setHeader(EmailDeliveryChannelStrategy.HEADER_SES_CONFIGURATION_SET, sesConfigurationSet);
            }

            javaMailSender.send(mimeMessage);

            result.setStatus(ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO.StatusEnum.SUCCESS);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Immediate email send failed for {}: {}", recipientEmail, e.getMessage());
            result.setStatus(ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO.StatusEnum.FAILED);
            result.setErrorMessage(String.format(ERROR_SEND_FAILED, recipientEmail, e.getMessage()));
        }

        return result;
    }
}
