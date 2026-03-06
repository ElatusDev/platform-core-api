/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.EmailRepository;
import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.NotificationDataModel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * Delivers notifications via email using Jakarta Mail (SMTP).
 * <p>
 * Uses the notification's title as the email subject and content as the HTML body.
 * The {@code recipientIdentifier} is expected to be the target email address.
 *
 * @see WebappDeliveryChannelStrategy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDeliveryChannelStrategy implements DeliveryChannelStrategy {

    /** Error message when email sending fails. */
    public static final String ERROR_SEND_FAILED = "Failed to send email to %s: %s";

    /** Error message when no email is found for the notification. */
    public static final String ERROR_EMAIL_NOT_FOUND = "Email not found for notification: %s";

    private final JavaMailSender javaMailSender;
    private final EmailRepository emailRepository;

    @Value("${akademia.email.from-address}")
    private String fromAddress;

    @Value("${akademia.email.from-name}")
    private String fromName;

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.EMAIL;
    }

    /**
     * Delivers a notification as an email to the specified recipient address.
     *
     * @param notification        the notification whose title and content become the email subject and body
     * @param recipientIdentifier the recipient's email address
     * @return {@link DeliveryResult#sent()} on success, or {@link DeliveryResult#failed(String)} on error
     */
    @Override
    public DeliveryResult deliver(NotificationDataModel notification, String recipientIdentifier) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(recipientIdentifier);
            helper.setSubject(notification.getTitle());
            helper.setText(notification.getContent(), true);

            javaMailSender.send(mimeMessage);
            return DeliveryResult.sent();
        } catch (MailException e) {
            log.warn("Email send failed for {}: {}", recipientIdentifier, e.getMessage());
            return DeliveryResult.failed(String.format(ERROR_SEND_FAILED, recipientIdentifier, e.getMessage()));
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("Email preparation failed for {}: {}", recipientIdentifier, e.getMessage());
            return DeliveryResult.failed(String.format(ERROR_SEND_FAILED, recipientIdentifier, e.getMessage()));
        }
    }
}
