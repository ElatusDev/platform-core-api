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
import com.akademiaplus.notifications.DeliveryStatus;
import com.akademiaplus.notifications.NotificationDataModel;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("EmailDeliveryChannelStrategy")
@ExtendWith(MockitoExtension.class)
class EmailDeliveryChannelStrategyTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 200L;
    private static final String RECIPIENT_EMAIL = "student@example.com";
    private static final String TEST_TITLE = "Course Enrollment Confirmation";
    private static final String TEST_CONTENT = "<p>Welcome to the course!</p>";
    private static final String FROM_ADDRESS = "noreply@akademiaplus.com";
    private static final String FROM_NAME = "AkademiaPlus";
    private static final String MAIL_SEND_ERROR = "Could not connect to SMTP server";
    private static final String MESSAGING_ERROR = "Invalid address format";

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private EmailRepository emailRepository;

    private EmailDeliveryChannelStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new EmailDeliveryChannelStrategy(javaMailSender, emailRepository);
        ReflectionTestUtils.setField(strategy, "fromAddress", FROM_ADDRESS);
        ReflectionTestUtils.setField(strategy, "fromName", FROM_NAME);
    }

    private NotificationDataModel buildNotification() {
        NotificationDataModel notification = new NotificationDataModel();
        notification.setTenantId(TENANT_ID);
        notification.setTargetUserId(USER_ID);
        notification.setTitle(TEST_TITLE);
        notification.setContent(TEST_CONTENT);
        return notification;
    }

    @Nested
    @DisplayName("Channel Identity")
    class ChannelIdentity {

        @Test
        @DisplayName("Should return EMAIL as the delivery channel")
        void shouldReturnEmailChannel() {
            // Given — strategy instance

            // When
            DeliveryChannel channel = strategy.getChannel();

            // Then
            assertThat(channel).isEqualTo(DeliveryChannel.EMAIL);
            verifyNoInteractions(javaMailSender, emailRepository);
        }
    }

    @Nested
    @DisplayName("Delivery")
    class Delivery {

        @Test
        @DisplayName("Should return SENT when email send succeeds")
        void shouldReturnSent_whenSendSucceeds() {
            // Given
            NotificationDataModel notification = buildNotification();
            MimeMessage mimeMessage = new MimeMessage((Session) null);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(javaMailSender).send(mimeMessage);

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_EMAIL);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
            assertThat(result.failureReason()).isNull();
            verify(javaMailSender, times(1)).createMimeMessage();
            verify(javaMailSender, times(1)).send(mimeMessage);
            verifyNoMoreInteractions(javaMailSender, emailRepository);
        }

        @Test
        @DisplayName("Should return FAILED when MailException occurs during send")
        void shouldReturnFailed_whenMailExceptionOccurs() {
            // Given
            NotificationDataModel notification = buildNotification();
            MimeMessage mimeMessage = new MimeMessage((Session) null);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailSendException(MAIL_SEND_ERROR))
                    .when(javaMailSender).send(mimeMessage);

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_EMAIL);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(result.failureReason()).isEqualTo(
                    String.format(EmailDeliveryChannelStrategy.ERROR_SEND_FAILED,
                            RECIPIENT_EMAIL, MAIL_SEND_ERROR));
            verify(javaMailSender, times(1)).createMimeMessage();
            verify(javaMailSender, times(1)).send(mimeMessage);
            verifyNoMoreInteractions(javaMailSender, emailRepository);
        }

        @Test
        @DisplayName("Should return FAILED when MessagingException occurs during preparation")
        void shouldReturnFailed_whenMessagingExceptionOccurs() throws MessagingException {
            // Given
            NotificationDataModel notification = buildNotification();
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MessagingException(MESSAGING_ERROR))
                    .when(mimeMessage).setRecipient(Message.RecipientType.TO,
                            new InternetAddress(RECIPIENT_EMAIL));

            // When
            DeliveryResult result = strategy.deliver(notification, RECIPIENT_EMAIL);

            // Then
            assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(result.failureReason()).isEqualTo(
                    String.format(EmailDeliveryChannelStrategy.ERROR_SEND_FAILED,
                            RECIPIENT_EMAIL, MESSAGING_ERROR));
            verify(javaMailSender, times(1)).createMimeMessage();
            verifyNoMoreInteractions(javaMailSender, emailRepository);
        }
    }
}
