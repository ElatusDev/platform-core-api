/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import openapi.akademiaplus.domain.notification.system.dto.EmailRecipientsDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailDeliveryResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailNotificationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@DisplayName("ImmediateSendUseCase")
@ExtendWith(MockitoExtension.class)
class ImmediateSendUseCaseTest {

    private static final String DEFAULT_FROM_ADDRESS = "test@example.com";
    private static final String DEFAULT_FROM_NAME = "Test Sender";
    private static final String TEST_SUBJECT = "Test Email Subject";
    private static final String TEST_BODY = "<p>Hello from test</p>";
    private static final String RECIPIENT_ONE = "recipient1@example.com";
    private static final String RECIPIENT_TWO = "recipient2@example.com";
    private static final String RECIPIENT_THREE = "recipient3@example.com";
    private static final String MAIL_EXCEPTION_MESSAGE = "SMTP connection refused";

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private ApplicationContext applicationContext;

    private ImmediateSendUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ImmediateSendUseCase(javaMailSender, applicationContext);
        ReflectionTestUtils.setField(useCase, "defaultFromAddress", DEFAULT_FROM_ADDRESS);
        ReflectionTestUtils.setField(useCase, "defaultFromName", DEFAULT_FROM_NAME);
    }

    private ImmediateEmailNotificationRequestDTO buildRequest(List<String> toRecipients) {
        EmailRecipientsDTO recipients = new EmailRecipientsDTO();
        recipients.setTo(toRecipients);

        ImmediateEmailNotificationRequestDTO dto = new ImmediateEmailNotificationRequestDTO();
        dto.setSubject(TEST_SUBJECT);
        dto.setBody(TEST_BODY);
        dto.setRecipients(recipients);
        return dto;
    }

    @Nested
    @DisplayName("Send")
    class Send {

        @Test
        @DisplayName("Should send to all recipients when all succeed")
        void shouldSendToAllRecipients_whenAllSucceed() {
            // Given
            ImmediateEmailNotificationRequestDTO dto = buildRequest(List.of(RECIPIENT_ONE, RECIPIENT_TWO));

            MimeMessage mimeMessage1 = new MimeMessage((Session) null);
            MimeMessage mimeMessage2 = new MimeMessage((Session) null);
            when(javaMailSender.createMimeMessage())
                    .thenReturn(mimeMessage1)
                    .thenReturn(mimeMessage2);
            doNothing().when(javaMailSender).send(mimeMessage1);
            doNothing().when(javaMailSender).send(mimeMessage2);

            // When
            ImmediateEmailDeliveryResponseDTO response = useCase.send(dto);

            // Then
            assertThat(response.getMessage()).isEqualTo(ImmediateSendUseCase.MESSAGE_SENT);
            assertThat(response.getDeliveryResults()).hasSize(2);
            assertThat(response.getDeliveryResults().get(0).getStatus())
                    .isEqualTo(ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO.StatusEnum.SUCCESS);
            assertThat(response.getDeliveryResults().get(0).getRecipientEmail()).isEqualTo(RECIPIENT_ONE);
            assertThat(response.getDeliveryResults().get(1).getStatus())
                    .isEqualTo(ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO.StatusEnum.SUCCESS);
            assertThat(response.getDeliveryResults().get(1).getRecipientEmail()).isEqualTo(RECIPIENT_TWO);
        }

        @Test
        @DisplayName("Should return failed status when MailException occurs")
        void shouldReturnFailedStatus_whenMailExceptionOccurs() {
            // Given
            ImmediateEmailNotificationRequestDTO dto = buildRequest(List.of(RECIPIENT_ONE));

            MimeMessage mimeMessage = new MimeMessage((Session) null);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailSendException(MAIL_EXCEPTION_MESSAGE))
                    .when(javaMailSender).send(mimeMessage);

            // When
            ImmediateEmailDeliveryResponseDTO response = useCase.send(dto);

            // Then
            assertThat(response.getDeliveryResults()).hasSize(1);
            assertThat(response.getDeliveryResults().get(0).getStatus())
                    .isEqualTo(ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO.StatusEnum.FAILED);
            assertThat(response.getDeliveryResults().get(0).getRecipientEmail()).isEqualTo(RECIPIENT_ONE);
            assertThat(response.getDeliveryResults().get(0).getErrorMessage())
                    .isEqualTo(String.format(ImmediateSendUseCase.ERROR_SEND_FAILED,
                            RECIPIENT_ONE, MAIL_EXCEPTION_MESSAGE));
        }

        @Test
        @DisplayName("Should send to remaining recipients when one fails partially")
        void shouldSendToRemainingRecipients_whenOneFailsPartially() {
            // Given
            ImmediateEmailNotificationRequestDTO dto = buildRequest(
                    List.of(RECIPIENT_ONE, RECIPIENT_TWO, RECIPIENT_THREE));

            MimeMessage mimeMessage1 = new MimeMessage((Session) null);
            MimeMessage mimeMessage2 = new MimeMessage((Session) null);
            MimeMessage mimeMessage3 = new MimeMessage((Session) null);
            when(javaMailSender.createMimeMessage())
                    .thenReturn(mimeMessage1)
                    .thenReturn(mimeMessage2)
                    .thenReturn(mimeMessage3);

            doNothing().when(javaMailSender).send(mimeMessage1);
            doThrow(new MailSendException(MAIL_EXCEPTION_MESSAGE))
                    .when(javaMailSender).send(mimeMessage2);
            doNothing().when(javaMailSender).send(mimeMessage3);

            // When
            ImmediateEmailDeliveryResponseDTO response = useCase.send(dto);

            // Then
            assertThat(response.getDeliveryResults()).hasSize(3);
            assertThat(response.getDeliveryResults().get(0).getStatus())
                    .isEqualTo(ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO.StatusEnum.SUCCESS);
            assertThat(response.getDeliveryResults().get(0).getRecipientEmail()).isEqualTo(RECIPIENT_ONE);
            assertThat(response.getDeliveryResults().get(1).getStatus())
                    .isEqualTo(ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO.StatusEnum.FAILED);
            assertThat(response.getDeliveryResults().get(1).getRecipientEmail()).isEqualTo(RECIPIENT_TWO);
            assertThat(response.getDeliveryResults().get(2).getStatus())
                    .isEqualTo(ImmediateEmailDeliveryResponseDeliveryResultsInnerDTO.StatusEnum.SUCCESS);
            assertThat(response.getDeliveryResults().get(2).getRecipientEmail()).isEqualTo(RECIPIENT_THREE);
        }
    }
}
