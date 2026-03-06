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
import openapi.akademiaplus.domain.notification.system.dto.EmailAttachmentDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailRecipientsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmailCreationUseCase")
@ExtendWith(MockitoExtension.class)
class EmailCreationUseCaseTest {

    private static final String TEST_SUBJECT = "Welcome Email";
    private static final String TEST_BODY = "<p>Hello World</p>";
    private static final String TEST_SENDER = "sender@example.com";
    private static final String DEFAULT_FROM_ADDRESS = "test@example.com";
    private static final String RECIPIENT_TO = "user@example.com";
    private static final String RECIPIENT_CC = "cc@example.com";
    private static final String RECIPIENT_BCC = "bcc@example.com";
    private static final String ATTACHMENT_FILENAME = "report.pdf";
    private static final URI ATTACHMENT_URL = URI.create("https://storage.example.com/report.pdf");

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private EmailRepository emailRepository;

    private EmailCreationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EmailCreationUseCase(applicationContext, emailRepository);
        ReflectionTestUtils.setField(useCase, "defaultFromAddress", DEFAULT_FROM_ADDRESS);
    }

    private EmailRecipientsDTO buildRecipients() {
        EmailRecipientsDTO recipients = new EmailRecipientsDTO();
        recipients.setTo(List.of(RECIPIENT_TO));
        return recipients;
    }

    private List<EmailAttachmentDTO> buildAttachments() {
        EmailAttachmentDTO attachment = new EmailAttachmentDTO();
        attachment.setFilename(ATTACHMENT_FILENAME);
        attachment.setUrl(ATTACHMENT_URL);
        return List.of(attachment);
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Should save and flush email when create is called")
        void shouldSaveAndFlushEmail_whenCreateCalled() {
            // Given
            EmailRecipientsDTO recipients = buildRecipients();
            EmailDataModel email = new EmailDataModel();
            when(applicationContext.getBean(EmailDataModel.class)).thenReturn(email);
            when(applicationContext.getBean(EmailRecipientDataModel.class)).thenReturn(new EmailRecipientDataModel());
            when(emailRepository.saveAndFlush(email)).thenReturn(email);

            // When
            useCase.create(TEST_SUBJECT, TEST_BODY, TEST_SENDER, recipients, null);

            // Then
            verify(emailRepository).saveAndFlush(email);
        }
    }

    @Nested
    @DisplayName("Transform")
    class Transform {

        @Test
        @DisplayName("Should set subject and body on the email entity")
        void shouldSetSubjectAndBody() {
            // Given
            EmailRecipientsDTO recipients = buildRecipients();
            EmailDataModel email = new EmailDataModel();
            when(applicationContext.getBean(EmailDataModel.class)).thenReturn(email);
            when(applicationContext.getBean(EmailRecipientDataModel.class)).thenReturn(new EmailRecipientDataModel());

            // When
            EmailDataModel result = useCase.transform(TEST_SUBJECT, TEST_BODY, TEST_SENDER, recipients, null);

            // Then
            assertThat(result.getSubject()).isEqualTo(TEST_SUBJECT);
            assertThat(result.getBody()).isEqualTo(TEST_BODY);
        }

        @Test
        @DisplayName("Should create recipients when 'to' addresses are provided")
        void shouldCreateRecipients_whenToProvided() {
            // Given
            EmailRecipientsDTO recipients = buildRecipients();
            EmailDataModel email = new EmailDataModel();
            EmailRecipientDataModel recipientModel = new EmailRecipientDataModel();
            when(applicationContext.getBean(EmailDataModel.class)).thenReturn(email);
            when(applicationContext.getBean(EmailRecipientDataModel.class)).thenReturn(recipientModel);

            // When
            EmailDataModel result = useCase.transform(TEST_SUBJECT, TEST_BODY, TEST_SENDER, recipients, null);

            // Then
            assertThat(result.getRecipients()).hasSize(1);
            assertThat(result.getRecipients().get(0).getRecipientEmail()).isEqualTo(RECIPIENT_TO);
            assertThat(result.getRecipients().get(0).getEmail()).isEqualTo(email);
        }

        @Test
        @DisplayName("Should create recipients for to, cc, and bcc addresses")
        void shouldCreateRecipients_whenAllTypesProvided() {
            // Given
            EmailRecipientsDTO recipients = new EmailRecipientsDTO();
            recipients.setTo(List.of(RECIPIENT_TO));
            recipients.setCc(List.of(RECIPIENT_CC));
            recipients.setBcc(List.of(RECIPIENT_BCC));

            EmailDataModel email = new EmailDataModel();
            when(applicationContext.getBean(EmailDataModel.class)).thenReturn(email);
            when(applicationContext.getBean(EmailRecipientDataModel.class))
                    .thenReturn(new EmailRecipientDataModel())
                    .thenReturn(new EmailRecipientDataModel())
                    .thenReturn(new EmailRecipientDataModel());

            // When
            EmailDataModel result = useCase.transform(TEST_SUBJECT, TEST_BODY, TEST_SENDER, recipients, null);

            // Then
            assertThat(result.getRecipients()).hasSize(3);
        }

        @Test
        @DisplayName("Should create attachments when provided")
        void shouldCreateAttachments_whenProvided() {
            // Given
            EmailRecipientsDTO recipients = buildRecipients();
            List<EmailAttachmentDTO> attachments = buildAttachments();
            EmailDataModel email = new EmailDataModel();
            EmailAttachmentDataModel attachmentModel = new EmailAttachmentDataModel();
            when(applicationContext.getBean(EmailDataModel.class)).thenReturn(email);
            when(applicationContext.getBean(EmailRecipientDataModel.class)).thenReturn(new EmailRecipientDataModel());
            when(applicationContext.getBean(EmailAttachmentDataModel.class)).thenReturn(attachmentModel);

            // When
            EmailDataModel result = useCase.transform(TEST_SUBJECT, TEST_BODY, TEST_SENDER, recipients, attachments);

            // Then
            assertThat(result.getAttachments()).hasSize(1);
            assertThat(result.getAttachments().get(0).getAttachmentUrl()).isEqualTo(ATTACHMENT_URL.toString());
            assertThat(result.getAttachments().get(0).getEmail()).isEqualTo(email);
        }

        @Test
        @DisplayName("Should use default sender when sender is null")
        void shouldUseDefaultSender_whenSenderIsNull() {
            // Given
            EmailRecipientsDTO recipients = buildRecipients();
            EmailDataModel email = new EmailDataModel();
            when(applicationContext.getBean(EmailDataModel.class)).thenReturn(email);
            when(applicationContext.getBean(EmailRecipientDataModel.class)).thenReturn(new EmailRecipientDataModel());

            // When
            EmailDataModel result = useCase.transform(TEST_SUBJECT, TEST_BODY, null, recipients, null);

            // Then
            assertThat(result.getSender()).isEqualTo(DEFAULT_FROM_ADDRESS);
        }

        @Test
        @DisplayName("Should use provided sender when sender is not null")
        void shouldUseProvidedSender_whenSenderIsNotNull() {
            // Given
            EmailRecipientsDTO recipients = buildRecipients();
            EmailDataModel email = new EmailDataModel();
            when(applicationContext.getBean(EmailDataModel.class)).thenReturn(email);
            when(applicationContext.getBean(EmailRecipientDataModel.class)).thenReturn(new EmailRecipientDataModel());

            // When
            EmailDataModel result = useCase.transform(TEST_SUBJECT, TEST_BODY, TEST_SENDER, recipients, null);

            // Then
            assertThat(result.getSender()).isEqualTo(TEST_SENDER);
        }
    }
}
